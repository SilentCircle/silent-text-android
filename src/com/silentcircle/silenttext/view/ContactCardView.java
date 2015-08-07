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
package com.silentcircle.silenttext.view;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.silentcircle.silenttext.Extra;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.activity.ConversationActivity;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.model.Conversation;
import com.silentcircle.silenttext.util.AsyncUtils;
import com.silentcircle.silenttext.util.StringUtils;

public class ContactCardView extends RelativeLayout implements OnClickListener {

	class UpdateDisplayNameTask extends AsyncTask<String, Void, String> {

		private final Conversation conversation;

		UpdateDisplayNameTask( Conversation conversation ) {
			this.conversation = conversation;
		}

		@Override
		protected String doInBackground( String... args ) {
			return SilentTextApplication.from( getContext() ).getDisplayName( args[0] );
		}

		@Override
		protected void onPostExecute( String displayName ) {

			if( conversation != getTag() ) {
				return;
			}

			if( StringUtils.isMinimumLength( displayName, 1 ) ) {
				conversation.getPartner().setAlias( displayName );
				( (TextView) findViewById( R.id.display_name ) ).setText( conversation.getPartner().getAlias() );
			}

		}
	}

	public ContactCardView( Context context ) {
		super( context );
	}

	public ContactCardView( Context context, AttributeSet attrs ) {
		super( context, attrs );
	}

	public ContactCardView( Context context, AttributeSet attrs, int defStyle ) {
		super( context, attrs, defStyle );
	}

	@Override
	public void onClick( View _this ) {
		Object tag = getTag();
		if( tag instanceof Conversation ) {
			Intent intent = new Intent( getContext(), ConversationActivity.class );
			intent.putExtra( Extra.PARTNER.getName(), ( (Conversation) tag ).getPartner().getUsername() );
			getContext().startActivity( intent );
		}
	}

	@Override
	protected void onMeasure( int widthMeasureSpec, int heightMeasureSpec ) {
		super.onMeasure( widthMeasureSpec, widthMeasureSpec );
	}

	public void setConversation( final Conversation conversation ) {

		AvatarView avatar = (AvatarView) findViewById( R.id.avatar );

		avatar.setContact( conversation.getPartner(), R.dimen.avatar_xlarge );
		avatar.setInteractive( false );
		avatar.setSecondaryOnClickListener( this );

		( (TextView) findViewById( R.id.display_name ) ).setText( conversation.getPartner().getAlias() );
		( (TextView) findViewById( R.id.unread_message_count ) ).setText( Integer.toString( conversation.getUnreadMessageCount() ) );

		AsyncUtils.execute( new UpdateDisplayNameTask( conversation ), conversation.getPartner().getUsername() );

	}

	@Override
	public void setTag( Object tag ) {
		super.setTag( tag );
		if( tag instanceof Conversation ) {
			setConversation( (Conversation) tag );
		}
	}

}
