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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {

	public static boolean equals( String a, String b ) {
		return a == null ? a == b : b != null && a.equals( b );
	}

	/**
	 * This method is an alternative to {@link String#replaceAll(String, String)} for performing a
	 * multi-line search for a specific pattern.
	 *
	 * @param input
	 *            The input string.
	 * @param regex
	 *            A regular expression that contains at least {@code group} parenthetical groups.
	 * @param group
	 *            The index of the group within the given {@code regex} to return.
	 * @return The string matching the pattern in the given {@code group}. If the string was not
	 *         found, then {@code null} is returned.
	 * @throws IllegalArgumentException
	 *             if the given input string matched the given regular expression, but the requested
	 *             group is greater than the number of groups specified in the given regular
	 *             expression.
	 */
	public static String find( String input, String regex, int group ) {
		Pattern pattern = Pattern.compile( regex, Pattern.MULTILINE | Pattern.DOTALL );
		Matcher matcher = pattern.matcher( input );
		if( matcher.find() ) {
			if( matcher.groupCount() < group ) {
				throw new IllegalArgumentException();
			}
			return matcher.group( group );
		}
		return null;
	}

	public static String formatUsername( String username ) {
		return username != null ? username.replaceAll( "^([^@]+)@(.+)$", "@$1" ) : null;
	}

	public static String fromByteArray( byte [] b ) {
		try {
			return new String( b, "UTF-8" );
		} catch( Throwable exception ) {
			return null;
		}
	}

	public static String getFirstName( String fullName ) {
		if( fullName == null ) {
			return fullName;
		}
		int index = fullName.indexOf( ' ' );
		if( index < 0 ) {
			return fullName;
		}
		if( index < 3 ) {
			int nextIndex = fullName.indexOf( ' ', index + 1 );
			if( nextIndex < 0 ) {
				return fullName.substring( 0, index );
			}
			return fullName.substring( 0, nextIndex );
		}
		return fullName.substring( 0, index );
	}

	public static String getStringUntil( String input, String until ) {
		return getStringUntil( input, until, 0 );
	}

	public static String getStringUntil( String input, String until, int searchOffset ) {

		if( input == null ) {
			return null;
		}

		int index = input.indexOf( until, searchOffset );

		if( index > -1 ) {
			return input.substring( 0, index );
		}

		return input;

	}

	public static boolean isAnyOf( String input, String... possibleValues ) {
		if( possibleValues == null ) {
			return false;
		}

		boolean is = false;
		for( int i = 0; !is && i < possibleValues.length; i++ ) {
			String possibleValue = possibleValues[i];
			if( input == null ) {
				if( possibleValue == null ) {
					is = true;
					break;
				}
				continue;
			}
			is = possibleValue != null && input.equals( possibleValue );
		}
		return is;
	}

	public static boolean isEmpty( CharSequence s ) {
		return s == null || s.length() < 1;
	}

	public static boolean isMinimumLength( String s, int length ) {
		return s != null && s.length() >= length;
	}

	public static String pad( String in, int blockSize, char paddingCharacter ) {

		if( in == null ) {
			return null;
		}

		int padding = blockSize - in.length() % blockSize;

		if( padding >= blockSize ) {
			return in;
		}

		StringBuilder sb = new StringBuilder( in );

		for( int i = 0; i < padding; i++ ) {
			sb.append( paddingCharacter );
		}

		return sb.toString();

	}

	public static byte [] toByteArray( String s ) {
		try {
			return s.getBytes( "UTF-8" );
		} catch( Throwable exception ) {
			return null;
		}
	}

}
