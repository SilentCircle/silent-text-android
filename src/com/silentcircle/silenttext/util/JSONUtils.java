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

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.twuni.twoson.JSONGenerator;

import android.text.Html;
import android.text.Spanned;

import com.silentcircle.silentstorage.util.Base64;

public class JSONUtils {

	private static final SimpleDateFormat ISO8601 = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss'Z'" );

	static {
		ISO8601.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
	}

	public static boolean getBoolean( JSONObject json, String key ) {
		if( json == null ) {
			return false;
		}
		try {
			return json.has( key ) && json.getBoolean( key );
		} catch( JSONException exception ) {
			return false;
		}
	}

	public static byte [] getBytes( JSONObject json, String key ) {
		return json == null ? null : Base64.decodeBase64( getString( json, key ) );
	}

	public static long getDate( char [] value ) {
		return value == null ? 0 : getDate( new String( value ) );
	}

	public static long getDate( JSONObject json, String key ) {
		if( json == null ) {
			return 0;
		}
		return getDate( getString( json, key ) );
	}

	public static String getDate( long value ) {
		Date date = new Date( value );
		return ISO8601.format( date );
	}

	public static long getDate( String value ) {
		if( value == null ) {
			return 0;
		}
		try {
			Date date = ISO8601.parse( value );
			return date == null ? 0 : date.getTime();
		} catch( ParseException exception ) {
			return 0;
		}
	}

	public static int getInt( JSONObject json, String key, int defaultValue ) {
		if( json == null ) {
			return defaultValue;
		}
		try {
			return json.has( key ) ? json.getInt( key ) : defaultValue;
		} catch( JSONException exception ) {
			return defaultValue;
		}
	}

	public static JSONArray getJSONArray( JSONObject json, String key ) {
		if( json == null ) {
			return new JSONArray();
		}
		try {
			return json.has( key ) ? json.getJSONArray( key ) : new JSONArray();
		} catch( JSONException exception ) {
			return new JSONArray();
		}
	}

	public static JSONObject getJSONObject( JSONArray json, int index ) {
		if( json == null ) {
			return new JSONObject();
		}
		try {
			return json.getJSONObject( index );
		} catch( JSONException exception ) {
			return new JSONObject();
		}
	}

	public static JSONObject getJSONObject( JSONObject json, String key ) {
		if( json == null ) {
			return null;
		}
		try {
			return json.has( key ) ? json.getJSONObject( key ) : null;
		} catch( JSONException exception ) {
			return null;
		}
	}

	public static String getString( JSONArray json, int index ) {
		if( json == null ) {
			return null;
		}
		try {
			return index < json.length() ? json.getString( index ) : null;
		} catch( JSONException exception ) {
			return null;
		}
	}

	public static String getString( JSONObject json, String key ) {
		if( json == null ) {
			return null;
		}
		try {
			return json.has( key ) ? json.getString( key ) : null;
		} catch( JSONException exception ) {
			return null;
		}
	}

	public static void next( JSONGenerator json, String key, CharSequence value ) throws IOException {
		if( !StringUtils.isEmpty( value ) ) {
			json.next();
			write( json, key, value );
		}
	}

	public static JSONArray parseJSONArray( String json ) {
		if( json == null ) {
			return null;
		}
		try {
			return new JSONArray( json );
		} catch( JSONException exception ) {
			return null;
		}
	}

	public static JSONObject parseJSONObject( String json ) {
		if( json == null ) {
			return null;
		}
		try {
			return new JSONObject( json );
		} catch( JSONException exception ) {
			return null;
		}
	}

	public static void putDate( JSONObject json, String key, long value ) {
		if( json == null || key == null || value <= 0 ) {
			return;
		}
		try {
			json.put( key, ISO8601.format( new Date( value ) ) );
		} catch( JSONException exception ) {
			// Ignore this.
		}
	}

	public static JSONArray readJSONArray( InputStream in ) {
		return parseJSONArray( IOUtils.readAsString( in ) );
	}

	public static JSONObject readJSONObject( InputStream in ) {
		return parseJSONObject( IOUtils.readAsString( in ) );
	}

	public static String toFormattedText( JSONObject json ) {
		return toFormattedText( json, new StringBuilder(), 0 );
	}

	private static String toFormattedText( JSONObject json, StringBuilder text, int indent ) {
		Iterator<?> it = json.keys();
		while( it.hasNext() ) {
			String key = String.valueOf( it.next() );
			Object value = json.opt( key );
			for( int i = 0; i < indent; i++ ) {
				text.append( " " );
			}
			text.append( key ).append( ':' );
			if( value instanceof JSONObject ) {
				text.append( "\n" );
				toFormattedText( (JSONObject) value, text, indent + 2 );
			} else {
				text.append( ' ' ).append( value ).append( "\n" );
			}
		}
		return text.toString();
	}

	public static Spanned toHTML( JSONObject json ) {
		return toHTML( json, new StringBuilder(), 0 );
	}

	private static Spanned toHTML( JSONObject json, StringBuilder html, int indent ) {
		Iterator<?> it = json.keys();
		while( it.hasNext() ) {
			String key = String.valueOf( it.next() );
			Object value = json.opt( key );
			for( int i = 0; i < indent; i++ ) {
				html.append( "-" );
			}
			html.append( "<b>" ).append( key ).append( "</b>:" );
			if( value instanceof JSONObject ) {
				html.append( "<br/>\n" );
				toHTML( (JSONObject) value, html, indent + 2 );
			} else {
				html.append( ' ' ).append( value ).append( "<br/>\n" );
			}
		}
		return Html.fromHtml( html.toString() );
	}

	public static void write( JSONGenerator json, String key, CharSequence value ) throws IOException {
		if( !StringUtils.isEmpty( value ) ) {
			json.writeKey( key );
			json.writeString( com.silentcircle.silentstorage.util.IOUtils.toByteArray( value ) );
		}
	}

}
