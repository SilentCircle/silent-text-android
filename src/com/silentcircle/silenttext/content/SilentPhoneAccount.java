/*
Copyright © 2013, Silent Circle, LLC.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Any redistribution, use, or modification is done solely for personal 
      benefit and not for any commercial purpose or for monetary gain
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name Silent Circle nor the names of its contributors may 
      be used to endorse or promote products derived from this software 
      without specific prior written permission.

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
package com.silentcircle.silenttext.content;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import com.silentcircle.silenttext.log.Log;
import com.silentcircle.silenttext.util.CursorUtils;

public class SilentPhoneAccount {

	public static class Columns {

		public static final String API_KEY = "api_key";
		public static final String DEVICE_ID = "device_id";

	}

	private static final Log LOG = new Log( SilentPhoneAccount.class.getSimpleName() );

	private static final Uri CONTENT_URI = Uri.parse( "content://com.silentcircle.silentphone.accounts/account" );

	public static SilentPhoneAccount [] list( ContentResolver resolver ) {

		Cursor cursor = null;
		try {
			cursor = resolver.query( CONTENT_URI, null, null, null, null );
		} catch( Throwable exception ) {
			LOG.warn( exception, "#list" );
			cursor = null;
		}

		if( cursor == null ) {
			return new SilentPhoneAccount [0];
		}

		SilentPhoneAccount [] accounts = new SilentPhoneAccount [cursor.getCount()];

		if( accounts.length > 0 ) {
			cursor.moveToFirst();
			for( int i = 0; i < accounts.length; i++ ) {
				accounts[i] = new SilentPhoneAccount( cursor );
				cursor.moveToNext();
			}
		}

		cursor.close();

		return accounts;

	}

	public final CharSequence apiKey;
	public final CharSequence deviceID;

	public SilentPhoneAccount( CharSequence apiKey, CharSequence deviceID ) {
		this.apiKey = apiKey;
		this.deviceID = deviceID;
	}

	private SilentPhoneAccount( Cursor cursor ) {
		apiKey = CursorUtils.getString( cursor, Columns.API_KEY );
		deviceID = CursorUtils.getString( cursor, Columns.DEVICE_ID );
	}

}
