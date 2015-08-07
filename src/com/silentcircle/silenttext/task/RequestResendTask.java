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
package com.silentcircle.silenttext.task;

import java.lang.ref.SoftReference;
import java.util.UUID;

import android.content.Context;
import android.os.AsyncTask;

import com.silentcircle.scimp.DefaultPacketOutput;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.model.Conversation;
import com.silentcircle.silenttext.model.event.Event;
import com.silentcircle.silenttext.model.event.OutgoingMessage;
import com.silentcircle.silenttext.repository.ConversationRepository;
import com.silentcircle.silenttext.repository.EventRepository;

public class RequestResendTask extends AsyncTask<Void, Void, Void> {

	private final SoftReference<Context> contextReference;
	private final String packetID;
	private final String localUserID;
	private final String remoteUserID;

	public RequestResendTask( Context context, String packetID, String localUserID, String remoteUserID ) {
		contextReference = new SoftReference<Context>( context );

		this.packetID = packetID;
		this.localUserID = localUserID;
		this.remoteUserID = remoteUserID;
	}

	@Override
	protected Void doInBackground( Void... args ) {

		if( packetID != null ) {
			final ConversationRepository conversations = SilentTextApplication.from( contextReference.get() ).getConversations();
			final Conversation conversation = conversations.findByPartner( remoteUserID );

			EventRepository events = conversations.historyOf( conversation );
			Event event = new OutgoingMessage( localUserID, DefaultPacketOutput.requestResend( SilentTextApplication.from( contextReference.get() ), packetID ) );

			event.setId( UUID.randomUUID().toString() );
			event.setConversationID( remoteUserID );

			events.save( event );
		}

		return null;

	}
}
