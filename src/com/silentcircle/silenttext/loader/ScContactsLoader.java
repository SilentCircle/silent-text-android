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
package com.silentcircle.silenttext.loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.ContentResolver;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.silentcircle.silenttext.activity.ConversationListActivity;
import com.silentcircle.silenttext.log.Log;
import com.silentcircle.silenttext.util.Constants;

public class ScContactsLoader {

	private static final String TAG = "ScContactsLoader";

	private final ConversationListActivity mActivity;

	static String mCurQuery;

	private static String getJid( ContentResolver resolver, Uri result, long id ) {
		Cursor c;
		try {
			c = resolver.query( result, null, null, null, null );
			if( c != null && c.moveToFirst() ) {
				for( int a = 0; a < c.getCount(); a++ ) {
					for( int b = 0; b < c.getColumnCount(); b++ ) {
						if( c.getLong( c.getColumnIndex( Constants.RAW_CONTACT_ID ) ) == id ) {
							return c.getString( c.getColumnIndex( Constants.IM ) );
						}
					}
					c.moveToNext();
				}
				return null;
			}
		} catch( Exception e ) {
			Log.w( TAG, "Silent Contacts picker query Exception, cannot use contacts data to get JID." );
			return null;
		}
		return null;
	}

	private static String getPhoneNumber( ContentResolver resolver, Uri result, long id ) {
		Cursor c;
		c = resolver.query( result, null, null, null, null );
		if( c != null && c.moveToFirst() ) {
			for( int a = 0; a < c.getCount(); a++ ) {
				for( int b = 0; b < c.getColumnCount(); b++ ) {
					if( c.getLong( c.getColumnIndex( Constants.RAW_CONTACT_ID ) ) == id ) {
						return c.getString( c.getColumnIndex( com.silentcircle.silentcontacts2.ScContactsContract.CommonDataKinds.Phone.NUMBER ) );
					}
				}
				c.moveToNext();
			}
		}
		return null;
	}

	private final List<ContactUser> mUserList = new ArrayList<ContactUser>();

	// com.silentcircle.silentcontacts2.ScContactsContract.RawContacts.CONTENT_URI
	// _id : 1
	// display_name: Rong Li
	// starred : 1
	// photo_thumb_uri : content://com.silentcircle.contacts2/raw_contacts/1/photo

	// com.silentcircle.silentcontacts2.ScContactsContract.CommonDataKinds.Phone.CONTENT_URI
	// data1 : 444-4445555

	public ScContactsLoader( ConversationListActivity conversationListActivity, String query ) {
		mActivity = conversationListActivity;
		mCurQuery = query;
	}

	public List<ContactUser> getUserList() {
		return mUserList;
	}

	// com.silentcircle.silentcontacts2.ScContactsContract.CommonDataKinds.Im.CONTENT_URI
	// data1 : rli@xmpp-dev.silentcircle.net
	public List<ContactUser> loadScContants() {
		if( TextUtils.isEmpty( mCurQuery ) ) {
			return mUserList;
		}
		CursorLoader cursorLoader = new CursorLoader( mActivity, com.silentcircle.silentcontacts2.ScContactsContract.RawContacts.CONTENT_URI, null, null, null, null );
		// String RawContacts_CONTENT_URI = "content://com.silentcircle.contacts2/raw_contacts";
		// CursorLoader cursorLoader = new CursorLoader( mActivity, Uri.parse(
		// RawContacts_CONTENT_URI ), null, null, null, null );
		Cursor c = cursorLoader.loadInBackground();
		if( c != null && c.moveToFirst() ) {
			for( int a = 0; a < c.getCount(); a++ ) {
				ContactUser user = new ContactUser();
				long raw_id = c.getLong( c.getColumnIndex( Constants._ID ) );
				user.setRaw_id( raw_id );
				String displayName = c.getString( c.getColumnIndex( Constants.DISPLAY_NAME ) );
				user.setDisplayName( displayName );
				String jid = getJid( mActivity.getContentResolver(), com.silentcircle.silentcontacts2.ScContactsContract.CommonDataKinds.Im.CONTENT_URI, raw_id );
				// String Im_CONTENT_URI = "content://com.silentcircle.contacts2/data/im";
				// String jid = Constants.getJid( mActivity.getContentResolver(), Uri.parse(
				// Im_CONTENT_URI ), raw_id );
				String [] nameAry;
				boolean isAddedFullname = false;
				if( !TextUtils.isEmpty( displayName ) ) {
					nameAry = displayName.split( " " );
					for( int l = 0; l < nameAry.length; l++ ) {
						if( nameAry[l].toUpperCase().startsWith( mCurQuery.toUpperCase() ) ) {
							isAddedFullname = true;
							break;
						}
					}
				}
				// if( jid != null && !jid.split( "@" )[0].toUpperCase().contains(
				// mCurQuery.toUpperCase() ) || displayName != null &&
				// !displayName.toUpperCase().contains( mCurQuery.toUpperCase() ) ) {
				if( jid != null && !jid.split( "@" )[0].toUpperCase().startsWith( mCurQuery.toUpperCase() ) && !isAddedFullname ) {
					c.moveToNext();
					continue;
				}
				user.setJid( jid );
				String starred = c.getString( c.getColumnIndex( Constants.STARRED ) );
				user.setStarred( false );
				if( starred.equals( "1" ) ) {
					user.setStarred( true );
				}
				user.setAvatarUrl( c.getString( c.getColumnIndex( Constants.PHOTO_THUMB_URI ) ) );
				String phoneNumber = getPhoneNumber( mActivity.getContentResolver(), com.silentcircle.silentcontacts2.ScContactsContract.CommonDataKinds.Phone.CONTENT_URI, raw_id );
				// String Phone_CONTENT_URI = "content://com.silentcircle.contacts2/data/phones";
				// String phoneNumber = Constants.getPhoneNumber( mActivity.getContentResolver(),
				// Uri.parse( Phone_CONTENT_URI ), raw_id );
				user.setNumbers( phoneNumber );

				mUserList.add( user );
				c.moveToNext();
			}
			c.close();
		}

		Collections.sort( mUserList );
		return mUserList;
	}

	public void setSearchQuery( String query ) {
		mCurQuery = query;
		if( mUserList != null ) {
			mUserList.clear();
		}
	}
}
