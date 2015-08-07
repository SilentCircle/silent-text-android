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
package com.silentcircle.silenttext.thread;

import java.util.List;

import org.twuni.twoson.JSONParser;

import com.silentcircle.api.AuthenticatedSession;
import com.silentcircle.api.model.Key;
import com.silentcircle.api.model.User;
import com.silentcircle.api.web.model.json.JSONObjectParser;
import com.silentcircle.http.client.exception.NetworkException;
import com.silentcircle.http.client.exception.http.client.HTTPClientUnauthorizedException;
import com.silentcircle.http.client.exception.http.client.HTTPClientUnknownResourceException;
import com.silentcircle.scimp.NamedKeyPair;
import com.silentcircle.silentstorage.repository.Repository;
import com.silentcircle.silenttext.log.Log;
import com.silentcircle.silenttext.thread.NamedThread.HasThreadName;

public class SynchronizePublicKeysWithServer implements Runnable, HasThreadName {

	private static final Log LOG = new Log( "SynchronizePublicKeysWithServer" );
	private static boolean running = false;

	private static com.silentcircle.api.model.Key extractPublicKey( NamedKeyPair keyPair ) {
		if( keyPair == null || keyPair.getPublicKey() == null || keyPair.getPublicKey().length <= 0 ) {
			return null;
		}
		return JSONObjectParser.parseKey( JSONParser.parse( keyPair.getPublicKey() ) );
	}

	private final AuthenticatedSession session;
	private final Repository<NamedKeyPair> repository;

	public SynchronizePublicKeysWithServer( AuthenticatedSession session, Repository<NamedKeyPair> repository ) {
		this.session = session;
		this.repository = repository;
	}

	@Override
	public String getThreadName() {
		return "SynchronizePublicKeysWithServer";
	}

	private User getUser() {
		try {
			return session.findOwnUser();
		} catch( HTTPClientUnauthorizedException exception ) {
			onSessionInvalid();
			throw exception;
		} catch( NetworkException exception ) {
			throw exception;
		}
	}

	protected void onException( Throwable exception ) {
		LOG.warn( exception, "#onException" );
	}

	protected void onSessionInvalid() {
		// By default, do nothing.
	}

	private void revokeUnmanagedRemoteKeys( List<NamedKeyPair> localKeys, List<Key> remoteKeys ) {

		if( remoteKeys == null ) {
			return;
		}

		for( Key remoteKey : remoteKeys ) {

			boolean existsLocally = false;
			String locator = String.valueOf( remoteKey.getLocator() );

			if( localKeys != null ) {
				for( NamedKeyPair localKey : localKeys ) {
					if( localKey.locator.equals( locator ) ) {
						existsLocally = true;
						break;
					}
				}
			}

			if( !existsLocally ) {
				try {
					session.revokeKey( locator );
				} catch( HTTPClientUnknownResourceException ignore ) {
					// This just means the key has already been removed.
				}
			}

		}

	}

	@Override
	public void run() {
		if( running ) {
			return;
		}
		running = true;
		try {
			synchronizePublicKeysWithServer();
		} catch( Throwable exception ) {
			onException( exception );
		}
		running = false;
	}

	private void synchronizePublicKeysWithServer() {

		User user = getUser();
		List<Key> remoteKeys = user.getKeys();
		List<NamedKeyPair> localKeys = repository.list().fetchAll();

		revokeUnmanagedRemoteKeys( localKeys, remoteKeys );
		uploadMissingRemoteKeys( localKeys, remoteKeys );

	}

	private void uploadMissingRemoteKeys( List<NamedKeyPair> localKeys, List<Key> remoteKeys ) {

		if( localKeys == null ) {
			return;
		}

		for( NamedKeyPair localKey : localKeys ) {

			boolean existsRemotely = false;
			String locator = localKey.locator;

			if( remoteKeys != null ) {
				for( Key remoteKey : remoteKeys ) {
					if( String.valueOf( remoteKey.getLocator() ).equals( locator ) ) {
						existsRemotely = true;
						break;
					}
				}
			}

			if( !existsRemotely ) {
				Key remoteKey = extractPublicKey( localKey );
				session.uploadKey( remoteKey );
			}

		}

	}

}
