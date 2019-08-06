package com.samsung.vidplay.ui;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.viewpager.widget.ViewPager;

import com.samsung.mars.base.Message;
import com.samsung.mars.base.Response;
import com.samsung.mars.util.Log;
import com.samsung.mars.util.Runner;
import com.samsung.smesh.client.ClientMessageHandler;
import com.samsung.smesh.client.SmeshProxy;
import com.samsung.smesh.util.FileUtils;
import com.samsung.vidplay.R;
import com.samsung.vidplay.VidPlayApp;
import com.samsung.vidplay.controllers.CarouselPagerAdapter;
import com.samsung.vidplay.interfaces.GetImagePositionCallback;
import com.samsung.vidplay.utils.PlayListManager;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends BaseActivity implements SurfaceHolder.Callback, GetImagePositionCallback {
    private static final String LOGTAG = "VidPlay.MainAct";

    private enum State {
        IDLE,
        INITIALIZED,
        PREPARING,
        PREPARED,
        STARTED,
        PAUSED,
        STOPED,
        PLAYBACK_COMPLETE,
        ERROR,
        END,
    }

    private State state = State.IDLE;

    public static final int PERM_READ_EXTERNAL_STORAGE = 1;
    public static final int PERM_WRITE_EXTERNAL_STORAGE = 2;
    public static final int PERM_READ_PHONE_STATE = 3;
    private int uncheckedPerms = 2;

    private MediaPlayer videoPlayer;
    private MediaPlayer audioPlayer;
    private MediaPlayer mediaPlayer;  // current player,  either videoPlayer or audioPlayer;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private View notPane;
    private View imagePane;
    private TextView timePast;
    private TextView timeTotal;
    private ImageView pastBar;

    private SmeshProxy smeshProxy;
    private boolean surfaceCreated;
    private boolean showingBars = true;
    private boolean sendStatus;
    private String requesterSmeshNode;
    private Timer statusTimer;
    private List<ClientMessageHandler> messageHandlers = new ArrayList<>();
    private PowerManager.WakeLock wakeLock;
    private int volume = 50;   // 0-100
    private int durationSecs;
    public ViewPager albumsPager;
    public CarouselPagerAdapter adapter;

    /**
     * You shouldn't define first page = 0.
     * Let define firstPage = 'number viewpager size' to make endless carousel
     */
    public static int FIRST_PAGE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkForPermissions();

        if (uncheckedPerms <= 0) {
            albumsPager = findViewById(R.id.albumspager);
            timeTotal = findViewById(R.id.timeTotal);
            timePast = findViewById(R.id.timePast);
            pastBar = findViewById(R.id.pastBar);

            getMediaContent();

            String defaultVideoName = "TheFrame.ts";
            String featureVideoName = null;

            Intent intent = getIntent();
            Bundle extras = intent.getExtras();
            Log.i(LOGTAG, "onCreate: extras: " + extras);
            if (extras != null) {
                String action = extras.getString("Action");
                Log.i(LOGTAG, "onCreate: action: '" + action + "'");
                String videoName = extras.getString("videoName");
                Log.i(LOGTAG, "onCreate: videoName: " + videoName);
                if ("LaunchAttractor".equals(action)) {
                    defaultVideoName = videoName;
                    Log.i(LOGTAG, "onCreate: defaultVideoName set: " + defaultVideoName);
                }
                if ("LaunchFeature".equals(action)) {
                    featureVideoName = videoName;
                    Log.i(LOGTAG, "onCreate: featureVideoName set: " + featureVideoName);
                }
            }
            boolean vidFound = initialize(defaultVideoName, featureVideoName);

            /////////////////////  setup staus timer and smesh proxy  ////////////////////////////////////////

            if (statusTimer != null) {
                statusTimer.cancel();
                statusTimer.purge();
                statusTimer = null;
            }
            statusTimer = new Timer();
            statusTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    int pos = 0;
                    if (mediaPlayer == null) {
                        mediaPlayer = new MediaPlayer();
                    }
                    if (mediaPlayer.isPlaying()) {
                        pos = mediaPlayer.getCurrentPosition();
                    }
                    updateProgressBar(pos / 1000);
                    if (sendStatus) {
                        Message message = new Message("VidPlay_PlayerStatus");
                        message.setExpectingResponse(false);
                        message.setParamInteger("postion", pos);
                        message.setParamInteger("volume", volume);
                        message.setParamBoolean("isPlaying", mediaPlayer.isPlaying());
                        message.setLoggable(false);
                        smeshProxy.sendRequest(message, requesterSmeshNode, null);
                    }
                }
            }, 900, 900);

            synchronized (this) {
                smeshProxy = getSmeshProxy();
                if (smeshProxy == null) {
                    smeshProxy = SmeshProxy.getInstance();
                    setSmeshProxy(smeshProxy);
                    Log.w(LOGTAG, "onCreate: calling smeshProxy.connect... ");
                    smeshProxy.connect(this).done(new Runner<String>() {
                        @Override
                        public void run(String connectMsg) {
                            synchronized (MainActivity.this) {
                                Log.w(LOGTAG, "onCreate: smeshProxy.connect done ");
                                registerHandlers();
                            }
//                        String val = smeshProxy.getConfigValue( "Camera", "cameraNodeName" );
//                        Log.i( LOGTAG, "smeshProxy.connect done: config value: cameraNodeName = "+ val );
                        }
                    }).fail(new Runner<String>() {
                        @Override
                        public void run(String connectMsg) {
                            Log.w(LOGTAG, "onCreate: Smesh proxy connect error: " + connectMsg);
                        }
                    });
                } else {
                    Log.w(LOGTAG, "onCreate: smeshProxy already defined ");
                    registerHandlers();
                }
            }

            if ("ODROID".equals(Build.BRAND)) {
                View decorView = getWindow().getDecorView();
                decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        // Note that system bars will only be "visible" if none of the
                        // LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
                        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                            Log.w(LOGTAG, "onSystemUiVisibilityChange: The system bars are visible, visibility: " + visibility);
                            hideSystemUI();
                        } else {
                            Log.w(LOGTAG, "onSystemUiVisibilityChange: The system bars are NOT visible, visibility: " + visibility);
                        }
                    }
                });
            }

            if (vidFound) {
                initializeVideoPlayer();
            }
        }
    }

    private void setPagerData(int imageFilePosition) {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int pageMargin = ((metrics.widthPixels / 4) * 2);
        albumsPager.setPageMargin(-pageMargin);

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        } else {
            adapter = new CarouselPagerAdapter(this, getSupportFragmentManager());
            albumsPager.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        }
        albumsPager.addOnPageChangeListener(adapter);
        albumsPager.setCurrentItem(imageFilePosition);
        albumsPager.setOffscreenPageLimit(10);
    }

    private void checkForPermissions() {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Log.d(LOGTAG, "onCreate: has permission: READ_EXTERNAL_STORAGE");
            uncheckedPerms--;
        } else {
            Log.w(LOGTAG, "onCreate: NO READ_EXTERNAL_STORAGE permission");
            String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};
            requestPermissions(permissions, PERM_READ_EXTERNAL_STORAGE);
        }
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Log.d(LOGTAG, "onCreate: has permission: WRITE_EXTERNAL_STORAGE");
            uncheckedPerms--;
        } else {
            Log.w(LOGTAG, "onCreate: NO WRITE_EXTERNAL_STORAGE permission");
            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
            requestPermissions(permissions, PERM_WRITE_EXTERNAL_STORAGE);
        }
    }

    @Override
    protected void onStart() {
        Log.i(LOGTAG, "onStart: ==========================");
        super.onStart();
        PowerManager powerMgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerMgr.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "WakeLock1");
        wakeLock.acquire();
//        if ( state == State.PAUSED ) {
//            start();
//        }
    }

    @Override
    protected void onRestart() {
        Log.i(LOGTAG, "onRestart: ==========================");
        super.onRestart();
    }

    @Override
    protected void onResume() {
        Log.i(LOGTAG, "onResume: ==========================");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.i(LOGTAG, "onPause: ==========================");
        super.onPause();
    }

    @Override
    protected void onStop() {
        wakeLock.release();
        finish();   //!! Warning: this will also kill this activity
//        pause();
        super.onStop();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.i(LOGTAG, "onRestoreInstanceState: ==========================");
        super.onRestoreInstanceState(savedInstanceState);
    }

    public void onActivityReenter(int resultCode, Intent data) {
        Log.i(LOGTAG, "onActivityReenter: ==========================");
        super.onActivityReenter(resultCode, data);
    }

    @Override
    public void onBackPressed() {
        if (!"ODROID".equals(Build.BRAND)) {   // block Back on ODROID
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void showNotification(final boolean show) {
        runOnUiThread(new Runnable() {
                          @Override
                          public void run() {
                              notPane.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
                          }
                      }
        );
    }

    private void showImage(final boolean show, final String imageFilename, final boolean autoExit, final int durationSeconds) {
        runOnUiThread(new Runnable() {
                          @Override
                          public void run() {
                              Log.i(LOGTAG, "showImage: show: " + show + ", imageFilename: " + imageFilename + ", autoExit: " + autoExit);
                              if (show) {
                                  File imageDir = new File(FileUtils.getCuraContentsDirectory(MainActivity.this), "images");
                                  File imagePath = new File(imageDir, imageFilename);
                                  String pathName = imagePath.getAbsolutePath();
                                  //send imagefilepath into EventBus
                                  EventBus.getDefault().postSticky(pathName);
                                  imagePane.setVisibility(View.VISIBLE);
                                  if (autoExit) {
                                      Log.i(LOGTAG, "showImage: scheduling exit timer for durationSeconds: " + durationSeconds);
                                      new Timer().schedule(new TimerTask() {
                                          @Override
                                          public void run() {
                                              imagePane.setVisibility(View.INVISIBLE);
                                              enableVolume(true);
                                          }
                                      }, durationSeconds * 1000);
                                  }
                              } else {
                                  imagePane.setVisibility(View.INVISIBLE);
                              }
                          }
                      }
        );
    }

    private void enableVolume(boolean enable) {
        float mediaVolume = 0;
        if (enable) {
            mediaVolume = volume / 100.0f;
        }
        Log.i(LOGTAG, "enableVolume: volume: " + volume + ", mediaVolume: " + mediaVolume + ", enable: " + enable);
        mediaPlayer.setVolume(mediaVolume, mediaVolume);
    }

    private void playTrack(String trackFilename, boolean loop, int startPosMillis) {
        Log.i(LOGTAG, "playTrack: trackFilename: " + trackFilename + ", loop: " + loop + ", startPosMillis: " + startPosMillis);
        enableVolume(false);
        if (audioPlayer != null) {
            audioPlayer.release();
        }

//        AudioManager audioMgr = (AudioManager) getSystemService( Context.AUDIO_SERVICE );
//        int res = audioMgr.requestAudioFocus( new AudioManager.OnAudioFocusChangeListener() {
//            @Override
//            public void onAudioFocusChange( int focusChange ) {
//                Log.i( LOGTAG, "playTrack: onAudioFocusChange: focusChange: "+ focusChange );
//            }
//        }, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN );

        File imageDir = new File(FileUtils.getCuraContentsDirectory(MainActivity.this), "tracks");
        File imagePath = new File(imageDir, trackFilename);

        audioPlayer = new MediaPlayer();
        audioPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer aPlayer) {
                // lets play the default attractor video
                stopTrack();
//                mediaPlayer = videoPlayer;
//                enableVolume( true );
//                showImage( false, null, false, 0 );
//                audioPlayer.release();
//                audioPlayer = null;
            }
        });

        try {
            audioPlayer.setLooping(loop);
            audioPlayer.setDataSource(imagePath.getAbsolutePath());
            audioPlayer.prepare();
            durationSecs = audioPlayer.getDuration() / 1000;

            Log.i(LOGTAG, "playTrack: start play music: startPosMillis: " + startPosMillis + ", durationSecs: " + durationSecs);
            audioPlayer.start();
            if (startPosMillis > 0)
                audioPlayer.seekTo(startPosMillis);
            mediaPlayer = audioPlayer;
//            float mediaVolume = volume / 100.0f;
//            mediaPlayer.setVolume( mediaVolume, mediaVolume );
            enableVolume(true);
            updateProgressBar(startPosMillis / 1000);
        } catch (IOException ex) {
            Log.i(LOGTAG, "playTrack: IOException: " + ex.getLocalizedMessage(), ex);
        }
    }

    private void updateProgressBar(final int posSecs) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (durationSecs > 0) {
                    int mins = durationSecs / 60;
                    int secs = durationSecs - (mins * 60);
                    timeTotal.setText(mins + (secs < 10 ? ":0" : ":") + secs);

                    int totalWith = findViewById(R.id.totalBar).getWidth();
                    ViewGroup.LayoutParams params = pastBar.getLayoutParams();
                    float newWidth = ((float) posSecs / (float) durationSecs) * totalWith;
                    params.width = (int) newWidth;
                    pastBar.setLayoutParams(params);
                }
                if (posSecs >= 0) {
                    int mins = posSecs / 60;
                    int secs = posSecs - (mins * 60);
                    timePast.setText(mins + (secs < 10 ? ":0" : ":") + secs);
                }
            }
        });
    }

    private void stopTrack() {
        mediaPlayer = videoPlayer;
        enableVolume(true);
        showImage(false, null, false, 0);
        audioPlayer.release();
        audioPlayer = null;
    }

    /**
     * Finds the videos
     * Sets the click listeners that can show the SystemUI
     *
     * @param defaultVideoName the default attractor video
     * @param featureVideoName video to play before the deafult attractor video, may be null for none
     */
    private boolean initialize(String defaultVideoName, String featureVideoName) {
        boolean found = findVideos(defaultVideoName, null);
        if (!found) {
            Log.w(LOGTAG, "initialize: default video not found: " + defaultVideoName);
            Toast.makeText(this, "default video '" + defaultVideoName + "' not found", Toast.LENGTH_LONG).show();
        }

        notPane = findViewById(R.id.notification);
        imagePane = findViewById(R.id.imagePane);

        if (!"ODROID".equals(Build.BRAND)) {
            View mainPane = findViewById(R.id.mainPane);
            mainPane.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (showingBars) {
                        hideSystemUI();
                    } else {
                        showSystemUI();
                    }
                    return true;
                }
            });
        }
        return found;
    }

    private void initializeVideoPlayer() {
        boolean loop = false;
        final VidPlayApp app = getApp();
        String videoFilename = app.getFeatureVideoFilename();
        Log.i(LOGTAG, "initializeVideoPlayer: feature videoFilename: " + videoFilename);
        if (videoFilename == null) {
            videoFilename = app.getDefaultVideoFilename();
            Log.i(LOGTAG, "initializeVideoPlayer: default videoFilename: " + videoFilename);
            loop = true;
        }
        //       Uri videoUri = getVideoUri( videoFilename );
        if (videoFilename != null) {
            if (videoPlayer == null) {
                surfaceView = findViewById(R.id.surface);
                surfaceHolder = surfaceView.getHolder();
                surfaceHolder.addCallback(this);

                videoPlayer = new MediaPlayer();
                mediaPlayer = videoPlayer;
                videoPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer videoPlayer, int what, int extra) {
                        Log.e(LOGTAG, "onError: what: " + what + ", extra: " + extra);
                        state = State.ERROR;
                        return false;
                    }
                });
                videoPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer videoPlayer) {
                        // lets play the default attractor video
                        Log.i(LOGTAG, "onCompletion: ========================  ");
                        state = State.PLAYBACK_COMPLETE;
                        app.setFeatureVideoFilename(null);
                        denitializePlayer();
                        initializeVideoPlayer();
                    }
                });
                videoPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                    @Override
                    public boolean onInfo(MediaPlayer videoPlayer, int what, int extra) {
                        Log.i(LOGTAG, "onInfo: what: " + what + ", extra: " + extra);
                        return false;
                    }
                });
                videoPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        Log.i(LOGTAG, "onPrepared: ========================  ");
                        state = State.PREPARED;
                    }
                });
            } else {
                reset();
            }
            Log.w(LOGTAG, "initializeVideoPlayer: videoPlayer: " + videoPlayer + ", videoFilename: " + videoFilename);
            setDataSource(videoFilename);

            prepare();

            videoPlayer.setLooping(loop);
            start();
        } else {
            Log.w(LOGTAG, "initializeVideoPlayer: Video not found, videoFilename: " + videoFilename);
        }
    }

    private void denitializePlayer() {
        Log.i(LOGTAG, "denitializePlayer");
        stop();
    }

    private String numSeq = "";

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.i(LOGTAG, "onKeyUp: ==========================keyCode: " + keyCode);
        if (keyCode > 6 && keyCode < 17) {
            int numPressed = keyCode - 7;
            numSeq = numSeq + numPressed;
            onNumberEntered(numSeq);
            return true;
        } else {
            numSeq = "";
        }
        return super.onKeyUp(keyCode, event);
    }

    private void onNumberEntered(String number) {
        if (number.equals("9009")) {
            stop();
            finish();
        } else if (number.equals("1010")) {
            startVideoListActivity();
        }
    }

    @Override
    protected void onDestroy() {
        Log.w(LOGTAG, "onDestroy: ==========================");
        denitializePlayer();
        if (statusTimer != null) {
            statusTimer.cancel();
            statusTimer.purge();
            statusTimer = null;
        }
        synchronized (this) {
            unregisterHandlers();
            if (smeshProxy != null) {
                smeshProxy.disconnect();
                smeshProxy = null;
            }
        }
        release();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_video:
                startVideoListActivity();
                break;
        }
        return true;
    }

    private void startVideoListActivity() {
        Intent intent = new Intent(getApplicationContext(), VideoListActivity.class);
        startActivity(intent);
    }

    //////////////////////////////  MediaPlayer methods  ///////////////////////////////////////////

    private void reset() {
        Log.i(LOGTAG, "reset: state: " + state);
        try {
            videoPlayer.reset();
            state = State.IDLE;
        } catch (Exception ex) {
            Log.e(LOGTAG, "reset: state: " + state + ", ex: " + ex.getLocalizedMessage(), ex);
            state = State.ERROR;
        }
    }

    private void setDataSource(String videoFilename) {
        Log.i(LOGTAG, "setDataSource: state: " + state + ", videoFilename: " + videoFilename);
        try {
            //videoPlayer.setDataSource( context, uri );

            File videoDir = new File(FileUtils.getCuraContentsDirectory(MainActivity.this), "videos");
            File videoPath = new File(videoDir, videoFilename);
            videoPlayer.setDataSource(videoPath.getAbsolutePath());

            state = State.INITIALIZED;
        } catch (IOException ex) {
            Log.e(LOGTAG, "setDataSource: state: " + state + ", ex: " + ex.getLocalizedMessage(), ex);
            state = State.ERROR;
        }
    }

    private void prepare() {
        Log.i(LOGTAG, "prepare: state: " + state);
        try {
            videoPlayer.prepare();
            state = State.PREPARED;
        } catch (IOException ex) {
            Log.e(LOGTAG, "prepare: " + ex.getLocalizedMessage(), ex);
            state = State.ERROR;
        }
    }

    private void prepareAsync() {
        Log.i(LOGTAG, "prepareAsync: state: " + state);
        videoPlayer.prepareAsync();
        state = State.PREPARING;
    }

    private void start() {
        Log.i(LOGTAG, "start: state: " + state);
        mediaPlayer.start();
        state = State.STARTED;
    }

    private void stop() {
        Log.i(LOGTAG, "stop: state: " + state);
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
        state = State.STOPED;
    }

    private void seekTo(int millis) {
        Log.i(LOGTAG, "seekTo: state: " + state);
        mediaPlayer.seekTo(millis);
        if (state != State.PLAYBACK_COMPLETE) {
            state = State.STARTED;
        }
    }

    private void pause() {
        Log.i(LOGTAG, "pause: state: " + state);
        if (mediaPlayer != null)
            mediaPlayer.pause();
        state = State.PAUSED;
    }

    private void release() {
        Log.i(LOGTAG, "release: state: " + state);
        if (videoPlayer != null)
            videoPlayer.release();
        state = State.END;
    }

    //////////////////////////////  SystemUI methods  ///////////////////////////////////////////

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
//        if ( "ODROID".equals( Build.BRAND ) ) {
        if (hasFocus) {
            hideSystemUI();
//            }
        }
    }

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
        showingBars = false;
    }

    // Shows the system bars by removing all the flags
    // except for the ones that make the content appear under the system bars.
    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        showingBars = true;
    }


    //////////////////////////////  Surface methods  ///////////////////////////////////////////

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.i(LOGTAG, "surfaceCreated: ==========================");
        surfaceCreated = true;
        videoPlayer.setDisplay(surfaceHolder);
        start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
//        stop();
    }

    //////////////////////////////  Video methods  ///////////////////////////////////////////

    private boolean findVideos(String defaultVideoName, String featureVideoName) {
        Log.i(LOGTAG, "loadMedia: defaultVideoName: " + defaultVideoName + ", featureVideoName: " + featureVideoName);
        VidPlayApp app = getApp();

        boolean found = false;
//        app.setDefaultVideoFilename( null );
        app.setFeatureVideoFilename(null);

        //!! set these only if found
        File videoDir = new File(FileUtils.getCuraContentsDirectory(MainActivity.this), "videos");
        if (defaultVideoName != null) {
            File videoPath = new File(videoDir, defaultVideoName);
            if (videoPath.exists()) {
                found = true;
                app.setDefaultVideoFilename(defaultVideoName);
            }
        }
        if (featureVideoName != null) {
            File videoPath = new File(videoDir, featureVideoName);
            if (videoPath.exists()) {
                found = true;
                app.setFeatureVideoFilename(featureVideoName);
            }
        }
        return found;
    }

    private Uri getVideoUri(long id) {
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        return ContentUris.withAppendedId(uri, id);
    }

    //////////////////////////////  Smesh message handlers  ///////////////////////////////////////////

    private void registerHandlers() {
        messageHandlers.add(new ClientMessageHandler("VidPlay_SetAttractorVideo") {
            @Override
            public void handleMessage(Message message) throws RemoteException {
                String defaultVideoName = message.getParamString("videoName");
                Log.i(LOGTAG, "VidPlay_SetAttractorVideo: defaultVideoName: " + defaultVideoName);

                boolean vidFound = findVideos(defaultVideoName, null);
                Response response = message.createResponseMessage();
                if (vidFound) {
                    denitializePlayer();
                    initializeVideoPlayer();
                    response.setResponseText("VidPlay_SetAttractorVideo successful with video name: " + defaultVideoName);
                } else {
                    response.setCodedError("SetAttractorVideo_Fail", "VidPlay_SetAttractorVideo successful with video name: " + defaultVideoName);
                }

                smeshProxy.sendResponse(response);
            }
        });
        messageHandlers.add(new ClientMessageHandler("VidPlay_PlayFeature") {
            @Override
            public void handleMessage(Message message) throws RemoteException {
                String featureVideoName = message.getParamString("videoName");

                Log.i(LOGTAG, "VidPlay_PlayFeature: featureVideoName: " + featureVideoName);
                findVideos(null, featureVideoName);

                reset();
                setDataSource(featureVideoName);
                prepare();
                videoPlayer.setLooping(false);
                start();

                Response response = message.createResponseMessage();
                response.setResponseText("PlayFeature Completed");
                smeshProxy.sendResponse(response);
            }
        });
        messageHandlers.add(new ClientMessageHandler("VidPlay_StopFeature") {
            @Override
            public void handleMessage(Message message) throws RemoteException {
                Log.i(LOGTAG, "VidPlay_StopFeature: current featureVideoName: " + getApp().getFeatureVideoFilename());
                getApp().setFeatureVideoFilename(null);
                denitializePlayer();
                initializeVideoPlayer();

                Response response = message.createResponseMessage();
                response.setResponseText("StopFeature Completed");
                smeshProxy.sendResponse(response);
            }
        });
        messageHandlers.add(new ClientMessageHandler("VidPlay_StopAttractor") {
            @Override
            public void handleMessage(Message message) throws RemoteException {
                Log.i(LOGTAG, "VidPlay_StopAttractor: current DefaultVideoFilename: " + getApp().getDefaultVideoFilename());

                Response response = message.createResponseMessage();
                response.setResponseText("VidPlay_StopAttractor Completed");
                smeshProxy.sendResponse(response);

                finish();
            }
        });
        messageHandlers.add(new ClientMessageHandler("VidPlay_Notify") {
            @Override
            public void handleMessage(Message message) throws RemoteException {
                String boolStr = message.getParamString("AppParameters");
                boolean bool = Boolean.parseBoolean(boolStr);
                showNotification(bool);

                Response response = message.createResponseMessage();
                response.setResponseText("Notify Completed");
                smeshProxy.sendResponse(response);
            }
        });
        messageHandlers.add(new ClientMessageHandler("VidPlay_CameraView") {
            @Override
            public void handleMessage(Message message) throws RemoteException {
                startActivity(new Intent("com.samsung.VidPlayApp.CameraView.ACTION"));

                Response response = message.createResponseMessage();
                response.setResponseText("VidPlay_CameraView Completed");
                smeshProxy.sendResponse(response);
            }
        });
        messageHandlers.add(new ClientMessageHandler("VidPlay_RequestPlayerStatus") {
            @Override
            public void handleMessage(Message message) throws RemoteException {
                sendStatus = message.getParamBoolean("sendStatus");
                requesterSmeshNode = message.getSmeshSource();
                Response response = message.createResponseMessage();
                response.setResponseText("VidPlay_PlayerStatus Completed");
                smeshProxy.sendResponse(response);
            }
        });


        messageHandlers.add(new ClientMessageHandler("VidPlay_StopSoundtrack") {
            @Override
            public void handleMessage(Message message) throws RemoteException {
                stopTrack();

                Response response = message.createResponseMessage();
                response.setResponseText("OnButtonPressed Completed");
                smeshProxy.sendResponse(response);
            }
        });
        messageHandlers.add(new ClientMessageHandler("VidPlay_PlaySoundtrack") {
            @Override
            public void handleMessage(Message message) throws RemoteException {
                String imageName = message.getParamString("imageName");
                String soundtrackFilename = message.getParamString("soundtrackName");
                boolean loop = message.getParamBoolean("loop");
                int startPosMillis = (int) message.getParamLong("startPosMillis");

                showImage(true, imageName, false, 0);
                playTrack(soundtrackFilename, loop, startPosMillis);

                Response response = message.createResponseMessage();
                response.setResponseText("OnButtonPressed Completed");

                smeshProxy.sendResponse(response);
            }
        });
        messageHandlers.add(new ClientMessageHandler("VidPlay_PlaySoundtrack_Next") {
            @Override
            public void handleMessage(Message message) throws RemoteException {
                // Todo add Next

                Response response = message.createResponseMessage();
                response.setResponseText("OnButtonPressed Completed");

                smeshProxy.sendResponse(response);
            }
        });
        messageHandlers.add(new ClientMessageHandler("VidPlay_PlaySoundtrack_Previous") {
            @Override
            public void handleMessage(Message message) throws RemoteException {
                // Todo add Previous

                Response response = message.createResponseMessage();
                response.setResponseText("OnButtonPressed Completed");

                smeshProxy.sendResponse(response);
            }
        });
        messageHandlers.add(new ClientMessageHandler("VidPlay_ShowImage") {
            @Override
            public void handleMessage(Message message) throws RemoteException {
                String imageName = message.getParamString("imageName");
                int durationSeconds = (int) message.getParamLong("durationSeconds");

                showImage(true, imageName, true, durationSeconds);
                enableVolume(false);

                Response response = message.createResponseMessage();
                response.setResponseText("OnButtonPressed Completed");
                smeshProxy.sendResponse(response);
            }
        });
        messageHandlers.add(new ClientMessageHandler("VidPlay_Volume") {
            @Override
            public void handleMessage(Message message) throws RemoteException {
                boolean volumeUp = message.getParamBoolean("volumeUp");
                boolean volumeDown = message.getParamBoolean("volumeDown");
                if (volumeUp) {
                    volume = Math.min(volume + 5, 100);
                } else if (volumeDown) {
                    volume = Math.max(volume - 5, 0);
                } else {
                    volume = message.getParamInteger("volume");
                }
//                    float mediaVolume = volume / 100.0f;
//                    mediaPlayer.setVolume( mediaVolume, mediaVolume );
//                    Log.i( LOGTAG, "VidPlay_Volume: volume: "+ volume +", mediaVolume: "+ mediaVolume );
                enableVolume(true);

                Response response = message.createResponseMessage();
                response.setResponseText("Setting Volume completed");
                smeshProxy.sendResponse(response);
            }
        });
        messageHandlers.add(new ClientMessageHandler("VidPlay_ControlMedia") {
            @Override
            public void handleMessage(Message message) throws RemoteException {
                String action = message.getAction();
                if ("play".equals(action)) {
                    start();
                }
                if ("pause".equals(action)) {
                    pause();
                }
                if ("seek0".equals(action)) {
                    seekTo(0);
                }

                Response response = message.createResponseMessage();
                response.setResponseText("ControlVideo Completed");
                smeshProxy.sendResponse(response);
            }
        });
        for (ClientMessageHandler handler : messageHandlers) {
            smeshProxy.registerMessageHandler(handler, VidPlayApp.APP_NAME);
            Log.e(LOGTAG, "registerHandlers: registered handler " + handler.getRequestName());
        }
    }

    private void getMediaContent() {
        PlayListManager playListManager = new PlayListManager();
        playListManager.getMediaContentFromSDCARD();
        setPagerData(FIRST_PAGE);
    }

    private void unregisterHandlers() {
        for (ClientMessageHandler handler : messageHandlers) {
            smeshProxy.unregisterMessageHandler(handler, VidPlayApp.APP_NAME);
            Log.e(LOGTAG, "unregisterHandlers: unregistered handler " + handler.getRequestName());
        }
        messageHandlers.clear();
    }

    @Override
    public void getImagePosition(int imageFilePosition) {
        adapter.notifyDataSetChanged();
        albumsPager.setCurrentItem(imageFilePosition);
    }
}
