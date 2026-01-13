package com.zylr.pipbrowser.widgets;

import com.zylr.pipbrowser.PIPBrowser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Widget implements IWidget {
    public Minecraft mc = Minecraft.getInstance();

    private static final Logger LOGGER = PIPBrowser.LOGGER;

    public Properties config = null;
    protected IWidget parent;
    protected List<IWidget> children = new ArrayList<>();
    public int backgroundColor;
    public static final int DEFAULT_BACKGROUND_COLOR = 1258291200;
    public boolean isParent;

    protected int x;
    protected int y;
    protected int anchorX;
    protected int anchorY;
    public double relativeAnchorx;
    public double relativeAnchorY;
    protected int widgetWidth;
    protected int widgetHeight;
    protected int width;
    protected int height;
    protected double widthPercent;
    protected double heightPercent;

    protected WidgetType type;
    protected boolean visible;
    boolean isBottomHalf;
    boolean isTopHalf;
    boolean isLeftSide;
    boolean isRightSide;

    List<Button> buttons;

    public Widget() {
        this.type = null;
        this.width = mc.getWindow().getWidth();
        this.height = mc.getWindow().getHeight();
        this.visible = true;
        buttons = new ArrayList<>();
    }

    // Config File
    public void setupConfig() {
        File configFile = this.getType().getFile();
        Properties chatConfig = new Properties();
        try {
            if (!configFile.exists()) {
                LOGGER.info("making file");
                configFile.createNewFile();
                // Load defaults for config files
                InputStream chatConfigInput = this.getClass().getClassLoader().getResourceAsStream(this.getType().getDefaultFile());
                chatConfig.load(chatConfigInput);
                // Save default config files
                OutputStream chatConfigOutput = new FileOutputStream(this.getType().getFile().getPath());
                chatConfig.store(chatConfigOutput, null);
            }
            InputStream input = new FileInputStream(this.getType().getFile().getPath());
            this.config = new Properties();
            // Load config
            this.config.load(input);
        }catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    public void resetConfig() {
        try {
            File configFile = this.getType().getFile();

            // Delete the existing config file if it exists
            if (configFile.exists()) {
                if (!configFile.delete()) {
                    LOGGER.error("Failed to delete config file: " + configFile.getPath());
                    return;
                }
            }

            // Regenerate the config with defaults
            setupConfig();

            LOGGER.info("Config file reset to defaults: " + configFile.getPath());

        } catch (Exception ex) {
            LOGGER.error("Error resetting config file: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void saveConfig() {
        try (OutputStream output = new FileOutputStream(this.getType().getFile().getPath())) {
            // Save the config
            this.config.store(output, null);
        }catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public Properties getConfig() {
        return this.config;
    }

    public void render(GuiGraphics gui) {this.fixAnchors();}

    public void renderChildren() {

    }

    public void drawBackground(GuiGraphics gui) {
        gui.fill(this.getLeftSide(), this.getTop(), this.getRightSide(), this.getBottom(), this.backgroundColor);
    }
    public void drawBackground(GuiGraphics gui, int color) {
        gui.fill(this.getLeftSide(), this.getTop(), this.getRightSide(), this.getBottom(), color);
    }

    public List<Button> getButtons() {
        return List.of();
    }

    public int getTop() {
        return this.anchorY;
    }

    public int getBottom() {
        return this.anchorY + widgetHeight;
    }

    public int getLeftSide() {
        return this.anchorX;
    }

    public int getRightSide() {
        return this.anchorX + widgetWidth;
    }

    public boolean isTopHalf() {
        this.isTopHalf = this.anchorY < mc.getWindow().getGuiScaledHeight() / 2;
        return false;
    }

    public boolean isBottomHalf() {
        this.isBottomHalf = this.anchorY > mc.getWindow().getGuiScaledHeight() / 2;
        return false;
    }

    public boolean isLeftSide() {
        this.isLeftSide = this.anchorX + this.widgetWidth/2 < mc.getWindow().getGuiScaledWidth() / 2;
        return this.isLeftSide;
    }
    public boolean isRightSide() {
        this.isRightSide = this.anchorX + this.widgetWidth/2 > mc.getWindow().getGuiScaledWidth() / 2;
        return this.isRightSide;
    }

    public void fixAnchors() {
        if (getRightSide() > (mc.getWindow().getGuiScaledWidth()))
            this.setRelativeX((int)((mc.getWindow().getGuiScaledWidth())) - this.widgetWidth);
        if (getLeftSide() < 0)
            this.setRelativeX(0);
        if (getBottom() > (mc.getWindow().getGuiScaledHeight()))
            this.setRelativeY((int)(mc.getWindow().getGuiScaledHeight()) - this.widgetHeight);
        if (getTop() < 0)
            this.setRelativeY(0);
    }

    public boolean isHovered() {
        double pointerX = (mc.mouseHandler.xpos()/mc.getWindow().getGuiScale());
        double pointerY = (mc.mouseHandler.ypos()/mc.getWindow().getGuiScale());

        return pointerX > getLeftSide() && pointerX < getRightSide() && pointerY > getTop() && pointerY < getBottom();
    }

    public void setWidgetWidth(int width) {
        this.widgetWidth = width;
    }
    public void setWidgetHeight(int height) {
        this.widgetHeight = height;
    }

    public double setRelativeX(int anchorX) {

        this.relativeAnchorx = (double)anchorX / mc.getWindow().getGuiScaledWidth();
        this.getAnchorXFromRelative(this.relativeAnchorx);
        return this.relativeAnchorx;
    }
    public double setRelativeY(int anchorY) {
        this.relativeAnchorY = (double)anchorY / mc.getWindow().getGuiScaledHeight();
        this.getAnchorYFromRelative(this.relativeAnchorY);
        return this.relativeAnchorY;
    }
    public int getAnchorXFromRelative(double relativeAnchorX) {
        this.relativeAnchorx = relativeAnchorX;
        this.anchorX = (int) (this.relativeAnchorx * mc.getWindow().getGuiScaledWidth());
        return this.anchorX;
    }
    public int getAnchorYFromRelative(double relativeAnchorY) {
        this.relativeAnchorY = relativeAnchorY;
        this.anchorY = (int) (this.relativeAnchorY * mc.getWindow().getGuiScaledHeight());
        return this.anchorY;
    }

    public int getAnchorX() {
        return this.anchorX;
    }

    public int getAnchorY() {
        return this.anchorY;
    }

    public void setAnchorX(int x) {
        this.anchorX = x;
    }

    public void setAnchorY(int y) {
        this.anchorY = y;
    }

    public int getWidgetWidth() {
        return this.widgetWidth;
    }

    public int getWidgetHeight() {
        return this.widgetHeight;
    }

    public WidgetType getType() {
        return this.type;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return this.visible;
    }

    public List<IWidget> getChildren() {
        return this.children;
    }

    public IWidget getParent() {
        return this.parent;
    }

    @Override
    public double getRelativeAnchorX() {
        return this.relativeAnchorx;
    }

    @Override
    public double getRelativeAnchorY() {
        return this.relativeAnchorY;
    }

    public ResourceLocation getHighlight() { return null; }

    public double getWidthPercent() {
        return widthPercent;
    }

    public void setWidthPercent(double widthPercent) {
        this.widthPercent = widthPercent;
    }

    public double getHeightPercent() {
        return heightPercent;
    }

    public void setHeightPercent(double heightPercent) {
        this.heightPercent = heightPercent;
    }
}
