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

package com.bourke.kitchentimer.receivers;

import java.util.ArrayList;

import com.bourke.kitchentimer.R;
import com.bourke.kitchentimer.misc.Constants;
import com.bourke.kitchentimer.ui.MainActivity;
import com.bourke.kitchentimer.utils.Utils;
//import com.bourke.kitchentimer.R.drawable;
//import com.bourke.kitchentimer.R.raw;
//import com.bourke.kitchentimer.R.string;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver {
	private PowerManager.WakeLock mWakeLock = null;
	private NotificationManager mNotificationManager;
	private SharedPreferences mPrefs;
	private Context mContext;
	private String timerName;

	@Override
	public void onReceive(Context context, Intent intent) {
		// Hold a wake lock for 5 seconds, enough to give any
		// services we start time to take their own wake locks.
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, MainActivity.TAG);
		mWakeLock.acquire(Constants.WAKELOCK_TIMEOUT);

		mContext = context;
		mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(context);

		Bundle b = intent.getExtras();
		int timer = b.getInt(Constants.TIMER);
		timerName = b.getString(Constants.TIMER_NAME);
		sendTimeIsOverNotification(timer);

		if (mPrefs.getBoolean(mContext.getString(R.string.pref_clear_timer_label_key), false)){
			SharedPreferences.Editor editor = mPrefs.edit();
			editor.putString(Constants.PREF_TIMERS_NAMES[timer], mContext.getString(R.string.timer) + " " + timer);
			editor.commit();
		}

		if (mPrefs.getBoolean(mContext.getString(R.string.pref_show_tips_key), true)){
			Toast toast = Toast.makeText(mContext, mContext.getString(R.string.tip1), Toast.LENGTH_LONG);
			toast.setGravity(Gravity.TOP, 0, 0);
			toast.show();
		}

		Intent mIntent = new Intent(mContext, MainActivity.class);
		mIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		// La lanciamo
		mContext.startActivity(mIntent);
	}

	/**
	 * 
	 * @param timer
	 * @param tickerText
	 * @param contentTitle
	 * @param contentText
	 */
	private void sendTimeIsOverNotification(int timer) {
		int icon;
		String ns = Context.NOTIFICATION_SERVICE;
		mNotificationManager = (NotificationManager) mContext.getSystemService(ns);

		icon = R.drawable.notify_alarm_default;
		CharSequence mTickerText = timerName + " - " + mContext.getResources().getString(R.string.app_name);
		long when = System.currentTimeMillis();
		Notification notification = new Notification(icon, mTickerText, when);
		notification.number = timer + 1;

		CharSequence mContentTitle = timerName;
		CharSequence mContentText = mContext.getResources().getString(R.string.countdown_ended);

		Intent clickIntent = new Intent(mContext, MainActivity.class);
		clickIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP
				| Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, clickIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		notification.setLatestEventInfo(mContext, mContentTitle, mContentText, contentIntent);

		String defaultNotification = "android.resource://com.bourke.kitchentimer/" + R.raw.mynotification;
		if (mPrefs.getBoolean(mContext.getString(R.string.pref_notification_sound_key), true)) {
			if (mPrefs.getBoolean(mContext.getString(R.string.pref_notification_custom_sound_key), false)) {
				String customNotification = mPrefs.getString(mContext
						.getString(R.string.pref_notification_ringtone_key), defaultNotification);
				if (!customNotification.equals(defaultNotification)) {
					notification.sound = Uri.parse(customNotification);
				}
			} else {
				notification.sound = Uri.parse(defaultNotification);
			}
		}
		if (mPrefs.getBoolean(mContext.getString(R.string.pref_notification_insistent_key), true))
			notification.flags |= Notification.FLAG_INSISTENT;
		if (mPrefs.getBoolean(mContext.getString(R.string.pref_notification_vibrate_key), true)) {
			String mVibratePattern = mPrefs.getString(mContext
					.getString(R.string.pref_notification_vibrate_pattern_key), "");
			if (!mVibratePattern.equals("")) {
				notification.vibrate = parseVibratePattern(mVibratePattern);
			} else {
				notification.defaults |= Notification.DEFAULT_VIBRATE;
			}
		}
		if (mPrefs.getBoolean(mContext.getString(R.string.pref_notification_led_key), true)) {
			notification.flags |= Notification.FLAG_SHOW_LIGHTS;
			notification.ledARGB = Color.parseColor(mPrefs.getString(mContext
					.getString(R.string.pref_notification_led_color_key), "red"));
			int mLedBlinkRate = Integer.parseInt(mPrefs.getString(mContext
					.getString(R.string.pref_notification_led_blink_rate_key), "2"));
			notification.ledOnMS = 500;
			notification.ledOffMS = mLedBlinkRate * 1000;
		}
		if (mPrefs.getBoolean(mContext.getString(R.string.pref_notification_server_key), false)) {
			String mServerUrl = mPrefs.getString(mContext
					.getString(R.string.pref_notification_server_url_key), "");
			if (!mServerUrl.equals("")) {
				Utils.notifyServer(String.format(mServerUrl, "alarm", 0, Integer.toString(timer), Uri.encode(timerName)));
			}
		}

		notification.flags |= Notification.FLAG_AUTO_CANCEL;

		mNotificationManager.notify(timer + 10, notification);
	}

	/**
	 * Parse the user provided custom vibrate pattern into a long[] Borrowed
	 * from SMSPopup
	 */
	public static long[] parseVibratePattern(String stringPattern) {
		ArrayList<Long> arrayListPattern = new ArrayList<Long>();
		Long l;
		String[] splitPattern = stringPattern.split(",");
		int VIBRATE_PATTERN_MAX_SECONDS = 60000;
		int VIBRATE_PATTERN_MAX_PATTERN = 100;

		for (int i = 0; i < splitPattern.length; i++) {
			try {
				l = Long.parseLong(splitPattern[i].trim());
			} catch (NumberFormatException e) {
				return null;
			}
			if (l > VIBRATE_PATTERN_MAX_SECONDS) {
				return null;
			}
			arrayListPattern.add(l);
		}

		int size = arrayListPattern.size();
		if (size > 0 && size < VIBRATE_PATTERN_MAX_PATTERN) {
			long[] pattern = new long[size];
			for (int i = 0; i < pattern.length; i++) {
				pattern[i] = arrayListPattern.get(i);
			}
			return pattern;
		}

		return null;
	}
}
