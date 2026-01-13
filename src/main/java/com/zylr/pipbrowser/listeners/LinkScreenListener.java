package com.zylr.pipbrowser.listeners;

import com.zylr.pipbrowser.PIPBrowser;
import com.zylr.pipbrowser.screens.BrowserScreen;
import com.zylr.pipbrowser.widgets.BrowserWidget;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.client.gui.components.events.GuiEventListener;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.util.function.Consumer;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Listener that intercepts ConfirmLinkScreen to open links in the in-game browser
 */
public class LinkScreenListener {

    // Keep track of which runtime classes we've dumped to avoid spamming logs
    private static final Set<String> dumpedRuntimeClasses = Collections.synchronizedSet(new HashSet<>());

    public static void register() {
        AtomicReference<String> capturedUrl = new AtomicReference<>();

        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof ConfirmLinkScreen confirmLinkScreen) {

                try {
                    Class<?> runtimeClass = confirmLinkScreen.getClass();
                    // First attempt: common deobf field name on runtime class
                    try {
                        Field urlField = runtimeClass.getDeclaredField("url");
                        urlField.setAccessible(true);
                        String url = (String) urlField.get(confirmLinkScreen);
                        if (isValidUrl(url)) {
                            capturedUrl.set(url);
                            PIPBrowser.LOGGER.info("[PIPBrowser] Captured URL from ConfirmLinkScreen field 'url': {}", url);
                            return;
                        }
                    } catch (NoSuchFieldException ignored) {
                        // fall through to scanning fields
                    }

                    // Fallback: scan all String fields on runtime class and pick the one that contains an http(s) URL
                    for (Field f : runtimeClass.getDeclaredFields()) {
                        if (f.getType() == String.class) {
                            try {
                                f.setAccessible(true);
                                Object val = f.get(confirmLinkScreen);
                                if (val instanceof String s && isValidUrl(s)) {
                                    capturedUrl.set(s);
                                    PIPBrowser.LOGGER.info("[PIPBrowser] Captured URL from ConfirmLinkScreen field '{}': {}", f.getName(), s);
                                    break;
                                }
                            } catch (Exception inner) {
                                // ignore and continue scanning other fields
                            }
                        }
                    }

                    if (capturedUrl.get() == null) {
                        PIPBrowser.LOGGER.warn("Could not find URL field on ConfirmLinkScreen (runtime may be obfuscated)");
                    }
                } catch (Exception e) {
                    PIPBrowser.LOGGER.warn("Failed to get URL from ConfirmLinkScreen: {}", e.getMessage(), e);
                }
            }
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof ConfirmLinkScreen && capturedUrl.get() != null) {
                String url = capturedUrl.get();

                // Do not auto-open: let the ConfirmLinkScreen appear and inject our button into it.
                // capturedUrl will be cleared after we either inject or handle the user's action.

                // Find the 'Yes' like button by label
                screen.children().stream()
                     .filter(widget -> widget instanceof Button)
                     .map(widget -> (Button) widget)
                            // Match by rendered string text for robustness across locales/obfuscation
                            .filter(button -> {
                                try {
                                    String label = button.getMessage() == null ? "" : button.getMessage().getString();
                                    String yesLabel = CommonComponents.GUI_YES.getString();
                                    return label.equals(yesLabel) || label.equalsIgnoreCase("yes") || label.toLowerCase().contains("open") || label.toLowerCase().contains("proceed");
                                } catch (Throwable t) {
                                    return false;
                                }
                            })
                     .findFirst()
                     .ifPresent(yesButton -> {
                        int x = yesButton.getX();
                        int y = yesButton.getY();
                        int width = yesButton.getWidth();
                        int height = yesButton.getHeight();

                        // Try to remove the original button: prefer a method that accepts the actual runtime button class,
                        // otherwise try removing from any List fields (renderables/children)
                        try {
                            // Try to patch the button's action directly (safer than removing/replacing)
                            boolean patched = patchButtonAction(yesButton, url);
                            if (patched) {
                                capturedUrl.set(null);
                                return;
                            }

                            Method removeMethod = findMethodForArg(screen.getClass(), yesButton.getClass(), "remove");
                            if (removeMethod != null) {
                                removeMethod.setAccessible(true);
                                Object[] args = buildArgsForMethod(removeMethod.getParameterTypes(), yesButton);
                                removeMethod.invoke(screen, args);
                            } else {
                                boolean removed = tryRemoveFromListField(screen, yesButton);
                                if (!removed) {
                                    try {
                                        Method rm = findMethodForArg(Screen.class, yesButton.getClass(), "remove");
                                        if (rm != null) {
                                            rm.setAccessible(true);
                                            Object[] rargs = buildArgsForMethod(rm.getParameterTypes(), yesButton);
                                            rm.invoke(screen, rargs);
                                        }
                                    } catch (Exception ignored) {
                                        // ignore
                                    }
                                }
                            }
                        } catch (Exception e) {
                            PIPBrowser.LOGGER.warn("Failed to remove button: {}", e.getMessage());
                        }

                        Button newYesButton = Button.builder(CommonComponents.GUI_YES, (button) -> {
                            if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                                Minecraft minecraft = Minecraft.getInstance();

                                if (PIPBrowser.getInstance().mainHud.getBrowserWidget() == null) {
                                    PIPBrowser.getInstance().mainHud.setBrowserWidget(new BrowserWidget(0, 0, 267, 150, url));
                                    PIPBrowser.getInstance().mainHud.fillHudList();
                                }

                                // Open the BrowserScreen with the URL, which will create a new tab
                                minecraft.setScreen(null);
                                minecraft.setScreen(new BrowserScreen(url));
                            } else {
                                client.setScreen(null);
                            }
                            capturedUrl.set(null);
                        }).bounds(x, y, width, height).build();

                        // Add our replacement button using a robust reflection strategy; fall back to inserting into List fields
                         try {
                             Method addMethod = findMethodForArg(screen.getClass(), newYesButton.getClass(), "add");
                             if (addMethod != null) {
                                 addMethod.setAccessible(true);
                                 Object[] args = buildArgsForMethod(addMethod.getParameterTypes(), newYesButton);
                                 addMethod.invoke(screen, args);
                             } else {
                                 boolean added = tryAddToListField(screen, newYesButton);
                                 if (!added) {
                                     try {
                                         Method am = findMethodForArg(Screen.class, newYesButton.getClass(), "add");
                                         if (am != null) {
                                             am.setAccessible(true);
                                             Object[] aargs = buildArgsForMethod(am.getParameterTypes(), newYesButton);
                                             am.invoke(screen, aargs);
                                         }
                                     } catch (Exception e) {
                                        PIPBrowser.LOGGER.warn("Failed to add button: {}", e.getMessage());
                                     }
                                 }
                             }
                         } catch (Exception e) {
                            PIPBrowser.LOGGER.warn("Failed to add button: {}", e.getMessage());
                         }
                     });
             }
         });
    }

    private static boolean isValidUrl(String s) {
        return s != null && (s.startsWith("http://") || s.startsWith("https://"));
    }

    /**
     * Find a method on the runtime class hierarchy that accepts the provided argument type (by runtime class)
     * Prefer methods with parameter count 1 (add/remove widget) and optionally whose name contains a hint.
     */
    private static Method findMethodForArg(Class<?> startClass, Class<?> argClass, String nameHint) {
        Class<?> c = startClass;
        while (c != null) {
            for (Method m : c.getDeclaredMethods()) {
                Class<?>[] params = m.getParameterTypes();
                // prefer single-arg methods where param is assignable from actual arg class
                if (params.length == 1 && params[0].isAssignableFrom(argClass)) {
                    if (nameHint == null || m.getName().toLowerCase().contains(nameHint)) return m;
                    return m;
                }
                // accept two-arg variants where one param accepts the arg
                if (params.length == 2) {
                    if (params[0].isAssignableFrom(argClass) || params[1].isAssignableFrom(argClass)) {
                        if (nameHint == null || m.getName().toLowerCase().contains(nameHint)) return m;
                        return m;
                    }
                }
            }
            if (c == Screen.class) break;
            c = c.getSuperclass();
        }

        // lastly check Screen.class explicitly
        for (Method m : Screen.class.getDeclaredMethods()) {
            Class<?>[] params = m.getParameterTypes();
            if (params.length == 1 && params[0].isAssignableFrom(argClass)) {
                if (nameHint == null || m.getName().toLowerCase().contains(nameHint)) return m;
                return m;
            }
            if (params.length == 2) {
                if (params[0].isAssignableFrom(argClass) || params[1].isAssignableFrom(argClass)) {
                    if (nameHint == null || m.getName().toLowerCase().contains(nameHint)) return m;
                    return m;
                }
            }
        }
        return null;
    }

    private static Object[] buildArgsForMethod(Class<?>[] params, Object widget) {
        Object[] args = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            Class<?> p = params[i];
            if (GuiEventListener.class.isAssignableFrom(p) || p.isAssignableFrom(widget.getClass())) {
                args[i] = widget;
            } else if (p == int.class) {
                args[i] = 0;
            } else if (p == boolean.class) {
                args[i] = false;
            } else {
                args[i] = null;
            }
        }
        return args;
    }

    /**
     * Attempt to add the widget into any List fields on the screen (renderables/children lists).
     */
    private static boolean tryAddToListField(Screen screen, Object widget) {
        for (Field f : screen.getClass().getDeclaredFields()) {
            if (java.util.List.class.isAssignableFrom(f.getType())) {
                try {
                    f.setAccessible(true);
                    Object val = f.get(screen);
                    if (val instanceof List list) {
                        // try add
                        try {
                            list.add(widget);
                            return true;
                        } catch (Throwable ignored) {
                            // continue
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        // also try Screen.class fields
        for (Field f : Screen.class.getDeclaredFields()) {
            if (java.util.List.class.isAssignableFrom(f.getType())) {
                try {
                    f.setAccessible(true);
                    Object val = f.get(screen);
                    if (val instanceof List list) {
                        try {
                            list.add(widget);
                            return true;
                        } catch (Throwable ignored) {
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        return false;
    }

    private static boolean tryRemoveFromListField(Screen screen, Object widget) {
        for (Field f : screen.getClass().getDeclaredFields()) {
            if (java.util.List.class.isAssignableFrom(f.getType())) {
                try {
                    f.setAccessible(true);
                    Object val = f.get(screen);
                    if (val instanceof List list) {
                        try {
                            if (list.remove(widget)) return true;
                        } catch (Throwable ignored) {
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        for (Field f : Screen.class.getDeclaredFields()) {
            if (java.util.List.class.isAssignableFrom(f.getType())) {
                try {
                    f.setAccessible(true);
                    Object val = f.get(screen);
                    if (val instanceof List list) {
                        try {
                            if (list.remove(widget)) return true;
                        } catch (Throwable ignored) {
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        return false;
    }

    /**
     * Try to replace the button's press handler by finding a functional-interface field and setting a Proxy
     * that opens the internal BrowserScreen with the provided URL.
     */
     private static boolean patchButtonAction(Object buttonObj, String url) {
         if (buttonObj == null) return false;
         Class<?> bc = buttonObj.getClass();
         try {
             for (Field f : bc.getDeclaredFields()) {
                 try {
                     Class<?> ft = f.getType();
                     if (!ft.isInterface() && !Consumer.class.isAssignableFrom(ft)) continue;
                     // Determine candidate method(s) on the interface
                     Method[] methods = ft.getMethods();
                     Method target = null;
                     for (Method m : methods) {
                         if (m.getDeclaringClass() == Object.class) continue;
                         // prefer single-arg methods or void methods
                         target = m;
                         break;
                     }
                     if (target == null) continue;

                     f.setAccessible(true);
                     Object proxy = Proxy.newProxyInstance(ft.getClassLoader(), new Class[]{ft}, (InvocationHandler) (p, method, args) -> {
                         // When pressed, open internal browser
                         try {
                             if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                                 Minecraft minecraft = Minecraft.getInstance();
                                 if (PIPBrowser.getInstance().mainHud.getBrowserWidget() == null) {
                                     PIPBrowser.getInstance().mainHud.setBrowserWidget(new BrowserWidget(0, 0, 267, 150, url));
                                     PIPBrowser.getInstance().mainHud.fillHudList();
                                 }
                                 minecraft.setScreen(null);
                                 minecraft.setScreen(new BrowserScreen(url));
                             }
                         } catch (Throwable t) {
                             // patched action failed; ignore and continue
                         }
                         return null;
                     });

                     // Set proxy into field
                     f.set(buttonObj, proxy);
                     // patched button field
                     return true;
                 } catch (Throwable ignored) {
                     // try next field
                 }
             }
         } catch (Throwable t) {
            PIPBrowser.LOGGER.warn("patchButtonAction failed: {}", t.getMessage());
         }
         return false;
     }

 }
