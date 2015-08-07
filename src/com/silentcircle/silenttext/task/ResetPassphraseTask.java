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
package com.silentcircle.silenttext.task;

import java.lang.ref.SoftReference;

import android.content.Context;
import android.os.AsyncTask;

import com.silentcircle.silenttext.crypto.CryptoUtils;
import com.silentcircle.silenttext.crypto.EncryptedStorage;
import com.silentcircle.silenttext.crypto.Hash;

public class ResetPassphraseTask extends AsyncTask<CharSequence, Void, Boolean> {

	private static boolean mInvalideCurrentPasscode;

	private static char [] hash( CharSequence passphrase ) {
		char [] buffer = CryptoUtils.copyAsCharArray( passphrase );
		char [] passphraseHash = Hash.sha1( buffer );
		CryptoUtils.randomize( buffer );
		return passphraseHash;
	}

	/**
	 * @throws IllegalArgumentException
	 *             if the current passphrase is invalid.
	 */
	private static void resetMasterKeyPassphrase( Context context, CharSequence currentPassphrase, CharSequence newPassphrase ) {

		char [] currentPassphraseHash = hash( currentPassphrase );
		char [] newPassphraseHash = hash( newPassphrase );

		try {
			byte [] masterKey = EncryptedStorage.loadMasterKey( context, currentPassphraseHash );
			if( masterKey == null ) {
				mInvalideCurrentPasscode = true;
			}
			EncryptedStorage.saveMasterKey( context, newPassphraseHash, masterKey );
			CryptoUtils.randomize( masterKey );
		} finally {
			if( !mInvalideCurrentPasscode ) {
				CryptoUtils.randomize( currentPassphraseHash );
				CryptoUtils.randomize( newPassphraseHash );
			}
		}

	}

	private final SoftReference<Context> contextReference;

	public ResetPassphraseTask( Context context ) {
		contextReference = new SoftReference<Context>( context );
	}

	@Override
	protected Boolean doInBackground( CharSequence... args ) {

		if( args.length < 2 ) {
			return Boolean.valueOf( false );
		}

		try {
			if( !mInvalideCurrentPasscode ) {
				resetMasterKeyPassphrase( getContext(), args[0], args[1] );
				return Boolean.valueOf( true );
			}
			mInvalideCurrentPasscode = false;
			return Boolean.valueOf( false );
		} catch( IllegalArgumentException exception ) {
			// Ignore.
		}

		return Boolean.valueOf( false );

	}

	protected Context getContext() {
		return contextReference.get();
	}

	protected void onPassphraseReset() {
		// By default, do nothing.
	}

	protected void onPassphraseResetFailed() {
		// By default, do nothing.
	}

	@Override
	protected void onPostExecute( Boolean success ) {
		if( !success.booleanValue() ) {
			onPassphraseResetFailed();
		} else {
			onPassphraseReset();
		}
	}

}
