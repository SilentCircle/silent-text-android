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
package com.silentcircle.silenttext.thread;

import java.io.IOException;
import java.io.InputStream;

import org.json.JSONException;
import org.json.JSONObject;

import com.silentcircle.scloud.NativePacket;
import com.silentcircle.scloud.SCloudEncryptOutputStream;
import com.silentcircle.scloud.listener.IndexBuilder;
import com.silentcircle.scloud.listener.OnBlockEncryptedListener;
import com.silentcircle.scloud.listener.OnBlockEncryptedListenerChain;
import com.silentcircle.scloud.listener.SCloudObjectSave;
import com.silentcircle.scloud.model.SCloudObject;
import com.silentcircle.silenttext.repository.SCloudObjectRepository;
import com.silentcircle.silenttext.thread.NamedThread.HasThreadName;
import com.silentcircle.silenttext.util.IOUtils;

public class SCloudEncrypt implements Runnable, HasThreadName {

	protected class ProgressPublisher implements OnBlockEncryptedListener {

		@Override
		public void onBlockEncrypted( String key, String locator, byte [] data ) {
			next( new SCloudObject( key, locator, data.length ) );
		}

	}

	private final IndexBuilder index = new IndexBuilder();
	private final JSONObject metaData;
	private final SCloudEncryptOutputStream output;
	private final InputStream input;
	private SCloudObject mostRecentObject;
	protected int count;
	protected int progress;

	public SCloudEncrypt( String context, JSONObject metaData, SCloudObjectRepository repository, InputStream input ) {
		this.metaData = metaData;
		this.input = input;
		output = new SCloudEncryptOutputStream( new NativePacket( new OnBlockEncryptedListenerChain( index, new SCloudObjectSave( repository ), new ProgressPublisher() ) ), context, null );
	}

	protected double getProgress() {
		return (double) progress / count;
	}

	protected int getProgressPercent() {
		return (int) Math.ceil( getProgress() * 100 );
	}

	@Override
	public String getThreadName() {
		return "SCloudEncrypt";
	}

	protected void next( SCloudObject object ) {
		mostRecentObject = object;
		onEncrypted( object );
	}

	/**
	 * @param object
	 */
	protected void onEncrypted( SCloudObject object ) {
		// By default, do nothing.
	}

	protected void onEncryptionCancelled() {
		// By default, do nothing.
	}

	/**
	 * @param object
	 */
	protected void onEncryptionComplete( SCloudObject object ) {
		// By default, do nothing.
	}

	/**
	 * @param exception
	 */
	protected void onEncryptionError( Throwable exception ) {
		// By default, do nothing.
	}

	@Override
	public void run() {

		try {

			byte [] buffer = new byte [64 * 1024];
			count = (int) Math.ceil( (double) input.available() / buffer.length );

			try {
				metaData.put( "Scloud_Segments", count );
			} catch( JSONException impossible ) {
				// No.
			}

			if( count > 1 ) {
				output.setMetaData( "{\"MediaType\":\"com.silentcircle.scloud.segment\"}" );
			} else {
				output.setMetaData( metaData.toString() );
			}

			for( int size = input.read( buffer ); size > 0; size = input.read( buffer, 0, size ) ) {
				progress++;
				output.write( buffer, 0, size );
			}

			if( count > 1 ) {
				output.setMetaData( metaData.toString() );
				progress++;
				output.write( index.getIndex().toString().getBytes( "UTF-8" ) );
			}

		} catch( IOException exception ) {
			onEncryptionError( exception );
			return;
		} catch( IllegalStateException exception ) {
			onEncryptionCancelled();
			return;
		} finally {
			IOUtils.close( input, output );
		}

		onEncryptionComplete( mostRecentObject );

	}

}
