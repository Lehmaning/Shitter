package org.nuclearfog.twidda.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener;
import com.google.android.material.tabs.TabLayout.Tab;
import com.squareup.picasso.Picasso;

import org.nuclearfog.tag.Tagger;
import org.nuclearfog.tag.Tagger.OnTagClickListener;
import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.adapter.FragmentAdapter;
import org.nuclearfog.twidda.adapter.FragmentAdapter.AdapterType;
import org.nuclearfog.twidda.backend.ProfileLoader;
import org.nuclearfog.twidda.backend.helper.FontTool;
import org.nuclearfog.twidda.backend.items.TwitterUser;
import org.nuclearfog.twidda.backend.items.UserProperties;
import org.nuclearfog.twidda.database.GlobalSettings;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;

import static android.content.Intent.ACTION_VIEW;
import static android.os.AsyncTask.Status.RUNNING;
import static android.view.Gravity.CENTER;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_UP;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.widget.LinearLayout.LayoutParams.WRAP_CONTENT;
import static android.widget.Toast.LENGTH_SHORT;
import static org.nuclearfog.twidda.activity.MediaViewer.KEY_MEDIA_LINK;
import static org.nuclearfog.twidda.activity.MediaViewer.KEY_MEDIA_TYPE;
import static org.nuclearfog.twidda.activity.MediaViewer.MEDIAVIEWER_IMAGE;
import static org.nuclearfog.twidda.activity.MessagePopup.KEY_DM_PREFIX;
import static org.nuclearfog.twidda.activity.SearchPage.KEY_SEARCH_QUERY;
import static org.nuclearfog.twidda.activity.TweetPopup.KEY_TWEETPOPUP_PREFIX;
import static org.nuclearfog.twidda.activity.UserDetail.KEY_USERDETAIL_ID;
import static org.nuclearfog.twidda.activity.UserDetail.KEY_USERDETAIL_MODE;
import static org.nuclearfog.twidda.activity.UserDetail.USERLIST_FOLLOWER;
import static org.nuclearfog.twidda.activity.UserDetail.USERLIST_FRIENDS;
import static org.nuclearfog.twidda.activity.UserList.KEY_USERLIST_ID;
import static org.nuclearfog.twidda.backend.ProfileLoader.Action.LDR_PROFILE;


public class UserProfile extends AppCompatActivity implements OnClickListener,
        OnTouchListener, OnTagClickListener, OnTabSelectedListener {

    public static final String KEY_PROFILE_ID = "profile_uid";
    public static final int RETURN_PROFILE_CHANGED = 2;
    private static final int REQUEST_PROFILE_CHANGED = 1;
    private static final int TRANSPARENCY = 0xafffffff;

    private ProfileLoader profileAsync;
    private FragmentAdapter adapter;
    private GlobalSettings settings;

    private TextView tweetTabTxt, favorTabTxt, txtUser, txtScrName;
    private TextView txtLocation, txtCreated, lnkTxt, bioTxt, follow_back;
    private Button following, follower;
    private ImageView profile, banner;
    private View profile_head, profile_layer;
    private ViewPager pager;

    @Nullable
    private UserProperties properties;
    @Nullable
    private TwitterUser user;
    private long userId;
    private boolean isHome;

    private int tabIndex = 0;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.page_profile);

        settings = GlobalSettings.getInstance(this);
        Bundle param = getIntent().getExtras();
        if (param != null && param.containsKey(KEY_PROFILE_ID)) {
            userId = param.getLong(KEY_PROFILE_ID);
            isHome = userId == settings.getUserId();
        }
        Toolbar tool = findViewById(R.id.profile_toolbar);
        TabLayout tab = findViewById(R.id.profile_tab);
        ViewGroup root = findViewById(R.id.user_view);
        bioTxt = findViewById(R.id.bio);
        following = findViewById(R.id.following);
        follower = findViewById(R.id.follower);
        lnkTxt = findViewById(R.id.links);
        profile = findViewById(R.id.profile_img);
        banner = findViewById(R.id.profile_banner);
        txtUser = findViewById(R.id.profile_username);
        txtScrName = findViewById(R.id.profile_screenname);
        txtLocation = findViewById(R.id.location);
        profile_head = findViewById(R.id.profile_header);
        profile_layer = findViewById(R.id.profile_layer);
        txtCreated = findViewById(R.id.profile_date);
        follow_back = findViewById(R.id.follow_back);
        pager = findViewById(R.id.profile_pager);
        tweetTabTxt = new TextView(this);
        favorTabTxt = new TextView(this);

        setSupportActionBar(tool);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowTitleEnabled(false);

        FontTool.setViewFontAndColor(settings, root);
        txtUser.setBackgroundColor(settings.getBackgroundColor() & TRANSPARENCY);
        txtScrName.setBackgroundColor(settings.getBackgroundColor() & TRANSPARENCY);
        follow_back.setBackgroundColor(settings.getBackgroundColor() & TRANSPARENCY);
        bioTxt.setMovementMethod(LinkMovementMethod.getInstance());
        tab.setSelectedTabIndicatorColor(settings.getHighlightColor());
        bioTxt.setLinkTextColor(settings.getHighlightColor());
        lnkTxt.setTextColor(settings.getHighlightColor());
        root.setBackgroundColor(settings.getBackgroundColor());
        tweetTabTxt.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.home_profile, 0, 0);
        favorTabTxt.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.favorite_profile, 0, 0);
        tweetTabTxt.setGravity(CENTER);
        favorTabTxt.setGravity(CENTER);
        tweetTabTxt.setSingleLine();
        favorTabTxt.setSingleLine();
        tweetTabTxt.setTextSize(10);
        favorTabTxt.setTextSize(10);
        tweetTabTxt.setTypeface(settings.getFontFace());
        favorTabTxt.setTypeface(settings.getFontFace());
        tweetTabTxt.setTextColor(settings.getFontColor());
        favorTabTxt.setTextColor(settings.getFontColor());

        adapter = new FragmentAdapter(getSupportFragmentManager(), AdapterType.PROFILE_TAB, userId, "");
        pager.setOffscreenPageLimit(2);
        pager.setAdapter(adapter);
        tab.setupWithViewPager(pager);
        Tab tweetTab = tab.getTabAt(0);
        Tab favorTab = tab.getTabAt(1);
        if (tweetTab != null && favorTab != null) {
            tweetTab.setCustomView(tweetTabTxt);
            favorTab.setCustomView(favorTabTxt);
        }
        tab.addOnTabSelectedListener(this);
        following.setOnClickListener(this);
        follower.setOnClickListener(this);
        profile.setOnClickListener(this);
        banner.setOnClickListener(this);
        lnkTxt.setOnClickListener(this);
        bioTxt.setOnTouchListener(this);
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (profileAsync == null) {
            profileAsync = new ProfileLoader(this, LDR_PROFILE);
            profileAsync.execute(userId);
        }
    }


    @Override
    protected void onDestroy() {
        if (profileAsync != null && profileAsync.getStatus() == RUNNING)
            profileAsync.cancel(true);
        super.onDestroy();
    }


    @Override
    public void onActivityResult(int reqCode, int returnCode, Intent i) {
        if (reqCode == REQUEST_PROFILE_CHANGED && returnCode == RETURN_PROFILE_CHANGED) {
            profileAsync = null;
            adapter.clearData();
        }
        super.onActivityResult(reqCode, returnCode, i);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu m) {
        getMenuInflater().inflate(R.menu.profile, m);
        if (isHome) {
            MenuItem dmIcon = m.findItem(R.id.profile_message);
            MenuItem setting = m.findItem(R.id.profile_settings);
            dmIcon.setVisible(true);
            setting.setVisible(true);
        } else {
            MenuItem followIcon = m.findItem(R.id.profile_follow);
            MenuItem blockIcon = m.findItem(R.id.profile_block);
            MenuItem muteIcon = m.findItem(R.id.profile_mute);
            followIcon.setVisible(true);
            blockIcon.setVisible(true);
            muteIcon.setVisible(true);
        }
        return super.onCreateOptionsMenu(m);
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu m) {
        if (user != null) {
            if (user.followRequested()) {
                MenuItem followIcon = m.findItem(R.id.profile_follow);
                followIcon.setIcon(R.drawable.follow_requested);
                followIcon.setTitle(R.string.follow_requested);
            }
            if (user.isLocked() && !isHome) {
                MenuItem listItem = m.findItem(R.id.profile_lists);
                listItem.setVisible(false);
            }
        }
        if (properties != null) {
            if (properties.isFriend()) {
                MenuItem followIcon = m.findItem(R.id.profile_follow);
                MenuItem listItem = m.findItem(R.id.profile_lists);
                followIcon.setIcon(R.drawable.follow_enabled);
                followIcon.setTitle(R.string.user_unfollow);
                listItem.setVisible(true);
            }
            if (properties.isBlocked()) {
                MenuItem blockIcon = m.findItem(R.id.profile_block);
                blockIcon.setTitle(R.string.user_unblock);
            }
            if (properties.isMuted()) {
                MenuItem muteIcon = m.findItem(R.id.profile_mute);
                muteIcon.setTitle(R.string.user_unmute);
            }
            if (properties.canDm()) {
                MenuItem dmIcon = m.findItem(R.id.profile_message);
                dmIcon.setVisible(true);
            }
            if (properties.isFollower()) {
                follow_back.setVisibility(VISIBLE);
            }
        }

        return super.onPrepareOptionsMenu(m);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (profileAsync != null && profileAsync.getStatus() != RUNNING) {
            switch (item.getItemId()) {
                case R.id.profile_tweet:
                    if (user != null) {
                        Intent tweet = new Intent(this, TweetPopup.class);
                        if (!isHome)
                            tweet.putExtra(KEY_TWEETPOPUP_PREFIX, user.getScreenname());
                        startActivity(tweet);
                    }
                    break;

                case R.id.profile_settings:
                    Intent editProfile = new Intent(this, ProfileEditor.class);
                    startActivityForResult(editProfile, REQUEST_PROFILE_CHANGED);
                    break;

                case R.id.profile_follow:
                    if (properties != null) {
                        profileAsync = new ProfileLoader(this, ProfileLoader.Action.ACTION_FOLLOW);
                        if (!properties.isFriend()) {
                            profileAsync.execute(userId);
                        } else {
                            new Builder(this).setMessage(R.string.confirm_unfollow)
                                    .setNegativeButton(R.string.confirm_no, null)
                                    .setPositiveButton(R.string.confirm_yes, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            profileAsync.execute(userId);
                                        }
                                    }).show();
                        }
                    }
                    break;

                case R.id.profile_mute:
                    if (properties != null) {
                        profileAsync = new ProfileLoader(this, ProfileLoader.Action.ACTION_MUTE);
                        if (properties.isMuted()) {
                            profileAsync.execute(userId);
                        } else {
                            new Builder(this).setMessage(R.string.confirm_mute)
                                    .setNegativeButton(R.string.confirm_no, null)
                                    .setPositiveButton(R.string.confirm_yes, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            profileAsync.execute(userId);
                                        }
                                    }).show();
                        }
                    }
                    break;

                case R.id.profile_block:
                    if (properties != null) {
                        profileAsync = new ProfileLoader(this, ProfileLoader.Action.ACTION_BLOCK);
                        if (properties.isBlocked()) {
                            profileAsync.execute(userId);
                        } else {
                            new Builder(this).setMessage(R.string.confirm_block)
                                    .setNegativeButton(R.string.confirm_no, null)
                                    .setPositiveButton(R.string.confirm_yes, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            profileAsync.execute(userId);
                                        }
                                    }).show();
                        }
                    }
                    break;

                case R.id.profile_message:
                    if (properties != null) {
                        Intent dmPage;
                        if (properties.isHome()) {
                            dmPage = new Intent(this, DirectMessage.class);
                        } else {
                            dmPage = new Intent(this, MessagePopup.class);
                            dmPage.putExtra(KEY_DM_PREFIX, properties.getTargetScreenname());
                        }
                        startActivity(dmPage);
                    }
                    break;

                case R.id.profile_lists:
                    Intent listPage = new Intent(this, UserList.class);
                    listPage.putExtra(KEY_USERLIST_ID, userId);
                    startActivity(listPage);
                    break;
            }
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onBackPressed() {
        if (tabIndex == 0) {
            super.onBackPressed();
        } else {
            pager.setCurrentItem(0);
        }
    }


    @Override
    public void onTagClick(String text) {
        Intent intent = new Intent(this, SearchPage.class);
        intent.putExtra(KEY_SEARCH_QUERY, text);
        startActivity(intent);
    }


    @Override
    public void onLinkClick(String link) {
        if (TweetDetail.linkPattern.matcher(link).matches()) {
            Intent intent = new Intent(this, TweetDetail.class);
            intent.setData(Uri.parse(link));
            startActivity(intent);
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
            if (intent.resolveActivity(getPackageManager()) != null)
                startActivity(intent);
            else
                Toast.makeText(this, R.string.error_connection, LENGTH_SHORT).show();
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.following:
                if (user != null && properties != null) {
                    if (!user.isLocked() || properties.isFriend()) {
                        Intent following = new Intent(this, UserDetail.class);
                        following.putExtra(KEY_USERDETAIL_ID, userId);
                        following.putExtra(KEY_USERDETAIL_MODE, USERLIST_FRIENDS);
                        startActivity(following);
                    }
                }
                break;

            case R.id.follower:
                if (user != null && properties != null) {
                    if (!user.isLocked() || properties.isFriend()) {
                        Intent follower = new Intent(this, UserDetail.class);
                        follower.putExtra(KEY_USERDETAIL_ID, userId);
                        follower.putExtra(KEY_USERDETAIL_MODE, USERLIST_FOLLOWER);
                        startActivity(follower);
                    }
                }
                break;

            case R.id.links:
                if (user != null && !user.getLink().isEmpty()) {
                    String link = user.getLink();
                    Intent browserIntent = new Intent(ACTION_VIEW, Uri.parse(link));
                    if (browserIntent.resolveActivity(getPackageManager()) != null)
                        startActivity(browserIntent);
                    else
                        Toast.makeText(this, R.string.error_connection, LENGTH_SHORT).show();
                }
                break;

            case R.id.profile_img:
                if (user != null) {
                    Intent image = new Intent(this, MediaViewer.class);
                    image.putExtra(KEY_MEDIA_LINK, new String[]{user.getImageLink()});
                    image.putExtra(KEY_MEDIA_TYPE, MEDIAVIEWER_IMAGE);
                    startActivity(image);
                }
                break;

            case R.id.profile_banner:
                if (user != null) {
                    Intent image = new Intent(this, MediaViewer.class);
                    image.putExtra(KEY_MEDIA_LINK, new String[]{user.getBannerLink() + "/1500x500"});
                    image.putExtra(KEY_MEDIA_TYPE, MEDIAVIEWER_IMAGE);
                    startActivity(image);
                }
                break;
        }
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case ACTION_DOWN:
                v.getParent().requestDisallowInterceptTouchEvent(true);
                break;

            case ACTION_UP:
                v.getParent().requestDisallowInterceptTouchEvent(false);
                break;
        }
        return v.performClick();
    }


    @Override
    public void onTabSelected(Tab tab) {
        tabIndex = tab.getPosition();
    }


    @Override
    public void onTabUnselected(Tab tab) {
        adapter.scrollToTop(tab.getPosition());
    }


    @Override
    public void onTabReselected(Tab tab) {
    }


    /**
     * Set User Information
     *
     * @param user User data
     */
    public void setUser(final TwitterUser user) {
        this.user = user;
        NumberFormat formatter = NumberFormat.getIntegerInstance();
        Spanned bio = Tagger.makeTextWithLinks(user.getBio(), settings.getHighlightColor(), this);
        int verify = user.isVerified() ? R.drawable.verify : 0;
        int locked = user.isLocked() ? R.drawable.lock : 0;

        txtUser.setCompoundDrawablesWithIntrinsicBounds(verify, 0, 0, 0);
        txtScrName.setCompoundDrawablesWithIntrinsicBounds(locked, 0, 0, 0);
        tweetTabTxt.setText(formatter.format(user.getTweetCount()));
        favorTabTxt.setText(formatter.format(user.getFavorCount()));
        following.setText(formatter.format(user.getFollowing()));
        follower.setText(formatter.format(user.getFollower()));
        txtUser.setText(user.getUsername());
        txtScrName.setText(user.getScreenname());
        bioTxt.setText(bio);

        if (profile_head.getVisibility() != VISIBLE) {
            profile_head.setVisibility(VISIBLE);
            String date = SimpleDateFormat.getDateTimeInstance().format(user.getCreatedAt());
            txtCreated.setText(date);
        }
        if (!user.getLocation().isEmpty()) {
            txtLocation.setText(user.getLocation());
            txtLocation.setVisibility(VISIBLE);
        } else {
            txtLocation.setVisibility(GONE);
        }
        if (!user.getLink().isEmpty()) {
            String link = user.getLink();
            if (link.startsWith("http://"))
                lnkTxt.setText(link.substring(7));
            else if (link.startsWith("https://"))
                lnkTxt.setText(link.substring(8));
            else
                lnkTxt.setText(link);
            lnkTxt.setVisibility(VISIBLE);
        } else {
            lnkTxt.setVisibility(GONE);
        }
        if (settings.getImageLoad()) {
            if (user.hasBannerImg()) {
                String bannerLink = user.getBannerLink() + "/600x200";
                Picasso.get().load(bannerLink).into(banner);
                profile_layer.getLayoutParams().height = (int) getResources().getDimension(R.dimen.profile_banner_height);
                profile_layer.requestLayout();
            } else {
                profile_layer.getLayoutParams().height = WRAP_CONTENT;
                profile_layer.requestLayout();
            }
            if (user.hasProfileImage()) {
                String imgLink = user.getImageLink() + "_bigger";
                Picasso.get().load(imgLink).into(profile);
            }
        }
    }


    /**
     * Set User Relationship
     *
     * @param properties relationship to the current user
     */
    public void setConnection(UserProperties properties) {
        this.properties = properties;
        invalidateOptionsMenu();
    }
}