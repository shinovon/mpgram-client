import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.StringItem;

import cc.nnproject.json.JSONObject;

public class ChatInfoForm extends MPForm {

	String id;
	String phone;
	ChatForm chatForm;
	
	public ChatInfoForm(String id, ChatForm chatForm) {
		super(id);
		this.id = id;
		this.chatForm = chatForm;
	}

	void loadInternal(Thread thread) throws Exception {
		JSONObject peer = MP.getPeer(id, true);
		
		id = peer.getString("id");
		boolean isUser = id.charAt(0) != '-';
		String name = MP.getName(peer);
		setTitle(name);
		
		StringItem s;
		
		s = new StringItem(null, name);
		s.setFont(MP.medPlainFont);
		s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
		append(s);
		
		JSONObject fullInfo = (JSONObject) MP.api("getFullInfo&id=".concat(id));
		JSONObject full = fullInfo.getObject("full");
		
		if (isUser) {
			JSONObject user = fullInfo.getObject("User");
			if (user.has("phone")) {
				phone = "+".concat(user.getString("phone"));
				addCommand(MP.callCmd);
				
				s = new StringItem("Phone number", phone);
				s.setFont(MP.medPlainFont);
				s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				s.addCommand(MP.callItemCmd);
				s.setItemCommandListener(MP.midlet);
				append(s);
			}
			
			if (full.has("about")) {
				s = new StringItem("About", full.getString("about"));
				s.setFont(MP.medPlainFont);
				s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				s.setItemCommandListener(MP.midlet);
				append(s);
			}
			
			if (user.has("username")) {
				s = new StringItem("Username", "@".concat(user.getString("username")));
				s.setFont(MP.medPlainFont);
				s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				s.setItemCommandListener(MP.midlet);
				append(s);
			}
		} else {
			JSONObject chat = fullInfo.getObject("Chat");
			
			if (full.has("participants_count")) {
				s = new StringItem(null, full.getString("participants_count").concat(" members"));
				s.setFont(MP.medPlainFont);
				s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				s.setItemCommandListener(MP.midlet);
				append(s);
			}
			
			if (full.has("about")) {
				s = new StringItem("About", full.getString("about"));
				s.setFont(MP.medPlainFont);
				s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				s.setItemCommandListener(MP.midlet);
				append(s);
			}
			
			if (chat.has("username")) {
				s = new StringItem("Link", "t.me/".concat(chat.getString("username")));
				s.setFont(MP.medPlainFont);
				s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				s.setItemCommandListener(MP.midlet);
				append(s);
			}
			
			// TODO leave
		}
	}

}
