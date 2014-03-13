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
package com.silentcircle.silenttext.view.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;

import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.repository.ContactRepository;
import com.silentcircle.silenttext.view.ContactView;

public class ContactSuggestionAdapter extends CursorAdapter {

	private final ContactRepository repository;

	public ContactSuggestionAdapter( Context context, Cursor cursor, int flags ) {
		super( context, cursor, flags );
		repository = SilentTextApplication.from( context ).getContacts();
	}

	@Override
	public void bindView( View view, Context context, Cursor cursor ) {
		if( cursor == null || cursor.isClosed() ) {
			return;
		}
		view.setTag( repository.getContact( cursor ) );
	}

	@Override
	public CharSequence convertToString( Cursor cursor ) {
		if( cursor == null || cursor.isClosed() ) {
			return null;
		}
		return repository.getContact( cursor ).getUsername().replaceAll( "^([^@]+)@([^@]+)$", "$1" );
	}

	@Override
	public View newView( Context context, Cursor cursor, ViewGroup parent ) {
		ContactView view = (ContactView) View.inflate( context, R.layout.contact_suggestion, null );
		if( cursor == null || !cursor.isClosed() ) {
			view.setTag( repository.getContact( cursor ) );
		}
		return view;
	}

}
