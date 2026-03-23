/*
Copyright (c) 2024-2025 Arman Jussupgaliyev

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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.microedition.io.StreamConnection;

// Streaming JSON

public class JSONStream extends Reader {

	Reader reader;
	boolean isObject;
	int index;
	private boolean eof;
	private char prev;
	boolean usePrev;
	StreamConnection connection;
	
	JSONStream() {}
	
	void init(InputStream in) throws IOException {
		reader = new InputStreamReader(in, MP.encoding);
		buffer = new char[16384];
	}
	
	// Streaming JSON functions
	
	public Object nextValue() throws IOException {
		char c = nextTrim();
		switch(c) {
		case '{':
			return nextObject(false);
		case '[':
			return nextArray(false);
		case '"':
			return nextString(false);
		case 't': // true
			skip(3);
			return MP.TRUE;
		case 'f': // false
			skip(4);
			return MP.FALSE;
		case 'n': // null
			skip(3);
			return MP.json_null;
		default:
			back();
			return nextValue(true);
		}
	}
	
	public JSONObject nextObject() throws IOException {
		return nextObject(true);
	}
	
	public JSONArray nextArray() throws IOException {
		return nextArray(true);
	}
	
	public String nextString() throws IOException {
		return nextString(true);
	}
	
	public Object nextNumber() throws IOException {
		Object v = nextValue(true);
		if (v instanceof String)
			throw new RuntimeException("JSON: nextNumber: not number: ".concat(String.valueOf(v)));
		return v;
	}
	
	public boolean isObject() {
		return isObject;
	}
	
	public boolean isArray() {
		return !isObject;
	}
	
	// Search functions
	
	// Jumps to key in object
	// Result is found, if false will skip to the end of object
	public boolean jumpToKey(String key) throws IOException {
//		if (!isObject)
//			throw new RuntimeException("JSON: jumpToKey: not object");
		
		char c;
//		while((c = nextTrim()) != '"' && c != 0);
//		back();
		
		while (true) {
			c = nextTrim();
			if (c == ',') continue;
			if (c != '"')
				throw new RuntimeException("JSON: jumpToKey: malformed object at ".concat(Integer.toString(index)));
			back();
			if (nextString(true).equals(key)) {
				// jump to value
				if (nextTrim() != ':')
					throw new RuntimeException("JSON: jumpToKey: malformed object at ".concat(Integer.toString(index)));
				return true;
			}
			if (nextTrim() != ':')
				throw new RuntimeException("JSON: jumpToKey: malformed object at ".concat(Integer.toString(index)));
			
//			skipValue();
			c = nextTrim();
			
			switch(c) {
			case '{':
				skipObject();
				break;
			case '[':
				skipArray();
				break;
			case '"':
				skipString();
				break;
			case 0:
				return false;
			default:
				while ((c = next()) != 0 && c != ',' && c != '}');
				back();
				break;
			}
			
			c = nextTrim();
			if (c == ',') {
				continue;
			}
			if (c == '}')
				return false;
			throw new RuntimeException("JSON: jumpToKey: malformed object at ".concat(Integer.toString(index)));
		}
	}
	
	// Skip N elements in array
	// If param is less than 1 or bigger than left elements count, will skip to the end of array
	// Result is success
	public boolean skipArrayElements(int count) throws IOException {
		while (true) {
			char c = nextTrim();
			switch(c) {
			case ']':
				return false;
			case '{':
				skipObject();
				break;
			case '[':
				skipArray();
				break;
			case '"':
				skipString();
				break;
			case 0:
				return false;
			default:
				while ((c = next()) != 0 && c != ',' && c != ']');
				back();
				break;
			}
			c = nextTrim();
			if (c == ',') {
				if(--count == 0)
					return true;
				continue;
			}
			return false;
		}
	}
	
//	public boolean jumpToKeyGlobal(String key) throws IOException { 
//		char c = 0;
//		boolean p = false;
//		boolean q = false;
//		boolean e = false;
//		while(true) {
//			if(p) {
//				p = false;
//			} else c = next();
//			if(c == 0) return false;
//			if(!e) {
//				if(c == '\\') e = true;
//				else if(c == '"') q = !q;
//			} else e = false;
//			if(!q)
//			if(c == '{' || c == ',') {
//				if((c = next()) == '\"') {
//					back();
//					String s = nextString();
//					if(nextTrim() != ':')
//						throw new RuntimeException("JSON: jumpToKey: malformed object at ".concat(Integer.toString(index)));
//					if(key.equals(s)) return true;
//				} else p = true;
//			}
//		}
//	}
	
	//
	
	public void skipValue() throws IOException {
		char c = nextTrim();
		switch(c) {
		case '{':
			skipObject();
			break;
		case '[':
			skipArray();
			break;
		case '"':
			skipString();
			break;
		case 0:
			return;
		default:
			while ((c = next()) != 0 && c != ',' && c != ':' && c != '}' && c != ']');
			back();
			break;
		}
	}
	
	// Basic reader functions
	
	public char next() throws IOException {
		if (usePrev) {
			usePrev = false;
			index++;
			return prev;
		}
//		if (eof) return 0;
		int r = read();
		if (r <= 0) {
			eof = true;
			return 0;
		}
		index++;
		return prev = (char) r;
	}
	
	public char nextTrim() throws IOException {
		char c;
		while ((c = next()) <= ' ' && c != 0);
		return c;
	}
	
	public void skip(int n) throws IOException {
		if (usePrev) {
			usePrev = false;
			n--;
		}
		index += n;
		skip((long) n);
	}

	public void back() {
		if (usePrev || index <= 0) throw new RuntimeException("JSON: back");
		usePrev = true;
		index--;
	}
	
	public boolean end() {
		return eof;
	}
	
	public void expectNext(char c) throws IOException {
		char n;
		if ((n = next()) != c)
			throw new RuntimeException("JSON: Expected '" + c + "', but got '" + n + "' at " + (index-1));
	}
	
	public void expectNextTrim(char c) throws IOException {
		char n;
		if ((n = nextTrim()) != c)
			throw new RuntimeException("JSON: Expected '" + c + "', but got '" + n + "' at " + (index-1));
	}
	
	/**
	 * @deprecated mark is probably not supported, since target is CLDC
	 */
	public void reset() throws IOException {
		index = prev = 0;
		usePrev = false;
		eof = false;
		reader.reset();
	}
	
	public void reset(InputStream is) throws IOException {
		try {
			close();
		} catch (IOException e) {}
		index = prev = 0;
		usePrev = false;
		eof = false;
		init(is);
	}
	
	//
	
	JSONObject nextObject(boolean check) throws IOException {
		if (check && nextTrim() != '{') {
			back();
			throw new RuntimeException("JSON: nextObject: not object at ".concat(Integer.toString(index)));
		}
		JSONObject r = new JSONObject();
		object: {
		while (true) {
			char c = nextTrim();
			if (c == '}') break object;
			back();
			String key = nextString(true);
			if (nextTrim() != ':')
				throw new RuntimeException("JSON: nextObject: malformed object at ".concat(Integer.toString(index)));
			Object val = null;
			c = nextTrim();
			switch (c) {
			case '}':
				break object;
			case '{':
				val = nextObject(false);
				break;
			case '[':
				val = nextArray(false);
				break;
			case '"':
				val = nextString(false);
				break;
			case 'n': // null
				skip(3);
				val = MP.json_null;
				break;
			case 't': // true
				skip(3);
				val = MP.TRUE;
				break;
			case 'f': // false
				skip(4);
				val = MP.FALSE;
				break;
			default:
				back();
				val = nextValue(true);
				break;
			}
			r.put(key, val);
			c = nextTrim();
			if (c == ',') {
				continue;
			}
			if (c == '}') break;
			throw new RuntimeException("JSON: nextObject: malformed object at ".concat(Integer.toString(index)));
		}
		}
		if (eof)
			throw new IOException("nextObject: Unexpected end");
		return r;
	}
	
	JSONArray nextArray(boolean check) throws IOException {
		if (check && nextTrim() != '[') {
			back();
			throw new RuntimeException("JSON: nextArray: not array at ".concat(Integer.toString(index)));
		}
		JSONArray r = new JSONArray();
		array: {
		while (true) {
			Object val = null;
			char c = nextTrim();
			switch(c) {
			case ']':
				break array;
			case '{':
				val = nextObject(false);
				break;
			case '[':
				val = nextArray(false);
				break;
			case '"':
				val = nextString(false);
				break;
			case 'n': // null
				skip(3);
				val = MP.json_null;
				break;
			case 't': // true
				skip(3);
				val = MP.TRUE;
				break;
			case 'f': // false
				skip(4);
				val = MP.FALSE;
				break;
			default:
				back();
				val = nextValue(true);
				break;
			}
			r.addElement(val);
			c = nextTrim();
			if (c == ',') {
				continue;
			}
			if (c == ']') break;
			throw new RuntimeException("JSON: nextArray: malformed array at ".concat(Integer.toString(index)));
		}
		}
		if (eof)
			throw new IOException("nextArray: Unexpected end");
		return r;
	}
	
	private String nextString(boolean check) throws IOException {
		if (check && nextTrim() != '"') {
			back();
			throw new RuntimeException("JSON: nextString: not string at ".concat(Integer.toString(index)));
		}
		StringBuffer sb = new StringBuffer();
		char l = 0;
		while (true) {
			char c = next();
			if (c == '\\' && l != '\\') {
				l = c;
				continue;
			}
			if (c == 'u' && l == '\\') {
				char[] chars = new char[4];
				chars[0] = next();
				chars[1] = next();
				chars[2] = next();
				chars[3] = next();
				sb.append(l = (char) Integer.parseInt(new String(chars), 16));
				continue;
			}
			if (c == 'n' && l == '\\') {
				sb.append(l = '\n');
				continue;
			}
			if (c == 'r' && l == '\\') {
				sb.append(l = '\r');
				continue;
			}
			if (c == 't' && l == '\\') {
				sb.append(l = '\t');
				continue;
			}
			if (c == 0 || (l != '\\' && c == '"')) break;
			sb.append(c);
			l = l == '\\' ? 0 : c;
		}
		if (eof)
			throw new IOException("nextString: Unexpected end");
		return sb.toString();
	}
	
	private void skipObject() throws IOException {
		while (true) {
			if (nextTrim() != '"')
				throw new RuntimeException("JSON: skipObject: malformed object at ".concat(Integer.toString(index)));
			skipString();
			if (nextTrim() != ':')
				throw new RuntimeException("JSON: skipObject: malformed object at ".concat(Integer.toString(index)));
			char c = nextTrim();
			switch(c) {
			case '}':
				return;
			case '{':
				skipObject();
				break;
			case '[':
				skipArray();
				break;
			case '"':
				skipString();
				break;
			case 0:
				return;
			default:
				while ((c = next()) != 0 && c != ',' && c != ':' && c != '}');
				back();
				break;
			}
			c = nextTrim();
			if (c == ',') {
				continue;
			}
			if (c == '}') return;
			throw new RuntimeException("JSON: skipObject: malformed object at ".concat(Integer.toString(index)));
		}
	}
	
	private void skipArray() throws IOException {
		while (true) {
			char c = nextTrim();
			switch(c) {
			case ']':
				return;
			case '{':
				skipObject();
				break;
			case '[':
				skipArray();
				break;
			case '"':
				skipString();
				break;
			case 0:
				return;
			default:
				while ((c = next()) != 0 && c != ',' && c != ']');
				back();
				break;
			}
			c = nextTrim();
			if (c == ',') {
				continue;
			}
			return;
		}
	}
	
	private void skipString() throws IOException {
		char l = 0;
		while (true) {
			char c = next();
			if (c == 0 || (l != '\\' && c == '"')) break;
			l = c;
		}
	}
	
	private Object nextValue(boolean convertToNumber) throws IOException {
		StringBuffer sb = new StringBuffer();
		while (true) {
			char c = next();
			if (c == 0) throw new RuntimeException("JSON: nextValue: Unexpected end");
			if (c == ',' || c == ']' || c == '}' || c == ':' || c <= ' ') {
				back();
				break;
			}
			sb.append(c);
		}
		String str = sb.toString();
		if (convertToNumber) {
			char first = str.charAt(0);
			int length = str.length();
			if ((first >= '0' && first <= '9') || first == '-') {
				try {
					// hex
					if (length > 1 && first == '0' && str.charAt(1) == 'x') {
						if (length > 9) // str.length() > 10
							return new Long(Long.parseLong(str.substring(2), 16));
						return new Integer(Integer.parseInt(str.substring(2), 16));
					}
					// decimal
					if (str.indexOf('.') != -1 || str.indexOf('E') != -1 || "-0".equals(str))
//						return new Double(Double.parseDouble(str));
						return str;
					if (first == '-') length--;
					if (length > 8) // (str.length() - (str.charAt(0) == '-' ? 1 : 0)) >= 10
						return new Long(Long.parseLong(str));
					return new Integer(Integer.parseInt(str));
				} catch (Exception ignored) {}
			}
		}
		return str;
	}
	
	// Reader

	private char[] buffer;
	private int bufferSize;
	private int bufferIndex;

	public void close() throws IOException {
		buffer = null;
		bufferSize = 0;
		bufferIndex = 0;
		if (reader != null) {
			reader.close();
		}
		if (connection != null) {
			connection.close();
		}
	}

	public int read() throws IOException {
		int res = 0;
		if (bufferIndex >= bufferSize) {
			res = fill();
		}
		if (res != -1) {
			res = buffer[bufferIndex++];
		}
		return res;
	}

	public int read(char[] buf) throws IOException {
		return read(buf, 0, buf.length);
	}

	public int read(char[] buf, int off, int len) throws IOException {
		if (off < 0 || off >= buf.length) throw new IllegalArgumentException();
		int charsToRead = buf.length - off;
		if (charsToRead > len) {
			charsToRead = len;
		}
		int bufCharCount = bufferSize - bufferIndex;
		int readCount = 0;
		if (charsToRead <= bufCharCount) {
			for (int i = 0; i < charsToRead; i++) {
				buf[off + i] = buffer[bufferIndex++];
			}
			readCount += charsToRead;
		} else {
			for (int i = 0; i < bufCharCount; i++) {
				buf[off + i] = buffer[bufferIndex++];
			}
			readCount += bufCharCount;
			if (fill() != -1) {
				readCount += read(buf, off + readCount, len - readCount);
			}
		}
		if (readCount <= 0) {
			readCount = -1;
		}
		return readCount;
	}

	public boolean ready() throws IOException {
		if (bufferIndex < bufferSize) {
			return true;
		}
		if (reader != null) {
			return reader.ready();
		}
		return false;
	}

	public long skip(long count) throws IOException {
		if (count < 0) throw new IllegalArgumentException();
		long skipped = 0;
		int bufCharCount = bufferSize - bufferIndex;
		if (count <= bufCharCount) {
			bufferIndex += (int) count;
			skipped += count;
		} else {
			bufferIndex += bufCharCount;
			skipped += bufCharCount;
			if (reader != null) {
				skipped += reader.skip(count - skipped);
			}
		}
		return skipped;
	}

	private int fill() throws IOException {
		if (reader == null) {
			return -1;
		}
		int readCount = reader.read(buffer);
		if (readCount != -1) {
			bufferSize = readCount;
			bufferIndex = 0;
		}
		return readCount;
	}

}
