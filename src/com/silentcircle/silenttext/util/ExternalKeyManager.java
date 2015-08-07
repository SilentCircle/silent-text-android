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

import com.silentcircle.silenttext.Extra;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.crypto.CryptoUtils;

public abstract class ExternalKeyManager {

	protected final ContentResolver contentResolver;
	protected final PackageManager packageManager;
	protected final String packageName;
	protected final String applicationName;
	protected final Locker locker;

	public ExternalKeyManager( ContentResolver contentResolver, PackageManager packageManager, String packageName, String applicationName, Locker locker ) {
		this.contentResolver = contentResolver;
		this.packageManager = packageManager;
		this.packageName = packageName;
		this.applicationName = applicationName;
		this.locker = locker;
	}

	public ExternalKeyManager( Context context, Locker locker ) {
		this( context.getContentResolver(), context.getPackageManager(), context.getPackageName(), context.getString( R.string.silent_text ), locker );
	}

	protected void assertRegistered() {
		if( !isRegistered() ) {
			throw new ExternalKeyManagerNotRegisteredException( this );
		}
	}

	protected void assertSupported() {
		if( !isSupported() ) {
			throw new ExternalKeyManagerNotSupportedException( this );
		}
	}

	public abstract void deletePrivateKeyData( String tag );

	public abstract void deleteSharedKeyData( String tag );

	protected void dispatchLock() {
		if( locker != null ) {
			locker.lock();
		}
	}

	protected void dispatchUnlock() {
		if( locker == null ) {
			return;
		}
		byte [] passPhrase = getPrivateKeyData( Extra.PASSWORD.getName() );
		try {
			if( passPhrase != null ) {
				char [] hashedPassPhrase = CryptoUtils.toCharArraySafe( passPhrase );
				try {
					locker.unlock( hashedPassPhrase );
				} finally {
					CryptoUtils.randomize( hashedPassPhrase );
				}
			}
		} finally {
			CryptoUtils.randomize( passPhrase );
		}
	}

	public abstract String getDeviceAuthDataTag();

	public abstract String getDeviceUniqueIDTag();

	public abstract byte [] getPrivateKeyData( String tag );

	public abstract byte [] getSharedKeyData( String tag );

	public abstract boolean isRegistered();

	public abstract boolean isSupported();

	public abstract void register();

	public abstract void storePrivateKeyData( String tag, byte [] data );

	public abstract void storeSharedKeyData( String tag, byte [] data );

}
