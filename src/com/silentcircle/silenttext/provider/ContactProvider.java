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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import com.silentcircle.silenttext.ServiceConfiguration;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.model.Contact;
import com.silentcircle.silenttext.model.Conversation;
import com.silentcircle.silenttext.repository.ContactRepository;
import com.silentcircle.silenttext.repository.ConversationRepository;
import com.silentcircle.silenttext.repository.resolver.SilentContactRepositoryV1;
import com.silentcircle.silenttext.repository.resolver.SilentContactRepositoryV2;
import com.silentcircle.silenttext.repository.resolver.SystemContactRepository;

public class ContactProvider {

	public static class AllLoader extends CursorLoader {

		public AllLoader( Context context ) {
			super( context );
		}

		public AllLoader( Context context, Uri uri, String [] projection, String selection, String [] selectionArgs, String sortOrder ) {
			super( context, uri, projection, selection, selectionArgs, sortOrder );
		}

		@Override
		public Cursor loadInBackground() {
			return list( getContext() );
		}

	}

	public static class Result implements Comparable<Result> {

		public int source;
		public String username;
		public String displayName;
		public String raw_contact_id;

		@Override
		public int compareTo( Result other ) {

			if( other == null ) {
				return -1;
			}

			String a = displayName == null ? username : displayName;
			String b = other.displayName == null ? other.username : other.displayName;

			if( a == null && b != null ) {
				return 1;
			}

			if( a != null && b == null ) {
				return -1;
			}

			return a == null ? 0 : a.compareTo( b );

		}

		@Override
		public boolean equals( Object object ) {
			return object != null && object instanceof Result && hashCode() == object.hashCode();
		}

		@Override
		public int hashCode() {
			return username == null ? 0 : username.hashCode();
		}

	}

	public static class SearchLoader extends CursorLoader {

		private final String query;

		public SearchLoader( String query, Context context ) {
			super( context );
			this.query = query;
		}

		public SearchLoader( String query, Context context, Uri uri, String [] projection, String selection, String [] selectionArgs, String sortOrder ) {
			super( context, uri, projection, selection, selectionArgs, sortOrder );
			this.query = query;
		}

		@Override
		public Cursor loadInBackground() {
			return search( getContext(), query );
		}

	}

	public static final int SOURCE_LOCAL_CONTACTS = 0x08;
	public static final int SOURCE_REMOTE_CONTACTS = 0x04;
	public static final int SOURCE_SILENT_CONTACTS = 0x02;
	public static final int SOURCE_SYSTEM_CONTACTS = 0x01;

	public static final String ID = "_id";
	public static final String DISPLAY_NAME = "display_name";
	public static final String USERNAME = "username";
	public static final String SOURCE = "source";
	public static final String PHONE_NUMBER = "phone_number";
	public static final String JID = "jid";
	public static final String AVATAR = "avatar";
	public static final String FAVORITE = "favorite";

	public static final String [] PROJECTION = {
		ID,
		USERNAME,
		DISPLAY_NAME,
		SOURCE,
		PHONE_NUMBER,
		JID,
		AVATAR,
		FAVORITE
	};

	private static void appendResults( ContactRepository in, Cursor intermediate, Set<Result> out, int source ) {
		if( intermediate != null ) {
			while( intermediate.moveToNext() ) {
				Contact contact = in.getContact( intermediate );
				Result result = getResult( contact, source );
				if( result != null ) {
					out.add( result );
				}
			}
		}
	}

	private static void appendResults( List<Conversation> intermediate, Set<Result> out, int source ) {
		if( intermediate != null ) {
			int size = intermediate.size();
			for( int i = 0; i < size; i++ ) {
				Contact contact = intermediate.get( i ).getPartner();
				Result result = getResult( contact, source );
				if( result != null ) {
					out.add( result );
				}
			}
		}
	}

	private static Cursor collect( Set<Result> results ) {
		int size = results.size();
		MatrixCursor cursor = new MatrixCursor( PROJECTION, size );
		for( Result result : results ) {
			if( result.username == null ) {
				continue;
			}
			long id = result.username.hashCode();
			cursor.newRow().add( Long.valueOf( id ) ).add( result.username ).add( result.displayName ).add( Integer.valueOf( result.source ) );
		}
		return cursor;
	}

	public static Contact getContact( Cursor cursor ) {
		Contact contact = new Contact();
		contact.setUsername( getUsername( cursor ) );
		contact.setAlias( getDisplayName( cursor ) );
		return contact;
	}

	public static String getDisplayName( Cursor cursor ) {
		return cursor != null && !cursor.isClosed() ? cursor.getString( cursor.getColumnIndex( DISPLAY_NAME ) ) : null;
	}

	private static ConversationRepository getLocalContacts( Context context ) {
		return SilentTextApplication.from( context ).getConversations();
	}

	private static Result getResult( Contact contact, int source ) {

		Result result = new Result();

		result.username = contact.getUsername();

		if( !isUsernameAcceptable( result.username ) ) {
			return null;
		}

		result.displayName = contact.getAlias();
		result.source = source;

		return result;

	}

	private static ContactRepository getSilentContacts( Context context ) {
		if( SilentContactRepositoryV2.supports( context ) ) {
			return new SilentContactRepositoryV2( context.getContentResolver() );
		} else if( SilentContactRepositoryV1.supports( context ) ) {
			return new SilentContactRepositoryV1( context.getContentResolver() );
		}
		return null;
	}

	public static ContactRepository getSource( Context context, Cursor cursor ) {
		int source = getSource( cursor );
		switch( source ) {
			case ContactProvider.SOURCE_SYSTEM_CONTACTS:
				return SilentTextApplication.from( context ).getSystemContacts();
			case ContactProvider.SOURCE_SILENT_CONTACTS:
				return SilentTextApplication.from( context ).getSilentContacts();
		}
		return null;
	}

	public static int getSource( Cursor cursor ) {
		return cursor != null && !cursor.isClosed() ? cursor.getInt( cursor.getColumnIndex( SOURCE ) ) : 0;
	}

	private static SystemContactRepository getSystemContacts( Context context ) {
		return new SystemContactRepository( context.getContentResolver() );
	}

	public static String getUsername( Cursor cursor ) {
		return cursor != null && !cursor.isClosed() ? cursor.getString( cursor.getColumnIndex( USERNAME ) ) : null;
	}

	private static boolean isUsernameAcceptable( String username ) {
		return username != null && username.contains( ServiceConfiguration.getInstance().getXMPPServiceName() );
	}

	private static void list( ContactRepository in, Set<Result> out, int source ) {
		if( in != null ) {
			appendResults( in, in.list(), out, source );
		}
	}

	public static Cursor list( Context context ) {

		Set<Result> results = new TreeSet<Result>();

		list( getLocalContacts( context ), results, SOURCE_LOCAL_CONTACTS );
		list( getSystemContacts( context ), results, SOURCE_SYSTEM_CONTACTS );
		list( getSilentContacts( context ), results, SOURCE_SILENT_CONTACTS );

		return collect( results );

	}

	private static void list( ConversationRepository in, Set<Result> out, int source ) {
		if( in != null ) {
			appendResults( in.list(), out, source );
		}
	}

	public static CursorLoader loaderForAll( Context context ) {
		return new AllLoader( context );
	}

	public static CursorLoader loaderForSearch( Context context, String query ) {
		return new SearchLoader( query, context );
	}

	public static Cursor search( Context context, String query ) {

		Set<Result> results = new TreeSet<Result>();

		search( query, getLocalContacts( context ), results, SOURCE_LOCAL_CONTACTS );
		search( query, getSystemContacts( context ), results, SOURCE_SYSTEM_CONTACTS );
		search( query, getSilentContacts( context ), results, SOURCE_SILENT_CONTACTS );

		return collect( results );

	}

	private static void search( String query, ContactRepository in, Set<Result> out, int source ) {
		if( in != null ) {
			appendResults( in, in.search( query ), out, source );
		}
	}

	private static List<Conversation> search( String query, ConversationRepository in ) {
		String q = query.toLowerCase( Locale.ENGLISH );
		List<Conversation> out = new ArrayList<Conversation>();
		for( Conversation conversation : in.list() ) {
			Contact contact = conversation.getPartner();
			String username = contact.getUsername().toLowerCase( Locale.ENGLISH ).replaceAll( "^(.+)@(.+)$", "$1" );
			String alias = contact.getAlias().toLowerCase( Locale.ENGLISH );
			if( username.contains( q ) || alias.contains( q ) ) {
				out.add( conversation );
			}
		}
		return out;
	}

	private static void search( String query, ConversationRepository in, Set<Result> out, int source ) {
		if( in != null ) {
			appendResults( search( query, in ), out, source );
		}
	}

}
