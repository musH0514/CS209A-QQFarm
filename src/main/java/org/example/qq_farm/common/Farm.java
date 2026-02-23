package org.example.qq_farm.common;

import static org.example.qq_farm.common.PlotState.*;

public class Farm {
    private Plot[][] farm;
    private int money;

    public Farm() {
        this.farm = new Plot[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                farm[i][j] = new Plot(i, j);
                farm[i][j].setState(EMPTY);
                farm[i][j].setCrop(null);
            }
        }
        this.money = 10000;
    }

    public void setMoney(int n) {
        this.money = n;
    }
    public int getMoney() {
        return money;
    }
    public Plot getPlot(int x, int y) {
        return farm[x][y];
    }
}