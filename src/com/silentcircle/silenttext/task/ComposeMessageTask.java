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
package com.silentcircle.silenttext.task;

import org.jivesoftware.smack.packet.Message.Type;
import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;
import android.os.AsyncTask;

import com.silentcircle.silenttext.model.Conversation;
import com.silentcircle.silenttext.model.MessageState;
import com.silentcircle.silenttext.model.event.Message;
import com.silentcircle.silenttext.model.event.OutgoingMessage;
import com.silentcircle.silenttext.repository.ConversationRepository;

public class ComposeMessageTask extends AsyncTask<String, Void, Message> {

	private final boolean shouldRequestDeliveryNotification;
	private final Conversation conversation;
	private final ConversationRepository repository;
	private final String self;
	private final Location location;

	public ComposeMessageTask( String self, Conversation conversation, ConversationRepository repository, Location location, boolean shouldRequestDeliveryNotification ) {
		this.self = self;
		this.conversation = conversation;
		this.repository = repository;
		this.location = location;
		this.shouldRequestDeliveryNotification = shouldRequestDeliveryNotification;
	}

	private Message composeMessagePacket( String plaintext ) {

		Message message = new OutgoingMessage( self, plaintext );
		message.setConversationID( conversation.getPartner().getUsername() );

		message.setId( new org.jivesoftware.smack.packet.Message( conversation.getPartner().getUsername(), Type.chat ).getPacketID() );
		if( conversation.hasBurnNotice() ) {
			message.setBurnNotice( conversation.getBurnDelay() );
		}

		try {

			JSONObject json = new JSONObject();

			json.put( "message", plaintext );

			if( shouldRequestDeliveryNotification ) {
				json.put( "request_receipt", 1 );
			}

			if( conversation.hasBurnNotice() ) {
				json.put( "shred_after", conversation.getBurnDelay() );
			}

			if( location != null ) {

				JSONObject jsonLocation = new JSONObject();

				jsonLocation.put( "latitude", location.getLatitude() );
				jsonLocation.put( "longitude", location.getLongitude() );
				jsonLocation.put( "timestamp", location.getTime() );
				jsonLocation.put( "altitude", location.getAltitude() );
				jsonLocation.put( "horizontalAccuracy", location.getAccuracy() );
				jsonLocation.put( "verticalAccuracy", location.getAccuracy() );

				json.put( "location", jsonLocation.toString() );

			}

			message.setText( json.toString() );

		} catch( JSONException exception ) {
			// In practice, this superfluous exception can never actually happen.
		}

		message.setState( isNoteToSelf() ? MessageState.SENT : MessageState.COMPOSED );

		return message;

	}

	@Override
	protected Message doInBackground( String... messages ) {
		for( String messageText : messages ) {
			return save( composeMessagePacket( messageText ) );
		}
		return null;
	}

	private boolean isNoteToSelf() {
		return self.equals( conversation.getPartner().getUsername() );
	}

	private Message save( Message message ) {
		repository.historyOf( conversation ).save( message );
		return message;
	}

}
