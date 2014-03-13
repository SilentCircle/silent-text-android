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

import java.lang.ref.SoftReference;
import java.util.List;

import android.content.Context;

import com.silentcircle.scimp.DefaultPacketOutput;
import com.silentcircle.scimp.NamedKeyPair;
import com.silentcircle.scimp.NativePacket;
import com.silentcircle.scimp.PacketInput;
import com.silentcircle.scimp.model.ResourceState;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.crypto.CryptoUtils;
import com.silentcircle.silenttext.log.Log;
import com.silentcircle.silenttext.model.Contact;
import com.silentcircle.silenttext.model.Conversation;
import com.silentcircle.silenttext.repository.ConversationRepository;
import com.silentcircle.silenttext.repository.Repository;
import com.silentcircle.silenttext.util.IOUtils;

public class NativeBridge {

	protected String identity;
	private final SoftReference<Context> contextReference;
	private final PacketInput in;

	protected final Log log = new Log( "SCimpBridge" );

	public NativeBridge( Context context ) {
		contextReference = new SoftReference<Context>( context );
		in = new NativePacket( context );
	}

	public void connect( String partner ) {

		log.debug( "CONNECT to:%s", partner );
		byte [] storageKey = getStorageKey( partner );
		String localUserID = getIdentity();
		ResourceState resourceState = getResourceState( partner );
		String context = resourceState == null ? null : resourceState.getState();

		if( resourceState != null && resourceState.isSecure() ) {
			// We've already completed a DH key exchange, and we're just negotiating new symmetric
			// keys.
			getInput().connect( storageKey, null, localUserID, partner, context );
			return;
		}

		byte [] privateKey = getPrivateKey();

		if( privateKey != null ) {

			byte [] publicKey = getPublicKey( partner );

			if( publicKey != null ) {
				// We have both a public key and a private key, and we're not yet DH-secured.
				// Let's just passively let PKI handle it.
				return;
			}

		}

		// Partner doesn't have a public key available for PKI, and we haven't yet completed a DH
		// key exchange, so let's trigger one now.
		getInput().connect( storageKey, null, localUserID, partner, context );

	}

	public void decrypt( String partner, String messageID, String message, boolean notifiable, boolean badgeworthy ) {
		log.debug( "DECRYPT from:%s id:%s\n%s", partner, messageID, message, Boolean.valueOf( notifiable ), Boolean.valueOf( badgeworthy ) );
		byte [] storageKey = getStorageKey( partner );
		String localUserID = getIdentity();
		ResourceState resourceState = getResourceState( partner );
		decrypt( partner, messageID, message, notifiable, badgeworthy, storageKey, localUserID, resourceState );
	}

	private void decrypt( String partner, String messageID, String message, boolean notifiable, boolean badgeworthy, byte [] storageKey, String localUserID, ResourceState resourceState ) {

		String context = resourceState == null ? null : resourceState.getState();

		byte [] privateKey = getPrivateKey();
		if( privateKey != null ) {
			byte [] publicKey = getPublicKey( partner );
			if( publicKey != null ) {
				getInput().receivePacketPKI( storageKey, getLocalStorageKey(), privateKey, publicKey, messageID, localUserID, partner, message, context, notifiable, badgeworthy );
				return;
			}
		}

		getInput().receivePacket( storageKey, messageID, localUserID, partner, message, context, notifiable, badgeworthy );

	}

	public void encrypt( String partner, String messageID, String message, boolean notifiable, boolean badgeworthy ) {
		log.debug( "ENCRYPT to:%s id:%s\n%s", partner, messageID, message, Boolean.valueOf( notifiable ), Boolean.valueOf( badgeworthy ) );
		byte [] storageKey = getStorageKey( partner );
		String localUserID = getIdentity();
		ResourceState resourceState = getResourceState( partner );
		String context = resourceState == null ? null : resourceState.getState();
		encrypt( partner, messageID, message, notifiable, badgeworthy, storageKey, localUserID, resourceState, context );
	}

	private void encrypt( String partner, String messageID, String message, boolean notifiable, boolean badgeworthy, byte [] storageKey, String localUserID, ResourceState resourceState, String context ) {

		if( resourceState != null && resourceState.isSecure() ) {
			getInput().sendPacket( storageKey, messageID, localUserID, partner, message, context, notifiable, badgeworthy );
			return;
		}

		byte [] privateKey = getPrivateKey();
		if( privateKey != null ) {
			byte [] publicKey = getPublicKey( partner );
			if( publicKey != null ) {
				getInput().sendPacketPKI( storageKey, getLocalStorageKey(), privateKey, publicKey, messageID, localUserID, partner, message, context, notifiable, badgeworthy );
			}
		}

	}

	private SilentTextApplication getApplication() {
		return (SilentTextApplication) getContext().getApplicationContext();
	}

	protected Context getContext() {
		return contextReference.get();
	}

	protected ConversationRepository getConversations() {
		return getApplication().getConversations();
	}

	public String getIdentity() {
		return identity;
	}

	public PacketInput getInput() {
		return in;
	}

	private byte [] getLocalStorageKey() {
		return getApplication().getLocalStorageKey();
	}

	private byte [] getPrivateKey() {
		NamedKeyPair pair = getApplication().getOrCreateKeyPair();
		if( pair != null ) {
			testSCKeyDeserialize( new String( pair.getPublicKey() ) );
			return pair.getPrivateKey();
		}
		return null;
	}

	public byte [] getPublicKey( String partner ) {
		return getApplication().getPublicKey( partner );
	}

	private ResourceState getResourceState( String partner ) {
		ConversationRepository conversations = getConversations();
		Conversation conversation = conversations.findByPartner( partner );
		if( conversation == null ) {
			// log.warn( "Conversation not found: %s", partner );
			return null;
		}
		Repository<ResourceState> resourceStates = conversations.contextOf( conversation );
		String resource = conversation.getPartner().getDevice();
		ResourceState resourceState = resourceStates.findById( resource );
		if( resourceState == null ) {
			// log.warn( "State not found: %s/%s", partner, resource );
			resourceState = resourceStates.findById( "" );
			if( resourceState == null ) {
				// log.warn( "State not found: %s", partner );
				return null;
			}
			if( resource != null ) {
				// log.info( "State upgraded: %s/%s", partner, resource );
				resourceStates.remove( resourceState );
				resourceState.setResource( resource );
				resourceStates.save( resourceState );
			}
		} else {
			// log.debug( "Previous state found: %s/%s", partner, resource );
		}
		return resourceState;
	}

	private byte [] getStorageKey( String partner ) {
		ConversationRepository conversations = getConversations();
		Conversation conversation = conversations.findByPartner( partner );
		if( conversation == null ) {
			conversation = new Conversation();
			conversation.setPartner( new Contact( partner ) );
		}
		if( conversation.getStorageKey() == null ) {
			byte [] storageKey = CryptoUtils.randomBytes( 64 );
			migrateStorageKey( conversations, conversation, storageKey );
			conversation.setStorageKey( storageKey );
			conversations.save( conversation );
		}
		return conversation.getStorageKey();
	}

	private void migrateStorageKey( ConversationRepository conversations, Conversation conversation, byte [] storageKey ) {

		Repository<ResourceState> resourceStates = conversations.contextOf( conversation );

		if( resourceStates != null && resourceStates.exists() ) {

			byte [] oldStorageKey = conversation.getId().getBytes();
			byte [] oldStorageKeyBytes = new byte [64];
			IOUtils.copy( oldStorageKey, 0, oldStorageKeyBytes, 0, 64 );

			List<ResourceState> states = resourceStates.list();
			for( int i = 0; i < states.size(); i++ ) {
				getInput().resetStorageKey( oldStorageKeyBytes, states.get( i ).getState(), storageKey );
			}

		}

	}

	public void onCreate() {
		PacketInput input = getInput();
		input.onCreate();
		input.setPacketOutput( new DefaultPacketOutput( getContext() ) );
	}

	public void onDestroy() {
		PacketInput input = getInput();
		input.setPacketOutput( null );
		input.onDestroy();
	}

	public void setIdentity( String identity ) {
		this.identity = identity;
	}

	public void testSCKeyDeserialize( String serializedKey ) {
		int result = ( (NativePacket) getInput() ).testSCKeyDeserialize( serializedKey );
		log.debug( "#testSCKeyDeserialize result:%d\n%s", Integer.valueOf( result ), serializedKey );
	}

}
