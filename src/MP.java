/*
Copyright (c) 2022-2025 Arman Jussupgaliyev

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
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;
import java.util.TimeZone;
import java.util.Vector;

import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.ImageItem;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemCommandListener;
import javax.microedition.lcdui.ItemStateListener;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.Spacer;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import javax.microedition.midlet.MIDlet;
import javax.microedition.rms.RecordStore;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import cc.nnproject.json.JSONStream;

public class MP extends MIDlet implements CommandListener, ItemCommandListener, ItemStateListener, Runnable, LangConstants {

	static final int RUN_SEND_MESSAGE = 1;
	static final int RUN_VALIDATE_AUTH = 2;
	static final int RUN_IMAGES = 3;
	static final int RUN_LOAD_FORM = 4;
	static final int RUN_LOAD_LIST = 5;
	static final int RUN_AUTH = 6;
	static final int RUN_DELETE_MESSAGE = 7;
	static final int RUN_RESOLVE_INVITE = 8;
	static final int RUN_IMPORT_INVITE = 9;
	static final int RUN_JOIN_CHANNEL = 10;
	static final int RUN_LEAVE_CHANNEL = 11;
	static final int RUN_CHECK_OTA = 12;
	static final int RUN_CHAT_UPDATES = 13;
	static final int RUN_SET_TYPING = 14;
	static final int RUN_KEEP_ALIVE = 15;
	static final int RUN_CLOSE_CONNECTION = 16;
	static final int RUN_BOT_CALLBACK = 17;
	static final int RUN_BAN_MEMBER = 18;
	
	private static final String SETTINGS_RECORD_NAME = "mp4config";
	private static final String AUTH_RECORD_NAME = "mp4user";
	private static final String AVATAR_RECORD_PREFIX = "mcA";
	
	private static final String DEFAULT_INSTANCE_URL = "http://mp.nnchan.ru/";
	static final String API_URL = "api.php";
	static final String AVA_URL = "ava.php";
	static final String FILE_URL = "file.php";
	static final String OTA_URL = "http://nnproject.cc/mp/upd.php";
	
	static final String API_VERSION = "6";
	
	static final String[][] LANGS = {
		{
			"en",
			"es",
			"fi",
			"ru",
			"uk",
			"ar",
		},
		{
			"English",
			"Español",
			"Suomi",
			"Русский",
			"Українська",
			"العربية"
		}
	};
	
	static final Font largePlainFont = Font.getFont(0, 0, Font.SIZE_LARGE);
	static final Font medPlainFont = Font.getFont(0, 0, Font.SIZE_MEDIUM);
	static final Font medBoldFont = Font.getFont(0, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
	static final Font medItalicFont = Font.getFont(0, Font.STYLE_ITALIC, Font.SIZE_MEDIUM);
	static final Font medItalicBoldFont = Font.getFont(0, Font.STYLE_BOLD | Font.STYLE_ITALIC, Font.SIZE_MEDIUM);
	static final Font smallPlainFont = Font.getFont(0, 0, Font.SIZE_SMALL);
	static final Font smallBoldFont = Font.getFont(0, Font.STYLE_BOLD, Font.SIZE_SMALL);
	static final Font smallItalicFont = Font.getFont(0, Font.STYLE_ITALIC, Font.SIZE_SMALL);

	static final IllegalStateException cancelException = new IllegalStateException("cancel");
	
	static final int ONE_LINE_LENGTH = 50;
	
	// midp lifecycle
	static MP midlet;
	static Display display;
	static Displayable current;
	static boolean paused;

	private static String version;

	// localization
	static String[] L;
	
	// settings
	static String instanceUrl = DEFAULT_INSTANCE_URL;
	private static String instancePassword;
	private static int tzOffset;
	static boolean symbianJrt;
	static boolean useLoadingForm;
	private static int avatarSize;
	private static int photoSize = 120;
	static boolean loadAvatars = true;
	static boolean loadThumbs = true;
	static boolean reverseChat = true;
	static boolean showMedia = true;
	static int avatarsCache = 3; // 0 - off, 1 - hashtable, 2 - storage, 3 - both
	static boolean threadedImages;
	static int avatarsCacheThreshold = 20;
	static int chatsLimit = 20;
	static int messagesLimit = 20;
	static int peersCacheThreshold = 200;
	static boolean jsonStream = true;
	static boolean parseRichtext = true;
	static boolean parseLinks = true;
	static String lang = "en";
	static boolean checkUpdates = true; // ota
	static boolean chatUpdates = true;
	static boolean chatStatus = false;
	static boolean focusNewMessages = false;
	static long updatesDelay = 3000L;
	static int updatesTimeout = 30;
	static boolean sendTyping = true;
	static int chatsListFontSize = 0; // 0 - default, 1 - small, 2 - medium
	static boolean keepAlive = true;
	static boolean utf = true;
	static long keepAliveInterval = 30000L;
	static String deviceName;
	static String systemName;
	static boolean chatField;

	// threading
	private static int run;
	private static Object runParam;
//	private static int running;
	static Thread updatesThread;
	static Hashtable threadConnections = new Hashtable();
	static Vector closingConnections = new Vector();
	private static boolean sending;
	
	private static Object imagesLoadLock = new Object();
	private static Vector imagesToLoad = new Vector(); // TODO hashtable?
	
	// auth
	private static String user;
	private static int userState;
	private static String phone;
	static String selfId;

	// commands
	private static Command exitCmd;
	static Command backCmd;

	private static Command settingsCmd;
	private static Command aboutCmd;
	
	private static Command authCmd;
	static Command authNextCmd;
	private static Command authCodeCmd;
	private static Command authPasswordCmd;
	private static Command authNewSessionCmd;
	private static Command authImportSessionCmd;
	
	private static Command logoutCmd;
	private static Command clearCacheCmd;

	static Command refreshCmd;
	static Command archiveCmd;
	static Command foldersCmd;
	static Command contactsCmd;
	static Command searchChatsCmd;
	static Command openLinkCmd;

	static Command itemChatCmd;
	static Command itemChatInfoCmd;
	static Command replyMsgCmd;
	static Command forwardMsgCmd;
	static Command copyMsgCmd;
	static Command messageLinkCmd;
	static Command deleteMsgCmd;
	static Command editMsgCmd;
	static Command gotoMsgCmd;
	static Command botCallbackCmd;
	static Command banMemberCmd;
	
	static Command richTextLinkCmd;
	static Command openImageCmd;
	static Command callItemCmd;
	static Command documentCmd;

	static Command writeCmd;
	static Command chatInfoCmd;
	static Command olderMessagesCmd;
	static Command newerMessagesCmd;
	static Command latestCmd;
	static Command searchMsgCmd;
	
	static Command sendCmd;
	static Command openTextBoxCmd;
	static Command chooseFileCmd;
	
	static Command callCmd;
	static Command openChatCmd;
	static Command acceptInviteCmd;
	static Command joinChatCmd;
	static Command leaveChatCmd;
	static Command chatMediaCmd;
	static Command gotoPinnedMsgCmd;
	static Command chatMembersCmd;

	static Command okCmd;
	static Command cancelCmd;
	static Command goCmd;
	
	static Command nextPageCmd;
	static Command prevPageCmd;
	
	private static Command updateCmd;
	
	// ui
	private static Displayable mainDisplayable;
	static Form loadingForm;
	static ChatsList chatsList;
	static FoldersList foldersList;
	private static Form settingsForm;
	private static Form authForm;
	private static Form writeForm;
	private static Vector formHistory = new Vector();

	// auth items
	private static TextField instanceField;
	private static TextField instancePasswordField;
	
	// settings items
	private static ChoiceGroup imagesChoice;
	private static ChoiceGroup avaCacheChoice;
	private static ChoiceGroup uiChoice;
	private static ChoiceGroup behChoice;
	private static ChoiceGroup langChoice;
	private static ChoiceGroup chatsFontSizeCoice;
	private static Gauge avaCacheGauge;
	private static Gauge photoSizeGauge;
	private static Gauge profileCacheGauge;
	private static Gauge chatsGauge;
	private static Gauge msgsGauge;
	private static Gauge updateTimeoutGauge;
	
	// write items
	private static TextField messageField;
//	private static TextField fileField;
	private static ChoiceGroup sendChoice;
	private static StringItem fileLabel;

	// cache
	private static JSONObject usersCache = new JSONObject();
	private static JSONObject chatsCache = new JSONObject();
	private static Hashtable imagesCache = new Hashtable();
	
	private static Image userDefaultImg, chatDefaultImg;
	private static Image fileImg, folderImg;
	
	// temp
	private static String richTextUrl;
	private static String writeTo;
	private static String replyTo;
	private static String sendFile;
	private static String edit;
	private static String updateUrl;
	private static long lastType;
	
	// file picker
	private static Vector rootsList;
	
	protected void destroyApp(boolean u) {
	}

	protected void pauseApp() {
		paused = true;
	}

	protected void startApp()  {
		paused = false;
		if (midlet != null) return;
		midlet = this;

		version = getAppProperty("MIDlet-Version");
		display = Display.getDisplay(this);
		
		Form f = new Form("MPGram");
		f.append("Loading");
		display.setCurrent(mainDisplayable = f);
		
		try {
			// check for j2me loader
			Class.forName("javax.microedition.shell.MicroActivity");
			f.deleteAll();
			f.addCommand(exitCmd = new Command("Exit", Command.EXIT, 1));
			f.setCommandListener(midlet);
			f.append("J2ME Loader is not supported.");
			return;
		} catch (Exception ignored) {}
		
		// get device name
		String p, v;
		if ((p = System.getProperty("microedition.platform")) != null) {
			symbianJrt = p.indexOf("platform=S60") != -1;
			try {
				Class.forName("emulator.custom.CustomMethod");
				p = "KEmulator";
				if ((v = System.getProperty("kemulator.mod.version")) != null) {
					p = p.concat(" ".concat(v));
				}
			} catch (Exception e) {
				int i;
				
				if ((i = p.indexOf('/')) != -1 || (i = p.indexOf(' ')) != -1) {
					p = p.substring(0, i);
				}
			}
			deviceName = p;
		}
		if ((p = System.getProperty("os.name")) != null) {
			if ((v = System.getProperty("os.version")) != null) {
				p = p.concat(" ".concat(v));
			}
			systemName = p;
		}

		// init platform dependent settings
		useLoadingForm = !symbianJrt;
		jsonStream = symbianJrt ||
				((System.getProperty("com.symbian.midp.serversocket.support") == null &&
				System.getProperty("com.symbian.default.to.suite.icon") == null));
		threadedImages = symbianJrt;
		
		avatarSize = Math.min(display.getBestImageHeight(Display.LIST_ELEMENT), display.getBestImageWidth(Display.LIST_ELEMENT));
		if (avatarSize < 8) avatarSize = 16;
		else if (avatarSize > 120) avatarSize = 120;
		
		photoSize = Math.min(f.getWidth(), f.getHeight()) / 3;
		
		try {
			tzOffset = TimeZone.getDefault().getRawOffset() / 1000;
		} catch (Throwable e) {} // just to be sure
		
		// load settings
		try {
			RecordStore r = RecordStore.openRecordStore(SETTINGS_RECORD_NAME, false);
			JSONObject j = JSONObject.parseObject(new String(r.getRecord(1), "UTF-8"));
			r.closeRecordStore();
			
			reverseChat = j.getBoolean("reverseChat", reverseChat);
			loadAvatars = j.getBoolean("loadAvatars", loadAvatars);
			avatarSize = j.getInt("avatarSize", avatarSize);
			showMedia = j.getBoolean("showMedia", showMedia);
			photoSize = j.getInt("photoSize", photoSize);
			loadThumbs = j.getBoolean("loadThumbs", loadThumbs);
			threadedImages = j.getBoolean("threadedImages", threadedImages);
			avatarsCache = j.getInt("avatarsCache", avatarsCache);
			avatarsCacheThreshold = j.getInt("avatarsCacheThreshold", avatarsCacheThreshold);
			useLoadingForm = j.getBoolean("useLoadingForm", useLoadingForm);
			chatsLimit = j.getInt("chatsLimit", chatsLimit);
			messagesLimit = j.getInt("messagesLimit", messagesLimit);
			peersCacheThreshold = j.getInt("profilesCacheThreshold", peersCacheThreshold);
			jsonStream = j.getBoolean("jsonStream", jsonStream);
			parseRichtext = j.getBoolean("parseRichtext", parseRichtext);
			parseLinks = j.getBoolean("parseLinks", parseLinks);
			lang = j.getString("lang", lang);
			checkUpdates = j.getBoolean("checkUpdates", checkUpdates);
			chatUpdates = j.getBoolean("chatUpdates", chatUpdates);
			chatStatus = j.getBoolean("chatStatus", chatStatus);
			focusNewMessages = j.getBoolean("focusNewMessages", focusNewMessages);
			updatesDelay = j.getLong("updatesDelay", updatesDelay);
			updatesTimeout = j.getInt("updatesTimeout", updatesTimeout);
			chatsListFontSize = j.getInt("chatsListFontSize", chatsListFontSize);
			keepAlive = j.getBoolean("keepAlive", keepAlive);
			chatField = j.getBoolean("chatField", chatField);
		} catch (Exception ignored) {}
		
		// load auth
		try {
			RecordStore r = RecordStore.openRecordStore(AUTH_RECORD_NAME, false);
			JSONObject j = JSONObject.parseObject(new String(r.getRecord(1), "UTF-8"));
			r.closeRecordStore();

			user = j.getString("user", user);
			userState = j.getInt("state", 0);
			phone = j.getString("phone", null);
			instanceUrl = j.getString("url", instanceUrl);
			instancePassword = j.getString("instPass", instancePassword);
		} catch (Exception ignored) {}
	
		
		// load locale
		(L = new String[300])[0] = "MPGram";
		try {
			loadLocale(lang);
		} catch (Exception e) {
			e.printStackTrace();
			try {
				loadLocale(lang = "en");
			} catch (Exception e2) {
				// crash on fail
				throw new RuntimeException(e2.toString());
			}
		}
		
		// commands
		
		exitCmd = new Command(L[Exit], Command.EXIT, 15);
		backCmd = new Command(L[Back], Command.BACK, 15);
		
		settingsCmd = new Command(L[Settings], Command.SCREEN, 5);
		aboutCmd = new Command(L[About], Command.SCREEN, 6);
		
		authCmd = new Command(L[Auth], Command.ITEM, 1);
		authNextCmd = new Command(L[Next], Command.OK, 1);
		authCodeCmd = new Command(L[Next], Command.OK, 1);
		authPasswordCmd = new Command(L[Next], Command.OK, 1);
		authNewSessionCmd = new Command(L[NewSession], Command.SCREEN, 1);
		authImportSessionCmd = new Command(L[ImportSession], Command.SCREEN, 2);
		
		logoutCmd = new Command(L[Logout], Command.ITEM, 1);
		clearCacheCmd = new Command(L[ClearCache], Command.ITEM, 1);

		foldersCmd = new Command(L[Folders], Command.SCREEN, 4);
		refreshCmd = new Command(L[Refresh], Command.SCREEN, 5);
		archiveCmd = new Command(L[ArchivedChats], Command.SCREEN, 8);
		contactsCmd = new Command(L[Contacts], Command.SCREEN, 9);
		searchChatsCmd = new Command(L[Search], Command.SCREEN, 10);
		openLinkCmd = new Command(L[OpenByLink], Command.SCREEN, 11);
		
		itemChatCmd = new Command(L[OpenChat], Command.ITEM, 1);
		itemChatInfoCmd = new Command(L[Profile], Command.ITEM, 2);
		replyMsgCmd = new Command(L[Reply], Command.ITEM, 3);
		forwardMsgCmd = new Command(L[Forward], Command.ITEM, 4);
		copyMsgCmd = new Command(L[CopyMessage], Command.ITEM, 5);
		messageLinkCmd = new Command(L[CopyMessageLink], Command.ITEM, 7);
		deleteMsgCmd = new Command(L[Delete], Command.ITEM, 8);
		editMsgCmd = new Command(L[Edit], Command.ITEM, 9);
		gotoMsgCmd = new Command(L[GoTo], Command.ITEM, 1);
		botCallbackCmd = new Command("", Command.ITEM, 1); // TODO unlocalized
		banMemberCmd = new Command(L[BanMember], Command.ITEM, 10);
		
		richTextLinkCmd = new Command(L[Link_Cmd], Command.ITEM, 1);
		openImageCmd = new Command(L[ViewImage], Command.ITEM, 1);
		callItemCmd = new Command(L[Call], Command.ITEM, 1);
		documentCmd = new Command(L[Download], Command.ITEM, 1);
		
		writeCmd = new Command(L[WriteMessage], Command.SCREEN, 5);
		latestCmd = new Command(L[LatestMessages_Cmd], Command.SCREEN, 6);
		chatInfoCmd = new Command(L[ChatInfo], Command.SCREEN, 7);
		olderMessagesCmd = new Command(L[Older], Command.ITEM, 1);
		newerMessagesCmd = new Command(L[Newer], Command.ITEM, 1);
		searchMsgCmd = new Command(L[Search], Command.SCREEN, 10);
		
		sendCmd = new Command(L[Send], Command.OK, 1);
		openTextBoxCmd = new Command(L[OpenTextBox], Command.ITEM, 1);
		chooseFileCmd = new Command(L[ChooseFile], Command.ITEM, 1);
		
		callCmd = new Command(L[Call], Command.SCREEN, 5);
		openChatCmd = new Command(L[OpenChat], Command.SCREEN, 1);
		acceptInviteCmd = new Command(L[Join], Command.ITEM, 1);
		joinChatCmd = new Command(L[JoinGroup], Command.SCREEN, 1);
		leaveChatCmd = new Command(L[LeaveGroup], Command.ITEM, 1);
		chatMediaCmd = new Command(L[Media], Command.ITEM, 1);
		gotoPinnedMsgCmd = new Command(L[GoTo], Command.ITEM, 1);
		chatMembersCmd = new Command(L[Members], Command.SCREEN, 6);
		
		okCmd = new Command(L[Ok], Command.OK, 1);
		cancelCmd = new Command(L[Cancel], Command.CANCEL, 20);
		goCmd = new Command(L[Ok], Command.OK, 1);

		nextPageCmd = new Command(L[NextPage], Command.SCREEN, 6);
		prevPageCmd = new Command(L[PrevPage], Command.SCREEN, 7);
		
		updateCmd = new Command(L[Download], Command.OK, 1);
		
		loadingForm = new Form(L[mpgram]);
		loadingForm.append(L[Loading]);
		loadingForm.addCommand(cancelCmd);
		loadingForm.setCommandListener(this);
		
		// load resources
		
		if (loadAvatars) {
			try {
				userDefaultImg = resize(Image.createImage("/us.png"), avatarSize, avatarSize);
				chatDefaultImg = resize(Image.createImage("/gr.png"), avatarSize, avatarSize);
			} catch (Throwable ignored) {}
		}
		
		// start image loader threads

		start(RUN_IMAGES, null);

		if (threadedImages) {
			start(RUN_IMAGES, null);
			start(RUN_IMAGES, null);
		}
		
		// create auth form
		
		f = new Form("Auth");
		f.addCommand(exitCmd);
		f.addCommand(aboutCmd);
		f.addCommand(settingsCmd);
		f.setCommandListener(midlet);
		
		TextField t = new TextField(L[InstanceURL], instanceUrl, 200, TextField.URL);
		instanceField = t;
		f.append(t);
		
		t = new TextField(L[InstancePassword], instancePassword, 200, TextField.NON_PREDICTIVE);
		instancePasswordField = t;
		f.append(t);
		
		StringItem s = new StringItem(null, L[Auth_Btn], StringItem.BUTTON);
		s.setDefaultCommand(authCmd);
		s.setItemCommandListener(midlet);
		s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
		f.append(s);
		
		authForm = f;
		
		// load main form
		
		if (user == null || userState < 3) {
			display(mainDisplayable = authForm);
		} else {
			run = RUN_VALIDATE_AUTH;
			run();
			
			if (selfId != null) {
				openLoad(mainDisplayable = mainChatsList());
			}
		}
		
		start(RUN_CHECK_OTA, null);
	}

	public void itemStateChanged(Item item) {
		if (item == messageField
				|| (current instanceof ChatForm && item instanceof TextField)) {
			if (!sendTyping) return;
			long l = System.currentTimeMillis();
			if (l - lastType < 5000L) return;
			
			lastType = l;
			start(RUN_SET_TYPING, ((TextField) item).getString().length() == 0 ? "Cancel" : null);
		}
	}
	
	// threading
	public void run() {
		int run;
		Object param;
		synchronized (this) {
			run = MP.run;
			param = MP.runParam;
			notify();
		}
//		running++;
		switch (run) {
		case RUN_VALIDATE_AUTH: {
			display(loadingAlert(L[Authorizing]), null);
			
			try {
				selfId = ((JSONObject) api("me&status=1")).getString("id");
				userState = 4;

				if (param != null) {
					openLoad(mainDisplayable = mainChatsList());
					writeAuth();
				}
				
				start(RUN_KEEP_ALIVE, null);
			} catch (APIException e) {
				if (param != null) mainDisplayable = authForm;
				if (e.code == 401) {
					userState = 0;
					user = null;
					display(errorAlert(e.toString()), mainDisplayable);
					break;
				}
				display(errorAlert(e.toString()), mainDisplayable);
			} catch (Exception e) {
				display(errorAlert(e.toString()), mainDisplayable);
			}
			break;
		}
		case RUN_IMAGES: { // avatars loading loop
			try {
				while (true) {
					synchronized (imagesLoadLock) {
						imagesLoadLock.wait();
					}
					Thread.sleep(200);
					while (imagesToLoad.size() > 0) {
						Object[] o = null;
						
						try {
							synchronized (imagesLoadLock) {
								o = (Object[]) imagesToLoad.elementAt(0);
								imagesToLoad.removeElementAt(0);
							}
						} catch (Exception e) {
							continue;
						}
						
						if (o == null) continue;
						
						Object src = (Object) o[0];
						Object target = o[1];
						
						if (src == null) continue;
						
						try {
							String url;
							Image img = null;
							String recordName = null;
							if (src instanceof String) { // avatar
								recordName = AVATAR_RECORD_PREFIX + avatarSize + "r" + (String) src;
								url = instanceUrl + AVA_URL + "?a&c=" + ((String) src) + "&p=r" + avatarSize;

								// load avatar from cache
								if ((avatarsCache & 1) == 1 && imagesCache.containsKey(src)) {
									img = (Image) imagesCache.get(src);
								} else if ((avatarsCache & 2) == 2) {
									try {
										RecordStore r = RecordStore.openRecordStore(recordName, false);
										try {
											byte[] b = r.getRecord(1);
											img = Image.createImage(b, 0, b.length);
										} finally {
											r.closeRecordStore();
										}
									} catch (Exception ignored) {}
								}
							} else if (src instanceof String[]) { // message file
								String peer = ((String[]) src)[0];
								String id = ((String[]) src)[1];
								String p = ((String[]) src)[3];
								url = instanceUrl + FILE_URL + "?a&c=" + peer + "&m=" + id + "&p=" + p + "&s=" + photoSize;
							} else {
								continue;
							}
							if (img == null) {
								try {
									byte[] b = get(url);
									if (recordName != null) {
										// save avatar to storage cache
										if ((avatarsCache & 2) == 2) {
											try {
												RecordStore.deleteRecordStore(recordName);
											} catch (Exception ignored) {}
											try {
												RecordStore r = RecordStore.openRecordStore(recordName, true);
												try {
													r.addRecord(b, 0, b.length);
												} finally {
													r.closeRecordStore();
												}
											} catch (Exception ignored) {}
										}
									}
									img = Image.createImage(b, 0, b.length);
								} catch (Exception e) {
									e.printStackTrace();
									if (src instanceof String) {
										img = ((String) src).charAt(0) == '-' ? chatDefaultImg : userDefaultImg;
									}
								}
							}
							
							if (img == null) continue;
							
							// save avatar to hashtable cache
							if (recordName != null && (avatarsCache & 1) == 1) {
								if (imagesCache.size() > avatarsCacheThreshold) {
									imagesCache.clear();
								}
								imagesCache.put(src, img);
							}
							
							putImage(target, img);
						} catch (Exception e) {
							e.printStackTrace();
						} 
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}
		case RUN_LOAD_FORM: {
			((MPForm) param).load();
			break;
		}
		case RUN_LOAD_LIST: {
			((MPList) param).load();
			break;
		}
		case RUN_AUTH: {
			StringBuffer sb = new StringBuffer();
			try {
				if (param instanceof CaptchaForm) {
					sb.append(user == null ? "initLogin" : "phoneLogin")
						.append("&captcha_id=").append(((CaptchaForm) param).id)
						.append("&captcha_key=");
					appendUrl(sb, ((CaptchaForm) param).field.getString())
					.append("&phone=");
					appendUrl(sb, phone);
					
					JSONObject j = (JSONObject) api(sb.toString());
					String res = j.getString("res");
					if (j.has("user")) {
						user = j.getString("user");
					}
					if (res.indexOf("captcha") != -1) {
						display(errorAlert(res), null);
						((CaptchaForm) param).load();
						break;
					}
					if (!"code_sent".equals(res)) {
						userState = 1;
						display(errorAlert(res), null);
						break;
					}

					writeAuth();
					TextBox t = new TextBox(L[Code], "", 5, TextField.NUMERIC);
					t.addCommand(authCodeCmd);
					t.setCommandListener(this);
					display(t);
				} else {
					if (userState == 2) {
						// check cloud password
						sb.append("complete2faLogin&password=");
						appendUrl(sb, (String) param);
					
						try {
							JSONObject j = (JSONObject) api(sb.toString());
							String res = j.getString("res");
							if (j.has("user")) {
								user = j.getString("user");
							}
							
							if (!"1".equals(res)) {
								display(errorAlert(res), null);
								break;
							}
						} catch (APIException e) {
							commandAction(backCmd, current);
							display(errorAlert(e.toString()), null);
							break;
						}
					} else {
						// check code
						sb.append("completePhoneLogin&code=").append((String) param);
					
						JSONObject j = (JSONObject) api(sb.toString());
						String res = j.getString("res");
						if (j.has("user")) {
							user = j.getString("user");
						}
						if ("password".equals(res)) {
							userState = 2;
							writeAuth();
							TextBox t = new TextBox(L[CloudPassword], "", 100, TextField.NON_PREDICTIVE);
							t.addCommand(authPasswordCmd);
							t.setCommandListener(this);
							display(t);
							break;
						}
						if (!"1".equals(res)) {
							display(errorAlert(res), null);
							break;
						}
					}
					
					// auth complete
					userState = 3;
					MP.run = RUN_VALIDATE_AUTH;
					run();
				}

			} catch (Exception e) {
				display(errorAlert(e.toString()), null);
			}
			break;
		}
		case RUN_DELETE_MESSAGE: {
			try {
				String[] s = (String[]) param;
				MP.api("deleteMessage&peer=".concat(s[0].concat("&id=").concat(s[1])));

				// refresh chat after deleting
				commandAction(refreshCmd, current);
//				display(infoAlert(L[MessageDeleted_Alert]), current);
			} catch (Exception e) {
				display(errorAlert(e.toString()), current);
			}
			break;
		}
		case RUN_SEND_MESSAGE: {
			try {
				String text = (String) ((Object[]) param)[0];
				String writeTo = (String) ((Object[]) param)[1];
				String replyTo = (String) ((Object[]) param)[2];
				String edit = (String) ((Object[]) param)[3];
				String file = (String) ((Object[]) param)[4];
				ChoiceGroup sendChoice = (ChoiceGroup) ((Object[]) param)[5];
				if (file != null && file.length() <= 8) {
					file = null;
				}
				StringBuffer sb;
				if (edit != null) {
					sb = new StringBuffer(edit != null ? "editMessage" : "sendMessage");
					sb.append("&peer=").append(writeTo);
					if (edit != null) {
						sb.append("&id=").append(edit);
					}
					if (replyTo != null) {
						sb.append("&reply=").append(replyTo);
					}
				} else {
					sb = new StringBuffer(file != null ? "sendMedia" : "sendMessage");
					sb.append("&peer=").append(writeTo);
					if (replyTo != null) {
						sb.append("&reply=").append(replyTo);
					}
					if (sendChoice != null) {
						if (sendChoice.isSelected(0)) {
							sb.append("&uncompressed=1");
						}
						if (sendChoice.isSelected(1)) {
							sb.append("&spoiler=1");
						}
					}
				}
				postMessage(sb.toString(), file, text);
				
				// go to latest message after sending
				if (!(current instanceof ChatForm)) {
					commandAction(backCmd, current);
				} else if (((ChatForm) current).textField != null) {
					((ChatForm) current).textField.setString("");
				}
				commandAction(latestCmd, current);
//				display(infoAlert(L[MessageSent_Alert]), current);
			} catch (Exception e) {
				e.printStackTrace();
				display(errorAlert(e.toString()), current);
			} finally {
				sending = false;
			}
			break;
		}
		case RUN_RESOLVE_INVITE: {
			try {
				JSONObject r = ((JSONObject) MP.api("checkChatInvite&id=".concat((String) param))).getObject("res");
				JSONObject rawPeer = r.getObject("chat");
				String id = rawPeer.getString("id");
				String type = r.getString("_");
				if ("chatInviteAlready".equals(type)) {
					// already in chat, just open it
					openChat(id, 0);
					break;
				}
				
				openLoad(new ChatInfoForm(id, (String) param, getNameRaw(rawPeer), "chatInvitePeek".equals(type) ? 2 : 3));
			} catch (Exception e) {
				display(errorAlert(e.toString()), current);
			}
			break;
		}
		case RUN_IMPORT_INVITE: { // join chat by invite link
			try {
				ChatInfoForm d = (ChatInfoForm) param;
				MP.api("importChatInvite&id=".concat(d.invite));
				
				commandAction(backCmd, current);
				openChat(d.id, 0);
			} catch (Exception e) {
				display(errorAlert(e.toString()), current);
			}
			break;
		}
		case RUN_JOIN_CHANNEL: 
		case RUN_LEAVE_CHANNEL: {
			try {
				MP.api((run == RUN_JOIN_CHANNEL ? "join" : "leave").concat("channel&id=").concat((String) param));
				
				if (run == RUN_JOIN_CHANNEL) {
					commandAction(backCmd, current);
					openChat((String) param, 0);
				} else {
					commandAction(refreshCmd, current);
				}
			} catch (Exception e) {
				display(errorAlert(e.toString()), current);
			}
			break;
		}
		case RUN_CHECK_OTA: { // check for client updates
			try {
				JSONObject j = JSONObject.parseObject(new String(get(OTA_URL + "?v=" + version + "&l=" + lang), "UTF-8"));
				if (j.getBoolean("update_available", false) && checkUpdates) {
					updateUrl = j.getString("download_url");
					Alert a = new Alert("", "", null, AlertType.INFO);
					a.setTimeout(-2);
					a.setString(j.getString("message", L[UpdateAvailable_Alert]));
					a.addCommand(cancelCmd);
					a.addCommand(updateCmd);
					a.setCommandListener(this);
					display(a);
				}
			} catch (Exception ignored) {}
			break;
		}
		case RUN_CHAT_UPDATES: { // chat updates loop
			Thread thread;
			updatesThread = thread = Thread.currentThread();
			try {
				StringBuffer sb = new StringBuffer();
				ChatForm form = (ChatForm) param;
				JSONObject j;

				int offset = 0;
				int fails = 0;
				boolean check = true;
				while (form.update && updatesThread == thread) {
					try {
						Thread.sleep(updatesDelay);
						if (!form.update || updatesThread != thread) break;
						if (check) {
							sb.setLength(0);
							sb.append("getLastUpdate&peer=").append(form.id);
							if (offset <= 0) {
								sb.append("&id=").append(form.firstMsgId);
							}
							int off = 0;
							try {
								j = ((JSONObject) api(sb.toString())).getObject("res");
								off = j.getInt("update_id");
								if (!j.getBoolean("exact", false))
									off -= 1;
								if (offset <= 0 || off < offset)
									offset = off;
							} catch (Exception ignored) {}
							check = false;
						}
						if (!form.update || updatesThread != thread) break;
						
						sb.setLength(0);
						sb.append("updates&media=1&read=1&peer=").append(form.id)
						.append("&offset=").append(offset)
						.append("&timeout=").append(updatesTimeout);
						
						j = (JSONObject) api(sb.toString());
						
						JSONArray updates = j.getArray("res");
						int l = updates.size();
						
						for (int i = 0; i < l; ++i) {
							JSONObject update = updates.getObject(i);
							offset = update.getInt("update_id");
							update = update.getObject("update");
							String type = update.getString("_");
							if ("updateUserStatus".equals(type)) {
								form.handleUpdate(ChatForm.UPDATE_USER_STATUS, update);
							} else if ("updateUserTyping".equals(type)
									|| "updateChatUserTyping".equals(type)
									|| "updateChannelUserTyping".equals(type)) {
								form.handleUpdate(ChatForm.UPDATE_USER_TYPING, update);
							} else if ("updateNewMessage".equals(type)
									|| "updateNewChannelMessage".equals(type)) {
								form.handleUpdate(ChatForm.UPDATE_NEW_MESSAGE, update);
							} else if ("updateDeleteChannelMessages".equals(type)) {
								form.handleUpdate(ChatForm.UPDATE_DELETE_MESSAGES, update);
							} else if ("updateEditMessages".equals(type)
									|| "updateEditChannelMessage".equals(type)) {
								form.handleUpdate(ChatForm.UPDATE_EDIT_MESSAGE, update);
							}
						}
						
					} catch (Exception e) {
						if (e.toString().indexOf("Interrupted") != -1) {
							form.update = false;
							break;
						}
						e.printStackTrace();
						fails++;
						check = true;
						if (fails >= 5 && form.update) {
							form.update = false;
							display(errorAlert("Updates thread died!\n" + e.toString()), null);
							break;
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (updatesThread == thread)
					updatesThread = null;
			}
			break;
		}
		case RUN_SET_TYPING: {
			try {
				api("setTyping&action=" + (param == null ? "Typing" : (String) param)
						+ "&peer=" + writeTo);
			} catch (Exception ignored) {}
			break;
		}
		case RUN_KEEP_ALIVE: { // Keep session alive
			try {
				boolean wasShown = true;
				while (keepAlive) {
					Thread.sleep(keepAliveInterval);
					if (threadConnections.size() != 0) continue;
					try {
						boolean shown = !paused && current.isShown();
						if (shown || wasShown != shown) {
							api(wasShown != shown ?
								("updateStatus".concat(!shown ? "&off=1" : "")) : "me");
						}
						wasShown = shown;
					} catch (Exception e) {}
				}
			} catch (Exception e) {}
			break;
		}
		case RUN_CLOSE_CONNECTION: {
			try {
				closingConnections.addElement(param);
				((Connection) param).close();
			} catch (Exception ignored) {}
			break;
		}
		case RUN_BOT_CALLBACK: {
			try {
				StringBuffer sb = new StringBuffer("botCallback");
				sb.append("&peer=").append(((String[]) param)[0]);
				sb.append("&id=").append(((String[]) param)[1]);
				sb.append("&data=").append(((String[]) param)[2]);
				
				JSONObject j = (JSONObject) api(sb.toString());
				System.out.println(j);
				// TODO alert

				commandAction(latestCmd, current);
			} catch (Exception e) {
				display(errorAlert(e.toString()), current);
			} finally {
				sending = false;
			}
			break;
		}
		case RUN_BAN_MEMBER: {
			try {
				String[] s = (String[]) param;
				MP.api("banMember&peer=".concat(s[0].concat("&id=").concat(s[2])));

				display(infoAlert(L[MemberBanned_Alert]), current);
			} catch (Exception e) {
				display(errorAlert(e.toString()), current);
			}
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
	
	static void cancel(Thread thread, boolean updates) {
		if (thread == null) return;
//		if (updates) updatesThread = null;
		thread.interrupt();
		if (symbianJrt)
			return;
		Connection c = (Connection) threadConnections.get(thread);
		if (c == null || closingConnections.contains(c))
			return;
		midlet.start(RUN_CLOSE_CONNECTION, c);
	}

	public void commandAction(Command c, Displayable d) {
		if (d instanceof ChatsList) { // chats list commands
			if (c == archiveCmd) {
				chatsList.changeFolder(1, L[Archive]);
				return;
			}
			if (c == foldersCmd) {
				if (foldersList == null) {
					openLoad(foldersList = new FoldersList());
				} else if (foldersList.size() == 0) {
					openLoad(foldersList);
				} else {
					display(foldersList);
				}
				return;
			}
			if (c == contactsCmd) {
				openLoad(new ChatsList(L[Contacts], "getContacts&fields=status", null));
				return;
			}
			if (c == nextPageCmd) {
				chatsList.paginate(1);
				return;
			}
			if (c == prevPageCmd) {
				chatsList.paginate(-1);
				return;
			}
			if (c == openLinkCmd) {
				TextBox t = new TextBox("", "https://t.me/", 200, TextField.URL);
				t.addCommand(cancelCmd);
				t.addCommand(goCmd);
				t.setCommandListener(this);
				
				display(t);
				return;
			}
			if (c == searchChatsCmd) {
				TextBox t = new TextBox(L[Search], "", 200, TextField.ANY);
				t.addCommand(cancelCmd);
				t.addCommand(searchChatsCmd);
				t.setCommandListener(this);
				
				display(t);
				return;
			}
		}
		if (d instanceof ChatForm) { // chat form commands
			if (c == latestCmd) {
				((ChatForm) d).reset();
				start(RUN_LOAD_FORM, d);
				return;
			}
			if (c == olderMessagesCmd || c == newerMessagesCmd) {
				((ChatForm) d).paginate(c == olderMessagesCmd ? -1 : 1);
				return;
			}
			if (c == chatInfoCmd) {
				openProfile(((ChatForm) d).id, (ChatForm) d, 0);
				return;
			}
			if (c == writeCmd) {
				display(writeForm(((ChatForm) d).id, null, "", null));
				return;
			}
			if (c == searchMsgCmd) {
				TextBox t = new TextBox(L[Search], "", 200, TextField.ANY);
				t.addCommand(cancelCmd);
				t.addCommand(searchMsgCmd);
				t.setCommandListener(this);
				
				display(t);
				return;
			}
			if (c == backCmd && ((ChatForm) d).query != null && ((ChatForm) d).switched) {
				// close search
				((ChatForm) current).reset();
				start(RUN_LOAD_FORM, current);
				return;
			}
		}
		if (d instanceof TextBox && c == searchMsgCmd) {
			commandAction(backCmd, d);
			if (current instanceof ChatForm) {
				((ChatForm) current).reset();
				((ChatForm) current).query = ((TextBox) d).getString();
				((ChatForm) current).switched = true;
				start(RUN_LOAD_FORM, current);
			} else {
				openLoad(new ChatForm(((ChatInfoForm) current).id, ((TextBox) d).getString(), 0, 0));
			}
			return;
		}
		if (d instanceof TextBox && c == searchChatsCmd) {
			commandAction(backCmd, d);
			openLoad(new ChatsList(L[Search],
					appendUrl(new StringBuffer("searchChats&q="), ((TextBox) d).getString()).toString(),
					"results"));
			return;
		}
		if (d instanceof ChatInfoForm) { // profile commands
			if (c == callCmd) {
				browse("tel:".concat(((ChatInfoForm) d).phone));
				return;
			}
			if (c == openChatCmd) {
				openChat(((ChatInfoForm) d).id, -1);
				return;
			}
			if (c == acceptInviteCmd) {
				start(RUN_IMPORT_INVITE, d);
				return;
			}
			if (c == joinChatCmd) {
				start(RUN_JOIN_CHANNEL, ((ChatInfoForm) d).id);
				return;
			}
			if (c == leaveChatCmd) {
				start(RUN_LEAVE_CHANNEL, ((ChatInfoForm) d).id);
				return;
			}
			if (c == searchMsgCmd) {
				TextBox t = new TextBox(L[Search], "", 200, TextField.ANY);
				t.addCommand(cancelCmd);
				t.addCommand(searchMsgCmd);
				t.setCommandListener(this);
				
				display(t);
				return;
			}
			if (c == chatMediaCmd) {
				openLoad(new ChatForm(((ChatInfoForm) current).id, "Photos"));
				return;
			}
			if (c == gotoPinnedMsgCmd) {
				int id = ((ChatInfoForm) current).pinnedMessageId;
				if (((ChatInfoForm) d).chatForm != null) {
					commandAction(backCmd, d);
					((ChatInfoForm) d).chatForm.openMessage(Integer.toString(id), 0);
				} else {
					openLoad(new ChatForm(((ChatInfoForm) current).id, null, id, 0));
				}
				return;
			}
			if (c == chatMembersCmd) {
				openLoad(new ChatsList(L[Members], "getParticipants&peer=" + ((ChatInfoForm) current).id + "&fields=status", null));
				return;
			}
		}
		{ // auth commands
			if (c == authCmd) {
				if (d instanceof TextBox) {
					// user code
					user = ((TextBox) d).getString();
					if (user.length() < 32) {
						display(errorAlert(""), null);
						return;
					}
					writeAuth();
					
					display(loadingAlert(L[WaitingForServerResponse]), null);
					start(RUN_VALIDATE_AUTH, user);
					return;
				}
				instanceUrl = instanceField.getString();
				if ((instancePassword = instancePasswordField.getString()).length() == 0) {
					instancePassword = null;
				}
				
				if (instanceUrl == null || instanceUrl.length() < 6 || !instanceUrl.startsWith("http")) {
					display(errorAlert(""), null);
				}
				writeAuth();
				
				Alert a = new Alert("", L[ChooseAuthMethod], null, null);
				a.addCommand(authImportSessionCmd);
				a.addCommand(authNewSessionCmd);
				a.setCommandListener(this);
				
				display(a, null);
				return;
			}
			if (c == authImportSessionCmd) {
				TextBox t = new TextBox(L[SessionCode], user == null ? "" : user, 200, TextField.NON_PREDICTIVE);
				t.addCommand(cancelCmd);
				t.addCommand(authCmd);
				t.setCommandListener(this);
				
				display(t);
				return;
			}
			if (c == authNewSessionCmd) {
				user = null;
				userState = 0;
				
				TextBox t = new TextBox(L[PhoneNumber], phone == null ? "" : phone, 30, TextField.PHONENUMBER);
				t.addCommand(cancelCmd);
				t.addCommand(authNextCmd);
				t.setCommandListener(this);
				
				display(t);
				return;
			}
			if (c == authNextCmd) {
				if (d instanceof TextBox) {
					// phone number
					phone = ((TextBox) d).getString();
					if (phone.length() < 10 && !phone.startsWith("+")) {
						display(errorAlert(""), null);
						return;
					}
					writeAuth();

					display(loadingAlert(L[WaitingForServerResponse]), null);
					openLoad(new CaptchaForm());
					return;
				}
				if (d instanceof CaptchaForm) {
					// captcha
					String key = ((CaptchaForm) d).field.getString();
					if (key.length() < 4) {
						display(errorAlert(""), null);
						return;
					}
					display(loadingAlert(L[WaitingForServerResponse]), null);
					start(RUN_AUTH, d);
					return;
				}
				return;
			}
			if (c == authCodeCmd) {
				// code
				String code = ((TextBox) d).getString();
				if (code.length() < 5) {
					display(errorAlert(""), null);
					return;
				}

				display(loadingAlert(L[WaitingForServerResponse]), null);
				start(RUN_AUTH, code);
				return;
			}
			if (c == authPasswordCmd) {
				// password
				String pass = ((TextBox) d).getString();
				if (pass.length() == 0) {
					display(errorAlert(""), null);
					return;
				}

				display(loadingAlert(L[WaitingForServerResponse]), null);
				start(RUN_AUTH, pass);
				return;
			}
		}
		{ // settings
			if (c == settingsCmd) {
				if (settingsForm == null) {
					Form f = new Form(L[Settings]);
					f.addCommand(backCmd);
					f.setCommandListener(this);
					StringItem s;
					
					// ui
					
					s = new StringItem(null, L[UI]);
					s.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					s.setFont(largePlainFont);
					f.append(s);
					
					langChoice = new ChoiceGroup(L[Language], Choice.POPUP, LANGS[1], null);
					for (int i = 0; i < LANGS[0].length; ++i) {
						if (lang.equals(LANGS[0][i])) {
							langChoice.setSelectedIndex(i, true);
							break;
						}
					}
					langChoice.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(langChoice);
					
					uiChoice = new ChoiceGroup("", Choice.MULTIPLE, new String[] {
							L[ReversedChat],
							L[ShowMedia],
							L[ShowChatStatus],
							L[FocusNewMessages],
							L[ChatTextField]
					}, null);
					uiChoice.setSelectedIndex(0, reverseChat);
					uiChoice.setSelectedIndex(1, showMedia);
					uiChoice.setSelectedIndex(2, chatStatus);
					uiChoice.setSelectedIndex(3, focusNewMessages);
					uiChoice.setSelectedIndex(4, chatField);
					uiChoice.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(uiChoice);
					
					photoSizeGauge = new Gauge(L[ThumbnailsSize], true, 64, photoSize / 8);
					photoSizeGauge.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(photoSizeGauge);
					
					chatsGauge = new Gauge(L[ChatsCount], true, 50, chatsLimit);
					chatsGauge.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(chatsGauge);
					
					msgsGauge = new Gauge(L[MessagesCount], true, 50, messagesLimit);
					msgsGauge.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(msgsGauge);
					
					chatsFontSizeCoice = new ChoiceGroup(L[ChatsListFontSize], Choice.POPUP, new String[] {
						L[Default],
						L[Small],
						L[Medium]
					}, null);
					chatsFontSizeCoice.setSelectedIndex(chatsListFontSize, true);
					f.append(chatsFontSizeCoice);
					
					// behaviour
					
					s = new StringItem(null, L[Behaviour]);
					s.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					s.setFont(largePlainFont);
					f.append(s);
					
					behChoice = new ChoiceGroup("", Choice.MULTIPLE, new String[] {
							L[WaitForPageToLoad],
							L[UseJSONStream],
							L[FormatText],
							L[ParseLinks],
							L[ChatAutoUpdate],
							L[KeepSessionAlive]
					}, null);
					behChoice.setSelectedIndex(0, useLoadingForm);
					behChoice.setSelectedIndex(1, jsonStream);
					behChoice.setSelectedIndex(2, parseRichtext);
					behChoice.setSelectedIndex(3, parseLinks);
					behChoice.setSelectedIndex(4, chatUpdates);
					behChoice.setSelectedIndex(5, keepAlive);
					behChoice.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(behChoice);
					
					updateTimeoutGauge = new Gauge(L[UpdatesTimeout], true, 20, updatesTimeout / 5);
					updateTimeoutGauge.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(updateTimeoutGauge);
					
					imagesChoice = new ChoiceGroup(L[Images], Choice.MULTIPLE, new String[] {
							L[LoadMediaThumbnails],
							L[LoadAvatars],
							L[MultiThreadedLoading]
					}, null);
					imagesChoice.setSelectedIndex(0, loadThumbs);
					imagesChoice.setSelectedIndex(1, loadAvatars);
					imagesChoice.setSelectedIndex(2, threadedImages);
					imagesChoice.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(imagesChoice);

					avaCacheChoice = new ChoiceGroup(L[AvatarsCaching], Choice.POPUP, new String[] {
							L[Disabled],
							L[HoldInRAM],
							L[Store],
							L[HoldInRAMandStore]
					}, null);
					avaCacheChoice.setSelectedIndex(avatarsCache, true);
					avaCacheChoice.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(avaCacheChoice);
					
					avaCacheGauge = new Gauge(L[AvatarsCacheThreshold], true, 20, avatarsCacheThreshold / 5);
					avaCacheGauge.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(avaCacheGauge);
					
					profileCacheGauge = new Gauge(L[ProfilesCacheThreshold], true, 30, peersCacheThreshold / 10);
					profileCacheGauge.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(profileCacheGauge);
					
					s = new StringItem(null, L[ClearCache], Item.BUTTON);
					s.setDefaultCommand(clearCacheCmd);
					s.setItemCommandListener(this);
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(s);
					
					// authorization
					
					s = new StringItem(null, L[Authorization]);
					s.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					s.setFont(largePlainFont);
					f.append(s);
					
					s = new StringItem(null, L[Logout], Item.BUTTON);
					s.setDefaultCommand(logoutCmd);
					s.setItemCommandListener(this);
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(s);
					
					settingsForm = f;
				}
				
				display(settingsForm);
				return;
			}
			if (c == backCmd && d == settingsForm) {
				// apply and save settings
				lang = LANGS[0][langChoice.getSelectedIndex()];
				
				reverseChat = uiChoice.isSelected(0);
				showMedia = uiChoice.isSelected(1);
				chatStatus = uiChoice.isSelected(2);
				focusNewMessages = uiChoice.isSelected(3);
				chatField = uiChoice.isSelected(4);
				
				if ((photoSize = (photoSizeGauge.getValue() * 8)) < 16) {
					photoSizeGauge.setValue((photoSize = 16) / 8);
				}
				if ((chatsLimit = chatsGauge.getValue()) < 5) {
					chatsGauge.setValue(chatsLimit = 5);
				}
				if ((messagesLimit = msgsGauge.getValue()) < 5) {
					msgsGauge.setValue(messagesLimit = 5);
				}
				
				chatsListFontSize = chatsFontSizeCoice.getSelectedIndex();
				
				useLoadingForm = behChoice.isSelected(0);
				jsonStream = behChoice.isSelected(1);
				parseRichtext = behChoice.isSelected(2);
				parseLinks = behChoice.isSelected(3);
				chatUpdates = behChoice.isSelected(4);
				keepAlive = behChoice.isSelected(5);
				
				if ((updatesTimeout = updateTimeoutGauge.getValue() * 5) < 5) {
					updateTimeoutGauge.setValue((updatesTimeout = 5) / 5);
				}
				
				loadThumbs = imagesChoice.isSelected(0);
				loadAvatars = imagesChoice.isSelected(1);
				threadedImages = imagesChoice.isSelected(2);
				
				avatarsCache = avaCacheChoice.getSelectedIndex();
				avatarsCacheThreshold = avaCacheGauge.getValue() * 5;
				peersCacheThreshold = profileCacheGauge.getValue() * 10;
				
				try {
					RecordStore.deleteRecordStore(SETTINGS_RECORD_NAME);
				} catch (Exception e) {}
				try {
					JSONObject j = new JSONObject();
					j.put("reverseChat", reverseChat);
					j.put("loadAvatars", loadAvatars);
					j.put("avatarSize", avatarSize);
					j.put("showMedia", showMedia);
					j.put("photoSize", photoSize);
					j.put("loadThumbs", loadThumbs);
					j.put("threadedImages", threadedImages);
					j.put("avatarsCache", avatarsCache);
					j.put("avatarsCacheThreshold", avatarsCacheThreshold);
					j.put("useLoadingForm", useLoadingForm);
					j.put("chatsLimit", chatsLimit);
					j.put("messagesLimit", messagesLimit);
					j.put("profilesCacheThreshold", peersCacheThreshold);
					j.put("jsonStream", jsonStream);
					j.put("parseRichtext", parseRichtext);
					j.put("parseLinks", parseLinks);
					j.put("lang", lang);
					j.put("checkUpdates", checkUpdates);
					j.put("chatUpdates", chatUpdates);
					j.put("chatStatus", chatStatus);
					j.put("focusNewMessages", focusNewMessages);
					j.put("updatesDelay", updatesDelay);
					j.put("updatesTimeout", updatesTimeout);
					j.put("chatsListFontSize", chatsListFontSize);
					j.put("keepAlive", keepAlive);
					j.put("chatField", chatField);
					
					byte[] b = j.toString().getBytes("UTF-8");
					RecordStore r = RecordStore.openRecordStore(SETTINGS_RECORD_NAME, true);
					r.addRecord(b, 0, b.length);
					r.closeRecordStore();
				} catch (Exception e) {}
			}
			if (c == logoutCmd) {
				userState = 0;
				display(mainDisplayable = authForm);
				writeAuth();
				return;
			}
			if (c == clearCacheCmd) {
				try {
					String[] s = RecordStore.listRecordStores();
					for (int i = 0; i < s.length; ++i) {
						if (s[i].startsWith(AVATAR_RECORD_PREFIX)) {
							try {
								RecordStore.deleteRecordStore(s[i]);
							} catch (Exception ignored) {}
						}
					}
				} catch (Exception ignored) {}
				usersCache.clear();
				chatsCache.clear();
				imagesCache.clear();
				imagesToLoad.removeAllElements();
				commandAction(backCmd, d);
				return;
			}
		}
		{ // write form commands
			if (c == sendCmd) {
				if (sending) return;
				sending = true;
				String t = messageField.getString();
				if (t.trim().length() == 0 && sendFile == null) {
					return;
				}
				if (MP.updatesThread != null) {
					MP.cancel(MP.updatesThread, true);
				}
				display(loadingAlert(L[Sending]), d);
				start(RUN_SEND_MESSAGE, new Object[] { t, writeTo, replyTo, edit, sendFile, sendChoice });
				return;
			}
			if (c == openTextBoxCmd) {
				TextBox t = new TextBox(L[Message], messageField.getString(), 500, TextField.ANY);
				t.addCommand(okCmd);
				t.addCommand(cancelCmd);
				t.setCommandListener(this);
				display(t);
				return;
			}
			if (c == chooseFileCmd) {
				openFilePicker("");
				return;
			}
			if (c == okCmd) {
				messageField.setString(((TextBox) d).getString());
				
				c = backCmd;
			}
		}
		if (c == aboutCmd) {
			Form f = new Form(L[About]);
			f.addCommand(backCmd);
			f.setCommandListener(this);
			
			try {
				f.append(new ImageItem(null, Image.createImage("/m.png"), Item.LAYOUT_LEFT, null));
			} catch (Exception ignored) {}
			
			StringItem s;
			s = new StringItem(null, "MPGram v".concat(version));
			s.setFont(largePlainFont);
			s.setLayout(Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_VCENTER | Item.LAYOUT_LEFT);
			f.append(s);
			
			s = new StringItem(null, L[AboutText]);
			s.setFont(Font.getDefaultFont());
			s.setLayout(Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			f.append(s);

			s = new StringItem(L[Developer], "shinovon");
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			f.append(s);

			s = new StringItem(L[Author], "twsparkle");
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			s.setItemCommandListener(this);
			f.append(s);

			s = new StringItem("GitHub", "github.com/shinovon");
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			s.setDefaultCommand(richTextLinkCmd);
			s.setItemCommandListener(this);
			f.append(s);

			s = new StringItem("Web", "nnproject.cc");
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			s.setDefaultCommand(richTextLinkCmd);
			s.setItemCommandListener(this);
			f.append(s);

			s = new StringItem(MP.L[Donate], "boosty.to/nnproject/donate");
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			s.setDefaultCommand(richTextLinkCmd);
			s.setItemCommandListener(this);
			f.append(s);

			s = new StringItem(MP.L[Chat], "t.me/nnmidletschat");
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			s.setDefaultCommand(richTextLinkCmd);
			s.setItemCommandListener(this);
			f.append(s);
			display(f);
			return;
		}
		if (c == List.SELECT_COMMAND) {
			if (d instanceof MPList) {
				((MPList) d).select(((List) d).getSelectedIndex());
				return;
			}
			
			// file picker
			int i = ((List) d).getSelectedIndex();
			if (i == -1) return;
			boolean dir = ((List) d).getImage(i) == folderImg;
			String name = ((List) d).getString(i);
			String path = d.getTitle();
			
			if ("/".equals(path)) path = "";
			path = path.concat("/").concat(name);
			
			if (dir) {
				openFilePicker(path.concat("/"));
				return;
			}
			
			// file selected
			commandAction(cancelCmd, d);
			sendFile = "file://".concat(path);
			fileLabel.setText(L[File_Prefix].concat(path));
			return;
		}
		if (c == cancelCmd && d instanceof List) { // go back to write form
			Displayable t = writeForm;
			System.out.println("c: " + t + ", h: " + formHistory);
			synchronized (formHistory) {
				int i = formHistory.size();
				while (i-- != 0) {
					if (formHistory.elementAt(i) == t) {
						break;
					}
					formHistory.removeElementAt(i);
				}
			}
			System.out.println("a: " + formHistory);
			
			display(t, true);
			return;
		}
		if (c == goCmd) { // url dialog submit
			commandAction(backCmd, d);
			
			openUrl(((TextBox) d).getString());
			return;
		}
		if (c == refreshCmd) {
			if (d instanceof MPForm) {
				((MPForm) d).cancel();
				start(RUN_LOAD_FORM, d);
				return;
			}
			if (d instanceof MPList) {
				((MPList) d).cancel();
				start(RUN_LOAD_LIST, d);
				return;
			}
			return;
		}
		if (c == updateCmd) {
			browse(updateUrl);
			return;
		}
		if (c == backCmd || c == cancelCmd) {
			// cancel ota update dialog
			updateUrl = null;
			
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
		if (c == itemChatCmd) {
			String[] s = (String[]) ((MPForm) current).urls.get(item);
			if (s == null) return;
			openChat(s[2], -1);
			return;
		}
		if (c == itemChatInfoCmd) {
			String[] s = (String[]) ((MPForm) current).urls.get(item);
			if (s == null) return;
			openProfile(s[2], null, 0);
			return;
		}
		if (c == replyMsgCmd) {
			String[] s = (String[]) ((MPForm) current).urls.get(item);
			if (s == null) return;
			display(writeForm(((ChatForm) current).id, s[1], "", null));
			return;
		}
		if (c == forwardMsgCmd) {
			// TODO
			return;
		}
		if (c == copyMsgCmd) {
			// copy message text
			String[] s = (String[]) ((MPForm) current).urls.get(item);
			if (s == null) return;
			copy("", (String) ((Object[]) ((MPForm) current).urls.get(s[1]))[2]);
			return;
		}
		if (c == messageLinkCmd) {
			// copy message link
			String[] s = (String[]) ((MPForm) current).urls.get(item);
			if (s == null) return;
			StringBuffer sb = new StringBuffer("https://t.me/"); 
			String username = ((ChatForm) current).username;
			if (s[0].charAt(0) == '-' && username == null) {
				sb.append("c/");
			}
			sb.append(username != null ? username : s[0]).append('/').append(s[1]);
			copy("", sb.toString());
			return;
		}
		if (c == richTextLinkCmd) {
			String url = null;
			try {
				url = (String) ((MPForm) current).urls.get(item);
			} catch (Exception ignored) {}
			if (url == null) url = ((StringItem) item).getText();
			
			openUrl(url);
			return;
		}
		if (c == openImageCmd) {
			// TODO
			String[] s = (String[]) ((MPForm) current).urls.get(item);
			if (s == null) return;
			browse(instanceUrl + FILE_URL + "?c=" + s[0] + "&m=" + s[1] + "&user=" + user);
			return;
		}
		if (c == callItemCmd) {
			browse("tel:".concat(((StringItem) item).getText()));
			return;
		}
		if (c == deleteMsgCmd) {
			String[] s = (String[]) ((MPForm) current).urls.get(item);
			if (s == null) return;
			display(loadingAlert(L[Loading]), current);
			start(RUN_DELETE_MESSAGE, s);
			return;
		}
		if (c == documentCmd) {
			// TODO
			String[] s = (String[]) ((MPForm) current).urls.get(item);
			if (s == null) return;
			browse(instanceUrl + FILE_URL + "?c=" + s[0] + "&m=" + s[1] + "&user=" + user);
			return;
		}
		if (c == editMsgCmd) {
			String[] s = (String[]) ((MPForm) current).urls.get(item);
			if (s == null) return;
			display(writeForm(s[0], null, (String) ((MPForm) current).urls.get(s[1]), s[1]));
			return;
		}
		if (c == gotoMsgCmd) {
			String[] s = (String[]) ((MPForm) current).urls.get(item);
			if (s == null) return;
			if (s[0] == null || s[0].equals(((ChatForm) current).id)) {
				((ChatForm) current).openMessage(s[1], -1);
				return;
			}
			
			openLoad(new ChatForm(s[0], null, Integer.parseInt(s[1]), 0));
			return;
		}
		if (c == sendCmd) { 
			if (sending || !(current instanceof ChatForm))
				return;
			sending = true;
			
			String t = ((TextField) item).getString();
			if (t.trim().length() == 0)
				return;
			if (MP.updatesThread != null) {
				MP.cancel(MP.updatesThread, true);
			}
			display(loadingAlert(L[Sending]), current);
			start(RUN_SEND_MESSAGE, new Object[] { t, ((ChatForm) current).id, null, null, null, null });
			return;
		}
		if (c == botCallbackCmd) {
			String[] p = (String[]) ((MPForm) current).urls.get(item);
			if (sending || p == null) return;
			sending = true;
			display(loadingAlert(L[Sending]), current);
			start(RUN_BOT_CALLBACK, p);
			return;
		}
		if (c == banMemberCmd) {
			String[] s = (String[]) ((MPForm) current).urls.get(item);
			if (s == null) return;

			display(loadingAlert(L[Loading]), current);
			start(RUN_BAN_MEMBER, s);
			return;
		}
		commandAction(c, display.getCurrent());
	}

	private void loadLocale(String lang) throws IOException {
		InputStreamReader r = new InputStreamReader(getClass().getResourceAsStream("/l/".concat(lang)), "UTF-8");
		StringBuffer s = new StringBuffer();
		int c;
		int i = 1;
		while ((c = r.read()) > 0) {
			if (c == '\r') continue;
			if (c == '\\') {
				s.append((c = r.read()) == 'n' ? '\n' : (char) c);
				continue;
			}
			if (c == '\n') {
				L[i++] = s.toString();
				s.setLength(0);
				continue;
			}
			s.append((char) c);
		}
		r.close();
	}

	private static void writeAuth() {
		try {
			RecordStore.deleteRecordStore(AUTH_RECORD_NAME);
		} catch (Exception ignored) {}
		try {
			JSONObject j = new JSONObject();
			
			j.put("user", user);
			j.put("state", userState);
			j.put("phone", phone);
			j.put("url", instanceUrl);
			j.put("instPass", instancePassword);
			
			byte[] b = j.toString().getBytes("UTF-8");
			RecordStore r = RecordStore.openRecordStore(AUTH_RECORD_NAME, true);
			r.addRecord(b, 0, b.length);
			r.closeRecordStore();
		} catch (Exception e) {}
	}
	
	static void queueAvatar(String id, Object target) {
		if (target == null || id == null || !loadAvatars) return;
		
		JSONObject peer = getPeer(id, false);
		if (peer != null) {
			id = peer.getString("id");
			// put placeholder avatar if peer doesn't have it
			if (!peer.has("p")) {
				putImage(target, id.charAt(0) == '-' ? chatDefaultImg : userDefaultImg);
				return;
			}
		}
		
		synchronized (imagesLoadLock) {
			imagesToLoad.addElement(new Object[] { id, target });
			imagesLoadLock.notifyAll();
		}
	}

	private static void putImage(Object target, Image img) {
		if (target instanceof ImageItem) {
			((ImageItem) target).setImage(img);
			return;
		}
		if (target instanceof Object[]) {
			if (((Object[]) target)[0] instanceof List) {
				List list = ((List) ((Object[]) target)[0]);
				int idx = (((Integer) ((Object[]) target)[1])).intValue();
				list.set(idx, list.getString(idx), img);
			}
		}
	}
	
	static void queueImage(Object src, Object target) {
		if (target == null || src == null || !loadThumbs) return;
		synchronized (imagesLoadLock) {
			imagesToLoad.addElement(new Object[] { src, target });
			imagesLoadLock.notifyAll();
		}
	}

	static void fillPeersCache(JSONObject r) {
		JSONObject users = r.getObject("users", null);
		if (users != null && usersCache != null) {
			if (usersCache.size() > peersCacheThreshold) {
				usersCache.clear();
			}
			for (Enumeration e = users.keys(); e.hasMoreElements(); ) {
				String k = (String) e.nextElement();
				if ("0".equals(k)) continue;
				JSONObject user = (JSONObject) users.get(k);
				if (user.has("name")) usersCache.put(user.getString("name"), k);
				usersCache.put(k, user);
			}
		}
		JSONObject chats = r.getObject("chats", null);
		if (chats != null && chatsCache != null) {
			if (chatsCache.size() > peersCacheThreshold) {
				chatsCache.clear();
			}
			for (Enumeration e = chats.keys(); e.hasMoreElements(); ) {
				String k = (String) e.nextElement();
				if ("0".equals(k)) continue;
				JSONObject chat = (JSONObject) chats.get(k);
				if (chat.has("name")) usersCache.put(chat.getString("name"), k);
				chatsCache.put(k, chat);
			}
		}
	}
	
	static StringBuffer appendOneLine(StringBuffer sb, String s) {
		if (s == null) return sb;
		int i = 0, l = s.length();
		while (i < l && i < ONE_LINE_LENGTH) {
			char c = s.charAt(i++);
			if (c == '\r') continue;
			if (c != '\n') sb.append(c);
			else sb.append(' ');
		}
		if (i == ONE_LINE_LENGTH) sb.append("..");
		return sb;
	}
	
	static JSONObject getPeer(String id, boolean loadIfNeeded) {
		if (id == null) return null;

		Object o = id;
		
		try {
			while (o instanceof String) {
				if (id.charAt(0) == '-') {
					o = chatsCache.get((String) o, null);
				} else {
					o = usersCache.get((String) o, null);
				}
			}
			
			if (o == null && loadIfNeeded) {
				Long.parseLong(id);
				try {
					fillPeersCache((JSONObject) api("getPeers&id=".concat(id)));
				} catch (Exception ignored) {}
				
				if (id.charAt(0) == '-') {
					o = chatsCache.getObject(id, null);
				} else {
					o = usersCache.getObject(id, null);
				}
			}
		} catch (Exception e) {
			// username not in cache
			try {
				o = (JSONObject) api("getPeer&id=".concat(id));
			} catch (Exception e2) {
				o = null;
			}
		}
		
		return (JSONObject) o;
	}

	// variant: false - full; true - short
	static String getName(String id, boolean variant, boolean loadIfNeeded) {
		if (id == null) return null;
		String res;
		JSONObject o;
		if (id.charAt(0) == '-') {
			o = chatsCache.getObject(id, null);
			if (o == null) {
				o = getPeer(id, loadIfNeeded);
			}
			if (o == null) return null;
			res = o.getString("t");
		} else {
			o = usersCache.getObject(id, null);
			if (o == null) {
				o = getPeer(id, loadIfNeeded);
			}
			if (o == null) return null;
			res = variant ? getShortName(o) : getName(o);
		}
		return res;
	}
	
	static String getName(String id, boolean variant) {
		return getName(id, variant, true);
	}
	
	static String getName(JSONObject p) {
		if (p == null) return null;
		if (p.has("t")) {
			return p.getString("t");
		}
		
		String fn = p.getString("fn");
		String ln = p.getString("ln");
		
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
	
	static String getNameRaw(JSONObject p) {
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
		if (p.has("t")) {
			return p.getString("t");
		}
		
		String fn = p.getString("fn");
		String ln = p.getString("ln");
		
		if (fn != null) {
			return fn;
		}
		
		if (ln != null) {
			return ln;
		}
		
		return "Deleted";
	}
	
	static ChatsList mainChatsList() {
		ChatsList l = chatsList = new ChatsList(L[mpgram], 0);
		l.removeCommand(backCmd);
		l.addCommand(exitCmd);
		l.addCommand(aboutCmd);
		l.addCommand(settingsCmd);
		l.addCommand(contactsCmd);
		l.addCommand(searchChatsCmd);
		l.addCommand(openLinkCmd);
		return l;
	}
	
	static Form writeForm(String id, String reply, String text, String editId) {
		writeTo = id;
		replyTo = reply;
		edit = editId;
		sendFile = null;
		
		Form f = new Form(editId != null ? editId : reply != null ? L[Reply_Title] : L[Write_Title]);
		f.setCommandListener(midlet);
		f.setItemStateListener(midlet);
		f.addCommand(backCmd);
		f.addCommand(sendCmd);
		
		TextField t = new TextField(L[Message], text, 500, TextField.ANY);
		t.addCommand(sendCmd);
		t.setItemCommandListener(midlet);
		f.append(messageField = t);
		
		StringItem s = new StringItem(null, "...", Item.BUTTON);
		s.setDefaultCommand(openTextBoxCmd);
		s.setItemCommandListener(midlet);
		f.append(s);
		
		// file
		
		s = new StringItem(null, L[File_Prefix].concat(L[NotSelected]));
		s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
		f.append(fileLabel = s);
		
		s = new StringItem(null, L[ChooseFile], Item.BUTTON);
		s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
		s.setDefaultCommand(chooseFileCmd);
		s.setItemCommandListener(midlet);
		f.append(s);
		
//		t = new TextField("File", "file:///", 300, TextField.ANY);
//		f.append(fileField = t);
		
		f.append(sendChoice = new ChoiceGroup("", Choice.MULTIPLE, new String[] {
				"Send uncompressed",
				"Hide with spoiler"
		}, null));
		
		return writeForm = f;
	}
	
	static void openUrl(String url) {
		if (!handleDeepLink(url)) {
			midlet.browse(url);
		}
	}
	
	static boolean handleDeepLink(String url) {
		if (url.startsWith("@")) {
			openChat(url.substring(1), -1);
			return true;
		}
		int i;
		String[] query = null;
		boolean tg = false;
		boolean profile = false;
		String domain = null;
		boolean phone = false;
//		boolean privat = false;
		String messageId = null;
		String thread = null;
		String invite = null;
		String start = null;
//		String text = null;
		String stickers = null;
		
		try {
			if ((i = url.indexOf("t.me")) == 0 || i == 8) {
				url = url.substring(i + 5);
				if ((i = url.indexOf('#')) != -1) {
					url = url.substring(0, i);
				}
				String[] s = split(url, '/');
				
				tg = true;
				
				if ((i = s[s.length - 1].indexOf('?')) != -1) {
					query = split(s[s.length - 1].substring(i + 1), '&');
					s[s.length - 1] = s[s.length - 1].substring(0, i);
				}
				if ("c".equals(s[0]) && s.length > 1) {
//					privat = true;
					domain = s[1];
					if (s.length == 3) {
						messageId = s[2];
					} else if (s.length == 4) {
						thread = s[2];
						messageId = s[3];
					}
				} else if (s.length == 1) {
					domain = s[0];
					if (domain.startsWith("+")) {
						domain = domain.substring(1);
						try {
							Long.parseLong(domain);
							phone = true;
						} catch (Exception e) {
							invite = domain;
							domain = null;
						}
					}
				} else if ("addstickers".equals(s[0])) {
					stickers = s[1];
				} else if ("addemoji".equals(s[0])) {
//					slug = s[1];
				} else if ("joinchat".equals(s[0])) {
					invite = s[1];
				} else if ("addlist".equals(s[0])) {
//					slug = s[1];
				} else if ("proxy".equals(s[0])) {
				} else if ("socks".equals(s[0])) {
				} else if ("addtheme".equals(s[0])) {
				} else if ("bg".equals(s[0])) {
				} else if ("contact".equals(s[0])) {
				} else if ("share".equals(s[0])) {
				} else if ("m".equals(s[0])) {
				} else if ("setlanguage".equals(s[0])) {
				} else if ("invoice".equals(s[0])) {
				} else if ("login".equals(s[0])) {
				} else if ("confirmphone".equals(s[0])) {
				} else if ("giftcode".equals(s[0])) {
				} else if ("boost".equals(s[0])) {
				} else {
					domain = s[0];
					if (s.length == 2) {
						messageId = s[1];
					} else if (s.length == 3) {
						thread = s[1];
						messageId = s[2];
					}
				}
			} else if (url.startsWith("tg://")) {
				url = url.substring(5);
				if ((i = url.indexOf('#')) != -1) {
					url = url.substring(0, i);
				}
				
				if ((i = url.indexOf('?')) != -1) {
					query = split(url.substring(i + 1), '&');
					url = url.substring(0, i);
				}
				if (url.startsWith("settings")) {
					return true;
				} else if ("resolve".equals(url)
						|| "privatepost".equals(url)
						|| "user".equals(url)
						|| "join".equals(url)
						|| "addstickers".equals(url)) {
					tg = true;
//					privat = "privatepost".equals(url);
//				} else if ("addlist".equals(url)) {
//				} else if ("addemoji".equals(url)) {
				}
			}
			
			if (tg) {
				if (query != null) {
					for (int n = 0; n < query.length; ++n) {
						if ("profile".equals(query[n])) {
							profile = true;
							continue;
						}
						if (query[n].startsWith("thread=")) {
							thread = query[n].substring(7);
							continue;
						}
						if (query[n].startsWith("domain=")) {
							domain = query[n].substring(7);
							continue;
						}
						if (query[n].startsWith("phone=")) {
							domain = query[n].substring(6);
							phone = true;
							continue;
						}
						if (query[n].startsWith("start=")) {
							start = query[n].substring(6);
							continue;
						}
						if (query[n].startsWith("id=")) {
							domain = query[n].substring(3);
							continue;
						}
						if (query[n].startsWith("text=")) {
//							text = query[n].substring(5);
							continue;
						}
						if (query[n].startsWith("invite=")) {
							invite = query[n].substring(7);
							continue;
						}
						if (query[n].startsWith("slug=")) {
							stickers = query[n].substring(5);
							continue;
						}
						if (query[n].startsWith("post=")) {
							messageId = query[n].substring(5);
							continue;
						}
					}
				}
				
				if (domain != null) {
					if (phone) {
						//  resolve number
						openProfile(domain, null, 2);
						
						return true;
					} else {
						if (profile) {
							openProfile(domain, null, 0);
							
							return true;
						}
						int msg = 0;
						int topMsg = 0;
						if (messageId != null) {
							try {
								msg = Integer.parseInt(messageId);
							} catch (Exception e) {
								messageId = null;
							}
						}
						if (thread != null) {
							try {
								topMsg = Integer.parseInt(thread);
							} catch (Exception ignored) {}
						}
						if (current instanceof ChatForm &&
								(domain.equals(((ChatForm) current).id)
								|| domain.equals(((ChatForm) current).username))) {
							((ChatForm) current).openMessage(messageId, topMsg);
						} else {
							ChatForm f = new ChatForm(domain, null, msg, topMsg);
							if (start != null) {
								f.startBot = start;
							}
							openLoad(f);
						}
						return true;
					}
				} else if (invite != null) {
					// resolve invite
					midlet.start(RUN_RESOLVE_INVITE, invite);
					
					return true;
				} else if (stickers != null) {
					// add stickers TODO
					
					return true;
				}
				
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		System.out.println("Unhandled deep link: " + url);
		if (url.startsWith("tg://")) {
			return true;
		}
		return false;
	}
	
	static void openFilePicker(String path) {
		if (path.length() == 0) path = "/";
		display(loadingAlert(L[Loading]), current);
		try {
			if (fileImg == null) {
				fileImg = Image.createImage("/file.png");
				folderImg = Image.createImage("/folder.png");
			}
			
			List list = new List(path, List.IMPLICIT);
			list.addCommand(backCmd);
			list.addCommand(cancelCmd);
			list.addCommand(List.SELECT_COMMAND);
			list.setSelectCommand(List.SELECT_COMMAND);
			list.setCommandListener(midlet);
			
			if ("/".equals(path)) {
				// roots
				if (rootsList == null) {
					rootsList = new Vector();
					Enumeration roots = FileSystemRegistry.listRoots();
					while (roots.hasMoreElements()) {
						String s = (String) roots.nextElement();
						if (s.startsWith("file:///")) s = s.substring("file:///".length());
						rootsList.addElement(s);
					}
				}
				
				int l = rootsList.size();
				for (int i = 0; i < l; i++) {
					String s = (String) rootsList.elementAt(i);
					if (s.startsWith("file:///")) s = s.substring("file:///".length());
					if (s.endsWith("/")) s = s.substring(0, s.length() - 1);
					list.append(s, folderImg);
				}
			} else {
				FileConnection fc = (FileConnection) Connector.open("file://".concat(path));
				try {
					Enumeration en = fc.list();
					while (en.hasMoreElements()) {
						String s = (String) en.nextElement();
						if (s.endsWith("/")) {
							list.append(s.substring(0, s.length() - 1), folderImg);
						} else {
							list.append(s, fileImg);
						}
					}
				} finally {
					fc.close();
				}
			}
			display(list);
		} catch (Exception e) {
			display(errorAlert(e.toString()), current);
			e.printStackTrace();
		}
	}
	
	static void openChat(String id, int msg) {
		openLoad(new ChatForm(id, null, msg, 0));
	}
	
	static void openProfile(String id, ChatForm chatForm, int mode) {
		if (chatForm == null && current instanceof ChatForm && id.equals(((ChatForm) current).id)) {
			chatForm = (ChatForm) current;
		}
		openLoad(new ChatInfoForm(id, chatForm, mode));
	}
	
	static void copy(String title, String text) {
		// TODO use nokiaui?
		TextBox t = new TextBox(title, text, text.length() + 1, TextField.UNEDITABLE);
		t.addCommand(backCmd);
		t.setCommandListener(midlet);
		display(t);
	}
	
	static void openLoad(Displayable d) {
		display(d);
		midlet.start(d instanceof MPList ? RUN_LOAD_LIST : RUN_LOAD_FORM, d);
	}
	
	static void display(Alert a, Displayable d) {
		if (d == null) {
			if (display.getCurrent() instanceof Alert && current != null) {
				display.setCurrent(a, current);
				return;
			}
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
		if (d instanceof Alert) {
			display.setCurrent((Alert) d, mainDisplayable);
			return;
		}
		if (d == loadingForm) {
			display.setCurrent(d);
			return;
		}
		if (updateUrl != null) {
			return;
		}
		if (d == null || d == mainDisplayable) {
			d = mainDisplayable;
			
			formHistory.removeAllElements();
			if (back) imagesToLoad.removeAllElements();
		}
		Displayable p = display.getCurrent();
		if (p == loadingForm) p = current;
		display.setCurrent(current = d);
		if (p == null || p == d) return;
		
		if (p instanceof MPForm) {
			((MPForm) p).closed(back);
		} else if (p instanceof MPList) {
			((MPList) p).closed(back);
		}
		if (back) {
			if (p instanceof MPForm || p instanceof MPList) {
				imagesToLoad.removeAllElements();
			}
		}
		
		if (d instanceof MPForm) {
			((MPForm) d).shown();
		} else if (d instanceof MPList) {
			((MPList) d).shown();
		}
		if (back) return;
		// push to history
		if (d != mainDisplayable && (formHistory.isEmpty() || formHistory.lastElement() != d)) {
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

	static Alert infoAlert(String text) {
		Alert a = new Alert("");
		a.setType(AlertType.CONFIRMATION);
		a.setString(text);
		a.setTimeout(1500);
		return a;
	}

	static Alert loadingAlert(String s) {
		Alert a = new Alert("", s, null, null);
		a.setCommandListener(midlet);
		a.addCommand(Alert.DISMISS_COMMAND);
		a.setIndicator(new Gauge(null, false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING));
		a.setTimeout(Alert.FOREVER);
		return a;
	}

	void browse(String url) {
		try {
			if (url.indexOf(':') == -1) {
				url = "http://".concat(url);
			}
			if (platformRequest(url)) notifyDestroyed();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	static Object api(String url) throws IOException {
		Object res;

		HttpConnection hc = null;
		InputStream in = null;
		Thread thread = Thread.currentThread();
		try {
			String t = instanceUrl.concat(API_URL + "?v=" + API_VERSION + "&method=").concat(url);
			try {
				while (closingConnections.size() != 0) {
					System.out.println("wait " + closingConnections);
					Thread.sleep(1000);
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e.toString());
			}
			threadConnections.put(thread, hc = openHttpConnection(t));
			hc.setRequestMethod("GET");
			int c = hc.getResponseCode();
			if ((c == 502 || c == 504) && !url.startsWith("updates")) {
				try {
					hc.close();
				} catch (Exception ignored) {}
				// repeat
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					throw new RuntimeException(e.toString());
				}
				
				hc = openHttpConnection(t);
				hc.setRequestMethod("GET");
				
				c = hc.getResponseCode();
			}
			try {
				if (jsonStream) {
					res = JSONStream.getStream(in = hc.openInputStream()).nextValue();
				} else {
					res = JSONObject.parseJSON(readUtf(in = hc.openInputStream(), (int) hc.getLength()));
				}
			} catch (RuntimeException e) {
				if (c >= 400) {
					throw new APIException(url, c, null);
				} else throw e;
			}
			if (c >= 400 || (res instanceof JSONObject && ((JSONObject) res).has("error"))) {
				throw new APIException(url, c, res);
			}
		} finally {
			closingConnections.removeElement(hc);
			threadConnections.remove(thread);
			if (in != null) try {
				in.close();
			} catch (IOException e) {}
			if (hc != null) try {
				hc.close();
			} catch (IOException e) {}
		}
//		System.out.println(res instanceof JSONObject ?
//				((JSONObject) res).format(0) : res instanceof JSONArray ?
//						((JSONArray) res).format(0) : res);
		return res;
	}

	static JSONStream apiStream(String url) throws IOException {
		JSONStream res = null;

		HttpConnection hc = null;
		InputStream in = null;
		try {
			hc = openHttpConnection(instanceUrl.concat(API_URL + "?v=" + API_VERSION + "&method=").concat(url));
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
	
	static Object postMessage(String url, String fileUrl, String text) throws IOException {
		Object res;

		HttpConnection http = null;
		InputStream httpIn = null;
		OutputStream httpOut = null;
		
		FileConnection file = null;
		InputStream fileIn = null;
		
		StringBuffer sb = new StringBuffer();
		Random rng = new Random();
		for (int i = 0; i < 27; i++) {
			sb.append('-');
		}
		for (int i = 0; i < 11; i++) {
			sb.append(rng.nextInt(10));
		}
		String boundary = sb.toString();
		int boundaryLength = boundary.length();
		sb.setLength(0);
		byte[] CRLF = new byte[] { (byte) '\r', (byte) '\n' };
		byte[] DASHDASH = new byte[] { (byte) '-', (byte) '-' };
		
		try {
			http = openHttpConnection(instanceUrl.concat(API_URL + "?v=" + API_VERSION + "&method=").concat(url));
			http.setRequestMethod("POST");
			http.setRequestProperty("Content-Type", "multipart/form-data; charset=UTF-8; boundary=".concat(boundary));

			int contentLength = 0;
			if (text != null) {
				contentLength += 43 + 10 + boundaryLength + text.getBytes("UTF-8").length;
			}
			if (fileUrl != null) {
				file = (FileConnection) Connector.open(fileUrl);
				contentLength += 55 + 1 + 10 + boundaryLength + file.getName().getBytes("UTF-8").length + (int) file.fileSize();
			}
			contentLength += boundaryLength + 4;
			http.setRequestProperty("Content-Length", String.valueOf(contentLength));
			
			httpOut = http.openOutputStream();
			try {
				if (text != null) {
					httpOut.write(DASHDASH);
					httpOut.write(boundary.getBytes());
					httpOut.write(CRLF);
					httpOut.write("Content-Disposition: form-data; name=\"text\"".getBytes());
					httpOut.write(CRLF);
					httpOut.write(CRLF);
					byte[] b = text.getBytes("UTF-8");
					httpOut.write(b);
					httpOut.write(CRLF);
				}

				if (fileUrl != null) {
					fileIn = file.openInputStream();
					try {
						httpOut.write(DASHDASH);
						httpOut.write(boundary.getBytes());
						httpOut.write(CRLF);
						httpOut.write("Content-Disposition: form-data; name=\"file\"; filename=\"".getBytes());
						httpOut.write(file.getName().getBytes("UTF-8"));
						httpOut.write((byte) '"');
						httpOut.write(CRLF);
						httpOut.write(CRLF);
						byte[] b = new byte[4096];
						int i;
						while ((i = fileIn.read(b)) != -1) {
							httpOut.write(b, 0, i);
						}
						httpOut.write(CRLF);
					} finally {
						fileIn.close();
					}
				}

				httpOut.write(DASHDASH);
				httpOut.write(boundary.getBytes());
				httpOut.write(DASHDASH);
				httpOut.flush();
			} finally {
				httpOut.close();
			}
			
			int c = http.getResponseCode();
			try {
				if (jsonStream) {
					res = JSONStream.getStream(httpIn = http.openInputStream()).nextValue();
				} else {
					res = JSONObject.parseJSON(readUtf(httpIn = http.openInputStream(), (int) http.getLength()));
				}
			} catch (RuntimeException e) {
				if (c >= 400) {
					throw new APIException(url, c, null);
				} else throw e;
			}
			if (c >= 400 || (res instanceof JSONObject && ((JSONObject) res).has("error"))) {
				throw new APIException(url, c, res);
			}
			return res;
		} finally {
			if (file != null) try {
				file.close();
			} catch (IOException e) {}
			if (httpIn != null) try {
				httpIn.close();
			} catch (IOException e) {}
			if (http != null) try {
				http.close();
			} catch (IOException e) {}
		}
	}

	static Image getImage(String url) throws IOException {
		byte[] b = get(url);
		return Image.createImage(b, 0, b.length);
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
		System.out.println(url);
		HttpConnection hc = (HttpConnection) Connector.open(url, Connector.READ_WRITE,
				url.indexOf("method=updates") == -1 || OTA_URL.equals(url));
		hc.setRequestProperty("User-Agent", "mpgram4/".concat(version).concat(" (https://github.com/shinovon/mpgram-client)"));
		if (url.startsWith(instanceUrl)) {
			if (user != null) {
				hc.setRequestProperty("X-mpgram-user", user);
			}
			if (deviceName != null) {
				hc.setRequestProperty("X-mpgram-device", deviceName);
			}
			hc.setRequestProperty("X-mpgram-system", systemName != null ? systemName : "J2ME");
			if (utf) hc.setRequestProperty("X-mpgram-unicode", "1");
			hc.setRequestProperty("X-mpgram-app-version", version);
			if (instancePassword != null) {
				hc.setRequestProperty("X-mpgram-instance-password", instancePassword);
			}
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
	
	static StringBuffer appendTime(StringBuffer sb, long date) {
		date = (date + tzOffset) / 60;
		return sb.append(n(((int) date / 60) % 24))
				.append(':')
				.append(n((int) date % 60));
	}

	static String localizePlural(int n, int i) {
		String s = Integer.toString(n);
		if (L[LocaleSlavicPlurals].length() == 0)
			return s.concat(L[n == 1 ? i : i + 1]);
		
		int a = n % 10;
		int b = n % 100;
		if ("pl".equals(lang) ? n == 1 : (a == 1 && b != 11))
			return s.concat(L[i]);
		if ((a >= 2 && a <= 4) && !(b >= 12 && b <= 14))
			return s.concat(L[i + 1]);
		return s.concat(L[i + 2]);
	}
	
	static StringBuffer appendLocalizedPlural(StringBuffer sb, int n, int i) {
		sb.append(n);
		if (L[LocaleSlavicPlurals].length() == 0)
			return sb.append(L[n == 1 ? i : i + 1]);
		
		int a = n % 10;
		int b = n % 100;
		if ("pl".equals(lang) ? n == 1 : (a == 1 && b != 11))
			return sb.append(L[i]);
		if ((a >= 2 && a <= 4) && !(b >= 12 && b <= 14))
			return sb.append(L[i + 1]);
		return sb.append(L[i + 2]);
	}

	// mode: 0 - date, 1 - detailed date, 2 - short date, 3 - last seen detailed, 4 - last seen
	static String localizeDate(long date, int mode) {
		long now = System.currentTimeMillis() / 1000L;
		long d = now - date;
		boolean ru = "ru".equals(lang);

		StringBuffer sb = new StringBuffer();
		Calendar c = Calendar.getInstance();
		int currentYear = c.get(Calendar.YEAR);
		c.setTime(new Date(date * 1000L));
		
		if (mode == 4) {
			if (d < 60) {
				return L[JustNow];
			}
			
			if (d < 60 * 60) {
				d /= 60L;
				return appendLocalizedPlural(sb, (int) d, _minuteAgo).toString();
			}
			
			if (d < 12 * 60 * 60) {
				d /= 60 * 60L;
				return appendLocalizedPlural(sb, (int) d, _hourAgo).toString();
			}
		}
		
		if (mode == 2) {
			if (d < 24 * 60 * 60) {
				sb.append(n(c.get(Calendar.HOUR_OF_DAY)))
				.append(':')
				.append(n(c.get(Calendar.MINUTE)));
			} else if (d < 6 * 24 * 60 * 60) {
				sb.append(L[Sun + c.get(Calendar.DAY_OF_WEEK) - 1]);
			} else {
				sb.append(n(c.get(Calendar.DAY_OF_MONTH)))
				.append('.')
				.append(n(c.get(Calendar.MONTH) + 1))
				.append('.')
				.append(n(c.get(Calendar.YEAR)));
			}
		} else {
			int dayNow = (int) (now / (24 * 60 * 60L));
			int day = (int) (date / (24 * 60 * 60L));
			if (mode == 3 || mode == 4) {
				boolean b = true;
				if (day == dayNow) {
					sb.append(L[Today]);
				} else if (day == dayNow - 1) {
					sb.append(L[Yesterday]);
				} else {
					b = false;
					sb.append(n(c.get(Calendar.DAY_OF_MONTH)))
					.append('.')
					.append(n(c.get(Calendar.MONTH) + 1))
					.append('.')
					.append(n(c.get(Calendar.YEAR)));
				}
				if (b || mode == 3) {
					sb.append(L[at_Time])
					.append(n(c.get(Calendar.HOUR_OF_DAY)))
					.append(':')
					.append(n(c.get(Calendar.MINUTE)));
				}
			} else {
				if (!ru) sb.append(L[Jan + c.get(Calendar.MONTH)]).append(' ');
				sb.append(c.get(Calendar.DAY_OF_MONTH));
				if (ru) sb.append(' ').append(L[Jan + c.get(Calendar.MONTH)]);
				
				int year = c.get(Calendar.YEAR);
				if (year != currentYear || mode == 1) {
					sb.append(' ').append(year);
				}
				
				if (mode == 1) {
					sb.append(' ')
					.append(n(c.get(Calendar.HOUR_OF_DAY)))
					.append(':')
					.append(n(c.get(Calendar.MINUTE)));
				}
			}
		}
		
		return sb.toString();
	}
	
	static String n(int n) {
		if (n < 10) {
			return "0".concat(Integer.toString(n));
		} else return Integer.toString(n);
	}

	static String[] split(String str, char d) {
		int i = str.indexOf(d);
		if (i == -1)
			return new String[] {str};
		Vector v = new Vector();
		v.addElement(str.substring(0, i));
		while (i != -1) {
			str = str.substring(i + 1);
			if ((i = str.indexOf(d)) != -1)
				v.addElement(str.substring(0, i));
			i = str.indexOf(d);
		}
		v.addElement(str);
		String[] r = new String[v.size()];
		v.copyInto(r);
		return r;
	}
	
	// Rich text
	
	private static final int
			RT_BOLD = 0,
			RT_ITALIC = 1,
			RT_PRE = 2,
			RT_UNDERLINE = 3,
			RT_STRIKE = 4,
			RT_SPOILER = 5,
			RT_URL = 6;

	static int wrapRichText(MPForm form, Thread thread, String text, JSONArray entities, int insert) {
		return wrapRichText(form, thread, text, entities, insert, new int[8]);
	}
	
	private static int wrapRichNestedText(MPForm form, Thread thread, String text, JSONObject entity, JSONArray allEntities, int insert, int[] state) {
		int off = entity.getInt("offset");
		int len = entity.getInt("length");
		JSONArray entities = new JSONArray();
		
		int l = allEntities.size();
		for (int i = 0; i < l; ++i) {
			JSONObject e = allEntities.getObject(i);
			if (e == entity) continue;
			if (e.getInt("offset") >= off && e.getInt("offset")+e.getInt("length") <= off+len) {
				JSONObject ne = new JSONObject();
				for (Enumeration en = e.keys(); en.hasMoreElements(); ) {
					String k = (String) en.nextElement();
					ne.put(k, e.get(k));
				}
				
				ne.put("offset", ne.getInt("offset") - off);
				entities.add(ne);
			}
		}
		
		if (entities.size() > 0) {
			return wrapRichText(form, thread, text, entities, insert, state);
		}
		return flush(form, thread, text, insert, state);
	}

	private static int wrapRichText(MPForm form, Thread thread, String text, JSONArray entities, int insert, int[] state) {
		int len = entities.size();
		int lastOffset = 0;
		for (int i = 0; i < len; ++i) {
			JSONObject entity = entities.getObject(i);
			if (entity.getInt("offset") > lastOffset) {
				insert = flush(form, thread, text.substring(lastOffset, entity.getInt("offset")), insert, state);
			} else if (entity.getInt("offset") < lastOffset) {
				continue;
			}
			boolean skipEntity = false;
			String entityText = text.substring(entity.getInt("offset"), entity.getInt("offset") + entity.getInt("length"));
			String type = entity.getString("_");
			if ("messageEntityUrl".equals(type)) {
				state[RT_URL] ++;
				insert = flush(form, thread, richTextUrl = entityText, insert, state);
				state[RT_URL] --;
			} else if ("messageEntityTextUrl".equals(type)) {
				state[RT_URL] ++;
				richTextUrl = entity.getString("url");
				insert = wrapRichNestedText(form, thread, entityText, entity, entities, insert, state);
				state[RT_URL] --;
			} else if ("messageEntityBold".equals(type)) {
				state[RT_BOLD] ++;
				insert = wrapRichNestedText(form, thread, entityText, entity, entities, insert, state);
				state[RT_BOLD] --;
			} else if ("messageEntityItalic".equals(type)) {
				state[RT_ITALIC] ++;
				insert = wrapRichNestedText(form, thread, entityText, entity, entities, insert, state);
				state[RT_ITALIC] --;
			} else if ("messageEntityCode".equals(type) || "messageEntityPre".equals(type)) {
				state[RT_PRE] ++;
				insert = wrapRichNestedText(form, thread, entityText, entity, entities, insert, state);
				state[RT_PRE] --;
			} else if ("messageEntityUnderline".equals(type)) {
				state[RT_UNDERLINE] ++;
				insert = wrapRichNestedText(form, thread, entityText, entity, entities, insert, state);
				state[RT_UNDERLINE] --;
			} else if ("messageEntityStrike".equals(type)) {
				state[RT_STRIKE] ++;
				insert = wrapRichNestedText(form, thread, entityText, entity, entities, insert, state);
				state[RT_STRIKE] --;
			} else if ("messageEntitySpoiler".equals(type)) {
				state[RT_SPOILER] ++;
				insert = wrapRichNestedText(form, thread, entityText, entity, entities, insert, state);
				state[RT_SPOILER] --;
			} else {
				skipEntity = true;
			}
			lastOffset = entity.getInt("offset") + (skipEntity ? 0 : entity.getInt("length"));
		}
		
		return flush(form, thread, text.substring(lastOffset), insert, state);
	}
	
	static int flush(MPForm form, Thread thread, String text, int insert, int[] state) {
		if (text.length() == 0) return insert;
		
		StringBuffer sb = new StringBuffer(text);
		int space = 0;
		while (sb.length() != 0 && sb.charAt(sb.length() - 1) == ' ') {
			sb.setLength(sb.length() - 1);
			space ++;
		}
		text = sb.toString();
		Font f = getFont(state);
		StringItem s;
		
		// find links
		if (parseLinks && (state == null || (state[RT_PRE] == 0 && state[RT_URL] == 0))
				&& (text.indexOf("http://") != -1 || text.indexOf("https://") != -1
				|| text.indexOf('@') != -1)) {
			int i, j, k, d = 0;
			while (true) {
				boolean b = false;
				i = text.indexOf("://", d);
				j = text.indexOf('@', d);
				if (i == -1 && j == -1) break;
				if (j != -1 && (i == -1 || i > j)) {
					i = j;
				} else b = i != -1;
				
				if (b) {
					b: {
						boolean https;
						char c;
						if (i < 4 || ((https = text.charAt(i - 1) != 'p')
								&& (i < 5 || text.charAt(i - 1) != 's'))
							|| (i != (j = https ? 5 : 4)
							&& (c = text.charAt(i - j - 1)) > ' ' && c != '(')) {
							break b;
						}
						j = i - j;
						boolean valid = false;
						int len = text.length();
						for (k = j; k < len; ++k) {
							c = text.charAt(k);
							if (c <= ' ' || c == ',') break;
							if (c == '.') valid = true;
						}
						if (!valid) break b;
						
						if (i != 0) {
							s = new StringItem(null, text.substring(0, j));
							s.setFont(f);
							form.safeInsert(thread, insert++, s);
						}
						s = new StringItem(null, text.substring(j, k));
						s.setFont(f);
						s.setDefaultCommand(richTextLinkCmd);
						s.setItemCommandListener(midlet);
						form.safeInsert(thread, insert++, s);
						
						text = text.substring(k);
						d = 0;
						continue;
					}
					d = i + 3;
				} else {
					b: {
						char c;
						if (i != 0 && (c = text.charAt(i - 1)) > ' ' && c != '(') {
							break b;
						}
						b = text.charAt(i) == '@';
						int len = text.length();
						for (k = i + 1; k < len && k < i + 10; ++k) {
							c = text.charAt(k);
							if (c <= ' ' || c == ')' || c == ',' || c == '.') break;
							if (!b && (c < '0' || c > '9')) break b;
						}
						if (k == i + 10 || k == i + 1) break b;
						if (i != 0) {
							s = new StringItem(null, text.substring(0, i));
							s.setFont(f);
							form.safeInsert(thread, insert++, s);
						}
						s = new StringItem(null, text.substring(i, k));
						s.setFont(f);
						s.setDefaultCommand(richTextLinkCmd);
						s.setItemCommandListener(midlet);
						form.safeInsert(thread, insert++, s);
						
						text = text.substring(k);
						d = 0;
						continue;
					}
					d = i + 1;
				}
			}
		}
		
		s = new StringItem(null, text);
		s.setFont(f);
		if (state != null && state[RT_URL] != 0) {
			form.urls.put(s, richTextUrl);
			s.setDefaultCommand(richTextLinkCmd);
			s.setItemCommandListener(midlet);
		}
		
		if (text.length() != 0) {
			form.safeInsert(thread, insert++, s);
		}

		if (space != 0) {
			form.safeInsert(thread, insert++, new Spacer(f.charWidth(' ') * space, f.getBaselinePosition()));
		}
		
		return insert;
	}

	private static Font getFont(int[] state) {
		if (state == null) return smallPlainFont;
		int face = 0, style = 0, size = Font.SIZE_SMALL;
		if (state[RT_PRE] != 0) {
			face = Font.FACE_MONOSPACE;
			style = Font.STYLE_BOLD;
			size = Font.SIZE_SMALL;
		} else {
			if (state[RT_BOLD] != 0) {
				style |= Font.STYLE_BOLD;
			}
			if (state[RT_ITALIC] != 0) {
				style |= Font.STYLE_ITALIC;
			}
			if (state[RT_UNDERLINE] != 0) {
				style |= Font.STYLE_UNDERLINED;
			}
			// there is no strikethrough font in midp
//			if (state[RT_STRIKE] != 0) {
//				style |= Font.STYLE_UNDERLINED;
//			}
		}
		return getFont(face, style, size);
	}

	private static Font getFont(int face, int style, int size) {
		if (face == 0) {
//			int setSize = fontSize;
//			if (setSize == 0) {
//				size = size == Font.SIZE_LARGE ? Font.SIZE_MEDIUM : Font.SIZE_SMALL;
//			} else if (setSize == 2) {
//				size = size == Font.SIZE_SMALL ? Font.SIZE_MEDIUM : Font.SIZE_LARGE;
//			}
			
			if (size == Font.SIZE_SMALL) {
				if (style == Font.STYLE_BOLD) {
					return smallBoldFont;
				}
				if (style == Font.STYLE_ITALIC) {
					return smallItalicFont;
				}
				if (style == Font.STYLE_PLAIN) {
					return smallPlainFont;
				}
			} else if (size == Font.SIZE_MEDIUM) {
				if (style == Font.STYLE_BOLD) {
					return medBoldFont;
				}
				if (style == Font.STYLE_ITALIC) {
					return medItalicFont;
				}
				if (style == (Font.STYLE_BOLD | Font.STYLE_ITALIC)) {
					return medItalicBoldFont;
				}
				if (style == Font.STYLE_PLAIN) {
					return medPlainFont;
				}
			}
			if (size == Font.SIZE_LARGE && style == Font.STYLE_PLAIN) {
				return largePlainFont;
			}
		}
		return Font.getFont(face, style, size);
	}
	
/*
 * Part of the TUBE42 imagelib, released under the LGPL license.
 *
 * Development page: https://github.com/tube42/imagelib
 * License:          http://www.gnu.org/copyleft/lesser.html
 */
	
	static Image resize(Image src_i, int size_w, int size_h) {
		// set source size
		int w = src_i.getWidth();
		int h = src_i.getHeight();

		// no change??
		if (size_w == w && size_h == h)
			return src_i;

		int[] dst = new int[size_w * size_h];

		resize_rgb_filtered(src_i, dst, w, h, size_w, size_h);

		// not needed anymore
		src_i = null;

		return Image.createRGBImage(dst, size_w, size_h, true);
	}
	
	private static final void resize_rgb_filtered(Image src_i, int[] dst, int w0, int h0, int w1, int h1) {
		int[] buffer1 = new int[w0];
		int[] buffer2 = new int[w0];

		// UNOPTIMIZED bilinear filtering:
		//
		// The pixel position is defined by y_a and y_b,
		// which are 24.8 fixed point numbers
		// 
		// for bilinear interpolation, we use y_a1 <= y_a <= y_b1
		// and x_a1 <= x_a <= x_b1, with y_d and x_d defining how long
		// from x/y_b1 we are.
		//
		// since we are resizing one line at a time, we will at most 
		// need two lines from the source image (y_a1 and y_b1).
		// this will save us some memory but will make the algorithm 
		// noticeably slower

		for (int index1 = 0, y = 0; y < h1; y++) {

			final int y_a = ((y * h0) << 8) / h1;
			final int y_a1 = y_a >> 8;
			int y_d = y_a & 0xFF;

			int y_b1 = y_a1 + 1;
			if (y_b1 >= h0) {
				y_b1 = h0 - 1;
				y_d = 0;
			}

			// get the two affected lines:
			src_i.getRGB(buffer1, 0, w0, 0, y_a1, w0, 1);
			if (y_d != 0)
				src_i.getRGB(buffer2, 0, w0, 0, y_b1, w0, 1);

			for (int x = 0; x < w1; x++) {
				// get this and the next point
				int x_a = ((x * w0) << 8) / w1;
				int x_a1 = x_a >> 8;
				int x_d = x_a & 0xFF;

				int x_b1 = x_a1 + 1;
				if (x_b1 >= w0) {
					x_b1 = w0 - 1;
					x_d = 0;
				}

				// interpolate in x
				int c12, c34;
				int c1 = buffer1[x_a1];
				int c3 = buffer1[x_b1];

				// interpolate in y:
				if (y_d == 0) {
					c12 = c1;
					c34 = c3;
				} else {
					int c2 = buffer2[x_a1];
					int c4 = buffer2[x_b1];

					final int v1 = y_d & 0xFF;
					final int a_c2_RB = c1 & 0x00FF00FF;
					final int a_c2_AG_org = c1 & 0xFF00FF00;

					final int b_c2_RB = c3 & 0x00FF00FF;
					final int b_c2_AG_org = c3 & 0xFF00FF00;

					c12 = (a_c2_AG_org + ((((c2 >>> 8) & 0x00FF00FF) - (a_c2_AG_org >>> 8)) * v1)) & 0xFF00FF00
							| (a_c2_RB + ((((c2 & 0x00FF00FF) - a_c2_RB) * v1) >> 8)) & 0x00FF00FF;
					c34 = (b_c2_AG_org + ((((c4 >>> 8) & 0x00FF00FF) - (b_c2_AG_org >>> 8)) * v1)) & 0xFF00FF00
							| (b_c2_RB + ((((c4 & 0x00FF00FF) - b_c2_RB) * v1) >> 8)) & 0x00FF00FF;
				}

				// final result

				final int v1 = x_d & 0xFF;
				final int c2_RB = c12 & 0x00FF00FF;

				final int c2_AG_org = c12 & 0xFF00FF00;
				dst[index1++] = (c2_AG_org + ((((c34 >>> 8) & 0x00FF00FF) - (c2_AG_org >>> 8)) * v1)) & 0xFF00FF00
						| (c2_RB + ((((c34 & 0x00FF00FF) - c2_RB) * v1) >> 8)) & 0x00FF00FF;
			}
		}
	}

}
