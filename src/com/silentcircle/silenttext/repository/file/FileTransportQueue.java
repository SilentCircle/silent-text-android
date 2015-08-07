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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.twuni.xmppt.xmpp.XMPPClientConnection;
import org.twuni.xmppt.xmpp.XMPPClientConnection.AcknowledgmentListener;

import android.content.Context;

import com.silentcircle.core.util.StringUtils;
import com.silentcircle.http.client.exception.NetworkException;
import com.silentcircle.silentstorage.io.BufferedBlockCipherFactory;
import com.silentcircle.silentstorage.io.MacFactory;
import com.silentcircle.silentstorage.io.Serializer;
import com.silentcircle.silentstorage.repository.file.SecureFileRepository;
import com.silentcircle.silentstorage.repository.helper.RepositoryHelper;
import com.silentcircle.silentstorage.repository.lazy.LazyList;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.client.AcknowledgmentRequestor;
import com.silentcircle.silenttext.log.Log;
import com.silentcircle.silenttext.task.GetDeviceChangedTask;
import com.silentcircle.silenttext.transport.Envelope;
import com.silentcircle.silenttext.transport.TransportQueue;
import com.silentcircle.silenttext.util.AsyncUtils;

public class FileTransportQueue extends SecureFileRepository<Envelope> implements TransportQueue, AcknowledgmentListener {

	static class EnvelopeHelper extends RepositoryHelper<Envelope> {

		static class EnvelopeSerializer extends Serializer<Envelope> {

			private static final EnvelopeSerializer SERIALIZER_INSTANCE = new EnvelopeSerializer();

			private static final int VERSION = 3;

			public static EnvelopeSerializer getInstance() {
				return SERIALIZER_INSTANCE;
			}

			@Override
			public Envelope read( DataInputStream in ) throws IOException {
				return read( in, new Envelope() );
			}

			@Override
			public Envelope read( DataInputStream in, Envelope out ) throws IOException {

				int version = in.readInt();

				switch( version ) {

					case 3:

						out.time = in.readLong();
						// $FALL-THROUGH$

					case 2:

						out.id = readString( in );
						out.from = readString( in );
						out.to = readString( in );
						out.content = readString( in );
						out.state = Envelope.State.parse( readString( in ) );
						out.badgeworthy = in.readBoolean();
						out.notifiable = in.readBoolean();

						break;

					case 1:

						out.id = in.readUTF();
						out.from = in.readUTF();
						out.to = in.readUTF();
						out.content = in.readUTF();
						out.notifiable = in.readBoolean();
						out.badgeworthy = in.readBoolean();
						out.state = Envelope.State.parse( in.readUTF() );

						break;

				}

				return out;

			}

			@Override
			public Envelope write( Envelope in, DataOutputStream out ) throws IOException {
				out.writeInt( VERSION );
				out.writeLong( in.time );
				writeString( in.id, out );
				writeString( in.from, out );
				writeString( in.to, out );
				writeString( in.content, out );
				writeString( in.state.id, out );
				out.writeBoolean( in.badgeworthy );
				out.writeBoolean( in.notifiable );
				return in;
			}

		}

		private static final EnvelopeHelper HELPER_INSTANCE = new EnvelopeHelper();

		public static EnvelopeHelper getInstance() {
			return HELPER_INSTANCE;
		}

		public EnvelopeHelper() {
			super( EnvelopeSerializer.getInstance() );
		}

		@Override
		public char [] identify( Envelope envelope ) {
			return envelope != null && envelope.id != null ? envelope.id.toCharArray() : null;
		}

	}

	final Context context;
	private final Log log = new Log( "FileTransportQueue" );

	public FileTransportQueue( Context context, File root, BufferedBlockCipherFactory cipherFactory, MacFactory macFactory ) {
		super( root, EnvelopeHelper.getInstance(), cipherFactory, macFactory );

		this.context = context;
	}

	@Override
	public void add( Envelope envelope ) {
		save( envelope );
	}

	@Override
	public synchronized void onFailedAcknowledgment( XMPPClientConnection connection, int expected, int actual ) {
		LazyList<Envelope> queue = list();
		int count = queue.size();
		int retransmissionCount = 0;
		for( int i = 0; i < count; i++ ) {
			Envelope packet = null;
			try {
				packet = queue.get( i );
				if( packet != null && Envelope.State.PROCESSED.equals( packet.state ) ) {
					packet.state = Envelope.State.PENDING;
					save( packet );
					retransmissionCount++;
				}
			} catch( Throwable exception ) {
				if( packet != null ) {
					log.warn( exception, "id:%s to:%s", packet.id, packet.to );
				} else {
					log.warn( exception, "index:%d", Integer.valueOf( i ) );
				}
			}
		}
		if( retransmissionCount > 0 ) {
			connection.markForRetransmission( retransmissionCount );
		}
	}

	@Override
	public synchronized void onSuccessfulAcknowledgment( XMPPClientConnection connection ) {
		LazyList<Envelope> queue = list();
		int count = queue.size();
		for( int i = 0; i < count; i++ ) {
			Envelope packet = null;
			try {
				packet = queue.get( i );
				if( packet != null && Envelope.State.PROCESSED.equals( packet.state ) ) {
					remove( packet );
				}
			} catch( Throwable exception ) {
				if( packet != null ) {
					log.warn( exception, "id:%s to:%s", packet.id, packet.to );
				} else {
					log.warn( exception, "index:%d", Integer.valueOf( i ) );
				}
			}
		}
	}

	@Override
	public synchronized void process( Processor processor ) {
		List<Envelope> queue = sortedList();
		int count = queue.size();
		int processed = 0;
		for( int i = 0; i < count; i++ ) {
			Envelope packet = null;
			try {
				packet = queue.get( i );
				if( packet != null ) {
					log.debug( "#process id:%s state:%s", packet.id, packet.state );
					if( Envelope.State.PENDING.equals( packet.state ) ) {
						try {
							processor.process( packet );
							processed++;
							packet.state = Envelope.State.PROCESSED;
							save( packet );
						} catch( IllegalStateException exception ) {
							// We expect this to happen.
						}
					}
				}
			} catch( NetworkException networkException ) {
				if( StringUtils.equals( networkException.getMessage(), "<not-authorized/>" ) ) {
					AsyncUtils.execute( new GetDeviceChangedTask( context ) {

						@Override
						protected void onPostExecute( Void result ) {
							if( deviceChanged ) {
								( (SilentTextApplication) context ).deactivate();
							}
						}

					}, SilentTextApplication.from( context ).getUsername() );
				}
			} catch( Throwable exception ) {
				if( packet != null ) {
					log.warn( exception, "id:%s to:%s", packet.id, packet.to );
				} else {
					log.warn( exception, "index:%d", Integer.valueOf( i ) );
				}
			}
		}
		if( processed > 0 ) {
			if( processor instanceof AcknowledgmentRequestor ) {
				try {
					( (AcknowledgmentRequestor) processor ).requestAcknowledgment();
				} catch( Throwable exception ) {
					log.warn( exception, "#onRequestAcknowledgment" );
				}
			}
		}
	}

	private List<Envelope> sortedList() {
		LazyList<Envelope> lazyQueue = list();
		List<Envelope> queue = new ArrayList<Envelope>();
		int count = lazyQueue.size();
		for( int i = 0; i < count; i++ ) {
			queue.add( lazyQueue.get( i ) );
		}
		Collections.sort( queue );
		return queue;
	}

}
