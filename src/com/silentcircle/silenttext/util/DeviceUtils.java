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
package com.silentcircle.silenttext.util;

import android.content.Context;
import android.content.res.Resources;

import com.silentcircle.silenttext.R;

public class DeviceUtils {

	public static interface Eligibility {

		public boolean test( String manufacturer, String model );

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

	public static String getManufacturer() {
		String manufacturer = System.getProperty( "ro.product.manufacturer" );
		return manufacturer == null ? android.os.Build.MANUFACTURER : manufacturer;
	}

	public static String getModel() {
		String model = System.getProperty( "ro.product.model" );
		return model == null ? android.os.Build.MODEL : model;
	}

	public static boolean isEligibleForAccountCreation( Context context ) {
		return context != null && isEligibleForAccountCreation( context.getResources() );
	}

	public static boolean isEligibleForAccountCreation( Eligibility eligibility ) {
		String model = getModel();
		String manufacturer = getManufacturer();
		return eligibility.test( manufacturer, model );
	}

	public static boolean isEligibleForAccountCreation( Resources resources ) {
		return resources != null && isEligibleForAccountCreation( resources.getString( R.string.build_partners ).split( "," ) );
	}

	public static boolean isEligibleForAccountCreation( String [] manufacturers ) {
		return manufacturers.length > 0 && StringUtils.isAnyOf( getManufacturer(), manufacturers );
	}

}
