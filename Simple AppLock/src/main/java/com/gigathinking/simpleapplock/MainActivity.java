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

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;

import com.android.vending.billing.IInAppBillingService;
import com.haibison.android.lockpattern.LockPatternActivity;
import com.haibison.android.lockpattern.util.Settings;

import java.util.ArrayList;

public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    private SharedPreferences mPrefs;
    private static IInAppBillingService mService;
    private ServiceConnection mServiceConn;
    private static AppListData mAppListData;

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mServiceConn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mService = IInAppBillingService.Stub.asInterface(service);
                new CheckPurchases().execute();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mService = null;
            }
        };

        bindService(new Intent("com.android.vending.billing.InAppBillingService.BIND"), mServiceConn, Context.BIND_AUTO_CREATE);

        if(mPrefs.getString("welcome_shown", null) == null){
            startActivity(new Intent(this,Welcome.class));
        }

        mAppListData = new AppListData(this);
        mAppListData.init();
        if(!mPrefs.getBoolean(getString(R.string.register_complete),false)){
            new Register().execute();
        }

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        if(savedInstanceState != null){
            return;
        }

        if(mPrefs.getBoolean(AppLockApplication.LOCKSET, false)){
            if(mPrefs.getString(AppLockApplication.LOCKTYPE,"").equals(AppLockApplication.LOCKTYPE_PIN)){
                startActivityForResult(new Intent(this,UnlockWithPIN.class).putExtra(AppLockApplication.UNLOCK_SELF,true),23);
            }
            if(mPrefs.getString(AppLockApplication.LOCKTYPE,"").equals(AppLockApplication.LOCKTYPE_GESTURE)){
                startActivityForResult(new Intent(this,UnlockWithGesture.class).putExtra(AppLockApplication.UNLOCK_SELF,true),23);
            }
            if(mPrefs.getString(AppLockApplication.LOCKTYPE,"").equals(AppLockApplication.LOCKTYPE_PATTERN)){
                Intent intent = new Intent(LockPatternActivity.ACTION_COMPARE_PATTERN, null,this, LockPatternActivity.class);
                intent.putExtra(LockPatternActivity.EXTRA_PATTERN, Settings.Security.getPattern(this));
                intent.putExtra(LockPatternActivity.EXTRA_INTENT_ACTIVITY_FORGOT_PATTERN,new Intent(this,ResetUnlockMethod.class));
                startActivityForResult(intent, 23);
            }
        }

        Intent intent = new Intent(this,AppLaunchDetectorService.class);
        startService(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mTitle = getTitle();
        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
        rateMyApp();
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {

        // update the main content by replacing fragments
        switch (position) {
            case 0:
                getSupportFragmentManager().beginTransaction().replace(R.id.container, new AppListFragment()).commit();
                onSectionAttached(position);
                break;
            case 1:
                startActivity(new Intent(this,PrefActivity.class));
                //onSectionAttached(position);
                break;
            default:
                getSupportFragmentManager().beginTransaction().replace(R.id.container, new AppListFragment()).commit();
                onSectionAttached(position);
                break;
        }
        restoreActionBar();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 0:
                mTitle = getString(R.string.app_list);
                break;
            case 1:
                mTitle = getString(R.string.prefs);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 23:
                if(mPrefs.getString(AppLockApplication.LOCKTYPE,"").equals(AppLockApplication.LOCKTYPE_PATTERN)){
                    if((resultCode == LockPatternActivity.RESULT_OK)){
                        break;
                    }
                }
                if((resultCode != 1)){
                    finish();
                }
                break;
            default:
                super.onActivityResult(requestCode,resultCode,data);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAppListData.close();
        if (mServiceConn != null) {
            unbindService(mServiceConn);
        }
    }

    public static AppListData getAppListData(){
        return mAppListData;
    }

    private class Register extends AsyncTask<Void,Void,Void>{

        @Override
        protected Void doInBackground(Void... params) {
            RegisterDevice rd = new RegisterDevice(MainActivity.this);
            rd.doRegisterDevice();
            return null;
        }
    }

    private class CheckPurchases extends AsyncTask<Void, Void, Integer>{

        @Override
        protected Integer doInBackground(Void... params) {
            try {
                mPrefs.edit().putBoolean("no_ads_purchased",false).commit();
                mPrefs.edit().putBoolean("adv_prot_purchased",false).commit();
                Bundle ownedItems = mService.getPurchases(3,getPackageName(),"inapp",null);
                int response = ownedItems.getInt("RESPONSE_CODE");
                if(response == 0){
                    ArrayList ownedSkus =
                            ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
                    ArrayList purchaseDataList =
                            ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
                    ArrayList signatureList =
                            ownedItems.getStringArrayList("INAPP_DATA_SIGNATURE");

                    for (int i = 0; i < purchaseDataList.size(); ++i) {
                        String purchaseData = purchaseDataList.get(i).toString();
                        //String signature = signatureList.get(i).toString();
                        String sku = ownedSkus.get(i).toString();
                        if(sku.equals("no_ads")){
                            mPrefs.edit().putBoolean("no_ads_purchased",true).commit();
                        }
                        if(sku.equals("adv_prot")){
                            mPrefs.edit().putBoolean("adv_prot_purchased",true).commit();
                        }
                    }
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (NullPointerException e){
                e.printStackTrace();
            }
            return null;
        }
    }

    public static IInAppBillingService getBillingService(){
        return mService;
    }

    private void rateMyApp() {
        if (mPrefs.getString("rating","").equals("Not Interested")) {
            return;
        }
        if (mPrefs.getString("rating","").equals("RATED")) {
            return;
        }
        if(mPrefs.getBoolean("initial_run",true)) {
            mPrefs.edit().putBoolean("initial_run",false).commit();
        }
        int launchCount = mPrefs.getInt("launch_count",0);
        if(launchCount >= 10 && !mPrefs.getString("rating","").equals("May be later")) {
            mPrefs.edit().putInt("launch_count",0).commit();
            if(!mPrefs.getBoolean("rating_dialog_showing",false)){
                showRatingDialog();
            }
        }
        if(launchCount >= 7 && mPrefs.getString("rating","").equals("May be later")) {
            mPrefs.edit().putInt("launch_count",0).commit();
            if(!mPrefs.getBoolean("rating_dialog_showing",false)){
                showRatingDialog();
            }
        }
        mPrefs.edit().putInt("launch_count",launchCount+1).commit();
    }

    private void showRatingDialog() {
        mPrefs.edit().putBoolean("rating_dialog_showing",true).commit();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog;
        builder.setTitle(getString(R.string.please_rate));
        builder.setMessage(getString(R.string.please_rate_message));
        builder.setPositiveButton(getString(R.string.rate_it),new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mPrefs.edit().putString("rating","RATED").commit();
                Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(
                        "http://market.android.com/details?id=" + getPackageName()));
                startActivity(marketIntent);
                mPrefs.edit().putBoolean("rating_dialog_showing",false).commit();
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(getString(R.string.not_interested), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mPrefs.edit().putString("rating","Not Interested").commit();
                mPrefs.edit().putBoolean("rating_dialog_showing",false).commit();
                dialog.dismiss();
            }
        });
        builder.setNeutralButton(getString(R.string.may_be_later), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mPrefs.edit().putString("rating","May be later").commit();
                mPrefs.edit().putInt("launch_count",0).commit();
                mPrefs.edit().putBoolean("rating_dialog_showing",false).commit();
                dialog.dismiss();
            }
        });
        builder.setCancelable(false);
        dialog = builder.create();
        dialog.show();
    }
}
