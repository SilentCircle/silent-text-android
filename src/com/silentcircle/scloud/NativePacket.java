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
package com.silentcircle.scloud;

import com.silentcircle.scloud.listener.OnBlockDecryptedListener;
import com.silentcircle.scloud.listener.OnBlockEncryptedListener;

public class NativePacket implements PacketInput, OnBlockEncryptedListener, OnBlockDecryptedListener {

	private static boolean ready = false;

	private static void load() {
		if( ready ) {
			return;
		}
		System.loadLibrary( "sccrypto" );
		System.loadLibrary( "sccrypto-jni" );
		ready = true;
	}

	private OnBlockEncryptedListener onBlockEncrypted;
	private OnBlockDecryptedListener onBlockDecrypted;

	public NativePacket() {
		this( null, null );
	}

	public NativePacket( OnBlockDecryptedListener onBlockDecrypted ) {
		this( null, onBlockDecrypted );
	}

	public NativePacket( OnBlockEncryptedListener onBlockEncrypted ) {
		this( onBlockEncrypted, null );
	}

	public NativePacket( OnBlockEncryptedListener onBlockEncrypted, OnBlockDecryptedListener onBlockDecrypted ) {
		this.onBlockEncrypted = onBlockEncrypted;
		this.onBlockDecrypted = onBlockDecrypted;
		load();
	}

	@Override
	public native void decrypt( byte [] data, String key );

	@Override
	public native void encrypt( String context, String metaData, byte [] data );

	@Override
	public OnBlockDecryptedListener getOnBlockDecryptedListener() {
		return onBlockDecrypted;
	}

	@Override
	public OnBlockEncryptedListener getOnBlockEncryptedListener() {
		return onBlockEncrypted;
	}

	@Override
	public void onBlockDecrypted( byte [] data, byte [] metaData ) {
		OnBlockDecryptedListener listener = getOnBlockDecryptedListener();
		if( listener != null ) {
			listener.onBlockDecrypted( data, metaData );
		}
	}

	@Override
	public void onBlockEncrypted( String key, String locator, byte [] data ) {
		OnBlockEncryptedListener listener = getOnBlockEncryptedListener();
		if( listener != null ) {
			listener.onBlockEncrypted( key, locator, data );
		}
	}

	@Override
	public native void onCreate();

	@Override
	public native void onDestroy();

	@Override
	public void setOnBlockDecryptedListener( OnBlockDecryptedListener onBlockDecryptedListener ) {
		onBlockDecrypted = onBlockDecryptedListener;
	}

	@Override
	public void setOnBlockEncryptedListener( OnBlockEncryptedListener onBlockEncryptedListener ) {
		onBlockEncrypted = onBlockEncryptedListener;
	}

}
