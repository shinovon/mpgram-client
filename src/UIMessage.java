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
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Displayable;
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
	static final int COLOR_MESSAGE_OUT_READ = 36;
	
	static final int STYLE_MESSAGE_FILL = 0;
	static final int STYLE_MESSAGE_ROUND = 1;
	static final int STYLE_MESSAGE_BORDER = 2;
	
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
	private static final int READ_WIDTH = 20;
	
	private static final int FOCUS_SENDER = 0;
	private static final int FOCUS_FORWARD = 1;
	private static final int FOCUS_REPLY = 2;
	private static final int FOCUS_MEDIA = 3;
	private static final int FOCUS_TEXT = 4;
	private static final int FOCUS_BUTTONS = 5; // TODO
	private static final int FOCUS_COMMENT = 6;
	
	UILabel text;
	String origText;
	
	UIItem focusChild;
	int subFocusCurrent = -1;
	int[] subFocus = new int[8];
	int subFocusLength;
	int[] touchZones = new int[31]; /* [x1, y1, x2, y2, action], ..., Integer.MIN_VALUE */
	boolean selected;
	
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
	boolean read;
	
	boolean fwd, reply, media, photo, sticker, animatedSticker;
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
	long fileSize;
	boolean voice;
	
	String time, nameRender, dateRender;
	String replyNameRender, replyTextRender, forwardRender;
	String mediaTitleRender, mediaSubtitleRender;
	String reactsText;
	int timeWidth, dateWidth, senderWidth, replyPrefixWidth, forwardNameWidth;
	int mediaRenderHeight;
	boolean showDate, hideName, timeBreak, space, showReadMark;
	int forwardedFromWidth;
	int photoRenderWidth, photoRenderHeight;
	
	Image mediaImage;
	
	boolean updateColors;
	int focusDir = 1;
	private boolean imageQueued;
	
	UIMessage(JSONObject message, ChatCanvas chat) {
		focusable = true;

		date = message.getLong("date");
		id = message.getInt("id");
		fromId = message.has("from_id") ? message.getString("from_id") : chat.id;
		out = (!chat.broadcast && chat.mediaFilter == null) && (message.getBoolean("out", false)
				|| (fromId.equals(MP.selfId)) && (!chat.selfChat || !message.has("fwd")));
		hideName = chat.selfChat || chat.user || out || chat.mediaFilter != null;
		space = chat.broadcast;
		name = out && !chat.broadcast ? MP.L[LYou] : MP.getName(fromId, true).trim();
		dateRender = MP.localizeDate(date, 0);
		dateWidth = MP.smallBoldFont.stringWidth(dateRender);
		edited = message.has("edit") && chat.mediaFilter == null;
		peerId = chat.id;
		showReadMark = out && !chat.selfChat;
		
		if ((action = message.has("act"))) {
			JSONObject act = message.getObject("act");
			String type = act.getString("_");
			String user = act.getString("user", null);
			
			UILabel label = new UILabel();
			label.color = ChatCanvas.colors[COLOR_MESSAGE_FG];
			label.linkColor = ChatCanvas.colors[COLOR_MESSAGE_FG];
			label.bgColor = ChatCanvas.colors[COLOR_ACTION_BG];
			label.focusColor = ChatCanvas.colors[COLOR_MESSAGE_LINK_FOCUS];
//			label.background = true;
			label.center = true;
			
			String t = null;
			l: {
				if ("ChatCreate".equals(type)) {
					t = MP.L[LGroupCreated_Action];
				} else if ("ChannelCreate".equals(type)) {
					t = MP.L[LChannelCreated_Action];
				} else if ("ChatEditPhoto".equals(type)) {
					t = MP.L[LPhotoUpdated_Action];
				} else if ("HistoryClear".equals(type)) {
					t = MP.L[LChatHistoryCleared_Action];
				} else if ("ChatEditTitle".equals(type)) {
					t = MP.L[LNameChanged_Action].concat(act.getString("t", ""));
				} else {
					label.append(name, MP.smallBoldFont, "t.me/".concat(fromId));
					if ("PinMessage".equals(type)) {
						t = MP.L[LPinnedMessage_Action];
					} else if ("ChatJoinedByLink".equals(type)) {
						t = MP.L[LJoinedByLink_Action];
					} else if ("ChatJoinedByRequest".equals(type)) {
						t = MP.L[LJoinedByRequest_Action];
					} else {
						if ("ChatAddUser".equals(type) || "ChatDeleteUser".equals(type)) {
							if (fromId.equals(user)) {
								t = MP.L["ChatAddUser".equals(type) ? LJoined_Action : LLeft_Action];
							} else {
								label.append(MP.L["ChatAddUser".equals(type) ? LAdded_Action : LRemoved_Action], MP.smallPlainFont, null);
								label.append(" ", MP.smallPlainFont, null);
								label.append(MP.getName(user, false), MP.smallBoldFont, "t.me/".concat(user));
								break l;
							}
						} else {
							label.append(MP.L[LAction], MP.smallPlainFont, null);
							break l;
						}
					}
				}
				
				if (t != null) {
					label.append(t, MP.smallPlainFont, null);
				}
			}
			label.container = this;
			this.text = label;
			subFocusCurrent = 0;
			subFocus[0] = FOCUS_TEXT;
			subFocusLength = 1;
			
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
		
		if (chat.mediaFilter == null) {
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
				forwardedFromWidth = chat.selfChat ? 0 : MP.smallPlainFont.stringWidth(MP.L[LForwardedFrom]);
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
						replyName = t == null && chat.user ? (chat.selfChat ? MP.L[LYou] : chat.title) : t;
						if (replyMsg.has("media")) {
							String type = replyMsg.getObject("media").getString("type", null);
							t = MP.L[LMedia];
							if (type != null) {
								if ("photo".equals(type)) {
									t = MP.L[LPhoto];
								} else if ("document".equals(type)) {
//									t = MP.L[LVideo];
									t = MP.L[LFile];
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
		}
		
		// media
		if (message.has("media")) {
			JSONObject media = message.getObject("media");
			if (!MP.showMedia || message.isNull("media")) {
				this.media = true;
				mediaTitle = MP.L[LMedia];
			} else {
				String t;
				String type = media.getString("type");
				if (type.equals("undefined")) {
					mediaTitle = MP.L[LMedia];
				} else if (type.equals("webpage")) {
					if ((t = media.getString("name", null)) != null && t.length() != 0) {
						mediaTitle = t;
					}
					if ((t = media.getString("title", null)) != null && t.length() != 0) {
						mediaSubtitle = t;
					}
					mediaUrl = media.getString("url");
				} else if (type.equals("document")) {
					t = media.getString("mime", null);
					if ((animatedSticker = "application/x-tgsticker".equals(t))
							|| ("image/webp".equals(t) && "sticker.webp".equals(media.getString("name", null)))) {
						sticker = true;
						if (MP.loadThumbs) {
							if (!MP.lazyLoading) loadImage();
						} else {
							mediaTitle = MP.L[LSticker];
						}
					} else {
						mediaDownload = true;
						mediaPlayable = media.has("audio")
								&& ("audio/mpeg".equals(t)
										|| "audio/aac".equals(t)
										/*|| "audio/m4a".equals(t)*/);
						mediaFileName = media.getString("name", null);
						StringBuffer sb = new StringBuffer();
						name: {
							if (media.has("audio")) {
								JSONObject audio = media.getObject("audio");
								if (audio.getBoolean("voice", false)) {
									int time = audio.getInt("time");
									sb.append(time / 60).append(':').append(MP.n(time % 60));
									mediaSubtitle = sb.toString();
									sb.setLength(0);
									
									sb.append(MP.L[LVoiceMessage]);
									voice = true;
									break name;
								} else {
									if ((t = audio.getString("artist", null)) != null && t.length() != 0) {
										sb.append(t).append(" - ");
									}
									if ((t = audio.getString("title", null)) != null && t.length() != 0) {
										sb.append(t);
										break name;
									}
								}
							}
							if ((t = media.getString("name", null)) != null && t.length() != 0) {
								sb.append(t);
							}
						}
						mediaTitle = sb.toString();
						
						if (!media.isNull("size") && !voice) {
							sb.setLength(0);
							long size = fileSize = media.getLong("size");
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
							if (!MP.lazyLoading) loadImage();
						}
					}
				} else if (type.equals("photo")) {
					mediaDownload = true;
					photo = true;
					if (MP.loadThumbs) {
						photoRawWidth = media.getInt("w", 0);
						photoRawHeight = media.getInt("h", 0);
						if (!MP.lazyLoading) loadImage();
					} else {
						mediaTitle = MP.L[LPhoto];
					}
				} else if (type.equals("poll")) {
					// TODO
					mediaTitle = MP.L[LPoll];
				} else if (type.equals("geo")) {
					mediaTitle = MP.L[LGeo];
					mediaSubtitle = media.get("lat") + ", " + media.get("long");
				} else {
					mediaTitle = MP.L[LMedia];
				}
				this.media = true;
				if (!sticker) subFocus[order++] = FOCUS_MEDIA;
			}
		}
		
		if (chat.mediaFilter == null) {
			// text
			String text = origText = message.getString("text", null);
			if (text != null && text.length() != 0) {
				UILabel label;
				if (MP.parseRichtext && message.has("entities")) {
					label = new UILabel(text, message.getArray("entities"));
				} else {
					label = new UILabel(text, MP.getFont(null), null);
				}
				label.color = ChatCanvas.colors[COLOR_MESSAGE_FG];
				label.linkColor = ChatCanvas.colors[COLOR_MESSAGE_LINK];
				label.focusColor = ChatCanvas.colors[COLOR_MESSAGE_LINK_FOCUS];
				if (label.focusable) subFocus[order++] = FOCUS_TEXT;
				label.container = this;
				this.text = label;
			}
			
			// buttons
			if (message.has("markup")) {
				subFocus[order++] = FOCUS_BUTTONS;

				JSONArray markup = message.getArray("markup");
				int rows = markup.size();
				for (int i = 0; i < rows; i++) {
					JSONArray markupRow = markup.getArray(i);
					int cols = markupRow.size();
					for (int j = 0; j < cols; j++) {
						JSONObject markupItem = markupRow.getObject(j);

						// TODO
						if (markupItem.has("data")) {

						} else if (markupItem.has("url")) {

						}
					}
				}
			}
			
			// comments
			if (message.has("comments")) {
				JSONObject comments = message.getObject("comments");
				commentsText = MP.localizePlural(comments.getInt("count"), L_comment);
				commentPeer = comments.getString("peer");
				commentRead = comments.getInt("read", 0);
				subFocus[order++] = FOCUS_COMMENT;
			}

			if (message.has("reacts")) {
				JSONObject reacts = message.getObject("reacts");
				int reactsCount = reacts.getInt("count");
				if (reactsCount != 0) {
					reactsText = MP.localizePlural(reactsCount, L_react);
				}
			}
			read = id <= chat.readOutboxId;
		}
		
		subFocusLength = order;
	}
	
	void paint(Graphics g, int x, int y, int w) {
		int h = contentHeight;
		if (selected) {
			g.setColor(ChatCanvas.colors[ChatCanvas.COLOR_CHAT_HIGHLIGHT_BG]);
			g.fillRect(0, y, w, h);
		}
		if (space) {
			if (((ChatCanvas) container).reverse) y += SPACE_HEIGHT;
			h -= SPACE_HEIGHT;
		}

		if (dateRender != null) dateWidth = MP.smallBoldFont.stringWidth(dateRender);
		if (time != null) timeWidth = MP.smallPlainFont.stringWidth(time);
		
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
				if (updateColors) {
					updateColors = false;
					text.color = ChatCanvas.colors[COLOR_MESSAGE_FG];
					text.linkColor = ChatCanvas.colors[COLOR_MESSAGE_FG];
					text.bgColor = ChatCanvas.colors[COLOR_ACTION_BG];
					text.focusColor = ChatCanvas.colors[COLOR_MESSAGE_LINK_FOCUS];
				}
				text.paint(g, x, y + PADDING_HEIGHT, w);
			}
			return;
		}
		
		int cw = contentWidth;
		if (out && w < 900) {
			x += w - cw;
		}
		x += MARGIN_WIDTH + (out && w < 900 ? MARGIN_SIDE : 0);
		y += MARGIN_TOP;
		cw -= MARGIN_WIDTH * 2 + MARGIN_SIDE;
		h -= MARGIN_TOP;
		
		g.setColor(ChatCanvas.colors[out ? COLOR_MESSAGE_OUT_BG : COLOR_MESSAGE_BG]);
		if (ChatCanvas.style[STYLE_MESSAGE_FILL] != 0 && (!sticker || commentsText != null)) {
			g.fillRect(x, y, cw, h);
		}
		if (focus && focusChild == null && subFocusCurrent == -1 && focusDir != 0) {
			g.setColor(ChatCanvas.colors[COLOR_MESSAGE_FOCUS_BORDER]);
			g.drawRect(x, y, cw - 1, h - 1);
		} else if (sticker) {
		} else if (ChatCanvas.style[STYLE_MESSAGE_BORDER] != 0) {
			g.drawRect(x, y, cw - 1, h - 1);
		} else if (ChatCanvas.style[STYLE_MESSAGE_ROUND] != 0) {
			// fake rounding
			g.setColor(ChatCanvas.colors[ChatCanvas.COLOR_CHAT_BG]);
			g.drawLine(x, y, x, y);
			g.drawLine(x + cw - 1, y, x + cw - 1, y);
			if (out) g.drawLine(x, y + h - 1, x, y + h - 1);
			else g.drawLine(x + cw - 1, y + h - 1, x + cw - 1, y + h - 1);
		}
		
		int rw = cw;
		cw -= PADDING_WIDTH * 2;
		int rx = x;
		x += PADDING_WIDTH;
		y += PADDING_HEIGHT;
//		cw -= PADDING;
		
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
				g.drawString(MP.L[LForwardedFrom], x, y, 0);
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
				g.setColor(ChatCanvas.colors[COLOR_MESSAGE_LINK]);
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
					if (!imageQueued && MP.lazyLoading) loadImage();
					g.setColor(ChatCanvas.colors[COLOR_MESSAGE_IMAGE]);
					g.fillRect(x, y + 1, photoRenderWidth, photoRenderHeight);
				} else {
					int clipX = g.getClipX(), clipY = g.getClipY(), clipW = g.getClipWidth(), clipH = g.getClipHeight();
					g.clipRect(x, y + 1, photoRenderWidth, photoRenderHeight);
					g.drawImage(mediaImage, x, y + 1, 0);
					g.setClip(clipX, clipY, clipW, clipH);
				}
				if (focus && subFocusCurrent != -1 && subFocus[subFocusCurrent] == FOCUS_MEDIA) {
					// TODO improve contrast
					g.setColor(ChatCanvas.colors[COLOR_MESSAGE_LINK_FOCUS]);
					g.drawRect(x, y + 1, photoRenderWidth, photoRenderHeight);
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
						int clipX = g.getClipX(), clipY = g.getClipY(), clipW = g.getClipWidth(), clipH = g.getClipHeight();
						g.clipRect(px, y + 1, s - 2, s - 2);
						g.drawImage(mediaImage, px, y + ((s - mediaImage.getHeight()) >> 1), 0);
						g.setClip(clipX, clipY, clipW, clipH);
					} else {
						// TODO thumb placeholder
//						g.setColor(ChatCanvas.colors[COLOR_MESSAGE_IMAGE]);
//						g.fillRect(px, y, s, s);
						if (!imageQueued && MP.lazyLoading) loadImage();
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
			if (updateColors) {
				updateColors = false;
				text.color = ChatCanvas.colors[COLOR_MESSAGE_FG];
				text.linkColor = ChatCanvas.colors[COLOR_MESSAGE_LINK];
				text.focusColor = ChatCanvas.colors[COLOR_MESSAGE_LINK_FOCUS];
			}
			text.paint(g, x, y, cw);
			y += text.contentHeight;
		}

		// reacts
		if (reactsText != null) {
			g.setFont(MP.smallPlainFont);
			g.setColor(ChatCanvas.colors[COLOR_MESSAGE_LINK]);
			g.drawString(reactsText, x, y + MP.smallPlainFontHeight + PADDING_HEIGHT - TIME_PADDING_HEIGHT, Graphics.BOTTOM | Graphics.LEFT);
			y += MP.smallPlainFontHeight;
		}
		
		// buttons
		
		// time
		if (timeBreak) y += MP.smallPlainFontHeight;
		int mw = 0;
		if (showReadMark) {
			// TODO
			mw = READ_WIDTH;
			int ty = y - 10;
			int tx = rx + rw - mw - 2;
			if (read) {
				g.setColor(ChatCanvas.colors[COLOR_MESSAGE_OUT_READ]);
				g.fillTriangle(tx, ty, tx + 9, ty + 10, tx + 19, ty);
				g.setColor(ChatCanvas.colors[ChatCanvas.style[STYLE_MESSAGE_FILL] != 0 ? (out ? COLOR_MESSAGE_OUT_BG : COLOR_MESSAGE_BG) : ChatCanvas.COLOR_CHAT_BG]);
				g.fillTriangle(tx + 2, ty, tx + 9, ty + 10 - 2, tx + 19 - 2, ty);
			}
			tx -= 3;
			g.setColor(ChatCanvas.colors[COLOR_MESSAGE_OUT_READ]);
			g.fillTriangle(tx, ty, tx + 9, ty + 10, tx + 19, ty);
			g.setColor(ChatCanvas.colors[ChatCanvas.style[STYLE_MESSAGE_FILL] != 0 ? (out ? COLOR_MESSAGE_OUT_BG : COLOR_MESSAGE_BG) : ChatCanvas.COLOR_CHAT_BG]);
			g.fillTriangle(tx + 2, ty, tx + 9, ty + 10 - 2, tx + 19 - 2, ty);
			g.fillRect(tx, ty, 9, 6);
		}
		g.setColor(ChatCanvas.colors[sticker ? ChatCanvas.COLOR_CHAT_FG : out ? COLOR_MESSAGE_OUT_TIME : COLOR_MESSAGE_TIME]);
		g.setFont(MP.smallPlainFont);
		g.drawString(time, rx + rw - TIME_PADDING_WIDTH - mw, y + PADDING_HEIGHT - TIME_PADDING_HEIGHT, Graphics.BOTTOM | Graphics.RIGHT);
		if (edited) {
			g.drawString(MP.L[LEdited], rx + rw - timeWidth - MP.smallPlainFontSpaceWidth - TIME_PADDING_WIDTH - mw, y + PADDING_HEIGHT - TIME_PADDING_HEIGHT, Graphics.BOTTOM | Graphics.RIGHT);
		}
		
		y += PADDING_HEIGHT;
		
		// comment
		if (commentsText != null) {
			y += PADDING_HEIGHT;
			g.setColor(ChatCanvas.colors[COLOR_MESSAGE_COMMENT_BORDER]);
			g.drawLine(rx, y, rx + rw, y++);
			
			g.setFont(MP.smallBoldFont);
			g.setColor(ChatCanvas.colors[COLOR_MESSAGE_LINK]);
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
				if (this.next != null && this.next instanceof UIMessage) {
					UIMessage next = (UIMessage) this.next;
					if (dateRender.equals(next.dateRender)) {
						showDate = false;
						break date;
					}
				} else {
//					showDate = false;
//					break date;
				}
				showDate = true;
				h += MARGIN_TOP + DATE_PADDING_HEIGHT * 2 + MP.smallBoldFontHeight;
				if (!reverse) y -= h;
			}
			if (!chat.broadcast) {
				boolean group = false;
				if (this.next != null && this.next instanceof UIMessage) {
					UIMessage next = (UIMessage) this.next;
					if (fromId.equals(next.fromId) && date - next.date < 6 * 60) {
						group = true;
					}
				}
				space = !group;
				if (!reverse) {
					group = false;
					if (this.prev != null && this.prev instanceof UIMessage) {
						UIMessage prev = (UIMessage) this.prev;
						if (fromId.equals(prev.fromId) && prev.date - date < 6 * 60) {
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
		
		int timeWidth = this.timeWidth + TIME_PADDING_WIDTH;
		boolean timeBreak = false;
		if (showReadMark) {
			timeWidth += READ_WIDTH;
		}
		if (edited) {
			timeWidth += MP.smallPlainFontSpaceWidth + MP.smallPlainFont.stringWidth(MP.L[LEdited]);
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
			int tw = 0;
			if (l != 0) {
				int[] pos = (int[]) ((Object[]) text.render.elementAt(l - 1))[3];
				timeBreak = (tw = pos[0] + pos[2] + timeWidth) >= cw;
				maxW = Math.max(maxW, minW + tw);
			}
			maxW = Math.max(maxW, minW + text.contentWidth);
		} else if (!(timeBreak = maxW + timeWidth >= cw)) {
			maxW += timeWidth;
		}
		
		// buttons

		// reacts
		if (reactsText != null) {
			int reactWidth = MP.smallPlainFont.stringWidth(reactsText);
			h += MP.smallPlainFontHeight;
			if (minW + timeWidth + reactWidth >= cw) {
				timeBreak = true;
			} else {
				maxW = Math.max(maxW, minW + timeWidth + reactWidth);
				timeBreak = false;
			}
		}
		
		// time
		if (timeBreak) {
			h += MP.smallPlainFontHeight;
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
		focusDir = dir;
		focus = true;
		if (((ChatCanvas) container).selected != 0) {
			subFocusCurrent = -1;
			return true;
		}
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
	
	int traverse(int dir) {
		if (((ChatCanvas) container).selected != 0) {
			return Integer.MIN_VALUE;
		}
		if (dir == Canvas.LEFT && ((ChatCanvas) container).canWrite) {
			menuAction(LReply);
			return Integer.MAX_VALUE;
		}
		if (focusChild != null) {
			int t = focusChild.traverse(dir);
			if (t != Integer.MIN_VALUE) {
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
						return Integer.MIN_VALUE;
					if (subFocusCurrent == 1 && subFocus[0] == FOCUS_SENDER && hideName)
						return Integer.MIN_VALUE;
					subFocus(--subFocusCurrent);
					if (focusChild != null) {
						if (!focusChild.grabFocus(dir)) focusChild = null;
					}
					return Integer.MAX_VALUE;
				} else if (dir == Canvas.DOWN) {
					if (subFocusCurrent == subFocusLength - 1)
						return Integer.MIN_VALUE;
					subFocus(++subFocusCurrent);
					if (focusChild != null) {
						if (!focusChild.grabFocus(dir)) focusChild = null;
					}
					return Integer.MAX_VALUE;
				}
			}
		}
		return Integer.MIN_VALUE;
	}
	
	boolean action() {
		if (selected) {
			selected = false;
			((ChatCanvas) container).unselected(this);
			return true;
		}
		if (((ChatCanvas) container).selected != 0) {
			selected = true;
			((ChatCanvas) container).selected(this);
			return true;
		}
		if (focusChild != null && focusChild.action()) {
			return true;
		}
		if (subFocusCurrent != -1) {
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
				menuAction(LPlay_Item);
			} else if (photo) {
				menuAction(LViewImage);
			} else if (mediaDownload) {
				menuAction(LDownload);
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
		case LReply:
//			MP.display(MP.writeForm(peerId, idStr, "", null, null, null));
			((ChatCanvas) container).startReply(this);
			break;
		case LEdit:
			((ChatCanvas) container).startEdit(this);
			break;
		case LPin: {
			MP.confirm(MP.RUN_PIN_MESSAGE | 0x100 | 0x200,
					new String[] { peerId, idStr },
					null,
					MP.L[LPinMessage_Alert]);
			break;
		}
		case LCopyMessage:
//			if (text != null) {
//				StringBuffer sb = new StringBuffer();
//				int l = text.parsed.size();
//				for (int i = 0; i < l; i++) {
//					sb.append((String) ((Object[]) text.parsed.elementAt(i))[0]);
//				}
//				MP.copy("", sb.toString());
//			}
			MP.copy("", origText);
			break;
		case LCopyMessageLink:
			MP.copyMessageLink(peerId, idStr);
			break;
		case LForward:
			MP.openLoad(new ChatsList(peerId, idStr));
			break;
		case LDelete: {
			MP.confirm(MP.RUN_DELETE_MESSAGE | 0x100 | 0x200,
					new String[] { peerId, idStr },
					null,
					MP.L[LDeleteMessage_Alert]);
			break;
		}
		case LViewImage:
			if (MP.useView) {
				MP.display(new ViewCanvas(peerId, idStr));
				break;
			}
		case LDownload:
			String name = mediaFileName;
			if (name == null && photo) {
				name = peerId + '_' + id + ".jpg";
			}
			MP.midlet.downloadDocument(peerId, idStr, name, Long.toString(fileSize));
			break;
		case LPlay_Item:
			MP.display(MP.loadingAlert(MP.L[LLoading]), MP.current);
			MP.midlet.start(MP.RUN_LOAD_PLAYLIST, new String[] {peerId, "3", idStr});
			break;
		case LGoTo:
			MPChat parent = ((ChatCanvas) container).parent;
			if (parent != null) {
				MP.goBackTo((Displayable) parent);
				parent.reset();
				parent.openMessage(idStr, -1);
			} else {
				((ChatCanvas) container).reset();
				((ChatCanvas) container).openMessage(idStr, -1);
			}
			break;
		case LSelect:
			if (selected) break;
			selected = true;
			((ChatCanvas) container).selected(this);
			break;
		}
	}
	
	private int[] subMenu(int focus) {
		switch (focus) {
		case FOCUS_MEDIA:
			if (mediaPlayable) {
				return new int[] { LPlay_Item, LDownload };
			} else if (photo) {
				return new int[] { LViewImage, LDownload };
			} else if (mediaDownload) {
				return new int[] { LDownload };
			}
		}
		return null;
	}
	
	int[] menu() {
		if (selected) {
			return null;
		}
		if (((ChatCanvas) container).selected != 0) {
			return null;
		}
		int[] item = focusChild != null ? focusChild.menu() : null;
		
		if (item == null && subFocusCurrent != -1 && subFocusLength != 0) {
			item = subMenu(subFocus[subFocusCurrent]);
		}
		
		int[] general = new int[10];
		int count = 0;
		ChatCanvas chat = (ChatCanvas) container;
		if (chat.query != null || chat.mediaFilter != null) {
			general[count++] = LGoTo;
			general[count++] = LForward;
		} else if (action) {
			if (chat.canWrite) general[count++] = LReply;
			if (chat.channel) general[count++] = LCopyMessageLink;
			if (chat.canDelete) general[count++] = LDelete;
		} else {
			if (chat.canWrite) general[count++] = LReply;
			if (out) general[count++] = LEdit;
			if (chat.canPin) general[count++] = LPin;
			if (origText != null && origText.length() != 0) general[count++] = LCopyMessage;
			if (!chat.selfChat && !chat.user) general[count++] = LCopyMessageLink;
			general[count++] = LForward;
			if (chat.canDelete || out) general[count++] = LDelete;
		}
		general[count++] = LSelect;
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
		if (selected) {
			selected = false;
			((ChatCanvas) container).unselected(this);
			return false;
		}
		if (((ChatCanvas) container).selected != 0) {
			if (!selected) {
				selected = true;
				((ChatCanvas) container).selected(this);
				return false;
			}
			return false;
		}
		int w = ((ChatCanvas) container).width;
		int cw = contentWidth;
		if (out && w < 900) {
			x -= w - cw;
		}
		x -= (out && w < 900 ? MARGIN_SIDE : 0);
		if (text != null && text.focusable && y > text.y && y < text.y + text.contentHeight) {
			focusChild = text;
			if (text.tap(x - PADDING_WIDTH - MARGIN_WIDTH, y - text.y, longTap))
				return true;
		}
		if (x > 0 && x < contentWidth) {
			for (int i = 0; i < touchZones.length && touchZones[i] != Integer.MIN_VALUE; i += 5) {
				if (x >= touchZones[i] && y >= touchZones[i + 1] && x <= touchZones[i + 2] && y <= touchZones[i + 3]) {
					int focus = touchZones[i + 4];
					for (int j = 0; j < subFocusLength; j++) {
						if (subFocus[j] == focus) {
							subFocus(subFocusCurrent = j);
							break;
						}
					}
					int[] menu;
					if (longTap && (menu = subMenu(focus)) != null) {
						((ChatCanvas) container).showMenu(this, menu);
						return true;
					}
					if (!longTap && action(focus)) {
						return true;
					}
					break;
				}
			}
		}
		if (!longTap && (((ChatCanvas) container).query != null || ((ChatCanvas) container).mediaFilter != null)) {
			menuAction(LGoTo);
			return true;
		}
		if (longTap) {
			selected = true;
			((ChatCanvas) container).selected(this);
			return true;
		}
		((ChatCanvas) container).showMenu(this, menu());
		return true;
	}
	
	private void profileAction() {
		if (fromId != null) MP.openProfile(fromId, null, 0);
	}
	
	private void commentAction() {
		MP.openLoad(new ChatCanvas(commentPeer, peerId, Integer.toString(id), commentRead));
	}

	public void edit(JSONObject msg, ChatCanvas chat) {
		edited = true;
		init(msg, chat);
	}
	
	void select() {
		if (selected) return;
		selected = true;
		((ChatCanvas) container).selected(this);
	}
	
	void unselect() {
		if (!selected) return;
		selected = false;
		((ChatCanvas) container).unselected(this);
	}
	
	private void loadImage() {
		if (imageQueued) return;
		imageQueued = true;
		MP.queueImage(this, this);
	}

}
//#endif
