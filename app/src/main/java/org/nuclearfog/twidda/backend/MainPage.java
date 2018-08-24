package org.nuclearfog.twidda.backend;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Toast;

import org.nuclearfog.twidda.MainActivity;
import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.backend.listitems.Trend;
import org.nuclearfog.twidda.backend.listitems.Tweet;
import org.nuclearfog.twidda.database.DatabaseAdapter;
import org.nuclearfog.twidda.viewadapter.TimelineAdapter;
import org.nuclearfog.twidda.viewadapter.TrendAdapter;

import java.lang.ref.WeakReference;
import java.util.List;

import twitter4j.TwitterException;

public class MainPage extends AsyncTask<Integer, Void, Integer> {

    public static final int HOME = 0;
    public static final int TRND = 1;
    public static final int MENT = 2;
    public static final int H_LOAD = 3;
    public static final int T_LOAD = 4;
    public static final int M_LOAD = 5;
    private static final int FAIL = -1;

    private WeakReference<MainActivity> ui;
    private TwitterEngine mTwitter;

    private TimelineAdapter timelineAdapter, mentionAdapter;
    private TrendAdapter trendsAdapter;
    private DatabaseAdapter tweetDb;
    private int woeId;
    private String errMsg = "E: Main Page, ";
    private int returnCode = 0;

    /**
     * Main View
     * @see MainActivity
     */
    public MainPage(Context context) {
        ui = new WeakReference<>((MainActivity)context);
        mTwitter = TwitterEngine.getInstance(context);
        GlobalSettings settings = GlobalSettings.getInstance(context);
        tweetDb = new DatabaseAdapter(context);
        woeId = settings.getWoeId();
        int highlight = settings.getHighlightColor();
        int font = settings.getFontColor();
        boolean image = settings.loadImages();

        RecyclerView timelineList = ui.get().findViewById(R.id.tl_list);
        RecyclerView trendList = ui.get().findViewById(R.id.tr_list);
        RecyclerView mentionList = ui.get().findViewById(R.id.m_list);
        timelineAdapter = (TimelineAdapter) timelineList.getAdapter();
        trendsAdapter = (TrendAdapter) trendList.getAdapter();
        mentionAdapter = (TimelineAdapter) mentionList.getAdapter();

        if(timelineAdapter == null) {
            timelineAdapter = new TimelineAdapter(ui.get());
            timelineList.setAdapter(timelineAdapter);
            timelineAdapter.setColor(highlight, font);
            timelineAdapter.toggleImage(image);
        }
        if(trendsAdapter == null) {
            trendsAdapter = new TrendAdapter(ui.get());
            trendList.setAdapter(trendsAdapter);
            trendsAdapter.setColor(font);
        }
        if(mentionAdapter == null) {
            mentionAdapter = new TimelineAdapter(ui.get());
            mentionList.setAdapter(mentionAdapter);
            mentionAdapter.setColor(highlight, font);
            mentionAdapter.toggleImage(image);
        }
    }

    /**
     * @param args [0] Execution Mode: (0)HomeTL, (1)Trend, (2)Mention
     * @return Mode
     */
    @Override
    protected Integer doInBackground(Integer... args) {
        final int MODE = args[0];
        int page = args[1];
        long id = 1L;
        List<Tweet> tweets;
        try {
            switch (MODE) {
                case HOME:

                    if(timelineAdapter.getItemCount() > 0) {
                        id = timelineAdapter.getItemId(0);
                        tweets = mTwitter.getHome(page,id);
                        timelineAdapter.addNew(tweets);
                    } else {
                        tweets = mTwitter.getHome(page,id);
                        timelineAdapter.setData(tweets);
                    }
                    tweetDb.storeHomeTimeline(tweets);
                    break;

                case H_LOAD:

                    tweets = tweetDb.getHomeTimeline();
                    timelineAdapter.setData(tweets);
                    break;

                case TRND:

                    List<Trend> trends = mTwitter.getTrends(woeId);
                    tweetDb.store(trends, woeId);
                    trendsAdapter.setData(trends);
                    break;

                case T_LOAD:

                    trendsAdapter.setData(tweetDb.load(woeId));
                    break;

                case MENT:

                    List<Tweet> mention;
                    if(mentionAdapter.getItemCount() != 0) {
                        id = mentionAdapter.getItemId(0);
                        mention = mTwitter.getMention(page,id);
                        mentionAdapter.addNew(mention);
                    } else {
                        mention = mTwitter.getMention(page,id);
                        mentionAdapter.setData(mention);
                    }
                    tweetDb.storeMentions(mention);
                    break;

                case M_LOAD:

                    mention = tweetDb.getMentions();
                    mentionAdapter.setData(mention);
                    break;
            }
        } catch (TwitterException e) {
            returnCode = e.getErrorCode();
            if (returnCode > 0 && returnCode != 420) {
                errMsg += e.getMessage();
            }
            return FAIL;
        }
        catch (Exception e) {
            Log.e("Main Page", e.getMessage());
            return FAIL;
        }
        return MODE;
    }

    @Override
    protected void onPostExecute(Integer MODE) {
        MainActivity connect = ui.get();
        if(connect == null)
            return;
        SwipeRefreshLayout timelineRefresh = connect.findViewById(R.id.timeline);
        SwipeRefreshLayout trendRefresh = connect.findViewById(R.id.trends);
        SwipeRefreshLayout mentionRefresh = connect.findViewById(R.id.mention);

        switch(MODE) {
            case HOME:
            case H_LOAD:
                timelineAdapter.notifyDataSetChanged();
                timelineRefresh.setRefreshing(false);
                break;

            case TRND:
            case T_LOAD:
                trendsAdapter.notifyDataSetChanged();
                trendRefresh.setRefreshing(false);
                break;

            case MENT:
            case M_LOAD:
                mentionAdapter.notifyDataSetChanged();
                mentionRefresh.setRefreshing(false);
                break;

            case FAIL:
                if (returnCode > 0) {
                    if (returnCode == 420) {
                        Toast.makeText(connect, R.string.rate_limit_exceeded, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(connect, errMsg, Toast.LENGTH_LONG).show();
                    }
                }
            default:
                timelineRefresh.setRefreshing(false);
                trendRefresh.setRefreshing(false);
                mentionRefresh.setRefreshing(false);
        }
    }
}