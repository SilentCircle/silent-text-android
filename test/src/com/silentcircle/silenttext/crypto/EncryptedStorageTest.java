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
package com.silentcircle.silenttext.crypto;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import android.app.Activity;

import com.silentcircle.silenttext.Assert;
import com.silentcircle.silenttext.SilentTestRunner;
import com.silentcircle.silenttext.activity.UnlockActivity;

@RunWith( SilentTestRunner.class )
public class EncryptedStorageTest extends Assert {

	@Test
	public void loadMasterKey_ifKeyCreated_lengthShouldBe32Bytes() {
		Activity activity = Robolectric.buildActivity( UnlockActivity.class ).create().get();
		char [] passphrase = "This is a good test.".toCharArray();
		byte [] masterKey = EncryptedStorage.loadMasterKey( activity, passphrase );
		assertEquals( 16, masterKey.length );// Why is the master key 16 bytes, while the user key
												// is 32?
	}

	@Test
	public void loadUserKey_ifKeyCreated_lengthShouldBe32Bytes() {
		Activity activity = Robolectric.buildActivity( UnlockActivity.class ).create().get();
		char [] passphrase = "This is a good test.".toCharArray();
		byte [] masterKey = EncryptedStorage.loadMasterKey( activity, passphrase );
		byte [] userKey = EncryptedStorage.loadUserKey( activity, masterKey, "alice@example.com" );
		assertEquals( 32, userKey.length );
	}

	@Before
	public void unlockStrongCryptography() {
		Liberator.rebel();
	}

}
