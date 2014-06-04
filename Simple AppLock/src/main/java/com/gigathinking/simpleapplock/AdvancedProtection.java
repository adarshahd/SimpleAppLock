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

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.Toast;


public class AdvancedProtection extends DeviceAdminReceiver{

    public AdvancedProtection(){
    }

    @Override
    public void onEnabled(final Context context, Intent intent) {
        new AsyncTask<Context,Void,Integer>(){
            @Override
            protected Integer doInBackground(Context... params) {
                try{
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(params[0]);
                if(preferences.getBoolean("adv_prot_purchased",false)){
                    preferences.edit().putBoolean("adv_prot",true).commit();
                    preferences.edit().putBoolean("admin_active",true).commit();
                    return 1;
                } else {
                    DevicePolicyManager mgr = (DevicePolicyManager) params[0].getSystemService(Context.DEVICE_POLICY_SERVICE);
                    mgr.removeActiveAdmin(new ComponentName(params[0],AdvancedProtection.class));
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Integer res) {
                if(res == null){
                    Toast.makeText(context, context.getString(R.string.adv_prot_not_purchased), Toast.LENGTH_LONG).show();
                }
            }
        }.execute(context);
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return context.getString(R.string.disable_adv_protection);
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("adv_prot",false).commit();
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("admin_active",false).commit();
    }
}
