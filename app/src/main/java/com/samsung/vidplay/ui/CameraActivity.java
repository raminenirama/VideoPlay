package com.samsung.vidplay.ui;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.samsung.mars.base.Message;
import com.samsung.mars.base.Request;
import com.samsung.mars.util.Log;
import com.samsung.mars.util.Runner;
import com.samsung.smesh.client.ClientDefer;
import com.samsung.vidplay.R;


public class CameraActivity extends BaseActivity 
{
    private static final String LOGTAG = "VidPlay.CameraAct";

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_camera );

        String smeshDest = "js0";

        final WebView webview = (WebView) findViewById( R.id.webview );
        WebSettings webSettings = webview.getSettings();
        webSettings.setJavaScriptEnabled( true );
        webview.setWebViewClient( new WebClient() );

//        String url = "http://192.168.1.63/html";
//        Log.i( LOGTAG, "onCreateView: loading url: "+ url );
//        webview.loadUrl( url );
//        Log.i( LOGTAG, "onCreateView: done loading url: "+ url );

        Request request = new Request( "CameraControl" );
        ClientDefer clientDefer = new ClientDefer().done( new Runner<Message>() {
            @Override
            public void run( Message response ) {
                Log.i( LOGTAG, "defer.done: response ----------------- Json: "+ response.toJsonBundle() );
                final String url = response.getParamString( "MARS_Result" ) +"/cam_pic_new.php";
                CameraActivity.this.runOnUiThread( new Runnable() {
                    @Override
                    public void run() {
                        Log.i( LOGTAG, "CameraControl done: loading url: "+ url );
                        webview.loadUrl( url );
                        Log.i( LOGTAG, "CameraControl done: loaded url: "+ url );
                    }
                } );
            }
        } ).fail( new Runner<com.samsung.mars.base.Message>() {
            @Override
            public void run( com.samsung.mars.base.Message response ) {
                Log.w( LOGTAG, "defer.fail: response ----------------- Json: "+ response.toJsonBundle() );
            }
        } );
        getSmeshProxy().sendRequest( request, smeshDest, clientDefer );
    }
}


class WebClient extends WebViewClient
{
    private static final String LOGTAG = Log.setLogLevel( "WebClient", Log.DEBUG );
    /**
     * Notify the host application that a page has started loading. This method
     * is called once for each main frame load so a page with iframes or
     * framesets will call onPageStarted one time for the main frame. This also
     * means that onPageStarted will not be called when the contents of an
     * embedded frame changes, i.e. clicking a link whose target is an iframe,
     * it will also not be called for fragment navigations (navigations to
     * #fragment_id).
     *
     * @param view    The WebView that is initiating the callback.
     * @param url     The url to be loaded.
     * @param favicon The favicon for this page if it already exists in the
     */
    @Override
    public void onPageStarted( WebView view, String url, Bitmap favicon ) {
        Log.i( LOGTAG, "onPageStarted" );
        super.onPageStarted( view, url, favicon );
    }

    /**
     * Notify the host application that a page has finished loading. This method
     * is called only for main frame. When onPageFinished() is called, the
     * rendering picture may not be updated yet. To get the notification for the
     * new Picture, use {@link WebView.PictureListener#onNewPicture}.
     *
     * @param view The WebView that is initiating the callback.
     * @param url  The url of the page.
     */
    @Override
    public void onPageFinished( WebView view, String url ) {
        Log.i( LOGTAG, "onPageFinished" );
        super.onPageFinished( view, url );
    }

    /**
     * Notify the host application that the WebView will load the resource
     * specified by the given url.
     *
     * @param view The WebView that is initiating the callback.
     * @param url  The url of the resource the WebView will load.
     */
    @Override
    public void onLoadResource( WebView view, String url ) {
        Log.i( LOGTAG, "onLoadResource" );
        super.onLoadResource( view, url );
    }

    /**
     * Report web resource loading error to the host application. These errors usually indicate
     * inability to connect to the server. Note that unlike the deprecated version of the callback,
     * the new version will be called for any resource (iframe, image, etc), not just for the main
     * page. Thus, it is recommended to perform minimum required work in this callback.
     *
     * @param view    The WebView that is initiating the callback.
     * @param request The originating request.
     * @param error   Information about the error occured.
     */
    @Override
    public void onReceivedError( WebView view, WebResourceRequest request, WebResourceError error ) {
        Log.i( LOGTAG, "onReceivedError" );
        super.onReceivedError( view, request, error );
    }

    /**
     * Notify the host application that an HTTP error has been received from the server while
     * loading a resource.  HTTP errors have status codes &gt;= 400.  This callback will be called
     * for any resource (iframe, image, etc), not just for the main page. Thus, it is recommended to
     * perform minimum required work in this callback. Note that the content of the server
     * response may not be provided within the <b>errorResponse</b> parameter.
     *
     * @param view          The WebView that is initiating the callback.
     * @param request       The originating request.
     * @param errorResponse Information about the error occured.
     */
    @Override
    public void onReceivedHttpError( WebView view, WebResourceRequest request, WebResourceResponse errorResponse ) {
        Log.i( LOGTAG, "onReceivedHttpError: request: "+ request.getUrl() );
        Log.i( LOGTAG, "onReceivedHttpError: errorResponse: "+ errorResponse.getStatusCode() +" : "+ errorResponse.getReasonPhrase() );
        super.onReceivedHttpError( view, request, errorResponse );
    }

    /**
     * As the host application if the browser should resend data as the
     * requested page was a result of a POST. The default is to not resend the
     * data.
     *
     * @param view       The WebView that is initiating the callback.
     * @param dontResend The message to send if the browser should not resend
     * @param resend     The message to send if the browser should resend data
     */
    @Override
    public void onFormResubmission( WebView view, android.os.Message dontResend, android.os.Message resend ) {
        Log.i( LOGTAG, "onFormResubmission" );
        super.onFormResubmission( view, dontResend, resend );
    }

    /**
     * Notify the host application that the scale applied to the WebView has
     * changed.
     *
     * @param view     The WebView that is initiating the callback.
     * @param oldScale The old scale factor
     * @param newScale The new scale factor
     */
    @Override
    public void onScaleChanged( WebView view, float oldScale, float newScale ) {
        Log.i( LOGTAG, "onScaleChanged" );
        super.onScaleChanged( view, oldScale, newScale );
    }

    /**
     * Notify host application that the given webview's render process has exited.
     * <p>
     * Multiple WebView instances may be associated with a single render process;
     * onRenderProcessGone will be called for each WebView that was affected.
     * The application's implementation of this callback should only attempt to
     * clean up the specific WebView given as a parameter, and should not assume
     * that other WebView instances are affected.
     * <p>
     * The given WebView can't be used, and should be removed from the view hierarchy,
     * all references to it should be cleaned up, e.g any references in the Activity
     * or other classes saved using findViewById and similar calls, etc
     * <p>
     * To cause an render process crash for test purpose, the application can
     * call loadUrl("chrome://crash") on the WebView. Note that multiple WebView
     * instances may be affected if they share a render process, not just the
     * specific WebView which loaded chrome://crash.
     *
     * @param view   The WebView which needs to be cleaned up.
     * @param detail the reason why it exited.
     * @return true if the host application handled the situation that process has
     * exited, otherwise, application will crash if render process crashed,
     * or be killed if render process was killed by the system.
     */
    @Override
    public boolean onRenderProcessGone( WebView view, RenderProcessGoneDetail detail ) {
        Log.i( LOGTAG, "onRenderProcessGone" );
        return super.onRenderProcessGone( view, detail );
    }
}
