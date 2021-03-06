package org.nuclearfog.twidda.activity;

import android.app.Dialog;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.backend.MessageUpdater;
import org.nuclearfog.twidda.backend.engine.EngineException;
import org.nuclearfog.twidda.backend.holder.MessageHolder;
import org.nuclearfog.twidda.backend.utils.AppStyles;
import org.nuclearfog.twidda.backend.utils.DialogBuilder;
import org.nuclearfog.twidda.backend.utils.DialogBuilder.OnDialogConfirmListener;
import org.nuclearfog.twidda.backend.utils.DialogBuilder.OnProgressStopListener;
import org.nuclearfog.twidda.backend.utils.ErrorHandler;
import org.nuclearfog.twidda.database.GlobalSettings;

import static android.os.AsyncTask.Status.RUNNING;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.widget.Toast.LENGTH_SHORT;
import static org.nuclearfog.twidda.activity.MediaViewer.KEY_MEDIA_LINK;
import static org.nuclearfog.twidda.activity.MediaViewer.KEY_MEDIA_TYPE;
import static org.nuclearfog.twidda.activity.MediaViewer.MEDIAVIEWER_IMG_S;
import static org.nuclearfog.twidda.backend.utils.DialogBuilder.DialogType.MESSAGE_EDITOR_ERROR;
import static org.nuclearfog.twidda.backend.utils.DialogBuilder.DialogType.MESSAGE_EDITOR_LEAVE;

/**
 * Direct message popup activity
 *
 * @author nuclearfog
 */
public class MessageEditor extends MediaActivity implements OnClickListener, OnDialogConfirmListener, OnProgressStopListener {

    /**
     * key for the screen name if any
     */
    public static final String KEY_DM_PREFIX = "dm_prefix";

    private MessageUpdater messageAsync;

    private EditText receiver, message;
    private ImageButton media, preview;
    private Dialog loadingCircle, leaveDialog;
    private AlertDialog errorDialog;
    @Nullable
    private String mediaPath;


    @Override
    protected void onCreate(@Nullable Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.popup_dm);
        View root = findViewById(R.id.dm_popup);
        ImageButton send = findViewById(R.id.dm_send);
        ImageView background = findViewById(R.id.dm_background);
        media = findViewById(R.id.dm_media);
        preview = findViewById(R.id.dm_preview);
        receiver = findViewById(R.id.dm_receiver);
        message = findViewById(R.id.dm_text);
        loadingCircle = DialogBuilder.createProgress(this, this);
        leaveDialog = DialogBuilder.create(this, MESSAGE_EDITOR_LEAVE, this);
        errorDialog = DialogBuilder.create(this, MESSAGE_EDITOR_ERROR, this);

        String prefix = getIntent().getStringExtra(KEY_DM_PREFIX);
        if (prefix != null) {
            receiver.append(prefix);
        }
        send.setImageResource(R.drawable.right);
        media.setImageResource(R.drawable.image_add);
        preview.setImageResource(R.drawable.image);
        preview.setVisibility(GONE);
        GlobalSettings settings = GlobalSettings.getInstance(this);
        AppStyles.setEditorTheme(settings, root, background);

        send.setOnClickListener(this);
        media.setOnClickListener(this);
        preview.setOnClickListener(this);
    }


    @Override
    public void onBackPressed() {
        if (receiver.getText().length() == 0 && message.getText().length() == 0 && mediaPath == null) {
            super.onBackPressed();
        } else if (!leaveDialog.isShowing()) {
            leaveDialog.show();
        }
    }


    @Override
    protected void onDestroy() {
        if (messageAsync != null && messageAsync.getStatus() == RUNNING)
            messageAsync.cancel(true);
        super.onDestroy();
    }


    @Override
    protected void onAttachLocation(@Nullable Location location) {
    }


    @Override
    protected void onMediaFetched(int resultType, String path) {
        if (resultType == REQUEST_IMAGE) {
            preview.setVisibility(VISIBLE);
            media.setVisibility(GONE);
            mediaPath = path;
        }
    }


    @Override
    public void onClick(View v) {
        // send direct message
        if (v.getId() == R.id.dm_send) {
            if (messageAsync == null || messageAsync.getStatus() != RUNNING) {
                String username = receiver.getText().toString();
                String message = this.message.getText().toString();
                if (!username.trim().isEmpty() && (!message.trim().isEmpty() || mediaPath != null)) {
                    MessageHolder messageHolder = new MessageHolder(username, message, mediaPath);
                    messageAsync = new MessageUpdater(this, messageHolder);
                    messageAsync.execute();
                    if (!loadingCircle.isShowing()) {
                        loadingCircle.show();
                    }
                } else {
                    Toast.makeText(this, R.string.error_dm, LENGTH_SHORT).show();
                }
            }
        }
        // get media
        else if (v.getId() == R.id.dm_media) {
            getMedia(REQUEST_IMAGE);
        }
        // open media
        else if (v.getId() == R.id.dm_preview) {
            Intent image = new Intent(this, MediaViewer.class);
            image.putExtra(KEY_MEDIA_LINK, new String[]{mediaPath});
            image.putExtra(KEY_MEDIA_TYPE, MEDIAVIEWER_IMG_S);
            startActivity(image);
        }
    }


    @Override
    public void stopProgress() {
        if (messageAsync != null && messageAsync.getStatus() == RUNNING) {
            messageAsync.cancel(true);
        }
    }


    @Override
    public void onConfirm(DialogBuilder.DialogType type) {
        if (type == MESSAGE_EDITOR_LEAVE || type == MESSAGE_EDITOR_ERROR) {
            finish();
        }
    }

    /**
     * called when direct message is sent
     */
    public void onSuccess() {
        Toast.makeText(this, R.string.info_dm_send, Toast.LENGTH_SHORT).show();
        finish();
    }

    /**
     * called when an error occurs
     *
     * @param error Engine Exception
     */
    public void onError(EngineException error) {
        if (!errorDialog.isShowing()) {
            String message = ErrorHandler.getErrorMessage(this, error);
            errorDialog.setMessage(message);
            errorDialog.show();
        }
        if (loadingCircle.isShowing()) {
            loadingCircle.dismiss();
        }
    }
}