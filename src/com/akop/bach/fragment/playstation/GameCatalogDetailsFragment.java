/*
 * GameCatalogDetailsFragment.java 
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

package com.akop.bach.fragment.playstation;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.Calendar;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.akop.bach.App;
import com.akop.bach.IAccount;
import com.akop.bach.ImageCache;
import com.akop.bach.ImageCache.CachePolicy;
import com.akop.bach.ImageCache.OnImageReadyListener;
import com.akop.bach.PSN.GameCatalogItem;
import com.akop.bach.PSN.GameCatalogItemDetails;
import com.akop.bach.PsnAccount;
import com.akop.bach.R;
import com.akop.bach.TaskController;
import com.akop.bach.TaskController.CustomTask;
import com.akop.bach.TaskController.TaskListener;
import com.akop.bach.fragment.GenericFragment;
import com.akop.bach.parser.AuthenticationException;
import com.akop.bach.parser.Parser;
import com.akop.bach.parser.ParserException;
import com.akop.bach.parser.PsnParser;

public class GameCatalogDetailsFragment extends GenericFragment
{
	private CachePolicy mCp = null;
	private PsnAccount mAccount;
	private Handler mHandler = new Handler();
	private TextView mMessage;
	private WebView mDescription;
	private Gallery mGallery;
	private GameCatalogItem mItem;
	private GameCatalogItemDetails mDetails;
	
	private ImageAdapter mAdapter;
	private int mScaledScreenshotWidth;
	private int mScaledScreenshotHeight;
    private CachePolicy mScreenshotPolicy;
	
	private TaskListener mListener = new TaskListener()
	{
		@Override
		public void onTaskFailed(IAccount account, final Exception e)
		{
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					if (getActivity() != null && e != null)
						mMessage.setText(Parser.getErrorMessage(getActivity(), e));
				}
			});
		}
		
		@Override
		public void onTaskSucceeded(IAccount account, Object requestParam, final Object result)
		{
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					if (result != null && result instanceof GameCatalogItemDetails)
					{
						mDetails = (GameCatalogItemDetails)result;
						
						resetAdapter();
						synchronizeLocal();
					}
				}
			});
		}
	};
	
	public static GameCatalogDetailsFragment newInstance(PsnAccount account)
	{
		return newInstance(account, null);
	}
	
	public static GameCatalogDetailsFragment newInstance(PsnAccount account,
			GameCatalogItem item)
	{
		GameCatalogDetailsFragment f = new GameCatalogDetailsFragment();
		
		Bundle args = new Bundle();
		args.putSerializable("account", account);
		args.putSerializable("gameItem", item);
		f.setArguments(args);
		
		return f;
	}
	
	@Override
	public void onCreate(Bundle state)
	{
		super.onCreate(state);
		
	    Bundle args = getArguments();
		
	    mCp = new CachePolicy();
		mCp.resizeHeight = 96;
		
	    mAccount = (PsnAccount)args.getSerializable("account");
	    mItem = (GameCatalogItem)args.getSerializable("gameItem");
	    mDetails = null;
	    
	    if (state != null)
			mDetails = (GameCatalogItemDetails)state.getSerializable("details");
	    
	    setHasOptionsMenu(true);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		
		if (mDetails != null)
			outState.putSerializable("details", mDetails);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState)
	{
		if (container == null)
			return null;
		
		View layout = inflater.inflate(R.layout.psn_fragment_game_catalog_details,
				container, false);
		
		float scale = getResources().getDisplayMetrics().density;
		mScaledScreenshotWidth = (int)(scale * 178f + 0.5f);
		mScaledScreenshotHeight = (int)(scale * 100f + 0.5f);
		
		mScreenshotPolicy = new CachePolicy(mScaledScreenshotWidth,
				mScaledScreenshotHeight);
		
		mMessage = (TextView)layout.findViewById(R.id.unselected);
		mMessage.setText(R.string.select_a_game);
		
		mDescription = (WebView)layout.findViewById(R.id.game_description);
		mDescription.setBackgroundColor(0);
		WebSettings settings = mDescription.getSettings();
		
		//settings.setDefaultFontSize(12);
		settings.setDefaultTextEncodingName("utf-8");
		
		mGallery = (Gallery)layout.findViewById(R.id.catalog_gallery);
		mGallery.setOnItemClickListener(new OnItemClickListener() 
		{
			public void onItemClick(AdapterView<?> parent, View v, int position,
					long id)
			{
				if (mDetails == null || mDetails.ScreenshotsLarge == null)
					return;
				
				if (position >= mDetails.ScreenshotsLarge.length)
					return;
				
				Toast toast = Toast.makeText(getActivity(), 
						R.string.opening_screenshot, Toast.LENGTH_SHORT);
				toast.show();
				
				AsyncTask<String, Void, File> task = new AsyncTask<String, Void, File>()
				{
					@Override
					protected File doInBackground(String... params)
					{
						File file = null;
						
						try
						{
							file = ImageCache.get().downloadImage(params[0]);
						}
						catch(Exception e)
						{
						}
						
						return file;
					}
					
					protected void onPostExecute(File result)
					{
						if (result != null)
						{
							Intent intent = new Intent(Intent.ACTION_VIEW);
							intent.setDataAndType(Uri.fromFile(result), "image/*");
							startActivity(intent);
						}
					}
				};
				
				task.execute(mDetails.ScreenshotsLarge[position]);
			}
		});
		
		return layout;
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		
		ImageCache.get().removeListener(this);
		ImageCache.get().removeListener(mGalleryListener);
		
		TaskController.get().removeListener(mListener);
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		
		TaskController.get().addListener(mListener);
		ImageCache.get().addListener(this);
		ImageCache.get().addListener(mGalleryListener);
		
	    if (mDetails == null && mItem != null)
	    {
    		mDetails = GameCatalogItemDetails.fromItem(mItem);
    		synchronizeWithServer();
	    }
	    
	    if (mDetails != null && mAdapter == null)
	    	resetAdapter();
	    
		synchronizeLocal();
	}
	
	public void resetTitle(GameCatalogItem item)
	{
		mItem = item;
		
	    if (mItem != null)
	    {
    		mDetails = GameCatalogItemDetails.fromItem(mItem);
    		synchronizeWithServer();
	    }
	    else
	    {
	    	mDetails = null;
	    }
		
	    resetAdapter();
		synchronizeLocal();
	}
	
	private void synchronizeLocal()
	{
		View container = getView();
		if (container == null)
			return;
		
        if (android.os.Build.VERSION.SDK_INT >= 11)
        	new HoneyCombHelper().invalidateMenu();
        
		if (mDetails == null)
		{
			container.findViewById(R.id.unselected).setVisibility(View.VISIBLE);
			container.findViewById(R.id.catalog_details).setVisibility(View.GONE);
		}
		else
		{
			container.findViewById(R.id.unselected).setVisibility(View.GONE);
			container.findViewById(R.id.catalog_details).setVisibility(View.VISIBLE);
			
			ImageView iv;
			TextView tv;
			View item;
			ImageCache ic = ImageCache.get();
			
			if ((item = container.findViewById(R.id.catalog_item)) != null)
			{
				tv = (TextView)item.findViewById(R.id.cat_item_title);
				tv.setText(mItem.Title);
				tv = (TextView)item.findViewById(R.id.cat_item_genre);
				tv.setText(mItem.Genre);
				tv = (TextView)item.findViewById(R.id.cat_item_relDate);
				tv.setVisibility((mItem.ReleaseDate != null) ? View.VISIBLE : View.INVISIBLE);
				tv.setText(getString(R.string.release_date_f, 
						mItem.ReleaseDate));
				iv = (ImageView)item.findViewById(R.id.cat_item_boxart);
				
				String iconUrl = mItem.BoxartUrl;
				Bitmap bmp;
				
				if ((bmp = ic.getCachedBitmap(iconUrl, mCp)) != null)
				{
					iv.setImageBitmap(bmp);
				}
				else
				{
					if (iconUrl != null)
					{
						ic.requestImage(iconUrl, new OnImageReadyListener()
						{
							@Override
							public void onImageReady(long id, Object param,
									Bitmap bmp)
							{
								mHandler.post(new Runnable()
								{
									@Override
									public void run()
									{
										synchronizeLocal();											
									}
								});
							}
						}, 0, null, true, mCp);
					}
				}
			}
			
			String webContent = getString(R.string.web_template,
					mDetails.Description == null ? "" : mDetails.Description);
			
			mDescription.loadDataWithBaseURL(null, webContent, "text/html", "utf-8", null);
			
			if ((iv = (ImageView)container.findViewById(R.id.game_boxart)) != null)
			{
				String iconUrl = mDetails.BoxartUrl;
				Bitmap bmp;
				
				if ((bmp = ic.getCachedBitmap(iconUrl, sCp)) == null)
					ic.requestImage(iconUrl, this, 0, null, sCp);
				
				iv.setImageBitmap(bmp);
			}
			
			mGallery.setVisibility((mDetails.ScreenshotsThumb == null)
					? View.GONE : View.VISIBLE);
		}
	}
	
	private void resetAdapter()
	{
		if (App.LOGV)
			App.logv("Resetting gallery adapter...");
		
		if (mGallery != null)
		{
		    mAdapter = new ImageAdapter(getActivity());
		    mGallery.setAdapter(mAdapter);
		}
	}
	
	public static void openSite(Context context, GameCatalogItem item)
	{
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(item.DetailUrl));
		
		context.startActivity(intent);
	}
	
	public static boolean isRemindable(GameCatalogItem item)
	{
	    Calendar now = Calendar.getInstance();
	    
		return item != null && 
	    		item.ReleaseDateTicks > now.getTimeInMillis();
	}
	
	public static void addReminder(Context context, GameCatalogItem item)
	{
		if (item == null || item.ReleaseDateTicks <= 0)
		{
			Toast toast = Toast.makeText(context, 
					R.string.cannot_add_reminder, Toast.LENGTH_LONG);
			toast.show();
			
			return;
		}
		
		Intent intent = new Intent(Intent.ACTION_EDIT);
		intent.setType("vnd.android.cursor.item/event");
		intent.putExtra("beginTime", item.ReleaseDateTicks);
		intent.putExtra("allDay", true);
		intent.putExtra("title", 
				context.getString(R.string.game_release_reminder_title_f, 
						item.Title));
		
		try
		{
			context.startActivity(intent);
		}
		catch(Exception e)
		{
			Toast toast = Toast.makeText(context, 
					R.string.cannot_launch_calendar_activity, Toast.LENGTH_LONG);
			toast.show();
		}
	}
	
	private void synchronizeWithServer()
	{
		if (mItem == null)
			return;
		
		TaskController.get().runCustomTask(null, new CustomTask<GameCatalogItemDetails>()
				{
					@Override
					public void runTask() throws AuthenticationException,
							IOException, ParserException
					{
						PsnParser p = mAccount.createLocaleBasedParser(getActivity());
						
						try
						{
							GameCatalogItemDetails details = p.fetchGameCatalogItemDetails(mAccount,
									mItem);
							setResult(details);
						}
						finally
						{
							p.dispose();
						}
					}
				}, mListener);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
	    super.onCreateOptionsMenu(menu, inflater);
	    
    	inflater.inflate(R.menu.psn_game_catalog_details, menu);
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu)
	{
	    super.onPrepareOptionsMenu(menu);
	    
	    // Don't allow reminders for past releases
	    
	    menu.setGroupVisible(R.id.menu_group_remindable,
	    		isRemindable(mItem));
	    
	    menu.setGroupVisible(R.id.menu_group_selected, mDetails != null);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
	    switch (item.getItemId()) 
	    {
	    case R.id.menu_catalog_reminder:
	    	addReminder(getActivity(), mItem);
	    	return true;
	    case R.id.menu_catalog_website:
	    	openSite(getActivity(), mItem);
	    	return true;
	    }
	    
	    return false;
	}
	
	@Override 
	public void onImageReady(long id, Object param, Bitmap bmp)
	{
		super.onImageReady(id, param, bmp);
		
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				synchronizeLocal();
			}
		});
	}
	
	private OnImageReadyListener mGalleryListener = new OnImageReadyListener() 
	{
		@Override
		public void onImageReady(long id, Object param, Bitmap bmp)
		{
			mIconCache.put((String)param, new SoftReference<Bitmap>(bmp));
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					mAdapter.notifyDataSetChanged();
				}
			});
		}
	};
	
	private class ImageAdapter extends BaseAdapter
	{
		int mGalleryItemBackground;
		private Context mContext;
		
		public ImageAdapter(Context c)
		{
			mContext = c;
			
			TypedArray a = c.obtainStyledAttributes(R.styleable.ScreenshotGallery);
			mGalleryItemBackground = a.getResourceId(R.styleable.ScreenshotGallery_android_galleryItemBackground, 0);
			
			a.recycle();
		}
		
		public int getCount()
		{
			if (mDetails == null || mDetails.ScreenshotsThumb == null)
				return 0;
			
			return mDetails.ScreenshotsThumb.length;
		}
		
		public Object getItem(int position)
		{
			return position;
		}
		
		public long getItemId(int position)
		{
			return position;
		}
		
		public View getView(int position, View convertView, ViewGroup parent)
		{
			ImageView i = new ImageView(mContext);
			
			if (mDetails != null && mDetails.ScreenshotsThumb != null)
			{
				String url = mDetails.ScreenshotsThumb[position];
				Bitmap bmp = null;
				
				if (url != null)
				{
					ImageCache ic = ImageCache.get();
					SoftReference<Bitmap> cachedBmp = mIconCache.get(url);
	
					if (cachedBmp == null || (bmp = cachedBmp.get()) == null)
					{
						if ((bmp = ic.getCachedBitmap(url)) != null)
						{
							mIconCache.put(url, new SoftReference<Bitmap>(bmp));
						}
						else
						{
							ic.requestImage(url, mGalleryListener, 0, url,
							        mScreenshotPolicy);
						}
					}
				}
				
				i.setImageBitmap(bmp);
				i.setLayoutParams(new Gallery.LayoutParams(mScaledScreenshotWidth,
				        mScaledScreenshotHeight));
				i.setScaleType(ImageView.ScaleType.FIT_XY);
				i.setBackgroundResource(mGalleryItemBackground);
			}
			
			return i;
		}
	}
}
