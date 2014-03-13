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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.silentcircle.silenttext.crypto.Hash;
import com.silentcircle.silenttext.filter.Filterable;
import com.silentcircle.silenttext.filter.IOFilter;
import com.silentcircle.silenttext.log.Log;
import com.silentcircle.silenttext.repository.ModelAdapter;
import com.silentcircle.silenttext.repository.Repository;
import com.silentcircle.silenttext.util.IOUtils;

public abstract class BaseFileRepository<T> implements Filterable<String>, Repository<T> {

	static class ObjectNotFoundException extends RuntimeException {

		private static final long serialVersionUID = 1L;

	}

	private static final String UTF8 = "UTF-8";
	protected final File root;
	protected IOFilter<String> filter;
	protected ModelAdapter<T> adapter;
	protected final Log log = new Log( getClass().getSimpleName() );
	protected boolean pendingRemoval;

	private final Map<String, T> files = new HashMap<String, T>();

	public BaseFileRepository( File root ) {
		this( root, null );
	}

	public BaseFileRepository( File root, IOFilter<String> filter ) {
		this.root = root;
		root.mkdirs();
		this.filter = filter;
	}

	protected T cache( String hash, T object ) {
		if( hash == null || object == null ) {
			return object;
		}
		files.put( hash, object );
		return object;
	}

	protected T cached( String hash ) {
		if( hash == null ) {
			return null;
		}
		T result = files.get( hash );
		if( result == null ) {
			throw new ObjectNotFoundException();
		}
		return result;
	}

	@Override
	public void clear() {
		files.clear();
		pendingRemoval = true;
		if( delete( root ) ) {
			pendingRemoval = false;
		}
	}

	protected Collection<T> createEmptyCollection() {
		return new ArrayList<T>();
	}

	protected boolean delete( File file ) {
		if( file == null ) {
			return true;
		}
		files.remove( file.getName() );
		if( file.isDirectory() ) {
			for( File child : file.listFiles() ) {
				if( !delete( child ) ) {
					return false;
				}
			}
		}
		return file.delete();
	}

	protected T deserialize( String serial ) {
		if( adapter == null ) {
			return null;
		}
		return adapter.deserialize( serial );
	}

	@Override
	public boolean exists() {
		if( pendingRemoval ) {
			clear();
		}
		return !pendingRemoval && root.exists();
	}

	@Override
	public boolean exists( String id ) {
		return id != null && getFile( id ).exists();
	}

	protected boolean exists( T object ) {
		return object != null && getFile( object ).exists();
	}

	@Override
	public T findById( String id ) {
		try {
			return cached( Hash.sha1( id ) );
		} catch( ObjectNotFoundException exception ) {
			// Oh well.
		}
		if( !exists( id ) ) {
			return null;
		}
		File file = id == null ? null : getFile( id );
		String contents = file == null ? null : readFile( file );
		T object = contents == null ? null : deserialize( contents );
		return cache( file == null ? null : file.getName(), object );
	}

	private File getFile( String id ) {
		String hash = Hash.sha1( id );
		return id == null ? null : new File( root, hash );
	}

	private File getFile( T object ) {
		return object == null ? null : getFile( identify( object ) );
	}

	protected String identify( T object ) {
		if( adapter == null ) {
			return null;
		}
		return adapter.identify( object );
	}

	@Override
	public List<T> list() {
		if( !root.exists() ) {
			root.mkdirs();
		}
		Collection<T> objects = createEmptyCollection();
		for( File file : root.listFiles() ) {
			if( file.isDirectory() ) {
				continue;
			}
			try {
				objects.add( cached( file.getName() ) );
			} catch( ObjectNotFoundException exception ) {
				String contents = readFile( file );
				T object = contents == null ? null : deserialize( contents );
				if( object != null ) {
					objects.add( object );
					cache( file.getName(), object );
				}
			}
		}
		return new ArrayList<T>( objects );
	}

	private String readFile( File file ) {
		if( file == null || !file.exists() ) {
			return null;
		}
		InputStream in = null;
		try {
			in = new FileInputStream( file );
			byte [] buffer = new byte [in.available()];
			if( buffer.length > 0 ) {
				int size = in.read( buffer, 0, buffer.length );
				if( size > 0 ) {
					String value = new String( buffer, 0, size, UTF8 );
					value = readValue( value );
					return value;
				}
			}
		} catch( Throwable exception ) {
			log.error( exception, "READ from:%s", file );
		} finally {
			IOUtils.close( in );
		}
		file.delete();
		return null;
	}

	private String readValue( String value ) {
		return filter == null ? value : filter.filterInput( value );
	}

	@Override
	public void remove( T object ) {
		delete( getFile( object ) );
	}

	@Override
	public void save( T object ) {
		if( object == null ) {
			return;
		}
		File file = getFile( object );
		cache( file.getName(), object );
		writeFile( file, serialize( object ) );
	}

	protected String serialize( T object ) {
		if( adapter == null ) {
			return null;
		}
		return adapter.serialize( object );
	}

	public void setAdapter( ModelAdapter<T> adapter ) {
		this.adapter = adapter;
	}

	@Override
	public void setFilter( IOFilter<String> filter ) {
		this.filter = filter;
	}

	private void writeFile( File file, String contents ) {
		if( file == null || contents == null ) {
			return;
		}
		root.mkdirs();
		OutputStream out = null;
		try {
			out = new FileOutputStream( file, false );
			String value = writeValue( contents );
			byte [] buffer = value.getBytes( UTF8 );
			out.write( buffer );
			out.flush();
		} catch( IOException exception ) {
			log.error( exception, "WRITE to:%s\n%s", file, contents );
		} finally {
			IOUtils.close( out );
		}
	}

	private String writeValue( String value ) {
		return filter == null ? value : filter.filterOutput( value );
	}

}
