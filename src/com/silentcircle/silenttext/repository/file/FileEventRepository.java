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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.json.JSONException;
import org.json.JSONObject;

import com.silentcircle.silenttext.crypto.Hash;
import com.silentcircle.silenttext.filter.IOFilter;
import com.silentcircle.silenttext.model.event.Event;
import com.silentcircle.silenttext.model.event.Message;
import com.silentcircle.silenttext.model.json.JSONEventAdapter;
import com.silentcircle.silenttext.repository.EventRepository;
import com.silentcircle.silenttext.repository.SCloudObjectRepository;

public class FileEventRepository extends BaseFileRepository<Event> implements EventRepository {

	private final JSONEventAdapter adapter = new JSONEventAdapter();

	private final Map<String, SCloudObjectRepository> objects = new HashMap<String, SCloudObjectRepository>();
	private final File ephemeralRoot;

	public FileEventRepository( File root ) {
		this( root, root );
	}

	public FileEventRepository( File root, File ephemeralRoot ) {
		super( root );
		this.ephemeralRoot = ephemeralRoot;
	}

	public FileEventRepository( File root, File ephemeralRoot, IOFilter<String> filter ) {
		super( root, filter );
		this.ephemeralRoot = ephemeralRoot;
	}

	public FileEventRepository( File root, IOFilter<String> filter ) {
		this( root, root, filter );
	}

	@Override
	protected Collection<Event> createEmptyCollection() {
		return new TreeSet<Event>();
	}

	@Override
	protected Event deserialize( String serial ) {
		try {
			return adapter.adapt( new JSONObject( serial ) );
		} catch( JSONException exception ) {
			return null;
		}
	}

	@Override
	public Event findById( String id ) {
		Event event = super.findById( id );
		if( event instanceof Message ) {
			Message message = (Message) event;
			if( message.isExpired() ) {
				log.info( "#findById removing expired id:%s", id );
				remove( message );
				return null;
			}
		}
		return event;
	}

	@Override
	public void flush() {
		super.flush();
		for( SCloudObjectRepository repository : objects.values() ) {
			BaseFileRepository.flush( repository );
		}
	}

	@Override
	protected String getLogTag() {
		return "FileEventRepository";
	}

	@Override
	protected String identify( Event message ) {
		return message.getId();
	}

	@Override
	public List<Event> list() {
		List<Event> list = super.list();
		for( int i = 0; i < list.size(); i++ ) {
			Event event = list.get( i );
			if( event instanceof Message ) {
				Message message = (Message) event;
				if( message.isExpired() ) {
					log.info( "#list removing expired id:%s", message.getId() );
					remove( message );
					list.remove( i );
					i--;
				}
			}
		}
		return list;
	}

	@Override
	public SCloudObjectRepository objectsOf( Event event ) {
		if( event == null ) {
			return null;
		}
		String id = identify( event );
		SCloudObjectRepository repository = objects.get( id );
		if( repository == null ) {
			repository = new FileSCloudObjectRepository( new File( root, Hash.sha1( id + ".objects" ) ), ephemeralRoot, filter );
			objects.put( id, repository );
		}
		return repository;
	}

	@Override
	public void remove( Event event ) {
		if( event == null ) {
			return;
		}
		objectsOf( event ).clear();
		super.remove( event );
	}

	@Override
	protected String serialize( Event message ) {
		return adapter.adapt( message ).toString();
	}

}
