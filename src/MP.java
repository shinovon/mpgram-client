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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
//#ifndef NO_FILE
import java.util.Random;
//#endif
import java.util.TimeZone;
import java.util.Vector;

import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.StreamConnection;
//#ifndef NO_FILE
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;
//#endif
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
import javax.microedition.lcdui.Ticker;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.control.VolumeControl;
import javax.microedition.midlet.MIDlet;
import javax.microedition.rms.RecordStore;

//#ifndef NO_ZIP
import zip.GZIPInputStream;
import zip.Inflater;
import zip.InflaterInputStream;
//#endif

public class MP extends MIDlet
	implements CommandListener, ItemCommandListener, ItemStateListener, Runnable, LangConstants, PlayerListener
{

	// region Constants
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
	static final int RUN_ZOOM_VIEW = 19;
	static final int RUN_PIN_MESSAGE = 20;
	static final int RUN_SEND_STICKER = 21;
	static final int RUN_INSTALL_STICKER_SET = 22;
	static final int RUN_LOAD_PLAYLIST = 23;
	static final int RUN_PLAYER_LOOP = 24;
	static final int RUN_CANCEL_UPDATES = 25;
	static final int RUN_DOWNLOAD_DOCUMENT = 26;
	static final int RUN_LOGOUT = 27;
	static final int RUN_START_PLAYER = 28;
	static final int RUN_OPEN_URL = 29;
	
	static final long ZERO_CHANNEL_ID = -1000000000000L;
	
	// RMS
	private static final String SETTINGS_RECORD_NAME = "mp4config";
	private static final String AUTH_RECORD_NAME = "mp4user";
	private static final String AVATAR_RECORD_PREFIX = "mcA";
	
	// URLs
	private static final String DEFAULT_INSTANCE_URL = "http://mp.nnchan.ru/";
	static final String API_URL = "api.php";
	static final String AVA_URL = "ava.php";
	static final String FILE_URL = "file.php";
	static final String OTA_URL = "http://nnproject.cc/mp/upd.php";
	
	static final String API_VERSION = "10";
	
	static final String[][] LANGS = {
		{
			"az",
			"ca",
			"de",
			"en",
			"es",
			"fi",
			"pt",
			"ru",
			"uk",
			"ar",
		},
		{
			"Azərbaycan",
			"Català",
			"Deutsch",
			"English",
			"Español",
			"Suomi",
			"Português",
			"Русский",
			"Українська",
			"العربية",
		}
	};
	
//#ifndef NO_CHAT_CANVAS
	static final String[][] THEMES = {
		{
			"tint",
			"webdark",
			"weblight",
			"edge",
			"light2",
		},
		{
			"Tint",
			"Dark",
			"Light",
			"Edges",
			"Desktop Light (nallion)",
		}
	};
//#endif
	
//#ifdef MINI
//#	static final boolean MINI_BUILD = true;
//#else
	static final boolean MINI_BUILD = false;
//#endif
	// endregion
	
	// Fonts
	static final Font largePlainFont = Font.getFont(0, 0, Font.SIZE_LARGE);
	static final Font medPlainFont = Font.getFont(0, 0, Font.SIZE_MEDIUM);
	static final Font medBoldFont = Font.getFont(0, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
	static final Font medItalicFont = Font.getFont(0, Font.STYLE_ITALIC, Font.SIZE_MEDIUM);
	static final Font medItalicBoldFont = Font.getFont(0, Font.STYLE_BOLD | Font.STYLE_ITALIC, Font.SIZE_MEDIUM);
	static final Font smallPlainFont = Font.getFont(0, 0, Font.SIZE_SMALL);
	static final Font smallBoldFont = Font.getFont(0, Font.STYLE_BOLD, Font.SIZE_SMALL);
	static final Font smallItalicFont = Font.getFont(0, Font.STYLE_ITALIC, Font.SIZE_SMALL);
	
	static int smallPlainFontHeight = smallPlainFont.getHeight();
	static int smallPlainFontSpaceWidth = smallPlainFont.charWidth(' ');
	static int smallBoldFontHeight = smallBoldFont.getHeight();
	static int medPlainFontHeight = medPlainFont.getHeight();
	static int medBoldFontHeight = medBoldFont.getHeight();

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
	
	// region Settings
	static String instanceUrl = DEFAULT_INSTANCE_URL;
	private static String instancePassword;
	private static int tzOffset;
	static boolean useLoadingForm;
	private static int avatarSize;
	static int photoSize = 120;
	static boolean loadAvatars = true;
	static boolean loadThumbs = true;
	static boolean reverseChat;
	static boolean showMedia = true;
	static int avatarsCache = 3; // 0: off, 1: hashtable, 2: storage, 3: both
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
	static long updatesDelay = 1000L;
	static int updatesTimeout = 30;
	static boolean sendTyping = true;
	static int chatsListFontSize = 0; // 0: default, 1: small, 2: medium
	static boolean keepAlive;
	static boolean utf = true;
	static long pushInterval = 30000L, pushBgInterval = 30000L;
	static boolean chatField = true;
	static boolean roundAvatars;
	static boolean useView = true;
	static boolean compress;
	static boolean fileRewrite;
	static int blackberryNetwork = -1; // -1: undefined, 0: data, 1: wifi
	static int playerCreateMethod = 0; // 0: auto, 1: pass url, 2: pass connection stream
	static boolean reopenChat;
	static boolean fullPlayerCover;
	static boolean notifications;
//#ifndef NO_NOTIFY
	static boolean muteUsers, muteChats, muteBroadcasts;
	static boolean notifySound = true;
	static int notifyMethod = 1; // 0: off, 1: alert, 2: nokiaui, 3: pigler api
	static boolean notifyAvas = true;
	static int notificationVolume = 100;
//#endif
	static boolean globalUpdates;
//#ifndef NO_CHAT_CANVAS
	static boolean legacyChatUI;
//#endif
	static boolean longpoll = true;
	static int playerVolume = 50;
	static boolean voiceConversion;
//#ifndef NO_CHAT_CANVAS
	static String theme = "tint";
	static int textMethod; // 0 - auto, 1 - nokiaui, 2 - j2mekeyboard, 3 - fullscreen textbox
	static boolean fastScrolling; // disable animations
	static final boolean forceKeyUI = false;
	static String[] inputLanguages = new String[] { "en", "ru" };
	static boolean pngStickers;
	static boolean lazyLoading = true;
//#endif
//#ifndef NO_FILE
	static int downloadMethod; // 0 - always ask, 1 - in app, 2 - browser
	static String downloadPath;
	static boolean chunkedUpload;
	private static String lastDownloadPath;
	private static String lastUploadPath;
	static int playMethod; // 0: stream, 1: write to file
//#endif
	private static boolean playlistDirection = true;

	private static boolean needWriteConfig;
	
	// platform
	static boolean symbianJrt;
	static String deviceName;
	static String systemName;
	public static String encoding = "UTF-8";
	static boolean blackberry;
	static boolean symbian;
	static boolean series40;
	// endregion

	// threading
	private static int run;
	private static Object runParam;
//	private static int running;
	static Thread updatesThread, updatesThreadCopy;
	static Hashtable threadConnections = new Hashtable();
	static Vector closingConnections = new Vector();
	static boolean sending;
	static boolean updatesRunning;
	static boolean updatesSleeping;
//#ifndef NO_FILE
	static boolean downloading;
//#endif
	
	private static final Object imagesLoadLock = new Object();
	private static final Vector imagesToLoad = new Vector(); // TODO hashtable?
	
	// auth
	private static String user;
	private static int userState;
	private static String phone;
	static String selfId;
	private static String phoneCodeHash;

	// region Commands
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
	private static Command authRetryCmd;
	
	private static Command logoutCmd;
	private static Command clearCacheCmd;
	private static Command downloadPathCmd;
	private static Command keyboardLanguagesCmd;
	private static Command saveLanguagesCmd;
	private static Command exportSessionCmd;

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
	static Command pinMsgCmd;
	static Command postCommentsCmd;
	
	static Command richTextLinkCmd;
	static Command openImageCmd;
	static Command callItemCmd;
	static Command documentCmd;
	static Command playItemCmd;
	static Command playVoiceCmd;

	static Command writeCmd;
	static Command chatInfoCmd;
	static Command olderMessagesCmd;
	static Command newerMessagesCmd;
	static Command latestCmd;
	static Command searchMsgCmd;
	static Command sendStickerCmd;
	
	static Command sendCmd;
	static Command openTextBoxCmd;
	static Command chooseFileCmd;
	
	static Command callCmd;
	static Command openChatCmd;
	static Command acceptInviteCmd;
	static Command joinChatCmd;
	static Command leaveChatCmd;
	static Command chatPhotosCmd;
	static Command chatVideosCmd;
	static Command chatFilesCmd;
	static Command chatMusicCmd;
	static Command chatVoiceCmd;
	static Command gotoPinnedMsgCmd;
	static Command chatMembersCmd;
	
	static Command stickerItemCmd;
	static Command addStickerPackCmd;

	static Command okCmd;
	static Command cancelCmd;
	static Command goCmd;
	static Command copyCmd;
	static Command downloadInappCmd;
	static Command downloadBrowserCmd;
	static Command cancelDownloadCmd;
	static Command okDownloadCmd;
	static Command openDownloadedCmd;
	static Command cancelUploadCmd;
	static Command confirmCmd;
	
	static Command nextPageCmd;
	static Command prevPageCmd;
	
	static Command playlistPlayCmd;
	static Command playlistPauseCmd;
	static Command playlistNextCmd;
	static Command playlistPrevCmd;
	static Command playlistCmd;
	static Command playerCmd;
	static Command togglePlaylistOrderCmd;
	
	private static Command updateCmd;
	// endregion
	
	// ui
	private static Displayable mainDisplayable;
	static Form loadingForm;
	static ChatsList chatsList;
	static FoldersList foldersList;
	private static Form settingsForm;
	private static Form authForm;
	private static Form writeForm;
	private static Form playerForm;
	private static List playlistList;
	private static final Vector formHistory = new Vector();

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
	private static ChoiceGroup networkChoice;
	private static ChoiceGroup playMethodChoice;
	private static ChoiceGroup playerCreateMethodChoice;
	private static Gauge avaCacheGauge;
	private static Gauge photoSizeGauge;
	private static Gauge profileCacheGauge;
	private static Gauge chatsGauge;
	private static Gauge msgsGauge;
	private static Gauge updateTimeoutGauge;
//#ifndef NO_NOTIFY
	private static ChoiceGroup notifyChoice;
	private static ChoiceGroup notifyMethodChoice;
	private static Gauge pushIntervalGauge;
	private static Gauge pushBgIntervalGauge;
	private static Gauge notificationVolumeGauge;
//#endif
//#ifndef NO_FILE
	private static ChoiceGroup downloadMethodChoice;
	private static TextField downloadPathField;
//#endif
//#ifndef NO_CHAT_CANVAS
	private static ChoiceGroup textMethodChoice;
	private static ChoiceGroup themeChoice;
//#endif
	
	// write items
	private static TextField messageField;
//	private static TextField fileField;
	private static ChoiceGroup sendChoice;
//#ifndef NO_FILE
	private static StringItem fileLabel;
//#endif
	
	// player items
	private static StringItem playerTitleLabel, playerArtistLabel;
	private static Gauge playerProgress;
	private static Gauge playerVolumeGauge;
	private static StringItem playerPlaypauseBtn;
	private static ImageItem playerCover;

	// cache
	private static final JSONObject usersCache = new JSONObject();
	private static final JSONObject chatsCache = new JSONObject();
	private static final Hashtable imagesCache = new Hashtable();
	
	private static Image userDefaultImg, chatDefaultImg;
//#ifndef NO_FILE
	private static Image fileImg, folderImg;
//#endif
	
	// temp
	private static String richTextUrl;
	private static String writeTo, replyTo, sendFile, edit, fwdPeer, fwdMsg;
	private static String updateUrl;
	private static long lastType;
	private static String[] downloadMessage;
	private static String downloadCurrentPath;
	private static String downloadedPath;
	
	static int confirmationTask;
	static Object confirmationParam;
	
//#ifndef NO_FILE
	// file picker
	private static Vector rootsList;
	private static boolean fileMode;
//#endif
	
	// music
	private static JSONArray playlist;
	private static int playlistIndex;
	private static int playlistSize;
	private static int playlistOffset;
	private static String playlistPeer;
	private static JSONObject currentMusic;
	static int playerState; // 1 - playing, 2 - paused, 3 - loading
	private static Player currentPlayer;
	
	// notifications
//#ifndef NO_NOTIFY
//#ifndef NO_NOKIAUI
	static Hashtable notificationMessages = new Hashtable();
//#endif
	static Player notificationPlayer;
//#endif
	
	// region MIDlet
	
	protected void destroyApp(boolean u) {
//#ifndef NO_NOTIFY
//#ifndef NO_NOKIAUI
		try {
			Notifier.close();
		} catch (Throwable ignored) {}
//#endif
//#endif
		if (needWriteConfig) {
			try {
				writeConfig();
			} catch (Throwable ignored) {}
		}
		notifyDestroyed();
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
		
		// sanity check
		if (!"nnproject".equals(getAppProperty("MIDlet-Vendor")))
			throw new RuntimeException();
		
		Form f = new Form("MPGram");
		f.append("Loading");
		display.setCurrent(mainDisplayable = f);
//#ifndef NO_J2ME_LOADER_CHECK
//		try {
//			// check for j2me loader
//			Class.forName("javax.microedition.shell.MicroActivity");
//			f.deleteAll();
//			f.addCommand(exitCmd = new Command("Exit", Command.EXIT, 1));
//			f.setCommandListener(midlet);
//			f.append("J2ME Loader is not supported.");
//			return;
//		} catch (Exception ignored) {}
//#endif	
		// get device name
		String p, v, d;
		if ((p = System.getProperty("microedition.platform")) != null) {
			d = p;
			if ((symbianJrt = p.indexOf("platform=S60") != -1)) {
				int i;
				v = p.substring(i = p.indexOf("platform_version=") + 17, i = p.indexOf(';', i));
				if (v.charAt(0) == '5') {
					switch (v.charAt(2)) {
					case '2':
						systemName = (p.indexOf("java_build_version=2.2") != -1) ? "Symbian Anna" : "Symbian^3";
						break;
					case '3':
						systemName = "Symbian Belle";
						break;
					case '4':
						systemName = "Symbian Belle FP1";
						break;
					case '5':
						systemName = "Symbian Belle FP2";
						break;
					default:
						systemName = "S60 5th Edition";
					}
				} else {
					// 3.2
					systemName = "S60 3rd Edition FP2";
				}
			}
			
			blackberry = p.toLowerCase().startsWith("blackberry");
			try {
				Class.forName("emulator.custom.CustomMethod");
				d = "KEmulator";
				if ((v = System.getProperty("kemulator.mod.version")) != null) {
					d = d.concat(" ".concat(v));
				}
			} catch (Exception e) {
				int i;
				
				if ((i = p.indexOf('/')) != -1 || (i = p.indexOf(' ')) != -1) {
					d = p.substring(0, i);
				}
			}
			deviceName = d;
		}
		
		symbian = symbianJrt
				|| System.getProperty("com.symbian.midp.serversocket.support") != null
				|| System.getProperty("com.symbian.default.to.suite.icon") != null
				|| checkClass("com.symbian.midp.io.protocol.http.Protocol")
				|| checkClass("com.symbian.lcdjava.io.File");
		if (symbian && systemName == null) {
			systemName = "Symbian";
		}

		boolean s40 = false;
		try {
			// s40 check
			Class.forName("com.nokia.mid.impl.isa.jam.Jam");
			series40 = s40 = true;
			systemName = "Series 40";
		} catch (Exception ignored) {}
		
		if (systemName == null && (p = System.getProperty("os.name")) != null) {
			if ((v = System.getProperty("os.version")) != null) {
				p = p.concat(" ".concat(v));
			}
			systemName = p;
		}
		
		// Test UTF-8 support
		byte[] b = new byte[] { (byte) 0xF0, (byte) 0x9F, (byte) 0x98, (byte) 0x83 };
		try {
			new InputStreamReader(new ByteArrayInputStream(b), encoding = "UTF-8").read();
			if (new String(b, encoding).length() != 2) throw new Exception();
		} catch (Exception e) {
			try {
				new InputStreamReader(new ByteArrayInputStream(b), encoding = "UTF8").read();
				if (new String(b, encoding).length() != 2) throw new Exception();
			} catch (Exception e2) {
				utf = false;
				b = new byte[] { (byte) 0xD0, (byte) 0xB2, (byte) 0xD1, (byte) 0x8B, (byte) 0xD1, (byte) 0x84 };
				try {
					new InputStreamReader(new ByteArrayInputStream(b), encoding = "UTF-8").read();
					if (new String(b, encoding).length() != 3) throw new Exception();
				} catch (Exception e3) {
					try {
						new InputStreamReader(new ByteArrayInputStream(b), encoding = "UTF8").read();
						if (new String(b, encoding).length() != 3) throw new Exception();
					} catch (Exception e4) {
						encoding = "ISO-8859-1";
					}
				}
			}
		}
		
		// get system language
		if ((p = System.getProperty("user.language")) == null) {
			p = System.getProperty("microedition.locale");
		}
		
		if (p != null) {
			lang = (p.length() > 2 ? p.substring(0, 2) : p).toLowerCase();
		}

		// init platform dependent settings
		useLoadingForm = !symbianJrt;
		jsonStream = symbianJrt || !symbian;
		threadedImages = symbianJrt;
		
		avatarSize = Math.min(display.getBestImageHeight(Display.LIST_ELEMENT), display.getBestImageWidth(Display.LIST_ELEMENT));
		if (avatarSize < 8) avatarSize = 16;
		else if (avatarSize > 120) avatarSize = 120;
		
		photoSize = Math.min(f.getWidth(), f.getHeight()) / 3;
		
		try {
			tzOffset = TimeZone.getDefault().getRawOffset() / 1000;
		} catch (Throwable ignored) {} // just to be sure
		
		fullPlayerCover = reverseChat = f.getHeight() >= 360;
		
//#ifndef NO_NOTIFY
//#ifndef NO_NOKIAUI
		notifyMethod = checkClass("org.pigler.api.PiglerAPI") ? 3 :
			// softnotification is stubbed in s40
			checkClass("com.nokia.mid.ui.SoftNotification") && !s40 ? 2 : 1;
//#endif
//#endif
		
		longpoll = !s40;
		parseRichtext = !s40;
//#ifndef NO_FILE
//		chunkedUpload = (!symbian || anna) && (!s40 || checkClass("javax.microedition.location.Location"));
		if (blackberry) textMethod = 3;
		else if (s40 && System.getProperty("com.nokia.mid.ui.version") == null) {
			textMethod = 2;
		}
//#endif
		
		// load settings
		try {
			RecordStore r = RecordStore.openRecordStore(SETTINGS_RECORD_NAME, false);
			JSONObject j = parseObject(new String(r.getRecord(1), "UTF-8"));
			r.closeRecordStore();
			
			reverseChat = j.getBoolean("reverseChat", reverseChat);
//#ifndef NO_AVATARS
			loadAvatars = j.getBoolean("loadAvatars", loadAvatars);
			avatarSize = j.getInt("avatarSize", avatarSize);
//#endif
			showMedia = j.getBoolean("showMedia", showMedia);
			photoSize = j.getInt("photoSize", photoSize);
			loadThumbs = j.getBoolean("loadThumbs", loadThumbs);
			threadedImages = j.getBoolean("threadedImages", threadedImages);
//#ifndef NO_AVATARS
			avatarsCache = j.getInt("avatarsCache", avatarsCache);
			avatarsCacheThreshold = j.getInt("avatarsCacheThreshold", avatarsCacheThreshold);
//#endif
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
//#ifndef NO_AVATARS
			roundAvatars = j.getBoolean("roundAvatars", roundAvatars);
//#endif
			utf = j.getBoolean("utf", utf);
//#ifndef NO_ZIP
			compress = j.getBoolean("compress", compress);
//#endif
			useView = j.getBoolean("useView", useView);
			blackberryNetwork = j.getInt("blackberryNetwork", blackberryNetwork);
			fullPlayerCover = j.getBoolean("fullPlayerCover", fullPlayerCover);
//#ifndef NO_NOTIFY
			notifications = j.getBoolean("notifications", notifications);
			notifySound = j.getBoolean("notifySound", notifySound);
			pushInterval = j.getLong("pushInterval", pushInterval);
			pushBgInterval = j.getLong("pushBgInterval", pushBgInterval);
			notifyMethod = j.getInt("notifyMethod", notifyMethod);
			notificationVolume = j.getInt("notifyVolume", notificationVolume);
//#endif
//#ifndef NO_CHAT_CANVAS
			legacyChatUI = j.getBoolean("legacyChatUI", legacyChatUI);
			textMethod = j.getInt("textMethod", textMethod);
			theme = j.getString("theme", theme);
			
			JSONArray inputLanguagesJson = j.getArray("inputLanguages", null);
			if (inputLanguagesJson != null) {
				int l = inputLanguagesJson.size();
				inputLanguages = new String[l];
				for (int i = 0; i < l; i++) {
					inputLanguages[i] = inputLanguagesJson.getString(i);
				}
			}
			lazyLoading = j.getBoolean("lazyLoading", lazyLoading);
			fastScrolling = j.getBoolean("fastScrolling", fastScrolling);
//#endif
//#ifndef NO_FILE
			downloadPath = j.getString("downloadPath", downloadPath);
			chunkedUpload = j.getBoolean("uploadChunked", chunkedUpload);
			downloadMethod = j.getInt("downloadMethod", downloadMethod);
			lastDownloadPath = j.getString("lastDownloadPath", lastDownloadPath);
			lastUploadPath = j.getString("lastUploadPath", lastUploadPath);
			playMethod = j.getInt("playMethod", playMethod);
//#endif
			longpoll = j.getBoolean("longpoll", longpoll);
			playlistDirection = j.getBoolean("playlistDirection", playlistDirection);
			playerVolume = j.getInt("playerVolume", playerVolume);
			playerCreateMethod = j.getInt("playerCreateMethod", playerCreateMethod);
		} catch (Exception ignored) {}
		
		// load auth
		try {
			RecordStore r = RecordStore.openRecordStore(AUTH_RECORD_NAME, false);
			JSONObject j = parseObject(new String(r.getRecord(1), "UTF-8"));
			r.closeRecordStore();

			user = j.getString("user", user);
			userState = j.getInt("state", 0);
			phone = j.getString("phone", null);
			instanceUrl = j.getString("url", instanceUrl);
			instancePassword = j.getString("instPass", instancePassword);
		} catch (Exception ignored) {}
	
		
		// load locale
		(L = new String[LLocaleStrings + 2])[Lmpgram] = "MPGram";
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
		
		exitCmd = new Command(L[LExit], Command.EXIT, 27);
		backCmd = new Command(L[LBack], Command.BACK, 25);
		
		settingsCmd = new Command(L[LSettings], Command.SCREEN, 20);
		aboutCmd = new Command(L[LAbout], Command.SCREEN, 21);
		
		authCmd = new Command(L[LAuth], Command.ITEM, 1);
		authNextCmd = new Command(L[LNext], Command.OK, 1);
		authCodeCmd = new Command(L[LNext], Command.OK, 1);
		authPasswordCmd = new Command(L[LNext], Command.OK, 1);
		authNewSessionCmd = new Command(L[LNewSession], Command.SCREEN, 1);
		authImportSessionCmd = new Command(L[LImportSession], Command.SCREEN, 2);
		authRetryCmd = new Command(L[LRetry], Command.OK, 1);
		
		logoutCmd = new Command(L[LLogout], Command.ITEM, 1);
		clearCacheCmd = new Command(L[LClearCache], Command.ITEM, 1);
		downloadPathCmd = new Command(L[LLocate], Command.ITEM, 1);
		keyboardLanguagesCmd = new Command(L[LSelect], Command.ITEM, 1);
		saveLanguagesCmd = new Command(L[LBack], Command.BACK, 1);
		exportSessionCmd = new Command(L[LShowSessionCode], Command.ITEM, 1);

		foldersCmd = new Command(L[LFolders], Command.SCREEN, 4);
		refreshCmd = new Command(L[LRefresh], Command.SCREEN, 5);
		archiveCmd = new Command(L[LArchivedChats], Command.SCREEN, 8);
		contactsCmd = new Command(L[LContacts], Command.SCREEN, 9);
		searchChatsCmd = new Command(L[LSearch], Command.SCREEN, 10);
		openLinkCmd = new Command(L[LOpenByLink], Command.SCREEN, 11);
		
		itemChatCmd = new Command(L[LOpenChat], Command.ITEM, 1);
		itemChatInfoCmd = new Command(L[LProfile], Command.ITEM, 2);
		replyMsgCmd = new Command(L[LReply], Command.ITEM, 3);
		forwardMsgCmd = new Command(L[LForward], Command.ITEM, 4);
		copyMsgCmd = new Command(L[LCopyMessage], Command.ITEM, 5);
		messageLinkCmd = new Command(L[LCopyMessageLink], Command.ITEM, 7);
		deleteMsgCmd = new Command(L[LDelete], Command.ITEM, 8);
		editMsgCmd = new Command(L[LEdit], Command.ITEM, 9);
		gotoMsgCmd = new Command(L[LGoTo], Command.ITEM, 1);
		botCallbackCmd = new Command(L[LRunBotAction], Command.ITEM, 1);
		banMemberCmd = new Command(L[LBanMember], Command.ITEM, 11);
		pinMsgCmd = new Command(L[LPin], Command.ITEM, 10);
		postCommentsCmd = new Command(L[LComments], Command.ITEM, 1); 
		
		richTextLinkCmd = new Command(L[LLink_Cmd], Command.ITEM, 1);
		openImageCmd = new Command(L[LViewImage], Command.ITEM, 1);
		callItemCmd = new Command(L[LCall], Command.ITEM, 1);
		documentCmd = new Command(L[LDownload], Command.ITEM, 2);
		playItemCmd = new Command(L[LPlay_Item], Command.ITEM, 1);
		playVoiceCmd = new Command(L[LPlay_Item], Command.ITEM, 1);
		
		writeCmd = new Command(L[LWriteMessage], Command.SCREEN, 5);
		latestCmd = new Command(L[LLatestMessages_Cmd], Command.SCREEN, 7);
		chatInfoCmd = new Command(L[LChatInfo], Command.SCREEN, 8);
		olderMessagesCmd = new Command(L[LOlder], Command.ITEM, 1);
		newerMessagesCmd = new Command(L[LNewer], Command.ITEM, 1);
		searchMsgCmd = new Command(L[LSearch], Command.SCREEN, 10);
		sendStickerCmd = new Command(L[LSendSticker], Command.SCREEN, 6);
		
		sendCmd = new Command(L[LSend], Command.OK, 1);
		openTextBoxCmd = new Command(L[LOpenTextBox], Command.ITEM, 1);
		chooseFileCmd = new Command(L[LChooseFile], Command.ITEM, 1);
		
		callCmd = new Command(L[LCall], Command.SCREEN, 5);
		openChatCmd = new Command(L[LOpenChat], Command.SCREEN, 1);
		acceptInviteCmd = new Command(L[LJoin], Command.ITEM, 1);
		joinChatCmd = new Command(L[LJoinGroup], Command.SCREEN, 1);
		leaveChatCmd = new Command(L[LLeaveGroup], Command.ITEM, 1);
		chatPhotosCmd = new Command(L[LPhotos], Command.ITEM, 1);
		chatVideosCmd = new Command(L[LVideos], Command.ITEM, 2);
		chatFilesCmd = new Command(L[LFiles], Command.ITEM, 3);
		chatMusicCmd = new Command(L[LAudioFiles], Command.ITEM, 4);
		chatVoiceCmd = new Command(L[LVoiceMessages], Command.ITEM, 5);
		gotoPinnedMsgCmd = new Command(L[LGoTo], Command.ITEM, 1);
		chatMembersCmd = new Command(L[LMembers], Command.SCREEN, 6);
		
		stickerItemCmd = new Command(L[LSticker], Command.ITEM, 1);
		addStickerPackCmd = new Command(L[LAddStickers], Command.OK, 2);
		
		okCmd = new Command(L[LOk], Command.OK, 1);
		cancelCmd = new Command(L[LCancel], Command.CANCEL, 26);
		goCmd = new Command(L[LOk], Command.OK, 1);
		copyCmd = new Command(L[LCopy], Command.OK, 1);
		downloadInappCmd = new Command(L[LInApp], Command.OK, 0);
		downloadBrowserCmd = new Command(L[LWithBrowser], Command.CANCEL, 1);
		cancelDownloadCmd = new Command(L[LCancel], Command.CANCEL, 1);
		okDownloadCmd = new Command(L[LOk], Command.CANCEL, 1);
		openDownloadedCmd = new Command("Open", Command.SCREEN, 2); // TODO unlocalized
		cancelUploadCmd = new Command(L[LCancel], Command.CANCEL, 1);
		confirmCmd = new Command(L[LOk], Command.OK, 1);

		nextPageCmd = new Command(L[LNextPage], Command.SCREEN, 6);
		prevPageCmd = new Command(L[LPrevPage], Command.SCREEN, 7);
		
		updateCmd = new Command(L[LDownload], Command.OK, 1);
		
		playlistPlayCmd = new Command(L[LPlay_Player], Command.ITEM, 1);
		playlistPauseCmd = new Command(L[LPause_Player], Command.ITEM, 1);
		playlistNextCmd = new Command(L[LNext_Player], Command.ITEM, 1);
		playlistPrevCmd = new Command(L[LPrev_Player], Command.ITEM, 1);
		playlistCmd = new Command(L[LOpenPlaylist], Command.SCREEN, 2);
		playerCmd = new Command(L[LOpenPlayer], Command.SCREEN, 20);
		togglePlaylistOrderCmd = new Command(L[LTogglePlaylistOrder], Command.SCREEN, 3);
		
		loadingForm = new Form(L[Lmpgram]);
		loadingForm.append(L[LLoading]);
		loadingForm.addCommand(cancelCmd);
		loadingForm.setCommandListener(this);
		
		// load resources	
//#ifndef NO_AVATARS
		if (loadAvatars) {
			try {
				userDefaultImg = resize(Image.createImage("/us.png"), avatarSize, avatarSize);
				chatDefaultImg = resize(Image.createImage("/gr.png"), avatarSize, avatarSize);
				if (roundAvatars) {
					userDefaultImg = roundImage(userDefaultImg);
					chatDefaultImg = roundImage(chatDefaultImg);
				}
			} catch (Throwable ignored) {}
		}
//#endif
		
		// start image loader threads

		start(RUN_IMAGES, null);

		if (threadedImages) {
			start(RUN_IMAGES, null);
			start(RUN_IMAGES, null);
		}
		
		// create auth form
		
		f = new Form(L[LAuth_Title]);
		f.addCommand(exitCmd);
		f.addCommand(aboutCmd);
		f.addCommand(settingsCmd);
		f.setCommandListener(midlet);
		
		TextField t = new TextField(L[LInstanceURL], instanceUrl, 200, TextField.URL);
		t.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
		instanceField = t;
		f.append(t);
		
		t = new TextField(L[LInstancePassword], instancePassword, 200, TextField.NON_PREDICTIVE);
		t.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
		instancePasswordField = t;
		f.append(t);
		
		StringItem s;
		
		s = new StringItem(null, L[LAuth_Hint1]);
		s.setFont(smallPlainFont);
		s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
		f.append(s);
		
		s = new StringItem(null, L[LCreateNewSession_Btn], StringItem.BUTTON);
		s.setDefaultCommand(authNewSessionCmd);
		s.setItemCommandListener(midlet);
		s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
		f.append(s);

		s = new StringItem(null, L[LImportSession_Btn], StringItem.BUTTON);
		s.setDefaultCommand(authImportSessionCmd);
		s.setItemCommandListener(midlet);
		s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
		f.append(s);
		
//#ifndef NO_NOTIFY
//#ifndef NO_NOKIAUI
		// init notifications api wrapper
		try {
			Notifier.init();
		} catch (Throwable ignored) {}
//#endif
//#endif
		
		authForm = f;
		// load main form
		if (user == null || userState < 3) {
			display(mainDisplayable = authForm);
			// show network access settings on blackberry
			if (blackberry && blackberryNetwork == -1) {
				commandAction(settingsCmd, current);
				display(infoAlert(L[LChooseNetwork_Alert]), current);
				return;
			}
//#ifndef NO_NOTIFY
//#ifndef NO_NOKIAUI
			else if (notifyMethod == 3 && symbianJrt && !checkClass("org.pigler.api.PiglerAPI")) {
				display(alert(null, L[LPiglerNotAvailable_Alert], AlertType.WARNING), current);
			}
//#endif
//#endif
			start(RUN_CHECK_OTA, null);
			return;
		}
		
		start(RUN_VALIDATE_AUTH, null);
	}
	
	// endregion
	
	// region Threading

	public void run() {
		int run;
		Object param;
		synchronized (this) {
			run = MP.run;
			param = MP.runParam;
			MP.run = 0;
			MP.runParam = null;
			notify();
		}
//		running++;
		switch (run) {
		case RUN_VALIDATE_AUTH: {
			Displayable returnTo = param == null ? authForm : current;
			Alert alert = loadingAlert(L[LAuthorizing]);
			if (param == null) {
				alert.addCommand(exitCmd);
				alert.setCommandListener(this);
			}
			display(alert, param == null ? null : returnTo);
			try {
				selfId = ((JSONObject) api("me&status=1")).getString("id");
				userState = 4;

				openLoad(mainDisplayable = mainChatsList());
				if (param != null) {
					writeAuth();
				} else {
					start(RUN_CHECK_OTA, null);
				}

				if (keepAlive || notifications || globalUpdates)
					start(RUN_KEEP_ALIVE, null);
				break;
			} catch (APIException e) {
				if (e.code == 401) {
					if (param == null) {
						mainDisplayable = returnTo;
					}
					userState = 0;
					user = null;
					display(errorAlert(e), returnTo);
					break;
				}
				alert = errorAlert(e);
			} catch (Exception e) {
				alert = errorAlert(e);
			}
			if (param == null) {
				mainDisplayable = returnTo;
			}
			alert.addCommand(authRetryCmd);
			alert.addCommand(cancelCmd);
			alert.setCommandListener(this);
			display(alert, returnTo);
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
						Object[] o;
						
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
//#ifndef NO_AVATARS
							if (src instanceof String) { // avatar
								recordName = AVATAR_RECORD_PREFIX + avatarSize + "r" + (String) src;
								url = instanceUrl + AVA_URL + "?a&c=" + ((String) src)
										+ "&p=r" + /*(roundAvatars ? "c" : "") +*/ avatarSize;

								// load avatar from cache
								if ((avatarsCache & 1) == 1 && imagesCache.containsKey(src)) {
									img = (Image) imagesCache.get(src);
								} else if ((avatarsCache & 2) == 2) {
									try {
										RecordStore r = RecordStore.openRecordStore(recordName, false);
										try {
											byte[] b = r.getRecord(1);
											img = Image.createImage(b, 0, b.length);
											if (roundAvatars) img = roundImage(img);
										} finally {
											r.closeRecordStore();
										}
									} catch (Exception ignored) {}
								}
							} else
//#endif
							if (src instanceof String[]) { // message file
								String peer = ((String[]) src)[0];
								String id = ((String[]) src)[1];
								String p = ((String[]) src)[3];
								StringBuffer sb = new StringBuffer(instanceUrl);
								sb.append(FILE_URL)
								.append("?a&c=").append(peer)
								.append("&m=").append(id)
								.append("&p=").append(p);
								if (p.indexOf("&s=") == -1) {
									sb.append("&s=").append(photoSize);
								}
								url = sb.toString();
							} else if (src instanceof JSONObject) { // sticker or document
								url = instanceUrl + FILE_URL + "?a&sticker=" + ((JSONObject) src).getString("id")
										+ "&access_hash=" + ((JSONObject) src).getString("access_hash") + "&s=32&p=r" + 
										("application/x-tgsticker".equals(((JSONObject) src).getString("mime", ""))
												? "tgss" : "sprevs");
//#ifndef NO_CHAT_CANVAS
							} else if (src instanceof UIMessage) {
								UIMessage msg = (UIMessage) src;
								StringBuffer sb = new StringBuffer(instanceUrl);
								sb.append(FILE_URL)
								.append("?a&c=").append(msg.peerId)
								.append("&m=").append(msg.id)
								.append("&p=");
								if (msg.photo) {
									sb.append("rprev&s=").append(photoSize);
								} else if (msg.animatedSticker) {
									sb.append("rtgs");
									if (pngStickers) {
										sb.append('p');
									}
									sb.append("s&s=").append(photoSize);
								} else if (msg.sticker) {
									sb.append("rsticker");
									if (pngStickers) {
										sb.append('p');
									}
									sb.append("&s=").append(photoSize);
								} else {
									// document thumbnail
									sb.append("thumbrsprevs&s=").append(MP.smallBoldFontHeight + MP.smallPlainFontHeight - 2);
								}
								url = sb.toString();
//#endif
							} else {
								continue;
							}
							if (img == null) {
								try {
									byte[] b = get(url);
//#ifndef NO_AVATARS
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
//#endif
									img = Image.createImage(b, 0, b.length);
//#ifndef NO_AVATARS
									if (recordName != null && roundAvatars)
										img = roundImage(img);
//#endif
								} catch (Exception e) {
									e.printStackTrace();
									if (src instanceof String) {
										img = ((String) src).charAt(0) == '-' ? chatDefaultImg : userDefaultImg;
									}
								}
							}
							
							if (img == null) continue;
//#ifndef NO_AVATARS
							// save avatar to hashtable cache
							if (recordName != null && (avatarsCache & 1) == 1) {
								if (imagesCache.size() > avatarsCacheThreshold) {
									imagesCache.clear();
								}
								imagesCache.put(src, img);
							}
//#endif
							
							putImage(target, img);
						} catch (OutOfMemoryError e) {
							gc();
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
			if (param instanceof MPList) {
				((MPList) param).load();
				break;
			}
			if (param instanceof MPChat) {
				((MPChat) param).load();
				break;
			}
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
					if (j.has("phone_code_hash")) {
						phoneCodeHash = j.getString("phone_code_hash");
					}
					if (res.indexOf("captcha") != -1) {
						display(errorAlert(L[LInvalidCaptcha_Alert]), null);
						((CaptchaForm) param).load();
						break;
					}
					if (!"code_sent".equals(res)) {
						if ("phone_number_invalid".equals(res)) {
							display(errorAlert(L[LInvalidPhoneNumber_Alert]), null);
						} else {
							display(errorAlert(res), null);
						}
						userState = 1;
						break;
					}

					writeAuth();
					TextBox t = new TextBox(L[LCode], "", 6, TextField.NUMERIC);
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
								if ("password_hash_invalid".equals(res)) {
									res = L[LInvalidPassword_Alert];
								}
								display(errorAlert(res), null);
								break;
							}
						} catch (APIException e) {
							commandAction(backCmd, current);
							display(errorAlert(e), null);
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
							TextBox t = new TextBox(L[LCloudPassword], "", 100, TextField.NON_PREDICTIVE);
							t.addCommand(authPasswordCmd);
							t.setCommandListener(this);
							display(t);
							break;
						}
						if (!"1".equals(res)) {
							if ("phone_code_invalid".equals(res)) {
								if (phoneCodeHash != null) {
									sb.setLength(0);
									sb.append("resendCode&hash=").append(phoneCodeHash).append("&phone=");
									appendUrl(sb, phone);
									api(sb.toString());
								}
								
								res = L[LInvalidCode_Alert];
							}
							display(errorAlert(res), null);
							break;
						}
					}
					
					// auth complete
					userState = 3;
					MP.run = RUN_VALIDATE_AUTH;
					MP.runParam = user;
					run();
				}

			} catch (Exception e) {
				display(errorAlert(e), null);
			}
			break;
		}
		case RUN_DELETE_MESSAGE: {
			try {
//#ifndef NO_CHAT_CANVAS
				if (param instanceof UIMessage[]) {
					UIMessage[] msgs = (UIMessage[]) param;
					StringBuffer sb = new StringBuffer("deleteMessage&id=");
					for (int i = 0; i < msgs.length; ++i) {
						sb.append(msgs[i].id).append(',');
					}
					sb.setLength(sb.length() - 1);
					sb.append("&peer=").append(msgs[0].peerId);
					MP.api(sb.toString());
					
					commandAction(refreshCmd, current);
					break;
				}
//#endif
				String[] s = (String[]) param;
				MP.api("deleteMessage&peer=".concat(s[0].concat("&id=").concat(s[1])));

				// refresh chat after deleting
				commandAction(refreshCmd, current);
//				display(infoAlert(L[LMessageDeleted_Alert]), current);
			} catch (Exception e) {
				display(errorAlert(e), current);
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
				String fwdPeer = (String) ((Object[]) param)[6];
				String fwdMsg = (String) ((Object[]) param)[7];
//#ifndef NO_CHAT_CANVAS
				UIMessage[] fwdMsgs = ((Object[]) param).length < 9 ? null : (UIMessage[]) ((Object[]) param)[8];
//#endif
				
				Alert alert = null;
				if (sendChoice != null || file != null) {
					alert = new Alert(symbian ? L[Lmpgram] : "");
					alert.setString(L[LSending]);
					alert.setIndicator(new Gauge(null, false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING));
					alert.addCommand(cancelUploadCmd);
					alert.setCommandListener(this);
					alert.setTimeout(Alert.FOREVER);
					display(alert, current);
				}
				
				if (file != null && file.length() <= 8) {
					file = null;
				}
				StringBuffer sb;
				if (edit != null) {
					sb = new StringBuffer("editMessage");
					sb.append("&peer=").append(writeTo)
					.append("&id=").append(edit);
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
//#ifndef NO_CHAT_CANVAS
					if (fwdMsgs != null && fwdMsgs.length != 0) {
						if (fwdMsgs.length == 1) {
							sb.append("&fwd_from=").append(fwdMsgs[0].peerId)
							.append("&id=").append(fwdMsgs[0].id);
						} else {
							sb.append("&fwd_from=").append(fwdMsgs[0].peerId)
							.append("&id=");
							int l = fwdMsgs.length;
							for (int i = 0; i < l; ++i) {
								sb.append(fwdMsgs[i].id).append(',');
							}
							sb.setLength(sb.length() - 1);
						}
					} else
//#endif
					if (fwdPeer != null && fwdMsg != null) {
						sb.append("&fwd_from=").append(fwdPeer)
						.append("&id=").append(fwdMsg);
					}
				}
				sb.append("&r=").append(System.currentTimeMillis());
//#ifndef NO_FILE
				try {
					if (!checkClass("javax.microedition.io.file.FileConnection")) throw new Error();
					postMessage(sb.toString(), file, text, alert);
				} catch (Error e)
//#endif
				{
//#ifndef NO_FILE
					if (e instanceof OutOfMemoryError) {
						gc();
						display(errorAlert(L[LNotEnoughMemory_Alert]), current);
						break;
					}
//#endif
					api(appendUrl(sb.append("&text="), text).toString());
				}

				// cancel alerts
				display(current);
				Thread.sleep(10);
				
				// go back to chat screen
				if (!(current instanceof MPChat)) {
					commandAction(backCmd, current);
				} else {
					((MPChat) current).sent();
				}

				if ((reopenChat || !longpoll || !((MPChat) current).updating() || !((MPChat) current).endReached()) && !globalUpdates) {
					// load latest messages
					commandAction(latestCmd, current);
					// for some reason these commented lines cause weird crash on nokia e52
//				} else if (display.getCurrent() != current) {
//					display(current);
				}
//				display(infoAlert(L[LMessageSent_Alert]), current);
			} catch (Exception e) {
				e.printStackTrace();
				if (e == cancelException) {
					display(alert(null, L[LDownloadCanceled_Alert], AlertType.WARNING), current);
				} else {
					display(errorAlert(e), current);
				}
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
				
				openLoad(new ChatInfoForm(id, getNameRaw(rawPeer), "chatInvitePeek".equals(type) ? 2 : 3));
			} catch (Exception e) {
				display(errorAlert(e), current);
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
				display(errorAlert(e), current);
			}
			break;
		}
		case RUN_JOIN_CHANNEL: 
		case RUN_LEAVE_CHANNEL: {
			try {
				MP.api((run == RUN_JOIN_CHANNEL ? "join" : "leave").concat("Channel&id=").concat((String) param));
				
				if (run == RUN_JOIN_CHANNEL) {
					if (current instanceof MPChat) {
						commandAction(latestCmd, current);
					} else {
						commandAction(backCmd, current);
						openChat((String) param, 0);
					}
				} else {
					commandAction(refreshCmd, current);
				}
			} catch (Exception e) {
				display(errorAlert(e), current);
			}
			break;
		}
		case RUN_CHECK_OTA: { // check for client updates
			try {
				JSONObject j = parseObject(new String(get(OTA_URL + "?v=" + version + "&l=" + lang
						+ (MINI_BUILD ? "&m=1" : "")
						+ (midlet.getAppProperty("mpgram-blackberry-build") != null ? "&bb=1" : "")
						+ (midlet.getAppProperty("mpgram-samsung-build") != null ? "&sams=1" : "")), encoding));
				if (j.getBoolean("update_available", false) && checkUpdates) {
					updateUrl = j.getString("download_url");
					Alert a = new Alert("", "", null, AlertType.INFO);
					a.setTimeout(-2);
					a.setString(j.getString("message", L[LUpdateAvailable_Alert]));
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
			updatesThread = updatesThreadCopy = thread = Thread.currentThread();
			updatesRunning = true;
			MPChat form = (MPChat) param;
			try {
				StringBuffer sb = new StringBuffer();
				JSONObject j;
				
				int offset = 0;
				int fails = 0;
				boolean check = true;
				while (form.updating() && updatesThread == thread) {
					try {
						if (!form.updating() || updatesThread != thread) break;
						if (!form.isShown() || paused) {
							updatesSleeping = true;
							Thread.sleep(updatesDelay);
							updatesSleeping = false;
							continue;
						}
						Thread.sleep(10);
						if (check) {
							sb.setLength(0);
							sb.append("getLastUpdate&peer=").append(form.id());
							if (offset <= 0) {
								sb.append("&id=").append(form.firstMsgId());
							}
							try {
								j = ((JSONObject) api(sb.toString())).getObject("res");
								int off = j.getInt("update_id");
								if (!j.getBoolean("exact", false))
									off -= 1;
								if (offset <= 0 || off < offset)
									offset = off;
							} catch (Exception ignored) {}
							check = false;
						}
						if (!form.updating() || updatesThread != thread) break;
						
						sb.setLength(0);
						sb.append("updates&media=1&read=1&peer=").append(form.id())
						.append("&offset=").append(offset)
						.append("&timeout=").append(longpoll ? updatesTimeout : 1)
						.append("&message=").append(form.firstMsgId())
						.append("&limit=200");
						if (!longpoll) {
							sb.append("&longpoll=0");
						}
						if (form.topMsgId() != 0) {
							sb.append("&top_msg=").append(form.topMsgId());
						}
						
						j = (JSONObject) api(sb.toString());
						
						if (j.has("cancel")) {
							throw cancelException;
						}
						
						JSONArray updates = j.getArray("res");
						int l = updates.size();
						
						for (int i = 0; i < l; ++i) {
							JSONObject update = updates.getObject(i);
							offset = update.getInt("update_id");
							update = update.getObject("update");
							String type = update.getString("_");
							if ("updateUserStatus".equals(type)) {
								form.handleUpdate(MPChat.UPDATE_USER_STATUS, update);
							} else if ("updateUserTyping".equals(type)
									|| "updateChatUserTyping".equals(type)
									|| "updateChannelUserTyping".equals(type)) {
								form.handleUpdate(MPChat.UPDATE_USER_TYPING, update);
							} else if ("updateNewMessage".equals(type)
									|| "updateNewChannelMessage".equals(type)) {
								form.handleUpdate(MPChat.UPDATE_NEW_MESSAGE, update);
							} else if ("updateDeleteChannelMessages".equals(type)) {
								form.handleUpdate(MPChat.UPDATE_DELETE_MESSAGES, update);
							} else if ("updateEditMessage".equals(type)
									|| "updateEditChannelMessage".equals(type)) {
								form.handleUpdate(MPChat.UPDATE_EDIT_MESSAGE, update);
							} else if ("updateReadHistoryOutbox".equals(type)
									|| "updateReadChannelOutbox".equals(type)) {
								form.handleUpdate(MPChat.UPDATE_READ_OUTBOX, update);
							}
						}
						
						if (!longpoll) {
							updatesSleeping = true;
							Thread.sleep(updatesTimeout * 1000L);
							updatesSleeping = false;
						}
						if (fails != 0) --fails;
					} catch (Exception e) {
						if (e.toString().indexOf("Interrupted") != -1 || e == cancelException) {
							form.setUpdate(false);
							break;
						}
						e.printStackTrace();
						check = true;
						if (++fails >= 5 && form.updating()) {
							form.setUpdate(false);
							if (form.isShown()) {
								display(errorAlert("Updates thread died!\n".concat(e.toString())), null);
							}
							break;
						}
						updatesSleeping = true;
						Thread.sleep(updatesDelay);
						updatesSleeping = false;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (form != null && form.updating()) {
					form.setUpdate(false);
				}
				if (updatesThread == thread)
					updatesThread = null;
				updatesRunning = false;
				updatesSleeping = false;
			}
			break;
		}
		case RUN_SET_TYPING: {
			try {
				String peer = writeTo;
				if (current instanceof MPChat) {
					peer = ((MPChat) current).id();
				}
				if (peer == null) return;
				api("setTyping&action=" + (param == null ? "Typing" : (String) param)
						+ "&peer=" + peer);
			} catch (Exception ignored) {}
			break;
		}
		case RUN_KEEP_ALIVE: { // Keep session alive & notifications
			try {
				boolean wasShown = false;
				StringBuffer sb = new StringBuffer();
				JSONObject j;

//#ifndef NO_NOTIFY
				while (notifications && user != null) {
					try {
						j = ((JSONObject) api("getNotifySettings"));
						muteUsers = j.getInt("users") != 0;
						muteChats = j.getInt("chats") != 0;
						muteBroadcasts = j.getInt("broadcasts") != 0;
						break;
					} catch (Exception ignored) {
						continue;
					}
				}
//#endif
				
				int offset = 0;
				boolean check = true;
				while (user != null
						&& (keepAlive
//#ifndef NO_NOTIFY
						|| notifications
						|| globalUpdates
//#endif
						)) {
					Thread.sleep(globalUpdates ? 1 : wasShown ? pushInterval : pushBgInterval);
					if (
//#ifndef NO_NOTIFY
							(!notifications || !globalUpdates) &&
//#endif
							(threadConnections.size() != 0
							|| (playerState == 1 && (reopenChat || series40))))
						continue;
					
					boolean shown = false;
					try {
						Displayable c = display.getCurrent();
						shown = !paused && c != null && c.isShown();
					} catch (Exception ignored) {}

					// update status
					if (keepAlive && !globalUpdates) {
						try {
							if ((shown && !notifications) || wasShown != shown) {
								api(wasShown != shown ?
									("updateStatus".concat(!shown ? "&off=1" : "")) : "me");
							}
							wasShown = shown;
						} catch (Exception ignored) {}
					}

//#ifndef NO_NOTIFY
					// get notifications
					if (!globalUpdates) {
						if (!notifications && (!chatsList.isShown()))
							continue;
						if (updatesThread != null && !paused) {
							check = true;
							continue;
						}
					}
					try {
						while (check && user != null) {
							try {
								j = ((JSONObject) api("getLastUpdate")).getObject("res");
								int off = j.getInt("update_id");
								if (!j.getBoolean("exact", false))
									off -= 1;
								if (offset <= 0 || off < offset)
									offset = off;
							} catch (Exception ignored) {
								continue;
							}
							check = false;
							break;
						}
						
						sb.setLength(0);
						if (globalUpdates) {
							sb.append("updates&media=1")
									.append("&offset=").append(offset)
									.append("&timeout=").append(longpoll ? updatesTimeout : 1)
									.append("&limit=200")
									.append("&p=1&m=1")
							;
							if (!longpoll) {
								sb.append("&longpoll=0");
							} else {
								sb.append("&delay=").append(shown ? 1 : 5);
							}

							j = (JSONObject) api(sb.toString());

							if (j.has("cancel")) {
								throw cancelException;
							}

							JSONArray updates = j.getArray("res");
							int l = updates.size();

							offset = updates.getObject(l - 1).getInt("update_id");

							MPChat form = current instanceof MPChat ? (MPChat) current : null;

							if (form != null && form.updating()) {
								String peer = form.id();
								boolean channel = form.channel();
								boolean user = peer.charAt(0) != '-';
								int topMsgId = form.topMsgId();

								for (int i = 0; i < l; ++i) {
									JSONObject update = updates.getObject(i);
									update = update.getObject("update");
									String type = update.getString("_");

									JSONObject msg = update.getObject("message", null);

									if (user && "updateUserStatus".equals(type)) {
										if (!update.getString("user_id").equals(peer)) continue;
										form.handleUpdate(MPChat.UPDATE_USER_STATUS, update);
									} else if (user ? "updateUserTyping".equals(type)
											: channel ? "updateChannelUserTyping".equals(type)
											: "updateChatUserTyping".equals(type)) {
										if (user && !update.getString("user_id").equals(peer)) continue;
										if (channel) {
											if (!peer.equals(update.getString("channel_id"))
													|| (update.has("top_msg_id") && update.getInt("top_msg_id") != topMsgId)) {
												continue;
											}
										} else if (!update.getString("chat_id").equals(peer)) continue;
										form.handleUpdate(MPChat.UPDATE_USER_TYPING, update);
									} else if (channel ? "updateNewChannelMessage".equals(type)
											: "updateNewMessage".equals(type)) {
										if (msg.getInt("id") <= form.firstMsgId()) continue;
										if (user) {
											if ((peer.equals(selfId) && !peer.equals(msg.getString("peer_id", peer)))
													|| !msg.getString("peer_id").equals(peer) &&
													(msg.getBoolean("out", false) || !selfId.equals(msg.getString("peer_id")) || !peer.equals(msg.getString("from_id")))) {
												continue;
											}
										} else if (!peer.equals(msg.getString("peer_id"))
												|| (msg.has("reply") && msg.getObject("reply").getInt("top") != topMsgId)) {
											continue;
										}
										form.handleUpdate(MPChat.UPDATE_NEW_MESSAGE, update);
									} else if (channel && "updateDeleteChannelMessages".equals(type)) {
										if (!peer.equals(update.getString("channel_id"))
												|| (update.has("top_msg_id") && update.getInt("top_msg_id") != topMsgId)) {
											continue;
										}
										form.handleUpdate(MPChat.UPDATE_DELETE_MESSAGES, update);
									} else if (channel ? "updateEditChannelMessage".equals(type)
											: "updateEditMessage".equals(type)) {
										if (user) {
											if ((peer.equals(selfId) && !peer.equals(msg.getString("peer_id", peer)))
													|| !msg.getString("peer_id").equals(peer) &&
													(msg.getBoolean("out", false) || !selfId.equals(msg.getString("peer_id")) || !peer.equals(msg.getString("from_id")))) {
												continue;
											}
										} else if (!peer.equals(msg.getString("peer_id"))
												|| (msg.has("reply") && msg.getObject("reply").getInt("top") != topMsgId)) {
											continue;
										}
										form.handleUpdate(MPChat.UPDATE_EDIT_MESSAGE, update);
									} else if (channel ? "updateReadChannelOutbox".equals(type)
											: "updateReadHistoryOutbox".equals(type)) {
										if ((update.has("peer") && !update.getString("peer").equals(peer))
												|| (update.has("channel_id") && !update.getString("channel_id").equals(peer)))
											continue;
										form.handleUpdate(MPChat.UPDATE_READ_OUTBOX, update);
									} else if (!channel && "updateDeleteMessages".equals(type)) {
										form.handleUpdate(MPChat.UPDATE_DELETE_MESSAGES, update);
									}
								}
							}

							if (notifications) handleNotifications(updates, sb, true);
						} else {
							sb.setLength(0);
							sb.append("notifications")
							.append("&offset=").append(offset)
							.append("&mu=").append(muteUsers ? '1' : '0')
							.append("&mc=").append(muteChats ? '1' : '0')
							.append("&mb=").append(muteBroadcasts ? '1' : '0')
							;
							if (shown) sb.append("&online=1");

							j = (JSONObject) api(sb.toString());
							
							JSONArray updates = j.getArray("res");
							
							offset = j.getInt("offset");
							
							handleNotifications(updates, sb, false);
						}
					} catch (Exception e) {
						e.printStackTrace();
						if (e.toString().indexOf("Interrupted") != -1 || e == cancelException) {
							break;
						}
						check = true;
					}
//#endif
				}
			} catch (Exception ignored) {}
			break;
		}
		case RUN_CANCEL_UPDATES: {
			if (longpoll) {
				try {
					api("cancelUpdates");
				} catch (Exception ignored) {}
				try {
					Thread.sleep(100);
				} catch (Exception e) {
					break;
				}
				if (!closingConnections.contains(param))
					break;
			}
			// continue
		}
		case RUN_CLOSE_CONNECTION: {
			try {
				closingConnections.addElement(param);
				InputStream in = (InputStream) threadConnections.get(param);
				if (in != null) {
					try {
						in.close();
					} catch (Exception ignored) {}
				}
				((Connection) param).close();
			} catch (Exception ignored) {}
			break;
		}
		case RUN_BOT_CALLBACK: {
			MPChat form = (MPChat) current;
			Ticker ticker;
			form.setTicker(ticker = new Ticker(MP.L[LSending]));
			
			try {
				StringBuffer sb = new StringBuffer("sendBotCallback&timeout=1&r=")
				.append(System.currentTimeMillis())
				.append("&peer=").append(((String[]) param)[0])
				.append("&id=").append(((String[]) param)[1])
				.append("&data=").append(((String[]) param)[2])
				;
				
				JSONObject j = null;
				
				try {
					j = (JSONObject) api(sb.toString());
				} catch (APIException e) {
					// treat api errors as proper answers,
					// since it will probably be a timeout error
					if (e.response instanceof JSONObject) {
						j = (JSONObject) e.response;
					} else throw e;
				}

				if ((reopenChat || !longpoll || !form.updating() || !form.endReached()) && !globalUpdates) {
					// see ChatForm#postLoad() for answer handling
					form.setBotAnswer(j);
					commandAction(latestCmd, current);
				} else if (display.getCurrent() != current) {
					display(current);
					((MPChat) current).handleBotAnswer(j);
				}
			} catch (Exception e) {
				display(errorAlert(e), current);
			} finally {
				sending = false;
				if (form.getTicker() == ticker) {
					form.setTicker(null);
				}
			}
			break;
		}
		case RUN_BAN_MEMBER: {
			try {
				String[] s = (String[]) param;
				MP.api("banMember&peer=".concat(s[0].concat("&id=").concat(s[2])));

				display(infoAlert(L[LMemberBanned_Alert]), current);
			} catch (Exception e) {
				display(errorAlert(e), current);
			}
			break;
		}
		case RUN_ZOOM_VIEW: {
			try {
				((ViewCanvas) param).resize();
				((ViewCanvas) param).repaint();
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		}
		case RUN_PIN_MESSAGE: {
			try {
				String[] s = (String[]) param;
				MP.api("pinMessage&peer=".concat(s[0].concat("&id=").concat(s[1])));
				
				if ((reopenChat || !longpoll || !((MPChat) current).updating() || !((MPChat) current).endReached()) && !globalUpdates) {
					// load latest messages
					commandAction(latestCmd, current);
				} else if (display.getCurrent() != current) {
					display(current);
				}
			} catch (Exception e) {
				display(errorAlert(e), current);
			}
			break;
		}
		case RUN_SEND_STICKER: {
			try {
				JSONObject s = (JSONObject) param;
				StickerPackForm form = (StickerPackForm) current;
				
				StringBuffer sb = new StringBuffer("sendMedia&peer=").append(form.chatForm.id());
				sb.append("&doc_id=").append(s.getString("id"))
				.append("&doc_access_hash=").append(s.getString("access_hash"))
				.append("&r=").append(System.currentTimeMillis());
				
				MP.api(sb.toString());
				
				goBackTo((Displayable) form.chatForm);
				if ((reopenChat || !longpoll || !((MPChat) current).updating() || !((MPChat) current).endReached()) && !globalUpdates) {
					// load latest messages
					commandAction(latestCmd, (Displayable) form.chatForm);
				}
			} catch (Exception e) {
				display(errorAlert(e), current);
			} finally {
				sending = false;
			}
			break;
		}
		case RUN_INSTALL_STICKER_SET: {
			try {
				StickerPackForm s = (StickerPackForm) param;
				MP.api("installStickerSet&id=".concat(s.id.concat("&access_hash=").concat(s.accessHash)));
				
//				s.removeCommand(addStickerPackCmd);
				commandAction(backCmd, s);
				display(infoAlert(L[LStickersAdded_Alert]), current);
			} catch (Exception e) {
				display(errorAlert(e), current);
			}
			break;
		}
		case RUN_LOAD_PLAYLIST: {
			int mode;
			{
				String s = ((String[]) param)[1];
				if (s == null) {
					mode = 0;
				} else mode = s.charAt(0) - '0';
			}
			String peer = ((String[]) param)[0];
			if (peer == null) {
				peer = playlistPeer;
			} else {
				playlistPeer = peer;
			}
			try {
				StringBuffer sb = new StringBuffer("searchMessages&filter=Music&media=1");
				sb.append("&peer=").append(peer);
				sb.append("&limit=").append(messagesLimit);
				if (mode == 1 || mode == 4) {
					sb.append("&offset_id=").append(playlist.getObject(playlist.size() - 1).getInt("id"));
				} else if (mode == 2 || mode == 5) {
					sb.append("&min_id=").append(playlist.getObject(0).getInt("id"));
				} else if (mode == 3) {
					sb.append("&offset_id=").append(((String[]) param)[2])
					.append("&add_offset=-1");
				}

				List list = playlistList;
				if (list == null) {
					list = new List(L[LPlaylist_Title], List.IMPLICIT);
					list.addCommand(backCmd);
					list.addCommand(nextPageCmd);
					list.addCommand(prevPageCmd);
					list.addCommand(List.SELECT_COMMAND);
					list.setCommandListener(midlet);
					
					playlistList = list;
				}

				boolean cur = false;
				if (current == list) {
					cur = true;
					display(loadingAlert(null), current);
				}
				
				JSONObject j = (JSONObject) MP.api(sb.toString());
				JSONArray messages = j.getArray("messages");
				int l = messages.size();
				playlistSize = j.getInt("count", -1);
				
				if (mode == 0 || mode == 3) {
					playlist = messages;
					playlistOffset = j.getInt("off", 0);
					playlistIndex = 0;
					currentMusic = playlist.getObject(0);
					if (mode == 3) {
						display(initPlayerForm());
						startPlayer(currentMusic);
					} else {
						display(list);
					}
				} else if (mode == 1 || mode == 4) {
					for (int i = 0; i < l; ++i) {
						playlist.add(messages.getObject(i));
					}
					if (mode == 1) startNextMusic(playlistDirection, playlistIndex);
				} else if (mode == 2 || mode == 5) {
					playlistIndex += l;
					playlistOffset -= l;
					if (playlistOffset < 0) playlistOffset = 0;
					for (int i = l - 1; i >= 0; --i) {
						playlist.put(0, messages.getObject(i));
					}
					if (mode == 2) startNextMusic(!playlistDirection, playlistIndex);
				}
				
				list.deleteAll();
				l = playlist.size();
				String t;
				for (int i = 0; i < l; ++i) {
					JSONObject media = playlist.getObject(i).getObject("media");
					sb.setLength(0);
					if ((t = media.getObject("audio").getString("artist", null)) != null) {
						sb.append(t).append(" - ");
					}
					if ((t = media.getObject("audio").getString("title", null)) != null) {
						sb.append(t);
					} else {
						sb.append(media.getString("name", ""));
					}
					list.append(sb.toString(), null);
				}
				list.setSelectedIndex(playlistIndex, true);

				if (cur) display(list);
			} catch (Exception e) {
				display(errorAlert(e), current);
			}
			break;
		}
		case RUN_PLAYER_LOOP: {
			try {
				while (currentPlayer != null && playerState == 1) {
					playerUpdate(currentPlayer, null, null);
					Thread.sleep(500L);
				}
			} catch (Exception ignored) {}
			break;
		}
//#ifndef NO_FILE
		case RUN_DOWNLOAD_DOCUMENT: {
			downloading = true;
			String downloadPath = (String) param;
			String[] msg = downloadMessage;
			String name;
			
			Alert alert = new Alert(name = msg[2]);
			alert.setString(L[LLoading]);
			alert.setIndicator(new Gauge(null, false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING));
			alert.addCommand(cancelDownloadCmd);
			alert.setCommandListener(this);
			alert.setTimeout(Alert.FOREVER);
			display(alert, current);
			
			if (reopenChat && updatesThread != null) {
				MP.cancel(MP.updatesThread, true);
			}
			
			String error;
			try {
				if (!downloadPath.endsWith("/")) downloadPath = downloadPath.concat("/");
				int size = msg[3] == null ? 0 : Integer.parseInt(msg[3]);
				if (name == null) {
					name = msg[0] + '_' + msg[1];
				}

				downloadDocument(instanceUrl + FILE_URL + "?c=" + msg[0] + "&m=" + msg[1],
						"file:///".concat(downloadPath.concat(name)),
						alert,
						null,
						size,
						true
						);
				return;
			} catch (Exception e) {
				if (e == cancelException) {
					display(alert(null, L[LDownloadCanceled_Alert], AlertType.WARNING), current);
					break;
				}
				// failed to open file
				e.printStackTrace();
				error = e.toString();
			} finally {
				downloading = false;
			}
			
			display(errorAlert(error), current);
			break;
		}
//#endif
		case RUN_LOGOUT: {
			userState = 0;
			user = null;
			phone = null;
			selfId = null;
			display(mainDisplayable = authForm);
			if (needWriteConfig) {
				try {
					writeConfig();
				} catch (Exception ignored) {}
			}
			closePlayer();
			writeAuth();
			break;
		}
		case RUN_START_PLAYER: {
			try {
				JSONObject msg = (JSONObject) param;

				playerTitleLabel.setText(L[LLoading]);
				playerArtistLabel.setText("");
				try {
					playerProgress.setValue(Gauge.CONTINUOUS_RUNNING);
					playerProgress.setMaxValue(Gauge.INDEFINITE);
				} catch (Exception ignored) {}

				StringBuffer url = new StringBuffer(instanceUrl);
				String name;
				if ((name = msg.getObject("media").getString("name", null)) != null && fileRewrite) {
					appendUrl(url.append("file/"), name);
				} else {
					url.append(FILE_URL);
				}
				url.append("?c=").append(msg.getString("peer_id"))
						.append("&m=").append(msg.getInt("id"))
						.append("&user=").append(user);

				JSONObject media = msg.getObject("media");

				if (playlistList != null) {
					try {
						playlistList.setSelectedIndex(playlistIndex, true);
					} catch (Exception ignored) {
					}
				}

				if (loadThumbs && playerCover != null && media.getBoolean("thumb", false)) {
					String q = "min";
					int size;
					if (fullPlayerCover && (size = Math.min(playerForm.getWidth(), playerForm.getHeight()) - 20) > 20) {
						q = "prev&s=".concat(Integer.toString(size));
					}
					MP.queueImage(new String[] { msg.getString("peer_id"), msg.getString("id"), null, "thumbr".concat(q) }, playerCover);
					playerForm.insert(0, playerCover);
				}

				Player p;
//#ifndef NO_FILE
				if (playMethod == 1) { // file
					playerTitleLabel.setText(L[LDownloading]);
					try {
						playerProgress.setValue(0);
						playerProgress.setMaxValue(100);
					} catch (Exception ignored) {}

					String fileUrl = System.getProperty("fileconn.dir.private");
					if (fileUrl == null) {
						fileUrl = System.getProperty("fileconn.dir.music");
					}
					if (fileUrl == null) {
						fileUrl = "file:///C:/";
					}
					String ext = media.getString("name", "");
					int i;
					if ((i = ext.lastIndexOf('.')) != -1) {
						ext = ext.substring(i + 1);
					} else {
						ext = "mp3";
					}
					MP.downloadDocument(url.toString(), fileUrl = fileUrl.concat("temp.".concat(ext)), null, playerProgress, media.getInt("size", 0), false);

					try {
						playerProgress.setValue(Gauge.CONTINUOUS_RUNNING);
						playerProgress.setMaxValue(Gauge.INDEFINITE);
					} catch (Exception ignored) {}

					int method = playerCreateMethod;
					if (method == 0) { // auto
						if (series40) {
							method = 2;
						} else {
							method = 1;
						}
					}
					if (method == 2) { // pass connector stream
						p = Manager.createPlayer(Connector.openInputStream(fileUrl), media.getString("mime", "audio/mpeg"));
					} else { // pass url
						p = Manager.createPlayer(fileUrl);
					}
				} else
//#endif
				{ // stream
					int method = playerCreateMethod;
					if (method == 0) { // auto
						if (series40) {
							try {
								Class.forName("com.sun.mmedia.protocol.CommonDS");
								// s40v1 uses sun impl for media and i/o so it should work fine
								method = 1;
							} catch (Exception e) {
								// s40v2+ breaks http locator parsing
								method = 2;
							}
						} else {
							method = 1;
							if (symbian) {
								String platform = System.getProperty("microedition.platform");
								if (symbianJrt &&
										(platform.indexOf("java_build_version=2.") != -1
												|| platform.indexOf("java_build_version=1.4") != -1)) {
									// emc (s60v5+), supports mp3 streaming
								} else if (checkClass("com.symbian.mmapi.PlayerImpl")) {
									// uiq
								} else {
									// mmf (s60v3.2-)
									method = 2;
								}
							}
						}
					}
					if (method == 2) { // pass connector stream
						p = Manager.createPlayer(openHttpConnection(url.toString()).openInputStream(),
								media.getString("mime", "audio/mpeg"));
					} else { // pass url
						p = Manager.createPlayer(url.toString());
					}
				}

				try {
					playerProgress.setValue(0);
					playerProgress.setMaxValue(100);
				} catch (Exception ignored) {}

				String t;
				if (playerTitleLabel != null) {
					if ((t = media.getObject("audio").getString("title", null)) == null) {
						if ((t = name) == null) {
							t = L[LUnknownTrack];
						}
					}
					playerTitleLabel.setText(t);
				}
				if (playerArtistLabel != null) {
					if ((t = media.getObject("audio").getString("artist", null)) == null) {
						t = "";
					}
					playerArtistLabel.setText(t);
				}

				p.addPlayerListener(midlet);
				currentPlayer = p;

				p.realize();
				try {
					((VolumeControl) p.getControl("VolumeControl")).setLevel(playerVolume);
				} catch (Throwable ignored) {}
				p.prefetch();
				p.start();
				playerState = 1;
			} catch (Exception e) {
				display(errorAlert(e), current);
				closePlayer();
				playerState = 0;
			}
			break;
		}
		case RUN_OPEN_URL: {
			if (((String) param).startsWith("tel:")) {
				browse((String) param);
				break;
			}
			openUrl((String) param, false);
			break;
		}
		}
//		running--;
	}

//#ifndef NO_NOTIFY
	void handleNotifications(JSONArray updates, StringBuffer sb, boolean global) {
		int l = updates.size();
		JSONArray newMsgs = null;

		for (int i = 0; i < l; ++i) {
			JSONObject update = updates.getObject(i);
			if (update.has("update")) update = update.getObject("update");
			String type = update.getString("_");
			if ("updateReadChannelOutbox".equals(type) || "updateReadHistoryOutbox".equals(type)) {
				String peerId = update.getString("peer", update.getString("channel_id", null));
				if (peerId == null) continue;
				
//#ifndef NO_NOKIAUI
				try {
					Notifier.remove(peerId);
				} catch (Throwable ignored) {}
				notificationMessages.remove(peerId);
//#endif
				continue;
			}
			if (!"updateNewMessage".equals(type) && !"updateNewChannelMessage".equals(type))
				continue;

			JSONObject msg = update.getObject("message");

			String peerId = msg.getString("peer_id");
			if (peerId.equals(selfId)) {
				peerId = msg.getString("from_id");
			}
			sb.setLength(0);

			if (msg.getBoolean("out", false)
					|| update.getBoolean("muted", false)
					|| msg.getBoolean("silent", false)
				    || (global && (update.has("left")
					|| (!msg.getBoolean("mentioned", false)
					&& (update.has("mute_until") || update.has("broadcast") ?
					muteBroadcasts : update.has("chat") ? muteChats : muteUsers))))) {
				if (!global || update.has("left")) continue;
				msg.put("muted", true);
			}

			if (newMsgs == null) {
				newMsgs = new JSONArray();
			}

			JSONObject peer = getPeer(peerId, true);
			String text = appendDialog(sb, peer, peerId, msg).toString();

			msg.put("peer_id", peerId);
			msg.put("text", text);

			// filter latest messages
			f: {
				int n = newMsgs.size();
				for (int m = 0; m < n; ++m) {
					JSONObject t = newMsgs.getObject(m);
					if (peerId.equals(t.getString("peer_id"))) {
						newMsgs.set(m, msg);
						break f;
					}
				}
				newMsgs.add(msg);
			}
//#ifndef NO_NOKIAUI
			notificationMessages.put(peerId, msg);
//#endif
		}

		if (newMsgs != null && newMsgs.size() != 0) {
			boolean notified = false;

			l = newMsgs.size();
			for (int i = 0; i < l; ++i) {
				JSONObject msg = newMsgs.getObject(i);
				String text = msg.getString("text");
				String peerId = msg.getString("peer_id");

				sb.setLength(0);
				sb.append(getName(peerId, false));
				int count = msg.getInt("unread", 0);
				if (count != 0) {
					sb.append(" +").append(count);
				}
				String title = sb.toString();
				
				if (globalUpdates && chatsList != null) {
					try {
						synchronized (chatsList.lock) {
							Vector ids = chatsList.ids;
							int idx = ids.indexOf(peerId);
							int newIdx;
							if (idx != -1) {
								ids.removeElementAt(idx);
								chatsList.delete(idx);
								newIdx = Math.min(chatsList.size(), idx < chatsList.pinnedCount ? 0 : chatsList.pinnedCount);
	
								chatsList.insert(null, newIdx, sb.append('\n').append(text).toString(), peerId);
								ids.insertElementAt(peerId, newIdx);
//							} else {
//								newIdx = Math.min(chatsList.size(), chatsList.pinnedCount);
							}
						}
					} catch (Exception ignored) {}
				}

				if (!paused && current instanceof MPChat && current.isShown() && peerId.equals(((MPChat) current).id())) {
//#ifndef NO_NOKIAUI
					try {
						Notifier.remove(peerId);
					} catch (Throwable ignored) {}
					notificationMessages.remove(peerId);
//#endif
					continue;
				}

				if (!notifications || msg.has("muted")) continue;
				if (notifyMethod != 0) {
//#ifndef NO_NOKIAUI
					if (notifyMethod != 1) {
						Image img = null;
						if (!Notifier.has(peerId) && imagesCache.containsKey(peerId)) {
							img = (Image) imagesCache.get(peerId);
						}
						try {
							if (Notifier.post(peerId, title, text, notifyMethod, img) && img == null && notifyAvas) {
								MP.queueAvatar(peerId, peerId);
							}
						} catch (Throwable ignored) {}
					} else
//#endif
					{
						Alert alert = new Alert(title);
						alert.setString(text);
						alert.setTimeout(1500);
						display(alert, null);
					}
				}
				notified = true;
			}

			if (notified && notifySound) {
				try {
					if (notificationPlayer == null) {
						notificationPlayer = Manager.createPlayer(getClass().getResourceAsStream("/msg.mid"), "audio/midi");
						notificationPlayer.realize();
						notificationPlayer.prefetch();
					}
					notificationPlayer.stop();
					try {
						((VolumeControl) notificationPlayer.getControl("VolumeControl")).setLevel(notificationVolume);
					} catch (Throwable ignored) {}
					notificationPlayer.setMediaTime(0);
					notificationPlayer.start();
				} catch (Exception e) {
					AlertType.ALARM.playSound(display);
				}
			}
		}
	}
//#endif

	Thread start(int i, Object param) {
		Thread t = null;
		try {
			synchronized (this) {
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
		if (updates) updatesThread = null;
		Connection c = (Connection) threadConnections.get(thread);
		if (c == null) {
			if (updates && updatesSleeping) {
				thread.interrupt();
			}
			return;
		}
		if (closingConnections.contains(c)) return;
		midlet.start(updates ? RUN_CANCEL_UPDATES : RUN_CLOSE_CONNECTION, c);
	}

	// endregion
	
	// region UI Listeners

	public void commandAction(Command c, Displayable d) {
		if (d instanceof ChatsList) { // chats list commands
			if (c == archiveCmd) {
				((ChatsList) d).changeFolder(1, L[LArchive]);
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
				openLoad(new ChatsList(L[LContacts], "getContacts&fields=status", null, null, false));
				return;
			}
			if (c == nextPageCmd) {
				((ChatsList) d).paginate(1);
				return;
			}
			if (c == prevPageCmd) {
				((ChatsList) d).paginate(-1);
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
				TextBox t = new TextBox(L[LSearch], "", 200, TextField.ANY);
				t.addCommand(cancelCmd);
				t.addCommand(searchChatsCmd);
				t.setCommandListener(this);
				
				display(t);
				return;
			}
			if (c == banMemberCmd) {
				int i = ((List) d).getSelectedIndex();
				if (i == -1) return;
				
				String id = (String) ((ChatsList) d).ids.elementAt(i);
				if (id == null) return;

				display(loadingAlert(L[LLoading]), current);
				start(RUN_BAN_MEMBER, new String[] {((ChatsList) d).peerId, null, id});
				return;
			}
		}
		if (d instanceof MPChat) { // chat form commands
			if (c == latestCmd) {
				((MPChat) d).resetChat();
				start(RUN_LOAD_FORM, d);
				return;
			}
			if (c == olderMessagesCmd || c == newerMessagesCmd) {
				((MPChat) d).paginate(c == olderMessagesCmd ? -1 : 1);
				return;
			}
			if (c == chatInfoCmd) {
				openProfile(((MPChat) d).id(), (MPChat) d, 0);
				return;
			}
			if (c == writeCmd) {
				int r = ((MPChat) d).topMsgId();
				display(writeForm(((MPChat) d).id(), r == 0 ? null : Integer.toString(r), "", null, null, null));
				return;
			}
			if (c == searchMsgCmd) {
				TextBox t = new TextBox(L[LSearch], "", 200, TextField.ANY);
				t.addCommand(cancelCmd);
				t.addCommand(searchMsgCmd);
				t.setCommandListener(this);
				
				display(t);
				return;
			}
			if (c == sendStickerCmd) {
				openLoad(new StickerPacksList((MPChat) d));
				return;
			}
			if (c == backCmd && ((MPChat) d).query() != null && ((MPChat) d).switched()) {
				// close search
				((MPChat) current).resetChat();
				start(RUN_LOAD_FORM, current);
				return;
			}
		}
		if (d instanceof TextBox && c == searchMsgCmd) {
			commandAction(backCmd, d);
			if (current instanceof MPChat) {
				((MPChat) current).resetChat();
				((MPChat) current).setQuery(((TextBox) d).getString());
				start(RUN_LOAD_FORM, current);
				return;
			}
			
			MPChat form;
			
//#ifndef NO_CHAT_CANVAS
			if (!legacyChatUI) {
				form = new ChatCanvas(((ChatInfoForm) current).id, ((TextBox) d).getString(), 0, ((ChatInfoForm) current).chatForm.topMsgId());
			} else
//#endif
			{
				form = new ChatForm(((ChatInfoForm) current).id, ((TextBox) d).getString(), 0, ((ChatInfoForm) current).chatForm.topMsgId());
			}
			
			form.setParent(((ChatInfoForm) current).chatForm);
			openLoad((Displayable) form);
			return;
		}
		if (d instanceof TextBox && c == searchChatsCmd) {
			commandAction(backCmd, d);
			openLoad(new ChatsList(L[LSearch],
					appendUrl(new StringBuffer("searchChats&q="), ((TextBox) d).getString()).toString(),
					"results", null, false));
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
				TextBox t = new TextBox(L[LSearch], "", 200, TextField.ANY);
				t.addCommand(cancelCmd);
				t.addCommand(searchMsgCmd);
				t.setCommandListener(this);
				
				display(t);
				return;
			}
			// Chat media categories
			if (c == chatPhotosCmd || c == chatVideosCmd || c == chatFilesCmd || c == chatMusicCmd || c == chatVoiceCmd) {
				String mediaFilter;
				switch (c.getPriority()) {
				case 1:
					mediaFilter = "Photos";
					break;
				case 2:
					mediaFilter = "Video";
					break;
				case 3:
					mediaFilter = "Document";
					break;
				case 4:
					mediaFilter = "Music";
					break;
				case 5:
					mediaFilter = "Voice";
					break;
				default:
					return;
				}
				MPChat form;
//#ifndef NO_CHAT_CANVAS
				if (!legacyChatUI) {
					form = new ChatCanvas(((ChatInfoForm) current).id, mediaFilter, ((ChatInfoForm) current).chatForm.topMsgId());
				} else
//#endif
				{
					form = new ChatForm(((ChatInfoForm) current).id, mediaFilter, ((ChatInfoForm) current).chatForm.topMsgId());
				}
				form.setParent(((ChatInfoForm) current).chatForm);
				openLoad((Displayable) form);
				return;
			}
			if (c == gotoPinnedMsgCmd) {
				int id = ((ChatInfoForm) current).pinnedMessageId;
				if (((ChatInfoForm) d).chatForm != null) {
					commandAction(backCmd, d);
					((ChatInfoForm) d).chatForm.openMessage(Integer.toString(id), -1);
				} else {
					openChat(((ChatInfoForm) current).id, id);
				}
				return;
			}
			if (c == chatMembersCmd) {
				openLoad(new ChatsList(L[LMembers],
						"getParticipants&peer=" + ((ChatInfoForm) current).id + "&fields=status",
						null,
						((ChatInfoForm) d).id, ((ChatInfoForm) d).canBan));
				return;
			}
		}
		if (d instanceof ChatTopicsList) {
			if (c == chatInfoCmd) {
				MPChat f = ((ChatTopicsList) d).chatForm;
				openProfile(f.id(), /* f */ null, 0);
				return;
			}
		}
		{ // auth commands
			if (c == authCmd) {
				if (d instanceof TextBox) {
					// user code
					user = ((TextBox) d).getString().trim();
					if (user.length() < 32) {
						display(errorAlert(L[LInvalidSessionCode_Alert]), null);
						return;
					}
					writeAuth();
					
					display(loadingAlert(L[LWaitingForServerResponse]), null);
					start(RUN_VALIDATE_AUTH, user);
					return;
				}
//				instanceUrl = instanceField.getString();
//				if ((instancePassword = instancePasswordField.getString()).length() == 0) {
//					instancePassword = null;
//				}
//				
//				if (instanceUrl == null || instanceUrl.length() < 6 || !instanceUrl.startsWith("http")) {
//					display(errorAlert(L[LInvalidInstance_Alert]), null);
//					return;
//				}
//				writeAuth();
//				
//				Alert a = new Alert("", L[LChooseAuthMethod], null, null);
//				a.addCommand(authImportSessionCmd);
//				a.addCommand(authNewSessionCmd);
//				a.setCommandListener(this);
//				
//				display(a, null);
//				return;
			}
			if (c == authImportSessionCmd || c == authNewSessionCmd) {
				// set instance password
				if ((instancePassword = instancePasswordField.getString()).length() == 0) {
					instancePassword = null;
				}
				
				// set and check instance url
				if ((instanceUrl = instanceField.getString()) == null
						|| instanceUrl.length() < 6 || !instanceUrl.startsWith("http")) {
					display(errorAlert(L[LInvalidInstance_Alert]), null);
					return;
				}
				if (!instanceUrl.endsWith("/")) {
					instanceUrl = instanceUrl.concat("/");
				}
				writeAuth();
				
				if (c == authImportSessionCmd) {
					// import session
					TextBox t = new TextBox(L[LSessionCode], user == null ? "" : user, 200, TextField.NON_PREDICTIVE);
					t.addCommand(cancelCmd);
					t.addCommand(authCmd);
					t.setCommandListener(this);
					
					display(t);
					return;
				}
				
				// new session
				user = null;
				userState = 0;
				
				TextBox t = new TextBox(L[LPhoneNumber], phone == null ? "" : phone, 30, TextField.PHONENUMBER);
				t.addCommand(cancelCmd);
				t.addCommand(authNextCmd);
				t.setCommandListener(this);
				
				display(t);
				return;
			}
			if (c == authNextCmd) { // continue auth
				if (d instanceof TextBox) {
					// phone number entered
					phone = ((TextBox) d).getString();
					if (phone.length() < 10 && !phone.startsWith("+")) {
						display(errorAlert(L[LInvalidPhoneNumber_Alert]), null);
						return;
					}
					writeAuth();

					display(loadingAlert(L[LWaitingForServerResponse]), null);
					openLoad(new CaptchaForm());
					return;
				}
				if (d instanceof CaptchaForm) {
					// captcha entered
					String key = ((CaptchaForm) d).field.getString();
					if (key.length() < 4) {
						display(errorAlert(L[LInvalidCaptcha_Alert]), null);
						return;
					}
					display(loadingAlert(L[LWaitingForServerResponse]), null);
					start(RUN_AUTH, d);
					return;
				}
				return;
			}
			if (c == authCodeCmd) {
				// code entered
				String code = ((TextBox) d).getString();
				if (code.length() < 5) {
					display(errorAlert(L[LInvalidCode_Alert]), null);
					return;
				}

				display(loadingAlert(L[LWaitingForServerResponse]), null);
				start(RUN_AUTH, code);
				return;
			}
			if (c == authPasswordCmd) {
				// password entered
				String pass = ((TextBox) d).getString();
				if (pass.length() == 0) {
					display(errorAlert(L[LCloudPasswordEmpty_Alert]), null);
					return;
				}

				display(loadingAlert(L[LWaitingForServerResponse]), null);
				start(RUN_AUTH, pass);
				return;
			}
			if (c == authRetryCmd) {
				start(RUN_VALIDATE_AUTH, mainDisplayable);
				return;
			}
		}
		{ // settings
			if (c == settingsCmd) {
				if (settingsForm == null) {
					Form f = new Form(L[LSettings]);
					f.addCommand(backCmd);
					f.setCommandListener(this);
//					f.setItemStateListener(this);
					StringItem s;
					int i;
					
					// ui
					
					s = new StringItem(null, L[LUI]);
					s.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					s.setFont(largePlainFont);
					f.append(s);
					
					langChoice = new ChoiceGroup(L[LLanguage], Choice.POPUP, LANGS[1], null);
					for (i = 0; i < LANGS[0].length; ++i) {
						if (lang.equals(LANGS[0][i])) {
							langChoice.setSelectedIndex(i, true);
							break;
						}
					}
					langChoice.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(langChoice);
					
					uiChoice = new ChoiceGroup("", Choice.MULTIPLE, new String[] {
							L[LReversedChat],
							L[LShowMedia],
							L[LShowChatStatus],
							L[LFocusNewMessages],
							L[LChatTextField],
							L[LBuiltinImageViewer],
							L[LLargeMusicCover],
//#ifndef NO_CHAT_CANVAS
							L[LLegacyUI],
							L[LFastScrolling],
//#endif
					}, null);
					uiChoice.setSelectedIndex(i = 0, reverseChat);
					uiChoice.setSelectedIndex(++i, showMedia);
					uiChoice.setSelectedIndex(++i, chatStatus);
					uiChoice.setSelectedIndex(++i, focusNewMessages);
					uiChoice.setSelectedIndex(++i, chatField);
					uiChoice.setSelectedIndex(++i, useView);
					uiChoice.setSelectedIndex(++i, fullPlayerCover);
//#ifndef NO_CHAT_CANVAS
					uiChoice.setSelectedIndex(++i, legacyChatUI);
					uiChoice.setSelectedIndex(++i, fastScrolling);
//#endif
					uiChoice.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(uiChoice);
					
//#ifndef NO_CHAT_CANVAS
					themeChoice = new ChoiceGroup(L[LTheme], Choice.POPUP, THEMES[1], null);
					for (i = 0; i < THEMES[0].length; ++i) {
						if (theme.equals(THEMES[0][i])) {
							themeChoice.setSelectedIndex(i, true);
							break;
						}
					}
					themeChoice.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(themeChoice);
//#endif
					
					photoSizeGauge = new Gauge(L[LThumbnailsSize], true, 64, Math.min(64, photoSize / 8));
					photoSizeGauge.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(photoSizeGauge);
					
					chatsGauge = new Gauge(L[LChatsCount], true, 50, chatsLimit);
					chatsGauge.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(chatsGauge);
					
					msgsGauge = new Gauge(L[LMessagesCount], true, 50, messagesLimit);
					msgsGauge.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(msgsGauge);
					
					chatsFontSizeCoice = new ChoiceGroup(L[LChatsListFontSize], Choice.POPUP, new String[] {
						L[LDefault],
						L[LSmall],
						L[LMedium]
					}, null);
					chatsFontSizeCoice.setSelectedIndex(chatsListFontSize, true);
					f.append(chatsFontSizeCoice);
					
//#ifndef NO_CHAT_CANVAS
					textMethodChoice = new ChoiceGroup(L[LKeyboard], Choice.POPUP, new String[] {
							L[LAuto],
							"Nokia UI",
							"j2mekeyboard",
							L[LFullscreenTextBox]
					}, null);
					textMethodChoice.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					textMethodChoice.setSelectedIndex(textMethod, true);
					f.append(textMethodChoice);
					
					s = new StringItem(null, L[LInputLanguages], Item.BUTTON);
					s.setDefaultCommand(keyboardLanguagesCmd);
					s.setItemCommandListener(this);
					s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(s);
//#endif
					
//#ifndef NO_NOTIFY
					// notifications
					
					s = new StringItem(null, L[LNotifications]);
					s.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					s.setFont(largePlainFont);
					f.append(s);
					
					notifyChoice = new ChoiceGroup("", ChoiceGroup.MULTIPLE, new String[] {
							L[LEnableNotifications],
							L[LEnableSound]
					}, null);
					notifyChoice.setSelectedIndex(0, notifications);
					notifyChoice.setSelectedIndex(1, notifySound);
					notifyChoice.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(notifyChoice);
					
					notifyMethodChoice = new ChoiceGroup(L[LNotificationMethod], ChoiceGroup.POPUP, new String[] {
							L[LOff],
							L[LAlertWindow],
//#ifndef NO_NOKIAUI
							"Nokia UI",
							"Pigler API"
//#endif
					}, null);
					notifyMethodChoice.setSelectedIndex(Math.min(notifyMethod, notifyMethodChoice.size() - 1), true);
					notifyMethodChoice.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(notifyMethodChoice);
					
					notificationVolumeGauge = new Gauge(L[LVolume], true, 100, notificationVolume);
					notificationVolumeGauge.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(notificationVolumeGauge);
					
					pushIntervalGauge = new Gauge(L[LPushInterval], true, 120, (int) (pushInterval / 1000));
					pushIntervalGauge.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(pushIntervalGauge);
					
					pushBgIntervalGauge = new Gauge(L[LPushBackgroundInterval], true, 120, (int) (pushBgInterval / 1000));
					pushBgIntervalGauge.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(pushBgIntervalGauge);
//#endif
					
					// behaviour
					
					s = new StringItem(null, L[LBehaviour]);
					s.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					s.setFont(largePlainFont);
					f.append(s);
					
					if (blackberry) {
						networkChoice = new ChoiceGroup(L[LNetworkAccess], Choice.POPUP, new String[] {
								L[LMobileData],
								L[LWiFi]
						}, null);
						networkChoice.setSelectedIndex(blackberryNetwork == -1 ? 0 : blackberryNetwork, true);
						networkChoice.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
						f.append(networkChoice);
					}
					
					behChoice = new ChoiceGroup("", Choice.MULTIPLE, new String[] {
							L[LWaitForPageToLoad],
							L[LUseJSONStream],
							L[LFormatText],
							L[LParseLinks],
							L[LChatAutoUpdate],
							L[LKeepSessionAlive],
							L[LUseUnicode],
							L[LLongpoll],
//#ifndef NO_ZIP
							L[LUseCompression],
//#endif
//#ifndef NO_FILE
							L[LPartialUpload],
//#endif
					}, null);
					behChoice.setSelectedIndex(i = 0, useLoadingForm);
					behChoice.setSelectedIndex(++i, jsonStream);
					behChoice.setSelectedIndex(++i, parseRichtext);
					behChoice.setSelectedIndex(++i, parseLinks);
					behChoice.setSelectedIndex(++i, chatUpdates);
					behChoice.setSelectedIndex(++i, keepAlive);
					behChoice.setSelectedIndex(++i, utf);
					behChoice.setSelectedIndex(++i, longpoll);
//#ifndef NO_ZIP
					behChoice.setSelectedIndex(++i, compress);
//#endif
//#ifndef NO_FILE
					behChoice.setSelectedIndex(++i, chunkedUpload);
//#endif
					behChoice.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(behChoice);
					
					updateTimeoutGauge = new Gauge(L[longpoll ? LUpdatesTimeout : LUpdatesInterval], true, 20, updatesTimeout / 5);
					updateTimeoutGauge.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(updateTimeoutGauge);
					
					// images
					
					imagesChoice = new ChoiceGroup(L[LImages], Choice.MULTIPLE, new String[] {
							L[LLoadMediaThumbnails],
//#ifndef NO_AVATARS
							L[LLoadAvatars],
							L[LRoundAvatars],
//#endif
							L[LMultiThreadedLoading],
//#ifndef NO_CHAT_CANVAS
							L[LLazyLoading],
//#endif
					}, null);
					imagesChoice.setSelectedIndex(i = 0, loadThumbs);
//#ifndef NO_AVATARS
					imagesChoice.setSelectedIndex(++i, loadAvatars);
					imagesChoice.setSelectedIndex(++i, roundAvatars);
//#endif
					imagesChoice.setSelectedIndex(++i, threadedImages);
//#ifndef NO_CHAT_CANVAS
					imagesChoice.setSelectedIndex(++i, lazyLoading);
//#endif
					imagesChoice.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(imagesChoice);
					
					// cache

//#ifndef NO_AVATARS
					avaCacheChoice = new ChoiceGroup(L[LAvatarsCaching], Choice.POPUP, new String[] {
							L[LDisabled],
							L[LHoldInRAM],
							L[LStore],
							L[LHoldInRAMandStore]
					}, null);
					avaCacheChoice.setSelectedIndex(avatarsCache, true);
					avaCacheChoice.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(avaCacheChoice);
					
					avaCacheGauge = new Gauge(L[LAvatarsCacheThreshold], true, 20, avatarsCacheThreshold / 5);
					avaCacheGauge.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(avaCacheGauge);
//#endif
					
					profileCacheGauge = new Gauge(L[LProfilesCacheThreshold], true, 30, peersCacheThreshold / 10);
					profileCacheGauge.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(profileCacheGauge);
					
					s = new StringItem(null, L[LClearCache], Item.BUTTON);
					s.setDefaultCommand(clearCacheCmd);
					s.setItemCommandListener(this);
					s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(s);
					
					// player
//#ifndef NO_FILE
					playMethodChoice = new ChoiceGroup(L[LPlaybackMethod], Choice.POPUP, new String[] {
							L[LStream],
							L[LCacheToFile]
					}, null);
					playMethodChoice.setSelectedIndex(playMethod, true);
					playMethodChoice.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(playMethodChoice);
//#endif

					playerCreateMethodChoice = new ChoiceGroup(L[LPlayerCreationMethod], Choice.POPUP, new String[] {
							L[LAuto],
							L[LPassURL],
							L[LPassConnectionStream]
					}, null);
					playerCreateMethodChoice.setSelectedIndex(playerCreateMethod, true);
					playerCreateMethodChoice.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(playerCreateMethodChoice);
					
//#ifndef NO_FILE
					// downloads
					
					downloadMethodChoice = new ChoiceGroup(L[LDownloadMethod], Choice.POPUP, new String[] {
							L[LAlwaysAsk],
							L[LInApp],
							L[LWithBrowser]
					}, null);
					downloadMethodChoice.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					downloadMethodChoice.setSelectedIndex(downloadMethod, true);
					f.append(downloadMethodChoice);
					
					downloadPathField = new TextField(L[LDownloadPath], downloadPath, 500, TextField.ANY);
					downloadPathField.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(downloadPathField);
					
					s = new StringItem(null, "...", Item.BUTTON);
					s.setDefaultCommand(downloadPathCmd);
					s.setItemCommandListener(this);
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(s);
//#endif
					
					// authorization
					
					s = new StringItem(null, L[LAuthorization]);
					s.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					s.setFont(largePlainFont);
					f.append(s);
					
					s = new StringItem(null, L[LShowSessionCode], Item.BUTTON);
					s.setDefaultCommand(exportSessionCmd);
					s.setItemCommandListener(this);
					s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(s);
					
					s = new StringItem(null, L[LLogout], Item.BUTTON);
					s.setDefaultCommand(logoutCmd);
					s.setItemCommandListener(this);
					s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(s);
					
					settingsForm = f;
				} else {
					updateTimeoutGauge.setLabel(L[longpoll ? LUpdatesTimeout : LUpdatesInterval]);
				}
				
				display(settingsForm);
				if (blackberry && blackberryNetwork == -1) {
					try {
						display.setCurrentItem(networkChoice);
					} catch (Exception ignored) {}
				}
				return;
			}
			if (c == backCmd && d == settingsForm) {
				// apply and save settings
				try {
					int i;
					lang = LANGS[0][Math.max(0, langChoice.getSelectedIndex())];
					
					reverseChat = uiChoice.isSelected(i = 0);
					showMedia = uiChoice.isSelected(++i);
					chatStatus = uiChoice.isSelected(++i);
					focusNewMessages = uiChoice.isSelected(++i);
					chatField = uiChoice.isSelected(++i);
					useView = uiChoice.isSelected(++i);
					fullPlayerCover = uiChoice.isSelected(++i);
//#ifndef NO_CHAT_CANVAS
					legacyChatUI = uiChoice.isSelected(++i);
					fastScrolling = uiChoice.isSelected(++i);
					
					String prevTheme = theme;
					theme = THEMES[0][Math.max(0, themeChoice.getSelectedIndex())];
					if (!theme.equals(prevTheme)) {
						ChatCanvas.colorsCopy = null;
					}
//#endif
				
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
					
//#ifndef NO_CHAT_CANVAS
					textMethod = textMethodChoice.getSelectedIndex();
//#endif
				
//#ifndef NO_NOTIFY
					notifications = notifyChoice.isSelected(0);
					notifySound = notifyChoice.isSelected(1);
					
					notifyMethod = notifyMethodChoice.getSelectedIndex();
					
					notificationVolume = notificationVolumeGauge.getValue();
					
					if ((pushInterval = pushIntervalGauge.getValue() * 1000L) < 5000L) {
						pushIntervalGauge.setValue((int) ((pushInterval = 5000) / 1000L));
					}
					if ((pushBgInterval = pushBgIntervalGauge.getValue() * 1000L) < 5000L) {
						pushBgIntervalGauge.setValue((int) ((pushBgInterval = 5000) / 1000L));
					}
//#endif
				
					if (networkChoice != null)
						blackberryNetwork = networkChoice.getSelectedIndex();
					
					useLoadingForm = behChoice.isSelected(i = 0);
					jsonStream = behChoice.isSelected(++i);
					parseRichtext = behChoice.isSelected(++i);
					parseLinks = behChoice.isSelected(++i);
					chatUpdates = behChoice.isSelected(++i);
					keepAlive = behChoice.isSelected(++i);
					utf = behChoice.isSelected(++i);
					longpoll = behChoice.isSelected(++i);
//#ifndef NO_ZIP
					compress = behChoice.isSelected(++i);
//#endif
//#ifndef NO_FILE
					chunkedUpload = behChoice.isSelected(++i);
//#endif
					
					if ((updatesTimeout = updateTimeoutGauge.getValue() * 5) < 5) {
						updateTimeoutGauge.setValue((updatesTimeout = 5) / 5);
					}
					
					loadThumbs = imagesChoice.isSelected(i = 0);
//#ifndef NO_AVATARS
					loadAvatars = imagesChoice.isSelected(++i);
					roundAvatars = imagesChoice.isSelected(++i);
//#endif
					threadedImages = imagesChoice.isSelected(++i);
//#ifndef NO_CHAT_CANVAS
					lazyLoading = imagesChoice.isSelected(++i);
//#endif
				
//#ifndef NO_AVATARS
					avatarsCache = avaCacheChoice.getSelectedIndex();
					avatarsCacheThreshold = avaCacheGauge.getValue() * 5;
//#endif
					peersCacheThreshold = profileCacheGauge.getValue() * 10;

//#ifndef NO_FILE
					playMethod = playMethodChoice.getSelectedIndex();
//#endif
					playerCreateMethod = playerCreateMethodChoice.getSelectedIndex();

//#ifndef NO_FILE
					downloadMethod = downloadMethodChoice.getSelectedIndex();
					downloadPath = downloadPathField.getString();
//#endif

					writeConfig();
				} catch (Exception e) {
					e.printStackTrace();
					display(errorAlert(L[LFailedToSaveSettings_Alert] + ": " + e));
					return;
				}
//#ifndef NO_NOTIFY
//#ifndef NO_NOKIAUI
			if (notifyMethod == 3 && symbianJrt && !checkClass("org.pigler.api.PiglerAPI")) {
				display(alert(null, L[LPiglerNotAvailable_Alert], AlertType.WARNING), mainDisplayable);
				return;
			}
//#endif
//#endif
			}
			if (c == logoutCmd) {
				if (userState == 0) return;
				
				MP.confirm(RUN_LOGOUT,
						null,
						null,
						MP.L[MP.LLogout_Alert]);
				return;
			}
			if (c == clearCacheCmd) {
				try {
					// clear avatars in storage
					String[] s = RecordStore.listRecordStores();
					for (int i = 0; i < s.length; ++i) {
						if (s[i].startsWith(AVATAR_RECORD_PREFIX)) {
							try {
								RecordStore.deleteRecordStore(s[i]);
							} catch (Exception ignored) {}
						}
					}
				} catch (Exception ignored) {}
				// clear hashtables
				usersCache.clear();
				chatsCache.clear();
				imagesCache.clear();
				imagesToLoad.removeAllElements();
				commandAction(backCmd, d);
				return;
			}
//#ifndef NO_FILE
			if (c == downloadPathCmd) {
				downloadMessage = null;
				try {
					openFilePicker(downloadPath, false);
				} catch (Throwable ignored) {}
				return;
			}
//#endif
//#ifndef NO_CHAT_CANVAS
			if (c == keyboardLanguagesCmd) {
				List l = new List(L[LInputLanguages], List.MULTIPLE);
				l.addCommand(saveLanguagesCmd);
				l.setCommandListener(this);
				
				String[] langs = Keyboard.getSupportedLanguages();
				
				for (int i = 0; i < langs.length; ++i) {
					String s = langs[i];
					String lang = s.substring(s.lastIndexOf('[')+1,s.lastIndexOf(']'));
					l.append(s, null);
					
					for (int j = 0; j < inputLanguages.length; j++) {
						if (inputLanguages[j] == null) break;
						if (inputLanguages[j].equals(lang)) {
							l.setSelectedIndex(i, true);
							break;
						}
					}
				}
				
				display(l);
				return;
			}
			if (c == saveLanguagesCmd) {
				List l = (List) d;
				boolean[] selected = new boolean[l.size()];
				
				inputLanguages = new String[l.getSelectedFlags(selected)];
				int k = 0;
				for (int i = 0; i < selected.length; ++i) {
					if (selected[i]) {
						String s = l.getString(i);
						inputLanguages[k++] = s.substring(s.lastIndexOf('[')+1, s.lastIndexOf(']'));
					}
				}
				
				c = backCmd;
			}
			if (c == exportSessionCmd) {
				copy("", user);
				return;
			}
//#endif
		}
		{ // write form commands
			if (c == sendCmd) {
				String t = messageField.getString();
				if (t.trim().length() == 0 && sendFile == null && fwdPeer == null) {
					return;
				}

				synchronized (this) {
					if (sending) return;
					sending = true;
				}

				display(loadingAlert(L[LSending]), d);
				if ((reopenChat || !longpoll || sendFile != null) && MP.updatesThread != null) {
					MP.cancel(MP.updatesThread, true);
				}
				start(RUN_SEND_MESSAGE, new Object[] { t, writeTo, replyTo, edit, sendFile, sendChoice, fwdPeer, fwdMsg });
				return;
			}
			if (c == openTextBoxCmd) {
				TextBox t = new TextBox(L[LMessage], messageField.getString(), 500, TextField.ANY);
				t.addCommand(okCmd);
				t.addCommand(cancelCmd);
				t.setCommandListener(this);
				display(t);
				return;
			}
//#ifndef NO_FILE
			if (c == chooseFileCmd) {
				try {
					openFilePicker(lastUploadPath, true);
				} catch (Throwable ignored) {}
				return;
			}
//#endif
			if (c == okCmd) {
				// full texbox finished
//#ifndef NO_CHAT_CANVAS
				if (!legacyChatUI) {
					commandAction(backCmd, d);
					String s = ((ChatCanvas) current).text = ((TextBox) d).getString();
					if (ChatCanvas.keyboard != null) {
						ChatCanvas.keyboard.setText(s);
					}
					((ChatCanvas) current).queueRepaint();
					return;
				}
//#endif
				
				messageField.setString(((TextBox) d).getString());
				c = backCmd;
			}
		}
		// stickers
		if (c == addStickerPackCmd) {
			display(loadingAlert(L[LLoading]), current);
			start(RUN_INSTALL_STICKER_SET, d);
			return;
		}
		if (c == aboutCmd) {
			// about form
			Form f = new Form(L[LAbout]);
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
			
			s = new StringItem(null, L[LAboutText].concat("\n"));
			s.setFont(Font.getDefaultFont());
			s.setLayout(Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			f.append(s);
			
			f.append(new Spacer(2, 2));

			s = new StringItem(null, L[LDeveloper].concat("\n"));
			s.setFont(smallPlainFont);
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			f.append(s);

			s = new StringItem(null, "shinovon\n");
			s.setFont(medBoldFont);
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			f.append(s);
			
			f.append(new Spacer(2, 2));

			s = new StringItem(null, L[LAuthor].concat("\n"));
			s.setFont(smallPlainFont);
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			f.append(s);

			s = new StringItem(null, "twsparkle\n");
			s.setFont(medBoldFont);
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			s.setItemCommandListener(this);
			f.append(s);
			
			f.append(new Spacer(2, 2));

			s = new StringItem(null, "GitHub\n");
			s.setFont(smallPlainFont);
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			f.append(s);

			s = new StringItem(null, "github.com/shinovon/mpgram-client\n");
			s.setFont(medBoldFont);
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			s.setDefaultCommand(richTextLinkCmd);
			s.setItemCommandListener(this);
			f.append(s);
			
			f.append(new Spacer(2, 2));

			s = new StringItem(null, "Web\n");
			s.setFont(smallPlainFont);
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			f.append(s);

			s = new StringItem(null, "nnproject.cc\n");
			s.setFont(medBoldFont);
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			s.setDefaultCommand(richTextLinkCmd);
			s.setItemCommandListener(this);
			f.append(s);
			
			f.append(new Spacer(2, 2));

			s = new StringItem(null, MP.L[LDonate].concat("\n"));
			s.setFont(smallPlainFont);
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			f.append(s);

			s = new StringItem(null, "boosty.to/nnproject/donate\n");
			s.setFont(medBoldFont);
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			s.setDefaultCommand(richTextLinkCmd);
			s.setItemCommandListener(this);
			f.append(s);
			
			f.append(new Spacer(2, 2));

			s = new StringItem(null, MP.L[LChat].concat("\n"));
			s.setFont(smallPlainFont);
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			f.append(s);

			s = new StringItem(null, "t.me/nnmidletschat\n");
			s.setFont(medBoldFont);
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			s.setDefaultCommand(richTextLinkCmd);
			s.setItemCommandListener(this);
			f.append(s);

			s = new StringItem(null, "Community translations:\n\n"
					+ "Arabic: ZAIOOD999, nourhan5908\n"
					+ "Azerbaijani: Red Fixrai\n"
					+ "Catalan: Dragan232\n"
					+ "Spanish: Jazmin Rocio\n"
					+ "Finnish: gtrxAC\n"
					+ "Portuguese: kefelili\n"
					+ "Ukrainian: karusel33, PhantomHorror\n");
			s.setFont(smallPlainFont);
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			f.append(s);

			s = new StringItem(null, "\n\nReleased under the MIT license.\n"
					+ "Copyright (C) 2022-2025 Arman Jussupgaliyev\n\n"
//#ifndef NO_AVATARS
					+ "Contains parts of the TUBE42 imagelib, licensed under the LGPL.\n"
					+ "Copyright (C) 2007 Free Software Foundation, Inc.\n\n"
//#endif
//#ifndef NO_ZIP
					+ "Contains parts of GNU Classpath, licensed under the GPL v2\n"
					+ "Copyright (C) 1999-2004 Free Software Foundation, Inc."
//#endif
					);
			s.setFont(smallPlainFont);
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			f.append(s);
			
			display(f);
			return;
		}
		if (c == List.SELECT_COMMAND) {
			if (d instanceof MPList) {
				((MPList) d).select(((List) d).getSelectedIndex());
				return;
			}
			if (d == playlistList) {
				if (playerState == 3) return;
				startPlayer(currentMusic = playlist.getObject(playlistIndex = ((List) d).getSelectedIndex()));
				return;
			}
			if (d instanceof ChatTopicsList) {
				MPChat form = ((ChatTopicsList) d).chatForm;
				int i = ((List) d).getSelectedIndex();
				if (form == null || i == -1) return;
				JSONObject topic = form.topics().getObject(i);
				form.resetChat();
				// TODO unread offset
				form.openTopic(topic.getInt("id"),
						!topic.getBoolean("closed", false),
						topic.getString("title", ""));
				openLoad((Displayable) form);
				
				return;
			}
//#ifndef NO_FILE
			// file picker
			int i = ((List) d).getSelectedIndex();
			if (i == -1) return;
			boolean dir = ((List) d).getImage(i) == folderImg;
			String name = ((List) d).getString(i);
			String path = d.getTitle();
			
			if ("/".equals(path)) {
				path = "";
			} else if (L[LBack].equals(name) && i == 0) {
				path = path.substring(0, path.lastIndexOf('/', path.lastIndexOf('/') - 1) + 1);

				commandAction(backCmd, d);
				openFilePicker(path, fileMode);
				return;
			}
			
			if (dir) {
				openFilePicker(path.concat(name).concat("/"), fileMode);
				return;
			}
			
			if (fileMode) {
				// file selected
				lastUploadPath = path;
				path = path.concat(name);
				commandAction(cancelCmd, d);
				sendFile = "file:///".concat(path);
				fileLabel.setText(L[LFile_Prefix].concat(path));
			} else {
				// folder selected
				if (downloadMessage == null) {
					// default download path selected in settings
					downloadPathField.setString(lastDownloadPath = path);
					goBackTo(settingsForm);
				} else if (!downloading) {
					// download
					start(RUN_DOWNLOAD_DOCUMENT, downloadCurrentPath = lastDownloadPath = path);
				}
			}
			needWriteConfig = true;
//#endif
			return;
		}
//#ifndef NO_FILE
		if (c == cancelCmd && (d instanceof List) && !(d instanceof ChatsList)) {
			if (fileMode) {
				// go back to write form from file picker
				goBackTo(writeForm);
			} else if (downloadMessage == null) {
				goBackTo(settingsForm);
			} else {
				goBackToChat();
			}
			return;
		}
//#endif
		if (d == playlistList) {
			if (c == nextPageCmd) {
				if (playlist.size() != playlistSize) {
					start(RUN_LOAD_PLAYLIST, new String[] { null, "4" });
				}
				return;
			}
			if (c == prevPageCmd) {
				if (playlistOffset != 0) {
					start(RUN_LOAD_PLAYLIST, new String[] { null, "5" });
				}
				return;
			}
		}
		if (c == goCmd) { // url dialog submit
			commandAction(backCmd, d);
			
			openUrl(((TextBox) d).getString(), false);
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
//#ifndef NO_CHAT_CANVAS
			if (d instanceof ChatCanvas) {
				((ChatCanvas) d).cancel();
				start(RUN_LOAD_FORM, d);
				return;
			}
//#endif
			return;
		}
		if (c == updateCmd) {
			browse(updateUrl);
			return;
		}
		if (c == cancelCmd && d == writeForm && fwdPeer != null) {
			// cancel forwarding
			commandAction(backCmd, d);
			commandAction(backCmd, current);
			return;
		}
		{ // Playlist commands
			if (c == playlistPlayCmd) {
				if (currentPlayer != null) {
					try {
						currentPlayer.start();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				return;
			}
			if (c == playlistPauseCmd) {
				if (currentPlayer != null) {
					try {
						currentPlayer.stop();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				return;
			}
			if (c == playlistNextCmd) {
				if (playerState == 3) return;
				startNextMusic(true, playlistIndex);
				return;
			}
			if (c == playlistPrevCmd) {
				if (playerState == 3) return;
				startNextMusic(false, playlistIndex);
				return;
			}
			if (c == playlistCmd) {
				if (playlistList == null) return;
				display(playlistList);
				return;
			}
			if (c == playerCmd) {
				display(initPlayerForm());
				return;
			}
			if (c == togglePlaylistOrderCmd) {
				playlistDirection = !playlistDirection;
				needWriteConfig = true;
				return;
			}
		}
//#ifndef NO_NOKIAUI
		if (c == copyCmd) {
			try {
				if (checkClass("com.nokia.mid.ui.Clipboard")) {
					NokiaAPI.copy(((TextBox) d).getString());
					display(infoAlert(L[LTextCopied_Alert]), current);
					return;
				}
			} catch (Throwable ignored) {}
			return;
		}
//#endif
//#ifndef NO_FILE
		{ // download dialog
			if (c == downloadInappCmd) {
				downloadContinue(1);
				return;
			}
			if (c == downloadBrowserCmd) {
				display(current);
				downloadContinue(2);
				return;
			}
			if (c == cancelDownloadCmd) {
				downloading = false;
				if (confirmationTask == RUN_DOWNLOAD_DOCUMENT) {
					 c = backCmd;
				} else {
					return;
				}
			}
			if (c == cancelUploadCmd) {
				sending = false;
				return;
			}
			if (c == okDownloadCmd) {
				display(current);
				if (current instanceof List) {
					commandAction(backCmd, current);
				}
				return;
			}
			if (c == openDownloadedCmd) {
				browse(downloadedPath);
				return;
			}
		}
//#endif
		if (c == confirmCmd) {
			commandAction(backCmd, d);
			if ((confirmationTask & 0x100) != 0) {
				display(loadingAlert(L[LLoading]), current);
			}
			if ((confirmationTask & 0x200) != 0) {
				if ((reopenChat || !longpoll) && MP.updatesThread != null) {
					MP.cancel(MP.updatesThread, true);
				}
			}
			start(confirmationTask & 0xFF, confirmationParam);

			confirmationTask = 0;
			confirmationParam = null;
			return;
		}
		if (c == backCmd || c == cancelCmd) {
			// cancel dialogs
			if (c == cancelCmd) {
				updateUrl = null;
				confirmationTask = 0;
				confirmationParam = null;
			}
			
			if (formHistory.size() == 0) {
				if (d == mainDisplayable && symbian) {
					// minimize app
					display.setCurrent(null);
					return;
				}
				display(null, true);
				return;
			}
			
			if (d instanceof Alert) {
				display(current, true);
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
			destroyApp(true);
		}
	}

	public void commandAction(Command c, Item item) {
		{ // message
			// note: no need to check if canvas ui is enabled, since event came from lcdui item
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
				display(writeForm(((ChatForm) current).id, s[1], "", null, null, null));
				return;
			}
			if (c == forwardMsgCmd) {
				String[] s = (String[]) ((MPForm) current).urls.get(item);
				if (s == null) return;
				openLoad(new ChatsList(s[0], s[1]));
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
				copyMessageLink(s[0], s[1]);
				return;
			}
			if (c == openImageCmd) {
				String[] s = (String[]) ((MPForm) current).urls.get(item);
				if (s == null) return;
				if (useView) {
					display(new ViewCanvas(s[0], s[1]));
				} else {
					browse(instanceUrl + FILE_URL + "?c=" + s[0] + "&m=" + s[1] + "&user=" + user);
				}
				return;
			}
			if (c == deleteMsgCmd) {
				String[] s = (String[]) ((MPForm) current).urls.get(item);
				if (s == null) return;
				
				MP.confirm(MP.RUN_DELETE_MESSAGE | 0x100 | 0x200,
						s,
						null,
						MP.L[MP.LDeleteMessage_Alert]);
				return;
			}
			if (c == documentCmd) {
				String[] s = (String[]) ((MPForm) current).urls.get(item);
				if (s == null) return;
				downloadDocument(s[0], s[1], s[4] == null ? s[0] + '_' + s[1] + ".jpg" : s[4], s[5]);
				return;
			}
			if (c == editMsgCmd) {
				String[] s = (String[]) ((MPForm) current).urls.get(item);
				if (s == null) return;
				display(writeForm(s[0], null, (String) ((MPForm) current).urls.get(s[1]), s[1], null, null));
				return;
			}
			if (c == gotoMsgCmd) {
				String[] s = (String[]) ((MPForm) current).urls.get(item);
				if (s == null) return;
				if (((ChatForm) current).parent != null) {
					// from search
					goBackTo((Displayable) ((ChatForm) current).parent);
					((ChatForm) current).openMessage(s[1], -1);
					return;
				}
				if ((s[0] == null || s[0].equals(((ChatForm) current).id))
						&& ((ChatForm) current).query == null) {
					// from current chat
					((ChatForm) current).openMessage(s[1], -1);
					return;
				}
				
				// open new chat form
				openChat(s[0], Integer.parseInt(s[1]));
				return;
			}
			if (c == botCallbackCmd) {
				String[] p = (String[]) ((MPForm) current).urls.get(item);
				if (p == null) return;
				
				synchronized (this) {
					if (sending) return;
					sending = true;
				}

				if ((reopenChat || !longpoll) && !globalUpdates) {
					display(loadingAlert(L[LSending]), current);
					if (MP.updatesThread != null) {
						MP.cancel(MP.updatesThread, true);
					}
				}
				start(RUN_BOT_CALLBACK, p);
				return;
			}
			if (c == banMemberCmd) {
				String[] s = (String[]) ((MPForm) current).urls.get(item);
				if (s == null) return;
				
				MP.confirm(RUN_BAN_MEMBER | 0x100,
						s,
						null,
						MP.L[LBanMember_Alert]);
				return;
			}
			if (c == pinMsgCmd) {
				String[] s = (String[]) ((MPForm) current).urls.get(item);
				if (s == null) return;
				
				MP.confirm(RUN_PIN_MESSAGE | 0x100 | 0x200,
						s,
						null,
						MP.L[LPinMessage_Alert]);
				return;
			}
		}
		if (c == richTextLinkCmd) {
			String url = null;
			try {
				url = (String) ((MPForm) current).urls.get(item);
			} catch (Exception ignored) {}
			if (url == null) url = ((StringItem) item).getText();
			
			openUrl(url, true);
			return;
		}
		if (c == callItemCmd) {
			browse("tel:".concat(((StringItem) item).getText()));
			return;
		}
		if (c == sendCmd && current instanceof ChatForm) { 
			// textfield send
			String t;
			if ((t = ((TextField) item).getString().trim()).length() == 0)
				return;
			
			synchronized (this) {
				if (sending) return;
				sending = true;
			}

			display(loadingAlert(L[LSending]), current);
			if ((reopenChat || !longpoll) && MP.updatesThread != null) {
				MP.cancel(MP.updatesThread, true);
			}
			start(RUN_SEND_MESSAGE, new Object[] { t, ((ChatForm) current).id, null, null, null, null, null, null });
			return;
		}
		if (c == stickerItemCmd) {
			// sticker selected, send it
			JSONObject s = (JSONObject) ((MPForm) current).urls.get(item);
			if (s == null) return;
			
			synchronized (this) {
				if (sending) return;
				sending = true;
			}

			display(loadingAlert(L[LSending]), current);
			if ((reopenChat ||!longpoll) && MP.updatesThread != null) {
				MP.cancel(MP.updatesThread, true);
			}
			imagesToLoad.removeAllElements();
			
			start(RUN_SEND_STICKER, s);
			return;
		}
		if (c == playItemCmd) {
			// play message media
			String[] s = (String[]) ((MPForm) current).urls.get(item);
			if (s == null) return;
			
			display(loadingAlert(L[LLoading]), current);
			start(RUN_LOAD_PLAYLIST, new String[] {s[0], "3", s[1]});
			return;
		}
		if (c == playVoiceCmd) {
			// play voice message TODO
			String[] s = (String[]) ((MPForm) current).urls.get(item);
			if (s == null) return;
			
			return;
		}
		if (c == postCommentsCmd) {
			// open post discussion
			String[] s = (String[]) ((MPForm) current).urls.get(item);
			if (s == null) return;
			
			openLoad(new ChatForm(s[0], s[1], s[2], Integer.parseInt(s[3])));
			return;
		}
		commandAction(c, display.getCurrent());
	}

	public void itemStateChanged(Item item) {
		if (item == messageField
				|| (current instanceof ChatForm && item instanceof TextField)) {
			String s;
			// auto send on two returns
			if (item != messageField &&
				((s = ((TextField) item).getString()).endsWith("\n\n")
					|| s.endsWith("\r\n\r\n"))) {
				commandAction(sendCmd, item);
				return;
			}
			sendTyping(((TextField) item).getString().trim().length() == 0);
			return;
		}
		if (item == playerVolumeGauge) {
			try { 
				((VolumeControl) currentPlayer.getControl("VolumeControl"))
				.setLevel(playerVolume = playerVolumeGauge.getValue());
			} catch (Throwable ignored) {}
			needWriteConfig = true;
			return;
		}
		if (item == behChoice) {
			// label needs to be changed according to longpoll setting
			try {
				updateTimeoutGauge.setLabel(L[behChoice.isSelected(7) ? LUpdatesTimeout : LUpdatesInterval]);
			} catch (Exception ignored) {}
			return;
		}
	}
	
	public void sendTyping(boolean cancel) {
		if (!sendTyping) return;
		long l = System.currentTimeMillis();
		if (l - lastType < 5000L) return;
		
		lastType = l;
		start(RUN_SET_TYPING, cancel ? "Cancel" : null);
	}
	
	// endregion
	
	// region Music player
	
	public void playerUpdate(Player player, String event, Object eventData) {
		if (PlayerListener.END_OF_MEDIA.equals(event)) {
			playerState = 2;
			if (playerProgress != null) {
				playerProgress.setValue(100);
			}
			if (playerPlaypauseBtn != null) {
				playerPlaypauseBtn.setText(L[LPlay_Player]);
				playerPlaypauseBtn.removeCommand(playlistPauseCmd);
				playerPlaypauseBtn.setDefaultCommand(playlistPlayCmd);
			}
			startNextMusic(true, playlistIndex);
			return;
		}
		if (PlayerListener.STARTED.equals(event)) {
			playerState = 1;
			if (playerPlaypauseBtn != null) {
				playerPlaypauseBtn.setText(L[LPause_Player]);
				playerPlaypauseBtn.removeCommand(playlistPlayCmd);
				playerPlaypauseBtn.setDefaultCommand(playlistPauseCmd);
			}
			start(RUN_PLAYER_LOOP, player);
		} else if (PlayerListener.STOPPED.equals(event) || PlayerListener.STOPPED_AT_TIME.equals(event)) {
			playerState = 2;
			if (playerPlaypauseBtn != null) {
				playerPlaypauseBtn.setText(L[LPlay_Player]);
				playerPlaypauseBtn.removeCommand(playlistPauseCmd);
				playerPlaypauseBtn.setDefaultCommand(playlistPlayCmd);
			}
		} else if (PlayerListener.ERROR.equals(event)) {
			closePlayer();
			return;
		}
		
		if (playerProgress != null) {
			try {
				int progress = 0;
				final long duration = currentMusic.getObject("media").getObject("audio").getInt("time", 0) * 1000000L;
				progress = (int) ((player.getMediaTime() * 100) / duration);
	
				if (progress < 0) progress = 0;
				if (progress > 100) progress = 100;
				
				playerProgress.setValue(progress);
			} catch (Exception ignored) {}
		}
	}
	
	private void startNextMusic(boolean dir, int start) {
		int tmp = playerState;
		playerState = 3;
		if (playlist != null) {
			int idx = start;
			for (;;) {
				if (playlistDirection == dir) {
					if (++idx >= playlist.size()) {
						if (playlist.size() != playlistSize) {
							start(RUN_LOAD_PLAYLIST, new String[] {null, "1"});
							return;
						}
						break;
					}
				} else {
					if (--idx < 0) {
						if (playlistOffset != 0) {
							start(RUN_LOAD_PLAYLIST, new String[] {null, "2"});
							return;
						}
						break;
					}
				}
				JSONObject msg = (JSONObject) playlist.get(idx);
				String t;
				// filter playable media
				if (!"audio/mpeg".equals(t = msg.getObject("media").getString("mime"))
						&& !"audio/aac".equals(t)
						/*&& !"audio/m4a".equals(t)*/)
					continue;
				playlistIndex = idx;
				playerState = 0;
				startPlayer(currentMusic = msg);
				return;
			}
		}
		playerState = tmp;
	}

	static void startPlayer(JSONObject msg) {
		if (playerState == 3) return;
		closePlayer();
		playerState = 3;
			
		MP.midlet.start(RUN_START_PLAYER, msg);
	}
	
	static void closePlayer() {
		if (currentPlayer != null) {
			try {
				currentPlayer.stop();
			} catch (Throwable ignored) {}
			try {
				currentPlayer.close();
			} catch (Throwable ignored) {}
			currentPlayer = null;
			playerState = 0;
		}
		if (playerTitleLabel != null) {
			playerTitleLabel.setText("");
		}
		if (playerArtistLabel != null) {
			playerArtistLabel.setText("");
		}
		if (playerProgress != null) {
			try {
				playerProgress.setValue(0);
				playerProgress.setMaxValue(100);
			} catch (Exception ignored) {}
		}
		while (playerForm != null && playerForm.get(0) == playerCover) {
			playerForm.delete(0);
		}
	}
	
	// endregion
	
	// region Image queue
	
	static void queueAvatar(String id, Object target) {
//#ifndef NO_AVATARS
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
//#endif
	}

	private static void putImage(Object target, Image img) {
		if (target instanceof ImageItem) {
			((ImageItem) target).setImage(img);
			return;
		}
//#ifndef NO_AVATARS
		if (target instanceof Object[]) {
			// list item
			if (((Object[]) target)[0] instanceof List) {
				List list = ((List) ((Object[]) target)[0]);
				if (((Object[]) target)[1] instanceof String) {
					((ChatsList) list).setImage((String) ((Object[]) target)[1], img);
					return;
				}
				int idx = (((Integer) ((Object[]) target)[1])).intValue();
				list.set(idx, list.getString(idx), img);
				return;
			}
		}
//#endif
//#ifndef NO_CHAT_CANVAS
		if (target instanceof UIMessage) {
			((UIMessage) target).mediaImage = img;
			if (((UIMessage) target).photoRenderHeight == 0) ((UIMessage) target).layoutWidth = 0;
			((UIMessage) target).requestPaint();
			return;
		}
//#endif
//#ifndef NO_NOTIFY
//#ifndef NO_NOKIAUI
		if (target instanceof String) {
			// notification
			try {
				Notifier.updateImage((String) target, img);
			} catch (Throwable ignored) {}
			return;
		}
//#endif
//#endif
	}
	
	static void queueImage(Object src, Object target) {
		if (target == null || src == null
				// always load stickers for StickerPackForm
				|| (!loadThumbs && !(target instanceof JSONObject)))
			return;
		synchronized (imagesLoadLock) {
			imagesToLoad.addElement(new Object[] { src, target });
			imagesLoadLock.notifyAll();
		}
	}
	
	//endregion
	
	// region Peers

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
			res = getName(o, variant);
		}
		return res;
	}
	
	static String getName(String id, boolean variant) {
		return getName(id, variant, true);
	}
	
	static String getName(JSONObject p, boolean variant) {
		if (p == null) return null;
		if (p.has("t")) {
			return p.getString("t");
		}
		
		String fn = p.getString("fn");
		String ln = p.getString("ln");
		
		if (!variant && fn != null && ln != null) {
			return fn.concat(" ".concat(ln));
		}
		
		if (fn != null && fn.length() != 0) {
			return fn;
		}
		
		if (ln != null) {
			return ln;
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
			return fn.concat(" ".concat(ln));
		}
		
		if (fn != null && fn.length() != 0) {
			return fn;
		}
		
		if (ln != null) {
			return ln;
		}
		
		return "Deleted";
	}

	// endregion
	
	// region UI builders
	
	static ChatsList mainChatsList() {
		ChatsList l = chatsList = new ChatsList(L[Lmpgram], 0);
		if (symbian) {
			l.addCommand(backCmd);
		} else {
			l.removeCommand(backCmd);
		}
		l.addCommand(exitCmd);
		l.addCommand(aboutCmd);
		l.addCommand(settingsCmd);
		l.addCommand(contactsCmd);
		l.addCommand(searchChatsCmd);
		l.addCommand(openLinkCmd);
		return l;
	}
	
	static Form writeForm(String id, String reply, String text, String editId, String fwdPeerId, String fwdMsgId) {
		writeTo = id;
		replyTo = "0".equals(reply) ? null : reply;
		edit = editId;
		sendFile = null;
		fwdPeer = fwdPeerId;
		fwdMsg = fwdMsgId;
		
		Form f = new Form(fwdPeerId != null ? L[LForward_Title] :
			editId != null ? L[LEdit_Title] :
				reply != null ? L[LReply_Title] :
					L[LWrite_Title]);
		f.setCommandListener(midlet);
		f.setItemStateListener(midlet);
		f.addCommand(cancelCmd);
		f.addCommand(sendCmd);
		
		TextField t = new TextField(L[LMessage], text, 500, TextField.ANY);
//		t.addCommand(sendCmd);
		t.setItemCommandListener(midlet);
		f.append(messageField = t);
		
		StringItem s = new StringItem(null, "...", Item.BUTTON);
		s.setDefaultCommand(openTextBoxCmd);
		s.setItemCommandListener(midlet);
		f.append(s);
//#ifndef NO_FILE
		// file
		
		s = new StringItem(null, L[LFile_Prefix].concat(L[LNotSelected]));
		s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
		f.append(fileLabel = s);
		
		s = new StringItem(null, L[LChooseFile], Item.BUTTON);
		s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
		s.setDefaultCommand(chooseFileCmd);
		s.setItemCommandListener(midlet);
		f.append(s);
		
//		t = new TextField("File", "file:///", 300, TextField.ANY);
//		f.append(fileField = t);
		
		f.append(sendChoice = new ChoiceGroup("", Choice.MULTIPLE, new String[] {
				L[LSendUncompressed],
				L[LHideWithSpoiler]
		}, null));
//#endif
		
		return writeForm = f;
	}
	
//#ifndef NO_FILE
	static void openFilePicker(String path, boolean file) {
		fileMode = file;
		if (path == null || path.length() == 0) path = "/";
		display(loadingAlert(L[LLoading]), current);
		try {
			if (fileImg == null) {
				fileImg = Image.createImage("/file.png");
				folderImg = Image.createImage("/folder.png");
			}
			
			List list = new List(path, List.IMPLICIT);
			list.addCommand(cancelCmd);
			list.addCommand(List.SELECT_COMMAND);
			list.setSelectCommand(List.SELECT_COMMAND);
			list.setCommandListener(midlet);

			int fails = 0;
			for (;;) {
				if ("/".equals(path)) {
					list.setTitle("/");
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
					break;
				} else {
					list.append(L[LBack], null);
					if (!file) {
						list.append(L[LSaveHere], null);
					}
					try {
						FileConnection fc = (FileConnection) Connector.open("file:///".concat(path));
						try {
							Enumeration en = fc.list();
							while (en.hasMoreElements()) {
								String s = (String) en.nextElement();
								if (s.endsWith("/")) {
									list.append(s.substring(0, s.length() - 1), folderImg);
								} else if (file) {
									list.append(s, fileImg);
								}
							}
						} finally {
							fc.close();
						}
						break;
					} catch (IOException e) {
						if (fails++ != 0) throw e;
						list.deleteAll();
						path = "/";
						continue;
					}
				}
			}
			display(list);
		} catch (Exception e) {
			display(errorAlert(e), current);
			e.printStackTrace();
		}
	}
//#endif
	
	static void openChat(String id, int msg) {
		Displayable d = MP.current;
		if (d instanceof MPChat && id.equals(((MPChat) d).id())
				&& ((MPChat) d).postId() == null && ((MPChat) d).query() == null
				&& ((MPChat) d).mediaFilter() == null) {
			if (msg > 0) {
				((MPChat) d).openMessage(Integer.toString(msg), -1);
			}
			return;
		}
//#ifndef NO_CHAT_CANVAS
		if (!legacyChatUI) {
			openLoad(new ChatCanvas(id, null, msg, 0));
			return;
		}
//#endif
		openLoad(new ChatForm(id, null, msg, 0));
	}
	
	static void openProfile(String id, MPChat chatForm, int mode) {
		if (chatForm == null && current instanceof MPChat && id.equals(((MPChat) current).id())) {
			chatForm = (MPChat) current;
		}
		openLoad(new ChatInfoForm(id, chatForm, mode));
	}

	static Form initPlayerForm() {
		if (playerForm != null) {
			return playerForm;
		}
		
		Form f = new Form(L[LPlayer_Title].concat(" - mpgram"));
		f.addCommand(backCmd);
		f.addCommand(playlistCmd);
		f.addCommand(togglePlaylistOrderCmd);
		f.setCommandListener(midlet);
		f.setItemStateListener(midlet);
		
		ImageItem img = playerCover = new ImageItem("", null, 0, "");
		try {
			img.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
		} catch (Exception ignored) {}
		// cover item is added in MP#startPlayer()
//		f.append(img);
		
		StringItem s;
		
		s = new StringItem(null, L[LUnknownTrack]);
		s.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
		s.setFont(largePlainFont);
		f.append(playerTitleLabel = s);
		
		s = new StringItem(null, "");
		s.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
		s.setFont(smallPlainFont);
		f.append(playerArtistLabel = s);
		
		Gauge g = new Gauge(null, false, 100, 0);
		f.append(playerProgress = g);
		
		s = new StringItem(null, L[LPrev_Player], Item.BUTTON);
		s.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_NEWLINE_BEFORE);
		s.setDefaultCommand(playlistPrevCmd);
		s.setItemCommandListener(midlet);
		f.append(s);
		
		s = new StringItem(null, L[LPlay_Player], Item.BUTTON);
		s.setLayout(Item.LAYOUT_CENTER);
		s.setDefaultCommand(playlistPlayCmd);
		s.setItemCommandListener(midlet);
		f.append(playerPlaypauseBtn = s);
		
		s = new StringItem(null, L[LNext_Player], Item.BUTTON);
		s.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_NEWLINE_AFTER);
		s.setDefaultCommand(playlistNextCmd);
		s.setItemCommandListener(midlet);
		f.append(s);
		
		g = new Gauge(L[LVolume], true, 100, playerVolume);
		g.setLayout(Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_EXPAND);
		f.append(playerVolumeGauge = g);
		
		return playerForm = f;
	}
	
	static void copy(String title, String text) {
		TextBox t = new TextBox(title, text, Math.max(2000, text.length() + 1), TextField.UNEDITABLE);
		t.addCommand(backCmd);
//#ifndef NO_NOKIAUI
		if (checkClass("com.nokia.mid.ui.Clipboard")) t.addCommand(copyCmd);
//#endif
		t.setCommandListener(midlet);
		display(t);
	}
	
	static void copyMessageLink(String peerId, String msgId) {
		StringBuffer sb = new StringBuffer("https://t.me/"); 
		String username = ((MPChat) current).username();
		if (peerId.charAt(0) == '-' && username == null) {
			sb.append("c/");
			peerId = Long.toString(-Long.parseLong(peerId) + ZERO_CHANNEL_ID);
		}
		sb.append(username != null ? username : peerId).append('/').append(msgId);
		copy("", sb.toString());
	}
	
	void downloadDocument(String peerId, String msgId, String fileName, String size) {
//#ifndef NO_FILE
		if (downloading) return;
//#endif
		downloadMessage = new String[] { peerId, msgId, fileName, size };
//#ifndef NO_FILE
		file: {
			if (fileName != null && downloadMethod != 2) {
				if (fileName.endsWith(".jar") || fileName.endsWith(".jad")) {
					if (downloadMethod == 1) {
						// .jar -> .jar0
						if (System.getProperty("forcedomain") == null)
							downloadMessage[2] = fileName.concat("0");
					} else break file;
				} else if (downloadMethod == 0) {
					Alert a = new Alert(fileName);
					a.setString(L[LChooseDownloadMethod_Alert]);
					a.addCommand(downloadInappCmd);
					a.addCommand(downloadBrowserCmd);
					a.setCommandListener(this);
					display(a, current);
					return;
				}
				downloadContinue(1);
				return;
			}
		}
//#endif
		downloadContinue(2);
	}
	
	void downloadContinue(int state) {
//#ifndef NO_FILE
		if (downloading) return;
		try {
			Class.forName("javax.microedition.io.file.FileConnection");
			if (state == 1) {
				if (downloadPath == null || downloadPath.trim().length() == 0) {
					openFilePicker(lastDownloadPath, false);
					return;
				} else {
					start(RUN_DOWNLOAD_DOCUMENT, downloadCurrentPath = downloadPath);
					return;
				}
			}
		} catch (Throwable ignored) {
			// no jsr 75
		}
//#endif
		String peerId = downloadMessage[0];
		String msgId = downloadMessage[1];
		String fileName = downloadMessage[2];
		if (fileRewrite && fileName != null) {
			browse(instanceUrl + "file/" + url(fileName) + "?c=" + peerId + "&m=" + msgId + "&user=" + user);
		} else {
			browse(instanceUrl + FILE_URL + "?c=" + peerId + "&m=" + msgId + "&user=" + user);
		}
	}
	
	static void confirm(int task, Object param, String title, String text) {
		MP.confirmationTask = task;
		MP.confirmationParam = param;

		Alert d = MP.alert(title, text, AlertType.WARNING);
		d.addCommand(MP.cancelCmd);
		d.addCommand(MP.confirmCmd);
		d.setCommandListener(MP.midlet);
		display(d, current);
	}

	static Alert errorAlert(Exception e) {
		e.printStackTrace();
		if (!(e instanceof APIException)) {
			return errorAlert(e == cancelException ? "Operation canceled" : e.toString());
		}
		// parse api errors
		
		Object r = ((APIException) e).response;
		StringBuffer sb = new StringBuffer();
		sb.append(r);
		String stackTrace = null;
		if (r instanceof JSONObject) {
			if (((JSONObject) r).has("error")) {
				String message = ((JSONObject) r).getObject("error").getString("message", null);
				if (message != null) {
					if ("Unsupported API version".equals(message)) {
						sb.setLength(0);
						sb.append(L[LClientOutdated_Alert]);
					} else if ("Login API is disabled".equals(message)) {
						sb.setLength(0);
						sb.append(L[LLoginDisabled_Alert]);
					} else if ("API is disabled".equals(message)) {
						sb.setLength(0);
						sb.append(L[LAPIDisabled_Alert]);
					} else if ("Wrong instance password".equals(message)
							|| "Instance password is required".equals(message)) {
						sb.setLength(0);
						sb.append(L[LInvalidInstancePassword_Alert]);
					} else {
						sb.setLength(0);
						sb.append(message);
					}
					stackTrace = ((JSONObject) r).getObject("error").getString("stack_trace", null);
				}
			}
		}
		
		sb.append(" \n\nDetails: \n")
		.append(((APIException) e).code).append(' ').append(((APIException) e).url);
		if (stackTrace != null) {
			sb.append(" \nStack trace: \n").append(stackTrace);
		}
		
		Alert a = new Alert(symbian ? L[Lmpgram] : "");
		a.setType(AlertType.ERROR);
		a.setString(sb.toString());
		a.setTimeout(4000);
		return a;
	}

	static Alert errorAlert(String text) {
		return alert(null, text, AlertType.ERROR);
	}

	static Alert infoAlert(String text) {
		return alert(null, text, AlertType.CONFIRMATION);
	}

	static Alert alert(String title, String text, AlertType type) {
		Alert a = new Alert(title == null ? (symbian ? L[Lmpgram] : "") : title);
		a.setType(type);
		a.setString(text);
		a.setTimeout(type == AlertType.ERROR ? 3000 : 1500);
		return a;
	}

	static Alert loadingAlert(String s) {
		Alert a = new Alert(symbian ? L[Lmpgram] : "", s == null ? L[LLoading] : s, null, null);
		a.setCommandListener(midlet);
		a.addCommand(Alert.DISMISS_COMMAND);
		a.setIndicator(new Gauge(null, false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING));
		a.setTimeout(Alert.FOREVER);
		return a;
	}
	
	// endregion
	
	// region Display logic
	
	static void openLoad(Displayable d) {
		display(d);
		midlet.start(d instanceof MPList ? RUN_LOAD_LIST : RUN_LOAD_FORM, d);
	}
	
	// jump back at history, discarding everything after
	static void goBackTo(Displayable d) {
		synchronized (formHistory) {
			int i = formHistory.size();
			while (i-- != 0) {
				if (formHistory.elementAt(i) == d) {
					break;
				}
				formHistory.removeElementAt(i);
			}
		}
		display(d, true);
	}
	
	static void goBackToChat() {
		synchronized (formHistory) {
			int i = formHistory.size();
			while (i-- != 0) {
				Object d = formHistory.elementAt(i);
				if (d instanceof MPChat) {
					display((Displayable) d, true);
					break;
				}
				formHistory.removeElementAt(i);
			}
		}
	}
	
	static void deleteFromHistory(Displayable d) {
		synchronized (formHistory) {
			formHistory.removeElement(d);
			if (display.getCurrent() == d) {
				display(formHistory.size() == 0 ? null : (Displayable) formHistory.lastElement());
			}
		}
	}
	
	static void display(Alert a, Displayable d) {
		if (updateUrl != null) {
			return;
		}
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
		
		if (d != playerForm) {
			if (playerState != 0) {
				if (playerState == 1 && (series40 || reopenChat)) {
					closePlayer();
					d.removeCommand(playerCmd);
				} else {
					d.addCommand(playerCmd);
				}
			} else {
				d.removeCommand(playerCmd);
			}
		}
		
		Displayable p = display.getCurrent();
		if (p == loadingForm) p = current;
		display.setCurrent(current = d);
		if (p == null || p == d) return;
		
		if (p instanceof MPForm) {
			((MPForm) p).closed(back);
		} else if (p instanceof MPList) {
			((MPList) p).closed(back);
		} else if (p instanceof MPChat) {
			((MPChat) p).closed(back);
		}
		if (back && (p instanceof MPForm || p instanceof MPList || p instanceof MPChat)) {
			imagesToLoad.removeAllElements();
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
	
	// endregion
	
	// region URLs

	void browse(String url) {
		try {
			if (url.indexOf(':') == -1) {
				url = "http://".concat(url);
			}
			if (platformRequest(url)) destroyApp(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	static void openUrl(String url, boolean ask) {
		if (selfId != null && handleDeepLink(url)) {
			return;
		}
		if (ask) {
			MP.confirm(RUN_OPEN_URL, url, null, url); // TODO unlocalized
			return;
		}
		midlet.browse(url);
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
			if ((i = url.indexOf("t.me")) == 0
					|| (url.startsWith("http") && (i == 7 || i == 8))) {
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
					domain = Long.toString(-Long.parseLong(s[1]) + ZERO_CHANNEL_ID);
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
						if (current instanceof MPChat &&
								(domain.equals(((MPChat) current).id())
								|| domain.equals(((MPChat) current).username()))) {
							((MPChat) current).openMessage(messageId, topMsg);
							if (start != null) {
								((MPChat) current).setStartBot(start);
							}
						} else {
							if (start != null) {
								MP.confirm(RUN_OPEN_URL, url, null, url); // TODO unlocalized
								return true;
							}
							MPChat chat;
//#ifndef NO_CHAT_CANVAS
							if (!legacyChatUI) {
								chat = new ChatCanvas(domain, null, msg, topMsg);
							} else {
//#endif
								chat = new ChatForm(domain, null, msg, topMsg);
//#ifndef NO_CHAT_CANVAS
							}
//#endif
							if (start != null) chat.setStartBot(start);
							openLoad((Displayable) chat);
						}
						return true;
					}
				} else if (invite != null) {
					// resolve invite
					midlet.start(RUN_RESOLVE_INVITE, invite);
					
					return true;
				} else if (stickers != null) {
					// add stickers
					openLoad(new StickerPackForm(stickers));
					
					return true;
				}
				
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		System.out.println("Unhandled deep link: " + url);
		return url.startsWith("tg://");
	}
	
	// endregion
	
	// region Networking
	
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

			int c;
			try {
				threadConnections.put(thread, hc = openHttpConnection(t));
				hc.setRequestMethod("GET");
				c = hc.getResponseCode();
			} catch (IOException e) {
				if (e.toString().indexOf("-36") != -1) {
					c = 504;
				} else {
					throw e;
				}
			}
			
			// repeat request on server timeout
			if ((c == 502 || c == 504)
					&& !url.startsWith("updates") && !url.startsWith("send")) {
				try {
					hc.close();
				} catch (Exception ignored) {}
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					throw new RuntimeException(e.toString());
				}

				threadConnections.put(thread, hc = openHttpConnection(t));
				hc.setRequestMethod("GET");
				
				c = hc.getResponseCode();
			}
			
			try {
				// check if server has FILE_REWRITE
				if (hc.getHeaderField("X-file-rewrite-supported") != null) {
					fileRewrite = true;
				}
				if (hc.getHeaderField("X-voice-conversion-supported") != null) {
					voiceConversion = true;
				}
			} catch (Exception ignored) {}

			threadConnections.put(hc, in = openInputStream(hc));
			res = readResponse(in, hc, c, url);
		} finally {
			threadConnections.remove(thread);
			if (hc != null) {
				closingConnections.removeElement(hc);
				threadConnections.remove(hc);
			}
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

//#ifndef NO_FILE
	static Object postMessage(String url, String fileUrl, String text, Alert alert) throws IOException {
		HttpConnection http = null;
		InputStream httpIn = null;
		
		FileConnection file = null;
		
		Gauge gauge = null;
		if (alert != null) {
			alert.setTitle(L[LSending]);
			gauge = new Gauge(null, false, 100, 0);
			alert.setIndicator(gauge);
		}
		int fileTotal = 0, fileSent = 0;
		
		StringBuffer sb = new StringBuffer();
		Random rng = new Random();
		for (int i = 0; i < 27; i++) {
			sb.append('-');
		}
		for (int i = 0; i < 11; i++) {
			sb.append(rng.nextInt(10));
		}
		String boundary = sb.toString();
//		int boundaryLength = boundary.length();
		sb.setLength(0);
		byte[] CRLF = new byte[] { (byte) '\r', (byte) '\n' };
		byte[] DASHDASH = new byte[] { (byte) '-', (byte) '-' };
		
		if (!sending) throw cancelException;
		
		try {
			http = openHttpConnection(instanceUrl.concat(API_URL + "?v=" + API_VERSION + "&method=").concat(url));
			http.setRequestMethod("POST");
			http.setRequestProperty("Content-Type", "multipart/form-data; charset=UTF-8; boundary=".concat(boundary));

//			int contentLength = 0;
//			if (text != null) {
//				contentLength += 43 + 10 + boundaryLength + text.getBytes("UTF-8").length;
//			}
			if (fileUrl != null) {
				if (chunkedUpload) {
					http.setRequestProperty("Transfer-Encoding", "chunked");
				}
				file = (FileConnection) Connector.open(fileUrl);
				fileTotal = (int) file.fileSize();
//				contentLength += 55 + 1 + 10 + boundaryLength + file.getName().getBytes("UTF-8").length + fileTotal;
			}
//			contentLength += boundaryLength + 4;
//			http.setRequestProperty("Content-Length", String.valueOf(contentLength));

			OutputStream httpOut = http.openOutputStream();
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

				if (!sending) throw cancelException;
				if (fileUrl != null) {
					InputStream fileIn = file.openInputStream();
					
					if (alert != null && fileTotal != 0) {
						display(alert, current);
					} else {
						gauge = null;
					}
					
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
							if (chunkedUpload) {
								httpOut.flush();
							}
							fileSent += i;
							if (gauge != null) {
								gauge.setValue(Math.min((fileSent * 100) / fileTotal, 100));
							}
							if (!sending) throw cancelException;
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
			if (!sending) throw cancelException;

			return readResponse(httpIn = openInputStream(http), http, http.getResponseCode(), url);
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
//#endif

	private static Object readResponse(InputStream in, HttpConnection hc, int c, String url) throws IOException {
		Object res;
		try {
			if (jsonStream && (!url.startsWith("updates") || !series40)) {
				res = getJSONStream(in).nextValue();
			} else {
				res = parseJSON(readUtf(in, (int) hc.getLength()));
			}
		} catch (RuntimeException e) {
			if (c >= 400) {
				String r = null;
				if (c >= 520) {
					r = "Cloudflare: web server is down";
				} else {
					try {
						r = hc.getResponseMessage();
					} catch (Exception ignored) {}
				}
				throw new APIException(url, c, r);
			} else throw e;
		}
		if (c >= 400 || (res instanceof JSONObject && ((JSONObject) res).has("error"))) {
			throw new APIException(url, c, res);
		}
		return res;
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
		return new String(buf, 0, i, encoding);
	}
	
	private static byte[] readBytes(InputStream inputStream, int initialSize)
			throws IOException {
		if (initialSize <= 0) initialSize = 8192;
		byte[] buf = new byte[initialSize];
		int count = 0;
		byte[] readBuf = new byte[8192];
		int readLen;
		while ((readLen = inputStream.read(readBuf)) != -1) {
			if (count + readLen > buf.length) {
				System.arraycopy(buf, 0, buf = new byte[count + 16384], 0, count);
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
	
	static byte[] get(String url) throws IOException {
		HttpConnection hc = null;
		InputStream in = null;
		try {
			hc = openHttpConnection(url);
			hc.setRequestMethod("GET");
			int r;
			if ((r = hc.getResponseCode()) >= 400) {
				throw new IOException("HTTP ".concat(Integer.toString(r)));
			}
			return readBytes(in = openInputStream(hc), (int) hc.getLength());
		} finally {
			try {
				if (in != null) in.close();
			} catch (IOException e) {}
			try {
				if (hc != null) hc.close();
			} catch (IOException e) {}
		}
	}
	
	// wrapper for compression handling
	private static InputStream openInputStream(HttpConnection hc) throws IOException {
		InputStream i = hc.openInputStream();
//#ifndef NO_ZIP
		String enc = hc.getHeaderField("Content-Encoding");
		if ("deflate".equalsIgnoreCase(enc))
			i = new InflaterInputStream(i, new Inflater(true));
		else if ("gzip".equalsIgnoreCase(enc))
			i = new GZIPInputStream(i);
//#endif
		return i;
	}
	
	private static HttpConnection openHttpConnection(String url) throws IOException {
		System.out.println(url);
		if (blackberry && blackberryNetwork == 1) {
			url = url.concat(";deviceside=true;interface=wifi");
		}
		boolean u = (url.indexOf("method=updates") != -1 || url.indexOf("method=notifications") != -1
				|| url.indexOf(OTA_URL) != -1);
		HttpConnection hc = (HttpConnection) Connector.open(url, Connector.READ_WRITE, u);
		hc.setRequestProperty("User-Agent", "mpgram4/".concat(version).concat(" (https://github.com/shinovon/mpgram-client)"));
//#ifndef NO_ZIP
		if (!u && compress) {
			hc.setRequestProperty("Accept-Encoding", "gzip, deflate");
		}
//#endif
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

//#ifndef NO_FILE
	private static void downloadDocument(String url, String dest, Alert alert, Gauge gauge, int size, boolean chat) throws Exception {
		downloading = true;
		FileConnection fc = (FileConnection) Connector.open(dest);
		try {
			if (!fc.exists()) fc.create();
			else if (chat && downloadCurrentPath != null) {
				// file already exists, ask overwrite permission
				confirmationTask = RUN_DOWNLOAD_DOCUMENT;
				confirmationParam = downloadCurrentPath;
				downloadCurrentPath = null;

				alert.setIndicator(null);
				alert.setString(L[LRewriteFile_Alert]);
				alert.addCommand(confirmCmd);
				display(alert, current);
				return;
			} else {
				fc.delete();
				fc.create();
			}
			OutputStream out = fc.openOutputStream();
			try {
				if (!downloading) throw cancelException;
				HttpConnection hc = (HttpConnection) openHttpConnection(url);
				try {
					InputStream in = openInputStream(hc);
					try {
						if (alert != null) {
							if (gauge != null && size != 0) {
								alert.setIndicator(gauge = new Gauge(null, false, 100, 0));
							}
							alert.setString(L[LDownloading]);
						}

						byte[] buf = new byte[4096];
						int readTotal = 0;
						int read;
						int c = 0;
						while ((read = in.read(buf)) != -1) {
							out.write(buf, 0, read);
							if (gauge != null && (c++ % 4) == 0) {
								gauge.setValue(Math.min((int) ((readTotal * 100) / size), 100));
							}
							if (!downloading) throw cancelException;
							readTotal += read;
						}

						// done
						if (chat) {
							downloadedPath = dest;
							goBackToChat();
							alert.setIndicator(null);
							alert.addCommand(okDownloadCmd);
							if (!series40) alert.addCommand(openDownloadedCmd);
							alert.removeCommand(cancelDownloadCmd);
							alert.setString(L[LDownloadedTo] + downloadPath);
							display(alert, current);
						}
					} finally {
						try {
							in.close();
						} catch (Exception ignored) {}
					}
				} finally {
					try {
						hc.close();
					} catch (Exception ignored) {}
				}
			} finally {
				try {
					out.close();
				} catch (Exception ignored) {}
			}
		} finally {
			try {
				fc.close();
			} catch (Exception ignored) {} // TODO: should I left it ignored?
		}
	}
//#endif
	
	// endregion
	
	// region Localizations
	
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
	
	static StringBuffer appendTime(StringBuffer sb, long date) {
		date = (date + tzOffset) / 60;
		return sb.append(n(((int) date / 60) % 24))
				.append(':')
				.append(n((int) date % 60));
	}

	static String localizeFormattedPlural(int n, int i) {
		String s = Integer.toString(n);
		String l;
		if (L[LLocaleSlavicPlurals].length() == 0) {
			l = L[n == 1 ? i : i + 1];
		} else {
			int a = n % 10;
			int b = n % 100;
			if ("pl".equals(lang) ? n == 1 : (a == 1 && b != 11))
				l = L[i];
			else if ((a >= 2 && a <= 4) && !(b >= 12 && b <= 14))
				l = L[i + 1];
			else
				l = L[i + 2];
		}

		int idx;
		if ((idx = l.indexOf('%')) != -1) {
			return l.substring(0, idx).concat(s.concat(l.substring(idx + 1)));
		}
		return s.concat(l);
	}

	static String localizePlural(int n, int i) {
		String s = Integer.toString(n);
		if (L[LLocaleSlavicPlurals].length() == 0) {
			String l = L[n == 1 ? i : i + 1];
			if (L[LLocaleCustomPlurals].length() != 0) {
				int idx;
				if ((idx = l.indexOf('%')) != -1) {
					return l.substring(0, idx).concat(s.concat(l.substring(idx + 1)));
				}
			}
			return s.concat(l);
		}
		
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
		if (L[LLocaleSlavicPlurals].length() == 0) {
			String l = L[n == 1 ? i : i + 1];
			if (L[LLocaleCustomPlurals].length() != 0) {
				int idx;
				if ((idx = l.indexOf('%')) != -1) {
					return sb.insert(0, l.substring(0, idx)).append(l.substring(idx + 1));
				}
			}
			return sb.append(l);
		}
		
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
				return L[LJustNow];
			}
			
			if (d < 60 * 60) {
				d /= 60L;
				return appendLocalizedPlural(sb, (int) d, L_minuteAgo).toString();
			}
			
			if (d < 12 * 60 * 60) {
				d /= 60 * 60L;
				return appendLocalizedPlural(sb, (int) d, L_hourAgo).toString();
			}
		}
		
		if (mode == 2) {
			if (d < 24 * 60 * 60) {
				sb.append(n(c.get(Calendar.HOUR_OF_DAY)))
				.append(':')
				.append(n(c.get(Calendar.MINUTE)));
			} else if (d < 6 * 24 * 60 * 60) {
				sb.append(L[LSun + c.get(Calendar.DAY_OF_WEEK) - 1]);
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
					sb.append(L[LToday]);
				} else if (day == dayNow - 1) {
					sb.append(L[LYesterday]);
				} else {
					b = false;
					sb.append(n(c.get(Calendar.DAY_OF_MONTH)))
					.append('.')
					.append(n(c.get(Calendar.MONTH) + 1))
					.append('.')
					.append(n(c.get(Calendar.YEAR)));
				}
				if (b || mode == 3) {
					sb.append(L[Lat_Time])
					.append(n(c.get(Calendar.HOUR_OF_DAY)))
					.append(':')
					.append(n(c.get(Calendar.MINUTE)));
				}
			} else {
				if (!ru) sb.append(L[LJan + c.get(Calendar.MONTH)]).append(' ');
				sb.append(c.get(Calendar.DAY_OF_MONTH));
				if (ru) sb.append(' ').append(L[LJan + c.get(Calendar.MONTH)]);
				
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

	// text for dialogs, notifications
	static StringBuffer appendDialog(StringBuffer sb, JSONObject peer, String id, JSONObject message) {
		if (message != null) {
			sb.append(MP.localizeDate(message.getLong("date"), 2)).append(' ');
			if (!peer.getBoolean("c", false)) {
				if (message.getBoolean("out", false) && !id.equals(selfId)) {
					sb.append(MP.L[LYou_Prefix]);
				} else if (id.charAt(0) == '-' && message.has("from_id")) {
					MP.appendOneLine(sb, MP.getName(message.getString("from_id"), true)).append(": ");
				}
			}
			if (message.has("media")) {
				sb.append(MP.L[LMedia]);
			} else if (message.has("fwd")) {
				sb.append(MP.L[LForwardedMessage]);
			} else  if (message.has("act")) {
				sb.append(MP.L[LAction]);
			} else {
				MP.appendOneLine(sb, message.getString("text"));
			}
		}
		return sb;
	}
	
	// endregion
	
	// region Misc utils

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
	
	// compact text
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
	
//#ifndef NO_AVATARS
	public static Image roundImage(Image img) {
		if (img == null) return null;
		try {
			int w = img.getWidth(), h = img.getHeight();
			int[] c = new int[w * h];
			img.getRGB(c, 0, w, 0, 0, w, h);
			int cx = w / 2, cy = h / 2;
			int r = Math.min(cx, cy);
			int r2 = r * r;
			for (int y = 0; y < h; y++) {
				int dy = y - cy;
				for (int x = 0; x < w; x++) {
					int dx = x - cx;
					if (dx * dx + dy * dy > r2)
						c[y * w + x] = 0x00FFFFFF;
				}
			}
			return Image.createRGBImage(c, w, h, true);
		} catch (Exception e) {
			return img;
		}
	}
//#endif

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

	private static void writeConfig() throws Exception {
		try {
			RecordStore.deleteRecordStore(SETTINGS_RECORD_NAME);
		} catch (Exception ignored) {}

		JSONObject j = new JSONObject();
		j.put("reverseChat", reverseChat);
//#ifndef NO_AVATARS
		j.put("loadAvatars", loadAvatars);
		j.put("avatarSize", avatarSize);
//#endif
		j.put("showMedia", showMedia);
		j.put("photoSize", photoSize);
		j.put("loadThumbs", loadThumbs);
		j.put("threadedImages", threadedImages);
//#ifndef NO_AVATARS
		j.put("avatarsCache", avatarsCache);
		j.put("avatarsCacheThreshold", avatarsCacheThreshold);
//#endif
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
		j.put("roundAvatars", roundAvatars);
		j.put("utf", utf);
//#ifndef NO_ZIP
		j.put("compress", compress);
//#endif
		j.put("useView", useView);
		j.put("blackberryNetwork", blackberryNetwork);
		j.put("fullPlayerCover", fullPlayerCover);
//#ifndef NO_NOTIFY
		j.put("notifications", notifications);
		j.put("notifySound", notifySound);
		j.put("pushInterval", pushInterval);
		j.put("pushBgInterval", pushBgInterval);
		j.put("notifyMethod", notifyMethod);
		j.put("notifyVolume", notificationVolume);
//#endif
//#ifndef NO_CHAT_CANVAS
		j.put("legacyChatUI", legacyChatUI);
		j.put("textMethod", textMethod);
		j.put("theme", theme);

		JSONArray inputLanguagesJson = new JSONArray();
		if (inputLanguages != null) {
			for (int k = 0; k < inputLanguages.length; ++k) {
				if (inputLanguages[k] == null) break;
				inputLanguagesJson.add(inputLanguages[k]);
			}
			j.put("inputLanguages", inputLanguagesJson);
		}
		j.put("lazyLoading", lazyLoading);
		j.put("fastScrolling", fastScrolling);
//#endif
//#ifndef NO_FILE
		j.put("downloadPath", downloadPath);
		j.put("uploadChunked", chunkedUpload);
		j.put("downloadMethod", downloadMethod);
		j.put("lastDownloadPath", lastDownloadPath);
		j.put("lastUploadPath", lastUploadPath);
		j.put("playMethod", playMethod);
//#endif
		j.put("longpoll", longpoll);
		j.put("playlistDirection", playlistDirection);
		j.put("playerVolume", playerVolume);
		j.put("playerCreateMethod", playerCreateMethod);

		byte[] b = j.toString().getBytes("UTF-8");
		RecordStore r = RecordStore.openRecordStore(SETTINGS_RECORD_NAME, true);
		r.addRecord(b, 0, b.length);
		r.closeRecordStore();

		needWriteConfig = false;
	}
	
	private static boolean checkClass(String s) {
		try {
			Class.forName(s);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	static void gc() {
		System.out.println("EMERGENCY COLLECTION");
		imagesToLoad.removeAllElements();
		usersCache.clear();
		chatsCache.clear();
		imagesCache.clear();
		if (current instanceof MPChat) {
			((MPChat) current).gc();
		}
		formHistory.removeAllElements();
		System.gc();
	}
	
	// endregion
	
	// region Rich text
	
	private static final int
			RT_BOLD = 0,
			RT_ITALIC = 1,
			RT_PRE = 2,
			RT_UNDERLINE = 3,
			RT_STRIKE = 4,
			RT_SPOILER = 5,
			RT_URL = 6;

	static int wrapRichText(Object form, Thread thread, String text, JSONArray entities, int insert) {
		return wrapRichText(form, thread, text, entities, insert, new int[8]);
	}
	
	private static int wrapRichNestedText(Object form, Thread thread, String text, JSONObject entity, JSONArray allEntities, int insert, int[] state) {
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

	private static int wrapRichText(Object form, Thread thread, String text, JSONArray entities, int insert, int[] state) {
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
			if ("messageEntityUrl".equals(type) || "messageEntityMention".equals(type)) {
				state[RT_URL] ++;
				insert = flush(form, thread, richTextUrl = entityText, insert, state);
				state[RT_URL] --;
			} else if ("messageEntityMentionName".equals(type)) {
				state[RT_URL] ++;
				richTextUrl = "@".concat(entity.getString("user_id"));
				insert = flush(form, thread, entityText, insert, state);
				state[RT_URL] --;
			} else if ("messageEntityPhone".equals(type)) {
				state[RT_URL] ++;
				richTextUrl = "tel:".concat(entityText);
				insert = flush(form, thread, entityText, insert, state);
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
	
	static int flush(Object form, Thread thread, String text, int insert, int[] state) {
		if (text.length() == 0) return insert;
		
		StringBuffer sb = new StringBuffer(text);
		int space = 0;
		if (form instanceof MPForm) {
			while (sb.length() != 0 && sb.charAt(sb.length() - 1) == ' ') {
				sb.setLength(sb.length() - 1);
				space ++;
			}
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
						
//#ifndef NO_CHAT_CANVAS
						if (form instanceof UILabel) {
							if (i != 0) {
								((UILabel) form).append(text.substring(0, j), f, null);
							}
							((UILabel) form).append(text.substring(j, k), f, null);
						} else
//#endif
						{
							if (i != 0) {
								s = new StringItem(null, text.substring(0, j));
								s.setFont(f);
								((MPForm) form).safeInsert(thread, insert++, s);
							}
							s = new StringItem(null, text.substring(j, k));
							s.setFont(f);
							s.setDefaultCommand(richTextLinkCmd);
							s.setItemCommandListener(midlet);
							((MPForm) form).safeInsert(thread, insert++, s);
						}
						
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
//#ifndef NO_CHAT_CANVAS
						if (form instanceof UILabel) {
							if (i != 0) {
								((UILabel) form).append(text.substring(0, i), f, null);
							}
							((UILabel) form).append(text.substring(i, k), f, null);
						} else
//#endif
						{
							if (i != 0) {
								s = new StringItem(null, text.substring(0, i));
								s.setFont(f);
								((MPForm) form).safeInsert(thread, insert++, s);
							}
							s = new StringItem(null, text.substring(i, k));
							s.setFont(f);
							s.setDefaultCommand(richTextLinkCmd);
							s.setItemCommandListener(midlet);
							((MPForm) form).safeInsert(thread, insert++, s);
						}
						
						text = text.substring(k);
						d = 0;
						continue;
					}
					d = i + 1;
				}
			}
		}

//#ifndef NO_CHAT_CANVAS
		if (form instanceof UILabel) {
			((UILabel) form).append(text, f, state != null && state[RT_URL] != 0 ? richTextUrl : null);
		} else
//#endif
		{
			s = new StringItem(null, text);
			s.setFont(f);
			if (state != null && state[RT_URL] != 0) {
				((MPForm) form).urls.put(s, richTextUrl);
				s.setDefaultCommand(richTextLinkCmd);
				s.setItemCommandListener(midlet);
			}
			
			if (text.length() != 0) {
				((MPForm) form).safeInsert(thread, insert++, s);
			}
		}

		if (space != 0 /* && instanceof MPForm */ ) {
			((MPForm) form).safeInsert(thread, insert++, new Spacer(f.charWidth(' ') * space, f.getBaselinePosition()));
		}
		
		return insert;
	}

	static Font getFont(int[] state) {
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
	
	// endregion
	
//#ifndef NO_AVATARS
	// region ImageUtils
	
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
	
	private static void resize_rgb_filtered(Image src_i, int[] dst, int w0, int h0, int w1, int h1) {
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
	
	// endregion
//#endif
	
	// region JSON
	
	// cc.nnproject.json.JSON

	// parse all nested elements once
	static final boolean parse_members = false;
	
	// identation for formatting
	static final String FORMAT_TAB = "  ";
	
	// used for storing nulls, get methods must return real null
	public static final Object json_null = new Object();
	
	public static final Boolean TRUE = new Boolean(true);
	public static final Boolean FALSE = new Boolean(false);

	public static JSONObject parseObject(String text) {
		if (text == null || text.length() <= 1)
			throw new RuntimeException("JSON: Empty text");
		if (text.charAt(0) != '{')
			throw new RuntimeException("JSON: Not JSON object: " + text);
		return (JSONObject) parseJSON(text);
	}

	public static JSONArray parseArray(String text) {
		if (text == null || text.length() <= 1)
			throw new RuntimeException("JSON: Empty text");
		if (text.charAt(0) != '[')
			throw new RuntimeException("JSON: Not JSON array");
		return (JSONArray) parseJSON(text);
	}

	static Object getJSON(Object obj) {
		if (obj instanceof Hashtable) {
			return new JSONObject((Hashtable) obj);
		}
		if (obj instanceof Vector) {
			return new JSONArray((Vector) obj);
		}
		return obj == null ? json_null : obj;
	}

	public static Object parseJSON(String str) {
		char first = str.charAt(0);
		if (first <= ' ')
			first = (str = str.trim()).charAt(0);
		int length;
		char last = str.charAt(length = str.length() - 1);
		if (last <= ' ')
			last = (str = str.trim()).charAt(length = str.length() - 1);
		switch(first) {
		case '"': { // string
			if (last != '"')
				throw new RuntimeException("JSON: Unexpected end of text");
			if (str.indexOf('\\') != -1) {
				char[] chars = str.toCharArray();
				str = null;
				int l = chars.length - 1;
				StringBuffer sb = new StringBuffer();
				int i = 1;
				// parse escaped chars in string
				loop: {
					while (i < l) {
						char c = chars[i];
						switch (c) {
						case '\\': {
							next: {
								replace: {
									if (l < i + 1) {
										sb.append(c);
										break loop;
									}
									char c1 = chars[i + 1];
									switch (c1) {
									case 'u':
										i+=2;
										sb.append((char) Integer.parseInt(
												new String(new char[] {chars[i++], chars[i++], chars[i++], chars[i++]}),
												16));
										break replace;
									case 'x':
										i+=2;
										sb.append((char) Integer.parseInt(
												new String(new char[] {chars[i++], chars[i++]}),
												16));
										break replace;
									case 'n':
										sb.append('\n');
										i+=2;
										break replace;
									case 'r':
										sb.append('\r');
										i+=2;
										break replace;
									case 't':
										sb.append('\t');
										i+=2;
										break replace;
									case 'f':
										sb.append('\f');
										i+=2;
										break replace;
									case 'b':
										sb.append('\b');
										i+=2;
										break replace;
									case '\"':
									case '\'':
									case '\\':
									case '/':
										i+=2;
										sb.append((char) c1);
										break replace;
									default:
										break next;
									}
								}
								break;
							}
							sb.append(c);
							i++;
							break;
						}
						default:
							sb.append(c);
							i++;
						}
					}
				}
				str = sb.toString();
				sb = null;
				return str;
			}
			return str.substring(1, length);
		}
		case '{': // JSON object or array
		case '[': {
			boolean object = first == '{';
			if (object ? last != '}' : last != ']')
				throw new RuntimeException("JSON: Unexpected end of text");
			int brackets = 0;
			int i = 1;
			char nextDelimiter = object ? ':' : ',';
			boolean escape = false;
			String key = null;
			Object res = object ? (Object) new JSONObject() : (Object) new JSONArray();
			
			for (int splIndex; i < length; i = splIndex + 1) {
				// skip all spaces
				for (; i < length - 1 && str.charAt(i) <= ' '; i++);

				splIndex = i;
				boolean quote = false;
				for (; splIndex < length && (quote || brackets > 0 || str.charAt(splIndex) != nextDelimiter); splIndex++) {
					char c = str.charAt(splIndex);
					if (!escape) {
						if (c == '\\') {
							escape = true;
						} else if (c == '"') {
							quote = !quote;
						}
					} else escape = false;
	
					if (!quote) {
						if (c == '{' || c == '[') {
							brackets++;
						} else if (c == '}' || c == ']') {
							brackets--;
						}
					}
				}

				// fail if unclosed quotes or brackets left
				if (quote || brackets > 0) {
					throw new RuntimeException("JSON: Corrupted JSON");
				}

				if (object && key == null) {
					key = str.substring(i, splIndex);
					key = key.substring(1, key.length() - 1);
					nextDelimiter = ',';
				} else {
					Object value = str.substring(i, splIndex).trim();
					// don't check length because if value is empty, then exception is going to be thrown anyway
					char c = ((String) value).charAt(0);
					// leave JSONString as value to parse it later, if its object or array and nested parsing is disabled
					value = parse_members || (c != '{' && c != '[') ?
							parseJSON((String) value) : new String[] {(String) value};
					if (object) {
						((JSONObject) res).table.put(key, value);
						key = null;
						nextDelimiter = ':';
					} else if (splIndex > i) {
						((JSONArray) res).addElement(value);
					}
				}
			}
			return res;
		}
		case 'n': // null
			return json_null;
		case 't': // true
			return TRUE;
		case 'f': // false
			return FALSE;
		default: // number
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
				} catch (Exception e) {}
			}
			throw new RuntimeException("JSON: Couldn't be parsed: ".concat(str));
//			return new JSONString(str);
		}
	}
	
	public static boolean isNull(Object obj) {
		return obj == json_null || obj == null;
	}

	// transforms string for exporting
	static String escape_utf8(String s) {
		int len = s.length();
		StringBuffer sb = new StringBuffer();
		int i = 0;
		while (i < len) {
			char c = s.charAt(i);
			switch (c) {
			case '"':
			case '\\':
				sb.append("\\").append(c);
				break;
			case '\b':
				sb.append("\\b");
				break;
			case '\f':
				sb.append("\\f");
				break;
			case '\n':
				sb.append("\\n");
				break;
			case '\r':
				sb.append("\\r");
				break;
			case '\t':
				sb.append("\\t");
				break;
			default:
				if (c < 32 || c > 1103 || (c >= '\u0080' && c < '\u00a0')) {
					String u = Integer.toHexString(c);
					sb.append("\\u");
					for (int z = u.length(); z < 4; z++) {
						sb.append('0');
					}
					sb.append(u);
				} else {
					sb.append(c);
				}
			}
			i++;
		}
		return sb.toString();
	}

//	static double getDouble(Object o) {
//		try {
//			if (o instanceof String[])
//				return Double.parseDouble(((String[]) o)[0]);
//			if (o instanceof Integer)
//				return ((Integer) o).intValue();
//			if (o instanceof Long)
//				return ((Long) o).longValue();
//			if (o instanceof Double)
//				return ((Double) o).doubleValue();
//		} catch (Throwable e) {}
//		throw new RuntimeException("JSON: Cast to double failed: " + o);
//	}

	static int getInt(Object o) {
		try {
			if (o instanceof String[])
				return Integer.parseInt(((String[]) o)[0]);
			if (o instanceof Integer)
				return ((Integer) o).intValue();
			if (o instanceof Long)
				return (int) ((Long) o).longValue();
//			if (o instanceof Double)
//				return ((Double) o).intValue();
		} catch (Throwable e) {}
		throw new RuntimeException("JSON: Cast to int failed: " + o);
	}

	public static long getLong(Object o) {
		try {
			if (o instanceof String[])
				return Long.parseLong(((String[]) o)[0]);
			if (o instanceof Integer)
				return ((Integer) o).longValue();
			if (o instanceof Long)
				return ((Long) o).longValue();
//			if (o instanceof Double)
//				return ((Double) o).longValue();
		} catch (Throwable e) {}
		throw new RuntimeException("JSON: Cast to long failed: " + o);
	}

	public static void writeString(OutputStream out, String s) throws IOException {
		int len = s.length();
		for (int i = 0; i < len; ++i) {
			char c = s.charAt(i);
			switch (c) {
			case '"':
			case '\\':
				out.write((byte) '\\');
				out.write((byte) c);
				break;
			case '\b':
				out.write((byte) '\\');
				out.write((byte) 'b');
				break;
			case '\f':
				out.write((byte) '\\');
				out.write((byte) 'f');
				break;
			case '\n':
				out.write((byte) '\\');
				out.write((byte) 'n');
				break;
			case '\r':
				out.write((byte) '\\');
				out.write((byte) 'r');
				break;
			case '\t':
				out.write((byte) '\\');
				out.write((byte) 't');
				break;
			default:
				if (c < 32 || c > 255) {
					String u = Integer.toHexString(c);
					out.write((byte) '\\');
					out.write((byte) 'u');
					for (int z = u.length(); z < 4; z++) {
						out.write((byte) '0');
					}
					out.write(u.getBytes());
				} else {
					out.write((byte) c);
				}
			}
		}
	}
	
	// JSONStream static
	
	public static JSONStream getJSONStream(InputStream in) throws IOException {
		JSONStream json = new JSONStream();
		json.init(in);
		char c = json.nextTrim();
		if (c != '{' && c != '[')
			throw new RuntimeException("JSON: getStream: Not json");
		json.isObject = c == '{';
		json.usePrev = true;
		return json;
	}
	
	public static JSONStream getJSONStream(StreamConnection sc) throws IOException {
		JSONStream json = new JSONStream();
		json.connection = sc;
		json.init(sc.openInputStream());
		char c = json.nextTrim();
		if (c != '{' && c != '[')
			throw new RuntimeException("JSON: getStream: Not json");
		json.isObject = c == '{';
		json.usePrev = true;
		return json;
	}
	
	// endregion

}
