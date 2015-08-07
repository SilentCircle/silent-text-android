/*
Copyright (C) 2014-2015, Silent Circle, LLC. All rights reserved.

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
package com.silentcircle.silenttext.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;

import com.silentcircle.keymngrsupport.KeyManagerSupport;
import com.silentcircle.keymngrsupport.KeyManagerSupport.KeyManagerListener;

public class ExternalKeyManagerV1 extends ExternalKeyManager implements KeyManagerListener {

	protected long keyManagerToken;

	public ExternalKeyManagerV1( ContentResolver contentResolver, PackageManager packageManager, String packageName, String applicationName, Locker locker ) {
		super( contentResolver, packageManager, packageName, applicationName, locker );
	}

	public ExternalKeyManagerV1( Context context, Locker locker ) {
		super( context, locker );
	}

	@Override
	public void deletePrivateKeyData( String tag ) {
		assertRegistered();
		KeyManagerSupport.deletePrivateKeyData( contentResolver, tag );
	}

	@Override
	public void deleteSharedKeyData( String tag ) {
		assertRegistered();
		KeyManagerSupport.deleteSharedKeyData( contentResolver, tag );
	}

	@Override
	public String getDeviceAuthDataTag() {
		return KeyManagerSupport.DEV_AUTH_DATA_TAG;
	}

	@Override
	public String getDeviceUniqueIDTag() {
		return KeyManagerSupport.DEV_UNIQUE_ID_TAG;
	}

	@Override
	public byte [] getPrivateKeyData( String tag ) {
		assertRegistered();
		return KeyManagerSupport.getPrivateKeyData( contentResolver, tag );
	}

	@Override
	public byte [] getSharedKeyData( String tag ) {
		assertRegistered();
		return KeyManagerSupport.getSharedKeyData( contentResolver, tag );
	}

	@Override
	public boolean isRegistered() {
		return keyManagerToken != 0;
	}

	@Override
	public boolean isSupported() {
		return KeyManagerSupport.hasKeyManager( packageManager ) && KeyManagerSupport.signaturesMatch( packageManager, packageName );
	}

	@Override
	public void onKeyDataRead() {
		// Do nothing.
	}

	@Override
	public void onKeyManagerLockRequest() {
		dispatchLock();
	}

	@Override
	public void onKeyManagerUnlockRequest() {
		dispatchUnlock();
	}

	@Override
	public void register() {
		// assertSupported();
		// if( keyManagerToken == 0 ) {
		// keyManagerToken = KeyManagerSupport.registerWithKeyManager( contentResolver, packageName,
		// applicationName );
		// KeyManagerSupport.addListener( this );
		// }

		assertSupported();
		if( keyManagerToken == 0 ) {
			new Handler().post( new Runnable() {

				@Override
				public void run() {
					keyManagerToken = KeyManagerSupport.registerWithKeyManager( contentResolver, packageName, applicationName );
					KeyManagerSupport.addListener( ExternalKeyManagerV1.this );
				}
			} );
		}
	}

	@Override
	public void storePrivateKeyData( String tag, byte [] data ) {
		assertRegistered();
		KeyManagerSupport.storePrivateKeyData( contentResolver, data, tag );
	}

	@Override
	public void storeSharedKeyData( String tag, byte [] data ) {
		assertRegistered();
		KeyManagerSupport.storeSharedKeyData( contentResolver, data, tag );
	}

}
