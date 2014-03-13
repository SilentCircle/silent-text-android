/*
Copyright © 2012-2013, Silent Circle, LLC.  All rights reserved.

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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import com.silentcircle.silentstorage.repository.Repository;
import com.silentcircle.silenttext.client.SRVResolver;
import com.silentcircle.silenttext.client.URLBuilder;
import com.silentcircle.silenttext.client.model.ServiceEndpoint;

public class ServiceConfiguration {

	public static class API {

		public boolean performSRVLookup = true;
		public String host;
		public int port = 443;
		private String cachedURL;

		public void removeFromCache( Repository<ServiceEndpoint> cache ) {
			SRVResolver.clear( cache, "broker", host );
		}

		public String getURL() {
			return getURL( null );
		}

		public String getURL( Repository<ServiceEndpoint> cache ) {
			if( cachedURL == null ) {
				if( performSRVLookup ) {
					ServiceEndpoint address = SRVResolver.resolve( cache, "broker", host, port );
					if( address != null ) {
						cachedURL = URLBuilder.build( address.host, address.port );
					}
				} else {
					cachedURL = URLBuilder.build( host, port );
				}
			}
			return cachedURL;
		}

		public void load( Resources resources ) {
			host = resources.getString( R.string.build_environment );
			port = 443;
			performSRVLookup = true;
			cachedURL = null;
		}

		public void load( SharedPreferences preferences ) {
			host = preferences.getString( "api_host", host );
			port = getInt( preferences, "api_port", port );
			performSRVLookup = preferences.getBoolean( "api_perform_srv_lookup", performSRVLookup );
			cachedURL = null;
		}

		public void save( SharedPreferences.Editor editor ) {
			editor.putString( "api_host", host );
			editor.putString( "api_port", Integer.toString( port ) );
			editor.putBoolean( "api_perform_srv_lookup", performSRVLookup );
		}

	}

	public static class GCM {

		public String senderID;

		public void load( Resources resources ) {
			senderID = resources.getString( R.string.build_gcm_sender );
		}

		public void load( SharedPreferences preferences ) {
			senderID = preferences.getString( "gcm_sender_id", senderID );
		}

		public void save( SharedPreferences.Editor editor ) {
			editor.putString( "gcm_sender_id", senderID );
		}

	}

	public static class SCimp {

		public boolean enablePKI = false;

		/**
		 * @param resources
		 */
		public void load( Resources resources ) {
			enablePKI = false;
		}

		public void load( SharedPreferences preferences ) {
			enablePKI = preferences.getBoolean( "scimp_enable_pki", enablePKI );
		}

		public void save( SharedPreferences.Editor editor ) {
			editor.putBoolean( "scimp_enable_pki", enablePKI );
		}
	}

	public static class SCloud {

		public String url = "https://s3.amazonaws.com/com.silentcircle.silenttext.scloud/";

		public void load( Resources resources ) {
			url = resources.getString( R.string.build_scloud_url );
		}

		public void load( SharedPreferences preferences ) {
			url = preferences.getString( "scloud_url", url );
		}

		public void save( SharedPreferences.Editor editor ) {
			editor.putString( "scloud_url", url );
		}

	}

	public static class XMPP {

		public String serviceName;
		public boolean performSRVLookup = true;
		public String host;
		public int port = 5223;
		private String cachedURL;

		public void removeFromCache( Repository<ServiceEndpoint> cache ) {
			SRVResolver.clear( cache, "xmpp", host );
		}

		public String getURL() {
			return getURL( null );
		}

		public String getURL( Repository<ServiceEndpoint> cache ) {
			if( cachedURL == null ) {
				if( performSRVLookup ) {
					ServiceEndpoint address = SRVResolver.resolve( cache, "xmpp", host, port );
					if( address != null ) {
						cachedURL = URLBuilder.build( address.host, address.port );
					}
				} else {
					cachedURL = URLBuilder.build( host, port );
				}
			}
			return cachedURL;
		}

		public void load( Resources resources ) {
			serviceName = resources.getString( R.string.build_environment );
			host = resources.getString( R.string.build_environment );
			port = 5223;
			performSRVLookup = true;
			cachedURL = null;
		}

		public void load( SharedPreferences preferences ) {
			serviceName = preferences.getString( "xmpp_service_name", serviceName );
			host = preferences.getString( "xmpp_host", host );
			port = getInt( preferences, "xmpp_port", port );
			performSRVLookup = preferences.getBoolean( "xmpp_perform_srv_lookup", performSRVLookup );
			cachedURL = null;
		}

		public void save( SharedPreferences.Editor editor ) {
			editor.putString( "xmpp_service_name", serviceName );
			editor.putString( "xmpp_host", host );
			editor.putString( "xmpp_port", Integer.toString( port ) );
			editor.putBoolean( "xmpp_perform_srv_lookup", performSRVLookup );
		}

	}

	private static final ServiceConfiguration DEFAULT = new ServiceConfiguration();

	public static ServiceConfiguration getInstance() {
		return DEFAULT;
	}

	protected static int getInt( SharedPreferences preferences, String key, int defaultValue ) {
		try {
			return Integer.parseInt( preferences.getString( key, Integer.toString( defaultValue ) ) );
		} catch( Throwable exception ) {
			return defaultValue;
		}
	}

	public static ServiceConfiguration refresh( Context context ) {
		DEFAULT.load( context.getResources() );
		DEFAULT.load( PreferenceManager.getDefaultSharedPreferences( context ) );
		return DEFAULT;
	}

	public boolean debug = BuildConfig.DEBUG;
	public boolean loggingEnabled = debug;
	public boolean shouldValidateCertificates = !debug;
	public API api = new API();
	public GCM gcm = new GCM();
	public XMPP xmpp = new XMPP();
	public SCloud scloud = new SCloud();
	public SCimp scimp = new SCimp();

	public void load( Resources resources ) {
		debug = resources.getBoolean( R.bool.build_debug );
		loggingEnabled = debug;
		shouldValidateCertificates = !debug;
		api.load( resources );
		gcm.load( resources );
		xmpp.load( resources );
		scloud.load( resources );
		scimp.load( resources );
	}

	public void load( SharedPreferences preferences ) {
		if( !debug ) {
			return;
		}
		loggingEnabled = preferences.getBoolean( "enable_logging", loggingEnabled );
		shouldValidateCertificates = preferences.getBoolean( "should_validate_certificates", shouldValidateCertificates );
		api.load( preferences );
		gcm.load( preferences );
		xmpp.load( preferences );
		scloud.load( preferences );
		scimp.load( preferences );
	}

	public void save( SharedPreferences.Editor editor ) {
		if( !debug ) {
			return;
		}
		editor.putBoolean( "enable_logging", loggingEnabled );
		editor.putBoolean( "should_validate_certificates", shouldValidateCertificates );
		api.save( editor );
		gcm.save( editor );
		xmpp.save( editor );
		scloud.save( editor );
		scimp.save( editor );
	}

}
