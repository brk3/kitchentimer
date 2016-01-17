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

package com.bourke.kitchentimer.utils;

import java.lang.Thread;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.bourke.kitchentimer.R;
import com.bourke.kitchentimer.customtypes.Food;
import com.bourke.kitchentimer.customtypes.Food.FoodMetaData;
import com.bourke.kitchentimer.database.DbTool;
import com.bourke.kitchentimer.misc.Constants;
import com.bourke.kitchentimer.misc.Log;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.util.Base64;
import android.view.View;
import android.webkit.WebView;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

public class Utils {

	/**
	 * 
	 * @param totalSeconds
	 * @param timer
	 * @return
	 */
	public static String formatTime(long totalSeconds, int timer) {
		if (timer == 0) {
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
			sdf.setTimeZone(TimeZone.getTimeZone("GMT+0"));
			return sdf.format(new Date(totalSeconds * 1000));
		} else {

			String seconds = Integer.toString((int) (totalSeconds % 60));
			String minutes = Integer.toString((int) (totalSeconds / 60));
			if (seconds.length() < 2) {
				seconds = "0" + seconds;
			}
			if (minutes.length() < 2) {
				minutes = "0" + minutes;
			}
			return minutes + ":" + seconds;
		}
	}


	public static float dp2px(int dip, Context context){
		float scale = context.getResources().getDisplayMetrics().density;
		return dip * scale + 0.5f;
	}

	public static View dialogWebView(Context context, String fileName) {
		View view = View.inflate(context, R.layout.dialog_webview, null);
		//		TextView textView = (TextView) view.findViewById(R.id.message);
		//		textView.setMovementMethod(LinkMovementMethod.getInstance());
		//		CharSequence cs =  readTextFile(context, fileName);
		//
		//		SpannableString s = new SpannableString(Html.fromHtml(cs.toString()));
		//		Linkify.addLinks(s, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
		//		textView.setText(s);
		WebView web = (WebView) view.findViewById(R.id.wv_dialog);
		web.loadUrl("file:///android_asset/"+fileName);
		return view;
	}

	public static CharSequence readTextFile(Context context, String fileName) {
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(context.getAssets().open(fileName)));
			String line;
			StringBuilder buffer = new StringBuilder();
			while ((line = in.readLine()) != null) buffer.append(line).append('\n');
			return buffer;
		} catch (IOException e) {
			Log.e("readTextFile", "Error readind file " + fileName, e);
			return "";
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					// Ignore
				}
			}
		}
	}

	public static boolean exportCSV(Context context, Cursor cursor) {
		if( cursor.getCount()==0) return true;
		// An array to hold the rows that will be written to the CSV file.
		final int rowLenght = FoodMetaData.COLUMNS.length-1;
		String[] row = new String[rowLenght];
		System.arraycopy(FoodMetaData.COLUMNS, 1, row, 0, rowLenght);
		CSVWriter writer = null;
		boolean success = false;

		try {
			writer = new CSVWriter(new FileWriter(Constants.CSV_FILE));

			// Write descriptive headers.
			writer.writeNext(row);

			int nameIndex = cursor.getColumnIndex(FoodMetaData.NAME);
			int hoursIndex = cursor.getColumnIndex(FoodMetaData.HOURS);
			int minutesIndex = cursor.getColumnIndex(FoodMetaData.MINUTES);
			int secondsIndex = cursor.getColumnIndex(FoodMetaData.SECONDS);

			if(cursor.requery()){
				while (cursor.moveToNext()) {
					row[0] = cursor.getString(nameIndex);
					row[1] = cursor.getInt(hoursIndex)+"";
					row[2] = cursor.getInt(minutesIndex)+"";
					row[3] = cursor.getInt(secondsIndex)+"";
					// NOTE: writeNext() handles nulls in row[] gracefully.
					writer.writeNext(row);
				}
			}
			success = true;
		} catch (Exception ex) {
			Log.e("Utils", "exportCSV", ex);
		} finally {
			try {
				if (null != writer) writer.close();
			} catch (IOException ex) {

			}
		}
		return success;
	}

	public static boolean importCSV(Context context, DbTool dbTool) {
		CSVReader reader = null;
		Food food = new Food();
		try {
			reader = new CSVReader(new FileReader(Constants.CSV_FILE));
		} catch (FileNotFoundException ex) {
			//Log.e("Utils", "importCSV - File Not Found", ex);
			return false;
		}
		try {

			// Use the first line to determine the type of csv file.  Secrets will
			// output 5 columns, with the names as used in the exportSecrets()
			// function.  OI Safe 1.1.0 is also detected.
			reader.readNext();

			// Read all the rest of the lines as secrets.

			String[] row;
			while ((row = reader.readNext()) != null) {
				if(row.length==4){
					food.name = row[0];
					try{
						food.hours = Integer.parseInt(row[1]);
						food.minutes = Integer.parseInt(row[2]);
						food.seconds = Integer.parseInt(row[3]);
					}catch(NumberFormatException nFE) {
						food.hours = 0;
						food.minutes = 0;
						food.seconds = 0;
						Log.e("importCSV", "NumberFormat Error", nFE);
					}
					dbTool.open();
					dbTool.insert(food);
				}
			}
		} catch (Exception ex) {
			Log.e("Utils", "importCSV", ex);
		} finally {
			try {
				if (null != reader) reader.close();
			} catch (IOException ex) {

			}
		}
		return true;
	}

	public static boolean isSdPresent() {
		return Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
	}

	public static void donate(Context mContext) {
		Intent intent = new Intent( Intent.ACTION_VIEW,
				Uri.parse("market://search?q=Donation%20pub:%22Roberto%20Leinardi%22") );
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		mContext.startActivity(intent);
	}

	public static void notifyServer(final String serverUrl) {
		new Thread(new Runnable(){
			@Override
			public void run() {
				try {
					Log.d("Utils", "notifyServer:" + serverUrl);
					DefaultHttpClient httpclient = new DefaultHttpClient();
					HttpGet httpget = new HttpGet(serverUrl);

					URL url = new URL(serverUrl);
					String userInfo = url.getUserInfo();
					if( userInfo != null ) {
						httpget.addHeader("Authorization", "Basic " + Base64.encodeToString(userInfo.getBytes(), Base64.NO_WRAP));
					}

					HttpResponse response = httpclient.execute(httpget);
					response.getEntity().getContent().close();
					httpclient.getConnectionManager().shutdown();

					int status = response.getStatusLine().getStatusCode();
					if ( status < 200 || status > 299 ) {
						throw new Exception(response.getStatusLine().toString());
					}
				} catch (Exception ex) {
					Log.e("Utils", "Error notifying server: ", ex);
				}
			}
		}).start();
	}

}
