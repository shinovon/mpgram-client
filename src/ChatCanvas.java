/*
Copyright (c) 2025 Arman Jussupgaliyev

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
//#ifndef NO_CHAT_CANVAS
import java.io.DataInputStream;
import java.util.Hashtable;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import javax.microedition.lcdui.Ticker;

public class ChatCanvas extends Canvas implements MPChat, LangConstants, Runnable {
	
	// colors enum
	static final int COLOR_CHAT_BG = 0;
	static final int COLOR_CHAT_FG = 1;
	static final int COLOR_CHAT_HIGHLIGHT_BG = 2;
	static final int COLOR_CHAT_PANEL_BG = 3;
	static final int COLOR_CHAT_PANEL_FG = 4;
	static final int COLOR_CHAT_PANEL_BORDER = 5;
	static final int COLOR_CHAT_MENU_BG = 6;
	static final int COLOR_CHAT_MENU_HIGHLIGHT_BG = 7;
	static final int COLOR_CHAT_MENU_FG = 8;
	static final int COLOR_CHAT_STATUS_FG = 9;
	static final int COLOR_CHAT_STATUS_HIGHLIGHT_FG = 10;
	static final int COLOR_CHAT_POINTER_HOLD = 11;
	static final int COLOR_CHAT_INPUT_ICON = 12;
	static final int COLOR_CHAT_SEND_ICON = 13;
	static final int COLOR_CHAT_INPUT_BORDER = 14;
	
	static int[] colors = new int[40];
	static int[] colorsCopy;
	static int[] style = new int[20];
	
//	static Image attachIcon;

	boolean loaded, finished, canceled;
	Thread thread;
	
	String id;
	String username;
	String query;
	String startBot;
	String title;
	String mediaFilter;
	
	int limit = MP.messagesLimit;
	int addOffset, offsetId;
	int messageId, topMsgId;

	boolean infoLoaded;
	boolean left, broadcast, forum;
	boolean canWrite, canDelete, canBan, canPin;
	
	int dir;
	int firstMsgId, lastMsgId;
	boolean endReached, hasOffset;
	private int idOffset;
	int readOutboxId;
	
	boolean switched;
	boolean update, shouldUpdate;
	
	boolean selfChat;
	boolean user;
	boolean channel;
	boolean bot;
	boolean reverse;
	
	MPChat parent;
	
	// discussion
	String postPeer, postId;
	ChatTopicsList topicsList;
	JSONArray topics;

	long typing;
	private final Object typingLock = new Object();
	private Thread typingThread;
	long wasOnline;
	String status, defaultStatus;
	
	JSONObject botAnswer;
	
	Hashtable table;
	
	int count;
	UIItem firstItem, lastItem;
	UIItem layoutStart;
	
	// boundaries
	int width, height;
	int clipHeight, top, bottom;
	int contentHeight;
	
	// scroll
	int scroll;
	int scrollTarget;
	int lastScrollDir;
	int lastDragDir;
	UIItem scrollCurrentItem, scrollTargetItem;
	UIItem focusedItem;
	UIItem nextFocusItem;
	float kineticScroll;
	
	// pointer
	int pressX, pressY, pointerX, pointerY;
	boolean pressed, dragging, longTap, contentPressed, draggingHorizontally;
	int dragged;
	long pressTime;
	int dragXHold, dragYHold;
	UIItem pointedItem, heldItem;
	boolean startSelectDir;
	
	static final int moveSamples = 10;
	int[] moves = new int[moveSamples];
	long[] moveTimes = new long[moveSamples];
	int movesIdx;
	
	long lastPaintTime;
	boolean animating;
	
	// animations
	boolean funcFocused;
	int bottomAnimTarget = -1;
	float bottomAnimProgress;
	boolean keyGuide;
	long keyGuideTime;
	
	int menuAnimTarget = -1;
	float menuAnimProgress;
	
	boolean loading;
	
	boolean touch = !MP.forceKeyUI && hasPointerEvents();
	
	// menu
	boolean menuFocused;
	UIItem menuItem;
	int[] menu;
	int menuCurrent, menuCount;
	
	String titleRender;
	
	int selected;
	
	boolean arrowShown;
	boolean skipRender;
	
	// input
	boolean hasInput;
	String text;
	int replyMsgId;
	int editMsgId;
	String file;
	static Keyboard keyboard;
	Object nokiaEditor;
	boolean updateEditor;
	int inputFieldHeight;
	boolean inputFocused;
	UIMessage[] forwardMsgs;
	String forwardPeer, forwardMsg;
	boolean editorShown;
	boolean funcWasFocused;
	
	ChatCanvas() {
		setFullScreenMode(true);
		
		if (colorsCopy == null) {
			try {
				DataInputStream d = new DataInputStream(getClass().getResourceAsStream("/c/".concat(MP.theme)));
				d.readUTF();
				for (int i = 0; i < 40; ++i) {
					colors[i] = d.readInt();
				}
				for (int i = 0; i < 20; ++i) {
					style[i] = d.readByte() & 0xFF;
				}
				d.close();
			} catch (Exception e) {
				colors[COLOR_CHAT_BG] = 0x0E1621;
				colors[COLOR_CHAT_FG] = 0xFFFFFF;
				colors[COLOR_CHAT_HIGHLIGHT_BG] = 0x1A3756;
				colors[COLOR_CHAT_PANEL_BG] = 0x17212B;
				colors[COLOR_CHAT_PANEL_FG] = 0xFFFFFF;
				colors[COLOR_CHAT_PANEL_BORDER] = 0x0A121B;
				colors[COLOR_CHAT_MENU_BG] = 0x17212B;
				colors[COLOR_CHAT_MENU_HIGHLIGHT_BG] = 0x232E3C;
				colors[COLOR_CHAT_MENU_FG] = 0xFFFFFF;
				colors[COLOR_CHAT_STATUS_FG] = 0x708499;
				colors[COLOR_CHAT_STATUS_HIGHLIGHT_FG] = 0x73B9F5;
				colors[COLOR_CHAT_POINTER_HOLD] = 0xFFFFFF;
				colors[COLOR_CHAT_INPUT_ICON] = 0x6A7580;
				colors[COLOR_CHAT_SEND_ICON] = 0x5288C1;
				colors[COLOR_CHAT_INPUT_BORDER] = 0x01C272D;
				
				colors[UIMessage.COLOR_MESSAGE_BG] = 0x182533;
				colors[UIMessage.COLOR_MESSAGE_OUT_BG] = 0x2B5278;
				colors[UIMessage.COLOR_MESSAGE_FG] = 0xFFFFFF;
				colors[UIMessage.COLOR_MESSAGE_LINK] = 0x71BAFA;
				colors[UIMessage.COLOR_MESSAGE_LINK_FOCUS] = 0xABABAB;
				colors[UIMessage.COLOR_MESSAGE_SENDER] = 0x71BAFA;
				colors[UIMessage.COLOR_MESSAGE_ATTACHMENT_BORDER] = 0x6AB3F3;
				colors[UIMessage.COLOR_MESSAGE_ATTACHMENT_TITLE] = 0xFFFFFF;
				colors[UIMessage.COLOR_MESSAGE_ATTACHMENT_SUBTITLE] = 0x7DA8D3;
				colors[UIMessage.COLOR_MESSAGE_ATTACHMENT_FOCUS_BG] = 0x1A3756;
				colors[UIMessage.COLOR_MESSAGE_COMMENT_BORDER] = 0x31404E;
				colors[UIMessage.COLOR_MESSAGE_IMAGE] = 0xABABAB;
				colors[UIMessage.COLOR_MESSAGE_FOCUS_BORDER] = 0xFFFFFF;
				colors[UIMessage.COLOR_MESSAGE_TIME] = 0x6D7F8F;
				colors[UIMessage.COLOR_MESSAGE_OUT_TIME] = 0x7DA8D3;
				colors[UIMessage.COLOR_ACTION_BG] = 0x1E2C3A;
				
				style[UIMessage.STYLE_MESSAGE_FILL] = 1;
				style[UIMessage.STYLE_MESSAGE_ROUND] = 1;
				style[UIMessage.STYLE_MESSAGE_BORDER] = 0;
			}
			
			colorsCopy = new int[colors.length];
			System.arraycopy(colors, 0, colorsCopy, 0, colors.length);
		}
		
		if (touch) {
			top = MP.smallBoldFontHeight + MP.smallPlainFontHeight + 8;
		} else {
			top = MP.smallBoldFontHeight + 4 + (MP.chatStatus && mediaFilter == null ? MP.smallPlainFontHeight + 4 : 0);
		}
		
		// initialize keyboard
		switch (MP.textMethod) {
		case 0: // auto
		case 1: // nokiaui
//#ifndef NO_NOKIAUI
			if (touch) {
				try {
					nokiaEditor = NokiaAPI.createTextEditor(500, TextField.ANY, 40, 40);
					if (nokiaEditor != null) {
						NokiaAPI.TextEditor_setContent(nokiaEditor, "");
					}
				} catch (Throwable ignored) {}
				if (nokiaEditor != null) {
					updateEditor = true;
					keyboard = null;
					break;
				}
			}
//#endif
			if (MP.textMethod == 1) break;
		case 2: // j2mekeyboard
			nokiaEditor = null;
			if (keyboard == null) {
				// do not use multiline in j2mekeyboard as it's too broken
				keyboard = Keyboard.getKeyboard(this, false, getWidth(), getHeight());
				
				keyboard.setTextColor(colors[COLOR_CHAT_PANEL_FG]);
				keyboard.setTextHintColor(colors[COLOR_CHAT_INPUT_ICON]);
				keyboard.setCaretColor(colors[COLOR_CHAT_PANEL_FG]);
				keyboard.setTextHint(MP.L[LTextField_Hint]);
				keyboard.setLanguages(MP.inputLanguages);
			} else {
				keyboard.setListener(this);
				keyboard.reset();
			}
			break;
		case 3: // textbox
			keyboard = null;
			nokiaEditor = null;
		}
	}
	
	public ChatCanvas(String id, String query, int message, int topMsg) {
		this();
		this.id = id;
		this.query = query;
		this.messageId = message;
		this.topMsgId = topMsg;
		init(query == null);
	}
	
	// forward multiple message
	public ChatCanvas(String id, int topMsg, UIMessage[] forward) {
		this();
		this.id = id;
		this.topMsgId = topMsg;
		this.forwardMsgs = forward;
		init(query == null);
	}
	
	// forward one message
	public ChatCanvas(String id, int topMsg, String forwardPeerId, String forwardMsgId) {
		this();
		this.id = id;
		this.topMsgId = topMsg;
		this.forwardPeer = forwardPeerId;
		this.forwardMsg = forwardMsgId;
		init(query == null);
	}
	
	
	// create in media mode
	public ChatCanvas(String id, String mediaFilter, int topMsg) {
		this();
		this.id = id;
		if (mediaFilter == null) mediaFilter = "Photos";
		this.mediaFilter = mediaFilter;
		this.topMsgId = topMsg;
		init(false);
	}
	
	// post discussion
	public ChatCanvas(String id, String postPeer, String postId, int readMaxId) {
		this();
		this.id = id;
		this.postPeer = postPeer;
		this.postId = postId;
		this.messageId = readMaxId;
		init(true);
	}
	
	private void init(boolean input) {
		hasInput = input;
	}

	public void load() {
		if (loaded) return;
		loaded = true;
		canceled = finished = false;
		selected = 0;
		
		loading = true;
		Thread thread = this.thread = Thread.currentThread();
		try {
			// remove all
			count = 0;
			firstItem = lastItem = null;
			scrollCurrentItem = scrollTargetItem = focusedItem = null;
			titleRender = null;
			
			if (!MP.globalUpdates && (MP.reopenChat || (query == null && mediaFilter == null))
					&& MP.chatUpdates
					&& (MP.updatesThread != null || MP.updatesRunning)) {
				MP.display(MP.loadingAlert(MP.L[LWaitingForPrevChat]), this);
				
				// TODO check if cancel is already in progress
				MP.cancel(MP.updatesThreadCopy, true);
				while (MP.updatesThread != null || MP.updatesRunning) {
					Thread.sleep(1000L);
				}
				
				if (MP.current == this) {
					MP.display(MP.useLoadingForm ? (Displayable) MP.loadingForm : this);
				}
			}
			
			if (mediaFilter == null && query == null && postPeer == null) {
				JSONObject dialog = ((JSONObject) MP.api("getDialog&id=".concat(id))).getObject("res");
				if (messageId == -1 && dialog.has("read_inbox_max_id")) {
					messageId = 0;
					int maxId = dialog.getInt("read_inbox_max_id");
					if (maxId != 0) {
						messageId = maxId;
						if (dialog.getInt("unread_count", 0) > limit) {
							offsetId = maxId;
							addOffset = -limit;
							dir = 1;
						} else {
							offsetId = -1;
						}
					}
				}
				if (dialog.has("read_outbox_max_id")) {
					readOutboxId = dialog.getInt("read_outbox_max_id");
				}
			}
			
			StringBuffer sb = new StringBuffer();
			if (!infoLoaded) {
				if (postPeer != null) {
					sb.append("getDiscussionMessage&peer=").append(postPeer)
					.append("&id=").append(postId);
					JSONObject j = (JSONObject) MP.api(sb.toString());
					id = j.getString("peer_id");
					topMsgId = j.getInt("id");
					if (messageId == 0) {
						messageId = j.getInt("read");
					} else if (messageId != 0 && j.getInt("unread", 0) > limit) {
						offsetId = messageId = j.getInt("read");
						addOffset = -limit;
						dir = 1;
					} else {
						messageId = 0;
					}
					sb.setLength(0);
				}
				
				JSONObject peer = MP.getPeer(id, true);
				
				left = peer.getBoolean("l", false);
				broadcast = peer.getBoolean("c", false);
				forum = peer.getBoolean("f", false);
				id = peer.getString("id");
				username = peer.getString("name", null);
				channel = MP.ZERO_CHANNEL_ID >= Long.parseLong(id);
				title = MP.getName(id, false);
	
				if (mediaFilter == null) {
					canWrite = !broadcast;
					
					JSONObject info = (JSONObject) MP.api((!user && !forum ? "getFullInfo&id=" : "getInfo&id=").concat(id));
					JSONObject full = info.getObject("full", null);
					
					if (id.charAt(0) == '-') {
						JSONObject chat = info.getObject("Chat");
						if (chat.has("admin_rights")) {
							JSONObject adminRights = chat.getObject("admin_rights");
							canWrite = !broadcast || adminRights.getBoolean("post_messages", false);
							canDelete = adminRights.getBoolean("delete_messages", false);
							canBan = !broadcast && adminRights.getBoolean("ban_users", false);
							canPin = adminRights.getBoolean("pin_messages", false);
						}
						
						if (forum && topMsgId == 0) {
							ChatTopicsList list = new ChatTopicsList(this, title);
							
							JSONArray topics = ((JSONObject) MP.api("getForumTopics&peer=".concat(id))).getArray("res");
							int l = topics.size();
							for (int i = 0; i < l; i++) {
								JSONObject topic = topics.getObject(i);
								// TODO
								list.append(topic.getString("title", "General"), null);
							}
							
							if (thread != this.thread) throw MP.cancelException;
							MP.deleteFromHistory(this);
							MP.display(list);
							
							this.topics = topics;
							this.topicsList = list;
							infoLoaded = true;
							return;
						}
						
						if (full != null && full.has("participants_count")) {
							defaultStatus = MP.localizePlural(full.getInt("participants_count"),
									broadcast ? L_subscriber : L_member);
						}
					} else {
						user = true;
						canPin = true;
						canDelete = true;
						if (bot = info.getObject("User").getBoolean("bot", false)) {
							defaultStatus = MP.L[LBot];
						}
						if (MP.chatStatus && info.getObject("User").has("status")) {
							setStatus(info.getObject("User").getObject("status"));
						}
					}
				}
				infoLoaded = true;
			}

			this.selfChat = MP.selfId.equals(id);
			this.reverse = MP.reverseChat && mediaFilter == null;
			
			if (selfChat) {
				title = MP.L[LSavedMessages];
			} else if (postId != null || topMsgId != 0) {
				title = MP.L[LComments];
			}
			
			if (startBot != null) {
				try {
					sb.append("startBot&id=").append(id);
					MP.appendUrl(sb.append("&start="), startBot);
					
					MP.api(sb.toString());
				} catch (Exception e) {
					e.printStackTrace();
				}
				sb.setLength(0);
			}

			if (messageId == -1) messageId = 0;
			if (messageId != 0 && offsetId == 0) {
				// message to focus
				offsetId = messageId;
				addOffset = -1;
			}

			if (query != null || topMsgId != 0 || mediaFilter != null) {
				sb.append("searchMessages");
				if (mediaFilter != null) {
					sb.append("&filter=").append(mediaFilter);
					setTitle(MP.L[LChatMedia_Title]);
				}
				if (query != null) {
					if (mediaFilter == null) {
						setTitle(MP.L[LSearch_TitlePrefix].concat(title));
					}
					MP.appendUrl(sb.append("&q="), query);
				}
			} else {
				sb.append("getHistory&read=1");
			}
			
			sb.append("&media=1&peer=").append(id);
			if (limit != 0) {
				sb.append("&limit=").append(limit);
			}
			if (addOffset != 0) {
				sb.append("&add_offset=").append(addOffset);
			}
			if (offsetId > 0) {
				sb.append("&offset_id=").append(offsetId);
			}
			if (topMsgId != 0) {
				sb.append("&top_msg_id=").append(topMsgId);
			}
			
			if (thread != this.thread) throw MP.cancelException;
			
			JSONObject j = (JSONObject) MP.api(sb.toString());
			MP.fillPeersCache(j);
			
			if (thread != this.thread) throw MP.cancelException;
			
			idOffset = j.getInt("off", Integer.MIN_VALUE);
			if (idOffset != Integer.MIN_VALUE && addOffset < 0) {
				idOffset += addOffset;
			}
			endReached = idOffset == 0 || (idOffset == Integer.MIN_VALUE && addOffset <= 0);
			hasOffset = addOffset > 0 || offsetId > 0;
			
			JSONArray messages = j.getArray("messages");
			int l = messages.size();
			table = new Hashtable();
			
			if (!endReached && hasOffset) {
				add(new UIPageButton(1));
			}
			
			for (int i = 0; i < l; i++) {
				JSONObject message = messages.getObject(i);
				int id = message.getInt("id");
				if (i == 0) {
					firstMsgId = id;
				} else if (i == l - 1) {
					lastMsgId = id;
				}
				do {
					try {
						safeAdd(thread, new UIMessage(message, this),
								this.messageId != 0 ? (messageId == id)
								: (i == 0 ? ((endReached && dir == 0) || dir == -1) : (i == l - 1 && dir == 1)));
					} catch (OutOfMemoryError e) {
						MP.gc();
						continue;
					}
				} while (false);
			}
			
			if (l == limit && j.has("count")) {
				add(new UIPageButton(-1));
			}
			
			finished = true;
	
			if (thread != this.thread) return;
			
			// postLoad
			loading = false;
			if (hasInput && (canWrite || left) && mediaFilter == null && query == null) {
				inputFieldHeight = Math.max(MP.medPlainFontHeight + 16, 40);
				if (forwardMsgs != null || forwardMsg != null) {
					bottom = inputFieldHeight + MP.smallBoldFontHeight + 8;
					if (!touch) inputFocused = true;
				} else if (touch) {
					bottom = inputFieldHeight;
				}
			} else {
				bottom = 0;
			}
			layoutStart = firstItem;
			if (endReached && !hasOffset
					&& query == null && mediaFilter == null
					&& MP.chatUpdates && !update) {
				// start updater thread
				update = shouldUpdate = true;
				if (!MP.globalUpdates) {
					MP.midlet.start(MP.RUN_CHAT_UPDATES, this);
				}
				(typingThread = new Thread(this)).start();
			}
			if (botAnswer != null) {
				JSONObject b = botAnswer;
				botAnswer = null;
				handleBotAnswer(b);
			}
//#ifndef NO_NOTIFY
//#ifndef NO_NOKIAUI
			try {
				Notifier.remove(id);
			} catch (Throwable ignored) {}
			MP.notificationMessages.remove(id);
//#endif
//#endif
			MP.display(this);
			queueRepaint();
		} catch (Exception e) {
			if (e == MP.cancelException || canceled || this.thread != thread) {
				// ignore exception if cancel detected
				return;
			}
			MP.display(MP.errorAlert(e), this);
			e.printStackTrace();
		} finally {
			if (this.thread == thread) {
				loading = false;
				this.thread = null;
			}
		}
	}
	
	public void closed(boolean destroy) {
		if (destroy) cancel();
		closeMenu();
//#ifndef NO_NOKIAUI
		if (nokiaEditor != null) {
			try {
				NokiaAPI.TextEditor_setFocus(nokiaEditor, false);
				NokiaAPI.TextEditor_setVisible(nokiaEditor, false);
				NokiaAPI.TextEditor_setParent(nokiaEditor, null);
			} catch (Throwable ignored) {}
		}
//#endif
		if (destroy && keyboard != null && keyboard.isVisible()) {
			keyboard.reset();
			keyboard.hide();
			keyboard.setListener(null);
		}
	}
	
	public void showNotify() {
		skipRender = false;
		if (!touch && keyGuideTime == 0) {
			keyGuide = true;
			bottomAnimTarget = MP.smallBoldFontHeight + 2;
		}
		if (shouldUpdate && !update && !loading) {
			update = true;
			if (!MP.globalUpdates) {
				MP.midlet.start(MP.RUN_CHAT_UPDATES, this);
			}
			if (typingThread == null) {
				(typingThread = new Thread(this)).start();
			}
		}
//#ifndef NO_NOKIAUI
		if (nokiaEditor != null) {
			NokiaAPI.TextEditor_setParent(nokiaEditor, this);
			updateEditor = true;
		}
//#endif
		if (keyboard != null) {
			keyboard.setListener(this);
		}
		repaint();
	}
	
	void cancel() {
		// close updater thread
		if (update) {
			update = false;
			if (!MP.globalUpdates && (MP.updatesThread != null || MP.updatesRunning)) {
				MP.cancel(MP.updatesThread, true);
			}
			if (typingThread != null) typingThread.interrupt();
		}
		
		shouldUpdate = false;
		loaded = false;
		if (finished || thread == null) return;
		canceled = true;
		MP.cancel(thread, false);
		thread = null;
	}
	
	// Canvas

	protected void paint(Graphics g) {
		int w = getWidth(), h = getHeight();
		if (keyboard != null && keyboard.isVisible()) {
			h -= keyboard.paint(g, w, h);
		}
		g.setClip(0, 0, w, h);
		
//		if (loading) {
//			g.setColor(colors[COLOR_CHAT_BG]);
//			g.fillRect(0, 0, w, h);
//			g.setColor(colors[COLOR_CHAT_FG]);
//			g.setFont(MP.medPlainFont);
//			g.drawString(MP.L[LLoading], w >> 1, h >> 1, Graphics.TOP | Graphics.HCENTER);
//			return;
//		}
		
		int contentHeight = this.contentHeight;
		
		long now = System.currentTimeMillis();
		long deltaTime = now - lastPaintTime;
		if (deltaTime > 500) deltaTime = 500;
		if (width != w) {
			layoutStart = firstItem;
			titleRender = null;
		}
		width = w; height = h;
		
		boolean animate = false;

		// animations
		
		if (bottomAnimTarget != -1) {
			if (slide(1, bottomAnimProgress, bottomAnimTarget, deltaTime)) {
				animate = true;
			} else {
				bottomAnimProgress = bottom = bottomAnimTarget;
				bottomAnimTarget = -1;
			}
		}
		if (menuAnimTarget != -1) {
			if (slide(2, menuAnimProgress, menuAnimTarget, deltaTime)) {
				animate = true;
			} else {
				menuAnimProgress = menuAnimTarget;
				menuAnimTarget = -1;
				updateEditor = true;
			}
		}
		
		int clipHeight = this.clipHeight = h - top - bottom;
		
		if (!loading) {
			// layout
			if (layoutStart != null) {
				UIItem idx = layoutStart;
				layoutStart = null;
				layout(idx, w, clipHeight);
				contentHeight = this.contentHeight;
			}
			
			if (nextFocusItem != null && nextFocusItem.layoutWidth != 0) {
				focusItem(nextFocusItem, touch ? 0 : reverse ? -1 : 1);
				if (!isVisible(nextFocusItem)) scrollTo(nextFocusItem);
				nextFocusItem = null;
			}
			
			// key scroll
			
			if (scrollTarget != -1) {
				if (contentHeight <= clipHeight) {
					scroll = 0;
					scrollTarget = -1;
				} else {
					if (slide(0, scroll, scrollTarget, deltaTime)) {
						animate = true;
					} else {
						scroll = scrollTarget;
						scrollTarget = -1;
					}
					if (scroll <= 0) {
						scroll = 0;
						scrollTarget = -1;
						animate = false;
					} else if (scroll >= contentHeight - clipHeight) {
						scroll = contentHeight - clipHeight;
						scrollTarget = -1;
						animate = false;
					}
				}
			}
			
			if (!touch && scrollTarget == -1) {
				if (focusedItem == null && scrollCurrentItem == null && scrollTargetItem == null) {
					int d = lastScrollDir;
					if (d == 0) d = 1;
					UIItem item = getItemAt(height >> 1);
					if (item == null || !item.focusable) {
						item = getFirstFocusableItemOnScreen(null, d, clipHeight / 4);
					}
					if (item != null) focusItem(item, d);
				} else if (focusedItem != null && scrollCurrentItem == null && !isVisible(focusedItem)) {
					scrollTo(focusedItem);
				}
			}
			
			// touch scroll
			
			if (kineticScroll != 0) {
				if ((kineticScroll > -1 && kineticScroll < 1)
						|| contentHeight <= clipHeight
						|| scroll <= 0
						|| scroll >= contentHeight - clipHeight) {
					kineticScroll = 0;
				} else {
					float mul = pressed && !dragging ? 0.5f : 0.965f;
					scroll += (int) kineticScroll;
					kineticScroll *= mul;
					float f;
					if ((f = deltaTime > 33 && animating ? deltaTime / 33f : 1f) >= 2) {
						int j = (int) f - 1;
						for (int i = 0; i < j; i++) {
							scroll += (int) kineticScroll;
							kineticScroll *= mul;
						}
					}
					animate = true;
				}
			}
		}
		
		// limit scroll
		int scroll = this.scroll;
		if (scroll < 0) this.scroll = scroll = 0;
		else if (contentHeight <= clipHeight) this.scroll = scroll = 0;
		else if (scroll > contentHeight - clipHeight) this.scroll = scroll = contentHeight - clipHeight;
		
		if (!skipRender || !menuFocused) {
			// background
			g.setColor(colors[COLOR_CHAT_BG]);
			g.fillRect(0, 0, w, h);
			g.setColor(colors[COLOR_CHAT_FG]);
			
			// render items
			
			UIItem item = firstItem;
			if (item != null && !loading) {
				int x = 0;
				if (pressed && draggingHorizontally && selected == 0) {
					int d = pointerX - pressX;
					if (d > 0) {
						x = d;
					} else if (pointedItem instanceof UIMessage) {
						x = Math.max(-100, Math.min(0, d));
					}
				}
				
				g.setClip(0, top, w, clipHeight);
				if (reverse) {
					clipHeight += top;
					int y = h - bottom + scroll;
					do {
						if (y < 0) break;
						y -= item.contentHeight;
						if (y > clipHeight) continue;
						item.paint(g, pointedItem == item || x > 0 ? x : 0, y, w);
					} while ((item = item.next) != null);
				} else {
					int y = top - scroll;
					clipHeight += top;
					do {
						int ih = item.contentHeight;
						if (y < -ih) {
							y += ih;
							continue;
						}
						item.paint(g, pointedItem == item || x > 0 ? x : 0, y, w);
						if ((y += ih) > clipHeight) break;
					} while ((item = item.next) != null);
				}
			}
			
			g.setClip(0, 0, w, h);
			
			// top panel
			if (top != 0) {
				int th = top;
				g.setColor(colors[COLOR_CHAT_PANEL_BG]);
				g.fillRect(0, 0, w, th);
				g.setColor(colors[COLOR_CHAT_PANEL_BORDER]);
				g.drawLine(0, th, w, th);
				
				int tx = 4;
				int tw = w - 8;
				if (touch) {
					g.setColor(colors[COLOR_CHAT_PANEL_FG]);
					tx = 40;
					tw = w - 80;
					int bty = (th - 2) >> 1;
					if (selected != 0) g.setColor(colors[COLOR_CHAT_PANEL_FG]);
					// back button
					g.drawLine(12, bty, 28, bty);
					g.drawLine(12, bty, 20, bty-8);
					g.drawLine(12, bty, 20, bty+8);
					
					if (loading) { // do nothing
					} else if (selected != 0) {
						// selected messages options
						
						// delete
						g.drawLine(w - 29, bty - 8, w - 13, bty + 8);
						g.drawLine(w - 29, bty + 8, w - 13, bty - 8);
						
						// forward
						g.drawLine(w - 52, bty, w - 68, bty);
						g.drawLine(w - 68, bty, w - 68, bty + 6);
						g.drawLine(w - 58, bty - 6, w - 52, bty);
						g.drawLine(w - 58, bty + 6, w - 52, bty);
						
					} else if (/*query == null && */ mediaFilter == null) {
						// menu button
						g.fillRect(w - 22, bty - 6, 3, 3);
						g.fillRect(w - 22, bty, 3, 3);
						g.fillRect(w - 22, bty + 6, 3, 3);
					}
				}
				boolean medfont = (MP.chatStatus && mediaFilter == null) || touch;
				if (selected != 0 || mediaFilter != null || loading) {
					g.setFont(medfont ? MP.medPlainFont : MP.smallPlainFont);
					g.setColor(colors[COLOR_CHAT_PANEL_FG]);
					g.drawString(loading ? MP.L[LLoading] : selected != 0 ? Integer.toString(selected) : mediaFilter /* TODO unlocalized */, tx, medfont ? ((th - MP.medPlainFontHeight) >> 1) : 2, 0);
				} else {
					boolean hideStatus = medfont && (selfChat || postId != null || query != null);
					if (title != null) {
						Font font = hideStatus ? MP.medPlainFont : MP.smallBoldFont;
						if (titleRender == null) {
							titleRender = UILabel.ellipsis(query != null ? MP.L[LSearch] : title, font, tw - 4);
						}
						g.setColor(colors[COLOR_CHAT_PANEL_FG]);
						g.setFont(font);
						g.drawString(titleRender, tx, medfont ? (hideStatus ? (th - MP.medPlainFontHeight) >> 1 : 4) : 2, 0);
					}
					// TODO status ellipsis
					if (medfont && !hideStatus) {
						g.setColor(colors[typing != 0 ? COLOR_CHAT_STATUS_HIGHLIGHT_FG : COLOR_CHAT_STATUS_FG]);
						g.setFont(MP.smallPlainFont);
						String status = this.status;
						if (status == null) {
							status = this.defaultStatus;
						}
						if (status != null) {
							g.drawString(status, tx, 4 + MP.smallBoldFontHeight, 0);
						}
					}
				}
			}
			
			// bottom panel
			if (bottom != 0) {
				g.setFont(MP.smallBoldFont);
				g.setColor(colors[COLOR_CHAT_PANEL_BG]);
				int by = h - bottom;
				int ih = inputFieldHeight;
				int iy = h - ih;
				g.fillRect(0, by, w, bottom);
				g.setColor(colors[COLOR_CHAT_PANEL_BORDER]);
				g.drawLine(0, by, w, by);
				g.setColor(colors[COLOR_CHAT_PANEL_FG]);
				if (selected != 0 || loading) {
					if (!touch) {
						if (!loading)
							g.drawString(MP.L[LMenu], 2, by + 1, Graphics.TOP | Graphics.LEFT);
						g.drawString(MP.L[LCancel], w - 2, by + 1, Graphics.TOP | Graphics.RIGHT);
					}
				} else if (keyGuide) {
					animate = true;
					g.drawString(MP.L[LMenu], 2, by + 1, Graphics.TOP | Graphics.LEFT);
					g.drawString(MP.L[LChat], w - 2, by + 1, Graphics.TOP | Graphics.RIGHT);
					if (keyGuideTime == 0) {
						keyGuideTime = now;
					} else if (now - keyGuideTime > 3000) {
						bottomAnimTarget = 0;
						keyGuide = false;
					}
				} else if (touch || inputFocused) {
					if (!touch && inputFocused) {
						int bh = 4 + MP.smallBoldFontHeight;
						g.setColor(colors[COLOR_CHAT_INPUT_BORDER]);
						g.drawLine(0, h - bh, w, h - bh);
						g.setColor(colors[COLOR_CHAT_PANEL_FG]);
						
						iy -= bh;
						bh -= 1;
						g.drawString(MP.L[LMenu], 2, h - bh, Graphics.TOP | Graphics.LEFT);
						g.drawString(MP.L[LEdit], w >> 1, h - bh, Graphics.TOP | Graphics.HCENTER);
						g.drawString(MP.L[keyboard != null && text != null && text.length() != 0 ? LClear : LCancel], w - 2, h - bh, Graphics.TOP | Graphics.RIGHT);
					}
					if (bottomAnimTarget != -1) {
						// don't draw input field when animation is in progress
					} else if (canWrite && hasInput) {
						g.setFont(MP.smallBoldFont);
						g.setColor(colors[COLOR_CHAT_SEND_ICON]);
						if (replyMsgId != 0) {
							g.drawString(MP.L[LReply], 2, iy - MP.smallBoldFontHeight - 4, 0);
						}
						if (editMsgId != 0) {
							g.drawString(MP.L[LEdit], 2, iy - MP.smallBoldFontHeight - 4, 0);
						}
						if (forwardMsgs != null || forwardMsg != null) {
							g.drawString(MP.L[LForward], 2, iy - MP.smallBoldFontHeight - 4, 0);
						}
						if (ih != bottom) {
							g.setColor(colors[COLOR_CHAT_INPUT_BORDER]);
							g.drawLine(0, iy, w, iy);
							
							if (touch) {
								// cancel icon
								int ty = by + ((MP.smallBoldFontHeight + 8 - 12) >> 1);
								g.setColor(colors[COLOR_CHAT_INPUT_ICON]);
								g.drawLine(w - 20, ty, w - 8, ty + 12);
								g.drawLine(w - 20, ty + 12, w - 8, ty);
							}
						}
						g.setColor(colors[COLOR_CHAT_INPUT_ICON]);
//#ifndef NO_NOKIAUI
						if (nokiaEditor != null && editorShown) {
							if (updateEditor) {
								updateEditor = false;
								try {
									if (menuFocused || menuAnimProgress != 0) {
										NokiaAPI.TextEditor_setFocus(nokiaEditor, false);
										NokiaAPI.TextEditor_setVisible(nokiaEditor, false);
										NokiaAPI.TextEditor_setParent(nokiaEditor, null);
									} else {
										NokiaAPI.TextEditor_setParent(nokiaEditor, this);
										NokiaAPI.TextEditor_setMultiline(nokiaEditor, true);
										NokiaAPI.TextEditor_setSize(nokiaEditor, 10, iy + 8, w - 40, ih - 8);
										NokiaAPI.TextEditor_setIndicatorVisibility(nokiaEditor, false);
										NokiaAPI.TextEditor_setBackgroundColor(nokiaEditor, colors[COLOR_CHAT_PANEL_BG] | 0xFF000000);
										NokiaAPI.TextEditor_setForegroundColor(nokiaEditor, colors[COLOR_CHAT_PANEL_FG] | 0xFF000000);
										NokiaAPI.TextEditor_setFont(nokiaEditor, MP.smallPlainFont);
										NokiaAPI.TextEditor_setVisible(nokiaEditor, true);
										NokiaAPI.TextEditor_setFocus(nokiaEditor, true);
										NokiaAPI.TextEditor_setTouchEnabled(nokiaEditor, true);
										NokiaAPI.TextEditor_setIndicatorVisibility(nokiaEditor, false);
									}
								} catch (Throwable ignored) {}
							}
						} else
//#endif
						if (keyboard != null) {
							if (!touch && !menuFocused && !keyboard.isVisible()) {
								keyboard.show();
							}
							keyboard.drawTextBox(g, 10, iy, w - 40, ih);
							if (keyboard.isVisible()) keyboard.drawOverlay(g);
						} else if (text == null || text.length() == 0) {
							g.setFont(MP.medPlainFont);
							g.drawString(MP.L[LTextField_Hint], 10, iy + ((ih - MP.medPlainFontHeight) >> 1), 0);
						} else {
							g.setFont(MP.smallPlainFont);
							g.setColor(colors[COLOR_CHAT_PANEL_FG]);
							g.drawString(text, 10, iy + ((ih - MP.smallPlainFontHeight) >> 1), 0);
						}
							
						if ((text != null && text.trim().length() != 0) || file != null || forwardMsgs != null || forwardMsg != null) {
							// send icon
							int ty = iy + ((ih - 20) >> 1);
							
							// TODO: better sending indication
							if (MP.sending) { // disabled
								g.setColor(colors[COLOR_CHAT_INPUT_ICON]);
							} else {
								g.setColor(colors[COLOR_CHAT_SEND_ICON]);
							}
							
							g.fillTriangle(w - 8 - 20, ty, w - 8, ty + 10, w - 8 - 20, ty + 20);
							g.setColor(colors[COLOR_CHAT_PANEL_BG]);
							g.fillTriangle(w - 8 - 20, ty, w - 8 - 18, ty + 10, w - 8 - 20, ty + 20);
							g.drawLine(w - 8 - 20, ty + 10, w - 8 - 10, ty + 10);
						} else if (touch) {
							// attach icon
							int ty = iy + ((ih - 24) >> 1);
							
							g.setColor(colors[COLOR_CHAT_INPUT_ICON]);
							g.fillRect(w - 40 + 12, ty + 12, 17, 1);
							g.fillRect(w - 40 + 20, ty + 4, 1, 17);
						}
					} else if (left) {
						g.setColor(colors[COLOR_CHAT_INPUT_ICON]);
						g.drawString(MP.L[LJoinGroup], w >> 1, iy + ((ih - MP.medPlainFontHeight) >> 1), Graphics.TOP | Graphics.HCENTER);
					}
				} else if (funcFocused) {
					by += 1;
					g.drawString(MP.L[LMenu], 2, by + 1, Graphics.TOP | Graphics.LEFT);
					g.drawString(MP.L[LBack], w - 2, by + 1, Graphics.TOP | Graphics.RIGHT);
					if (hasInput && canWrite)
						g.drawString(MP.L[LWrite], w >> 1, by + 1, Graphics.TOP | Graphics.HCENTER);
				}
			}
		} else {
			g.setClip(0, 0, w, h);
		}
		
		// popup menu
		if (menuAnimProgress != 0) {
			skipRender = true;
			int my = h - (int)menuAnimProgress;
			g.setColor(colors[COLOR_CHAT_MENU_BG]);
			g.fillRect(0, my, w, (int)menuAnimProgress);
			if (menu != null) {
				int[] menu = this.menu;
				g.setFont(MP.medPlainFont);
				for (int i = 0; i < menu.length; i++) {
					if (menu[i] == Integer.MIN_VALUE) break;
					if (i == menuCurrent && (!touch || menuCurrent != -1)) {
						g.setColor(colors[COLOR_CHAT_MENU_HIGHLIGHT_BG]);
						g.fillRect(0, my, w, MP.medPlainFontHeight + 8);
					}
					g.setColor(colors[COLOR_CHAT_MENU_FG]);
					g.drawString(MP.L[menu[i]], 4, my + 4, 0);
					my += MP.medPlainFontHeight + 8;
//					g.setColor(0x232F39);
//					g.drawLine(0, my, w, my);
				}
			}
		} else {
			skipRender = false;
			if (touch && !loading && lastDragDir == (reverse ? -1 : 1) && (scroll >= clipHeight || (!endReached && hasOffset))) {
				g.setColor(colors[COLOR_CHAT_PANEL_FG]);
				int tx = width - 40, ty = reverse ? height - bottom - 40 : top + 40;
				g.fillTriangle(tx, ty, tx + 32, ty, tx + 16, reverse ? ty + 32 : ty - 32);
				arrowShown = true;
			} else {
				arrowShown = false;
			}
			
			// process long tap
			if (pressed) {
				if (now - pressTime > 100) {
					kineticScroll = 0;
				}
				if (!longTap && dragged < 10 && pointedItem != null && pointedItem.focusable
						&& Math.abs(pointerX - pressX) < 5 && Math.abs(pointerY - pressY) < 5) {
					animate = true;
					if (now - pressTime > 200) {
						kineticScroll = 0;
						int size = Math.min(360, (int) (now - pressTime - 200) / 2);
//						g.setColor(colors[COLOR_CHAT_POINTER_HOLD]);
//						g.fillArc(pointerX - 25, pointerY - 25, 50, 50, 90, (size * 360) / 200);
						if (size >= 200) {
							// handle long tap
							longTap = true;
							focusItem(pointedItem, 0);
							int y = pointerY;
							pointedItem.tap(pointerX,
									reverse ? y - (scroll - bottom - pointedItem.y + height - pointedItem.contentHeight)
											: y - pointedItem.y - top - scroll,
											true);
						}
					}
				}
			}
		}
		
//		g.setColor(-1);
//		g.setFont(MP.smallPlainFont);
//		g.drawString("f" + (System.currentTimeMillis() - now) + " r" + (deltaTime) + " i" + renderedItems, 20, 20, 0);

		// limit fps
		if (deltaTime < 32) {
			try {
				Thread.sleep(33 - deltaTime);
			} catch (Exception ignored) {}
		}
		animating = animate;
		if (animate) {
			repaint();
		}
		lastPaintTime = now;
	}

	private boolean slide(int mode, float val, int target, long deltaTime) {
		if (!MP.fastScrolling) {
			float d = Math.abs(target - val);
			boolean dir = target - val > 0;
			if (d < 1) {
				return false;
			} else // if (mode == 0) {
				if (d < 5) {
					if (dir) {
						++ val;
					} else {
						-- val;
					}
				} else for (int i = 0; i < 1 + (deltaTime > 33 && animating ? (deltaTime / 33) - 1 : 0); ++i) {
//					val = MP.lerp(val, target, 4, 20);
					val = val + ((target - val) * 4F / 20);
				}
			/* } else {
				float f = 20F * (1 + (deltaTime > 33 && animating ? (deltaTime / 33f) - 1 : 0));
				if (dir) {
					if ((val += f) >= target) {
						val = target;
					}
				} else {
					if ((val -= f) <= target) {
						val = target;
					}
				}
				
			}*/
		} else {
			val = target;
		}
		
		if (mode == 0) {
			scroll = (int) val;
		} else if (mode == 1) {
			bottom = (int) (bottomAnimProgress = val);
		} else /* (mode == 2) */ {
			menuAnimProgress = val;
		}
		
		return !MP.fastScrolling;
	}

	protected void keyPressed(int key) {
		key(key, false);
	}
	
	private void back() {
		if (inputFocused) {
			if (editMsgId != 0) {
				text = "";
			}
			replyMsgId = 0;
			editMsgId = 0;
			file = null;
			forwardPeer = null;
			forwardMsg = null;
			forwardMsgs = null;
			inputFocused = false;
			if ((funcFocused = funcWasFocused)) {
				bottomAnimTarget = MP.smallBoldFontHeight + 4;
			} else {
				bottomAnimTarget = 0;
			}
			if (keyboard != null) {
				onKeyboardCancel();
			}
			return;
		}
		if (selected != 0) {
			unselectAll();
			return;
		}
		MP.midlet.commandAction(MP.backCmd, this);
	}
	
	protected void keyRepeated(int key) {
		// TODO own repeater thread
		key(key, true);
	}
	
	protected void keyReleased(int key) {
		if (keyboard != null && keyboard.isVisible() && keyboard.keyReleased(key)) {
			return;
		}
	}
	
	private int mapGameAction(int key) {
		switch (key) {
		case -1:
			return Canvas.UP;
		case -2:
			return Canvas.DOWN;
		case -3:
			return Canvas.LEFT;
		case -4:
			return Canvas.RIGHT;
		default:
			int g = 0;
			try {
				g = getGameAction(key);
			} catch (Exception ignored) {}
			return g <= 0 ? 0 : g;
		}
	}
	
	private int mapKey(int key) {
		if (key == -21 || key == 21) {
			return -6;
		}
		if (key == -22 || key == 22) {
			return -7;
		}
		return key;
	}
	
	private void key(int key, boolean repeat) {
		int game = mapGameAction(key);
		key = mapKey(key);
		String s = null;
		try {
			s = getKeyName(key).toLowerCase();
			if (s.equals("send") || s.equals("call")
					|| (MP.symbian && key == -10)) {
				game = -10;
			} else if (s.indexOf("back") != -1) {
				game = -11;
			}
		} catch (Exception ignored) {}
		boolean repaint = false;
		if (keyboard != null && keyboard.isVisible() && game >= 0 && (repeat ? keyboard.keyRepeated(key) : keyboard.keyPressed(key))) {
			// keyboard grabbed event
		} else if (key == -7 || (MP.blackberry && (key == 'p' || key == 'P'))) {
			if (repeat) return;
			// back
			if (menuFocused) {
				closeMenu();
			} else if (inputFocused) {
				back();
			} else if (keyboard != null && keyboard.isVisible()) {
				onKeyboardCancel();
				return;
			} else if (touch || query != null || mediaFilter != null || selected != 0 || loading) {
				back();
				return;
			} else if (funcFocused) {
//				fieldFocused = false;
//				fieldAnimTarget = 0;
				back();
				return;
			} else {
				funcFocused = true;
				bottomAnimTarget = MP.smallBoldFontHeight + 4;
				keyGuide = false;
			}
			repaint = true;
		} else if (key == -6 || (MP.blackberry && (key == 'q' || key == 'Q'))) {
			if (repeat || loading) return;
			// menu
			if (menuFocused) {
				closeMenu();
			} else if (inputFocused) {
				// TODO use LFullscreenTextBox instead of LEdit?
				showMenu(null,
						(text != null && text.trim().length() != 0) || file != null || forwardMsgs != null || forwardMsg != null ?
						new int[] { LSend, LEdit, LClear, LCancel } :
						new int[] { LEdit, LCancel });
			} else if (selected != 0) {
				showMenu(null, new int[] { LDelete, LForward });
			} else if (funcFocused) {
				showMenu(null, canWrite && hasInput ? new int[] { LRefresh, LChatInfo, LSearchMessages, LSendSticker, LWriteMessage } : new int[] { LRefresh, LChatInfo, LSearchMessages });
			} else {
				if (focusedItem != null && focusedItem.focusable) {
					int[] menu = focusedItem.menu();
					if (menu != null && menu.length != 0) {
						showMenu(focusedItem, menu);
					}
				}
			}
			repaint = true;
		} else if (menuFocused && game >= 0) {
			if (menuCurrent == -1) menuCurrent = 0;
			if (game == Canvas.UP) {
				if (menuCurrent-- == 0) {
					menuCurrent = menuCount - 1;
				}
				repaint = true;
			} else if (game == Canvas.DOWN) {
				if (menuCurrent++ == menuCount - 1) {
					menuCurrent = 0;
				}
				repaint = true;
			} else if (key == -5 || game == Canvas.FIRE) {
				menuAction(menuCurrent);
				repaint = true;
			}
		} else if (loading) {
			// prevent NPE below
		} else if (inputFocused && game >= 0) {
			if (key == -5 || game == Canvas.FIRE) {
//				if (keyboard == null) {
				showTextBox();
			}
		} else if (funcFocused && game >= 0) {
			if (game == Canvas.UP) {
				funcFocused = false;
				bottomAnimTarget = 0;
				repaint = true;
			} else if (key == -5 || game == Canvas.FIRE) {
				if (canWrite && hasInput) {
					focusInput();
				}
			}
		} else if (key == -5 || game == Canvas.FIRE) {
			// action
			if (focusedItem != null) {
				if (focusedItem.action()) {
					repaint = true;
				}
			}
		} else if (key == Canvas.KEY_NUM2 || key == Canvas.KEY_NUM8) {
			int dir = key == Canvas.KEY_NUM2 ? -1 : 1;
			if (reverse) dir = -dir;
			focusItem(null, 0);
			focusedItem = scrollCurrentItem = scrollTargetItem = null;
			scrollTo(scroll + ((clipHeight * 7 / 8) * dir));
			repaint = true;
		} else if (key == Canvas.KEY_NUM1) {
			// search
			if (canWrite && hasInput) MP.midlet.commandAction(MP.searchMsgCmd, this);
		} else if (key == Canvas.KEY_NUM3) {
			// chat info
			openProfile();
		} else if (key == Canvas.KEY_NUM4) {
			// write message
			resetInput();
			MP.midlet.commandAction(MP.writeCmd, this);
		} else if (key == Canvas.KEY_NUM6) {
			// refresh
			MP.midlet.commandAction(MP.latestCmd, this);
		} else if (key >= Canvas.KEY_NUM0 && key <= Canvas.KEY_NUM9) {
			// ignore
		} else if (game == Canvas.DOWN || game == Canvas.UP) {
			scroll: {
				int dir = game == Canvas.DOWN ? 1 : -1;
				int dir2 = dir;
				if (reverse) dir = -dir;
				final int scrollAmount = clipHeight / 4;
				if (scrollTargetItem == null && scrollCurrentItem == null) {
					scrollTargetItem = getFirstFocusableItemOnScreen(null, 1, 0);
					if (touch && isVisible(scrollTargetItem)) {
						focusItem(scrollTargetItem, reverse ? -1 : 1);
						repaint = true;
						break scroll;
					}
				}
				if (focusedItem != null) {
					int t = focusedItem.traverse(game);
					repaint = true;
					if (t != Integer.MIN_VALUE) {
						repaint = true;
						if (t != Integer.MAX_VALUE) {
							scrollTo(scroll + scrollAmount * dir);
						}
						break scroll;
					}
				}
				if (lastScrollDir != dir) {
					UIItem item = scrollCurrentItem;
					if (item != null && !isVisible(item)) {
						scrollCurrentItem = null;
					}
					scrollTargetItem = null;
					lastScrollDir = dir;
				}
				if (scrollCurrentItem != null && scrollTargetItem == null) {
					// get next scroll target
					scrollTargetItem = getFirstFocusableItemOnScreen(scrollCurrentItem, dir, 0);
				}
				UIItem item = scrollTargetItem;
				if (item != null && isVisible(item)) {
					focusItem(item, dir2);
				}
				if (scrollTargetItem != null && isVisible(scrollTargetItem)) {
					repaint = true;
					focusItem(scrollTargetItem, dir2);
					scrollCurrentItem = scrollTargetItem;
					if (isCornerVisible(scrollTargetItem, dir2) && isVisible(scrollTargetItem)) {
						scrollTargetItem = getFirstFocusableItemOnScreen(scrollCurrentItem, dir, 0);
						if (scrollTargetItem != null && isCornerVisible(scrollTargetItem, dir2) && isVisible(scrollTargetItem, clipHeight / 8)) {
							break scroll;
						}
					}
				}
				repaint = true;
				if (dir == 1) {
					scrollTo(Math.min(scroll + scrollAmount, contentHeight - clipHeight));
				} else {
					scrollTo(Math.max(scroll - scrollAmount, 0));
				}
				
				if (scrollTargetItem != null && isVisible(scrollTargetItem) && isCornerVisible(scrollTargetItem, dir2)) {
					scrollTargetItem = null;
				}
			}
		} else if (game == Canvas.LEFT || game == Canvas.RIGHT) {
			if (focusedItem != null) {
				focusedItem.traverse(game);
			}
		} else if (game == -10) {
			send();
			repaint = true;
		} else if (game == -11) {
			back();
		}
		if (repaint) {
			queueRepaint();
		}
	}
	
	protected void pointerPressed(int x, int y) {
		if (keyboard != null && keyboard.pointerPressed(x, y)) return;
		focusItem(null, 0);
		pressed = true;
		dragging = false;
		draggingHorizontally = false;
		dragged = 0;
		dragXHold = 0;
		dragYHold = 0;
		pressX = pointerX = x;
		pressY = pointerY = y;
		movesIdx = 0;
		pressTime = System.currentTimeMillis();
		if (!menuFocused && !loading && y > top && y < top + clipHeight &&
				// not touching arrow icon
				!(arrowShown && x > width - 40
					&& (reverse ? (y > height - bottom - 40) : (y < top + 40)))) {
			pointedItem = getItemAt(y);
			contentPressed = true;
		} else {
			pointedItem = null;
		}
//		queueRepaint();
	}
	
	protected void pointerDragged(int x, int y) {
		if (keyboard != null && keyboard.pointerDragged(x, y)) return;
		long now = System.currentTimeMillis();
		if (contentPressed) {
			if (longTap) {
				boolean d = y > pointerY;
				if (heldItem == null) {
					heldItem = pointedItem;
					startSelectDir = d;
				}
				if (heldItem instanceof UIMessage) {
					UIItem item = getItemAt(y);
					if (item instanceof UIMessage && (item != pointedItem)) {
						if (d == startSelectDir) {
							if (((UIMessage) heldItem).selected) {
								((UIMessage) item).select();
							} else {
								((UIMessage) item).unselect();
							}
						} else if (((UIMessage) heldItem).selected) {
							((UIMessage) pointedItem).unselect();
						} else {
							((UIMessage) pointedItem).select();
						}
						pointedItem = item;
					}
				}
			} else {
				final int dY = pointerY - y;
				final int dX = pointerX - x;
				dragged += Math.abs(dX) + Math.abs(dY);
				if (dragging || dY > 1 || dY < -1
						|| dragYHold + dY > 2 || dragYHold + dY < -2
						|| dX > 1 || dX < -1
						|| dragXHold + dX > 2 || dragXHold + dX < -2) {
					int dx2 = dX;
					int dy2 = dY;
					if (now - pressTime < 100) {
//						dx2 += dragXHold;
						dy2 += dragYHold;
					}
					if (draggingHorizontally || (!dragging && Math.abs(dx2) > Math.abs(dy2))) {
						if (!draggingHorizontally) {
							focusItem(pointedItem, 0);
						}
						draggingHorizontally = true;
					} else if (!draggingHorizontally) {
						if (reverse) dy2 = -dy2;
						scroll += dy2;
						if (kineticScroll * dy2 < 0) kineticScroll = 0;
						lastDragDir = dy2 < 0 ? -1 : 1;
					}
					dragging = true;
					dragXHold = 0;
					dragYHold = 0;
					scrollTarget = -1;
				} else {
					// hold dragged units until it reaches threshold 
					dragXHold += dX;
					dragYHold += dY;
				}
				int prev = movesIdx - 1;
				if (prev < 0) prev += moveSamples;
				long prevTime = moveTimes[prev];
				if (now - prevTime <= 1) {
					moves[prev] += dY;
					moveTimes[prev] = now;
				} else {
					moves[movesIdx] = dY;
					moveTimes[movesIdx] = now;
					movesIdx = (movesIdx + 1) % moveSamples;
				}
			}
		} else if (menuFocused) {
			// TODO menu scroll
		}
		pointerX = x;
		pointerY = y;
		queueRepaint();
	}
	
	protected void pointerReleased(int x, int y) {
		if (keyboard != null && keyboard.pointerReleased(x, y)) return;
		long now = System.currentTimeMillis();
		if (contentPressed) {
			if (!longTap) {
				if (!dragging || (dragged < 10 && Math.abs(x - pressX) < 4 && Math.abs(y - pressY) < 4)) {
					if (kineticScroll != 0) {
						kineticScroll = 0;
					} else if (now - pressTime < 300 && pointedItem != null && pointedItem.focusable) {
						focusItem(pointedItem, 0);
						pointedItem.tap(x,
								reverse ? y - (scroll - bottom - pointedItem.y + height - pointedItem.contentHeight)
										: y - pointedItem.y - top - scroll,
										false);
					}
				} else if (draggingHorizontally && selected == 0) {
					int d = pointerX - pressX;
					if (d > 50) {
						MP.midlet.commandAction(MP.backCmd, this);
					} else if (d < -50 && pointedItem instanceof UIMessage) {
						startReply((UIMessage) pointedItem);
					}
				} else {
					int move = 0;
					long moveTime = 0, lastTime = 0, prevTime = 0;
					for (int i = 0; i < moveSamples; i++) {
						int idx = (movesIdx + moveSamples - 1 - i) % moveSamples;
						long time = moveTimes[idx];
						if (time < prevTime) {
							break;
						}
						prevTime = time;
						if ((time = now - (lastTime = time)) > 200) {
							break;
						}
						move += moves[idx];
						moveTime += time;
					}
					if (moveTime == 0) moveTime = 1;
					long holdTime = now - lastTime;
					if (moveTime > 0 && holdTime < 150) {
						// release kinetic velocity
						float res = (130f * move) / moveTime;
						if (holdTime > 28) {
							if (res > move)
								res = move;
							res *= 25f / (holdTime - 10);
						}
						float abs = Math.abs(res);
						if (abs >= 1) {
							if (abs > 100) {
								res = (res < 0 ? -60 : 60);
							}
							if (reverse) res = -res;
							if (kineticScroll * res < 0) kineticScroll = 0;
							kineticScroll += res;
						}
					}
				}
			}
		} else if (menuFocused) {
			int my = height - (int)menuAnimProgress;
			if (y < my || x < 20 || x > width - 20 || menu == null) {
				closeMenu();
				queueRepaint();
			} else if (!longTap && now - pressTime < 300 && menuAnimTarget == -1) {
				menuAction((y - my) / (MP.medPlainFontHeight + 8));
			}
		} else if (touch && now - pressTime < 300) {
			if (y < top) {
				if (x < 40) { // back button
					if (keyboard != null && keyboard.isVisible()) {
						onKeyboardCancel();
					} else {
						keyPressed(-7);
					}
				} else if (selected != 0) { // selected messages actions
					if (x > width - 40) {
						deleteSelected();
					} else if (x > width - 90) {
						forwardSelected();
					}
				} else if (loading) { // do nothing
				} else if (x > width - 48) { // menu button
					if (query != null) {
						showMenu(null, new int[] { LSearchMessages });
					} else if (mediaFilter == null) {
						showMenu(null, new int[] { LRefresh, LSearchMessages });
					}
				} else if (postId == null) {
					openProfile();
				}
			} else if (y > height - bottom && hasInput) {
				if (selected != 0 || loading) { // do nothing
				} else if (left) {
					MP.midlet.start(MP.RUN_JOIN_CHANNEL, id);
				} else if (canWrite) {
					if (y < height - inputFieldHeight) {
						if (x > width - 48) {
							if (editMsgId != 0 || forwardMsgs != null || forwardMsg != null) {
								resetInput();
							} else {
								replyMsgId = 0;
								bottom = inputFieldHeight;
							}
						}
					} else if (x > width - 48) {
						send();
					} else { 
						if (nokiaEditor != null) {
							if (!editorShown) {
								editorShown = true;
								updateEditor = true;
							}
						} else if (keyboard != null) {
							keyboard.show();
						} else {
							showTextBox();
						}
					}
				}
			} else if (arrowShown && x > width - 40
					&& (reverse ? (y > height - bottom - 40) : (y < top + 40))) {
				if (!endReached && hasOffset) {
					MP.midlet.commandAction(MP.latestCmd, this);
				} else {
					scroll = clipHeight * 2;
					scrollTo(0);
				}
			}
		}
		dragXHold = 0;
		dragYHold = 0;
		pointedItem = null;
		heldItem = null;
		dragging = false;
		draggingHorizontally = false;
		contentPressed = false;
		pressed = false;
		longTap = false;
		pointerX = x;
		pointerY = y;
		queueRepaint();
	}
	
	private void showTextBox() {
		if (text == null) text = "";
		TextBox t = new TextBox("", text, 500, TextField.ANY);
		t.addCommand(MP.okCmd);
		t.addCommand(MP.cancelCmd);
		t.setCommandListener(MP.midlet);
		
		MP.display(t);
	}
	
	private void menuAction(int i) {
		if (i < menu.length) {
			if (menu[i] == LBack) {
				// just close
			} else if (menuItem == null) {
				switch (menu[i]) {
				case LRefresh:
					MP.midlet.commandAction(MP.latestCmd, this);
					break;
				case LChatInfo:
					openProfile();
					break;
				case LSearchMessages:
					MP.midlet.commandAction(MP.searchMsgCmd, this);
					break;
				case LSendSticker:
					MP.midlet.commandAction(MP.sendStickerCmd, this);
					break;
				case LWriteMessage:
					if (replyMsgId != 0 || editMsgId != 0) {
						int r = Math.max(replyMsgId, topMsgId);
						MP.display(MP.writeForm(id, r == 0 ? null : Integer.toString(r), text, editMsgId == 0 ? null : Integer.toString(editMsgId), null, null));
						resetInput();
						break;
					}
					MP.midlet.commandAction(MP.writeCmd, this);
					break;
				case LDelete: {
					deleteSelected();
					break;
				}
				case LForward: {
					forwardSelected();
					break;
				}
				case LOpenPlayer: {
					MP.midlet.commandAction(MP.playerCmd, this);
					break;
				}
				
				// input menu
				case LEdit: {
					showTextBox();
					break;
				}
				case LCancel: {
					back();
					break;
				}
				case LClear: {
					text = "";
					if (keyboard != null) {
						keyboard.setText("");
					}
				}
				case LSend: {
					send();
					break;
				}
				}
			} else {
				menuItem.menuAction(menu[i]);
			}
			closeMenu();
			queueRepaint();
		}
	}

	protected void sizeChanged(int width, int height) {
		skipRender = false;
		updateEditor = true;
		MP.smallPlainFontHeight = MP.smallPlainFont.getHeight();
		MP.smallPlainFontSpaceWidth = MP.smallPlainFont.charWidth(' ');
		MP.smallBoldFontHeight = MP.smallBoldFont.getHeight();
		MP.medPlainFontHeight = MP.medPlainFont.getHeight();
		MP.medBoldFontHeight = MP.medBoldFont.getHeight();
		queueRepaint();
	}
	
	// ui
	
	public void requestLayout(UIItem item) {
		if (layoutStart != null || item == null) {
			layoutStart = firstItem;
		} else {
			layoutStart = (UIItem) item;
		}
		if (!loading) queueRepaint();
	}
	
	private void layout(UIItem offsetItem, int w, int h) {
		if (count == 0 || offsetItem == null) return;
		boolean offset = false;
		if (offsetItem.prev != null) {
			offsetItem = offsetItem.prev;
			offset = true;
		}
		
		int prevScroll = scroll;
		int prevScrollItemY = 0;
		UIItem scrollItem = scrollCurrentItem;
		if (scrollItem != null) {
			prevScrollItemY = scrollItem.y;
		}
		
		UIItem item = offsetItem;
		int y = 0;
		do {
			if (offset && item == offsetItem) {
				y = item.y;
			} else {
				item.y = y;
			}
			y += item.layout(w);
		} while ((item = item.next) != null);
		
		contentHeight = y;
		
		if (prevScrollItemY != 0 && scroll != 0) {
			scroll = prevScroll - prevScrollItemY + scrollItem.y;
		}
	}
	
	void safeAdd(Thread thread, UIMessage item, boolean focus) {
		if (thread != this.thread) throw MP.cancelException;
		table.put(Integer.toString(item.id), item);
		add(item);
		if (focus) nextFocusItem = item;
	}
	
	void safeAddFirst(Thread thread, UIMessage item) {
		if (thread != this.thread) throw MP.cancelException;
		table.put(Integer.toString(item.id), item);
		addFirst(item);
	}
	
	void addFirst(UIItem item) {
		if (item == null) return;
		count++;
		if (firstItem == null) {
			firstItem = lastItem = item;
		} else {
			item.next = firstItem;
			firstItem.layoutWidth = 0;
			firstItem = (firstItem.prev = item);
		}
		item.container = this;
		requestLayout(item);
	}
	
	void add(UIItem item) {
		if (item == null) return;
		count++;
		if (firstItem == null || lastItem == null) {
			firstItem = lastItem = item;
		} else {
			item.prev = lastItem;
			lastItem.layoutWidth = 0;
			lastItem = (lastItem.next = item);
		}
		item.container = this;
		requestLayout(item);
	}
	
	void remove(UIItem item) {
		if (item == null) return;
		UIItem i = firstItem;
		if (i == null) return;
		if (item instanceof UIMessage) {
			if (((UIMessage) item).selected)
				-- selected;
			table.remove(Integer.toString(((UIMessage) item).id));
		}
		do {
			if (i == item) {
				if (item == firstItem) {
					firstItem = item.next;
				}
				if (item == lastItem) {
					lastItem = item.prev;
				}
				if (item.prev != null) {
					item.prev.layoutWidth = 0;
					item.prev.next = item.next;
				}
				if (item.next != null) {
					item.next.layoutWidth = 0;
					item.next.prev = item.prev;
				}
				item.container = null;
				count--;
				requestLayout(item.prev);
				break;
			}
		} while ((i = i.next) != null);
		if (lastItem == null) {
			lastItem = firstItem;
		}
	}
	
	public void queueRepaint() {
		if (isShown()) repaint();
	}
	
	private boolean focusItem(UIItem item, int dir) {
		if (focusedItem == item) return true;
		if (focusedItem != null) {
			focusedItem.lostFocus();
		}
		scrollCurrentItem = item;
		if (scrollTargetItem != item) scrollTargetItem = null;
		focusedItem = item;
		if (item != null) {
			if (!focusedItem.grabFocus(dir)) {
				focusedItem = null;
				return false;
			}
			return true;
		}
		return false;
	}
	
	private void scrollTo(UIItem item) {
		scrollCurrentItem = item;
		scrollTo(item.y);
	}
	
	private void scrollTo(int y) {
		if (MP.fastScrolling) {
			scroll = y;
			return;
		}
		scrollTarget = y;
	}
	
	private boolean isVisible(UIItem item) {
		return item.y + item.contentHeight > scroll && item.y < scroll + clipHeight;
	}
	
	private boolean isVisible(UIItem item, int offset) {
		return item.y + item.contentHeight + offset > scroll && item.y < scroll + clipHeight - offset;
	}
	
	private boolean isCornerVisible(UIItem item, int dir) {
		if (dir == (reverse ? 1 : -1)) {
			// isTopVisible
			return item.y >= scroll && item.y < scroll + height;
		} else {
			// isBottomVisible
			return item.y + item.contentHeight > scroll && item.y + item.contentHeight <= scroll + clipHeight;
		}
	}
	
	private UIItem getFirstFocusableItemOnScreen(UIItem offset, int dir, int offsetHeight) {
		offset = offset == null ? (dir == 1 ? firstItem : lastItem) : (dir == -1 ? offset.prev : offset.next);
		UIItem res = null;
		while (offset != null) {
			UIItem t = offset;
			if (t.focusable && isVisible(t, offsetHeight)) {
				res = t;
				break;
			}
			offset = (dir == -1 ? offset.prev : offset.next);
		}
		return res;
	}
	
	private UIItem getItemAt(int y) {
		if (reverse) {
			y = height + scroll - bottom - y;
		} else {
			y -= top - scroll;
		}
		if (y < scroll || y > clipHeight + scroll) return null;
		UIItem item = firstItem;
		if (item == null) return null;
		do {
			if (y >= item.y && y < item.y + item.contentHeight) {
				return item;
			}
		} while ((item = item.next) != null);
		return null;
	}
	
	private void openProfile() {
		MP.openLoad(new ChatInfoForm(id, this, 0));
	}

	void showMenu(UIItem item, int[] menu) {
		if (keyboard != null && keyboard.isVisible()) onKeyboardCancel();
		if (item == null && menu[0] != LDelete && MP.playerState != 0) {
			int[] t = menu;
			menu = new int[menu.length + 1];
			System.arraycopy(t, 0, menu, 0, t.length);
			menu[t.length] = LOpenPlayer;
		}
		kineticScroll = 0;
		this.menuItem = item;
		this.menu = menu;
		menuCurrent = touch ? -1 : 0;
		menuFocused = true;
		skipRender = false;
		updateEditor = true;
		int len = menu.length;
		for (int i = 0; i < len; i++) {
			if (menu[i] == Integer.MIN_VALUE) {
				len = i;
				break;
			}
		}
		menuCount = len;
		int h = (MP.medPlainFontHeight + 8) * len;
		if (touch && h >= height - 20) {
			this.menu = new int[len + 1];
			System.arraycopy(menu, 0, this.menu, 0, len);
			this.menu[len] = LBack;
			menuCount++;
			h += MP.medPlainFontHeight + 8;
		}
		menuAnimTarget = h;
		
		if (len != 0 && menu != null) {
			for (int i = 0; i < colors.length; ++i) {
				if (i == COLOR_CHAT_MENU_BG || i == COLOR_CHAT_MENU_HIGHLIGHT_BG || i == COLOR_CHAT_MENU_FG)
					continue;
				int c = colorsCopy[i];
				colors[i] = ((((c >> 16) & 0xFF) * 15) >> 5) << 16 | ((((c >> 8) & 0xFF) * 15) >> 5) << 8 | ((((c) & 0xFF) * 15) >> 5);
			}
		}
		
		updateColors();
	}
	
	void closeMenu() {
		menuFocused = false;
		menuItem = null;
		menu = null;
		menuAnimTarget = 0;
		System.arraycopy(colorsCopy, 0, colors, 0, colors.length);
		updateColors();
		skipRender = false;
		updateEditor = true;
	}
	
	public void requestPaint(UIItem item) {
		if (item == null || !isVisible(item)) return;
		queueRepaint();
	}
	
	void selected(UIMessage msg) {
		++ selected;
	}
	
	void unselected(UIMessage msg) {
		-- selected;
	}
	
	private UIMessage[] getSelected() {
		UIMessage[] msgs = new UIMessage[selected];
		int count = 0;
		UIItem item = firstItem;
		if (item != null) {
			do {
				if (!(item instanceof UIMessage) || !((UIMessage) item).selected)
					continue;
				msgs[count++] = (UIMessage) item;
			} while ((item = item.next) != null);
		}
		return msgs;
	}
	
	private void unselectAll() {
		UIItem item = firstItem;
		if (item == null) return;
		do {
			if (!(item instanceof UIMessage) || !((UIMessage) item).selected)
				continue;
			((UIMessage) item).selected = false;
		} while ((item = item.next) != null);
		selected = 0;
		queueRepaint();
	}
	
	private void deleteSelected() {
		UIMessage[] msgs = getSelected();
		unselectAll();
		
		MP.confirm(MP.RUN_DELETE_MESSAGE,
				msgs,
				null,
				MP.localizeFormattedPlural(MP.LDeleteNMessage_Alert, msgs.length));
	}
	
	private void forwardSelected() {
		UIMessage[] msgs = getSelected();
		unselectAll();

		MP.openLoad(new ChatsList(id, msgs));
	}

	public void startEdit(UIMessage item) {
		resetInput();
		text = item.origText;
		editMsgId = item.id;
		focusInput();
	}
	
	public void startReply(UIMessage item) {
		if (editMsgId != 0 || forwardMsgs != null || forwardMsg != null) resetInput();
		replyMsgId = item.id;
		focusInput();
	}
	
	public void startForward(String peer, String msg, UIMessage[] msgs) {
		resetInput();
		MP.display(this);
		this.forwardPeer = peer;
		this.forwardMsg = msg;
		this.forwardMsgs = msgs;
		focusInput();
	}
	
	private void focusInput() {
		int h = inputFieldHeight;
		if (replyMsgId != 0 || editMsgId != 0 || forwardMsgs != null || forwardMsg != null) {
			h += MP.smallBoldFontHeight + 8;
		}
		if (text == null) text = "";
		if (!touch) {
			this.bottomAnimTarget = h + MP.smallBoldFontHeight + 4;
			keyGuide = false;
			inputFocused = true;
			funcWasFocused = funcFocused;
		} else {
//#ifndef NO_NOKIAUI
			if (nokiaEditor != null) {
				NokiaAPI.TextEditor_setContent(nokiaEditor, text);
				editorShown = true;
				updateEditor = true;
			}
//#endif
			this.bottom = h;
		}

		if (keyboard != null) {
			keyboard.setText(text);
			keyboard.show();
		}
		queueRepaint();
	}
	
	private void send() {
		if (!touch && !inputFocused) {
			focusInput();
		} else if ((text != null && text.trim().length() != 0) || file != null || forwardMsgs != null || forwardMsg != null) {
			if (!MP.sending) {
				MP.sending = true;
				MP.midlet.start(MP.RUN_SEND_MESSAGE, new Object[] {
						text, id,
						replyMsgId == 0 ? null : Integer.toString(replyMsgId),
						editMsgId == 0 ? null : Integer.toString(editMsgId),
						file,
						null, forwardPeer, forwardMsg, forwardMsgs
						});
			}
		} else {
			showMenu(null, new int[] { LSendSticker, LWriteMessage });
		}
	}
	
	private void resetInput() {
		text = "";
		replyMsgId = 0;
		editMsgId = 0;
		file = null;
		if (touch) bottom = inputFieldHeight;
		forwardPeer = null;
		forwardMsg = null;
		forwardMsgs = null;
//#ifndef NO_NOKIAUI
		if (nokiaEditor != null) {
			NokiaAPI.TextEditor_setContent(nokiaEditor, "");
		}
//#endif
		if (keyboard != null) {
			keyboard.clear();
		}
	}
	
	private void updateColors() {
		UIItem item = firstItem;
		if (item == null) return;
		do {
			if (!(item instanceof UIMessage))
				continue;
			((UIMessage) item).updateColors = true;
		} while ((item = item.next) != null);
	}
	
	// interface getters

	public String id() {
		return id;
	}

	public String postId() {
		return postId;
	}

	public String query() {
		return query;
	}

	public String mediaFilter() {
		return mediaFilter;
	}

	public String username() {
		return username;
	}

	public boolean updating() {
		return update;
	}

	public boolean endReached() {
		return endReached;
	}

	public boolean forum() {
		return forum;
	}

	public boolean switched() {
		return switched;
	}

	public boolean channel() {
		return channel;
	}

	public int topMsgId() {
		return topMsgId;
	}

	public int firstMsgId() {
		return firstMsgId;
	}

	public JSONArray topics() {
		return topics;
	}

	public MPChat parent() {
		return parent;
	}
	
	// interface setters

	public void setParent(MPChat parent) {
		this.parent = parent;
	}

	public void setQuery(String s) {
		query = s;
		switched = true;
	}

	public void setUpdate(boolean b) {
		this.update = b;
	}

	public void setBotAnswer(JSONObject j) {
		botAnswer = j;
	}
	
	public void setStartBot(String s) {
		this.startBot = s;
	}
	
	//
	
	public String getTitle() {
		return super.getTitle();
	}
	
	public boolean isShown() {
		return super.isShown();
	}
	
	public Ticker getTicker() {
		return super.getTicker();
	}
	
	public void setTicker(Ticker t) {
		super.setTicker(t);
	}
	
	//

	public void resetChat() {
		cancel();
		dir = 0;
		messageId = 0;
		addOffset = 0;
		offsetId = 0;
		typing = 0;
		query = null;
		selected = 0;
		if (table != null) table.clear();
		switched = false;
		shouldUpdate = false;
		mediaFilter = null;
		focusItem(null, 0);
		firstItem = lastItem = null;
		kineticScroll = scroll = 0;
	}

	public void openMessage(String msg, int topMsg) {
		if (table != null && table.containsKey(msg)) {
			UIItem focus = (UIItem) table.get(msg);
			if (focus != null) {
				nextFocusItem = focus;
				return;
			}
		}
		resetChat();
		this.messageId = Integer.parseInt(msg);
		if (topMsg != -1) this.topMsgId = topMsg;
		MP.openLoad(this);
	}

	public void sent() {
		resetInput();
		if (!MP.reopenChat && MP.longpoll && update) queueRepaint();
	}

	public void handleUpdate(int type, JSONObject update) {
		if (!this.update) return;
		switch (type) {
		case UPDATE_USER_STATUS: {
			if (MP.chatStatus) {
				setStatus(update.getObject("status"));
				typing = 0;
				typingThread.interrupt();
			}
			break;
		}
		case UPDATE_USER_TYPING: {
			if ("sendMessageCancelAction".equals(update.getObject("action").getString("_"))) {
				setStatus(null);
				typing = 0;
				typingThread.interrupt();
				break;
			}
			if (id.charAt(0) == '-' && update.has("top_msg_id") && topMsgId != update.getInt("top_msg_id"))
				break;
			this.status = user ? MP.L[LTyping] : MP.L[LSomeoneIsTyping]; // TODO
			typing = System.currentTimeMillis();
			typingThread.interrupt();
			synchronized (typingLock) {
				typingLock.notify();
			}
			queueRepaint();
			break;
		}
		case UPDATE_NEW_MESSAGE: {
			// check for duplicate
			if (update.getObject("message").getInt("id") == firstMsgId)
				break;
			
			typing = 0;
			typingThread.interrupt();
			
			// delete old messages
			while (count >= limit) {
				remove(lastItem instanceof UIPageButton ? lastItem.prev : lastItem);
			}
			
			safeAddFirst(null, new UIMessage(update.getObject("message"), this));
			firstMsgId = update.getObject("message").getInt("id");
			break;
		}
		case UPDATE_DELETE_MESSAGES: {
			JSONArray messages = update.getArray("messages");
			int l = messages.size();
			
			for (int i = 0; i < l; ++i) {
				UIItem item = (UIItem) table.get(messages.getString(i));
				if (item != null) {
					remove(item);
				}
			}
			break;
		}
		case UPDATE_EDIT_MESSAGE: {
			typing = 0;
			typingThread.interrupt();
			
			JSONObject msg = update.getObject("message");
			UIMessage item = (UIMessage) table.get(msg.getString("id"));
			if (item != null) {
				item.edit(msg, this);
				item.layoutWidth = 0;
				requestLayout(item);
			}
			break;
		}
		case UPDATE_READ_OUTBOX: {
			int maxId = readOutboxId = update.getInt("max_id");
			
			UIItem item = firstItem;
			if (item == null) break;
			do {
				if (!(item instanceof UIMessage) || ((UIMessage) item).id > maxId)
					continue;
				((UIMessage) item).read = true;
			} while ((item = item.next) != null);
			queueRepaint();
			break;
		}
		}
	}

	public void handleBotAnswer(JSONObject j) {
		if (j == null) return;
		
		if (j.has("message")) {
			Alert a = new Alert(title);
			a.setType(AlertType.CONFIRMATION);
			a.setString(j.getString("message"));
			a.setTimeout(1500);
			MP.display(a, this);
		}
		
		if (j.has("url")) {
			MP.openUrl(j.getString("url"));
		}
	}

	public void paginate(int dir) {
		this.dir = dir;
		cancel();

		focusItem(null, 0);
		firstItem = lastItem = null;
		kineticScroll = scroll = 0;
		messageId = 0;
		update = false;
		if (dir == 1) {
			if ((idOffset != Integer.MIN_VALUE && idOffset <= limit) || addOffset == limit) {
				addOffset = 0;
				offsetId = 0;
			} else {
				addOffset = -limit - 1;
				offsetId = firstMsgId;
			}
		} else {
			offsetId = firstMsgId;
			addOffset = limit - 1;
		}
		MP.openLoad(this);
	}

	public void openTopic(int topMsgId, boolean canWrite, String title) {
		this.topMsgId = topMsgId;
		this.canWrite = canWrite;
		setTitle(this.title = title);
	}
	
	// KeyboardListener
	
	public boolean onKeyboardType(char c) {
		return true;
	}
	
	public boolean onKeyboardBackspace() {
		return true;
	}
	
	public void onKeyboardLanguageChanged() {
		
	}
	
	public void onKeyboardTextUpdated() {
		text = keyboard.getText();
		MP.midlet.sendTyping(text.trim().length() == 0);
		queueRepaint();
	}
	
	public void onKeyboardDone() {
		skipRender = false;
		keyboard.hide();
		queueRepaint();
	}
	
	public void onKeyboardCancel() {
		skipRender = false;
		keyboard.hide();
		queueRepaint();
	}
	
	public void onKeyboardRepaintRequested() {
		queueRepaint();
	}
	
	public void onTextBoxRepaintRequested() {
		queueRepaint();
	}
	
	public boolean requestLanguageChange() {
		// TODO
		return false;
	}

//#ifndef NO_NOKIAUI
	// TextEditorListener

	public void inputAction(int actions) {
		if ((actions & NokiaAPI.ACTION_CONTENT_CHANGE) != 0) {
			String p = text;
			text = NokiaAPI.TextEditor_getContent(nokiaEditor);
			if (!text.equals(p) && (p != null || text.length() != 0)) {
				MP.midlet.sendTyping(text.trim().length() == 0);
			}
			queueRepaint();
		} else if ((actions & NokiaAPI.ACTION_PAINT_REQUEST) != 0) {
			queueRepaint();
		}
		if ((actions & NokiaAPI.ACTION_TRAVERSE_PREVIOUS) != 0) {
			key(-1, false);
		}
		if ((actions & NokiaAPI.ACTION_TRAVERSE_NEXT) != 0) {
			key(-2, false);
		}
	}
//#endif
	
	//
	
	private void setStatus(JSONObject status) {
		String s;
		if (status == null) {
			this.status = null;
			if (MP.chatStatus) {
				if (wasOnline == 1) {
					s = MP.L[LOnline];
				} else if (wasOnline == 2) {
					s = MP.L[LOffline];
				} else if (wasOnline != 0) {
					s = MP.L[LLastSeen] + MP.localizeDate(wasOnline, 4);
				} else {
					s = null;
				}
				if (s != this.status) {
					this.status = s;
					queueRepaint();
				}
			}
			return;
		}
		if ("userStatusOnline".equals(status.getString("_"))) {
			wasOnline = 1;
			s = MP.L[LOnline];
		} else if ((wasOnline = status.getInt("was_online", 0)) != 0) {
			s = MP.L[LLastSeen] + MP.localizeDate(wasOnline, 4);
		} else {
			s = MP.L[LOffline];
			wasOnline = 2;
		}
		this.status = s;
		queueRepaint();
	}
	
	// typing timer loop
	public void run() {
		try {
			while (update) {
				try {
					if (typing == 0) {
						synchronized (typingLock) {
							typingLock.wait(60000);
						}
					}
					Thread.sleep(5000);
					typing = 0;
				} catch (Exception e) {}
				if (typing == 0) {
					setStatus(null);
				}
			}

			setStatus(null);
			typing = 0;
		} catch (Exception ignored) {
		} finally {
			typingThread = null;
		}
	}
	
	public void gc() {
		UIItem item = firstItem;
		if (item == null) return;
		do {
			if (!(item instanceof UIMessage))
				continue;
			((UIMessage) item).mediaImage = null;
		} while ((item = item.next) != null);
	}

}
//#endif
