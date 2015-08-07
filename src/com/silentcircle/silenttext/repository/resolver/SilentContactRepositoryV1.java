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
package com.silentcircle.silenttext.repository.resolver;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Binder;
import android.support.v4.content.CursorLoader;
import android.view.View;

import com.silentcircle.silentcontacts.ScContactsContract;
import com.silentcircle.silentcontacts.ScContactsContract.CommonDataKinds.Im;
import com.silentcircle.silentcontacts.ScContactsContract.CommonDataKinds.Photo;
import com.silentcircle.silentcontacts.ScContactsContract.Data;
import com.silentcircle.silentcontacts.ScContactsContract.Intents.Insert;
import com.silentcircle.silentcontacts.ScContactsContract.QuickContact;
import com.silentcircle.silentcontacts.ScContactsContract.RawContacts;
import com.silentcircle.silenttext.ServiceConfiguration;
import com.silentcircle.silenttext.log.Log;
import com.silentcircle.silenttext.model.Contact;
import com.silentcircle.silenttext.repository.ContactRepository;
import com.silentcircle.silenttext.repository.resolver.criteria.Criteria;
import com.silentcircle.silenttext.repository.resolver.criteria.SilentContactsIMCriteriaV1;
import com.silentcircle.silenttext.util.CursorUtils;

/**
 * @deprecated Remove this class when this application no longer needs to support the standalone
 *             Silent Contacts application.
 */
@Deprecated
public class SilentContactRepositoryV1 implements ContactRepository {

	public static class Permission {

		public static final String READ = "com.silentcircle.silentcontacts.permission.READ";
		public static final String WRITE = "com.silentcircle.silentcontacts.permission.WRITE";

	}

	public static boolean supports( Context context ) {
		if( context.getPackageManager().resolveContentProvider( ScContactsContract.AUTHORITY, 0 ) == null ) {
			return false;
		}
		int grant = context.checkPermission( Permission.READ, Binder.getCallingPid(), Binder.getCallingUid() );
		if( PackageManager.PERMISSION_DENIED == grant ) {
			return false;
		}
		return true;
	}

	protected final Log log = new Log( "SilentContactRepositoryV1" );
	private final ContentResolver resolver;
	private final Criteria criteria = new SilentContactsIMCriteriaV1();

	public SilentContactRepositoryV1( ContentResolver resolver ) {
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
	public InputStream getAvatar( String username ) {

		Cursor cursor = search( username );

		try {

			if( cursor != null && cursor.moveToFirst() ) {

				int id = CursorUtils.getInt( cursor, RawContacts.PHOTO_ID );

				Cursor bitmaps = resolver.query( ContentUris.withAppendedId( Data.CONTENT_URI, id ), new String [] {
					Photo.PHOTO
				}, null, null, null );

				if( bitmaps == null ) {
					return null;
				}

				try {
					if( bitmaps.moveToFirst() ) {
						byte [] buffer = bitmaps.getBlob( 0 );
						return new ByteArrayInputStream( buffer );
					}
				} finally {
					bitmaps.close();
				}

			}

		} finally {
			if( cursor != null ) {
				cursor.close();
			}
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
	public String getDisplayName( String username ) {

		String name = null;

		Cursor contact = search( username );

		if( contact == null ) {
			return name;
		}

		while( contact.moveToNext() ) {
			if( name != null ) {
				break;
			}
			name = criteria.getDisplayName( contact );
		}

		contact.close();

		return name;

	}

	@Override
	public Intent getShowOrCreateIntent( String username ) {

		Intent intent = criteria.getShowOrCreateIntent( username );

		intent.putExtra( Insert.NAME, getDisplayName( username ) );
		intent.putExtra( Insert.IM_PROTOCOL, Im.PROTOCOL_JABBER );
		intent.putExtra( Insert.IM_HANDLE, username );

		return intent;

	}

	@Override
	public boolean isSecure() {
		return true;
	}

	@Override
	public boolean isWritable() {
		return true;
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
