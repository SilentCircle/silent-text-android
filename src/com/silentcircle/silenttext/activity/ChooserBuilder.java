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
package com.silentcircle.silenttext.activity;

import java.util.ArrayList;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

public class ChooserBuilder {

	private final Context context;
	private final List<Intent> intents = new ArrayList<Intent>();
	private Intent chooser;
	private String label;

	public ChooserBuilder( Context context ) {
		this.context = context;
	}

	public Intent build() {
		if( chooser != null && !intents.isEmpty() ) {
			chooser.putExtra( Intent.EXTRA_INITIAL_INTENTS, intents.toArray( new Intent [0] ) );
		}
		return chooser;
	}

	public ChooserBuilder intent( Intent intent ) {

		if( intent == null ) {
			return this;
		}

		PackageManager packageManager = context.getPackageManager();
		List<ResolveInfo> activities = packageManager.queryIntentActivities( intent, 0 );

		if( activities == null || activities.isEmpty() ) {
			return this;
		}

		if( chooser == null ) {
			chooser = Intent.createChooser( intent, label );
			return this;
		}

		for( int i = 0; i < activities.size(); i++ ) {

			ResolveInfo activity = activities.get( i );
			String packageName = activity.activityInfo.packageName;
			String activityName = activity.activityInfo.name;
			ComponentName component = new ComponentName( packageName, activityName );
			CharSequence label = activity.loadLabel( packageManager );

			Intent choice = new Intent( intent );
			choice.setComponent( component );
			intents.add( new LabeledIntent( choice, packageName, label, activity.icon ) );

		}

		return this;

	}

	public ChooserBuilder intent( String action ) {
		return intent( new Intent( action ) );
	}

	public ChooserBuilder label( int labelResourceID ) {
		return label( context.getString( labelResourceID ) );
	}

	public ChooserBuilder label( String label ) {
		this.label = label;
		return this;
	}

	public ChooserBuilder reset() {
		label = null;
		intents.clear();
		chooser = null;
		return this;
	}

}
