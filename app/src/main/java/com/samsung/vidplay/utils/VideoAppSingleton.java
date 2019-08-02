package com.samsung.vidplay.utils;

import java.util.ArrayList;

public enum VideoAppSingleton {

    INSTANCE;

    int totalCountOfImage=0;

    ArrayList<String> imageFilesPathList;

    public void setTotalCountOfImage(int totalCountOfImage) {
        this.totalCountOfImage = totalCountOfImage;
    }

    public int getTotalCountOfImage() {
        return totalCountOfImage;
    }

    public void setImageFilesPathList(ArrayList<String> imageFilesPathList) {
        this.imageFilesPathList = imageFilesPathList;
    }

    public ArrayList<String> getImageFilesPathList() {
        return imageFilesPathList;
    }
}