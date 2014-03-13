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
package com.silentcircle.scimp;

import java.lang.ref.SoftReference;

import android.content.Context;

import com.silentcircle.silenttext.application.SilentTextApplication;

public class NativePacket implements PacketInput, PacketOutput {

	private static boolean ready = false;

	private static void load() {
		if( ready ) {
			return;
		}
		System.loadLibrary( "yajl" );
		System.loadLibrary( "tommath" );
		System.loadLibrary( "tomcrypt" );
		System.loadLibrary( "scimp" );
		System.loadLibrary( "scimp-jni" );
		ready = true;
	}

	private final SoftReference<Context> contextReference;
	private PacketOutput packetOutput;
	private final KeyGenerator keyGenerator = new NativeKeyGenerator();

	public NativePacket( Context context ) {
		load();
		contextReference = new SoftReference<Context>( context );
	}

	@Override
	public native void connect( byte [] storageKey, String packetID, String localUserID, String remoteUserID, String context );

	@Override
	public PacketOutput getPacketOutput() {
		return packetOutput;
	}

	public byte [] getPrivateKey( String locator ) {
		try {
			return SilentTextApplication.from( contextReference.get() ).getKeyPair( locator ).getPrivateKey();
		} catch( Throwable exception ) {
			return null;
		}
	}

	/**
	 * @param locator
	 */
	public byte [] getPrivateKeyStorageKey( String locator ) {
		try {
			return SilentTextApplication.from( contextReference.get() ).getLocalStorageKey();
		} catch( Throwable exception ) {
			return null;
		}
	}

	@Override
	public void onConnect( byte [] storageKey, String packetID, String localUserID, String remoteUserID, String context, String secret ) {
		getPacketOutput().onConnect( storageKey, packetID, localUserID, remoteUserID, context, secret );
	}

	@Override
	public native void onCreate();

	@Override
	public native void onDestroy();

	@Override
	public void onError( byte [] storageKey, String packetID, String localUserID, String remoteUserID, int errorCode ) {
		getPacketOutput().onError( storageKey, packetID, localUserID, remoteUserID, errorCode );
	}

	@Override
	public void onReceivePacket( byte [] storageKey, String packetID, String localUserID, String remoteUserID, String data, String context, String secret, boolean notifiable, boolean badgeworthy ) {
		getPacketOutput().onReceivePacket( storageKey, packetID, localUserID, remoteUserID, data, context, secret, notifiable, badgeworthy );
	}

	@Override
	public void onSendPacket( byte [] storageKey, String packetID, String localUserID, String remoteUserID, String data, String context, String secret, boolean notifiable, boolean badgeworthy ) {
		getPacketOutput().onSendPacket( storageKey, packetID, localUserID, remoteUserID, data, context, secret, notifiable, badgeworthy );
	}

	@Override
	public void onStateTransition( byte [] storageKey, String localUserID, String remoteUserID, int toState ) {
		getPacketOutput().onStateTransition( storageKey, localUserID, remoteUserID, toState );
	}

	@Override
	public void onWarning( byte [] storageKey, String packetID, String localUserID, String remoteUserID, int warningCode ) {
		getPacketOutput().onWarning( storageKey, packetID, localUserID, remoteUserID, warningCode );
	}

	@Override
	public native void receivePacket( byte [] storageKey, String packetID, String localUserID, String remoteUserID, String data, String context, boolean notifiable, boolean badgeworthy );

	@Override
	public native void receivePacketPKI( byte [] storageKey, byte [] privateKeyStorageKey, byte [] privateKey, byte [] publicKey, String packetID, String localUserID, String remoteUserID, String data, String context, boolean notifiable, boolean badgeworthy );

	@Override
	public native void resetStorageKey( byte [] oldStorageKey, String context, byte [] newStorageKey );

	@Override
	public native void sendPacket( byte [] storageKey, String packetID, String localUserID, String remoteUserID, String data, String context, boolean notifiable, boolean badgeworthy );

	@Override
	public native void sendPacketPKI( byte [] storageKey, byte [] privateKeyStorageKey, byte [] privateKey, byte [] publicKey, String packetID, String localUserID, String remoteUserID, String data, String context, boolean notifiable, boolean badgeworthy );

	@Override
	public void setPacketOutput( PacketOutput packetOutput ) {
		this.packetOutput = packetOutput;
	}

	public native int testSCKeyDeserialize( String serializedKey );

}
