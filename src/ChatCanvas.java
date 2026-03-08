/*
Copyright (c) 2025-2026 Arman Jussupgaliyev

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
import java.util.Hashtable;

import javax.microedition.lcdui.*;

public class ChatCanvas extends MPCanvas implements MPChat, Runnable {

	static final int COLOR_CHAT_HIGHLIGHT_BG = 2;
	static final int COLOR_CHAT_PANEL_BG = 3;
	static final int COLOR_CHAT_PANEL_FG = 4;
	static final int COLOR_CHAT_PANEL_BORDER = 5;
	static final int COLOR_CHAT_STATUS_FG = 10;
	static final int COLOR_CHAT_STATUS_HIGHLIGHT_FG = 11;
	static final int COLOR_CHAT_POINTER_HOLD = 12;
	static final int COLOR_CHAT_INPUT_ICON = 13;
	static final int COLOR_CHAT_SEND_ICON = 14;
	static final int COLOR_CHAT_INPUT_BORDER = 15;

	static Image attachIcon;
//	static Image backIcon;
//	static Image moreIcon;

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
	boolean canWrite, canDelete, canBan, canPin, canSeeRead;

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

	MPChat parent;

	// discussion
	String postPeer, postId;
	ChatTopicsList topicsList;
	JSONArray topics;

	final long[] typing = new long[11];
	private final Object typingLock = new Object();
	private Thread typingThread;
	long wasOnline;
	String status, defaultStatus;
	String statusRender;

	JSONObject botAnswer;

	Hashtable table;

	String titleRender;

	// input
	boolean hasInput;
	String text;
	int replyMsgId;
	int editMsgId;
	String file;
	String fileRender;
	int inputFieldHeight;
	boolean inputFocused;
	UIMessage[] forwardMsgs;
	String forwardPeer, forwardMsg;
	boolean editorShown;
	boolean funcWasFocused;

	int topButtonWidth;

	Image photo;
	int photoHeight;
	boolean showPhoto;
	boolean photoQueued;

	int focusedMessage;

	int selected;

	boolean largeButtons;

	ChatCanvas() {
		super();
		setFullScreenMode(true);

		if (touch) {
			top = MP.smallBoldFontHeight + MP.smallPlainFontHeight + 8;
			largeButtons = Math.max(getWidth(), getHeight()) >= 640;
			if (largeButtons) top = Math.max(60, top);
		} else {
			top = MP.smallBoldFontHeight + 4 + (MP.chatStatus && mediaFilter == null ? MP.smallPlainFontHeight + 4 : 0);
		}
		if (MP.chatAvatar) {
			top = Math.max(8 + MP.avatarSize, top);
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

	public boolean loadInternal(Thread thread) throws Exception {
		selected = 0;
		titleRender = null;
		statusRender = null;
		fileRender = null;
		resetInput();
		bottomAnimTarget = -1;

		if (!MP.globalUpdates && (MP.reopenChat || (query == null && mediaFilter == null))
				&& MP.chatUpdates
				&& (MP.updatesThread != null || MP.updatesRunning)) {
			MP.display(MP.loadingAlert(MP.L[LWaitingForPrevChat]), this);

			MP.midlet.cancel(MP.updatesThreadCopy, true);
			while (MP.updatesThread != null || MP.updatesRunning) {
				//noinspection BusyWait
				Thread.sleep(1000L);
			}

			if (MP.current == this) {
				MP.display(MP.useLoadingForm ? (Displayable) MP.loadingForm : this);
			}
		}

		if (mediaFilter == null && query == null && postPeer == null) {
			JSONObject dialog = ((JSONObject) MP.api("getDialog&id=".concat(id))).getObject("res");
			if (dialog.has("read_out")) {
				readOutboxId = dialog.getInt("read_out");
			}
			if (messageId == -1 && dialog.has("read_in")) {
				messageId = 0;
				int maxId = Math.max(readOutboxId, dialog.getInt("read_in"));
				if (maxId != 0) {
					messageId = maxId;
					if (dialog.getInt("unread", 0) > limit) {
						offsetId = maxId;
						addOffset = -limit;
						dir = 1;
					} else {
						offsetId = -1;
					}
				}
			}
			if (MP.chatAvatar && photo == null && !photoQueued) {
				MP.queueImage(id, this);
				photoHeight = MP.avatarSize;
				photoQueued = true;
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
				} else if (j.getInt("unread", 0) > limit) {
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

				JSONObject info = (JSONObject) MP.api("getPeerInfo&id=".concat(id));
				if (id.charAt(0) == '-') {
					if (info.has("admin_rights")) {
						JSONObject adminRights = info.getObject("admin_rights");
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
							list.append(topic.getString("title", "General"), null);
						}

						if (thread != this.thread) throw MP.cancelException;
						MP.deleteFromHistory(this);
						MP.display(list);

						this.topics = topics;
						this.topicsList = list;
						infoLoaded = true;
						return false;
					}

					if (info.has("count")) {
						int members = info.getInt("count");
						canSeeRead = !broadcast && members < 100;
						defaultStatus = MP.localizePlural(members,
								broadcast ? L_subscriber : L_member);
						statusRender = null;
					}
				} else {
					user = true;
					canPin = true;
					canDelete = true;
					//noinspection AssignmentUsedAsCondition
					if (bot = info.getBoolean("bot", false)) {
						defaultStatus = MP.L[LBot];
					}
					if (MP.chatStatus && info.has("status")) {
						setStatus(info.getObject("status"));
					}
				}
			}
			infoLoaded = true;
		}

		this.selfChat = MP.selfId.equals(id);
		this.reverse = MP.reverseChat && mediaFilter == null;

		if (selfChat) {
			title = MP.L[LSavedMessages];
		} else if (postId != null /*|| topMsgId != 0*/) {
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
			int a = 0;
			do {
				try {
					safeAdd(thread, new UIMessage(message, this),
							this.messageId != 0 ? (messageId == id)
							: (i == 0 ? ((endReached && dir == 0) || dir == -1) : (i == l - 1 && dir == 1)));
					break;
				} catch (OutOfMemoryError e) {
					MP.gc();
					if (a++ == 0) continue;
					MP.display(MP.errorAlert(MP.L[LNotEnoughMemory_Alert]), this);
					return false;
				}
			} while (true);
		}

		if (l == limit && j.has("count")) {
			add(new UIPageButton(-1));
		}

		finished = true;

		if (thread != this.thread) return false;

		// postLoad
		loading = false;
		if (hasInput && (canWrite || left) && mediaFilter == null && query == null) {
			if (touch) {
				inputFieldHeight = Math.max(MP.medPlainFontHeight + 20, largeButtons ? 60 : 48);
			} else {
				inputFieldHeight = Math.max(MP.medPlainFontHeight + 16, 40);
			}
			if (forwardMsgs != null || forwardMsg != null) {
				bottomAnimProgress = bottom = inputFieldHeight + MP.smallBoldFontHeight + 8;
				if (!touch) inputFocused = true;
			} else if (!keyGuide) {
				bottomAnimProgress = bottom = touch ? inputFieldHeight : 0;
			}
		} else if (!keyGuide) {
			bottomAnimProgress = bottom = 0;
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
			if (MP.chatStatus) {
				(typingThread = new Thread(this)).start();
			}
		}
		if (botAnswer != null) {
			JSONObject b = botAnswer;
			botAnswer = null;
			handleBotAnswer(b);
		}
		showPhoto = MP.chatAvatar && !selfChat && mediaFilter == null && query == null && postId == null;
		if (showPhoto && photo == null && !photoQueued) {
			MP.queueImage(id, this);
			photoHeight = MP.avatarSize;
			photoQueued = true;
		}
//#ifndef NO_NOTIFY
//#ifndef NO_NOKIAUI
		try {
			Notifier.remove(id);
		} catch (Throwable ignored) {}
		MP.notificationMessages.remove(id);
//#endif
//#endif

		return true;
	}

	public void closed(boolean destroy) {
		super.closed(destroy);
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
		super.showNotify();
	}

	void cancel() {
		// close updater thread
		if (update) {
			update = false;
			if (!MP.globalUpdates && (MP.updatesThread != null || MP.updatesRunning)) {
				MP.midlet.cancel(MP.updatesThread, true);
			}
			if (typingThread != null) typingThread.interrupt();
		}

		shouldUpdate = false;
		super.cancel();
	}

	// Canvas

	protected boolean paintInternal(Graphics g, int w, int h, long now) {
		if (touch && !loading && lastDragDir == (reverse ? -1 : 1) && (scroll >= clipHeight || (!endReached && hasOffset))) {
			g.setColor(colors[COLOR_CHAT_PANEL_FG]);
			int tx = width - 40, ty = reverse ? height - bottom - 40 : top + 40;
			g.fillTriangle(tx, ty, tx + 32, ty, tx + 16, reverse ? ty + 32 : ty - 32);
			arrowShown = true;
		} else {
			arrowShown = false;
		}

		boolean animate = false;

		// top panel
		if (top != 0) {
			int th = top;
			g.setColor(colors[COLOR_CHAT_PANEL_BG]);
			g.fillRect(0, 0, w, th);
			g.setColor(colors[COLOR_CHAT_PANEL_BORDER]);
			g.drawLine(0, th, w, th);
//			if (h > 240) g.drawLine(0, th - 1, w, th - 1);

			int tx = 4;
			int tw = w - 8;
			if (touch) {
				g.setColor(colors[COLOR_CHAT_PANEL_FG]);
				tx = topButtonWidth;
				if (tx == 0) {
					tx = topButtonWidth = w < 320 ? 40 : largeButtons ? 60 : 50;
				}
				tw = w - 100;
				int bty = (th - 2) >> 1;
				if (selected != 0) g.setColor(colors[COLOR_CHAT_PANEL_FG]);
				// back icon
//				if (backIcon != null) {
//					int ax = (tx - 32) >> 1;
//					int ay = (th - 32) >> 1;
//					g.drawImage(backIcon, ax, ay, 0);
//				} else
				{
					int bx = (tx - 16) >> 1;
					g.fillRect(bx, bty, 16, 2);
					g.drawLine(bx, bty, bx + 7, bty-7);
					g.drawLine(bx, bty + 1, bx + 8, bty-7);
					g.drawLine(bx, bty, bx + 8, bty+8);
					g.drawLine(bx, bty + 1, bx + 7, bty+8);
				}
				if (showPhoto) {
					if (photo != null) g.drawImage(photo, tx, (th - photoHeight) >> 1, 0);
				}

				if (loading) { // do nothing
				} else if (selected != 0) {
					// selected messages options

					// delete
					int bx = w - tx + ((tx - 16) >> 1);
					g.drawLine(bx, bty - 8, bx + 16, bty + 8);
					g.drawLine(bx, bty + 8, bx + 16, bty - 8);

					// forward
					bx = w - tx * 2 + ((tx - 16) >> 1);
					g.drawLine(bx + 16, bty, bx, bty);
					g.drawLine(bx, bty, bx, bty + 6);
					g.drawLine(bx + 10, bty - 6, bx + 16, bty);
					g.drawLine(bx + 10, bty + 6, bx + 16, bty);

				} else if (/*query == null && */ mediaFilter == null) {
					// menu icon
//					if (moreIcon != null) {
//						int ax = w - tx + ((tx - 5) >> 1);
//						int ay = (th - 21) >> 1;
//						g.drawImage(moreIcon, ax, ay, 0);
//					} else {
					int bw = largeButtons ? 4 : 3;
					int bx = w - tx + ((tx - 3) >> 1);
					g.fillRect(bx, bty - (bw * 2), bw, bw);
					g.fillRect(bx, bty, bw, bw);
					g.fillRect(bx, bty + (bw * 2), bw, bw);
//					}
				}
			} else if (showPhoto) {
				if (photo != null) g.drawImage(photo, tx, (th - photoHeight) >> 1, 0);
			}
			if (showPhoto) {
				int p = photoHeight + (touch ? 8 : 4);
				tx += p;
				tw -= p;
			}
			boolean medfont = MP.chatStatus || touch;
			if (selected != 0 || mediaFilter != null || loading) {
				g.setFont(medfont ? MP.medPlainFont : MP.smallPlainFont);
				g.setColor(colors[COLOR_CHAT_PANEL_FG]);
				String s;
				if (loading) {
					s = MP.L[LLoading];
				} else if (selected != 0) {
					s = Integer.toString(selected);
				} else /*if (mediaFilter != null)*/ {
					if ("Photo".equals(mediaFilter)) {
						s = MP.L[LPhotos];
					} else if ("Video".equals(mediaFilter)) {
						s = MP.L[LVideos];
					} else if ("Document".equals(mediaFilter)) {
						s = MP.L[LFiles];
					} else if ("Music".equals(mediaFilter)) {
						s = MP.L[LAudioFiles];
					} else if ("Voice".equals(mediaFilter)) {
						s = MP.L[LVoiceMessages];
					} else {
						s = mediaFilter;
					}
				}
				g.drawString(s, tx, (th - (medfont ? MP.medPlainFontHeight : MP.smallPlainFontHeight)) >> 1, 0);
			} else {
				boolean hideStatus = medfont && (selfChat || postId != null || query != null);

				int tth = 0;
				tth += medfont ? MP.medPlainFontHeight : MP.smallPlainFontHeight;
				if (medfont && !hideStatus) tth += 2 + MP.smallBoldFontHeight;

				int ty = (th - tth) >> 1;
				if (title != null) {
					Font font = hideStatus ? MP.medPlainFont : MP.smallBoldFont;
					String title = titleRender;
					if (title == null) {
						titleRender = title = UILabel.ellipsis(query != null ? MP.L[LSearch] : this.title, font, tw - 4);
					}
					g.setColor(colors[COLOR_CHAT_PANEL_FG]);
					g.setFont(font);
					g.drawString(title, tx, ty, 0);
				}
				if (medfont && !hideStatus) {
					g.setColor(colors[typing[0] != 0 ? COLOR_CHAT_STATUS_HIGHLIGHT_FG : COLOR_CHAT_STATUS_FG]);
					g.setFont(MP.smallPlainFont);
					String status = statusRender;
					if (status == null) {
						status = this.status;
						if (status == null) {
							status = this.defaultStatus;
						}
						if (status != null) {
							statusRender = status = UILabel.ellipsis(status, MP.smallPlainFont, tw - 4);
						}
					}
					if (status != null) {
						g.drawString(status, tx, 2 + MP.smallBoldFontHeight + ty, 0);
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
				if (!touch) {
					int bh = 4 + MP.smallBoldFontHeight;
					g.setColor(colors[COLOR_CHAT_INPUT_BORDER]);
					g.drawLine(0, h - bh, w, h - bh);
					g.setColor(colors[COLOR_CHAT_PANEL_FG]);

					iy -= bh;
					bh -= 1;
					g.drawString(MP.L[LMenu], 2, h - bh, Graphics.TOP | Graphics.LEFT);
					g.drawString(MP.L[LEdit], w >> 1, h - bh, Graphics.TOP | Graphics.HCENTER);
					g.drawString(MP.L[keyboard != null && text != null && text.length() != 0
							&& keyboard.getPhysicalKeyboardType() == Keyboard.PHYSICAL_KEYBOARD_PHONE_KEYPAD ?
							LClear : LCancel], w - 2, h - bh, Graphics.TOP | Graphics.RIGHT);
				}
				if (bottomAnimTarget != -1) {
					// don't draw input field when animation is in progress
				} else if (canWrite && hasInput) {
					g.setFont(MP.smallBoldFont);
					g.setColor(colors[COLOR_CHAT_SEND_ICON]);
					int ry = h - bottom;
					int c = 0;
					int rh = MP.smallBoldFontHeight + 8;
					if (replyMsgId != 0) {
						g.drawString(MP.L[LReply], 2, ry + 4, 0);
						ry += rh;
						c++;
					}
					if (editMsgId != 0) {
						g.drawString(MP.L[LEdit], 2, ry + 4, 0);
						ry += rh;
						c++;
					}
					if (forwardMsgs != null || forwardMsg != null) {
						g.drawString(MP.L[LForward], 2, ry + 4, 0);
						ry += rh;
						c++;
					}
					if (file != null) {
						String file = fileRender;
						if (file == null) {
							String s = MP.L[LFile_Prefix].concat(this.file.substring(this.file.lastIndexOf('/') + 1));
							fileRender = file = UILabel.ellipsis(s, MP.smallBoldFont, w - 28);
						}
						g.drawString(file, 2, ry + 4, 0);
						ry += rh;
						c++;
					}
					if (ih != bottom) {
						g.setColor(colors[COLOR_CHAT_INPUT_BORDER]);
						g.drawLine(0, iy, w, iy);

						if (touch) {
							// cancel icon
							g.setColor(colors[COLOR_CHAT_INPUT_ICON]);
							ry = h - bottom;
							for (int i = 0; i < c; ++i) {
								int ty = ry + ((rh - 12) >> 1);
								g.drawLine(w - 20, ty, w - 8, ty + 12);
								g.drawLine(w - 20, ty + 12, w - 8, ty);
								ry += rh;
							}
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
									int yo = ih / 6;
									NokiaAPI.TextEditor_setSize(nokiaEditor, 10, iy + yo, w - topButtonWidth - 8, ih - yo);
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

					int aw = topButtonWidth;
					if ((text != null && text.trim().length() != 0) || file != null || forwardMsgs != null || forwardMsg != null) {
						// send icon
						int ty = iy + ((ih - 20) >> 1);

						// TODO: better sending indication
						if (MP.sending) { // disabled
							g.setColor(colors[COLOR_CHAT_INPUT_ICON]);
						} else {
							g.setColor(colors[COLOR_CHAT_SEND_ICON]);
						}

						int ax = w - aw + ((aw - 20) >> 1);

						g.fillTriangle(ax, ty, ax + 20, ty + 10, ax, ty + 20);
						g.setColor(colors[COLOR_CHAT_PANEL_BG]);
						g.fillTriangle(ax, ty, ax + 2, ty + 10, ax, ty + 20);
						g.drawLine(ax, ty + 10, ax + 10, ty + 10);
					} else if (touch) {
						// attach icon

						if (attachIcon != null) {
							int ax = w - aw + ((aw - 24) >> 1);
							int ay = iy + ((ih - 24) >> 1);
							g.drawImage(attachIcon, ax, ay, 0);
						} else {
						g.setColor(colors[COLOR_CHAT_INPUT_ICON]);
//						if (largeButtons) {
//							int ax = w - aw + ((aw - 24) >> 1);
//							int ay = iy + ((ih - 26) >> 1);
//							g.fillRect(ax, ay + 12, 24, 2);
//							g.fillRect(ax + 11, ay + 1, 2, 24);
//						} else {
							int ax = w - aw + ((aw - 17) >> 1);
							int ay = iy + ((ih - 24) >> 1);
							g.fillRect(ax, ay + 12, 17, 1);
							g.fillRect(ax + 8, ay + 4, 1, 17);
						}
//						}
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

		return animate;
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
			keyGuide = false;
			//noinspection AssignmentUsedAsCondition
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

	protected void tap(int x, int y) {
		if (y < top) {
			if (x < topButtonWidth) { // back button
				if (keyboard != null && keyboard.isVisible()) {
					onKeyboardCancel();
				} else {
					keyPressed(-7);
				}
			} else if (selected != 0) { // selected messages actions
				if (x > width - topButtonWidth) {
					deleteSelected();
				} else if (x > width - (topButtonWidth * 2)) {
					forwardSelected();
				}
			} else if (loading) { // do nothing
			} else if (x > width - topButtonWidth) { // menu button
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
					boolean cancel = x > width - topButtonWidth;
					if (file != null && y - height + inputFieldHeight + MP.medPlainFontHeight + 8 > 0) {
						if (cancel) {
							file = null;
							focusInput(false);
						} else {
							// TODO file options
						}
					} else if (cancel) {
						if (editMsgId != 0 || forwardMsgs != null || forwardMsg != null) {
							resetInput();
						} else {
							replyMsgId = 0;
							keyGuide = false;
							focusInput(false);
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

	protected void scrolled() {
		focusedMessage = -1;
	}

	protected void resized() {
		titleRender = null;
		fileRender = null;
		topButtonWidth = 0;
	}

	protected boolean handleSwipe() {
		if (selected != 0) return false;
		// swipe gestures
		int d = pointerX - pressX;
		if (d > 50) {
			MP.midlet.commandAction(MP.backCmd, this);
		} else if (d < -50 && pointedItem instanceof UIMessage) {
			startReply((UIMessage) pointedItem);
		}
		return true;
	}

	protected boolean handleLeftSoft() {
		if (inputFocused) {
			showMenu(null,
					(text != null && text.trim().length() != 0) || file != null || forwardMsgs != null || forwardMsg != null ?
							new int[] {
									LSend,
									LFullscreenTextBox,
									LClear,
//#ifndef NO_FILE
									LAttachFile,
//#endif
									LCancel
							} :
							new int[] {
									LFullscreenTextBox,
//#ifndef NO_FILE
									LAttachFile,
//#endif
									LCancel
							});
			return true;
		} else if (selected != 0) {
			showMenu(null, new int[] { LDelete, LForward });
			return true;
		} else if (funcFocused) {
			showMenu(null, canWrite && hasInput ? new int[] {
					LRefresh,
					LChatInfo,
					LSearchMessages,
					LSendSticker,
//#ifndef NO_FILE
					LAttachFile,
//#endif
					LWriteMessage
			} : new int[] { LRefresh, LChatInfo, LSearchMessages });
			return true;
		}
		return false;
	}

	protected boolean handleRightSoft() {
		if (inputFocused) {
			back();
		} else if (keyboard != null && keyboard.isVisible()) {
			onKeyboardCancel();
			return true;
		} else if (touch || query != null || mediaFilter != null || selected != 0 || loading) {
			back();
			return true;
		} else if (funcFocused) {
//				fieldFocused = false;
//				fieldAnimTarget = 0;
			back();
			return true;
		}
		return false;
	}

	protected boolean handleKey(int key, int game) {
		if (inputFocused && game >= 0) {
			if (key == -5 || game == Canvas.FIRE) {
//				if (keyboard == null) {
				showTextBox();
			}
			return true;
		} else if (funcFocused && game >= 0) {
			if (game == Canvas.UP) {
				funcFocused = false;
				keyGuide = false;
				bottomAnimTarget = 0;
				queueRepaint();
			} else if (key == -5 || game == Canvas.FIRE) {
				if (canWrite && hasInput) {
					focusInput(true);
				}
			}
			return true;
		} else if (key == Canvas.KEY_NUM2 || key == Canvas.KEY_NUM8) {
			int dir = key == Canvas.KEY_NUM2 ? -1 : 1;
			if (reverse) dir = -dir;
			focusItem(null, 0);
			focusedItem = scrollCurrentItem = scrollTargetItem = null;
			scrollTo(scroll + ((clipHeight * 7 / 8) * dir));
			queueRepaint();
			return true;
		} else if (key == Canvas.KEY_NUM1) {
			// search
			if (canWrite && hasInput) MP.midlet.commandAction(MP.searchMsgCmd, this);
			return true;
		} else if (key == Canvas.KEY_NUM3) {
			// chat info
			openProfile();
			return true;
		} else if (key == Canvas.KEY_NUM4) {
			// write message
			resetInput();
			MP.midlet.commandAction(MP.writeCmd, this);
			return true;
		} else if (key == Canvas.KEY_NUM6) {
			// refresh
			MP.midlet.commandAction(MP.latestCmd, this);
			return true;
		} else if (game == -10) {
			send();
			queueRepaint();
			return true;
		} else if (game == -11) {
			back();
			return true;
		}
		return false;
	}

	private void showTextBox() {
		if (text == null) text = "";
		TextBox t = new TextBox("", text, 500, TextField.ANY);
		t.addCommand(MP.okCmd);
		t.addCommand(MP.cancelCmd);
		t.setCommandListener(MP.midlet);

		MP.display(t);
	}

	protected void menuAction(int i) {
		switch (i) {
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
			if (replyMsgId != 0 || editMsgId != 0 || topMsgId != 0) {
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
//#ifndef NO_FILE
		case LAttachFile: {
			MP.openFilePicker(MP.lastUploadPath, 3);
			break;
		}
//#endif
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

	void safeAdd(Thread thread, UIItem item, boolean focus) {
		super.safeAdd(thread, item, focus);
		UIMessage msg = (UIMessage) item;
		table.put(Integer.toString(msg.id), item);
		if (focus && (!endReached || firstMsgId != msg.id)) {
			focusedMessage = msg.id;
		}
	}

	void safeAddFirst(Thread thread, UIItem item) {
		super.safeAddFirst(thread, item);
		table.put(Integer.toString(((UIMessage) item).id), item);
	}

	void remove(UIItem item) {
		super.remove(item);
		if (item instanceof UIMessage) {
			if (((UIMessage) item).selected)
				-- selected;
			table.remove(Integer.toString(((UIMessage) item).id));
		}
	}

	private void openProfile() {
		MP.openLoad(new ChatInfoForm(id, this, 0));
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
				MP.localizeFormattedPlural(msgs.length, MP.LDeleteNMessage_Alert));
	}

	private void forwardSelected() {
		UIMessage[] msgs = getSelected();
		unselectAll();

//		MP.openLoad(new ChatsList(id, msgs));
		MP.openLoad(new ChatsCanvas(id, msgs));
	}

	public void startEdit(UIMessage item) {
		resetInput();
		text = item.origText;
		editMsgId = item.id;
		focusInput(true);
	}

	public void startReply(UIMessage item) {
		if (editMsgId != 0 || forwardMsgs != null || forwardMsg != null) resetInput();
		replyMsgId = item.id;
		focusInput(true);
	}

	public void setFile(String s) {
		file = s;
		fileRender = null;
		focusInput(false);
	}

	public void startForward(String peer, String msg, UIMessage[] msgs) {
		resetInput();
		MP.display(this);
		this.forwardPeer = peer;
		this.forwardMsg = msg;
		this.forwardMsgs = msgs;
		focusInput(true);
	}

	private void focusInput(boolean showKeyboard) {
		int h = inputFieldHeight;
		if (replyMsgId != 0 || editMsgId != 0 || forwardMsgs != null || forwardMsg != null) {
			h += MP.smallBoldFontHeight + 8;
		}
		if (file != null) {
			h += MP.smallBoldFontHeight + 8;
		}
		if (text == null) text = "";
		if (!touch) {
			bottomAnimTarget = h + MP.smallBoldFontHeight + 4;
			keyGuide = false;
			inputFocused = true;
			funcWasFocused = funcFocused;
		} else {
//#ifndef NO_NOKIAUI
			if (nokiaEditor != null) {
				NokiaAPI.TextEditor_setContent(nokiaEditor, text);
				editorShown = true;
				updateEditor = true;
				bottomAnimProgress = bottom = h;
			} else
//#endif
			{
				bottomAnimTarget = h;
			}
		}

		if (keyboard != null) {
			keyboard.setText(text);
			if (showKeyboard) keyboard.show();
		}
		queueRepaint();
	}

	private void send() {
		if (!canWrite || !hasInput) return;
		if (!touch && !inputFocused) {
			focusInput(true);
		} else if ((text != null && text.trim().length() != 0) || file != null || forwardMsgs != null || forwardMsg != null) {
			synchronized (this) {
				if (!MP.sending) {
					MP.sending = true;
					int r = Math.max(replyMsgId, topMsgId);
					MP.midlet.start(MP.RUN_SEND_MESSAGE, new Object[] {
							text, id,
							r == 0 ? null : Integer.toString(r),
							editMsgId == 0 ? null : Integer.toString(editMsgId),
							file,
							null, forwardPeer, forwardMsg, forwardMsgs
					});
				}
			}
		} else {
			showMenu(null, new int[] {
//#ifndef NO_FILE
					LAttachFile,
//#endif
					LSendSticker,
			});
		}
	}

	private void resetInput() {
		text = "";
		replyMsgId = 0;
		editMsgId = 0;
		file = null;
		if (touch) {
			bottomAnimTarget = inputFieldHeight;
		} else {
			int h = funcFocused ? MP.smallBoldFontHeight + 4 : 0;
			if (inputFocused) {
				h += inputFieldHeight;
			}
			bottomAnimTarget = h;
		}
		keyGuide = false;
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
		typing[0] = 0;
		query = null;
		selected = 0;
		if (table != null) table.clear();
		switched = false;
		shouldUpdate = false;
		mediaFilter = null;
		focusItem(null, 0);
		firstItem = lastItem = null;
		kineticScroll = scroll = 0;
		skipRender = false;
		menuFocused = false;
		inputFocused = false;
		funcWasFocused = false;
		funcFocused = false;
	}

	public void openMessage(String msg, int topMsg) {
		int id = Integer.parseInt(msg);
		if (table != null && table.containsKey(msg)) {
			UIItem focus = (UIItem) table.get(msg);
			if (focus != null) {
				nextFocusItem = focus;
				focusedMessage = id;
				return;
			}
		}
		resetChat();
		this.messageId = id;
		if (topMsg != -1) this.topMsgId = topMsg;
		MP.openLoad(this);
	}

	public void sent() {
		resetInput();
		if (!MP.reopenChat && MP.longpoll && update) queueRepaint();
	}

	private void removeTyper(String id) {
		if (user) {
			typing[0] = 0;
			return;
		}

		if (id == null) return;
		long l = Long.parseLong(id);
		synchronized (typing) {
			for (int i = 0; i < 5; ++i) {
				int idx = (i << 1) + 1;
				if (typing[idx] != l) continue;
				typing[idx] = 0;
				typing[idx + 1] = 0;
				typing[0]--;
				if (i != 4) {
					System.arraycopy(typing, idx + 2, typing, idx, typing.length - idx - 2);
				}
			}
		}
	}

	private void updateTypingStatus() {
		if (user) {
			if (typing[0] != 0) {
				this.status = MP.L[LTyping];
			}
			return;
		}

		long id1 = 0, id2 = 0;
		int count = 0;
		synchronized (typing) {
			count = (int) typing[0];
			for (int i = 0; i < 5; ++i) {
				int idx = (i << 1) + 1;
				if (typing[idx] == 0) continue;
				if (id1 == 0) {
					id1 = typing[idx];
				} else {
					id2 = typing[idx];
				}
			}
		}

		String name1 = id1 == 0 ? null : MP.getName(Long.toString(id1), true);
		if (count == 1) {
			this.status = MP.localizeFormatted(L_isTyping, name1);
		} else if (count == 2) {
			String l = MP.L[L_areTyping];
			String name2 = MP.getName(Long.toString(id2), true);
			int idx1 = l.indexOf('%');
			int idx2 = l.indexOf('%', idx1 + 1);
			this.status = l.substring(0, idx1).concat(name1)
					.concat(l.substring(idx1 + 1, idx2).concat(name2).concat(l.substring(idx2 + 1))
			);
		} else if (count > 4) {
			this.status = MP.L[LManyPeopleAreTyping];
		} else {
			this.status = MP.localizeFormattedPlural(count, L_peopleAreTyping);
		}
	}

	public void handleUpdate(int type, JSONObject update) {
		if (!this.update) return;
		switch (type) {
		case UPDATE_USER_STATUS: {
			if (!MP.chatStatus) break;

			setStatus(update.getObject("status"));
			typing[0] = 0;
			if (typingThread != null) typingThread.interrupt();
			break;
		}
		case UPDATE_USER_TYPING: {
			if (!MP.chatStatus) break;

			if ("sendMessageCancelAction".equals(update.getObject("action").getString("_"))) {
				setStatus(null);
				if (user) {
					typing[0] = 0;
				} else {
					removeTyper(update.getString("from_id"));
				}
				if (typingThread != null) typingThread.interrupt();
				break;
			}
			if (id.charAt(0) == '-' && update.has("top_msg_id") && topMsgId != update.getInt("top_msg_id"))
				break;
			statusRender = null;

			long now = System.currentTimeMillis();
			if (user) {
				this.status = MP.L[LTyping];
				typing[0] = now;
			} else {
				synchronized (typing) {
					long fromid = Long.parseLong(update.getString("from_id"));
					add: {
						for (int i = 0; i < 5; ++i) {
							int idx = (i << 1) + 1;
							boolean replace = false;
							if (typing[idx] != 0) {
								if (now - typing[idx + 1] < 4000 && typing[idx] != fromid) {
									continue;
								} else replace = true;
							}

							typing[idx] = fromid;
							typing[idx + 1] = now;
							if (!replace) typing[0]++;
							break add;
						}
						typing[1] = fromid;
						typing[2] = now;
					}
				}
				updateTypingStatus();
			}
			if (typingThread != null) typingThread.interrupt();
			synchronized (typingLock) {
				typingLock.notify();
			}
			queueRepaint();
			break;
		}
		case UPDATE_NEW_MESSAGE: {
			// check for duplicate
			JSONObject msg = update.getObject("message");
			if (msg.getInt("id") == firstMsgId)
				break;

			if (user) {
				typing[0] = 0;
			} else if (typing[0] != 0) {
				removeTyper(msg.getString("from_id", null));
			}
			if (typingThread != null) typingThread.interrupt();

			// delete old messages
			while (count >= limit) {
				remove(lastItem instanceof UIPageButton ? lastItem.prev : lastItem);
			}

			safeAddFirst(null, new UIMessage(msg, this));
			firstMsgId = msg.getInt("id");
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
			JSONObject msg = update.getObject("message");

			if (user) {
				typing[0] = 0;
			} else if (typing[0] != 0) {
				removeTyper(msg.getString("from_id", null));
			}
			if (typingThread != null) typingThread.interrupt();

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
			MP.openUrl(j.getString("url"), true);
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

	public void invite(String id) {
		MP.midlet.start(Constants.RUN_INVITE_MEMBER, new String[] {
				channel ? "inviteToChannel" : "addChatUser",
				this.id,
				id
		});
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
		if (!touch) {
			send();
			return;
		}
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
			statusRender = null;
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
		statusRender = null;
		queueRepaint();
	}

	// typing timer loop
	public void run() {
		try {
			while (update) {
				try {
					if (typing[0] == 0) {
						synchronized (typingLock) {
							typingLock.wait(60000);
						}
					}
					if (user) {
						//noinspection BusyWait
						Thread.sleep(5000);
						typing[0] = 0;
					} else {
						long now = System.currentTimeMillis();
						synchronized (typing) {
							for (int i = 0; i < 5; ++i) {
								int idx = (i << 1) + 1;
								if (typing[idx] == 0) continue;
								if (now - typing[idx + 1] < 5000) continue;
								typing[idx] = 0;
								typing[idx + 1] = 0;
								typing[0]--;
								if (i != 4) {
									System.arraycopy(typing, idx + 2, typing, idx, typing.length - idx - 2);
								}
								updateTypingStatus();
							}
						}
						//noinspection BusyWait
						Thread.sleep(1000);
					}
				} catch (Exception ignored) {}
				if (typing[0] == 0) {
					setStatus(null);
				}
			}

			setStatus(null);
			typing[0] = 0;
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
