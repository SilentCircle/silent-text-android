/*
Copyright © 2012-2013, Silent Circle, LLC.  All rights reserved.

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
package com.silentcircle.silenttext.repository.remote;

import java.io.InputStream;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.support.v4.content.CursorLoader;
import android.view.View;

import com.silentcircle.api.Session;
import com.silentcircle.api.model.User;
import com.silentcircle.silenttext.log.Log;
import com.silentcircle.silenttext.model.Contact;
import com.silentcircle.silenttext.repository.ContactRepository;
import com.silentcircle.silenttext.util.CursorUtils;

/**
 * A contacts repository that directly consults the Silent Circle directory server API.
 * 
 * @deprecated This repository is not yet ready to be used.
 */
@Deprecated
public class RemoteContactRepository implements ContactRepository {

	private static final String CONTACT_ID = "id";
	private static final String CONTACT_ALIAS = "alias";
	private static final String CONTACT_DEVICE = "device";

	private static final String [] PROJECTION = new String [] {
		CONTACT_ID,
		CONTACT_ALIAS,
		CONTACT_DEVICE
	};

	private static MatrixCursor add( MatrixCursor cursor, User user ) {
		cursor.newRow().add( user.getID() ).add( getDisplayName( user ) ).add( null );
		return cursor;
	}

	private static String getDisplayName( User user ) {
		if( user == null ) {
			return null;
		}
		String firstName = user.getFirstName() == null ? null : user.getFirstName().toString();
		String lastName = user.getLastName() == null ? null : user.getLastName().toString();
		if( firstName == null ) {
			return lastName;
		}
		if( lastName == null ) {
			return firstName;
		}
		return String.format( "%s %s", firstName, lastName );
	}

	private static Cursor toCursor( User user ) {
		if( user == null ) {
			return null;
		}
		return add( new MatrixCursor( PROJECTION ), user );
	}

	private final Session session;
	private final Log log = new Log( getClass().getSimpleName() );

	public RemoteContactRepository( Session session ) {
		this.session = session;
	}

	@Override
	public boolean exists( String id ) {
		try {
			return session.getUser( id ) != null;
		} catch( Throwable exception ) {
			log.warn( exception, "#exists id:%s", id );
			return false;
		}
	}

	@Override
	public InputStream getAvatar( String id ) {
		return null;
	}

	@Override
	public Contact getContact( Cursor cursor ) {
		Contact contact = new Contact();
		contact.setUsername( CursorUtils.getString( cursor, CONTACT_ID ) );
		contact.setAlias( CursorUtils.getString( cursor, CONTACT_ALIAS ) );
		contact.setDevice( CursorUtils.getString( cursor, CONTACT_DEVICE ) );
		return contact;
	}

	@Override
	public String getDisplayName( String id ) {
		return getDisplayName( getUser( id ) );
	}

	@Override
	public Intent getShowOrCreateIntent( String id ) {
		// TODO: There is not yet a SHOW_OR_CREATE intent for Directory contacts.
		return null;
	}

	private User getUser( String id ) {
		try {
			return session.getUser( id );
		} catch( Throwable exception ) {
			log.warn( exception, "#getUser id:%s", id );
		}
		return null;
	}

	@Override
	public boolean isSecure() {
		return true;
	}

	@Override
	public boolean isWritable() {
		return false;
	}

	@Override
	public Cursor list() {
		return new MatrixCursor( PROJECTION );
	}

	@Override
	public CursorLoader list( Context context ) {
		// FIXME: This CursorLoader is completely useless.
		return new CursorLoader( context );
	}

	@Override
	public CursorLoader search( Context context, String query ) {
		// FIXME: This CursorLoader is completely useless.
		return new CursorLoader( context, null, null, null, null, null );
	}

	@Override
	public Cursor search( String query ) {
		return toCursor( getUser( query ) );
	}

	@Override
	public void showQuickContact( View targetView, String id ) {
		// TODO: There is not yet a Quick Contact view for Directory contacts.
	}

}
