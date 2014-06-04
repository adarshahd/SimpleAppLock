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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.gcm.GoogleCloudMessaging;

public class ResetUnlockReceiver extends BroadcastReceiver {
    public ResetUnlockReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
        String messageType = gcm.getMessageType(intent);
        if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
        } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
        } else {
            String action = intent.getExtras().getString("action");
            if(action.equals("reset")) {
                String unlockKey = intent.getExtras().getString("pin");
                Intent intentReset = new Intent(AppLockApplication.RESET_UNLOCK);
                intentReset.putExtra(AppLockApplication.PASSWORD,unlockKey);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intentReset);
            }
            if(action.equals("notification")) {
                //show the notification
                String msg = intent.getExtras().getString("msg");
            }
        }
    }
}
