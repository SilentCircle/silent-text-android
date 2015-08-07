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
package com.silentcircle.silenttext.client.model.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.silentcircle.silentstorage.io.Serializer;
import com.silentcircle.silenttext.client.model.ServiceEndpoint;

public class ServiceEndpointSerializer extends Serializer<ServiceEndpoint> {

	private static final int VERSION = 2;

	@Override
	public ServiceEndpoint read( DataInputStream in ) throws IOException {
		return read( in, new ServiceEndpoint() );
	}

	@Override
	public ServiceEndpoint read( DataInputStream in, ServiceEndpoint out ) throws IOException {

		int version = in.readInt();

		switch( version ) {

			case 2:

				out.serviceName = readString( in );
				out.host = readString( in );
				out.port = in.readInt();

				break;

			case 1:

				char [] temp = readChars( in );
				out.serviceName = temp != null ? new String( temp ) : null;

				temp = readChars( in );
				out.host = temp != null ? new String( temp ) : null;

				out.port = in.readInt();

				break;

		}

		return out;

	}

	@Override
	public ServiceEndpoint write( ServiceEndpoint in, DataOutputStream out ) throws IOException {

		out.writeInt( VERSION );

		writeString( in.serviceName, out );
		writeString( in.host, out );
		out.writeInt( in.port );

		return in;

	}

}
