/*
Copyright (C) 2014-2015, Silent Circle, LLC. All rights reserved.

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
package com.silentcircle.silenttext.repository.resolver.criteria;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.CursorLoader;

import com.silentcircle.silentcontacts2.ScBaseColumns;
import com.silentcircle.silentcontacts2.ScContactsContract;
import com.silentcircle.silentcontacts2.ScContactsContract.CommonDataKinds.Email;
import com.silentcircle.silentcontacts2.ScContactsContract.Intents;
import com.silentcircle.silentcontacts2.ScContactsContract.RawContacts;
import com.silentcircle.silenttext.util.CursorUtils;
import com.silentcircle.silenttext.util.StringUtils;

public class SilentContactsEmailCriteriaV2 implements Criteria {

	private static final String [] PROJECTION = new String [] {
		ScBaseColumns._ID,
		Email.RAW_CONTACT_ID,
		RawContacts.PHOTO_ID,
		Email.DISPLAY_NAME,
		Email.DATA
	};

	private static final String FILTER = Email.DATA + " LIKE ? OR " + Email.DISPLAY_NAME + " LIKE ?";
	private static final String SORT_BY_NAME = Email.DISPLAY_NAME + " COLLATE LOCALIZED ASC";
	private static final String BASE_URI = "mailto:";

	private static String [] getFilterParameters( String domainRestriction, String query ) {
		return new String [] {
			query == null ? "%@%" : query.indexOf( '@' ) > -1 ? query : new StringBuilder( query ).append( "%@" ).append( domainRestriction ).toString(),
			query == null ? "%" : new StringBuilder( "%" ).append( query ).append( "%" ).toString()
		};
	}

	private static Uri safe( Uri uri ) {
		return uri.buildUpon().appendQueryParameter( ScContactsContract.NON_BLOCKING, Boolean.toString( true ) ).build();
	}

	@Override
	public String getData( Cursor cursor ) {
		return CursorUtils.getString( cursor, Email.DATA );
	}

	@Override
	public String getDisplayName( Cursor cursor ) {
		return StringUtils.getStringUntil( CursorUtils.getString( cursor, Email.DISPLAY_NAME ), DISPLAY_NAME_HACK, 2 );
	}

	@Override
	public Uri getLookupURI( Cursor cursor ) {
		return safe( RawContacts.getLookupUri( CursorUtils.getLong( cursor, ScBaseColumns._ID ) ) );
	}

	@Override
	public Intent getShowOrCreateIntent( String username ) {
		return new Intent( Intents.SHOW_OR_CREATE_CONTACT, Uri.parse( new StringBuilder( BASE_URI ).append( Uri.encode( username ) ).toString() ) );
	}

	@Override
	public Uri getURI( Cursor cursor ) {
		long id = CursorUtils.getLong( cursor, Email.RAW_CONTACT_ID );
		Uri uri = ContentUris.withAppendedId( RawContacts.CONTENT_URI, id );
		return safe( uri );
	}

	@Override
	public Cursor query( ContentResolver resolver, String domainRestriction, String query ) {
		try {
			return resolver.query( safe( Email.CONTENT_URI ), PROJECTION, FILTER, getFilterParameters( domainRestriction, query ), SORT_BY_NAME );
		} catch( SecurityException exception ) {
			return null;// Unauthorized to access Silent Contacts.
		}
	}

	@Override
	public CursorLoader query( Context context, String domainRestriction, String query ) {
		try {
			return new CursorLoader( context, safe( Email.CONTENT_URI ), PROJECTION, FILTER, getFilterParameters( domainRestriction, query ), SORT_BY_NAME );
		} catch( SecurityException exception ) {
			return null;// Unauthorized to access Silent Contacts.
		}
	}

}
