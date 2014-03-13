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
package com.silentcircle.silenttext.repository;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import org.junit.Before;
import org.junit.Test;

import com.silentcircle.silenttext.Assert;
import com.silentcircle.silenttext.MockUtils;
import com.silentcircle.silenttext.model.event.Event;

public abstract class BaseEventRepositoryTest extends Assert {

	private EventRepository repository;

	@Test
	public void clear_shouldRemoveAllEvents() {
		repository.save( MockUtils.mockEvent() );
		repository.clear();
		assertThat( repository.list(), empty() );
	}

	protected abstract EventRepository createRepository();

	@Test
	public void findById_shouldFailSilentlyIfGivenNullID() {
		repository.findById( null );
	}

	@Test
	public void findById_shouldFindEventWhenGivenItsID() {
		Event expected = MockUtils.mockEvent();
		repository.save( expected );
		Event actual = repository.findById( expected.getId() );
		assertThat( actual, equalTo( expected ) );
	}

	@Test
	public void save_shouldFailSilentlyIfGivenNullEvent() {
		repository.save( null );
	}

	@Test
	public void save_shouldSaveEvent() {
		Event expected = MockUtils.mockEvent();
		repository.save( expected );
		assertThat( repository.list(), hasItem( expected ) );
	}

	@Before
	public void setUp() {
		repository = createRepository();
		repository.clear();
	}

}
