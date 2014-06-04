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
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class Upgrade extends Activity implements View.OnClickListener {

    private ArrayList<String> mProducts;
    private Map<String,String> mPriceMap;
    private ProgressDialog mDialog;
    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_upgrade);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        mDialog = new ProgressDialog(this,ProgressDialog.STYLE_SPINNER);
        mDialog.setMessage(getString(R.string.processing));
        mDialog.show();
        findViewById(R.id.id_btn_upgrade).setOnClickListener(this);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        new GetSKUDetails().execute();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.id_btn_upgrade){
            int purchaseItem = ((RadioGroup)findViewById(R.id.id_rg_upgrade)).getCheckedRadioButtonId();
            String sku = "";
            if(purchaseItem == R.id.id_rb_no_ads){
                sku = "no_ads";
            }
            if(purchaseItem == R.id.id_rb_adv_prot){
                sku = "adv_prot";
            }
            new DoPurchase().execute(sku);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 777){
            if(resultCode == RESULT_OK){
                int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
                String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
                String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");
                if(responseCode == 0){
                    try{
                        JSONObject jo = new JSONObject(purchaseData);
                        String sku = jo.getString("productId");
                        if(sku.equals("no_ads")){
                            mPrefs.edit().putBoolean("no_ads_purchased",true).commit();
                        }
                        if(sku.equals("adv_prot")){
                            mPrefs.edit().putBoolean("adv_prot_purchased",true).commit();
                        }
                        Toast.makeText(this,getString(R.string.upgrage_on_next_restart),Toast.LENGTH_LONG).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private class GetSKUDetails extends AsyncTask<Void,Void,Integer> {

        @Override
        protected Integer doInBackground(Void... params) {
            ArrayList<String> skuList = new ArrayList<String>();
            skuList.add("no_ads");
            skuList.add("adv_prot");
            Bundle querySkus = new Bundle();
            querySkus.putStringArrayList("ITEM_ID_LIST", skuList);
            Bundle skuDetails;
            try {
                skuDetails = MainActivity.getBillingService().getSkuDetails(3, getPackageName(), "inapp", querySkus);
                int response = skuDetails.getInt("RESPONSE_CODE");
                if (response == 0) {
                    ArrayList<String> responseList = skuDetails.getStringArrayList("DETAILS_LIST");
                    if(responseList == null){
                        return null;
                    }
                    mProducts = new ArrayList<String>();
                    mPriceMap = new HashMap<String, String>();
                    for (String thisResponse : responseList) {
                        JSONObject object = new JSONObject(thisResponse);
                        String sku = object.getString("productId");
                        String price = object.getString("price");
                        mProducts.add(sku);
                        mPriceMap.put(sku,price);
                    }
                } else {
                    return null;
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (NullPointerException e){
                e.printStackTrace();
            }
            return 1;
        }

        @Override
        protected void onPostExecute(Integer res) {
            mDialog.dismiss();
            if(res == null){
                Toast.makeText(Upgrade.this,getString(R.string.get_details_failed),Toast.LENGTH_LONG).show();
                return;
            }
            if(mPrefs.getBoolean("no_ads_purchased",false)){
                findViewById(R.id.id_rb_no_ads).setEnabled(false);
            }
            if(mPrefs.getBoolean("adv_prot_purchased",false)){
                findViewById(R.id.id_rb_adv_prot).setEnabled(false);
            }
            ((RadioButton)findViewById(R.id.id_rb_no_ads)).setText(getString(R.string.no_ads) + mPriceMap.get("no_ads"));
            ((RadioButton)findViewById(R.id.id_rb_adv_prot)).setText(getString(R.string.adv_prot) + mPriceMap.get("adv_prot"));
        }
    }

    private class DoPurchase extends AsyncTask<String,Void,Integer>{
        Bundle buyIntentBundle;

        @Override
        protected Integer doInBackground(String... params) {
            try {
                buyIntentBundle = MainActivity.getBillingService().getBuyIntent(3, getPackageName(),params[0], "inapp",
                        PreferenceManager.getDefaultSharedPreferences(Upgrade.this).getString(RegisterDevice.PROPERTY_REG_ID,""));
            } catch (RemoteException e) {
                return null;
            } catch (NullPointerException e){
                return null;
            }

            return RESULT_OK;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            if(integer == null){
                Toast.makeText(Upgrade.this,getString(R.string.could_not_process),Toast.LENGTH_LONG).show();
                return;
            } else {
                PendingIntent buyIntent = buyIntentBundle.getParcelable("BUY_INTENT");
                try {
                    startIntentSenderForResult(buyIntent.getIntentSender(),777,new Intent(),0,0,0);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                } catch (NullPointerException e){
                    e.printStackTrace();
                }
            }
            super.onPostExecute(integer);
        }
    }
}
