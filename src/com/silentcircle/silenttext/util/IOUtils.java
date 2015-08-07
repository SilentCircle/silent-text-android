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
package com.silentcircle.silenttext.util;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URL;
import java.util.Stack;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import com.silentcircle.silenttext.listener.OnProgressUpdateListener;

public class IOUtils {

	private static final int KB = 1024;

	private static final byte [] SHARED_BUFFER = new byte [32 * KB];

	public static void close( Closeable... closeables ) {
		for( int i = 0; i < closeables.length; i++ ) {
			Closeable closeable = closeables[i];
			if( closeable == null ) {
				continue;
			}
			try {
				closeable.close();
			} catch( IOException exception ) {
				// Ignore it.
			}
		}
	}

	public static void close( Socket... sockets ) {
		for( Socket socket : sockets ) {
			if( socket != null ) {
				try {
					socket.close();
				} catch( Throwable ignore ) {
					// Okay to ignore this.
				}
			}
		}
	}

	public static int copy( byte [] source, int sourceOffset, byte [] target, int targetOffset, int length ) {
		int size = 0;
		for( size = 0; size < length; size++ ) {
			int sourceIndex = sourceOffset + size;
			int targetIndex = targetOffset + size;
			if( sourceIndex >= source.length || targetIndex >= target.length ) {
				return size;
			}
			target[targetIndex] = source[sourceIndex];
		}
		return size;
	}

	public static boolean delete( File file ) {
		if( file != null ) {
			Stack<File> pending = new Stack<File>();
			pending.push( file );
			while( !pending.isEmpty() ) {
				File parent = pending.pop();
				if( !parent.delete() ) {
					if( !parent.isDirectory() ) {
						return false;
					}
					pending.push( parent );
					if( parent.isDirectory() ) {
						File [] children = parent.listFiles();
						if( children == null || children.length < 1 ) {
							return false;
						}
						for( File child : children ) {
							pending.push( child );
						}
					}
				}
			}
		}
		return true;
	}

	public static void flush( Flushable... flushables ) {
		for( int i = 0; i < flushables.length; i++ ) {
			Flushable flushable = flushables[i];
			if( flushable == null ) {
				continue;
			}
			try {
				flushable.flush();
			} catch( IOException exception ) {
				// Ignore it.
			}
		}
	}

	public static boolean isIdentical( InputStream a, InputStream b ) {
		boolean identical = true;
		try {
			while( identical ) {
				int m = a.read();
				int n = b.read();
				if( m != n ) {
					identical = false;
					break;
				}
				if( m == -1 ) {
					break;
				}
			}
		} catch( IOException exception ) {
			identical = false;
		} finally {
			IOUtils.close( a, b );
		}
		return identical;
	}

	public static InputStream openURL( CharSequence url ) throws IOException {
		return new URL( String.valueOf( url ) ).openStream();
	}

	public static synchronized long pipe( InputStream in, OutputStream out ) throws IOException {
		return pipe( in, out, SHARED_BUFFER );
	}

	public static long pipe( InputStream in, OutputStream out, byte [] buffer ) throws IOException {
		return pipe( in, out, buffer, 0, buffer.length );
	}

	public static long pipe( InputStream in, OutputStream out, byte [] buffer, int offset, int length ) throws IOException {
		return pipe( in, out, buffer, offset, length, null );
	}

	public static long pipe( InputStream in, OutputStream out, byte [] buffer, int offset, int length, OnProgressUpdateListener onProgressUpdate ) throws IOException {
		long total = 0;
		for( int size = in.read( buffer, offset, length ); size > 0; size = in.read( buffer, offset, length ) ) {
			out.write( buffer, offset, size );
			total += size;
			if( onProgressUpdate != null ) {
				onProgressUpdate.onProgressUpdate( total );
			}
		}
		return total;
	}

	public static long pipe( InputStream in, OutputStream out, byte [] buffer, OnProgressUpdateListener onProgressUpdate ) throws IOException {
		return pipe( in, out, buffer, 0, buffer.length, onProgressUpdate );
	}

	public static long pipe( InputStream in, OutputStream out, int bufferSize ) throws IOException {
		return pipe( in, out, new byte [bufferSize] );
	}

	public static synchronized long pipe( InputStream in, OutputStream out, OnProgressUpdateListener onProgressUpdate ) throws IOException {
		return pipe( in, out, SHARED_BUFFER, onProgressUpdate );
	}

	public static String readAsString( File file ) {
		InputStream in = null;
		try {
			in = new FileInputStream( file );
			return readAsString( in );
		} catch( IOException exception ) {
			return null;
		} finally {
			close( in );
		}
	}

	public static String readAsString( HttpEntity entity ) {
		if( entity == null ) {
			return null;
		}
		String body = null;
		try {
			InputStream content = entity.getContent();
			body = readAsString( content );
			entity.consumeContent();
		} catch( IOException exception ) {
			// Ignore.
		}
		return body;
	}

	public static String readAsString( HttpResponse response ) {
		return response != null ? readAsString( response.getEntity() ) : null;
	}

	public static String readAsString( InputStream in ) {
		return new String( readFully( in ) );
	}

	public static String readAsString( InputStream in, String charsetName ) {
		byte [] buffer = readFully( in );
		try {
			return new String( buffer, charsetName );
		} catch( UnsupportedEncodingException exception ) {
			return new String( buffer );
		}
	}

	public static byte [] readFully( File file ) {
		try {
			return readFully( new FileInputStream( file ) );
		} catch( FileNotFoundException exception ) {
			return null;
		}
	}

	public static byte [] readFully( HttpEntity entity ) {
		if( entity == null ) {
			return null;
		}
		byte [] body = null;
		try {
			InputStream content = entity.getContent();
			body = readFully( content );
			entity.consumeContent();
		} catch( IOException exception ) {
			// Ignore.
		}
		return body;
	}

	public static byte [] readFully( HttpResponse response ) {
		return response != null ? readFully( response.getEntity() ) : null;
	}

	public static synchronized byte [] readFully( InputStream in ) {
		return readFully( in, SHARED_BUFFER );
	}

	public static byte [] readFully( InputStream in, byte [] buffer ) {
		return readFully( in, buffer, 0, buffer.length );
	}

	public static byte [] readFully( InputStream in, byte [] buffer, int offset, int length ) {
		if( in == null ) {
			return null;
		}
		ByteArrayOutputStream out = null;
		try {
			out = new ByteArrayOutputStream();
			IOUtils.pipe( in, out, buffer, offset, length );
			return out.toByteArray();
		} catch( IOException exception ) {
			return null;
		} finally {
			IOUtils.close( in, out );
		}
	}

	/**
	 * Opens the given file, reads the first four bytes, closes the file, then returns the result as
	 * an integer.
	 * 
	 * @return An integer representing the first four bytes of the given file. If an error occurred,
	 *         the error is silently discarded and this method returns 0.
	 */
	public static int readIntegerFromFile( File file ) {
		InputStream in = null;
		try {
			in = new FileInputStream( file );
			return new DataInputStream( in ).readInt();
		} catch( IOException exception ) {
			return 0;
		} finally {
			IOUtils.close( in );
		}
	}

	public static void write( byte [] buffer, File file ) throws IOException {
		if( buffer != null ) {
			write( buffer, 0, buffer.length, file );
		}
	}

	public static void write( byte [] buffer, int offset, int length, File file ) throws IOException {
		if( buffer == null ) {
			return;
		}
		OutputStream out = null;
		try {
			out = new FileOutputStream( file, false );
			out.write( buffer, offset, length );
			flush( out );
		} catch( IOException exception ) {
			close( out );
			file.delete();
			throw exception;
		} finally {
			close( out );
		}
	}

	/**
	 * Replaces the contents of the given file with the binary representation of the given integer.
	 * 
	 * @throws IOException
	 *             if anything went wrong while attempting to write to the file.
	 */
	public static void writeIntegerToFile( int value, File file ) throws IOException {
		OutputStream out = null;
		try {
			out = new FileOutputStream( file, false );
			new DataOutputStream( out ).writeInt( value );
		} finally {
			IOUtils.close( out );
		}
	}

}
