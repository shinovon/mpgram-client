/*
Copyright (c) 2026 Arman Jussupgaliyev

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package cc.nnproject.json;

import java.io.IOException;
import java.io.Reader;

public class BufferedReader extends Reader {
	
	private Reader in;
	private char[] buffer;
	private int idx;
	private int length;
	
	public BufferedReader(Reader in) {
		this(in, 16384);
	}
	
	public BufferedReader(Reader in, int size) {
		this.in = in;
		buffer = new char[size];
	}
	
	public void close() throws IOException {
		in.close();
		buffer = null;
	}
	
	public int read() throws IOException {
		if (idx >= length && !fill()) {
			return -1;
		}
		return buffer[idx++];
	}

	public int read(char[] b) throws IOException {
		return read(b, 0, b.length);
	}
	
	public int read(char[] b, int off, int len) throws IOException {
		if (off < 0 || len < 0 || len > b.length - off) {
			throw new IllegalArgumentException();
		}
		if (len == 0) return 0;
		
		int n = b.length - off;
		if (n > len) n = len;
		int left = length - idx;
		
		if (n <= left) {
			System.arraycopy(buffer, idx, b, off, n);
			idx += n;
			return n;
		}
		
		if (left != 0) {
			System.arraycopy(buffer, idx, b, off, left);
			idx += left;
		}
		
		if (fill()) {
			return left + read(b, off + left, len - left);
		}
		
		return left == 0 ? -1 : left;
	}
	
	public long skip(long n) throws IOException {
		if (n < 0) {
			throw new IllegalArgumentException();
		}
		
		int left = length - idx;
		if (n <= left) {
			idx += n;
			return n;
		}
		
		idx += left;
		return left + in.skip(n - left);
	}
	
	public boolean ready() throws IOException {
		return idx < length || in.ready();
	}
	
	private boolean fill() throws IOException {
		int r = in.read(buffer);
		if (r != -1) {
			idx = 0;
			length = r;
			return true;
		}
		return false;
	}

}
