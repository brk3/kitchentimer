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

package com.bourke.kitchentimer.misc;

import java.io.File;

import android.os.Environment;

public class Constants
{
	
	public static final String CSV_FILENAME = "KitchenTimer.csv";
	public static File SD_PATH = Environment.getExternalStorageDirectory();
	public static File CSV_FILE = new File(SD_PATH, CSV_FILENAME);
	
	public static final String INTENT_TIMER_ENDED = "kitchentimer.custom.intent.action.TIMER_ENDED";
	public static final int APP_NOTIF_ID = 99;
	public static final String TIMER = "timer";
	public static final String TIMER_NAME = "timer_name";
	public static final int REQUEST_PRESETS = 1;

	// Symbolic names for the keys used for preference lookup
	public static final String PREF_HOURS = "Hours";
	public static final String PREF_MINUTES = "Minutes";
	public static final String PREF_SECONDS = "Seconds";
	
	public static final String[] PREF_TIMERS_NAMES = { 
		"pref_timer_name_0",
		"pref_timer_name_1",
		"pref_timer_name_2" };
	public static final String[] PREF_TIMERS_SECONDS = { 
		"timer_seconds_0",
		"timer_seconds_1",
		"timer_seconds_2" };
	public static final String[] PREF_START_TIMES = { 
		"start_time_0",
		"start_time_1", 
		"start_time_2" };
	public static final String[] PREF_TIMER_INCREMENTING = { 
		"timer_incrementing_0",
		"timer_incrementing_1", 
		"timer_incrementing_2" };

	public static final int NUM_TIMERS = 3;
	
	public static final long WAKELOCK_TIMEOUT = 5000;
	
	public static final String PREF_CHANGELOG = "changelog";
	public static final String PREF_APP_VERSION = "app.version";
	
	public static final String PREF_EULA = "eula";
	public static final String PREF_EULA_ACCEPTED = "eula.accepted";
	
	public static final String APP_NAMESPACE = "http://schemas.android.com/apk/res/com.bourke.kitchentimer";
}