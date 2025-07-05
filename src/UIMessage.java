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

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;

public class UIMessage extends UIItem implements LangConstants {
	
	private static final int MAX_WIDTH = 440;
	private static final int MARGIN_TOP = 1;
	private static final int MARGIN_WIDTH = 2;
	private static final int MARGIN_SIDE = 10;
	private static final int PADDING_WIDTH = 4;
	private static final int PADDING_HEIGHT = 4;
	private static final int TIME_PADDING_WIDTH = 2;
	private static final int TIME_PADDING_HEIGHT = 1;
	private static final int DATE_PADDING_HEIGHT = 2;
	private static final int DATE_MARGIN_HEIGHT = 1;
	private static final int SPACE_HEIGHT = 4;
	
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
	
	int id;
	boolean out;
	boolean edited;
	String name;
	String fromId;
	boolean action;
	long date;
	String commentPeer;
	int commentRead;
	
	boolean fwd, reply, media, photo;
	String replyName, replyText, replyPrefix;
	String commentsText;
	String mediaTitle, mediaSubtitle;
	boolean mediaPlayable, mediaDownload, mediaBrowser;
	String forwardName;
	
	String time, nameRender, dateRender, replyNameRender, replyTextRender, forwardRender;
	int timeWidth, dateWidth, senderWidth, replyPrefixWidth;
	int mediaRenderHeight;
	boolean showDate, hideName, timeBreak, space;
	int forwardedFromWidth;
	
	UIMessage(JSONObject message, ChatCanvas chat) {
		focusable = true;
		subFocus = new int[8];
		date = message.getLong("date");
		id = message.getInt("id");
		fromId = message.has("from_id") ? message.getString("from_id") : chat.id;
		out = (message.getBoolean("out", false) && !chat.broadcast) || chat.selfChat;
		hideName = chat.selfChat || chat.user || out;
		space = chat.broadcast;
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
					label.appendWord(name, MP.smallBoldFont, "t.me/".concat(fromId));
					if ("ChatAddUser".equals(type) || "ChatDeleteUser".equals(type)) {
						if (fromId.equals(user)) {
							t = MP.L["ChatAddUser".equals(type) ? Joined_Action : Left_Action];
						} else {
							label.appendWord(MP.L["ChatAddUser".equals(type) ? Added_Action : Removed_Action], MP.smallPlainFont, null);
							label.appendWord(" ", MP.smallPlainFont, null);
							label.appendWord(MP.getName(user, false), MP.smallBoldFont, "t.me/".concat(user));
							break l;
						}
					} else {
						label.appendWord(" ", MP.smallPlainFont, null);
						break l;
					}
				}
				
				if (t != null) {
					label.appendWord(t, MP.smallPlainFont, null);
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
			String t = null;
			if ((t = fwd.getString("from_name", null)) == null) {
				t = MP.getName(fwd.getString("from_id", null), true);
			}
			forwardName = t;
			forwardedFromWidth = MP.smallPlainFont.stringWidth(MP.L[ForwardedFrom]);
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
					if (replyMsg.has("media")) {
						String type = replyMsg.getObject("media").getString("type", null);
						t = MP.L[Media];
						if (type != null) {
							if ("photo".equals(type)) {
								t = MP.L[Photo];
							} else if ("photo".equals(type)) {
								t = MP.L[Video];
							} else if ("document".equals(type)) {
								t = MP.L[File];
							}
						}
						replyPrefix = t;
						replyPrefixWidth = MP.smallPlainFont.stringWidth(replyPrefix);
					}
					
					if (reply.has("quote")) {
						replyText = reply.getString("quote");
					} else if ((t = replyMsg.getString("text", null)) != null && t.length() != 0) {
						replyText = t;
					}
					if (replyText != null || replyPrefix != null || replyName != null) {
						subFocus[order++] = FOCUS_REPLY;
					}
				}
			}
		}
		
		// photo TODO
		boolean photo = false;
		JSONObject media = message.getObject("media", null);
		if (MP.showMedia
				&& message.has("media") && !message.isNull("media")
				&& media.getString("type").equals("photo")) {
			photo = true;
//			this.photo = true;
//			subFocus[order++] = FOCUS_PHOTO;
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
		
		// media TODO
		if (message.has("media") && !photo) {
			if (!MP.showMedia || message.isNull("media")) {
				
			} else {
//				this.media = true;
//				subFocus[order++] = FOCUS_MEDIA;
			}
		}
		
		// buttons TODO
//		if (message.has("markup")) {
//			subFocus[order++] = FOCUS_BUTTONS;
//		}
		
		// comments
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
		if (space) {
			if (((ChatCanvas) container).reverse) y += SPACE_HEIGHT;
			h -= SPACE_HEIGHT;
		}
		// date
		if (showDate) {
			if (((ChatCanvas) container).reverse) {
				y += MARGIN_TOP;
				g.setColor(0x1E2C3A);
				g.setFont(MP.smallBoldFont);
				g.fillRect((((w - dateWidth)) >> 1) - 4, y, dateWidth + 8, MP.smallBoldFontHeight + DATE_PADDING_HEIGHT * 2);
				g.setColor(-1);
				g.drawString(dateRender, (w - dateWidth) >> 1, y += DATE_PADDING_HEIGHT, 0);
				y += DATE_MARGIN_HEIGHT + DATE_PADDING_HEIGHT + MP.smallBoldFontHeight;
			}
			h -= DATE_MARGIN_HEIGHT * 2 + DATE_PADDING_HEIGHT * 2 + MP.smallBoldFontHeight;
		}
		// chat action
		if (action) {
			if (text != null) {
				y += MARGIN_TOP;
				g.setColor(0x1E2C3A);
				g.fillRect(x + (w - text.contentWidth - PADDING_WIDTH) >> 1, y, text.contentWidth + PADDING_WIDTH, text.contentHeight + PADDING_HEIGHT * 2);
				text.paint(g, x, y + PADDING_HEIGHT, w);
			}
			return;
		}
		int cw = contentWidth;
		if (out && w < 900) {
			x += w - cw;
		}
		g.setColor(out ? 0x2B5278 : 0x182533); // TODO message bg color
		g.fillRect(x += MARGIN_WIDTH + (out && w < 900 ? MARGIN_SIDE : 0), y += MARGIN_TOP,
				cw -= MARGIN_WIDTH * 2 + MARGIN_SIDE, h -= (MARGIN_TOP));
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
		if (fwd) {
			g.setColor(0x71BAFA);
			g.setFont(MP.smallPlainFont);
			g.drawString(MP.L[ForwardedFrom], x, y, 0);

			g.setFont(MP.smallPlainFont);
			if (forwardRender != null) g.drawString(forwardRender, x + forwardedFromWidth, y, 0);
			y += MP.smallBoldFontHeight;
		}
		
		// reply
		if (reply) {
			y += 2;
			g.setColor(0x71BAFA);
			int rh = MP.smallPlainFontHeight;
			if (replyName != null) {
				rh += MP.smallPlainFontHeight;
			}
			g.fillRect(x, y, 2, rh);
			if (replyNameRender != null) {
				g.setColor(-1);
				g.setFont(MP.smallBoldFont);
				g.drawString(replyNameRender, x + 6, y, 0);
				y += MP.smallPlainFontHeight;
			}
			int px = x + 6;
			if (replyPrefix != null) {
				g.setColor(0x71BAFA);
				g.setFont(MP.smallPlainFont);
				g.drawString(replyPrefix, x + 6, y, 0);
				px += MP.smallPlainFontSpaceWidth + replyPrefixWidth;
			}
			if (replyTextRender != null) {
				g.setColor(-1);
				g.setFont(MP.smallPlainFont);
				g.drawString(replyTextRender, px, y, 0);
			}
			y += MP.smallPlainFontHeight;
			y += 2;
		}
		
		// photos
		if (photo) {
			
		}

		// text
		if (text != null) {
			UILabel text = this.text;
			text.paint(g, x, y, cw);
			y += text.contentHeight;
		}
		
		// media
		if (media) {
			g.setColor(0x6AB3F3);
			int rh = mediaRenderHeight;
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
		
		// comment
		if (commentsText != null) {
			y += PADDING_HEIGHT;
			g.setColor(0x31404E);
			g.drawLine(rx, y, rx + cw, y++);
			
			g.setFont(MP.smallBoldFont);
			g.setColor(0x71BAFA);
			g.drawString(commentsText, x, y, 0);
			y += PADDING_HEIGHT + MP.smallBoldFontHeight;
		}
		
		//y += MARGIN_BOTTOM;
		
		// date not reversed
		if (showDate && !((ChatCanvas) container).reverse) {
			y += DATE_MARGIN_HEIGHT;
			g.setColor(0x1E2C3A);
			g.setFont(MP.smallBoldFont);
			g.fillRect(x - PADDING_WIDTH + (w - dateWidth - x) >> 1, y, dateWidth + PADDING_WIDTH * 2, MP.smallBoldFontHeight + DATE_PADDING_HEIGHT * 2);
			g.setColor(-1);
			g.drawString(dateRender, x + (w - dateWidth - x) >> 1, y += DATE_PADDING_HEIGHT, 0);
			y += DATE_MARGIN_HEIGHT + DATE_PADDING_HEIGHT + MP.smallBoldFontHeight;
		}
	}
	
	public int layout(int width) {
		if (!layoutRequest && layoutWidth == width) {
			return contentHeight;
		}
		layoutWidth = width;
		int h = MARGIN_TOP + PADDING_HEIGHT * 2;
		// grouping
		if (container instanceof ChatCanvas) {
			ChatCanvas chat = ((ChatCanvas) container);
			boolean reverse = chat.reverse;
			
			date: {
				if (this.next != null) {
					UIMessage next = (UIMessage) this.next;
					if (dateRender.equals(next.dateRender)) {
						showDate = false;
						break date;
					}
				} else {
					showDate = false;
					break date;
				}
				showDate = true;
				h += MARGIN_TOP + DATE_PADDING_HEIGHT * 2 + MP.smallBoldFontHeight;
			}
			if (!chat.broadcast) {
				boolean group = false;
				if (this.next != null) {
					UIMessage next = (UIMessage) this.next;
					if (fromId.equals(next.fromId) && date - next.date < 6 * 60) {
						group = true;
					}
				}
				space = !group;
				if (!reverse) {
					group = false;
					if (this.prev != null) {
						UIMessage next = (UIMessage) this.prev;
						if (fromId.equals(next.fromId) && next.date - date < 6 * 60) {
							group = true;
						}
					}
				}
				hideName = hideName || (!showDate && group);
			}
			if (space && reverse) h += SPACE_HEIGHT;
		}
		// chat action
		if (action) {
//			h += MP.medPlainFontHeight;
			if (text != null) {
				text.y = h - MARGIN_TOP - PADDING_HEIGHT;
				h += text.layout(width);
			}
			return contentHeight = h;
		}
		
		int maxW = Math.min(MAX_WIDTH, width);
		int cw = maxW - PADDING_WIDTH * 2 - MARGIN_WIDTH * 2 - MARGIN_SIDE;
		
		// sender
		if (!hideName) {
			h += MP.smallBoldFontHeight;
			nameRender = UILabel.ellipsis(name, MP.smallBoldFont, cw - PADDING_WIDTH * 2);
			senderWidth = MP.smallBoldFont.stringWidth(nameRender);
		}
		
		// forward
		if (fwd) {
			forwardRender = UILabel.ellipsis(forwardName, MP.smallBoldFont, cw - PADDING_WIDTH * 2 - forwardedFromWidth);
			h += MP.smallBoldFontHeight;
		}
		
		// reply
		if (reply) {
			if (replyName != null) {
				h += MP.smallPlainFontHeight;
				replyNameRender = UILabel.ellipsis(replyName, MP.smallBoldFont, cw - 10);
			}
			int rw = cw - 10;
			if (replyPrefix != null) {
				rw -= replyPrefixWidth + MP.smallPlainFontSpaceWidth;
			}
			if (replyText != null) {
				replyTextRender = UILabel.ellipsis(replyText, MP.smallPlainFont, rw);
			}
			h += 4 + MP.smallPlainFontHeight;
		}
		
		int timeWidth = this.timeWidth;
		boolean timeBreak = text == null;
		if (edited) {
			timeWidth += MP.smallPlainFontSpaceWidth + MP.smallPlainFont.stringWidth(MP.L[Edited]);
		}
		
		// text
		if (text != null) {
			text.y = h - MARGIN_TOP - PADDING_HEIGHT;
			h += text.layout(cw);
			int l = text.render.size(); 
			if (l != 0) {
				int[] pos = (int[]) ((Object[]) text.render.elementAt(l - 1))[3];
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
		if (space && !((ChatCanvas) container).reverse) h += SPACE_HEIGHT;
		
		// TODO pack width
		contentWidth = maxW;
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
		if (!out && y < MP.smallPlainFontHeight + PADDING_HEIGHT + MARGIN_TOP && x < PADDING_WIDTH + senderWidth) {
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

}
