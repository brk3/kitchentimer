/**
 *  Kitchen Timer
 *  Copyright (C) 2010 Roberto Leinardi
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.bourke.kitchentimer.ui;

/*
 * TODO:
 * Opzione per far suonare anche quando silenziato
 * Rendere modificabile il nome dei Timers
 */

import com.bourke.kitchentimer.R;
import com.bourke.kitchentimer.misc.Changelog;
import com.bourke.kitchentimer.misc.Constants;
import com.bourke.kitchentimer.misc.Eula;
import com.bourke.kitchentimer.utils.Utils;

import android.widget.NumberPicker;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

public class MainActivity extends Activity {
    public final static String TAG = "MainActivity";
    private final static int[] TIMERS = { 0, 1, 2 };

    private PowerManager.WakeLock mWakeLock = null;
    private AlarmManager mAlarmManager;
    private PendingIntent[] mPendingIntent;
    private Handler mHandler = new Handler();
    NotificationManager mNotificationManager;
    private SharedPreferences mPrefs;

    private MyRunnable[] countdownRunnable;

    private boolean[] timerIsRunning;
    private boolean[] timerIsIncrementing;
    private int[] timerSeconds;
    private long[] timerStartTime;

    private String[] timerDefaultName;

    private NumberPicker npHours;
    private NumberPicker npMinutes;
    private NumberPicker npSeconds;

    private Button btnTimer;
    private TextView[] tvTimer;
    private TextView[] tvTimerLabel;

    private String presetName;

    private static final int TIMER_0 = 0;
    private static final int TIMER_1 = 1;
    private static final int TIMER_2 = 2;
    private int mSelectedTimerView = TIMER_0;

    ColorStateList timerDefaultColor;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        //Eula.show(this);
        //Changelog.show(this);

        timerSeconds = new int[Constants.NUM_TIMERS];
        timerStartTime = new long[Constants.NUM_TIMERS];
        timerIsIncrementing = new boolean[Constants.NUM_TIMERS];
        timerIsRunning = new boolean[Constants.NUM_TIMERS];

        timerDefaultName = new String[Constants.NUM_TIMERS];
        timerDefaultName[0]=getString(R.string.timer1);
        timerDefaultName[1]=getString(R.string.timer2);
        timerDefaultName[2]=getString(R.string.timer3);

        mPendingIntent = new PendingIntent[Constants.NUM_TIMERS];

        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, TAG);
        mWakeLock.setReferenceCounted(false);

        countdownRunnable = new MyRunnable[Constants.NUM_TIMERS];

        for (int timer = 0; timer < Constants.NUM_TIMERS; timer++) {
            timerIsRunning[timer] = false;
            timerIsIncrementing[timer] = false;
            countdownRunnable[timer] = new MyRunnable(timer);
            Intent intent = new Intent(Constants.INTENT_TIMER_ENDED);
            intent.putExtra(Constants.TIMER, timer);
            // intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            mPendingIntent[timer] = PendingIntent.getBroadcast(this, timer,
                    intent, 0);
        }

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        acquireWakeLock();

        initWidgets();

        if (mPrefs.getBoolean(getString(R.string.pref_show_tips_key), true)){
            Toast toast = Toast.makeText(this, getString(R.string.tip1), Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP, 0, 0);
            toast.show();
        }
    }

    public static final NumberPicker.Formatter TWO_DIGIT_FORMATTER =
        new NumberPicker.Formatter() {

            final StringBuilder mBuilder = new StringBuilder();
            final java.util.Formatter mFmt = new java.util.Formatter(mBuilder);
            final Object[] mArgs = new Object[1];

            public String format(int value) {
                mArgs[0] = value;
                mBuilder.delete(0, mBuilder.length());
                mFmt.format("%02d", mArgs);
                return mFmt.toString();
            }
    };

    /** Get references to UI widgets and initialize them if needed */
    private void initWidgets() {
        npHours = (NumberPicker) findViewById(R.id.npHours);
        npMinutes = (NumberPicker) findViewById(R.id.npMinutes);
        npSeconds = (NumberPicker) findViewById(R.id.npSeconds);

        setNumberPickerKeyboard(npHours, false);
        setNumberPickerKeyboard(npMinutes, false);
        setNumberPickerKeyboard(npSeconds, false);

        npHours.setFormatter(TWO_DIGIT_FORMATTER);
        npMinutes.setFormatter(TWO_DIGIT_FORMATTER);
        npSeconds.setFormatter(TWO_DIGIT_FORMATTER);

        npHours.setMinValue(0);
        npHours.setMaxValue(23);

        npMinutes.setMinValue(0);
        npMinutes.setMaxValue(59);

        npSeconds.setMinValue(0);
        npSeconds.setMaxValue(59);

        npHours.setOnLongPressUpdateInterval(50);
        npMinutes.setOnLongPressUpdateInterval(50);
        npSeconds.setOnLongPressUpdateInterval(50);

        npHours.setValue(mPrefs.getInt(Constants.PREF_HOURS, 0));
        npMinutes.setValue(mPrefs.getInt(Constants.PREF_MINUTES, 0));
        npSeconds.setValue(mPrefs.getInt(Constants.PREF_SECONDS, 0));

        btnTimer = (Button)findViewById(R.id.btnTimer0);

        tvTimer = new TextView[Constants.NUM_TIMERS];
        tvTimer[0] = (TextView) this.findViewById(R.id.tvTimer0);
        tvTimer[1] = (TextView) this.findViewById(R.id.tvTimer1);
        tvTimer[2] = (TextView) this.findViewById(R.id.tvTimer2);

        tvTimerLabel = new TextView[Constants.NUM_TIMERS];
        tvTimerLabel[0] = (TextView) this.findViewById(R.id.tvTimer0_label);
        tvTimerLabel[1] = (TextView) this.findViewById(R.id.tvTimer1_label);
        tvTimerLabel[2] = (TextView) this.findViewById(R.id.tvTimer2_label);

        timerDefaultColor = tvTimer[0].getTextColors();
        tvTimer[0].setSelected(true);

        btnTimer.setOnClickListener(this.clickListener);
        for (int timer = 0; timer < Constants.NUM_TIMERS; timer++) {
            tvTimerLabel[timer].setText(mPrefs.getString(
                        Constants.PREF_TIMERS_NAMES[timer],
                        timerDefaultName[timer]));
            tvTimerLabel[timer].setOnClickListener(this.clickListener);
            tvTimer[timer].setOnClickListener(this.clickListener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putInt(Constants.PREF_HOURS, npHours.getValue());
        editor.putInt(Constants.PREF_MINUTES, npMinutes.getValue());
        editor.putInt(Constants.PREF_SECONDS, npSeconds.getValue());
        editor.commit();
        for (int timer = 0; timer < Constants.NUM_TIMERS; timer++) {
            if (timerIsRunning[timer])
                mHandler.removeCallbacks(countdownRunnable[timer]);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        for (int timer = 0; timer < Constants.NUM_TIMERS; timer++) {
            timerSeconds[timer] = mPrefs.getInt(Constants.PREF_TIMERS_SECONDS[timer], 0);
            timerStartTime[timer] = mPrefs.getLong(Constants.PREF_START_TIMES[timer], 0L);
            timerIsIncrementing[timer] = mPrefs.getBoolean(Constants.PREF_TIMER_INCREMENTING[timer], false);
            timerIsRunning[timer] = (timerStartTime[timer] != 0L);
            startRunnable(timerIsRunning[timer], timer);
            tvTimerLabel[timer].setText(mPrefs.getString(Constants.PREF_TIMERS_NAMES[timer], timerDefaultName[timer]));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        acquireWakeLock();
    }

    @Override
    public void onPause() {
        super.onPause();
        releaseWakeLock();
    }

    private void acquireWakeLock() {
        // It's okay to double-acquire this because we are not using it
        // in reference-counted mode.
        if (mPrefs.getBoolean(getString(R.string.pref_keep_screen_on_key), false))
            mWakeLock.acquire();
    }

    private void releaseWakeLock() {
        // Don't release the wake lock if it hasn't been created and acquired.
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_PRESETS) {
            if (resultCode == RESULT_OK) {
                Bundle extras = data.getExtras();
                npHours.setValue(extras.getInt("hours"));
                npMinutes.setValue(extras.getInt("minutes"));
                npSeconds.setValue(extras.getInt("seconds"));
                presetName=extras.getString("name");
            }
        }
    }

    private OnClickListener clickListener = new OnClickListener() {
        public void onClick(View v) {
            switch (v.getId()) {
            case R.id.tvTimer0_label:
                setTimerName(TIMERS[0]);
                break;
            case R.id.tvTimer1_label:
                setTimerName(TIMERS[1]);
                break;
            case R.id.tvTimer2_label:
                setTimerName(TIMERS[2]);
                break;
            case R.id.tvTimer0:
                mSelectedTimerView = TIMER_0;
                updateTimerWidgets();
                cancelTimeIsOverNotification(TIMERS[0]);
                break;
            case R.id.tvTimer1:
                mSelectedTimerView = TIMER_1;
                updateTimerWidgets();
                cancelTimeIsOverNotification(TIMERS[1]);
                break;
            case R.id.tvTimer2:
                mSelectedTimerView = TIMER_2;
                updateTimerWidgets();
                cancelTimeIsOverNotification(TIMERS[2]);
                break;
            case R.id.btnTimer0:
                startTimer(TIMERS[mSelectedTimerView]);
                break;
            }
        }
    };

    private void updateTimerWidgets() {
        if (timerIsRunning[mSelectedTimerView]) {
            btnTimer.setText(R.string.stop);
        } else {
            btnTimer.setText(R.string.start);
        }

        for (int timer = 0; timer < Constants.NUM_TIMERS; timer++) {
            tvTimer[timer].setSelected(false);
        }
        tvTimer[mSelectedTimerView].setSelected(true);
    }

    private void setTimerName(final int timer) {
        AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);

        alt_bld.setTitle(R.string.edit_timer_name);
        alt_bld.setIcon(R.drawable.icon);

        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setPadding((int)Utils.dp2px(10, this), 0, (int)Utils.dp2px(10, this), 0);
        final EditText etTimerName = new EditText(this);
        etTimerName.setHint(R.string.timer_name);
        etTimerName.setMaxLines(1);
        etTimerName.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        etTimerName.setFilters(new InputFilter[] {
                new InputFilter.LengthFilter(50) });
        etTimerName.setText(mPrefs.getString(Constants.PREF_TIMERS_NAMES[timer], timerDefaultName[timer]));
        frameLayout.addView(etTimerName);
        alt_bld.setView(frameLayout);

        alt_bld.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String timerName = (etTimerName.getText().toString());
                if(timerName.length()>0){
                    tvTimerLabel[timer].setText(timerName);
                    SharedPreferences.Editor editor = mPrefs.edit();
                    editor.putString(Constants.PREF_TIMERS_NAMES[timer], timerName);
                    editor.commit();
                }
            }
        });

        alt_bld.setNeutralButton(R.string.default_text, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                tvTimerLabel[timer].setText(timerDefaultName[timer]);
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putString(Constants.PREF_TIMERS_NAMES[timer], timerDefaultName[timer]);
                editor.commit();
            }
        });

        alt_bld.setNegativeButton(R.string.cancel, null);

        AlertDialog alert = alt_bld.create();
        alert.show();
    }

    private void startTimer(int timer) {
        if (mNotificationManager != null) {
            mNotificationManager.cancel(timer + 10);
        }
        mPendingIntent[timer].cancel();
        Intent intent = new Intent(Constants.INTENT_TIMER_ENDED);
        intent.putExtra(Constants.TIMER, timer);
        SharedPreferences.Editor editor = mPrefs.edit();
        if(presetName!=null){
            intent.putExtra(Constants.TIMER_NAME, presetName);
            tvTimerLabel[timer].setText(presetName);
            editor.putString(Constants.PREF_TIMERS_NAMES[timer], presetName);
        }else{
            intent.putExtra(Constants.TIMER_NAME, mPrefs.getString(Constants.PREF_TIMERS_NAMES[timer], timerDefaultName[timer]));
        }
        editor.commit();
        mPendingIntent[timer] = PendingIntent.getBroadcast(this, timer, intent, 0);

        btnTimer.requestFocusFromTouch();
        if (timerIsRunning[timer])
            setTimerState(false, timer);
        else {
            timerSeconds[timer] = npHours.getValue() * 3600
            + npMinutes.getValue() * 60 + npSeconds.getValue();
            if (timerSeconds[timer] > 0){
                timerIsIncrementing[timer] = false;
                setTimerState(true, timer);
                if (mPrefs.getBoolean(getString(R.string.pref_show_tips_key), true)){
                    Toast toast = Toast.makeText(this, getString(R.string.tip1), Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.TOP, 0, 0);
                    toast.show();
                }
            }else{
                timerIsIncrementing[timer] = true;
                setTimerState(true, timer);
                Toast.makeText(this, getString(R.string.timer_incrementing), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void cancelTimeIsOverNotification(int timer) {
        if (!timerIsRunning[timer]) {
            tvTimer[timer].setTextColor(timerDefaultColor);
            tvTimer[timer].setShadowLayer(0f, 0f, 0f, 0);
            if (mNotificationManager != null) {
                mNotificationManager.cancel(timer + 10);
            }
        }
    }

    /**
     *
     * @author jd
     *
     */
    class MyRunnable implements Runnable {
        private int timer;

        public MyRunnable(int timer) {
            this.timer = timer;
        }

        @Override
        public void run() {
            if (timerIsIncrementing[timer]) {
                long elapsedSeconds = (SystemClock.elapsedRealtime() - timerStartTime[timer]) / 1000;
                tvTimer[timer].setText(Utils.formatTime(Math.max(elapsedSeconds, 0L), timer));
            } else {
                long remainingSeconds = timerSeconds[timer] - (SystemClock.elapsedRealtime() - timerStartTime[timer]) / 1000;
                tvTimer[timer].setText(Utils.formatTime(Math.max(remainingSeconds, 0L), timer));
                if (remainingSeconds < 0) {
                    setTimerState(false, timer);
                    if (mPrefs.getBoolean(getString(R.string.pref_clear_timer_label_key), false)){
                        tvTimerLabel[timer].setText(timerDefaultName[timer]);
                    }
                    tvTimer[timer].setTextColor(getResources().getColor(R.color.indian_red_1));
                    tvTimer[timer].setShadowLayer(Utils.dp2px(7, getApplicationContext()), 0f, 0f, getResources().getColor(R.color.indian_red_1));
                }
            }
            if (timerIsRunning[timer]) {
                mHandler.postDelayed(this, 1000);
            }
        }
    }

    /**
     * Sets the timer on or off
     *
     * @param timer
     */
    private void setTimerState(boolean state, int timer) {
        timerIsRunning[timer] = state;
        setAlarmState(state, timer);
        startRunnable(state, timer);
        sendTimerIsRunningNotification(state, timer);
    }

    /**
     * Sets the alarm on or off This makes use of the alarm system service
     *
     * @param timer
     */
    private void setAlarmState(boolean state, int timer) {
        // Log.d(TAG, String.format("SetAlarmOn(on=%b)", on));
        if (state) {
            timerStartTime[timer] = SystemClock.elapsedRealtime();
            if (!timerIsIncrementing[timer]) {
                mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock
                        .elapsedRealtime()
                        + timerSeconds[timer] * 1000, mPendingIntent[timer]);
            }
        } else {
            timerStartTime[timer] = 0L;
            mAlarmManager.cancel(mPendingIntent[timer]);
        }
        presetName=null;
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putInt(Constants.PREF_TIMERS_SECONDS[timer], timerSeconds[timer]);
        editor.putLong(Constants.PREF_START_TIMES[timer], timerStartTime[timer]);
        editor.putBoolean(Constants.PREF_TIMER_INCREMENTING[timer], timerIsIncrementing[timer]);
        editor.commit();
    }

    /**
     *
     * @param start
     * @param timer
     */
    private void startRunnable(boolean start, int timer) {
        // Log.d(TAG, String.format("SetCountDownVisible(visible=%b)",
        // visible));
        if (start) {
            tvTimer[timer].setTextColor(getResources().getColor(R.color.white));
            tvTimer[timer].setShadowLayer(Utils.dp2px(4, this), 0f, 0f, getResources().getColor(R.color.white));
            btnTimer.setText(R.string.stop);
            mHandler.removeCallbacks(countdownRunnable[timer]);
            countdownRunnable[timer].run();

        } else {
            tvTimer[timer].setTextColor(timerDefaultColor);
            tvTimer[timer].setShadowLayer(0f, 0f, 0f, 0);
            tvTimer[timer].setText(timer == 0 ? R.string.sixzeros
                    : R.string.fourzeros);
            btnTimer.setText(R.string.start);
            mHandler.removeCallbacks(countdownRunnable[timer]);
        }
    }

    /**
     *
     * @param running
     * @param timer
     */
    private void sendTimerIsRunningNotification(boolean running, int timer) {
        if (running) {
            int icon = R.drawable.stat_notify_alarm;
            CharSequence mTickerText = getString(R.string.timer_started);
            long when = System.currentTimeMillis();
            Notification notification = new Notification(icon, mTickerText,
                    when);

            Context context = getApplicationContext();
            CharSequence mContentTitle = getString(R.string.app_name);
            CharSequence mContentText = getString(R.string.click_to_open);

            Intent clickIntent = new Intent(this, MainActivity.class);
            clickIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                    clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            notification.setLatestEventInfo(context, mContentTitle,
                    mContentText, contentIntent);

            notification.ledARGB = 0x00000000;
            notification.ledOnMS = 0;
            notification.ledOffMS = 0;
            notification.flags |= Notification.FLAG_SHOW_LIGHTS;
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
            notification.flags |= Notification.FLAG_NO_CLEAR;

            mNotificationManager.notify(Constants.APP_NOTIF_ID, notification);
        } else {
            boolean thereAreTimersRunning = false;
            for (int i = 0; i < timerIsRunning.length; i++) {
                thereAreTimersRunning = thereAreTimersRunning || timerIsRunning[i];
            }
            if (!thereAreTimersRunning) {
                mNotificationManager.cancel(Constants.APP_NOTIF_ID);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        AlertDialog.Builder alt_bld;
        AlertDialog alert;
        switch (item.getItemId()) {
        case R.id.menu_info:
            intent = new Intent(this, InfoActivity.class);
            startActivity(intent);
            return true;
        case R.id.menu_donate:
            alt_bld = new AlertDialog.Builder(this);

            alt_bld.setTitle(R.string.pref_donate);
            alt_bld.setMessage(R.string.donate_message);

            alt_bld.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    Utils.donate(getApplicationContext());
                }
            });

            alt_bld.setNegativeButton(R.string.no, null);

            alert = alt_bld.create();
            alert.show();
            return true;
        case R.id.menu_presets:
            intent = new Intent(this, PresetsActivity.class);
            startActivityForResult(intent, Constants.REQUEST_PRESETS);
            return true;
        case R.id.menu_preferences:
            intent = new Intent(this, ConfigActivity.class);
            startActivity(intent);
            return true;
        case R.id.menu_exit:
            alt_bld = new AlertDialog.Builder(this);

            alt_bld.setTitle(R.string.exit_title);
            alt_bld.setIcon(R.drawable.ic_dialog_alert);
            alt_bld.setMessage(R.string.exit_message);

            alt_bld.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    boolean flag = false;
                    for (int i = 0; i < timerIsRunning.length; i++) {
                        if (timerIsRunning[i]) {
                            mAlarmManager.cancel(mPendingIntent[i]);
                            mHandler.removeCallbacks(countdownRunnable[i]);
                            flag = true;
                        }
                        SharedPreferences.Editor editor = mPrefs.edit();
                        editor.putInt(Constants.PREF_TIMERS_SECONDS[i], 0);
                        editor.putLong(Constants.PREF_START_TIMES[i], 0L);
                        editor.putBoolean(Constants.PREF_TIMER_INCREMENTING[i], false);
                        editor.commit();
                        timerSeconds[i] = 0;
                        timerStartTime[i] = 0L;
                        timerIsIncrementing[i] = false;
                        timerIsRunning[i] = false;
                    }
                    if (flag) {
                        mNotificationManager.cancel(Constants.APP_NOTIF_ID);
                    }
                    finish();
                }
            });

            alt_bld.setNeutralButton(R.string.cancel, null);

            alt_bld.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    finish();
                }
            });

            alert = alt_bld.create();
            alert.show();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setNumberPickerKeyboard(NumberPicker numPicker,
            boolean enable) {
        int childCount = numPicker.getChildCount();

        for (int i = 0; i < childCount; i++) {
            View childView = numPicker.getChildAt(i);

            if (childView instanceof EditText) {
                EditText et = (EditText) childView;
                et.setFocusable(enable);
                return;
            }
        }
    }

}
