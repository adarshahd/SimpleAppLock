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

import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.haibison.android.lockpattern.LockPatternActivity;
import com.haibison.android.lockpattern.util.Settings;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppLaunchDetectorService extends Service {

    private final IBinder mIBinder = new MyBinder();
    private SharedPreferences mPrefs;
    private ArrayList<String> mList;
    private Map<String,Boolean> mAppMap;
    private AppListData mAppListData;
    private Map<String,Integer>mLastOpen;

    private BroadcastReceiver updateReceiver;

    public AppLaunchDetectorService() {
    }

    public class MyBinder extends Binder{
        AppLaunchDetectorService getService(){
            return AppLaunchDetectorService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mAppListData = new AppListData(getApplicationContext());
        mAppListData.init();
        updateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(!mList.isEmpty()){
                    mList.clear();
                }
                if(!mAppMap.isEmpty()){
                    mAppMap.clear();
                }
                Cursor cursor = mAppListData.getAppListInfo();
                int count = cursor.getCount();
                for (int i=0;i<count;++i){
                    cursor.moveToPosition(i);
                    mList.add(cursor.getString(2));
                    mAppMap.put(cursor.getString(2),Boolean.valueOf(cursor.getString(3)));
                    mLastOpen.put(cursor.getString(2),cursor.getInt(4));
                }
            }
        };
        mList = new ArrayList<String>();
        mAppMap = new HashMap<String, Boolean>();
        mLastOpen = new HashMap<String, Integer>();
        Cursor cursor = mAppListData.getAppListInfo();
        int count = cursor.getCount();
        for (int i=0;i<count;++i){
            cursor.moveToPosition(i);
            mList.add(cursor.getString(2));
            mAppMap.put(cursor.getString(2),Boolean.valueOf(cursor.getString(3)));
            mLastOpen.put(cursor.getString(2),cursor.getInt(4));
        }
        cursor.close();
        LocalBroadcastManager.getInstance(this).registerReceiver(updateReceiver,new IntentFilter(AppLockApplication.UPDATE_LIST));
        new DetectAppLaunch().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updateReceiver);
        mAppListData.close();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mIBinder;
    }

    private class DetectAppLaunch extends AsyncTask<Void,Void,Void>{

        @Override
        protected Void doInBackground(Void... params) {

            /*
             *  FIXME
                Need to think upon the logic!
             */

            ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            Log.i("APP_LOCK", "Starting detector service . . .");
            while(true){

                List<ActivityManager.RunningTaskInfo> list = manager.getRunningTasks(1);
                String activity = (list.get(0).topActivity).getPackageName();
                String activityClass = list.get(0).topActivity.getClassName();
                boolean isTimerEnabled = mPrefs.getBoolean("enable_timer",false);

                /*
                *   Code to detect user trying to disable Simple AppLock as Device administrator. If the admin is active fire unlock activity.
                * */

                if (activityClass.equals("com.android.settings.DeviceAdminAdd") && !mPrefs.getBoolean(AppLockApplication.UNLOCKED,false)){
                    if(mPrefs.getBoolean("admin_active",false)){
                        Intent intent = new Intent(AppLaunchDetectorService.this,UnlockWithPIN.class);
                        if(mPrefs.getString(AppLockApplication.LOCKTYPE,"").equals(AppLockApplication.LOCKTYPE_GESTURE)) {
                            intent = new Intent(AppLaunchDetectorService.this,UnlockWithGesture.class);
                        }
                        if(mPrefs.getString(AppLockApplication.LOCKTYPE,"").equals(AppLockApplication.LOCKTYPE_PATTERN)){
                            intent = new Intent(LockPatternActivity.ACTION_COMPARE_PATTERN, null,AppLaunchDetectorService.this, LockPatternActivity.class);
                            intent.putExtra(LockPatternActivity.EXTRA_PATTERN, Settings.Security.getPattern(AppLaunchDetectorService.this));
                            intent.putExtra(LockPatternActivity.EXTRA_INTENT_ACTIVITY_FORGOT_PATTERN,new Intent(AppLaunchDetectorService.this,ResetUnlockMethod.class));
                            LockPatternActivity.setContext(AppLaunchDetectorService.this);
                            intent.putExtra("unlock_self",false);
                        }
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        intent.putExtra(Intent.EXTRA_TEXT,activityClass);
                        intent.putExtra(AppLockApplication.UNLOCK_SELF,true);
                        startActivity(intent);
                    }
                }

                /*
                *   Code to detect application uninstall activity. If lock is enabled for uninstall of applications
                *   fire the unlock activity.
                * */

                if(activityClass.equals("com.android.packageinstaller.UninstallerActivity") && !mPrefs.getBoolean(AppLockApplication.UNLOCKED,false) && mPrefs.getBoolean("lock_uninstall",false)){
                    Intent intent = new Intent(AppLaunchDetectorService.this,UnlockWithPIN.class);
                    if(mPrefs.getString(AppLockApplication.LOCKTYPE,"").equals(AppLockApplication.LOCKTYPE_GESTURE)) {
                        intent = new Intent(AppLaunchDetectorService.this,UnlockWithGesture.class);
                    }
                    if(mPrefs.getString(AppLockApplication.LOCKTYPE,"").equals(AppLockApplication.LOCKTYPE_PATTERN)){
                        intent = new Intent(LockPatternActivity.ACTION_COMPARE_PATTERN, null,AppLaunchDetectorService.this, LockPatternActivity.class);
                        intent.putExtra(LockPatternActivity.EXTRA_PATTERN, Settings.Security.getPattern(AppLaunchDetectorService.this));
                        intent.putExtra(LockPatternActivity.EXTRA_INTENT_ACTIVITY_FORGOT_PATTERN,new Intent(AppLaunchDetectorService.this,ResetUnlockMethod.class));
                        LockPatternActivity.setContext(AppLaunchDetectorService.this);
                        intent.putExtra("unlock_self",false);
                    }
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    intent.putExtra(Intent.EXTRA_TEXT,activityClass);
                    startActivity(intent);
                }

                /*
                *   Check if activity is being launched from Recent activity button
                *
                * */

                if(activityClass.equals("com.android.systemui.recent.RecentsActivity")){
                    mPrefs.edit().putString(AppLockApplication.LASTAPP,activityClass).commit();
                }

                /*
                *   Prevent repeated starting of unlock activity for self and SystemUI
                * */

                if(activity.equals(getPackageName()) || activity.equals("com.android.systemui")){
                    try {
                        Thread.sleep(600L);                 //Let's not hog the CPU
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }

                /*
                *   Detect if the application just unlocked is running. If it is unlock activity should not be started repeatedly.
                * */

                if((mPrefs.getString(AppLockApplication.LASTAPP,"").equals(activity) || mPrefs.getString(AppLockApplication.LASTAPP,"").equals(activityClass))){
                    try {
                        Thread.sleep(600L);                 //Let's not hog the CPU
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                } else {
                    mPrefs.edit().putBoolean(AppLockApplication.UNLOCKED,false).commit();
                    mPrefs.edit().putString(AppLockApplication.LASTAPP,activity).commit();
                }

                /*
                *   Check for incoming calls. If locking is not explicitly selected from preference the do not lock incoming calls.
                * */

                boolean isRinging = (((TelephonyManager)getSystemService(TELEPHONY_SERVICE)).getCallState() == TelephonyManager.CALL_STATE_RINGING)
                        || (((TelephonyManager)getSystemService(TELEPHONY_SERVICE)).getCallState() == TelephonyManager.CALL_STATE_OFFHOOK);
                boolean lockWhileRing = mPrefs.getBoolean("in_call",false);

                /*
                *   General app launch detection loop. If the app is in the lock list, show the unlock activity.
                * */

                if(mList.contains(activity) && mAppMap.get(activity) && !mPrefs.getBoolean(AppLockApplication.UNLOCKED,false)){
                    if(isRinging){
                        if(!lockWhileRing){
                            try {
                                Thread.sleep(600L);                 //Let's not hog the CPU
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            Log.i("APP_LOCK","In a call, continuing . . .");
                            continue;
                        }
                    }
                    if(isTimerEnabled){
                        int window = Integer.parseInt(mPrefs.getString("timer_value", "10"));
                        int curTime = (int) (new Date().getTime()/1000);
                        int appTimeLimit = mLastOpen.get(activity) + window;
                        if(curTime < appTimeLimit){
                            try {
                                Thread.sleep(600L);                 //Let's not hog the CPU
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            continue;
                        }
                        Log.i("APP_LOCK","Timeout! unlcok to continue");
                    }
                    Intent intent = new Intent(AppLaunchDetectorService.this,UnlockWithPIN.class);
                    if(mPrefs.getString(AppLockApplication.LOCKTYPE,"").equals(AppLockApplication.LOCKTYPE_GESTURE)) {
                        intent = new Intent(AppLaunchDetectorService.this,UnlockWithGesture.class);
                    }
                    if(mPrefs.getString(AppLockApplication.LOCKTYPE,"").equals(AppLockApplication.LOCKTYPE_PATTERN)){
                        intent = new Intent(LockPatternActivity.ACTION_COMPARE_PATTERN, null,AppLaunchDetectorService.this, LockPatternActivity.class);
                        intent.putExtra(LockPatternActivity.EXTRA_PATTERN, Settings.Security.getPattern(AppLaunchDetectorService.this));
                        intent.putExtra(LockPatternActivity.EXTRA_INTENT_ACTIVITY_FORGOT_PATTERN,new Intent(AppLaunchDetectorService.this,ResetUnlockMethod.class));
                        LockPatternActivity.setContext(AppLaunchDetectorService.this);
                        intent.putExtra("unlock_self",false);
                        int time = (int) (new Date().getTime()/1000);
                        mLastOpen.put(activity,time);
                    }
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    intent.putExtra(Intent.EXTRA_TEXT,activity);

                    startActivity(intent);
                    Log.i("APP_LOCK", "detected activity launch");
                }
            }
        }
    }
}
