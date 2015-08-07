/*
Copyright (C) 2015, Silent Circle, LLC. All rights reserved.

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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.silentcircle.keymanagersupport.KeyManagerSupport;
import com.silentcircle.silentcontacts.ScBaseColumns;
import com.silentcircle.silentcontacts.ScContactsContract.CommonDataKinds.Phone;
import com.silentcircle.silenttext.application.SilentTextApplication;

public class Constants {

	public static class PhoneQuery {

		public static final String [] PROJECTION_PRIMARY = new String [] {
			ScBaseColumns._ID, // 0
			Phone.TYPE, // 1
			Phone.LABEL, // 2
			Phone.NUMBER, // 3
			Phone.RAW_CONTACT_ID, // 4
			Phone.PHOTO_ID, // 5
			Phone.DISPLAY_NAME_PRIMARY, // 6
		};

		// keep this as reference to SPA project
		// private static final String[] PROJECTION_ALTERNATIVE = new String[] {
		// ScBaseColumns._ID, // 0
		// Phone.TYPE, // 1
		// Phone.LABEL, // 2
		// Phone.NUMBER, // 3
		// Phone.RAW_CONTACT_ID, // 4
		// Phone.PHOTO_ID, // 5
		// Phone.DISPLAY_NAME_ALTERNATIVE, // 6
		// };

		public static final int PHONE_ID = 0;
		public static final int PHONE_TYPE = 1;
		public static final int PHONE_LABEL = 2;
		public static final int PHONE_NUMBER = 3;
		public static final int PHONE_CONTACT_ID = 4;
		public static final int PHONE_PHOTO_ID = 5;
		public static final int PHONE_DISPLAY_NAME = 6;
	}

	public static final String SILENT_CALL_ACTION = "com.silentcircle.silentphone.action.NEW_OUTGOING_CALL";

	public static final String PROVIDER_NAME = "com.silentcircle.silenttext";
	public static final String URL = "content://" + PROVIDER_NAME + "/starred";
	public static final Uri STARRED_CONTENT_URI = Uri.parse( URL );

	public static final String USER_NAME = "user_name";
	public static final String FULL_NAME = "full_name";

	public static final String PHONE_NUMBERS = "phone_numbers";
	public static final String _ID = "_id";
	public static final String RAW_CONTACT_ID = "raw_contact_id";
	public static final String TYPE = "data2";
	public static final String LABEL = "data3";
	public static final String NUMBER = "data1";
	public static final String PHOTO_ID = "photo_id";
	public static final String IM = "data1";

	public static final String DISPLAY_NAME_PRIMARY = "display_name";

	public final static String PHONE_NUMBER = "phone_number";
	public final static String DISPLAY_NAME = "display_name";
	public final static String JID = "jid";
	public final static String STARRED = "starred";
	public final static String PHOTO_THUMB_URI = "photo_thumb_uri";

	public final static String IS_FAVORITE = "is_favorite";

	public static final Integer TYPE_SILENT = new Integer( 30 );
	public static final int NO_ORGANIZATION = 111;

	public static final int STOP_LOADING = 112;
	public static final int DISPLAY_ORDER_PRIMARY = 1;

	public static final int SORT_ORDER_PRIMARY = 1;

	public static final int CONTACTS_SECTION = 0;
	public static final int DIRECTORY_SECTION = 1;

	public static final int DIRECTORY_SEARCH_DIALOG = 1;
	public static final int DIRECTORY_SEARCH_DIALOG_NO_SPA = 2;
	public static final int BG_IMPORTANCE_LIVE = 7;
	public static final int BG_IMPORTANCE_EXIT = 9;

	public static final String SPA_PACKAGE_NAME = "com.silentcircle.silentphone";
	public static final String DEV_AUTH_DATA_TAG_DEV = "device_authorization_dev";

	// mIsDirectorySearchEnabled is used as config for directory search.
	public static boolean mIsDirectorySearchEnabled = true;
	public static boolean mIsShowDirectorySerachCheckBox = false;
	public static boolean mLaunchConversationActivity;
	public static boolean mIsSubmitQuery = true;
	public static boolean mIsLaunchSPA;
	public static boolean mIsMessageClicked;
	public static boolean mIsContactInfoClicked;
	public static boolean mIsDirectorySearchFragment;
	public static boolean mConversationListItemClicked;
	public static boolean mIsExitApp = true;
	public static boolean mIsHomeClicked;
	public static boolean mIsIncrementalSearch;

	public static int mCurrentRow = -1;
	public static int mCurrentSection = -1;

	public static int OnConversationListActivityCreateCalled = 0;

	public static String mOrgName = "";
	public static String mApiKey = "";

	public static boolean mIsSharePhoto = false;
	public static Date mAccountExpirationDate = null;
	public static boolean mIsAccountExpired = false;
	public static final int ACCOUNT_EXPIRED_DIALOG = 3;

	public static final String TAG = "Constans";

	public static String getIpAddressAsText( byte [] arrAddr ) {
		if( arrAddr == null ) {
			Log.e( TAG, "  Parameter arrAddr is null." );
			return null;
		}

		boolean isIpv6;

		if( arrAddr.length == 4 ) {
			isIpv6 = false;
		} else if( arrAddr.length == 16 ) {
			isIpv6 = true;
		} else {
			Log.e( TAG, "Incorrect size of array arrAddr " + arrAddr.length );
			return "";
		}

		StringBuilder sb = new StringBuilder();

		if( isIpv6 ) {
			int temp1, temp2;
			for( int i = 0; i < 16; i++ ) {
				temp1 = arrAddr[i];
				temp1 <<= 8;
				temp2 = arrAddr[i + 1];
				temp1 |= temp2;
				temp1 &= 0x0000ffff;
				i++;

				sb.append( temp1 );
				if( i != 15 ) {
					sb.append( ":" );
				}
			}

		} else {
			short temp;
			for( int i = 0; i < arrAddr.length; i++ ) {
				temp = arrAddr[i];
				temp &= 0x00ff;
				sb.append( temp );
				if( i != arrAddr.length - 1 ) {
					sb.append( "." );
				}
			}
		}

		String arress = sb.toString();

		return arress;
	}

	private static String getPreferableIpAddress( List<InetAddress> IpAddresses ) {

		InetAddress bestInetAddr = null;

		if( IpAddresses.size() == 0 ) {
			return "";
		} else if( IpAddresses.size() > 1 ) {
			Iterator<InetAddress> inadItr = IpAddresses.iterator();
			InetAddress inadx;
			boolean foundipv4 = false;
			while( inadItr.hasNext() ) {
				inadx = inadItr.next();
				if( inadx instanceof Inet4Address ) {
					if( !inadx.isLoopbackAddress() ) {
						bestInetAddr = inadx;
						foundipv4 = true;
						break;
					}
				}
			}

			if( !foundipv4 ) {
				inadItr = IpAddresses.listIterator();
				while( inadItr.hasNext() ) {
					inadx = inadItr.next();
					if( !inadx.isLoopbackAddress() ) {
						bestInetAddr = inadx;
						break;
					}
				}
			}

		} else {
			InetAddress inaddr = IpAddresses.get( 0 );
			if( !inaddr.isLoopbackAddress() ) {
				bestInetAddr = inaddr;
			}

		}

		if( bestInetAddr != null ) {
			String ipAddress = getIpAddressAsText( bestInetAddr.getAddress() );
			if( !TextUtils.isEmpty( ipAddress ) ) {
				return ipAddress;
			}
			android.util.Log.w( TAG, "Faild to get IP address from byte array" );
		} else {
			Log.w( TAG, "No valid InetAddress for the current Network Interface" );
		}

		return "";
	}

	public static String getShardAuthTag( Context context ) {
		return SilentTextApplication.from( context ).getAPIURL().contains( "dev" ) ? DEV_AUTH_DATA_TAG_DEV : KeyManagerSupport.DEV_AUTH_DATA_TAG;
	}

	public static String getVpnDnsAddress( String ip ) {
		String vpnDns = "";
		String [] tmp = ip.split( "\\." );
		if( !TextUtils.isEmpty( ip ) ) {
			tmp[tmp.length - 1] = "1";
			vpnDns = tmp[0];
			for( int i = 1; i < tmp.length; i++ ) {
				vpnDns += "." + tmp[i];
			}
		}
		return vpnDns;
	}

	public static String getVpnLocalIp() {
		try {
			Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
			if( en != null ) {
				String netIfc = null;
				while( en.hasMoreElements() ) {
					NetworkInterface intf = en.nextElement();
					netIfc = intf.getDisplayName();
					if( netIfc == null ) {
						continue;
					}

					if( netIfc.length() < 3 ) {
						continue;
					}

					String protocol = netIfc.substring( 0, 3 );
					if( protocol.equals( "ppp" ) || protocol.equals( "tun" ) || protocol.equals( "tap" ) || protocol.equals( "l2t" ) ) {
						List<InetAddress> listIpAddr = Collections.list( intf.getInetAddresses() );
						if( listIpAddr.isEmpty() ) {
							continue;
						}
						return getPreferableIpAddress( listIpAddr );
					}

				}
			}
		} catch( SocketException ex ) {
			Log.i( TAG, ex.toString() );
		}

		return "";
	}

	public static boolean isAccountExpired() {
		if( mAccountExpirationDate != null ) {
			Calendar now = Calendar.getInstance();
			now.setTimeInMillis( System.currentTimeMillis() );
			Calendar expiry = Calendar.getInstance();
			expiry.setTime( mAccountExpirationDate );

			return expiry.after( now ) ? false : true;
		}

		return false;
	}

	public static boolean isAltDNS() {
		if( Build.VERSION.SDK_INT >= 21 ) {
			return true;
		}
		return false;
	}

	public static boolean isRTL() {
		String language = Locale.getDefault().getLanguage();
		if( Build.VERSION.SDK_INT >= 17 ) {
			if( language.equals( "ar" ) ) {
				return true;
			}
		}
		return false;
	}
}
