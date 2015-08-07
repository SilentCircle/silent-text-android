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
package com.silentcircle.silenttext.listener;

import android.location.Location;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.silentcircle.silenttext.SCimpBridge;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.location.LocationObserver;
import com.silentcircle.silenttext.location.OnLocationReceivedListener;
import com.silentcircle.silenttext.model.Conversation;
import com.silentcircle.silenttext.model.event.Message;
import com.silentcircle.silenttext.repository.ConversationRepository;
import com.silentcircle.silenttext.task.ComposeMessageTask;
import com.silentcircle.silenttext.util.AsyncUtils;

public class SendMessageOnClick implements OnClickListener, OnLocationReceivedListener {

	private static SilentTextApplication getApplication( View view ) {
		return SilentTextApplication.from( view.getContext() );
	}

	protected final TextView source;
	protected final String username;
	protected final Conversation conversation;
	protected final ConversationRepository repository;
	protected final SCimpBridge encryptor;
	private String text;
	private final boolean shouldRequestDeliveryNotification;

	public SendMessageOnClick( TextView source, String username, Conversation conversation, ConversationRepository repository, SCimpBridge encryptor, boolean shouldRequestDeliveryNotification ) {
		this.source = source;
		this.username = username;
		this.conversation = conversation;
		this.repository = repository;
		this.encryptor = encryptor;
		this.shouldRequestDeliveryNotification = shouldRequestDeliveryNotification;
	}

	@Override
	public void onClick( View button ) {

		if( !getApplication( button ).isUserKeyUnlocked() ) {
			return;
		}

		text = source.getText().toString();

		if( text == null || "".equals( text.trim() ) ) {
			return;
		}

		source.setText( null );

		if( conversation.isLocationEnabled() ) {
			LocationObserver.observe( source.getContext(), this );
			return;
		}

		onLocationReceived( null );

	}

	@Override
	public void onLocationReceived( Location location ) {

		AsyncUtils.execute( new ComposeMessageTask( username, conversation, repository, location, shouldRequestDeliveryNotification ) {

			@Override
			protected void onPostExecute( Message message ) {
				withMessage( message );
			}

		}, text );

	}

	@Override
	public void onLocationUnavailable() {
		onLocationReceived( null );
	}

	/**
	 * Allows for custom handling of the outgoing message.
	 * 
	 * @param message
	 *            the outgoing message being sent.
	 */
	protected void withMessage( Message message ) {
		// By default, do nothing.
	}

}
