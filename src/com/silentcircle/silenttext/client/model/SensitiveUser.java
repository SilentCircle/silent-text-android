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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.silentcircle.api.aspect.Sensitive;
import com.silentcircle.api.aspect.Stateful;
import com.silentcircle.api.aspect.util.Sensitivity;
import com.silentcircle.api.aspect.util.Statefulness;
import com.silentcircle.api.aspect.util.Statefulness.Reader;
import com.silentcircle.api.model.Entitlement;
import com.silentcircle.api.model.Key;
import com.silentcircle.api.model.User;
import com.silentcircle.silenttext.crypto.CryptoUtils;

public class SensitiveUser implements User, Sensitive, Stateful {

	public static final int VERSION = 2;

	public static User from( DataInputStream in ) throws IOException {
		SensitiveUser user = new SensitiveUser();
		user.load( in );
		return user;
	}

	private CharSequence id;
	private CharSequence firstName;
	private CharSequence lastName;
	private final Set<Entitlement> entitlements = new HashSet<Entitlement>();
	private List<Key> keys;

	@Override
	public void burn() {
		entitlements.clear();
		Sensitivity.burn( keys );
		keys = null;
		if( id != null ) {
			CryptoUtils.randomize( id );
			id = null;
		}
		if( firstName != null ) {
			CryptoUtils.randomize( firstName );
			firstName = null;
		}
		if( lastName != null ) {
			CryptoUtils.randomize( lastName );
			lastName = null;
		}
	}

	@Override
	public Set<Entitlement> getEntitlements() {
		return entitlements;
	}

	@Override
	public CharSequence getFirstName() {
		return firstName;
	}

	@Override
	public CharSequence getID() {
		return id;
	}

	@Override
	public List<Key> getKeys() {
		return keys;
	}

	@Override
	public CharSequence getLastName() {
		return lastName;
	}

	@Override
	public void load( DataInputStream in ) throws IOException {

		int version = in.readInt();

		switch( version ) {

			case 2:

				firstName = Statefulness.readCharSequence( in );
				lastName = Statefulness.readCharSequence( in );
				//$FALL-THROUGH$

			case 1:

				id = Statefulness.readCharSequence( in );
				entitlements.clear();
				Set<Entitlement> entitlementSet = Statefulness.readSet( in, new Reader<Entitlement>() {

					@Override
					public List<Entitlement> createList() {
						return new ArrayList<Entitlement>();
					}

					@Override
					public Set<Entitlement> createSet() {
						return new HashSet<Entitlement>();
					}

					@Override
					public Entitlement read( DataInputStream in ) throws IOException {
						return Entitlement.valueOf( in.readUTF() );
					}

				} );

				if( entitlementSet != null ) {
					entitlements.addAll( entitlementSet );
				}

				keys = Statefulness.readList( in, SensitiveKey.READER );

				break;
		}
	}

	@Override
	public void save( DataOutputStream out ) throws IOException {
		out.writeInt( VERSION );
		Statefulness.writeCharSequence( firstName, out );
		Statefulness.writeCharSequence( lastName, out );
		Statefulness.writeCharSequence( id, out );
		Statefulness.writeCollection( entitlements, out );
		Statefulness.writeList( keys, out );
	}

	public void setFirstName( CharSequence firstName ) {
		this.firstName = firstName;
	}

	public void setID( CharSequence id ) {
		this.id = id;
	}

	public void setKeys( List<Key> keys ) {
		this.keys = keys;
	}

	public void setLastName( CharSequence lastName ) {
		this.lastName = lastName;
	}

}
