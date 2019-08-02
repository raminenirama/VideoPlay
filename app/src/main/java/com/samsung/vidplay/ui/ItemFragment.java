package com.samsung.vidplay.ui;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.samsung.vidplay.R;
import com.samsung.vidplay.utils.CarouselLinearLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

public class ItemFragment extends Fragment {

    private static final String POSITON = "position";
    private static final String SCALE = "scale";

    private int screenWidth;
    private int screenHeight;
    private ImageView imageView;

    private ArrayList<String> imageFilesPathList = new ArrayList<>();

    public static Fragment newInstance(MainActivity context, int pos, float scale) {
        Bundle b = new Bundle();
        b.putInt(POSITON, pos);
        b.putFloat(SCALE, scale);
        return Fragment.instantiate(context, ItemFragment.class.getName(), b);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWidthAndHeight();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null) {
            return null;
        }

        final int position = Objects.requireNonNull(this.getArguments()).getInt(POSITON);
        float scale = this.getArguments().getFloat(SCALE);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(screenWidth / 2, screenHeight / 2);
        LinearLayout linearLayout = (LinearLayout) inflater.inflate(R.layout.fragment_image, container, false);

        TextView textView = linearLayout.findViewById(R.id.text);
        CarouselLinearLayout root = linearLayout.findViewById(R.id.root_container);
        imageView = linearLayout.findViewById(R.id.pagerImg);
        //getImagesFromSDCARD();
        textView.setText("Music: " + position);
        imageView.setLayoutParams(layoutParams);
        Drawable drawable = Drawable.createFromPath(imageFilesPathList.get(position));
        imageView.setImageDrawable(drawable);
        root.setScaleBoth(scale);
        return linearLayout;
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void onEvent(String imageFilePath) {
        if (!TextUtils.isEmpty(imageFilePath) && !imageFilesPathList.isEmpty()) {
            for (String imagePathFromList : imageFilesPathList) {
                if (imagePathFromList.equalsIgnoreCase(imageFilePath)) {
                    MainActivity.FIRST_PAGE = imageFilesPathList.indexOf(imageFilePath);
                }
            }
        }
    }

    /**
     * Get device screen width and height
     */
    private void getWidthAndHeight() {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        Objects.requireNonNull(getActivity()).getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        screenHeight = displaymetrics.heightPixels;
        screenWidth = displaymetrics.widthPixels;
    }

    private void getImagesFromSDCARD() {
        File file = new File(Environment.getExternalStorageDirectory(), "CuraContents/images");
        if (file.isDirectory()) {
            File[] listFile = file.listFiles();
            for (File file1 : listFile) {
                imageFilesPathList.add(file1.getAbsolutePath());
            }
        }
    }
}
