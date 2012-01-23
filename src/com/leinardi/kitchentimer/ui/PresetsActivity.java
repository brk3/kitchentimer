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
import com.leinardi.kitchentimer.customtypes.Food;
import com.leinardi.kitchentimer.customtypes.Food.FoodMetaData;
import com.leinardi.kitchentimer.database.DbTool;
import com.leinardi.kitchentimer.misc.Constants;
import com.leinardi.kitchentimer.utils.Utils;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.NumberPicker;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class PresetsActivity extends ListActivity {
    // Identificatore delle voci del Menu Contestuale
    private final static int DELETE_MENU_OPTION = 1;
    private final static int UPDATE_MENU_OPTION = 2;

    private DbTool dbTool;

    private Cursor cursor;
    private FoodCursorAdapter myAdapter;

    private NumberPicker npHours;
    private NumberPicker npMinutes;
    private NumberPicker npSeconds;

    private EditText etPresetName;
    private Button btnSave;

    private Food food;
    private boolean isEdit;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.presets);

        dbTool = new DbTool(getApplicationContext());

        dbTool.open();
        cursor = dbTool.getRecords();
        startManagingCursor(cursor);

        myAdapter=new FoodCursorAdapter(cursor);

        setListAdapter(myAdapter);

        registerForContextMenu(getListView());
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v,int position, long id) {
                Cursor tmpCursor = dbTool.query(id);
                startManagingCursor(tmpCursor);
                if (tmpCursor.moveToNext()) {
                    food.hours = tmpCursor.getInt(tmpCursor.getColumnIndex(FoodMetaData.HOURS));
                    food.minutes = tmpCursor.getInt(tmpCursor.getColumnIndex(FoodMetaData.MINUTES));
                    food.seconds = tmpCursor.getInt(tmpCursor.getColumnIndex(FoodMetaData.SECONDS));
                    food.name = tmpCursor.getString(tmpCursor.getColumnIndex(FoodMetaData.NAME));
                    //System.out.println(food.name + "=" + food.hours + ":" + food.minutes + ":" + food.seconds);
                    Bundle bundle = new Bundle();
                    bundle.putInt("hours", food.hours);
                    bundle.putInt("minutes", food.minutes);
                    bundle.putInt("seconds", food.seconds);
                    bundle.putString("name", food.name);
                    Intent intent = new Intent();
                    intent.putExtras(bundle);
                    setResult(RESULT_OK, intent);
                    finish();
                }
                
            }
        });
        isEdit=false;
        food = new Food();
        initWidgets();
    }

    /** Get references to UI widgets and initialize them if needed */
    private void initWidgets() {
        npHours = (NumberPicker)findViewById(R.id.npHours);
        npMinutes = (NumberPicker)findViewById(R.id.npMinutes);
        npSeconds= (NumberPicker)findViewById(R.id.npSeconds);

        npHours.setFormatter(MainActivity.TWO_DIGIT_FORMATTER);
        npMinutes.setFormatter(MainActivity.TWO_DIGIT_FORMATTER);
        npSeconds.setFormatter(MainActivity.TWO_DIGIT_FORMATTER);

        npHours.setMinValue(0);
        npHours.setMaxValue(23);

        npMinutes.setMinValue(0);
        npMinutes.setMaxValue(59);

        npSeconds.setMinValue(0);
        npSeconds.setMaxValue(59);

        btnSave = (Button)findViewById(R.id.btnSave);
        etPresetName = (EditText)findViewById(R.id.etPresetName);

        btnSave.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                btnSave.requestFocusFromTouch();
                food.name = etPresetName.getText().toString();
                food.hours = npHours.getValue();
                food.minutes = npMinutes.getValue();
                food.seconds = npSeconds.getValue();

                boolean validato=true;
                StringBuffer validationFailMessage =  new StringBuffer();
                if(!food.isTimeSet()){
                    validationFailMessage.append(getString(R.string.error_time));
                    validato=false;
                }
                if(!food.isNameSet()){
                    if (validato==false) validationFailMessage.append("\n");
                    validationFailMessage.append(getString(R.string.error_name));
                    validato=false;
                }
                if(!validato){
                    Toast.makeText(getApplicationContext(), validationFailMessage.toString(), Toast.LENGTH_LONG).show();
                }else{
                    if(isEdit){
                        dbTool.open();
                        dbTool.update(food);
                        isEdit=false;
                        btnSave.setText(R.string.save);
                    }else{
                        dbTool.open();
                        dbTool.insert(food);
                    }
                    updateListView();
                    etPresetName.setText("");

                    // Hide Soft Keyboard
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(etPresetName.getWindowToken(), 0);
                }
            }

        });
    }

    class FoodCursorAdapter extends BaseAdapter {
        Cursor c;
        LayoutInflater mInflater;

        public FoodCursorAdapter(Cursor c) {
            this.c = c;         
            this.mInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return c.getCount();
        }

        @Override
        public Object getItem(int position) {
            // Auto-generated method stub
            return null;
        }

        @Override
        public long getItemId(int position) {
            c.moveToPosition(position);
            return c.getInt(c.getColumnIndex(Food.FoodMetaData.ID));
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.row_layout, null);
            }

            TextView tvName = (TextView) convertView.findViewById(R.id.name);
            TextView tvTime = (TextView) convertView.findViewById(R.id.time);

            if (c.moveToFirst()) {
                StringBuffer time =  new StringBuffer();
                c.moveToPosition(position);
                food.name = c.getString(c.getColumnIndex(Food.FoodMetaData.NAME));
                food.hours = c.getInt(c.getColumnIndex(Food.FoodMetaData.HOURS));
                food.minutes =c.getInt(c.getColumnIndex(Food.FoodMetaData.MINUTES));
                food.seconds =c.getInt(c.getColumnIndex(Food.FoodMetaData.SECONDS));

                String str;
                if(food.hours != 0){
                    str = food.hours+"";
                    if (str.length() < 2) str = "0" + food.hours;
                    time.append(str+":");
                }

                str = food.minutes+"";
                if (str.length() < 2) str = "0" + food.minutes;
                time.append(str+":");

                str = food.seconds+"";
                if (str.length() < 2) str = "0" + food.seconds;
                time.append(str);

                tvName.setText(food.name);
                tvTime.setText(time.toString());
            }
            return convertView;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbTool.close();
    }
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        int group = Menu.FIRST;
        menu.add(group, UPDATE_MENU_OPTION, Menu.FIRST, R.string.edit);
        menu.add(group, DELETE_MENU_OPTION, Menu.FIRST + 1, R.string.delete);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        long examId = info.id;

        switch (item.getItemId()) {
        case UPDATE_MENU_OPTION:
            Cursor tmpCursor = dbTool.query(examId);
            if (tmpCursor.moveToNext()) {
                food.id = examId;
                food.hours = tmpCursor.getInt(tmpCursor.getColumnIndex(FoodMetaData.HOURS));
                food.minutes = tmpCursor.getInt(tmpCursor.getColumnIndex(FoodMetaData.MINUTES));
                food.seconds = tmpCursor.getInt(tmpCursor.getColumnIndex(FoodMetaData.SECONDS));
                food.name = tmpCursor.getString(tmpCursor.getColumnIndex(FoodMetaData.NAME));

                npHours.setValue(food.hours);
                npMinutes.setValue(food.minutes);
                npSeconds.setValue(food.seconds);
                etPresetName.setText(food.name);
                isEdit=true;
                btnSave.setText(R.string.edit);
            }
            return true;
        case DELETE_MENU_OPTION:
            dbTool.open();
            dbTool.delete(examId);
            updateListView();
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }

    /*
     * Metodo di utilità che permettedi aggiornare il contenuto della ListView
     */
    private void updateListView() {
        cursor.requery();
        myAdapter.notifyDataSetChanged();
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
        inflater.inflate(R.menu.presets_menu, menu);
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_import:
            if(Utils.isSdPresent()){
                if(!Utils.importCSV(this, dbTool)){
                    Toast.makeText(getApplicationContext(), getString(R.string.unable_to_read_file) + " " + Constants.CSV_FILENAME, Toast.LENGTH_LONG)
                    .show();
                }
                updateListView();
            }else{
                Toast.makeText(getApplicationContext(), getString(R.string.sd_not_mounted), Toast.LENGTH_LONG)
                .show();
            }
            return true;
        case R.id.menu_export:
            if(cursor.getCount()>0){
                if(Utils.isSdPresent()){
                    if(Utils.exportCSV(this, cursor)){
                        Toast.makeText(getApplicationContext(), getString(R.string.presets_saved) + " " + Constants.CSV_FILENAME, Toast.LENGTH_LONG)
                        .show();
                    }else{
                        Toast.makeText(getApplicationContext(), getString(R.string.presets_export_error), Toast.LENGTH_LONG)
                        .show();
                    }
                }else{
                    Toast.makeText(getApplicationContext(), getString(R.string.sd_not_mounted), Toast.LENGTH_LONG)
                    .show();
                }
            }else{
                Toast.makeText(getApplicationContext(), getString(R.string.preset_list_empty), Toast.LENGTH_LONG)
                .show();
            }
            return true;
        case R.id.menu_truncate:
            new AlertDialog.Builder(this)
            .setIcon(R.drawable.ic_dialog_alert)
            .setTitle(R.string.delete_preset_title)
            .setMessage(R.string.delete_preset_message)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    dbTool.open();
                    dbTool.truncate();
                    updateListView();
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
