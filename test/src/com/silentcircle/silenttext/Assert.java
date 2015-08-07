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
package com.silentcircle.silenttext;

import com.silentcircle.silenttext.model.Contact;
import com.silentcircle.silenttext.model.Conversation;
import com.silentcircle.silenttext.model.event.Event;
import com.silentcircle.silenttext.model.event.Message;

public class Assert extends org.junit.Assert {

	public static void assertEquals( Contact expected, Contact actual ) {
		assertEquals( expected.getUsername(), actual.getUsername() );
		assertEquals( expected.getAlias(), actual.getAlias() );
		assertEquals( expected.getDevice(), actual.getDevice() );
	}

	public static void assertEquals( Conversation expected, Conversation actual ) {
		assertEquals( expected.getId(), actual.getId() );
		assertEquals( expected.getBurnDelay(), actual.getBurnDelay() );
		assertTrue( expected.hasBurnNotice() == actual.hasBurnNotice() );
		assertTrue( expected.isLocationEnabled() == actual.isLocationEnabled() );
		assertNotNull( actual.getPartner() );
		assertEquals( expected.getPartner().getUsername(), actual.getPartner().getUsername() );
		assertEquals( expected.getPartner().getAlias(), actual.getPartner().getAlias() );
		assertEquals( expected.getPartner().getDevice(), actual.getPartner().getDevice() );
	}

	public static void assertEquals( Event expected, Event actual ) {
		assertEquals( expected.getId(), actual.getId() );
		assertEquals( expected.getText(), actual.getText() );
		assertEquals( expected.getTime(), actual.getTime() );
		assertEquals( expected.getClass(), actual.getClass() );
	}

	public static void assertEquals( Message expected, Message actual ) {
		assertEquals( (Event) expected, (Event) actual );
		assertEquals( expected.getClass(), actual.getClass() );
		assertEquals( expected.getCiphertext(), actual.getCiphertext() );
		assertEquals( expected.getSender(), actual.getSender() );
		assertEquals( expected.getBurnNotice(), actual.getBurnNotice() );
		assertEquals( expected.getState(), actual.getState() );
	}

}
