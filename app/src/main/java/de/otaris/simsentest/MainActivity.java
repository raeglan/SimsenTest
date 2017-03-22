package de.otaris.simsentest;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.otaris.simsentest.BroadcastReceivers.SmsBroadcastReceiver;
import de.otaris.simsentest.data.LogPersistence;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.btn_send_sms)
    Button sendSMSButton;

    @BindView(R.id.lv_logs)
    ListView logsListView;

    @BindView(R.id.tv_status)
    TextView statusTextView;

    @BindString(R.string.pref_json_logs_key)
    String logsPreferenceKey;

    private final static String SENT_FILTER = "SMS_SENT";
    private final static String DELIVERED_FILTER = "SMS_DELIVERED";
    private final static int PERMISSION_CALLBACK = 1;

    private SmsBroadcastReceiver mSmsReceiver;
    private ArrayAdapter<String> mLogsAdapter;
    private SharedPreferences.OnSharedPreferenceChangeListener mPreferenceChangeListener;

    private String phoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        //register SMS event receiver
        mSmsReceiver = new SmsBroadcastReceiver();
        IntentFilter smsIntentFilter = new IntentFilter();
        smsIntentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
        registerReceiver(mSmsReceiver, smsIntentFilter);

        // sets our list view
        mLogsAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1);
        logsListView.setAdapter(mLogsAdapter);
        displayLogs();

        // also makes that when the logs changes that we also display it.
        mPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (logsPreferenceKey.equals(key))
                    displayLogs();
            }
        };
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(mPreferenceChangeListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // unregister the receiver and the change listener
        unregisterReceiver(mSmsReceiver);
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(mPreferenceChangeListener);
    }

    /**
     * Sends the sms, this button is disabled while one sms is being sent.
     */
    @OnClick(R.id.btn_send_sms)
    void sendSMS() {
        int sendPermissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS);
        int receivePermissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECEIVE_SMS);
        int readPhoneStatePermissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_PHONE_STATE);

        if (sendPermissionCheck != PackageManager.PERMISSION_GRANTED ||
                receivePermissionCheck != PackageManager.PERMISSION_GRANTED ||
                readPhoneStatePermissionCheck != PackageManager.PERMISSION_GRANTED) {
            askForPermissions();
        } else {

            // gets the phone number
            if (phoneNumber == null)
                phoneNumber = getPhoneNumber();

            PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
                    new Intent(SENT_FILTER), 0);

            PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
                    new Intent(DELIVERED_FILTER), 0);

            //---when the SMS has been sent---
            registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context arg0, Intent arg1) {
                    switch (getResultCode()) {
                        case Activity.RESULT_OK:
                            statusTextView.setText(R.string.status_sent);
                            String sentLog = getString(R.string.log_sent, getReadableCurrentDate());
                            LogPersistence.appendStringToArrayPref(MainActivity.this,
                                    logsPreferenceKey, sentLog);
                            break;
                        case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        case SmsManager.RESULT_ERROR_NO_SERVICE:
                        case SmsManager.RESULT_ERROR_NULL_PDU:
                        case SmsManager.RESULT_ERROR_RADIO_OFF:
                            statusTextView.setText(R.string.status_failed);
                            sendSMSButton.setEnabled(true);
                            break;
                    }
                    unregisterReceiver(this);
                }
            }, new IntentFilter(SENT_FILTER));

            //---when the SMS has been delivered---
            registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context arg0, Intent arg1) {
                    switch (getResultCode()) {
                        case Activity.RESULT_OK:
                            statusTextView.setText(R.string.status_delivered);
                            sendSMSButton.setEnabled(true);
                            break;
                        case Activity.RESULT_CANCELED:
                            statusTextView.setText(R.string.status_failed);
                            sendSMSButton.setEnabled(true);
                            break;
                    }
                    unregisterReceiver(this);
                }
            }, new IntentFilter(DELIVERED_FILTER));

            // log the sending process
            String logStartSending = getString(R.string.log_start_sending, getReadableCurrentDate());
            LogPersistence.appendStringToArrayPref(this, logsPreferenceKey, logStartSending);

            // send message
            String message = getString(R.string.sms_body, getReadableCurrentDate());
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
            sendSMSButton.setEnabled(false);
        }
    }

    /**
     * Gets a string with the readable date.
     * @return the date, formatted as defined on the strings.xml
     */
    private String getReadableCurrentDate() {
        long currentTimeInMillis = System.currentTimeMillis();
        SimpleDateFormat dateFormat = new SimpleDateFormat(getString(R.string.date_format),
                Locale.getDefault());
        return dateFormat.format(new Date(currentTimeInMillis));
    }

    /**
     * Displays all the logs in our list view.
     */
    private void displayLogs() {
        List<String> logs = LogPersistence.
                getStringArrayPref(this, logsPreferenceKey);
        mLogsAdapter.clear();
        mLogsAdapter.addAll(logs);
    }

    /**
     * The current phone number, we assume the app has the read phone state permission.
     *
     * @return the current phone number, should do a vality check.
     */
    @SuppressLint("HardwareIds")
    private String getPhoneNumber() {
        TelephonyManager tMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        return tMgr.getLine1Number();
    }

    /**
     * Asks for sending and receiving sms permission.
     */
    private void askForPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.READ_PHONE_STATE
                },
                PERMISSION_CALLBACK);
    }
}
