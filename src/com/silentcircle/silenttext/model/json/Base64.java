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
package com.silentcircle.silenttext.model.json;

import java.io.Closeable;
import java.io.IOException;

/**
 * <p>
 * Encodes and decodes to and from Base64 notation.
 * </p>
 * <p>
 * Homepage: <a href="http://iharder.net/base64">http://iharder.net/base64</a>.
 * </p>
 * <p>
 * Example:
 * </p>
 * <code>String encoded = Base64.encode( myByteArray );</code> <br />
 * <code>byte[] myByteArray = Base64.decode( encoded );</code>
 * <p>
 * The <tt>options</tt> parameter, which appears in a few places, is used to pass several pieces of
 * information to the encoder. In the "higher level" methods such as encodeBytes( bytes, options )
 * the options parameter can be used to indicate such things as first gzipping the bytes before
 * encoding them, not inserting linefeeds, and encoding using the URL-safe and Ordered dialects.
 * </p>
 * <p>
 * Note, according to <a href="http://www.faqs.org/rfcs/rfc3548.html">RFC3548</a>, Section 2.1,
 * implementations should not add line feeds unless explicitly told to do so. I've got Base64 set to
 * this behavior now, although earlier versions broke lines by default.
 * </p>
 * <p>
 * The constants defined in Base64 can be OR-ed together to combine options, so you might make a
 * call like this:
 * </p>
 * <code>String encoded = Base64.encodeBytes( mybytes, Base64.GZIP | Base64.DO_BREAK_LINES );</code>
 * <p>
 * to compress the data before encoding it and then making the output have newline characters.
 * </p>
 * <p>
 * Also...
 * </p>
 * <code>String encoded = Base64.encodeBytes( crazyString.getBytes() );</code>
 * <p>
 * Change Log:
 * </p>
 * <ul>
 * <li>v2.3.7 - Fixed subtle bug when base 64 input stream contained the value 01111111, which is an
 * invalid base 64 character but should not throw an ArrayIndexOutOfBoundsException either. Led to
 * discovery of mishandling (or potential for better handling) of other bad input characters. You
 * should now get an IOException if you try decoding something that has bad characters in it.</li>
 * <li>v2.3.6 - Fixed bug when breaking lines and the final byte of the encoded string ended in the
 * last column; the buffer was not properly shrunk and contained an extra (null) byte that made it
 * into the string.</li>
 * <li>v2.3.5 - Fixed bug in {@link #encodeFromFile} where estimated buffer size was wrong for files
 * of size 31, 34, and 37 bytes.</li>
 * <li>v2.3.4 - Fixed bug when working with gzipped streams whereby flushing the Base64.OutputStream
 * closed the Base64 encoding (by padding with equals signs) too soon. Also added an option to
 * suppress the automatic decoding of gzipped streams. Also added experimental support for
 * specifying a class loader when using the
 * {@link #decodeToObject(java.lang.String, int, java.lang.ClassLoader)} method.</li>
 * <li>v2.3.3 - Changed default char encoding to US-ASCII which reduces the internal Java footprint
 * with its CharEncoders and so forth. Fixed some javadocs that were inconsistent. Removed imports
 * and specified things like java.io.IOException explicitly inline.</li>
 * <li>v2.3.2 - Reduced memory footprint! Finally refined the "guessing" of how big the final
 * encoded data will be so that the code doesn't have to create two output arrays: an oversized
 * initial one and then a final, exact-sized one. Big win when using the
 * {@link #encodeBytesToBytes(byte[])} family of methods (and not using the gzip options which uses
 * a different mechanism with streams and stuff).</li>
 * <li>v2.3.1 - Added {@link #encodeBytesToBytes(byte[], int, int, int)} and some similar helper
 * methods to be more efficient with memory by not returning a String but just a byte array.</li>
 * <li>v2.3 - <strong>This is not a drop-in replacement!</strong> This is two years of comments and
 * bug fixes queued up and finally executed. Thanks to everyone who sent me stuff, and I'm sorry I
 * wasn't able to distribute your fixes to everyone else. Much bad coding was cleaned up including
 * throwing exceptions where necessary instead of returning null values or something similar. Here
 * are some changes that may affect you:
 * <ul>
 * <li><em>Does not break lines, by default.</em> This is to keep in compliance with <a
 * href="http://www.faqs.org/rfcs/rfc3548.html">RFC3548</a>.</li>
 * <li><em>Throws exceptions instead of returning null values.</em> Because some operations
 * (especially those that may permit the GZIP option) use IO streams, there is a possiblity of an
 * java.io.IOException being thrown. After some discussion and thought, I've changed the behavior of
 * the methods to throw java.io.IOExceptions rather than return null if ever there's an error. I
 * think this is more appropriate, though it will require some changes to your code. Sorry, it
 * should have been done this way to begin with.</li>
 * <li><em>Removed all references to System.out, System.err, and the like.</em> Shame on me. All I
 * can say is sorry they were ever there.</li>
 * <li><em>Throws NullPointerExceptions and IllegalArgumentExceptions</em> as needed such as when
 * passed arrays are null or offsets are invalid.</li>
 * <li>Cleaned up as much javadoc as I could to avoid any javadoc warnings. This was especially
 * annoying before for people who were thorough in their own projects and then had gobs of javadoc
 * warnings on this file.</li>
 * </ul>
 * <li>v2.2.1 - Fixed bug using URL_SAFE and ORDERED encodings. Fixed bug when using very small
 * files (~&lt; 40 bytes).</li>
 * <li>v2.2 - Added some helper methods for encoding/decoding directly from one file to the next.
 * Also added a main() method to support command line encoding/decoding from one file to the next.
 * Also added these Base64 dialects:
 * <ol>
 * <li>The default is RFC3548 format.</li>
 * <li>Calling Base64.setFormat(Base64.BASE64_FORMAT.URLSAFE_FORMAT) generates URL and file name
 * friendly format as described in Section 4 of RFC3548. http://www.faqs.org/rfcs/rfc3548.html</li>
 * <li>Calling Base64.setFormat(Base64.BASE64_FORMAT.ORDERED_FORMAT) generates URL and file name
 * friendly format that preserves lexical ordering as described in
 * http://www.faqs.org/qa/rfcc-1940.html</li>
 * </ol>
 * Special thanks to Jim Kellerman at <a
 * href="http://www.powerset.com/">http://www.powerset.com/</a> for contributing the new Base64
 * dialects.</li>
 * <li>v2.1 - Cleaned up javadoc comments and unused variables and methods. Added some convenience
 * methods for reading and writing to and from files.</li>
 * <li>v2.0.2 - Now specifies UTF-8 encoding in places where the code fails on systems with other
 * encodings (like EBCDIC).</li>
 * <li>v2.0.1 - Fixed an error when decoding a single byte, that is, when the encoded data was a
 * single byte.</li>
 * <li>v2.0 - I got rid of methods that used booleans to set options. Now everything is more
 * consolidated and cleaner. The code now detects when data that's being decoded is gzip-compressed
 * and will decompress it automatically. Generally things are cleaner. You'll probably have to
 * change some method calls that you were making to support the new options format (<tt>int</tt>s
 * that you "OR" together).</li>
 * <li>v1.5.1 - Fixed bug when decompressing and decoding to a byte[] using
 * <tt>decode( String s, boolean gzipCompressed )</tt>. Added the ability to "suspend" encoding in
 * the Output Stream so you can turn on and off the encoding if you need to embed base64 data in an
 * otherwise "normal" stream (like an XML file).</li>
 * <li>v1.5 - Output stream pases on flush() command but doesn't do anything itself. This helps when
 * using GZIP streams. Added the ability to GZip-compress objects before encoding them.</li>
 * <li>v1.4 - Added helper methods to read/write files.</li>
 * <li>v1.3.6 - Fixed OutputStream.flush() so that 'position' is reset.</li>
 * <li>v1.3.5 - Added flag to turn on and off line breaks. Fixed bug in input stream where last
 * buffer being read, if not completely full, was not returned.</li>
 * <li>v1.3.4 - Fixed when "improperly padded stream" error was thrown at the wrong time.</li>
 * <li>v1.3.3 - Fixed I/O streams which were totally messed up.</li>
 * </ul>
 * <p>
 * I am placing this code in the private Domain. Do with it as you will. This software comes with no
 * guarantees or warranties but with plenty of well-wishing instead! Please visit <a
 * href="http://iharder.net/base64">http://iharder.net/base64</a> periodically to check for updates
 * or to contribute improvements.
 * </p>
 * 
 * @author Robert Harder
 * @author rob@iharder.net
 * @version 2.3.7
 */
class Base64 {

	/**
	 * A {@link Base64.OutputStream} will write data to another <tt>java.io.OutputStream</tt>, given
	 * in the constructor, and encode/decode to/from Base64 notation on the fly.
	 * 
	 * @see Base64
	 * @since 1.3
	 */
	private static class OutputStream extends java.io.FilterOutputStream {

		private final boolean encode;
		private int position;
		private byte [] buffer;
		private final int bufferLength;
		private int lineLength;
		private final boolean breakLines;
		private final byte [] b4; // Scratch used in a few places
		private final boolean suspendEncoding;
		private final int options; // Record for later
		private final byte [] decodabet; // Local copies to avoid extra method calls

		/**
		 * Constructs a {@link Base64.OutputStream} in either ENCODE or DECODE mode.
		 * <p>
		 * Valid options:
		 * 
		 * <pre>
		 *   ENCODE or DECODE: Encode or Decode as data is read.
		 *   DO_BREAK_LINES: don't break lines at 76 characters
		 *     (only meaningful when encoding)</i>
		 * </pre>
		 * <p>
		 * Example: <code>new Base64.OutputStream( out, Base64.ENCODE )</code>
		 * 
		 * @param out
		 *            the <tt>java.io.OutputStream</tt> to which data will be written.
		 * @param options
		 *            Specified options.
		 * @see Base64#ENCODE
		 * @see Base64#DECODE
		 * @see Base64#DO_BREAK_LINES
		 * @since 1.3
		 */
		public OutputStream( java.io.OutputStream out, int options ) {
			super( out );
			breakLines = ( options & DO_BREAK_LINES ) != 0;
			encode = ( options & ENCODE ) != 0;
			bufferLength = encode ? 3 : 4;
			buffer = new byte [bufferLength];
			position = 0;
			lineLength = 0;
			suspendEncoding = false;
			b4 = new byte [4];
			this.options = options;
			decodabet = getDecodabet( options );
		}

		@Override
		public void close() throws java.io.IOException {
			flushBase64();
			super.close();
			buffer = null;
			out = null;
		}

		private void flushBase64() throws java.io.IOException {
			if( position > 0 ) {
				if( encode ) {
					out.write( encode3to4( b4, buffer, position, options ) );
					position = 0;
				} else {
					throw new java.io.IOException( "Base64 input not properly padded." );
				}
			}

		}

		/**
		 * Calls {@link #write(int)} repeatedly until <var>len</var> bytes are written.
		 * 
		 * @param theBytes
		 *            array from which to read bytes
		 * @param off
		 *            offset for array
		 * @param len
		 *            max number of bytes to read into array
		 * @since 1.3
		 */
		@Override
		public void write( byte [] theBytes, int off, int len ) throws java.io.IOException {

			if( suspendEncoding ) {
				out.write( theBytes, off, len );
				return;
			}

			for( int i = 0; i < len; i++ ) {
				write( theBytes[off + i] );
			}

		}

		/**
		 * Writes the byte to the output stream after converting to/from Base64 notation. When
		 * encoding, bytes are buffered three at a time before the output stream actually gets a
		 * write() call. When decoding, bytes are buffered four at a time.
		 * 
		 * @param theByte
		 *            the byte to write
		 * @since 1.3
		 */
		@Override
		public void write( int theByte ) throws java.io.IOException {

			if( suspendEncoding ) {
				out.write( theByte );
				return;
			}

			if( encode ) {

				buffer[position++] = (byte) theByte;

				if( position >= bufferLength ) { // Enough to encode.

					out.write( encode3to4( b4, buffer, bufferLength, options ) );

					lineLength += 4;
					if( breakLines && lineLength >= MAX_LINE_LENGTH ) {
						out.write( NEW_LINE );
						lineLength = 0;
					}

					position = 0;

				}

			} else {

				if( decodabet[theByte & 0x7f] > WHITE_SPACE_ENC ) {

					buffer[position++] = (byte) theByte;

					if( position >= bufferLength ) { // Enough to output.
						int len = decode4to3( buffer, 0, b4, 0, options );
						out.write( b4, 0, len );
						position = 0;
					}

				} else if( decodabet[theByte & 0x7f] != WHITE_SPACE_ENC ) {
					throw new java.io.IOException( "Invalid character in Base64 data." );
				}

			}

		}

	}

	private final static int NO_OPTIONS = 0;
	private final static int ENCODE = 1;
	private final static int GZIP = 2;
	private final static int DONT_GUNZIP = 4;
	private final static int DO_BREAK_LINES = 8;
	private final static int URL_SAFE = 16;
	private final static int ORDERED = 32;
	private final static int MAX_LINE_LENGTH = 76;
	private final static byte EQUALS_SIGN = (byte) '=';
	private final static byte NEW_LINE = (byte) '\n';
	private final static String PREFERRED_ENCODING = "US-ASCII";
	private final static byte WHITE_SPACE_ENC = -5;

	private final static byte EQUALS_SIGN_ENC = -1;
	private final static byte [] _STANDARD_ALPHABET = {
		(byte) 'A',
		(byte) 'B',
		(byte) 'C',
		(byte) 'D',
		(byte) 'E',
		(byte) 'F',
		(byte) 'G',
		(byte) 'H',
		(byte) 'I',
		(byte) 'J',
		(byte) 'K',
		(byte) 'L',
		(byte) 'M',
		(byte) 'N',
		(byte) 'O',
		(byte) 'P',
		(byte) 'Q',
		(byte) 'R',
		(byte) 'S',
		(byte) 'T',
		(byte) 'U',
		(byte) 'V',
		(byte) 'W',
		(byte) 'X',
		(byte) 'Y',
		(byte) 'Z',
		(byte) 'a',
		(byte) 'b',
		(byte) 'c',
		(byte) 'd',
		(byte) 'e',
		(byte) 'f',
		(byte) 'g',
		(byte) 'h',
		(byte) 'i',
		(byte) 'j',
		(byte) 'k',
		(byte) 'l',
		(byte) 'm',
		(byte) 'n',
		(byte) 'o',
		(byte) 'p',
		(byte) 'q',
		(byte) 'r',
		(byte) 's',
		(byte) 't',
		(byte) 'u',
		(byte) 'v',
		(byte) 'w',
		(byte) 'x',
		(byte) 'y',
		(byte) 'z',
		(byte) '0',
		(byte) '1',
		(byte) '2',
		(byte) '3',
		(byte) '4',
		(byte) '5',
		(byte) '6',
		(byte) '7',
		(byte) '8',
		(byte) '9',
		(byte) '+',
		(byte) '/'
	};
	private final static byte [] _STANDARD_DECODABET = {
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 0 - 8
		-5,
		-5, // Whitespace: Tab and Linefeed
		-9,
		-9, // Decimal 11 - 12
		-5, // Whitespace: Carriage Return
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 14 - 26
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 27 - 31
		-5, // Whitespace: Space
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 33 - 42
		62, // Plus sign at decimal 43
		-9,
		-9,
		-9, // Decimal 44 - 46
		63, // Slash at decimal 47
		52,
		53,
		54,
		55,
		56,
		57,
		58,
		59,
		60,
		61, // Numbers zero through nine
		-9,
		-9,
		-9, // Decimal 58 - 60
		-1, // Equals sign at decimal 61
		-9,
		-9,
		-9, // Decimal 62 - 64
		0,
		1,
		2,
		3,
		4,
		5,
		6,
		7,
		8,
		9,
		10,
		11,
		12,
		13, // Letters 'A' through 'N'
		14,
		15,
		16,
		17,
		18,
		19,
		20,
		21,
		22,
		23,
		24,
		25, // Letters 'O' through 'Z'
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 91 - 96
		26,
		27,
		28,
		29,
		30,
		31,
		32,
		33,
		34,
		35,
		36,
		37,
		38, // Letters 'a' through 'm'
		39,
		40,
		41,
		42,
		43,
		44,
		45,
		46,
		47,
		48,
		49,
		50,
		51, // Letters 'n' through 'z'
		-9,
		-9,
		-9,
		-9,
		-9 // Decimal 123 - 127
		,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 128 - 139
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 140 - 152
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 153 - 165
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 166 - 178
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 179 - 191
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 192 - 204
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 205 - 217
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 218 - 230
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 231 - 243
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9
	// Decimal 244 - 255
	};
	private final static byte [] _URL_SAFE_ALPHABET = {
		(byte) 'A',
		(byte) 'B',
		(byte) 'C',
		(byte) 'D',
		(byte) 'E',
		(byte) 'F',
		(byte) 'G',
		(byte) 'H',
		(byte) 'I',
		(byte) 'J',
		(byte) 'K',
		(byte) 'L',
		(byte) 'M',
		(byte) 'N',
		(byte) 'O',
		(byte) 'P',
		(byte) 'Q',
		(byte) 'R',
		(byte) 'S',
		(byte) 'T',
		(byte) 'U',
		(byte) 'V',
		(byte) 'W',
		(byte) 'X',
		(byte) 'Y',
		(byte) 'Z',
		(byte) 'a',
		(byte) 'b',
		(byte) 'c',
		(byte) 'd',
		(byte) 'e',
		(byte) 'f',
		(byte) 'g',
		(byte) 'h',
		(byte) 'i',
		(byte) 'j',
		(byte) 'k',
		(byte) 'l',
		(byte) 'm',
		(byte) 'n',
		(byte) 'o',
		(byte) 'p',
		(byte) 'q',
		(byte) 'r',
		(byte) 's',
		(byte) 't',
		(byte) 'u',
		(byte) 'v',
		(byte) 'w',
		(byte) 'x',
		(byte) 'y',
		(byte) 'z',
		(byte) '0',
		(byte) '1',
		(byte) '2',
		(byte) '3',
		(byte) '4',
		(byte) '5',
		(byte) '6',
		(byte) '7',
		(byte) '8',
		(byte) '9',
		(byte) '-',
		(byte) '_'
	};
	private final static byte [] _URL_SAFE_DECODABET = {
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 0 - 8
		-5,
		-5, // Whitespace: Tab and Linefeed
		-9,
		-9, // Decimal 11 - 12
		-5, // Whitespace: Carriage Return
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 14 - 26
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 27 - 31
		-5, // Whitespace: Space
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 33 - 42
		-9, // Plus sign at decimal 43
		-9, // Decimal 44
		62, // Minus sign at decimal 45
		-9, // Decimal 46
		-9, // Slash at decimal 47
		52,
		53,
		54,
		55,
		56,
		57,
		58,
		59,
		60,
		61, // Numbers zero through nine
		-9,
		-9,
		-9, // Decimal 58 - 60
		-1, // Equals sign at decimal 61
		-9,
		-9,
		-9, // Decimal 62 - 64
		0,
		1,
		2,
		3,
		4,
		5,
		6,
		7,
		8,
		9,
		10,
		11,
		12,
		13, // Letters 'A' through 'N'
		14,
		15,
		16,
		17,
		18,
		19,
		20,
		21,
		22,
		23,
		24,
		25, // Letters 'O' through 'Z'
		-9,
		-9,
		-9,
		-9, // Decimal 91 - 94
		63, // Underscore at decimal 95
		-9, // Decimal 96
		26,
		27,
		28,
		29,
		30,
		31,
		32,
		33,
		34,
		35,
		36,
		37,
		38, // Letters 'a' through 'm'
		39,
		40,
		41,
		42,
		43,
		44,
		45,
		46,
		47,
		48,
		49,
		50,
		51, // Letters 'n' through 'z'
		-9,
		-9,
		-9,
		-9,
		-9 // Decimal 123 - 127
		,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 128 - 139
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 140 - 152
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 153 - 165
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 166 - 178
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 179 - 191
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 192 - 204
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 205 - 217
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 218 - 230
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 231 - 243
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9
	// Decimal 244 - 255
	};
	private final static byte [] _ORDERED_ALPHABET = {
		(byte) '-',
		(byte) '0',
		(byte) '1',
		(byte) '2',
		(byte) '3',
		(byte) '4',
		(byte) '5',
		(byte) '6',
		(byte) '7',
		(byte) '8',
		(byte) '9',
		(byte) 'A',
		(byte) 'B',
		(byte) 'C',
		(byte) 'D',
		(byte) 'E',
		(byte) 'F',
		(byte) 'G',
		(byte) 'H',
		(byte) 'I',
		(byte) 'J',
		(byte) 'K',
		(byte) 'L',
		(byte) 'M',
		(byte) 'N',
		(byte) 'O',
		(byte) 'P',
		(byte) 'Q',
		(byte) 'R',
		(byte) 'S',
		(byte) 'T',
		(byte) 'U',
		(byte) 'V',
		(byte) 'W',
		(byte) 'X',
		(byte) 'Y',
		(byte) 'Z',
		(byte) '_',
		(byte) 'a',
		(byte) 'b',
		(byte) 'c',
		(byte) 'd',
		(byte) 'e',
		(byte) 'f',
		(byte) 'g',
		(byte) 'h',
		(byte) 'i',
		(byte) 'j',
		(byte) 'k',
		(byte) 'l',
		(byte) 'm',
		(byte) 'n',
		(byte) 'o',
		(byte) 'p',
		(byte) 'q',
		(byte) 'r',
		(byte) 's',
		(byte) 't',
		(byte) 'u',
		(byte) 'v',
		(byte) 'w',
		(byte) 'x',
		(byte) 'y',
		(byte) 'z'
	};

	private final static byte [] _ORDERED_DECODABET = {
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 0 - 8
		-5,
		-5, // Whitespace: Tab and Linefeed
		-9,
		-9, // Decimal 11 - 12
		-5, // Whitespace: Carriage Return
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 14 - 26
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 27 - 31
		-5, // Whitespace: Space
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 33 - 42
		-9, // Plus sign at decimal 43
		-9, // Decimal 44
		0, // Minus sign at decimal 45
		-9, // Decimal 46
		-9, // Slash at decimal 47
		1,
		2,
		3,
		4,
		5,
		6,
		7,
		8,
		9,
		10, // Numbers zero through nine
		-9,
		-9,
		-9, // Decimal 58 - 60
		-1, // Equals sign at decimal 61
		-9,
		-9,
		-9, // Decimal 62 - 64
		11,
		12,
		13,
		14,
		15,
		16,
		17,
		18,
		19,
		20,
		21,
		22,
		23, // Letters 'A' through 'M'
		24,
		25,
		26,
		27,
		28,
		29,
		30,
		31,
		32,
		33,
		34,
		35,
		36, // Letters 'N' through 'Z'
		-9,
		-9,
		-9,
		-9, // Decimal 91 - 94
		37, // Underscore at decimal 95
		-9, // Decimal 96
		38,
		39,
		40,
		41,
		42,
		43,
		44,
		45,
		46,
		47,
		48,
		49,
		50, // Letters 'a' through 'm'
		51,
		52,
		53,
		54,
		55,
		56,
		57,
		58,
		59,
		60,
		61,
		62,
		63, // Letters 'n' through 'z'
		-9,
		-9,
		-9,
		-9,
		-9 // Decimal 123 - 127
		,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 128 - 139
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 140 - 152
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 153 - 165
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 166 - 178
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 179 - 191
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 192 - 204
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 205 - 217
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 218 - 230
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 231 - 243
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9
	// Decimal 244 - 255
	};

	protected static void close( Closeable... closeables ) {
		for( int i = 0; i < closeables.length; i++ ) {
			Closeable closeable = closeables[i];
			if( closeable != null ) {
				try {
					closeable.close();
				} catch( IOException exception ) {
					// Ignore it.
				}
			}
		}
	}

	/**
	 * Low-level access to decoding ASCII characters in the form of a byte array. <strong>Ignores
	 * GUNZIP option, if it's set.</strong> This is not generally a recommended method, although it
	 * is used internally as part of the decoding process. Special case: if len = 0, an empty array
	 * is returned. Still, if you need more speed and reduced memory footprint (and aren't
	 * gzipping), consider this method.
	 * 
	 * @param source
	 *            The Base64 encoded data
	 * @param off
	 *            The offset of where to begin decoding
	 * @param len
	 *            The length of characters to decode
	 * @param options
	 *            Can specify options such as alphabet type to use
	 * @return decoded data
	 * @throws java.io.IOException
	 *             If bogus characters exist in source data
	 * @since 1.3
	 */
	private static byte [] decode( byte [] source, int off, int len, int options ) throws java.io.IOException {

		// Lots of error checking and exception throwing
		if( source == null ) {
			throw new NullPointerException( "Cannot decode null source array." );
		}
		if( off < 0 || off + len > source.length ) {
			throw new IllegalArgumentException( String.format( "Source array with length %d cannot have offset of %d and process %d bytes.", Integer.valueOf( source.length ), Integer.valueOf( off ), Integer.valueOf( len ) ) );
		}

		if( len == 0 ) {
			return new byte [0];
		} else if( len < 4 ) {
			throw new IllegalArgumentException( "Base64-encoded string must have at least four characters, but length specified was " + len );
		}

		byte [] DECODABET = getDecodabet( options );

		int len34 = len * 3 / 4; // Estimate on array size
		byte [] outBuff = new byte [len34]; // Upper limit on size of output
		int outBuffPosn = 0; // Keep track of where we're writing

		byte [] b4 = new byte [4]; // Four byte buffer from source, eliminating white space
		int b4Posn = 0; // Keep track of four byte input buffer
		int i = 0; // Source array counter
		byte sbiDecode = 0; // Special value from DECODABET

		for( i = off; i < off + len; i++ ) { // Loop through source

			sbiDecode = DECODABET[source[i] & 0xFF];

			// White space, Equals sign, or legit Base64 character
			// Note the values such as -5 and -9 in the
			// DECODABETs at the top of the file.
			if( sbiDecode >= WHITE_SPACE_ENC ) {
				if( sbiDecode >= EQUALS_SIGN_ENC ) {
					b4[b4Posn++] = source[i]; // Save non-whitespace
					if( b4Posn > 3 ) { // Time to decode?
						outBuffPosn += decode4to3( b4, 0, outBuff, outBuffPosn, options );
						b4Posn = 0;

						// If that was the equals sign, break out of 'for' loop
						if( source[i] == EQUALS_SIGN ) {
							break;
						}
					}
				}
			} else {
				// There's a bad input character in the Base64 stream.
				throw new java.io.IOException( String.format( "Bad Base64 input character decimal %d in array position %d", Integer.valueOf( source[i] & 0xFF ), Integer.valueOf( i ) ) );
			}
		} // each input character

		byte [] out = new byte [outBuffPosn];
		System.arraycopy( outBuff, 0, out, 0, outBuffPosn );
		return out;
	}

	/**
	 * Decodes data from Base64 notation, automatically detecting gzip-compressed data and
	 * decompressing it.
	 * 
	 * @param s
	 *            the string to decode
	 * @return the decoded data
	 * @throws java.io.IOException
	 *             If there is a problem
	 * @since 1.4
	 */
	public static byte [] decode( String s ) throws java.io.IOException {
		return decode( s, NO_OPTIONS );
	}

	/**
	 * Decodes data from Base64 notation, automatically detecting gzip-compressed data and
	 * decompressing it.
	 * 
	 * @param s
	 *            the string to decode
	 * @param options
	 *            encode options such as URL_SAFE
	 * @return the decoded data
	 * @throws java.io.IOException
	 *             if there is an error
	 * @throws NullPointerException
	 *             if <tt>s</tt> is null
	 * @since 1.4
	 */
	private static byte [] decode( String s, int options ) throws java.io.IOException {

		if( s == null ) {
			throw new NullPointerException( "Input string was null." );
		}

		byte [] bytes;
		try {
			bytes = s.getBytes( PREFERRED_ENCODING );
		} catch( java.io.UnsupportedEncodingException uee ) {
			bytes = s.getBytes();
		}
		// </change>

		// Decode
		bytes = decode( bytes, 0, bytes.length, options );

		// Check to see if it's gzip-compressed
		// GZIP Magic Two-Byte Number: 0x8b1f (35615)
		boolean dontGunzip = ( options & DONT_GUNZIP ) != 0;
		if( bytes != null && bytes.length >= 4 && !dontGunzip ) {

			int head = bytes[0] & 0xff | bytes[1] << 8 & 0xff00;
			if( java.util.zip.GZIPInputStream.GZIP_MAGIC == head ) {
				java.io.ByteArrayInputStream bais = null;
				java.util.zip.GZIPInputStream gzis = null;
				java.io.ByteArrayOutputStream baos = null;
				byte [] buffer = new byte [2048];
				int length = 0;

				try {
					baos = new java.io.ByteArrayOutputStream();
					bais = new java.io.ByteArrayInputStream( bytes );
					gzis = new java.util.zip.GZIPInputStream( bais );

					while( ( length = gzis.read( buffer ) ) >= 0 ) {
						baos.write( buffer, 0, length );
					}

					// No error? Get new bytes.
					bytes = baos.toByteArray();

				} catch( java.io.IOException e ) {
					e.printStackTrace();
					// Just return originally-decoded bytes
				} finally {
					close( baos, gzis, bais );
				}

			}
		}

		return bytes;
	}

	/**
	 * Decodes four bytes from array <var>source</var> and writes the resulting bytes (up to three
	 * of them) to <var>destination</var>. The source and destination arrays can be manipulated
	 * anywhere along their length by specifying <var>srcOffset</var> and <var>destOffset</var>.
	 * This method does not check to make sure your arrays are large enough to accomodate
	 * <var>srcOffset</var> + 4 for the <var>source</var> array or <var>destOffset</var> + 3 for the
	 * <var>destination</var> array. This method returns the actual number of bytes that were
	 * converted from the Base64 encoding.
	 * <p>
	 * This is the lowest level of the decoding methods with all possible parameters.
	 * </p>
	 * 
	 * @param source
	 *            the array to convert
	 * @param srcOffset
	 *            the index where conversion begins
	 * @param destination
	 *            the array to hold the conversion
	 * @param destOffset
	 *            the index where output will be put
	 * @param options
	 *            alphabet type is pulled from this (standard, url-safe, ordered)
	 * @return the number of decoded bytes converted
	 * @throws NullPointerException
	 *             if source or destination arrays are null
	 * @throws IllegalArgumentException
	 *             if srcOffset or destOffset are invalid or there is not enough room in the array.
	 * @since 1.3
	 */
	protected static int decode4to3( byte [] source, int srcOffset, byte [] destination, int destOffset, int options ) {

		// Lots of error checking and exception throwing
		if( source == null ) {
			throw new NullPointerException( "Source array was null." );
		}
		if( destination == null ) {
			throw new NullPointerException( "Destination array was null." );
		}
		if( srcOffset < 0 || srcOffset + 3 >= source.length ) {
			throw new IllegalArgumentException( String.format( "Source array with length %d cannot have offset of %d and still process four bytes.", Integer.valueOf( source.length ), Integer.valueOf( srcOffset ) ) );
		}
		if( destOffset < 0 || destOffset + 2 >= destination.length ) {
			throw new IllegalArgumentException( String.format( "Destination array with length %d cannot have offset of %d and still store three bytes.", Integer.valueOf( destination.length ), Integer.valueOf( destOffset ) ) );
		}

		byte [] DECODABET = getDecodabet( options );

		// Example: Dk==
		if( source[srcOffset + 2] == EQUALS_SIGN ) {
			// Two ways to do the same thing. Don't know which way I like best.
			// int outBuff = ( ( DECODABET[ source[ srcOffset ] ] << 24 ) >>> 6 )
			// | ( ( DECODABET[ source[ srcOffset + 1] ] << 24 ) >>> 12 );
			int outBuff = ( DECODABET[source[srcOffset]] & 0xFF ) << 18 | ( DECODABET[source[srcOffset + 1]] & 0xFF ) << 12;

			destination[destOffset] = (byte) ( outBuff >>> 16 );
			return 1;
		}

		// Example: DkL=
		else if( source[srcOffset + 3] == EQUALS_SIGN ) {
			// Two ways to do the same thing. Don't know which way I like best.
			// int outBuff = ( ( DECODABET[ source[ srcOffset ] ] << 24 ) >>> 6 )
			// | ( ( DECODABET[ source[ srcOffset + 1 ] ] << 24 ) >>> 12 )
			// | ( ( DECODABET[ source[ srcOffset + 2 ] ] << 24 ) >>> 18 );
			int outBuff = ( DECODABET[source[srcOffset]] & 0xFF ) << 18 | ( DECODABET[source[srcOffset + 1]] & 0xFF ) << 12 | ( DECODABET[source[srcOffset + 2]] & 0xFF ) << 6;

			destination[destOffset] = (byte) ( outBuff >>> 16 );
			destination[destOffset + 1] = (byte) ( outBuff >>> 8 );
			return 2;
		}

		// Example: DkLE
		else {
			// Two ways to do the same thing. Don't know which way I like best.
			// int outBuff = ( ( DECODABET[ source[ srcOffset ] ] << 24 ) >>> 6 )
			// | ( ( DECODABET[ source[ srcOffset + 1 ] ] << 24 ) >>> 12 )
			// | ( ( DECODABET[ source[ srcOffset + 2 ] ] << 24 ) >>> 18 )
			// | ( ( DECODABET[ source[ srcOffset + 3 ] ] << 24 ) >>> 24 );
			int outBuff = ( DECODABET[source[srcOffset]] & 0xFF ) << 18 | ( DECODABET[source[srcOffset + 1]] & 0xFF ) << 12 | ( DECODABET[source[srcOffset + 2]] & 0xFF ) << 6 | DECODABET[source[srcOffset + 3]] & 0xFF;

			destination[destOffset] = (byte) ( outBuff >> 16 );
			destination[destOffset + 1] = (byte) ( outBuff >> 8 );
			destination[destOffset + 2] = (byte) outBuff;

			return 3;
		}
	}

	protected static byte [] encode3to4( byte [] b4, byte [] threeBytes, int numSigBytes, int options ) {
		encode3to4( threeBytes, 0, numSigBytes, b4, 0, options );
		return b4;
	}

	private static byte [] encode3to4( byte [] source, int srcOffset, int numSigBytes, byte [] destination, int destOffset, int options ) {

		byte [] ALPHABET = getAlphabet( options );

		int inBuff = ( numSigBytes > 0 ? source[srcOffset] << 24 >>> 8 : 0 ) | ( numSigBytes > 1 ? source[srcOffset + 1] << 24 >>> 16 : 0 ) | ( numSigBytes > 2 ? source[srcOffset + 2] << 24 >>> 24 : 0 );

		switch( numSigBytes ) {
			case 3:
				destination[destOffset] = ALPHABET[inBuff >>> 18];
				destination[destOffset + 1] = ALPHABET[inBuff >>> 12 & 0x3f];
				destination[destOffset + 2] = ALPHABET[inBuff >>> 6 & 0x3f];
				destination[destOffset + 3] = ALPHABET[inBuff & 0x3f];
				return destination;

			case 2:
				destination[destOffset] = ALPHABET[inBuff >>> 18];
				destination[destOffset + 1] = ALPHABET[inBuff >>> 12 & 0x3f];
				destination[destOffset + 2] = ALPHABET[inBuff >>> 6 & 0x3f];
				destination[destOffset + 3] = EQUALS_SIGN;
				return destination;

			case 1:
				destination[destOffset] = ALPHABET[inBuff >>> 18];
				destination[destOffset + 1] = ALPHABET[inBuff >>> 12 & 0x3f];
				destination[destOffset + 2] = EQUALS_SIGN;
				destination[destOffset + 3] = EQUALS_SIGN;
				return destination;

			default:
				return destination;
		}
	}

	/* ******** D E C O D I N G M E T H O D S ******** */

	public static String encodeBytes( byte [] source ) {
		// Since we're not going to have the GZIP encoding turned on,
		// we're not going to have an java.io.IOException thrown, so
		// we should not force the user to have to catch it.
		String encoded = null;
		try {
			encoded = encodeBytes( source, 0, source.length, NO_OPTIONS );
		} catch( java.io.IOException ex ) {
			assert false : ex.getMessage();
		}
		assert encoded != null;
		return encoded;
	}

	/**
	 * Encodes a byte array into Base64 notation.
	 * <p>
	 * Example options:
	 * 
	 * <pre>
	 *   GZIP: gzip-compresses object before encoding it.
	 *   DO_BREAK_LINES: break lines at 76 characters
	 *     <i>Note: Technically, this makes your encoding non-compliant.</i>
	 * </pre>
	 * <p>
	 * Example: <code>encodeBytes( myData, Base64.GZIP )</code> or
	 * <p>
	 * Example: <code>encodeBytes( myData, Base64.GZIP | Base64.DO_BREAK_LINES )</code>
	 * <p>
	 * As of v 2.3, if there is an error with the GZIP stream, the method will throw an
	 * java.io.IOException. <b>This is new to v2.3!</b> In earlier versions, it just returned a null
	 * value, but in retrospect that's a pretty poor way to handle it.
	 * </p>
	 * 
	 * @param source
	 *            The data to convert
	 * @param off
	 *            Offset in array where conversion should begin
	 * @param len
	 *            Length of data to convert
	 * @param options
	 *            Specified options
	 * @return The Base64-encoded data as a String
	 * @see Base64#GZIP
	 * @see Base64#DO_BREAK_LINES
	 * @throws java.io.IOException
	 *             if there is an error
	 * @throws NullPointerException
	 *             if source array is null
	 * @throws IllegalArgumentException
	 *             if source array, offset, or length are invalid
	 * @since 2.0
	 */
	private static String encodeBytes( byte [] source, int off, int len, int options ) throws java.io.IOException {
		byte [] encoded = encodeBytesToBytes( source, off, len, options );

		// Return value according to relevant encoding.
		try {
			return new String( encoded, PREFERRED_ENCODING );
		} catch( java.io.UnsupportedEncodingException uue ) {
			return new String( encoded );
		}

	}

	/**
	 * Similar to {@link #encodeBytes(byte[], int, int, int)} but returns a byte array instead of
	 * instantiating a String. This is more efficient if you're working with I/O streams and have
	 * large data sets to encode.
	 * 
	 * @param source
	 *            The data to convert
	 * @param off
	 *            Offset in array where conversion should begin
	 * @param len
	 *            Length of data to convert
	 * @param options
	 *            Specified options
	 * @return The Base64-encoded data as a String
	 * @see Base64#GZIP
	 * @see Base64#DO_BREAK_LINES
	 * @throws java.io.IOException
	 *             if there is an error
	 * @throws NullPointerException
	 *             if source array is null
	 * @throws IllegalArgumentException
	 *             if source array, offset, or length are invalid
	 * @since 2.3.1
	 */
	private static byte [] encodeBytesToBytes( byte [] source, int off, int len, int options ) throws java.io.IOException {

		if( source == null ) {
			throw new NullPointerException( "Cannot serialize a null array." );
		}

		if( off < 0 ) {
			throw new IllegalArgumentException( "Cannot have negative offset: " + off );
		}

		if( len < 0 ) {
			throw new IllegalArgumentException( "Cannot have length offset: " + len );
		}

		if( off + len > source.length ) {
			throw new IllegalArgumentException( String.format( "Cannot have offset of %d and length of %d with array of length %d", Integer.valueOf( off ), Integer.valueOf( len ), Integer.valueOf( source.length ) ) );
		}

		// Compress?
		if( ( options & GZIP ) != 0 ) {
			java.io.ByteArrayOutputStream baos = null;
			java.util.zip.GZIPOutputStream gzos = null;
			Base64.OutputStream b64os = null;

			try {
				// GZip -> Base64 -> ByteArray
				baos = new java.io.ByteArrayOutputStream();
				b64os = new Base64.OutputStream( baos, ENCODE | options );
				gzos = new java.util.zip.GZIPOutputStream( b64os );

				gzos.write( source, off, len );
				gzos.close();
			} catch( java.io.IOException e ) {
				// Catch it and then throw it immediately so that
				// the finally{} block is called for cleanup.
				throw e;
			} finally {
				close( gzos, b64os, baos );
			}

			return baos.toByteArray();
		}
		boolean breakLines = ( options & DO_BREAK_LINES ) != 0;

		// int len43 = len * 4 / 3;
		// byte[] outBuff = new byte[ ( len43 ) // Main 4:3
		// + ( (len % 3) > 0 ? 4 : 0 ) // Account for padding
		// + (breakLines ? ( len43 / MAX_LINE_LENGTH ) : 0) ]; // New lines
		// Try to determine more precisely how big the array needs to be.
		// If we get it right, we don't have to do an array copy, and
		// we save a bunch of memory.
		int encLen = len / 3 * 4 + ( len % 3 > 0 ? 4 : 0 ); // Bytes needed for actual
															// encoding
		if( breakLines ) {
			encLen += encLen / MAX_LINE_LENGTH; // Plus extra newline characters
		}
		byte [] outBuff = new byte [encLen];

		int d = 0;
		int e = 0;
		int len2 = len - 2;
		int lineLength = 0;
		for( ; d < len2; d += 3, e += 4 ) {
			encode3to4( source, d + off, 3, outBuff, e, options );

			lineLength += 4;
			if( breakLines && lineLength >= MAX_LINE_LENGTH ) {
				outBuff[e + 4] = NEW_LINE;
				e++;
				lineLength = 0;
			}
		} // en dfor: each piece of array

		if( d < len ) {
			encode3to4( source, d + off, len - d, outBuff, e, options );
			e += 4;
		}

		// Only resize array if we didn't guess it right.
		if( e <= outBuff.length - 1 ) {
			// If breaking lines and the last byte falls right at
			// the line length (76 bytes per line), there will be
			// one extra byte, and the array will need to be resized.
			// Not too bad of an estimate on array size, I'd say.
			byte [] finalOut = new byte [e];
			System.arraycopy( outBuff, 0, finalOut, 0, e );
			// System.err.println("Having to resize array from " + outBuff.length + " to " + e
			// );
			return finalOut;
		}
		// System.err.println("No need to resize array.");
		return outBuff;

	}

	private final static byte [] getAlphabet( int options ) {
		if( ( options & URL_SAFE ) == URL_SAFE ) {
			return _URL_SAFE_ALPHABET;
		} else if( ( options & ORDERED ) == ORDERED ) {
			return _ORDERED_ALPHABET;
		} else {
			return _STANDARD_ALPHABET;
		}
	}

	protected final static byte [] getDecodabet( int options ) {
		if( ( options & URL_SAFE ) == URL_SAFE ) {
			return _URL_SAFE_DECODABET;
		} else if( ( options & ORDERED ) == ORDERED ) {
			return _ORDERED_DECODABET;
		} else {
			return _STANDARD_DECODABET;
		}
	}

	private Base64() {
		// Prevents this object from being instantiated.
	}

}
