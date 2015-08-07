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
package com.silentcircle.silenttext.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;

import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.model.Contact;
import com.silentcircle.silenttext.model.ContactComparator;
import com.silentcircle.silenttext.provider.ContactProvider;
import com.silentcircle.silenttext.util.StringUtils;

public class ListContactsTask extends AsyncTask<Context, Void, List<Contact>> {

	@Override
	protected List<Contact> doInBackground( Context... args ) {

		Context context = args[0];
		Set<Contact> uniques = new HashSet<Contact>();

		Cursor cursor = ContactProvider.list( context );

		if( cursor != null ) {
			while( cursor.moveToNext() ) {
				Contact contact = ContactProvider.getContact( cursor );
				if( !uniques.contains( contact ) ) {
					uniques.add( contact );
				}
			}
			cursor.close();
		}

		List<Contact> contacts = new ArrayList<Contact>();

		SilentTextApplication application = SilentTextApplication.from( context );

		for( Contact contact : uniques ) {
			String displayName = application.getDisplayName( contact.getUsername() );
			if( StringUtils.isMinimumLength( displayName, 1 ) ) {
				contact.setAlias( displayName );
			}
			contacts.add( contact );
		}

		Collections.sort( contacts, ContactComparator.getInstance() );

		return contacts;

	}

}
