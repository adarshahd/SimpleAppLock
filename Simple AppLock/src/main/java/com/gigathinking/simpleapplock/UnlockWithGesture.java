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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

public class UnlockWithGesture extends Activity{
    private GestureLibrary library;
    private String mPackage;
    private boolean unlockSelf;
    private boolean changeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_unlockwithgesture);
        GestureOverlayView view = (GestureOverlayView) findViewById(R.id.gestureOverlayView);
        view.addOnGestureListener(new GestureProcessor());
        File gestureFile = new File(Environment.getDataDirectory() + "/data/" + getPackageName(), AppLockApplication.GESTUREFILE);
        library = GestureLibraries.fromFile(gestureFile);
        Bundle bundle = getIntent().getExtras();
        mPackage = bundle.getString(Intent.EXTRA_TEXT);
        unlockSelf = bundle.getBoolean(AppLockApplication.UNLOCK_SELF);
        changeLock = false;
        changeLock = bundle.getBoolean(AppLockApplication.CHANGELOCK);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!unlockSelf) {
            try {
                ((ImageView)findViewById(R.id.iv_icon_unlock)).setImageDrawable(getPackageManager().getApplicationIcon(mPackage));
                ((TextView)findViewById(R.id.tv_app_unlock)).setText(getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(mPackage,0)));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            findViewById(R.id.id_ll_icon).setVisibility(View.GONE);
            ((TextView)findViewById(R.id.tv_unlock_message)).setText(getString(R.string.unlock_self_gesture));
        }
        findViewById(R.id.id_btn_forgot_gesture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(UnlockWithGesture.this,ResetUnlockMethod.class));
                finish();
            }
        });
        library.load();
    }

    @Override
    public void onBackPressed() {
        if(changeLock){
            setResult(AppLockApplication.RESULT_NOT_OK);
            finish();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private class GestureProcessor implements GestureOverlayView.OnGestureListener {
        @Override
        public void onGestureStarted(GestureOverlayView overlay, MotionEvent event) {
            findViewById(R.id.tv_gesture_desc).setVisibility(View.INVISIBLE);
        }

        @Override
        public void onGesture(GestureOverlayView overlay, MotionEvent event) {

        }

        @Override
        public void onGestureEnded(GestureOverlayView overlay, MotionEvent event) {
            ArrayList<Prediction> list = library.recognize(overlay.getGesture());
            float score = (float) list.get(0).score;
            if(Float.isNaN(score) || score < 2){
                // gesture do not match
                Toast.makeText(UnlockWithGesture.this, getString(R.string.gesture_invalid), Toast.LENGTH_SHORT).show();
            } else {
                // gestures match
                Toast.makeText(UnlockWithGesture.this, getString(R.string.unlocked), Toast.LENGTH_SHORT).show();
                PreferenceManager.getDefaultSharedPreferences(UnlockWithGesture.this).edit().putString(AppLockApplication.LASTAPP, mPackage).commit();
                PreferenceManager.getDefaultSharedPreferences(UnlockWithGesture.this).edit().putBoolean(AppLockApplication.UNLOCKED,true).commit();
                setResult(AppLockApplication.RESULT_OK);
                AppListData appListData = new AppListData(UnlockWithGesture.this);
                appListData.init();
                appListData.updateLastOpen(mPackage,(int) (new Date().getTime()/1000));
                LocalBroadcastManager.getInstance(UnlockWithGesture.this).sendBroadcast(new Intent(AppLockApplication.UPDATE_LIST));
                finish();
            }
        }

        @Override
        public void onGestureCancelled(GestureOverlayView overlay, MotionEvent event) {

        }
    }
}
