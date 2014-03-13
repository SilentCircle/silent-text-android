/*
Copyright © 2012-2013, Silent Circle, LLC.  All rights reserved.

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
package com.silentcircle.scimp;

public class NativeSCimpContext {

	public NativeSCimpContext() {
		initialize();
	}

	public native void create( byte [] storageKey, String localUserID, String remoteUserID );

	private native void initialize();

	public native boolean isSecure();

	public void onAdviseSaveState() {
		// Blah.
	}

	/**
	 * @param errorCode
	 */
	public void onError( int errorCode ) {
		// Blah.
	}

	/**
	 * @param sas
	 */
	public void onKeyExchangeCompleted( String sas ) {
		// Blah.
	}

	/**
	 * @param packet
	 */
	public void onReceivePacket( byte [] packet ) {
		// Blah.
	}

	/**
	 * @param packet
	 */
	public void onSendPacket( byte [] packet ) {
		// Blah.
	}

	/**
	 * @param warningCode
	 */
	public void onWarning( int warningCode ) {
		// Blah.
	}

	public native void receivePacket( byte [] packet );

	public native void restore( byte [] storageKey, byte [] state );

	public native byte [] save();

	public native void sendPacket( byte [] packet );

	public native void setPrivateKey( byte [] storageKey, byte [] privateKey );

	public native void setPublicKey( byte [] publicKey );

}
