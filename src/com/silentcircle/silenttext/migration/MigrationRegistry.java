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
package com.silentcircle.silenttext.migration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

import com.silentcircle.silenttext.util.IOUtils;

public class MigrationRegistry {

	private static PackageInfo getPackageInfo( Context context ) {
		try {
			return context.getPackageManager().getPackageInfo( context.getPackageName(), 0 );
		} catch( NameNotFoundException exception ) {
			throw new RuntimeException( exception );
		}
	}

	private final List<Migration> registry = new ArrayList<Migration>();

	public void migrate( Context context ) {

		File file = new File( context.getFilesDir(), "version" );

		int currentVersion = getPackageInfo( context ).versionCode;
		int previousVersion = IOUtils.readIntegerFromFile( file );

		for( int i = 0; i < registry.size(); i++ ) {
			Migration migration = registry.get( i );
			if( migration.isRequired( context, previousVersion, currentVersion ) ) {
				migration.migrate( context, previousVersion, currentVersion );
			}
		}

		try {
			IOUtils.writeIntegerToFile( currentVersion, file );
		} catch( IOException exception ) {
			throw new RuntimeException( exception );
		}

	}

	public void register( Migration migration ) {
		registry.add( migration );
	}

}
