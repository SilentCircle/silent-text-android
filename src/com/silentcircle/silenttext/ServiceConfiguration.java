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

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.TextUtils;

import com.silentcircle.core.util.CollectionUtils;
import com.silentcircle.core.util.StringUtils;
import com.silentcircle.http.client.URLBuilder;
import com.silentcircle.http.client.dns.SRVResolver.SRVRecord;
import com.silentcircle.silenttext.client.dns.CachingSRVResolver;
import com.silentcircle.silenttext.client.model.ServiceEndpoint;
import com.silentcircle.silenttext.log.Log;
import com.silentcircle.silenttext.util.Constants;
import com.silentcircle.silenttext.util.PreferenceUtils;

public class ServiceConfiguration {

	public static class API extends RemoteServiceConfiguration {

		public static final String SRV_RECORD_ID = "broker";

		public boolean requireStrongCiphers;

		@Override
		protected String getSRVRecordID() {
			return SRV_RECORD_ID;
		}

		public void load( Resources resources ) {
			serviceName = resources.getString( R.string.build_environment );
			host = serviceName;
			port = 443;
			performSRVLookup = true;
			requireStrongCiphers = false;
			override = false;
			cachedURL = null;
		}

		public void load( SharedPreferences preferences ) {
			override = preferences.getBoolean( "api_override", override );
			serviceName = preferences.getString( "api_service_name", serviceName );
			host = preferences.getString( "api_host", host );
			port = getInt( preferences, "api_port", port );
			performSRVLookup = preferences.getBoolean( "api_perform_srv_lookup", performSRVLookup );
			requireStrongCiphers = preferences.getBoolean( "api_require_strong_ciphers", requireStrongCiphers );
			cachedURL = null;
		}

		public void removeFromCache( CachingSRVResolver cache ) {
			cache.clear( "broker", serviceName );
		}

		public void removeFromCache( CachingSRVResolver cache, String environment ) {
			cache.clear( SRV_RECORD_ID, override ? serviceName : environment );
		}

		public void save( SharedPreferences.Editor editor ) {
			editor.putBoolean( "api_override", override );
			editor.putString( "api_service_name", serviceName );
			editor.putString( "api_host", host );
			editor.putString( "api_port", Integer.toString( port ) );
			editor.putBoolean( "api_perform_srv_lookup", performSRVLookup );
			editor.putBoolean( "api_require_strong_ciphers", requireStrongCiphers );
		}

		public void update( PreferenceScreen screen ) {
			PreferenceUtils.setSummary( screen, "api_service_name", serviceName );
			PreferenceUtils.setSummary( screen, "api_host", host );
			PreferenceUtils.setSummary( screen, "api_port", Integer.toString( port ) );
		}

	}

	public static class Features {

		public boolean checkUserAvailability;
		public boolean generateDefaultAvatars;
		public boolean writableSystemContacts;

		/**
		 * @param resources
		 */
		public void load( Resources resources ) {
			generateDefaultAvatars = true;
			checkUserAvailability = true;
			writableSystemContacts = false;
		}

		public void load( SharedPreferences preferences ) {
			generateDefaultAvatars = preferences.getBoolean( "ui_generate_default_avatars", generateDefaultAvatars );
			checkUserAvailability = preferences.getBoolean( "check_user_availability", checkUserAvailability );
			writableSystemContacts = preferences.getBoolean( "system_contacts_writable", writableSystemContacts );
		}

		public void save( SharedPreferences.Editor editor ) {
			editor.putBoolean( "ui_generate_default_avatars", generateDefaultAvatars );
			editor.putBoolean( "check_user_availability", checkUserAvailability );
			editor.putBoolean( "system_contacts_writable", writableSystemContacts );
		}

	}

	public static class GCM {

		public String senderID;
		public String target;

		public void load( Resources resources ) {
			senderID = resources.getString( R.string.build_gcm_sender );
			target = resources.getString( R.string.build_gcm_target );
		}

		public void load( SharedPreferences preferences ) {
			senderID = preferences.getString( "gcm_sender_id", senderID );
			target = preferences.getString( "gcm_target", target );
		}

		public void save( SharedPreferences.Editor editor ) {
			editor.putString( "gcm_sender_id", senderID );
			editor.putString( "gcm_target", target );
		}

		public void update( PreferenceScreen screen ) {
			PreferenceUtils.setSummary( screen, "gcm_sender_id", senderID );
			PreferenceUtils.setSummary( screen, "gcm_target", target );
		}

	}

	static abstract class RemoteServiceConfiguration {

		private static List<ServiceEndpoint> toServiceEndpoints( String serviceEndpointID, List<SRVRecord> records ) {

			List<ServiceEndpoint> addresses = new ArrayList<ServiceEndpoint>();

			if( !CollectionUtils.isEmpty( records ) ) {
				for( SRVRecord record : records ) {
					addresses.add( ServiceEndpoint.fromSRVRecord( serviceEndpointID, record ) );
				}
			}

			return addresses;

		}

		public String serviceName;
		public boolean performSRVLookup;
		public String host;
		public int port;
		public boolean override;

		protected String cachedURL;

		public List<ServiceEndpoint> getServiceEndpoints( CachingSRVResolver resolver, String environment ) {

			List<ServiceEndpoint> addresses = null;

			try {
				if( !override || performSRVLookup ) {

					String serviceEndpointID = override ? serviceName : environment;
					String vpnLocalIp = Constants.getVpnLocalIp();
					if( Constants.isAltDNS() && !TextUtils.isEmpty( vpnLocalIp ) ) {
						addresses = toServiceEndpoints( serviceEndpointID, resolver.resolve( getSRVRecordID(), serviceEndpointID, Constants.getVpnDnsAddress( vpnLocalIp ) ) );
					} else {
						addresses = toServiceEndpoints( serviceEndpointID, resolver.resolve( getSRVRecordID(), serviceEndpointID ) );
					}

				} else {

					addresses = new ArrayList<ServiceEndpoint>();

					ServiceEndpoint address = new ServiceEndpoint();

					address.serviceName = override ? serviceName : environment;
					address.host = host;
					address.port = port;

					addresses.add( address );

				}
			} catch( Throwable e ) {
				// TODO: Deal with a Socket Timeout
				Log.e( "ServiceConfiguration", e, "#getServiceEndpoints - Unknown exception" );
				return null;
			}

			return addresses;

		}

		public List<ServiceEndpoint> getServiceEndpoints( String environment ) {
			return getServiceEndpoints( null, environment );
		}

		protected abstract String getSRVRecordID();

		public String getURL( CachingSRVResolver cache, String environment ) {

			if( cachedURL == null ) {
				List<ServiceEndpoint> endpoints = getServiceEndpoints( cache, environment );
				if( endpoints != null ) {
					for( ServiceEndpoint endpoint : endpoints ) {
						if( endpoint.test() ) {
							cachedURL = URLBuilder.build( endpoint.host, endpoint.port );
							break;
						}
					}
				}
			}

			// SRV lookup failed, resort to a backup method
			if( cachedURL == null || StringUtils.isEmpty( cachedURL ) ) {
				if( StringUtils.equals( environment, "silentcircle.com" ) ) {
					return "https://accounts.silentcircle.com";
				} else if( StringUtils.equals( environment, "xmpp-dev.silentcircle.net" ) ) {
					return "https://accounts-dev.silentcircle.com";
				} else {
					return "https://accounts.silentcircle.com";
				}
			}

			return cachedURL;

		}

		public String getURL( String environment ) {
			return getURL( null, environment );
		}

	}

	public static class SCimp {

		public boolean enablePKI = true;

		/**
		 * @param resources
		 */
		public void load( Resources resources ) {
			enablePKI = true;
		}

		public void load( SharedPreferences preferences ) {
			enablePKI = preferences.getBoolean( "scimp_enable_pki", enablePKI );
		}

		public void save( SharedPreferences.Editor editor ) {
			editor.putBoolean( "scimp_enable_pki", enablePKI );
		}

		/**
		 * @param screen
		 */
		public void update( PreferenceScreen screen ) {
			// Nothing to do.
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

		public void update( PreferenceScreen screen ) {
			PreferenceUtils.setSummary( screen, "scloud_url", url );
		}

	}

	public static class XMPP extends RemoteServiceConfiguration {

		public static final String SRV_RECORD_ID = "xmpp";

		public boolean requireStrongCiphers;
		public boolean background;

		@Override
		protected String getSRVRecordID() {
			return SRV_RECORD_ID;
		}

		public void load( Resources resources ) {
			override = false;
			serviceName = resources.getString( R.string.build_environment );
			host = serviceName;
			port = 5223;
			performSRVLookup = true;
			requireStrongCiphers = false;
			cachedURL = null;
			background = false;
		}

		public void load( SharedPreferences preferences ) {
			override = preferences.getBoolean( "xmpp_override", override );
			serviceName = preferences.getString( "xmpp_service_name", serviceName );
			host = preferences.getString( "xmpp_host", host );
			port = getInt( preferences, "xmpp_port", port );
			performSRVLookup = preferences.getBoolean( "xmpp_perform_srv_lookup", performSRVLookup );
			requireStrongCiphers = preferences.getBoolean( "xmpp_require_strong_ciphers", requireStrongCiphers );
			cachedURL = null;
			background = preferences.getBoolean( "xmpp_background", background );
		}

		public void removeFromCache( CachingSRVResolver cache, String environment ) {
			cache.clear( SRV_RECORD_ID, override ? serviceName : environment );
		}

		public void save( SharedPreferences.Editor editor ) {
			editor.putString( "xmpp_service_name", serviceName );
			editor.putString( "xmpp_host", host );
			editor.putString( "xmpp_port", Integer.toString( port ) );
			editor.putBoolean( "xmpp_override", override );
			editor.putBoolean( "xmpp_perform_srv_lookup", performSRVLookup );
			editor.putBoolean( "xmpp_background", background );
			editor.putBoolean( "xmpp_require_strong_ciphers", requireStrongCiphers );
		}

		public void update( PreferenceScreen screen ) {
			PreferenceUtils.setSummary( screen, "xmpp_service_name", serviceName );
			PreferenceUtils.setSummary( screen, "xmpp_host", host );
			PreferenceUtils.setSummary( screen, "xmpp_port", Integer.toString( port ) );
		}

	}

	private static final String DEFAULT_CUSTOM_DNS_SERVER = "8.8.8.8";

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
	public boolean experimental = debug;
	public boolean loggingEnabled = debug;
	public boolean shouldValidateCertificates = !debug;
	public String environment;
	public boolean useCustomDNS;
	public String customDNS;
	public API api = new API();
	public GCM gcm = new GCM();
	public XMPP xmpp = new XMPP();
	public SCloud scloud = new SCloud();
	public SCimp scimp = new SCimp();
	public Features features = new Features();

	public List<ServiceEndpoint> getAPIServiceEndpoints() {
		return api.getServiceEndpoints( environment );
	}

	public List<ServiceEndpoint> getAPIServiceEndpoints( CachingSRVResolver cache ) {
		return api.getServiceEndpoints( cache, environment );
	}

	public String getAPIServiceName() {
		return api.override ? api.serviceName : environment;
	}

	public String getAPIURL( CachingSRVResolver cache ) {
		return api.getURL( cache, environment );
	}

	public List<ServiceEndpoint> getXMPPServiceEndpoints() {
		return xmpp.getServiceEndpoints( environment );
	}

	public List<ServiceEndpoint> getXMPPServiceEndpoints( CachingSRVResolver cache ) {
		return xmpp.getServiceEndpoints( cache, environment );
	}

	public String getXMPPServiceName() {
		return xmpp.override ? xmpp.serviceName : environment;
	}

	public String getXMPPURL( CachingSRVResolver cache ) {
		return xmpp.getURL( cache, environment );
	}

	public void load( Resources resources ) {

		try {
			debug = resources.getBoolean( R.bool.build_debug );
		} catch( Resources.NotFoundException exception ) {
			debug = true;
		}

		experimental = debug;
		loggingEnabled = debug;
		shouldValidateCertificates = !debug;
		environment = resources.getString( R.string.build_environment );
		useCustomDNS = false;
		customDNS = DEFAULT_CUSTOM_DNS_SERVER;

		api.load( resources );
		gcm.load( resources );
		xmpp.load( resources );
		scloud.load( resources );
		scimp.load( resources );
		features.load( resources );

	}

	public void load( SharedPreferences preferences ) {

		if( !debug ) {
			return;
		}

		experimental = preferences.getBoolean( "experimental", experimental );
		loggingEnabled = preferences.getBoolean( "enable_logging", loggingEnabled );
		shouldValidateCertificates = preferences.getBoolean( "should_validate_certificates", shouldValidateCertificates );
		environment = preferences.getString( "environment_domain", environment );
		useCustomDNS = preferences.getBoolean( "enable_custom_dns", useCustomDNS );
		customDNS = preferences.getString( "custom_dns", customDNS );

		api.load( preferences );
		gcm.load( preferences );
		xmpp.load( preferences );
		scloud.load( preferences );
		scimp.load( preferences );
		features.load( preferences );

	}

	public void removeAPIFromCache( CachingSRVResolver cache ) {
		api.removeFromCache( cache, environment );
	}

	public void removeXMPPFromCache( CachingSRVResolver cache ) {
		xmpp.removeFromCache( cache, environment );
	}

	public void save( SharedPreferences.Editor editor ) {

		if( !debug ) {
			return;
		}

		editor.putBoolean( "experimental", experimental );
		editor.putBoolean( "enable_logging", loggingEnabled );
		editor.putBoolean( "should_validate_certificates", shouldValidateCertificates );
		editor.putString( "environment_domain", environment );
		editor.putBoolean( "enable_custom_dns", useCustomDNS );
		editor.putString( "custom_dns", customDNS );

		api.save( editor );
		gcm.save( editor );
		xmpp.save( editor );
		scloud.save( editor );
		scimp.save( editor );
		features.save( editor );

	}

	public void update( PreferenceScreen screen ) {

		load( screen.getSharedPreferences() );

		PreferenceUtils.setSummary( screen, "environment_domain", environment );
		PreferenceUtils.setSummary( screen, "custom_dns", customDNS );

		api.update( screen );
		gcm.update( screen );
		xmpp.update( screen );
		scloud.update( screen );
		scimp.update( screen );

	}

}
