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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.silentcircle.scloud.model.SCloudObject;
import com.silentcircle.scloud.model.json.JSONSCloudObjectAdapter;
import com.silentcircle.silenttext.crypto.Hash;
import com.silentcircle.silenttext.filter.IOFilter;
import com.silentcircle.silenttext.repository.SCloudObjectRepository;
import com.silentcircle.silenttext.util.IOUtils;

public class FileSCloudObjectRepository extends BaseFileRepository<SCloudObject> implements SCloudObjectRepository {

	private static final String DESCRIPTOR = ":descriptor";
	private static final String DATA = ":data";
	private static final ByteArrayOutputStream SHARED_BUFFER = new ByteArrayOutputStream();

	private final File dataRoot;

	public FileSCloudObjectRepository( File root ) {
		this( root, root );
	}

	public FileSCloudObjectRepository( File root, File dataRoot ) {
		this( root, dataRoot, null );
	}

	public FileSCloudObjectRepository( File root, File dataRoot, IOFilter<String> filter ) {
		super( new File( root, Hash.sha1( DESCRIPTOR ) ), filter );
		this.dataRoot = new File( dataRoot, Hash.sha1( root.getName() + DATA ) );
		this.dataRoot.mkdirs();
		adapter = new JSONSCloudObjectAdapter();
		migrateDataToEphemeralRoot();
	}

	public FileSCloudObjectRepository( File root, IOFilter<String> filter ) {
		this( root, root, filter );
	}

	@Override
	protected SCloudObject cache( String hash, SCloudObject object ) {
		// Do not cache SCloud objects.
		return object;
	}

	@Override
	public void clear() {
		super.clear();
		delete( dataRoot );
	}

	@Override
	protected SCloudObject deserialize( String serial ) {
		return adapter.deserialize( serial );
	}

	private File getDataFile( SCloudObject object ) {
		dataRoot.mkdirs();
		return new File( dataRoot, Hash.sha1( object.getLocator() ) );
	}

	@Override
	protected String identify( SCloudObject object ) {
		return adapter.identify( object );
	}

	private void migrateDataToEphemeralRoot() {
		File permanent = new File( root, dataRoot.getName() );
		if( !permanent.getAbsolutePath().equals( dataRoot.getAbsolutePath() ) && permanent.exists() ) {
			dataRoot.delete();
			permanent.renameTo( dataRoot );
		}
	}

	@Override
	public byte [] read( SCloudObject object ) {
		InputStream input = null;
		try {
			input = new FileInputStream( getDataFile( object ) );
			SHARED_BUFFER.reset();
			int length = IOUtils.pipe( input, SHARED_BUFFER );
			object.setData( SHARED_BUFFER.toByteArray(), 0, length );
		} catch( IOException exception ) {
			throw new RuntimeException( exception );
		} finally {
			IOUtils.close( input );
		}
		return object.getData();
	}

	@Override
	protected String serialize( SCloudObject object ) {
		return adapter.serialize( object );
	}

	@Override
	public void write( SCloudObject object ) {
		if( object.getData() == null ) {
			return;
		}
		OutputStream output = null;
		try {
			output = new FileOutputStream( getDataFile( object ), false );
			output.write( object.getData(), object.getOffset(), object.getSize() );
			output.flush();
		} catch( IOException exception ) {
			throw new RuntimeException( exception );
		} finally {
			IOUtils.close( output );
		}
	}

}
