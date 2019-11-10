package org.nuclearfog.twidda.fragment.backend;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.nuclearfog.twidda.adapter.TrendAdapter;
import org.nuclearfog.twidda.backend.TwitterEngine;
import org.nuclearfog.twidda.backend.helper.ErrorHandler;
import org.nuclearfog.twidda.database.AppDatabase;
import org.nuclearfog.twidda.database.GlobalSettings;
import org.nuclearfog.twidda.fragment.TrendListFragment;

import java.lang.ref.WeakReference;
import java.util.List;

import twitter4j.TwitterException;


public class TrendLoader extends AsyncTask<Void, Void, List<String>> {

    @Nullable
    private TwitterException twException;
    private WeakReference<TrendListFragment> ui;
    private TwitterEngine mTwitter;
    private AppDatabase db;
    private TrendAdapter adapter;
    private int woeId;


    public TrendLoader(@NonNull TrendListFragment fragment) {
        ui = new WeakReference<>(fragment);
        db = new AppDatabase(fragment.getContext());
        mTwitter = TwitterEngine.getInstance(fragment.getContext());
        GlobalSettings settings = GlobalSettings.getInstance(fragment.getContext());
        woeId = settings.getTrendLocation().getWoeId();
        adapter = fragment.getAdapter();
    }


    @Override
    protected void onPreExecute() {
        if (ui.get() != null)
            ui.get().setRefresh(true);
    }


    @Override
    protected List<String> doInBackground(Void[] v) {
        List<String> trends;
        try {
            if (adapter.isEmpty()) {
                trends = db.getTrends(woeId);
                if (trends.isEmpty()) {
                    trends = mTwitter.getTrends(woeId);
                    db.storeTrends(trends, woeId);
                }
            } else {
                trends = mTwitter.getTrends(woeId);
                db.storeTrends(trends, woeId);
            }
            return trends;
        } catch (TwitterException twException) {
            this.twException = twException;
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return null;
    }


    @Override
    protected void onPostExecute(@Nullable List<String> trends) {
        if (ui.get() != null) {
            if (trends != null)
                adapter.setData(trends);
            if (twException != null)
                ErrorHandler.printError(ui.get().getContext(), twException);
            ui.get().setRefresh(false);
        }
    }


    @Override
    protected void onCancelled() {
        if (ui.get() != null)
            ui.get().setRefresh(false);
    }


    @Override
    protected void onCancelled(@Nullable List<String> trends) {
        if (ui.get() != null) {
            if (trends != null)
                adapter.setData(trends);
            ui.get().setRefresh(false);
        }
    }
}