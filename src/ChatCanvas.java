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

import java.util.Hashtable;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Ticker;

public class ChatCanvas extends Canvas implements MPChat, LangConstants, Runnable {

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
	
	boolean switched;
	boolean update;
	
	boolean selfChat;
	boolean user;
	boolean reverse;
	
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
	UIItem firstMessage, lastMessage;
	UIItem layoutStart;
	
	// boundaries
	int width, height;
	int clipHeight, top, bottom;
	int contentHeight;
	
	// scroll
	int scroll;
	int scrollTarget;
	int lastScrollDir;
	UIItem scrollCurrentItem, scrollTargetItem;
	UIItem focusedItem;
	UIItem nextFocusItem;
	float kineticScroll;
	
	// pointer
	int pressX, pressY, pointerX, pointerY;
	boolean pressed, dragging, longTap, contentPressed;
	long pressTime;
	int dragYHold;
	UIItem pointedItem;
	
	static final int moveSamples = 5;
	int[] moves = new int[moveSamples];
	long[] moveTimes = new long[moveSamples];
	int movesIdx;
	
	long lastPaintTime;
	boolean animating;
	
	// animations
	boolean fieldFocused;
	int fieldAnimTarget = -1;
	float fieldAnimProgress;
	boolean keyGuide;
	long keyGuideTime;
	
	int menuAnimTarget = -1;
	float menuAnimProgress;
	
	boolean loading;
	
	boolean touch = hasPointerEvents();
	
	// menu
	boolean menuFocused;
	UIItem menuItem;
	int[] menu;
	int menuCurrent, menuCount;
	
	String titleRender;
	
	ChatCanvas() {
		setFullScreenMode(true);
		if (touch) {
			top = MP.smallBoldFontHeight + MP.smallPlainFontHeight + 8;
		} else {
			top = MP.smallBoldFontHeight + 4 + (MP.chatStatus ? MP.smallPlainFontHeight + 4 : 0);
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
	
	// post discussion
	public ChatCanvas(String id, String postPeer, String postId, int readMaxId) {
		this();
		this.id = id;
		this.postPeer = postPeer;
		this.postId = postId;
		this.messageId = readMaxId;
		init(true);
	}
	
	private void init(boolean field) {
	}

	public void load() {
		if (loaded) return;
		loaded = true;
		canceled = finished = false;
		
		loading = true;
		Thread thread = this.thread = Thread.currentThread();
		try {
			// remove all
			count = 0;
			firstMessage = lastMessage = null;
			scrollCurrentItem = scrollTargetItem = focusedItem = null;
			
			if ((MP.reopenChat || (query == null && mediaFilter == null))
					&& MP.chatUpdates
					&& (MP.updatesThread != null || MP.updatesRunning)) {
				MP.display(MP.loadingAlert(MP.L[WaitingForPrevChat]), this);
				
				MP.cancel(MP.updatesThreadCopy, true);
				while (MP.updatesThread != null || MP.updatesRunning) {
					Thread.sleep(1000L);
				}
				
				if (MP.current == this) {
					MP.display(MP.useLoadingForm ? (Displayable) MP.loadingForm : this);
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
	
				title = MP.getName(id, false);
	
				if (mediaFilter == null) {
					canWrite = !broadcast;
					JSONObject info = (JSONObject) MP.api((messageId == -1 && !forum ? "getFullInfo&id=" : "getInfo&id=").concat(id));
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
					} else {
						user = true;
						canPin = true;
						if (MP.chatStatus && info.getObject("User").has("status")) {
							setStatus(info.getObject("User").getObject("status"));
						}
					}
					JSONObject full = info.getObject("full");
					if (messageId == -1 && full.has("read_inbox_max_id")) {
						messageId = 0;
						int maxId = full.getInt("read_inbox_max_id");
						if (maxId != 0 && full.getInt("unread_count", 0) > limit) {
							offsetId = messageId = maxId;
							addOffset = -limit;
							dir = 1;
						}
					}
					if (full.has("participants_count")) {
						defaultStatus = MP.localizePlural(full.getInt("participants_count"),
								broadcast ? _subscriber : _member);
					}
				}
				infoLoaded = true;
			}

			this.selfChat = MP.selfId.equals(id);
			this.reverse = MP.reverseChat && mediaFilter == null;
			
			if (selfChat) {
				title = MP.L[SavedMessages];
			} else if (postId != null || topMsgId != 0) {
				title = MP.L[Comments];
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
					setTitle(MP.L[ChatMedia_Title]);
				}
				if (query != null) {
					if (mediaFilter == null) {
						setTitle(MP.L[Search_TitlePrefix].concat(title));
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
			if (offsetId != 0) {
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
			
			for (int i = 0; i < l; i++) {
				JSONObject message = messages.getObject(i);
				int id = message.getInt("id");
				if (i == 0) {
					firstMsgId = id;
				} else if (i == l - 1) {
					lastMsgId = id;
				}
				safeAdd(thread, new UIMessage(message, this),
						this.messageId != 0 ? (messageId == id)
						: (i == 0 ? ((endReached && dir == 0) || dir == -1) : (i == l - 1 && dir == 1)));
			}
			
			finished = true;
	
			if (thread != this.thread) return;
			
			// postLoad
			loading = false;
			if (touch && (canWrite || left)) {
				bottom = Math.max(MP.medPlainFontHeight + 16, 48);
			}
			layoutStart = firstMessage;
			if (endReached && !hasOffset
					&& query == null && mediaFilter == null
					&& MP.chatUpdates && !update) {
				// start updater thread
				update = true;
				MP.midlet.start(MP.RUN_CHAT_UPDATES, this);
				(typingThread = new Thread(this)).start();
			}
			if (botAnswer != null) {
				JSONObject b = botAnswer;
				botAnswer = null;
				handleBotAnswer(b);
			}
//#ifndef NO_NOTIFY
			try {
				Notifier.remove(id);
			} catch (Throwable ignored) {}
			MP.notificationMessages.remove(id);
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
	}
	
	public void showNotify() {
//		if (!touch && keyGuideTime == 0) {
//			keyGuide = true;
//			fieldAnimTarget = MP.smallBoldFontHeight + 2;
//		}
		repaint();
	}
	
	void cancel() {
		// close updater thread
		if (update) {
			update = false;
			if (MP.updatesThread != null || MP.updatesRunning) {
				MP.cancel(MP.updatesThread, true);
			}
		}
		
		loaded = false;
		if (finished || thread == null) return;
		canceled = true;
		MP.cancel(thread, false);
		thread = null;
	}
	
	// Canvas

	protected void paint(Graphics g) {
		int w = getWidth(), h = getHeight();
		g.setClip(0, 0, w, h);
		
		if (loading) {
			g.setColor(0x0E1621);
			g.fillRect(0, 0, w, h);
			g.setColor(-1);
			g.setFont(MP.medPlainFont);
			g.drawString(MP.L[Loading], w >> 1, h >> 1, Graphics.TOP | Graphics.HCENTER);
			return;
		}
		
		int contentHeight = this.contentHeight;
		
		long now = System.currentTimeMillis();
		long deltaTime = now - lastPaintTime;
		if (deltaTime > 500) deltaTime = 500;
		if (width != w) {
			layoutStart = firstMessage;
			titleRender = null;
		}
		width = w; height = h;
		
		boolean animate = false;

		// animations
		
		if (fieldAnimTarget != -1) {
			if (slide(1, fieldAnimProgress, fieldAnimTarget, deltaTime)) {
				animate = true;
			} else {
				fieldAnimProgress = bottom = fieldAnimTarget;
				fieldAnimTarget = -1;
			}
		}
		if (menuAnimTarget != -1) {
			if (slide(2, menuAnimProgress, menuAnimTarget, deltaTime)) {
				animate = true;
			} else {
				menuAnimProgress = menuAnimTarget;
				menuAnimTarget = -1;
			}
		}
		
		int clipHeight = this.clipHeight = h - top - bottom;
		
		// layout
		if (layoutStart != null) {
			UIItem idx = layoutStart;
			layoutStart = null;
			layout(idx, w, clipHeight);
			contentHeight = this.contentHeight;
		}
		
		// focus
		
		if (nextFocusItem != null) {
			setCurrentItem(nextFocusItem);
			nextFocusItem = null;
		}
		
		if (!touch && scrollTarget == -1) {
			if (focusedItem == null && scrollCurrentItem == null && scrollTargetItem == null) {
				focusItem(getFirstFocusableItemOnScreen(null, 1, clipHeight / 5), 1);
			} else if (focusedItem != null && scrollCurrentItem == null && !isVisible(focusedItem)) {
				scrollTo(focusedItem);
			}
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
		
		// touch scroll
		
		if (kineticScroll != 0) {
			if ((kineticScroll > -1 && kineticScroll < 1)
					|| contentHeight <= clipHeight
					|| scroll <= 0
					|| scroll >= contentHeight - clipHeight) {
				kineticScroll = 0;
			} else {
				float mul = pressed ? 0.5f : 0.96f;
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
		
		// limit scroll

		int scroll = this.scroll;
		if (scroll < 0) this.scroll = scroll = 0;
		else if (contentHeight <= clipHeight) this.scroll = scroll = 0;
		else if (scroll > contentHeight - clipHeight) this.scroll = scroll = contentHeight - clipHeight;
//		System.out.println("scroll " + scroll + " / " + (contentHeight - clipHeight));
		
		// background
		g.setColor(0x0E1621);
		g.fillRect(0, 0, w, h);
		g.setColor(-1);
		
		// render items
		
		UIItem msg = firstMessage;
		if (msg != null) {
			g.setClip(0, top, w, clipHeight);
			if (reverse) {
				clipHeight += top;
				int y = h - bottom + scroll;
				do {
					if (y < 0) break;
					y -= msg.contentHeight;
					if (y > clipHeight) continue;
					msg.paint(g, 0, y, w);
				} while ((msg = msg.next) != null);
			} else {
				int y = top - scroll;
				clipHeight += bottom;
				do {
					int ih = msg.contentHeight;
					if (y < -ih) {
						y += ih;
						continue;
					}
					msg.paint(g, 0, y, w);
					if ((y += ih) > clipHeight) break;
				} while ((msg = msg.next) != null);
			}
		}
		
		g.setClip(0, 0, w, h);
		
		// top panel
		if (top != 0) {
			int th = top;
			g.setColor(0x17212B);
			g.fillRect(0, 0, w, th);
			g.setColor(0x0A121B);
			g.drawLine(0, th, w, th);
			
			int tx = 4;
			int tw = w - 8;
			if (touch) {
				g.setColor(-1);
				tx = 40;
				tw = w - 80;
				int bty = (th - 2) >> 1;
				// back button
				g.drawLine(12, bty, 28, bty);
				g.drawLine(12, bty, 20, bty-8);
				g.drawLine(12, bty, 20, bty+8);
				
				// menu button
				g.drawLine(w - 30, bty - 8, w - 10, bty - 8);
				g.drawLine(w - 30, bty, w - 10, bty);
				g.drawLine(w - 30, bty + 8, w - 10, bty + 8);
			}
			boolean showStatus = MP.chatStatus || touch;
			if (title != null) {
				boolean noStatus = showStatus && status == null && defaultStatus == null;
				Font font = noStatus ? MP.medPlainFont : MP.smallBoldFont;
				if (titleRender == null) {
					titleRender = UILabel.ellipsis(title, font, tw - 4);
				}
				g.setColor(-1);
				g.setFont(font);
				g.drawString(titleRender, tx, showStatus ? noStatus ? (th - MP.medPlainFontHeight) >> 1 : 4 : 2, 0);
			}
			// TODO status ellipsis
			if (showStatus) {
				g.setColor(typing != 0 ? 0x73B9F5 : 0x708499);
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
		
		// bottom panel
		if (bottom != 0) {
			g.setColor(0x17212B);
			int by = h - bottom;
			g.fillRect(0, by, w, bottom);
			g.setColor(0x0A121B);
			g.drawLine(0, by, w, by);
			if (fieldFocused) {
				// TODO
				by += 1;
				g.setColor(-1);
				g.drawString(MP.L[Chat], 2, by, Graphics.TOP | Graphics.LEFT);
				g.drawString(MP.L[Back], w - 2, by, Graphics.TOP | Graphics.RIGHT);
				g.drawString(MP.L[WriteMessage], w >> 1, by, Graphics.TOP | Graphics.HCENTER);
			} else if (keyGuide) {
				animate = true;
				g.setColor(-1);
				g.drawString("Menu", 2, by + 1, Graphics.TOP | Graphics.LEFT);
				g.drawString(MP.L[Back], w - 2, by + 1, Graphics.TOP | Graphics.RIGHT);
				if (keyGuideTime == 0) {
					keyGuideTime = now;
				} else if (now - keyGuideTime > 3000) {
					fieldAnimTarget = 0;
					keyGuide = false;
				}
			}
		}
		
		// popup menu TODO
		if (menuAnimProgress != 0) {
			int my = h - (int)menuAnimProgress;
			g.setColor(0x17212B);
			g.fillRect(0, my, w, (int)menuAnimProgress);
			if (menu != null) {
				int[] menu = this.menu;
				g.setFont(MP.medPlainFont);
				for (int i = 0; i < menu.length; i++) {
					if (menu[i] == Integer.MIN_VALUE) break;
					if (i == menuCurrent && (!touch || menuCurrent != -1)) {
						g.setColor(0x232E3C);
						g.fillRect(0, my, w, MP.medPlainFontHeight + 8);
					}
					g.setColor(-1);
					g.drawString(MP.L[menu[i]], 4, my + 4, 0);
					my += MP.medPlainFontHeight + 8;
//					g.setColor(0x232F39);
//					g.drawLine(0, my, w, my);
				}
			}
		}
		
		// process long tap
		if (pressed && !dragging && !longTap
				&& pointedItem != null && pointedItem.focusable) {
			animate = true;
			if (now - pressTime > 200) {
				g.setColor(-1);
				int size = Math.min(360, (int) (now - pressTime - 200) / 2);
				g.fillArc(pointerX - 25, pointerY - 25, 50, 50, 90, size);
				if (size >= 360) {
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
					val = MP.lerp(val, target, 4, 20);
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
			bottom = (int) (fieldAnimProgress = val);
		} else /* (mode == 2) */ {
			menuAnimProgress = val;
		}
		
		return true;
	}

	protected void keyPressed(int key) {
		if (!loading) key(key, false);
	}
	
	protected void keyRepeated(int key) {
		// TODO own repeater thread
		if (!loading) key(key, true);
	}
	
	protected void keyReleased(int key) {
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
			return getGameAction(key);
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
		boolean repaint = false;
		if (key == -7) {
			if (repeat) return;
			// back
			if (touch) {
				MP.midlet.commandAction(MP.backCmd, this);
				return;
			} else if (menuFocused) {
				menuFocused = false;
				menuAnimTarget = 0;
				menuItem = null;
				menu = null;
			} else if (fieldFocused) {
//				fieldFocused = false;
//				fieldAnimTarget = 0;
				MP.midlet.commandAction(MP.backCmd, this);
				return;
			} else {
				fieldFocused = true;
				fieldAnimTarget = MP.smallBoldFontHeight + 4;
			}
			repaint = true;
		} else if (key == -6) {
			if (repeat) return;
			// menu
			if (menuFocused) {
				menuFocused = false;
				menuAnimTarget = 0;
				menuItem = null;
				menu = null;
			} else if (fieldFocused) {
				showMenu(null, canWrite ? new int[] { Refresh, ChatInfo, SendSticker } : new int[] { Refresh, ChatInfo });
			} else {
				if (focusedItem != null && focusedItem.focusable) {
					int[] menu = focusedItem.menu();
					if (menu != null && menu.length != 0) {
						menuFocused = true;
						showMenu(focusedItem, menu);
					}
				}
			}
			repaint = true;
		} else if (menuFocused) {
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
			}
		} else if (fieldFocused) {
			if (game == Canvas.UP) {
				fieldFocused = false;
				fieldAnimTarget = 0;
				repaint = true;
			} else if (key == -5 || game == Canvas.FIRE) {
				// TODO text field
				MP.midlet.commandAction(MP.writeCmd, this);
			}
		} else if (key == -5 || game == Canvas.FIRE) {
			// action
			if (focusedItem != null) {
				if (focusedItem.action()) {
					repaint = true;
				}
			}
		} else if (key == Canvas.KEY_NUM2) {
			focusItem(null, 0);
			focusedItem = scrollCurrentItem = scrollTargetItem = null;
			scrollTo(scroll - clipHeight);
			repaint = true;
		} else if (key == Canvas.KEY_NUM8) {
			focusItem(null, 0);
			focusedItem = scrollCurrentItem = scrollTargetItem = null;
			scrollTo(scroll + clipHeight);
			repaint = true;
		} else if (key >= Canvas.KEY_NUM0 && key <= Canvas.KEY_NUM9) {
			// ignore
		} else if (game == Canvas.DOWN || game == Canvas.UP) {
			scroll: {
				int dir = game == Canvas.DOWN ? 1 : -1;
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
					int t = focusedItem.traverse(game, clipHeight, scroll);
					repaint = true;
					if (t != 0) {
						repaint = true;
						if (t != Integer.MAX_VALUE) {
							scrollTo(t);
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
					focusItem(item, reverse ? -dir : dir);
				}
				if (scrollTargetItem != null && isVisible(scrollTargetItem)) {
					repaint = true;
					focusItem(scrollTargetItem, reverse ? -dir : dir);
					scrollCurrentItem = scrollTargetItem;
					if (isCornerVisible(scrollTargetItem, dir) && isVisible(scrollTargetItem)) {
						scrollTargetItem = getFirstFocusableItemOnScreen(scrollCurrentItem, dir, 0);
						if (scrollTargetItem != null && isCornerVisible(scrollTargetItem, dir) && isVisible(scrollTargetItem, clipHeight / 5)) {
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
				
				if (scrollTargetItem != null && isVisible(scrollTargetItem) && isCornerVisible(scrollTargetItem, dir)) {
					scrollTargetItem = null;
				}
			}
		} else if (game == Canvas.LEFT || game == Canvas.RIGHT) {
			if (focusedItem != null) {
				focusedItem.traverse(game, clipHeight, scroll);
			}
		}
		if (repaint) {
			queueRepaint();
		}
	}
	
	protected void pointerPressed(int x, int y) {
		if (loading) return;
		focusItem(null, 0);
		pressed = true;
		dragging = false;
		dragYHold = 0;
		pressX = pointerX = x;
		pressY = pointerY = y;
		movesIdx = 0;
		pressTime = System.currentTimeMillis();
		if (!menuFocused && y > top && y < top + clipHeight) {
			pointedItem = getItemAt(x, y);
			contentPressed = true;
		}
		queueRepaint();
	}
	
	protected void pointerDragged(int x, int y) {
		if (loading) return;
		long now = System.currentTimeMillis();
		if (!longTap && contentPressed) {
			final int dY = pointerY - y;
			if (dragging || dY > 1 || dY < -1
					|| dragYHold + dY > 2 || dragYHold + dY < -2) {
				dragging = true;
				int d = dY + dragYHold;
				if (reverse) d = -d;
				scroll += d;
				dragYHold = 0;
				if (kineticScroll * d < 0) kineticScroll = 0;
				scrollTarget = -1;
			} else {
				// hold dragged units until it reaches threshold 
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
		pointerX = x;
		pointerY = y;
		queueRepaint();
	}
	
	protected void pointerReleased(int x, int y) {
		if (loading) return;
		long now = System.currentTimeMillis();
		if (contentPressed) {
			if (!longTap) {
				if (!dragging) {
					if (now - pressTime < 300 && pointedItem != null && pointedItem.focusable) {
						focusItem(pointedItem, 0);
						pointedItem.tap(x,
								reverse ? y - (scroll - bottom - pointedItem.y + height - pointedItem.contentHeight)
										: y - pointedItem.y - top - scroll,
										false);
					}
				} else {
					int move = 0;
					long moveTime = 0;
					for (int i = 0; i < moveSamples; i++) {
						int idx = (movesIdx + moveSamples - 1 - i) % moveSamples;
						long time;
						if ((time = now - moveTimes[idx]) > 200) {
							break;
						}
						move += moves[idx];
						moveTime += time;
					}
					System.out.println("k " + move + " " + moveTime);
					if (moveTime > 0) {
						// release kinetic velocity
						float res = (130f * move) / moveTime; 
						if (Math.abs(res) > 60) {
							res = (res < 0 ? -60 : 60);
						}
						if (reverse) res = -res;
						if (kineticScroll * res < 0) kineticScroll = 0;
						kineticScroll += res;
					}
				}
			}
		} else if (menuFocused) {
			int my = height - (int)menuAnimProgress;
			if (y < my || x < 20 || x > width - 20 || menu == null) {
				closeMenu();
			} else if (!longTap && now - pressTime < 300) {
				menuAction((y - my) / (MP.medPlainFontHeight + 8));
			}
		} else if (touch && now - pressTime < 300) {
			if (y < top) {
				if (x < 40) {
					keyPressed(-7);
				} else if (x > width - 40) {
					showMenu(null, new int[] { Refresh });
				} else if (!selfChat && postId == null) {
					openProfile();
				}
			} else if (y > height - bottom) {
				// TODO
				MP.midlet.commandAction(MP.writeCmd, this);
			}
		}
		dragYHold = 0;
		pointedItem = null;
		dragging = false;
		contentPressed = false;
		pressed = false;
		longTap = false;
		pointerX = x;
		pointerY = y;
		queueRepaint();
	}
	
	private void menuAction(int i) {
		if (i < menu.length) {
			if (menuItem == null) {
				switch (menu[i]) {
				case Refresh:
					MP.midlet.commandAction(MP.latestCmd, this);
					break;
				case ChatInfo:
					openProfile();
					break;
				case SearchMessages:
					MP.midlet.commandAction(MP.searchMsgCmd, this);
					break;
				case SendSticker:
					MP.midlet.commandAction(MP.sendStickerCmd, this);
					break;
				}
			} else {
				menuItem.menuAction(menu[i]);
			}
			closeMenu();
		}
	}

	protected void sizeChanged(int width, int height) {
		queueRepaint();
	}
	
	// ui
	
	public void requestLayout(UIItem item) {
//		layoutStart = Math.min(layoutStart, idx);
		if (layoutStart != null || item == null) {
			layoutStart = firstMessage;
		} else {
			layoutStart = (UIItem) item;
		}
		queueRepaint();
	}
	
	private void layout(UIItem offsetItem, int w, int h) {
		if (count == 0 || offsetItem == null) return;
		boolean offset = false;
		if (offsetItem.prev != null) {
			offsetItem = offsetItem.prev;
			offset = true;
		}
		System.out.println("layout " + offsetItem);
		
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
		System.out.println("layout done " + y);
		
		if (prevScrollItemY != 0) {
			scroll = prevScroll - prevScrollItemY + scrollItem.y;
		}
	}
	
	void safeAdd(Thread thread, UIMessage item, boolean focus) {
		if (thread != this.thread) throw MP.cancelException;
		table.put(Integer.toString(item.id), item);
		add(item);
		if (focus) {
			nextFocusItem = item;
		}
	}
	
	void safeAddFirst(Thread thread, UIMessage item) {
		if (thread != this.thread) throw MP.cancelException;
		table.put(Integer.toString(item.id), item);
		addFirst(item);
	}
	
	void addFirst(UIItem item) {
		if (item == null) return;
		count++;
		if (firstMessage == null) {
			firstMessage = item;
		} else {
			item.next = firstMessage;
			firstMessage = (firstMessage.prev = item);
		}
		item.container = this;
		requestLayout(item);
	}
	
	void add(UIItem item) {
		if (item == null) return;
		count++;
		if (firstMessage == null || lastMessage == null) {
			firstMessage = lastMessage = item;
		} else {
			item.prev = lastMessage;
			lastMessage = (lastMessage.next = item);
		}
		item.container = this;
		requestLayout(item);
	}
	
	void remove(UIItem item) {
		// TODO
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
	
	public void setCurrentItem(UIItem item) {
		focusItem(item, 0);
		// check if item is visible on screen
		if (item.y + item.contentHeight >= scroll && item.y < scroll + clipHeight) {
			scrollTo(item);
		}
	}
	
	private void scrollTo(UIItem item) {
		scrollCurrentItem = item;
		scrollTo(item.y);
	}
	
	private void scrollTo(int y) {
		scrollTarget = y;
	}
	
	private boolean isVisible(UIItem item) {
		return item.y + item.contentHeight > scroll && item.y < scroll + clipHeight;
	}
	
	private boolean isVisible(UIItem item, int offset) {
		return item.y + item.contentHeight + offset > scroll && item.y < scroll + clipHeight - offset;
	}
	
	private boolean isCornerVisible(UIItem item, int dir) {
		if (dir == -1) {
			// isTopVisible
			return item.y >= scroll && item.y < scroll + height;
		} else {
			// isBottomVisible
			return item.y + item.contentHeight >= scroll && item.y + item.contentHeight < scroll + clipHeight;
		}
	}
	
	private UIItem getFirstFocusableItemOnScreen(UIItem offset, int dir, int offsetHeight) {
		if (offset == null) offset = firstMessage;
		UIItem res = null;
		for (offset = (dir == -1 ? offset.prev : offset.next); offset != null; offset = (dir == -1 ? offset.prev : offset.next)) {
			UIItem t = offset;
			if (t.focusable && isVisible(t, offsetHeight)) {
				res = t;
				break;
			}
		}
		return res;
	}
	
	private UIItem getItemAt(int x, int y) {
		if (reverse) {
			y = height + scroll - bottom - y;
		} else {
			y -= top - scroll;
		}
		if (y < scroll || y > clipHeight + scroll) return null;
		UIItem item = firstMessage;
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
		this.menuItem = item;
		this.menu = menu;
		menuCurrent = touch ? -1 : 0;
		menuFocused = true;
		int len = menu.length;
		for (int i = 0; i < len; i++) {
			if (menu[i] == Integer.MIN_VALUE) {
				len = i;
				break;
			}
		}
		menuCount = len;
		menuAnimTarget = (MP.medPlainFontHeight + 8) * len;
	}
	
	void closeMenu() {
		menuFocused = false;
		menuItem = null;
		menu = null;
		menuAnimTarget = 0;
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

	public boolean update() {
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
		// TODO Auto-generated method stub
		return null;
	}
	
	// interface setters

	public void setParent(MPChat parent) {
		// TODO Auto-generated method stub
		
	}

	public void setQuery(String s) {
		query = s;
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

	public void reset() {
		cancel();
		dir = 0;
		messageId = 0;
		addOffset = 0;
		offsetId = 0;
		typing = 0;
		if (table != null) table.clear();
		switched = false;
	}

	public void openMessage(String msg, int topMsg) {
		if (table != null && table.containsKey(msg)) {
			UIItem focus = (UIItem) table.get(msg);
			if (focus != null) {
				setCurrentItem(focus);
				return;
			}
		}
		reset();
		this.messageId = Integer.parseInt(msg);
		if (topMsg != -1) this.topMsgId = topMsg;
		MP.openLoad(this);
	}

	public void sent() {
		// TODO Auto-generated method stub
		
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
			if (id.charAt(0) != '-' && update.has("top_msg_id") && topMsgId != update.getInt("top_msg_id"))
				break;
			this.status = "Someone is typing..."; // TODO
			typing = System.currentTimeMillis();
			typingThread.interrupt();
			synchronized (typingLock) {
				typingLock.notify();
			}
			break;
		}
		case UPDATE_NEW_MESSAGE: {
			// check for duplicate
			if (update.getObject("message").getInt("id") == firstMsgId)
				break;
			
			addFirst(new UIMessage(update.getObject("message"), this));
			firstMsgId = update.getObject("message").getInt("id");
			break;
		}
		case UPDATE_DELETE_MESSAGES: {
			// TODO
//			JSONArray messages = update.getArray("messages");
//			int l = messages.size();
//			
//			for (int i = 0; i < l; ++i) {
//				deleteMessage(messages.getString(i));
//			}
			break;
		}
		case UPDATE_EDIT_MESSAGE: {
			// TODO
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
	
	private void setStatus(JSONObject status) {
		String s;
		if (status == null) {
			this.status = null;
			if (MP.chatStatus) {
				if (wasOnline == 1) {
					s = MP.L[Online];
				} else if (wasOnline == 2) {
					s = MP.L[Offline];
				} else if (wasOnline != 0) {
					s = MP.L[LastSeen] + MP.localizeDate(wasOnline, 4);
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
			s = MP.L[Online];
		} else if ((wasOnline = status.getInt("was_online", 0)) != 0) {
			s = MP.L[LastSeen] + MP.localizeDate(wasOnline, 4);
		} else {
			s = MP.L[Offline];
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

			setTicker(null);
			typing = 0;
		} catch (Exception e) {}
	}

}
