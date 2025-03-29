import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.TimeZone;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemCommandListener;
import javax.microedition.lcdui.List;
import javax.microedition.midlet.MIDlet;
import javax.microedition.rms.RecordStore;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import cc.nnproject.json.JSONStream;

public class MP extends MIDlet implements CommandListener, ItemCommandListener, Runnable {

	private static final int RUN_SEND_MESSAGE = 4;
	private static final int RUN_VALIDATE_AUTH = 5;
	private static final int RUN_AVATARS = 6;
	private static final int RUN_UPDATES = 7;
	private static final int RUN_LOAD_FORM = 8;
	private static final int RUN_LOAD_LIST = 9;
	
	private static final String SETTINGS_RECORDNAME = "mp4config";
	private static final String AUTH_RECORDNAME = "mp4user";
	
	private static final String DEFAULT_INSTANCE_URL = "http://mp2.nnchan.ru/";
	
	private static final String API_VERSION = "5";
	
	static final Font largePlainFont = Font.getFont(0, 0, Font.SIZE_LARGE);
	static final Font medPlainFont = Font.getFont(0, 0, Font.SIZE_MEDIUM);
	static final Font medBoldFont = Font.getFont(0, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
	static final Font medItalicFont = Font.getFont(0, Font.STYLE_ITALIC, Font.SIZE_MEDIUM);
	static final Font medItalicBoldFont = Font.getFont(0, Font.STYLE_BOLD | Font.STYLE_ITALIC, Font.SIZE_MEDIUM);
	static final Font smallPlainFont = Font.getFont(0, 0, Font.SIZE_SMALL);
	static final Font smallBoldFont = Font.getFont(0, Font.STYLE_BOLD, Font.SIZE_SMALL);
	static final Font smallItalicFont = Font.getFont(0, Font.STYLE_ITALIC, Font.SIZE_SMALL);

	static final IllegalStateException cancelException = new IllegalStateException("cancel");
	
	// midp lifecycle
	static MP midlet;
	private static Display display;
	static Displayable current;

	private static String version;

	// localization
	static String[] L;
	
	// settings
	private static String instance = DEFAULT_INSTANCE_URL;
	private static int tzOffset;
	private static boolean showMedia;
	private static boolean avatars;
	private static boolean symbianJrt;
	static boolean useLoadingForm;

	// threading
	private static int run;
	private static Object runParam;
//	private static int running;
	private static boolean avatarsRunning;
	private static boolean updatesRunning;
	
	// auth
	private static String user;
	private static int userState;

	// commands
	private static Command exitCmd;
	static Command backCmd;

	private static Command settingsCmd;
	private static Command aboutCmd;
	
	private static Command authCmd;
	private static Command authNewSessionCmd;
	private static Command authImportSessionCmd;

	static Command peerItemCmd;
	
	static Command writeCmd;
	static Command sendCmd;
	static Command updateCmd;

	private static Command okCmd;
	private static Command nextCmd;
	private static Command cancelCmd;
	
	// ui
	private static Displayable mainDisplayable;
	static Form loadingForm;
	private static Vector formHistory = new Vector();

	// ui elements
//	private static TextField tokenField;
//	
//	private static JSONArray dialogs;

	private static JSONObject usersCache = new JSONObject();
	private static JSONObject chatsCache = new JSONObject();

	protected void destroyApp(boolean u) {
	}

	protected void pauseApp() {
	}

	protected void startApp()  {
		if (midlet != null) return;
		midlet = this;

		version = getAppProperty("MIDlet-Version");
		display = Display.getDisplay(this);
		
		
		String p = System.getProperty("microedition.platform");
		symbianJrt = p != null && p.indexOf("platform=S60") != -1;
		useLoadingForm = !symbianJrt &&
				(System.getProperty("com.symbian.midp.serversocket.support") != null ||
				System.getProperty("com.symbian.default.to.suite.icon") != null);
		
		// load settings
		try {
			RecordStore r = RecordStore.openRecordStore(SETTINGS_RECORDNAME, false);
			JSONObject j = JSONObject.parseObject(new String(r.getRecord(1), "UTF-8"));
			r.closeRecordStore();
			
			// TODO
		} catch (Exception ignored) {}
		
		// load auth
		try {
			RecordStore r = RecordStore.openRecordStore(AUTH_RECORDNAME, false);
			JSONObject j = JSONObject.parseObject(new String(r.getRecord(1), "UTF-8"));
			r.closeRecordStore();

			user = j.getString("user", user);
			userState = j.getInt("userState", 0);
		} catch (Exception ignored) {}
	
		
		// load locale TODO
//		(L = new String[200])[0] = "mpgram";
//		try {
//			loadLocale(lang);
//		} catch (Exception e) {
//			try {
//				loadLocale(lang = "en");
//			} catch (Exception e2) {
//				// crash on fail
//				throw new RuntimeException(e2.toString());
//			}
//		}
		
		// commands
		
		exitCmd = new Command("Exit", Command.EXIT, 10);
		backCmd = new Command("Back", Command.BACK, 10);
		
		settingsCmd = new Command("Settings", Command.SCREEN, 3);
		aboutCmd = new Command("About", Command.SCREEN, 4);
		
		authCmd = new Command("Auth", Command.ITEM, 1);
		authNewSessionCmd = new Command("New session", Command.SCREEN, 1);
		authImportSessionCmd = new Command("Import session", Command.SCREEN, 2);
		
		peerItemCmd = new Command("Peer", Command.ITEM, 1);
		
		writeCmd = new Command("Write", Command.SCREEN, 2);
		sendCmd = new Command("Send", Command.OK, 1);
		updateCmd = new Command("Update", Command.SCREEN, 3);
		
		okCmd = new Command("Ok", Command.OK, 1);
		nextCmd = new Command("Next", Command.OK, 1);
		cancelCmd = new Command("Cancel", Command.CANCEL, 2);
		
		loadingForm = new Form("mpgram");
		loadingForm.append("Loading");
		loadingForm.addCommand(cancelCmd);
		loadingForm.setCommandListener(this);
		
		Form f = new Form("mpgram");
		f.append("Loading");
		display(mainDisplayable = f);
		
		try {
			tzOffset = TimeZone.getDefault().getRawOffset() / 1000;
		} catch (Throwable e) {} // just to be sure
		
		if (user == null) {
			display(mainDisplayable = initialAuthForm());
			return;
		} else {
			run = RUN_VALIDATE_AUTH;
			run();
		}

		start(RUN_AVATARS, null);
		
		ChatsList l = new ChatsList("Chats");
		l.removeCommand(backCmd);
		l.addCommand(backCmd);
		l.addCommand(aboutCmd);
		l.addCommand(settingsCmd);
		
		start(RUN_LOAD_LIST, mainDisplayable = l);
		display(l);
	}
	
	public void run() {
		int run;
		Object param;
		synchronized (this) {
			run = MP.run;
			param = MP.runParam;
			notify();
		}
		System.out.println("run " + run + " " + param);
//		running++;
		switch (run) {
		case RUN_VALIDATE_AUTH: {
			display(loadingAlert("Authorizing"), null);
			
			try {
				api("me");
				
//				if (updatesRunning) break;
//				start(RUN_UPDATES, null);
			} catch (APIException e) {
				
			} catch (IOException e) {
				
				break;
			}
			break;
		}
		case RUN_UPDATES: {
			// TODO
			if (updatesRunning) break;
			updatesRunning = true;
			try {
				while (true) {
					
					Thread.sleep(30000L);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		}
		case RUN_AVATARS: {
			if (avatarsRunning) break;
			avatarsRunning = true;
			try {
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		}
		case RUN_LOAD_FORM: {
			((MPForm) param).load();
			break;
		}
		case RUN_LOAD_LIST: {
			((MPList) param).load();
			break;
		}
		}
//		running--;
	}

	Thread start(int i, Object param) {
		Thread t = null;
		try {
			synchronized(this) {
				run = i;
				runParam = param;
				(t = new Thread(this)).start();
				wait();
			}
		} catch (Exception e) {}
		return t;
	}

	public void commandAction(Command c, Displayable d) {
		if (d instanceof MPList && c == List.SELECT_COMMAND) {
			((MPList) d).select(((List) d).getSelectedIndex());
			return;
		}
		if (c == backCmd) {
			if (formHistory.size() == 0) {
				display(null, true);
				return;
			}
			Displayable p = null;
			synchronized (formHistory) {
				int i = formHistory.size();
				while (i-- != 0) {
					if (formHistory.elementAt(i) == d) {
						break;
					}
				}
				if (i > 0) {
					p = (Displayable) formHistory.elementAt(i - 1);
					formHistory.removeElementAt(i);
				}
			}
			display(p, true);
			return;
		}
		if (c == exitCmd) {
			notifyDestroyed();
		}
	}
	
	public void commandAction(Command c, Item item) {
		if (c == peerItemCmd) {
			String id = (String) ((MPForm) current).ids.get(item);
			if (id == null) return;
			openChat(id);
			
			return;
		}
	}

	private static void writeAuth() {
		try {
			RecordStore.deleteRecordStore(AUTH_RECORDNAME);
		} catch (Exception ignored) {}
		try {
			JSONObject j = new JSONObject();
			
			j.put("user", user);
			j.put("state", userState);
			
			byte[] b = j.toString().getBytes("UTF-8");
			RecordStore r = RecordStore.openRecordStore(AUTH_RECORDNAME, true);
			r.addRecord(b, 0, b.length);
			r.closeRecordStore();
		} catch (Exception e) {}
	}
	
	static void queueAvatar(String id, Object target) {
		// TODO
	}

	static void fillPeersCache(JSONObject users, JSONObject chats) {
		if (users != null && usersCache != null) {
			if (usersCache.size() > 200) {
				usersCache.clear();
			}
			for (Enumeration e = users.keys(); e.hasMoreElements(); ) {
				String k = (String) e.nextElement();
				if ("0".equals(k)) continue;
				usersCache.put(k, (JSONObject) users.get(k));
			}
		}
		if (chats != null && chatsCache != null) {
			if (chatsCache.size() > 200) {
				chatsCache.clear();
			}
			for (Enumeration e = chats.keys(); e.hasMoreElements(); ) {
				String k = (String) e.nextElement();
				if ("0".equals(k)) continue;
				chatsCache.put(k, (JSONObject) chats.get(k));
			}
		}
	}
	
	static StringBuffer appendOneLine(StringBuffer sb, String s) {
		if (s == null) return sb;
		int i = 0, l = s.length();
		while (i < l && i < 64) {
			char c = s.charAt(i++);
			if (c == '\r') continue;
			if (c != '\n') sb.append(c);
			else sb.append(' ');
		}
		return sb;
	}
	
	static String getName(String id) {
		return getName(id, false, true);
	}
	
	static String getShortName(String id) {
		return getName(id, true, true);
	}
	
	static String getName(String id, boolean variant, boolean loadIfNeeded) {
		String res;
		if (id.charAt(0) == '-') {
			res = chatsCache.getObject(id).getString("title");
		} else {
			JSONObject o = usersCache.getObject(id);
			res = variant ? getShortName(o) : getName(o);
		}
		if (res == null) {
			if (!loadIfNeeded) return null;
			// TODO put to load queue
			throw new RuntimeException("Not implemented");
		}
		return res;
	}
	
	static String getNameLater(String id, Object target, boolean variant) {
		String r = getName(id, variant, false);
		if (r != null) {
			return r;
		}
		// TODO
		return null;
	}
	
	static String getName(JSONObject p) {
		if (p == null) return null;
		if (p.has("title")) {
			return p.getString("title");
		}
		
		String fn = p.getString("first_name");
		String ln = p.getString("last_name");
		
		if (fn != null && ln != null) {
			return fn.concat(" ").concat(ln);
		}
		
		if (ln != null) {
			return ln;
		}
		
		if (fn != null) {
			return fn;
		}
		
		return "Deleted";
	}
	
	private static String getShortName(JSONObject p) {
		if (p.has("title")) {
			return p.getString("title");
		}
		
		String fn = p.getString("first_name");
		String ln = p.getString("last_name");
		
		if (fn != null) {
			return fn;
		}
		
		if (ln != null) {
			return ln;
		}
		
		return "Deleted";
	}
	
	static Form initialAuthForm() {
		Form f = new Form("Auth");
		f.setCommandListener(midlet);
		f.addCommand(exitCmd);
		f.addCommand(settingsCmd);
		
		return f;
	}
	
	static void openChat(String id) {
		Form f = new ChatForm(id);
		display(f);
		midlet.start(RUN_LOAD_FORM, f);
	}
	
	static void display(Alert a, Displayable d) {
		if (d == null) {
			display.setCurrent(a);
			return;
		}
		if (display.getCurrent() != d) {
			display(d);
		}
		display.setCurrent(a, d);
	}
	
	static void display(Displayable d) {
		display(d, false);
	}

	static void display(Displayable d, boolean back) {
		System.out.println("display " + d);
		if (d instanceof Alert) {
			display.setCurrent((Alert) d, mainDisplayable);
			return;
		}
		if (d == loadingForm) {
			display.setCurrent(d);
			return;
		}
		if (d == null || d == mainDisplayable) {
			d = mainDisplayable;
			
			formHistory.removeAllElements();
		}
		Displayable p = display.getCurrent();
		if (p == loadingForm) p = current;
		display.setCurrent(current = d);
		if (p == null || p == d) return;
		
		if (p instanceof MPForm) {
			((MPForm) p).closed(back);
		}
		// push to history
		if (!back && d != mainDisplayable && (formHistory.isEmpty() || formHistory.lastElement() != d)) {
			formHistory.addElement(d);
		}
	}

	static Alert errorAlert(String text) {
		Alert a = new Alert("");
		a.setType(AlertType.ERROR);
		a.setString(text);
		a.setTimeout(3000);
		return a;
	}

	private static Alert infoAlert(String text) {
		Alert a = new Alert("");
		a.setType(AlertType.CONFIRMATION);
		a.setString(text);
		a.setTimeout(1500);
		return a;
	}

	private static Alert loadingAlert(String s) {
		Alert a = new Alert("", s, null, null);
		a.setCommandListener(midlet);
		a.addCommand(Alert.DISMISS_COMMAND);
		a.setIndicator(new Gauge(null, false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING));
		a.setTimeout(Alert.FOREVER);
		return a;
	}
	
	static Object api(String url) throws IOException {
		Object res;

		HttpConnection hc = null;
		InputStream in = null;
		try {
			hc = openHttpConnection(instance.concat("api.php?v=" + API_VERSION + "&method=").concat(url));
			hc.setRequestMethod("GET");
			
			int c = hc.getResponseCode();
			try {
				res = JSONStream.getStream(in = hc.openInputStream()).nextValue();
			} catch (RuntimeException e) {
				if (c >= 400) {
					throw new APIException(url, c, null);
				} else throw e;
			}
			if (c >= 400 || (res instanceof JSONObject && ((JSONObject) res).has("error"))) {
				throw new APIException(url, c, res);
			}
		} finally {
			if (in != null) try {
				in.close();
			} catch (IOException e) {}
			if (hc != null) try {
				hc.close();
			} catch (IOException e) {}
		}
		System.out.println(res instanceof JSONObject ?
				((JSONObject) res).format(0) : res instanceof JSONArray ?
						((JSONArray) res).format(0) : res);
		return res;
	}

	static JSONStream apiStream(String url) throws IOException {
		JSONStream res = null;

		HttpConnection hc = null;
		InputStream in = null;
		try {
			hc = openHttpConnection(instance.concat("api.php?v=" + API_VERSION + "&method=").concat(url));
			hc.setRequestMethod("GET");
			
			int c = hc.getResponseCode();
			if (c >= 400) {
				throw new APIException(url, c, null);
			}
			res = JSONStream.getStream(hc);
		} finally {
			if (res == null) {
				if (in != null) try {
					in.close();
				} catch (IOException e) {}
				if (hc != null) try {
					hc.close();
				} catch (IOException e) {}
			}
		}
		return res;
	}

	private static Image getImage(String url) throws IOException {
		byte[] b = get(url);
		return Image.createImage(b, 0, b.length);
	}
	
	private static byte[] readBytes(InputStream inputStream, int initialSize, int bufferSize, int expandSize)
			throws IOException {
		if (initialSize <= 0) initialSize = bufferSize;
		byte[] buf = new byte[initialSize];
		int count = 0;
		byte[] readBuf = new byte[bufferSize];
		int readLen;
		while ((readLen = inputStream.read(readBuf)) != -1) {
			if (count + readLen > buf.length) {
				System.arraycopy(buf, 0, buf = new byte[count + expandSize], 0, count);
			}
			System.arraycopy(readBuf, 0, buf, count, readLen);
			count += readLen;
		}
		if (buf.length == count) {
			return buf;
		}
		byte[] res = new byte[count];
		System.arraycopy(buf, 0, res, 0, count);
		return res;
	}
	
	private static String readUtf(InputStream in, int i) throws IOException {
		byte[] buf = new byte[i <= 0 ? 1024 : i];
		i = 0;
		int j;
		while ((j = in.read(buf, i, buf.length - i)) != -1) {
			if ((i += j) >= buf.length) {
				System.arraycopy(buf, 0, buf = new byte[i + 2048], 0, i);
			}
		}
		return new String(buf, 0, i, "UTF-8");
	}
	
	private static byte[] get(String url) throws IOException {
		HttpConnection hc = null;
		InputStream in = null;
		try {
			hc = openHttpConnection(url);
			hc.setRequestMethod("GET");
			int r;
			if ((r = hc.getResponseCode()) >= 400) {
				throw new IOException("HTTP ".concat(Integer.toString(r)));
			}
			in = hc.openInputStream();
			return readBytes(in, (int) hc.getLength(), 8*1024, 16*1024);
		} finally {
			try {
				if (in != null) in.close();
			} catch (IOException e) {}
			try {
				if (hc != null) hc.close();
			} catch (IOException e) {}
		}
	}
	
	private static HttpConnection openHttpConnection(String url) throws IOException {
		HttpConnection hc = (HttpConnection) Connector.open(url);
		hc.setRequestProperty("User-Agent", "mpgram4/".concat(version));
		if (user != null) {
			hc.setRequestProperty("X-mpgram-user", user);
		}
		return hc;
	}
	
	public static String url(String url) {
		return appendUrl(new StringBuffer(), url).toString();
	}

	public static StringBuffer appendUrl(StringBuffer sb, String url) {
		char[] chars = url.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			int c = chars[i];
			if (65 <= c && c <= 90) {
				sb.append((char) c);
			} else if (97 <= c && c <= 122) {
				sb.append((char) c);
			} else if (48 <= c && c <= 57) {
				sb.append((char) c);
			} else if (c == 32) {
				sb.append("%20");
			} else if (c == 45 || c == 95 || c == 46 || c == 33 || c == 126 || c == 42 || c == 39 || c == 40
					|| c == 41) {
				sb.append((char) c);
			} else if (c <= 127) {
				sb.append('%');
				byte b = (byte) c;
				sb.append(Integer.toHexString(b >> 4 & 0xf));
				sb.append(Integer.toHexString(b & 0xf));
			} else if (c <= 2047) {
				sb.append('%');
				byte b = (byte) (0xC0 | c >> 6);
				sb.append(Integer.toHexString(b >> 4 & 0xf));
				sb.append(Integer.toHexString(b & 0xf));
				
				sb.append('%');
				b = (byte) (0x80 | c & 0x3F);
				sb.append(Integer.toHexString(b >> 4 & 0xf));
				sb.append(Integer.toHexString(b & 0xf));
			} else {
				sb.append('%');
				byte b = (byte) (0xE0 | c >> 12);
				sb.append(Integer.toHexString(b >> 4 & 0xf));
				sb.append(Integer.toHexString(b & 0xf));
				
				sb.append('%');
				b = (byte) (0x80 | c >> 6 & 0x3F);
				sb.append(Integer.toHexString(b >> 4 & 0xf));
				sb.append(Integer.toHexString(b & 0xf));
				
				sb.append('%');
				b = (byte) (0x80 | c & 0x3F);
				sb.append(Integer.toHexString(b >> 4 & 0xf));
				sb.append(Integer.toHexString(b & 0xf));
			}
		}
		return sb;
	}

}
