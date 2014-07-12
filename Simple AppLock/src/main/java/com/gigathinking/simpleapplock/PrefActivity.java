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
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.MenuItem;

import com.haibison.android.lockpattern.LockPatternActivity;
import com.haibison.android.lockpattern.util.Settings;

import java.util.ArrayList;

public class PrefActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    SharedPreferences mPrefs;
    ProgressDialog mDialog;

    public PrefActivity() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null) {
            return;
        }
        addPreferencesFromResource(R.xml.prefs_main);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mPrefs.registerOnSharedPreferenceChangeListener(this);

        if(mPrefs.getBoolean("adv_prot_purchased",false)){
            findPreference("adv_prot").setEnabled(true);
        }
        mDialog = new ProgressDialog(this,ProgressDialog.STYLE_SPINNER);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setTitle("Preferences");
        findPreference("unlock_method").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if(mPrefs.getBoolean(AppLockApplication.LOCKSET, false)){
                    if(mPrefs.getString(AppLockApplication.LOCKTYPE,"").equals(AppLockApplication.LOCKTYPE_PIN)){
                        startActivityForResult(new Intent(PrefActivity.this,UnlockWithPIN.class).putExtra(AppLockApplication.UNLOCK_SELF,true).putExtra(AppLockApplication.CHANGELOCK,true),23);
                    }
                    if(mPrefs.getString(AppLockApplication.LOCKTYPE,"").equals(AppLockApplication.LOCKTYPE_GESTURE)){
                        startActivityForResult(new Intent(PrefActivity.this,UnlockWithGesture.class).putExtra(AppLockApplication.UNLOCK_SELF,true).putExtra(AppLockApplication.CHANGELOCK,true),23);
                    }
                    if(mPrefs.getString(AppLockApplication.LOCKTYPE,"").equals(AppLockApplication.LOCKTYPE_PATTERN)){
                        Intent intent = new Intent(LockPatternActivity.ACTION_COMPARE_PATTERN, null,PrefActivity.this, LockPatternActivity.class);
                        intent.putExtra(LockPatternActivity.EXTRA_PATTERN, Settings.Security.getPattern(PrefActivity.this));
                        intent.putExtra(AppLockApplication.UNLOCK_SELF,true);
                        intent.putExtra(AppLockApplication.CHANGELOCK,true);
                        startActivityForResult(intent, 23);
                    }
                } else {
                    showSelectorDialog();
                }
                return true;
            }
        });

        findPreference("upgrade").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(PrefActivity.this,Upgrade.class));
                return true;
            }
        });

        findPreference("restore_purchase").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                mDialog.setMessage(getString(R.string.restore_purchase));
                mDialog.show();
                new RestorePurchase().execute();
                return true;
            }
        });

        findPreference("about").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(PrefActivity.this,About.class));
                return true;
            }
        });

        findPreference("rate").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                mPrefs.edit().putString("rating","RATED").commit();
                Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(
                        "http://market.android.com/details?id=" + getPackageName()));
                startActivity(marketIntent);
                return true;
            }
        });

        String unlock_method = getString(R.string.unlock_with_pin);
        if(mPrefs.getString(AppLockApplication.LOCKTYPE,"").equals(AppLockApplication.LOCKTYPE_GESTURE)){
            unlock_method = getString(R.string.unlock_with_gesture);
        }
        if(mPrefs.getString(AppLockApplication.LOCKTYPE,"").equals(AppLockApplication.LOCKTYPE_PATTERN)){
            unlock_method = getString(R.string.unlock_with_pattern);
        }
        findPreference("unlock_method").setSummary(unlock_method);
        findPreference("timer_value").setSummary(mPrefs.getString("timer_value","10") + " seconds");
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals("timer_value")){
            findPreference("timer_value").setSummary(mPrefs.getString("timer_value","10") + " seconds");
        }

        if(key.equals("adv_prot")){
            if(mPrefs.getBoolean("adv_prot",false)){
                //Check for advanced protection purchase and enable it.
                startActivityForResult(
                        new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                                .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, new ComponentName(this, AdvancedProtection.class))
                                .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.advanced_protection)), 778
                );
            } else {
                //Disable advanced protection
                DevicePolicyManager mgr = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
                ComponentName admin = new ComponentName(this,AdvancedProtection.class);
                if(mgr.isAdminActive(admin)){
                    mgr.removeActiveAdmin(admin);
                }
                mPrefs.edit().putBoolean("admin_active",false).commit();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 23:
                if (resultCode != RESULT_CANCELED) {
                    if(mPrefs.getString(AppLockApplication.LOCKTYPE,"").equals(AppLockApplication.LOCKTYPE_PATTERN)){
                        if((resultCode == LockPatternActivity.RESULT_OK)){
                            showSelectorDialog();
                            break;
                        }
                    }
                    if((resultCode == AppLockApplication.RESULT_OK)){
                        showSelectorDialog();
                    }
                }
                break;
            case 24:
                if(resultCode == LockPatternActivity.RESULT_OK){
                    mPrefs.edit().putString(AppLockApplication.LOCKTYPE,AppLockApplication.LOCKTYPE_PATTERN).commit();
                    findPreference("unlock_method").setSummary(R.string.unlock_with_pattern);
                }
                break;
            case 25:
                findPreference("unlock_method").setSummary(R.string.unlock_with_pin);
                break;
            case 26:
                findPreference("unlock_method").setSummary(R.string.unlock_with_gesture);
                break;
            case 778:
                if(resultCode != RESULT_OK){
                    ((CheckBoxPreference)findPreference("adv_prot")).setChecked(false);
                } else  {
                    mPrefs.edit().putBoolean("admin_active",true).commit();
                }
                break;
            default:
                super.onActivityResult(requestCode,resultCode,data);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    private void showSelectorDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //builder.setMessage("Before you can add an app, please select an unlock method");
        builder.setSingleChoiceItems(R.array.unlock_options,Integer.parseInt(mPrefs.getString(AppLockApplication.LOCKTYPE,"0")),new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        //Unlock with PIN
                        startActivityForResult(new Intent(PrefActivity.this, SetupPINLock.class).putExtra(AppLockApplication.CHANGELOCK, true),25);
                        break;
                    case 1:
                        startActivityForResult(new Intent(PrefActivity.this, SetupGestureLock.class).putExtra(AppLockApplication.CHANGELOCK, true),26);
                        //Unlock with Gesture
                        break;
                    case 2:
                        //Unlock with Pattern
                        Settings.Security.setAutoSavePattern(PrefActivity.this,true);
                        Intent intent = new Intent(LockPatternActivity.ACTION_CREATE_PATTERN, null,PrefActivity.this, LockPatternActivity.class).putExtra(AppLockApplication.CHANGELOCK,true);
                        startActivityForResult(intent, 24);
                        break;
                    default:
                        //Unlock with PIN
                        startActivity(new Intent(PrefActivity.this, SetupPINLock.class).putExtra(AppLockApplication.CHANGELOCK,true));
                        break;
                }
                dialog.dismiss();
            }
        });
        builder.setTitle("Select unlock method");
        builder.setNegativeButton("Cancel",null);
        builder.create().show();
    }

    private class RestorePurchase extends AsyncTask<Void,Void,Void>{
        @Override
        protected Void doInBackground(Void... params) {
            try {
                Bundle ownedItems = MainActivity.getBillingService().getPurchases(3,getPackageName(),"inapp",null);
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
                        String signature = signatureList.get(i).toString();
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

        @Override
        protected void onPostExecute(Void aVoid) {
            mDialog.dismiss();
        }
    }
}
