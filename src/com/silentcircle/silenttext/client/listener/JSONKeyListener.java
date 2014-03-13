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
package com.silentcircle.silenttext.client.listener;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.silentcircle.api.model.Key;
import com.silentcircle.api.model.Signature;
import com.silentcircle.silenttext.client.model.SensitiveKey;
import com.silentcircle.silenttext.listener.JSONObjectListener;
import com.silentcircle.silenttext.util.JSONUtils;

public class JSONKeyListener implements JSONObjectListener {

	public Key key;

	@Override
	public void onObjectReceived( JSONObject json ) {
		SensitiveKey k = key instanceof SensitiveKey ? (SensitiveKey) key : new SensitiveKey();
		k.setVersion( JSONUtils.getInt( json, "version", 1 ) );
		k.setLocator( JSONUtils.getString( json, "locator" ) );
		k.setOwner( JSONUtils.getString( json, "owner" ) );
		k.setSuite( JSONUtils.getString( json, "keySuite" ) );
		k.setComment( JSONUtils.getString( json, "comment" ) );
		k.setCreationDate( JSONUtils.getDate( json, "start_date" ) );
		k.setExpirationDate( JSONUtils.getDate( json, "expire_date" ) );
		k.setPublicKey( JSONUtils.getBytes( json, "pubKey" ) );
		k.setPrivateKey( JSONUtils.getBytes( json, "privKey" ) );
		k.setSignatures( new ArrayList<Signature>() );
		JSONArray signaturesJSON = JSONUtils.getJSONArray( json, "signatures" );
		JSONSignatureListener signatureParser = new JSONSignatureListener();
		for( int i = 0; i < signaturesJSON.length(); i++ ) {
			JSONObject signatureJSON = JSONUtils.getJSONObject( signaturesJSON, i );
			signatureParser.onObjectReceived( signatureJSON );
			k.getSignatures().add( signatureParser.signature );
		}
		key = k;
	}

}
