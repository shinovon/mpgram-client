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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.TimeZone;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
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

public class MP extends MIDlet implements CommandListener, ItemCommandListener, Runnable {

	static final int RUN_SEND_MESSAGE = 4;
	static final int RUN_VALIDATE_AUTH = 5;
	static final int RUN_IMAGES = 6;
	static final int RUN_UPDATES = 7;
	static final int RUN_LOAD_FORM = 8;
	static final int RUN_LOAD_LIST = 9;
	static final int RUN_AUTH = 10;
	static final int RUN_DELETE_MESSAGE = 11;
	static final int RUN_RESOLVE_INVITE = 12;
	static final int RUN_IMPORT_INVITE = 13;
	static final int RUN_JOIN_CHANNEL = 14;
	static final int RUN_LEAVE_CHANNEL = 15;
	
	private static final String SETTINGS_RECORD_NAME = "mp4config";
	private static final String AUTH_RECORD_NAME = "mp4user";
	private static final String AVATAR_RECORD_PREFIX = "mcA";
	
	private static final String DEFAULT_INSTANCE_URL = "http://mp2.nnchan.ru/";
	static final String API_URL = "api.php";
	static final String AVA_URL = "ava.php";
	static final String FILE_URL = "file.php";
	
	static final String API_VERSION = "5";
	
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
	static Display display;
	static Displayable current;

	private static String version;

	// localization
	static String[] L;
	
	// settings
	static String instanceUrl = DEFAULT_INSTANCE_URL;
	private static String instancePassword;
	private static int tzOffset;
	private static boolean symbianJrt;
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
	static int profilesCacheThreshold = 200;

	// threading
	private static int run;
	private static Object runParam;
//	private static int running;
	
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
	static Command searchCmd;

	static Command itemChatCmd;
	static Command itemChatInfoCmd;
	static Command replyMsgCmd;
	static Command forwardMsgCmd;
	static Command copyMsgCmd;
	static Command messageLinkCmd;
	static Command deleteMsgCmd;
	static Command editMsgCmd;
	static Command gotoMsgCmd;
	
	static Command richTextLinkCmd;
	static Command openImageCmd;
	static Command callItemCmd;
	static Command documentCmd;

	static Command writeCmd;
	static Command chatInfoCmd;
	static Command olderMessagesCmd;
	static Command newerMessagesCmd;
	static Command latestCmd;
	
	static Command sendCmd;
	static Command openTextBoxCmd;
	
	static Command callCmd;
	static Command openChatCmd;
	static Command acceptInviteCmd;
	static Command joinChatCmd;
	static Command leaveChatCmd;

	static Command okCmd;
	static Command cancelCmd;
	
	// ui
	private static Displayable mainDisplayable;
	static Form loadingForm;
	static ChatsList chatsList;
	static FoldersList foldersList;
	private static Form settingsForm;
	private static Vector formHistory = new Vector();

	// auth items
	private static TextField instanceField;
	private static TextField instancePasswordField;
	
	// settings items
	private static ChoiceGroup imagesChoice;
	private static ChoiceGroup avaCacheChoice;
	private static ChoiceGroup uiChoice;
	private static ChoiceGroup behChoice;
	private static Gauge avaCacheGauge;
	private static Gauge photoSizeGauge;
	private static Gauge profileCacheGauge;
	private static Gauge chatsGauge;
	private static Gauge msgsGauge;
	
	// write items
	private static TextField messageField;

	// cache
	private static JSONObject usersCache = new JSONObject();
	private static JSONObject chatsCache = new JSONObject();
	private static Hashtable imagesCache = new Hashtable();
	
	// temp
	private static String richTextUrl;
	private static String writeTo;
	private static String replyTo;
	private static String edit;
	
	protected void destroyApp(boolean u) {
	}

	protected void pauseApp() {
	}

	protected void startApp()  {
		if (midlet != null) return;
		midlet = this;

		version = getAppProperty("MIDlet-Version");
		display = Display.getDisplay(this);
		
		Form f = new Form("mpgram");
		f.append("Loading");
		display.setCurrent(mainDisplayable = f);
		
		String p = System.getProperty("microedition.platform");
		symbianJrt = p != null && p.indexOf("platform=S60") != -1;
		useLoadingForm = !symbianJrt &&
				(System.getProperty("com.symbian.midp.serversocket.support") != null ||
				System.getProperty("com.symbian.default.to.suite.icon") != null);
		
		threadedImages = symbianJrt;
		
		try {
			Class.forName("javax.microedition.shell.MicroActivity");
			f.deleteAll();
			f.addCommand(exitCmd);
			f.setCommandListener(midlet);
			f.append("J2ME Loader is not supported.");
			return;
		} catch (Exception ignored) {}
		
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
			profilesCacheThreshold = j.getInt("profilesCacheThreshold", profilesCacheThreshold);
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
		
		settingsCmd = new Command("Settings", Command.SCREEN, 5);
		aboutCmd = new Command("About", Command.SCREEN, 6);
		
		authCmd = new Command("Auth", Command.ITEM, 1);
		authNextCmd = new Command("Next", Command.OK, 1);
		authCodeCmd = new Command("Next", Command.OK, 1);
		authPasswordCmd = new Command("Next", Command.OK, 1);
		authNewSessionCmd = new Command("New session", Command.SCREEN, 1);
		authImportSessionCmd = new Command("Import session", Command.SCREEN, 2);
		
		logoutCmd = new Command("Logout", Command.ITEM, 1);
		clearCacheCmd = new Command("Clear cache", Command.ITEM, 1);

		foldersCmd = new Command("Folders", Command.SCREEN, 4);
		refreshCmd = new Command("Refresh", Command.SCREEN, 5);
		archiveCmd = new Command("Archived chats", Command.SCREEN, 5);
		contactsCmd = new Command("Contacts", Command.SCREEN, 6);
		searchCmd = new Command("Search", Command.SCREEN, 7);
		
		itemChatCmd = new Command("Open chat", Command.ITEM, 1);
		itemChatInfoCmd = new Command("Profile", Command.ITEM, 2);
		replyMsgCmd = new Command("Reply", Command.ITEM, 3);
		forwardMsgCmd = new Command("Forward", Command.ITEM, 4);
		copyMsgCmd = new Command("Copy message", Command.ITEM, 5);
		messageLinkCmd = new Command("Copy message link", Command.ITEM, 7);
		deleteMsgCmd = new Command("Delete", Command.ITEM, 8);
		editMsgCmd = new Command("Edit", Command.ITEM, 9);
		gotoMsgCmd = new Command("Go to", Command.ITEM, 1);
		
		richTextLinkCmd = new Command("Link", Command.ITEM, 1);
		openImageCmd = new Command("View image", Command.ITEM, 1);
		callItemCmd = new Command("Call", Command.ITEM, 1);
		documentCmd = new Command("Download", Command.ITEM, 1);
		
		writeCmd = new Command("Write message", Command.SCREEN, 5);
		latestCmd = new Command("Refresh", Command.SCREEN, 6);
		chatInfoCmd = new Command("Chat info", Command.SCREEN, 7);
		olderMessagesCmd = new Command("Older", Command.ITEM, 1);
		newerMessagesCmd = new Command("Newer", Command.ITEM, 1);
		
		sendCmd = new Command("Send", Command.OK, 1);
		openTextBoxCmd = new Command("Open text box", Command.ITEM, 1);
		
		callCmd = new Command("Call", Command.SCREEN, 5);
		openChatCmd = new Command("Open chat", Command.SCREEN, 1);
		acceptInviteCmd = new Command("Join", Command.ITEM, 1);
		joinChatCmd = new Command("Join group", Command.SCREEN, 1);
		leaveChatCmd = new Command("Leave group", Command.ITEM, 1);
		
		okCmd = new Command("Ok", Command.OK, 1);
		cancelCmd = new Command("Cancel", Command.CANCEL, 2);
		
		loadingForm = new Form("mpgram");
		loadingForm.append("Loading");
		loadingForm.addCommand(cancelCmd);
		loadingForm.setCommandListener(this);

		start(RUN_IMAGES, null);

		if (threadedImages) {
			start(RUN_IMAGES, null);
			start(RUN_IMAGES, null);
		}
		
		if (user == null || userState < 3) {
			display(mainDisplayable = initialAuthForm());
		} else {
			run = RUN_VALIDATE_AUTH;
			run();
			
			ChatsList l = mainChatsList();
			start(RUN_LOAD_LIST, l);
			display(mainDisplayable = l);
		}
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
				selfId = ((JSONObject) api("me")).getString("id");
				userState = 4;
				
				if (param != null) {
					ChatsList l = mainChatsList();
					start(RUN_LOAD_LIST, l);
					display(mainDisplayable = l);
					writeAuth();
				}
			} catch (APIException e) {
				if (e.code == 401) {
					userState = 0;
					user = null;
					display(errorAlert(e.toString()), mainDisplayable = initialAuthForm());
					break;
				}
				display(errorAlert(e.toString()), null);
			} catch (IOException e) {
				display(errorAlert(e.toString()), null);
			}
			break;
		}
		case RUN_IMAGES: { // avatars loading
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
							if (src instanceof String) {
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
							} else if (src instanceof String[]) {
								String peer = ((String[]) src)[0];
								String id = ((String[]) src)[1];
								String p = ((String[]) src)[3];
								url = instanceUrl + FILE_URL + "?a&c=" + peer + "&m=" + id + "&p=" + p + "&s=" + photoSize;
							} else {
								continue;
							}
							if (img == null) {
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
							}
							
							if (img == null) continue;
							
							// save avatar to hashtable cache
							if (recordName != null && (avatarsCache & 1) == 1) {
								if (imagesCache.size() > avatarsCacheThreshold) {
									imagesCache.clear();
								}
								imagesCache.put(src, img);
							}
							
							if (target instanceof ImageItem) {
								((ImageItem) target).setImage(img);
							} else if (target instanceof Object[]) {
								if (((Object[]) target)[0] instanceof List) {
									List list = ((List) ((Object[]) target)[0]);
									int idx = (((Integer) ((Object[]) target)[1])).intValue();
									list.set(idx, list.getString(idx), img);
								}
							}
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
						// TODO
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
					TextBox t = new TextBox("Code", "", 5, TextField.NUMERIC);
					t.addCommand(authCodeCmd);
					t.setCommandListener(this);
					display(t);
				} else {
					if (userState == 2) {
						// cloud password
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
						// code
						sb.append("completePhoneLogin&code=").append((String) param);
					
						JSONObject j = (JSONObject) api(sb.toString());
						String res = j.getString("res");
						if (j.has("user")) {
							user = j.getString("user");
						}
						if ("password".equals(res)) {
							userState = 2;
							writeAuth();
							TextBox t = new TextBox("Cloud password", "", 100, TextField.NUMERIC);
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

				commandAction(refreshCmd, current);
				display(infoAlert("Deleted"), current);
			} catch (Exception e) {
				display(errorAlert(e.toString()), current);
			}
			break;
		}
		case RUN_SEND_MESSAGE: {
			try {
				StringBuffer sb = new StringBuffer(edit != null ? "editMessage" : "sendMessage");
				sb.append("&peer=").append(writeTo);
				if (edit != null) {
					sb.append("&id=").append(edit);
				}
				if (replyTo != null) {
					sb.append("&reply=").append(replyTo);
				}
				appendUrl(sb.append("&text="), (String) param);
				api(sb.toString());
				
				commandAction(backCmd, current);
				commandAction(latestCmd, current);
				display(infoAlert("Sent"), current);
			} catch (Exception e) {
				display(errorAlert(e.toString()), current);
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
					openChat(id);
					break;
				}
				
				Form f = new ChatInfoForm(id, (String) param, getNameRaw(rawPeer), "chatInvitePeek".equals(type) ? 2 : 3);
				display(f);
				midlet.start(RUN_LOAD_FORM, f);
			} catch (Exception e) {
				display(errorAlert(e.toString()), current);
			}
			break;
		}
		case RUN_IMPORT_INVITE: {
			try {
				ChatInfoForm d = (ChatInfoForm) param;
				MP.api("importChatInvite&id=".concat(d.invite));
				
				commandAction(backCmd, current);
				openChat(d.id);
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
					openChat((String) param);
				} else {
					commandAction(refreshCmd, current);
				}
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

	public void commandAction(Command c, Displayable d) {
		if (d instanceof ChatsList) { // chats list commands
			if (c == archiveCmd) {
				chatsList.changeFolder(1);
				return;
			}
			if (c == foldersCmd) {
				if (foldersList == null) {
					foldersList = new FoldersList();
					start(RUN_LOAD_LIST, foldersList);
				}
				display(foldersList);
				return;
			}
		}
		if (d instanceof ChatForm) { // chat form commands
			if (c == latestCmd) {
				((ChatForm) d).reset();
				((MPForm) d).load();
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
			if (c == searchCmd) {
				// TODO
				return;
			}
		}
		if (d instanceof ChatInfoForm) { // profile commands
			if (c == callCmd) {
				browse("tel:".concat(((ChatInfoForm) d).phone));
				return;
			}
			if (c == openChatCmd) {
				openChat(((ChatInfoForm) d).id);
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
					
					display(loadingAlert("Waiting for server response.."), null);
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
				
				Alert a = new Alert("", "Choose authorization method", null, null);
				a.addCommand(authImportSessionCmd);
				a.addCommand(authNewSessionCmd);
				a.setCommandListener(this);
				
				display(a, null);
				return;
			}
			if (c == authImportSessionCmd) {
				TextBox t = new TextBox("User code", user == null ? "" : user, 200, TextField.NON_PREDICTIVE);
				t.addCommand(cancelCmd);
				t.addCommand(authCmd);
				t.setCommandListener(this);
				
				display(t);
				return;
			}
			if (c == authNewSessionCmd) {
				user = null;
				userState = 0;
				
				TextBox t = new TextBox("Phone number", phone == null ? "" : phone, 30, TextField.PHONENUMBER);
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

					display(loadingAlert("Waiting for server response.."), null);
					CaptchaForm f = new CaptchaForm();
					start(RUN_LOAD_FORM, f);
					return;
				}
				if (d instanceof CaptchaForm) {
					// captcha
					String key = ((CaptchaForm) d).field.getString();
					if (key.length() < 4) {
						display(errorAlert(""), null);
						return;
					}
					display(loadingAlert("Waiting for server response.."), null);
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

				display(loadingAlert("Waiting for server response.."), null);
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

				display(loadingAlert("Waiting for server response.."), null);
				start(RUN_AUTH, pass);
				return;
			}
		}
		{ // settings
			if (c == settingsCmd) {
				if (settingsForm == null) {
					Form f = new Form("Settings");
					f.addCommand(backCmd);
					f.setCommandListener(this);
					StringItem s;
					
					s = new StringItem(null, "UI");
					s.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					s.setFont(largePlainFont);
					f.append(s);
					
					uiChoice = new ChoiceGroup("", Choice.MULTIPLE, new String[] {
							"Reversed chat",
							"Show media"
					}, null);
					uiChoice.setSelectedIndex(0, reverseChat);
					uiChoice.setSelectedIndex(1, showMedia);
					uiChoice.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(uiChoice);
					
					photoSizeGauge = new Gauge("Thumbnails size", true, 64, photoSize / 8);
					photoSizeGauge.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(photoSizeGauge);
					
					chatsGauge = new Gauge("Chats count", true, 50, chatsLimit);
					chatsGauge.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(chatsGauge);
					
					msgsGauge = new Gauge("Messages count", true, 50, messagesLimit);
					msgsGauge.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(msgsGauge);
					
					s = new StringItem(null, "Behaviour");
					s.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					s.setFont(largePlainFont);
					f.append(s);
					
					behChoice = new ChoiceGroup("", Choice.MULTIPLE, new String[] {
							"Wait for page to load"
					}, null);
					behChoice.setSelectedIndex(0, useLoadingForm);
					behChoice.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(behChoice);
					
					imagesChoice = new ChoiceGroup("Images", Choice.MULTIPLE, new String[] {
							"Load media thumbnails",
							"Load avatars",
							"Multi-threaded loading"
					}, null);
					imagesChoice.setSelectedIndex(0, loadThumbs);
					imagesChoice.setSelectedIndex(1, loadAvatars);
					imagesChoice.setSelectedIndex(2, threadedImages);
					imagesChoice.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(imagesChoice);

					avaCacheChoice = new ChoiceGroup("Avatars caching", Choice.POPUP, new String[] {
							"Disabled",
							"Hold in RAM",
							"Store",
							"Hold in RAM & Store"
					}, null);
					avaCacheChoice.setSelectedIndex(avatarsCache, true);
					avaCacheChoice.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(avaCacheChoice);
					
					avaCacheGauge = new Gauge("Avatars cache threshold", true, 20, avatarsCacheThreshold / 5);
					avaCacheGauge.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(avaCacheGauge);
					
					profileCacheGauge = new Gauge("Profiles cache threshold", true, 30, profilesCacheThreshold / 10);
					profileCacheGauge.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(profileCacheGauge);
					
					s = new StringItem(null, "Clear cache", Item.BUTTON);
					s.setDefaultCommand(clearCacheCmd);
					s.setItemCommandListener(this);
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(s);
					
					s = new StringItem(null, "Authorization");
					s.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					s.setFont(largePlainFont);
					f.append(s);
					
					s = new StringItem(null, "Logout", Item.BUTTON);
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
				reverseChat = uiChoice.isSelected(0);
				showMedia = uiChoice.isSelected(1);
				
				if ((photoSize = (photoSizeGauge.getValue() * 8)) < 16) {
					photoSizeGauge.setValue((photoSize = 16) / 8);
				}
				if ((chatsLimit = chatsGauge.getValue()) < 5) {
					chatsGauge.setValue(chatsLimit = 5);
				}
				if ((messagesLimit = msgsGauge.getValue()) < 5) {
					msgsGauge.setValue(messagesLimit = 5);
				}
				
				useLoadingForm = behChoice.isSelected(0);
				
				loadThumbs = imagesChoice.isSelected(0);
				loadAvatars = imagesChoice.isSelected(1);
				threadedImages = imagesChoice.isSelected(2);
				
				avatarsCache = avaCacheChoice.getSelectedIndex();
				avatarsCacheThreshold = avaCacheGauge.getValue() * 5;
				profilesCacheThreshold = profileCacheGauge.getValue() * 10;
				
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
					j.put("profilesCacheThreshold", profilesCacheThreshold);
					
					byte[] b = j.toString().getBytes("UTF-8");
					RecordStore r = RecordStore.openRecordStore(SETTINGS_RECORD_NAME, true);
					r.addRecord(b, 0, b.length);
					r.closeRecordStore();
				} catch (Exception e) {}
			}
			if (c == logoutCmd) {
				userState = 0;
				display(mainDisplayable = initialAuthForm());
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
				commandAction(backCmd, d);
				return;
			}
		}
		{ // write form commands
			if (c == sendCmd) {
				display(loadingAlert("Sending"), d);
				start(RUN_SEND_MESSAGE, messageField.getString());
				return;
			}
			if (c == openTextBoxCmd) {
				TextBox t = new TextBox("Message", messageField.getString(), 500, TextField.ANY);
				t.addCommand(okCmd);
				t.addCommand(cancelCmd);
				t.setCommandListener(this);
				display(t);
				return;
			}
			if (c == okCmd) {
				messageField.setString(((TextBox) d).getString());
				
				c = backCmd;
			}
		}
		if (c == aboutCmd) {
			Form f = new Form("About");
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
			
			s = new StringItem(null, "Server-assisted Telegram client for J2ME, based on MPGram Web API.");
			s.setFont(Font.getDefaultFont());
			s.setLayout(Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			f.append(s);

			s = new StringItem("Developer", "shinovon");
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			f.append(s);

			s = new StringItem("Author", "twsparkle");
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

			s = new StringItem("Donate", "boosty.to/nnproject/donate");
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			s.setDefaultCommand(richTextLinkCmd);
			s.setItemCommandListener(this);
			f.append(s);

			s = new StringItem("Chat", "t.me/nnmidletschat");
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
			return;
		}
		if (c == refreshCmd) {
			if (d instanceof MPForm) {
				((MPForm) d).cancel();
				((MPForm) d).load();
				return;
			}
			if (d instanceof MPList) {
				((MPList) d).cancel();
				((MPList) d).load();
				return;
			}
			return;
		}
		if (c == backCmd || c == cancelCmd) {
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
			openChat(s[2]);
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
			String[] s = (String[]) ((MPForm) current).urls.get(item);
			if (s == null) return;
			copy("", (String) ((MPForm) current).urls.get(s[1]));
			return;
		}
		if (c == messageLinkCmd) {
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
			display(loadingAlert("Loading"), current);
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
			
			ChatForm f = new ChatForm(s[0], null, Integer.parseInt(s[1]), 0);
			display(f);
			start(RUN_LOAD_FORM, f);
			return;
		}
		commandAction(c, display.getCurrent());
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
			if (!peer.has("p")) {
				return;
			}
			id = peer.getString("id");
		}
		
		synchronized (imagesLoadLock) {
			imagesToLoad.addElement(new Object[] { id, target });
			imagesLoadLock.notifyAll();
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
			if (usersCache.size() > profilesCacheThreshold) {
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
			if (chatsCache.size() > profilesCacheThreshold) {
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
		while (i < l && i < 64) {
			char c = s.charAt(i++);
			if (c == '\r') continue;
			if (c != '\n') sb.append(c);
			else sb.append(' ');
		}
		if (i == 64) sb.append("..");
		return sb;
	}
	
	static JSONObject getPeer(String id, boolean now) {
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
			
			if (o == null && now) {
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
	
	static String getName(String id, boolean variant) {
		return getName(id, variant, true);
	}
	
	static String getName(String id, boolean variant, boolean now) {
		if (id == null) return null;
		String res;
		JSONObject o;
		if (id.charAt(0) == '-') {
			o = chatsCache.getObject(id, null);
			if (o == null) {
				o = getPeer(id, now);
			}
			if (o == null) return null;
			res = o.getString("t");
		} else {
			o = usersCache.getObject(id, null);
			if (o == null) {
				o = getPeer(id, now);
			}
			if (o == null) return null;
			res = variant ? getShortName(o) : getName(o);
		}
		return res;
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
		ChatsList l = chatsList = new ChatsList("mpgram", 0);
		l.removeCommand(backCmd);
		l.addCommand(exitCmd);
		l.addCommand(aboutCmd);
		l.addCommand(settingsCmd);
		return l;
	}
	
	static Form initialAuthForm() {
		Form f = new Form("Auth");
		f.addCommand(exitCmd);
		f.addCommand(aboutCmd);
		f.addCommand(settingsCmd);
		f.setCommandListener(midlet);
		
		TextField t = new TextField("Instance URL", instanceUrl, 200, TextField.URL);
		instanceField = t;
		f.append(t);
		
		t = new TextField("Instance Password", instancePassword, 200, TextField.NON_PREDICTIVE);
		instancePasswordField = t;
		f.append(t);
		
		StringItem s = new StringItem(null, "Auth", StringItem.BUTTON);
		s.setDefaultCommand(authCmd);
		s.setItemCommandListener(midlet);
		s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
		f.append(s);
		
		return f;
	}
	
	static Form writeForm(String id, String reply, String text, String editId) {
		writeTo = id;
		replyTo = reply;
		edit = editId;
		
		Form f = new Form(editId != null ? editId : reply != null ? "Reply" : "Write");
		f.setCommandListener(midlet);
		f.addCommand(backCmd);
		f.addCommand(sendCmd);
		
		TextField t = new TextField("Message", text, 500, TextField.ANY);
		f.append(messageField = t);
		
		StringItem s = new StringItem(null, "...", Item.BUTTON);
		s.setDefaultCommand(openTextBoxCmd);
		s.setItemCommandListener(midlet);
		f.append(s);
		
		return f;
	}
	
	static void openUrl(String url) {
		if (!handleDeepLink(url)) {
			midlet.browse(url);
		}
	}
	
	static boolean handleDeepLink(String url) {
		if (url.startsWith("@")) {
			openChat(url.substring(1));
			return true;
		}
		int i;
		String[] query = null;
		boolean tg = false;
		boolean profile = false;
		String domain = null;
		boolean phone = false;
		boolean privat = false;
		String messageId = null;
		String thread = null;
		String invite = null;
		String start = null;
		String text = null;
		String slug = null;
		
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
					privat = true;
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
					slug = s[1];
				} else if ("addemoji".equals(s[0])) {
					slug = s[1];
				} else if ("joinchat".equals(s[0])) {
					invite = s[1];
				} else if ("addlist".equals(s[0])) {
					slug = s[1];
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
						|| "join".equals(url)) {
					tg = true;
					privat = "privatepost".equals(url);
//				} else if ("addlist".equals(url)) {
//				} else if ("addstickers".equals(url)) {
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
							text = query[n].substring(5);
							continue;
						}
						if (query[n].startsWith("invite=")) {
							invite = query[n].substring(7);
							continue;
						}
						if (query[n].startsWith("slug=")) {
							slug = query[n].substring(5);
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
						openProfile(invite, null, 2);
						
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
							display(f);
							midlet.start(RUN_LOAD_FORM, f);
						}
						return true;
					}
				} else if (invite != null) {
					// resolve invite
					openProfile(invite, null, 1);
					
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
	
	static void openChat(String id) {
		Form f = new ChatForm(id, null, 0, 0);
		display(f);
		midlet.start(RUN_LOAD_FORM, f);
	}
	
	static void openProfile(String id, ChatForm chatForm, int mode) {
		if (chatForm == null && current instanceof ChatForm && id.equals(((ChatForm) current).id)) {
			chatForm = (ChatForm) current;
		}
		Form f = new ChatInfoForm(id, chatForm, mode);
		display(f);
		midlet.start(RUN_LOAD_FORM, f);
	}
	
	static void copy(String title, String text) {
		// TODO use nokiaui?
		TextBox t = new TextBox(title, text, text.length() + 1, TextField.UNEDITABLE);
		t.addCommand(backCmd);
		t.setCommandListener(midlet);
		display(t);
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
		if (d == null || d == mainDisplayable) {
			d = mainDisplayable;
			
			formHistory.removeAllElements();
			imagesToLoad.removeAllElements();
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
		try {
			String t = instanceUrl.concat(API_URL + "?v=" + API_VERSION + "&method=").concat(url);
			hc = openHttpConnection(t);
			hc.setRequestMethod("GET");
			
			int c = hc.getResponseCode();
			if (c == 502 || c == 504) {
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

	static Image getImage(String url) throws IOException {
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
		HttpConnection hc = (HttpConnection) Connector.open(url);
		hc.setRequestProperty("User-Agent", "mpgram4/".concat(version));
		if (url.startsWith(instanceUrl)) {
			if (user != null) {
				hc.setRequestProperty("X-mpgram-user", user);
			}
			hc.setRequestProperty("X-mpgram-unicode", "1");
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
		if ((state == null || (state[RT_PRE] == 0 && state[RT_URL] == 0))
				&& (text.indexOf("http://") != -1 || text.indexOf("https://") != -1
				|| text.indexOf('#') != -1|| text.indexOf('@') != -1)) {
			int i, j, k, d = 0;
			while (true) {
				boolean b = false;
				i = text.indexOf("://", d);
				j = text.indexOf('#', d);
				k = text.indexOf('@', d);
				if (i == -1 && j == -1 && k == -1) break;
				
				if (k != -1 && (j == -1 || j > k)) {
					j = k;
				}
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
		
		while (space-- != 0) {
			form.safeInsert(thread, insert++, new Spacer(f.charWidth(' '), f.getBaselinePosition()));
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

}
