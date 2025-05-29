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
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpsConnection;
import javax.microedition.io.SecurityInfo;
import javax.microedition.io.SocketConnection;

import org.bouncycastle.crypto.tls.Certificate;
import org.bouncycastle.crypto.tls.DefaultTlsClient;
import org.bouncycastle.crypto.tls.TlsAuthentication;
import org.bouncycastle.crypto.tls.TlsClientProtocol;
import org.bouncycastle.crypto.tls.ServerOnlyTlsAuthentication;



/**
 * 
 * HttpsConnectionImpl is an alternative implementation for the
 * {@link javax.microedition.io.HttpsConnection} interface.  A few
 * additional parameters are available to manage implementation specific
 * details. For those, see
 * {@link #getCertificate()}.
 * 
 */
public class HttpsConnectionImpl implements HttpsConnection {

	final private static int DEFAULT_HTTPS_PORT = 443;
	final private static int CONN_STATE_SETUP = 1;
	final private static int CONN_STATE_TLS_CONNECTED = 2;
	final private static int CONN_STATE_CONNECTED = 3;
	final private static int CONN_STATE_CLOSED = 4;
	final private static String MSG_REQUEST_ALREADY_SENT = "Request already sent";
	final private static String MSG_CONNECTION_CLOSED = "Connection close";
	final private static String MSG_ALREADY_CONNECTED = "Already connected";
	final private static String MSG_NOT_CONNECTED = "Not connected";
	final public static String REQUEST_METHOD_GET = "GET";
	final public static String REQUEST_METHOD_POST = "POST";
	final public static String REQUEST_METHOD_HEAD = "HEAD";
	private int port = 0;
	private String host = null;
	private String path = null;
	private String requestMethod = "GET";
	private int state = CONN_STATE_SETUP;
	private Hashtable headersMap = new Hashtable();
	private Vector headerKeys = new Vector();
	private Hashtable requestProperties = new Hashtable();
	private String responseMessage = null;
	private int responseCode = -1;
	private DataInputStream dataInputStream = null;
	private DataOutputStream dataOutputStream = null;
	private InputStream inputStream = null;
	private OutputStream outputStream = null;
	private SocketConnection connection = null;
	private TlsClientProtocol tlsClientProtocol = null;
	private Certificate certificate = null;

	/**
	 * Creates an httpsConnectionImpl instance to handle an https connection. The
	 * following default values are assigned to the Port and Path attributes: <BR>
	 * port: 443 <BR>
	 * path: "/"
	 * 
	 * @param host
	 *            Server hostname.
	 */
	public HttpsConnectionImpl(String host) {
		this(host, DEFAULT_HTTPS_PORT);
	}

	/**
	 * Creates an httpsConnectionImpl instance to handle an https connection. The
	 * following default value is assigned to the Path attribute: <BR>
	 * path: "/"
	 * 
	 * @param host
	 *            Server hostname.
	 * @param port
	 *            Server TCP port.
	 */
	public HttpsConnectionImpl(String host, int port) {
		this(host, port, "/");
	}

	/**
	 * Creates an httpsConnectionImpl instance to handle an https connection. The
	 * following default value is assigned to the Path attribute:
	 * 
	 * @param host
	 *            Server hostname.
	 * @param port
	 *            Server TCP port.
	 * @param path
	 *            URL Path.
	 */
	public HttpsConnectionImpl(String host, int port, String path) {
		this.host = host;
		this.port = port;
		this.path = path;
	}

	/**
	 * 
	 * @see javax.microedition.io.HttpsConnection#getPort()
	 * 
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * @see javax.microedition.io.HttpsConnection
	 */
	public SecurityInfo getSecurityInfo() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * 
	 * Refer to http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html
	 * 
	 * @see javax.microedition.io.HttpsConnection#getDate()
	 * 
	 */
	public long getDate() throws IOException {
		return this.getHeaderFieldDate("date", 0);
	}

	/**
	 * 
	 * @see javax.microedition.io.HttpsConnection#getExpiration()
	 * 
	 */
	public long getExpiration() throws IOException {
		return this.getHeaderFieldDate("expires", 0);
	}

	/**
	 * 
	 * @see javax.microedition.io.HttpsConnection#getFile()
	 * 
	 */
	public String getFile() {
		int lastQueryChar = path
				.indexOf(HttpsConnectionUtils.URL_QUERY_SEPARATOR);
		int lastRefChar = path
				.indexOf(HttpsConnectionUtils.URL_REFERENCE_SEPARATOR);
		int last = lastQueryChar < lastRefChar ? lastQueryChar : lastRefChar;
		if (last == -1) {
			last = path.length();
		}
		if (last == 0)
			return null;
		return path.substring(0, last);
	}

	/**
	 * 
	 * @see javax.microedition.io.HttpsConnection#getHeaderField(String)
	 * 
	 */
	public String getHeaderField(String name) throws IOException {
		performRequestIfNotConnected();
		return (String) headersMap.get(name);
	}

	/**
	 * 
	 * @see javax.microedition.io.HttpsConnection#getHeaderField(int)
	 * 
	 */
	public String getHeaderField(int n) throws IOException {

		return this.getHeaderField(getHeaderFieldKey(n));
	}

	/**
	 * 
	 * @see javax.microedition.io.HttpsConnection#getHeaderFieldDate(String,
	 *      long)
	 * 
	 */
	public long getHeaderFieldDate(String name, long def) throws IOException {
		try {
			String date = getHeaderField(name);
			if (date == null)
				return def;
			return HttpsConnectionUtils.parseDate(date).getTime();
		} catch (Exception e) {
			return def;
		}
	}

	/**
	 * 
	 * @see javax.microedition.io.HttpsConnection#getHeaderFieldInt(String, int)
	 * 
	 */
	public int getHeaderFieldInt(String name, int def) throws IOException {
		try {
			String number = getHeaderField(name);
			if (number == null)
				return def;
			return Integer.parseInt(number);
		} catch (Exception e) {
			return def;
		}
	}

	/**
	 * 
	 * @see javax.microedition.io.HttpsConnection#getHeaderFieldKey(int)
	 * 
	 */
	public String getHeaderFieldKey(int n) throws IOException {
		performRequestIfNotConnected();
		if (n >= headerKeys.size())
			return null;
		return (String) headerKeys.elementAt(n);
	}

	/**
	 * 
	 * @see javax.microedition.io.HttpsConnection#getHost()
	 * 
	 */
	public String getHost() {
		return this.host;
	}

	/**
	 * 
	 * @see javax.microedition.io.HttpsConnection#getLastModified()
	 * 
	 */
	public long getLastModified() throws IOException {
		return this.getHeaderFieldDate("last-modified", 0);
	}

	/**
	 * 
	 * @see javax.microedition.io.HttpsConnection#getProtocol()
	 * 
	 */
	public String getProtocol() {
		return "https"; // always returns https
	}

	/**
	 * 
	 * @see javax.microedition.io.HttpsConnection#getQuery()
	 * 
	 */
	public String getQuery() {
		int last = path.length();
		int first = 0;

		if (last == 0)
			return null;

		if (path.indexOf(HttpsConnectionUtils.URL_QUERY_SEPARATOR) != -1) {
			first = path.indexOf(HttpsConnectionUtils.URL_QUERY_SEPARATOR) + 1;
		} else {
			return null;
		}

		if (path.indexOf(HttpsConnectionUtils.URL_REFERENCE_SEPARATOR) != -1
				&& path.indexOf(HttpsConnectionUtils.URL_REFERENCE_SEPARATOR) > first) {
			last = path.indexOf(HttpsConnectionUtils.URL_REFERENCE_SEPARATOR);
		}

		if (first == last)
			return null;

		return path.substring(first, last);

	}

	/**
	 * 
	 * @see javax.microedition.io.HttpsConnection#getRef()
	 * 
	 */
	public String getRef() {
		int last = path.length();
		int first = 0;

		if (last == 0)
			return null;

		if (path.indexOf(HttpsConnectionUtils.URL_REFERENCE_SEPARATOR) != -1) {
			first = path.indexOf(HttpsConnectionUtils.URL_REFERENCE_SEPARATOR) + 1;
		}

		if (first == last)
			return null;

		return path.substring(first, last);
	}

	/**
	 * 
	 * @see javax.microedition.io.HttpsConnection#getRequestMethod()
	 * 
	 */
	public String getRequestMethod() {

		return requestMethod;
	}

	/**
	 * 
	 * @see javax.microedition.io.HttpsConnection#getRequestProperty(String)
	 * 
	 */
	public String getRequestProperty(String name) {
		return (String) this.requestProperties.get(name);
	}

	/**
	 * 
	 * @see javax.microedition.io.HttpsConnection#getResponseCode()
	 * 
	 */
	public int getResponseCode() throws IOException {
		performRequestIfNotConnected();
		return this.responseCode;
	}

	/**
	 * 
	 * @see javax.microedition.io.HttpsConnection#getResponseMessage()
	 * 
	 */
	public String getResponseMessage() throws IOException {
		performRequestIfNotConnected();
		return this.responseMessage;
	}

	/**
	 * 
	 * @see javax.microedition.io.HttpsConnection#getURL()
	 * 
	 */
	public String getURL() {
		return ("https://"
				+ this.host
				+ (port != DEFAULT_HTTPS_PORT ? HttpsConnectionUtils.HEADER_DELIMITER
						+ port
						: HttpsConnectionUtils.EMPTY) + this.path);
	}

	/**
	 * 
	 * @see javax.microedition.io.HttpsConnection#setRequestMethod(String)
	 * 
	 */
	public void setRequestMethod(String requestMethod) throws IOException {
		ensureReqNotSent();
		if (requestMethod == null
				|| (requestMethod.equalsIgnoreCase(REQUEST_METHOD_GET) == false
						&& requestMethod.equalsIgnoreCase(REQUEST_METHOD_HEAD) == false && requestMethod
						.equalsIgnoreCase(REQUEST_METHOD_POST) == false)) {
			throw new IOException("Invalid request method:  " + requestMethod
					+ ".");
		}
		this.requestMethod = requestMethod;
	}

	/**
	 * 
	 * @see javax.microedition.io.HttpsConnection#setRequestProperty(String,
	 *      String)
	 * 
	 */
	public void setRequestProperty(String name, String value)
			throws IOException {
		ensureReqNotSent();
		this.requestProperties.put(name, value);
	}

	/**
	 * 
	 * @see javax.microedition.io.HttpsConnection#getEncoding()
	 * 
	 */
	public String getEncoding() {
		try {
			return this.getHeaderField("content-encoding");
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 
	 * @see javax.microedition.io.HttpsConnection#getLength()
	 * 
	 */
	public long getLength() {
		try {
			return this.getHeaderFieldInt("content-length", -1);
		} catch (Exception e) {
			return -1;
		}
	}

	/**
	 * 
	 * @see javax.microedition.io.HttpsConnection#getType()
	 * 
	 */
	public String getType() {
		try {
			return this.getHeaderField("content-type");
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 
	 * @see javax.microedition.io.HttpsConnection#close()
	 * 
	 */
	public void close() throws IOException {
		ensureConnOpen();
		switchState(CONN_STATE_CLOSED);

		/*
		 * if(tlsHandler!=null) { tlsHandler.close(); }
		 */

		if (connection != null) {
			connection.close();
		}
	}

	/**
	 * 
	 * @see javax.microedition.io.HttpsConnection#openDataInputStream()
	 * 
	 */
	public DataInputStream openDataInputStream() throws IOException {
		dataInputStream = new DataInputStream(openInputStream());
		return dataInputStream;
	}

	/**
	 * 
	 * @see javax.microedition.io.HttpsConnection#openInputStream()
	 * 
	 */
	public InputStream openInputStream() throws IOException {
		performRequestIfNotConnected();
		if (inputStream != null) {
			throw new IOException("Input streams already open.");
		}
		inputStream = tlsClientProtocol.getInputStream();
		return inputStream;
	}

	/**
	 * 
	 * @see javax.microedition.io.HttpsConnection#openDataOutputStream()
	 * 
	 */
	public DataOutputStream openDataOutputStream() throws IOException {
		dataOutputStream = new DataOutputStream(openOutputStream());
		return dataOutputStream;
	}

	/**
	 * 
	 * @see javax.microedition.io.HttpsConnection#openOutputStream()
	 * 
	 */
	public OutputStream openOutputStream() throws IOException {
		if (outputStream != null) {
			throw new IOException("Outputstream already open.");
		}
		outputStream = new HttpsMessageOutputStream(
				tlsClientProtocol.getOutputStream(), this);
		return outputStream;
	}

	protected void validateState(int[] validStates, String message)
			throws IOException {
		for (int i = 0; i < validStates.length; i++) {
			if (validStates[i] == getState()) {
				return;
			}
		}
		throw new IOException(message);
	}

	protected void ensureConnOpen() throws IOException {
		validateState(new int[] { CONN_STATE_SETUP, CONN_STATE_TLS_CONNECTED,
				CONN_STATE_CONNECTED },
				HttpsConnectionImpl.MSG_CONNECTION_CLOSED);
	}

	protected void ensureConnectedState() throws IOException {
		validateState(new int[] { CONN_STATE_CONNECTED }, MSG_NOT_CONNECTED);
	}

	protected void ensureSetupState() throws IOException {
		validateState(new int[] { CONN_STATE_SETUP },
				HttpsConnectionImpl.MSG_ALREADY_CONNECTED);
	}

	protected void ensureReqNotSent() throws IOException {
		validateState(new int[] { CONN_STATE_SETUP, CONN_STATE_TLS_CONNECTED },
				HttpsConnectionImpl.MSG_REQUEST_ALREADY_SENT);
	}

	protected void switchState(int newState) throws IOException {
		this.state = newState;
	}

	protected int getState() {
		return this.state;
	}

	protected void connect() throws IOException {
		ensureSetupState();
		connection = (SocketConnection) Connector.open("socket://"
				+ this.getHost() + ":" + this.getPort());
		tlsClientProtocol = new TlsClientProtocol(connection.openInputStream(),
				connection.openOutputStream());
		DefaultTlsClient tlsClient = new DefaultTlsClient() {          
            public TlsAuthentication getAuthentication() throws IOException {
                return new ServerOnlyTlsAuthentication() {                  
                    public void notifyServerCertificate(Certificate serverCertificate) throws IOException {
                        // validateCertificate(serverCertificate);
                    }
                };
            }
        };
		tlsClientProtocol.connect(tlsClient);
		switchState(CONN_STATE_TLS_CONNECTED);
	}

	protected void performRequestIfNotConnected() throws IOException {
		if (getState() != CONN_STATE_CONNECTED) {
			performRequest();
		}
	}

	protected void performRequest() throws IOException {
		ensureConnOpen();
		connect();
		String message = HttpsConnectionUtils.getRequestMessage(this);
		HttpsConnectionUtils.logDebug("REQUEST:[[[[\n" + message + "\n]]]]");
		tlsClientProtocol.getOutputStream().write(message.getBytes());
		try {
			handleResponse();
		} catch (Exception e) {
			IOException ioe = new IOException(e.getMessage());
			throw ioe;
		}
		switchState(CONN_STATE_CONNECTED);
	}

	protected void handleResponse() throws Exception {
		InputStream in = tlsClientProtocol.getInputStream();
		// extract status line
		String line = HttpsConnectionUtils.readLine(in);
		HttpsConnectionUtils.logDebug("HTTP Response Status:[[[[" + line
				+ "]]]]");

		handleResponseStatus(line);
		// extract header fields
		while ((line = HttpsConnectionUtils.readLine(in)).length() > 0) {
			HttpsConnectionUtils.logDebug("HTTP Response Header:[[[[" + line
					+ "]]]]");
			handleResponseHeader(line);
		}

	}

	protected void handleResponseHeader(String line) throws Exception {
		String[] pair = HttpsConnectionUtils.parseHeaderLine(line);
		this.headerKeys.addElement(pair[0]);
		this.headersMap.put(pair[0], pair[1]);
	}

	protected void handleResponseStatus(String statusLine) throws Exception {
		String[] status = HttpsConnectionUtils.parseStatusLine(statusLine);
		this.responseMessage = status[2];
		this.responseCode = Integer.parseInt(status[1]);
	}

	Hashtable getRequestProperties() {
		return this.requestProperties;
	}

	/**
	 * Return the certificate associated with this connect.
	 * 
	 * @return The server certificate.
	 */
	public Certificate getCertificate() {
		return certificate;
	}

	/**
	 * See {@link #getCertificate()}.
	 * 
	 * @param serverCertificate
	 *            The certificate associated with this connect.
	 */
	public void setCertificate(Certificate serverCertificate) {
		this.certificate = serverCertificate;
	}
}
