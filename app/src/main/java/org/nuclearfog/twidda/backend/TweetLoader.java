package org.nuclearfog.twidda.backend;

import android.os.AsyncTask;

import androidx.annotation.Nullable;

import org.nuclearfog.twidda.activity.TweetActivity;
import org.nuclearfog.twidda.backend.engine.EngineException;
import org.nuclearfog.twidda.backend.engine.TwitterEngine;
import org.nuclearfog.twidda.backend.items.Tweet;
import org.nuclearfog.twidda.database.AppDatabase;

import java.lang.ref.WeakReference;


/**
 * Background task to download tweet informations and to take actions
 *
 * @see TweetActivity
 */
public class TweetLoader extends AsyncTask<TweetLoader.Action, Tweet, TweetLoader.Action> {

    /**
     * actions for the tweet
     */
    public enum Action {
        /**
         * Load tweet
         */
        LOAD,
        /**
         * retweet tweet
         */
        RETWEET,
        /**
         * remove retweet
         */
        UNRETWEET,
        /**
         * favorite tweet
         */
        FAVORITE,
        /**
         * remove tweet from favorites
         */
        UNFAVORITE,
        /**
         * delete own tweet
         */
        DELETE
    }

    @Nullable
    private EngineException twException;
    private TwitterEngine mTwitter;
    private WeakReference<TweetActivity> callback;
    private AppDatabase db;
    private long tweetId, myRetweetId;

    /**
     * @param callback Callback to return tweet information
     * @param tweet    Tweet information
     */
    public TweetLoader(TweetActivity callback, Tweet tweet) {
        this(callback, tweet.getId());
        this.myRetweetId = tweet.getMyRetweetId();
    }

    /**
     * @param callback Callback to return tweet information
     * @param tweetId  ID of the tweet
     */
    public TweetLoader(TweetActivity callback, long tweetId) {
        super();
        db = new AppDatabase(callback);
        mTwitter = TwitterEngine.getInstance(callback);
        this.callback = new WeakReference<>(callback);
        this.tweetId = tweetId;
    }


    @Override
    protected Action doInBackground(Action[] action) {
        try {
            switch (action[0]) {
                case LOAD:
                    boolean updateStatus = false;
                    Tweet tweet = db.getStatus(tweetId);
                    if (tweet != null) {
                        publishProgress(tweet);
                        updateStatus = true;
                    }
                    tweet = mTwitter.getStatus(tweetId);
                    publishProgress(tweet);
                    if (updateStatus) {
                        // update tweet if there is a database entry
                        db.updateStatus(tweet);
                    }
                    break;

                case DELETE:
                    mTwitter.deleteTweet(tweetId);
                    db.removeStatus(tweetId);
                    break;

                case RETWEET:
                    tweet = mTwitter.retweet(tweetId, true);
                    publishProgress(tweet);
                    db.updateStatus(tweet);
                    break;

                case UNRETWEET:
                    tweet = mTwitter.retweet(tweetId, false);
                    publishProgress(tweet);
                    db.updateStatus(tweet);
                    // remove status pointing on the retweeted status
                    db.removeStatus(myRetweetId);
                    break;

                case FAVORITE:
                    tweet = mTwitter.favorite(tweetId, true);
                    publishProgress(tweet);
                    db.storeFavorite(tweet);
                    break;

                case UNFAVORITE:
                    tweet = mTwitter.favorite(tweetId, false);
                    publishProgress(tweet);
                    db.removeFavorite(tweetId);
                    break;
            }
        } catch (EngineException twException) {
            this.twException = twException;
            if (twException.resourceNotFound()) {
                db.removeStatus(tweetId);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return action[0];
    }


    @Override
    protected void onProgressUpdate(Tweet[] tweets) {
        if (callback.get() != null) {
            callback.get().setTweet(tweets[0]);
        }
    }


    @Override
    protected void onPostExecute(Action action) {
        if (callback.get() != null) {
            if (twException != null) {
                callback.get().onError(twException);
            } else {
                callback.get().onAction(action);
            }
        }
    }
}