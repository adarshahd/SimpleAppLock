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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cocosw.undobar.UndoBarController;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.haibison.android.lockpattern.LockPatternActivity;
import com.haibison.android.lockpattern.util.Settings;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppListFragment extends Fragment implements AdapterView.OnItemClickListener, AbsListView.MultiChoiceModeListener, View.OnClickListener, UndoBarController.UndoListener {

    private AppListAdapter mAdapter;
    private static ArrayList<String> mList;
    private Map<String,Boolean> mAppMap;
    private SharedPreferences mPrefs;
    private BroadcastReceiver receiver;
    private boolean allItemsChecked;
    private View view;
    private boolean noAds;

    private InterstitialAd interstitial;



    public AppListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null){
            return;
        }
        setHasOptionsMenu(true);
        setRetainInstance(true);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        noAds = mPrefs.getBoolean("no_ads_purchased",false);
        allItemsChecked = false;
        loadAppList();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

    }

    @Override
    public void onStart() {
        super.onStart();

        if (!noAds) {
            final AdView adView = (AdView)getActivity().findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().addTestDevice("YOUR DEVICE 1").addTestDevice("YOUR DEVICE 2").build();
            adView.loadAd(adRequest);
            adView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    LinearLayout ll = (LinearLayout) getActivity().findViewById(R.id.id_ll_app_list);
                    ll.findViewById(R.id.adView).setVisibility(View.VISIBLE);
                }
            });
        } else {
            (getActivity().findViewById(R.id.adView)).setVisibility(View.GONE);
        }

        interstitial = new InterstitialAd(getActivity());
        interstitial.setAdUnitId("YOUR AD UNIT ID");
        interstitial.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                super.onAdClosed();
                mPrefs.edit().putInt("ad_count", (mPrefs.getInt("ad_count",0) + 1)).commit();
            }
        });

        // Create ad request.
        AdRequest adRequestInterestial = new AdRequest.Builder().addTestDevice("YOUR DEVICE 1").addTestDevice("YOUR DEVICE 2").build();

        // Begin loading your interstitial.
        if (!noAds) {
            interstitial.loadAd(adRequestInterestial);
        }


        if(!mList.isEmpty()){
            showInitialStart(false);
        } else {
            showInitialStart(true);
        }
        AbsListView listView = (AbsListView) getActivity().findViewById(R.id.lv_app_list);
        listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(this);
        mAdapter = new AppListAdapter(getActivity(),R.layout.layout_applist_item,mList);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(this);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(isAdded()){
                    startActivityForResult(new Intent(context,AppPicker.class),22);
                }
            }
        };
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, new IntentFilter(AppLockApplication.LAUNCH_PICKER));

        //Limit 5 interstitial ads per day.
        int today = Integer.parseInt(DateFormat.format("d", Calendar.getInstance()).toString());
        if(mPrefs.getInt("today",0) != today){
            mPrefs.edit().putInt("ad_count",0).commit();
            mPrefs.edit().putInt("today",today).commit();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_app_list, container, false);
        View btnAddApp = view.findViewById(R.id.btn_add_app);
        if (btnAddApp != null) {
            btnAddApp.setOnClickListener(this);
        }
        return view;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    }



    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main,menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()){
            case R.id.action_newapp:
                if(!mPrefs.getBoolean(AppLockApplication.LOCKSET,false)) {
                    showSelectorDialog();
                    return true;
                }
                startActivityForResult(new Intent(getActivity(), AppPicker.class), 22);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case 22:
                if(data == null){
                    return;
                }
                ArrayList<String> packageNames = data.getStringArrayListExtra(Intent.EXTRA_TEXT);
                if (packageNames != null) {
                    try {
                        for (String packageName : packageNames) {
                            String label = getActivity().getPackageManager().getApplicationLabel(getActivity().getPackageManager().getApplicationInfo(packageName,0)).toString();
                            mList.add(packageName);
                            mAppMap.put(packageName, true);
                            mAdapter.notifyDataSetChanged();
                            MainActivity.getAppListData().insertApp(label,packageName,true,0);
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent(AppLockApplication.UPDATE_LIST));
                    String message = getActivity().getString(R.string.lock_message_selected_apps);
                    Toast.makeText(getActivity(),message,Toast.LENGTH_SHORT).show();
                }
                if(mList.size() != 0){
                    showInitialStart(false);
                }
                if (mPrefs.getInt("ad_count",0) < 5) {
                    new Handler().postDelayed(new Runnable(){
                        @Override
                        public void run() {
                            if (interstitial.isLoaded()) {
                                interstitial.show();
                            }
                        }
                    },700);
                }
                break;
            case 24: {
                if (resultCode == LockPatternActivity.RESULT_OK) {
                    mPrefs.edit().putBoolean(AppLockApplication.LOCKSET,true).commit();
                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent(AppLockApplication.LAUNCH_PICKER));
                }
                break;
            }
            default:
                break;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        getActivity().getMenuInflater().inflate(R.menu.menu_app_list,menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        if(menu.size() == 0){
            return false;
        }
        String menu_selectt = allItemsChecked ? getString(R.string.select_none) : getString(R.string.select_all);
        menu.findItem(R.id.select_all).setTitle(menu_selectt);
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        AbsListView listView = ((AbsListView)getActivity().findViewById(R.id.lv_app_list));
        int count = listView.getCount();
        switch (item.getItemId()){
            case R.id.delete_app:
                ArrayList<String> positions = new ArrayList<String>();
                for (int i=0;i<count;++i){
                    if(listView.isItemChecked(i)){
                        positions.add(mList.get(i));
                    }
                }
                deleteApp(positions);
                mode.finish();
                break;
            case R.id.select_all:
                if (!allItemsChecked) {
                    for (int i=0;i<count;++i) {
                        listView.setItemChecked(i, true);
                    }
                    allItemsChecked = true;
                } else {
                    for (int i=0;i<count;++i) {
                        listView.setItemChecked(i, false);
                    }
                    allItemsChecked = false;
                }
                break;
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_add_app:
                if(mPrefs.getBoolean(AppLockApplication.LOCKSET,false)){
                    startActivityForResult(new Intent(getActivity(),AppPicker.class),22);
                } else {
                    showSelectorDialog();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onUndo(Parcelable token) {
        mList.clear();
        ArrayList<String> list = ((Bundle)token).getStringArrayList(AppLockApplication.APPLIST);
        for(String app : list){
            mList.add(app);
            String label = "";
            try {
                label = getActivity().getPackageManager().getApplicationLabel(getActivity().getPackageManager().getApplicationInfo(app,0)).toString();
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            MainActivity.getAppListData().insertApp(label,app,true,0);
        }
        mAdapter.notifyDataSetChanged();
        if(mList.size() == 0){
            showInitialStart(true);
        } else {
            showInitialStart(false);
        }
    }

    /*@Override
    public void onStop() {
        super.onStop();
        try {
            for (String aMList : mList) {
                String label = getActivity().getPackageManager().getApplicationLabel(getActivity().getPackageManager().getApplicationInfo(aMList, 0)).toString();
                MainActivity.getAppListData().insertApp(label,aMList,mAppMap.get(aMList),0);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }*/

    private class AppListAdapter extends ArrayAdapter{

        public AppListAdapter(Context context, int resource, List objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (mList.size() == 0) {
                return null;
            }
            if(convertView == null){
                convertView = getLayoutInflater(null).inflate(R.layout.layout_applist_item,parent,false);
            }
            try {
                final String name = getActivity().getPackageManager().getApplicationLabel(getActivity().getPackageManager().getApplicationInfo(mList.get(position),0)).toString();
                ((TextView)convertView.findViewById(R.id.tv_app_name)).setText(name);
                ((ImageView)convertView.findViewById(R.id.iv_app_icon)).setImageDrawable(getActivity().getPackageManager().getApplicationIcon(mList.get(position)));
                Drawable drawable = mAppMap.get(mList.get(position)) ? getResources().getDrawable(R.drawable.lock) : getResources().getDrawable(R.drawable.unlcok);
                ((ImageButton) convertView.findViewById(R.id.btn_lock)).setImageDrawable(drawable);
                ((ImageButton)convertView.findViewById(R.id.btn_delete)).setImageDrawable(getResources().getDrawable(R.drawable.ic_action_delete));
                final View finalConvertView = convertView;
                convertView.findViewById(R.id.btn_lock).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Boolean lock = !mAppMap.get(mList.get(position));
                        mAppMap.put(mList.get(position), lock);
                        Drawable drawable = mAppMap.get(mList.get(position)) ? getResources().getDrawable(R.drawable.lock) : getResources().getDrawable(R.drawable.unlcok);
                        ((ImageButton) finalConvertView.findViewById(R.id.btn_lock)).setImageDrawable(drawable);
                        MainActivity.getAppListData().updateLock(mList.get(position),lock);
                        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent(AppLockApplication.UPDATE_LIST));
                        String message = lock ? name + " " + getString(R.string.lock_message) : name + " " + getString(R.string.unlock_message);
                        Toast.makeText(getActivity(),message,Toast.LENGTH_SHORT).show();
                    }
                });
                convertView.findViewById(R.id.btn_delete).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deleteApp(position);
                        mAdapter.notifyDataSetChanged();
                    }
                });
            } catch (PackageManager.NameNotFoundException e) {
                /*
                 * The package may have been uninstalled, remove it from the mList and update the database too!
                 */
                Log.i("APP_LOCK","package '" + mList.get(position) + "' has been uninstalled. Removing it from database . . .");
                mAppMap.remove(mList.get(position));
                MainActivity.getAppListData().deleteApp(mList.get(position));
                mList.remove(mList.get(position));
                mAdapter.notifyDataSetChanged();
            }
            return convertView;
        }
    }

    private void loadAppList(){
        Cursor cursor = MainActivity.getAppListData().getAppListInfo();
        cursor.moveToFirst();
        if(mList == null){
            mList = new ArrayList<String>();
        } else {
            mList.clear();
        }
        mAppMap = new HashMap<String, Boolean>();
        int count = cursor.getCount();
        for (int i=0;i<count;++i){
            cursor.moveToPosition(i);
            mList.add(cursor.getString(2));
            mAppMap.put(cursor.getString(2),Boolean.valueOf(cursor.getString(3)));
        }
        cursor.close();
    }

    private void deleteApp(final int position){
        String message  = "";
        UndoBarController.clear(getActivity());
        Bundle bundle = new Bundle();
        bundle.putStringArrayList(AppLockApplication.APPLIST,new ArrayList<String>(mList));
        try {
            message = getActivity().getPackageManager().getApplicationLabel(getActivity().getPackageManager().getApplicationInfo(mList.get(position),0))  + getActivity().getString(R.string.deleted);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        ListViewAnimationHelper helper = new ListViewAnimationHelper(mAdapter,((AbsListView)getActivity().findViewById(R.id.lv_app_list)));
        helper.animateRemoval(((AbsListView)getActivity().findViewById(R.id.lv_app_list)),((AbsListView)getActivity().findViewById(R.id.lv_app_list)).getChildAt(position));
        MainActivity.getAppListData().deleteApp(mList.get(position));
        mList.remove(position);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent(AppLockApplication.UPDATE_LIST));
        if(mList.size() == 0){
            showInitialStart(true);
        }
        UndoBarController.show(getActivity(), message, this, bundle);
    }

    private void deleteApp(ArrayList<String> items){
        UndoBarController.clear(getActivity());
        Bundle bundle = new Bundle();
        bundle.putStringArrayList(AppLockApplication.APPLIST, new ArrayList<String>(mList));
        for (String item : items){
            ListViewAnimationHelper helper = new ListViewAnimationHelper(mAdapter,((AbsListView)getActivity().findViewById(R.id.lv_app_list)));
            helper.animateRemoval(((AbsListView)getActivity().findViewById(R.id.lv_app_list)),((AbsListView)getActivity().findViewById(R.id.lv_app_list)).getChildAt(mList.indexOf(item)));
            MainActivity.getAppListData().deleteApp(item);
            mList.remove(item);
        }
        mAdapter.notifyDataSetChanged();
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent(AppLockApplication.UPDATE_LIST));
        if(mList.size() == 0){
            showInitialStart(true);
        }
        String message = null;
        try {
            message = items.size() > 1 ? "" + items.size() + getActivity().getString(R.string.apps_deleted) : getActivity().getPackageManager().getApplicationLabel(getActivity().getPackageManager().getApplicationInfo(items.get(0),0))  + getActivity().getString(R.string.deleted);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        UndoBarController.show(getActivity(), message, this, bundle);
    }

    private void showInitialStart(boolean b){
        if(b) {
            String message = mPrefs.getBoolean(AppLockApplication.LOCKSET,false) ? getString(R.string.message_no_apps) : getString(R.string.message_initial_start);
            ((Button)view.findViewById(R.id.btn_add_app)).setText(message);
            view.findViewById(R.id.btn_add_app).setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            view.findViewById(R.id.btn_add_app).setPadding(10,10,10,10);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.findViewById(R.id.btn_add_app).getLayoutParams();
            params.setMargins(15,10,15,10);
            view.findViewById(R.id.btn_add_app).setLayoutParams(params);
            view.findViewById(R.id.lv_app_list).setVisibility(View.INVISIBLE);
        } else {
            view.findViewById(R.id.btn_add_app).setLayoutParams(new LinearLayout.LayoutParams(0,0));
            view.findViewById(R.id.lv_app_list).setVisibility(View.VISIBLE);
        }
    }

    private void showSelectorDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        //builder.setMessage("Before you can add an app, please select an unlock method");
        builder.setSingleChoiceItems(R.array.unlock_options,Integer.parseInt(mPrefs.getString(AppLockApplication.LOCKTYPE,"0")),new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        //Unlock with PIN
                        mPrefs.edit().putString(AppLockApplication.LOCKTYPE,AppLockApplication.LOCKTYPE_PIN).commit();
                        startActivity(new Intent(getActivity(), SetupPINLock.class));
                        break;
                    case 1:
                        mPrefs.edit().putString(AppLockApplication.LOCKTYPE,AppLockApplication.LOCKTYPE_GESTURE).commit();
                        startActivity(new Intent(getActivity(), SetupGestureLock.class));
                        //Unlock with Gesture
                        break;
                    case 2:
                        //Unlock with Pattern
                        mPrefs.edit().putString(AppLockApplication.LOCKTYPE,AppLockApplication.LOCKTYPE_PATTERN).commit();
                        Settings.Security.setAutoSavePattern(getActivity(),true);
                        Intent intent = new Intent(LockPatternActivity.ACTION_CREATE_PATTERN, null,getActivity(), LockPatternActivity.class);
                        startActivityForResult(intent, 24);
                        break;
                    default:
                        //Unlock with PIN
                        mPrefs.edit().putString(AppLockApplication.LOCKTYPE,AppLockApplication.LOCKTYPE_PIN).commit();
                        startActivity(new Intent(getActivity(), SetupPINLock.class));
                        break;
                }
                dialog.dismiss();
            }
        });
        builder.setTitle(getActivity().getString(R.string.select_unlock_method));
        builder.setNegativeButton(getActivity().getString(R.string.cancel),null);
        builder.create().show();
    }

    public static ArrayList<String> getAppList(){
        return mList;
    }
}
