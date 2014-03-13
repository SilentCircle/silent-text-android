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
package com.silentcircle.silenttext;

import java.util.UUID;

import com.silentcircle.silenttext.model.Contact;
import com.silentcircle.silenttext.model.Conversation;
import com.silentcircle.silenttext.model.Credential;
import com.silentcircle.silenttext.model.MessageState;
import com.silentcircle.silenttext.model.Server;
import com.silentcircle.silenttext.model.event.Event;
import com.silentcircle.silenttext.model.event.Message;

public class MockUtils {

	public static Contact mockContact() {

		Contact contact = new Contact();

		contact.setUsername( "john.smith@silentcircle.com" );
		contact.setAlias( "John Q. Smith" );
		contact.setDevice( "Flip Phone" );

		return contact;

	}

	public static Conversation mockConversation() {

		Conversation conversation = new Conversation();

		conversation.setId( UUID.randomUUID().toString() );
		conversation.setBurnNotice( true );
		conversation.setBurnDelay( 12345 );
		conversation.setLocationEnabled( false );
		conversation.setPartner( mockContact() );

		return conversation;

	}

	public static Credential mockCredential() {

		Credential credential = new Credential();

		credential.setDomain( "silentcircle.com" );
		credential.setUsername( "john.smith@silentcircle.com" );
		credential.setPassword( "Password password is password." );

		return credential;

	}

	public static Event mockEvent() {
		return mockEvent( new Event() );
	}

	private static Event mockEvent( Event event ) {

		event.setId( UUID.randomUUID().toString() );
		event.setText( "The British are coming!" );
		event.setTime( 1234567890L );

		return event;

	}

	public static Message mockMessage() {
		return mockMessage( new Message() );
	}

	private static Message mockMessage( Message message ) {

		mockEvent( message );

		message.setBurnNotice( 123 );
		message.setCiphertext( "abracadabra" );
		message.setSender( "john.smith@silentcircle.com" );
		message.setState( MessageState.ENCRYPTED );

		return message;

	}

	public static Server mockServer() {

		Server server = new Server();

		server.setId( "test" );
		server.setCredential( mockCredential() );

		return server;

	}

}
