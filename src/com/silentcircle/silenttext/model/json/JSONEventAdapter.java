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
package com.silentcircle.silenttext.model.json;

import org.json.JSONException;
import org.json.JSONObject;

import com.silentcircle.silenttext.model.MessageState;
import com.silentcircle.silenttext.model.SCIMPError;
import com.silentcircle.silenttext.model.event.ErrorEvent;
import com.silentcircle.silenttext.model.event.Event;
import com.silentcircle.silenttext.model.event.EventType;
import com.silentcircle.silenttext.model.event.HandshakeEvent;
import com.silentcircle.silenttext.model.event.IncomingMessage;
import com.silentcircle.silenttext.model.event.Message;
import com.silentcircle.silenttext.model.event.OutgoingMessage;
import com.silentcircle.silenttext.model.event.ResourceChangeEvent;
import com.silentcircle.silenttext.model.event.WarningEvent;

public class JSONEventAdapter extends JSONAdapter {

	private static Event adapt( JSONObject from, ErrorEvent to ) {
		adapt( from, (Event) to );
		to.setError( SCIMPError.valueOf( getString( from, "error" ) ) );
		return to;
	}

	private static Event adapt( JSONObject from, Event to ) {

		to.setConversationID( getString( from, "conversation_id" ) );
		to.setId( getString( from, "id" ) );
		to.setText( getString( from, "text" ) );
		to.setTime( getLong( from, "time" ) );

		return to;

	}

	private static Event adapt( JSONObject from, Message to ) {

		adapt( from, (Event) to );

		to.setBurnNotice( getInt( from, "burn_notice", -1 ) );
		to.setCiphertext( getString( from, "ciphertext" ) );
		to.setSender( getString( from, "sender" ) );
		to.setState( MessageState.valueOf( getString( from, "state", MessageState.UNKNOWN.name() ) ) );
		to.setExpirationTime( getLong( from, "expiration_time", Long.MAX_VALUE ) );

		return to;

	}

	private static Event adapt( JSONObject from, WarningEvent to ) {
		adapt( from, (Event) to );
		to.setWarning( SCIMPError.valueOf( getString( from, "warning" ) ) );
		return to;
	}

	public JSONObject adapt( Event event ) {

		JSONObject json = new JSONObject();

		try {

			json.put( "type", EventType.forClass( event.getClass() ).name() );

			json.put( "conversation_id", event.getConversationID() );
			json.put( "id", event.getId() );
			json.put( "text", event.getText() );
			json.put( "time", event.getTime() );

			if( event instanceof Message ) {

				Message message = (Message) event;

				json.put( "burn_notice", message.getBurnNotice() );
				json.put( "ciphertext", message.getCiphertext() );
				json.put( "sender", message.getSender() );
				json.put( "state", message.getState().name() );
				json.put( "expiration_time", message.getExpirationTime() );

			}

			if( event instanceof ErrorEvent ) {
				ErrorEvent error = (ErrorEvent) event;
				json.put( "error", error.getError().name() );
			}

			if( event instanceof WarningEvent ) {
				WarningEvent warning = (WarningEvent) event;
				json.put( "warning", warning.getWarning().name() );
			}

		} catch( JSONException exception ) {
			// This should never happen because we control all of the keys.
		}

		return json;

	}

	public Event adapt( JSONObject json ) {
		switch( EventType.valueOf( getString( json, "type" ) ) ) {
			case MESSAGE:
				return adapt( json, new Message() );
			case ERROR_EVENT:
				return adapt( json, new ErrorEvent() );
			case HANDSHAKE_EVENT:
				return adapt( json, new HandshakeEvent() );
			case INCOMING_MESSAGE:
				return adapt( json, new IncomingMessage() );
			case OUTGOING_MESSAGE:
				return adapt( json, new OutgoingMessage() );
			case RESOURCE_CHANGE_EVENT:
				return adapt( json, new ResourceChangeEvent() );
			case WARNING_EVENT:
				return adapt( json, new WarningEvent() );
			case EVENT:
			default:
				return adapt( json, new Event() );
		}
	}

}
