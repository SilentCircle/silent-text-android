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
package com.silentcircle.silenttext.client;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class URLBuilder {

	private static final String HTTPS_PROTOCOL = "https://";
	private static final String DEFAULT_ENCODING = "UTF-8";
	private static final String EQUALS = "=";
	private static final String NEXT_QUERY = "&";
	private static final String FIRST_QUERY = "?";

	public static String build( String host, int port ) {
		StringBuilder url = new StringBuilder();
		url.append( HTTPS_PROTOCOL );
		url.append( host );
		if( port != 443 ) {
			url.append( ":" ).append( port );
		}
		return url.toString();
	}

	public static String encodeURIComponent( String component ) {
		try {
			return URLEncoder.encode( component, DEFAULT_ENCODING );
		} catch( UnsupportedEncodingException exception ) {
			return component;
		}
	}

	private final StringBuilder buffer = new StringBuilder();

	public URLBuilder( String... initialValue ) {
		for( String value : initialValue ) {
			append( value );
		}
	}

	public URLBuilder append( String text ) {
		buffer.append( text );
		return this;
	}

	public CharSequence build() {
		return buffer;
	}

	public URLBuilder component( String component ) {
		if( buffer.charAt( buffer.length() - 1 ) != '/' ) {
			buffer.append( "/" );
		}
		buffer.append( encodeURIComponent( component ) );
		return this;
	}

	public URLBuilder query( String key, CharSequence value ) {
		buffer.append( buffer.indexOf( FIRST_QUERY ) > -1 ? NEXT_QUERY : FIRST_QUERY );
		buffer.append( encodeURIComponent( key ) ).append( EQUALS ).append( value );
		return this;
	}

	public URLBuilder query( String key, String value ) {
		buffer.append( buffer.indexOf( FIRST_QUERY ) > -1 ? NEXT_QUERY : FIRST_QUERY );
		buffer.append( encodeURIComponent( key ) ).append( EQUALS ).append( encodeURIComponent( value ) );
		return this;
	}

	@Override
	public String toString() {
		return build().toString();
	}

}
