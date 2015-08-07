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

import java.util.Arrays;
import java.util.List;

import android.os.AsyncTask;

import com.silentcircle.api.Session;
import com.silentcircle.api.web.HasSession;
import com.silentcircle.http.client.exception.http.client.HTTPClientMethodNotAllowedException;
import com.silentcircle.http.client.exception.http.client.HTTPClientUnknownResourceException;
import com.silentcircle.http.client.exception.http.server.HTTPServerUnavailableException;
import com.silentcircle.scimp.NamedKeyPair;
import com.silentcircle.silentstorage.repository.Repository;

public class RevokeKeyPairTask extends AsyncTask<NamedKeyPair, NamedKeyPair, List<NamedKeyPair>> {

	private final Repository<NamedKeyPair> repository;
	private final HasSession hasSession;

	public RevokeKeyPairTask( Repository<NamedKeyPair> repository, HasSession hasSession ) {
		this.repository = repository;
		this.hasSession = hasSession;
	}

	@Override
	protected List<NamedKeyPair> doInBackground( NamedKeyPair... pairs ) {
		Session session = hasSession.getSession();
		for( NamedKeyPair pair : pairs ) {
			try {
				session.revokeKey( pair.locator );
			} catch( HTTPClientMethodNotAllowedException exception ) {
				// The API differs from what we expected.
			} catch( HTTPClientUnknownResourceException exception ) {
				// OK.
			} catch( HTTPServerUnavailableException exception ) {
				// Server is unavailable (rare).
			}
			repository.remove( pair );
			publishProgress( pair );
		}
		return Arrays.asList( pairs );
	}

}
