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

import java.lang.ref.SoftReference;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;

import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.log.Log;
import com.silentcircle.silenttext.model.Conversation;
import com.silentcircle.silenttext.model.event.Event;
import com.silentcircle.silenttext.model.event.Message;
import com.silentcircle.silenttext.repository.ConversationRepository;
import com.silentcircle.silenttext.repository.EventRepository;

public class SaveConversationHistoryOnConfirm implements OnConfirmListener {

	private static final Log LOG = new Log( SaveConversationHistoryOnConfirm.class.getSimpleName() );
	private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance();

	private static String getPrintableHistory( List<Event> events ) {
		StringBuilder history = new StringBuilder();
		for( int i = 0; i < events.size(); i++ ) {
			Event event = events.get( i );
			if( event instanceof Message && ( (Message) event ).expires() ) {
				continue;
			}
			history.append( "[" ).append( DATE_FORMAT.format( new Date( event.getTime() ) ) ).append( "] " );
			if( event instanceof Message ) {
				Message message = (Message) event;
				history.append( withoutDomain( message.getSender() ) ).append( ": " );
				try {
					JSONObject json = new JSONObject( message.getText() );
					if( json.has( "message" ) ) {
						history.append( json.getString( "message" ) );
					} else {
						history.append( "(attachment)" );
					}
				} catch( JSONException exception ) {
					history.append( message.getText() );
				}
			} else {
				history.append( event.getText() );
			}
			history.append( "\n" );
		}
		return history.toString();
	}

	private static String withoutDomain( String fullAddress ) {
		return fullAddress == null ? null : fullAddress.replaceAll( "^(.+)@(.+)$", "$1" );
	}

	private final SoftReference<Activity> activityReference;
	private final String remoteUserID;

	public SaveConversationHistoryOnConfirm( Activity activity, String remoteUserID ) {
		activityReference = new SoftReference<Activity>( activity );
		this.remoteUserID = remoteUserID;
	}

	@Override
	public void onConfirm( Context context ) {
		Activity activity = activityReference.get();
		if( activity != null ) {
			ConversationRepository conversations = SilentTextApplication.from( context ).getConversations();
			Conversation conversation = conversations.findByPartner( remoteUserID );
			EventRepository history = conversations.historyOf( conversation );
			List<Event> messages = history.list();
			Intent intent = new Intent( Intent.ACTION_SEND );
			intent.setType( "text/plain" );
			intent.putExtra( Intent.EXTRA_TEXT, getPrintableHistory( messages ) );
			intent.putExtra( Intent.EXTRA_SUBJECT, context.getString( R.string.conversation_with, withoutDomain( remoteUserID ) ) );
			try {
				activity.startActivity( intent );
			} catch( ActivityNotFoundException exception ) {
				LOG.warn( exception, "#onClick" );
			}
		}
	}

}
