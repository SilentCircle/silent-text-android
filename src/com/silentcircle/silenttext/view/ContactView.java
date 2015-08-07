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
package com.silentcircle.silenttext.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.model.Contact;
import com.silentcircle.silenttext.provider.ContactProvider;
import com.silentcircle.silenttext.repository.ContactRepository;
import com.silentcircle.silenttext.util.AsyncUtils;
import com.silentcircle.silenttext.util.StringUtils;

public class ContactView extends LinearLayout {

	class UpdateDisplayNameTask extends AsyncTask<String, Void, String> {

		private final ContactRepository repository;
		private final Contact contact;

		UpdateDisplayNameTask( ContactRepository repository, Contact contact ) {
			this.repository = repository;
			this.contact = contact;
		}

		@Override
		protected String doInBackground( String... args ) {
			return SilentTextApplication.from( getContext() ).getDisplayName( args[0], repository );
		}

		@Override
		protected void onPostExecute( String displayName ) {

			if( contact != getTag() ) {
				return;
			}

			if( StringUtils.isMinimumLength( displayName, 1 ) ) {
				contact.setAlias( displayName );
				getViews().displayName.setText( contact.getAlias() );
			}

		}
	}

	static class Views {

		public TextView displayName;
		public TextView username;
		public AvatarView photo;

	}

	private static String stylizeUsername( String username ) {
		return username == null ? null : String.format( "@%s", username.substring( 0, username.indexOf( '@' ) ) );
	}

	private Contact contact;
	private Views views;

	public ContactView( Context context ) {
		super( context );
	}

	public ContactView( Context context, AttributeSet attrs ) {
		super( context, attrs );
	}

	@TargetApi( Build.VERSION_CODES.HONEYCOMB )
	public ContactView( Context context, AttributeSet attrs, int defStyle ) {
		super( context, attrs, defStyle );
	}

	public Contact getContact() {
		return contact;
	}

	protected Views getViews() {
		if( views == null ) {
			views = new Views();
			views.displayName = (TextView) findViewById( R.id.alias );
			views.username = (TextView) findViewById( R.id.username );
			views.photo = (AvatarView) findViewById( R.id.avatar );
		}
		return views;
	}

	public void setContact( Contact contact ) {
		setContact( contact, SilentTextApplication.from( getContext() ).getContacts() );
	}

	public void setContact( final Contact contact, final ContactRepository repository ) {

		this.contact = contact;

		Views views = getViews();

		views.displayName.setText( contact.getAlias() );
		views.username.setText( stylizeUsername( contact.getUsername() ) );
		views.photo.setContact( contact, repository );

		AsyncUtils.execute( new UpdateDisplayNameTask( repository, contact ), contact.getUsername() );

	}

	public void setContact( Cursor cursor ) {

		Contact contact = new Contact();

		contact.setUsername( ContactProvider.getUsername( cursor ) );
		contact.setAlias( ContactProvider.getDisplayName( cursor ) );

		setContact( contact, ContactProvider.getSource( getContext(), cursor ) );

	}

	@Override
	public void setTag( Object tag ) {
		super.setTag( tag );
		if( tag instanceof Contact ) {
			setContact( (Contact) tag );
		}
		if( tag instanceof Cursor ) {
			setContact( (Cursor) tag );
		}
	}

}
