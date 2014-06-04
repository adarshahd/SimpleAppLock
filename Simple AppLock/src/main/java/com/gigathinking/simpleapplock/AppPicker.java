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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppPicker extends Activity {
	
	private ArrayList<String> mAppList;
    private List<ResolveInfo> mResolveList;
	private ArrayList<Drawable> mAppIcons;
	private static PackageManager mPackMan;
	private ProgressDialog mDialogProgress;
	private ArrayList<String> mPackageName;
    private ArrayList<String> mSelectedPackages;
    private ListView mListView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_app_picker);
		mDialogProgress = new ProgressDialog(this);
		mDialogProgress.setCancelable(true);
	}

	@Override
	protected void onStart() {
		super.onStart();
		new ListApps().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private class AppPickerAdapter extends ArrayAdapter<String> {

		public AppPickerAdapter(Context context, int textViewResourceId,
				List<String> objects) {
			super(context, textViewResourceId, objects);
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if(convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.layout_list_apps, parent, false);
                holder = new ViewHolder();
                holder.imgView = (ImageView) convertView.findViewById(R.id.id_iv_app_icon);
                holder.textView = (CheckedTextView) convertView.findViewById(R.id.id_tv_app_label);
                holder.textView.setFocusable(false);
                holder.isChecked = false;
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            (holder.imgView).setImageDrawable(mAppIcons.get(position));
            (holder.textView).setText(mAppList.get(position));
            (holder.textView).setChecked(mListView.isItemChecked(position));
			return convertView;
		}
	}

    private class ViewHolder{
        ImageView imgView;
        CheckedTextView textView;
        boolean isChecked;
    }
	
	private class ListApps extends AsyncTask<Void, Integer, String> {

		@Override
		protected String doInBackground(Void... params) {
            try {
                int count = mResolveList.size() - AppListFragment.getAppList().size();
                mAppList = new ArrayList<String>();
                mAppIcons = new ArrayList<Drawable>();
                mPackageName = new ArrayList<String>();
                Drawable[] icons = new Drawable[count];
                String[] packageName = new String[count];

                for (int i = 0; i < count && !isCancelled(); i++) {
                    String packName = mResolveList.get(i).activityInfo.packageName;
                    if (AppListFragment.getAppList().contains(packName)) {
                        continue;
                    }
                    mAppList.add(mPackMan.getApplicationLabel(mPackMan.getApplicationInfo(packName, 0)).toString() + " " + String.valueOf(i));
                    packageName[i] = packName;
                    icons[i] = mPackMan.getApplicationIcon(mPackMan.getApplicationInfo(packName, 0));
                    publishProgress(i + 1);
                }
                Collections.sort(mAppList);
                int j;
                count = mAppList.size();
                for (int i = 0; i < count; i++) {
                    j = Integer.parseInt(mAppList.get(i).substring(mAppList.get(i).lastIndexOf(" ") + 1));
                    mAppIcons.add(icons[j]);
                    mAppList.set(i, mAppList.get(i).substring(0, mAppList.get(i).lastIndexOf(" ")));
                    mPackageName.add(packageName[j]);

                }
                publishProgress(-1);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            return null;
		}

		@Override
		protected void onPreExecute() {
			mPackMan = getPackageManager();
            final Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            mResolveList = mPackMan.queryIntentActivities(intent,0);
			publishProgress(0);
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			if(values[0] == 0) {
				mDialogProgress.setMax(mResolveList.size() - AppListFragment.getAppList().size());
				mDialogProgress.setTitle(getString(R.string.retrieve_app_list));
				mDialogProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				mDialogProgress.show();
			} else if(values[0] == -1) {
                mDialogProgress.dismiss();
                mSelectedPackages = new ArrayList<String>();
                ArrayAdapter<String> adapter = new AppPickerAdapter(AppPicker.this, R.id.id_tv_app_label, mAppList);
                mListView = (ListView) findViewById(R.id.id_lv_app_picker);
                mListView.setAdapter(adapter);
                mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                mListView.setItemsCanFocus(false);
                mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                        mListView.setItemChecked(arg2,mListView.isItemChecked(arg2));
                        if(mListView.isItemChecked(arg2)){
                            mSelectedPackages.add(mPackageName.get(arg2));
                        } else {
                            mSelectedPackages.remove(mPackageName.get(arg2));
                        }
				/*Intent intentApp = new Intent(mAppList.get(arg2));
				intentApp.putExtra(Intent.EXTRA_TEXT, mPackageName.get(arg2));
				setResult(Activity.RESULT_OK, intentApp);
				finish();*/
                    }
                });
                findViewById(R.id.id_btn_select_apps).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent();
                        intent.putStringArrayListExtra(Intent.EXTRA_TEXT,mSelectedPackages);
                        setResult(RESULT_OK,intent);
                        finish();
                    }
                });
			} else {
				mDialogProgress.setProgress(values[0]);
			}
		}
	}
}
