package com.samsung.vidplay.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.text.TextUtils;

import com.samsung.vidplay.infobean.PlayListInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Hashtable;

public class PlayListManager {

    private Hashtable<Integer, PlayListInfo> fileMap = new Hashtable<>();
    private String defaultImagePath = "";
    private ArrayList<String> imageFilesPathList;
    private ArrayList<String> trackFilesPathList;
    private Context mContext;

    public PlayListManager(Context context) {
        mContext = context;
    }

    public void getMediaContentFromSDCARD() {

        imageFilesPathList = getImagesPathList();
        trackFilesPathList = getTrackPathList();
        defaultImagePath = Utils.getDefaultImage();

        if (imageFilesPathList != null && trackFilesPathList != null) {
            for (int i = 0; i < trackFilesPathList.size(); i++) {
                PlayListInfo info = new PlayListInfo();
                String trackFilePath = trackFilesPathList.get(i);
                String trackFileNameWithExt = trackFilePath.substring(trackFilePath.lastIndexOf('/') + 1);
                String trackFileNameWithoutExt = stripExtension(trackFileNameWithExt);
                if (!checkIfTrackNameExistInImageFolder(trackFileNameWithoutExt)) {
                    String[] parts = trackFileNameWithExt.split("\\.");
                    String extensionPart2 = parts[1];
                    if (extensionPart2.equalsIgnoreCase("mp3")) {
                        String imagePathOfMp3 = getThumbnailPath(trackFilePath);
                        info.setImagePath(imagePathOfMp3);
                        info.setTrackPath(trackFilePath);
                    }
                    if (extensionPart2.equalsIgnoreCase("wav")) {
                        info.setImagePath(defaultImagePath);
                        info.setTrackPath(trackFilePath);
                    }
                } else {
                    String imageFilePath = getImageFilePathFromTrackName(trackFileNameWithoutExt);
                    if (!TextUtils.isEmpty(imageFilePath))
                        info.setImagePath(imageFilePath);
                    else
                        info.setImagePath(defaultImagePath);
                    info.setTrackPath(trackFilePath);
                }
                fileMap.put(i, info);
            }
            System.out.println("hashtable data is:" + fileMap.toString());
            VideoAppSingleton.INSTANCE.setImageFilesPathList(fileMap);
        }
    }

    private String getImageFilePathFromTrackName(String trackFileNameWithoutExt) {
        String imagePath = "";
        for (String imageFilePath : imageFilesPathList) {
            String imageOnlyWithoutPath = imageFilePath.substring(imageFilePath.lastIndexOf('/') + 1);
            String imageOnlyWithoutExt = stripExtension(imageOnlyWithoutPath);
            if (imageOnlyWithoutExt.equalsIgnoreCase(trackFileNameWithoutExt)) {
                imagePath = imageFilePath;
            }
        }
        return imagePath;
    }

    /**
     * get path of thumbnail from album path get from list
     */
    private String getThumbnailPath(String albumTrack) {
        String uriFile = "";
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(albumTrack);
        byte[] data = mmr.getEmbeddedPicture();
        if (data != null) {
            Bitmap bitmapAlbumCover = BitmapFactory.decodeByteArray(data, 0, data.length);
            String trackName = albumTrack.substring(albumTrack.lastIndexOf('/') + 1);
            String imageOnlyWithoutExt = stripExtension(trackName);
            String imagePath = saveImageOnSDCARD(imageOnlyWithoutExt, bitmapAlbumCover);
            if (TextUtils.isEmpty(imagePath)) {
                uriFile = defaultImagePath;
            } else
                uriFile = imagePath;
        }
        return uriFile;
    }

    private String saveImageOnSDCARD(String nameOfImage, Bitmap finalBitmap) {
        String filePath = "";
        String MEDIA_IMAGES_PATH = "CuraContents/images";
        File fileImages = new File(Environment.getExternalStorageDirectory(), MEDIA_IMAGES_PATH);
        File file = new File(fileImages, nameOfImage + ".png");
        if (file.exists())
            file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
            out.close();
            filePath = file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return filePath;
    }

    /*
     check if any image name exist as comparison to track name
      */
    public boolean checkIfTrackNameExistInImageFolder(String albumTrackString) {
        for (String albumImagePath : imageFilesPathList) {
            String imageOnlyWithoutPath = albumImagePath.substring(albumImagePath.lastIndexOf('/') + 1);
            String imageOnlyWithoutExt = stripExtension(imageOnlyWithoutPath);
            if (imageOnlyWithoutExt.equalsIgnoreCase(albumTrackString))
                return true;
        }
        return false;
    }

    /**
     * remove extension from source file to make comparison of image & track
     */
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

