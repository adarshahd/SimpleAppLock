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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class AppListData {

    private static final String mDatabaseName = "app_lock.db";
    private static final int DATABASE_VERSION = 1;
    private static final String mTableNameAppList = "app_list";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_APP_NAME = "_name";
    private static final String COLUMN_PACKAGE_NAME = "_package_name";
    private static final String COLUMN_LOCKED = "_locked";
    private static final String COLUMN_LAST_OPEN = "_last_open";

    private DatabaseHelper mDBHelper;
    private SQLiteDatabase mDatabase;

    private static final String CREATE_TABLE_APP_LIST = "create table "
            + mTableNameAppList + "(" + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_APP_NAME + " text not null, "
            + COLUMN_PACKAGE_NAME + " text not null, "
            + COLUMN_LOCKED + " text not null, "
            + COLUMN_LAST_OPEN + " integer);";

    private class DatabaseHelper extends SQLiteOpenHelper{

        public DatabaseHelper(Context context) {
            super(context, mDatabaseName, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE_APP_LIST);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }

    public AppListData(Context context){
        mDBHelper = new DatabaseHelper(context);
    }

    public void init(){
        mDatabase = mDBHelper.getWritableDatabase();
    }

    public void close(){
        mDBHelper.close();
    }

    public void insertApp(String name, String packageName, Boolean locked, int lastOpened){
        String lock = locked ? "true" : "false";
        String selection = COLUMN_PACKAGE_NAME + "=\"" + packageName + "\"";
        if(mDatabase.query(mTableNameAppList,null,selection,null,null,null,null).getCount() !=0){
            //Application already exists, just update the lock state
            updateLock(packageName,locked);
            return;
        }
        ContentValues values = new ContentValues();
        values.put(COLUMN_APP_NAME,name);
        values.put(COLUMN_PACKAGE_NAME,packageName);
        values.put(COLUMN_LOCKED,lock);
        values.put(COLUMN_LAST_OPEN,lastOpened);

        mDatabase.insert(mTableNameAppList, null, values);
    }

    public void deleteApp(String packageName){
        String whereClause = COLUMN_PACKAGE_NAME + "=\"" + packageName + "\"";
        mDatabase.delete(mTableNameAppList, whereClause, null);
    }

    public void updateLastOpen(String packageName, int lastOpened){
        String whereClause = COLUMN_PACKAGE_NAME + "=\"" + packageName + "\"";
        ContentValues values = new ContentValues();
        values.put(COLUMN_LAST_OPEN,lastOpened);
        mDatabase.update(mTableNameAppList,values,whereClause,null);
    }

    public void updateLock(String packageName, boolean locked){
        String whereClause = COLUMN_PACKAGE_NAME + "=\"" + packageName + "\"";
        String lock = locked ? "true" : "false";
        ContentValues values = new ContentValues();
        values.put(COLUMN_LOCKED,lock);
        mDatabase.update(mTableNameAppList,values,whereClause,null);
    }

    public Cursor getAppListInfo(){
        String order = COLUMN_APP_NAME + " ASC";
        if(!mDatabase.isOpen()){
            init();
        }
        return mDatabase.query(mTableNameAppList,null,null,null,null,null,order);
    }
}
