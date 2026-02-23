package org.example.qq_farm.common;

import static org.example.qq_farm.common.PlotState.*;
import static org.example.qq_farm.common.Protocol.*;

public class Plot {
    private PlotState state;
    private Crop crop;
    private int amount;
    private int x;   //坐标
    private int y;

    public Plot(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setCrop(Crop crop) {
        this.crop = crop;
    }
    public void setState(PlotState state) {
        this.state = state;
        if (state == EMPTY) {
            this.amount = emptyAmount;
        }
        if (state == GROWING) {
            this.amount = plantAmount;
        }
    }
    public void setAmount(int amount) {
        this.amount = amount;
    }
    public void decreaseAmount(int value) {
        this.amount -= value;   //减去value
        if (this.amount < 0) {
            this.amount = 0;
        }
    }

    public PlotState getState() {
        return state;
    }
    public Crop getCrop() {
        return crop;
    }
    public int getAmount() {
        return amount;
    }
    public int getX() {
        return x;
    }
    public int getY() {
        return y;
    }
}
