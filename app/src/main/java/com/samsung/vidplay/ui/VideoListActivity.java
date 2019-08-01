package com.samsung.vidplay.ui;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.samsung.vidplay.R;


public class VideoListActivity extends BaseActivity 
{
    private static final String LOGTAG = "VidPlay.VideoListAct";

    private Cursor cursor;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_video_list );

        ListView videoList = findViewById( R.id.videoList );
        cursor = getVideosCursor();
        String[] fromColumns = { MediaStore.Video.Media.TITLE, MediaStore.Video.Media._ID, MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.DURATION, MediaStore.Video.Media.RESOLUTION };
        int[] toViews = { R.id.videoName, R.id.videoId, R.id.videoType, R.id.videoDuration, R.id.videoResolution };

        SimpleCursorAdapter adapter = new SimpleCursorAdapter( this, R.layout.item_video, cursor, fromColumns, toViews, 0 );

        videoList.setAdapter( adapter );
        videoList.setOnItemSelectedListener( new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected( AdapterView<?> adapterView, View view, int i, long id ) {
                Log.i( LOGTAG, "onItemSelected: i: "+ i +", id: "+ id );
            }
            @Override
            public void onNothingSelected( AdapterView<?> adapterView ) {
            }
        } );
        videoList.setOnItemClickListener( new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick( AdapterView<?> adapterView, View view, int i, long id ) {
                Log.i( LOGTAG, "onItemClick: i: "+ i +", id: "+ id );
//                getApp().setFeatureVideoId( id );
                finish();
            }
        } );
    }

    @Override
    protected void onDestroy() {
        if ( cursor != null ) {
            cursor.close();
        }
        super.onDestroy();
    }

    private Cursor getVideosCursor() {
        Uri movieUri = null;
        ContentResolver contentResolver = getContentResolver();
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

        Cursor cursor = contentResolver.query( uri, null, null, null, "title");
        if (cursor == null) {
            // query failed, handle error.
            Log.i( LOGTAG, "loadMedia: uri: "+ uri );
        } else if ( !cursor.moveToFirst() ) {
            // no media on the device
            Log.w( LOGTAG, "loadMedia: no media on the device: uri: "+ uri );
        } 
        return cursor;
    }

}
