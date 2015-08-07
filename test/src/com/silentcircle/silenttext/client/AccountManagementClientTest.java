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

import org.apache.http.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.tester.org.apache.http.RequestMatcher;

import com.silentcircle.api.model.Credential;
import com.silentcircle.api.model.User;
import com.silentcircle.api.model.UsernamePasswordCredential;
import com.silentcircle.api.web.UserManagerClient;
import com.silentcircle.api.web.model.BasicUser;
import com.silentcircle.http.client.AbstractHTTPClient;
import com.silentcircle.http.client.apache.ApacheHTTPClient;
import com.silentcircle.http.client.apache.HttpClient;
import com.silentcircle.silenttext.SilentTestRunner;
import com.silentcircle.silenttext.client.fixture.ClientTestFixture;

@RunWith( SilentTestRunner.class )
public class AccountManagementClientTest extends ClientTestFixture {

	protected static HttpResponse respondWithValidCreateUserResponse() {
		return respondWith( 200, validCreateUserResponse() );
	}

	protected static JSONObject validCreateUserResponse() {
		try {
			return new JSONObject( "{\"silent_text\": true, \"first_name\": \"\", \"last_name\": \"\", \"display_name\": \"test\", \"avatar_url\": \"/static/img/default_avatar.png\", \"keys\": [], \"silent_phone\": true, \"subscription\": {\"expires\": \"1900-01-01T00:00:00Z\"}, \"force_password_change\": false, \"permissions\": {\"silent_desktop\": false, \"silent_text\": false, \"silent_phone\": false, \"can_send_media\": true, \"has_oca\": false}}" );
		} catch( JSONException exception ) {
			return null;
		}
	}

	protected static RequestMatcher whenEditUserRequested() {
		return whenRequested( "PUT", "^https?://[^/]+/v1/user/[^/]+/$" );
	}

	@Test
	public void applyLicenseCode_shouldBeSuccessful_whenGivenValidParameters() {

		inject( whenEditUserRequested(), respondWithValidCreateUserResponse() );

		UserManagerClient client = client();
		Credential credential = credential();

		client.createUser( credential, emptyUser() );
		client.addLicense( credential, "ABCD2345EF016789" );

	}

	protected UserManagerClient client() {
		return new UserManagerClient( http(), "http://localhost" );
	}

	@Test
	public void createUser_shouldBeSuccessful_whenGivenFullValidParameters() {

		inject( whenEditUserRequested(), respondWithValidCreateUserResponse() );

		client().createUser( credential(), user(), "ABCD2345EF016789" );

	}

	@Test
	public void createUser_shouldBeSuccessful_whenGivenMinimumValidParameters() {
		createUserSuccessfully();
	}

	@Test( expected = IllegalArgumentException.class )
	public void createUser_shouldThrowIllegalArgumentException_whenGivenEmptyPassword() {

		UserManagerClient client = client();

		client.createUser( new UsernamePasswordCredential( "test", "" ), emptyUser() );

	}

	@Test( expected = IllegalArgumentException.class )
	public void createUser_shouldThrowIllegalArgumentException_whenGivenEmptyUserID() {

		UserManagerClient client = client();

		client.createUser( new UsernamePasswordCredential( "", "p855w0rd!" ), emptyUser() );

	}

	@Test( expected = IllegalArgumentException.class )
	public void createUser_shouldThrowIllegalArgumentException_whenGivenNullPassword() {

		UserManagerClient client = client();

		client.createUser( new UsernamePasswordCredential( "test", null ), emptyUser() );

	}

	@Test( expected = IllegalArgumentException.class )
	public void createUser_shouldThrowIllegalArgumentException_whenGivenNullUserID() {

		UserManagerClient client = client();

		client.createUser( new UsernamePasswordCredential( null, "p855w0rd!" ), emptyUser() );

	}

	protected User createUserSuccessfully() {

		inject( whenEditUserRequested(), respondWithValidCreateUserResponse() );

		return client().createUser( credential(), user() );

	}

	protected UsernamePasswordCredential credential() {
		return new UsernamePasswordCredential( "test", "p855w0rd!" );
	}

	protected User emptyUser() {
		return new BasicUser();
	}

	protected AbstractHTTPClient http() {
		return new ApacheHTTPClient( new HttpClient() );
	}

	protected User user() {
		BasicUser user = new BasicUser();
		user.setDisplayName( "John Q. Test" );
		user.setEmailAddress( "john.q.test@example.com" );
		return user;
	}

}
