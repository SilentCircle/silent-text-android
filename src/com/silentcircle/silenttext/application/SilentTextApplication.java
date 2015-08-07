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
package com.silentcircle.silenttext.application;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.CharBuffer;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;
import javax.net.SocketFactory;

import org.json.JSONException;
import org.json.JSONObject;
import org.twuni.twoson.IllegalFormatException;
import org.twuni.twoson.JSONParser;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;

import com.silentcircle.api.AuthenticatedSession;
import com.silentcircle.api.Authenticator;
import com.silentcircle.api.Session;
import com.silentcircle.api.UserManager;
import com.silentcircle.api.model.Entitlement;
import com.silentcircle.api.model.Key;
import com.silentcircle.api.model.User;
import com.silentcircle.api.web.AuthenticatedSessionClient;
import com.silentcircle.api.web.AuthenticatorClient;
import com.silentcircle.api.web.HasSession;
import com.silentcircle.api.web.UserManagerClient;
import com.silentcircle.api.web.model.BasicKey;
import com.silentcircle.api.web.model.BasicUser;
import com.silentcircle.api.web.model.json.JSONObjectParser;
import com.silentcircle.api.web.model.json.JSONObjectWriter;
import com.silentcircle.http.client.AbstractHTTPClient;
import com.silentcircle.http.client.CachingHTTPClient;
import com.silentcircle.http.client.HTTPResponseCache;
import com.silentcircle.http.client.TrustStoreCertificateVerifier;
import com.silentcircle.http.client.apache.ApacheHTTPClient;
import com.silentcircle.http.client.apache.HttpClient;
import com.silentcircle.http.client.apache.SSLSocketFactory;
import com.silentcircle.http.client.dns.AndroidNameserverProvider;
import com.silentcircle.http.client.exception.NetworkException;
import com.silentcircle.http.client.exception.http.HTTPException;
import com.silentcircle.http.client.exception.http.client.HTTPClientForbiddenException;
import com.silentcircle.http.client.exception.http.client.HTTPClientUnauthorizedException;
import com.silentcircle.http.client.exception.http.client.HTTPClientUnknownResourceException;
import com.silentcircle.scimp.KeyGenerator;
import com.silentcircle.scimp.NamedKeyPair;
import com.silentcircle.scimp.NamedKeyPairRepositoryHelper;
import com.silentcircle.scimp.NativeKeyGenerator;
import com.silentcircle.silentstorage.UnixSecureRandom;
import com.silentcircle.silentstorage.io.AESWithCBCAndPKCS7PaddingCipherFactory;
import com.silentcircle.silentstorage.io.BufferedBlockCipherFactory;
import com.silentcircle.silentstorage.io.HMacSHA256Factory;
import com.silentcircle.silentstorage.io.MacFactory;
import com.silentcircle.silentstorage.io.Serializer;
import com.silentcircle.silentstorage.repository.Repository;
import com.silentcircle.silentstorage.repository.file.FileRepository;
import com.silentcircle.silentstorage.repository.file.RepositoryLockedException;
import com.silentcircle.silentstorage.repository.file.SecureFileRepository;
import com.silentcircle.silentstorage.repository.helper.RepositoryHelper;
import com.silentcircle.silentstorage.repository.lazy.LazyList;
import com.silentcircle.silenttext.Action;
import com.silentcircle.silenttext.Extra;
import com.silentcircle.silenttext.Manifest;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.SCimpBridge;
import com.silentcircle.silenttext.ServiceConfiguration;
import com.silentcircle.silenttext.activity.UnlockActivity;
import com.silentcircle.silenttext.client.LegacyAccountCreationClient;
import com.silentcircle.silenttext.client.SCloudBroker;
import com.silentcircle.silenttext.client.XMPPSocketFactory;
import com.silentcircle.silenttext.client.XMPPTransport;
import com.silentcircle.silenttext.client.dns.CachingSRVResolver;
import com.silentcircle.silenttext.client.model.DownloadManagerEntry;
import com.silentcircle.silenttext.client.model.ServiceEndpoint;
import com.silentcircle.silenttext.client.model.UUIDEntry;
import com.silentcircle.silenttext.client.model.repository.helper.DownloadManagerHelper;
import com.silentcircle.silenttext.client.model.repository.helper.ServiceEndpointHelper;
import com.silentcircle.silenttext.client.model.repository.helper.UUIDHelper;
import com.silentcircle.silenttext.crypto.CryptoUtils;
import com.silentcircle.silenttext.crypto.EncryptedStorage;
import com.silentcircle.silenttext.crypto.Hash;
import com.silentcircle.silenttext.crypto.StorageKeySpec;
import com.silentcircle.silenttext.fragment.ChatsFragment;
import com.silentcircle.silenttext.listener.TransportListener;
import com.silentcircle.silenttext.log.Log;
import com.silentcircle.silenttext.migration.Base64SHA1PasscodeMigration;
import com.silentcircle.silenttext.migration.MigrationRegistry;
import com.silentcircle.silenttext.migration.OneShotPBKDF2Migration;
import com.silentcircle.silenttext.model.Attachment;
import com.silentcircle.silenttext.model.Contact;
import com.silentcircle.silenttext.model.Conversation;
import com.silentcircle.silenttext.model.Credential;
import com.silentcircle.silenttext.model.MessageState;
import com.silentcircle.silenttext.model.MigrationTask;
import com.silentcircle.silenttext.model.Server;
import com.silentcircle.silenttext.model.Siren;
import com.silentcircle.silenttext.model.UserPreferences;
import com.silentcircle.silenttext.model.event.ErrorEvent;
import com.silentcircle.silenttext.model.event.Event;
import com.silentcircle.silenttext.model.event.IncomingMessage;
import com.silentcircle.silenttext.model.event.Message;
import com.silentcircle.silenttext.model.event.OutgoingMessage;
import com.silentcircle.silenttext.model.io.json.JSONSirenSerializer;
import com.silentcircle.silenttext.model.repository.helper.AttachmentHelper;
import com.silentcircle.silenttext.receiver.LockApplicationOnReceive;
import com.silentcircle.silenttext.receiver.NotificationBroadcaster;
import com.silentcircle.silenttext.repository.ContactRepository;
import com.silentcircle.silenttext.repository.ConversationRepository;
import com.silentcircle.silenttext.repository.CredentialRepository;
import com.silentcircle.silenttext.repository.EventRepository;
import com.silentcircle.silenttext.repository.ServerRepository;
import com.silentcircle.silenttext.repository.file.FileTransportQueue;
import com.silentcircle.silenttext.repository.file.encrypted.EncryptedFileConversationRepository;
import com.silentcircle.silenttext.repository.file.encrypted.EncryptedFileCredentialRepository;
import com.silentcircle.silenttext.repository.file.encrypted.EncryptedFileServerRepository;
import com.silentcircle.silenttext.repository.remote.RemoteContactRepository;
import com.silentcircle.silenttext.repository.resolver.SilentContactRepositoryV1;
import com.silentcircle.silenttext.repository.resolver.SilentContactRepositoryV2;
import com.silentcircle.silenttext.repository.resolver.SystemContactRepository;
import com.silentcircle.silenttext.service.GCMService;
import com.silentcircle.silenttext.service.SysNetObserverService;
import com.silentcircle.silenttext.task.GetDeviceChangedTask;
import com.silentcircle.silenttext.task.RevokeKeyPairTask;
import com.silentcircle.silenttext.thread.Deactivation;
import com.silentcircle.silenttext.thread.NamedThread;
import com.silentcircle.silenttext.thread.NamedThread.HasThreadName;
import com.silentcircle.silenttext.thread.NewXMPPTransport;
import com.silentcircle.silenttext.thread.RemoveKeyPair;
import com.silentcircle.silenttext.thread.SynchronizePublicKeysWithServer;
import com.silentcircle.silenttext.transport.Envelope;
import com.silentcircle.silenttext.transport.TransportQueue;
import com.silentcircle.silenttext.util.AsyncUtils;
import com.silentcircle.silenttext.util.Constants;
import com.silentcircle.silenttext.util.ExternalKeyManager;
import com.silentcircle.silenttext.util.ExternalKeyManagerFactory;
import com.silentcircle.silenttext.util.ExternalKeyManagerV1;
import com.silentcircle.silenttext.util.ExternalKeyManagerV2;
import com.silentcircle.silenttext.util.IOUtils;
import com.silentcircle.silenttext.util.Locker;
import com.silentcircle.silenttext.util.StringUtils;
import com.silentcircle.silenttext.view.OptionsDrawer;

public class SilentTextApplication extends Application implements HasSession {

	public static class CharSequenceHelper extends com.silentcircle.silentstorage.repository.helper.RepositoryHelper<CharSequence> {

		public CharSequenceHelper() {
			super( new CharSequenceSerializer() );
		}

		@Override
		public char [] identify( CharSequence arg0 ) {
			return arg0.toString().toCharArray();
		}
	}

	public static class CharSequenceSerializer extends com.silentcircle.silentstorage.io.Serializer<CharSequence> {

		@Override
		public CharSequence read( DataInputStream in ) throws IOException {
			return read( in, new String() );
		}

		@Override
		public CharSequence read( DataInputStream in, CharSequence out ) throws IOException {
			char [] seq = readChars( in );
			return new String( seq );
		}

		@Override
		public CharSequence write( CharSequence in, DataOutputStream out ) throws IOException {
			writeChars( in.toString().toCharArray(), out );
			return in;
		}
	}

	public static class SilentTextApplicationLocker implements Locker, Runnable, HasThreadName {

		private final SilentTextApplication application;
		private final Log log = new Log( getThreadName() );

		private char [] hashedPassPhrase;

		public SilentTextApplicationLocker( SilentTextApplication application ) {
			this.application = application;
		}

		@Override
		public String getThreadName() {
			return "ExternalUnlockRequest";
		}

		@Override
		public void lock() {
			application.lock();
		}

		@Override
		public void run() {
			try {
				application.unlockWithHashedPassPhrase( hashedPassPhrase );
			} catch( Throwable exception ) {
				log.warn( exception, "#run" );
			} finally {
				CryptoUtils.randomize( hashedPassPhrase );
				hashedPassPhrase = null;
			}
		}

		@Override
		public void unlock( char [] hashedPassPhrase ) {
			this.hashedPassPhrase = hashedPassPhrase;
			new NamedThread( this ).start();
		}

	}

	private static final String TRUST_STORE_FILE = "silentcircle.bks.pubcerts";

	private static final String TRUST_STORE_PASSWORD = "silence";

	private SecureFileRepository<DownloadManagerEntry> downloadManagerRepository;

	private SecureFileRepository<UUIDEntry> UUIDRepository;

	private static final NamedKeyPair [] EMPTY_ARRAY_NAMED_KEY_PAIR = new NamedKeyPair [0];

	private static final String UTF16 = "UTF-16";

	private static final String KEY_ALGORITHM = "AES";

	private static final String APPLICATION_PREFERENCES_KEY = "(default)";

	private static final long DELAY_RESPOND_TO_NETWORK_STATE_CHANGE = 2500;

	private static final MigrationRegistry MIGRATIONS = new MigrationRegistry();

	private static final JSONSirenSerializer SIREN = new JSONSirenSerializer();

	public static final String ACCOUNT_CREATION_CLIENT_BASE_URL = "https://sccps.silentcircle.com";

	private static final String HARDCODED_VOICEMAIL_ID = "scvoicemail@silentcircle.com";

	static {
		MIGRATIONS.register( new Base64SHA1PasscodeMigration() );
		MIGRATIONS.register( new OneShotPBKDF2Migration() );
	}

	private static final AttachmentHelper ATTACHMENT_HELPER = new AttachmentHelper();

	protected static com.silentcircle.api.model.Key extractPublicKey( NamedKeyPair keyPair ) {
		if( keyPair == null || keyPair.getPublicKey() == null || keyPair.getPublicKey().length <= 0 ) {
			return null;
		}
		return parsePublicKey( keyPair.getPublicKey() );
	}

	public static SilentTextApplication from( Context context ) {
		return context == null ? null : (SilentTextApplication) context.getApplicationContext();
	}

	public static String getDisplayName( User user, ContactRepository repository ) {

		if( user == null || user.getID() == null || repository == null ) {
			return null;
		}

		String partner = user.getID().toString();
		String displayName = repository.getDisplayName( partner );

		if( !StringUtils.isMinimumLength( displayName, 1 ) ) {
			displayName = RemoteContactRepository.getDisplayName( user );
		}

		if( StringUtils.equals( displayName, "scvoicemail" ) ) {
			displayName = "Voice Mail";
		}

		return displayName;

	}

	private static String getDomain() {
		return ServiceConfiguration.getInstance().getXMPPServiceName();
	}

	private static String getLocator( byte [] publicKey ) {
		if( publicKey == null || publicKey.length <= 0 ) {
			return null;
		}
		return parsePublicKey( publicKey ).getLocator().toString();
	}

	private static String getLogTag() {
		return "SilentTextApplication";
	}

	private static boolean isExpired( NamedKeyPair keyPair ) {
		Key key = parsePublicKey( keyPair.getPublicKey() );
		return isPublicKeyExpired( key );
	}

	public static boolean isOutgoingResendRequest( Message message ) {
		try {
			return message instanceof OutgoingMessage && new JSONObject( message.getText() ).has( "request_resend" );
		} catch( JSONException exception ) {
			return false;
		}
	}

	private static boolean isPublicKeyExpired( com.silentcircle.api.model.Key key ) {
		return isPublicKeyExpired( key.getCreationDate(), key.getExpirationDate() );
	}

	private static boolean isPublicKeyExpired( long creationDate, long expirationDate ) {
		long now = System.currentTimeMillis();
		boolean notYetCreated = creationDate > now;
		boolean alreadyExpired = expirationDate <= now;
		return notYetCreated || alreadyExpired;
	}

	public static boolean isResendRequest( Message message ) {
		try {
			return new JSONObject( message.getText() ).has( "request_resend" );
		} catch( JSONException exception ) {
			return false;
		}
	}

	public static boolean isRunningInDalvikVM() {
		return "Dalvik".equals( System.getProperty( "java.vm.name" ) );
	}

	private static Key parsePublicKey( byte [] publicKey ) {
		return JSONObjectParser.parseKey( JSONParser.parse( publicKey ) );
	}

	private static Siren parseSiren( String text ) {
		try {
			return SIREN.parse( text );
		} catch( IOException exception ) {
			return null;
		} catch( IllegalFormatException exception ) {
			return null;
		}
	}

	private static com.silentcircle.api.model.Key pick( List<com.silentcircle.api.model.Key> keys ) {
		if( keys == null || keys.isEmpty() ) {
			return null;
		}
		com.silentcircle.api.model.Key best = null;
		for( int i = 0; i < keys.size(); i++ ) {
			com.silentcircle.api.model.Key current = keys.get( i );
			if( !isPublicKeyExpired( current ) ) {
				if( best == null || current.getExpirationDate() > best.getExpirationDate() ) {
					best = current;
				}
			}
		}
		return best;
	}

	protected boolean connected;

	protected SCimpBridge scimpBridge;

	protected XMPPTransport xmppTransport;

	protected SCloudBroker broker;

	protected ConversationRepository conversations;

	protected ServerRepository servers;
	protected CredentialRepository credentials;
	protected ContactRepository contacts;
	protected FileTransportQueue outgoingMessageQueue;
	protected byte [] userKey;
	protected byte [] applicationKey;
	private Log log;
	private long timeOfMostRecentActivity;
	protected String xmppTransportConnectionStatus;
	private Thread xmppConnectionThread;
	private final Timer timer = new Timer( "silent-text-application" );
	private TimerTask autoLock;
	private String license;
	private TimerTask respondToNetworkStateChange;
	protected TimerTask autoDisconnect;
	private SecureFileRepository<User> users;
	private SecureFileRepository<NamedKeyPair> keyPairs;
	private KeyGenerator keyGenerator;
	private SecureFileRepository<ServiceEndpoint> serviceEndpointRepository;
	private FileRepository<UserPreferences> applicationPreferencesRepository;
	private FileRepository<MigrationTask> migrationTasks;
	private SecureFileRepository<UserPreferences> userPreferencesRepository;
	private ContactRepository silentContacts;
	private ContactRepository systemContacts;
	private boolean foreground;
	private final HTTPResponseCache httpResponseCache = new HTTPResponseCache();
	private ExternalKeyManagerFactory externalKeyManagerFactory;
	private CachingSRVResolver cachingSRVResolver;

	private LegacyAccountCreationClient accountCreationClient;

	private SecureFileRepository<CharSequence> licenseCodeRepository;

	private List<String> deletedUsers = new ArrayList<String>();

	private final static String TAG = "SilentTextApplication";

	public void adviseReconnect() {
		adviseReconnect( false );
	}

	public void adviseReconnect( boolean interruptIfRunning ) {
		Server server = getServer( "xmpp" );
		if( server == null ) {
			return;
		}
		if( interruptIfRunning ) {
			if( xmppConnectionThread != null && xmppConnectionThread.isAlive() ) {
				xmppConnectionThread.interrupt();
			}
		}
		createXMPPTransportAsync( server.getCredential() );
	}

	public void cancelAutoDisconnect() {
		if( autoDisconnect != null ) {
			autoDisconnect.cancel();
			autoDisconnect = null;
			timer.purge();
		}
	}

	public void cancelAutoLock() {
		if( autoLock != null ) {
			autoLock.cancel();
			autoLock = null;
			timer.purge();
		}
	}

	private void checkNetworkState() {

		ConnectivityManager manager = (ConnectivityManager) getSystemService( CONNECTIVITY_SERVICE );

		if( manager == null ) {
			return;
		}

		NetworkInfo network = manager.getActiveNetworkInfo();
		boolean c = network != null && network.isConnected();

		if( c == connected ) {
			return;
		}

		connected = c;

	}

	private void clearUserData( String username ) {
		for( File file : getUserDirs( username ) ) {
			IOUtils.delete( file );
		}
	}

	private void createApplicationPreferencesRepository() {
		if( applicationPreferencesRepository == null ) {
			applicationPreferencesRepository = UserPreferences.repository( getFilesDir( "application_preferences" ) );
		}
	}

	private void createBroker() {
		Session session = getSession();
		if( session != null ) {
			broker = new SCloudBroker( session.getAccessToken(), createHTTPClient(), getCachingSRVResolver() );
		}
	}

	private CachingSRVResolver createCachingSRVResolver() {
		if( cachingSRVResolver == null ) {
			cachingSRVResolver = new CachingSRVResolver( getServiceEndpointRepository(), new AndroidNameserverProvider() );
		}
		return cachingSRVResolver;
	}

	private void createContactRepository() {
		if( contacts != null ) {
			return;
		}
		if( SilentContactRepositoryV2.supports( this ) ) {
			contacts = new SilentContactRepositoryV2( getContentResolver() );
		} else if( SilentContactRepositoryV1.supports( this ) ) {
			contacts = new SilentContactRepositoryV1( getContentResolver() );
		} else {
			contacts = new SystemContactRepository( getContentResolver() );
		}
	}

	private void createConversationRepository() {
		if( conversations != null || userKey == null ) {
			return;
		}
		String username = getServer( "xmpp" ).getCredential().getUsername();
		File file = getUserConversationsDir( username );
		conversations = new EncryptedFileConversationRepository( new SecretKeySpec( userKey, KEY_ALGORITHM ), file, getCacheDir() );
	}

	private void createCredentialRepository() {
		if( credentials != null || userKey == null ) {
			return;
		}
		String username = getServer( "xmpp" ).getCredential().getUsername();
		File file = getUserCredentialsDir( username );
		credentials = new EncryptedFileCredentialRepository( new SecretKeySpec( userKey, KEY_ALGORITHM ), file );
	}

	public UserPreferences createDefaultApplicationPreferences() {
		return createDefaultUserPreferences( APPLICATION_PREFERENCES_KEY );
	}

	public UserPreferences createDefaultUserPreferences() {
		return createDefaultUserPreferences( getUsername() );
	}

	public UserPreferences createDefaultUserPreferences( String username ) {

		UserPreferences preferences = new UserPreferences();

		preferences.userName = username == null ? null : username.toCharArray();
		preferences.deviceName = new String( getLocalResourceNameLegacy() ).toCharArray();

		preferences.notifications = NotificationBroadcaster.isEnabledLegacy( this );
		preferences.notificationSound = NotificationBroadcaster.isSoundEnabledLegacy( this );
		preferences.notificationVibrate = NotificationBroadcaster.isVibrateEnabledLegacy( this );

		preferences.passcodeUnlockValidityPeriod = OptionsDrawer.getInactivityTimeoutLegacy( this );
		preferences.isPasscodeSet = !OptionsDrawer.isEmptyPasscodeLegacy( this );
		preferences.sendDeliveryAcknowledgments = OptionsDrawer.isSendReceiptsEnabledLegacy( this );

		return preferences;

	}

	private void createDownloadManagerRepository() {
		if( downloadManagerRepository == null ) {
			File baseDirectory = getFilesDir( "downloadmanager" );
			DownloadManagerHelper helper = new DownloadManagerHelper();
			BufferedBlockCipherFactory cipherFactory = new AESWithCBCAndPKCS7PaddingCipherFactory();
			MacFactory macFactory = new HMacSHA256Factory();
			downloadManagerRepository = new SecureFileRepository<DownloadManagerEntry>( baseDirectory, helper, cipherFactory, macFactory );
		}
		if( applicationKey == null ) {
			downloadManagerRepository.lock();
		} else {
			downloadManagerRepository.unlock( applicationKey );
		}
	}

	private void createExternalKeyManagerFactory() {
		Locker locker = new SilentTextApplicationLocker( this );
		externalKeyManagerFactory = new ExternalKeyManagerFactory( new ExternalKeyManagerV2( this, locker ), new ExternalKeyManagerV1( this, locker ) );
	}

	private AbstractHTTPClient createHTTPClient() {

		ServiceConfiguration config = ServiceConfiguration.getInstance();

		if( !config.api.requireStrongCiphers ) {
			if( !config.shouldValidateCertificates ) {
				return new CachingHTTPClient( new ApacheHTTPClient( new HttpClient() ), httpResponseCache );
			}
			return new CachingHTTPClient( new ApacheHTTPClient( new HttpClient( getTrustStore() ) ), httpResponseCache );
		}

		return new CachingHTTPClient( new ApacheHTTPClient( new HttpClient( getHTTPSocketFactory() ) ), httpResponseCache );

	}

	private NamedKeyPair createKeyPair() {

		if( !ServiceConfiguration.getInstance().scimp.enablePKI ) {
			return null;
		}

		NamedKeyPair keyPair = new NamedKeyPair();

		byte [] storageKey = getLocalStorageKey();

		keyPair.setPrivateKey( getKeyGenerator().generateKey( getUsername(), storageKey ) );
		keyPair.setPublicKey( getKeyGenerator().getPublicKey( keyPair.getPrivateKey(), storageKey ) );
		keyPair.locator = getLocator( keyPair.getPublicKey() );

		if( keyPair.locator == null ) {
			return null;
		}

		save( keyPair );

		return keyPair;

	}

	private void createLicenseCodeRepository() {
		if( licenseCodeRepository == null ) {
			File baseDirectory = getFilesDir( "license" );
			BufferedBlockCipherFactory cipherFactory = new AESWithCBCAndPKCS7PaddingCipherFactory();
			MacFactory macFactory = new HMacSHA256Factory();
			licenseCodeRepository = new SecureFileRepository<CharSequence>( baseDirectory, new CharSequenceHelper(), cipherFactory, macFactory );
			// this repo stays unlocked
			// String deviceID = DeviceUtils.getDeviceID( this.getApplicationContext() );
			// deviceID = ((deviceID != null) && (deviceID.length() >= 16)) ? deviceID.substring( 0,
			// 16 ) : "B8CJ8AXHA89DACHD";
			licenseCodeRepository.unlock( "B8CJ8AXHA89DACHD".getBytes() );
		}
	}

	private void createLog() {
		if( log != null ) {
			return;
		}
		log = new Log( getLogTag() );
	}

	private void createNamedKeyPairRepository() {
		if( keyPairs == null ) {
			String baseDirectoryName = Hash.sha1( getUsername(), ".key_pairs" );
			File baseDirectory = new File( getFilesDir(), baseDirectoryName );
			NamedKeyPairRepositoryHelper helper = new NamedKeyPairRepositoryHelper();
			BufferedBlockCipherFactory cipherFactory = new AESWithCBCAndPKCS7PaddingCipherFactory();
			MacFactory macFactory = new HMacSHA256Factory();
			keyPairs = new SecureFileRepository<NamedKeyPair>( baseDirectory, helper, cipherFactory, macFactory );
		}
		if( userKey == null ) {
			keyPairs.lock();
		} else {
			keyPairs.unlock( userKey );
		}
	}

	private void createOutgoingMessageQueue() {

		if( outgoingMessageQueue == null ) {
			BufferedBlockCipherFactory cipherFactory = new AESWithCBCAndPKCS7PaddingCipherFactory();
			MacFactory macFactory = new HMacSHA256Factory();
			outgoingMessageQueue = new FileTransportQueue( this, getTransportQueueDir(), cipherFactory, macFactory );
		}

		if( userKey == null ) {
			outgoingMessageQueue.lock();
		} else {
			outgoingMessageQueue.unlock( userKey );
		}

	}

	private void createServerRepository() {
		if( servers != null || applicationKey == null ) {
			return;
		}
		File file = new File( getFilesDir(), Hash.sha1( "servers" ) );
		servers = new EncryptedFileServerRepository( new SecretKeySpec( applicationKey, KEY_ALGORITHM ), file );
	}

	private void createServiceEndpointRepository() {
		if( serviceEndpointRepository == null ) {
			File baseDirectory = getFilesDir( "networks" );
			ServiceEndpointHelper helper = new ServiceEndpointHelper();
			BufferedBlockCipherFactory cipherFactory = new AESWithCBCAndPKCS7PaddingCipherFactory();
			MacFactory macFactory = new HMacSHA256Factory();
			serviceEndpointRepository = new SecureFileRepository<ServiceEndpoint>( baseDirectory, helper, cipherFactory, macFactory );
		}
		if( applicationKey == null ) {
			serviceEndpointRepository.lock();
		} else {
			serviceEndpointRepository.unlock( applicationKey );
		}
	}

	private void createUserPreferencesRepository() {
		if( userPreferencesRepository == null ) {
			BufferedBlockCipherFactory cipherFactory = new AESWithCBCAndPKCS7PaddingCipherFactory();
			MacFactory macFactory = new HMacSHA256Factory();
			userPreferencesRepository = UserPreferences.repository( getFilesDir( "user_preferences" ), cipherFactory, macFactory );
		}
		if( applicationKey == null ) {
			userPreferencesRepository.lock();
		} else {
			userPreferencesRepository.unlock( applicationKey );
		}
	}

	private void createUUIDRepository() {
		if( UUIDRepository == null ) {
			File baseDirectory = getFilesDir( "UUID" );
			UUIDHelper helper = new UUIDHelper();
			BufferedBlockCipherFactory cipherFactory = new AESWithCBCAndPKCS7PaddingCipherFactory();
			MacFactory macFactory = new HMacSHA256Factory();
			UUIDRepository = new SecureFileRepository<UUIDEntry>( baseDirectory, helper, cipherFactory, macFactory );
		}
		if( applicationKey == null ) {
			UUIDRepository.lock();
		} else {
			UUIDRepository.unlock( applicationKey );
		}
	}

	public void createXMPPTransportAsync( Credential credential ) {

		if( xmppConnectionThread != null && xmppConnectionThread.isAlive() ) {
			return;
		}

		xmppTransportConnectionStatus = getString( R.string.not_connected );
		sendBroadcast( Action.XMPP_STATE_CHANGED.intent(), Manifest.permission.READ );

		if( credential == null ) {
			setXMPPTransport( (XMPPTransport) null );
			return;
		}

		xmppConnectionThread = new NamedThread( new NewXMPPTransport( this, getXMPPTransport(), credential, getOrCreateUUID(), getOutgoingMessageQueue(), getCachingSRVResolver(), getXMPPSocketFactory() ) {

			@Override
			protected void onClientConnected( XMPPTransport transport ) {
				if( transport != null && transport.isConnected() ) {
					getLog().debug( "XMPP #onClientConnected host:%s port:%d", transport.getServerHost(), Integer.valueOf( transport.getServerPort() ) );
					connected = true;
					xmppTransportConnectionStatus = getString( R.string.connected );
					setXMPPTransport( transport );
					sendBroadcast( Action.XMPP_STATE_CHANGED.intent(), Manifest.permission.READ );
				}
			}

			@Override
			protected void onClientInitialized( XMPPTransport transport ) {
				getLog().debug( "XMPP #onClientInitialized" );
				xmppTransportConnectionStatus = getString( R.string.connecting );
				sendBroadcast( Action.XMPP_STATE_CHANGED.intent(), Manifest.permission.READ );
				// setJabber( client );
			}

			@Override
			protected void onError( Throwable throwable ) {
				getLog().warn( throwable, "XMPP #onError" );
				if( throwable.getMessage() != null && !"".equals( throwable.getMessage() ) ) {
					xmppTransportConnectionStatus = throwable.getMessage();
				} else {
					xmppTransportConnectionStatus = getString( R.string.not_connected );
				}
				setXMPPTransport( (XMPPTransport) null ); // we do not have a valid client!
				ServiceConfiguration.getInstance().removeXMPPFromCache( getCachingSRVResolver() );
				sendBroadcast( Action.XMPP_STATE_CHANGED.intent(), Manifest.permission.READ );

				if( throwable instanceof IllegalStateException ) {
					AsyncUtils.execute( new GetDeviceChangedTask( getApplicationContext() ) {

						@Override
						protected void onPostExecute( Void result ) {
							if( deviceChanged ) {
								deactivate();
							}
						}

					}, getUsername() );
				}
			}

			@Override
			protected void onLookupServiceRecord( String domain ) {
				getLog().debug( "XMPP #onLookupServiceRecord domain:%s", domain );
				xmppTransportConnectionStatus = getString( R.string.performing_srv_lookup );
				sendBroadcast( Action.XMPP_STATE_CHANGED.intent(), Manifest.permission.READ );
			}

			@Override
			protected void onServiceRecordReceived( CharSequence host, int port ) {
				getLog().debug( "XMPP #onServiceRecordReceived host:%s port:%d", host, Integer.valueOf( port ) );
				xmppTransportConnectionStatus = getString( R.string.connecting );
				sendBroadcast( Action.XMPP_STATE_CHANGED.intent(), Manifest.permission.READ );
			}

		} );

		xmppConnectionThread.start();

	}

	public void deactivate() {
		deactivate( false );
	}

	public void deactivate( boolean removeUserData ) {

		Action.BEGIN_DEACTIVATE.broadcast( this, Manifest.permission.READ );

		final String username = getUsername();
		removeSharedSession();

		GCMService.unregister( this );
		getSCimpBridge().setIdentity( null );
		getSCimpBridge().onDestroy();
		scimpBridge = null;
		NotificationBroadcaster.cancel( this );
		UUIDRepository = null;

		try {
			Session session = getSession();
			if( session != null ) {
				if( session.getID() != null ) {
					try {
						session.stopReceivingPushNotifications( getPackageName() );
						session.logout();
					} catch( HTTPClientForbiddenException httpException ) {
						// session is already "deactivated"
					} catch( Throwable exception ) {
						getLog().warn( exception, "#deactivate" );
					}
				}
			}
			getServers().remove( getServer( "broker" ) );
			if( isXMPPTransportConnected() ) {
				XMPPTransport existingJabber = getXMPPTransport();
				if( existingJabber != null ) {
					existingJabber.disconnect();
					setXMPPTransport( (XMPPTransport) null );
				}
			}
			Server xmppServer = getServer( "xmpp" );
			if( xmppServer != null ) {
				save( xmppServer.getCredential() );
				getServers().remove( xmppServer );
			}
		} finally {
			if( removeUserData ) {
				clearUserData( username );
			}
			lock( false );
			Action.FINISH_DEACTIVATE.broadcast( this, Manifest.permission.READ );
		}

	}

	protected void enqueue( Message message ) {

		Envelope envelope = new Envelope();

		envelope.id = message.getId();

		if( envelope.id == null ) {
			envelope.id = UUID.randomUUID().toString();
		}

		envelope.time = System.currentTimeMillis();
		envelope.from = getUsername();
		envelope.to = message.getConversationID();
		envelope.content = message.getCiphertext();
		envelope.notifiable = true;
		envelope.badgeworthy = true;
		envelope.state = Envelope.State.PENDING;

		XMPPTransport xmpp = getXMPPTransport();

		if( xmpp != null ) {
			xmpp.sendEncryptedMessage( envelope );
		} else {
			try {
				getOutgoingMessageQueue().add( envelope );
			} catch( RepositoryLockedException exception ) {
				getLog().warn( exception, "#onSendPacket id:%s to:%s", envelope.id, envelope.to );
			}
		}

	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		getSCimpBridge().onDestroy();
	}

	protected Event findEventByID( String id ) {
		ConversationRepository conversations = getConversations();
		for( Conversation conversation : conversations.list() ) {
			EventRepository events = conversations.historyOf( conversation );
			Event event = events.findById( id );
			if( event != null ) {
				return event;
			}
		}
		return null;
	}

	public LegacyAccountCreationClient getAccountCreationClient() {
		if( accountCreationClient == null ) {
			accountCreationClient = new LegacyAccountCreationClient( this, createHTTPClient(), ACCOUNT_CREATION_CLIENT_BASE_URL );
		}
		return accountCreationClient;
	}

	public String getAPIURL() {
		return ServiceConfiguration.getInstance().getAPIURL( getCachingSRVResolver() );
	}

	public String getAppVersionName() {
		try {
			return getPackageManager().getPackageInfo( getPackageName(), 0 ).versionName;
		} catch( NameNotFoundException exception ) {
			return getString( R.string.unknown );
		}
	}

	public SecureFileRepository<Attachment> getAttachments() {
		return getSecureRepository( ATTACHMENT_HELPER, "attachments" );
	}

	public Authenticator getAuthenticator() {
		return new AuthenticatorClient( createHTTPClient(), getAPIURL() );
	}

	public SCloudBroker getBroker() {
		createBroker();
		return broker;
	}

	protected File getCacheDir( String... names ) {
		return new File( getCacheDir(), Hash.sha1( names ) );
	}

	public CachingSRVResolver getCachingSRVResolver() {
		createCachingSRVResolver();
		return cachingSRVResolver;
	}

	public ContactRepository getContacts() {
		createContactRepository();
		return contacts;
	}

	public Conversation getConversation( String partner ) {
		if( partner == null ) {
			return null;
		}
		ConversationRepository conversations = getConversations();
		if( conversations == null ) {
			return null;
		}
		return conversations.findByPartner( partner );
	}

	public ConversationRepository getConversations() {
		createConversationRepository();
		return conversations;
	}

	public Credential getCredential( String id ) {
		CredentialRepository repository = getCredentials();
		return repository == null ? null : repository.findById( id );
	}

	public CredentialRepository getCredentials() {
		createCredentialRepository();
		return credentials;
	}

	public List<String> getDeletedUsers() {
		if( deletedUsers.size() > 0 ) {
			return deletedUsers;
		}
		SharedPreferences prefs = getSharedPreferences( ChatsFragment.DELETED_USER_FROM_CONVERSATION_LIST, Context.MODE_PRIVATE );
		String serialized = prefs.getString( ChatsFragment.DELETED_USER_FROM_CONVERSATION_LIST, null );
		if( serialized != null ) {
			deletedUsers = new ArrayList<String>( Arrays.asList( TextUtils.split( serialized, "," ) ) );
		}
		return deletedUsers;
	}

	public String getDisplayName( String username ) {
		return getDisplayName( username, getContacts() );
	}

	public String getDisplayName( String username, ContactRepository repository ) {
		try {
			return getDisplayName( getUser( username ), repository );
		} catch( RepositoryLockedException exception ) {
			return null;
		}
	}

	public String getDisplayName( User user ) {
		return getDisplayName( user, getContacts() );
	}

	public SecureFileRepository<DownloadManagerEntry> getDownloadManagerRepository() {
		createDownloadManagerRepository();
		return downloadManagerRepository;
	}

	private char [] getEncryptionPassPhraseForUserKey( Credential credential ) {
		if( credential == null ) {
			return null;
		}
		return getEncryptionPassPhraseForUserKey( credential.getPassword() );
	}

	protected char [] getEncryptionPassPhraseForUserKey( Server server ) {
		if( server == null ) {
			return null;
		}
		return getEncryptionPassPhraseForUserKey( server.getCredential() );
	}

	private char [] getEncryptionPassPhraseForUserKey( String password ) {
		if( password == null ) {
			return null;
		}
		char [] applicationKeyChars = CryptoUtils.toCharArray( applicationKey );
		char [] passcode = new char [applicationKeyChars.length + password.length()];
		for( int i = 0; i < applicationKeyChars.length; i++ ) {
			passcode[i] = applicationKeyChars[i];
		}
		password.getChars( 0, password.length(), passcode, applicationKeyChars.length );
		CryptoUtils.randomize( applicationKeyChars );
		return passcode;
	}

	private ExternalKeyManager getExternalKeyManager() {
		return externalKeyManagerFactory.getExternalKeyManager();
	}

	protected File getFilesDir( String... names ) {
		return new File( getFilesDir(), Hash.sha1( names ) );
	}

	public CharSequence getFullJIDForUsername( CharSequence username ) {
		CharArrayWriter out = new CharArrayWriter();
		boolean shouldAppend = true;
		for( int i = 0; i < username.length(); i++ ) {
			char c = username.charAt( i );
			out.append( username.charAt( i ) );
			if( c == '@' ) {
				shouldAppend = false;
			}
		}
		if( shouldAppend ) {
			out.append( '@' );
			out.append( getDomain() );
		}
		return CharBuffer.wrap( out.toCharArray() );
	}

	public UserPreferences getGlobalPreferences() {

		createApplicationPreferencesRepository();

		UserPreferences preferences = null;
		try {
			preferences = applicationPreferencesRepository.findByID( APPLICATION_PREFERENCES_KEY.toCharArray() );
		} catch( RuntimeException exception ) {
			getLog().info( exception, "#getGlobalPreferences #load" );
		}

		if( preferences == null ) {
			preferences = createDefaultApplicationPreferences();
			try {
				applicationPreferencesRepository.save( preferences );
			} catch( RuntimeException exception ) {
				getLog().warn( exception.getCause(), "#getGlobalPreferences #save" );
			}
		}

		return preferences;

	}

	public HTTPResponseCache getHTTPResponseCache() {
		return httpResponseCache;
	}

	protected org.apache.http.conn.ssl.SSLSocketFactory getHTTPSocketFactory() {
		Resources resources = getResources();
		String [] protocols = resources.getStringArray( R.array.security_strict_network_protocols );
		String [] ciphers = resources.getStringArray( R.array.security_strict_cipher_suites );
		try {
			return new SSLSocketFactory( getTrustStore(), protocols, ciphers );
		} catch( Throwable exception ) {
			return null;
		}
	}

	public KeyGenerator getKeyGenerator() {
		if( keyGenerator == null ) {
			getSCimpBridge();
			keyGenerator = new NativeKeyGenerator();
		}
		return keyGenerator;
	}

	public NamedKeyPair getKeyPair( String locator ) {
		createNamedKeyPairRepository();
		return locator == null ? null : keyPairs.findByID( locator.toCharArray() );
	}

	public List<NamedKeyPair> getKeyPairs() {
		createNamedKeyPairRepository();
		return removeExpired( keyPairs.list() );
	}

	public String getLicense() {
		if( license == null ) {
			license = readAssetAsString( "LICENSE" );
		}
		return license;
	}

	public CharSequence getLicenseCode() {
		createLicenseCodeRepository();
		LazyList<CharSequence> list = licenseCodeRepository.list();
		if( list == null || list.size() == 0 ) {
			return null;
		}
		return list.get( 0 );
	}

	public String getLocalResourceName() {
		String defaultResourceName = Build.MODEL;
		try {
			UserPreferences preferences = getUserPreferences();
			return preferences == null || preferences.deviceName == null ? defaultResourceName : new String( preferences.deviceName );
		} catch( RuntimeException exception ) {
			getLog().error( exception, "#getLocalResourceName" );
			return defaultResourceName;
		}
	}

	private byte [] getLocalResourceNameLegacy() {
		try {
			return getLocalResourceNameLegacyOrThrowException();
		} catch( Throwable exception ) {
			return StringUtils.toByteArray( Build.MODEL );
		}
	}

	private byte [] getLocalResourceNameLegacyOrThrowException() {
		Server server = getServer( "xmpp" );
		Credential credential = server.getCredential();
		byte [] resource = credential.getResourceAsByteArray();
		if( resource == null ) {
			throw new NullPointerException();
		}
		return resource;
	}

	public byte [] getLocalStorageKey() {
		return userKey == null ? null : CryptoUtils.copyOf( userKey, 32 );
	}

	protected Log getLog() {
		createLog();
		return log;
	}

	public FileRepository<MigrationTask> getMigrationTasks() {
		if( migrationTasks == null ) {
			migrationTasks = new FileRepository<MigrationTask>( getFilesDir( "migrations" ), new MigrationTask.Helper() );
		}
		return migrationTasks;
	}

	public Repository<NamedKeyPair> getNamedKeyPairs() {
		createNamedKeyPairRepository();
		return keyPairs;
	}

	public Conversation getOrCreateConversation( CharSequence partner ) {
		User user = getUser( partner );
		if( user == null ) {
			BasicUser u = new BasicUser();
			u.setID( partner );
			user = u;
		}
		return getOrCreateConversation( user );
	}

	public Conversation getOrCreateConversation( User user ) {

		if( user == null ) {
			return null;
		}

		// String partner = user.getID().toString();
		// Conversation conversation = getConversation( partner );

		String partner = null;
		Conversation conversation = null;
		if( user.getID() != null ) {
			partner = user.getID().toString();
			conversation = getConversation( partner );
		}

		if( conversation == null ) {

			conversation = new Conversation();
			conversation.setStorageKey( CryptoUtils.randomBytes( 64 ) );
			conversation.setPartner( new Contact( partner ) );
			conversation.getPartner().setAlias( getDisplayName( user ) );

			if( isSelf( partner ) ) {
				conversation.getPartner().setDevice( getLocalResourceName() );
				getConversations().save( conversation );
			} else {
				getConversations().save( conversation );
			}

		}

		return conversation;

	}

	public NamedKeyPair getOrCreateKeyPair() {

		List<NamedKeyPair> pairs = getKeyPairs();

		if( pairs != null && !pairs.isEmpty() ) {
			return pairs.get( 0 );
		}

		return createKeyPair();

	}

	private Server getOrCreateServer( String id ) {
		Server server = getServer( id );
		if( server == null ) {
			server = new Server( id );
		}
		return server;
	}

	public String getOrCreateUUID() {

		List<UUIDEntry> UUIDs = null;

		try {
			UUIDs = getUUIDs();
		} catch( RepositoryLockedException e ) {
			log.error( e, "Repository locked error" );

			return null;
		}

		if( UUIDs != null && !UUIDs.isEmpty() && UUIDs.get( 0 ) != null && UUIDs.get( 0 ).getUUID() != null ) {
			return UUIDs.get( 0 ).getUUID();
		}

		UUIDEntry UUIDEntry = new UUIDEntry( UUID.randomUUID().toString() );

		UUIDRepository.save( UUIDEntry );

		return UUIDEntry.getUUID();
	}

	public TransportQueue getOutgoingMessageQueue() {
		createOutgoingMessageQueue();
		return outgoingMessageQueue;
	}

	public PackageInfo getPackageInfo() {
		try {
			return getPackageManager().getPackageInfo( getPackageName(), 0 );
		} catch( NameNotFoundException exception ) {
			throw new RuntimeException( exception );
		}
	}

	public String getPrivacyPolicy() {
		return readRawResourceAsString( R.raw.privacy );
	}

	private byte [] getPrivateKey() {
		NamedKeyPair pair = getOrCreateKeyPair();
		if( pair != null ) {
			return pair.getPrivateKey();
		}
		return null;
	}

	public byte [] getPublicKey( String partner ) {

		if( !ServiceConfiguration.getInstance().scimp.enablePKI ) {
			return null;
		}

		User user = getUser( partner );

		if( user == null ) {
			return null;
		}

		List<com.silentcircle.api.model.Key> keys = user.getKeys();

		if( keys == null || keys.isEmpty() ) {
			return null;
		}

		com.silentcircle.api.model.Key key = pick( keys );

		if( !( key instanceof BasicKey ) ) {
			return null;
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try {
			JSONObjectWriter.writeKey( (BasicKey) key, new DataOutputStream( out ) );
			return out.toByteArray();
		} catch( IOException exception ) {
			return null;
		}

	}

	public String getPushNotificationToken() {
		return getSharedPreferences( "gcm", MODE_PRIVATE ).getString( "token", null );
	}

	public SCimpBridge getSCimpBridge() {
		if( scimpBridge == null ) {
			scimpBridge = new SCimpBridge( this );
			scimpBridge.onCreate();
		}
		return scimpBridge;
	}

	public <T> SecureFileRepository<T> getSecureRepository( RepositoryHelper<T> helper, String... path ) {
		BufferedBlockCipherFactory cipherFactory = new AESWithCBCAndPKCS7PaddingCipherFactory();
		MacFactory macFactory = new HMacSHA256Factory();
		SecureFileRepository<T> repository = new SecureFileRepository<T>( getFilesDir( path ), helper, cipherFactory, macFactory );
		if( userKey == null ) {
			repository.lock();
		} else {
			repository.unlock( userKey );
		}
		return repository;
	}

	public Server getServer( String name ) {
		ServerRepository servers = getServers();
		return servers == null ? null : servers.findById( name );
	}

	public ServerRepository getServers() {
		createServerRepository();
		return servers;
	}

	public SecureFileRepository<ServiceEndpoint> getServiceEndpointRepository() {
		createServiceEndpointRepository();
		return serviceEndpointRepository;
	}

	@Override
	public Session getSession() {
		try {
			Credential credential = getServer( "broker" ).getCredential();
			AbstractHTTPClient http = createHTTPClient();
			String url = getAPIURL();
			CharBuffer apiKey = CharBuffer.wrap( credential.getPassword() );
			CharBuffer deviceID = CharBuffer.wrap( credential.getUsername() );
			if( http == null || url == null ) {
				throw new NullPointerException();
			}
			return new AuthenticatedSessionClient( http, url, apiKey, deviceID );
		} catch( NullPointerException exception ) {
			return getSharedSession();
		}
	}

	public byte [] getSharedDataFromKeyManagerAsByteArray( String id ) {
		ExternalKeyManager externalKeyManager = getExternalKeyManager();
		return externalKeyManager != null ? externalKeyManager.getSharedKeyData( id ) : null;
	}

	public char [] getSharedDataFromKeyManagerAsCharArray( String id ) {
		byte [] array = getSharedDataFromKeyManagerAsByteArray( id );
		return array == null ? null : CryptoUtils.toCharArraySafe( array );
	}

	public CharSequence getSharedDataFromKeyManagerAsCharSequence( String id ) {
		char [] array = getSharedDataFromKeyManagerAsCharArray( id );
		return array == null ? null : CharBuffer.wrap( array );
	}

	public Session getSharedSession() {
		ExternalKeyManager externalKeyManager = getExternalKeyManager();

		if( externalKeyManager == null ) {
			return null;
		}
		CharSequence apiKey = getSharedDataFromKeyManagerAsCharSequence( externalKeyManager.getDeviceAuthDataTag() );
		CharSequence deviceID = getSharedDataFromKeyManagerAsCharSequence( externalKeyManager.getDeviceUniqueIDTag() );
		CharSequence baseURL = getAPIURL();

		if( baseURL != null && apiKey != null && deviceID != null ) {
			Session session = new AuthenticatedSessionClient( createHTTPClient(), baseURL, apiKey, deviceID );
			save( session );
			return session;
		}

		// save apiKey for directory search.
		if( apiKey != null ) {
			Constants.mApiKey = apiKey.toString();
		}

		return null;

	}

	public ContactRepository getSilentContacts() {
		if( silentContacts == null ) {
			if( SilentContactRepositoryV2.supports( this ) ) {
				silentContacts = new SilentContactRepositoryV2( getContentResolver() );
			} else if( SilentContactRepositoryV1.supports( this ) ) {
				silentContacts = new SilentContactRepositoryV1( getContentResolver() );
			}
		}
		return silentContacts;
	}

	private File getStorageKeyFile( String username ) {
		return new File( getFilesDir(), Hash.sha1( username ) + ".key" );
	}

	public ContactRepository getSystemContacts() {
		if( systemContacts == null ) {
			systemContacts = new SystemContactRepository( getContentResolver() );
		}
		return systemContacts;
	}

	public long getTimeRemainingUntilInactive( int threshold ) {
		if( threshold <= 0 ) {
			return Long.MAX_VALUE;
		}
		long timeOfBecomingInactive = timeOfMostRecentActivity + threshold * 1000L;
		long now = System.currentTimeMillis();
		long timeUntilInactive = timeOfBecomingInactive - now;
		getLog().debug( "#getTimeUntilInactive threshold:%d activity_at:%d inactive_at:%d remaining:%d", Integer.valueOf( threshold ), Long.valueOf( timeOfMostRecentActivity ), Long.valueOf( timeOfBecomingInactive ), Long.valueOf( timeUntilInactive ) );
		return timeUntilInactive < 0 ? 0 : timeUntilInactive;
	}

	private File getTransportQueueDir() {
		File file = getUserTransportQueueDir( getUsername() );
		file.mkdirs();
		return file;
	}

	protected KeyStore getTrustStore() {
		InputStream in = null;
		try {
			KeyStore trustStore = KeyStore.getInstance( "BKS" );
			in = new FileInputStream( getTrustStorePath() );
			trustStore.load( in, TRUST_STORE_PASSWORD.toCharArray() );
			return trustStore;
		} catch( Exception exception ) {
			getLog().error( exception, "#getTrustStore" );
			return null;
		} finally {
			IOUtils.close( in );
		}
	}

	private String getTrustStorePath() {
		File file = new File( getFilesDir(), TRUST_STORE_FILE );
		if( file.exists() && !isIdentical( file, TRUST_STORE_FILE ) ) {
			file.delete();
		}
		if( !file.exists() ) {
			InputStream in = null;
			OutputStream out = null;
			try {
				in = getAssets().open( TRUST_STORE_FILE );
				out = new FileOutputStream( file, false );
				IOUtils.pipe( in, out );
			} catch( IOException exception ) {
				throw new RuntimeException( exception );
			} finally {
				IOUtils.close( in, out );
			}
		}
		return file.getAbsolutePath();
	}

	public User getUser( CharSequence username ) {
		return getUser( username, false );
	}

	public User getUser( CharSequence username, boolean forceUpdate ) {

		if( username == null ) {
			return null;
		}

		if( !forceUpdate ) {
			User user = getUserFromCache( username );

			if( user != null ) {
				return user;
			}

		}

		BasicUser updatedUser = getUserFromServer( username );

		if( updatedUser != null ) {
			try {
				getUsers().save( updatedUser );
			} catch( IllegalStateException exception ) {
				getLog().warn( exception, "#save user:%s", username );
			}
		}

		return updatedUser;

	}

	private File getUserConversationsDir( String username ) {
		return new File( getFilesDir(), Hash.sha1( username + ".conversations" ) );
	}

	private File getUserCredentialsDir( String username ) {
		return new File( getFilesDir(), Hash.sha1( username + ".credentials" ) );
	}

	public File [] getUserDirs( String username ) {
		return new File [] {
			getUserCredentialsDir( username ),
			getUserConversationsDir( username ),
			getUserKeyPairsDir( username ),
			getUserTransportQueueDir( username ),
			getStorageKeyFile( username )
		};
	}

	public User getUserFromCache( CharSequence username ) {
		if( username == null ) {
			return null;
		}

		char [] usernameAsArray = CryptoUtils.copyAsCharArray( username );

		try {
			User user = getUsers().findByID( usernameAsArray );
			if( user instanceof BasicUser ) {
				( (BasicUser) user ).setID( username );
				return user;
			}
		} catch( RuntimeException exception ) {
			getLog().warn( exception, "#findByID user:%s", username );
		}

		return null;
	}

	private BasicUser getUserFromServer( CharSequence username ) {

		User u = null;

		try {

			Session session = getSession();
			if( session != null ) {
				u = session.findUser( username );
			}

		} catch( HTTPClientUnknownResourceException exception ) {
			getLog().info( "#getUserFromServer username:%s %s", username, exception.getLocalizedMessage() );
			return null;
		} catch( HTTPClientForbiddenException exception ) {
			getLog().error( exception, "#getUserFromServer" );
			deactivate();
		} catch( HTTPException exception ) {
			ServiceConfiguration.getInstance().api.removeFromCache( getCachingSRVResolver() );
			getLog().error( exception, "#getUserFromServer" );
		} catch( NetworkException exception ) {
			ServiceConfiguration.getInstance().api.removeFromCache( getCachingSRVResolver() );
			getLog().info( exception, "#getUserFromServer" );
		} catch( RuntimeException exception ) {
			if( NetworkException.isNetworkOnMainThread( exception ) ) {
				getLog().warn( exception, "#getUserFromServer" );
			} else {
				throw exception;
			}
		}

		if( !( u instanceof BasicUser ) ) {
			return null;
		}

		BasicUser user = (BasicUser) u;
		user.setID( username );
		return user;

	}

	private File getUserKeyPairsDir( String username ) {
		return new File( getFilesDir(), Hash.sha1( username, ".key_pairs" ) );
	}

	public UserManager getUserManager() {
		return new UserManagerClient( createHTTPClient(), getAPIURL() );
	}

	public String getUsername() {
		Server server = getServer( "xmpp" );
		Credential credential = server == null ? null : server.getCredential();
		String username = credential == null ? null : credential.getUsername();
		return username;
	}

	public UserPreferences getUserPreferences() {
		return getUserPreferences( getUsername() );
	}

	public UserPreferences getUserPreferences( String username ) {

		if( username == null ) {
			return null;
		}

		createUserPreferencesRepository();

		if( userPreferencesRepository == null ) {
			return null;
		}

		UserPreferences preferences = null;

		preferences = userPreferencesRepository.findByID( username.toCharArray() );

		if( preferences == null ) {
			preferences = createDefaultUserPreferences( username );
			userPreferencesRepository.save( preferences );
		}

		return preferences;

	}

	public SecureFileRepository<User> getUsers() {

		if( users == null ) {

			File baseDirectory = getFilesDir( "users" );

			RepositoryHelper<User> helper = new RepositoryHelper<User>( new Serializer<User>() {

				@Override
				public User read( DataInputStream in ) throws IOException {
					return BasicUser.from( in );
				}

				@Override
				public User read( DataInputStream in, User out ) throws IOException {
					if( out instanceof BasicUser ) {
						( (BasicUser) out ).load( in );
						return out;
					}
					return BasicUser.from( in );
				}

				@Override
				public User write( User in, DataOutputStream out ) throws IOException {
					if( in instanceof BasicUser ) {
						( (BasicUser) in ).save( out );
					}
					return in;
				}

			} ) {

				@Override
				public char [] identify( User user ) {
					return CryptoUtils.toCharArraySafe( CharBuffer.wrap( user.getID() ) );
				}

			};
			BufferedBlockCipherFactory cipherFactory = new AESWithCBCAndPKCS7PaddingCipherFactory();
			MacFactory macFactory = new HMacSHA256Factory();
			users = new SecureFileRepository<User>( baseDirectory, helper, cipherFactory, macFactory );
		}
		if( applicationKey != null ) {
			users.unlock( applicationKey );
		} else {
			users.lock();
		}
		return users;
	}

	private File getUserTransportQueueDir( String username ) {
		return new File( getFilesDir(), Hash.sha1( username + ".queue" ) );
	}

	public List<UUIDEntry> getUUIDs() {
		createUUIDRepository();
		return UUIDRepository.list().toLazierList();
	}

	protected SocketFactory getXMPPSocketFactory() {
		return new XMPPSocketFactory( new TrustStoreCertificateVerifier( getTrustStore() ) );
	}

	public XMPPTransport getXMPPTransport() {
		return xmppTransport;
	}

	public String getXMPPTransportConnectionStatus() {
		return xmppTransportConnectionStatus;
	}

	public boolean hasSharedSession() {
		ExternalKeyManager externalKeyManager = getExternalKeyManager();
		if( externalKeyManager == null ) {
			return false;
		}
		CharSequence apiKey = getSharedDataFromKeyManagerAsCharSequence( externalKeyManager.getDeviceAuthDataTag() );
		CharSequence deviceID = getSharedDataFromKeyManagerAsCharSequence( externalKeyManager.getDeviceUniqueIDTag() );

		return apiKey != null && deviceID != null;
	}

	public boolean hasValidSharedSession() {
		if( isUnlocked() && isUserKeyUnlocked() && isExternalKeyManagerAvailable() && !hasSharedSession() ) {
			return false;
		}

		return true;
	}

	public boolean isApplicationInForeground() {
		return foreground;
	}

	public boolean isExternalKeyManagerAvailable() {
		ExternalKeyManager externalKeyManager = getExternalKeyManager();
		return externalKeyManager != null && externalKeyManager.isRegistered();
	}

	private boolean isIdentical( File file, String assetName ) {
		InputStream a = null;
		InputStream b = null;
		try {
			a = new FileInputStream( file );
			b = getAssets().open( assetName );
			return IOUtils.isIdentical( a, b );
		} catch( IOException exception ) {
			return false;
		} finally {
			IOUtils.close( a, b );
		}
	}

	public boolean isInactive( int threshold ) {
		return getTimeRemainingUntilInactive( threshold ) <= 0;
	}

	public boolean isNetworkConnected() {
		checkNetworkState();
		return connected;
	}

	public boolean isSelf( String username ) {
		String self = getUsername();
		if( username == null ) {
			return self == null;
		}
		return self != null && self.equalsIgnoreCase( username );
	}

	public boolean isUnlocked() {
		return applicationKey != null;
	}

	public boolean isUserEntitledToSilentText() {
		AuthenticatedSessionClient session = (AuthenticatedSessionClient) getSession();
		User user = session.getSelf();
		Set<Entitlement> entitlements = user.getEntitlements();
		return entitlements.contains( Entitlement.SILENT_TEXT );
	}

	public boolean isUserKeyUnlocked() {
		return userKey != null;
	}

	public boolean isXMPPTransportConnected() {
		return connected && getXMPPTransport() != null && getXMPPTransport().isConnected();
	}

	public void lock() {
		lock( true );
	}

	public void lock( boolean logout ) {

		if( logout && isXMPPTransportConnected() ) {
			new Thread( "xmpp-logout" ) {

				@Override
				public void run() {
					XMPPTransport jabber = getXMPPTransport();
					if( jabber != null ) {
						jabber.disconnect();
						setXMPPTransport( (XMPPTransport) null );
					}
				}

			}.start();
		}

		LockApplicationOnReceive.cancel( this );

		CryptoUtils.randomize( userKey );
		userKey = null;

		CryptoUtils.randomize( applicationKey );
		applicationKey = null;

		if( keyPairs != null ) {
			keyPairs.lock();
			keyPairs = null;
		}

		if( serviceEndpointRepository != null ) {
			serviceEndpointRepository.lock();
			serviceEndpointRepository = null;
		}

		if( users != null ) {
			users.lock();
			users = null;
		}

		if( userPreferencesRepository != null ) {
			userPreferencesRepository.lock();
			userPreferencesRepository = null;
		}

		httpResponseCache.clear();

		applicationPreferencesRepository = null;
		broker = null;
		servers = null;
		conversations = null;
		contacts = null;

		System.gc();

	}

	public void logout() {
		XMPPTransport jabber = getXMPPTransport();
		if( jabber != null ) {
			jabber.disconnect();
		}
	}

	private void migrateUserKey() {
		String username = getUsername();
		if( username != null ) {
			EncryptedStorage.saveUserKey( this, applicationKey, username, userKey );
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		try {
			UnixSecureRandom.register();
		} catch( SecurityException exception ) {
			getLog().warn( exception, "UnixSecureRandom#register" );
		}
		ServiceConfiguration.refresh( this );
		sendToBackground();
		startService( new Intent( this, GCMService.class ) );
		startService( new Intent( this, SysNetObserverService.class ) );
		MIGRATIONS.migrate( this );
		createExternalKeyManagerFactory();
	}

	public void onNetworkStateChanged() {
		checkNetworkState();
		timer.schedule( respondToNetworkStateChange(), DELAY_RESPOND_TO_NETWORK_STATE_CHANGE );
	}

	public void onNewPushNotificationToken( final String token ) {
		getLog().debug( "#onNewPushNotificationToken token:%s", token );
		SharedPreferences preferences = getSharedPreferences( "gcm", MODE_PRIVATE );
		preferences.edit().putString( "token", token ).commit();
		new Thread( "push-registration" ) {

			@Override
			public void run() {
				if( !isNetworkConnected() ) {
					return;
				}
				try {
					Session session = getSession();
					if( session != null ) {
						session.startReceivingPushNotifications( getPackageName(), "gcm", ServiceConfiguration.getInstance().gcm.target, token );
					}
				} catch( Throwable exception ) {
					getLog().warn( exception, "#registerPushNotifications" );
				}
			}

		}.start();
	}

	/**
	 * @param message
	 */
	public void onPushNotification() {
		if( isApplicationInForeground() ) {
			getLog().debug( "#onPushNotification app in foreground (ignoring)" );
			return;
		}
		if( !NotificationBroadcaster.isReadyForNextNotification() ) {
			getLog().debug( "#onPushNotification already broadcasting notification (ignoring)" );
			return;
		}
		sendBroadcast( Action.NOTIFY.intent(), Manifest.permission.READ );
	}

	private void onUserKeyUnlocked() {
		Server server = getServer( "xmpp" );
		if( server != null ) {
			Credential credential = server.getCredential();
			if( credential != null ) {
				createXMPPTransportAsync( credential );
				getSCimpBridge().setIdentity( credential.getUsername() );
				getOrCreateKeyPair();
				validateSession();
			}
		}
	}

	private String readAssetAsString( String path ) {
		InputStream in = null;
		try {
			in = getAssets().open( path );
			return IOUtils.readAsString( in );
		} catch( IOException exception ) {
			getLog().warn( exception, "#readAssetAsString path:%s", path );
			return null;
		} finally {
			IOUtils.close( in );
		}
	}

	private String readRawResourceAsString( int rawResourceID ) {
		InputStream in = null;
		try {
			in = getResources().openRawResource( rawResourceID );
			return IOUtils.readAsString( in, UTF16 );
		} finally {
			IOUtils.close( in );
		}
	}

	public void registerKeyManagerIfNecessary() {
		ExternalKeyManager externalKeyManager = getExternalKeyManager();
		if( externalKeyManager != null ) {
			externalKeyManager.register();
		}
	}

	public void removeAttachments( Event event ) {
		if( event instanceof Message ) {
			Siren siren = parseSiren( event.getText() );
			if( siren == null ) {
				return;
			}
			byte [] locator = siren.getCloudLocatorAsByteArray();
			if( locator != null ) {
				getAttachments().removeByID( new String( locator ).toCharArray() );
			}
		}
	}

	private List<NamedKeyPair> removeExpired( LazyList<NamedKeyPair> pairs ) {
		List<NamedKeyPair> remaining = new ArrayList<NamedKeyPair>();
		for( int i = 0; i < pairs.size(); i++ ) {
			NamedKeyPair pair = pairs.get( i );
			if( pair == null ) {
				continue;
			}
			if( isExpired( pair ) ) {
				Session session = getSession();
				if( session != null ) {
					new NamedThread( new RemoveKeyPair( session, keyPairs, pair.locator ) ).start();
				}
			} else {
				remaining.add( pair );
			}
		}
		return remaining;
	}

	public void removeSharedDataFromKeyManager( String id ) {
		ExternalKeyManager externalKeyManager = getExternalKeyManager();
		if( externalKeyManager != null ) {
			externalKeyManager.deleteSharedKeyData( id );
		}
	}

	public void removeSharedSession() {
		ExternalKeyManager externalKeyManager = getExternalKeyManager();
		if( externalKeyManager != null ) {
			removeSharedDataFromKeyManager( externalKeyManager.getDeviceAuthDataTag() );
			removeSharedDataFromKeyManager( externalKeyManager.getDeviceUniqueIDTag() );
		}
	}

	public void requestEncryptionPassPhrase() {
		Intent intent = new Intent( this, UnlockActivity.class );
		intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
		startActivity( intent );
	}

	private void rescheduleAutoDisconnect() {

		cancelAutoDisconnect();

		if( ServiceConfiguration.getInstance().xmpp.background || !GCMService.isAvailable( this ) ) {
			return;
		}

		autoDisconnect = new TimerTask() {

			@Override
			public void run() {
				logout();
			}

		};

		timer.schedule( autoDisconnect, 5 * 1000L );

	}

	private void rescheduleAutoLock() {

		cancelAutoLock();

		int timeout = OptionsDrawer.getInactivityTimeout( this );

		if( timeout > 10 ) {

			autoLock = new TimerTask() {

				@Override
				public void run() {
					lock();
				}

			};

			timer.schedule( autoLock, timeout * 1000L );

		}

	}

	private TimerTask respondToNetworkStateChange() {

		if( respondToNetworkStateChange != null ) {
			respondToNetworkStateChange.cancel();
			respondToNetworkStateChange = null;
		}

		final boolean wasConnected = connected;
		respondToNetworkStateChange = new TimerTask() {

			@Override
			public void run() {
				if( autoDisconnect != null ) {
					return;
				}
				if( wasConnected != connected ) {
					return;
				}
				if( connected ) {
					if( isUnlocked() && isUserKeyUnlocked() ) {
						XMPPTransport client = getXMPPTransport();
						if( client == null ) {
							Server server = getServer( "xmpp" );
							createXMPPTransportAsync( server != null ? server.getCredential() : null );
						}
					} else {
						sendBroadcast( Action.CONNECT.intent(), Manifest.permission.READ );
					}
				}
			}
		};

		return respondToNetworkStateChange;

	}

	public void revokeAllKeyPairs() {
		List<NamedKeyPair> pairs = getKeyPairs();
		if( pairs != null ) {
			AsyncUtils.execute( new RevokeKeyPairTask( getNamedKeyPairs(), this ), pairs.toArray( EMPTY_ARRAY_NAMED_KEY_PAIR ) );
		}
	}

	public void save( Credential credential ) {
		CredentialRepository repository = getCredentials();
		if( repository != null ) {
			repository.save( credential );
		}
	}

	public void save( Event event ) {
		ConversationRepository conversations = getConversations();
		if( conversations != null ) {
			Conversation conversation = conversations.findById( event.getConversationID() );
			if( conversation != null ) {
				EventRepository events = conversations.historyOf( conversation );
				if( events != null ) {
					events.save( event );
				}
			}
		}
	}

	public void save( NamedKeyPair keyPair ) {
		createNamedKeyPairRepository();
		keyPairs.save( keyPair );
		Session session = getSession();
		if( session instanceof AuthenticatedSession ) {
			new NamedThread( new SynchronizePublicKeysWithServer( (AuthenticatedSession) session, keyPairs ) ).start();
		}
	}

	public void save( Server server ) {
		ServerRepository servers = getServers();
		if( servers != null && server != null ) {
			servers.save( server );
		}
	}

	public void save( Session session ) {
		if( session == null ) {
			return;
		}
		Server server = getServer( "broker" );
		if( server == null ) {
			server = new Server( "broker" );
		}
		if( server.getCredential() == null ) {
			server.setCredential( new Credential() );
		}
		server.getCredential().setUsername( session.getID() );
		server.getCredential().setPassword( session.getAccessToken() );
		save( server );
	}

	public void saveApplicationPreferences( UserPreferences preferences ) {
		createApplicationPreferencesRepository();
		if( applicationPreferencesRepository != null ) {
			applicationPreferencesRepository.save( preferences );
		}
	}

	public void saveLicenseCode( CharSequence licenseCode ) {
		createLicenseCodeRepository();
		licenseCodeRepository.save( licenseCode );
	}

	public void saveUserPreferences( UserPreferences preferences ) {
		createUserPreferencesRepository();
		if( userPreferencesRepository != null ) {
			userPreferencesRepository.save( preferences );
		}
	}

	public void sendToBackground() {
		// getLog().debug( "#toBackground" );
		updateMostRecentActivity();
		foreground = false;
		rescheduleAutoDisconnect();
		rescheduleAutoLock();
	}

	public void sendToForeground() {
		// getLog().debug( "#toForeground" );
		foreground = true;
		cancelAutoDisconnect();
		cancelAutoLock();
	}

	public void setAPIKey( String apiKey ) {
		Server server = getServer( "broker" );
		server.getCredential().setPassword( apiKey );
		getServers().save( server );
	}

	public void setEncryptionPassPhrase( char [] encryptionPassPhrase ) {
		char [] hashedUnlockCode = Hash.sha1( encryptionPassPhrase );
		try {
			EncryptedStorage.saveMasterKey( this, hashedUnlockCode, applicationKey );
			getMigrationTasks().remove( Base64SHA1PasscodeMigration.TASK );
		} finally {
			CryptoUtils.randomize( hashedUnlockCode );
		}
	}

	public void setEncryptionPassPhrase( CharSequence encryptionPassPhrase ) {
		char [] buffer = CryptoUtils.copyAsCharArray( encryptionPassPhrase );
		try {
			setEncryptionPassPhrase( buffer );
		} finally {
			CryptoUtils.randomize( buffer );
		}
	}

	public void setXMPPTransport( XMPPTransport jabber ) {

		if( xmppTransport != jabber ) {

			if( xmppTransport != null ) {
				xmppTransport.onDestroy();
			}

			if( jabber != null ) {
				jabber.setLog( new Log( "XMPP" ) );
				jabber.getLog().onCreate();
			} else {
				sendBroadcast( Action.DISCONNECT.intent(), Manifest.permission.READ );
			}

		}

		xmppTransport = jabber;

		if( jabber == null ) {
			return;
		}

		xmppTransport.addListener( new TransportListener() {

			private void broadcastUpdate( Conversation conversation ) {
				broadcastUpdate( conversation.getPartner().getUsername() );
			}

			private void broadcastUpdate( String remoteUserID ) {
				Intent intent = Action.UPDATE_CONVERSATION.intent();
				Extra.PARTNER.to( intent, remoteUserID );
				sendBroadcast( intent, Manifest.permission.READ );
			}

			@Override
			public void onInsecureMessageReceived( Envelope envelope ) {
				getLog().info( "INSECURE MESSAGE [%s] %s: %s", envelope.id, envelope.from, envelope.content );
			}

			@Override
			public void onMessageAcknowledged( Envelope envelope ) {

				Event event = findEventByID( envelope.id );

				if( event instanceof OutgoingMessage ) {
					OutgoingMessage message = (OutgoingMessage) event;
					if( MessageState.SENT.equals( message.getState() ) ) {
						message.setState( MessageState.ACKNOWLEDGED );
						save( message );
						broadcastUpdate( event.getConversationID() );
					}
				}

			}

			@Override
			public void onMessageReturned( Envelope envelope, String reason ) {

				int index = envelope.to.indexOf( '/' );
				String recipientWithoutResource = index >= 0 ? envelope.to.substring( 0, index ) : envelope.to;
				ConversationRepository conversations = getConversations();
				Conversation conversation = conversations.findByPartner( recipientWithoutResource );

				if( conversation == null ) {
					return;
				}

				EventRepository events = conversations.historyOf( conversation );

				Event returnedEvent = events.findById( envelope.id );
				if( returnedEvent instanceof OutgoingMessage ) {
					Message message = (OutgoingMessage) returnedEvent;
					if( MessageState.SENT.equals( message.getState() ) ) {
						message.setState( MessageState.ENCRYPTED );
						events.save( message );
						enqueue( message );
					}
				}

				ErrorEvent event = new ErrorEvent();
				event.setConversationID( conversation.getPartner().getUsername() );
				event.setText( reason );
				events.save( event );

				broadcastUpdate( conversation );

			}

			@Override
			public void onMessageSent( Envelope envelope ) {

				ConversationRepository _conversations = getConversations();
				Conversation conversation = _conversations.findByPartner( envelope.to );

				if( conversation == null ) {
					return;
				}

				EventRepository events = _conversations.historyOf( conversation );
				Event event = events.findById( envelope.id );

				if( event instanceof Message ) {

					Message message = (Message) event;

					if( isOutgoingResendRequest( message ) ) {
						events.remove( message );
						return;
					}

					message.setState( MessageState.SENT );
					message.setTime( System.currentTimeMillis() );
					if( message.expires() ) {
						message.setExpirationTime( System.currentTimeMillis() + message.getBurnNotice() * 1000L );
					}

					events.save( message );
					broadcastUpdate( conversation );

				}

			}

			@Override
			public void onSecureMessageReceived( Envelope envelope ) {

				int index = envelope.from.indexOf( '/' );
				String partner = index >= 0 ? envelope.from.substring( 0, index ) : envelope.from;
				String resourceName = index >= 0 ? envelope.from.substring( index + 1 ) : null;

				ConversationRepository conversations = getConversations();
				Conversation conversation = getOrCreateConversation( partner );

				if( resourceName != null && !resourceName.equals( conversation.getPartner().getDevice() ) ) {
					conversation.getPartner().setDevice( resourceName );
					conversations.save( conversation );
				}

				EventRepository events = conversations.historyOf( conversation );
				if( events != null ) {
					if( !events.exists( envelope.id ) ) {
						IncomingMessage message = new IncomingMessage( conversation.getPartner().getUsername(), envelope.id, envelope.content );
						message.setState( MessageState.RECEIVED );
						message.setTime( System.currentTimeMillis() );
						events.save( message );
					}
				}

				// EA: At present, the Envelope does not provide any information about public-key
				// encrypted data such as voicemails. Thus, the only way for us to deal with
				// voicemail at present is by matching the "from" username. This is an ugly
				// solution, and it'd be better to pass a flag on the Envelope
				if( envelope.from.equalsIgnoreCase( HARDCODED_VOICEMAIL_ID ) ) {
					getSCimpBridge().decryptPublicKey( conversation.getPartner().getUsername(), envelope.id, envelope.content, envelope.notifiable, envelope.badgeworthy );
				} else {
					getSCimpBridge().decrypt( conversation.getPartner().getUsername(), envelope.id, envelope.content, envelope.notifiable, envelope.badgeworthy );
				}
			}
		} );

		sendBroadcast( Action.CONNECT.intent(), Manifest.permission.READ );

	}

	public void setXMPPTransportCredential( Credential credential ) {
		Server server = getOrCreateServer( "xmpp" );
		if( server.getCredential() == null ) {
			server.setCredential( credential );
		} else {
			server.getCredential().setUsername( credential.getUsername() );
			server.getCredential().setPassword( credential.getPassword() );
		}
		conversations = null;
		getServers().save( server );
		unlockUserKeyAndMigrate();
	}

	public void shareSession() {
		Session session = getSession();
		if( session instanceof AuthenticatedSession ) {
			shareSession( (AuthenticatedSession) session );
		}
	}

	public void shareSession( AuthenticatedSession session ) {
		if( session != null ) {
			ExternalKeyManager externalKeyManager = getExternalKeyManager();
			if( externalKeyManager != null ) {
				externalKeyManager.storeSharedKeyData( externalKeyManager.getDeviceAuthDataTag(), CryptoUtils.toByteArraySafe( (CharBuffer) session.getAccessToken() ) );
				externalKeyManager.storeSharedKeyData( externalKeyManager.getDeviceUniqueIDTag(), CryptoUtils.toByteArraySafe( (CharBuffer) session.getID() ) );
			}
		}
	}

	protected void transition( String remoteUserID, String packetID ) {
		Intent intent = Action.TRANSITION.intent();
		Extra.PARTNER.to( intent, remoteUserID );
		Extra.ID.to( intent, packetID );
		sendBroadcast( intent, Manifest.permission.READ );
	}

	public void unlock( char [] encryptionPassPhrase ) {
		char [] hashedUnlockCode = Hash.sha1( encryptionPassPhrase );
		try {
			unlockWithHashedPassPhrase( hashedUnlockCode );
		} catch( Throwable exception ) {
			if( getMigrationTasks().exists( Base64SHA1PasscodeMigration.TASK.id ) ) {
				unlockLegacy( encryptionPassPhrase );
			}
		} finally {
			CryptoUtils.randomize( hashedUnlockCode );
		}
	}

	public void unlock( CharSequence encryptionPassPhrase ) {
		char [] buffer = CryptoUtils.toCharArray( encryptionPassPhrase );
		try {
			unlock( buffer );
		} finally {
			CryptoUtils.randomize( buffer );
		}
	}

	@Deprecated
	private void unlockLegacy( char [] encryptionPassPhrase ) {

		char [] encryptionPassPhraseHash = Hash.sha1Legacy( encryptionPassPhrase );

		try {
			unlockWithHashedPassPhrase( encryptionPassPhraseHash );
			setEncryptionPassPhrase( encryptionPassPhrase );
			unlock( encryptionPassPhrase );
		} finally {
			CryptoUtils.randomize( encryptionPassPhraseHash );
		}

	}

	private void unlockUserKey() {
		userKey = EncryptedStorage.loadUserKey( this, applicationKey, getUsername() );
	}

	private void unlockUserKeyAndMigrate() {
		if( getMigrationTasks().exists( OneShotPBKDF2Migration.TASK.id ) ) {
			unlockUserKeyLegacy();
			migrateUserKey();
			getMigrationTasks().remove( OneShotPBKDF2Migration.TASK );
		} else {
			unlockUserKey();
		}
		onUserKeyUnlocked();
	}

	protected void unlockUserKeyLegacy() {

		Server server = getServer( "xmpp" );

		if( server == null ) {
			return;
		}

		Credential credential = server.getCredential();

		if( credential == null ) {
			return;
		}

		String username = credential.getUsername();

		if( username == null ) {
			return;
		}

		char [] passcode = getEncryptionPassPhraseForUserKey( credential );

		File file = getStorageKeyFile( username );

		if( file.exists() && file.length() > 0 ) {
			InputStream in = null;
			try {
				in = new FileInputStream( file );
				StorageKeySpec storageKey = new StorageKeySpec( in, passcode );
				if( storageKey.key == null ) {
					Log.d( TAG, "storageKey.key is null" );
				}
				userKey = storageKey.key.getEncoded();
			} catch( IOException exception ) {
				Log.e( TAG, "Exception: " + exception.getMessage() );
				throw new RuntimeException( exception );
			} finally {
				IOUtils.close( in );
			}
		}

		CryptoUtils.randomize( passcode );

	}

	protected void unlockWithHashedPassPhrase( char [] passcode ) {
		applicationKey = EncryptedStorage.loadMasterKey( this, passcode );
		if( applicationKey == null ) {
			throw new IllegalArgumentException( "Invalid passcode" );
		}
		ExternalKeyManager externalKeyManager = getExternalKeyManager();
		if( externalKeyManager != null ) {
			externalKeyManager.storePrivateKeyData( Extra.PASSWORD.getName(), CryptoUtils.toByteArraySafe( passcode ) );
		}
		unlockUserKeyAndMigrate();
	}

	public void updateMostRecentActivity() {
		timeOfMostRecentActivity = System.currentTimeMillis();
		getLog().debug( "#updateMostRecentActivity resetTime:%d", Long.valueOf( timeOfMostRecentActivity ) );
	}

	public boolean userSupportsPKI( String username ) {
		if( !ServiceConfiguration.getInstance().scimp.enablePKI ) {
			return false;
		}
		byte [] privateKey = getPrivateKey();
		if( privateKey != null ) {
			byte [] publicKey = getPublicKey( username );
			if( publicKey != null ) {
				return true;
			}
		}
		return false;
	}

	public void validateAnySession() {

		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground( Void... params ) {

				if( hasSharedSession() && hasValidSharedSession() ) {
					getLog().info( "Valid shared session." );
				} else if( hasSharedSession() ) {
					validateSharedSession();
				} else {
					validateSession();
				}

				return null;
			}
		}.execute();

	}

	public void validateSession() {

		AuthenticatedSessionClient session = (AuthenticatedSessionClient) getSession();

		if( session != null ) {

			new NamedThread( new SynchronizePublicKeysWithServer( session, getNamedKeyPairs() ) {

				@Override
				protected void onException( Throwable exception ) {
					if( exception instanceof HTTPClientUnauthorizedException || exception instanceof HTTPClientForbiddenException ) {
						JSONObject exceptionJSON = null;

						try {
							exceptionJSON = new JSONObject( ( (HTTPException) exception ).getBody() );
						} catch( JSONException e ) {
							return;
						}

						try {
							if( exceptionJSON.has( "error_code" ) && exceptionJSON.getInt( "error_code" ) == -2 ) {
								// Ignore backwards compatibility of retaining past device sessions
								// (i.e., the newest device should always have priority).
							} else {
								onSessionInvalid();
							}
						} catch( JSONException e ) {
							return;
						}
					}
				}

				@Override
				protected void onSessionInvalid() {
					super.onSessionInvalid();
					deactivate();
				}

			} ).start();

		}

	}

	public void validateSharedSession() {

		if( isUnlocked() && isUserKeyUnlocked() && isExternalKeyManagerAvailable() && !hasSharedSession() ) {

			new Thread( new Deactivation( this ), "deactivation" ).start();

		}

	}

}
