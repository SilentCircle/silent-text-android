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
package com.silentcircle.silenttext.fragment;

import java.util.List;

import android.app.Activity;

import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.model.Contact;
import com.silentcircle.silenttext.task.ListContactsTask;
import com.silentcircle.silenttext.util.AsyncUtils;

public class ContactListFragment extends ListFragment {

	public static interface Callback {

		public void onBeginLoading();

		public void onContactSelected( Contact contact );

		public void onFinishLoading();

	}

	protected void dispatchOnBeginLoading() {
		Callback callback = getCallback();
		if( callback != null ) {
			callback.onBeginLoading();
		}
	}

	protected void dispatchOnFinishLoading() {
		Callback callback = getCallback();
		if( callback != null ) {
			callback.onFinishLoading();
		}
	}

	protected Callback getCallback() {
		Activity activity = getActivity();
		return activity instanceof Callback ? (Callback) activity : null;
	}

	@Override
	public void onItemClick( Object contact ) {
		Callback callback = getCallback();
		if( callback != null && contact instanceof Contact ) {
			callback.onContactSelected( (Contact) contact );
		}
	}

	@Override
	public void onResume() {

		super.onResume();

		List<?> items = getItems();

		if( items != null ) {
			setItems( R.layout.contact, items );
			return;
		}

		dispatchOnBeginLoading();

		AsyncUtils.execute( new ListContactsTask() {

			@Override
			protected void onPostExecute( List<Contact> contacts ) {
				setItems( R.layout.contact, contacts );
				dispatchOnFinishLoading();
			}

		}, getActivity() );

	}

}
