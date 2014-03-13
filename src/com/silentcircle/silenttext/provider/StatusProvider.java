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
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MatrixCursor.RowBuilder;
import android.net.Uri;

import com.silentcircle.silenttext.application.SilentTextApplication;

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
			if( !application.isActivated() ) {
				return UNLOCKED;
			}
			if( !application.isOnline() ) {
				return ACTIVATED;
			}
			return ONLINE;
		}

		public static String getType( String authority ) {
			return "vnd.android.cursor.item/" + authority + "." + KEY;
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
	public static final Uri CONTENT_URI = Uri.parse( "content://" + AUTHORITY );

	private static boolean isSupportedURI( Uri uri ) {
		return Status.is( AUTHORITY, uri );
	}

	@Override
	public int delete( Uri uri, String selection, String [] selectionArgs ) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getType( Uri uri ) {
		if( !isSupportedURI( uri ) ) {
			throw new IllegalArgumentException();
		}
		return Status.getType( AUTHORITY );
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
		if( !isSupportedURI( uri ) ) {
			throw new IllegalArgumentException();
		}
		return Status.query( getContext() );
	}

	@Override
	public int update( Uri uri, ContentValues values, String selection, String [] selectionArgs ) {
		throw new UnsupportedOperationException();
	}

}
