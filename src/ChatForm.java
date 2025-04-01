import java.util.Enumeration;
import java.util.Hashtable;

import javax.microedition.lcdui.ImageItem;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.Spacer;
import javax.microedition.lcdui.StringItem;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

public class ChatForm extends MPForm {

	String id;
	String username;
	String query;
	String startBot;
	
	int limit = 20;
	int addOffset = 0;
	int offsetId = 0;
	
	boolean left, broadcast, forum;
	boolean canWrite, canDelete;
	int messageId, topMsgId;
	
	boolean info;
	
	int dir = 0;
	int firstMsgId, lastMsgId;
	boolean endReached, hasOffset;
	private int idOffset;
	
	public ChatForm(String id, String query, int message, int topMsg) {
		super(id);
		addCommand(MP.refreshCmd);
		addCommand(MP.chatInfoCmd);
//		addCommand(MP.searchCmd);
		this.id = id;
		this.query = query;
		this.messageId = message;
		this.topMsgId = topMsg;
	}

	void loadInternal(Thread thread) throws Exception {
		// TODO forum
		deleteAll();
		
		StringBuffer sb = new StringBuffer();
		if (!info) {
			JSONObject peer = MP.getPeer(id, true);
			
			left = peer.getBoolean("l", false);
			broadcast = peer.getBoolean("c", false);
			forum = peer.getBoolean("f", false);
			id = peer.getString("id");
			username = peer.getString("name", null);

			setTitle(MP.getName(id, false));

			canWrite = !broadcast;
			if (id.charAt(0) == '-') {
				JSONObject fullInfo = (JSONObject) MP.api("getFullInfo&id=".concat(id));
				JSONObject chat = fullInfo.getObject("Chat");
				
				if (chat.has("admin_rights")) {
					JSONObject adminRights = chat.getObject("admin_rights");
					canWrite = !broadcast || adminRights.getBoolean("post_messages", false);
					canDelete = adminRights.getBoolean("delete_messages", false);
				}
			}
			
			if (canWrite) {
				addCommand(MP.writeCmd);
			}
			info = true;
		}
		
		if (startBot != null) {
			// TODO
			try {
				sb.append("startBot&id=").append(id);
				MP.appendUrl(sb.append("&start="), startBot);
				
				MP.api(sb.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
			sb.setLength(0);
		}

		if (query != null || topMsgId != 0) {
			sb.append("searchMessages");
			if (query != null) MP.appendUrl(sb.append("q="), query);
		} else {
			sb.append("getHistory");
		}
		
		if (messageId != 0) {
			// message to focus
			offsetId = messageId;
			addOffset = -1;
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
		
		StringItem s;
		String t;
		Item focus = null;
		
		int top = size();
		
		if (l == limit) {
			s = new StringItem(null, "Older messages", Item.BUTTON);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			s.setDefaultCommand(MP.olderMessagesCmd);
			s.setItemCommandListener(MP.midlet);
			safeInsert(thread, MP.reverseChat ? 0 : size(), s);
			if (MP.reverseChat) top = size();
		}
		
		if (!endReached && hasOffset) {
			s = new StringItem(null, "Newer messages", Item.BUTTON);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			s.setDefaultCommand(MP.newerMessagesCmd);
			s.setItemCommandListener(MP.midlet);
			safeInsert(thread, MP.reverseChat ? size() : 0, s);
			if (!MP.reverseChat) top += 1;
		}
		
		int insert = top;
		long lastDate = 0;
		long group = 0;
		
		for (int i = l - 1; i >= 0 && thread == this.thread; --i) {
			if (!MP.reverseChat) insert = top;
			Item msgItem = null;
			JSONObject message = messages.getObject(i);
			
			int id = message.getInt("id");
			if (i == 0) {
				firstMsgId = id;
			} else if (i == l - 1) {
				lastMsgId = id;
			}
			String idString = Integer.toString(id);
			String fromId = message.has("from_id") ? message.getString("from_id") : this.id;
			boolean out = message.getBoolean("out", false);
			String text = message.getString("text", null);
			String[] key = new String[] { this.id, idString, fromId, null };

			// TODO date label
//			if (lastDate == 0) {
//				s = new StringItem(null, "April 1");
//				s.setFont(MP.medPlainFont);
//				s.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
//				safeInsert(thread, insert, s);
//				if (MP.reverseChat) insert++;
//			}
			
			sb.setLength(0);
			sb.append(out && !broadcast ? "You" : MP.getName(fromId, true));
			MP.appendTime(sb.append(' '), lastDate = message.getLong("date"));
			
			s = new StringItem(null, sb.toString());
			s.setFont(MP.smallBoldFont);
			if (!out) {
				s.addCommand(MP.itemChatInfoCmd);
			}
			if (canWrite) {
				s.addCommand(MP.replyMsgCmd);
			}
			s.addCommand(MP.forwardMsgCmd);
			s.addCommand(MP.messageLinkCmd);
			if (text != null && text.length() != 0) {
				s.addCommand(MP.copyMsgCmd);
			}
			// TODO
//			if (canDelete) {
//				s.addCommand(MP.deleteMsgCmd);
//			}
			
			s.setDefaultCommand(MP.itemChatCmd);
			s.setItemCommandListener(MP.midlet);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			if (group == 0 || group != message.getLong("group", 0)) {
				if (group != 0) {
					Spacer sp = new Spacer(10, 8);
					sp.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					safeInsert(thread, insert++, sp);
				}
				safeInsert(thread, insert++, s);
				urls.put(s, key);
				msgItem = s;
			}
			group = message.getLong("group", 0);
			
			if (message.has("fwd")) {
				// TODO
				JSONObject fwd = message.getObject("fwd");
				sb.setLength(0);
				sb.append("Forwarded from ").append(MP.getName(fwd.getNullableString("from_id"), true));
				s = new StringItem(null, sb.toString());
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				safeInsert(thread, insert++, s);
				urls.put(s, new String[] { fwd.getNullableString("peer"), fwd.getNullableString("msg") } );
			}
			
			if (message.has("reply")) {
				// TODO
//				JSONObject reply = message.getObject("reply");
//				if (reply.has("msg")) {
//					JSONObject replyMsg = reply.getObject("msg");
//					sb.setLength(0);
//					sb.append("Reply to ").append(MP.getName(replyMsg.getString("from_id"), true));
//					s = new StringItem(null, sb.toString());
//					s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
//					s.setFont(MP.smallItalicFont);
//					safeInsert(thread, insert++, s);
//					if (reply.has("peer") && !reply.isNull("peer")) {
//						urls.put(s, reply.getString("peer"));
//					}
//				}
			}
			
			// text
			if (text != null && text.length() != 0) {
				urls.put(idString, text);
				if (message.has("entities")) {
					insert = MP.wrapRichText(this, thread, text, message.getArray("entities"), insert);
				} else {
					insert = MP.flush(this, thread, text, insert, null);
				}
			}
			
			// media
			if (message.has("media")) {
				if (!MP.showMedia || message.isNull("media")) {
					s = new StringItem(null, "Media");
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					safeInsert(thread, insert++, s);
				} else {
					JSONObject media = message.getObject("media");
					
					String type = media.getString("type");
					if (type.equals("undefined")) {
						s = new StringItem(null, "Media");
						s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
						safeInsert(thread, insert++, s);
					} else if (type.equals("poll")) {
						// TODO
					} else if (type.equals("geo")) {
						// TODO
					} else if (type.equals("webpage")) {
						sb.setLength(0);
						if ((t = media.getString("name", null)) != null && t.length() != 0) {
							sb.append(t).append('\n');
						}
						if ((t = media.getString("title", null)) != null && t.length() != 0) {
							sb.append(t);
						}
						
						s = new StringItem(null, sb.toString());
						s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
						s.setFont(MP.smallBoldFont);
						s.setDefaultCommand(MP.richTextLinkCmd);
						s.setItemCommandListener(MP.midlet);
						safeInsert(thread, insert++, s);
						
						urls.put(s, media.getString("url"));
						
						if (msgItem == null) {
							msgItem = s;
						}
					} else if (type.equals("document")) {
						sb.setLength(0);
						if (!media.has("title") && (t = media.getString("name", null)) != null && t.length() != 0) {
							sb.append(t);
						}
						if ((t = media.getString("artist", null)) != null && t.length() != 0) {
							sb.append(t).append(" - ");
						}
						if ((t = media.getString("title", null)) != null && t.length() != 0) {
							sb.append(t);
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
							
							if (msgItem == null) {
								msgItem = img;
							}
						} else {
							s = new StringItem(null, sb.toString());
							s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
							s.setFont(MP.smallBoldFont);
							s.setDefaultCommand(MP.documentCmd);
							s.setItemCommandListener(MP.midlet);
							safeInsert(thread, insert++, s);

							urls.put(s, key);
							
							if (msgItem == null) {
								msgItem = s;
							}
						}
					} else if (type.equals("photo")) {
						ImageItem img = new ImageItem("", null, 0, "");
						if (text != null && text.length() != 0) {
							img.setLayout(Item.LAYOUT_NEWLINE_BEFORE);
						}
						img.setDefaultCommand(MP.openImageCmd);
						img.setItemCommandListener(MP.midlet);
						safeInsert(thread, insert++, img);
						
						key[3] = "rprev";
						urls.put(img, key);
						MP.queueImage(key, img);
						
						if (msgItem == null) {
							msgItem = img;
						}
					}
				}
			} else if (message.has("act")) {
				// TODO action
				s = new StringItem(null, "Action: " + message.getObject("act").getString("_"));
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				safeInsert(thread, insert++, s);
			}
			if (this.messageId != 0 ? (messageId == id)
					: (i == 0 ? ((endReached && dir == 0) || dir == -1) : (i == l - 1 ? (dir == 1) : false))) {
				focus = msgItem;
			}
			if (group != 0) {
				safeInsert(thread, insert++, new Spacer(10, 12));
			}
			
//			if (MP.reverseChat ? (i-- == 0) : (++i == l)) break;
		}
		
		if (focus != null) {
			MP.display(this);
			MP.display.setCurrentItem(focus);
		}
	}

	public void openMessage(int msg, int topMsg) {
		cancel();
		this.messageId = msg;
		this.topMsgId = topMsg;
		load();
	}
	
	void paginate(int dir) {
		this.dir = dir;
		cancel();
		messageId = 0;
		if (dir == 1) {
			if ((idOffset != Integer.MIN_VALUE && idOffset <= limit) || addOffset == limit) {
				addOffset = 0;
				offsetId = 0;
			} else {
				addOffset = -limit-1;
				offsetId = firstMsgId;
			}
		} else {
//			if (endReached) {
//				addOffset = limit;
//				offsetId = 0;
//			} else {
			addOffset = 0;
			offsetId = lastMsgId;
//			}
		}
		load();
	}
	
	void reset() {
		cancel();
		dir = 0;
		messageId = 0;
		addOffset = 0;
		offsetId = 0;
	}
	
	void shown() {
		if (!loaded || urls == null) return;
		for (Enumeration en = urls.keys(); en.hasMoreElements(); ) {
			Object key = en.nextElement();
			if (key instanceof ImageItem
					|| ((ImageItem) key).getImage() != null)
				continue;
			MP.queueImage(key, urls.get(key));
		}
	}

}
