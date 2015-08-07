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
package com.silentcircle.silenttext.receiver;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.silentcircle.silenttext.Action;
import com.silentcircle.silenttext.Extra;
import com.silentcircle.silenttext.Manifest;
import com.silentcircle.silenttext.SCimpBridge;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.model.Conversation;
import com.silentcircle.silenttext.model.MessageState;
import com.silentcircle.silenttext.model.event.Event;
import com.silentcircle.silenttext.model.event.IncomingMessage;
import com.silentcircle.silenttext.model.event.Message;
import com.silentcircle.silenttext.model.event.OutgoingMessage;
import com.silentcircle.silenttext.repository.ConversationRepository;
import com.silentcircle.silenttext.repository.EventRepository;

public class PacketStateReceiver extends BroadcastReceiver {

	private static String createBurnRequestText( Event event ) {
		JSONObject json = new JSONObject();
		try {
			json.put( "request_burn", event.getId() );
		} catch( JSONException impossible ) {
			// Ignore this stupid exception.
		}
		return json.toString();
	}

	@Override
	public void onReceive( Context context, Intent intent ) {

		SilentTextApplication application = (SilentTextApplication) context.getApplicationContext();
		ConversationRepository conversations = application.getConversations();

		if( conversations == null || !conversations.exists() ) {
			return;
		}

		String partner = Extra.PARTNER.from( intent );
		Conversation conversation = conversations.findByPartner( partner );

		if( conversation == null ) {
			return;
		}

		EventRepository events = conversations.historyOf( conversation );
		Event event = events.findById( Extra.ID.from( intent ) );

		if( event instanceof Message ) {

			Message message = (Message) event;

			if( message instanceof OutgoingMessage ) {

				MessageState messageState = MessageState.forValue( Extra.STATE.getInt( intent ) );

				if( messageState != null && !MessageState.UNKNOWN.equals( messageState ) ) {
					message.setState( messageState );
					events.save( message );
				}

				SCimpBridge scimp = application.getSCimpBridge();

				switch( message.getState() ) {

					case COMPOSED:

						scimp.encrypt( partner, message.getId(), message.getText(), true, true );

						break;

					case BURNED:

						scimp.encrypt( partner, null, createBurnRequestText( event ), false, false );
						events.remove( event );
						application.removeAttachments( event );

						break;

					default:

						// Do nothing.

						break;

				}

			}

			if( message instanceof IncomingMessage ) {

				switch( message.getState() ) {

					case READ:

						context.sendBroadcast( Extra.SILENT.flag( Action.NOTIFY.intent() ), Manifest.permission.READ );

						break;

					case DECRYPTED:

						context.sendBroadcast( Action.NOTIFY.intent(), Manifest.permission.READ );

						Intent metadata = Action.RECEIVE_MESSAGE.intent();

						Extra.PARTNER.to( metadata, partner );
						Extra.TIMESTAMP.to( metadata, message.getTime() );

						context.sendBroadcast( metadata, Manifest.permission.IDENTIFY_SENDER );

						break;

					default:

						break;

				}

			}

		}

	}

}
