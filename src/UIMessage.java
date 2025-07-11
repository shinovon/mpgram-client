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
import javax.microedition.lcdui.Image;

public class UIMessage extends UIItem implements LangConstants {
	
	// colors enum
	static final int COLOR_MESSAGE_BG = 20;
	static final int COLOR_MESSAGE_OUT_BG = 21;
	static final int COLOR_MESSAGE_FG = 22;
	static final int COLOR_MESSAGE_LINK = 23;
	static final int COLOR_MESSAGE_LINK_FOCUS = 24;
	static final int COLOR_MESSAGE_SENDER = 25;
	static final int COLOR_MESSAGE_ATTACHMENT_BORDER = 26;
	static final int COLOR_MESSAGE_ATTACHMENT_TITLE = 27;
	static final int COLOR_MESSAGE_ATTACHMENT_SUBTITLE = 28;
	static final int COLOR_MESSAGE_ATTACHMENT_FOCUS_BG = 29;
	static final int COLOR_MESSAGE_COMMENT_BORDER = 30;
	static final int COLOR_MESSAGE_IMAGE = 31;
	static final int COLOR_MESSAGE_FOCUS_BORDER = 32;
	static final int COLOR_MESSAGE_TIME = 33;
	static final int COLOR_MESSAGE_OUT_TIME = 34;
	static final int COLOR_ACTION_BG = 35;
	
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
	private static final int FOCUS_MEDIA = 3;
	private static final int FOCUS_TEXT = 4;
	private static final int FOCUS_BUTTONS = 5; // TODO
	private static final int FOCUS_COMMENT = 6;
	
	UILabel text;
	
	UIItem focusChild;
	int subFocusCurrent = -1;
	int[] subFocus = new int[8];
	int subFocusLength;
	int[] touchZones = new int[31]; /* [x1, y1, x2, y2, action], ..., Integer.MIN_VALUE */
	
	int id;
	boolean out;
	boolean edited;
	String name;
	String fromId;
	boolean action;
	long date;
	String commentPeer;
	int commentRead;
	String peerId;
	
	boolean fwd, reply, media, photo, sticker;
	String replyName, replyText, replyPrefix;
	String commentsText;
	String mediaTitle, mediaSubtitle;
	boolean mediaPlayable, mediaDownload, mediaThumb;
	String mediaFileName, mediaUrl;
	String forwardName;
	String fwdFromId, fwdPeer;
	int fwdMsgId;
	String replyPeer;
	int replyMsgId;
	int photoRawWidth, photoRawHeight;
	
	String time, nameRender, dateRender;
	String replyNameRender, replyTextRender, forwardRender;
	String mediaTitleRender, mediaSubtitleRender;
	int timeWidth, dateWidth, senderWidth, replyPrefixWidth, forwardNameWidth;
	int mediaRenderHeight;
	boolean showDate, hideName, timeBreak, space;
	int forwardedFromWidth;
	int photoRenderWidth, photoRenderHeight;
	
	Image mediaImage;
	
	UIMessage(JSONObject message, ChatCanvas chat) {
		focusable = true;

		date = message.getLong("date");
		id = message.getInt("id");
		fromId = message.has("from_id") ? message.getString("from_id") : chat.id;
		out = (message.getBoolean("out", false) && !chat.broadcast);
		hideName = chat.selfChat || chat.user || out;
		space = chat.broadcast;
		name = out && !chat.broadcast ? MP.L[You] : MP.getName(fromId, true).trim();
		dateRender = MP.localizeDate(date, 0);
		dateWidth = MP.smallBoldFont.stringWidth(dateRender);
		edited = message.has("edit");
		peerId = chat.id;
		
		if ((action = message.has("act"))) {
			JSONObject act = message.getObject("act");
			String type = act.getString("_");
			String user = act.getString("user", null);
			
			UILabel label = new UILabel();
			label.color = ChatCanvas.colors[COLOR_MESSAGE_FG];
			label.linkColor = ChatCanvas.colors[COLOR_MESSAGE_FG];
			label.bgColor = ChatCanvas.colors[COLOR_ACTION_BG];
			label.background = true;
			label.center = true;
			label.focusColor = ChatCanvas.colors[COLOR_MESSAGE_LINK_FOCUS];
			
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
					if ("PinMessage".equals(type)) {
						t = MP.L[PinnedMessage_Action];
					} else if ("ChatJoinedByLink".equals(type)) {
						t = MP.L[JoinedByLink_Action];
					} else if ("ChatJoinedByRequest".equals(type)) {
						t = MP.L[JoinedByRequest_Action];
					} else {
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
							label.appendWord(MP.L[Action], MP.smallPlainFont, null);
							break l;
						}
					}
				}
				
				if (t != null) {
					label.appendWord(t, MP.smallPlainFont, null);
				}
			}
			this.text = label;
			
			return;
		}
		
		init(message, chat);
		
		StringBuffer sb = new StringBuffer();
		sb.setLength(0);
		time = MP.appendTime(sb, date).toString();
		timeWidth = MP.smallPlainFont.stringWidth(time);
	}
	
	private void init(JSONObject message, ChatCanvas chat) {
		subFocusCurrent = -1;
		layoutWidth = 0;
		
		int order = 0;
		
		if (!out && !hideName) {
			subFocus[order++] = FOCUS_SENDER;
		}
		
		// forwarded from... label
		if (message.has("fwd")) {
			fwd = true;
			JSONObject fwd = message.getObject("fwd");
			String t = null;
			fwdFromId = fwd.getString("from_id", null);
			if ((t = fwd.getString("from_name", null)) == null) {
				t = MP.getName(fwdFromId, true);
			}
			if (t != null) t = t.trim();
			forwardName = t;
			forwardedFromWidth = chat.selfChat ? 0 : MP.smallPlainFont.stringWidth(MP.L[ForwardedFrom]);
			if (fwd.has("peer") && fwd.has("msg")) {
				fwdPeer = fwd.getString("peer");
				fwdMsgId = fwd.getInt("msg");
				subFocus[order++] = FOCUS_FORWARD;
			} else if (fwdFromId != null) {
				subFocus[order++] = FOCUS_FORWARD;
			}
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
							} else if ("document".equals(type)) {
//								t = MP.L[Video];
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
					replyPeer = reply.getString("peer", null);
					replyMsgId = reply.getInt("id", 0);
					if ((replyText != null || replyPrefix != null || replyName != null) && (replyMsgId != 0)) {
						subFocus[order++] = FOCUS_REPLY;
					}
				}
			}
		}
		
		// media
		if (message.has("media")) {
			JSONObject media = message.getObject("media");
			if (!MP.showMedia || message.isNull("media")) {
				this.media = true;
				mediaTitle = MP.L[Media];
			} else {
				String t;
				String type = media.getString("type");
				if (type.equals("undefined")) {
					mediaTitle = MP.L[Media];
				} else if (type.equals("webpage")) {
					if ((t = media.getString("name", null)) != null && t.length() != 0) {
						mediaTitle = t;
					}
					if ((t = media.getString("title", null)) != null && t.length() != 0) {
						mediaSubtitle = t;
					}
					mediaUrl = media.getString("url");
				} else if (type.equals("document")) {
					if ("image/webp".equals(media.getString("mime", null)) && "sticker.webp".equals(media.getString("name", null))) {
						sticker = true;
						if (MP.loadThumbs) {
							MP.queueImage(this, this);
						} else {
							mediaTitle = MP.L[Sticker];
						}
					} else {
						mediaDownload = true;
						mediaPlayable = media.has("audio")
								&& ("audio/mpeg".equals(t = media.getString("mime", null))
										|| "audio/aac".equals(t)
										|| "audio/m4a".equals(t));
						mediaFileName = media.getString("name", null);
						StringBuffer sb = new StringBuffer();
						name: {
							if (media.has("audio")) {
								JSONObject audio = media.getObject("audio");
								if ((t = audio.getString("artist", null)) != null && t.length() != 0) {
									sb.append(t).append(" - ");
								}
								if ((t = audio.getString("title", null)) != null && t.length() != 0) {
									sb.append(t);
									break name;
								}
							}
							if ((t = media.getString("name", null)) != null && t.length() != 0) {
								sb.append(t);
							}
						}
						mediaTitle = sb.toString();
						
						if (!media.isNull("size")) {
							sb.setLength(0);
							long size = media.getLong("size");
							if (size >= 1024 * 1024) {
								size = (size * 100) / (1024 * 1024);
								sb.append(size / 100).append('.').append(size % 100).append(" MB");
							} else {
								size = (size * 100) / 1024;
								sb.append(size / 100).append('.').append(size % 100).append(" KB");
							}
							mediaSubtitle = sb.toString();
						}
						
						if (MP.loadThumbs && media.getBoolean("thumb", false)) {
							mediaThumb = true;
							MP.queueImage(this, this);
						}
					}
				} else if (type.equals("photo")) {
					mediaDownload = true;
					photo = true;
					if (MP.loadThumbs) {
						photoRawWidth = media.getInt("w", 0);
						photoRawHeight = media.getInt("h", 0);
						MP.queueImage(this, this);
					} else {
						mediaTitle = MP.L[Photo];
					}
				} else if (type.equals("poll")) {
					mediaTitle = MP.L[Poll];
				} else if (type.equals("geo")) {
					mediaTitle = MP.L[Geo];
					mediaSubtitle = media.get("lat") + ", " + media.get("long");
				} else {
					mediaTitle = MP.L[Media];
				}
				this.media = true;
				if (!sticker) subFocus[order++] = FOCUS_MEDIA;
			}
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
			label.color = ChatCanvas.colors[COLOR_MESSAGE_FG];
			label.linkColor = ChatCanvas.colors[COLOR_MESSAGE_LINK];
			label.focusColor = ChatCanvas.colors[COLOR_MESSAGE_LINK_FOCUS];
			if (label.focusable) subFocus[order++] = FOCUS_TEXT;
			this.text = label;
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
				g.setColor(ChatCanvas.colors[COLOR_ACTION_BG]);
				g.setFont(MP.smallBoldFont);
				g.fillRect((((w - dateWidth)) >> 1) - 4, y, dateWidth + 8, MP.smallBoldFontHeight + DATE_PADDING_HEIGHT * 2);
				g.setColor(ChatCanvas.colors[COLOR_MESSAGE_FG]);
				g.drawString(dateRender, (w - dateWidth) >> 1, y += DATE_PADDING_HEIGHT, 0);
				y += DATE_MARGIN_HEIGHT + DATE_PADDING_HEIGHT + MP.smallBoldFontHeight;
			}
			h -= DATE_MARGIN_HEIGHT * 2 + DATE_PADDING_HEIGHT * 2 + MP.smallBoldFontHeight;
		}
		// chat action
		if (action) {
			if (text != null) {
				y += MARGIN_TOP;
				g.setColor(ChatCanvas.colors[COLOR_ACTION_BG]);
				g.fillRect(x + (w - text.contentWidth - PADDING_WIDTH) >> 1, y, text.contentWidth + PADDING_WIDTH, text.contentHeight + PADDING_HEIGHT * 2);
				text.paint(g, x, y + PADDING_HEIGHT, w);
			}
			return;
		}
		int cw = contentWidth;
		if (out && w < 900) {
			x += w - cw;
		}
		g.setColor(ChatCanvas.colors[out ? COLOR_MESSAGE_OUT_BG : COLOR_MESSAGE_BG]);
		g.fillRect(x += MARGIN_WIDTH + (out && w < 900 ? MARGIN_SIDE : 0), y += MARGIN_TOP,
				cw -= MARGIN_WIDTH * 2 + MARGIN_SIDE, h -= (MARGIN_TOP));
		if (focus && focusChild == null && subFocusCurrent == -1) {
			g.setColor(ChatCanvas.colors[COLOR_MESSAGE_FOCUS_BORDER]);
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
			g.setColor(ChatCanvas.colors[COLOR_MESSAGE_SENDER]);
			g.setFont(MP.smallBoldFont);
			if (nameRender != null) g.drawString(nameRender, x, y, 0);
			if (focus && subFocusCurrent != -1 && subFocus[subFocusCurrent] == FOCUS_SENDER) {
				g.setColor(ChatCanvas.colors[COLOR_MESSAGE_LINK_FOCUS]);
				g.drawRect(x, y, senderWidth, MP.smallBoldFontHeight);
			}
			y += MP.smallBoldFontHeight;
		}
		
		// forward
		if (fwd) {
			g.setColor(ChatCanvas.colors[COLOR_MESSAGE_SENDER]);
			if (forwardedFromWidth != 0) {
				g.setFont(MP.smallPlainFont);
				g.drawString(MP.L[ForwardedFrom], x, y, 0);
			}

			if (forwardRender != null) {
				g.setFont(MP.smallBoldFont);
				g.drawString(forwardRender, x + forwardedFromWidth, y, 0);
				if (focus && subFocusCurrent != -1 && subFocus[subFocusCurrent] == FOCUS_FORWARD) {
					g.setColor(ChatCanvas.colors[COLOR_MESSAGE_LINK_FOCUS]);
					g.drawRect(x + forwardedFromWidth, y, forwardNameWidth, MP.smallBoldFontHeight);
				}
			}
			y += MP.smallBoldFontHeight;
		}
		
		// reply
		if (reply) {
			y += 2;
			g.setColor(ChatCanvas.colors[COLOR_MESSAGE_ATTACHMENT_BORDER]);
			int rh = MP.smallPlainFontHeight;
			if (replyName != null) {
				rh += MP.smallPlainFontHeight;
			}
			g.fillRect(x, y, 2, rh);
			if (focus && subFocusCurrent != -1 && subFocus[subFocusCurrent] == FOCUS_REPLY) {
				g.setColor(ChatCanvas.colors[COLOR_MESSAGE_ATTACHMENT_FOCUS_BG]);
				g.fillRect(x + 2, y, cw - 4, rh);
			}
			int px = x + 6;
			if (replyNameRender != null) {
				g.setColor(ChatCanvas.colors[COLOR_MESSAGE_ATTACHMENT_TITLE]);
				g.setFont(MP.smallBoldFont);
				g.drawString(replyNameRender, px, y, 0);
				y += MP.smallBoldFontHeight;
			}
			if (replyPrefix != null) {
				g.setColor(ChatCanvas.colors[COLOR_MESSAGE_SENDER]);
				g.setFont(MP.smallPlainFont);
				g.drawString(replyPrefix, px, y, 0);
				px += MP.smallPlainFontSpaceWidth + replyPrefixWidth;
			}
			if (replyTextRender != null) {
				g.setColor(ChatCanvas.colors[COLOR_MESSAGE_FG]);
				g.setFont(MP.smallPlainFont);
				g.drawString(replyTextRender, px, y, 0);
			}
			y += MP.smallPlainFontHeight;
			y += 2;
		}
		
		// media
		if (media) {
			if ((photo || sticker) && mediaTitle == null) {
				if (mediaImage == null) {
					// TODO photo placeholder
					g.setColor(ChatCanvas.colors[COLOR_MESSAGE_IMAGE]);
					g.fillRect(x, y + 1, photoRenderWidth, photoRenderHeight);
				} else {
					int clipX = g.getClipX(), clipY = g.getClipY(), clipW = g.getClipWidth(), clipH = g.getClipHeight();
					g.setClip(x, y + 1, photoRenderWidth, photoRenderHeight);
					g.drawImage(mediaImage, x, y + 1, 0);
					g.setClip(clipX, clipY, clipW, clipH);
				}
				y += photoRenderHeight + 2;
			} else {
				g.setColor(ChatCanvas.colors[COLOR_MESSAGE_ATTACHMENT_BORDER]);
				int rh = mediaRenderHeight;
				g.fillRect(x, y, 2, rh);
				if (focus && subFocusCurrent != -1 && subFocus[subFocusCurrent] == FOCUS_MEDIA) {
					g.setColor(ChatCanvas.colors[COLOR_MESSAGE_ATTACHMENT_FOCUS_BG]);
					g.fillRect(x + 2, y, cw - 4, rh);
				}
				int px = x + 6;
				if (mediaThumb) {
					int s = MP.smallBoldFontHeight + MP.smallPlainFontHeight;
					if (mediaImage != null) {
						g.drawImage(mediaImage, px, ty, 0);
					} else {
						// TODO thumb placeholder
//						g.setColor(ChatCanvas.colors[COLOR_MESSAGE_IMAGE]);
//						g.fillRect(px, y, s, s);
					}
					px += s + 2;
				}
				if (mediaTitleRender != null) {
					g.setColor(ChatCanvas.colors[COLOR_MESSAGE_FG]);
					g.setFont(MP.smallBoldFont);
					g.drawString(mediaTitleRender, px, y, 0);
					y += MP.smallBoldFontHeight;
				}
				if (mediaSubtitleRender != null) {
					g.setColor(ChatCanvas.colors[COLOR_MESSAGE_ATTACHMENT_SUBTITLE]);
					g.setFont(MP.smallPlainFont);
					g.drawString(mediaSubtitleRender, px, y, 0);
					y += MP.smallPlainFontHeight;
				}
				y += 2;
			}
		}

		// text
		if (text != null) {
			UILabel text = this.text;
			text.paint(g, x, y, cw);
			y += text.contentHeight;
		}
		
		// buttons
		
		// time
		if (timeBreak) y += MP.smallPlainFontHeight;
		g.setColor(ChatCanvas.colors[out ? COLOR_MESSAGE_OUT_TIME : COLOR_MESSAGE_TIME]);
		g.setFont(MP.smallPlainFont);
		g.drawString(time, rx + rw - TIME_PADDING_WIDTH, y + PADDING_HEIGHT - TIME_PADDING_HEIGHT, Graphics.BOTTOM | Graphics.RIGHT);
		if (edited) {
			g.drawString(MP.L[Edited], rx + rw - timeWidth - MP.smallPlainFontSpaceWidth - TIME_PADDING_WIDTH, y + PADDING_HEIGHT - TIME_PADDING_HEIGHT, Graphics.BOTTOM | Graphics.RIGHT);
		}
		
		y += PADDING_HEIGHT;
		
		// comment
		if (commentsText != null) {
			y += PADDING_HEIGHT;
			g.setColor(ChatCanvas.colors[COLOR_MESSAGE_COMMENT_BORDER]);
			g.drawLine(rx, y, rx + cw, y++);
			
			g.setFont(MP.smallBoldFont);
			g.setColor(ChatCanvas.colors[COLOR_MESSAGE_SENDER]);
			g.drawString(commentsText, x, y, 0);
			y += PADDING_HEIGHT + MP.smallBoldFontHeight;
		}
		
		//y += MARGIN_BOTTOM;
		
		// date not reversed
		if (showDate && !((ChatCanvas) container).reverse) {
			y += DATE_MARGIN_HEIGHT;
			g.setColor(ChatCanvas.colors[COLOR_ACTION_BG]);
			g.setFont(MP.smallBoldFont);
			g.fillRect(x - PADDING_WIDTH + (w - dateWidth - x) >> 1, y, dateWidth + PADDING_WIDTH * 2, MP.smallBoldFontHeight + DATE_PADDING_HEIGHT * 2);
			g.setColor(ChatCanvas.colors[COLOR_MESSAGE_FG]);
			g.drawString(dateRender, x + (w - dateWidth - x) >> 1, y += DATE_PADDING_HEIGHT, 0);
			y += DATE_MARGIN_HEIGHT + DATE_PADDING_HEIGHT + MP.smallBoldFontHeight;
		}
	}
	
	public int layout(int width) {
		if (!layoutRequest && layoutWidth == width) {
			return contentHeight;
		}
		int order = 0;
		layoutWidth = width;
		int h = MARGIN_TOP + PADDING_HEIGHT;
		int y = 0;
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
				if (!reverse) y -= h;
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
				text.y = h + y;
				h += text.layout(width);
			}
			
			touchZones[order] = Integer.MIN_VALUE;
			return contentHeight = h += PADDING_HEIGHT;
		}
		
		int minW = PADDING_WIDTH * 2 + MARGIN_WIDTH * 2 + MARGIN_SIDE, maxW = minW;
		int cw = Math.min(MAX_WIDTH, width) - PADDING_WIDTH * 2 - MARGIN_WIDTH * 2 - MARGIN_SIDE;
		int x = MARGIN_WIDTH + PADDING_WIDTH;
		
		// sender
		if (!hideName) {
			nameRender = UILabel.ellipsis(name, MP.smallBoldFont, cw - PADDING_WIDTH * 2);
			senderWidth = MP.smallBoldFont.stringWidth(nameRender);
			maxW = Math.max(maxW, minW + senderWidth);
			
			touchZones[order ++] = x;
			touchZones[order ++] = h + y;
			touchZones[order ++] = x + senderWidth;
			touchZones[order ++] = (h += MP.smallBoldFontHeight) + y;
			touchZones[order ++] = FOCUS_SENDER;
		}
		
		// forward
		if (fwd) {
			forwardRender = UILabel.ellipsis(forwardName, MP.smallBoldFont, cw - PADDING_WIDTH * 2 - forwardedFromWidth);
			forwardNameWidth = MP.smallBoldFont.stringWidth(forwardRender);
			maxW = Math.max(maxW, minW + forwardNameWidth + forwardedFromWidth);
			
			int tx = x + forwardedFromWidth;
			touchZones[order ++] = tx;
			touchZones[order ++] = h + y;
			touchZones[order ++] = tx + forwardNameWidth;
			touchZones[order ++] = (h += MP.smallBoldFontHeight) + y;
			touchZones[order ++] = FOCUS_FORWARD;
		}
		
		// reply
		if (reply) {
			int ry = h;
			if (replyName != null) {
				replyNameRender = UILabel.ellipsis(replyName, MP.smallBoldFont, cw - 10);
				h += MP.smallBoldFontHeight;
				maxW = Math.max(maxW, minW + MP.smallBoldFont.stringWidth(replyNameRender) + 10);
			}
			int rw = cw - 10;
			if (replyPrefix != null) {
				rw -= replyPrefixWidth + MP.smallPlainFontSpaceWidth;
				maxW = Math.max(maxW, minW + replyPrefixWidth + 10);
			}
			if (replyText != null) {
				replyTextRender = UILabel.ellipsis(replyText, MP.smallPlainFont, rw);
				maxW = Math.max(maxW, minW + MP.smallPlainFont.stringWidth(replyTextRender) + replyPrefixWidth + MP.smallPlainFontSpaceWidth + 10);
			}
			h += 4 + MP.smallPlainFontHeight;
			
			touchZones[order ++] = x + 2;
			touchZones[order ++] = ry + y;
			touchZones[order ++] = x + cw;
			touchZones[order ++] = h + y;
			touchZones[order ++] = FOCUS_REPLY;
		}
		
		int timeWidth = this.timeWidth + TIME_PADDING_WIDTH * 2;
		boolean timeBreak = false;
		if (edited) {
			timeWidth += MP.smallPlainFontSpaceWidth + MP.smallPlainFont.stringWidth(MP.L[Edited]);
		}
		
		// media
		if (media) {
			if ((photo || sticker) && mediaTitle == null) {
				int pw, ph;
				if (mediaImage != null && Math.abs((ph = mediaImage.getHeight()) - photoRenderHeight) < 2) {
					pw = mediaImage.getWidth();
					if (pw > cw) {
						pw = cw;
						ph = (photoRawHeight * pw) / photoRawWidth;
					}
					// resize?
					photoRenderWidth = pw;
					photoRenderHeight = ph;
				} else if (photoRawHeight != 0) {
					int s = Math.min(cw, MP.photoSize);
					ph = MP.photoSize;
					pw = (photoRawWidth * ph) / photoRawHeight;
					if (pw > s) {
						pw = s;
						ph = (photoRawHeight * pw) / photoRawWidth;
					}
					photoRenderWidth = pw;
					photoRenderHeight = ph;
				} else {
					ph = pw = photoRenderWidth = photoRenderHeight = Math.min(cw, MP.photoSize);
				}
				maxW = Math.max(maxW, minW + pw);
				
				touchZones[order ++] = x;
				touchZones[order ++] = h + y;
				touchZones[order ++] = x + pw;
				touchZones[order ++] = (h += (ph) + 2) + y;
				touchZones[order ++] = FOCUS_MEDIA;
			} else {
				int mx = 10;
				if (mediaThumb) {
					mx += MP.smallBoldFontHeight + MP.smallPlainFontHeight + 2;
				}
				maxW = Math.max(maxW, minW + mx);
				int mh = 0;
				if (mediaTitle != null) {
					mediaTitleRender = UILabel.ellipsis(mediaTitle, MP.smallBoldFont, cw - mx);
					maxW = Math.max(maxW, minW + MP.smallBoldFont.stringWidth(mediaTitleRender) + mx);
					mh += MP.smallBoldFontHeight;
				}
				if (mediaSubtitle != null) {
					mediaSubtitleRender = UILabel.ellipsis(mediaSubtitle, MP.smallPlainFont, cw - mx);
					maxW = Math.max(maxW, minW + MP.smallBoldFont.stringWidth(mediaSubtitleRender) + mx);
					mh += MP.smallPlainFontHeight;
				}
				
				touchZones[order ++] = x + 2;
				touchZones[order ++] = h + y;
				touchZones[order ++] = x + cw;
				touchZones[order ++] = (h += (mediaRenderHeight = mh) + 2) + y;
				touchZones[order ++] = FOCUS_MEDIA;
			}
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
			maxW = Math.max(maxW, minW + text.contentWidth);
		} else {
			timeBreak = maxW + timeWidth >= cw;
		}
		
		// time
		if (timeBreak) {
			h += MP.smallPlainFontHeight;
		} else {
			maxW += timeWidth;
		}
		this.timeBreak = timeBreak;
		
		// comment
		if (commentsText != null) {
			maxW = Math.max(maxW, minW + MP.smallBoldFont.stringWidth(commentsText));
			touchZones[order ++] = x;
			touchZones[order ++] = h + y;
			touchZones[order ++] = x + cw;
			touchZones[order ++] = (h += PADDING_HEIGHT * 2 + 1 + MP.smallBoldFontHeight) + y;
			touchZones[order ++] = FOCUS_COMMENT;
		}
		if (space && !((ChatCanvas) container).reverse) h += SPACE_HEIGHT;
		
		touchZones[order] = Integer.MIN_VALUE;
		contentWidth = Math.min(maxW, Math.min(MAX_WIDTH, width));
		return contentHeight = h += PADDING_HEIGHT;
	}
	
	boolean grabFocus(int dir) {
		focus = true;
		if (dir != 0 && subFocusLength != 0) {
			if (subFocusLength != 1 || subFocus[0] != FOCUS_SENDER || !hideName) {
				if (subFocusCurrent == -1) {
					subFocusCurrent = dir == -1 ? subFocusLength - 1
							: subFocusLength > 1 && subFocus[0] == FOCUS_SENDER && hideName ? 1 : 0;
				}
				subFocus(subFocusCurrent);
			}
		} else {
			subFocusCurrent = -1;
		}
		if (focusChild != null) {
			if (!focusChild.grabFocus(dir)) focusChild = null;
		}
		return true;
	}
	
	private void subFocus(int idx) {
		switch (subFocus[idx]) {
		case FOCUS_TEXT:
			if (text != null) {
				focusChild = text;
			}
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
		
		// TODO scroll
		
		if (subFocusLength != 0) {
			if (subFocusLength != 1 || subFocus[0] != FOCUS_SENDER || !hideName) {
				if (subFocusLength > 1 && subFocusCurrent == 0 && subFocus[0] == FOCUS_SENDER && hideName) {
					subFocusCurrent = 1;
				} else if (subFocusCurrent == -1) {
					subFocusCurrent = 0;
				}
				if (dir == Canvas.UP) {
					if (subFocusCurrent == 0)
						return 0;
					if (subFocusCurrent == 1 && subFocus[0] == FOCUS_SENDER && hideName)
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
		}
		return 0;
	}
	
	boolean action() {
		if (focusChild != null && focusChild.action()) {
			return true;
		} else if (subFocusCurrent != -1) {
			return action(subFocus[subFocusCurrent]);
		}
		return false;
	}
	
	private boolean action(int focus) {
		switch (focus) {
		case FOCUS_SENDER:
			profileAction();
			return true;
		case FOCUS_FORWARD:
			if (fwdPeer != null) {
				MP.openChat(fwdPeer, fwdMsgId);
			} else if (fwdFromId != null) {
				MP.openProfile(fwdFromId, null, 0);
			}
			return true;
		case FOCUS_REPLY:
			if (replyPeer == null || replyPeer.equals(peerId)) {
				((ChatCanvas) container).openMessage(Integer.toString(replyMsgId), -1);
				return true;
			}
			MP.openChat(replyPeer, replyMsgId);
			return true;
		case FOCUS_MEDIA:
			if (mediaPlayable) {
				menuAction(Play_Item);
			} else if (photo) {
				menuAction(ViewImage);
			} else if (mediaDownload) {
				menuAction(Download);
			} else if (mediaUrl != null) {
				MP.midlet.browse(mediaUrl);
			}
			return true;
		case FOCUS_COMMENT:
			commentAction();
			return true;
		}
		return false;
	}
	
	void menuAction(int option) {
		String idStr = Integer.toString(this.id);
		switch (option) {
		case Reply:
			MP.display(MP.writeForm(peerId, idStr, "", null, null, null));
			break;
		case Edit:
			// TODO
			
			break;
		case Pin:
			MP.display(MP.loadingAlert(MP.L[Loading]), MP.current);
			if (MP.reopenChat && MP.updatesThread != null) {
				MP.cancel(MP.updatesThread, true);
			}
			MP.midlet.start(MP.RUN_PIN_MESSAGE, new String[] { peerId, idStr });
			break;
		case CopyMessage:
			if (text != null) {
				StringBuffer sb = new StringBuffer();
				int l = text.parsed.size();
				for (int i = 0; i < l; i++) {
					sb.append((String) ((Object[]) text.parsed.elementAt(i))[0]);
				}
				MP.copy("", sb.toString());
			}
			break;
		case CopyMessageLink:
			MP.copyMessageLink(peerId, idStr);
			break;
		case Forward:
			MP.openLoad(new ChatsList(peerId, idStr));
			break;
		case Delete:
			MP.display(MP.loadingAlert(MP.L[Loading]), MP.current);
			MP.midlet.start(MP.RUN_DELETE_MESSAGE, new String[] { peerId, idStr });
			break;
		case ViewImage:
			if (MP.useView) {
				MP.display(new ViewCanvas(peerId, idStr));
				break;
			}
		case Download:
			MP.midlet.downloadDocument(peerId, idStr, mediaFileName);
			break;
		case Play_Item:
			MP.display(MP.loadingAlert(MP.L[Loading]), MP.current);
			MP.midlet.start(MP.RUN_LOAD_PLAYLIST, new String[] {peerId, "3", idStr});
			break;
		case GoTo:
			// TODO close chat info
			((ChatCanvas) container).reset();
			((ChatCanvas) container).openMessage(idStr, -1);
			break;
		}
	}
	
	int[] menu() {
		int[] item = focusChild != null ? focusChild.menu() : null;
		
		if (item == null && subFocusCurrent != -1 && subFocusLength != 0) {
			switch (subFocus[subFocusCurrent]) {
			case FOCUS_MEDIA:
				if (mediaPlayable) {
					item = new int[] { Play_Item, Download };
				} else if (photo) {
					item = new int[] { ViewImage, Download };
				} else if (mediaDownload) {
					item = new int[] { Download };
				}
				break;
			}
		}
		
		int[] general = new int[10];
		int count = 0;
		ChatCanvas chat = (ChatCanvas) container;
		if (chat.query != null || chat.mediaFilter != null) {
			general[count++] = GoTo;
			general[count++] = Forward;
		} else {
			if (chat.canWrite) general[count++] = Reply;
			if (out) general[count++] = Edit;
			if (chat.canPin) general[count++] = Pin;
			general[count++] = CopyMessage;
			if (!chat.selfChat && !chat.user) general[count++] = CopyMessageLink;
			general[count++] = Forward;
			if (chat.canDelete) general[count++] = Delete;
		}
		general[count] = Integer.MIN_VALUE;
		if (item == null) {
			return general;
		}
		
		int[] menu = new int[item.length + general.length];
		System.arraycopy(item, 0, menu, 0, item.length);
		System.arraycopy(general, 0, menu, item.length, general.length);
		return menu;
	}
	
	boolean tap(int x, int y, boolean longTap) {
		subFocusCurrent = -1;
		int w = ((ChatCanvas) container).width;
		int cw = contentWidth;
		if (out && w < 900) {
			x -= w - cw;
		}
		x -= (out && w < 900 ? MARGIN_SIDE : 0);
		if (x < 0) return false;
		if (text != null && text.focusable && y > text.y && y < text.y + text.contentHeight) {
			focusChild = text;
			if (text.tap(x - PADDING_WIDTH - MARGIN_WIDTH, y - text.y, longTap))
				return true;
		}
		if (!longTap && (((ChatCanvas) container).query != null || ((ChatCanvas) container).mediaFilter != null)) {
			menuAction(GoTo);
			return true;
		}
		for (int i = 0; i < touchZones.length && touchZones[i] != Integer.MIN_VALUE; i += 5) {
			if (x >= touchZones[i] && y >= touchZones[i + 1] && x <= touchZones[i + 2] && y <= touchZones[i + 3]) {
				int focus = touchZones[i + 4];
				for (int j = 0; j < subFocusLength; j++) {
					if (subFocus[j] == focus) {
						subFocus(subFocusCurrent = j);
						break;
					}
				}
				if (!longTap && action(focus)) {
					return true;
				}
				break;
			}
		}
		((ChatCanvas) container).showMenu(this, menu());
		return true;
	}
	
	private void profileAction() {
		if (fromId != null) MP.openProfile(fromId, null, 0);
	}
	
	private void commentAction() {
		MP.openLoad(new ChatForm(commentPeer, peerId, id, commentRead));
	}

	public void edit(JSONObject msg, ChatCanvas chat) {
		edited = true;
		init(msg, chat);
	}

}
