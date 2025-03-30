import java.util.Hashtable;

import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.Spacer;
import javax.microedition.lcdui.StringItem;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

public class ChatForm extends MPForm {

	String id;
	int limit = 20;
	
	public ChatForm(String id) {
		super(id);
		addCommand(MP.refreshCmd);
		addCommand(MP.writeCmd);
		addCommand(MP.chatInfoCmd);
		this.id = id;
	}

	void loadInternal(Thread thread) throws Exception {
		deleteAll();
		
		StringBuffer sb = new StringBuffer("getHistory&media=1&peer=");
		sb.append(id);
		if (limit != 0) {
			sb.append("&limit=").append(limit);
		}
		
		JSONObject j = (JSONObject) MP.api(sb.toString());
		
		if (thread != this.thread) throw MP.cancelException;
		
		urls = new Hashtable();
		
		MP.fillPeersCache(j.getNullableObject("users"), j.getNullableObject("chats"));
		
		setTitle(MP.getName(id, false));
		
		JSONArray messages = j.getArray("messages");
		int l = messages.size();
		
		StringItem s;
		StringItem last = null;
		int i = MP.reverseChat ? (l - 1) : 0;
		while (thread == this.thread) {
			JSONObject message = messages.getObject(i);
			
			String id = message.getString("id");
			String fromId = message.has("from_id") ? message.getString("from_id") : this.id;
			boolean out = message.getBoolean("out", false);
			String text = message.getString("text", null);
			
			sb.setLength(0);
			sb.append(out ? "You" : MP.getName(fromId, true));
			MP.appendTime(sb.append(' '), message.getLong("date"));
			
			s = new StringItem(null, sb.toString());
			s.setFont(MP.smallBoldFont);
			if (!out) {
				s.addCommand(MP.itemChatInfoCmd);
			}
			s.addCommand(MP.replyMsgCmd);
			s.addCommand(MP.forwardMsgCmd);
			if (text != null && text.length() != 0) {
				s.addCommand(MP.copyMsgCmd);
			}
			
			s.setDefaultCommand(MP.itemChatCmd);
			s.setItemCommandListener(MP.midlet);
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			safeAppend(thread, s);
			urls.put(s, new String[] { fromId, id } );
			
			if (i == 0) {
				last = s;
			}
			
			if (message.has("fwd")) {
				// TODO
				JSONObject fwd = message.getObject("fwd");
				sb.setLength(0);
				sb.append("Forwarded from ").append(MP.getName(fwd.getNullableString("from_id"), true));
				s = new StringItem(null, sb.toString());
				s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				safeAppend(thread, s);
				urls.put(s, new String[] { fwd.getNullableString("peer"), fwd.getNullableString("msg") } );
			}
			
			if (message.has("reply")) {
				// TODO
				JSONObject reply = message.getObject("reply");
				if (reply.has("from_id")) {
					sb.setLength(0);
					sb.append("Reply to ").append(MP.getName(reply.getString("from_id"), true));
					s = new StringItem(null, sb.toString());
					s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					safeAppend(thread, s);
					if (reply.has("peer") && !reply.isNull("peer")) {
						urls.put(s, reply.getString("peer"));
					}
				}
			}
			
			if (text != null && text.length() != 0) {
				urls.put(id, text);
				if (message.has("entities")) {
					MP.wrapRichText(this, thread, text, message.getArray("entities"), size());
				} else {
					s = new StringItem(null, text);
					s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					safeAppend(thread, s);
				}
			}
			if (message.has("media")) {
				s = new StringItem(null, "Media");
				s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				safeAppend(thread, s);
			} else if (message.has("act")) {
				s = new StringItem(null, "Action: " + message.getObject("act").getString("_"));
				s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				safeAppend(thread, s);
			}
			safeAppend(thread, new Spacer(10, 6));
			
			if (MP.reverseChat ? (i-- == 0) : (++i == l)) break;
		}
		
		if (last != null) {
			MP.display.setCurrentItem(last);
		}
	}

}
