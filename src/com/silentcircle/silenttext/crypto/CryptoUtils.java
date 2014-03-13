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
package com.silentcircle.silenttext.crypto;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Random;

import android.text.SpannableStringBuilder;

public class CryptoUtils {

	private static final Charset UTF8 = Charset.forName( "UTF-8" );
	private static final Random RANDOM = new SecureRandom();

	public static byte [] clear( byte [] array ) {
		if( array != null ) {
			randomize( array );
		}
		return null;
	}

	public static char [] clear( char [] array ) {
		if( array != null ) {
			randomize( array );
		}
		return null;
	}

	public static CharSequence clear( CharSequence sequence ) {
		if( sequence != null ) {
			randomize( sequence );
		}
		return null;
	}

	public static char [] copyAsCharArray( CharSequence in ) {
		char [] out = new char [in.length()];
		for( int i = 0; i < out.length; i++ ) {
			out[i] = in.charAt( i );
		}
		return out;
	}

	public static byte [] copyOf( byte [] array, int length ) {
		if( array == null ) {
			return null;
		}
		byte [] copy = new byte [length];
		int count = length < array.length ? length : array.length;
		for( int i = 0; i < count; i++ ) {
			copy[i] = array[i];
		}
		return copy;
	}

	public static char [] copyOf( char [] array ) {
		char [] copy = new char [array.length];
		for( int i = 0; i < array.length; i++ ) {
			copy[i] = array[i];
		}
		return copy;
	}

	public static byte [] randomBytes( int length ) {
		return randomize( new byte [length] );
	}

	public static byte [] randomize( byte [] buffer ) {
		RANDOM.nextBytes( buffer );
		return buffer;
	}

	public static char [] randomize( char [] buffer ) {
		for( int i = 0; i < buffer.length; i++ ) {
			buffer[i] = (char) RANDOM.nextInt();
		}
		return buffer;
	}

	public static CharBuffer randomize( CharBuffer buffer ) {
		if( buffer.hasArray() ) {
			for( int i = 0; i < buffer.limit(); i++ ) {
				buffer.put( i, (char) RANDOM.nextInt() );
			}
		}
		return buffer;
	}

	public static CharSequence randomize( CharSequence sequence ) {
		if( sequence instanceof CharBuffer ) {
			return randomize( (CharBuffer) sequence );
		} else if( sequence instanceof StringBuffer ) {
			return randomize( (StringBuffer) sequence );
		} else if( sequence instanceof StringBuilder ) {
			return randomize( (StringBuilder) sequence );
		} else {
			throw new ReadOnlyBufferException();
		}
	}

	public static StringBuffer randomize( StringBuffer buffer ) {
		for( int i = 0; i < buffer.length(); i++ ) {
			buffer.setCharAt( i, (char) RANDOM.nextInt() );
		}
		return buffer;
	}

	public static StringBuilder randomize( StringBuilder buffer ) {
		for( int i = 0; i < buffer.length(); i++ ) {
			buffer.setCharAt( i, (char) RANDOM.nextInt() );
		}
		return buffer;
	}

	public static byte [] toByteArray( char [] buffer ) {
		return toByteArray( CharBuffer.wrap( buffer ) );
	}

	public static byte [] toByteArray( CharBuffer sensitiveData ) {
		return UTF8.encode( sensitiveData ).array();
	}

	public static byte [] toByteArray( CharSequence sequence ) {
		if( sequence instanceof CharBuffer ) {
			return toByteArray( (CharBuffer) sequence );
		} else if( sequence instanceof StringBuffer ) {
			return toByteArray( (StringBuffer) sequence );
		} else if( sequence instanceof StringBuilder ) {
			return toByteArray( (StringBuilder) sequence );
		} else if( sequence instanceof SpannableStringBuilder ) {
			return toByteArray( (SpannableStringBuilder) sequence );
		} else {
			throw new ReadOnlyBufferException();
		}
	}

	public static byte [] toByteArray( SpannableStringBuilder sensitiveData ) {
		char [] buffer = new char [sensitiveData.length()];
		sensitiveData.getChars( 0, buffer.length, buffer, 0 );
		return toByteArray( buffer );
	}

	public static byte [] toByteArray( StringBuffer sensitiveData ) {
		char [] buffer = new char [sensitiveData.length()];
		sensitiveData.getChars( 0, buffer.length, buffer, 0 );
		return toByteArray( buffer );
	}

	public static byte [] toByteArray( StringBuilder sensitiveData ) {
		char [] buffer = new char [sensitiveData.length()];
		sensitiveData.getChars( 0, buffer.length, buffer, 0 );
		return toByteArray( buffer );
	}

	public static char [] toCharArray( byte [] buffer ) {
		return toCharArray( buffer, false );
	}

	public static char [] toCharArray( byte [] buffer, boolean nullTerminated ) {
		return toCharArray( ByteBuffer.wrap( buffer ), nullTerminated );
	}

	public static char [] toCharArray( ByteBuffer buffer ) {
		return toCharArray( buffer, false );
	}

	public static char [] toCharArray( ByteBuffer buffer, boolean nullTerminated ) {
		return toCharArray( UTF8.decode( buffer ), nullTerminated );
	}

	public static char [] toCharArray( CharBuffer buffer, boolean nullTerminated ) {

		if( buffer == null ) {
			return null;
		}

		if( !nullTerminated ) {
			return buffer.array();
		}

		char [] array = new char [buffer.remaining()];

		int length = array.length;

		for( int i = 0; i < length; i++ ) {
			char c = buffer.charAt( i );
			if( c == '\0' && nullTerminated ) {
				length = i;
				break;
			}
			array[i] = c;
		}

		if( length < array.length ) {
			char [] copy = new char [length];
			System.arraycopy( array, 0, copy, 0, length );
			randomize( array );
			return copy;
		}

		return array;

	}

	public static char [] toCharArray( CharSequence sequence ) {
		return toCharArray( sequence, false );
	}

	public static char [] toCharArray( CharSequence sequence, boolean nullTerminated ) {
		if( sequence instanceof CharBuffer ) {
			return toCharArray( sequence, nullTerminated );
		} else if( sequence instanceof StringBuffer ) {
			return toCharArray( (StringBuffer) sequence );
		} else if( sequence instanceof StringBuilder ) {
			return toCharArray( (StringBuilder) sequence );
		} else if( sequence instanceof SpannableStringBuilder ) {
			return toCharArray( (SpannableStringBuilder) sequence );
		} else {
			throw new ReadOnlyBufferException();
		}
	}

	public static char [] toCharArray( SpannableStringBuilder buffer ) {
		char [] out = new char [buffer.length()];
		buffer.getChars( 0, out.length, out, 0 );
		return out;
	}

	public static char [] toCharArray( StringBuffer buffer ) {
		char [] out = new char [buffer.length()];
		buffer.getChars( 0, out.length, out, 0 );
		return out;
	}

	public static char [] toCharArray( StringBuilder buffer ) {
		char [] out = new char [buffer.length()];
		buffer.getChars( 0, out.length, out, 0 );
		return out;
	}

}
