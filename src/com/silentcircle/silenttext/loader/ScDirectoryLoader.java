/*
Copyright (C) 2015, Silent Circle, LLC. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Any redistribution, use, or modification is done solely for personal
      benefit and not for any commercial purpose or for monetary gain
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name Silent Circle nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL SILENT CIRCLE, LLC BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.silentcircle.silenttext.loader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.provider.ContactProvider;
import com.silentcircle.silenttext.util.Constants;

/**
 * Custom loader to load SC directory entries and setup a cursor. Created by Rong Li on 10.11.14.
 */
public class ScDirectoryLoader extends AsyncTaskLoader<Cursor> {

	public static class UserData {

		String raw_id;

		final String fullName;

		String displayName; // displayName == fullName

		String userName;

		String numbers;

		String jid;

		String avatarUrl;

		boolean favorite;

		public UserData( String full, String display, String un, String no, String jid, String avatar ) {
			fullName = full;
			displayName = display;
			userName = un;
			numbers = no;
			this.jid = jid;
			avatarUrl = avatar;
		}

		public String getAvatarUrl() {
			return avatarUrl;
		}

		public String getDisplayName() {
			return displayName;
		}

		public String getFullName() {
			return fullName;
		}

		public String getJid() {
			return jid;
		}

		public String getNumbers() {
			return numbers;
		}

		public String getRaw_id() {
			return raw_id;
		}

		public String getUserName() {
			return userName;
		}

		public boolean isFavorite() {
			return favorite;
		}

		public void setAvatarUrl( String avatarUrl ) {
			this.avatarUrl = avatarUrl;
		}

		public void setDisplayName( String displayName ) {
			this.displayName = displayName;
		}

		public void setFavorite( boolean favorite ) {
			this.favorite = favorite;
		}

		public void setJid( String jid ) {
			this.jid = jid;
		}

		public void setNumbers( String numbers ) {
			this.numbers = numbers;
		}

		public void setRaw_id( String raw_id ) {
			this.raw_id = raw_id;
		}

		public void setUserName( String userName ) {
			this.userName = userName;
		}

		@Override
		public String toString() {
			return fullName;
		}

	}

	private static final String TAG = "ScDirectoryLoader";
	private final static int MAX_SEARCH = 20;

	// private static final String[] PROJECTION = new String[]{
	// Phone._ID, // 0
	// Phone.TYPE, // 1
	// Phone.LABEL, // 2
	// Phone.NUMBER, // 3
	// Phone.RAW_CONTACT_ID, // 4
	// Phone.PHOTO_ID, // 5
	// Phone.DISPLAY_NAME_PRIMARY, // 6
	// };
	private static final String [] PROJECTION = new String [] {
		ContactProvider.ID,
		ContactProvider.USERNAME,
		ContactProvider.DISPLAY_NAME,
		// source is the same as JID in UserData object
		ContactProvider.SOURCE,
		ContactProvider.PHONE_NUMBER,
		ContactProvider.JID,
		ContactProvider.AVATAR,
		ContactProvider.FAVORITE
	};

	private static TreeMap<String, UserData> mUserMap;
	private static List<UserData> mUserList;

	private static String mSearchText = "b";

	private static String mPreviousSearchText;

	private static int mStart = 0;

	private static Cursor mCurrentCursor;
	private static Handler mHandler;

	public static final String DIRECTORY_SEARCH_ERROR_MESSAGE = "directory_search_error_message";

	public static final String DIRECTORY_SEARCH_WHAT = "directory_search_what";

	public static final int NO_ORGANIZATION = 111;

	@SuppressLint( {
		"NewApi",
		"UseValueOf"
	} )
	private static void addRow( MatrixCursor cursor, UserData ud, long _id ) {
		// set row_id to the UserData object to retrieve data later.
		ud.setRaw_id( _id + "" );

		MatrixCursor.RowBuilder row = cursor.newRow();
		row.add( new Long( _id ) ); // ID
		// row.add( Phone.TYPE_SILENT ); //TYPE
		row.add( ud.userName ); // USERNAME
		row.add( ud.displayName ); // NUMBER
		row.add( null ); // RAW_CONTACT_ID
		row.add( ud.numbers ); // PHONE_NUMBER
		row.add( ud.jid ); // JID
		row.add( ud.avatarUrl ); // PHOTO_THUMBNAIL_URI
	}

	public static void clearCachedData() {
		if( mUserList != null ) {
			mUserList.clear();
		}
		if( mUserMap != null ) {
			mUserMap.clear();
		}
		mSearchText = mPreviousSearchText = null;
	}

	public static List<UserData> getUserList() {
		return mUserList;
	}

	public static void setSearchQuery( String query ) {
		mSearchText = query;
	}

	public static void setStart( int val ) {
		mStart = val;
		if( mUserList != null ) {
			mUserList.clear();
		}
		if( mUserMap != null ) {
			mUserMap.clear();
		}
	}

	@SuppressLint( "DefaultLocale" )
	private static void setUserList( String response ) {
		Log.d( TAG, "Directory search response : " + response );
		try {
			JSONObject obj = new JSONObject( response );
			JSONArray arr = obj.getJSONArray( "people" );

			if( arr.length() == 0 ) {
				mHandler.sendEmptyMessage( Constants.STOP_LOADING );
			}

			if( arr.length() < MAX_SEARCH ) {
				Constants.mIsIncrementalSearch = false;
			} else {
				Constants.mIsIncrementalSearch = true;
			}
			for( int t = 0; t < arr.length(); t++ ) {

				JSONObject userObject = arr.getJSONObject( t );
				String fullName = userObject.getString( "full_name" );
				// NOTE: currently use userName as phone number, numbers is useless in this case.
				// JSONArray jphone = userObject.getJSONArray( "numbers" );
				// String [] phoneList = new String [jphone.length()];
				// for( int i = 0; i < phoneList.length; i++ ) {
				// phoneNumbers += jphone.getString( i ) + " ";
				// }
				String userName = userObject.getString( "username" );
				String phoneNumbers = userName;
				// if( fullName.toUpperCase().contains( mSearchText.toUpperCase() ) ||
				// userName.toUpperCase().contains( mSearchText.toUpperCase() ) ) {
				// UserData user = new UserData( fullName, fullName, userObject.getString(
				// "username" ), phoneNumbers, userObject.getString( "jid" ), userObject.getString(
				// "avatar_url" ) );
				// mUserList.add( user );
				// }
				String [] nameAry = fullName.split( " " );
				boolean isAddedFullname = false;
				for( int l = 0; l < nameAry.length; l++ ) {
					if( nameAry[l].toUpperCase().startsWith( mSearchText.toUpperCase() ) ) {
						isAddedFullname = true;
						break;
					}
				}
				if( isAddedFullname || userName.toUpperCase().startsWith( mSearchText.toUpperCase() ) ) {
					UserData user = new UserData( fullName, fullName, userObject.getString( "username" ), phoneNumbers, userObject.getString( "jid" ), userObject.getString( "avatar_url" ) );
					// mUserList.add( user );
					mUserMap.put( userName, user );
				}

			}
			mUserList = new ArrayList<UserData>( mUserMap.values() );
		} catch( Exception e ) {
			Log.e( TAG, "parseJsonToUsers Error: " + e.getMessage() );
		}

	}

	private final MatrixCursor mEmptyCursor = new MatrixCursor( PROJECTION, 0 );

	private String mDevAuthorization;

	private final Context mContext;

	private final boolean mUseScDirLoaderOrg;

	public ScDirectoryLoader( Context context, String query, Handler handler, boolean useScDirLoaderOrg ) {
		super( context );
		mContext = context;
		mSearchText = query;
		mHandler = handler;
		mUseScDirLoaderOrg = useScDirLoaderOrg;

		if( mUserList == null ) {
			mUserList = new ArrayList<UserData>();
		}
		if( mUserMap == null ) {
			mUserMap = new TreeMap<String, UserData>();
		}
		onContentChanged();
	}

	// private Cursor createCursorMatching() {
	// int size;
	//
	// if( mUserList == null || ( size = mUserList.size() ) == 0 ) {
	// return mEmptyCursor;
	// }
	//
	// MatrixCursor cursor = new MatrixCursor( PROJECTION, size * 2 );
	//
	// long _id = 3;
	// for( UserData ud : mUserList ) {
	// if( ud.displayName.toUpperCase().contains( mSearchText.toUpperCase() ) ||
	// ud.userName.toUpperCase().contains( mSearchText.toUpperCase() ) )
	// {
	// addRow( cursor, ud, _id );
	// _id++;
	// }
	// }
	// return cursor;
	// }

	private Cursor createCursor() {
		int size;
		if( mUserList == null || ( size = mUserList.size() ) == 0 ) {
			return mEmptyCursor;
		}

		MatrixCursor cursor = new MatrixCursor( PROJECTION, size );
		int _id = 3;
		for( int i = 0; i < mUserList.size(); i++ ) {
			addRow( cursor, mUserList.get( i ), _id );
			_id++;
		}

		return cursor;
	}

	private String getDirectorySearchUserURL() {
		// directory search url format.
		// "https://sccps-dev.silentcircle.com/v1/people/?terms=wern&api_key=5f7778192246508af6d6c986cefafee20b73321a1a2a47a96f2c0a36&start=6000&compact=false&max=10";
		mDevAuthorization = Constants.mApiKey;

		String url = SilentTextApplication.ACCOUNT_CREATION_CLIENT_BASE_URL + mContext.getResources().getString( R.string.sccps_directory_request ) + mSearchText + "&api_key=" + Uri.encode( mDevAuthorization ) + ( mUseScDirLoaderOrg ? "&org_only=1" : "" ) + "&start=" + mStart + "&compact=false&max=" + MAX_SEARCH;
		mStart += MAX_SEARCH;
		// Log.d( TAG, "Directory search url : " + url );
		return url;
	}

	private void handleResponseErrorCode() {
		mStart = 0;
		if( mUserList != null ) {
			mUserList.clear();
		}
		if( mUserMap != null ) {
			mUserMap.clear();
		}
		HttpURLConnection urlConnection = null;
		try {
			URL url = new URL( getDirectorySearchUserURL() );
			urlConnection = (HttpURLConnection) url.openConnection();
			if( urlConnection.getErrorStream() == null ) {
				Log.i( TAG, "urlConnection.getErrorStream() == null" );
			} else {
				InputStreamReader in = new InputStreamReader( urlConnection.getErrorStream(), "UTF-8" );
				BufferedReader reader = new BufferedReader( in );
				String line;
				StringBuilder builder = new StringBuilder();
				while( ( line = reader.readLine() ) != null ) {
					builder.append( line );
				}
				Log.d( TAG, "Directory Search Error: " + builder.toString() );
				try {
					JSONObject obj = new JSONObject( new String( builder ) );
					// server side defined error may not need to show. if it's needed, uncomment
					// mHandler.sendMessage( msg );
					Message msg = mHandler.obtainMessage();
					Bundle b = new Bundle();
					b.putString( DIRECTORY_SEARCH_ERROR_MESSAGE, obj.getString( "error_msg" ) );
					b.putInt( DIRECTORY_SEARCH_WHAT, NO_ORGANIZATION );
					msg.setData( b );
					// mHandler.sendMessage( msg );
				} catch( Exception e ) {
					Log.e( TAG, "parseJsonToUsers Error Code: " + e.getMessage() );
				}
			}
		} catch( MalformedURLException e ) {
			Log.e( TAG, "MalformedURLException: " + e.getLocalizedMessage() );
			e.printStackTrace();
		} catch( IOException e ) {
			Log.e( TAG, "IOException: " + e.getLocalizedMessage() );
			e.printStackTrace();
		} finally {
			if( urlConnection != null ) {
				urlConnection.disconnect();
			}
		}
	}

	@Override
	public Cursor loadInBackground() {
		HttpURLConnection urlConnection = null;
		InputStreamReader in;
		try {
			URL url = new URL( getDirectorySearchUserURL() );
			urlConnection = (HttpURLConnection) url.openConnection();
			if( urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK ) {
				in = new InputStreamReader( urlConnection.getInputStream(), "UTF-8" );
				BufferedReader reader = new BufferedReader( in );
				String line;
				StringBuilder builder = new StringBuilder();
				while( ( line = reader.readLine() ) != null ) {
					builder.append( line );
				}
				setUserList( new String( builder ) );

				mCurrentCursor = createCursor();
				return mCurrentCursor;
			}
			handleResponseErrorCode();
		} catch( MalformedURLException e ) {
			Log.e( TAG, "MalformedURLException: " + e.getLocalizedMessage() );
			e.printStackTrace();
		} catch( IOException e ) {
			Log.e( TAG, "IOException: " + e.getLocalizedMessage() );
			handleResponseErrorCode();
		} finally {
			if( urlConnection != null ) {
				urlConnection.disconnect();
			}
		}
		return null;
	}

	@Override
	public void onCanceled( Cursor data ) {
		if( data != null ) {
			data.close();
		}
	}

	@Override
	protected void onReset() {
		stopLoading();
	}

	@Override
	protected void onStartLoading() {
		if( TextUtils.isEmpty( mSearchText ) ) {
			if( mUserList != null ) {
				mUserList.clear();
			}
			if( mUserMap != null ) {
				mUserMap.clear();
			}
			deliverResult( mEmptyCursor );
			return;
		}
		// forceLoad() cause loadInBackground twice when come back from click home button,
		// But it works fine when come back after launch SPA
		// so commented it for now, if need uncommented it later.
		// forceLoad();

		// if( TextUtils.isEmpty( mSearchText ) ) {
		// mUserList = null;
		// deliverResult( mEmptyCursor );
		// return;
		// }
		// deliverResult( createCursorMatching() );
		// if( mUserList == null || mUserList.size() == 0 || takeContentChanged() ) {
		// forceLoad();
		// }
		// return;
	}

	@Override
	protected void onStopLoading() {
		cancelLoad();
	}

	public void setQueryString( String query ) {
		mStart = 0;
		if( mUserList != null ) {
			mUserList.clear();
		}
		if( mUserMap != null ) {
			mUserMap.clear();
		}
		mPreviousSearchText = mSearchText;
		mSearchText = query;
		if( mPreviousSearchText == null ) {
			mPreviousSearchText = query;
		}
	}

}
