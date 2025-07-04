import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;

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
public class UIMessage extends UIItem implements LangConstants {
	
	private static final int MAX_WIDTH = 440;
	private static final int MARGIN_HEIGHT = 1;
	private static final int MARGIN_WIDTH = 2;
	private static final int MARGIN_SIDE = 10;
	private static final int PADDING_WIDTH = 4;
	private static final int PADDING_HEIGHT = 4;
	private static final int TIME_PADDING_WIDTH = 2;
	private static final int TIME_PADDING_HEIGHT = 1;
	private static final int DATE_PADDING_HEIGHT = 2;
	
	private static final int FOCUS_SENDER = 0;
	private static final int FOCUS_FORWARD = 1;
	private static final int FOCUS_REPLY = 2;
	private static final int FOCUS_PHOTO = 3;
	private static final int FOCUS_TEXT = 4;
	private static final int FOCUS_MEDIA = 5;
	private static final int FOCUS_BUTTONS = 6;
	private static final int FOCUS_COMMENT = 7;
	
	UILabel text;
	
	UIItem focusChild;
	int subFocusCurrent = -1;
	int[] subFocus;
	int subFocusLength;
	
	static int c;
	
	int id;
	boolean out;
	boolean edited;
	String name;
	String fromId;
	boolean action;
	long date;
	String commentPeer;
	int commentRead;
	
	boolean fwd, reply, media;
	String replyName, replyText;
	String commentsText;
	int mediaRows;
	String mediaTitle, mediaSubtitle;
	
	String time, nameRender, dateRender, replyNameRender, replyTextRender;
	int timeWidth, dateWidth, senderWidth;
	boolean showDate, hideName, timeBreak;
	
	UIMessage(JSONObject message, ChatCanvas chat) {
		focusable = true;
		subFocus = new int[8];
		date = message.getLong("date");
		id = message.getInt("id");
		fromId = message.has("from_id") ? message.getString("from_id") : chat.id;
		out = (message.getBoolean("out", false) && !chat.broadcast) || chat.selfChat;
		hideName = chat.selfChat || chat.user || out;
		name = out && !chat.broadcast ? MP.L[You] : MP.getName(fromId, true);
		dateRender = MP.localizeDate(date, 0);
		dateWidth = MP.smallBoldFont.stringWidth(dateRender);
		
		if ((action = message.has("act"))) {
			JSONObject act = message.getObject("act");
			String type = act.getString("_");
			String user = act.getString("user", null);
			
			UILabel label = new UILabel();
			label.color = -1;
			label.linkColor = -1;
			label.bgColor = 0x1E2C3A;
			label.background = true;
			label.center = true;
			
			String t = null;
			l: {
				if ("ChatCreate".equals(type)) {
					t = MP.L[GroupCreated_Action];
				} else if ("ChannelCreate".equals(type)) {
					t = MP.L[ChannelCreated_Action];
				} else if ("ChatEditPhoto".equals(type)) {
					t = MP.L[PhotoUpdated_Action];
				} else if ("HistoryClear".equals(type)) {
					t = MP.L[ChatHistoryCleared_Action];
				} else if ("ChatEditTitle".equals(type)) {
					t = MP.L[NameChanged_Action].concat(act.getString("t", ""));
				} else {
					label.appendWord(name, MP.medPlainFont, "t.me/".concat(fromId));
					if ("ChatAddUser".equals(type) || "ChatDeleteUser".equals(type)) {
						if (fromId.equals(user)) {
							t = MP.L["ChatAddUser".equals(type) ? Joined_Action : Left_Action];
						} else {
							label.appendWord(MP.L["ChatAddUser".equals(type) ? Added_Action : Removed_Action], MP.medPlainFont, null);
							label.appendWord(" ", MP.medPlainFont, null);
							label.appendWord(MP.getName(user, false), MP.medPlainFont, "t.me/".concat(user));
							break l;
						}
					} else {
						label.appendWord(" ", MP.medPlainFont, null);
						break l;
					}
				}
				
				if (t != null) {
					label.appendWord(t, MP.medPlainFont, null);
				}
			}
			this.text = label;
			
			return;
		}
		
		int order = 0;
		
		if (!out) {
			subFocus[order++] = FOCUS_SENDER;
		}
		
		// forwarded from... label
		if (message.has("fwd")) {
			// TODO
			fwd = true;
			JSONObject fwd = message.getObject("fwd");
			subFocus[order++] = FOCUS_FORWARD;
		}
		
		// reply
		if (message.has("reply")) {
			JSONObject reply = message.getObject("reply");
			int topMsgId = chat.topMsgId;
			if (topMsgId == 0 || reply.getInt("id") != topMsgId) {
				if (reply.has("msg")) {
					this.reply = true;
					JSONObject replyMsg = reply.getObject("msg");
					JSONObject replyFwd;
					String t = null;
					if ((t = MP.getName(replyMsg.getString("from_id", null), true, true)) == null
							&& replyMsg.has("fwd") && (replyFwd = replyMsg.getObject("fwd")).getBoolean("s", false)) {
						if ((t = replyFwd.getString("from_name", null)) == null) {
							t = MP.getName(replyFwd.getString("from_id", null), true);
						}
					}
					replyName = t;
					if (reply.has("quote")) {
						replyText = reply.getString("quote");
					} else if ((t = replyMsg.getString("text", null)) != null && t.length() != 0) {
						replyText = t;
					} else if (replyMsg.has("media")) {
						replyText = MP.L[Media];
					}
					subFocus[order++] = FOCUS_REPLY;
				}
			}
		}
		
		// photo
		boolean photo = false;
		JSONObject media = message.getObject("media", null);
		if (MP.showMedia
				&& message.has("media") && !message.isNull("media")
				&& media.getString("type").equals("photo")) {
			photo = true;
			
		}
		
		// text
		String text = message.getString("text", null);
		if (text != null && text.length() != 0) {
			UILabel label;
			if (MP.parseRichtext && message.has("entities")) {
				label = new UILabel(text, message.getArray("entities"));
			} else {
				label = new UILabel(text, MP.smallPlainFont, null);
			}
			label.color = -1;
			label.linkColor = 0x71BAFA;
			if (label.focusable) subFocus[order++] = FOCUS_TEXT;
			this.text = label;
		}
		
		// media
		if (message.has("media") && !photo) {
			if (!MP.showMedia || message.isNull("media")) {
				
			} else {
				this.media = true;
				subFocus[order++] = FOCUS_MEDIA;
			}
		}
		
		// buttons TODO
//		if (message.has("markup")) {
//			subFocus[order++] = FOCUS_BUTTONS;
//		}
		
		if (message.has("comments")) {
			JSONObject comments = message.getObject("comments");
			commentsText = MP.localizePlural(comments.getInt("count"), _comment);
			commentPeer = comments.getString("peer");
			commentRead = comments.getInt("read", 0);
			subFocus[order++] = FOCUS_COMMENT;
		}
		
		StringBuffer sb = new StringBuffer();
		
		sb.setLength(0);
		time = MP.appendTime(sb, date).toString();
		timeWidth = MP.smallPlainFont.stringWidth(time);
		
		subFocusLength = order;
	}
	
	void paint(Graphics g, int x, int y, int w) {
		int h = contentHeight;
		int cw = Math.min(MAX_WIDTH, w);
		// date
		if (showDate) {
			if (((ChatCanvas) container).reverse) {
				y += MARGIN_HEIGHT;
				g.setColor(0x1E2C3A);
				g.setFont(MP.smallBoldFont);
				g.fillRect(x - PADDING_WIDTH + (w - dateWidth - x) >> 1, y, dateWidth + PADDING_WIDTH * 2, MP.smallBoldFontHeight + DATE_PADDING_HEIGHT * 2);
				g.setColor(-1);
				g.drawString(dateRender, x + (w - dateWidth - x) >> 1, y += DATE_PADDING_HEIGHT, 0);
				y += MARGIN_HEIGHT + DATE_PADDING_HEIGHT + MP.smallBoldFontHeight;
			}
			h -= MARGIN_HEIGHT * 2 + DATE_PADDING_HEIGHT * 2 + MP.medPlainFontHeight;
		}
		if (action) {
			if (text != null) {
				y += MARGIN_HEIGHT;
				g.setColor(0x1E2C3A);
				g.fillRect(x + (w - text.contentWidth - PADDING_WIDTH) >> 1, y, text.contentWidth + PADDING_WIDTH, text.contentHeight + PADDING_HEIGHT * 2);
				text.paint(g, x, y + PADDING_HEIGHT, w);
			}
			return;
		}
		if (out && w < 900) {
			x += w - cw;
		}
		g.setColor(out ? 0x2B5278 : 0x182533); // TODO message bg color
		g.fillRect(x += MARGIN_WIDTH + (out && w < 900 ? MARGIN_SIDE : 0), y += MARGIN_HEIGHT,
				cw -= MARGIN_WIDTH * 2 + MARGIN_SIDE, h -= (MARGIN_HEIGHT * 2));
		if (focus && focusChild == null && subFocusCurrent == -1) {
			g.setColor(-1);
			g.drawRect(x, y, cw - 1, h - 1);
		}
		int rw = cw;
		cw -= PADDING_WIDTH * 2;
		int rx = x;
		x += PADDING_WIDTH;
		y += PADDING_HEIGHT;
//		cw -= PADDING;
		int ty = y;
		
		// name
		if (!hideName) {
			g.setColor(0x71BAFA); // TODO message author color
			g.setFont(MP.smallBoldFont);
			if (nameRender != null) g.drawString(nameRender, x, y, 0);
			if (focus && subFocusCurrent != -1 && subFocus[subFocusCurrent] == FOCUS_SENDER) {
				g.setColor(0xababab);
				g.drawRect(x, y, senderWidth, MP.smallBoldFontHeight);
			}
			y += MP.smallBoldFontHeight;
		}
		
		// forward
		
		// reply
		if (reply) {
			y += 2;
			g.setColor(0x6AB3F3);
			int rh = MP.smallPlainFontHeight;
			if (replyName != null) {
				rh += MP.smallPlainFontHeight;
			}
			g.fillRect(x, y, 2, rh);
			if (replyTextRender != null) {
				g.setColor(0x71BAFA);
				g.setFont(MP.smallBoldFont);
				g.drawString(replyNameRender, x + 4, y, 0);
				y += MP.smallPlainFontHeight;
			}
			g.setColor(-1);
			g.setFont(MP.smallPlainFont);
			g.drawString(replyTextRender, x + 4, y, 0);
			y += MP.smallPlainFontHeight;
			y += 2;
		}
		
		// photos

		// text
		if (text != null) {
			UILabel text = this.text;
			text.paint(g, x, y, cw);
			y += text.contentHeight;
		}
		
		// media
		if (media) {
			g.setColor(0x6AB3F3);
			int rh = MP.smallPlainFontHeight * mediaRows;
			g.fillRect(x, y, 2, rh);
		}
		
		// buttons
		
		// time
		if (timeBreak) y += MP.smallPlainFontHeight;
		g.setColor(out ? 0x7DA8D3 : 0x6D7F8F);
		g.setFont(MP.smallPlainFont);
		g.drawString(time, rx + rw - TIME_PADDING_WIDTH, y + PADDING_HEIGHT - TIME_PADDING_HEIGHT, Graphics.BOTTOM | Graphics.RIGHT);
		if (edited) {
			g.drawString(MP.L[Edited], rx + rw - timeWidth - MP.smallPlainFontSpaceWidth - TIME_PADDING_WIDTH, y + PADDING_HEIGHT - TIME_PADDING_HEIGHT, Graphics.BOTTOM | Graphics.RIGHT);
		}
		
		y += PADDING_HEIGHT;
		
		if (commentsText != null) {
			y += PADDING_HEIGHT;
			g.setColor(0x31404E);
			g.drawLine(rx, y, rx + cw, y++);
			
			g.setFont(MP.smallBoldFont);
			g.setColor(0x71BAFA);
			g.drawString(commentsText, x, y, 0);
			y += PADDING_HEIGHT + MP.smallBoldFontHeight;
		}
		
		y += MARGIN_HEIGHT;
		
		// date not reversed
		if (showDate && !((ChatCanvas) container).reverse) {
			y += MARGIN_HEIGHT;
			g.setColor(0x1E2C3A);
			g.setFont(MP.smallBoldFont);
			g.fillRect(x - PADDING_WIDTH + (w - dateWidth - x) >> 1, y, dateWidth + PADDING_WIDTH * 2, MP.smallBoldFontHeight + DATE_PADDING_HEIGHT * 2);
			g.setColor(-1);
			g.drawString(dateRender, x + (w - dateWidth - x) >> 1, y += DATE_PADDING_HEIGHT, 0);
			y += MARGIN_HEIGHT + DATE_PADDING_HEIGHT + MP.smallBoldFontHeight;
		}
	}
	
	public int layout(int width) {
		if (!layoutRequest && layoutWidth == width) {
			return contentHeight;
		}
		layoutWidth = width;
		int h = MARGIN_HEIGHT * 2 + PADDING_HEIGHT * 2;
		if (container instanceof ChatCanvas) {
			int idx = ((ChatCanvas) container).items.indexOf(this);
			int l = ((ChatCanvas) container).items.size();
			
			date: {
				if (idx == l - 1) {
				} else if (idx < l - 1) {
					UIMessage next = (UIMessage) ((ChatCanvas) container).items.elementAt(idx + 1);
					if (dateRender.equals(next.dateRender)) {
						showDate = false;
						break date;
					}
				} else {
					showDate = false;
					break date;
				}
				showDate = true;
				h += MARGIN_HEIGHT * 2 + DATE_PADDING_HEIGHT * 2 + MP.smallBoldFontHeight;
			}
		}
		if (action) {
//			h += MP.medPlainFontHeight;
			if (text != null) {
				text.y = h - MARGIN_HEIGHT - PADDING_HEIGHT;
				h += text.layout(width);
			}
			return contentHeight = h;
		}
		int cw = Math.min(MAX_WIDTH, width) - PADDING_WIDTH * 2 - MARGIN_WIDTH * 2 - MARGIN_SIDE;
		// sender
		if (!hideName) {
			h += MP.smallBoldFontHeight;
			nameRender = UILabel.ellipsis(name, MP.smallBoldFont,
					cw - timeWidth - PADDING_WIDTH * 2 - (edited ? (MP.smallPlainFont.stringWidth(MP.L[Edited]) + 2) : 0));
			senderWidth = MP.smallBoldFont.stringWidth(nameRender);
		}
		
		// reply
		if (reply) {
			if (replyName != null) {
				h += MP.smallPlainFontHeight;
				replyNameRender = UILabel.ellipsis(replyName, MP.smallBoldFont, cw - 8);
			}
			replyTextRender = UILabel.ellipsis(replyText, MP.smallPlainFont, cw - 8);
			h += 4 + MP.smallPlainFontHeight;
		}
		
		int timeWidth = this.timeWidth;
		boolean timeBreak = text == null;
		if (edited) {
			timeWidth += MP.smallPlainFontSpaceWidth + MP.smallPlainFont.stringWidth(MP.L[Edited]);
		}
		
		// text
		if (text != null) {
			text.y = h - MARGIN_HEIGHT - PADDING_HEIGHT;
			h += text.layout(cw);
			int l = text.render.size(); 
			if (l != 0) {
				int[] pos = (int[]) ((Object[]) text.render.elementAt(l - 1))[3];
				System.out.println(pos[0] + pos[2] + " " + timeWidth + " - " + cw);
				timeBreak = pos[0] + pos[2] + timeWidth >= cw;
			}
		}
		
		// time
		if (timeBreak) h += MP.smallPlainFontHeight;
		this.timeBreak = timeBreak;
		
		// comment
		if (commentsText != null) {
			h += PADDING_HEIGHT * 2 + 1 + MP.smallBoldFontHeight;
		}
		
		return contentHeight = h;
	}
	
	boolean grabFocus(int dir) {
		focus = true;
		System.out.println("grabFocus " + dir);
		if (dir != 0 && subFocusLength != 0) {
			if (subFocusCurrent == -1) {
				subFocusCurrent = dir == -1 ? subFocusLength - 1 : 0;
			}
			subFocus(subFocusCurrent);
		} else {
			subFocusCurrent = -1;
		}
		if (focusChild != null) {
			if (!focusChild.grabFocus(dir)) focusChild = null;
		}
		return true;
	}
	
	private void subFocus(int idx) {
		System.out.println("subfocus [" + idx + "] = " + subFocus[idx]);
		switch (subFocus[idx]) {
		case FOCUS_SENDER:
			break;
		case FOCUS_TEXT:
			if (text != null) {
				focusChild = text;
			}
			break;
		case FOCUS_COMMENT:
			break;
		}
	}

	void lostFocus() {
		focus = false;
		if (focusChild != null) {
			focusChild.lostFocus();
			focusChild = null;
		}
	}
	
	int traverse(int dir, int height, int scrollY) {
		if (focusChild != null) {
			int t = focusChild.traverse(dir, height, scrollY);
			if (t != 0) {
				return t;
			}
			focusChild.lostFocus();
			focusChild = null;
		}
		if (subFocusLength != 0) {
			if (subFocusCurrent == -1) {
				subFocusCurrent = 0;
			}
			if (dir == Canvas.UP) {
				if (subFocusCurrent == 0)
					return 0;
				subFocus(--subFocusCurrent);
				if (focusChild != null) {
					if (!focusChild.grabFocus(dir)) focusChild = null;
				}
				return Integer.MAX_VALUE;
			} else if (dir == Canvas.DOWN) {
				if (subFocusCurrent == subFocusLength - 1)
					return 0;
				subFocus(++subFocusCurrent);
				if (focusChild != null) {
					if (!focusChild.grabFocus(dir)) focusChild = null;
				}
				return Integer.MAX_VALUE;
			}
		}
		return 0;
	}
	
	boolean action() {
		if (focusChild != null && focusChild.action()) {
			return true;
		} else if (subFocusCurrent != -1) {
			switch (subFocus[subFocusCurrent]) {
			case FOCUS_SENDER:
				profileAction();
				return true;
			case FOCUS_COMMENT:
				commentAction();
				return true;
			}
		}
		return false;
	}
	
	int[] menu() {
		int[] menu = focusChild != null ? focusChild.menu() : null;
		
		return menu;
	}
	
	void tap(int x, int y) {
		subFocusCurrent = -1;
		int w = ((ChatCanvas) container).width;
		int cw = Math.min(MAX_WIDTH, w);
		if (out && w < 900) {
			x -= w - cw;
		}
		x -= MARGIN_WIDTH + (out && w < 900 ? MARGIN_SIDE : 0);
		System.out.println("tap " + x + " " + y);
		if (x < 0) return;
		if (!out && y < MP.smallPlainFontHeight + PADDING_HEIGHT + MARGIN_HEIGHT && x < PADDING_WIDTH + senderWidth) {
			profileAction();
			return;
		}
		if (text != null && text.focusable && y > text.y && y < text.y + text.contentHeight) {
			focusChild = text;
			text.tap(x, y - text.y);
			return;
		}
		// TODO options
	}
	
	private void profileAction() {
		if (fromId != null) MP.openProfile(fromId, null, 0);
	}
	
	private void commentAction() {
		MP.openLoad(new ChatForm(commentPeer, ((ChatCanvas) container).id, id, commentRead));
	}
	
	public String toString() {
		return "UIMessage{" + y + " " + contentHeight + "}";
	}

}
