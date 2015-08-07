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
package com.silentcircle.silenttext.repository.remote;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MatrixCursor.RowBuilder;
import android.graphics.Bitmap;
import android.support.v4.content.CursorLoader;
import android.util.LruCache;
import android.view.View;

import com.silentcircle.api.Session;
import com.silentcircle.api.model.User;
import com.silentcircle.api.model.UserSearchResult;
import com.silentcircle.core.util.CollectionUtils;
import com.silentcircle.http.client.exception.NetworkException;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.log.Log;
import com.silentcircle.silenttext.model.Contact;
import com.silentcircle.silenttext.repository.ContactRepository;
import com.silentcircle.silenttext.util.CursorUtils;
import com.silentcircle.silenttext.util.IOUtils;
import com.silentcircle.silenttext.util.StringUtils;

/**
 * A contacts repository that directly consults the Silent Circle directory server API.
 */
public class RemoteContactRepository implements ContactRepository {

	private static final String CONTACT_ID = "id";
	private static final String CONTACT_ALIAS = "alias";
	private static final String CONTACT_DEVICE = "device";

	private static final String [] PROJECTION = new String [] {
		CONTACT_ID,
		CONTACT_ALIAS,
		CONTACT_DEVICE
	};

	private static final LruCache<String, Bitmap> AVATAR_CACHE = new LruCache<String, Bitmap>( 4 * 1024 * 1024 ) {

		@Override
		protected int sizeOf( String key, Bitmap value ) {
			return value.getByteCount();
		}

	};

	private static MatrixCursor add( MatrixCursor cursor, UserSearchResult userSearchResult ) {

		RowBuilder row = cursor.newRow();

		row.add( userSearchResult.getUserID() );
		row.add( userSearchResult.getDisplayName() );
		row.add( null );

		return cursor;

	}

	public static void cacheAvatar( String username, Bitmap bitmap ) {
		AVATAR_CACHE.put( username, bitmap );
	}

	public static InputStream getAvatar( Context context, String username ) {
		User user = null;
		user = SilentTextApplication.from( context ).getUser( username );

		CharSequence avatarURL = user != null ? user.getAvatarURL() : null;
		if( avatarURL != null ) {
			String APIURL = SilentTextApplication.from( context ).getAPIURL();
			avatarURL = String.format( "%s%s", APIURL, avatarURL );

			try {
				return IOUtils.openURL( avatarURL );
			} catch( IOException exception ) {
				// Avatar fetch failure, not a big deal :)
			}
		}
		return null;
	}

	public static Bitmap getCachedAvatar( String username ) {
		return AVATAR_CACHE.get( username );
	}

	public static String getDisplayName( User user ) {

		if( user == null ) {
			return null;
		}

		String displayName = user.getDisplayName() == null ? null : user.getDisplayName().toString();

		if( StringUtils.isMinimumLength( displayName, 1 ) ) {
			return displayName;
		}

		String firstName = user.getFirstName() == null ? null : user.getFirstName().toString();
		String lastName = user.getLastName() == null ? null : user.getLastName().toString();

		if( !StringUtils.isMinimumLength( firstName, 1 ) ) {
			firstName = null;
		}

		if( !StringUtils.isMinimumLength( lastName, 1 ) ) {
			lastName = null;
		}

		if( firstName == null ) {
			return lastName;
		}

		if( lastName == null ) {
			return firstName;
		}

		return String.format( "%s %s", firstName, lastName );

	}

	private static Cursor toCursor( List<UserSearchResult> userSearchResults ) {
		MatrixCursor cursor = new MatrixCursor( PROJECTION );
		if( !CollectionUtils.isEmpty( userSearchResults ) ) {
			for( UserSearchResult userSearchResult : userSearchResults ) {
				add( cursor, userSearchResult );
			}
		}
		return cursor;
	}

	private final Session session;

	private final Log log = new Log( "RemoteContactRepository" );

	public RemoteContactRepository( Session session ) {
		this.session = session;
	}

	@Override
	public boolean exists( String id ) {
		try {
			return session.findUser( id ) != null;
		} catch( Throwable exception ) {
			log.warn( exception, "#exists id:%s", id );
			return false;
		}
	}

	@Override
	public InputStream getAvatar( String id ) {
		User user = getUser( id );
		CharSequence avatarURL = user != null ? user.getAvatarURL() : null;
		if( avatarURL != null ) {
			try {
				return IOUtils.openURL( avatarURL );
			} catch( IOException exception ) {
				throw new NetworkException( exception );
			}
		}
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
			return session.findUser( id );
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
		return toCursor( session.searchUsers( query ) );
	}

	@Override
	public void showQuickContact( View targetView, String id ) {
		// TODO: There is not yet a Quick Contact view for Directory contacts.
	}

}
