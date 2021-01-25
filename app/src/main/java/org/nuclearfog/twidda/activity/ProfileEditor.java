package org.nuclearfog.twidda.activity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.backend.UserUpdater;
import org.nuclearfog.twidda.backend.engine.EngineException;
import org.nuclearfog.twidda.backend.holder.UserHolder;
import org.nuclearfog.twidda.backend.items.User;
import org.nuclearfog.twidda.backend.utils.AppStyles;
import org.nuclearfog.twidda.backend.utils.DialogBuilder;
import org.nuclearfog.twidda.backend.utils.DialogBuilder.OnDialogClick;
import org.nuclearfog.twidda.backend.utils.ErrorHandler;
import org.nuclearfog.twidda.database.GlobalSettings;

import java.io.File;

import jp.wasabeef.picasso.transformations.RoundedCornersTransformation;

import static android.os.AsyncTask.Status.RUNNING;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static android.view.Window.FEATURE_NO_TITLE;
import static org.nuclearfog.twidda.activity.UserProfile.RETURN_PROFILE_CHANGED;
import static org.nuclearfog.twidda.activity.UserProfile.RETURN_PROFILE_DATA;
import static org.nuclearfog.twidda.activity.UserProfile.TOOLBAR_TRANSPARENCY;
import static org.nuclearfog.twidda.backend.utils.DialogBuilder.DialogType.PROFILE_EDIT_LEAVE;
import static org.nuclearfog.twidda.database.GlobalSettings.BANNER_IMG_MID_RES;
import static org.nuclearfog.twidda.database.GlobalSettings.PROFILE_IMG_HIGH_RES;

/**
 * Activity for Twitter profile editor
 *
 * @author nuclearfog
 */
public class ProfileEditor extends MediaActivity implements OnClickListener, OnDismissListener, OnDialogClick, Callback {

    /**
     * key to preload user data
     */
    public static final String KEY_USER_DATA = "profile-editor-data";

    private UserUpdater editorAsync;
    private GlobalSettings settings;

    private ImageView profile_image, profile_banner, toolbar_background;
    private EditText name, link, loc, bio;
    private Dialog loadingCircle, closeDialog;
    private Button addBannerBtn;
    private ImageView changeBannerBtn;

    @Nullable
    private User user;
    private String profileLink, bannerLink;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.page_editprofile);
        Toolbar toolbar = findViewById(R.id.editprofile_toolbar);
        View root = findViewById(R.id.page_edit);
        ImageView changeImageBtn = findViewById(R.id.profile_change_image_btn);
        profile_image = findViewById(R.id.edit_pb);
        profile_banner = findViewById(R.id.edit_banner);
        addBannerBtn = findViewById(R.id.edit_add_banner);
        changeBannerBtn = findViewById(R.id.edit_change_banner);
        toolbar_background = findViewById(R.id.editprofile_toolbar_background);
        name = findViewById(R.id.edit_name);
        link = findViewById(R.id.edit_link);
        loc = findViewById(R.id.edit_location);
        bio = findViewById(R.id.edit_bio);
        loadingCircle = new Dialog(this, R.style.LoadingDialog);
        View load = View.inflate(this, R.layout.item_load, null);
        View cancelButton = load.findViewById(R.id.kill_button);

        toolbar.setTitle(R.string.page_profile_edior);
        setSupportActionBar(toolbar);

        settings = GlobalSettings.getInstance(this);
        toolbar.setBackgroundColor(settings.getBackgroundColor() & TOOLBAR_TRANSPARENCY);
        changeBannerBtn.setImageResource(R.drawable.add);
        changeImageBtn.setImageResource(R.drawable.add);
        profile_banner.setDrawingCacheEnabled(true);
        AppStyles.setTheme(settings, root);

        closeDialog = DialogBuilder.create(this, PROFILE_EDIT_LEAVE, this);
        loadingCircle.requestWindowFeature(FEATURE_NO_TITLE);
        loadingCircle.setCanceledOnTouchOutside(false);
        loadingCircle.setContentView(load);
        cancelButton.setVisibility(VISIBLE);

        Object data = getIntent().getSerializableExtra(KEY_USER_DATA);
        if (data instanceof User) {
            user = (User) data;
            setUser();
        }
        profile_image.setOnClickListener(this);
        profile_banner.setOnClickListener(this);
        addBannerBtn.setOnClickListener(this);
        cancelButton.setOnClickListener(this);
        loadingCircle.setOnDismissListener(this);
    }


    @Override
    protected void onDestroy() {
        if (editorAsync != null && editorAsync.getStatus() == RUNNING)
            editorAsync.cancel(true);
        super.onDestroy();
    }


    @Override
    public void onBackPressed() {
        String username = name.getText().toString();
        String userLink = link.getText().toString();
        String userLoc = loc.getText().toString();
        String userBio = bio.getText().toString();
        if (user != null && username.equals(user.getUsername()) && userLink.equals(user.getLink())
                && userLoc.equals(user.getLocation()) && userBio.equals(user.getBio())
                && profileLink == null && bannerLink == null) {
            finish();
        } else if (username.isEmpty() && userLink.isEmpty() && userLoc.isEmpty() && userBio.isEmpty()) {
            finish();
        } else if (!closeDialog.isShowing()) {
            closeDialog.show();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu m) {
        getMenuInflater().inflate(R.menu.edit, m);
        AppStyles.setMenuIconColor(m, settings.getIconColor());
        return super.onCreateOptionsMenu(m);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            if (editorAsync == null || editorAsync.getStatus() != RUNNING) {
                String username = name.getText().toString();
                String userLink = link.getText().toString();
                String userLoc = loc.getText().toString();
                String userBio = bio.getText().toString();
                if (username.trim().isEmpty()) {
                    String errMsg = getString(R.string.error_empty_name);
                    name.setError(errMsg);
                } else if (!userLink.isEmpty() && userLink.contains(" ")) {
                    String errMsg = getString(R.string.error_invalid_link);
                    link.setError(errMsg);
                } else {
                    UserHolder userHolder = new UserHolder(username, userLink, userLoc, userBio, profileLink, bannerLink);
                    editorAsync = new UserUpdater(this);
                    editorAsync.execute(userHolder);
                }
            }
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onAttachLocation(@Nullable Location location) {
    }


    @Override
    protected void onMediaFetched(int resultType, String path) {
        // Add image as profile image
        if (resultType == REQUEST_PROFILE) {
            Bitmap image = BitmapFactory.decodeFile(path);
            profile_image.setImageBitmap(image);
            profileLink = path;
        }
        // Add image as banner image
        else if (resultType == REQUEST_BANNER) {
            File img = new File(path);
            Point displaySize = new Point();
            getWindowManager().getDefaultDisplay().getSize(displaySize);
            Picasso.get().load(img).resize(displaySize.x, displaySize.x / 3).centerCrop(Gravity.TOP).into(profile_banner, this);
            addBannerBtn.setVisibility(INVISIBLE);
            changeBannerBtn.setVisibility(VISIBLE);
            bannerLink = path;
        }
    }


    @Override
    public void onClick(View v) {
        // select net profile image
        if (v.getId() == R.id.edit_pb) {
            getMedia(REQUEST_PROFILE);
        }
        // select new banner image
        else if (v.getId() == R.id.edit_add_banner || v.getId() == R.id.edit_banner) {
            getMedia(REQUEST_BANNER);
        }
        // stop update
        else if (v.getId() == R.id.kill_button) {
            loadingCircle.dismiss();
        }
    }


    @Override
    public void onDismiss(DialogInterface dialog) {
        if (editorAsync != null && editorAsync.getStatus() == RUNNING) {
            editorAsync.cancel(true);
        }
    }


    @Override
    public void onConfirm(DialogBuilder.DialogType type) {
        if (type == PROFILE_EDIT_LEAVE) {
            finish();
        }
    }


    @Override
    public void onSuccess() {
        AppStyles.setToolbarBackground(ProfileEditor.this, profile_banner, toolbar_background);
    }


    @Override
    public void onError(Exception e) {
    }

    /**
     * enable or disable loading dialog
     *
     * @param enable true to enable dialog
     */
    public void setLoading(boolean enable) {
        if (enable) {
            loadingCircle.show();
        } else {
            loadingCircle.dismiss();
        }
    }

    /**
     * called after user profile was updated successfully
     */
    public void onSuccess(User user) {
        Intent data = new Intent();
        data.putExtra(RETURN_PROFILE_DATA, user);
        Toast.makeText(this, R.string.info_profile_updated, Toast.LENGTH_SHORT).show();
        setResult(RETURN_PROFILE_CHANGED, data);
        finish();
    }

    /**
     * called after an error occurs
     *
     * @param err Engine Exception
     */
    public void onError(EngineException err) {
        ErrorHandler.handleFailure(this, err);
    }

    /**
     * Set current user's information
     */
    private void setUser() {
        if (user != null) {
            if (user.hasProfileImage()) {
                String pbLink = user.getImageLink();
                if (!user.hasDefaultProfileImage())
                    pbLink += PROFILE_IMG_HIGH_RES;
                Picasso.get().load(pbLink).transform(new RoundedCornersTransformation(5, 0)).into(profile_image);
            }
            if (user.hasBannerImage()) {
                String bnLink = user.getBannerLink() + BANNER_IMG_MID_RES;
                Picasso.get().load(bnLink).into(profile_banner, this);
                addBannerBtn.setVisibility(INVISIBLE);
                changeBannerBtn.setVisibility(VISIBLE);
            } else {
                addBannerBtn.setVisibility(VISIBLE);
                changeBannerBtn.setVisibility(INVISIBLE);
            }
            name.setText(user.getUsername());
            link.setText(user.getLink());
            loc.setText(user.getLocation());
            bio.setText(user.getBio());
        }
    }
}