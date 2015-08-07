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

import java.util.HashMap;
import java.util.Map;

public class MIME {

	private static final String TYPE_AUDIO = "audio/";
	private static final String TYPE_IMAGE = "image/";
	private static final String TYPE_VIDEO = "video/";
	private static final String TYPE_TEXT = "text/";
	private static final String [] TYPE_VCARD = {
		"text/x-vcard",
		"text/vcard"
	};

	private static final Map<String, String> UTI_TO_MIME = new HashMap<String, String>();

	static {
		UTI_TO_MIME.put( "public.jpeg", "image/jpeg" );
		UTI_TO_MIME.put( "public.tiff", "image/tiff" );
		UTI_TO_MIME.put( "public.png", "image/png" );
		UTI_TO_MIME.put( "public.mpeg", "video/mpeg" );
		UTI_TO_MIME.put( "com.apple.quicktime-movie", "video/quicktime" );
		UTI_TO_MIME.put( "public.avi", "video/avi" );
		UTI_TO_MIME.put( "public.mpeg-4", "video/mp4" );
		UTI_TO_MIME.put( "public.mp3", "audio/mpeg" );
		UTI_TO_MIME.put( "com.compuserve.gif", "image/gif" );
	}

	public static String fromUTI( String uti ) {
		return UTI_TO_MIME.get( uti );
	}

	public static boolean isAudio( String type ) {
		return type != null && type.startsWith( TYPE_AUDIO );
	}

	public static boolean isContact( String type ) {
		if( type == null ) {
			return false;
		}
		for( String t : TYPE_VCARD ) {
			if( type.equals( t ) ) {
				return true;
			}
		}
		return false;
	}

	public static boolean isImage( String type ) {
		return type != null && type.startsWith( TYPE_IMAGE );
	}

	public static boolean isText( String type ) {
		return type != null && type.startsWith( TYPE_TEXT );
	}

	public static boolean isVideo( String type ) {
		return type != null && type.startsWith( TYPE_VIDEO );
	}

	public static boolean isVisual( String type ) {
		return isImage( type ) || isVideo( type );
	}

}
