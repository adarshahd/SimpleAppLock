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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class SetupPINLock extends Activity implements View.OnClickListener {

    private String mPassword;
    private String mPasswordConfirm;
    private int count;
    private boolean first;
    private boolean changeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        changeLock = getIntent().getBooleanExtra(AppLockApplication.CHANGELOCK, false);
        String message = changeLock ? getString(R.string.change_pin) : getString(R.string.create_pin);
        setContentView(R.layout.layout_unlockwithpin);
        findViewById(R.id.id_ll_icon).setVisibility(View.GONE);
        ((TextView)findViewById(R.id.tv_unlock_message)).setText(message);
        findViewById(R.id.id_btn_forgot_pin).setVisibility(View.GONE);
        count = 0;
        mPassword = "";
        mPasswordConfirm = "";
        first = true;
    }

    @Override
    protected void onStart() {
        super.onStart();
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
    }

    @Override
    public void onBackPressed() {
        setResult(AppLockApplication.RESULT_NOT_OK);
        super.onBackPressed();
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
                if (first) {
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
                } else {
                    if(count <1){
                        break;
                    }
                    if(count == 1){
                        mPasswordConfirm = "";
                        ((TextView)findViewById(R.id.pass)).setText(mPasswordConfirm);
                        count = 0;
                        break;
                    }
                    char [] pass = mPasswordConfirm.toCharArray();
                    mPasswordConfirm = "";
                    for(int i=0;i<(count -1);++i){
                        mPasswordConfirm += pass[i];
                    }
                    count--;
                    ((TextView)findViewById(R.id.pass)).setText(mPasswordConfirm);
                }
            default:
                break;
        }
    }

    private void formPassword(int num){
        if(first){
            mPassword += num;
            ((TextView)findViewById(R.id.pass)).setText(mPassword);
        } else {
            mPasswordConfirm += num;
            ((TextView)findViewById(R.id.pass)).setText(mPasswordConfirm);
        }
    }

    private void validate() {
        if(first){
            ((TextView)findViewById(R.id.pass)).setText(mPasswordConfirm);
            ((TextView)findViewById(R.id.tv_unlock_message)).setText(getString(R.string.pin_confirm));
            count = 0;
            first = false;
        } else {
            if(mPasswordConfirm.equals(mPassword)){
                PreferenceManager.getDefaultSharedPreferences(this).edit().putString(AppLockApplication.PASSWORD,mPassword).commit();
                Toast.makeText(this,getString(R.string.pin_set),Toast.LENGTH_SHORT).show();
                PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(AppLockApplication.LOCKSET,true).commit();
                setResult(AppLockApplication.RESULT_OK);
                finish();
                if (!changeLock) {
                    LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(AppLockApplication.LAUNCH_PICKER));
                } else {
                    PreferenceManager.getDefaultSharedPreferences(this).edit().putString(AppLockApplication.LOCKTYPE,AppLockApplication.LOCKTYPE_PIN).commit();
                }
            } else {
                Toast.makeText(this,getString(R.string.pin_do_not_match),Toast.LENGTH_SHORT).show();
                ((TextView)findViewById(R.id.tv_unlock_message)).setText(getString(R.string.pin_do_not_match_message));
                ((TextView)findViewById(R.id.pass)).setText("");
                count = 0;
                first = true;
            }
        }

    }
}
