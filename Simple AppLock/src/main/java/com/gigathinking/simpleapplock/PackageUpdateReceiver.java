package com.gigathinking.simpleapplock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class PackageUpdateReceiver extends BroadcastReceiver {
    private SharedPreferences preferences;
    public PackageUpdateReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("APPLOCK","Package upgrade");
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putString(RegisterDevice.PROPERTY_REG_ID_OLD, preferences.getString(RegisterDevice.PROPERTY_REG_ID,"")).commit();
        new UpdateDeviceID().execute(context);
    }

    private class UpdateDeviceID extends AsyncTask<Context,Void,Boolean>{

        Context context;
        @Override
        protected Boolean doInBackground(Context... ctx) {
            this.context = ctx[0];
            GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
            HttpParams params = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(params, 5000);
            HttpConnectionParams.setSoTimeout(params, 10000);
            HttpClient client = new DefaultHttpClient(params);
            try
            {
                String regID = gcm.register(RegisterDevice.GCM_SENDER_ID);
                preferences.edit().putString(RegisterDevice.PROPERTY_REG_ID,regID).commit();
                String regIDOld = preferences.getString(RegisterDevice.PROPERTY_REG_ID_OLD,"");
                String post = ServerInfo.RESET_SERVER
                        + "/update_device_id.php?dev_id=" + regIDOld
                        + "&dev_id_new=" +  regID;
                HttpPost httpPost = new HttpPost(post);
                HttpEntity localHttpEntity = client.execute(httpPost).getEntity();
                if (localHttpEntity != null)
                {
                    int res = Integer.valueOf(new BufferedReader(new InputStreamReader(localHttpEntity.getContent(), "UTF-8")).readLine());
                    return res != -1;
                }
            }
            catch (HttpHostConnectException e) {
                return false;
            }
            catch (IOException localIOException){
                return false;
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if(aBoolean){
                preferences.edit().putBoolean(context.getString(R.string.register_complete),true).commit();
            } else {
                preferences.edit().putBoolean(context.getString(R.string.register_complete),false).commit();
            }
        }
    }
}
