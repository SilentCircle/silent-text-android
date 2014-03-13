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
package com.silentcircle.silenttext.util;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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

	public static int pipe( InputStream in, OutputStream out ) throws IOException {
		return pipe( in, out, SHARED_BUFFER );
	}

	public static int pipe( InputStream in, OutputStream out, byte [] buffer ) throws IOException {
		return pipe( in, out, buffer, 0, buffer.length );
	}

	public static int pipe( InputStream in, OutputStream out, byte [] buffer, int offset, int length ) throws IOException {
		int total = 0;
		for( int size = in.read( buffer, offset, length ); size > 0; size = in.read( buffer, offset, length ) ) {
			out.write( buffer, offset, size );
			total += size;
		}
		return total;
	}

	public static int pipe( InputStream in, OutputStream out, int bufferSize ) throws IOException {
		return pipe( in, out, new byte [bufferSize] );
	}

}
