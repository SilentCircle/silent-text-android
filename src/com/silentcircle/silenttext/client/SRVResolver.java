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
package com.silentcircle.silenttext.client;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.util.dns.DNSJavaResolver;
import org.jivesoftware.smack.util.dns.DNSResolver;
import org.jivesoftware.smack.util.dns.SRVRecord;

import com.silentcircle.silentstorage.repository.Repository;
import com.silentcircle.silenttext.client.model.ServiceEndpoint;

public class SRVResolver {

	protected static SRVRecord best( List<SRVRecord> addresses ) {

		List<SRVRecord> best = new ArrayList<SRVRecord>();

		for( int i = 0; i < addresses.size(); i++ ) {

			SRVRecord thisRecord = addresses.get( i );

			if( best.isEmpty() ) {
				best.add( thisRecord );
				continue;
			}

			SRVRecord bestRecord = best.get( 0 );

			if( thisRecord.getPriority() > bestRecord.getPriority() ) {
				best.clear();
				best.add( thisRecord );
				continue;
			}

			if( thisRecord.getPriority() == bestRecord.getPriority() ) {

				if( thisRecord.getWeight() > bestRecord.getWeight() ) {
					best.clear();
					best.add( thisRecord );
					continue;
				}

				if( thisRecord.getWeight() == bestRecord.getWeight() ) {
					best.add( thisRecord );
					continue;
				}

			}

		}

		if( best.isEmpty() ) {
			return null;
		}

		return random( best );

	}

	public static void clear( Repository<ServiceEndpoint> cache, String domain ) {
		if( cache != null && domain != null ) {
			cache.removeByID( domain.toCharArray() );
		}
	}

	public static void clear( Repository<ServiceEndpoint> cache, String service, String domain ) {
		clear( cache, getLookupDomain( service, domain ) );
	}

	protected static <T> T first( List<T> items ) {
		if( items.size() < 1 ) {
			return null;
		}
		return items.get( 0 );
	}

	public static String getLookupDomain( String service, String host ) {
		return service != null && host != null ? String.format( "_%s-client._tcp.%s", service, host ) : null;
	}

	protected static List<SRVRecord> matchingPort( List<SRVRecord> records, int port ) {
		List<SRVRecord> matches = new ArrayList<SRVRecord>();
		for( int i = 0; i < records.size(); i++ ) {
			SRVRecord record = records.get( i );
			if( record.getPort() == port ) {
				matches.add( record );
			}
		}
		return matches;
	}

	protected static <T> T random( List<T> items ) {
		if( items == null || items.isEmpty() ) {
			return null;
		}
		int index = (int) Math.floor( Math.random() * items.size() );
		return items.get( index );
	}

	public static ServiceEndpoint resolve( Repository<ServiceEndpoint> cache, String domain ) {
		if( cache == null ) {
			return resolve( domain );
		}
		ServiceEndpoint address = cache.findByID( domain.toCharArray() );
		if( address == null ) {
			address = resolve( domain );
			if( address != null ) {
				cache.save( address );
			}
		}
		return address;
	}

	public static ServiceEndpoint resolve( Repository<ServiceEndpoint> cache, String domain, int preferredPort ) {
		if( cache == null ) {
			return resolve( domain, preferredPort );
		}
		ServiceEndpoint address = cache.findByID( domain.toCharArray() );
		if( address == null ) {
			address = resolve( domain, preferredPort );
			if( address != null ) {
				cache.save( address );
			}
		}
		return address;
	}

	public static ServiceEndpoint resolve( Repository<ServiceEndpoint> cache, String service, String domain, int preferredPort ) {
		return resolve( cache, getLookupDomain( service, domain ), preferredPort );
	}

	public static ServiceEndpoint resolve( String domain ) {
		DNSResolver resolver = DNSJavaResolver.getInstance();
		List<SRVRecord> records = resolver.lookupSRVRecords( domain );
		return ServiceEndpoint.fromHostAddress( domain, best( records ) );
	}

	public static ServiceEndpoint resolve( String domain, int preferredPort ) {
		DNSResolver resolver = DNSJavaResolver.getInstance();
		List<SRVRecord> records = resolver.lookupSRVRecords( domain );
		List<SRVRecord> recordsMatchingPort = matchingPort( records, preferredPort );
		return ServiceEndpoint.fromHostAddress( domain, best( recordsMatchingPort.isEmpty() ? records : recordsMatchingPort ) );
	}

	public static ServiceEndpoint resolve( String service, String domain, int preferredPort ) {
		return resolve( getLookupDomain( service, domain ), preferredPort );
	}

}
