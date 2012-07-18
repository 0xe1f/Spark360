/*
 * MsPointConverter.java 
 * Copyright (C) 2010-2012 Akop Karapetyan
 *
 * This file is part of Spark 360, the online gaming service client.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 *
 */

package com.akop.bach.activity.xboxlive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.akop.bach.Preferences;
import com.akop.bach.R;

public class MsPointConverter extends Activity implements OnItemSelectedListener
{
	private static final String CURRENCY_PREF = "MsPoints.Currency";
	
	private static final int CUR_USD = 1;
	private static final int CUR_CAD = 2;
	private static final int CUR_GBP = 3;
	private static final int CUR_EUR = 4;
	private static final int CUR_NOK = 5;
	private static final int CUR_SEK = 6;
	private static final int CUR_AUD = 7;
	private static final int CUR_JPY = 8;
	
	private static final long[] MSPOINT_INCREMENTS = { 80, 160, 240, 400, 800, 1200, 1600, 2400 };  
	private static final HashMap<Integer, Long> CONVERSION_RATIOS = 
		new HashMap<Integer, Long>();
	private static final HashMap<Integer, String> CURRENCY_SYMBOLS = 
		new HashMap<Integer, String>();
	
	static
	{
		CONVERSION_RATIOS.put(CUR_USD, 125L);
		CONVERSION_RATIOS.put(CUR_CAD, 155L);
		CONVERSION_RATIOS.put(CUR_GBP, 85L);
		CONVERSION_RATIOS.put(CUR_EUR, 120L);
		CONVERSION_RATIOS.put(CUR_NOK, 820L);
		CONVERSION_RATIOS.put(CUR_SEK, 1200L);
		CONVERSION_RATIOS.put(CUR_AUD, 165L);
		CONVERSION_RATIOS.put(CUR_JPY, 14800L);
		
		CURRENCY_SYMBOLS.put(CUR_USD, "$%s");
		CURRENCY_SYMBOLS.put(CUR_CAD, "$%s");
		CURRENCY_SYMBOLS.put(CUR_GBP, "\u00a3%s");
		CURRENCY_SYMBOLS.put(CUR_EUR, "\u20ac%s");
		CURRENCY_SYMBOLS.put(CUR_NOK, "%s kr");
		CURRENCY_SYMBOLS.put(CUR_AUD, "$%s");
		CURRENCY_SYMBOLS.put(CUR_SEK, "%s kr");
		CURRENCY_SYMBOLS.put(CUR_JPY, "\u00a5%s");
	}
	
	private class SpinnerOption
	{
		public int value;
		public String label;
		
		public SpinnerOption(int value, String label)
		{
			this.value = value;
			this.label = label;
		}
		
		@Override
		public String toString()
		{
			return label;
		}
	}
	
	private class ConversionInfo
	{
		String from;
		String to;
		
		public ConversionInfo(long from, int currency)
		{
			float to = (float)(CONVERSION_RATIOS.get(currency) * from) / 10000.0F;
			
			this.from = from + "";
			this.to = String.format(CURRENCY_SYMBOLS.get(currency), String
					.format("%.2f", to));
		}
	}
	
	private Spinner mSpinner;
	private ListView mListView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.xbl_mspoint_converter);
		
		mSpinner = (Spinner)findViewById(R.id.mspoint_currency);
		mListView = (ListView)findViewById(R.id.mspoint_presets);
		
		SpinnerOption currencies[] = {
				new SpinnerOption(CUR_USD, getString(R.string.currency_usd)),
				new SpinnerOption(CUR_AUD, getString(R.string.currency_aud)),
				new SpinnerOption(CUR_CAD, getString(R.string.currency_cad)),
				new SpinnerOption(CUR_EUR, getString(R.string.currency_eur)),
				new SpinnerOption(CUR_JPY, getString(R.string.currency_jpy)),
				new SpinnerOption(CUR_NOK, getString(R.string.currency_nok)),
				new SpinnerOption(CUR_GBP, getString(R.string.currency_gbp)),
				new SpinnerOption(CUR_SEK, getString(R.string.currency_sek)),
		};
		
		ArrayAdapter<SpinnerOption> spinnerAdapter = new ArrayAdapter<SpinnerOption>(
				this, android.R.layout.simple_spinner_item, currencies);
		spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		mSpinner.setAdapter(spinnerAdapter);
		mSpinner.setOnItemSelectedListener(this);
		
		int selectedCurrency = 
			Preferences.get(this).getInt(CURRENCY_PREF, -1);
		
		if (selectedCurrency != -1)
			mSpinner.setSelection(selectedCurrency);
		
		loadConversionData(currencies[0].value);
	}
	
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		
		Preferences.get(this).set(CURRENCY_PREF,
				mSpinner.getSelectedItemPosition());
	}
	
	private void loadConversionData(int currency)
	{
		ArrayList<ConversionInfo> info = new ArrayList<ConversionInfo>();
		for (long increment : MSPOINT_INCREMENTS)
			info.add(new ConversionInfo(increment, currency));
		
		ListAdapter listViewAdapter = new MsPointAdapter(this, info);
		mListView.setAdapter(listViewAdapter);
	}
	
	public static void actionShow(Context context)
	{
		Intent intent = new Intent(context, MsPointConverter.class);
		context.startActivity(intent);
	}
	
	private class MsPointAdapter extends ArrayAdapter<ConversionInfo>
	{
		public MsPointAdapter(Context context, List<ConversionInfo> objects)
		{
			super(context, R.layout.xbl_mspoint_list_item, objects);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View row;
			if (convertView == null)
			{
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.xbl_mspoint_list_item, parent, false);
				
				TextView fromType = (TextView)row.findViewById(R.id.mspoint_curr_type_from);
				fromType.setText(R.string.points);
				TextView toType = (TextView)row.findViewById(R.id.mspoint_curr_type_to);
				toType.setText(((SpinnerOption)mSpinner.getSelectedItem()).label);
			}
			else
			{
				row = convertView;
			}
			
			TextView from = (TextView)row.findViewById(R.id.mspoint_curr_from);
			TextView to = (TextView)row.findViewById(R.id.mspoint_curr_to);
			
			from.setText(getItem(position).from);
			to.setText(getItem(position).to);
			
			return row;
		}
	}

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
			long arg3)
	{
		SpinnerOption selectedOption = (SpinnerOption)mSpinner.getSelectedItem();
		loadConversionData(selectedOption.value);
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0)
	{
	}
}
