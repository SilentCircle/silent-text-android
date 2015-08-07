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
package com.silentcircle.silenttext.io;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.silentcircle.silenttext.model.Contact;
import com.silentcircle.silenttext.util.IOUtils;

public class VCardReader {

	private static String join( String [] parts, String delimiter ) {
		if( parts == null ) {
			return null;
		}
		StringBuilder s = new StringBuilder();
		int count = parts.length;
		boolean first = true;
		for( int i = 0; i < count; i++ ) {
			String part = parts[i];
			if( !"".equals( part ) ) {
				if( !first ) {
					s.append( delimiter );
				}
				s.append( part );
				first = false;
			}
		}
		return s.toString();
	}

	public static Contact read( InputStream in ) {
		return read( IOUtils.readAsString( in ) );
	}

	private static Contact read( Map<String, String> properties ) {

		if( properties == null ) {
			return null;
		}

		Contact contact = new Contact();

		String displayName = properties.get( "FN" );

		if( displayName == null ) {
			displayName = join( split( properties.get( "N" ), ";" ), ", " );
		}

		contact.setAlias( displayName );

		for( String value : properties.values() ) {
			if( value.endsWith( "@silentcircle.com" ) ) {
				contact.setUsername( value );
				break;
			}
		}

		return contact;

	}

	public static Contact read( String raw ) {
		return read( toProperties( raw ) );
	}

	private static String [] split( String in, String delimiter ) {
		return in == null ? null : in.split( delimiter );
	}

	private static Map<String, String> toProperties( String raw ) {
		Map<String, String> properties = new HashMap<String, String>();
		String [] lines = raw.split( "\r?\n" );
		for( String line : lines ) {
			int index = line.indexOf( ':' );
			if( index > 0 ) {
				String key = line.substring( 0, index );
				String value = line.substring( index + 1 );
				properties.put( key, value );
			}
		}
		return properties;
	}

}
