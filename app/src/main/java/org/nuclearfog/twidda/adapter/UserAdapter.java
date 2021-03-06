package org.nuclearfog.twidda.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.squareup.picasso.Picasso;

import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.adapter.holder.Footer;
import org.nuclearfog.twidda.adapter.holder.UserHolder;
import org.nuclearfog.twidda.backend.items.User;
import org.nuclearfog.twidda.backend.lists.UserList;
import org.nuclearfog.twidda.database.GlobalSettings;

import java.text.NumberFormat;

import jp.wasabeef.picasso.transformations.RoundedCornersTransformation;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static androidx.recyclerview.widget.RecyclerView.NO_ID;
import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;

/**
 * Adapter class for user list
 *
 * @author nuclearfog
 * @see org.nuclearfog.twidda.fragment.UserFragment
 */
public class UserAdapter extends Adapter<ViewHolder> {

    /**
     * index of {@link #loadingIndex} if no index is defined
     */
    private static final int NO_INDEX = -1;

    /**
     * View type for an user item
     */
    private static final int ITEM_USER = 0;

    /**
     * View type for a placeholder item
     */
    private static final int ITEM_GAP = 1;

    private static final NumberFormat FORMATTER = NumberFormat.getIntegerInstance();

    private UserClickListener itemClickListener;
    private GlobalSettings settings;

    private UserList data = new UserList();
    private int loadingIndex = NO_INDEX;
    private boolean userRemovable = false;

    /**
     * @param settings          app settings
     * @param itemClickListener click listener
     */
    public UserAdapter(GlobalSettings settings, UserClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
        this.settings = settings;
    }

    /**
     * insert an user list depending on cursor to the top or bottom
     *
     * @param newData new userlist
     */
    @MainThread
    public void setData(@NonNull UserList newData) {
        if (newData.isEmpty()) {
            if (!data.isEmpty() && data.peekLast() == null) {
                // remove footer
                int end = data.size() - 1;
                data.remove(end);
                notifyItemRemoved(end);
            }
        } else if (data.isEmpty() || !newData.hasPrevious()) {
            data.replace(newData);
            if (newData.hasNext()) {
                // add footer
                data.add(null);
            }
            notifyDataSetChanged();
        } else {
            int end = data.size() - 1;
            if (!newData.hasNext()) {
                // remove footer
                data.remove(end);
                notifyItemRemoved(end);
            }
            data.addAt(newData, end);
            notifyItemRangeInserted(end, newData.size());
        }
        disableLoading();
    }

    /**
     * update user information
     *
     * @param user User update
     */
    @MainThread
    public void updateUser(User user) {
        int index = data.indexOf(user);
        if (index >= 0) {
            data.set(index, user);
            notifyItemChanged(index);
        }
    }

    /**
     * remove user from adapter
     *
     * @param screenname User to remove
     */
    @MainThread
    public void removeUser(String screenname) {
        int pos = data.removeItem(screenname);
        if (pos >= 0) {
            data.remove(pos);
            notifyItemRemoved(pos);
        }
    }


    @Override
    public int getItemCount() {
        return data.size();
    }


    @Override
    public long getItemId(int index) {
        User user = data.get(index);
        if (user != null)
            return user.getId();
        return NO_ID;
    }


    @Override
    public int getItemViewType(int index) {
        if (data.get(index) == null)
            return ITEM_GAP;
        return ITEM_USER;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ITEM_USER) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
            final UserHolder vh = new UserHolder(v, settings);
            v.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = vh.getLayoutPosition();
                    User user = data.get(position);
                    if (position != NO_POSITION && user != null) {
                        itemClickListener.onUserClick(user);
                    }
                }
            });
            if (userRemovable) {
                vh.delete.setVisibility(VISIBLE);
                vh.delete.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = vh.getLayoutPosition();
                        User user = data.get(position);
                        if (position != NO_POSITION && user != null) {
                            itemClickListener.onDelete(user.getScreenname());
                        }
                    }
                });
            } else {
                vh.delete.setVisibility(GONE);
            }
            return vh;
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_placeholder, parent, false);
            final Footer vh = new Footer(v, settings, false);
            vh.loadBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = vh.getLayoutPosition();
                    if (position != NO_POSITION) {
                        boolean success = itemClickListener.onFooterClick(data.getNext());
                        if (success) {
                            vh.loadCircle.setVisibility(VISIBLE);
                            vh.loadBtn.setVisibility(INVISIBLE);
                            loadingIndex = position;
                        }
                    }
                }
            });
            return vh;
        }
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int index) {
        User user = data.get(index);
        if (holder instanceof UserHolder && user != null) {
            UserHolder userholder = (UserHolder) holder;
            userholder.textViews[0].setText(user.getUsername());
            userholder.textViews[1].setText(user.getScreenname());
            userholder.textViews[2].setText(FORMATTER.format(user.getFollowing()));
            userholder.textViews[3].setText(FORMATTER.format(user.getFollower()));
            if (user.isVerified()) {
                userholder.verifyIcon.setVisibility(VISIBLE);
            } else {
                userholder.verifyIcon.setVisibility(GONE);
            }
            if (user.isLocked()) {
                userholder.lockedIcon.setVisibility(VISIBLE);
            } else {
                userholder.lockedIcon.setVisibility(GONE);
            }
            if (settings.getImageLoad() && user.hasProfileImage()) {
                String pbLink = user.getImageLink();
                if (!user.hasDefaultProfileImage())
                    pbLink += settings.getImageSuffix();
                Picasso.get().load(pbLink).transform(new RoundedCornersTransformation(2, 0))
                        .error(R.drawable.no_image).into(userholder.profileImg);
            } else {
                userholder.profileImg.setImageResource(0);
            }
        } else if (holder instanceof Footer) {
            Footer vh = (Footer) holder;
            if (loadingIndex != NO_INDEX) {
                vh.loadCircle.setVisibility(VISIBLE);
                vh.loadBtn.setVisibility(INVISIBLE);
            } else {
                vh.loadCircle.setVisibility(INVISIBLE);
                vh.loadBtn.setVisibility(VISIBLE);
            }
        }
    }

    /**
     * disable loading animation in footer
     */
    public void disableLoading() {
        if (loadingIndex != NO_INDEX) {
            int oldIndex = loadingIndex;
            loadingIndex = NO_INDEX;
            notifyItemChanged(oldIndex);
        }
    }

    /**
     * enables delete button for an user item
     *
     * @param enable true to enable delete button
     */
    public void enableDeleteButton(boolean enable) {
        userRemovable = enable;
    }

    /**
     * Listener for list click
     */
    public interface UserClickListener {

        /**
         * handle list item click
         *
         * @param user user item
         */
        void onUserClick(User user);

        /**
         * handle footer click
         *
         * @param cursor next cursor of the list
         * @return true if click was handled
         */
        boolean onFooterClick(long cursor);

        /**
         * remove user from a list
         *
         * @param name screen name of the user
         */
        void onDelete(String name);
    }
}