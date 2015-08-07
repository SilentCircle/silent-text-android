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
package com.silentcircle.silenttext.model.json.util;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.content.Context;
import android.location.Location;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;

import com.silentcircle.silenttext.model.Conversation;

public class JSONSirenDecorator {

	/**
	 * @param mimeType
	 */
	@TargetApi( Build.VERSION_CODES.GINGERBREAD_MR1 )
	public static JSONObject decorateSirenForAttachment( Context context, JSONObject siren, Uri uri, String mimeType ) {
		if( context == null || siren == null || uri == null ) {
			return siren;
		}
		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1 ) {
			try {
				MediaMetadataRetriever m = new MediaMetadataRetriever();
				m.setDataSource( context, uri );
				decorateSirenForMediaDuration( siren, m.extractMetadata( MediaMetadataRetriever.METADATA_KEY_DURATION ) );
				decorateSirenForVideoDimensions( siren, m );
			} catch( RuntimeException exception ) {
				// This probably isn't a media file.
			}
		} else {
			MediaPlayer player = MediaPlayer.create( context, uri );
			if( player != null ) {
				decorateSirenForMediaDuration( siren, player.getDuration() );
				decorateSirenForVideoDimensions( siren, player.getVideoWidth(), player.getVideoHeight() );
				player.release();
			}
		}
		return siren;
	}

	public static JSONObject decorateSirenForConversation( JSONObject json, Conversation conversation, Location location, boolean shouldRequestDeliveryNotification ) {

		if( conversation == null ) {
			return json;
		}

		try {

			if( shouldRequestDeliveryNotification ) {
				json.put( "request_receipt", 1 );
			}

			if( conversation.hasBurnNotice() ) {
				json.put( "shred_after", conversation.getBurnDelay() );
			}

			if( conversation.isLocationEnabled() ) {

				if( location != null ) {

					JSONObject jsonLocation = new JSONObject();

					jsonLocation.put( "latitude", location.getLatitude() );
					jsonLocation.put( "longitude", location.getLongitude() );
					jsonLocation.put( "timestamp", location.getTime() );
					jsonLocation.put( "altitude", location.getAltitude() );
					jsonLocation.put( "horizontalAccuracy", location.getAccuracy() );
					jsonLocation.put( "verticalAccuracy", location.getAccuracy() );

					json.put( "location", jsonLocation.toString() );

				}

			}

		} catch( JSONException impossible ) {
			// Ignore.
		}

		return json;

	}

	public static JSONObject decorateSirenForMediaDuration( JSONObject siren, int duration ) {
		try {
			siren.put( "duration", Double.toString( duration * 0.001 ) );
		} catch( JSONException impossible ) {
			// Ignore.
		}
		return siren;
	}

	public static JSONObject decorateSirenForMediaDuration( JSONObject siren, String duration ) {
		if( duration != null ) {
			try {
				return decorateSirenForMediaDuration( siren, Integer.parseInt( duration ) );
			} catch( NumberFormatException ignore ) {
				// Ignore.
			}
		}
		return siren;
	}

	public static JSONObject decorateSirenForVideoDimensions( JSONObject siren, int width, int height ) {
		try {
			siren.put( "video_width", width );
			siren.put( "video_height", height );
		} catch( JSONException impossible ) {
			// Ignore.
		}
		return siren;
	}

	@TargetApi( Build.VERSION_CODES.ICE_CREAM_SANDWICH )
	public static JSONObject decorateSirenForVideoDimensions( JSONObject siren, MediaMetadataRetriever metadata ) {
		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH ) {
			String width = metadata.extractMetadata( MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH );
			String height = metadata.extractMetadata( MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT );
			decorateSirenForVideoDimensions( siren, width, height );
		}
		return siren;
	}

	public static JSONObject decorateSirenForVideoDimensions( JSONObject siren, String width, String height ) {

		if( width != null && height != null ) {
			try {
				return decorateSirenForVideoDimensions( siren, Integer.parseInt( width ), Integer.parseInt( height ) );
			} catch( NumberFormatException ignore ) {
				// Ignore.
			}
		}

		return siren;

	}

}
