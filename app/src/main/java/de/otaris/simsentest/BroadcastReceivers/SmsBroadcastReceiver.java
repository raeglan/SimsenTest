package de.otaris.simsentest.BroadcastReceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.otaris.simsentest.R;
import de.otaris.simsentest.data.LogPersistence;

/**
 * A simple Broadcast receiver for receiving SMS messages while the app is active.
 *
 * @author Rafael Miranda
 * @version 0.1
 * @since 22.03.2017
 */
public class SmsBroadcastReceiver extends BroadcastReceiver {
    private final String TAG = this.getClass().getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();

        String strMessage = "";

        if (extras != null) {
            Object[] smsExtras = (Object[]) extras.get("pdus");
            if (smsExtras != null) {
                for (Object smsExtra : smsExtras) {
                    //noinspection deprecation
                    SmsMessage smsMsg = SmsMessage.createFromPdu((byte[]) smsExtra);

                    String strMsgBody = smsMsg.getMessageBody();
                    String strMsgSrc = smsMsg.getOriginatingAddress();

                    strMessage += "SMS from " + strMsgSrc + " : " + strMsgBody;
                    Log.i(TAG, strMessage);

                    // logs the message
                    long currentTimeInMillis = System.currentTimeMillis();
                    SimpleDateFormat dateFormat = new SimpleDateFormat(
                            context.getString(R.string.date_format),
                            Locale.getDefault());
                    String readableDate = dateFormat.format(new Date(currentTimeInMillis));
                    String receiveLog = context.getString(R.string.log_received, readableDate);
                    LogPersistence.appendStringToArrayPref(context,
                            context.getString(R.string.pref_json_logs_key), receiveLog);
                }
            }
        }
    }
}
