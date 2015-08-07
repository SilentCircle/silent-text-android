/*
Copyright (C) 2015, Silent Circle, LLC. All rights reserved.

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
package com.silentcircle.silenttext.loader;

import com.silentcircle.silenttext.provider.ContactProvider.Result;

public class ContactUser implements Comparable<ContactUser> {

	long raw_id;

	final String fullName = "";

	String displayName; // displayName == fullName

	String userName;

	String numbers;

	String jid;

	String avatarUrl;

	boolean starred;

	public ContactUser() {
	}

	@Override
	public int compareTo( ContactUser other ) {

		if( other == null ) {
			return -1;
		}

		String a = displayName == null ? userName : displayName;
		String b = other.displayName == null ? other.userName : other.displayName;

		if( a == null && b != null ) {
			return 1;
		}

		if( a != null && b == null ) {
			return -1;
		}

		return a == null ? 0 : a.compareTo( b );

	}

	@Override
	public boolean equals( Object object ) {
		return object != null && object instanceof Result && hashCode() == object.hashCode();
	}

	public String getAvatarUrl() {
		return avatarUrl;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getFullName() {
		return fullName;
	}

	public String getJid() {
		return jid;
	}

	public String getNumbers() {
		return numbers;
	}

	public long getRaw_id() {
		return raw_id;
	}

	public String getUserName() {
		return userName;
	}

	@Override
	public int hashCode() {
		return userName == null ? 0 : userName.hashCode();
	}

	public boolean isStarred() {
		return starred;
	}

	public void setAvatarUrl( String avatarUrl ) {
		this.avatarUrl = avatarUrl;
	}

	public void setDisplayName( String displayName ) {
		this.displayName = displayName;
	}

	public void setJid( String jid ) {
		this.jid = jid;
	}

	public void setNumbers( String numbers ) {
		this.numbers = numbers;
	}

	public void setRaw_id( long raw_id ) {
		this.raw_id = raw_id;
	}

	public void setStarred( boolean starred ) {
		this.starred = starred;
	}

	public void setUserName( String userName ) {
		this.userName = userName;
	}

	@Override
	public String toString() {
		return fullName;
	}

}
