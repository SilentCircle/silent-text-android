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
package com.silentcircle.silenttext.log;

import com.silentcircle.silenttext.ServiceConfiguration;

public class Log {

	public static void d( String tag, String format, Object... args ) {
		if( isEnabled() ) {
			android.util.Log.d( tag, String.format( format, args ) );
		}
	}

	public static void d( String tag, Throwable throwable, String format, Object... args ) {
		if( isEnabled() ) {
			android.util.Log.d( tag, String.format( format, args ), throwable );
		}
	}

	public static void e( String tag, String format, Object... args ) {
		if( isEnabled() ) {
			android.util.Log.e( tag, String.format( format, args ) );
		}
	}

	public static void e( String tag, Throwable throwable, String format, Object... args ) {
		if( isEnabled() ) {
			android.util.Log.e( tag, String.format( format, args ), throwable );
		}
	}

	public static void i( String tag, String format, Object... args ) {
		if( isEnabled() ) {
			android.util.Log.i( tag, String.format( format, args ) );
		}
	}

	public static void i( String tag, Throwable throwable, String format, Object... args ) {
		if( isEnabled() ) {
			android.util.Log.i( tag, String.format( format, args ), throwable );
		}
	}

	public static boolean isEnabled() {
		return ServiceConfiguration.getInstance().loggingEnabled;
	}

	public static void v( String tag, String format, Object... args ) {
		if( isEnabled() ) {
			android.util.Log.v( tag, String.format( format, args ) );
		}
	}

	public static void v( String tag, Throwable throwable, String format, Object... args ) {
		if( isEnabled() ) {
			android.util.Log.v( tag, String.format( format, args ), throwable );
		}
	}

	public static void w( String tag, String format, Object... args ) {
		if( isEnabled() ) {
			android.util.Log.w( tag, String.format( format, args ) );
		}
	}

	public static void w( String tag, Throwable throwable, String format, Object... args ) {
		if( isEnabled() ) {
			android.util.Log.w( tag, String.format( format, args ), throwable );
		}
	}

	private final String tag;

	public Log( String tag ) {
		this.tag = tag;
	}

	public void debug( String format, Object... args ) {
		d( tag, format, args );
	}

	public void debug( Throwable throwable, String format, Object... args ) {
		d( tag, throwable, format, args );
	}

	public void error( String format, Object... args ) {
		e( tag, format, args );
	}

	public void error( Throwable throwable, String format, Object... args ) {
		e( tag, throwable, format, args );
	}

	public String getTag() {
		return tag;
	}

	public void info( String format, Object... args ) {
		i( tag, format, args );
	}

	public void info( Throwable throwable, String format, Object... args ) {
		i( tag, throwable, format, args );
	}

	public void onCreate() {
		// Do nothing.
	}

	public void onDestroy() {
		// Do nothing.
	}

	public void verbose( String format, Object... args ) {
		v( tag, format, args );
	}

	public void verbose( Throwable throwable, String format, Object... args ) {
		v( tag, throwable, format, args );
	}

	public void warn( String format, Object... args ) {
		w( tag, format, args );
	}

	public void warn( Throwable throwable, String format, Object... args ) {
		w( tag, throwable, format, args );
	}

}
