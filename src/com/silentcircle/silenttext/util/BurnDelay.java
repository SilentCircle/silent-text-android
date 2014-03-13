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

import android.content.Context;
import android.text.format.DateUtils;
import android.util.SparseIntArray;

import com.silentcircle.silenttext.R;

public class BurnDelay {

	private static class DefaultBurnDelay extends BurnDelay {

		private static final int MINUTE = 60;
		private static final int HOUR = 60 * MINUTE;
		private static final int DAY = 24 * HOUR;

		public DefaultBurnDelay() {
			put( 0, 0 );
			put( 1, MINUTE );
			put( 2, 5 * MINUTE );
			put( 3, 15 * MINUTE );
			put( 4, 30 * MINUTE );
			put( 5, HOUR );
			put( 6, 2 * HOUR );
			put( 7, 4 * HOUR );
			put( 8, 6 * HOUR );
			put( 9, 12 * HOUR );
			put( 10, DAY );
		}

	}

	public static final BurnDelay Defaults = new DefaultBurnDelay();

	private final SparseIntArray levels = new SparseIntArray();

	public int getDelay( int level ) {
		return levels.get( level );
	}

	public String getLabel( Context context, int level ) {
		int delay = getDelay( level );
		if( delay <= 0 ) {
			return context.getResources().getString( R.string.no_expiration );
		}
		long a = System.currentTimeMillis();
		long b = a + 1000 * delay;
		return context.getResources().getString( R.string.messages_expire, DateUtils.getRelativeTimeSpanString( b, a, DateUtils.SECOND_IN_MILLIS ) );
	}

	public int getLevel( int delay ) {
		return levels.keyAt( levels.indexOfValue( delay ) );
	}

	public void put( int level, int delay ) {
		levels.put( level, delay );
	}

}
