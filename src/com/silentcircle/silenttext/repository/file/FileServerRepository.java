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
package com.silentcircle.silenttext.repository.file;

import java.io.File;

import org.json.JSONException;
import org.json.JSONObject;

import com.silentcircle.silenttext.filter.IOFilter;
import com.silentcircle.silenttext.model.Server;
import com.silentcircle.silenttext.model.json.JSONServerAdapter;
import com.silentcircle.silenttext.repository.ServerRepository;

public class FileServerRepository extends BaseFileRepository<Server> implements ServerRepository {

	private final JSONServerAdapter adapter = new JSONServerAdapter();

	public FileServerRepository( File root ) {
		super( root );
	}

	public FileServerRepository( File root, IOFilter<String> filter ) {
		super( root, filter );
	}

	@Override
	protected Server deserialize( String serial ) {
		try {
			return adapter.adapt( new JSONObject( serial ) );
		} catch( JSONException exception ) {
			return null;
		}
	}

	@Override
	protected String getLogTag() {
		return "FileServerRepository";
	}

	@Override
	protected String identify( Server server ) {
		return server.getId();
	}

	@Override
	protected String serialize( Server server ) {
		return adapter.adapt( server ).toString();
	}

}
