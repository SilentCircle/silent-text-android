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
package com.silentcircle.silenttext.client.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.silentcircle.core.util.IOUtils;
import com.silentcircle.core.util.StringUtils;
import com.silentcircle.http.client.dns.NameserverProvider;
import com.silentcircle.http.client.dns.SRVResolver;
import com.silentcircle.silentstorage.repository.Repository;
import com.silentcircle.silentstorage.repository.file.RepositoryLockedException;
import com.silentcircle.silenttext.client.model.ServiceEndpoint;
import com.silentcircle.silenttext.log.Log;

public class CachingSRVResolver extends SRVResolver {

	private final Repository<ServiceEndpoint> repository;

	public CachingSRVResolver( Repository<ServiceEndpoint> repository, byte [] nameserver ) {
		super( nameserver );
		this.repository = repository;
	}

	public CachingSRVResolver( Repository<ServiceEndpoint> repository, byte [] nameserver, int timeout ) {
		super( nameserver, timeout );
		this.repository = repository;
	}

	public CachingSRVResolver( Repository<ServiceEndpoint> repository, InetAddress nameserver ) {
		super( nameserver );
		this.repository = repository;
	}

	public CachingSRVResolver( Repository<ServiceEndpoint> repository, InetAddress nameserver, int timeout ) {
		super( nameserver, timeout );
		this.repository = repository;
	}

	public CachingSRVResolver( Repository<ServiceEndpoint> repository, NameserverProvider nameserverProvider ) {
		super( nameserverProvider );
		this.repository = repository;
	}

	public CachingSRVResolver( Repository<ServiceEndpoint> repository, NameserverProvider nameserverProvider, int timeout ) {
		super( nameserverProvider, timeout );
		this.repository = repository;
	}

	public CachingSRVResolver( Repository<ServiceEndpoint> repository, String nameserver ) throws UnknownHostException {
		super( nameserver );
		this.repository = repository;
	}

	public CachingSRVResolver( Repository<ServiceEndpoint> repository, String nameserver, int timeout ) throws UnknownHostException {
		super( nameserver, timeout );
		this.repository = repository;
	}

	public void clear( char [] domain ) {
		if( domain != null ) {
			try {
				repository.removeByID( domain );
			} catch( RepositoryLockedException e ) {
				Log.e( "CachingSRVResolver", e, "#CachingSRVResolver repository locked error" );
			}
		}
	}

	public void clear( CharSequence domain ) {
		if( domain != null ) {
			char [] domainChars = StringUtils.toCharArray( domain );
			clear( domainChars );
			IOUtils.randomize( domainChars );
		}
	}

	public void clear( CharSequence serviceName, CharSequence host ) {
		try {
			clear( getLookupDomain( serviceName, host ) );
		} catch( Throwable e ) {
			// TODO: Deal with a Socket Timeout
			Log.e( "CachingSRVResolver", e, "#clear - Unknown exception" );
		}
	}
}
