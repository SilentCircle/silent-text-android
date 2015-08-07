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
package com.silentcircle.silenttext.location;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.Settings;

import com.silentcircle.silenttext.R;

public class LocationUtils {

	public static final int DEFAULT_ZOOM_LEVEL = 11;
	public static final String URI_FORMAT_LOCATION_WEB_GOOGLE = "https://www.google.com/maps/@%1$.6f,%2$.6f,%3$fz";
	public static final String URI_FORMAT_LOCATION_GEO = "geo:0,0?q=%1$.6f,%2$.6f(%4$s)";

	public static boolean isLocationSharingAvailable( Context context ) {

		LocationManager manager = (LocationManager) context.getSystemService( Context.LOCATION_SERVICE );

		if( manager == null ) {
			return false;
		}

		Criteria criteria = new Criteria();
		criteria.setAccuracy( Criteria.ACCURACY_FINE );
		criteria.setAltitudeRequired( false );
		criteria.setBearingRequired( false );
		criteria.setCostAllowed( true );
		criteria.setPowerRequirement( Criteria.NO_REQUIREMENT );

		String provider = manager.getBestProvider( criteria, true );

		if( provider == null || LocationManager.PASSIVE_PROVIDER.equals( provider ) ) {
			return false;
		}

		return true;

	}

	public static void startLocationSettingsActivity( Context context ) {
		Intent intent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS );
		if( context.getPackageManager().resolveActivity( intent, 0 ) != null ) {
			context.startActivity( intent );
		}
	}

	public static void viewLocation( Context context, double latitude, double longitude ) {
		viewLocation( context, latitude, longitude, DEFAULT_ZOOM_LEVEL );
	}

	public static void viewLocation( Context context, double latitude, double longitude, float zoom ) {
		try {
			viewLocation( context, latitude, longitude, zoom, URI_FORMAT_LOCATION_GEO );
		} catch( ActivityNotFoundException exception ) {
			viewLocation( context, latitude, longitude, zoom, URI_FORMAT_LOCATION_WEB_GOOGLE );
		}
	}

	public static void viewLocation( Context context, double latitude, double longitude, float zoom, String format ) {
		context.startActivity( new Intent( Intent.ACTION_VIEW, Uri.parse( String.format( format, Double.valueOf( latitude ), Double.valueOf( longitude ), Float.valueOf( zoom ), context.getString( R.string.undisclosed_location ) ) ) ) );
	}

	public static void viewLocation( Context context, double latitude, double longitude, String format ) {
		viewLocation( context, latitude, longitude, DEFAULT_ZOOM_LEVEL, format );
	}

	public static void viewLocation( Context context, Location location ) {
		viewLocation( context, location, 11 );
	}

	public static void viewLocation( Context context, Location location, float zoom ) {
		if( location != null ) {
			viewLocation( context, location.getLatitude(), location.getLongitude(), zoom );
		}
	}

}
