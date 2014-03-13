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
package com.silentcircle.silenttext.listener;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class SilentLocationListener implements LocationListener {

	private static boolean equals( Object a, Object b ) {
		return a == null ? b == null : a.equals( b );
	}

	private static boolean isBetterLocation( Location suggestedLocation, Location knownLocation, long threshold ) {

		if( suggestedLocation == null ) {
			return false;
		}

		if( knownLocation == null ) {
			return true;
		}

		long timeDelta = suggestedLocation.getTime() - knownLocation.getTime();

		boolean isSignificantlyNewer = timeDelta > threshold;
		boolean isSignificantlyOlder = timeDelta < -threshold;
		boolean isNewer = timeDelta > 0;

		if( isSignificantlyNewer ) {
			return true;
		} else if( isSignificantlyOlder ) {
			return false;
		}

		int accuracyDelta = (int) ( suggestedLocation.getAccuracy() - knownLocation.getAccuracy() );

		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		boolean isFromSameProvider = equals( suggestedLocation.getProvider(), knownLocation.getProvider() );

		if( isMoreAccurate ) {
			return true;
		}
		if( isNewer ) {
			if( !isLessAccurate ) {
				return true;
			}
			if( !isSignificantlyLessAccurate ) {
				if( isFromSameProvider ) {
					return true;
				}
			}
		}

		return false;

	}

	private Location location;
	private boolean registered;

	public Location getLocation() {
		return location;
	}

	public boolean isRegistered() {
		return registered;
	}

	@Override
	public void onLocationChanged( Location freshLocation ) {
		if( isBetterLocation( freshLocation, location, 1000 * 60 * 2 ) ) {
			location = freshLocation;
		}
	}

	@Override
	public void onProviderDisabled( String provider ) {
		// Don't care.
	}

	@Override
	public void onProviderEnabled( String provider ) {
		// Don't care.
	}

	@Override
	public void onStatusChanged( String provider, int status, Bundle extras ) {
		// Don't care.
	}

	public void register( Context context ) {
		register( (LocationManager) context.getSystemService( Context.LOCATION_SERVICE ) );
	}

	public void register( LocationManager manager ) {
		register( manager, LocationManager.NETWORK_PROVIDER );
		register( manager, LocationManager.GPS_PROVIDER );
	}

	public void register( LocationManager manager, String provider ) {
		try {
			manager.requestLocationUpdates( provider, 0, 0, this );
			onLocationChanged( manager.getLastKnownLocation( provider ) );
			registered = true;
		} catch( IllegalArgumentException exception ) {
			Log.w( getClass().getSimpleName(), String.format( "The %s provider is not available.", provider ) );
		}
	}

	public void unregister( Context context ) {
		unregister( (LocationManager) context.getSystemService( Context.LOCATION_SERVICE ) );
	}

	public void unregister( LocationManager manager ) {
		manager.removeUpdates( this );
		registered = false;
	}

}
