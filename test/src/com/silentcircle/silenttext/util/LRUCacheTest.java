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

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.junit.Test;
import org.twuni.common.cache.LRUCache;

public class LRUCacheTest {

	static class IsCached implements Runnable {

		private final LRUCache<String, Object> cache;
		private final int iteration;
		private final String userID;

		public IsCached( LRUCache<String, Object> cache, int iteration, String userID ) {
			this.cache = cache;
			this.iteration = iteration;
			this.userID = userID;
		}

		@Override
		public void run() {
			if( iteration % 10 == 0 ) {
				synchronized( cache ) {
					cache.remove( userID );
				}
			} else if( iteration % 5 == 0 ) {
				synchronized( cache ) {
					cache.put( userID, new Object() );
				}
			}
			boolean cached = false;
			synchronized( cache ) {
				cached = cache.get( userID ) != null;
			}
			System.out.println( String.format( "[%d] isCached(%s)=%b", Integer.valueOf( iteration ), userID, Boolean.valueOf( cached ) ) );
		}

	}

	@Test
	public void massiveParallelism_shouldNotDisruptProperOperation() {

		LRUCache<String, Object> cache = new LRUCache<String, Object>( 4 );
		Executor executor = Executors.newFixedThreadPool( 8 );
		String [] userIDs = "alice,bob,charlie,david,eve,fae,george,harriett".split( "," );

		for( int i = 0; i < 10000; i++ ) {
			executor.execute( new IsCached( cache, i, userIDs[i % userIDs.length] ) );
		}

		try {
			Thread.sleep( 1000 );
		} catch( InterruptedException exception ) {
			// Ignore this.
		}

	}

}
