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

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;


public class Welcome extends FragmentActivity implements ViewPager.OnPageChangeListener {

    private ViewPager mViewPager;
    private SharedPreferences mPrefs;
    private static final int ws_count = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*new AsyncTask<Context,Void,Void>(){
            @Override
            protected Void doInBackground(Context... params) {
                Crashlytics.start(params[0]);
                return null;
            }
        }.execute(this);*/
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        if(mPrefs.getString("welcome_shown", null) != null){
            startActivity(new Intent(this,MainActivity.class));
            finish();
            return;
        }
        View view = getLayoutInflater().inflate(R.layout.layout_welcome_screen, null);
        setContentView(view);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            startImmersiveMode(view);
        }
        getActionBar().hide();
        mViewPager = (ViewPager) findViewById(R.id.pager_welcome);
        mViewPager.setAdapter(new WelcomePagerAdapter(getSupportFragmentManager()));
        mViewPager.setOnPageChangeListener(this);
        ((ProgressBar)findViewById(R.id.welcome_page_indicator)).setMax(ws_count+1);

    }

    @Override
    protected void onStart() {
        super.onStart();
        findViewById(R.id.welcome_button_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPrefs.edit().putString("welcome_shown","yes").commit();
                startActivity(new Intent(Welcome.this,MainActivity.class));
                finish();
            }
        });

        findViewById(R.id.welcome_button_continue).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toNext();
            }
        });
    }

    public void toNext()
    {
        if(mViewPager.getCurrentItem() == ws_count) {
            mPrefs.edit().putString("welcome_shown","yes").commit();
            startActivity(new Intent(Welcome.this,MainActivity.class));
            finish();
            return;
        }
        mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int i) {
        ((ProgressBar)findViewById(R.id.welcome_page_indicator)).setSecondaryProgress(i+1);
        ((ProgressBar)findViewById(R.id.welcome_page_indicator)).setProgress(i);
        if(i == ws_count) {
            findViewById(R.id.welcome_button_close).setVisibility(View.INVISIBLE);
            findViewById(R.id.welcome_button_close).setLayoutParams(new LinearLayout.LayoutParams(0, 0));
            findViewById(R.id.welcome_button_bar_divider).setVisibility(View.INVISIBLE);
            ((Button)findViewById(R.id.welcome_button_continue)).setText(getString(R.string.get_started));
        } else {
            findViewById(R.id.welcome_button_close).setVisibility(View.VISIBLE);
            findViewById(R.id.welcome_button_close).setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT,5));
            findViewById(R.id.welcome_button_continue).setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 5));
            findViewById(R.id.welcome_button_bar_divider).setVisibility(View.VISIBLE);
            ((Button)findViewById(R.id.welcome_button_continue)).setText(getString(R.string.next));
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    private class WelcomePagerAdapter extends FragmentPagerAdapter {

        public WelcomePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0:
                    return new PageOne();
                case 1:
                    return new PageTwo();
                case 2:
                    return new PageThree();
                case 3:
                    return new PageFour();
            }
            return null;
        }

        @Override
        public int getCount() {
            return ws_count+1;
        }
    }

    public static class PageOne extends Fragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.layout_ws_first,container,false);
        }
    }

    public static class PageTwo extends Fragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.layout_ws_second,container,false);
        }
    }

    public static class PageThree extends Fragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.layout_ws_three,container,false);
        }
    }

    public static class PageFour extends Fragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.layout_ws_four,container,false);
        }
    }

    @TargetApi(19)
    private void startImmersiveMode(View view){
        view.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }
}
