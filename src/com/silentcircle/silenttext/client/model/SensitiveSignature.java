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
package com.silentcircle.silenttext.client.model;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.silentcircle.api.aspect.Sensitive;
import com.silentcircle.api.aspect.Stateful;
import com.silentcircle.api.aspect.util.Statefulness;
import com.silentcircle.api.model.Signature;
import com.silentcircle.silenttext.crypto.CryptoUtils;

public class SensitiveSignature implements Signature, Stateful, Sensitive {

	public static class Reader extends Statefulness.Reader<Signature> {

		@Override
		public Signature read( DataInputStream in ) throws IOException {
			return SensitiveSignature.from( in );
		}

	}

	public static final int VERSION = 2;
	public static final Reader READER = new Reader();

	public static Signature from( DataInputStream in ) throws IOException {
		SensitiveSignature signature = new SensitiveSignature();
		signature.load( in );
		return signature;
	}

	private long date;
	private byte [] data;
	private CharSequence signerLocator;
	private CharSequence hashList;

	@Override
	public void burn() {
		date = 0;
		if( data != null ) {
			CryptoUtils.randomize( data );
			data = null;
		}
		if( signerLocator != null ) {
			CryptoUtils.randomize( signerLocator );
			signerLocator = null;
		}
		if( hashList != null ) {
			CryptoUtils.randomize( hashList );
			hashList = null;
		}
	}

	@Override
	public byte [] getData() {
		return data;
	}

	@Override
	public long getDate() {
		return date;
	}

	@Override
	public CharSequence getHashList() {
		return hashList;
	}

	@Override
	public CharSequence getSignerLocator() {
		return signerLocator;
	}

	@Override
	public void load( DataInputStream in ) throws IOException {
		int version = in.readInt();
		switch( version ) {
			case 2:
				hashList = Statefulness.readCharSequence( in );
				//$FALL-THROUGH$
			case 1:
				date = in.readLong();
				data = Statefulness.readBytes( in );
				signerLocator = Statefulness.readCharSequence( in );
				break;
		}
	}

	@Override
	public void save( DataOutputStream out ) throws IOException {
		out.writeInt( VERSION );
		Statefulness.writeCharSequence( hashList, out );
		out.writeLong( date );
		Statefulness.writeBytes( data, out );
		Statefulness.writeCharSequence( signerLocator, out );
	}

	public void setData( byte [] data ) {
		this.data = data;
	}

	public void setDate( long date ) {
		this.date = date;
	}

	public void setHashList( CharSequence hashList ) {
		this.hashList = hashList;
	}

	public void setSignerLocator( CharSequence signerLocator ) {
		this.signerLocator = signerLocator;
	}

}
