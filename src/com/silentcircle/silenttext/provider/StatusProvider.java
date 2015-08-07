/*
Copyright (C) 2013-2015, Silent Circle, LLC. All rights reserved.

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
package com.silentcircle.silenttext.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MatrixCursor.RowBuilder;
import android.net.Uri;

import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.util.DeviceUtils;

public class StatusProvider extends ContentProvider {

	public static enum Status {

		LOCKED( 0 ),
		UNLOCKED( 1 ),
		ACTIVATED( 2 ),
		ONLINE( 3 ),
		UNKNOWN( -1 );

		private final int value;

		public static final int CODE = 1;
		public static final String KEY = "status";

		public static Status from( Context context ) {
			SilentTextApplication application = SilentTextApplication.from( context );
			if( !application.isUnlocked() ) {
				return LOCKED;
			}
			if( !application.isUserKeyUnlocked() ) {
				return UNLOCKED;
			}
			if( !application.isXMPPTransportConnected() ) {
				return ACTIVATED;
			}
			return ONLINE;
		}

		public static String getType( String authority, String key ) {
			return "vnd.android.cursor.item/" + authority + "." + key;
		}

		public static boolean is( String authority, Uri uri ) {
			UriMatcher matcher = new UriMatcher( UriMatcher.NO_MATCH );
			matcher.addURI( authority, Status.KEY, Status.CODE );
			return matcher.match( uri ) == Status.CODE;
		}

		public static Cursor query( Context context ) {
			MatrixCursor cursor = new MatrixCursor( new String [] {
				KEY
			}, 1 );
			Status status = Status.from( context );
			RowBuilder row = cursor.newRow();
			row.add( Integer.valueOf( status.value() ) );
			return cursor;
		}

		private Status( int value ) {
			this.value = value;
		}

		public int value() {
			return value;
		}

	}

	public static final String AUTHORITY = "com.silentcircle.silenttext";
	// public static final Uri CONTENT_URI = Uri.parse( "content://" + AUTHORITY );

	public static final String BP_FEATURECODE = "FEATURECODE";
	public static final String BP_KEY = "bpw-status";

	private static final int STATUS = 1;

	private static final int BLACK_PHONE_STATUS = 2;
	private static final int BLACK_PHONE_PROVISION = 3;

	private static final UriMatcher sURIMatcher = new UriMatcher( UriMatcher.NO_MATCH );
	static {
		sURIMatcher.addURI( AUTHORITY, Status.KEY, STATUS );
		sURIMatcher.addURI( AUTHORITY, BP_KEY, BLACK_PHONE_STATUS );
		sURIMatcher.addURI( AUTHORITY, "bpw-provision", BLACK_PHONE_PROVISION );
	}

	// private static boolean isSupportedURI( Uri uri ) {
	// return Status.is( AUTHORITY, uri );
	// }

	@Override
	public int delete( Uri uri, String selection, String [] selectionArgs ) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getType( Uri uri ) {
		int match = sURIMatcher.match( uri );
		switch( match ) {
			case STATUS:
				return Status.getType( AUTHORITY, Status.KEY );
			case BLACK_PHONE_STATUS:
				return Status.getType( AUTHORITY, BP_KEY );
		}
		throw new IllegalArgumentException( "Unknown URI: " + uri );
	}

	@Override
	public Uri insert( Uri uri, ContentValues values ) {
		int match = sURIMatcher.match( uri );

		if( match != BLACK_PHONE_PROVISION ) {
			throw new UnsupportedOperationException( "Cannot insert URL: " + uri );
		}

		if( !values.containsKey( BP_FEATURECODE ) ) {
			throw new UnsupportedOperationException( "Missing field, Cannot insert URL: " + uri );
		}

		Context context = getContext();
		if( context == null ) {
			return null;
		}

		final String licenseCode = values.getAsString( BP_FEATURECODE );
		DeviceUtils.putApplicationLicenseCode( context, licenseCode );

		return uri;
	}

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public Cursor query( Uri uri, String [] projection, String selection, String [] selectionArgs, String sortOrder ) {
		int match = sURIMatcher.match( uri );
		switch( match ) {
			case STATUS: {
				return Status.query( getContext() );
			}
			// NYI: not required by setup wizard
			// case BLACK_PHONE_STATUS: {
			// final MatrixCursor c = new MatrixCursor( new String [] {
			// "PROVISIONSTATUS",
			// "STATUSTEXT",
			// "FEATURETEXT"
			// }, 1 );
			// final MatrixCursor.RowBuilder row = c.newRow();
			// int bp_status = BP_PROVISIONED;
			// String statusText = null;
			// int valid = LoadUserInfo.checkIfExpired();
			// Context ctx = getContext();
			// if( ctx == null ) {
			// return null;
			// }
			// if( valid == LoadUserInfo.INVALID ) {
			// bp_status = BP_EXPIRED;
			// statusText = ctx.getString( R.string.subscription_expired );
			// }
			// switch( status ) {
			// case NOT_STARTED:
			// bp_status = BP_ERROR;
			// statusText = ctx.getString( R.string.bp_not_started );
			// break;
			// case NOT_PROVISIONED:
			// bp_status = BP_NOT_PROVISIONED;
			// break;
			// case OFFLINE:
			// statusText = ctx.getString( R.string.bp_is_offline );
			// break;
			// case ONLINE:
			// statusText = ctx.getString( R.string.bp_is_online );
			// break;
			// case REGISTER:
			// statusText = ctx.getString( R.string.bp_registering );
			// break;
			// default:
			// bp_status = BP_ERROR;
			// statusText = ctx.getString( R.string.bp_status_unknown );
			// break;
			// }
			// row.add( bp_status );
			// row.add( statusText );
			// row.add( null ); // Or:
			// row.add( "feature text" );
			// return c;
			// }
		}

		throw new IllegalArgumentException();
	}

	@Override
	public int update( Uri uri, ContentValues values, String selection, String [] selectionArgs ) {
		throw new UnsupportedOperationException();
	}

}
