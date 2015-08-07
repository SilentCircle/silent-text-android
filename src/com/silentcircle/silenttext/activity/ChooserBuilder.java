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
	private String label;

	public ChooserBuilder( Context context ) {
		this.context = context;
	}

	public Intent build() {

		if( intents.isEmpty() ) {
			return null;
		}

		Intent chooser = Intent.createChooser( intents.get( 0 ), label );

		int size = intents.size();

		if( size > 1 ) {
			Intent [] extras = new Intent [size - 1];
			intents.subList( 1, size ).toArray( extras );
			chooser.putExtra( Intent.EXTRA_INITIAL_INTENTS, extras );
		}

		return chooser;

	}

	public ChooserBuilder intent( Intent intent ) {
		return intent( intent, 0 );
	}

	public ChooserBuilder intent( Intent intent, CharSequence label ) {

		if( intent == null ) {
			return this;
		}

		PackageManager packageManager = context.getPackageManager();
		List<ResolveInfo> activities = packageManager.queryIntentActivities( intent, 0 );

		if( activities == null || activities.isEmpty() ) {
			return this;
		}

		for( int i = 0; i < activities.size(); i++ ) {

			ResolveInfo activity = activities.get( i );
			String packageName = activity.activityInfo.packageName;
			String activityName = activity.activityInfo.name;
			ComponentName component = new ComponentName( packageName, activityName );

			Intent choice = new Intent( intent );
			choice.setComponent( component );

			LabeledIntent labeledIntent = new LabeledIntent( choice, packageName, label != null ? label : activity.loadLabel( packageManager ), activity.icon );

			intents.add( labeledIntent );

		}

		return this;

	}

	public ChooserBuilder intent( Intent intent, int labelStringResourceID ) {
		return intent( intent, labelStringResourceID != 0 ? context.getString( labelStringResourceID ) : null );
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
		return this;
	}

}
