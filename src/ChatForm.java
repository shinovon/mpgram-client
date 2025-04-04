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

import javax.microedition.lcdui.ImageItem;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.Spacer;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.Ticker;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

public class ChatForm extends MPForm implements LangConstants {
	
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
	boolean canWrite, canDelete;
	
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

	long typing;
	
	public ChatForm(String id, String query, int message, int topMsg) {
		super(id);
		addCommand(MP.latestCmd);
		addCommand(MP.chatInfoCmd);
//		addCommand(MP.searchCmd);
		this.id = id;
		this.query = query;
		this.messageId = message;
		this.topMsgId = topMsg;
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
					}
				} else if (MP.chatStatus) {
					setStatus(fullInfo.getObject("User").getObject("status"));
				}
				JSONObject full = fullInfo.getObject("full");
				if (messageId == -1 && full.has("read_inbox_max_id")) {
					messageId = 0;
					int maxId = full.getInt("read_inbox_max_id");
					if (maxId != 0 && full.getInt("unread_count", 0) > limit) {
						offsetId = maxId;
						addOffset = -limit;
						dir = 1;
					}
				}
			
				if (left) {
					addCommand(MP.joinChatCmd);
				} else if (canWrite) {
					addCommand(MP.writeCmd);
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
			sb.append("getHistory");
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
		
		if (query == null && mediaFilter == null && l != 0) {
			// mark messages as read
			try {
				sb.setLength(0);
				sb.append("readMessages&peer=").append(id)
				.append("&max=").append(messages.getObject(0).getString("id"));
				if (topMsgId != 0) {
					sb.append("&thread=").append(topMsgId);
				}
				MP.api(sb.toString());
			} catch (Exception e) {}
		}
		
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
			
			insert = message(message, insert, sb, c, reverse, selfChat, item);

			if (this.messageId != 0 ? (messageId == id)
					: (i == 0 ? ((endReached && dir == 0) || dir == -1) : (i == l - 1 ? (dir == 1) : false))) {
				focus = item[0];
			}
		}
		
		super.focusOnFinish = focus;
		
		if (endReached && MP.chatUpdates && mediaFilter == null) {
			if (MP.updatesThread != null) {
				try {
					MP.updatesConnection.close();
				} catch (Exception e) {}
				MP.updatesThread.interrupt();
			}
			update = true;
			MP.midlet.start(MP.RUN_CHAT_UPDATES, this);
		}
	}

	private int message(JSONObject message, int insert, StringBuffer sb, Calendar c, boolean reverse, boolean selfChat, Item[] itemPtr) {
		StringItem s;
		String t;
		Item msgItem = null;
		Item firstItem = null, lastItem = null;
		
		int id = message.getInt("id");
		String idString = Integer.toString(id);
		String fromId = message.has("from_id") ? message.getString("from_id") : this.id;
		boolean out = message.getBoolean("out", false);
		String text = message.getString("text", null);
		String[] key = new String[] { this.id, idString, fromId, null };
		
		loadedMsgs.addElement(idString);

		// date label
		long date = message.getLong("date");
		c.setTime(new Date(date * 1000L));
		int d;
		if (lastDay != (d = c.get(Calendar.DAY_OF_MONTH) + 100 * c.get(Calendar.MONTH) + 10000 * c.get(Calendar.YEAR))) {
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
		
		s = new StringItem(null, sb.toString());
		s.setFont(MP.smallBoldFont);
//		s.addCommand(MP.forwardMsgCmd);
		if (this.id.charAt(0) == '-') {
			s.addCommand(MP.messageLinkCmd);
		}
		if (text != null && text.length() != 0) {
			s.addCommand(MP.copyMsgCmd);
		}
		if (mediaFilter == null) {
			if (canWrite) {
				s.addCommand(MP.replyMsgCmd);
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
		
		s.setItemCommandListener(MP.midlet);
		s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
		// do not add name if message is grouped
		if (group == 0 || group != message.getLong("group", 0) || !reverse) {
			if (reverse && (space || group != 0)) {
				Spacer sp = new Spacer(10, SPACER_HEIGHT);
				sp.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				safeInsert(thread, insert++, sp);
				firstItem = sp;
			} else {
				firstItem = s;
			}
			safeInsert(thread, insert++, s);
			msgItem = s;
			lastItem = s;
			urls.put(s, key);
		}
		group = message.getLong("group", 0);
		space = false;
		
		if (mediaFilter == null) {
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
				safeInsert(thread, insert++, s);

				if (fwd.has("peer") && fwd.has("msg")) {
					s.setDefaultCommand(MP.gotoMsgCmd);
					s.setItemCommandListener(MP.midlet);
					urls.put(s, new String[] { fwd.getString("peer"), fwd.getString("msg") } );
				}
				space = true;
			}
			
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
						safeInsert(thread, insert++, s);
						
						urls.put(s, new String[] { reply.getString("peer", null), reply.getString("id", null) });
					}
					space = true;
				}
			}
		
			// text
			if (text != null && text.length() != 0) {
				if (MP.parseRichtext && message.has("entities")) {
					insert = MP.wrapRichText(this, thread, text, message.getArray("entities"), insert);
				} else {
					insert = MP.flush(this, thread, text, insert, null);
				}
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
				safeInsert(thread, insert++, s);
			} else {
				JSONObject media = message.getObject("media");
				
				String type = media.getString("type");
				if (type.equals("undefined")) {
					// server doesn't know this media type
					s = new StringItem(null, MP.L[Media]);
					s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					safeInsert(thread, insert++, s);
					
					if (msgItem == null) msgItem = s;
					lastItem = s;
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
					safeInsert(thread, insert++, s);
					
					urls.put(s, media.getString("url"));

					if (msgItem == null) msgItem = s;
					lastItem = s;
					space = true;
				} else if (type.equals("document")) {
					// document
					// TODO sticker
					sb.setLength(0);
					boolean nameSet = false;
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
						safeInsert(thread, insert++, img);
						
						key[3] = "thumbrmin";
						urls.put(img, key);
						MP.queueImage(key, img);

						if (msgItem == null) msgItem = img;
						lastItem = img;
					} else {
						s = new StringItem(null, sb.toString());
						s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
						s.setFont(MP.smallItalicFont);
						s.setDefaultCommand(MP.documentCmd);
						s.setItemCommandListener(MP.midlet);
						safeInsert(thread, insert++, s);

						urls.put(s, key);
						
						if (msgItem == null) msgItem = s;
						lastItem = s;
					}
					space = true;
				} else if (type.equals("photo")) {
					// photo
					ImageItem img = new ImageItem("", null, 0, "");
					img.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_TOP
							| ((text != null && text.length() != 0 || !reverse || mediaFilter != null) ?
									Item.LAYOUT_NEWLINE_BEFORE : 0));
					img.setDefaultCommand(MP.openImageCmd);
					img.setItemCommandListener(MP.midlet);
					safeInsert(thread, insert++, img);
					
					key[3] = "rprev";
					urls.put(img, key);
					MP.queueImage(key, img);

					if (msgItem == null) msgItem = img;
					lastItem = img;
				} else if (type.equals("poll")) {
					// TODO poll
					sb.setLength(0);
					sb.append(MP.L[Poll]);
					s = new StringItem(null, sb.toString());
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					safeInsert(thread, insert++, s);

					if (msgItem == null) msgItem = s;
					lastItem = s;
					space = true;
				} else if (type.equals("geo")) {
					// geo
					sb.setLength(0);
					sb.append(MP.L[Geo])
					.append(media.get("lat")).append(", ").append(media.get("long"));
					s = new StringItem(null, sb.toString());
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					safeInsert(thread, insert++, s);

					if (msgItem == null) msgItem = s;
					lastItem = s;
					space = true;
				} else {
					// unknown media type
					System.out.println(media);
					s = new StringItem(null, "Undefined media");
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					safeInsert(thread, insert++, s);

					if (msgItem == null) msgItem = s;
					lastItem = s;
					space = true;
				}
			}
		} else if (message.has("act")) {
			// TODO action
			s = new StringItem(null, MP.L[Action] + ": " + message.getObject("act").getString("_"));
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			safeInsert(thread, insert++, s);
			lastItem = s;
			space = true;
		}
		if (!reverse || (group == 0 && space)) {
			Spacer sp = new Spacer(10, SPACER_HEIGHT);
			sp.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			safeInsert(thread, insert++, sp);
			lastItem = sp;
		}
		if (itemPtr != null) {
			itemPtr[0] = msgItem;
		}
		
		if (firstItem == null) firstItem = msgItem;
		
		urls.put(idString, new Object[] { firstItem, lastItem, text });
		
		return insert;
	}

	public void openMessage(String msg, int topMsg) {
		if (urls != null && urls.contains(msg)) { // TODO doesn't work
			Item focus = null;
			for (Enumeration en = urls.keys(); en.hasMoreElements(); ) {
				Object key = en.nextElement();
				if (!(key instanceof StringItem))
					continue;
				Object value = urls.get(key);
				if (!(value instanceof String[])
						|| ((String[]) value).length != 4
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
		load();
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
		loadedMsgs.removeAllElements();
		switched = false;
	}
	
	void cancel() {
		super.cancel();
		// close updater loop
		update = false;
		if (MP.updatesThread != null) {
			try {
				MP.updatesConnection.close();
			} catch (Exception e) {}
			MP.updatesThread.interrupt();
		}
	}
	
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
	
	void handleUpdate(int type, JSONObject update) {
		if (!this.update) return;
		System.out.println("update: " + type + " " + update);
		switch (type) {
		case UPDATE_USER_STATUS: {
			if (MP.chatStatus) {
				typing = 0;
				setStatus(update.getObject("status"));
			}
			break;
		}
		case UPDATE_USER_TYPING: {
			// TODO
			if ("sendMessageCancelAction".equals(update.getObject("action").getString("_"))) {
				setTicker(null);
				typing = 0;
				break;
			}
			if (id.charAt(0) != '-') {
				setTicker(new Ticker(title + " is typing.."));
			} else {
				if (update.has("top_msg_id") && topMsgId != update.getInt("top_msg_id")) {
					break;
				}
				setTicker(new Ticker("Someone is typing.."));
			}
			typing = System.currentTimeMillis();
			break;
		}
		case UPDATE_NEW_MESSAGE: {
			setTicker(null);
			typing = 0;
			
			if (topMsgId != 0) break;
			
			// delete old messages
			while (loadedMsgs.size() >= limit) {
				deleteMessage((String) loadedMsgs.elementAt(0));
			}
			boolean reverse = MP.reverseChat;
			Item[] item = new Item[1];
			message(update.getObject("message"),
					reverse ? size() : 0,
					new StringBuffer(),
					Calendar.getInstance(),
					reverse,
					MP.selfId.equals(this.id),
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
					item);
		}
		}
		if (typing != 0 && System.currentTimeMillis() - typing >= 6000L) {
			setTicker(null);
			typing = 0;
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
		String s;
		if ("userStatusOnline".equals(status.getString("_"))) {
			s = MP.L[Online];
		} else if(status.has("was_online")) {
			s = MP.L[LastSeen] + MP.localizeDate(status.getInt("was_online"), 3);
		} else {
			s = MP.L[Offline];
		}
		setTicker(new Ticker(s));
	}

}
