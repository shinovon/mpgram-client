/* Copyright 2014 WS/Tech� Informatica LTDA.
 * 
 * MHC (ME HTTPS Client) - An alternative J2ME Https Client.
 * 
 * http://www.wstech2.net/mhc/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.io.IOException;
import java.io.OutputStream;

/**
 * 
 * An wrapper for the java.io.OutputStream instance create by
 * {@link org.bouncycastle.crypto.tls.TlsProtocol#getOutputStream()} and used to
 * write data to the remote host. This class will also coordinate with the
 * {@link HttpsConnectionImpl} instance before a flush() operation.
 * 
 */
public class HttpsMessageOutputStream extends OutputStream {

	private OutputStream outputStream = null;
	private HttpsConnectionImpl httpsConnectionInstance = null;

	public HttpsConnectionImpl getHttpsConnectionInstance() {
		return httpsConnectionInstance;
	}

	public void setHttpsConnectionInstance(
			HttpsConnectionImpl httpsConnectionInstance) {
		this.httpsConnectionInstance = httpsConnectionInstance;
	}

	/**
	 * Creates a new HttpsMessageOutputStream wrapping a java.io.OutputStream instance.
	 * 
	 * @param outputStream
	 *            An already existing OutputStream, probably associated or
	 *            created by the underline communication framework, like the one
	 *            from
	 *            {@link org.bouncycastle.crypto.tls.TlsProtocol#getOutputStream()}
	 * @param httpsConnectionInstance
	 *            The {@link HttpsConnectionImpl} instance representing the
	 *            connection.
	 */
	public HttpsMessageOutputStream(OutputStream outputStream,
			HttpsConnectionImpl httpsConnectionInstance) {
		this.outputStream = outputStream;
		this.httpsConnectionInstance = httpsConnectionInstance;
	}

	/**
	 * @see java.io.OutputStream#write(int)
	 */
	public void write(int c) throws IOException {
		outputStream.write(c);
	}

	/**
	 * @see java.io.OutputStream#write(byte[])
	 */
	public void write(byte[] b) throws IOException {
		outputStream.write(b);
	}

	/**
	 * @see java.io.OutputStream#write(byte[], int, int)
	 */
	public void write(byte[] b, int off, int len) throws IOException {
		outputStream.write(b, off, len);
	}

	/**
	 * If an httpsConnectionInstance is present, its
	 *      performRequestIfNotConnected() method will be invoked before any
	 *      flush() is performed.
	 *      
	 * @see java.io.OutputStream#flush()
	 * 
	 */
	public void flush() throws IOException {
		if (httpsConnectionInstance != null) {
			try {
				httpsConnectionInstance.performRequestIfNotConnected();
			} catch (Exception e) {
				throw new IOException(e.getMessage());
			}
		}
		outputStream.flush();
	}

	/**
	 * @see java.io.OutputStream#close()
	 */
	public void close() throws IOException {
		this.flush();
		outputStream.close();
	}

}