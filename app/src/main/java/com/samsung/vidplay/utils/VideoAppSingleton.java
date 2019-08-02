package com.samsung.vidplay.utils;

public enum VideoAppSingleton {
    INSTANCE;

    int totalCountOfImage=0;

    public void setTotalCountOfImage(int totalCountOfImage) {
        this.totalCountOfImage = totalCountOfImage;
    }

    public int getTotalCountOfImage() {
        return totalCountOfImage;
    }
}
