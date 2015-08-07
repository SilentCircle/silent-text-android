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
package com.silentcircle.silenttext.thread;

import com.silentcircle.api.Session;
import com.silentcircle.http.client.exception.NetworkException;
import com.silentcircle.http.client.exception.http.HTTPException;
import com.silentcircle.scimp.NamedKeyPair;
import com.silentcircle.silentstorage.repository.Repository;
import com.silentcircle.silentstorage.repository.file.RepositoryLockedException;
import com.silentcircle.silenttext.crypto.CryptoUtils;
import com.silentcircle.silenttext.log.Log;
import com.silentcircle.silenttext.thread.NamedThread.HasThreadName;

public class RemoveKeyPair implements Runnable, HasThreadName {

	private static final Log LOG = new Log( "RemoveKeyPair" );

	private final Session session;
	private final CharSequence locator;
	private final Repository<NamedKeyPair> keyPairs;

	public RemoveKeyPair( Session session, Repository<NamedKeyPair> keyPairs, CharSequence locator ) {
		this.session = session;
		this.keyPairs = keyPairs;
		this.locator = locator;
	}

	@Override
	public String getThreadName() {
		return "RemoveKeyPair";
	}

	@Override
	public void run() {
		try {
			session.revokeKey( locator );
		} catch( HTTPException exception ) {
			LOG.warn( exception, "#remove locator:%s", locator );
		} catch( NetworkException exception ) {
			LOG.warn( exception, "#remove locator:%s", locator );
		}
		try {
			keyPairs.removeByID( CryptoUtils.copyAsCharArray( locator ) );
		} catch( RepositoryLockedException exception ) {
			LOG.warn( exception, "#remove locator:%s", locator );
		}
	}

}
