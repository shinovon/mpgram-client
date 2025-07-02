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

public class ChatCanvas extends Canvas implements MPChat {

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
	
	Vector/*<UIItem>*/ items = new Vector();
	int layoutStart;
	
	int width, height;
	int contentHeight;
	
	int scroll;
	int scrollTarget;
	int lastScrollDir;
	UIItem scrollCurrentItem, scrollTargetItem;
	UIItem focusedItem;
	
	public void load() {
		// TODO
		setFullScreenMode(true);
		add(new UIMessage(null));
//		add(new UIMessage(null));
//		add(new UIMessage(null));
//		add(new UIMessage(null));
//		add(new UIMessage(null));
//		add(new UIMessage(null));
//		add(new UIMessage(null));
//		add(new UIMessage(null));
//		add(new UIMessage(null));
//		add(new UIMessage(null));
//		add(new UIMessage(null));
	}
	
	void closed(boolean destroy) {
		if (destroy) cancel();
	}
	
	void cancel() {
		
	}
	
	// Canvas

	protected void paint(Graphics g) {
		int w = getWidth(), h = getHeight();
		int contentHeight = this.contentHeight;
//		boolean reverse = MP.reverseChat;
		if (width != w) {
			layoutStart = 0;
		}
		width = w; height = h;
		
		if (layoutStart != Integer.MAX_VALUE) {
			int idx = layoutStart;
			layoutStart = Integer.MAX_VALUE;
			layout(idx, w, h, false);
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
			scroll = scrollTarget;
			scrollTarget = -1;
		}

		int scroll = this.scroll;
		if (scroll < 0) this.scroll = scroll = 0;
		else if (contentHeight <= h) this.scroll = scroll = 0;
		else if (scroll > contentHeight - h) this.scroll = scroll = contentHeight - h;
		
		g.setColor(0x0E1621);
		g.fillRect(0, 0, w, h);
		g.setColor(0);
		
		int l = items.size();
//		if (reverse) {
//			
//		} else { 
			int y = -scroll;
			for (int i = 0; i < l; ++i) {
				UIItem item = (UIItem) items.elementAt(i);
//				if (y < -item.contentHeight) continue;
				item.paint(g, 0, y, w);
				if ((y += item.contentHeight) > h) break;
			}
//		}
	}

	protected void keyPressed(int key) {
		key(key, false);
	}
	
	protected void keyRepeated(int key) {
		key(key, true);
	}
	
	protected void keyReleased(int key) {
	}
	
	private void key(int key, boolean repeat) {
		int game = getGameAction(key);
		boolean repaint = false;
		if (key == -7 || key == -22 || key == 22) {
			// back
			MP.midlet.commandAction(MP.backCmd, this);
		} else if (key == -6 || key == -21 || key == 21) {
			// menu
			
		} else if (key == -5 || game == Canvas.FIRE) {
			// action
			if (focusedItem != null) {
				focusedItem.action();
			}
		} else if (game == Canvas.DOWN || game == Canvas.UP) {
			scroll: {
				int dir = game == Canvas.DOWN ? 1 : -1;
				if (scrollTargetItem == null && scrollCurrentItem == null) {
					// get first visible item
					int l = items.size();
					UIItem item = null;
					for (int i = 0; i < l; ++i) {
						UIItem t = (UIItem) items.elementAt(i);
						if (t.y + t.contentHeight >= scroll) {
							item = t;
							break;
						}
					}
					scrollTargetItem = item;
				}
				if (focusedItem != null) {
					int t = focusedItem.traverse(game, height, scroll);
					if (t != 0) {
						repaint = true;
						if (t != Integer.MAX_VALUE) {
							scrollTarget = t;
						}
						break scroll;
					}
				}
				final int scrollAmount = height / 6;
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
				repaint = true;
				if (game == Canvas.DOWN) {
					if (scrollTargetItem != null && isVisible(scrollTargetItem)) {
						focusItem(scrollTargetItem);
						scrollCurrentItem = scrollTargetItem;
						if (isEndVisible(scrollTargetItem)) {
							scrollTargetItem = null;
							break scroll;
						}
					}
					scroll = Math.min(scroll + scrollAmount, contentHeight - height);
					
					if (scrollTargetItem != null && isVisible(scrollTargetItem) && isEndVisible(scrollTargetItem)) {
						scrollTargetItem = null;
					}
				} else {
					if (scrollTargetItem != null && isVisible(scrollTargetItem)) {
						focusItem(scrollTargetItem);
						scrollCurrentItem = scrollTargetItem;
						if (isTopVisible(scrollTargetItem)) {
							scrollTargetItem = null;
							break scroll;
						}
					}
					scroll = Math.max(scroll - scrollAmount, 0);
					
					if (scrollTargetItem != null && isVisible(scrollTargetItem) && isTopVisible(scrollTargetItem)) {
						scrollTargetItem = null;
					}
				}
			}
		} else if (game == Canvas.LEFT || game == Canvas.RIGHT) {
			if (focusedItem != null) {
				focusedItem.traverse(game, width, scroll);
			}
		}
		if (repaint) {
			queueRepaint();
		}
	}
	
	protected void pointerPressed(int x, int y) {
		
	}
	
	protected void pointerDragged(int x, int y) {
		
	}
	
	protected void pointerReleased(int x, int y) {
		
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
		idx = Math.min(l, idx);
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
			scroll = prevScroll - prevScrollItemY - scrollItem.y;
		}
	}
	
	void add(UIItem item) {
		if (item == null) return;
		items.addElement(item);
		item.container = this;
		requestLayout(items.size() - 1);
	}
	
	public void queueRepaint() {
		repaint();
	}
	
	private boolean focusItem(UIItem item) {
		if (focusedItem == item) return true;
		if (focusedItem != null) {
			focusedItem.lostFocus();
		}
		scrollCurrentItem = item;
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
		if (item.y + item.contentHeight >= scroll && item.y < scroll + height) {
			scrollTo(item);
		}
	}
	
	private void scrollTo(UIItem item) {
		scrollCurrentItem = item;
		scrollTarget = item.y;
	}
	
	private boolean isVisible(UIItem item) {
		return item.y + item.contentHeight >= scroll && item.y < scroll + height;
	}
	
	private boolean isTopVisible(UIItem item) {
		return item.y >= scroll && item.y < scroll + height;
	}
	
	private boolean isEndVisible(UIItem item) {
		return item.y + item.contentHeight >= scroll && item.y + item.contentHeight < scroll + height;
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
		// TODO Auto-generated method stub
		return false;
	}

	public boolean endReached() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean forum() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean switched() {
		// TODO Auto-generated method stub
		return false;
	}

	public int topMsgId() {
		// TODO Auto-generated method stub
		return 0;
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
		// TODO Auto-generated method stub
		
	}

	public void setBotAnswer(JSONObject j) {
		// TODO Auto-generated method stub
		
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
