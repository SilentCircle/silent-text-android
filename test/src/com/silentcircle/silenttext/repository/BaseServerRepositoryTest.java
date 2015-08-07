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
package com.silentcircle.silenttext.repository;

import static com.silentcircle.silenttext.MockUtils.mockServer;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import org.junit.Before;
import org.junit.Test;

import com.silentcircle.silenttext.Assert;
import com.silentcircle.silenttext.model.Server;

public abstract class BaseServerRepositoryTest extends Assert {

	private ServerRepository repository;

	@Test
	public void clear_shouldRemoveExistingServers() {
		Server a = mockServer();
		repository.save( a );
		repository.clear();
		assertThat( repository.findById( a.getId() ), nullValue() );
	}

	protected abstract ServerRepository createRepository();

	@Test
	public void findById_shouldReturnExactSameServerAsPreviouslySaved() {
		Server expected = mockServer();
		repository.save( expected );
		Server actual = repository.findById( expected.getId() );
		assertThat( actual, not( nullValue() ) );
		assertThat( actual.getDomain(), equalTo( expected.getDomain() ) );
		assertThat( actual.getId(), equalTo( expected.getId() ) );
		assertThat( actual.getServiceName(), equalTo( expected.getServiceName() ) );
		assertThat( actual.getCredential(), not( nullValue() ) );
		assertThat( actual.getCredential().getDomain(), equalTo( expected.getCredential().getDomain() ) );
		assertThat( actual.getCredential().getUsername(), equalTo( expected.getCredential().getUsername() ) );
		assertThat( actual.getCredential().getPassword(), equalTo( expected.getCredential().getPassword() ) );
	}

	@Test
	public void findById_shouldReturnExistingServerWhenGivenItsID() {
		Server expected = mockServer();
		repository.save( expected );
		Server actual = repository.findById( expected.getId() );
		assertThat( actual, equalTo( expected ) );
	}

	@Test
	public void findById_shouldReturnNullWhenGivenNullValue() {
		assertThat( repository.findById( null ), nullValue() );
	}

	@Test
	public void findById_shouldReturnNullWhenGivenUnknownID() {
		assertThat( repository.findById( "bogus ID" ), nullValue() );
	}

	@Test
	public void remove_shouldNotExplodeWhenGivenNullValue() {
		repository.remove( null );
	}

	@Test
	public void remove_shouldNotExplodeWhenGivenUnknownServer() {
		Server server = mockServer();
		repository.remove( server );
		assertThat( repository.findById( server.getId() ), nullValue() );
	}

	@Test
	public void remove_shouldRemoveGivenServer() {
		Server server = mockServer();
		repository.save( server );
		assertThat( repository.findById( server.getId() ), not( nullValue() ) );
		repository.remove( server );
		assertThat( repository.findById( server.getId() ), nullValue() );
	}

	@Test
	public void save_shouldNotExplodeWhenGivenNullValue() {
		repository.save( null );
	}

	@Before
	public void setUp() {
		repository = createRepository();
		repository.clear();
	}

}
