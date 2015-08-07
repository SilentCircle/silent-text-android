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
package com.silentcircle.silenttext.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SymmetricKey {

	public static class Helper extends com.silentcircle.silentstorage.repository.helper.RepositoryHelper<SymmetricKey> {

		public Helper() {
			super( new Serializer() );
		}

		@Override
		public char [] identify( SymmetricKey key ) {
			return key != null ? key.id : null;
		}

	}

	public static class Serializer extends com.silentcircle.silentstorage.io.Serializer<SymmetricKey> {

		public static final int VERSION = 1;

		@Override
		public SymmetricKey read( DataInputStream in ) throws IOException {
			return read( in, new SymmetricKey() );
		}

		@Override
		public SymmetricKey read( DataInputStream in, SymmetricKey out ) throws IOException {

			int version = in.readInt();

			switch( version ) {

				case 1:

					out.id = readChars( in );
					out.key = readBytes( in );

					break;

			}

			return out;

		}

		@Override
		public SymmetricKey write( SymmetricKey in, DataOutputStream out ) throws IOException {

			out.writeInt( VERSION );

			writeChars( in.id, out );
			writeBytes( in.key, out );

			return in;

		}

	}

	public char [] id;
	public byte [] key;

	public SymmetricKey() {
		// Default constructor.
	}

	public SymmetricKey( char [] id, byte [] key ) {
		this.id = id;
		this.key = key;
	}

	public SymmetricKey( String id, byte [] key ) {
		this( id.toCharArray(), key );
	}

}
