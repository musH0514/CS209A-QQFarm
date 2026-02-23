package org.example.qq_farm.common;

public enum Crop {
    RICE("rice", 10),
    CORN("corn", 15),
    WHEAT("wheat", 20);

    private String cropName;
    private int price;
    Crop(String cropName, int price) {
        this.cropName = cropName;
        this.price = price;
    }

    public String getCropName() {
        return cropName;
    }
    public int getPrice() {
        return price;
    }
}
