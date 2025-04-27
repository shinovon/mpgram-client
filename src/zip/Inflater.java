/* Inflater.java - Decompress a data stream
   Copyright (C) 1999, 2000, 2001, 2003  Free Software Foundation, Inc.

This file is part of GNU Classpath.

GNU Classpath is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.
 
GNU Classpath is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with GNU Classpath; see the file COPYING.  If not, write to the
Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
02111-1307 USA.

Linking this library statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on this library.  If you modify this library, you may extend
this exception to your version of the library, but you are not
obligated to do so.  If you do not wish to do so, delete this
exception statement from your version. */

package zip;

/* Written using on-line Java Platform 1.2 API Specification
 * and JCL book.
 * Believed complete and correct.
 */

/**
 * Inflater is used to decompress data that has been compressed according to the
 * "deflate" standard described in rfc1950.
 *
 * The usage is as following. First you have to set some input with
 * <code>setInput()</code>, then inflate() it. If inflate doesn't inflate any
 * bytes there may be three reasons:
 * <ul>
 * <li>needsInput() returns true because the input buffer is empty. You have to
 * provide more input with <code>setInput()</code>. NOTE: needsInput() also
 * returns true when, the stream is finished.</li>
 * <li>needsDictionary() returns true, you have to provide a preset dictionary
 * with <code>setDictionary()</code>.</li>
 * <li>finished() returns true, the inflater has finished.</li>
 * </ul>
 * Once the first output byte is produced, a dictionary will not be needed at a
 * later stage.
 *
 * @author John Leuner, Jochen Hoenicke
 * @author Tom Tromey
 * @date May 17, 1999
 * @since JDK 1.1
 */
public class Inflater {
	/* Copy lengths for literal codes 257..285 */
	private static final int CPLENS[] = { 3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31, 35, 43, 51, 59, 67,
			83, 99, 115, 131, 163, 195, 227, 258 };

	/* Extra bits for literal codes 257..285 */
	private static final int CPLEXT[] = { 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5,
			5, 5, 0 };

	/* Copy offsets for distance codes 0..29 */
	private static final int CPDIST[] = { 1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193, 257, 385, 513, 769,
			1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577 };

	/* Extra bits for distance codes */
	private static final int CPDEXT[] = { 0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11,
			11, 12, 12, 13, 13 };

	/* This are the state in which the inflater can be. */
	//private static final int DECODE_HEADER           = 0;
	//private static final int DECODE_DICT             = 1;
	private static final int DECODE_BLOCKS = 2;
	//private static final int DECODE_STORED_LEN1      = 3;
	//private static final int DECODE_STORED_LEN2      = 4;
	//private static final int DECODE_STORED           = 5;
	//private static final int DECODE_DYN_HEADER       = 6;
	private static final int DECODE_HUFFMAN = 7;
	private static final int DECODE_HUFFMAN_LENBITS = 8;
	private static final int DECODE_HUFFMAN_DIST = 9;
	private static final int DECODE_HUFFMAN_DISTBITS = 10;
	//private static final int DECODE_CHKSUM           = 11;
	//private static final int FINISHED                = 12;

	/** This variable contains the current state. */
	private int mode;

	/**
	 * The adler checksum of the dictionary or of the decompressed stream, as it is
	 * written in the header resp. footer of the compressed stream. <br>
	 *
	 * Only valid if mode is DECODE_DICT or DECODE_CHKSUM.
	 */
	private int readAdler;
	/**
	 * The number of bits needed to complete the current state. This is valid, if
	 * mode is DECODE_DICT, DECODE_CHKSUM, DECODE_HUFFMAN_LENBITS or
	 * DECODE_HUFFMAN_DISTBITS.
	 */
	private int neededBits;
	private int repLength, repDist;
	private int uncomprLen;
	/**
	 * True, if the last block flag was set in the last block of the inflated
	 * stream. This means that the stream ends after the current block.
	 */
	private boolean isLastBlock;

	/**
	 * The total number of inflated bytes.
	 */
	private long totalOut;
	/**
	 * The total number of bytes set with setInput(). This is not the value returned
	 * by getTotalIn(), since this also includes the unprocessed input.
	 */
	private long totalIn;
	/**
	 * This variable stores the nowrap flag that was given to the constructor. True
	 * means, that the inflated stream doesn't contain a header nor the checksum in
	 * the footer.
	 */
	private boolean nowrap;

	private StreamManipulator input;
	private OutputWindow outputWindow;
	private InflaterDynHeader dynHeader;
	private InflaterHuffmanTree litlenTree, distTree;
	private Adler32 adler;

	public Inflater() {
		this(false);
	}

	public Inflater(final boolean nowrap) {
		super();
		this.nowrap = nowrap;
		this.adler = new Adler32();
		this.input = new StreamManipulator();
		this.outputWindow = new OutputWindow();
		this.mode = (nowrap ? 2 : 0);
	}

	public final boolean finished() {
		return this.mode == 12 && this.outputWindow.getAvailable() == 0;
	}

	public final int getRemaining() {
		return this.input.getAvailableBytes();
	}

	public final int getTotalIn() {
		return (int) (this.totalIn - this.getRemaining());
	}

	public final int getTotalOut() {
		return (int) this.totalOut;
	}

	public final int inflate(final byte[] buf, int off, int len) throws Exception {
		if (len == 0) {
			return 0;
		}
		if (0 > off || off > off + len || off + len > buf.length) {
			throw new ArrayIndexOutOfBoundsException();
		}
		int count = 0;
		do {
			if (this.mode != 11) {
				final int more = this.outputWindow.copyOutput(buf, off, len);
				this.adler.update(buf, off, more);
				off += more;
				count += more;
				this.totalOut += more;
				if ((len -= more) == 0) {
					return count;
				}
				continue;
			}
		} while (this.decode() || (this.outputWindow.getAvailable() > 0 && this.mode != 11));
		return count;
	}

	public final boolean needsDictionary() {
		return this.mode == 1 && this.neededBits == 0;
	}

	public final boolean needsInput() {
		return this.input.needsInput();
	}

	public final void reset() {
		this.mode = (this.nowrap ? 2 : 0);
		final long n = 0L;
		this.totalOut = n;
		this.totalIn = n;
		this.input.reset();
		this.outputWindow.reset();
		this.dynHeader = null;
		this.litlenTree = null;
		this.distTree = null;
		this.isLastBlock = false;
		this.adler.reset();
	}

	public final void setInput(final byte[] buf, final int off, final int len) {
		this.input.setInput(buf, off, len);
		this.totalIn += len;
	}

	private boolean decodeHeader() throws Exception {
		final int header;
		if ((header = this.input.peekBits(16)) < 0) {
			return false;
		}
		this.input.dropBits(16);
		final int n;
		if ((n = ((header << 8 | header >> 8) & 0xFFFF)) % 31 != 0) {
			throw new Exception("Data format: Header checksum illegal");
		}
		if ((n & 0xF00) != 0x800) {
			throw new Exception("Data format: Compression Method unknown");
		}
		if ((n & 0x20) == 0x0) {
			this.mode = 2;
		} else {
			this.mode = 1;
			this.neededBits = 32;
		}
		return true;
	}

	private boolean decodeDict() {
		while (this.neededBits > 0) {
			final int dictByte;
			if ((dictByte = this.input.peekBits(8)) < 0) {
				return false;
			}
			this.input.dropBits(8);
			this.readAdler = (this.readAdler << 8 | dictByte);
			this.neededBits -= 8;
		}
		return false;
	}

	private boolean decodeHuffman() throws Exception {
		int free = outputWindow.getFreeSpace();
		while (free >= 258) {
			int symbol;
			switch (mode) {
			case DECODE_HUFFMAN:
				/* This is the inner loop so it is optimized a bit */
				while (((symbol = litlenTree.getSymbol(input)) & ~0xff) == 0) {
					outputWindow.write(symbol);
					if (--free < 258)
						return true;
				}
				if (symbol < 257) {
					if (symbol < 0)
						return false;
					else {
						/* symbol == 256: end of block */
						distTree = null;
						litlenTree = null;
						mode = DECODE_BLOCKS;
						return true;
					}
				}

				try {
					repLength = CPLENS[symbol - 257];
					neededBits = CPLEXT[symbol - 257];
				} catch (ArrayIndexOutOfBoundsException ex) {
					throw new Exception("Data format: Illegal rep length code");
				}
				/* fall through */
			case DECODE_HUFFMAN_LENBITS:
				if (neededBits > 0) {
					mode = DECODE_HUFFMAN_LENBITS;
					int i = input.peekBits(neededBits);
					if (i < 0)
						return false;
					input.dropBits(neededBits);
					repLength += i;
				}
				mode = DECODE_HUFFMAN_DIST;
				/* fall through */
			case DECODE_HUFFMAN_DIST:
				symbol = distTree.getSymbol(input);
				if (symbol < 0)
					return false;
				try {
					repDist = CPDIST[symbol];
					neededBits = CPDEXT[symbol];
				} catch (ArrayIndexOutOfBoundsException ex) {
					throw new Exception("Data format: Illegal rep dist code");
				}
				/* fall through */
			case DECODE_HUFFMAN_DISTBITS:
				if (neededBits > 0) {
					mode = DECODE_HUFFMAN_DISTBITS;
					int i = input.peekBits(neededBits);
					if (i < 0)
						return false;
					input.dropBits(neededBits);
					repDist += i;
				}
				outputWindow.repeat(repLength, repDist);
				free -= repLength;
				mode = DECODE_HUFFMAN;
				break;
			default:
				throw new IllegalStateException();
			}
		}
		return true;
	}

	private boolean decodeChksum() throws Exception {
		while (this.neededBits > 0) {
			final int chkByte;
			if ((chkByte = this.input.peekBits(8)) < 0) {
				return false;
			}
			this.input.dropBits(8);
			this.readAdler = (this.readAdler << 8 | chkByte);
			this.neededBits -= 8;
		}
		if ((int) this.adler.getValue() != this.readAdler) {
			throw new Exception("Data format: Adler chksum doesn't match: "
					+ Integer.toHexString((int) this.adler.getValue()) + " vs. " + Integer.toHexString(this.readAdler));
		}
		this.mode = 12;
		return false;
	}

	private boolean decode() throws Exception {
		switch (this.mode) {
		case 0:
			return this.decodeHeader();
		case 1:
			return this.decodeDict();
		case 11:
			return this.decodeChksum();
		case 2:
			if (this.isLastBlock) {
				if (this.nowrap) {
					this.mode = 12;
					return false;
				}
				this.input.skipToByteBoundary();
				this.neededBits = 32;
				this.mode = 11;
				return true;
			} else {
				final int type;
				if ((type = this.input.peekBits(3)) < 0) {
					return false;
				}
				this.input.dropBits(3);
				if ((type & 0x1) != 0x0) {
					this.isLastBlock = true;
				}
				switch (type >> 1) {
				case 0:
					this.input.skipToByteBoundary();
					this.mode = 3;
					break;
				case 1:
					this.litlenTree = InflaterHuffmanTree.defLitLenTree;
					this.distTree = InflaterHuffmanTree.defDistTree;
					this.mode = 7;
					break;
				case 2:
					this.dynHeader = new InflaterDynHeader();
					this.mode = 6;
					break;
				default:
					throw new Exception("Data format: Unknown block type " + type);
				}
				return true;
			}
		case 3: {
			final int nlen = this.input.peekBits(16);
			this.uncomprLen = nlen;
			if (nlen < 0) {
				return false;
			}
			this.input.dropBits(16);
			this.mode = 4;
		}
		case 4: {
			final int nlen;
			if ((nlen = this.input.peekBits(16)) < 0) {
				return false;
			}
			this.input.dropBits(16);
			if (nlen != (this.uncomprLen ^ 0xFFFF)) {
				throw new Exception("Data format: broken uncompressed block");
			}
			this.mode = 5;
		}
		case 5:
			this.uncomprLen -= this.outputWindow.copyStored(this.input, this.uncomprLen);
			if (this.uncomprLen == 0) {
				this.mode = 2;
				return true;
			}
			return !this.input.needsInput();
		case 6:
			if (!this.dynHeader.decode(this.input)) {
				return false;
			}
			this.litlenTree = this.dynHeader.buildLitLenTree();
			this.distTree = this.dynHeader.buildDistTree();
			this.mode = 7;
			return this.decodeHuffman();
		case 7:
		case 8:
		case 9:
		case 10:
			return this.decodeHuffman();
		case 12:
			return false;
		default:
			throw new IllegalStateException();
		}
	}
}
