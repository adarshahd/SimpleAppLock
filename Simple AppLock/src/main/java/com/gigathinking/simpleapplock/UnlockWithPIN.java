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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;


public class UnlockWithPIN extends Activity implements View.OnClickListener {

    public static final String MPASSWORD = "password";
    public static final String COUNT = "count";
    public static final String UNLOCKSELF = "unlock_self";
    public static final String CHANGE_LOCK = "change_lock";
    public static final String MPACKAGE = "m_package";

    private String mPackage;
    private static final int MAX = 3;
    private int count;
    private String mPassword;
    private boolean unlockSelf;
    private SharedPreferences mPrefs;
    private boolean changeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_unlockwithpin);
        if(savedInstanceState != null){
            mPassword = savedInstanceState.getString(MPASSWORD);
            count = savedInstanceState.getInt(COUNT);
            unlockSelf = savedInstanceState.getBoolean(UNLOCKSELF);
            changeLock = savedInstanceState.getBoolean(CHANGE_LOCK);
            mPackage = savedInstanceState.getString(MPACKAGE);
            ((TextView)findViewById(R.id.pass)).setText(mPassword);
            return;
        }
        Bundle bundle = getIntent().getExtras();
        mPackage = bundle.getString(Intent.EXTRA_TEXT);
        changeLock = false;
        unlockSelf = bundle.getBoolean(AppLockApplication.UNLOCK_SELF);
        changeLock = bundle.getBoolean(AppLockApplication.CHANGELOCK);
        count = 0;
        mPassword = "";
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(MPASSWORD,mPassword);
        outState.putInt(COUNT,count);
        outState.putBoolean(UNLOCKSELF,unlockSelf);
        outState.putBoolean(CHANGE_LOCK,changeLock);
        outState.putString(MPACKAGE,mPackage);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!unlockSelf) {
            try {
                ((ImageView)findViewById(R.id.iv_icon_unlock)).setImageDrawable(getPackageManager().getApplicationIcon(mPackage));
                ((TextView)findViewById(R.id.tv_app_unlock)).setText(getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(mPackage,0)));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            findViewById(R.id.id_ll_icon).setVisibility(View.GONE);
            ((TextView)findViewById(R.id.tv_unlock_message)).setText(getString(R.string.unlock_self_pin));
        }
        findViewById(R.id.zero).setOnClickListener(this);
        findViewById(R.id.one).setOnClickListener(this);
        findViewById(R.id.two).setOnClickListener(this);
        findViewById(R.id.three).setOnClickListener(this);
        findViewById(R.id.four).setOnClickListener(this);
        findViewById(R.id.five).setOnClickListener(this);
        findViewById(R.id.six).setOnClickListener(this);
        findViewById(R.id.seven).setOnClickListener(this);
        findViewById(R.id.eight).setOnClickListener(this);
        findViewById(R.id.nine).setOnClickListener(this);
        findViewById(R.id.backspace).setOnClickListener(this);
        findViewById(R.id.id_btn_forgot_pin).setOnClickListener(this);
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.zero:
                count++;
                formPassword(0);
                if(count == 4){
                    validate();
                }
                break;
            case R.id.one:
                count++;
                formPassword(1);
                if(count == 4){
                    validate();
                }
                break;
            case R.id.two:
                count++;
                formPassword(2);
                if(count == 4){
                    validate();
                }
                break;
            case R.id.three:
                count++;
                formPassword(3);
                if(count == 4){
                    validate();
                }
                break;
            case R.id.four:
                count++;
                formPassword(4);
                if(count == 4){
                    validate();
                }
                break;
            case R.id.five:
                count++;
                formPassword(5);
                if(count == 4){
                    validate();
                }
                break;
            case R.id.six:
                count++;
                formPassword(6);
                if(count == 4){
                    validate();
                }
                break;
            case R.id.seven:
                count++;
                formPassword(7);
                if(count == 4){
                    validate();
                }
                break;
            case R.id.eight:
                count++;
                formPassword(8);
                if(count == 4){
                    validate();
                }
                break;
            case R.id.nine:
                count++;
                formPassword(9);
                if(count == 4){
                    validate();
                }
                break;
            case R.id.backspace:
                if(count <1){
                    break;
                }
                if(count == 1){
                    mPassword = "";
                    ((TextView)findViewById(R.id.pass)).setText(mPassword);
                    count = 0;
                    break;
                }
                char [] pass = mPassword.toCharArray();
                mPassword = "";
                for(int i=0;i<(count -1);++i){
                    mPassword += pass[i];
                }
                count--;
                ((TextView)findViewById(R.id.pass)).setText(mPassword);
                break;
            case R.id.id_btn_forgot_pin:
                startActivity(new Intent(this,ResetUnlockMethod.class));
                finish();
                break;
            default:
                break;
        }
    }

    private void formPassword(int num){
        mPassword += num;
        ((TextView)findViewById(R.id.pass)).setText(mPassword);
    }

    private void validate() {
        if(mPrefs.getString(AppLockApplication.PASSWORD,"").equals(mPassword)){
            // finish this activity
            Toast.makeText(this,getString(R.string.unlocked),Toast.LENGTH_SHORT).show();
            mPrefs.edit().putBoolean(AppLockApplication.UNLOCKED,true).commit();
            setResult(AppLockApplication.RESULT_OK);
            AppListData appListData = new AppListData(this);
            appListData.init();
            appListData.updateLastOpen(mPackage,(int) (new Date().getTime()/1000));
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(AppLockApplication.UPDATE_LIST));
            appListData.close();
            finish();
            mPrefs.edit().putString(AppLockApplication.LASTAPP,mPackage).commit();
        } else {
            Toast.makeText(this, getString(R.string.pin_invalid), Toast.LENGTH_SHORT).show();
            mPassword = "";
            ((TextView)findViewById(R.id.pass)).setText(mPassword);
            count = 0;
        }
    }
}
