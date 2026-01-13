package com.zylr.pipbrowser.widgets;

import java.io.File;

public enum WidgetType {
    BROWSER(new File("browser/properties/widgets/browser.properties"), "configs/browserconfig.properties");


    private File file;
    private String defaultFile;

    WidgetType(File file, String defaultFile){
        this.file = file;
        this.defaultFile = defaultFile;
    }

    public File getFile() { return this.file; }
    public String getDefaultFile() { return this.defaultFile; }
}
