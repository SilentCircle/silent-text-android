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
package it.sephiroth.android.library.imagezoom.easing;

public class Elastic implements Easing {

	@Override
	public double easeIn( double time, double start, double end, double duration ) {
		return easeIn( time, start, end, duration, start + end, duration );
	}

	public double easeIn( double t, double b, double c, double d, double a, double p ) {
		double s;
		if ( t == 0 ) return b;
		if ( ( t /= d ) == 1 ) return b + c;
		if ( !( p > 0 ) ) p = d * .3;
		if ( !( a > 0 ) || a < Math.abs( c ) ) {
			a = c;
			s = p / 4;
		} else
			s = p / ( 2 * Math.PI ) * Math.asin( c / a );
		return -( a * Math.pow( 2, 10 * ( t -= 1 ) ) * Math.sin( ( t * d - s ) * ( 2 * Math.PI ) / p ) ) + b;
	}

	@Override
	public double easeOut( double time, double start, double end, double duration ) {
		return easeOut( time, start, end, duration, start + end, duration );
	}

	public double easeOut( double t, double b, double c, double d, double a, double p ) {
		double s;
		if ( t == 0 ) return b;
		if ( ( t /= d ) == 1 ) return b + c;
		if ( !( p > 0 ) ) p = d * .3;
		if ( !( a > 0 ) || a < Math.abs( c ) ) {
			a = c;
			s = p / 4;
		} else
			s = p / ( 2 * Math.PI ) * Math.asin( c / a );
		return ( a * Math.pow( 2, -10 * t ) * Math.sin( ( t * d - s ) * ( 2 * Math.PI ) / p ) + c + b );
	}

	@Override
	public double easeInOut( double t, double b, double c, double d ) {
		return easeInOut( t, b, c, d, b + c, d );
	}

	public double easeInOut( double t, double b, double c, double d, double a, double p ) {
		double s;

		if ( t == 0 ) return b;
		if ( ( t /= d / 2 ) == 2 ) return b + c;
		if ( !( p > 0 ) ) p = d * ( .3 * 1.5 );
		if ( !( a > 0 ) || a < Math.abs( c ) ) {
			a = c;
			s = p / 4;
		} else
			s = p / ( 2 * Math.PI ) * Math.asin( c / a );
		if ( t < 1 ) return -.5 * ( a * Math.pow( 2, 10 * ( t -= 1 ) ) * Math.sin( ( t * d - s ) * ( 2 * Math.PI ) / p ) ) + b;
		return a * Math.pow( 2, -10 * ( t -= 1 ) ) * Math.sin( ( t * d - s ) * ( 2 * Math.PI ) / p ) * .5 + c + b;
	}
}
