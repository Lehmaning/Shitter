package org.nuclearfog.twidda.activity;

import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.AsyncTask.Status;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.adapter.ImageAdapter;
import org.nuclearfog.twidda.adapter.ImageAdapter.OnImageClickListener;
import org.nuclearfog.twidda.backend.ImageLoader;
import org.nuclearfog.twidda.backend.engine.EngineException;
import org.nuclearfog.twidda.backend.helper.ErrorHandler;
import org.nuclearfog.zoomview.ZoomView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.media.MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL;


public class MediaViewer extends AppCompatActivity implements OnImageClickListener, OnPreparedListener {

    public static final String KEY_MEDIA_LINK = "media_link";
    public static final String KEY_MEDIA_TYPE = "media_type";

    /// Media Types
    private static final int MEDIAVIEWER_NONE = 0;
    public static final int MEDIAVIEWER_IMAGE = 1;
    public static final int MEDIAVIEWER_VIDEO = 2;
    public static final int MEDIAVIEWER_ANGIF = 3;

    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.GERMANY);
    private static final String[] REQ_WRITE_SD = {WRITE_EXTERNAL_STORAGE};
    private static final int REQCODE_SD = 6;

    private ImageLoader imageAsync;
    private ProgressBar video_progress;
    private ProgressBar image_progress;
    private MediaController videoController;
    private View imageWindow, videoWindow;
    private RecyclerView imageList;
    private ImageAdapter adapter;
    private VideoView videoView;
    private ZoomView zoomImage;
    private int type;

    private int videoPos = 0;


    @Override
    protected void onCreate(@Nullable Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.page_media);
        imageList = findViewById(R.id.image_list);
        imageWindow = findViewById(R.id.image_window);
        videoWindow = findViewById(R.id.video_window);
        image_progress = findViewById(R.id.image_load);
        video_progress = findViewById(R.id.video_load);
        zoomImage = findViewById(R.id.image_full);
        videoView = findViewById(R.id.video_view);
        videoController = new MediaController(this);
        adapter = new ImageAdapter(this);
        videoView.setZOrderOnTop(true);
        videoView.setOnPreparedListener(this);
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (imageWindow.getVisibility() != VISIBLE && videoWindow.getVisibility() != VISIBLE) {
            Bundle param = getIntent().getExtras();
            if (param != null && type == MEDIAVIEWER_NONE) {
                String[] link = param.getStringArray(KEY_MEDIA_LINK);
                type = param.getInt(KEY_MEDIA_TYPE, MEDIAVIEWER_NONE);

                if (link != null && link.length > 0) {
                    switch (type) {
                        case MEDIAVIEWER_IMAGE:
                            imageWindow.setVisibility(VISIBLE);
                            imageList.setLayoutManager(new LinearLayoutManager(this, HORIZONTAL, false));
                            imageList.setAdapter(adapter);
                            if (imageAsync == null) {
                                imageAsync = new ImageLoader(this);
                                imageAsync.execute(link);
                            }
                            break;

                        case MEDIAVIEWER_VIDEO:
                            videoView.setMediaController(videoController);
                        case MEDIAVIEWER_ANGIF:
                            videoWindow.setVisibility(VISIBLE);
                            Uri video = Uri.parse(link[0]);
                            videoView.setVideoURI(video);
                            videoView.start();
                            break;
                    }
                }
            }
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (type == MEDIAVIEWER_VIDEO) {
            videoPos = videoView.getCurrentPosition();
            videoView.pause();
        }
    }


    @Override
    protected void onDestroy() {
        if (imageAsync != null && imageAsync.getStatus() == Status.RUNNING)
            imageAsync.cancel(true);
        super.onDestroy();
    }


    @Override
    public void onImageClick(Bitmap image) {
        changeImage(image);
    }


    @Override
    public void onImageTouch(Bitmap image) {
        boolean accessGranted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int check = checkSelfPermission(WRITE_EXTERNAL_STORAGE);
            if (check == PERMISSION_DENIED) {
                requestPermissions(REQ_WRITE_SD, REQCODE_SD);
                accessGranted = false;
            }
        }
        if (accessGranted) {
            storeImage(image);
        }
    }


    @Override
    public void onPrepared(MediaPlayer mp) {
        if (type == MEDIAVIEWER_ANGIF) {
            mp.setLooping(true);
        } else {
            videoController.show(0);
            mp.seekTo(videoPos);
        }
        mp.start();

        mp.setOnInfoListener(new OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                if (what == MEDIA_INFO_VIDEO_RENDERING_START) {
                    video_progress.setVisibility(INVISIBLE);
                    return true;
                }
                return false;
            }
        });
    }


    public void onSuccess() {
        adapter.disableLoading();
    }


    public void onError(@Nullable EngineException err) {
        if (err != null) {
            ErrorHandler.handleFailure(getApplicationContext(), err);
        } else {
            Toast.makeText(getApplicationContext(), R.string.error_image_download, Toast.LENGTH_SHORT).show();
        }
        finish();
    }


    public void setImage(Bitmap image) {
        if (adapter.isEmpty()) {
            changeImage(image);
            image_progress.setVisibility(View.INVISIBLE);
        }
        adapter.addLast(image);
    }


    private void changeImage(Bitmap image) {
        int width = zoomImage.getMeasuredWidth();
        if (width > 0 && image.getWidth() > width) {
            float ratio = image.getWidth() / (float) width;
            int destHeight = (int) (image.getHeight() / ratio);
            image = Bitmap.createScaledBitmap(image, width, destHeight, false);
        }
        zoomImage.reset();
        zoomImage.setImageBitmap(image);
    }


    private void storeImage(Bitmap image) {
        String name = "shitter_" + formatter.format(new Date());
        try {
            MediaStore.Images.Media.insertImage(getContentResolver(), image, name, "");
            Toast.makeText(this, R.string.info_image_saved, Toast.LENGTH_LONG).show();
        } catch (Exception err) {
            Toast.makeText(this, R.string.error_image_save, Toast.LENGTH_SHORT).show();
        }
    }
}