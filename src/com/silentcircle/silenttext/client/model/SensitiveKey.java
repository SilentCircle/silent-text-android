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
import java.util.List;

import com.silentcircle.api.aspect.Sensitive;
import com.silentcircle.api.aspect.Stateful;
import com.silentcircle.api.aspect.util.Sensitivity;
import com.silentcircle.api.aspect.util.Statefulness;
import com.silentcircle.api.model.Key;
import com.silentcircle.api.model.Signature;
import com.silentcircle.silenttext.crypto.CryptoUtils;

public class SensitiveKey implements Key, Sensitive, Stateful {

	public static class Reader extends Statefulness.Reader<Key> {

		@Override
		public Key read( DataInputStream in ) throws IOException {
			return SensitiveKey.from( in );
		}

	}

	public static final int VERSION = 3;
	public static final Reader READER = new Reader();

	public static Key from( DataInputStream in ) throws IOException {
		SensitiveKey key = new SensitiveKey();
		key.load( in );
		return key;
	}

	private int version;
	private CharSequence comment;
	private long creationDate;
	private byte [] publicKey;
	private byte [] privateKey;
	private long expirationDate;
	private CharSequence locator;
	private CharSequence owner;
	private CharSequence suite;
	private List<Signature> signatures;

	@Override
	public void burn() {
		version = 0;
		creationDate = 0;
		expirationDate = 0;
		privateKey = CryptoUtils.clear( privateKey );
		publicKey = CryptoUtils.clear( publicKey );
		locator = CryptoUtils.clear( locator );
		owner = CryptoUtils.clear( owner );
		comment = CryptoUtils.clear( comment );
		suite = CryptoUtils.clear( suite );
		Sensitivity.burn( signatures );
		signatures = null;
	}

	@Override
	public CharSequence getComment() {
		return comment;
	}

	@Override
	public long getCreationDate() {
		return creationDate;
	}

	@Override
	public long getExpirationDate() {
		return expirationDate;
	}

	@Override
	public CharSequence getLocator() {
		return locator;
	}

	@Override
	public CharSequence getOwner() {
		return owner;
	}

	@Override
	public byte [] getPrivateKey() {
		return privateKey;
	}

	@Override
	public byte [] getPublicKey() {
		return publicKey;
	}

	@Override
	public List<Signature> getSignatures() {
		return signatures;
	}

	@Override
	public CharSequence getSuite() {
		return suite;
	}

	@Override
	public int getVersion() {
		return version;
	}

	@Override
	public void load( DataInputStream in ) throws IOException {
		int version = in.readInt();
		switch( version ) {
			case 3:
				privateKey = Statefulness.readBytes( in );
				//$FALL-THROUGH$
			case 2:
				signatures = Statefulness.readList( in, SensitiveSignature.READER );
				//$FALL-THROUGH$
			case 1:
				this.version = in.readInt();
				locator = Statefulness.readCharSequence( in );
				suite = Statefulness.readCharSequence( in );
				publicKey = Statefulness.readBytes( in );
				owner = Statefulness.readCharSequence( in );
				creationDate = in.readLong();
				expirationDate = in.readLong();
				comment = Statefulness.readCharSequence( in );
				break;
		}
	}

	@Override
	public void save( DataOutputStream out ) throws IOException {
		out.writeInt( VERSION );
		Statefulness.writeBytes( privateKey, out );
		Statefulness.writeList( signatures, out );
		out.writeInt( version );
		Statefulness.writeCharSequence( locator, out );
		Statefulness.writeCharSequence( suite, out );
		Statefulness.writeBytes( publicKey, out );
		Statefulness.writeCharSequence( owner, out );
		out.writeLong( creationDate );
		out.writeLong( expirationDate );
		Statefulness.writeCharSequence( comment, out );
	}

	public void setComment( CharSequence comment ) {
		this.comment = comment;
	}

	public void setCreationDate( long creationDate ) {
		this.creationDate = creationDate;
	}

	public void setExpirationDate( long expirationDate ) {
		this.expirationDate = expirationDate;
	}

	public void setLocator( CharSequence locator ) {
		this.locator = locator;
	}

	public void setOwner( CharSequence owner ) {
		this.owner = owner;
	}

	public void setPrivateKey( byte [] privateKey ) {
		this.privateKey = privateKey;
	}

	public void setPublicKey( byte [] publicKey ) {
		this.publicKey = publicKey;
	}

	public void setSignatures( List<Signature> signatures ) {
		this.signatures = signatures;
	}

	public void setSuite( CharSequence suite ) {
		this.suite = suite;
	}

	public void setVersion( int version ) {
		this.version = version;
	}

}
