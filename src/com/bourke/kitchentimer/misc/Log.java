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

import android.util.Config;

public class Log
{
	private final static String LOGTAG = "kitchentimer";

	//static final boolean DEBUG = false;
	//public static final boolean LOGV = DEBUG ? Config.LOGD : Config.LOGV;
	//Replace all Log.d with if(Log.LOGV) Log.v() to prevent Strings to be builded and to save memory

	public static void v(String TAG, String logMe)
	{
		if(Config.LOGV) android.util.Log.v(LOGTAG, TAG + ": " + logMe);
	}

	public static void d(String TAG, String logMe)
	{
		if(Config.LOGD) android.util.Log.d(LOGTAG, TAG + ": " + logMe);
	}

	public static void e(String TAG, String logMe)
	{
		android.util.Log.e(LOGTAG, TAG + ": " + logMe);
	}

	public static void e(String TAG, String logMe, Throwable ex)
	{
		android.util.Log.e(LOGTAG, TAG + ": " + logMe, ex);
	}

	public static void i(String TAG, String logMe)
	{
		if(Config.LOGD) android.util.Log.i(LOGTAG, TAG + ": " + logMe);
	}
}