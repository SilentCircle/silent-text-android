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
package com.silentcircle.silenttext.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MatrixCursor.RowBuilder;
import android.net.Uri;

import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.content.SilentPhoneAccount;
import com.silentcircle.silenttext.model.Credential;
import com.silentcircle.silenttext.model.Server;

public class AccountProvider extends ContentProvider {

	public static class Account {

		public static final String CONTENT_TYPE = "vnd.android.cursor.item/vnd.com.silentcircle.account";
		public static final String CONTENT_URI = AUTHORITY + ".account";

		public static boolean is( Uri uri ) {
			UriMatcher matcher = new UriMatcher( UriMatcher.NO_MATCH );
			matcher.addURI( AUTHORITY, "account", 1 );
			return matcher.match( uri ) == 1;
		}

	}

	public static final String AUTHORITY = "com.silentcircle.silenttext.accounts";

	@Override
	public int delete( Uri uri, String selection, String [] selectionArgs ) {
		throw new UnsupportedOperationException();
	}

	private Credential getBrokerCredential() {
		SilentTextApplication application = SilentTextApplication.from( getContext() );
		Server server = application.getServer( "broker" );
		return server == null ? null : server.getCredential();
	}

	@Override
	public String getType( Uri uri ) {
		return Account.CONTENT_TYPE;
	}

	@Override
	public Uri insert( Uri uri, ContentValues values ) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public Cursor query( Uri uri, String [] projection, String selection, String [] selectionArgs, String sortOrder ) {
		if( !Account.is( uri ) ) {
			throw new IllegalArgumentException();
		}
		MatrixCursor cursor = new MatrixCursor( new String [] {
			SilentPhoneAccount.Columns.DEVICE_ID,
			SilentPhoneAccount.Columns.API_KEY
		} );
		Credential credential = getBrokerCredential();
		if( credential != null ) {
			RowBuilder row = cursor.newRow();
			row.add( credential.getUsername() );
			row.add( credential.getPassword() );
		}
		return cursor;
	}

	@Override
	public int update( Uri uri, ContentValues values, String selection, String [] selectionArgs ) {
		throw new UnsupportedOperationException();
	}

}
