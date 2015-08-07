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

import java.math.BigInteger;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.silentcircle.http.client.apache.ApacheHTTPClient;
import com.silentcircle.http.client.apache.HttpClient;
import com.silentcircle.silenttext.SilentTestRunner;
import com.silentcircle.silenttext.client.fixture.ClientTestFixture;
import com.silentcircle.silenttext.crypto.Hash;

@RunWith( SilentTestRunner.class )
public class LegacyAccountCreationClientTest extends ClientTestFixture {

	private static String hashDeviceID( String deviceID ) {
		return new BigInteger( 1, Hash.sha1( "SilentCircle".getBytes(), deviceID.getBytes() ) ).toString( 16 );
	}

	protected static JSONObject validAccountCreationResponseJSON() {
		JSONObject json = validResponse();
		try {
			json.put( "provisioning_code", "A0B2C1D3" );
		} catch( JSONException ignore ) {
			// Never going to happen.
		}
		return json;
	}

	@Test
	public void createAccount() {
		String deviceID = "1234567890000000";
		String deviceSerial = "010203040506000";
		inject( whenRequested( "POST", "^https?://[^/]+/provisioning/imei/([^/]+)/?$" ), respondWith( 200, validAccountCreationResponseJSON() ) );
		LegacyAccountCreationClient client = new LegacyAccountCreationClient( null, new ApacheHTTPClient( new HttpClient( SSLSocketFactory.getSocketFactory() ) ), "https://accounts-dev.silentcircle.com", hashDeviceID( deviceID ), deviceSerial );
		CharSequence activationCode = client.createAccount( "AwesomeCorp0", "____DELETE ME____", "awesomecorp0@example.com", "Awesome", "Corp" );
		assertEquals( "A0B2C1D3", activationCode );
	}

}
