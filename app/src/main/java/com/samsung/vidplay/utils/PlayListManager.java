package com.samsung.vidplay.utils;

import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.provider.MediaStore;

import com.samsung.vidplay.infobean.PlayListInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.regex.Pattern;

public class PlayListManager {

    private Hashtable<Integer, PlayListInfo> fileMap = new Hashtable<>();
    private String defaultImagePath = "";
    private ArrayList<String> albumFilesPathList;
    private ArrayList<String> trackFilesPathList;

    public PlayListManager() {
        //empty constructor
    }

    public void getMediaContentFromSDCARD() {

        Pattern pattern = Pattern.compile("^([a-zA-Z]+)([0-9]+)(.*)");

        albumFilesPathList = getImagesPathList();
        trackFilesPathList = getTrackPathList();

        //to get default image file from sd-card
        //or for handling in case image not found for mp3/wav files
        String MEDIA_IMAGES_PATH = "CuraContents/images/default_image";
        File fileImages = new File(Environment.getExternalStorageDirectory(), MEDIA_IMAGES_PATH);
        defaultImagePath = fileImages.getAbsolutePath();

        if (albumFilesPathList != null && trackFilesPathList != null) {

            for (int i = 0; i < trackFilesPathList.size(); i++) {
                PlayListInfo info = new PlayListInfo();
                if (i >= albumFilesPathList.size() - 1) {
                    info.setImagePath(defaultImagePath);
                    info.setTrackPath(trackFilesPathList.get(i));
                } else {

                    String albumName = albumFilesPathList.get(i);
                    String albumNameNew = albumName.substring(albumName.lastIndexOf('/') + 1);
                    String albumString = stripExtension(albumNameNew);

                    for (String albumTrack : trackFilesPathList) {
                        String albumTrackNew = albumTrack.substring(albumTrack.lastIndexOf('/') + 1);
                        String albumTrackString = stripExtension(albumTrackNew);

                        if (!checkIfTrackNameExistInImageFolder(albumTrackString)) {
                            String[] parts = albumTrackNew.split(".");
                            String extension = parts[1];
                            if (extension.equalsIgnoreCase("mp3")) {
                                /*Cursor cursor = MediaStore.Images.Thumbnails.queryMiniThumbnails(
                                        getContentResolver(), selectedImageUri,
                                        MediaStore.Images.Thumbnails.MINI_KIND,
                                        null );
                                if( cursor != null && cursor.getCount() > 0 ) {
                                    cursor.moveToFirst();
                                    String uri = cursor.getString(cursor.getColumnIndex( MediaStore.Images.Thumbnails.DATA ) );
                                }*/
                            }
                            if (extension.equalsIgnoreCase("wav")) {
                                info.setImagePath(defaultImagePath);
                            }
                        } else {
                            if (albumString.equals(albumTrackString)) {
                                info.setImagePath(albumFilesPathList.get(i));
                                info.setTrackPath(trackFilesPathList.get(i));
                            }
                        }
                    }
                }
                fileMap.put(i, info);
            }
            System.out.println("hashtable data is:" + fileMap.toString());
            VideoAppSingleton.INSTANCE.setImageFilesPathList(fileMap);
        }
    }

    public boolean checkIfTrackNameExistInImageFolder(String albumTrackString) {
        for (String albumImagePath : albumFilesPathList) {
            String imageOnlyWithoutPath = albumImagePath.substring(albumImagePath.lastIndexOf('/') + 1);
            String imageOnlyWithoutExt = stripExtension(imageOnlyWithoutPath);
            if (imageOnlyWithoutExt.equalsIgnoreCase(albumTrackString))
                return true;
        }
        return false;
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
