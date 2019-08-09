package com.samsung.vidplay.utils;

import android.os.Environment;

import com.samsung.vidplay.infobean.PlayListInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;

public class PlayListManager {

    private Hashtable<Integer, PlayListInfo> fileMap = new Hashtable<>();
    private String defaultImagePath = "";

    public PlayListManager() {
        //empty constructor
    }

    public void getMediaContentFromSDCARD() {

        ArrayList<String> albumFilesPathList = getImagesPathList();
        ArrayList<String> trackFilesPathList = getTrackPathList();

        //to get default image file from sd-card
        String MEDIA_IMAGES_PATH = "CuraContents/images/default_image";
        File fileImages = new File(Environment.getExternalStorageDirectory(), MEDIA_IMAGES_PATH);
        defaultImagePath = fileImages.getAbsolutePath();

        if (albumFilesPathList != null && trackFilesPathList != null) {
            for (int i = 0; i < trackFilesPathList.size(); i++) {
                PlayListInfo info = new PlayListInfo();
                if (i >= albumFilesPathList.size()) {
                    info.setImagePath(defaultImagePath);
                    info.setTrackPath(trackFilesPathList.get(i));
                } else {
                    String albumName = albumFilesPathList.get(i);
                    String albumNameNew = albumName.substring(albumName.lastIndexOf('/')+1);
                    String albumString=stripExtension(albumNameNew);
                    for (String albumTrack : trackFilesPathList) {
                        String albumTrackNew = albumTrack.substring(albumTrack.lastIndexOf('/')+1);
                        String albumTrackString=stripExtension(albumTrackNew);
                        if (albumString.equals(albumTrackString)) {
                            info.setImagePath(albumFilesPathList.get(i));
                            info.setTrackPath(trackFilesPathList.get(i));
                        }
                    }
                }
                fileMap.put(i, info);
            }
            VideoAppSingleton.INSTANCE.setImageFilesPathList(fileMap);
        }
    }

    public String stripExtension(String s) {
        return s != null && s.lastIndexOf(".") > 0 ? s.substring(0, s.lastIndexOf(".")) : s;
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
