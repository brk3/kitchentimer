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

package com.leinardi.kitchentimer.customtypes;

import java.io.Serializable;

public class Food implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1377135485835288101L;

	public static class FoodMetaData {
		public static final String ID = "_id";
		public static final String NAME = "name";
		public static final String HOURS = "hours";
		public static final String MINUTES = "minutes";
		public static final String SECONDS = "seconds";

		public static String[] COLUMNS = new String[] { ID, NAME, HOURS, MINUTES, SECONDS };
	}

	public long id;
	public String name;
	public int hours;
	public int minutes;
	public int seconds;

	public Food() {
		hours = 0;
		minutes = 0;
		seconds = 0;
	}

	public Food(String name, int hours, int minutes, int seconds) {
		this.name=name;
		this.hours=hours;
		this.minutes=minutes;
		this.seconds=seconds;
	}
	
	public boolean isTimeSet(){
		return hours != 0 || minutes != 0 || seconds != 0;
	}

	public boolean isNameSet() {
		return !name.equals("") && name != null;
	}
}
