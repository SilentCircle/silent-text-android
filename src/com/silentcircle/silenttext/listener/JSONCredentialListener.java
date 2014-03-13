/*
Copyright © 2013, Silent Circle, LLC.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Any redistribution, use, or modification is done solely for personal 
      benefit and not for any commercial purpose or for monetary gain
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name Silent Circle nor the names of its contributors may 
      be used to endorse or promote products derived from this software 
      without specific prior written permission.

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
package com.silentcircle.silenttext.listener;

import org.json.JSONException;
import org.json.JSONObject;

import com.silentcircle.silenttext.model.Credential;

public class JSONCredentialListener implements JSONObjectListener {

	protected final CredentialListener listener;

	public JSONCredentialListener( CredentialListener listener ) {
		this.listener = listener;
	}

	@Override
	public void onObjectReceived( JSONObject envelope ) {
		try {
			if( !envelope.has( "silent_text" ) ) {
				listener.onCredentialError( "Your account is not authorized to use Silent Text." );
				return;
			}
			JSONObject json = envelope.getJSONObject( "silent_text" );
			if( json.has( "username" ) && json.has( "password" ) ) {
				Credential credential = new Credential();
				credential.setUsername( json.getString( "username" ) );
				credential.setPassword( json.getString( "password" ) );
				listener.onCredentialReceived( credential );
			} else if( json.has( "error_msg" ) ) {
				listener.onCredentialError( json.getString( "error_msg" ) );
			} else if( json.has( "result" ) && "error".equals( json.getString( "result" ) ) && json.has( "msg" ) ) {
				listener.onCredentialError( json.getString( "msg" ) );
			} else {
				throw new JSONException( String.format( "%s", json.toString() ) );
			}
		} catch( JSONException exception ) {
			throw new RuntimeException( exception );
		}
	}

}
