/*
Copyright © 2012-2013, Silent Circle, LLC.  All rights reserved.

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
package com.silentcircle.silenttext.client.listener;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;
import org.twuni.twoson.JSONParser;

import com.silentcircle.api.model.Entitlement;
import com.silentcircle.api.model.Key;
import com.silentcircle.api.model.User;
import com.silentcircle.silenttext.client.model.SensitiveKey;
import com.silentcircle.silenttext.client.model.SensitiveUser;
import com.silentcircle.silenttext.client.model.json.JSONSensitiveKeyListener;
import com.silentcircle.silenttext.listener.JSONObjectListener;
import com.silentcircle.silenttext.util.JSONUtils;

public class JSONUserListener implements JSONObjectListener {

	private static Key parseKey( byte [] in ) {
		return in == null ? null : parseKey( new ByteArrayInputStream( in ) );
	}

	private static Key parseKey( InputStream in ) {

		if( in == null ) {
			return null;
		}

		SensitiveKey key = new SensitiveKey();

		JSONParser parser = new JSONParser( in, new JSONSensitiveKeyListener( key ) );
		try {
			parser.read();
		} catch( IOException exception ) {
			throw new RuntimeException( exception );
		}

		return key;

	}

	private static Key parseKey( JSONObject in ) {
		return in == null ? null : parseKey( in.toString() );
	}

	private static Key parseKey( String in ) {
		return in == null ? null : parseKey( in.getBytes() );
	}

	public User user;

	@Override
	public void onObjectReceived( JSONObject json ) {
		SensitiveUser u = new SensitiveUser();
		if( JSONUtils.getBoolean( json, "silent_text" ) ) {
			u.getEntitlements().add( Entitlement.SILENT_CIRCLE_MOBILE );
		}
		u.setKeys( new ArrayList<Key>() );
		JSONArray keysJSON = JSONUtils.getJSONArray( json, "keys" );
		if( keysJSON != null ) {
			for( int i = 0; i < keysJSON.length(); i++ ) {
				Key key = parseKey( JSONUtils.getJSONObject( keysJSON, i ) );
				if( key != null ) {
					u.getKeys().add( key );
				}
			}
		}
		user = u;
	}

}
