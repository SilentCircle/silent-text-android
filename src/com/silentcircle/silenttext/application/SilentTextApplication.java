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
package com.silentcircle.silenttext.application;

import java.io.ByteArrayInputStream;
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
import java.security.Key;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;

import com.silentcircle.api.AuthenticatedSession;
import com.silentcircle.api.Session;
import com.silentcircle.api.model.User;
import com.silentcircle.keymngrsupport.KeyManagerSupport;
import com.silentcircle.keymngrsupport.KeyManagerSupport.KeyManagerListener;
import com.silentcircle.scimp.KeyGenerator;
import com.silentcircle.scimp.NamedKeyPair;
import com.silentcircle.scimp.NamedKeyPairRepositoryHelper;
import com.silentcircle.scimp.NativeKeyGenerator;
import com.silentcircle.silentstorage.UnixSecureRandom;
import com.silentcircle.silentstorage.repository.file.SecureFileRepository;
import com.silentcircle.silenttext.Action;
import com.silentcircle.silenttext.Extra;
import com.silentcircle.silenttext.Manifest;
import com.silentcircle.silenttext.NativeBridge;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.ServiceConfiguration;
import com.silentcircle.silenttext.activity.UnlockActivity;
import com.silentcircle.silenttext.client.AccountCreationClient;
import com.silentcircle.silenttext.client.AuthenticatedClientSession;
import com.silentcircle.silenttext.client.JabberClient;
import com.silentcircle.silenttext.client.JabberClient.OnDisconnectedListener;
import com.silentcircle.silenttext.client.SCloudBroker;
import com.silentcircle.silenttext.client.SimpleHTTPClient;
import com.silentcircle.silenttext.client.model.SensitiveKey;
import com.silentcircle.silenttext.client.model.SensitiveUser;
import com.silentcircle.silenttext.client.model.ServiceEndpoint;
import com.silentcircle.silenttext.client.model.json.JSONSensitiveKeySerializer;
import com.silentcircle.silenttext.client.model.repository.helper.ServiceEndpointHelper;
import com.silentcircle.silenttext.client.model.repository.helper.UserHelper;
import com.silentcircle.silenttext.crypto.CryptoUtils;
import com.silentcircle.silenttext.crypto.EphemeralKeySpec;
import com.silentcircle.silenttext.crypto.Hash;
import com.silentcircle.silenttext.crypto.StorageKeySpec;
import com.silentcircle.silenttext.listener.SilentLocationListener;
import com.silentcircle.silenttext.listener.TransportListener;
import com.silentcircle.silenttext.log.Log;
import com.silentcircle.silenttext.model.Contact;
import com.silentcircle.silenttext.model.Conversation;
import com.silentcircle.silenttext.model.Credential;
import com.silentcircle.silenttext.model.MessageState;
import com.silentcircle.silenttext.model.Server;
import com.silentcircle.silenttext.model.event.ErrorEvent;
import com.silentcircle.silenttext.model.event.Event;
import com.silentcircle.silenttext.model.event.Message;
import com.silentcircle.silenttext.model.event.OutgoingMessage;
import com.silentcircle.silenttext.model.event.ResourceChangeEvent;
import com.silentcircle.silenttext.receiver.LockApplicationOnReceive;
import com.silentcircle.silenttext.receiver.NotificationBroadcaster;
import com.silentcircle.silenttext.repository.ContactRepository;
import com.silentcircle.silenttext.repository.ConversationRepository;
import com.silentcircle.silenttext.repository.CredentialRepository;
import com.silentcircle.silenttext.repository.EventRepository;
import com.silentcircle.silenttext.repository.ServerRepository;
import com.silentcircle.silenttext.repository.file.encrypted.EncryptedFileConversationRepository;
import com.silentcircle.silenttext.repository.file.encrypted.EncryptedFileCredentialRepository;
import com.silentcircle.silenttext.repository.file.encrypted.EncryptedFileServerRepository;
import com.silentcircle.silenttext.repository.file.encrypted.EncryptedFileTransportQueue;
import com.silentcircle.silenttext.repository.resolver.AndroidContactRepository;
import com.silentcircle.silenttext.repository.resolver.SilentContactRepository;
import com.silentcircle.silenttext.service.GCMService;
import com.silentcircle.silenttext.thread.NewJabberClient;
import com.silentcircle.silenttext.thread.RemoveKeyPair;
import com.silentcircle.silenttext.thread.SessionValidator;
import com.silentcircle.silenttext.thread.UploadPublicKey;
import com.silentcircle.silenttext.transport.TransportQueue;
import com.silentcircle.silenttext.util.IOUtils;
import com.silentcircle.silenttext.util.StringUtils;
import com.silentcircle.silenttext.view.OptionsDrawer;

public class SilentTextApplication extends Application {

	private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
	private static final String KEY_ALGORITHM = "AES";
	private static final String APPLICATION_KEY_ALIAS = "___DEFAULT___";
	private static final String TRUST_STORE_FILE = "silentcircle.bks.keystore";
	private static final String TRUST_STORE_PASSWORD = "silence";
	private static final long XMPP_PING_INTERVAL = 60000;
	private static final String ACCOUNT_CREATION_CLIENT_BASE_URL = "https://sccps.silentcircle.com";

	private static final long DELAY_RESPOND_TO_NETWORK_STATE_CHANGE = 5000;

	protected static com.silentcircle.api.model.Key extractPublicKey( NamedKeyPair keyPair ) {
		if( keyPair == null || keyPair.getPublicKey() == null || keyPair.getPublicKey().length <= 0 ) {
			return null;
		}
		try {
			ByteArrayInputStream in = new ByteArrayInputStream( keyPair.getPublicKey() );
			DataInputStream inData = new DataInputStream( in );
			return JSON_KEY_SERIALIZER.read( inData );
		} catch( IOException exception ) {
			throw new RuntimeException( exception );
		}
	}

	public static SilentTextApplication from( Context context ) {
		return context == null ? null : (SilentTextApplication) context.getApplicationContext();
	}

	private static String getDomain() {
		return ServiceConfiguration.getInstance().xmpp.serviceName;
	}

	private static boolean isExpired( NamedKeyPair keyPair ) {
		try {
			SensitiveKey key = parsePublicKey( keyPair.getPublicKey() );
			return isPublicKeyExpired( key );
		} catch( IOException exception ) {
			return true;
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
			return message instanceof OutgoingMessage && new JSONObject( message.getText() ).has( "request_resend" );
		} catch( JSONException exception ) {
			return false;
		}
	}

	public static boolean isRunningInDalvikVM() {
		return "Dalvik".equals( System.getProperty( "java.vm.name" ) );
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

	private final SilentLocationListener locationListener = new SilentLocationListener();
	private final Map<String, StorageKeySpec> storageKeys = new HashMap<String, StorageKeySpec>();
	protected boolean connected;
	protected NativeBridge _native;
	protected JabberClient jabber;
	protected SCloudBroker broker;
	protected ConversationRepository conversations;
	protected ServerRepository servers;
	protected CredentialRepository credentials;
	protected ContactRepository contacts;
	protected TransportQueue outgoingMessageQueue;
	protected Key userKey;
	protected Key applicationKey;
	private Log log;
	private long timeOfMostRecentActivity;
	protected String onlineStatus;
	private Thread xmppConnectionThread;
	private final Timer timer = new Timer();
	private TimerTask autoLock;
	private String license;
	private String privacyPolicy;
	private long keyManagerToken;

	private TimerTask respondToNetworkStateChange;

	protected TimerTask autoDisconnect;

	private AccountCreationClient accountCreationClient;

	private SecureFileRepository<SensitiveUser> users;

	private SecureFileRepository<NamedKeyPair> keyPairs;

	private KeyGenerator keyGenerator;
	private SecureFileRepository<ServiceEndpoint> networks;

	private void createNetworks() {
		if( networks == null ) {
			networks = new SecureFileRepository<ServiceEndpoint>( getFilesDir( "networks" ), new ServiceEndpointHelper(), CIPHER_ALGORITHM, KEY_ALGORITHM );
		}
		if( applicationKey == null ) {
			networks.lock();
		} else {
			networks.unlock( applicationKey.getEncoded() );
		}
	}

	protected File getFilesDir( String... names ) {
		return new File( getFilesDir(), Hash.sha1( names ) );
	}

	protected File getCacheDir( String... names ) {
		return new File( getCacheDir(), Hash.sha1( names ) );
	}

	public SecureFileRepository<ServiceEndpoint> getNetworks() {
		createNetworks();
		return networks;
	}

	private static final JSONSensitiveKeySerializer JSON_KEY_SERIALIZER = new JSONSensitiveKeySerializer();

	private static String getLocator( byte [] publicKey ) {
		if( publicKey == null || publicKey.length <= 0 ) {
			return null;
		}
		try {
			return parsePublicKey( publicKey ).getLocator().toString();
		} catch( IOException exception ) {
			throw new RuntimeException( exception );
		}
	}

	private static SensitiveKey parsePublicKey( byte [] publicKey ) throws IOException {
		return JSON_KEY_SERIALIZER.read( new DataInputStream( new ByteArrayInputStream( publicKey ) ) );
	}

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
			server.setURL( null );
			getServers().save( server );
		}
		setJabberServer( server );
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

	private void createBroker() {
		if( broker != null ) {
			return;
		}
		Server server = getServer( "broker" );
		if( server == null ) {
			server = new Server( "broker" );
			Credential credential = new Credential();
			credential.setDomain( getDomain() );
			server.setCredential( credential );
			getServers().save( server );
		}
		if( !getDomain().equals( server.getCredential().getDomain() ) ) {
			server.getCredential().setDomain( getDomain() );
			getServers().save( server );
		}
		broker = new SCloudBroker( server.getCredential(), createHTTPClient(), getNetworks() );
	}

	private void createContactRepository() {
		if( contacts != null ) {
			return;
		}
		if( SilentContactRepository.supports( this ) ) {
			contacts = new SilentContactRepository( getContentResolver() );
		} else {
			contacts = new AndroidContactRepository( getContentResolver() );
		}
	}

	private void createConversationRepository() {
		if( conversations != null || userKey == null ) {
			return;
		}
		String username = getServer( "xmpp" ).getCredential().getUsername();
		File file = new File( getFilesDir(), Hash.sha1( username + ".conversations" ) );
		conversations = new EncryptedFileConversationRepository( userKey, file, getCacheDir() );
	}

	private void createCredentialRepository() {
		if( credentials != null || userKey == null ) {
			return;
		}
		String username = getServer( "xmpp" ).getCredential().getUsername();
		File file = new File( getFilesDir(), Hash.sha1( username + ".credentials" ) );
		credentials = new EncryptedFileCredentialRepository( userKey, file );
	}

	private SimpleHTTPClient createHTTPClient() {
		if( ServiceConfiguration.getInstance().shouldValidateCertificates ) {
			return new SimpleHTTPClient( getTrustStore() );
		}
		return new SimpleHTTPClient();
	}

	public String getAPIURL() {
		return ServiceConfiguration.getInstance().api.getURL( getNetworks() );
	}

	public String getXMPPURL() {
		return ServiceConfiguration.getInstance().xmpp.getURL( getNetworks() );
	}

	public boolean hasSharedSession() {
		CharSequence apiKey = getSharedDataFromKeyManagerAsCharSequence( KeyManagerSupport.DEV_AUTH_DATA_TAG );
		CharSequence deviceID = getSharedDataFromKeyManagerAsCharSequence( KeyManagerSupport.DEV_UNIQUE_ID_TAG );
		return apiKey != null && deviceID != null;
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

		save( keyPair );

		return keyPair;

	}

	private void createLog() {
		if( log != null ) {
			return;
		}
		log = new Log( getClass().getSimpleName() );
	}

	private void createNamedKeyPairRepository() {
		if( keyPairs == null ) {
			keyPairs = new SecureFileRepository<NamedKeyPair>( new File( getFilesDir(), Hash.sha1( getUsername(), ".key_pairs" ) ), new NamedKeyPairRepositoryHelper(), CIPHER_ALGORITHM, KEY_ALGORITHM );
		}
		if( userKey == null ) {
			keyPairs.lock();
		} else {
			keyPairs.unlock( userKey.getEncoded() );
		}
	}

	private void createOutgoingMessageQueue() {
		if( outgoingMessageQueue != null || userKey == null ) {
			return;
		}
		outgoingMessageQueue = new EncryptedFileTransportQueue( userKey, getTransportQueueDir() );
	}

	private void createServerRepository() {
		if( servers != null || applicationKey == null ) {
			return;
		}
		File file = new File( getFilesDir(), Hash.sha1( "servers" ) );
		servers = new EncryptedFileServerRepository( applicationKey, file );
	}

	public void deactivate() {

		removeSharedSession();
		GCMService.unregister( this );
		getNative().setIdentity( null );
		getNative().onDestroy();
		_native = null;
		NotificationBroadcaster.cancel( this );

		try {
			Session session = getSession();
			if( session != null ) {
				if( session.getID() != null ) {
					try {
						session.unregisterPushNotifications( "silent_text" );
						session.logout();
					} catch( Throwable exception ) {
						getLog().warn( exception, "#deactivate" );
					}
				}
			}
			getServers().remove( getServer( "broker" ) );
			if( isOnline() ) {
				JabberClient jabber = getJabber();
				if( jabber != null ) {
					jabber.logout();
					setJabber( (JabberClient) null );
				}
			}
			Server xmppServer = getServer( "xmpp" );
			if( xmppServer != null ) {
				save( xmppServer.getCredential() );
				getServers().remove( xmppServer );
			}
		} finally {
			lock( false );
		}

	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		getNative().onDestroy();
	}

	public AccountCreationClient getAccountCreationClient() {
		if( accountCreationClient == null ) {
			accountCreationClient = new AccountCreationClient( this, createHTTPClient(), ACCOUNT_CREATION_CLIENT_BASE_URL );
		}
		return accountCreationClient;
	}

	public SCloudBroker getBroker() {
		createBroker();
		return broker;
	}

	private int getCalibratedIterationCount( int seconds, int minimumIterations, int maximumIterations ) {

		long then = SystemClock.elapsedRealtime();
		long now = then;
		long threshold = seconds * 200;
		int iterations = 0;

		for( long elapsed = 0; elapsed < threshold; elapsed = now - then ) {
			for( int i = 0; i < 5000; i++ ) {
				Math.sqrt( 0.123456789 );
			}
			now = SystemClock.elapsedRealtime();
			iterations++;
		}

		iterations *= 2;

		getLog().debug( "#getCalibratedIterationCount iterations:%d", Integer.valueOf( iterations ) );

		return Math.min( maximumIterations, Math.max( minimumIterations, iterations ) );

	}

	public ContactRepository getContacts() {
		createContactRepository();
		return contacts;
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

	public JabberClient getJabber() {
		return jabber;
	}

	/**
	 * @param alias
	 * @param passcode
	 * @return
	 */
	private Key getKey( String alias, char [] passcode ) {

		File file = new File( getFilesDir(), Hash.sha1( alias ) + ".key" );

		if( file.exists() ) {
			InputStream in = null;
			try {
				in = new FileInputStream( file );
				StorageKeySpec storageKey = new StorageKeySpec( in, passcode );
				storageKeys.put( alias, storageKey );
				return storageKey.key;
			} catch( IOException exception ) {
				throw new RuntimeException( exception );
			} finally {
				IOUtils.close( in );
			}
		}

		EphemeralKeySpec ephemeralKey = new EphemeralKeySpec( KEY_ALGORITHM, "PBKDF2WithHmacSHA1", CIPHER_ALGORITHM, getCalibratedIterationCount( 2, 1024, 10240 ), 256, 16, 16 );
		try {
			StorageKeySpec storageKey = new StorageKeySpec( passcode, KEY_ALGORITHM, CIPHER_ALGORITHM, ephemeralKey );
			OutputStream out = null;
			try {
				out = new FileOutputStream( file, false );
				storageKey.write( out );
				storageKeys.put( alias, storageKey );
				return storageKey.key;
			} catch( IOException exception ) {
				throw new RuntimeException( exception );
			} finally {
				IOUtils.close( out );
			}
		} catch( NoSuchAlgorithmException exception ) {
			throw new RuntimeException( exception );
		}

	}

	public KeyGenerator getKeyGenerator() {
		if( keyGenerator == null ) {
			getNative();
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

	public String getLocalResourceName() {
		Credential credential = getCredential( getUsername() );
		if( credential == null ) {
			Server server = getServer( "xmpp" );
			if( server == null ) {
				return JabberClient.getDefaultResourceName();
			}
			credential = server.getCredential();
			if( credential == null ) {
				return JabberClient.getDefaultResourceName();
			}
		}
		String resource = credential.getResource();
		if( resource == null ) {
			return JabberClient.getDefaultResourceName();
		}
		return resource;
	}

	public byte [] getLocalStorageKey() {
		return userKey == null ? null : CryptoUtils.copyOf( userKey.getEncoded(), 32 );
	}

	public Location getLocation() {
		return locationListener.getLocation();
	}

	protected Log getLog() {
		createLog();
		return log;
	}

	public NativeBridge getNative() {
		if( _native == null ) {
			_native = new NativeBridge( this );
			_native.onCreate();
		}
		return _native;
	}

	public String getOnlineStatus() {
		return onlineStatus;
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

	public TransportQueue getOutgoingMessageQueue() {
		createOutgoingMessageQueue();
		return outgoingMessageQueue;
	}

	private char [] getPasscodeForUserKey( Credential credential ) {
		if( credential == null ) {
			return null;
		}
		return getPasscodeForUserKey( credential.getPassword() );
	}

	protected char [] getPasscodeForUserKey( Server server ) {
		if( server == null ) {
			return null;
		}
		return getPasscodeForUserKey( server.getCredential() );
	}

	private char [] getPasscodeForUserKey( String password ) {
		if( password == null ) {
			return null;
		}
		char [] applicationKeyChars = CryptoUtils.toCharArray( applicationKey.getEncoded() );
		char [] passcode = new char [applicationKeyChars.length + password.length()];
		for( int i = 0; i < applicationKeyChars.length; i++ ) {
			passcode[i] = applicationKeyChars[i];
		}
		password.getChars( 0, password.length(), passcode, applicationKeyChars.length );
		CryptoUtils.randomize( applicationKeyChars );
		return passcode;
	}

	public String getPrivacyPolicy() {
		if( privacyPolicy == null ) {
			privacyPolicy = readAssetAsString( "PRIVACY" );
		}
		return privacyPolicy;
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

		if( !( key instanceof SensitiveKey ) ) {
			return null;
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try {
			JSON_KEY_SERIALIZER.write( (SensitiveKey) key, new DataOutputStream( out ) );
			getNative().testSCKeyDeserialize( out.toString() );
			return out.toByteArray();
		} catch( IOException exception ) {
			return null;
		}

	}

	public String getPushNotificationToken() {
		return getSharedPreferences( "gcm", MODE_PRIVATE ).getString( "token", null );
	}

	public Server getServer( String name ) {
		ServerRepository servers = getServers();
		return servers == null ? null : servers.findById( name );
	}

	public ServerRepository getServers() {
		createServerRepository();
		return servers;
	}

	public Session getSession() {
		Server server = getServer( "broker" );
		if( server == null ) {
			return null;
		}
		Credential credential = server.getCredential();
		if( credential == null ) {
			return null;
		}
		return new AuthenticatedClientSession( createHTTPClient(), getAPIURL(), credential.getPassword(), credential.getUsername() );
	}

	public byte [] getSharedDataFromKeyManagerAsByteArray( String id ) {
		return isKeyManagerSupported() ? KeyManagerSupport.getSharedKeyData( getContentResolver(), id ) : null;
	}

	public char [] getSharedDataFromKeyManagerAsCharArray( String id ) {
		byte [] array = getSharedDataFromKeyManagerAsByteArray( id );
		return array == null ? null : CryptoUtils.toCharArray( array, true );
	}

	public CharSequence getSharedDataFromKeyManagerAsCharSequence( String id ) {
		char [] array = getSharedDataFromKeyManagerAsCharArray( id );
		return array == null ? null : CharBuffer.wrap( array );
	}

	public Session getSharedSession() {
		CharSequence apiKey = getSharedDataFromKeyManagerAsCharSequence( KeyManagerSupport.DEV_AUTH_DATA_TAG );
		CharSequence deviceID = getSharedDataFromKeyManagerAsCharSequence( KeyManagerSupport.DEV_UNIQUE_ID_TAG );
		return apiKey != null && deviceID != null ? new AuthenticatedClientSession( createHTTPClient(), getAPIURL(), apiKey, deviceID ) : null;
	}

	public long getTimeUntilInactive( int threshold ) {
		if( threshold < 0 ) {
			return Long.MAX_VALUE;
		}
		long timeOfBecomingInactive = timeOfMostRecentActivity + threshold * 1000L;
		long now = System.currentTimeMillis();
		long timeUntilInactive = timeOfBecomingInactive - now;
		getLog().debug( "#getTimeUntilInactive threshold:%d activity_at:%d inactive_at:%d remaining:%d", Integer.valueOf( threshold ), Long.valueOf( timeOfMostRecentActivity ), Long.valueOf( timeOfBecomingInactive ), Long.valueOf( timeUntilInactive ) );
		return timeUntilInactive < 0 ? 0 : timeUntilInactive;
	}

	private File getTransportQueueDir() {
		File file = new File( getFilesDir(), Hash.sha1( getServer( "xmpp" ).getCredential().getUsername() + ".queue" ) );
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

		if( username == null ) {
			return null;
		}

		try {
			User user = getUsers().findByID( CryptoUtils.copyAsCharArray( username ) );
			if( user instanceof SensitiveUser ) {
				( (SensitiveUser) user ).setID( username );
				return user;
			}
		} catch( IllegalStateException exception ) {
			getLog().warn( exception, "#findByID user:%s", username );
		}

		Session session = getSession();

		if( session == null ) {
			return null;
		}

		User u = session.getUser( username );

		if( !( u instanceof SensitiveUser ) ) {
			return null;
		}

		SensitiveUser user = (SensitiveUser) u;

		user.setID( username );

		try {
			getUsers().save( user );
		} catch( IllegalStateException exception ) {
			getLog().warn( exception, "#save user:%s", username );
		}

		return user;

	}

	public String getUsername() {
		Server server = getServer( "xmpp" );
		Credential credential = server == null ? null : server.getCredential();
		String username = credential == null ? null : credential.getUsername();
		return username;
	}

	public SecureFileRepository<SensitiveUser> getUsers() {
		if( users == null ) {
			users = new SecureFileRepository<SensitiveUser>( new File( getFilesDir(), Hash.sha1( "users" ) ), new UserHelper(), CIPHER_ALGORITHM, KEY_ALGORITHM );
		}
		if( applicationKey != null ) {
			users.unlock( applicationKey.getEncoded() );
		} else {
			users.lock();
		}
		return users;
	}

	public String getVersion() {
		try {
			return getPackageManager().getPackageInfo( getPackageName(), 0 ).versionName;
		} catch( NameNotFoundException exception ) {
			return getString( R.string.unknown );
		}
	}

	public boolean isActivated() {
		return userKey != null;
	}

	public boolean isConnected() {
		checkNetworkState();
		return connected;
	}

	public boolean isDebugModeEnabled() {
		return 0 != ( getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE );
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
		return getTimeUntilInactive( threshold ) <= 0;
	}

	public boolean isKeyManagerSupported() {
		return KeyManagerSupport.hasKeyManager( getPackageManager() ) && KeyManagerSupport.signaturesMatch( getPackageManager(), getPackageName() );
	}

	public boolean isListeningForLocationUpdates() {
		return locationListener.isRegistered();
	}

	public boolean isOnline() {
		return connected && getJabber() != null && getJabber().isOnline();
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

	public void lock() {
		lock( true );
	}

	public void lock( boolean logout ) {

		if( logout && isOnline() ) {
			new Thread() {

				@Override
				public void run() {
					JabberClient jabber = getJabber();
					if( jabber != null ) {
						jabber.logout();
						setJabber( (JabberClient) null );
					}
				}

			}.start();
		}

		LockApplicationOnReceive.cancel( this );

		storageKeys.clear();
		userKey = null;
		applicationKey = null;

		if( keyPairs != null ) {
			keyPairs.lock();
			keyPairs = null;
		}

		if( networks != null ) {
			networks.lock();
			networks = null;
		}

		if( users != null ) {
			users.lock();
			users = null;
		}

		broker = null;
		servers = null;
		conversations = null;
		contacts = null;

		System.gc();

	}

	public void logout() {
		JabberClient jabber = getJabber();
		if( jabber != null ) {
			jabber.logout();
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
		ping();
		startService( new Intent( this, GCMService.class ) );
	}

	public void onNetworkStateChanged() {
		checkNetworkState();
		timer.schedule( respondToNetworkStateChange(), DELAY_RESPOND_TO_NETWORK_STATE_CHANGE );
	}

	public void onNewPushNotificationToken( final String token ) {
		getLog().debug( "#onNewPushNotificationToken token:%s", token );
		SharedPreferences preferences = getSharedPreferences( "gcm", MODE_PRIVATE );
		preferences.edit().putString( "token", token ).commit();
		new Thread() {

			@Override
			public void run() {
				if( !isConnected() ) {
					return;
				}
				Session session = getSession();
				if( session != null ) {
					try {
						session.registerPushNotifications( "silent_text", "gcm", token );
					} catch( Throwable exception ) {
						getLog().warn( exception, "#registerPushNotifications" );
					}
				}
				JabberClient jabber = getJabber();
				if( jabber != null && jabber.isOnline() ) {
					jabber.registerPushNotificationToken( getPackageName(), token );
				}
			}

		}.start();
	}

	/**
	 * @param message
	 */
	public void onPushNotification() {
		// getLog().debug( "#onPushNotification" );
		sendBroadcast( Action.NOTIFY.intent(), Manifest.permission.READ );
	}

	public void ping() {
		timeOfMostRecentActivity = System.currentTimeMillis();
		rescheduleAutoDisconnect();
		rescheduleAutoLock();
	}

	private String readAssetAsString( String path ) {
		InputStream in = null;
		ByteArrayOutputStream out = null;
		try {
			in = getAssets().open( path );
			out = new ByteArrayOutputStream();
			IOUtils.pipe( in, out );
			return out.toString();
		} catch( IOException exception ) {
			return null;
		} finally {
			IOUtils.close( in, out );
		}
	}

	public void registerKeyManagerIfNecessary() {
		if( keyManagerToken != 0 ) {
			return;
		}
		if( isKeyManagerSupported() ) {

			keyManagerToken = KeyManagerSupport.registerWithKeyManager( getContentResolver(), getPackageName(), getString( R.string.silent_text ) );

			KeyManagerSupport.addListener( new KeyManagerListener() {

				@Override
				public void onKeyDataRead() {
					// Ignore.
				}

				@Override
				public void onKeyManagerLockRequest() {
					lock();
				}

				@Override
				public void onKeyManagerUnlockRequest() {
					byte [] unlockCode = KeyManagerSupport.getPrivateKeyData( getContentResolver(), Extra.PASSWORD.getName() );
					try {
						if( unlockCode != null ) {
							char [] hashedUnlockCode = CryptoUtils.toCharArray( unlockCode );
							try {
								unlockWithHashedPasscode( hashedUnlockCode );
							} finally {
								CryptoUtils.randomize( hashedUnlockCode );
							}
						}
					} finally {
						CryptoUtils.randomize( unlockCode );
					}
				}

			} );

		}
	}

	private List<NamedKeyPair> removeExpired( List<NamedKeyPair> pairs ) {
		for( int i = 0; i < pairs.size(); i++ ) {
			NamedKeyPair pair = pairs.get( i );
			if( isExpired( pair ) ) {
				new Thread( new RemoveKeyPair( getSession(), keyPairs, pair.locator ) ).start();
				pairs.remove( i );
				i--;
			}
		}
		return pairs;
	}

	public void removeSharedDataFromKeyManager( String id ) {
		if( isKeyManagerSupported() ) {
			KeyManagerSupport.deleteSharedKeyData( getContentResolver(), id );
		}
	}

	public void removeSharedSession() {
		removeSharedDataFromKeyManager( KeyManagerSupport.DEV_AUTH_DATA_TAG );
		removeSharedDataFromKeyManager( KeyManagerSupport.DEV_UNIQUE_ID_TAG );
	}

	public void requestUnlockCode() {
		Intent intent = new Intent( this, UnlockActivity.class );
		intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
		startActivity( intent );
	}

	private void rescheduleAutoDisconnect() {

		cancelAutoDisconnect();

		if( !GCMService.isAvailable( this ) ) {
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
					if( isUnlocked() && isActivated() ) {
						setJabberServer( getServer( "xmpp" ) );
					} else {
						sendBroadcast( Action.CONNECT.intent(), Manifest.permission.READ );
					}
				} else {
					setJabberServer( null );
				}
			}
		};

		return respondToNetworkStateChange;

	}

	public void save( Credential credential ) {
		CredentialRepository repository = getCredentials();
		if( repository != null ) {
			repository.save( credential );
		}
	}

	public void save( NamedKeyPair keyPair ) {
		createNamedKeyPairRepository();
		keyPairs.save( keyPair );
		new Thread( new UploadPublicKey( getSession(), extractPublicKey( keyPair ) ) ).start();
	}

	private void save( StorageKeySpec storageKey, String alias, char [] passcode ) {
		storageKey.ephemeralKey.iterationCount = getCalibratedIterationCount( 2, 1024, 10240 );
		storageKey.cycle( passcode );
		File file = new File( getFilesDir(), Hash.sha1( alias ) + ".key" );
		OutputStream out = null;
		try {
			out = new FileOutputStream( file, false );
			storageKey.write( out );
		} catch( IOException exception ) {
			throw new RuntimeException( exception );
		} finally {
			IOUtils.close( out );
		}
	}

	public void setAPIKey( String apiKey ) {
		Server server = getServer( "broker" );
		server.getCredential().setPassword( apiKey );
		getServers().save( server );
	}

	public void setJabber( Credential credential ) {
		Server server = getOrCreateServer( "xmpp" );
		credential.setDomain( getDomain() );
		if( server.getCredential() == null ) {
			server.setCredential( credential );
		} else {
			server.getCredential().setUsername( credential.getUsername() );
			server.getCredential().setPassword( credential.getPassword() );
		}
		conversations = null;
		getServers().save( server );
		unlockUserKey();
	}

	public void setJabber( JabberClient jabber ) {

		if( this.jabber != jabber ) {

			if( this.jabber != null ) {
				this.jabber.onDestroy();
			}

			if( jabber != null ) {
				jabber.setLog( new Log( jabber.getClass().getSimpleName() ) );
				jabber.getLog().onCreate();
			} else {
				sendBroadcast( Action.DISCONNECT.intent(), Manifest.permission.READ );
			}

		}

		this.jabber = jabber;

		getNative().setIdentity( jabber == null ? null : jabber.getUsername() );

		if( jabber == null ) {
			return;
		}

		this.jabber.addListener( new TransportListener() {

			private void broadcastUpdate( Conversation conversation ) {
				Intent intent = Action.UPDATE_CONVERSATION.intent();
				Extra.PARTNER.to( intent, conversation.getPartner().getUsername() );
				sendBroadcast( intent, Manifest.permission.READ );
			}

			@Override
			public void onInsecureMessageReceived( String id, String sender, String message ) {
				getLog().info( "INSECURE MESSAGE [%s] %s: %s", id, sender, message );
			}

			@Override
			public void onMessageReturned( String id, String recipient, String reason ) {

				int index = recipient.indexOf( '/' );
				String recipientWithoutResource = index >= 0 ? recipient.substring( 0, index ) : recipient;
				ConversationRepository conversations = getConversations();
				Conversation conversation = conversations.findByPartner( recipientWithoutResource );

				if( conversation == null ) {
					return;
				}

				EventRepository events = conversations.historyOf( conversation );

				Event returnedEvent = events.findById( id );
				if( returnedEvent instanceof OutgoingMessage ) {
					Message message = (OutgoingMessage) returnedEvent;
					if( MessageState.SENT.equals( message.getState() ) ) {
						message.setState( MessageState.ENCRYPTED );
						events.save( message );
					}
				}

				ErrorEvent event = new ErrorEvent();
				event.setConversationID( conversation.getPartner().getUsername() );
				event.setText( reason );
				events.save( event );

				broadcastUpdate( conversation );

			}

			@Override
			public void onMessageSent( String id, String recipient, String text ) {

				ConversationRepository _conversations = getConversations();
				Conversation conversation = _conversations.findByPartner( recipient );

				if( conversation == null ) {
					return;
				}

				EventRepository events = _conversations.historyOf( conversation );
				Event event = events.findById( id );

				if( event instanceof Message ) {

					Message message = (Message) event;

					if( isResendRequest( message ) ) {
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
			public void onSecureMessageReceived( String id, String sender, String message, boolean notifiable, boolean badgeworthy ) {

				int index = sender.indexOf( '/' );
				String partner = index >= 0 ? sender.substring( 0, index ) : sender;
				String resourceName = index >= 0 ? sender.substring( index + 1 ) : null;

				ConversationRepository conversations = getConversations();
				Conversation conversation = conversations.findByPartner( partner );

				if( conversation == null ) {
					conversation = new Conversation();
					conversation.setStorageKey( CryptoUtils.randomBytes( 64 ) );
					conversation.setPartner( new Contact( partner ) );
					conversations.save( conversation );
				}

				EventRepository events = conversations.historyOf( conversation );

				if( resourceName != null && !resourceName.equals( conversation.getPartner().getDevice() ) ) {
					conversation.getPartner().setDevice( resourceName );
					conversations.save( conversation );
					Event resourceChange = new ResourceChangeEvent( getString( R.string.connected_to, resourceName ) );
					resourceChange.setConversationID( conversation.getPartner().getUsername() );
					events.save( resourceChange );
				}

				getNative().decrypt( conversation.getPartner().getUsername(), id, message, notifiable, badgeworthy );

			}

		} );

		sendBroadcast( Action.CONNECT.intent(), Manifest.permission.READ );

	}

	public void setJabberServer( final Server server ) {

		if( xmppConnectionThread != null && xmppConnectionThread.isAlive() ) {
			return;
		}

		onlineStatus = getString( R.string.not_connected );
		sendBroadcast( Action.XMPP_STATE_CHANGED.intent(), Manifest.permission.READ );

		if( server == null || server.getDomain() == null ) {
			setJabber( (JabberClient) null );
			return;
		}

		xmppConnectionThread = new Thread( new NewJabberClient( getJabber(), server, getLocalResourceName(), getTrustStorePath(), getOutgoingMessageQueue(), new OnDisconnectedListener() {

			@Override
			public void onDisconnected( Throwable throwable ) {
				if( throwable.getMessage() != null && !"".equals( throwable.getMessage() ) ) {
					onlineStatus = throwable.getMessage();
				} else {
					onlineStatus = getString( R.string.not_connected );
				}
				ServiceConfiguration.getInstance().xmpp.removeFromCache( getNetworks() );
				sendBroadcast( Action.XMPP_STATE_CHANGED.intent(), Manifest.permission.READ );
			}

		}, XMPP_PING_INTERVAL, getNetworks() ) {

			@Override
			protected void onClientConnected( JabberClient client ) {
				if( client != null && client.isOnline() ) {
					connected = true;
					onlineStatus = getString( R.string.connected );
					getServers().save( server );
					sendBroadcast( Action.XMPP_STATE_CHANGED.intent(), Manifest.permission.READ );
					String pushToken = getPushNotificationToken();
					if( pushToken != null ) {
						client.registerPushNotificationToken( getPackageName(), pushToken );
					}
				}
			}

			@Override
			protected void onClientInitialized( JabberClient client ) {
				onlineStatus = getString( R.string.connecting );
				sendBroadcast( Action.XMPP_STATE_CHANGED.intent(), Manifest.permission.READ );
				setJabber( client );
			}

			@Override
			protected void onError( Throwable throwable ) {
				getLog().warn( throwable, "XMPP #onError" );
				if( throwable.getMessage() != null && !"".equals( throwable.getMessage() ) ) {
					onlineStatus = throwable.getMessage();
				} else {
					onlineStatus = getString( R.string.not_connected );
				}
				sendBroadcast( Action.XMPP_STATE_CHANGED.intent(), Manifest.permission.READ );
			}

			@Override
			protected void onLookupServiceRecord( String domain ) {
				onlineStatus = getString( R.string.performing_srv_lookup );
				sendBroadcast( Action.XMPP_STATE_CHANGED.intent(), Manifest.permission.READ );
			}

			@Override
			protected void onServiceRecordReceived( String host, int port ) {
				onlineStatus = getString( R.string.connecting );
				sendBroadcast( Action.XMPP_STATE_CHANGED.intent(), Manifest.permission.READ );
			}

		} );

		xmppConnectionThread.start();

	}

	public void setLocalResourceName( final String resourceName ) {
		Credential credential = getCredential( getUsername() );
		String r = resourceName.length() <= 0 ? null : resourceName;
		if( credential == null ) {
			credential = new Credential();
			credential.setUsername( getUsername() );
			credential.setDomain( getDomain() );
		}
		String previousValue = credential.getResource();
		if( StringUtils.equals( r, previousValue ) ) {
			return;
		}
		credential.setResource( r );
		save( credential );
		setJabberServer( getServer( "xmpp" ) );
	}

	public void setUnlockCode( char [] unlockCode ) {
		char [] hashedUnlockCode = Hash.sha1( unlockCode );
		try {
			StorageKeySpec storageKey = storageKeys.get( APPLICATION_KEY_ALIAS );
			save( storageKey, APPLICATION_KEY_ALIAS, hashedUnlockCode );
		} finally {
			CryptoUtils.randomize( hashedUnlockCode );
		}
	}

	public void setUnlockCode( CharSequence unlockCode ) {
		char [] buffer = CryptoUtils.copyAsCharArray( unlockCode );
		try {
			setUnlockCode( buffer );
		} finally {
			CryptoUtils.randomize( buffer );
		}
	}

	public void shareSession() {
		Session session = getSession();
		if( session instanceof AuthenticatedSession ) {
			shareSession( (AuthenticatedSession) session );
		}
	}

	public void shareSession( AuthenticatedSession session ) {
		if( session != null && isKeyManagerSupported() ) {
			KeyManagerSupport.storeSharedKeyData( getContentResolver(), CryptoUtils.toByteArray( session.getAccessToken() ), KeyManagerSupport.DEV_AUTH_DATA_TAG );
			KeyManagerSupport.storeSharedKeyData( getContentResolver(), CryptoUtils.toByteArray( session.getID() ), KeyManagerSupport.DEV_UNIQUE_ID_TAG );
		}
	}

	public void startListeningForLocationUpdates() {
		if( !locationListener.isRegistered() ) {
			locationListener.register( this );
		}
	}

	public void stopListeningForLocationUpdates() {
		if( locationListener.isRegistered() ) {
			locationListener.unregister( this );
		}
	}

	protected void transition( String remoteUserID, String packetID ) {
		Intent intent = Action.TRANSITION.intent();
		Extra.PARTNER.to( intent, remoteUserID );
		Extra.ID.to( intent, packetID );
		sendBroadcast( intent, Manifest.permission.READ );
	}

	protected void unlockWithHashedPasscode( char [] unlockCode ) {
		try {
			applicationKey = getKey( APPLICATION_KEY_ALIAS, unlockCode );
			if( isKeyManagerSupported() ) {
				KeyManagerSupport.storePrivateKeyData( getContentResolver(), CryptoUtils.toByteArray( unlockCode ), Extra.PASSWORD.getName() );
			}
			unlockUserKey();
		} finally {
			CryptoUtils.randomize( unlockCode );
		}
	}

	public void unlock( char [] unlockCode ) {
		char [] hashedUnlockCode = Hash.sha1( unlockCode );
		try {
			unlockWithHashedPasscode( hashedUnlockCode );
		} finally {
			CryptoUtils.randomize( hashedUnlockCode );
		}
	}

	public void unlock( CharSequence unlockCode ) {
		char [] buffer = CryptoUtils.toCharArray( unlockCode );
		try {
			unlock( buffer );
		} finally {
			CryptoUtils.randomize( buffer );
		}
	}

	protected void unlockUserKey() {
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
		char [] passcode = getPasscodeForUserKey( credential );
		userKey = getKey( username, passcode );
		CryptoUtils.randomize( passcode );
		setJabberServer( server );
		getOrCreateKeyPair();
		validateSession();
	}

	private void validateSession() {

		new Thread( new SessionValidator( (AuthenticatedClientSession) getSession(), getNetworks() ) {

			@Override
			protected void onSessionInvalid() {
				super.onSessionInvalid();
				deactivate();
			}

		} ).start();

	}

	public void validateSharedSession() {

		if( isUnlocked() && isActivated() && isKeyManagerSupported() && !hasSharedSession() ) {

			new Thread( new Runnable() {

				@Override
				public void run() {
					deactivate();
				}

			} ).start();

		}

	}

}
