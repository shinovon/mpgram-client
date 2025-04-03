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
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.StringItem;

import cc.nnproject.json.JSONObject;

public class ChatInfoForm extends MPForm {

	String id;
	String phone;
	String invite;
	ChatForm chatForm;
	int mode; // 0 - chat info or profile by id, 1 - phone, 2 - invite peek, 3 - invite
	int pinnedMessageId;
	
	public ChatInfoForm(String id, ChatForm chatForm, int mode) {
		super(id);
		this.id = id;
		this.chatForm = chatForm;
		this.mode = mode;
		if (chatForm == null && mode != 3) {
			addCommand(MP.openChatCmd);
		}
	}
	
	public ChatInfoForm(String id, String invite, String title, int mode) {
		super(title);
		this.id = id;
		this.mode = mode;
	}

	void loadInternal(Thread thread) throws Exception {
		JSONObject rawPeer = null;
		String name = null;
		if (mode == 0) {
			JSONObject peer = MP.getPeer(id, true);
			id = peer.getString("id");
			setTitle(name = MP.getName(peer));
		} else if (mode == 1) {
			JSONObject r = ((JSONObject) MP.api("resolvePhone&phone=".concat(id))).getObject("res");
			MP.fillPeersCache(r);
			
			rawPeer = r.getArray("users").getObject(0);
			id = rawPeer.getString("id");
			setTitle(name = MP.getNameRaw(rawPeer));
		}

		boolean isUser = id.charAt(0) != '-';
		
		StringItem s;
		
		s = new StringItem(null, name);
		s.setFont(MP.medPlainFont);
		s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
		append(s);
		
		JSONObject full = null;
		if (mode != 3) {
			JSONObject fullInfo = (JSONObject) MP.api("getFullInfo&id=".concat(id));
			full = fullInfo.getObject("full");
			rawPeer = fullInfo.getObject(isUser ? "User" : "Chat");
		}
		
		if (isUser) {
			if (rawPeer.has("phone")) {
				phone = "+".concat(rawPeer.getString("phone"));
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
			
			if (rawPeer.has("username")) {
				s = new StringItem("Username", "@".concat(rawPeer.getString("username")));
				s.setFont(MP.medPlainFont);
				s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				s.setItemCommandListener(MP.midlet);
				append(s);
			}
		} else {
			if (mode != 3) {
				// TODO
//				if (full.getBoolean("can_view_participants", false)) {
//					addCommand(MP.chatMembersCmd);
//				}
				
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
				
				if (rawPeer.has("username")) {
					s = new StringItem("Link", "t.me/".concat(rawPeer.getString("username")));
					s.setFont(MP.medPlainFont);
					s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					s.setItemCommandListener(MP.midlet);
					append(s);
				}
			}
			
			if (mode != 0) {
				s = new StringItem(null, mode == 2 ? "View group" : "Join group", Item.BUTTON);
				s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				s.setDefaultCommand(mode == 2 ? MP.openChatCmd : MP.acceptInviteCmd);
				s.setItemCommandListener(MP.midlet);
				append(s);
				return;
			}
		}
		
		if (full.has("pinned_msg_id")) {
			this.pinnedMessageId = full.getInt("pinned_msg_id");
			
			s = new StringItem(null, "Go to pinned message");
			s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			s.setDefaultCommand(MP.gotoPinnedMsgCmd);
			s.setItemCommandListener(MP.midlet);
			append(s);
		}
		
		if (chatForm != null) {
			s = new StringItem(null, "Search messages", Item.BUTTON);
			s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			s.setDefaultCommand(MP.searchCmd);
			s.setItemCommandListener(MP.midlet);
			append(s);
			
			s = new StringItem(null, "Chat media", Item.BUTTON);
			s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			s.setDefaultCommand(MP.chatMediaCmd);
			s.setItemCommandListener(MP.midlet);
			append(s);
		}
		
		if (!isUser) {
			boolean left = rawPeer.getBoolean("left", false);
			s = new StringItem(null, left ? "Join group" : "Leave group", Item.BUTTON);
			s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			s.setDefaultCommand(left ? MP.joinChatCmd : MP.leaveChatCmd);
			s.setItemCommandListener(MP.midlet);
			append(s);
		}
	}

}
