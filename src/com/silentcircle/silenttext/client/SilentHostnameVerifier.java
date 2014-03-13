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

import java.io.IOException;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.apache.http.conn.ssl.StrictHostnameVerifier;
import org.apache.http.conn.ssl.X509HostnameVerifier;

import com.silentcircle.silenttext.log.Log;

public class SilentHostnameVerifier implements X509HostnameVerifier {

	private static final X509HostnameVerifier STRICT = new StrictHostnameVerifier();
	private static final Log LOG = new Log( SilentHostnameVerifier.class.getSimpleName() );

	private static PublicKey [] collectCertificatePublicKeysForEachAliasIn( KeyStore trustStore ) {
		if( trustStore == null ) {
			return new PublicKey [0];
		}
		try {
			PublicKey [] trustedKeys = new PublicKey [trustStore.size()];
			int i = 0;
			Enumeration<String> aliases = trustStore.aliases();
			while( aliases.hasMoreElements() ) {
				String alias = aliases.nextElement();
				if( alias == null ) {
					continue;
				}
				if( trustStore.isCertificateEntry( alias ) ) {
					Certificate certificate = trustStore.getCertificate( alias );
					if( certificate == null ) {
						continue;
					}
					trustedKeys[i] = certificate.getPublicKey();
					i++;
					continue;
				}
			}
			return trustedKeys;
		} catch( Exception exception ) {
			LOG.error( exception, "while inspecting keystore for trusted certificates" );
			return new PublicKey [0];
		}
	}

	private final PublicKey [] trustedKeys;

	public SilentHostnameVerifier( KeyStore trustStore ) {
		this( collectCertificatePublicKeysForEachAliasIn( trustStore ) );
	}

	public SilentHostnameVerifier( PublicKey... trustedKeys ) {
		this.trustedKeys = trustedKeys;
	}

	private void verify( Certificate certificate ) throws SSLException {
		PublicKey key = certificate.getPublicKey();
		for( int i = 0; i < trustedKeys.length; i++ ) {
			PublicKey knownKey = trustedKeys[i];
			if( knownKey == null ) {
				continue;
			}
			if( Arrays.equals( knownKey.getEncoded(), key.getEncoded() ) ) {
				return;
			}
		}
		throw new SSLPeerUnverifiedException( "No matching keys found in trust store" );
	}

	private void verify( SSLSession session ) throws IOException {
		Certificate [] certificates = session.getPeerCertificates();
		if( certificates == null || certificates.length < 1 ) {
			throw new SSLException( "No peer certificates" );
		}
		verify( certificates[0] );
	}

	@Override
	public boolean verify( String host, SSLSession session ) {
		try {
			verify( session );
			return true;
		} catch( Exception exception ) {
			LOG.error( exception, "#verify host:%s session:SSLSession", host );
			return false;
		}
	}

	@Override
	public void verify( String host, SSLSocket socket ) throws IOException {
		verify( socket.getSession() );
	}

	@Override
	public void verify( String host, String [] commonNames, String [] subjectAlternativeNames ) throws SSLException {
		STRICT.verify( host, commonNames, subjectAlternativeNames );
	}

	@Override
	public void verify( String host, X509Certificate certificate ) throws SSLException {
		verify( certificate );
	}

}
