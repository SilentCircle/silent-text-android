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
package com.silentcircle.silenttext.repository.file;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.silentcircle.silenttext.crypto.Hash;
import com.silentcircle.silenttext.filter.IOFilter;
import com.silentcircle.silenttext.model.Conversation;
import com.silentcircle.silenttext.model.json.JSONConversationAdapter;
import com.silentcircle.silenttext.repository.ConversationRepository;
import com.silentcircle.silenttext.repository.EventRepository;
import com.silentcircle.silenttext.repository.ResourceStateRepository;

public class FileConversationRepository extends BaseFileRepository<Conversation> implements ConversationRepository {

	private final JSONConversationAdapter adapter = new JSONConversationAdapter();
	private final Map<String, EventRepository> histories = new HashMap<String, EventRepository>();
	private final Map<String, ResourceStateRepository> contexts = new HashMap<String, ResourceStateRepository>();

	private final File ephemeralRoot;

	public FileConversationRepository( File root ) {
		this( root, root );
	}

	public FileConversationRepository( File root, File ephemeralRoot ) {
		super( root );
		this.ephemeralRoot = ephemeralRoot;
	}

	public FileConversationRepository( File root, File ephemeralRoot, IOFilter<String> filter ) {
		super( root, filter );
		this.ephemeralRoot = ephemeralRoot;
	}

	public FileConversationRepository( File root, IOFilter<String> filter ) {
		this( root, root, filter );
	}

	@Override
	public ResourceStateRepository contextOf( Conversation conversation ) {
		if( conversation == null ) {
			return null;
		}
		String id = identify( conversation );
		ResourceStateRepository repository = contexts.get( id );
		if( repository == null ) {
			repository = new FileResourceStateRepository( new File( root, Hash.sha1( id + ".contexts" ) ) );
			( (FileResourceStateRepository) repository ).setFilter( filter );
			contexts.put( id, repository );
		}
		return repository;
	}

	@Override
	protected Conversation deserialize( String serial ) {
		if( serial == null ) {
			return null;
		}
		try {
			return adapter.adapt( new JSONObject( serial ) );
		} catch( JSONException exception ) {
			return null;
		}
	}

	@Override
	public Conversation findByPartner( String partner ) {
		return findById( partner );
	}

	@Override
	public EventRepository historyOf( Conversation conversation ) {
		if( conversation == null ) {
			return null;
		}
		String id = identify( conversation );
		EventRepository history = histories.get( id );
		if( history == null ) {
			history = new FileEventRepository( new File( root, Hash.sha1( id + ".history" ) ), ephemeralRoot );
			( (FileEventRepository) history ).setFilter( filter );
			histories.put( id, history );
		}
		return history;
	}

	@Override
	protected String identify( Conversation conversation ) {
		return conversation == null ? null : conversation.getPartner() == null ? null : conversation.getPartner().getUsername();
	}

	@Override
	public void remove( Conversation conversation ) {
		if( conversation == null ) {
			return;
		}
		contextOf( conversation ).clear();
		historyOf( conversation ).clear();
		super.remove( conversation );
	}

	@Override
	protected String serialize( Conversation conversation ) {
		if( conversation == null ) {
			return null;
		}
		return adapter.adapt( conversation ).toString();
	}

}
