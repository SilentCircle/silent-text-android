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
package com.silentcircle.silenttext.crypto;

import java.math.BigInteger;
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
		if( in == null ) {
			return null;
		}
		char [] out = new char [in.length()];
		for( int i = 0; i < out.length; i++ ) {
			out[i] = in.charAt( i );
		}
		return out;
	}

	public static byte [] copyOf( byte [] array ) {
		return copyOf( array, array == null ? 0 : array.length );
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
		if( array == null ) {
			return null;
		}
		char [] copy = new char [array.length];
		for( int i = 0; i < array.length; i++ ) {
			copy[i] = array[i];
		}
		return copy;
	}

	public static byte [] randomBytes( int length ) {
		return length < 0 ? null : randomize( new byte [length] );
	}

	public static int randomInt() {
		return new BigInteger( CryptoUtils.randomBytes( 4 ) ).intValue();
	}

	public static byte [] randomize( byte [] buffer ) {
		if( buffer == null ) {
			return null;
		}
		RANDOM.nextBytes( buffer );
		return buffer;
	}

	public static ByteBuffer randomize( ByteBuffer buffer ) {
		if( buffer != null && buffer.hasArray() ) {
			for( int i = 0; i < buffer.limit(); i++ ) {
				buffer.put( i, (byte) RANDOM.nextInt() );
			}
		}
		return buffer;
	}

	public static char [] randomize( char [] buffer ) {
		if( buffer == null ) {
			return null;
		}
		for( int i = 0; i < buffer.length; i++ ) {
			buffer[i] = (char) RANDOM.nextInt();
		}
		return buffer;
	}

	public static CharBuffer randomize( CharBuffer buffer ) {
		if( buffer != null && buffer.hasArray() ) {
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
		if( buffer == null ) {
			return null;
		}
		for( int i = 0; i < buffer.length(); i++ ) {
			buffer.setCharAt( i, (char) RANDOM.nextInt() );
		}
		return buffer;
	}

	public static StringBuilder randomize( StringBuilder buffer ) {
		if( buffer == null ) {
			return null;
		}
		for( int i = 0; i < buffer.length(); i++ ) {
			buffer.setCharAt( i, (char) RANDOM.nextInt() );
		}
		return buffer;
	}

	/**
	 * This implementation is broken. Use {@link #toByteArraySafe(ByteBuffer)} instead.
	 * 
	 * @deprecated
	 */
	@Deprecated
	public static byte [] toByteArray( ByteBuffer buffer ) {
		return buffer == null ? null : copyOf( buffer.array() );
	}

	/**
	 * This implementation is broken. Use {@link #toByteArraySafe(char[])} instead.
	 * 
	 * @deprecated
	 */
	@Deprecated
	public static byte [] toByteArray( char [] buffer ) {
		return buffer == null ? null : toByteArray( CharBuffer.wrap( buffer ) );
	}

	/**
	 * This implementation is broken. Use {@link #toByteArraySafe(CharBuffer)} instead.
	 * 
	 * @deprecated
	 */
	@Deprecated
	public static byte [] toByteArray( CharBuffer sensitiveData ) {
		if( sensitiveData == null ) {
			return null;
		}
		ByteBuffer bytes = UTF8.encode( sensitiveData );
		byte [] array = toByteArray( bytes );
		randomize( bytes );
		return array;
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
		if( sensitiveData == null ) {
			return null;
		}
		char [] buffer = new char [sensitiveData.length()];
		sensitiveData.getChars( 0, buffer.length, buffer, 0 );
		return toByteArray( buffer );
	}

	public static byte [] toByteArray( StringBuffer sensitiveData ) {
		if( sensitiveData == null ) {
			return null;
		}
		char [] buffer = new char [sensitiveData.length()];
		sensitiveData.getChars( 0, buffer.length, buffer, 0 );
		return toByteArray( buffer );
	}

	public static byte [] toByteArray( StringBuilder sensitiveData ) {
		if( sensitiveData == null ) {
			return null;
		}
		char [] buffer = new char [sensitiveData.length()];
		sensitiveData.getChars( 0, buffer.length, buffer, 0 );
		return toByteArray( buffer );
	}

	public static byte [] toByteArraySafe( ByteBuffer buffer ) {
		if( buffer == null ) {
			return null;
		}
		byte [] array = new byte [buffer.remaining()];
		for( int i = 0; i < array.length; i++ ) {
			array[i] = buffer.get( i );
		}
		return array;
	}

	public static byte [] toByteArraySafe( char [] buffer ) {
		return buffer == null ? null : toByteArraySafe( CharBuffer.wrap( buffer ) );
	}

	public static byte [] toByteArraySafe( CharBuffer sensitiveData ) {
		if( sensitiveData == null ) {
			return null;
		}
		ByteBuffer bytes = UTF8.encode( sensitiveData );
		byte [] array = toByteArraySafe( bytes );
		randomize( bytes );
		return array;
	}

	/**
	 * This implementation is broken. Use {@link #toCharArraySafe(byte[])} instead.
	 * 
	 * @deprecated
	 */
	@Deprecated
	public static char [] toCharArray( byte [] buffer ) {
		return buffer == null ? null : toCharArray( ByteBuffer.wrap( buffer ) );
	}

	/**
	 * This implementation is broken. Use {@link #toCharArraySafe(ByteBuffer)} instead.
	 * 
	 * @deprecated
	 */
	@Deprecated
	public static char [] toCharArray( ByteBuffer buffer ) {
		if( buffer == null ) {
			return null;
		}
		CharBuffer chars = UTF8.decode( buffer );
		char [] array = toCharArray( chars );
		randomize( chars );
		return array;
	}

	/**
	 * This implementation is broken. Use {@link #toCharArraySafe(CharBuffer)} instead.
	 * 
	 * @deprecated
	 */
	@Deprecated
	public static char [] toCharArray( CharBuffer buffer ) {
		return buffer == null ? null : copyOf( buffer.array() );
	}

	public static char [] toCharArray( CharSequence sequence ) {
		if( sequence instanceof CharBuffer ) {
			return toCharArray( (CharBuffer) sequence );
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
		if( buffer == null ) {
			return null;
		}
		char [] out = new char [buffer.length()];
		buffer.getChars( 0, out.length, out, 0 );
		return out;
	}

	public static char [] toCharArray( StringBuffer buffer ) {
		if( buffer == null ) {
			return null;
		}
		char [] out = new char [buffer.length()];
		buffer.getChars( 0, out.length, out, 0 );
		return out;
	}

	public static char [] toCharArray( StringBuilder buffer ) {
		if( buffer == null ) {
			return null;
		}
		char [] out = new char [buffer.length()];
		buffer.getChars( 0, out.length, out, 0 );
		return out;
	}

	public static char [] toCharArraySafe( byte [] buffer ) {
		return buffer == null ? null : toCharArraySafe( ByteBuffer.wrap( buffer ) );
	}

	public static char [] toCharArraySafe( ByteBuffer buffer ) {
		if( buffer == null ) {
			return null;
		}
		CharBuffer chars = UTF8.decode( buffer );
		char [] array = toCharArraySafe( chars );
		randomize( chars );
		return array;
	}

	public static char [] toCharArraySafe( CharBuffer buffer ) {

		if( buffer == null ) {
			return null;
		}

		char [] array = new char [buffer.remaining()];

		int length = array.length;

		for( int i = 0; i < length; i++ ) {
			char c = buffer.charAt( i );
			if( c == '\0' ) {
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

}
