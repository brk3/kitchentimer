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

package com.bourke.kitchentimer.database;

import com.bourke.kitchentimer.customtypes.Food;
import com.bourke.kitchentimer.customtypes.Food.FoodMetaData;
import com.bourke.kitchentimer.misc.Log;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbTool {
	SQLiteDatabase db;	
	Context context;
	DatabaseHelper dbHelper;
	static final String DB_NAME="DB_FOODS";
	static final String TABLE="FOOD";
	static final int DB_VERSION=2;

	public DbTool(Context context) {
		this.context=context;
		this.dbHelper=new DatabaseHelper(context);
	}

	public static class DatabaseHelper extends SQLiteOpenHelper{
		public DatabaseHelper(Context context){
			super(context, DB_NAME, null, DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE IF NOT EXISTS " +
					TABLE + " ("+ 
					FoodMetaData.ID +" INTEGER PRIMARY KEY AUTOINCREMENT," +
					FoodMetaData.NAME +" TEXT NOT NULL," +
					FoodMetaData.HOURS +" INTEGER NOT NULL," +
					FoodMetaData.MINUTES +" INTEGER NOT NULL," +
					FoodMetaData.SECONDS +" INTEGER NOT NULL)"
			);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.d("DbTool.onUpgrade", "old:"+oldVersion+" new:"+newVersion);
			if(db.getVersion()==1){
				db.execSQL("ALTER TABLE " + TABLE + " RENAME TO tmp_"+ TABLE);
				onCreate(db);
				db.execSQL("INSERT INTO " +
						TABLE + "("+ 
						FoodMetaData.NAME +", " +
						FoodMetaData.HOURS +", " +
						FoodMetaData.MINUTES +", " +
						FoodMetaData.SECONDS +") " +
						"SELECT " + 
						"nome, " +
						FoodMetaData.HOURS +", " +
						FoodMetaData.MINUTES +", " +
						FoodMetaData.SECONDS + " " +
						"FROM tmp_" + TABLE
				);
				db.execSQL("DROP TABLE IF EXISTS tmp_"+TABLE);
			}else{
				db.execSQL("DROP TABLE IF EXISTS "+TABLE);
				onCreate(db);
			}
		}
	}

	public void open(){
		db=dbHelper.getWritableDatabase();
	}
	public void close(){
		dbHelper.close();
	}

	public void insert(Food record){
		ContentValues values = new ContentValues();
		values.put(FoodMetaData.NAME, record.name);
		values.put(FoodMetaData.HOURS, record.hours);
		values.put(FoodMetaData.MINUTES, record.minutes);
		values.put(FoodMetaData.SECONDS, record.seconds);
		db.insert(TABLE, null, values);
	}

	public void update(Food record){
		ContentValues values = new ContentValues();
		values.put(FoodMetaData.NAME, record.name);
		values.put(FoodMetaData.HOURS, record.hours);
		values.put(FoodMetaData.MINUTES, record.minutes);
		values.put(FoodMetaData.SECONDS, record.seconds);
		db.update(TABLE, values, "_id="+record.id, null);
	}

	public void delete(long examId){
		db.delete(TABLE, "_id=" + examId, null);
	}

	public void truncate(){
		db.delete(TABLE, null, null);
	}

	public Cursor query(long examId){
		return db.query(TABLE, FoodMetaData.COLUMNS, "_id=" + examId, null, null, null,null);
	}

	public Cursor getRecords(){
		return db.rawQuery("SELECT * FROM "+ TABLE + " ORDER BY " + FoodMetaData.NAME, null);
	}
}

