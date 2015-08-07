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

import org.junit.Assert;
import org.junit.Test;

public class StringUtilsTest extends Assert {

	@Test
	public void find_shouldFindMessageBody() {
		String in = "<body>edtest\n</body><subject/><time xmlns='http://silentcircle.com/timestamp' stamp='2014-07-18T03:22:22.575Z'/><delay xmlns='urn:xmpp:delay' from='xmpp-dev.silentcircle.net' stamp='2014-07-18T03:22:22Z'>Offline Storage</delay><x xmlns='jabber:x:delay' stamp='20140718T03:22:22'/>";
		String expected = "edtest\n";
		String regex = "<body>([^<]+)</body>";
		String actual = StringUtils.find( in, regex, 1 );
		assertEquals( expected, actual );
	}

	@Test
	public void find_shouldFindSCimpBody() {
		String in = "<x xmlns=\"http://silentcircle.com\" badge=\"true\" notifiable=\"true\">?SCIMP:ewogICAgImNvbW1pdCI6IHsKICAgICAgICAidmVyc2lvbiI6IDEsCiAgICAgICAgImNpcGhlclN1aXRlIjogMSwKICAgICAgICAic2FzTWV0aG9kIjogMiwKICAgICAgICAiSHBraSI6ICJjdEJjOWtYeGhZR25KdUNxMkp0UmtYNTRyN2JDUGNMZmliYSswMnZycUtzPSIsCiAgICAgICAgIkhjcyI6ICJjdmlNWVlUYU5oRT0iCiAgICB9Cn0K.</x>";
		String expected = "?SCIMP:ewogICAgImNvbW1pdCI6IHsKICAgICAgICAidmVyc2lvbiI6IDEsCiAgICAgICAgImNpcGhlclN1aXRlIjogMSwKICAgICAgICAic2FzTWV0aG9kIjogMiwKICAgICAgICAiSHBraSI6ICJjdEJjOWtYeGhZR25KdUNxMkp0UmtYNTRyN2JDUGNMZmliYSswMnZycUtzPSIsCiAgICAgICAgIkhjcyI6ICJjdmlNWVlUYU5oRT0iCiAgICB9Cn0K.";
		String regex = "^.*<x.+>(\\?SCIMP:[^\\.]+.)</x>.*$";
		String actual = StringUtils.find( in, regex, 1 );
		assertEquals( expected, actual );
	}

	@Test
	public void find_shouldSearchMultipleLines() {
		String in = "apple\norange\nbanana";
		String expected = "orange";
		String regex = "^(orange)$";
		String actual = StringUtils.find( in, regex, 1 );
		assertEquals( expected, actual );
	}

}
