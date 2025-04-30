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
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.ImageItem;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.Spacer;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;
import javax.microedition.lcdui.Ticker;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

public class ChatForm extends MPForm
//#ifndef NO_UPDATES
		implements Runnable
//#endif
{
	
	private static final int SPACER_HEIGHT = 8;
	
	static final int UPDATE_USER_STATUS = 1;
	static final int UPDATE_USER_TYPING = 2;
	static final int UPDATE_NEW_MESSAGE = 3;
	static final int UPDATE_DELETE_MESSAGES = 4;
	static final int UPDATE_EDIT_MESSAGE = 5;

	String id;
	String username;
	String query;
	String startBot;
	String title;
	String mediaFilter;
	
	// params
	int limit = MP.messagesLimit;
	int addOffset, offsetId;
	int messageId, topMsgId;

	boolean infoLoaded;
	boolean left, broadcast, forum;
	boolean canWrite, canDelete, canBan, canPin;
	
	// pagination
	int dir;
	int firstMsgId, lastMsgId;
	boolean endReached, hasOffset;
	private int idOffset;
	
	boolean switched;
	boolean update;
	
	// messages state
	int lastDay;
	boolean space;
	long group;
	Vector loadedMsgs = new Vector();

//#ifndef NO_UPDATES
	long typing;
	private final Object typingLock = new Object();
	private Thread typingThread;
//#endif
	
	long wasOnline;
	
	TextField textField;
	ChatForm parent;

	JSONObject botAnswer;
	
	public ChatForm(String id, String query, int message, int topMsg) {
		super(id);
		addCommand(MP.latestCmd);
		addCommand(MP.chatInfoCmd);
//		addCommand(MP.searchCmd);
		this.id = id;
		this.query = query;
		this.messageId = message;
		this.topMsgId = topMsg;
		
		if (MP.chatField && query == null) {
			setItemStateListener(MP.midlet);
			textField = new TextField("", "", 500, TextField.ANY);
			textField.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			textField.addCommand(MP.sendCmd);
			textField.setItemCommandListener(MP.midlet);
		}
	}
	
	// create in media mode
	public ChatForm(String id, String mediaFilter) {
		super(id);
		this.id = id;
		if (mediaFilter == null) mediaFilter = "Photos";
		this.mediaFilter = mediaFilter;
		addCommand(MP.latestCmd);
	}

	void loadInternal(Thread thread) throws Exception {
		// TODO forum
		deleteAll();
//#ifndef NO_UPDATES
		if (query == null && mediaFilter == null && MP.chatUpdates
				&& (MP.updatesThread != null || MP.updatesRunning)) {
			MP.display(MP.loadingAlert(MP.L[WaitingForPrevChat]), this);
			
			MP.cancel(MP.updatesThreadCopy, true);
			while (MP.updatesThread != null || MP.updatesRunning) {
				Thread.sleep(1000L);
			}
			
			if (MP.current == this) {
				MP.display(MP.useLoadingForm ? MP.loadingForm : this);
			}
		}
//#endif
		
		StringBuffer sb = new StringBuffer();
		if (!infoLoaded) {
			JSONObject peer = MP.getPeer(id, true);
			
			left = peer.getBoolean("l", false);
			broadcast = peer.getBoolean("c", false);
			forum = peer.getBoolean("f", false);
			id = peer.getString("id");
			username = peer.getString("name", null);

			title = MP.getName(id, false);

			if (mediaFilter == null) {
				canWrite = !broadcast;
				JSONObject fullInfo = (JSONObject) MP.api("getFullInfo&id=".concat(id));
				if (id.charAt(0) == '-') {
					JSONObject chat = fullInfo.getObject("Chat");
					if (chat.has("admin_rights")) {
						JSONObject adminRights = chat.getObject("admin_rights");
						canWrite = !broadcast || adminRights.getBoolean("post_messages", false);
						canDelete = adminRights.getBoolean("delete_messages", false);
						canBan = !broadcast && adminRights.getBoolean("ban_users", false);
						canPin = adminRights.getBoolean("pin_messages", false);
					}
				} else {
					canPin = true;
//#ifndef NO_UPDATES
					if (MP.chatStatus && fullInfo.getObject("User").has("status")) {
						setStatus(fullInfo.getObject("User").getObject("status"));
					}
//#endif
				}
				JSONObject full = fullInfo.getObject("full");
				if (messageId == -1 && full.has("read_inbox_max_id")) {
					messageId = 0;
					int maxId = full.getInt("read_inbox_max_id");
					if (maxId != 0 && full.getInt("unread_count", 0) > limit) {
						offsetId = messageId = maxId;
						addOffset = -limit;
						dir = 1;
					}
				}
			
				if (left) {
					addCommand(MP.joinChatCmd);
				} else if (canWrite) {
					addCommand(MP.writeCmd);
//#ifndef NO_STICKERS
					addCommand(MP.sendStickerCmd);
//#endif
				}
			}
			infoLoaded = true;
		}
		setTitle(title);
		
		boolean selfChat = MP.selfId.equals(id);
		boolean reverse = MP.reverseChat && !"Photos".equals(mediaFilter);
		
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
		if (messageId != 0) {
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
		urls = new Hashtable();
		
//		if (query == null && mediaFilter == null && l != 0) {
//			// mark messages as read
//			try {
//				sb.setLength(0);
//				sb.append("readMessages&peer=").append(id)
//				.append("&max=").append(messages.getObject(0).getString("id"));
//				if (topMsgId != 0) {
//					sb.append("&thread=").append(topMsgId);
//				}
//				MP.api(sb.toString());
//			} catch (Exception e) {}
//		}
		
		StringItem s;
		Item focus = null;
		
		int top = size();
		
		if (l == limit && j.has("count")) {
			s = new StringItem(null, MP.L[OlderMessages], Item.BUTTON);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			s.setDefaultCommand(MP.olderMessagesCmd);
			s.setItemCommandListener(MP.midlet);
			safeInsert(thread, reverse ? 0 : size(), s);
			if (reverse) top = size();
		}
		
		if (!endReached && hasOffset) {
			s = new StringItem(null, MP.L[NewerMessages], Item.BUTTON);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			s.setDefaultCommand(MP.newerMessagesCmd);
			s.setItemCommandListener(MP.midlet);
			safeInsert(thread, reverse ? size() : 0, s);
			if (!reverse) top += 1;
		} else if (textField != null && canWrite) {
			safeInsert(thread, reverse ? size() : 0, textField);
			if (!reverse) top += 1;
			if (endReached && dir == 0 && messageId == 0) {
				focus = textField;
			}
		}
		
		int insert = top;
		Item[] item = new Item[1];
		Calendar c = Calendar.getInstance();
		
		for (int i = l - 1; i >= 0 && thread == this.thread; --i) {
			if (!reverse) insert = top;
			JSONObject message = messages.getObject(i);
			
			int id = message.getInt("id");
			if (i == 0) {
				firstMsgId = id;
			} else if (i == l - 1) {
				lastMsgId = id;
			}
			
			insert = message(message, insert, sb, c, reverse, selfChat, false, item);

			if (focus == null && (this.messageId != 0 ? (messageId == id)
					: (i == 0 ? ((endReached && dir == 0) || dir == -1) : (i == l - 1 && dir == 1)))) {
				focus = item[0];
			}
		}
		
		super.focusOnFinish = focus;
	}
	
	protected void postLoad(boolean success) {
		if (!success)
			return;
//#ifndef NO_UPDATES
		if (endReached && !hasOffset && query == null && mediaFilter == null && MP.chatUpdates) {
			// start updater thread
			update = true;
			MP.midlet.start(MP.RUN_CHAT_UPDATES, this);
			(typingThread = new Thread(this)).start();
		}
//#endif
		
		if (botAnswer != null) {
			JSONObject j = botAnswer;
			botAnswer = null;
			
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
	}

	private int message(JSONObject message, int insert, StringBuffer sb, Calendar c, boolean reverse, boolean selfChat, boolean edit, Item[] itemPtr) {
		StringItem s;
		String t;
		Item msgItem = null;
		Item firstItem = null, lastItem = null;
		
		int id = message.getInt("id");
		String idString = Integer.toString(id);
		String fromId = message.has("from_id") ? message.getString("from_id") : this.id;
		boolean out = message.getBoolean("out", false);
		String text = message.getString("text", null);
		// 0: peer id, 1: message id, 2: from id, 3: image quality, 4: file name
		String[] key = new String[] { this.id, idString, fromId, null, null };
		
		loadedMsgs.addElement(idString);

		// date label
		long date = message.getLong("date");
		c.setTime(new Date(date * 1000L));
		int d;
		if (!edit && lastDay != (d = c.get(Calendar.DAY_OF_MONTH) + 100 * c.get(Calendar.MONTH) + 10000 * c.get(Calendar.YEAR))) {
			s = new StringItem(null, MP.localizeDate(date, 0));
			s.setFont(MP.largePlainFont);
			s.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			safeInsert(thread, insert, s);
			if (reverse) insert++;
			lastDay = d;
		}
		
		sb.setLength(0);
		sb.append(out && !broadcast ? MP.L[You] : MP.getName(fromId, true));
		MP.appendTime(sb.append(' '), /*lastDate = */message.getLong("date"));

		// author and time label
		if (!message.has("act")
				&& (group == 0 || group != message.getLong("group", 0) || !reverse)) {
			s = new StringItem(null, sb.toString());
			s.setFont(MP.smallBoldFont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			s.setItemCommandListener(MP.midlet);
			
			if (this.id.charAt(0) == '-') {
				s.addCommand(MP.messageLinkCmd);
			}
			if (text != null && text.length() != 0) {
				s.addCommand(MP.copyMsgCmd);
			}
			if (query != null || mediaFilter != null) {
				s.setDefaultCommand(MP.gotoMsgCmd);
			} else {
				s.addCommand(MP.forwardMsgCmd);
				if (canWrite) {
					s.addCommand(MP.replyMsgCmd);
				}
				if (canPin) {
					s.addCommand(MP.pinMsgCmd);
				}
				if (canBan && !out) {
					s.addCommand(MP.banMemberCmd);
				}
				if (out || selfChat) {
					s.addCommand(MP.deleteMsgCmd);
					s.addCommand(MP.editMsgCmd);
				} else {
					s.setDefaultCommand(MP.itemChatCmd);
					s.addCommand(MP.itemChatInfoCmd);
					if (canDelete) {
						s.addCommand(MP.deleteMsgCmd);
					}
				}
			}
			if (reverse && (space || group != 0)) {
				Spacer sp = new Spacer(10, SPACER_HEIGHT);
				sp.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				safeInsert(thread, insert++, sp);
				firstItem = sp;
			} else {
				firstItem = s;
			}
			safeInsert(thread, insert++, lastItem = s);
			msgItem = s;
			urls.put(s, key);
		}
		group = message.getLong("group", 0);
		space = false;
		
		if (mediaFilter == null) {
			// 'forwarded from ...' label
			if (message.has("fwd")) {
				JSONObject fwd = message.getObject("fwd");
				
				sb.setLength(0);
				if ((t = fwd.getString("from_name", null)) == null) {
					t = MP.getName(fwd.getString("from_id", null), true);
				}
				sb.append(MP.L[ForwardedFrom]).append(t);
				
				s = new StringItem(null, sb.toString());
				s.setFont(MP.smallItalicFont);
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				safeInsert(thread, insert++, lastItem = s);

				if (fwd.has("peer") && fwd.has("msg")) {
					s.setDefaultCommand(MP.gotoMsgCmd);
					s.setItemCommandListener(MP.midlet);
					urls.put(s, new String[] { fwd.getString("peer"), fwd.getString("msg") } );
				}
				space = true;
			}
			
			// reply
			if (message.has("reply")) {
				JSONObject reply = message.getObject("reply");
				if (topMsgId == 0 || reply.getInt("id") != topMsgId) {
					sb.setLength(0);
					if (reply.has("msg")) {
						JSONObject replyMsg = reply.getObject("msg");
						JSONObject replyFwd;
						if ((t = MP.getName(replyMsg.getString("from_id", null), true, true)) == null
								&& replyMsg.has("fwd") && (replyFwd = replyMsg.getObject("fwd")).getBoolean("s", false)) {
							if ((t = replyFwd.getString("from_name", null)) == null) {
								t = MP.getName(replyFwd.getString("from_id", null), true);
							}
						}
						if (t != null) {
							sb.append("Reply to ").append(t);
						}
						
						sb.append("\n> ");
						if (reply.has("quote")) {
							sb.append(reply.getString("quote"));
						} else {
							if ((t = replyMsg.getString("text", null)) != null) {
								MP.appendOneLine(sb, t);
							} else if (replyMsg.has("media")) {
								sb.append(MP.L[Media]);
							}
						}
						
						s = new StringItem(null, sb.toString());
						s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
						s.setFont(MP.smallItalicFont);
						s.setDefaultCommand(MP.gotoMsgCmd);
						s.setItemCommandListener(MP.midlet);
						safeInsert(thread, insert++, lastItem = s);
						
						urls.put(s, new String[] { reply.getString("peer", null), reply.getString("id", null) });
					}
					space = true;
				}
			}
		
			// text
			if (text != null && text.length() != 0) {
//#ifndef NO_RICH_TEXT
				if (MP.parseRichtext && message.has("entities")) {
					insert = MP.wrapRichText(this, thread, text, message.getArray("entities"), insert);
				} else {
					insert = MP.flush(this, thread, text, insert, null);
				}
//#else
//#				s = new StringItem(null, text);
//#				s.setFont(MP.smallPlainFont);
//#				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
//#				safeInsert(thread, insert++, s);
//#endif
				lastItem = get(insert - 1);
				if (firstItem == null) firstItem = lastItem;
				space = true;
			}
		}
		
		// media
		if (message.has("media")) {
			if (!MP.showMedia || message.isNull("media")) {
				// media is disabled
				s = new StringItem(null, MP.L[Media]);
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				safeInsert(thread, insert++, lastItem = s);
			} else {
				JSONObject media = message.getObject("media");
				
				String type = media.getString("type");
				if (type.equals("undefined")) {
					// server doesn't know this media type
					s = new StringItem(null, MP.L[Media]);
					s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					safeInsert(thread, insert++, lastItem = s);
					
					if (msgItem == null) msgItem = s;
					space = true;
				} else if (type.equals("webpage")) {
					// webpage
					sb.setLength(0);
					if ((t = media.getString("name", null)) != null && t.length() != 0) {
						sb.append(t).append('\n');
					}
					if ((t = media.getString("title", null)) != null && t.length() != 0) {
						sb.append(t);
					}
					
					s = new StringItem(null, sb.toString());
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					s.setFont(MP.smallItalicFont);
					s.setDefaultCommand(MP.richTextLinkCmd);
					s.setItemCommandListener(MP.midlet);
					safeInsert(thread, insert++, lastItem = s);
					
					urls.put(s, media.getString("url"));

					if (msgItem == null) msgItem = s;
					space = true;
				} else if (type.equals("document")) {
					// document
					sb.setLength(0);
					
					if ("image/webp".equals(media.getString("mime", null)) && "sticker.webp".equals(media.getString("name", null))) {
						// sticker
						ImageItem img = new ImageItem(sb.toString(), null, 0, "");
						img.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
						img.setItemCommandListener(MP.midlet);
						safeInsert(thread, insert++, lastItem = img);
						
						key[3] = "rsticker";
						urls.put(img, key);
						MP.queueImage(key, img);

						if (msgItem == null) msgItem = img;
					} else {
						boolean nameSet = false;
						key[4] = media.getString("name", null);
						if (media.has("audio")) {
							JSONObject audio = media.getObject("audio");
							if ((t = audio.getString("artist", null)) != null && t.length() != 0) {
								sb.append(t).append(" - ");
							}
							if ((t = audio.getString("title", null)) != null && t.length() != 0) {
								sb.append(t);
								nameSet = true;
							}
						}
						
						if (!nameSet) {
							if ((t = media.getString("name", null)) != null && t.length() != 0) {
								sb.append(t);
							}
						}
						sb.append('\n');
						if (!media.isNull("size")) {
							long size = media.getLong("size");
							if (size >= 1024 * 1024) {
								sb.append(((int) (size / (1048576D) * 100)) / 100D).append(" MB");
							} else {
								sb.append(((int) (size / (1024D) * 100)) / 100D).append(" KB");
							}
						}
						
						if (media.getBoolean("thumb", false)) {
							ImageItem img = new ImageItem(sb.toString(), null, 0, "");
							img.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
							img.setDefaultCommand(MP.documentCmd);
							img.setItemCommandListener(MP.midlet);
							safeInsert(thread, insert++, lastItem = img);
							
							key[3] = "thumbrmin";
							urls.put(img, key);
							MP.queueImage(key, img);
	
							if (msgItem == null) msgItem = img;
						} else {
							s = new StringItem(null, sb.toString());
							s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
							s.setFont(MP.smallItalicFont);
							s.setDefaultCommand(MP.documentCmd);
							s.setItemCommandListener(MP.midlet);
							safeInsert(thread, insert++, lastItem = s);
	
							urls.put(s, key);
							
							if (msgItem == null) msgItem = s;
						}
					}
					space = true;
				} else if (type.equals("photo")) {
					// photo
					ImageItem img = new ImageItem("", null, 0, "");
					img.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_TOP
							| ((text != null && text.length() != 0 || !reverse || mediaFilter != null) ?
									Item.LAYOUT_NEWLINE_BEFORE : 0));
					img.setDefaultCommand(MP.openImageCmd);
					if (MP.useView) {
						img.addCommand(MP.documentCmd);
					}
					img.setItemCommandListener(MP.midlet);
					safeInsert(thread, insert++, lastItem = img);
					
					key[3] = "rprev";
					urls.put(img, key);
					MP.queueImage(key, img);

					if (msgItem == null) msgItem = img;
				} else if (type.equals("poll")) {
					// TODO poll
					sb.setLength(0);
					sb.append(MP.L[Poll]);
					s = new StringItem(null, sb.toString());
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					safeInsert(thread, insert++, lastItem = s);

					if (msgItem == null) msgItem = s;
					space = true;
				} else if (type.equals("geo")) {
					// geo
					sb.setLength(0);
					sb.append(MP.L[Geo])
					.append(media.get("lat")).append(", ").append(media.get("long"));
					s = new StringItem(null, sb.toString());
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					safeInsert(thread, insert++, lastItem = s);

					if (msgItem == null) msgItem = s;
					space = true;
				} else {
					// unknown media type
					System.out.println(media);
					s = new StringItem(null, "Undefined media");
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					safeInsert(thread, insert++, lastItem = s);

					if (msgItem == null) msgItem = s;
					space = true;
				}
			}
		} else if (message.has("act")) {
			// Action
			JSONObject act = message.getObject("act");
			String type = act.getString("_");
			String user = act.getString("user", null);
			
			t = null;
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
					s = new StringItem(null, MP.getName(fromId, false));
					s.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_NEWLINE_BEFORE);
					s.setFont(MP.medPlainFont);

					s.setDefaultCommand(MP.itemChatCmd);
					s.addCommand(MP.itemChatInfoCmd);
					s.addCommand(MP.replyMsgCmd);
					if (mediaFilter == null) {
						if (canWrite) {
							s.addCommand(MP.replyMsgCmd);
						}
					}
					urls.put(s, key);
					safeInsert(thread, insert++, s);
					if (msgItem == null) msgItem = s;
					
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
								s = new StringItem(null, MP.L["ChatAddUser".equals(type) ? Added_Action : Removed_Action]);
								s.setLayout(Item.LAYOUT_CENTER);
								s.setFont(MP.medPlainFont);
								safeInsert(thread, insert++, s);
								
								Spacer sp = new Spacer(MP.medPlainFont.charWidth(' '), MP.medPlainFont.getHeight());
								s.setLayout(Item.LAYOUT_CENTER);
								safeInsert(thread, insert++, sp);

								s = new StringItem(null, MP.getName(user, false));
								s.setLayout(Item.LAYOUT_CENTER);
								s.setFont(MP.medPlainFont);
								safeInsert(thread, insert++, lastItem = s);
								
								break l;
							}
						} else {
							// undefined action
							System.out.println(act);
							
							s = new StringItem(null, MP.L[Action]);
							s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
							s.setFont(MP.medPlainFont);
							safeInsert(thread, insert++, lastItem = s);
							break l;
						}
					}
					
					s = new StringItem(null, t);
					s.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_NEWLINE_AFTER);
					s.setFont(MP.medPlainFont);
					safeInsert(thread, insert++, lastItem = s);
					
					break l;
				}
				
				if (t != null) {
					s = new StringItem(null, t);
					s.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					s.setFont(MP.medPlainFont);
					safeInsert(thread, insert++, lastItem = s);
					
					if (msgItem == null) msgItem = s;
				}
			}
			
			space = true;
		}
		
		if (message.has("markup")) {
			JSONArray markup = message.getArray("markup");
			int rows = markup.size();
			for (int i = 0; i < rows; i++) {
				JSONArray markupRow = markup.getArray(i);
				int cols = markupRow.size();
				for (int j = 0; j < cols; j++) {
					JSONObject markupItem = markupRow.getObject(j);
					
					s = new StringItem(null, markupItem.getString("text"), Item.BUTTON);
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_EXPAND | (j == 0 ? Item.LAYOUT_NEWLINE_BEFORE : 0));
					if (markupItem.has("data")) {
						urls.put(s, new String[] { this.id, idString, markupItem.getString("data") });
						s.setDefaultCommand(MP.botCallbackCmd);
					} else if (markupItem.has("url")) {
						urls.put(s, markupItem.getString("url"));
						s.setDefaultCommand(MP.openLinkCmd);
					}
					s.setItemCommandListener(MP.midlet);
					safeInsert(thread, insert++, lastItem = s);
					
					if (msgItem == null) msgItem = s;
				}
			}
		}
		
		if (!reverse || (group == 0 && space)) {
			Spacer sp = new Spacer(10, SPACER_HEIGHT);
			sp.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			safeInsert(thread, insert++, lastItem = sp);
		}
		if (itemPtr != null) {
			itemPtr[0] = msgItem;
		}
		
		if (firstItem == null) firstItem = msgItem;
		
		urls.put(idString, new Object[] { firstItem, lastItem, text });
		
		return insert;
	}

	public void openMessage(String msg, int topMsg) {
		if (urls != null && urls.containsKey(msg)) {
			Item focus = null;
			for (Enumeration en = urls.keys(); en.hasMoreElements(); ) {
				Object key = en.nextElement();
				if (!(key instanceof StringItem))
					continue;
				Object value = urls.get(key);
				if (!(value instanceof String[])
						|| ((String[]) value).length != 5
						|| (!msg.equals(((String[]) value)[1])))
					continue;
				
				focus = (Item) key;
				break;
			}
			if (focus != null) {
				MP.display.setCurrentItem(focus);
				return;
			}
		}
		cancel();
		this.messageId = Integer.parseInt(msg);
		if (topMsg != -1) this.topMsgId = topMsg;
		MP.openLoad(this);
	}
	
	void paginate(int dir) {
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
		load();
	}
	
	void reset() {
		cancel();
		dir = 0;
		messageId = 0;
		addOffset = 0;
		offsetId = 0;
//#ifndef NO_UPDATES
		typing = 0;
//#endif
		loadedMsgs.removeAllElements();
		switched = false;
	}

//#ifndef NO_UPDATES
	void cancel() {
		// close updater thread
		if (update) {
			update = false;
			if (MP.updatesThread != null || MP.updatesRunning) {
				MP.cancel(MP.updatesThread, true);
			}
		}
		
		super.cancel();
	}
//#endif
	
//	void closed(boolean destroy) {
//		if (destroy) cancel();
//		else {
//			// close updater thread
//			update = false;
//			if (MP.updatesThread != null) {
//				MP.cancel(MP.updatesThread, true);
//			}
//		}
//	}
	
	void shown() {
		if (!finished || urls == null) return;
		for (Enumeration en = urls.keys(); en.hasMoreElements(); ) {
			Object key = en.nextElement();
			if (!(key instanceof ImageItem)
					|| ((ImageItem) key).getImage() != null)
				continue;
			MP.queueImage(key, urls.get(key));
		}
	}
	
//#ifndef NO_UPDATES
	void handleUpdate(int type, JSONObject update) {
		if (!this.update) return;
//		System.out.println("update: " + type);
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
			setTitle("(...) ".concat(title));
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
			
			typing = 0;
			typingThread.interrupt();
			
			if (topMsgId != 0) break;
			
			// delete old messages
			while (loadedMsgs.size() >= limit) {
				deleteMessage((String) loadedMsgs.elementAt(0));
			}
			boolean reverse = MP.reverseChat;
			Item[] item = new Item[1];
			int o = (textField != null ? 1 : 0);
			message(update.getObject("message"),
					reverse ? size() - o : o,
					new StringBuffer(),
					Calendar.getInstance(),
					reverse,
					MP.selfId.equals(this.id),
					false,
					item);
			firstMsgId = update.getObject("message").getInt("id");
			if (item[0] != null && MP.focusNewMessages && MP.current == this) {
				MP.display.setCurrentItem(item[0]);
			}
			break;
		}
		case UPDATE_DELETE_MESSAGES: {
			JSONArray messages = update.getArray("messages");
			int l = messages.size();
			
			for (int i = 0; i < l; ++i) {
				deleteMessage(messages.getString(i));
			}
			break;
		}
		case UPDATE_EDIT_MESSAGE: {
			typing = 0;
			typingThread.interrupt();
			
			JSONObject msg = update.getObject("message");
			String id = msg.getString("id");
			if (!loadedMsgs.contains(id)) break;
			
			int idx = deleteMessage(id);
			if (idx == -1) break;
			
			boolean reverse = MP.reverseChat;
			Item[] item = new Item[1];
			message(update.getObject("message"),
					idx,
					new StringBuffer(),
					Calendar.getInstance(),
					reverse,
					MP.selfId.equals(this.id),
					true,
					item);
		}
		}
	}
	
	int deleteMessage(String id) {
		Object[] p = (Object[]) urls.get(id);
		if (p == null) return -1;
		
		Item item = (Item) p[0];
		int size = size();
		int idx;
		for (idx = 0; idx < size && get(idx) != item; ++idx);
		if (idx == size) return -1;
		
		loadedMsgs.removeElement(id);
		do {
			item = get(idx);
			delete(idx);
		} while (item != p[1]);
		return idx;
	}
	
	private void setStatus(JSONObject status) {
		if (!title.equals(getTitle())) {
			setTitle(title);
		}
		String s;
		if (status == null) {
			setTicker(null);
			if (MP.chatStatus) {
				if (wasOnline == 1) {
					s = MP.L[Online];
				} else if (wasOnline == 2) {
					s = MP.L[Offline];
				} else if (wasOnline != 0) {
					s = /*MP.L[LastSeen] + */MP.localizeDate(wasOnline, 4);
				} else {
					s = null;
				}
				Ticker t = getTicker();
				if (t != null && t.getString().equals(s))
					return;
				setTicker(s == null ? null : new Ticker(s));
			}
			return;
		}
		if ("userStatusOnline".equals(status.getString("_"))) {
			wasOnline = 1;
			s = MP.L[Online];
		} else if ((wasOnline = status.getInt("was_online", 0)) != 0) {
			s = /*MP.L[LastSeen] + */MP.localizeDate(wasOnline, 4);
		} else {
			s = MP.L[Offline];
			wasOnline = 2;
		}
		setTicker(new Ticker(s));
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
//#endif

}
