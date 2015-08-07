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
package com.silentcircle.silenttext.client;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.silentcircle.api.model.Key;
import com.silentcircle.http.client.exception.http.client.HTTPClientUnknownResourceException;
import com.silentcircle.http.client.exception.http.client.HTTPClientUnprocessableEntityException;
import com.silentcircle.silenttext.MockUtils;
import com.silentcircle.silenttext.SilentTestRunner;
import com.silentcircle.silenttext.client.fixture.AccountsClientTestFixture;

@RunWith( SilentTestRunner.class )
public class AccountsClientTest extends AccountsClientTestFixture {

	@Test
	public void happyPath() {
		loginSuccessfully();
		logoutSuccessfully();
	}

	@Test( expected = HTTPClientUnknownResourceException.class )
	public void login_shouldThrowHTTPClientUnknownResourceExceptionIfEndpointIsInvalid() {

		inject( whenLoginRequested(), respondWithFileNotFound() );

		loginThenLogout();

	}

	@Test( expected = HTTPClientUnprocessableEntityException.class )
	public void login_shouldThrowHTTPClientUnprocessableEntityExceptionWhenGivenInvalidCredentials() {

		inject( whenLoginRequested(), respondWithError( 422, "You have specified an incorrect username or password." ) );

		login();

	}

	@Test( expected = HTTPClientUnknownResourceException.class )
	public void logout_shouldThrowHTTPClientUnknownResourceExceptionIfEndpointIsInvalid() {

		inject( whenLoginRequested(), respondWithValidLoginResponse() );
		inject( whenPushRegistrationRequested(), respondWithValidPushRegistrationResponse() );
		inject( whenPushUnregistrationRequested(), respondWithValidPushUnregistrationResponse() );
		inject( whenLogoutRequested(), respondWithFileNotFound() );

		loginThenLogout();

	}

	@Test( expected = HTTPClientUnknownResourceException.class )
	public void registerPush_shouldThrowHTTPClientUnknownResourceExceptionIfEndpointIsInvalid() {

		inject( whenLoginRequested(), respondWithValidLoginResponse() );
		inject( whenPushRegistrationRequested(), respondWithFileNotFound() );

		loginThenLogout();

	}

	@Test( expected = HTTPClientUnknownResourceException.class )
	public void unregisterPush_shouldThrowHTTPClientUnknownResourceExceptionIfEndpointIsInvalid() {

		inject( whenLoginRequested(), respondWithValidLoginResponse() );
		inject( whenPushRegistrationRequested(), respondWithValidPushRegistrationResponse() );
		inject( whenPushUnregistrationRequested(), respondWithFileNotFound() );

		loginThenLogout();

	}

	@Test
	public void uploadPublicKey_shouldBeSuccessful() {

		loginSuccessfully();

		inject( whenPublicKeyUploaded(), respondWithValidResponse() );
		inject( whenPublicKeyRevoked(), respondWithValidResponse() );

		Key key = MockUtils.mockPublicKey();

		session.uploadKey( key );
		session.revokeKey( key.getLocator() );

		logoutSuccessfully();

	}

}
