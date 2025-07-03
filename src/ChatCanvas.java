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

import java.util.Vector;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Ticker;

public class ChatCanvas extends Canvas implements MPChat, LangConstants {

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
	
	// discussion
	String postPeer, postId;
	ChatTopicsList topicsList;
	JSONArray topics;
	
	Vector/*<UIItem>*/ items = new Vector();
	int layoutStart;
	
	int width, height;
	int clipHeight, top, bottom;
	int contentHeight;
	
	int scroll;
	int scrollTarget;
	int lastScrollDir;
	UIItem scrollCurrentItem, scrollTargetItem;
	UIItem focusedItem;
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
	
	boolean fieldFocused;
	int fieldAnimTarget = -1;
	float fieldAnimProgress;
	int fieldHeight = 40;
	
	boolean menuFocused;
	int menuAnimTarget = -1;
	float menuAnimProgress;
	int menuHeight = 40;
	
	ChatCanvas() {
		setFullScreenMode(true);
	}
	
	public void load() {
		Thread thread = this.thread = Thread.currentThread();
		
		// remove all
		items.removeAllElements();
		scrollCurrentItem = scrollTargetItem = focusedItem = null;
		
		try {
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
						canPin = true;
						if (MP.chatStatus && info.getObject("User").has("status")) {
	//						setStatus(info.getObject("User").getObject("status"));
						}
					}
					JSONObject full;
					if (messageId == -1 && (full = info.getObject("full")).has("read_inbox_max_id")) {
						messageId = 0;
						int maxId = full.getInt("read_inbox_max_id");
						if (maxId != 0 && full.getInt("unread_count", 0) > limit) {
							offsetId = messageId = maxId;
							addOffset = -limit;
							dir = 1;
						}
					}
				}
				infoLoaded = true;
			}

			
			boolean selfChat = MP.selfId.equals(id);
			boolean reverse = MP.reverseChat && mediaFilter == null;
			
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
			
			for (int i = 0; i < l; i++) {
				JSONObject message = messages.getObject(i);
				if (message.has("action")) {
					add(new UIChatAction(message));
				} else {
					add(new UIMessage(message, this));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		for (int i = 0; i < 30; i++)
			add(new UIMessage(null, this));
	}
	
	void closed(boolean destroy) {
		if (destroy) cancel();
	}
	
	void cancel() {
		
	}
	
	// Canvas

	protected void paint(Graphics g) {
		int w = getWidth(), h = getHeight();
		g.setClip(0, 0, w, h);
		int contentHeight = this.contentHeight;
		
		long now = System.currentTimeMillis();
		long deltaTime = now - lastPaintTime;
		if (deltaTime > 500) {
			deltaTime = 1;
		}
		
//		boolean reverse = MP.reverseChat;
		if (width != w) {
			layoutStart = 0;
		}
		width = w; height = h;
		
		boolean animate = false;

		if (fieldAnimTarget != -1) {
			if (bottom == fieldAnimTarget || Math.abs(fieldAnimTarget - fieldAnimProgress) < 1) {
				fieldAnimProgress = bottom = fieldAnimTarget;
				fieldAnimTarget = -1;
			} else {
				bottom = (int) (fieldAnimProgress = MP.lerp(fieldAnimProgress, fieldAnimTarget, 4, 20));
				animate = true;
			}
		}
		if (menuAnimTarget != -1) {
			if (Math.abs(menuAnimTarget - menuAnimProgress) < 1) {
				menuAnimProgress = menuAnimTarget;
				menuAnimTarget = -1;
			} else {
				menuAnimProgress = MP.lerp(menuAnimProgress, menuAnimTarget, 4, 20);
				animate = true;
			}
		}
		
		int clipHeight = this.clipHeight = h - top - bottom;
		
		if (layoutStart != Integer.MAX_VALUE) {
			int idx = layoutStart;
			layoutStart = Integer.MAX_VALUE;
			layout(idx, w, clipHeight, false);
			contentHeight = this.contentHeight;
		}
		
		if (focusedItem == null && scrollCurrentItem == null && scrollTargetItem == null) {
//			if (reverse) {
//			} else
			focusItem(getFirstFocusableItemOnScreen(-1, 1));
		} else if (focusedItem != null && scrollCurrentItem == null && !isVisible(focusedItem)) {
			scrollTo(focusedItem);
		}
		
		if (scrollTarget != -1) {
			if (contentHeight <= clipHeight) {
				scroll = 0;
				scrollTarget = -1;
			} else {
				if (Math.abs(scroll - scrollTarget) < 1) {
					scroll = scrollTarget;
					scrollTarget = -1;
				} else {
					if (Math.abs(scrollTarget - scroll) < 5) {
						if (scrollTarget - scroll < 0) {
							-- scroll;
						} else {
							++ scroll;
						}
					} else {
						scroll = (int) MP.lerp(scroll, scrollTarget, 4, 20);
					}
					animate = true;
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
		
		if (kineticScroll != 0) {
			if ((kineticScroll > -1 && kineticScroll < 1)
					|| contentHeight <= clipHeight
					|| scroll <= 0
					|| scroll >= contentHeight - clipHeight) {
				kineticScroll = 0;
			} else {
				scroll += (int) kineticScroll;
				float mul = pressed ? 0.5f : 0.96f;
				kineticScroll *= mul;
				float f;
				if ((f = deltaTime > 33 ? deltaTime / 33f : 1f) >= 2) {
					int j = (int) f - 1;
					for (int i = 0; i < j; i++) {
						scroll += (int) kineticScroll;
						kineticScroll *= mul;
					}
				}
				animate = true;
			}
		}

		int scroll = this.scroll;
		if (scroll < 0) this.scroll = scroll = 0;
		else if (contentHeight <= clipHeight) this.scroll = scroll = 0;
		else if (scroll > contentHeight - clipHeight) this.scroll = scroll = contentHeight - clipHeight;
		
		g.setColor(0x0E1621);
		g.fillRect(0, 0, w, h);
		g.setColor(0);
		
		int l = items.size();
//		if (reverse) {
//			
//		} else {
		g.setClip(0, top, w, clipHeight);
		int y = -scroll + top;
		for (int i = 0; i < l; ++i) {
			UIItem item = (UIItem) items.elementAt(i);
			int ih = item.contentHeight;
			if (y < -ih) {
				y += ih;
				continue;
			}
			item.paint(g, 0, y, w);
			if (scrollTargetItem == item) {
				g.setColor(0xFF00FF);
				g.drawRect(0, y, 100, ih);
			}
			if (scrollCurrentItem == item) {
				g.setColor(0x00FFFF);
				g.drawRect(20, y, 100, ih);
			}
			if ((y += ih) > h) break;
		}
//		}
		
		g.setClip(0, 0, w, h);
		
		if (menuAnimProgress != 0) {
			g.setColor(-1);
			g.fillRect(20, h - (int)menuAnimProgress, w - 40, (int)menuAnimProgress);
		}
			
		if (pressed && !dragging && !longTap
				&& pointedItem != null && pointedItem.focusable) {
			animate = true;
			if (now - pressTime > 200) {
				g.setColor(-1);
				int size = Math.min(360, (int) (now - pressTime - 200) / 2);
				g.fillArc(pointerX - 16, pointerY - 16, 32, 32, 90, size);
				if (size >= 360) {
					// handle long tap
					longTap = true;
					focusItem(pointedItem);
					pointedItem.longTap(pointerX - scroll - top, pointerY);
				}
			}
		}
		
		g.setColor(-1);
		g.setFont(MP.smallPlainFont);
		g.drawString("w" + width + " h" + height + " c" + this.contentHeight + " l" + this.clipHeight + " s" + scroll + " t" + top, 20, 20, 0);

		// limit fps
		if (deltaTime < 32) {
			try {
				Thread.sleep(33 - deltaTime);
			} catch (Exception ignored) {}
		}
		if (animate) {
			repaint();
		}
		lastPaintTime = now;
	}

	protected void keyPressed(int key) {
		key(key, false);
	}
	
	protected void keyRepeated(int key) {
		key(key, true);
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
			// back
			if (menuFocused) {
				menuFocused = false;
				menuAnimTarget = 0;
			} else if (fieldFocused) {
//				fieldFocused = false;
//				fieldAnimTarget = 0;
				MP.midlet.commandAction(MP.backCmd, this);
				return;
			} else {
				fieldFocused = true;
				fieldAnimTarget = 40;
			}
			queueRepaint();
		} else if (key == -6) {
			// menu
			if (menuFocused) {
				menuFocused = false;
				menuAnimTarget = 0;
			} else if (fieldFocused) {
				fieldFocused = false;
				fieldAnimTarget = 0;
			} else {
				menuFocused = true;
				menuAnimTarget = 300;
			}
			queueRepaint();
		} else if (menuFocused) {
			
		} else if (fieldFocused) {
			
		} else if (key == -5 || game == Canvas.FIRE) {
			// action
			if (focusedItem != null) {
				focusedItem.action();
			}
		} else if (key == Canvas.KEY_NUM2) {
			focusedItem = scrollCurrentItem = scrollTargetItem = null;
			scrollTo(scroll - clipHeight);
			repaint = true;
		} else if (key == Canvas.KEY_NUM8) {
			focusedItem = scrollCurrentItem = scrollTargetItem = null;
			scrollTo(scroll + clipHeight);
			repaint = true;
		} else if (key >= Canvas.KEY_NUM0 && key <= Canvas.KEY_NUM9) {
			// ignore
		} else if (game == Canvas.DOWN || game == Canvas.UP) {
			scroll: {
				int dir = game == Canvas.DOWN ? 1 : -1;
				final int scrollAmount = clipHeight / 4;
				if (scrollTargetItem == null && scrollCurrentItem == null) {
					scrollTargetItem = getFirstFocusableItemOnScreen(-1, 1);
				}
				if (focusedItem != null) {
					int t = focusedItem.traverse(game, clipHeight, scroll);
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
					scrollTargetItem = getFirstFocusableItemOnScreen(items.indexOf(scrollCurrentItem), dir);
				}
				UIItem item = scrollTargetItem;
				if (item != null && isVisible(item)) {
					focusItem(item);
				}
				if (game == Canvas.DOWN) {
					if (scrollTargetItem != null && isVisible(scrollTargetItem, clipHeight / 5)) {
						repaint = true;
						focusItem(scrollTargetItem);
						scrollCurrentItem = scrollTargetItem;
						if (isEndVisible(scrollTargetItem)) {
							scrollTargetItem = getFirstFocusableItemOnScreen(items.indexOf(scrollCurrentItem), dir);
							if (scrollTargetItem != null && isEndVisible(scrollTargetItem)) {
								break scroll;
							}
						}
					}
					repaint = true;
					scrollTo(Math.min(scroll + scrollAmount, contentHeight - clipHeight));
					
					if (scrollTargetItem != null && isVisible(scrollTargetItem) && isEndVisible(scrollTargetItem)) {
						scrollTargetItem = null;
					}
				} else {
					if (scrollTargetItem != null && isVisible(scrollTargetItem, clipHeight / 5)) {
						repaint = true;
						focusItem(scrollTargetItem);
						scrollCurrentItem = scrollTargetItem;
						if (isTopVisible(scrollTargetItem)) {
							scrollTargetItem = getFirstFocusableItemOnScreen(items.indexOf(scrollCurrentItem), dir);
							if (scrollTargetItem != null && isTopVisible(scrollTargetItem)) {
								break scroll;
							}
						}
					}
					repaint = true;
					scrollTo(Math.max(scroll - scrollAmount, 0));
					
					if (scrollTargetItem != null && isVisible(scrollTargetItem) && isTopVisible(scrollTargetItem)) {
						scrollTargetItem = null;
					}
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
		pressed = true;
		dragging = false;
		dragYHold = 0;
		pressX = pointerX = x;
		pressY = pointerY = y;
		movesIdx = 0;
		pressTime = System.currentTimeMillis();
		if (y > top && y < top + clipHeight) {
			pointedItem = getItemAt(x, y);
			contentPressed = true;
		}
		queueRepaint();
	}
	
	protected void pointerDragged(int x, int y) {
		if (!longTap && contentPressed) {
			final int dY = pointerY - y;
			if (dragging || dY > 1 || dY < -1
					|| dragYHold + dY > 2 || dragYHold + dY < -2) {
				dragging = true;
				int d = dY + dragYHold;
				scroll += d;
				dragYHold = 0;
				if (kineticScroll * d < 0) kineticScroll = 0;
			} else {
				dragYHold += dY;
			}
			
			moves[movesIdx] = dY;
			moveTimes[movesIdx] = System.currentTimeMillis();
			movesIdx = (movesIdx + 1) % moveSamples;
		}
		pointerX = x;
		pointerY = y;
		queueRepaint();
	}
	
	protected void pointerReleased(int x, int y) {
		long now = System.currentTimeMillis();
		if (!longTap && contentPressed) {
			if (!dragging) {
				// TODO tap handling
				if (pointedItem != null && pointedItem.focusable) {
					focusItem(pointedItem);
					pointedItem.tap(x, y - pointedItem.y - top - scroll);
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
					float res = (150f * move) / moveTime; 
					if (kineticScroll * res < 0) kineticScroll = 0;
					kineticScroll += res;
				}
				dragging = false;
			}
		}
		dragYHold = 0;
		pointedItem = null;
		contentPressed = false;
		pressed = false;
		longTap = false;
		pointerX = x;
		pointerY = y;
		queueRepaint();
	}
	
	// ui
	
	public void requestLayout(UIItem item) {
		requestLayout(items.indexOf(item));
	}

	public void requestLayout(int idx) {
		if (idx == -1) return;
		layoutStart = Math.min(layoutStart, idx);
		queueRepaint();
	}
	
	private void layout(int idx, int w, int h, boolean reverse) {
		int l = items.size();
		if (l == 0 || idx == -1) return;
		idx = Math.min(l - 1, idx);
		if (idx < 0) idx = 0;
		else if (idx > 0) idx--;
		System.out.println("layout " + idx);
		
		int prevScroll = scroll;
		int prevScrollItemY = 0;
		UIItem scrollItem = scrollCurrentItem;
		if (scrollItem != null) {
			prevScrollItemY = scrollItem.y;
		}
		
		int y = 0;
		for (int i = idx; i < l; ++i) {
			UIItem item = (UIItem) items.elementAt(i);
			if (idx != 0 && i == idx) {
				y = item.y;
			} else {
				item.y = y;
			}
			y += item.layout(w);
		}
		
		contentHeight = y;
		System.out.println("layout done " + y);
		
		if (prevScrollItemY != 0) {
			scroll = prevScroll - prevScrollItemY + scrollItem.y;
		}
	}
	
	void add(UIItem item) {
		if (item == null) return;
		items.addElement(item);
		item.container = this;
		requestLayout(items.size() - 1);
	}
	
	public void queueRepaint() {
		if (isShown()) repaint();
	}
	
	private boolean focusItem(UIItem item) {
		if (focusedItem == item) return true;
		if (focusedItem != null) {
			focusedItem.lostFocus();
		}
		scrollCurrentItem = item;
		if (scrollTargetItem != item) scrollTargetItem = null;
		focusedItem = item;
		if (item != null) {
			if (!focusedItem.grabFocus()) {
				focusedItem = null;
				return false;
			}
			return true;
		}
		return false;
	}
	
	public void setCurrentItem(UIItem item) {
		focusItem(item);
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
	
	private boolean isTopVisible(UIItem item) {
		return item.y >= scroll && item.y < scroll + height;
	}
	
	private boolean isEndVisible(UIItem item) {
		return item.y + item.contentHeight >= scroll && item.y + item.contentHeight < scroll + clipHeight;
	}
	
	private UIItem getFirstFocusableItemOnScreen(int offset, int dir) {
		UIItem item = null;
		int l = items.size();
		for (offset += dir; offset >= 0 && offset < l; offset += dir) {
			UIItem t = (UIItem) items.elementAt(offset);
			if (t.focusable && isVisible(t)) {
				item = t;
				break;
			}
		}
		return item;
	}
	
	private UIItem getItemAt(int x, int y) {
		y -= top - scroll;
		if (y < 0) return null;
		int l = items.size();
		for (int i = 0; i < l; ++i) {
			UIItem item = (UIItem) items.elementAt(i);
			if (y >= item.y && y < item.y + item.contentHeight) {
				return item;
			}
		}
		return null;
	}
	
	// interface getters

	public String id() {
		return id;
	}

	public String postId() {
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub
		return false;
	}

	public int topMsgId() {
		return topMsgId;
	}

	public int firstMsgId() {
		// TODO Auto-generated method stub
		return 0;
	}

	public JSONArray topics() {
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub
		
	}

	public void setUpdate(boolean b) {
		this.update = b;
	}

	public void setBotAnswer(JSONObject j) {
		// TODO Auto-generated method stub
		
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
		
	}

	public void openMessage(String msg, int topMsg) {
		// TODO Auto-generated method stub
		
	}

	public void sent() {
		// TODO Auto-generated method stub
		
	}

	public void handleUpdate(int type, JSONObject update) {
		// TODO Auto-generated method stub
		
	}

	public void handleBotAnswer(JSONObject j) {
		// TODO Auto-generated method stub
		
	}

	public void paginate(int dir) {
		// TODO Auto-generated method stub
		
	}

	public void openTopic(int topMsgId, boolean canWrite, String title) {
		// TODO Auto-generated method stub
		
	}

}
