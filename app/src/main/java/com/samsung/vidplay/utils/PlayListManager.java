package com.samsung.vidplay.utils;

import android.os.Environment;

import com.samsung.vidplay.infobean.PlayListInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;

public class PlayListManager {

    private Hashtable<Integer, PlayListInfo> fileMap = new Hashtable<>();

    public PlayListManager() {
        //empty constructor
    }

    public void getMediaContentFromSDCARD() {

        ArrayList<String> imageFilesPathList = getImagesPathList();
        ArrayList<String> trackFilesPathList = getTrackPathList();

        if (imageFilesPathList != null && trackFilesPathList != null) {
            if (imageFilesPathList.size() == trackFilesPathList.size()) {
                for (int i = 0; i < imageFilesPathList.size(); i++) {
                    PlayListInfo info = new PlayListInfo();
                    info.setImagePath(imageFilesPathList.get(i));
                    info.setTrackPath(trackFilesPathList.get(i));
                    fileMap.put(i, info);
                }
                VideoAppSingleton.INSTANCE.setImageFilesPathList(fileMap);
            }
        }
    }

    /*---Return Tracks Path List---*/
    private ArrayList<String> getTrackPathList() {
        String MEDIA_TRACKS_PATH = "CuraContents/tracks";
        File fileTracks = new File(Environment.getExternalStorageDirectory(), MEDIA_TRACKS_PATH);
        if (fileTracks.isDirectory()) {
            File[] listTracksFile = fileTracks.listFiles();
            ArrayList<String> trackFilesPathList = new ArrayList<>();
            for (File file1 : listTracksFile) {
                trackFilesPathList.add(file1.getAbsolutePath());
            }
            return trackFilesPathList;
        }
        return null;
    }

    /*---Return Images Path List---*/
    private ArrayList<String> getImagesPathList() {
        String MEDIA_IMAGES_PATH = "CuraContents/images";
        File fileImages = new File(Environment.getExternalStorageDirectory(), MEDIA_IMAGES_PATH);
        if (fileImages.isDirectory()) {
            File[] listImagesFile = fileImages.listFiles();
            ArrayList<String> imageFilesPathList = new ArrayList<>();
            for (File file1 : listImagesFile) {
                imageFilesPathList.add(file1.getAbsolutePath());
            }
            return imageFilesPathList;
        }
        return null;
    }
}
