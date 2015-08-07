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
package com.silentcircle.silenttext.util;

import java.math.BigInteger;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceGroup;
import android.telephony.TelephonyManager;
import android.text.Spanned;

import com.silentcircle.silentstorage.util.Base64;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.ServiceConfiguration;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.client.XMPPTransport;
import com.silentcircle.silenttext.crypto.Hash;
import com.silentcircle.silenttext.preference.PreferenceTreeBuilder;
import com.silentcircle.silenttext.repository.resolver.SilentContactRepositoryV1;
import com.silentcircle.silenttext.repository.resolver.SilentContactRepositoryV2;
import com.silentcircle.silenttext.view.OptionsDrawer;

public class DeviceUtils {

	public static class ApplicationLicenseCodeProvider implements LicenseCodeProvider {

		@Override
		public CharSequence provideLicenseCode( Context context ) {
			SilentTextApplication global = SilentTextApplication.from( context );
			return global.getLicenseCode();
		}
	}

	public static class DeviceIDLicenseCodeProvider implements LicenseCodeProvider {

		// private static final String SALT = "SilentCircle";
		@Override
		public CharSequence provideLicenseCode( Context context ) {
			// return DeviceUtils.getHashedDeviceID( context, SALT );
			String serialNum = DeviceUtils.getSerialNumber();
			String deviceID = DeviceUtils.getDeviceID( context );
			String result = new BigInteger( 1, Hash.sha1( serialNum.getBytes(), deviceID.getBytes() ) ).toString( 16 );
			// left pad with leading zeros in case they get dropped
			return String.format( "%1$40s", result ).replace( ' ', '0' );
		}

	}

	public static interface Eligibility {

		public boolean test( String manufacturer, String model );

	}

	public static class EligibleLicenseCodeProvider implements LicenseCodeProvider {

		private final Eligibility eligibility;
		private final LicenseCodeProvider provider;

		public EligibleLicenseCodeProvider( Eligibility eligibility, LicenseCodeProvider provider ) {
			if( eligibility == null || provider == null ) {
				throw new IllegalArgumentException();
			}

			this.eligibility = eligibility;
			this.provider = provider;
		}

		@Override
		public CharSequence provideLicenseCode( Context context ) {
			if( DeviceUtils.isPartnerDevice( eligibility ) ) {
				return provider.provideLicenseCode( context );
			}
			return null;
		}
	}

	public static interface LicenseCodeProvider {

		public CharSequence provideLicenseCode( Context context );
	}

	public static class WhiteListEligibility implements Eligibility {

		private final String [] manufacturers;
		private final String [] models;

		public WhiteListEligibility( String [] manufacturers, String [] models ) {
			this.manufacturers = manufacturers;
			this.models = models;
		}

		@Override
		public boolean test( String manufacturer, String model ) {
			if( StringUtils.isAnyOf( manufacturer, manufacturers ) ) {
				return true;
			}
			if( StringUtils.isAnyOf( model, models ) ) {
				return true;
			}
			return false;
		}

	}

	private static final String LABEL_DEBUG_INFO = "DEBUG INFORMATION";

	private static final String ACTION_START_ENCRYPTION = "android.app.action.START_ENCRYPTION";

	@TargetApi( Build.VERSION_CODES.HONEYCOMB )
	public static Intent createEncryptDeviceIntent() {
		return new Intent( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? DevicePolicyManager.ACTION_START_ENCRYPTION : ACTION_START_ENCRYPTION );
	}

	public static String getDebugInformation( Context context ) {
		return JSONUtils.toFormattedText( getDebugInformationJSON( context ) );
	}

	public static Spanned getDebugInformationHTML( Context context ) {
		return JSONUtils.toHTML( getDebugInformationJSON( context ) );
	}

	public static JSONObject getDebugInformationJSON( Context context ) {

		try {

			JSONObject json = new JSONObject();

			json.put( "timestamp", JSONUtils.getDate( System.currentTimeMillis() ) );

			JSONObject android = new JSONObject();

			android.put( "version", Build.VERSION.RELEASE );
			android.put( "api", Build.VERSION.SDK_INT );
			android.put( "build", Build.FINGERPRINT );

			json.put( "android", android );

			JSONObject application = new JSONObject();

			try {

				PackageInfo pinfo = context.getPackageManager().getPackageInfo( context.getPackageName(), 0 );

				application.put( "version_name", pinfo.versionName );
				application.put( "version_code", pinfo.versionCode );
				setInstallAndUpdateTimes( pinfo, application );

			} catch( NameNotFoundException impossible ) {
				throw new RuntimeException( impossible );
			}

			json.put( "application", application );

			JSONObject device = new JSONObject();

			Configuration resourceConfiguration = context.getResources().getConfiguration();

			device.put( "locale", resourceConfiguration.locale );
			device.put( "orientation", resourceConfiguration.orientation == Configuration.ORIENTATION_LANDSCAPE ? "landscape" : "portrait" );
			device.put( "density", getScreenDensity( resourceConfiguration ) );
			device.put( "screen", getScreenDimensions( resourceConfiguration ) );

			device.put( "manufacturer", Build.MANUFACTURER );
			device.put( "model", Build.MODEL );

			JSONObject encryption = new JSONObject();

			encryption.put( "supported", isFullDiskEncryptionSupported( context ) );
			encryption.put( "enabled", isEncrypted( context ) );

			device.put( "encryption", encryption );

			json.put( "device", device );

			JSONObject build = new JSONObject();

			build.put( "date", context.getString( R.string.build_date ) );
			build.put( "version", context.getString( R.string.build_version ) );
			build.put( "commit", context.getString( R.string.build_commit ) );

			json.put( "build", build );

			JSONObject connection = new JSONObject();

			SilentTextApplication global = SilentTextApplication.from( context );

			XMPPTransport client = global.getXMPPTransport();

			JSONObject xmpp = new JSONObject();

			xmpp.put( "status", global.getXMPPTransportConnectionStatus() );

			if( client != null ) {

				boolean online = client.isConnected();

				xmpp.put( "online", online );

				if( online ) {
					xmpp.put( "host", client.getServerHost() );
					xmpp.put( "port", client.getServerPort() );
				}

			}

			connection.put( "xmpp", xmpp );

			json.put( "connection", connection );

			ServiceConfiguration serviceConfig = ServiceConfiguration.getInstance();

			JSONObject services = new JSONObject();

			services.put( "debug", serviceConfig.debug );
			services.put( "experimental", serviceConfig.experimental );
			services.put( "logging_enabled", serviceConfig.loggingEnabled );
			services.put( "environment", serviceConfig.environment );
			services.put( "dns.use_custom_server", serviceConfig.useCustomDNS );
			if( serviceConfig.useCustomDNS ) {
				services.put( "dns.custom_server", serviceConfig.customDNS );
			}
			services.put( "validate_certificates", serviceConfig.shouldValidateCertificates );
			services.put( "api.override", serviceConfig.api.override );
			services.put( "api.perform_srv_lookup", serviceConfig.api.performSRVLookup );
			services.put( "api.host", serviceConfig.api.host );
			services.put( "api.port", serviceConfig.api.port );
			services.put( "api.service_name", serviceConfig.api.serviceName );
			services.put( "feature.check_user_availability", serviceConfig.features.checkUserAvailability );
			services.put( "feature.generate_default_avatars", serviceConfig.features.generateDefaultAvatars );
			services.put( "gcm.sender_id", serviceConfig.gcm.senderID );
			services.put( "gcm.target", serviceConfig.gcm.target );
			services.put( "scimp.enable_pki", serviceConfig.scimp.enablePKI );
			services.put( "scloud.url", serviceConfig.scloud.url );
			services.put( "xmpp.override", serviceConfig.xmpp.override );
			services.put( "xmpp.perform_srv_lookup", serviceConfig.xmpp.performSRVLookup );
			services.put( "xmpp.host", serviceConfig.xmpp.host );
			services.put( "xmpp.port", serviceConfig.xmpp.port );
			services.put( "xmpp.service_name", serviceConfig.xmpp.serviceName );
			services.put( "xmpp.background", serviceConfig.xmpp.background );
			services.put( "passcode_set", !OptionsDrawer.isEmptyPasscode( context ) );
			services.put( "silent_contacts_v1", SilentContactRepositoryV1.supports( context ) );
			services.put( "silent_contacts_v2", SilentContactRepositoryV2.supports( context ) );
			services.put( "silent_phone", SilentPhone.supports( context ) );

			json.put( "configuration", services );

			return json;

		} catch( JSONException impossible ) {
			throw new RuntimeException( impossible );
		}

	}

	public static String getDeviceID( Context context ) {
		TelephonyManager telephony = context != null ? (TelephonyManager) context.getSystemService( Context.TELEPHONY_SERVICE ) : null;
		return telephony != null ? telephony.getDeviceId() : UUID.randomUUID().toString();
	}

	public static String getEncodedDebugInformation( Context context ) {
		return wrap( LABEL_DEBUG_INFO, Base64.encodeBase64URLSafeString( getDebugInformation( context ).getBytes() ) );
	}

	private static CharSequence getEncryptionLabel( JSONObject debugInformation ) {
		JSONObject device = JSONUtils.getJSONObject( debugInformation, "device" );
		JSONObject encryption = JSONUtils.getJSONObject( device, "encryption" );
		boolean supported = JSONUtils.getBoolean( encryption, "supported" );
		boolean enabled = JSONUtils.getBoolean( encryption, "enabled" );
		return supported ? enabled ? "Enabled" : "Disabled" : "Unsupported";
	}

	@Deprecated
	public static String getHashedDeviceID( Context context, String salt ) {
		String deviceID = getDeviceID( context );
		return new BigInteger( 1, Hash.sha1( salt.getBytes(), deviceID.getBytes() ) ).toString( 16 );
	}

	public static String getManufacturer() {
		String manufacturer = System.getProperty( "ro.product.manufacturer" );
		return manufacturer == null ? android.os.Build.MANUFACTURER : manufacturer;
	}

	public static String getModel() {
		String model = System.getProperty( "ro.product.model" );
		return model == null ? android.os.Build.MODEL : model;
	}

	public static Eligibility getPartnerEligibility( Context context ) {
		if( context == null ) {
			return null;
		}
		Resources resources = context.getResources();
		String [] manufacturers = resources.getString( R.string.build_partners ).split( "," );
		return new WhiteListEligibility( manufacturers, null );
	}

	@TargetApi( Build.VERSION_CODES.JELLY_BEAN_MR1 )
	private static int getScreenDensity( Configuration config ) {
		return Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 ? 0 : config.densityDpi;
	}

	@TargetApi( Build.VERSION_CODES.HONEYCOMB_MR2 )
	private static JSONObject getScreenDimensions( Configuration config ) throws JSONException {

		if( Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR2 ) {
			return null;
		}

		JSONObject screen = new JSONObject();

		screen.put( "width", config.screenWidthDp );
		screen.put( "height", config.screenHeightDp );

		return screen;

	}

	private static CharSequence getScreenDimensionsLabel( JSONObject debugInformation ) {
		JSONObject device = JSONUtils.getJSONObject( debugInformation, "device" );
		JSONObject screen = JSONUtils.getJSONObject( device, "screen" );
		int w = JSONUtils.getInt( screen, "width", 0 );
		int h = JSONUtils.getInt( screen, "height", 0 );
		int d = JSONUtils.getInt( device, "density", 0 );
		return String.format( "%dx%d (%ddpi)", Integer.valueOf( w ), Integer.valueOf( h ), Integer.valueOf( d ) );
	}

	@TargetApi( Build.VERSION_CODES.GINGERBREAD )
	public static String getSerialNumber() {
		return android.os.Build.SERIAL;
	}

	public static Intent getShareDebugInformationIntent( Context context ) {

		Intent intent = new Intent( Intent.ACTION_SENDTO, Uri.fromParts( "mailto", context.getString( R.string.support_email_address ), null ) );

		intent.putExtra( Intent.EXTRA_SUBJECT, context.getString( R.string.support_email_subject ) );
		intent.putExtra( Intent.EXTRA_TEXT, wrap( LABEL_DEBUG_INFO, getDebugInformation( context ) ) );

		ResolveInfo info = context.getPackageManager().resolveActivity( intent, 0 );

		if( info == null ) {
			intent.setAction( Intent.ACTION_SEND );
			intent.setDataAndType( null, "text/plain" );
		}

		return Intent.createChooser( intent, context.getString( R.string.share_with, context.getString( R.string.feedback ) ) );

	}

	public static boolean isEncrypted( Context context ) {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && isEncryptedHoneycomb( context );
	}

	@TargetApi( Build.VERSION_CODES.HONEYCOMB )
	private static boolean isEncryptedHoneycomb( Context context ) {
		DevicePolicyManager policy = (DevicePolicyManager) context.getSystemService( Context.DEVICE_POLICY_SERVICE );
		return DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE == policy.getStorageEncryptionStatus();
	}

	public static boolean isFullDiskEncryptionSupported( Context context ) {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && isFullDiskEncryptionSupportedHoneycomb( context );
	}

	@TargetApi( Build.VERSION_CODES.HONEYCOMB )
	private static boolean isFullDiskEncryptionSupportedHoneycomb( Context context ) {
		DevicePolicyManager policy = (DevicePolicyManager) context.getSystemService( Context.DEVICE_POLICY_SERVICE );
		int status = policy.getStorageEncryptionStatus();
		return DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED != status;
	}

	public static boolean isPartnerDevice( Context context ) {
		return context != null && isPartnerDevice( context.getResources() );
	}

	public static boolean isPartnerDevice( Eligibility eligibility ) {
		String model = getModel();
		String manufacturer = getManufacturer();
		return eligibility.test( manufacturer, model );
	}

	public static boolean isPartnerDevice( Resources resources ) {
		return resources != null && isPartnerDevice( resources.getString( R.string.build_partners ).split( "," ) );
	}

	public static boolean isPartnerDevice( String [] manufacturers ) {
		return manufacturers.length > 0 && StringUtils.isAnyOf( getManufacturer(), manufacturers );
	}

	public static void putApplicationLicenseCode( Context context, CharSequence licenseCode ) {
		SilentTextApplication global = SilentTextApplication.from( context );
		global.saveLicenseCode( licenseCode );
	}

	public static void putDebugInformation( PreferenceGroup container ) {

		if( container == null ) {
			return;
		}

		container.removeAll();

		PreferenceTreeBuilder tree = new PreferenceTreeBuilder( container );
		JSONObject json = getDebugInformationJSON( container.getContext() );

		tree.addString( "Application Version", json, "application", "version_name" );
		tree.addInt( "Version Code", json, "application", "version_code" );
		tree.addString( "Build Date", json, "build", "date" );
		tree.addString( "Build Version", json, "build", "version" );
		tree.addString( "Commit", json, "build", "commit" );
		tree.addString( "Installed", json, "application", "installed" );
		tree.addString( "Last Updated", json, "application", "updated" );
		tree.addString( "Locale", json, "device", "locale" );
		tree.addString( "Screen Orientation", json, "device", "orientation" );

		tree.open( "Hardware" );
		tree.addString( "Android Version", json, "android", "version" );
		tree.addString( "API Level", json, "android", "api" );
		tree.addString( "Build Version", json, "android", "build" );
		tree.addString( "Manufacturer", json, "device", "manufacturer" );
		tree.addString( "Model", json, "device", "model" );
		tree.add( "Screen Dimensions", getScreenDimensionsLabel( json ) );
		tree.add( "Device Encryption", getEncryptionLabel( json ) );
		tree.close();

		tree.open( "Jabber / XMPP" );

		tree.addString( "Domain", json, "configuration", "xmpp.service_name" );
		tree.addBoolean( "Auto-detect", json, "configuration", "xmpp.perform_srv_lookup" );
		tree.addString( "Host", json, "configuration", "xmpp.host" );
		tree.addInt( "Port", json, "configuration", "xmpp.port" );
		tree.addBoolean( "Run in background", json, "configuration", "xmpp.background" );
		tree.addBoolean( "Online", json, "connection", "xmpp", "online" );
		tree.addString( "Status", json, "connection", "xmpp", "status" );
		tree.addString( "Host", json, "connection", "xmpp", "host" );
		tree.addInt( "Port", json, "connection", "xmpp", "port" );
		tree.addString( "Username", json, "connection", "xmpp", "user" );

		tree.close();

		tree.open( "Accounts Web API" );
		tree.addString( "Domain", json, "configuration", "api.service_name" );
		tree.addBoolean( "Auto-detect", json, "configuration", "api.perform_srv_lookup" );
		tree.addString( "Host", json, "configuration", "api.host" );
		tree.addInt( "Port", json, "configuration", "api.port" );
		tree.close();

		tree.open( "Miscellaneous" );
		tree.addBoolean( "Debug Mode", json, "configuration", "debug" );
		tree.addBoolean( "Experimental", json, "configuration", "experimental" );
		tree.addBoolean( "Generate Default Avatars", json, "configuration", "feature.generate_default_avatars" );
		tree.addBoolean( "Check User Availability", json, "configuration", "feature.check_user_availability" );
		tree.addBoolean( "Validate Certificates", json, "configuration", "validate_certificates" );
		tree.addBoolean( "Passphrase Active", json, "configuration", "passcode_set" );
		tree.addBoolean( "SCimp PKI Enabled", json, "configuration", "scimp.enable_pki" );
		tree.addBoolean( "Logging Enabled", json, "configuration", "logging_enabled" );
		tree.addBoolean( "Silent Contacts (v1)", json, "configuration", "silent_contacts_v1" );
		tree.addBoolean( "Silent Contacts (v2)", json, "configuration", "silent_contacts_v2" );
		tree.addBoolean( "Silent Phone", json, "configuration", "silent_phone" );
		tree.addString( "GCM Sender ID", json, "configuration", "gcm.sender_id" );
		tree.addString( "GCM Target", json, "configuration", "gcm.target" );
		tree.addString( "SCloud URL", json, "configuration", "scloud.url" );
		tree.close();

		tree.addString( "Timestamp", json, "timestamp" );

	}

	@TargetApi( Build.VERSION_CODES.GINGERBREAD )
	private static void setInstallAndUpdateTimes( PackageInfo from, JSONObject to ) throws JSONException {
		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD ) {
			to.put( "installed", JSONUtils.getDate( from.firstInstallTime ) );
			to.put( "updated", JSONUtils.getDate( from.lastUpdateTime ) );
		}
	}

	public static void shareDebugInformation( Context context ) {

		Intent intent = getShareDebugInformationIntent( context );

		if( intent != null ) {
			context.startActivity( intent );
		}

	}

	private static String wrap( String label, String content ) {
		StringBuilder info = new StringBuilder();
		info.append( "---BEGIN " ).append( label ).append( "---\n" );
		info.append( content );
		info.append( "\n---END " ).append( label ).append( "---\n\n" );
		return info.toString();
	}
}
