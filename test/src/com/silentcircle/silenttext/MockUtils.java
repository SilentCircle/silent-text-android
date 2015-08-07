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
package com.silentcircle.silenttext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.silentcircle.api.model.Key;
import com.silentcircle.api.model.Signature;
import com.silentcircle.api.web.model.BasicKey;
import com.silentcircle.api.web.model.BasicSignature;
import com.silentcircle.silenttext.model.Contact;
import com.silentcircle.silenttext.model.Conversation;
import com.silentcircle.silenttext.model.Credential;
import com.silentcircle.silenttext.model.MessageState;
import com.silentcircle.silenttext.model.Server;
import com.silentcircle.silenttext.model.event.Event;
import com.silentcircle.silenttext.model.event.Message;

public class MockUtils {

	public static CharSequence mockAPIKey() {
		return UUID.randomUUID().toString();
	}

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

	public static CharSequence mockDeviceID() {
		return UUID.randomUUID().toString();
	}

	public static CharSequence mockDeviceName() {
		return UUID.randomUUID().toString();
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

	public static Key mockPublicKey() {
		BasicKey key = new BasicKey();
		key.setVersion( 1 );
		key.setSuite( "test" );
		key.setPublicKey( new byte [32] );
		key.setOwner( "alice@example.com" );
		key.setLocator( "abcdef0123456789abcdef01234567891029384756" );
		key.setCreationDate( System.currentTimeMillis() );
		key.setExpirationDate( key.getCreationDate() + 24000 );
		key.setComment( "This is a good comment." );
		key.setSignatures( mockSignatures() );
		return key;
	}

	public static CharSequence mockPushRegistrationToken() {
		return UUID.randomUUID().toString();
	}

	public static Server mockServer() {

		Server server = new Server();

		server.setId( "test" );
		server.setCredential( mockCredential() );

		return server;

	}

	public static Signature mockSignature() {
		BasicSignature signature = new BasicSignature();
		signature.setDate( System.currentTimeMillis() );
		signature.setHashList( "owner,locator,pubKey,start_date,expire_date" );
		signature.setData( new byte [40] );
		signature.setSignerLocator( "abcdef0123456789abcdef01234567891029384756" );
		return signature;
	}

	public static List<Signature> mockSignatures() {
		List<Signature> signatures = new ArrayList<Signature>();
		signatures.add( mockSignature() );
		return signatures;
	}

}
