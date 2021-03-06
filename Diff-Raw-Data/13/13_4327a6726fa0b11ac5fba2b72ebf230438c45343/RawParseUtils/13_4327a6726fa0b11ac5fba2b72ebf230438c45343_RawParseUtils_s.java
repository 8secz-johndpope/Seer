 /*
  * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
  *
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or
  * without modification, are permitted provided that the following
  * conditions are met:
  *
  * - Redistributions of source code must retain the above copyright
  *   notice, this list of conditions and the following disclaimer.
  *
  * - Redistributions in binary form must reproduce the above
  *   copyright notice, this list of conditions and the following
  *   disclaimer in the documentation and/or other materials provided
  *   with the distribution.
  *
  * - Neither the name of the Git Development Community nor the
  *   names of its contributors may be used to endorse or promote
  *   products derived from this software without specific prior
  *   written permission.
  *
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
  * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
  * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
  * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
  * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
  * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
  * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
  * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
  * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
  * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
  * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
  * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
  * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  */
 
 package org.spearce.jgit.util;
 
 import java.nio.ByteBuffer;
 import java.nio.charset.Charset;
 import java.util.Arrays;
 
 import org.spearce.jgit.lib.Constants;
 import org.spearce.jgit.lib.PersonIdent;
 
 /** Handy utility functions to parse raw object contents. */
 public final class RawParseUtils {
 	private static final byte[] author = Constants.encodeASCII("author ");
 
 	private static final byte[] committer = Constants.encodeASCII("committer ");
 
 	private static final byte[] encoding = Constants.encodeASCII("encoding ");
 
 	private static final byte[] digits;
 
 	static {
 		digits = new byte['9' + 1];
 		Arrays.fill(digits, (byte) -1);
 		for (char i = '0'; i <= '9'; i++)
 			digits[i] = (byte) (i - '0');
 	}
 
 	private static final int match(final byte[] b, int ptr, final byte[] src) {
 		if (ptr + src.length >= b.length)
 			return -1;
 		for (int i = 0; i < src.length; i++, ptr++)
 			if (b[ptr] != src[i])
 				return -1;
 		return ptr;
 	}
 
 	private static final byte[] base10byte = { '0', '1', '2', '3', '4', '5',
 			'6', '7', '8', '9' };
 
 	/**
 	 * Format a base 10 numeric into a temporary buffer.
 	 * <p>
 	 * Formatting is performed backwards. The method starts at offset
 	 * <code>o-1</code> and ends at <code>o-1-digits</code>, where
 	 * <code>digits</code> is the number of positions necessary to store the
 	 * base 10 value.
 	 * <p>
 	 * The argument and return values from this method make it easy to chain
 	 * writing, for example:
 	 * </p>
 	 * 
 	 * <pre>
 	 * final byte[] tmp = new byte[64];
 	 * int ptr = tmp.length;
 	 * tmp[--ptr] = '\n';
 	 * ptr = RawParseUtils.formatBase10(tmp, ptr, 32);
 	 * tmp[--ptr] = ' ';
 	 * ptr = RawParseUtils.formatBase10(tmp, ptr, 18);
 	 * tmp[--ptr] = 0;
 	 * final String str = new String(tmp, ptr, tmp.length - ptr);
 	 * </pre>
 	 * 
 	 * @param b
 	 *            buffer to write into.
 	 * @param o
 	 *            one offset past the location where writing will begin; writing
 	 *            proceeds towards lower index values.
 	 * @param value
 	 *            the value to store.
 	 * @return the new offset value <code>o</code>. This is the position of
 	 *         the last byte written. Additional writing should start at one
 	 *         position earlier.
 	 */
 	public static int formatBase10(final byte[] b, int o, int value) {
 		if (value == 0) {
 			b[--o] = '0';
 			return o;
 		}
 		final boolean isneg = value < 0;
 		while (value != 0) {
 			b[--o] = base10byte[value % 10];
 			value /= 10;
 		}
 		if (isneg)
 			b[--o] = '-';
 		return o;
 	}
 
 	/**
 	 * Parse a base 10 numeric from a sequence of ASCII digits.
 	 * <p>
 	 * Digit sequences can begin with an optional run of spaces before the
 	 * sequence, and may start with a '+' or a '-' to indicate sign position.
 	 * Any other characters will cause the method to stop and return the current
 	 * result to the caller.
 	 * 
 	 * @param b
 	 *            buffer to scan.
 	 * @param ptr
 	 *            position within buffer to start parsing digits at.
 	 * @param ptrResult
 	 *            optional location to return the new ptr value through. If null
 	 *            the ptr value will be discarded.
 	 * @return the value at this location; 0 if the location is not a valid
 	 *         numeric.
 	 */
 	public static final int parseBase10(final byte[] b, int ptr,
 			final MutableInteger ptrResult) {
 		int r = 0;
 		int sign = 0;
 		try {
 			final int sz = b.length;
 			while (ptr < sz && b[ptr] == ' ')
 				ptr++;
 			if (ptr >= sz)
 				return 0;
 
 			switch (b[ptr]) {
 			case '-':
 				sign = -1;
 				ptr++;
 				break;
 			case '+':
 				ptr++;
 				break;
 			}
 
 			while (ptr < sz) {
 				final byte v = digits[b[ptr]];
 				if (v < 0)
 					break;
 				r = (r * 10) + v;
 				ptr++;
 			}
 		} catch (ArrayIndexOutOfBoundsException e) {
 			// Not a valid digit.
 		}
 		if (ptrResult != null)
 			ptrResult.value = ptr;
 		return sign < 0 ? -r : r;
 	}
 
 	/**
 	 * Parse a Git style timezone string.
 	 * <p>
 	 * The sequence "-0315" will be parsed as the numeric value -195, as the
 	 * lower two positions count minutes, not 100ths of an hour.
 	 * 
 	 * @param b
 	 *            buffer to scan.
 	 * @param ptr
 	 *            position within buffer to start parsing digits at.
 	 * @return the timezone at this location, expressed in minutes.
 	 */
 	public static final int parseTimeZoneOffset(final byte[] b, int ptr) {
 		final int v = parseBase10(b, ptr, null);
 		final int tzMins = v % 100;
 		final int tzHours = v / 100;
 		return tzHours * 60 + tzMins;
 	}
 
 	/**
 	 * Locate the first position after a given character.
 	 * 
 	 * @param b
 	 *            buffer to scan.
 	 * @param ptr
 	 *            position within buffer to start looking for LF at.
 	 * @param chrA
 	 *            character to find.
 	 * @return new position just after chr.
 	 */
 	public static final int next(final byte[] b, int ptr, final char chrA) {
 		final int sz = b.length;
 		while (ptr < sz) {
 			if (b[ptr] == chrA)
 				return ptr + 1;
 			else
 				ptr++;
 		}
 		return ptr;
 	}
 
 	/**
 	 * Locate the first position after either the given character or LF.
 	 * <p>
 	 * This method stops on the first match it finds from either chrA or '\n'.
 	 * 
 	 * @param b
 	 *            buffer to scan.
 	 * @param ptr
 	 *            position within buffer to start looking for LF at.
 	 * @param chrA
 	 *            character to find.
 	 * @return new position just after the first chrA or chrB to be found.
 	 */
 	public static final int nextLF(final byte[] b, int ptr, final char chrA) {
 		final int sz = b.length;
 		while (ptr < sz) {
 			final byte c = b[ptr];
 			if (c == chrA || c == '\n')
 				return ptr + 1;
 			else
 				ptr++;
 		}
 		return ptr;
 	}
 
 	/**
 	 * Locate the "author " header line data.
 	 * 
 	 * @param b
 	 *            buffer to scan.
 	 * @param ptr
 	 *            position in buffer to start the scan at. Most callers should
 	 *            pass 0 to ensure the scan starts from the beginning of the
 	 *            commit buffer and does not accidentally look at message body.
 	 * @return position just after the space in "author ", so the first
 	 *         character of the author's name. If no author header can be
 	 *         located -1 is returned.
 	 */
 	public static final int author(final byte[] b, int ptr) {
 		final int sz = b.length;
 		if (ptr == 0)
 			ptr += 46; // skip the "tree ..." line.
 		while (ptr < sz && b[ptr] == 'p')
 			ptr += 48; // skip this parent.
 		return match(b, ptr, author);
 	}
 
 	/**
 	 * Locate the "committer " header line data.
 	 * 
 	 * @param b
 	 *            buffer to scan.
 	 * @param ptr
 	 *            position in buffer to start the scan at. Most callers should
 	 *            pass 0 to ensure the scan starts from the beginning of the
 	 *            commit buffer and does not accidentally look at message body.
 	 * @return position just after the space in "committer ", so the first
 	 *         character of the committer's name. If no committer header can be
 	 *         located -1 is returned.
 	 */
 	public static final int committer(final byte[] b, int ptr) {
 		final int sz = b.length;
 		if (ptr == 0)
 			ptr += 46; // skip the "tree ..." line.
 		while (ptr < sz && b[ptr] == 'p')
 			ptr += 48; // skip this parent.
 		if (ptr < sz && b[ptr] == 'a')
 			ptr = next(b, ptr, '\n');
 		return match(b, ptr, committer);
 	}
 
 	/**
 	 * Locate the "encoding " header line.
 	 * 
 	 * @param b
 	 *            buffer to scan.
 	 * @param ptr
 	 *            position in buffer to start the scan at. Most callers should
 	 *            pass 0 to ensure the scan starts from the beginning of the
 	 *            buffer and does not accidentally look at the message body.
 	 * @return position just after the space in "encoding ", so the first
 	 *         character of the encoding's name. If no encoding header can be
 	 *         located -1 is returned (and UTF-8 should be assumed).
 	 */
 	public static final int encoding(final byte[] b, int ptr) {
 		final int sz = b.length;
 		while (ptr < sz) {
 			if (b[ptr] == '\n')
 				return -1;
 			if (b[ptr] == 'e')
 				break;
 			ptr = next(b, ptr, '\n');
 		}
 		return match(b, ptr, encoding);
 	}
 
 	/**
 	 * Parse the "encoding " header into a character set reference.
 	 * <p>
 	 * Locates the "encoding " header (if present) by first calling
 	 * {@link #encoding(byte[], int)} and then returns the proper character set
 	 * to apply to this buffer to evaluate its contents as character data.
 	 * <p>
 	 * If no encoding header is present, {@link Constants#CHARSET} is assumed.
 	 * 
 	 * @param b
 	 *            buffer to scan.
 	 * @return the Java character set representation. Never null.
 	 */
 	public static Charset parseEncoding(final byte[] b) {
 		final int enc = encoding(b, 0);
 		if (enc < 0)
 			return Constants.CHARSET;
 		final int lf = next(b, enc, '\n');
 		return Charset.forName(decode(Constants.CHARSET, b, enc, lf - 1));
 	}
 
 	/**
 	 * Parse a name line (e.g. author, committer, tagger) into a PersonIdent.
 	 * <p>
 	 * When passing in a value for <code>nameB</code> callers should use the
 	 * return value of {@link #author(byte[], int)} or
 	 * {@link #committer(byte[], int)}, as these methods provide the proper
 	 * position within the buffer.
 	 * 
 	 * @param raw
 	 *            the buffer to parse character data from.
 	 * @param nameB
 	 *            first position of the identity information. This should be the
 	 *            first position after the space which delimits the header field
 	 *            name (e.g. "author" or "committer") from the rest of the
 	 *            identity line.
 	 * @return the parsed identity. Never null.
 	 */
 	public static PersonIdent parsePersonIdent(final byte[] raw, final int nameB) {
 		final Charset cs = parseEncoding(raw);
 		final int emailB = nextLF(raw, nameB, '<');
 		final int emailE = nextLF(raw, emailB, '>');
 
 		final String name = decode(cs, raw, nameB, emailB - 2);
 		final String email = decode(cs, raw, emailB, emailE - 1);
 
 		final MutableInteger ptrout = new MutableInteger();
 		final int when = parseBase10(raw, emailE + 1, ptrout);
 		final int tz = parseTimeZoneOffset(raw, ptrout.value);
 
 		return new PersonIdent(name, email, when * 1000L, tz);
 	}
 
 	/**
 	 * Decode a region of the buffer under the specified character set.
 	 * 
 	 * @param cs
 	 *            character set to use when decoding the buffer.
 	 * @param buffer
 	 *            buffer to pull raw bytes from.
 	 * @param start
 	 *            first position within the buffer to take data from.
 	 * @param end
 	 *            one position past the last location within the buffer to take
 	 *            data from.
 	 * @return a string representation of the range <code>[start,end)</code>,
 	 *         after decoding the region through the specified character set.
 	 */
 	public static String decode(final Charset cs, final byte[] buffer,
 			final int start, final int end) {
 		final ByteBuffer b = ByteBuffer.wrap(buffer, start, end - start);
 		return cs.decode(b).toString();
 	}
 
 	/**
 	 * Locate the position of the commit message body.
 	 * 
 	 * @param b
 	 *            buffer to scan.
 	 * @param ptr
 	 *            position in buffer to start the scan at. Most callers should
 	 *            pass 0 to ensure the scan starts from the beginning of the
 	 *            commit buffer.
 	 * @return position of the user's message buffer.
 	 */
 	public static final int commitMessage(final byte[] b, int ptr) {
 		final int sz = b.length;
 		if (ptr == 0)
 			ptr += 46; // skip the "tree ..." line.
 		while (ptr < sz && b[ptr] == 'p')
 			ptr += 48; // skip this parent.
 
 		// skip any remaining header lines, ignoring what their actual
 		// header line type is.
 		//
 		while (ptr < sz && b[ptr] != '\n')
 			ptr = next(b, ptr, '\n');
 		if (ptr < sz && b[ptr] == '\n')
 			return ptr + 1;
 		return -1;
 	}
 
 	/**
 	 * Locate the end of a paragraph.
 	 * <p>
 	 * A paragraph is ended by two consecutive LF bytes.
 	 * 
 	 * @param b
 	 *            buffer to scan.
	 * @param ptr
 	 *            position in buffer to start the scan at. Most callers will
 	 *            want to pass the first position of the commit message (as
 	 *            found by {@link #commitMessage(byte[], int)}.
 	 * @return position of the LF at the end of the paragraph;
 	 *         <code>b.length</code> if no paragraph end could be located.
 	 */
	public static final int endOfParagraph(final byte[] b, int ptr) {
 		final int sz = b.length;
 		while (ptr < sz && b[ptr] != '\n')
 			ptr = next(b, ptr, '\n');
		if (ptr < sz && b[ptr] == '\n')
			return ptr - 1;
		return sz;
 	}
 
 	private RawParseUtils() {
 		// Don't create instances of a static only utility.
 	}
 }
