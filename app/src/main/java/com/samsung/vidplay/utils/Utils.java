package com.samsung.vidplay.utils;

import android.os.Environment;

import java.io.File;

public class Utils {

    public static String getDefaultImage() {
        String MEDIA_IMAGES_PATH = "CuraContents/images/default_image.png";
        File fileImages = new File(Environment.getExternalStorageDirectory(), MEDIA_IMAGES_PATH);
        return fileImages.getAbsolutePath();
    }


    public static boolean isMusicMp3(File file) {
        final String REGEX = "(.*/)*.+\\.(mp3)$";
        return file.getName().matches(REGEX);
    }
}
