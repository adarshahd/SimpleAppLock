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
import android.gesture.Gesture;
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
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

public class SetupGestureLock extends Activity {

    private boolean first;
    private GestureLibrary library;
    private boolean changeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        changeLock = getIntent().getBooleanExtra(AppLockApplication.CHANGELOCK, false);
        String message = changeLock ? getString(R.string.change_gesture) : getString(R.string.create_gesture);
        setContentView(R.layout.layout_unlockwithgesture);
        File gestureFile = new File(Environment.getDataDirectory() + "/data/" + getPackageName(), AppLockApplication.GESTUREFILE);
        GestureOverlayView gestureOverlayView = (GestureOverlayView) findViewById(R.id.gestureOverlayView);
        gestureOverlayView.addOnGestureListener(new GestureProcessor());
        findViewById(R.id.id_ll_icon).setVisibility(View.GONE);
        findViewById(R.id.id_btn_forgot_gesture).setVisibility(View.GONE);
        ((TextView)findViewById(R.id.tv_unlock_message)).setText(message);
        first = true;
        library = GestureLibraries.fromFile(gestureFile);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onBackPressed() {
        setResult(AppLockApplication.RESULT_NOT_OK);
        super.onBackPressed();
    }

    private class GestureProcessor implements GestureOverlayView.OnGestureListener{

        @Override
        public void onGestureStarted(GestureOverlayView overlay, MotionEvent event) {
            findViewById(R.id.tv_gesture_desc).setVisibility(View.INVISIBLE);
        }

        @Override
        public void onGesture(GestureOverlayView overlay, MotionEvent event) {

        }

        @Override
        public void onGestureEnded(GestureOverlayView overlay, MotionEvent event) {
            if(first){
                Gesture gesture = overlay.getGesture();
                library.addGesture("first", gesture);
                first = false;
                ((TextView)findViewById(R.id.tv_unlock_message)).setText(getString(R.string.confirm_gesture));
            } else {
                Gesture gestureConfirm = overlay.getGesture();
                ArrayList<Prediction> list = library.recognize(gestureConfirm);
                if(list.get(0).score < 2) {
                    //gesture do not match
                    Toast.makeText(SetupGestureLock.this,getString(R.string.gesture_dont_match),Toast.LENGTH_SHORT).show();
                    ((TextView)findViewById(R.id.tv_unlock_message)).setText(getString(R.string.gesture_dont_match_message));
                    first = true;
                } else {
                    //gesture match
                    Toast.makeText(SetupGestureLock.this, getString(R.string.gesture_set), Toast.LENGTH_LONG).show();
                    library.save();
                    PreferenceManager.getDefaultSharedPreferences(SetupGestureLock.this).edit().putBoolean(AppLockApplication.LOCKSET,true).commit();
                    setResult(AppLockApplication.RESULT_OK);
                    finish();
                    if(!changeLock){
                        LocalBroadcastManager.getInstance(SetupGestureLock.this).sendBroadcast(new Intent(AppLockApplication.LAUNCH_PICKER));
                    } else {
                        PreferenceManager.getDefaultSharedPreferences(SetupGestureLock.this).edit().putString(AppLockApplication.LOCKTYPE,AppLockApplication.LOCKTYPE_GESTURE).commit();
                    }
                }
            }
        }

        @Override
        public void onGestureCancelled(GestureOverlayView overlay, MotionEvent event) {

        }
    }
}
