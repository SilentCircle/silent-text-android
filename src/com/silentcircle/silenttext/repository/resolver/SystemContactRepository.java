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
package com.silentcircle.silenttext.repository.resolver;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Intents.Insert;
import android.provider.ContactsContract.QuickContact;
import android.support.v4.content.CursorLoader;
import android.view.View;

import com.silentcircle.silenttext.ServiceConfiguration;
import com.silentcircle.silenttext.log.Log;
import com.silentcircle.silenttext.model.Contact;
import com.silentcircle.silenttext.repository.ContactRepository;
import com.silentcircle.silenttext.repository.resolver.criteria.Criteria;
import com.silentcircle.silenttext.repository.resolver.criteria.SystemContactsEmailCriteria;
import com.silentcircle.silenttext.util.CursorUtils;

public class SystemContactRepository implements ContactRepository {

	protected final Log log = new Log( "SystemContactRepository" );
	private final ContentResolver resolver;
	private final Criteria criteria = new SystemContactsEmailCriteria();

	public SystemContactRepository( ContentResolver resolver ) {
		this.resolver = resolver;
	}

	@Override
	public boolean exists( String id ) {
		Cursor cursor = search( id );
		boolean exists = cursor != null && cursor.moveToFirst();
		if( cursor != null ) {
			cursor.close();
		}
		return exists;
	}

	@Override
	public InputStream getAvatar( String email ) {

		Cursor contact = search( email );

		try {
			if( contact.moveToFirst() ) {
				int id = CursorUtils.getInt( contact, Contacts.PHOTO_ID );
				Cursor photos = resolver.query( ContentUris.withAppendedId( Data.CONTENT_URI, id ), new String [] {
					Photo.PHOTO
				}, null, null, null );
				try {
					if( photos.moveToFirst() ) {
						byte [] buffer = photos.getBlob( 0 );
						return new ByteArrayInputStream( buffer );
					}
				} finally {
					photos.close();
				}
			}
		} finally {
			contact.close();
		}

		return null;

	}

	@Override
	public Contact getContact( Cursor cursor ) {
		Contact contact = new Contact();
		contact.setAlias( criteria.getDisplayName( cursor ) );
		contact.setUsername( criteria.getData( cursor ) );
		return contact;
	}

	@Override
	public String getDisplayName( String email ) {

		String name = null;

		Cursor cursor = search( email );

		if( cursor == null ) {
			return name;
		}

		while( cursor.moveToNext() ) {
			if( name != null ) {
				break;
			}
			name = criteria.getDisplayName( cursor );
		}

		cursor.close();

		return name;

	}

	@Override
	public Intent getShowOrCreateIntent( String email ) {

		Intent intent = criteria.getShowOrCreateIntent( email );

		intent.putExtra( Insert.NAME, getDisplayName( email ) );
		intent.putExtra( Insert.IM_PROTOCOL, Im.PROTOCOL_JABBER );
		intent.putExtra( Insert.IM_HANDLE, email );

		return intent;

	}

	@Override
	public boolean isSecure() {
		return false;
	}

	@Override
	public boolean isWritable() {
		return ServiceConfiguration.getInstance().features.writableSystemContacts;
	}

	@Override
	public Cursor list() {
		return search( null );
	}

	@Override
	public CursorLoader list( Context context ) {
		return search( context, null );
	}

	@Override
	public CursorLoader search( Context context, String query ) {
		return criteria.query( context, ServiceConfiguration.getInstance().getXMPPServiceName(), query );
	}

	@Override
	public Cursor search( String query ) {
		return criteria.query( resolver, ServiceConfiguration.getInstance().getXMPPServiceName(), query );
	}

	@Override
	public void showQuickContact( View targetView, String username ) {

		Context context = targetView.getContext();
		Cursor cursor = search( username );

		if( cursor != null ) {
			if( cursor.moveToFirst() ) {
				QuickContact.showQuickContact( context, targetView, criteria.getLookupURI( cursor ), QuickContact.MODE_LARGE, null );
				cursor.close();
				return;
			}
		}

		if( isWritable() ) {
			Intent intent = getShowOrCreateIntent( username );
			try {
				context.startActivity( intent );
			} catch( ActivityNotFoundException exception ) {
				log.warn( exception, "#showQuickContact username:%s", username );
			} catch( SecurityException exception ) {
				log.warn( exception, "#showQuickContact username:%s", username );
			}
		}

		if( cursor != null ) {
			cursor.close();
		}

	}

}
