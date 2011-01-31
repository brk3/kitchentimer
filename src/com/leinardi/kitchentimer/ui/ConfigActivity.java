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

package com.leinardi.kitchentimer.ui;

import com.leinardi.kitchentimer.R;
import com.leinardi.kitchentimer.misc.Log;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;

public class ConfigActivity extends PreferenceActivity {
	private final String REPORT_A_BUG_URL = "http://code.google.com/p/kitchentimer/issues/list";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource (R.xml.preferences);

		String version;
		try {
			PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
			version = pi.versionName;
		} catch (NameNotFoundException e) {
			Log.e(MainActivity.TAG, "Package name not found", e);
			version = getString(R.string.pref_info_version_error);
		}

		findPreference(getString(R.string.pref_info_version_key)).setSummary(version);
		Preference reportABug = findPreference(getString(R.string.pref_report_a_bug_key));
		reportABug.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(REPORT_A_BUG_URL));
				startActivity(browserIntent);
				return false;
			}
		});
	}	
}