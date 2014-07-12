/*		Copyright (C) 2014  Adarsha HD
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package com.gigathinking.simpleapplock;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
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
import java.net.URLEncoder;

public class RegisterDevice {
    public static final String PROPERTY_REG_ID = "registration_id";
    public static final String PROPERTY_REG_ID_OLD = "registration_id_old";
    public static final String GCM_SENDER_ID = "YOUR_REGISTRATION_ID";
    private final String TAG = getClass().getName();

    private GoogleCloudMessaging gcm;
    private SharedPreferences prefs;
    private String regID;
    private Context mContext;

    public RegisterDevice(Context context){
        mContext = context;
    }
    public Boolean doRegisterDevice(){
        if(!gcmRegisterClient()){
            return false;
        }
        if(!registerDevice()){
            return false;
        }
        return true;
    }

    private boolean gcmRegisterClient() {
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        regID = prefs.getString(PROPERTY_REG_ID, null);

        gcm = GoogleCloudMessaging.getInstance(mContext);

        // If there is no registration ID, the app isn't registered.
        // Call registerBackground() to register it.
        if (regID == null) {
            try {
                regID = gcm.register(GCM_SENDER_ID);

                // You should send the registration ID to your server over HTTP,
                // so it can use GCM/HTTP or CCS to send messages to your app.

                // For this demo: we don't need to send it because the device
                // will send upstream messages to a server that will echo back
                // the message using the 'from' address in the message.

                // Save the regID for future use - no need to register again.
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(PROPERTY_REG_ID, regID);
                editor.commit();
                Log.i(TAG,"Successfully Registered with GCM Server");
                return true;
            } catch (IOException ex) {
                Log.e(TAG, "Could not register to GCM Server!");
                return false;
            }
        }
        return true;
    }

    private boolean registerDevice(){
        String username;
        String device = Build.MODEL;
        String version = Build.VERSION.RELEASE;
        if(prefs.getString(mContext.getString(R.string.user_name),null) == null){
            AccountManager manager = AccountManager.get(mContext);
            Account[] accounts = manager != null ? manager.getAccountsByType("com.google") : new Account[0];
            if(accounts.length == 0) {
                return false;
            }
            username = accounts[0].name.split("@")[0];
            prefs.edit().putString(mContext.getString(R.string.user_name),username).commit();
        } else {
            username = prefs.getString(mContext.getString(R.string.user_name),"");
        }

        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params,5000);
        HttpConnectionParams.setSoTimeout(params, 10000);
        HttpClient client = new DefaultHttpClient(params);
        try
        {
            String post;
            if(prefs.getString(RegisterDevice.PROPERTY_REG_ID_OLD,null) == null){
                post = ServerInfo.RESET_SERVER + "/register_device.php?user=" + username
                        + "&deviceid=" +  regID
                        + "&devicename=" + URLEncoder.encode(device,"utf-8")
                        + "&version=" + version;
            } else {
                post = ServerInfo.RESET_SERVER
                        + "/update_device_id.php?dev_id=" + prefs.getString(PROPERTY_REG_ID_OLD,"")
                        + "&dev_id_new=" +  prefs.getString(PROPERTY_REG_ID,"");
            }
            HttpPost httpPost = new HttpPost(post);
            HttpEntity localHttpEntity = client.execute(httpPost).getEntity();
            if (localHttpEntity != null)
            {
                int res = Integer.valueOf(new BufferedReader(new InputStreamReader(localHttpEntity.getContent(), "UTF-8")).readLine());
                prefs.edit().putBoolean(mContext.getString(R.string.register_complete),true).commit();
                return res != -1;
            }
        }
        catch (HttpHostConnectException e) {
            return false;
        }
        catch (IOException localIOException){
            return false;
        }
        return true;
    }
}
