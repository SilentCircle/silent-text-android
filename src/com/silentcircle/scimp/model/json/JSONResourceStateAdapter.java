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
package com.silentcircle.scimp.model.json;

import org.json.JSONException;
import org.json.JSONObject;

import com.silentcircle.scimp.model.ResourceState;
import com.silentcircle.silenttext.log.Log;
import com.silentcircle.silenttext.repository.ModelAdapter;
import com.silentcircle.silenttext.util.JSONUtils;

public class JSONResourceStateAdapter implements ModelAdapter<ResourceState> {

	private static final Log log = new Log( JSONResourceStateAdapter.class.getSimpleName() );

	public static ResourceState adapt( JSONObject json ) {

		ResourceState object = new ResourceState();

		object.setResource( JSONUtils.getString( json, "resource" ) );
		object.setState( JSONUtils.getString( json, "state" ) );
		object.setVerifyCode( JSONUtils.getString( json, "verify_code" ) );

		return object;

	}

	public static JSONObject adapt( ResourceState object ) {

		JSONObject json = new JSONObject();

		try {
			json.put( "resource", object.getResource() );
			json.put( "state", object.getState() );
			json.put( "verify_code", object.getVerifyCode() );
		} catch( JSONException impossible ) {
			log.error( impossible, "ADAPT resource:%s state:%s verify_code:%s", object.getResource(), object.getState(), object.getVerifyCode() );
		}

		return json;

	}

	@Override
	public ResourceState deserialize( String serial ) {
		if( serial == null ) {
			return null;
		}
		try {
			JSONObject json = new JSONObject( serial );
			return adapt( json );
		} catch( JSONException exception ) {
			log.error( exception, "DESERIALIZE serial:%s", serial );
			return null;
		}
	}

	@Override
	public String identify( ResourceState object ) {
		return object == null ? null : object.getResource();
	}

	@Override
	public String serialize( ResourceState object ) {
		return adapt( object ).toString();
	}

}
