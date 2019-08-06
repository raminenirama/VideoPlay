package com.samsung.vidplay.utils;

import com.samsung.vidplay.infobean.PlayListInfo;

import java.util.Hashtable;

public enum VideoAppSingleton {

    INSTANCE;

    private int positionOfTrack=0;

    private Hashtable<Integer, PlayListInfo> imageFilesPathList;

    public Hashtable<Integer, PlayListInfo> getImageFilesPathList() {
        return imageFilesPathList;
    }

    public void setImageFilesPathList(Hashtable<Integer, PlayListInfo> imageFilesPathList) {
        this.imageFilesPathList = imageFilesPathList;
    }

    public int getTotalCountOfImage() {
        return imageFilesPathList.size();
    }

    public int getPositionOfTrack() {
        return positionOfTrack;
    }

    public void setPositionOfTrack(int positionOfTrack) {
        this.positionOfTrack = positionOfTrack;
    }
}