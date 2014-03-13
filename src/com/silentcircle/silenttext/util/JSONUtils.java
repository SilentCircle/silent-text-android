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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.jivesoftware.smack.util.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
		return json == null ? null : Base64.decode( getString( json, key ) );
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

}
