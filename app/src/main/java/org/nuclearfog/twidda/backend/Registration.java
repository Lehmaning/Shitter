package org.nuclearfog.twidda.backend;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import org.nuclearfog.twidda.database.ErrorLog;
import org.nuclearfog.twidda.window.LoginPage;

import java.lang.ref.WeakReference;

public class Registration extends AsyncTask<String, Void, Boolean> {

    private WeakReference<LoginPage> ui;
    private TwitterEngine mTwitter;
    private String errorMessage;


    public Registration(Context context) {
        ui = new WeakReference<>((LoginPage)context);
        mTwitter = TwitterEngine.getInstance(context);
    }


    @Override
    protected Boolean doInBackground( String... twitterPin ) {
        String pin = twitterPin[0];
        try {
            if( pin.trim().isEmpty() ) {
                mTwitter.request(ui.get());
            } else {
                mTwitter.initialize(pin);
                return true;
            }
        } catch ( Exception e ) {
            errorMessage = e.getMessage();
            ErrorLog errorLog = new ErrorLog(ui.get());
            errorLog.add(errorMessage);
        }
        return false;
    }


    @Override
    protected void onPostExecute(Boolean success) {
        LoginPage connect = ui.get();
        if(connect == null)
            return;
        if(success) {
            connect.setResult(Activity.RESULT_OK);
            connect.finish();
        } else if(errorMessage != null) {
            Toast.makeText(connect,"Fehler: "+errorMessage,Toast.LENGTH_LONG).show();
        }
    }
}