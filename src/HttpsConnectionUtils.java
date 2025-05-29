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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * 
 * A collection of static, state independent, auxiliary methods to support the
 * execution of common tasks in HttpsConnectionImpl.
 * 
 */
public class HttpsConnectionUtils {
	final static int DATETYPE_UNKNOWN = -1;
	final static int DATE_TYPE_RFC1123 = 1;
	final static int DATE_TYPE_RFC850 = 2;
	final static int DATE_TYPE_ASCTIME = 3;
	final static String CRLF = "\r\n";
	final static char CR = '\r';
	final static char LF = '\n';
	final static String SPACE = " ";
	final static String EMPTY = "";
	final static String HEADER_DELIMITER = ":";
	final static String HTTP_REQUEST_VERSION = "HTTP/1.0";
	final static char COMMA_DATE_SEPARATOR = ',';
	final static char HYPHEN_DATE_SEPARATOR = '-';
	final static String DIRECTORY_SEPARATOR = "/";
	final static String URL_QUERY_SEPARATOR = "?";
	final static String URL_REFERENCE_SEPARATOR = "#";

	final static Hashtable weekdays = new Hashtable();
	final static Hashtable months = new Hashtable();
	private static final String HEADER_HOST = "Host";

	static {

		weekdays.put("Sun", new Integer(Calendar.SUNDAY));

		weekdays.put("Mon", new Integer(Calendar.MONDAY));
		weekdays.put("Tue", new Integer(Calendar.TUESDAY));

		weekdays.put("Wed", new Integer(Calendar.WEDNESDAY));
		weekdays.put("Thu", new Integer(Calendar.THURSDAY));
		weekdays.put("Fri", new Integer(Calendar.FRIDAY));

		weekdays.put("Sat", new Integer(Calendar.SATURDAY));

		weekdays.put("Sunday", new Integer(Calendar.SUNDAY));

		weekdays.put("Monday", new Integer(Calendar.MONDAY));
		weekdays.put("Tuesday", new Integer(Calendar.TUESDAY));

		weekdays.put("Wednesday", new Integer(Calendar.WEDNESDAY));
		weekdays.put("Thursday", new Integer(Calendar.THURSDAY));
		weekdays.put("Friday", new Integer(Calendar.FRIDAY));
		weekdays.put("Saturday", new Integer(Calendar.SATURDAY));

		months.put("Jan", new Integer(Calendar.JANUARY));
		months.put("Feb", new Integer(Calendar.FEBRUARY));
		months.put("Mar", new Integer(Calendar.MARCH));
		months.put("Apr", new Integer(Calendar.APRIL));
		months.put("May", new Integer(Calendar.MAY));
		months.put("Jun", new Integer(Calendar.JUNE));
		months.put("Jul", new Integer(Calendar.JULY));
		months.put("Aug", new Integer(Calendar.AUGUST));
		months.put("Sep", new Integer(Calendar.SEPTEMBER));
		months.put("Oct", new Integer(Calendar.OCTOBER));
		months.put("Nov", new Integer(Calendar.NOVEMBER));
		months.put("Dec", new Integer(Calendar.DECEMBER));

	}

	/**
	 * Parses a date string.
	 * Refer to http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html for possible
	 * date formats.
	 * @param date Date string.
	 * @return An java.util.Date corresponding to the string sent as parameter. 
	 * 
	 */

	public static Date parseDate(String date) throws IllegalArgumentException {

		HttpsConnectionUtils
				.logDebug("parseDate(String date) parsing string:[[[[" + date
						+ "]]]]");

		if (date == null)
			throw new IllegalArgumentException("Date must not be null.");

		Vector d = split(date, SPACE);

		// remove delimiters
		while (d.removeElement(EMPTY))
			;
		while (d.removeElement(SPACE))
			;

		try {
			String dweek = (String) d.elementAt(0);
			String year = null;
			String month = null;
			String day = null;
			String time = null;

			switch (getDateType(date)) {
			case DATE_TYPE_ASCTIME: {
				dweek = dweek.substring(0, dweek.indexOf(COMMA_DATE_SEPARATOR))
						.trim();
				year = (String) d.elementAt(4);
				month = (String) d.elementAt(1);
				day = (String) d.elementAt(2);
				time = (String) d.elementAt(3);

			}
			case DATE_TYPE_RFC1123: {
				dweek = dweek.substring(0, dweek.indexOf(COMMA_DATE_SEPARATOR))
						.trim();
				year = (String) d.elementAt(3);
				month = (String) d.elementAt(2);
				day = (String) d.elementAt(1);
				time = (String) d.elementAt(4);
			}
			case DATE_TYPE_RFC850: {

				year = ((String) d.elementAt(1)).substring(7, 9);
				month = ((String) d.elementAt(1)).substring(3, 6);
				day = ((String) d.elementAt(1)).substring(0, 2);
				time = (String) d.elementAt(2);
			}
			}
			return parseDate(dweek, year, month, day, time);

		} catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage());
		}

	}

	private static Date parseDate(String dweek, String year, String month,
			String day, String time) {

		int[] date = new int[7];
		Integer id = null;

		String hour = time.trim().substring(0, 2);
		String minute = time.trim().substring(3, 5);
		String second = time.trim().substring(6, 8);

		if ((id = (Integer) weekdays.get(dweek)) == null) {
			throw new IllegalArgumentException("Invalid day of week.");
		}
		date[0] = id.intValue();

		date[1] = Integer.parseInt(year.trim());

		if ((id = (Integer) months.get(month)) == null) {
			throw new IllegalArgumentException("Invalid month.");
		}
		date[2] = id.intValue();
		date[3] = Integer.parseInt(day.trim());
		date[4] = Integer.parseInt(hour.trim());
		date[5] = Integer.parseInt(minute.trim());
		date[6] = Integer.parseInt(second.trim());

		return getDate(date);
	}

	/**
	 * 
	 * Converts a date from an array into a java.util.Date instance.
	 * 
	 * The date array is expected to be in the following index order:
	 * 
	 * <BR>0 - WeekDay
	 * <BR>1 - Year
	 * <BR>2 - Month 
	 * <BR>3 - Day 
	 * <BR>4 - Hour 
	 * <BR>5 - Minute 
	 * <BR>6 - Second
	 * <BR>
	 * <BR>Always GMT
	 * 
	 * @param date An array representing a date.  
	 * @return A java.util.Date instance corresponding to the input date array.
	 */
	private static Date getDate(int[] date) {
		Calendar c = Calendar.getInstance();
		c.set(Calendar.YEAR, (date[1] > 100 ? date[1] : date[1] + 2000));
		c.set(Calendar.MONTH, date[2]);
		c.set(Calendar.DAY_OF_MONTH, date[3]);
		c.set(Calendar.HOUR_OF_DAY, date[4]);
		c.set(Calendar.MINUTE, date[5]);
		c.set(Calendar.SECOND, date[6]);
		return c.getTime();

	}

	private static int getDateType(String date) {

		// try to guess which date format was used based on 3 distinct marks
		// no , nor - = ASC
		// with , but no - = RFC 1123
		// otherwise = RFC 850

		if (date.indexOf(HYPHEN_DATE_SEPARATOR) != -1
				&& date.indexOf(COMMA_DATE_SEPARATOR) != -1) {
			return DATE_TYPE_RFC850;
		} else if (date.indexOf(COMMA_DATE_SEPARATOR) != -1) {
			return DATE_TYPE_RFC1123;
		} else {
			return DATE_TYPE_ASCTIME;
		}
	}

	public static Vector split(String str, String delimiter)
			throws IllegalArgumentException {
		if (str == null || delimiter == null){
			throw new IllegalArgumentException("Arguments cannot be null.");
		}

		Vector v = new Vector();
		int cursorPos = 0;
		while (cursorPos < str.length()) {
			int delimPos = str.indexOf(delimiter, cursorPos);
			if (delimPos == -1) {
				v.addElement(str.substring(cursorPos));
				break;
			}
			if(delimPos == cursorPos){
				cursorPos = cursorPos + delimiter.length();
				continue;
			}
			v.addElement(str.substring(cursorPos, delimPos));
			
			cursorPos = delimPos + delimiter.length();
		}

		return v;
	}

	/**
	 * Creates a HTTP Request Message.
	 * @param conn The {@link HttpsConnectionImpl} corresponding to the HTTP message.
	 * @return A HTTP Request Message.
	 */
	public static String getRequestMessage(HttpsConnectionImpl conn) {
		StringBuffer message = new StringBuffer();
		message.append(conn.getRequestMethod());
		message.append(SPACE);
		message.append(conn.getFile() != null ? conn.getFile()
				: DIRECTORY_SEPARATOR);
		if (conn.getQuery() != null) {
			message.append(URL_QUERY_SEPARATOR);
			message.append(conn.getQuery());
		}
		message.append(SPACE);
		message.append(HTTP_REQUEST_VERSION);
		message.append(CRLF);
		setDefaultHostPropertyIfNone(conn);
		for (Enumeration keys = conn.getRequestProperties().keys(); keys
				.hasMoreElements();) {
			String key = (String) keys.nextElement();
			String value = conn.getRequestProperty(key);
			message.append(key);
			message.append(HEADER_DELIMITER + SPACE);
			message.append(value);
			message.append(CRLF);
		}
		message.append(CRLF);// final \r\n

		return message.toString();
	}

	private static void setDefaultHostPropertyIfNone(HttpsConnectionImpl conn) {
		if (conn.getRequestProperties().get(HEADER_HOST) == null) {
			String host = conn.getHost() + ":" + conn.getPort();
			conn.getRequestProperties().put(HEADER_HOST, host);
		}
	}

	/**
	 * A simple method to read a complete line from an InputStream.
	 */
	public static String readLine(InputStream in) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();

		int c = 0;
		while ((c = in.read()) != -1 && c != LF) {
			if (c != CR) // ignore CR
			{
				bytes.write(c);
			}
		}
		return new String(bytes.toByteArray());
	}

	/**
	 * Parses a HTTP response status line.
	 * 
	 * Refer to http://www.w3.org/Protocols/HTTP/1.0/spec.html#Status-Line
	 * 
	 * @param statusLine A message status line.
	 * @return An array containing the response elements.
	 * @throws IOException
	 */
	public static String[] parseStatusLine(String statusLine) throws Exception {
		String[] status = new String[3];

		try {
			int space1 = statusLine.indexOf(SPACE);
			int space2 = statusLine.indexOf(SPACE, space1 + 1);
			// version
			status[0] = statusLine.substring(0, space1);
			// response code
			status[1] = statusLine.substring(space1 + 1, space2);
			// response message
			status[2] = statusLine.substring(space2);
			return status;
		} catch (Exception e) {
			throw new Exception("Unknown format in http response status line.");
		}
	}

	/**
	 * Parses a HTTP message header line.
	 * @param line The header line.
	 * @return A two-element String array containing the key name and the value of the line header.
	 * @throws Exception
	 */
	public static String[] parseHeaderLine(String line) throws Exception {
		try {
			String key, value;
			int delim = line.indexOf(HEADER_DELIMITER);
			key = line.substring(0, delim).trim();
			value = line.substring(delim + 1).trim();
			return new String[] { key, value };
		} catch (Exception e) {
			throw new Exception("Unknown format in http response headers.");
		}
	}

	/**
	 * 
	 * Uses System.out.println() to log debug messages.
	 * 
	 */
	public static void logDebug(String message) {
		System.out.println(message);
	}

	/**
	 * 
	 * Uses System.err.println() to log error messages.
	 * 
	 */
	public static void logError(String message) {
		System.err.println(message);
		System.err.flush();
		System.out.flush();
	}

	/**
	 * 
	 * Uses System.err.println() to log error messages.
	 * 
	 * The Throwable's message and stack trace are also logged.
	 * 
	 */
	public static void logError(String message, Throwable t) {
		System.err.println(message);
		System.err.println(t.getMessage());
		System.err.flush();
		t.printStackTrace();
		System.err.flush();
		System.out.flush();
	}
	
	/**
	 * Prints a log message separator. 
	 */
	public static void printSep(){
		System.out.println("++--------------------------------------------------------------------------------------------------------------++");
	}
	
	public static String replace(String text, String from, String to){
		
		String retval = text;
		int length = from.length();
		int start = 0;
		while(start<length && retval.indexOf(from, start)!=-1){
			int pos = retval.indexOf(from, start);
			String begin = "";
			String end = "";
			if(pos>0){
				begin = retval.substring(start, pos);
			}
			if(pos<(retval.length() - 1 - from.length())){
				end = retval.substring(pos+length);
			}
			retval = begin + to + end; 
			start = pos + to.length(); 
		}
		
		return retval;
	}
}