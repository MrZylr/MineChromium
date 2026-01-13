package com.zylr.pipbrowser.screens;

import com.zylr.pipbrowser.widgets.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.awt.*;
import java.util.Properties;

public class ColorPickerScreen extends Screen {
    private final Widget widget;
    private final Screen parentScreen;
    private Minecraft mc = Minecraft.getInstance();

    private int selectedRed = 255;
    private int selectedGreen = 255;
    private int selectedBlue = 255;
    private int selectedAlpha = 255;

    private boolean draggingRed = false;
    private boolean draggingGreen = false;
    private boolean draggingBlue = false;
    private boolean draggingAlpha = false;
    private boolean draggingHue = false;
    private boolean draggingSaturation = false;

    private float hue = 0.0f;
    private float saturation = 1.0f;
    private float brightness = 1.0f;

    public ColorPickerScreen(Widget widget, Screen parentScreen) {
        super(Component.literal("Color Picker"));
        this.widget = widget;
        this.parentScreen = parentScreen;

        // Initialize with current widget background color
        Color current = new Color(widget.backgroundColor, true);
        this.selectedRed = current.getRed();
        this.selectedGreen = current.getGreen();
        this.selectedBlue = current.getBlue();
        this.selectedAlpha = current.getAlpha();

        float[] hsb = Color.RGBtoHSB(selectedRed, selectedGreen, selectedBlue, null);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Apply button
        this.addRenderableWidget(Button.builder(Component.literal("Apply"), button -> {
                    Color newColor = new Color(selectedRed, selectedGreen, selectedBlue, selectedAlpha);
                    widget.backgroundColor = newColor.getRGB();
                    Properties config = widget.getConfig();
                    config.setProperty("backgroundColor", Integer.toString(widget.backgroundColor));
                    widget.saveConfig();
                    this.minecraft.setScreen(parentScreen);
                })
                .bounds(centerX - 100, centerY + 120, 95, 20)
                .build());

        // Cancel button
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button ->
                        this.minecraft.setScreen(parentScreen))
                .bounds(centerX + 5, centerY + 120, 95, 20)
                .build());

        // Reset button
        this.addRenderableWidget(Button.builder(Component.literal("Reset"), button -> {
                    Color color = new Color(Widget.DEFAULT_BACKGROUND_COLOR);
                    selectedRed = color.getRed();
                    selectedGreen = color.getGreen();
                    selectedBlue = color.getBlue();
                    selectedAlpha = color.getAlpha();
                    updateHSB();
                })
                .bounds(centerX - 50, centerY + 145, 100, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        super.render(gui, mouseX, mouseY, partialTick);
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Title
        gui.drawCenteredString(mc.font, "Color Picker", centerX, centerY - 140, 0xFFFFFF);

        // Color preview
        int previewColor = new Color(selectedRed, selectedGreen, selectedBlue, selectedAlpha).getRGB();
        gui.fill(centerX - 51, centerY - 121, centerX + 51, centerY - 89, 0xFFFFFFFF); // Border
        gui.fill(centerX - 50, centerY - 120, centerX + 50, centerY - 90, previewColor);

        // HSB Color wheel area
        drawColorWheel(gui, centerX - 100, centerY - 80, 200, 80);

        // RGB Sliders
        drawRGBSliders(gui, centerX, centerY);

        // Alpha slider
        drawAlphaSlider(gui, centerX, centerY + 60);

        // Color values text
        gui.drawString(mc.font, String.format("R: %d G: %d B: %d A: %d",
                        selectedRed, selectedGreen, selectedBlue, selectedAlpha),
                centerX - 80, centerY + 85, 0xFFFFFF);

        gui.drawString(mc.font, String.format("Hex: #%02X%02X%02X%02X",
                        selectedAlpha, selectedRed, selectedGreen, selectedBlue),
                centerX - 80, centerY + 100, 0xFFFFFF);

    }

    private void drawColorWheel(GuiGraphics gui, int x, int y, int width, int height) {
        // Hue bar
        for (int i = 0; i < width; i++) {
            float h = (float) i / width;
            Color color = Color.getHSBColor(h, 1.0f, 1.0f);
            gui.fill(x + i, y, x + i + 1, y + 20, color.getRGB());
        }

        // Hue selector
        int hueX = x + (int) (hue * width);
        gui.fill(hueX - 1, y - 2, hueX + 1, y + 22, 0xFFFFFFFF);

        // Saturation/Brightness area
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < 40; j++) {
                float s = (float) i / width;
                float b = 1.0f - (float) j / 40;
                Color color = Color.getHSBColor(hue, s, b);
                gui.fill(x + i, y + 25 + j, x + i + 1, y + 26 + j, color.getRGB());
            }
        }

        // Saturation/Brightness selector
        int satX = x + (int) (saturation * width);
        int brightY = y + 25 + (int) ((1.0f - brightness) * 40);
        gui.fill(satX - 2, brightY - 2, satX + 2, brightY + 2, 0xFFFFFFFF);
    }

    private void drawRGBSliders(GuiGraphics gui, int centerX, int centerY) {
        int sliderY = centerY + 10;
        int sliderWidth = 200;
        int sliderHeight = 10;

        // Red slider
        for (int i = 0; i < sliderWidth; i++) {
            int red = (int) ((float) i / sliderWidth * 255);
            Color color = new Color(red, selectedGreen, selectedBlue);
            gui.fill(centerX - 100 + i, sliderY, centerX - 100 + i + 1, sliderY + sliderHeight, color.getRGB());
        }
        int redX = centerX - 100 + (int) ((float) selectedRed / 255 * sliderWidth);
        gui.fill(redX - 1, sliderY - 2, redX + 1, sliderY + sliderHeight + 2, 0xFFFFFFFF);
        gui.drawString(mc.font, "R", centerX - 115, sliderY + 1, 0xFFFFFF);

        // Green slider
        sliderY += 15;
        for (int i = 0; i < sliderWidth; i++) {
            int green = (int) ((float) i / sliderWidth * 255);
            Color color = new Color(selectedRed, green, selectedBlue);
            gui.fill(centerX - 100 + i, sliderY, centerX - 100 + i + 1, sliderY + sliderHeight, color.getRGB());
        }
        int greenX = centerX - 100 + (int) ((float) selectedGreen / 255 * sliderWidth);
        gui.fill(greenX - 1, sliderY - 2, greenX + 1, sliderY + sliderHeight + 2, 0xFFFFFFFF);
        gui.drawString(mc.font, "G", centerX - 115, sliderY + 1, 0xFFFFFF);

        // Blue slider
        sliderY += 15;
        for (int i = 0; i < sliderWidth; i++) {
            int blue = (int) ((float) i / sliderWidth * 255);
            Color color = new Color(selectedRed, selectedGreen, blue);
            gui.fill(centerX - 100 + i, sliderY, centerX - 100 + i + 1, sliderY + sliderHeight, color.getRGB());
        }
        int blueX = centerX - 100 + (int) ((float) selectedBlue / 255 * sliderWidth);
        gui.fill(blueX - 1, sliderY - 2, blueX + 1, sliderY + sliderHeight + 2, 0xFFFFFFFF);
        gui.drawString(mc.font, "B", centerX - 115, sliderY + 1, 0xFFFFFF);
    }

    private void drawAlphaSlider(GuiGraphics gui, int centerX, int centerY) {
        int sliderWidth = 200;
        int sliderHeight = 10;

        // Checkerboard background for alpha
        for (int i = 0; i < sliderWidth; i += 8) {
            for (int j = 0; j < sliderHeight; j += 8) {
                Color checker = ((i + j) % 16 == 0) ? Color.WHITE : Color.LIGHT_GRAY;
                gui.fill(centerX - 100 + i, centerY + j,
                        centerX - 100 + Math.min(i + 8, sliderWidth),
                        centerY + Math.min(j + 8, sliderHeight),
                        checker.getRGB());
            }
        }

        // Alpha gradient
        for (int i = 0; i < sliderWidth; i++) {
            int alpha = (int) ((float) i / sliderWidth * 255);
            Color color = new Color(selectedRed, selectedGreen, selectedBlue, alpha);
            gui.fill(centerX - 100 + i, centerY, centerX - 100 + i + 1, centerY + sliderHeight, color.getRGB());
        }

        int alphaX = centerX - 100 + (int) ((float) selectedAlpha / 255 * sliderWidth);
        gui.fill(alphaX - 1, centerY - 2, alphaX + 1, centerY + sliderHeight + 2, 0xFFFFFFFF);
        gui.drawString(mc.font, "A", centerX - 115, centerY + 1, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean b) {
        if (e.button() == 0) {
            int centerX = this.width / 2;
            int centerY = this.height / 2;

            // Check hue bar
            if (e.x() >= centerX - 100 && e.x() <= centerX + 100 &&
                    e.y() >= centerY - 80 && e.y() <= centerY - 60) {
                draggingHue = true;
                updateHue(e.x(), centerX);
                return true;
            }

            // Check saturation/brightness area
            if (e.x() >= centerX - 100 && e.x() <= centerX + 100 &&
                    e.y() >= centerY - 55 && e.y() <= centerY - 15) {
                draggingSaturation = true;
                updateSaturationBrightness(e.x(), e.y(), centerX, centerY);
                return true;
            }

            // Check RGB sliders
            if (e.x() >= centerX - 100 && e.x() <= centerX + 100) {
                if (e.y() >= centerY + 10 && e.y() <= centerY + 20) {
                    draggingRed = true;
                    updateRed(e.x(), centerX);
                    return true;
                } else if (e.y() >= centerY + 25 && e.y() <= centerY + 35) {
                    draggingGreen = true;
                    updateGreen(e.x(), centerX);
                    return true;
                } else if (e.y() >= centerY + 40 && e.y() <= centerY + 50) {
                    draggingBlue = true;
                    updateBlue(e.x(), centerX);
                    return true;
                } else if (e.y() >= centerY + 60 && e.y() <= centerY + 70) {
                    draggingAlpha = true;
                    updateAlpha(e.x(), centerX);
                    return true;
                }
            }
        }

        return super.mouseClicked(e, b);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double deltaX, double deltaY) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (draggingHue) {
            updateHue(e.x(), centerX);
            return true;
        } else if (draggingSaturation) {
            updateSaturationBrightness(e.x(), e.y(), centerX, centerY);
            return true;
        } else if (draggingRed) {
            updateRed(e.x(), centerX);
            return true;
        } else if (draggingGreen) {
            updateGreen(e.x(), centerX);
            return true;
        } else if (draggingBlue) {
            updateBlue(e.x(), centerX);
            return true;
        } else if (draggingAlpha) {
            updateAlpha(e.x(), centerX);
            return true;
        }

        return super.mouseDragged(e, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        draggingHue = false;
        draggingSaturation = false;
        draggingRed = false;
        draggingGreen = false;
        draggingBlue = false;
        draggingAlpha = false;
        return super.mouseReleased(e);
    }

    private void updateHue(double mouseX, int centerX) {
        hue = Mth.clamp((float) (mouseX - (centerX - 100)) / 200, 0.0f, 1.0f);
        updateRGBFromHSB();
    }

    private void updateSaturationBrightness(double mouseX, double mouseY, int centerX, int centerY) {
        saturation = Mth.clamp((float) (mouseX - (centerX - 100)) / 200, 0.0f, 1.0f);
        brightness = Mth.clamp(1.0f - (float) (mouseY - (centerY - 55)) / 40, 0.0f, 1.0f);
        updateRGBFromHSB();
    }

    private void updateRed(double mouseX, int centerX) {
        selectedRed = Mth.clamp((int) ((mouseX - (centerX - 100)) / 200 * 255), 0, 255);
        updateHSB();
    }

    private void updateGreen(double mouseX, int centerX) {
        selectedGreen = Mth.clamp((int) ((mouseX - (centerX - 100)) / 200 * 255), 0, 255);
        updateHSB();
    }

    private void updateBlue(double mouseX, int centerX) {
        selectedBlue = Mth.clamp((int) ((mouseX - (centerX - 100)) / 200 * 255), 0, 255);
        updateHSB();
    }

    private void updateAlpha(double mouseX, int centerX) {
        selectedAlpha = Mth.clamp((int) ((mouseX - (centerX - 100)) / 200 * 255), 0, 255);
    }

    private void updateRGBFromHSB() {
        Color color = Color.getHSBColor(hue, saturation, brightness);
        selectedRed = color.getRed();
        selectedGreen = color.getGreen();
        selectedBlue = color.getBlue();
    }

    private void updateHSB() {
        float[] hsb = Color.RGBtoHSB(selectedRed, selectedGreen, selectedBlue, null);
        hue = hsb[0];
        saturation = hsb[1];
        brightness = hsb[2];
    }
}