package com.seankrail.coursegrabber.java;

import android.graphics.Color;

/**
 * Created by Krail on 3/14/15.
 */
public enum ColorEnum {
    WHITE("#CCCCCC"), BLACK("#444444"),
    RED("#FF0033"), RED_YELLOW("#FF9933"),
    YELLOW("#339933"), GREEN_CYAN("#33CC99"),
    CYAN("#33FFFF"), CYAN_BLUE("#33CCCC"),
    BLUE("#3366FF"), BLUE_MAGENTA("#9933FF"),
    MAGENTA("#CC33FF"), MAGENTA_REDf("#CC3399");

    private final String color;

    ColorEnum(String color) {this.color = color;}

    public String getColor() {return this.color;}
}
