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
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.silentcircle.silenttext.filter.IOFilter;
import com.silentcircle.silenttext.repository.file.FileTransportQueue.Packet;
import com.silentcircle.silenttext.transport.TransportQueue;

public class FileTransportQueue extends BaseFileRepository<Packet> implements TransportQueue {

	public static class Packet {

		public String id;
		public String to;
		public String text;
		public boolean notifiable;
		public boolean badgeworthy;

		public Packet() {
		}

		public Packet( String id, String to, String text, boolean notifiable, boolean badgeworthy ) {
			this.id = id;
			this.to = to;
			this.text = text;
			this.notifiable = notifiable;
			this.badgeworthy = badgeworthy;
		}

		@Override
		public boolean equals( Object o ) {
			return o != null && hashCode() == o.hashCode();
		}

		@Override
		public int hashCode() {
			return String.format( "id:%s to:%s", id, to ).hashCode();
		}

		@Override
		public String toString() {
			return String.format( "[%s]->(%s):%s", id, to, text );
		}

	}

	public FileTransportQueue( File root ) {
		super( root );
	}

	public FileTransportQueue( File root, IOFilter<String> filter ) {
		super( root, filter );
	}

	@Override
	public void add( String id, String to, String text, boolean notifiable, boolean badgeworthy ) {
		save( new Packet( id, to, text, notifiable, badgeworthy ) );
	}

	@Override
	protected Packet deserialize( String serial ) {
		Packet packet = new Packet();
		try {
			JSONObject json = new JSONObject( serial );
			if( json.has( "id" ) ) {
				packet.id = json.getString( "id" );
			}
			if( json.has( "to" ) ) {
				packet.to = json.getString( "to" );
			}
			if( json.has( "text" ) ) {
				packet.text = json.getString( "text" );
			}
			if( json.has( "notifiable" ) ) {
				packet.notifiable = json.getBoolean( "notifiable" );
			}
			if( json.has( "badgeworthy" ) ) {
				packet.badgeworthy = json.getBoolean( "badgeworthy" );
			}
		} catch( JSONException exception ) {
			log.error( exception, "DESERIALIZE serial:%s", serial );
		}
		return packet;
	}

	@Override
	protected String identify( Packet packet ) {
		return packet.id;
	}

	@Override
	public void process( Processor processor ) {
		List<Packet> pending = new ArrayList<Packet>();
		for( Packet packet : list() ) {
			pending.add( packet );
			try {
				processor.process( packet.id, packet.to, packet.text, packet.notifiable, packet.badgeworthy );
				remove( packet );
			} catch( Exception exception ) {
				log.warn( exception, "id:%s to:%s", packet.id, packet.to );
			}
		}
	}

	@Override
	protected String serialize( Packet packet ) {
		JSONObject json = new JSONObject();
		try {
			json.put( "id", packet.id );
			json.put( "to", packet.to );
			json.put( "text", packet.text );
			json.put( "notifiable", packet.notifiable );
			json.put( "badgeworthy", packet.badgeworthy );
		} catch( JSONException exception ) {
			log.error( exception, "SERIALIZE packet:%s", packet );
		}
		return json.toString();
	}

}
