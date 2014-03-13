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

public class InactivityTimeout {

	private static class DefaultInactivityTimeout extends InactivityTimeout {

		private static final int SECOND = 1;
		private static final int MINUTE = 60 * SECOND;

		public DefaultInactivityTimeout() {
			put( 0, -1 );
			put( 1, 30 * SECOND );
			put( 2, 1 * MINUTE );
			put( 3, 2 * MINUTE );
			put( 4, 3 * MINUTE );
			put( 5, 4 * MINUTE );
			put( 6, 5 * MINUTE );
			put( 7, 10 * MINUTE );
			put( 8, 15 * MINUTE );
			put( 9, 30 * MINUTE );
			put( 10, 60 * MINUTE );
		}

	}

	public static final InactivityTimeout Defaults = new DefaultInactivityTimeout();

	private final SparseIntArray levels = new SparseIntArray();

	public int getDelay( int level ) {
		return levels.get( level );
	}

	public String getLabel( Context context, int level ) {
		int delay = getDelay( level );
		if( delay <= 0 ) {
			return context.getResources().getString( R.string.never_lock );
		}
		long a = System.currentTimeMillis();
		long b = a + 1000 * delay;
		return context.getResources().getString( R.string.lock_after, DateUtils.getRelativeTimeSpanString( b, a, DateUtils.SECOND_IN_MILLIS ) );
	}

	public int getLevel( int delay ) {
		int index = levels.indexOfValue( delay );
		return index >= 0 ? levels.keyAt( index ) : levels.keyAt( 1 );
	}

	public void put( int level, int delay ) {
		levels.put( level, delay );
	}

}
