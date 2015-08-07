/*
Copyright (C) 2014-2015, Silent Circle, LLC. All rights reserved.

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

import java.util.Date;

import android.content.Context;
import android.content.res.Resources;
import android.text.format.DateFormat;

import com.silentcircle.silenttext.R;

public class DateUtils {

	public static CharSequence getRelativeTimeSpanString( Context context, long time ) {
		return android.text.format.DateUtils.getRelativeTimeSpanString( context, time );
	}

	public static CharSequence getShortTimeString( Context context, long interval ) {
		return getShortTimeString( context.getResources(), interval );
	}

	public static CharSequence getShortTimeString( Resources resources, long interval ) {
		if( interval >= DAY ) {
			int d = (int) ( interval / DAY );
			return resources.getQuantityString( R.plurals.short_time_days, d, Integer.valueOf( d ) );
		}
		if( interval >= HOUR ) {
			int h = (int) ( interval / HOUR );
			return resources.getQuantityString( R.plurals.short_time_hours, h, Integer.valueOf( h ) );
		}
		if( interval >= MINUTE ) {
			int m = (int) ( interval / MINUTE );
			return resources.getQuantityString( R.plurals.short_time_minutes, m, Integer.valueOf( m ) );
		}

		int s = interval >= SECOND ? (int) ( interval / SECOND ) : 0;
		return resources.getQuantityString( R.plurals.short_time_seconds, s, Integer.valueOf( s ) );
	}

	public static String getTimeString( Context context, long raw ) {
		java.text.DateFormat dateFormat = DateFormat.getLongDateFormat( context );
		java.text.DateFormat timeFormat = DateFormat.getTimeFormat( context );
		String now = dateFormat.format( new Date() );
		String date = dateFormat.format( new Date( raw ) );
		String time = timeFormat.format( new Date( raw ) );
		return now.equals( date ) ? time : String.format( "%s, %s", date, time );
	}

	private static final long MILLI = 1;
	private static final long SECOND = 1000 * MILLI;
	private static final long MINUTE = 60 * SECOND;
	private static final long HOUR = 60 * MINUTE;

	private static final long DAY = 24 * HOUR;
}
