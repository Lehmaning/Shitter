package org.nuclearfog.twidda.adapter;

import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.squareup.picasso.Picasso;

import org.nuclearfog.tag.Tagger;
import org.nuclearfog.tag.Tagger.OnTagClickListener;
import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.adapter.holder.Footer;
import org.nuclearfog.twidda.adapter.holder.MessageHolder;
import org.nuclearfog.twidda.backend.items.Message;
import org.nuclearfog.twidda.backend.items.User;
import org.nuclearfog.twidda.backend.lists.MessageList;
import org.nuclearfog.twidda.database.GlobalSettings;

import jp.wasabeef.picasso.transformations.RoundedCornersTransformation;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;
import static org.nuclearfog.twidda.backend.utils.StringTools.formatCreationTime;

/**
 * Adapter class for direct messages list
 *
 * @author nuclearfog
 * @see org.nuclearfog.twidda.fragment.MessageFragment
 */
public class MessageAdapter extends Adapter<ViewHolder> {

    private static final int NO_INDEX = -1;
    private static final int TYPE_MESSAGE = 0;
    private static final int TYPE_FOOTER = 1;

    private OnItemSelected itemClickListener;
    private GlobalSettings settings;

    private MessageList data = new MessageList(null, null);
    private int loadingIndex = NO_INDEX;

    /**
     * @param settings          App settings for theme
     * @param itemClickListener click listener
     */
    public MessageAdapter(GlobalSettings settings, OnItemSelected itemClickListener) {
        this.itemClickListener = itemClickListener;
        this.settings = settings;
    }

    /**
     * set messages
     *
     * @param newData new message list
     */
    @MainThread
    public void setData(MessageList newData) {
        if (newData.isEmpty()) {
            if (!data.isEmpty() && data.peekLast() == null) {
                int end = data.size() - 1;
                data.remove(end);
                notifyItemRemoved(end);
            }
        } else if (data.isEmpty() || !newData.hasPrev()) {
            data.replaceAll(newData);
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
     * Remove a single item from list if found
     *
     * @param id message ID
     */
    @MainThread
    public void remove(long id) {
        int pos = data.removeItem(id);
        if (pos >= 0) {
            notifyItemRemoved(pos);
        }
    }


    @Override
    public long getItemId(int index) {
        Message message = data.get(index);
        if (message != null)
            return message.getId();
        return -1;
    }


    @Override
    public int getItemCount() {
        return data.size();
    }


    @Override
    public int getItemViewType(int index) {
        if (data.get(index) == null)
            return TYPE_FOOTER;
        return TYPE_MESSAGE;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_MESSAGE) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dm, parent, false);
            final MessageHolder vh = new MessageHolder(view, settings);
            vh.buttons[0].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = vh.getLayoutPosition();
                    if (position != NO_POSITION) {
                        itemClickListener.onClick(data.get(position), OnItemSelected.Action.ANSWER);
                    }
                }
            });
            vh.buttons[1].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = vh.getLayoutPosition();
                    if (position != NO_POSITION) {
                        itemClickListener.onClick(data.get(position), OnItemSelected.Action.DELETE);
                    }
                }
            });
            vh.profile_img.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = vh.getLayoutPosition();
                    if (position != NO_POSITION) {
                        itemClickListener.onClick(data.get(position), OnItemSelected.Action.PROFILE);
                    }
                }
            });
            return vh;
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_placeholder, parent, false);
            final Footer footer = new Footer(v, settings, false);
            footer.loadBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = footer.getLayoutPosition();
                    if (position != NO_POSITION) {
                        boolean success = itemClickListener.onFooterClick(data.getNextCursor());
                        if (success) {
                            footer.loadCircle.setVisibility(VISIBLE);
                            footer.loadBtn.setVisibility(INVISIBLE);
                            loadingIndex = position;
                        }
                    }
                }
            });
            return footer;
        }
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder vh, int index) {
        if (vh instanceof MessageHolder) {
            Message message = data.get(index);
            if (message != null) {
                User sender = message.getSender();
                Spanned text = Tagger.makeTextWithLinks(message.getText(), settings.getHighlightColor(), itemClickListener);

                MessageHolder holder = (MessageHolder) vh;
                holder.textViews[0].setText(sender.getUsername());
                holder.textViews[1].setText(sender.getScreenname());
                holder.textViews[2].setText(message.getReceiver().getScreenname());
                holder.textViews[3].setText(formatCreationTime(message.getTime()));
                holder.textViews[4].setText(text);
                if (sender.isVerified()) {
                    holder.verifiedIcon.setVisibility(VISIBLE);
                } else {
                    holder.verifiedIcon.setVisibility(GONE);
                }
                if (sender.isLocked()) {
                    holder.lockedIcon.setVisibility(VISIBLE);
                } else {
                    holder.lockedIcon.setVisibility(GONE);
                }
                if (settings.getImageLoad() && sender.hasProfileImage()) {
                    String pbLink = sender.getImageLink();
                    if (!sender.hasDefaultProfileImage())
                        pbLink += settings.getImageSuffix();
                    Picasso.get().load(pbLink).transform(new RoundedCornersTransformation(2, 0))
                            .error(R.drawable.no_image).into(holder.profile_img);
                } else {
                    holder.profile_img.setImageResource(0);
                }
            }
        } else if (vh instanceof Footer) {
            Footer footer = (Footer) vh;
            if (loadingIndex != NO_INDEX) {
                footer.loadCircle.setVisibility(VISIBLE);
                footer.loadBtn.setVisibility(INVISIBLE);
            } else {
                footer.loadCircle.setVisibility(INVISIBLE);
                footer.loadBtn.setVisibility(VISIBLE);
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
     * callback for the click listener
     */
    public interface OnItemSelected extends OnTagClickListener {

        enum Action {
            ANSWER,
            DELETE,
            PROFILE,
        }

        /**
         * called when a button was clicked
         *
         * @param message Message information
         * @param action  what button was clicked
         */
        void onClick(Message message, Action action);

        /**
         * called when the footer was clicked
         *
         * @param cursor message cursor
         * @return true if task was started
         */
        boolean onFooterClick(String cursor);
    }
}