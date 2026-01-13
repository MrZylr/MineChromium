package com.zylr.pipbrowser.hud;

import com.zylr.pipbrowser.Config;
import com.zylr.pipbrowser.widgets.BrowserWidget;

public class MainHud extends Hud {

    private BrowserWidget browserWidget;

    public MainHud() {
        super();
        this.browserWidget = null;
        this.fillHudList();
    }

    @Override
    public void fillHudList() {
        this.widgets.clear();
        this.widgets.add(browserWidget);
    }

    public BrowserWidget getBrowserWidget() { return this.browserWidget; }
    public void setBrowserWidget(BrowserWidget browserWidget) {
        this.browserWidget = browserWidget;
    }
}
