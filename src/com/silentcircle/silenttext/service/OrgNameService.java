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
package com.silentcircle.silenttext.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.silentcircle.keymanagersupport.KeyManagerSupport;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.crypto.CryptoUtils;
import com.silentcircle.silenttext.log.Log;
import com.silentcircle.silenttext.util.Constants;

public class OrgNameService extends IntentService {

	public static final String TAG = "OrgNameService";
	public static final String DEV_AUTH_DATA_TAG_DEV = "device_authorization_dev";
	public static final String ACCOUNT_EXPIRED = "account_expired";

	public OrgNameService() {
		super( OrgNameService.class.getName() );
	}

	private void broadcastAccountExpired() {
		Intent intent = new Intent( ACCOUNT_EXPIRED );
		LocalBroadcastManager.getInstance( this ).sendBroadcast( intent );
	}

	String getShardAuthTag() {
		if( SilentTextApplication.ACCOUNT_CREATION_CLIENT_BASE_URL.contains( "dev" ) ) {
			return DEV_AUTH_DATA_TAG_DEV;
		}
		return KeyManagerSupport.DEV_AUTH_DATA_TAG;
	}

	@Override
	protected void onHandleIntent( Intent intent ) {
		if( SilentTextApplication.from( this ) == null ) {
			scheduleNextCheck();
			return;
		}
		if( SilentTextApplication.from( this ).getSession() == null ) {
			scheduleNextCheck();
			return;
		}
		if( SilentTextApplication.from( this ).getSession().getAccessToken() == null ) {
			scheduleNextCheck();
			return;
		}
		byte [] data = CryptoUtils.toByteArray( SilentTextApplication.from( this ).getSession().getAccessToken() );
		if( data == null ) {
			scheduleNextCheck();
			return;
		}
		BufferedReader reader = null;
		try {
			String devAuthorization = new String( data, "UTF-8" ).trim();
			// save to the global Constants for Directory Search later use.
			Constants.mApiKey = devAuthorization;
			// Log.d( TAG, "Authentication data SC directory search (API key) : " +
			// devAuthorization );
			URL mRequestUrl = new URL( SilentTextApplication.ACCOUNT_CREATION_CLIENT_BASE_URL + getResources().getString( R.string.sccps_user_management_base_alt ) + "?api_key=" + devAuthorization );
			// Log.i( TAG, "SC directory search request url : " + mRequestUrl );
			HttpsURLConnection urlConnection = (HttpsURLConnection) mRequestUrl.openConnection();
			urlConnection.setRequestProperty( "Accept-Language", Locale.getDefault().getLanguage() );
			int ret = urlConnection.getResponseCode();
			Log.d( TAG, "HTTP code: " + ret );

			if( ret == HttpURLConnection.HTTP_OK ) {
				Constants.mIsAccountExpired = false;
				reader = new BufferedReader( new InputStreamReader( urlConnection.getInputStream() ) );
				StringBuilder content = new StringBuilder();
				for( String str = reader.readLine(); str != null; str = reader.readLine() ) {
					content.append( str ).append( '\n' );
				}
				JSONObject jsonObj = new JSONObject( content.toString() );
				if( jsonObj.toString().contains( "organization" ) ) {
					Constants.mOrgName = jsonObj.getString( "organization" );
					// Log.i( TAG, "orgnization name: " + Constants.mOrgName );
				}

				if( jsonObj.toString().contains( "subscription" ) ) {
					JSONObject subscription = jsonObj.getJSONObject( "subscription" );
					if( subscription.toString().contains( "expires" ) ) {
						JSONObject jObj = new JSONObject( subscription.toString() );
						String expirationString = jObj.getString( "expires" );
						Log.i( TAG, "expires: " + expirationString );
						if( !TextUtils.isEmpty( expirationString ) ) {
							try {
								Constants.mAccountExpirationDate = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss" ).parse( expirationString );
							} catch( ParseException e ) {
								Log.e( TAG, "Date parsing failed: " + e.getMessage() );
								return;
							}
						}
					}
				}
			}
		} catch( UnsupportedEncodingException e ) {
			e.printStackTrace();
		} catch( MalformedURLException e ) {
			e.printStackTrace();
		} catch( IOException e ) {
			e.printStackTrace();
			// TODO: this exception has account expired info which shows Certificate expired on
			// xxxxxxx (date expired)
			if( e.getMessage().contains( "Certificate expired" ) ) {
				broadcastAccountExpired();
			}

		} catch( JSONException e ) {
			e.printStackTrace();
		} finally {
			try {
				if( reader != null ) {
					reader.close();
				}
			} catch( IOException e ) {
				e.printStackTrace();
			}
		}
	}

	private void scheduleNextCheck() {
		Intent intent = new Intent( this, this.getClass() );
		PendingIntent pendingIntent = PendingIntent.getService( this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT );

		long currentTimeMillis = System.currentTimeMillis();
		long nextUpdateTimeMillis = currentTimeMillis + 200;

		AlarmManager alarmManager = (AlarmManager) getSystemService( Context.ALARM_SERVICE );
		alarmManager.set( AlarmManager.RTC, nextUpdateTimeMillis, pendingIntent );

	}
}
