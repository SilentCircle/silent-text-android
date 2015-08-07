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
package com.silentcircle.silenttext.crypto;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import org.junit.Ignore;
import org.junit.Test;

@Ignore( "This test could intermittently fail. Verifying randomness is notoriously and innately imperfect. Run this manually as needed, for anecdotal evidence and sanity checking" )
public class SecureRandomTest {

	@Test
	public void nextBytes_shouldAvoidCollisions() throws Exception {
		SecureRandom random = new SecureRandom();
		int iterations = 0xFFFF;
		byte [] buffer = new byte [16];
		int [] hashCodes = new int [iterations];
		byte [][] sha1sums = new byte [iterations] [];
		MessageDigest sha1 = MessageDigest.getInstance( "SHA-1" );
		for( int i = 0; i < iterations; i++ ) {
			random.nextBytes( buffer );
			hashCodes[i] = Arrays.hashCode( buffer );
			sha1.reset();
			sha1sums[i] = sha1.digest( buffer );
			for( int j = 0; j < i; j++ ) {
				if( hashCodes[i] == hashCodes[j] ) {
					throw new RuntimeException( String.format( "#hashCode collision between index %d and %d!", Integer.valueOf( i ), Integer.valueOf( j ) ) );
				}
				if( Arrays.equals( sha1sums[i], sha1sums[j] ) ) {
					throw new RuntimeException( String.format( "SHA-1 collision between index %d and %d!", Integer.valueOf( i ), Integer.valueOf( j ) ) );
				}
			}
		}
	}

	@Test
	public void nextBytes_shouldHaveSufficientEntropy() {
		SecureRandom random = new SecureRandom();
		byte [] buffer = new byte [16];
		int [] entropy = new int [buffer.length * 8];
		int iterations = 0xFFFF;
		int [][] graph = new int [buffer.length * 8] [iterations];
		for( int n = 0; n < iterations; n++ ) {
			random.nextBytes( buffer );
			for( int i = 0; i < buffer.length; i++ ) {
				int j = i * 8;
				graph[j + 0][n] = ( buffer[i] & 0x80 ) == 0 ? -1 : 1;
				graph[j + 1][n] = ( buffer[i] & 0x40 ) == 0 ? -1 : 1;
				graph[j + 2][n] = ( buffer[i] & 0x20 ) == 0 ? -1 : 1;
				graph[j + 3][n] = ( buffer[i] & 0x10 ) == 0 ? -1 : 1;
				graph[j + 4][n] = ( buffer[i] & 0x08 ) == 0 ? -1 : 1;
				graph[j + 5][n] = ( buffer[i] & 0x04 ) == 0 ? -1 : 1;
				graph[j + 6][n] = ( buffer[i] & 0x02 ) == 0 ? -1 : 1;
				graph[j + 7][n] = ( buffer[i] & 0x01 ) == 0 ? -1 : 1;
				entropy[j + 0] += graph[j + 0][n];
				entropy[j + 1] += graph[j + 1][n];
				entropy[j + 2] += graph[j + 2][n];
				entropy[j + 3] += graph[j + 3][n];
				entropy[j + 4] += graph[j + 4][n];
				entropy[j + 5] += graph[j + 5][n];
				entropy[j + 6] += graph[j + 6][n];
				entropy[j + 7] += graph[j + 7][n];
			}
		}

		for( int i = 0; i < entropy.length; i++ ) {
			if( entropy[i] == 0 ) {
				System.out.println( String.format( "[WARN] Half-entropy at bit %d of %d", Integer.valueOf( i ), Integer.valueOf( entropy.length ) ) );
			}
			if( entropy[i] == iterations || -entropy[i] == iterations ) {
				System.out.println( String.format( "[ERROR] Zero entropy at bit %d of %d", Integer.valueOf( i ), Integer.valueOf( entropy.length ) ) );
			}
		}
	}

}
