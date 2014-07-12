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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.Random;


public class ResetUnlockMethod extends Activity {

    private BroadcastReceiver mResetReceiver;
    private ProgressDialog mDialog;
    private String mResetKey;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_reset);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mDialog = new ProgressDialog(this,ProgressDialog.STYLE_SPINNER);
        mDialog.setIndeterminate(true);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        mResetReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                prefs.edit().putString(AppLockApplication.LOCKTYPE,AppLockApplication.LOCKTYPE_PIN).commit();
                prefs.edit().putString(AppLockApplication.PASSWORD,intent.getStringExtra(AppLockApplication.PASSWORD)).commit();
                Toast.makeText(context, getString(R.string.pin_reset), Toast.LENGTH_LONG).show();
                startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                //startActivity(new Intent(ResetUnlockMethod.this,UnlockWithPIN.class).putExtra("test","test"));
                finish();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(mResetReceiver,new IntentFilter(AppLockApplication.RESET_UNLOCK));

        if(!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.register_complete),false)){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.reset_register_device));
            builder.setTitle(getString(R.string.info));
            builder.setPositiveButton(getString(R.string.register), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    new Register().execute();
                    if (mDialog != null) {
                        mDialog.setMessage(getString(R.string.register_ongoing));
                        mDialog.show();
                    }
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    finish();
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_HOME);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            });
            builder.create().show();
        } else {
            if (mDialog != null) {
                mDialog.setMessage(getString(R.string.connect_to_server));
                mDialog.show();
            }
            new DoRequestReset().execute();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mResetReceiver);
    }

    @Override
    public void onBackPressed() {
        finish();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mDialog != null){
            mDialog.dismiss();
        }
        mDialog = null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class DoRequestReset extends AsyncTask<Void,Void,Integer>{

        @Override
        protected Integer doInBackground(Void... params) {
            mResetKey = "Error!";
            HttpParams httpParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
            HttpConnectionParams.setSoTimeout(httpParams, 10000);
            HttpClient client = new DefaultHttpClient(httpParams);
            int res = -1;
            try
            {
                Random random = new Random();
                mResetKey = "" + (random.nextInt(900000) + 100000);
                String post = ServerInfo.RESET_SERVER + "/update_reset_code.php?user=" + prefs.getString(getString(R.string.user_name),"")
                        + "&code=" +  mResetKey;
                HttpPost httpPost = new HttpPost(post);
                HttpEntity localHttpEntity = client.execute(httpPost).getEntity();
                if (localHttpEntity != null)
                {
                    res = Integer.valueOf(new BufferedReader(new InputStreamReader(localHttpEntity.getContent(), "UTF-8")).readLine());
                }
            }
            catch (HttpHostConnectException e) {
                return -1;
            }
            catch (IOException localIOException){
                return -1;
            }

            return res;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (mDialog != null) {
                mDialog.dismiss();
            }
            if(result != 1){
                Toast.makeText(ResetUnlockMethod.this,getString(R.string.could_not_connect),Toast.LENGTH_SHORT).show();
                mResetKey = "Error!";
                ((TextView)findViewById(R.id.id_tv_reset_pin)).setText(mResetKey);
                return;
            }
            ((TextView)findViewById(R.id.id_tv_reset_pin)).setText(mResetKey);
        }
    }

    private class Register extends AsyncTask<Void,Void,Boolean>{

        @Override
        protected Boolean doInBackground(Void... params) {
            RegisterDevice rd = new RegisterDevice(ResetUnlockMethod.this);
            return rd.doRegisterDevice();
        }

        @Override
        protected void onPostExecute(Boolean b) {
            if(b){
                if (mDialog != null) {
                    mDialog.setMessage(getString(R.string.register_success));
                }
                new DoRequestReset().execute();
            } else {
                if (mDialog != null) {
                    mDialog.dismiss();
                }
                Toast.makeText(ResetUnlockMethod.this,getString(R.string.could_not_register),Toast.LENGTH_SHORT).show();
            }
        }
    }

}
