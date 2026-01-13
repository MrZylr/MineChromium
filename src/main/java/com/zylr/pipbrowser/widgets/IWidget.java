package com.zylr.pipbrowser.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Properties;

public interface IWidget {

    void setupConfig();
    void saveConfig();
    Properties getConfig();

    void render(GuiGraphics gui);
    void renderChildren();
    void drawBackground(GuiGraphics gui);
    void drawBackground(GuiGraphics gui, int color);
    List<Button> getButtons();

    int getTop();
    int getBottom();
    int getLeftSide();
    int getRightSide();
    boolean isTopHalf();
    boolean isBottomHalf();
    boolean isLeftSide();
    boolean isRightSide();
    boolean isHovered();
    public double setRelativeX(int anchorX);
    public double setRelativeY(int anchorY);
    public int getAnchorXFromRelative(double relativeAnchorX);
    public int getAnchorYFromRelative(double relativeAnchorY);

    int getAnchorX();
    int getAnchorY();
    void setAnchorX(int x);
    void setAnchorY(int y);
    int getWidgetWidth();
    int getWidgetHeight();
    boolean isVisible();
    void setVisible(boolean visible);

    void fixAnchors();

    WidgetType getType();
    List<IWidget> getChildren();
    IWidget getParent();

    double getRelativeAnchorX();

    double getRelativeAnchorY();

    ResourceLocation getHighlight();

    public double getWidthPercent();
    public void setWidthPercent(double widthPercent);
    public double getHeightPercent();
    public void setHeightPercent(double heightPercent);
}
