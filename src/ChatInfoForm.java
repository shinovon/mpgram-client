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
import javax.microedition.lcdui.ImageItem;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.Spacer;
import javax.microedition.lcdui.StringItem;

public class ChatInfoForm extends MPForm {

	String id;
	String phone;
	String invite;
	MPChat chatForm;
	int mode; // 0 - chat info or profile by id, 1 - phone, 2 - invite peek, 3 - invite
	int pinnedMessageId;
	boolean canBan;
	
	public ChatInfoForm(String id, MPChat chatForm, int mode) {
		super(id);
		this.id = id;
		this.chatForm = chatForm;
		this.mode = mode;
		if (chatForm == null && mode != 3) {
			addCommand(MP.openChatCmd);
		}
	}
	
	public ChatInfoForm(String id, String title, int mode) {
		super(title);
		this.id = id;
		this.mode = mode;
	}

	void loadInternal(Thread thread) throws Exception {
		StringBuffer sb = new StringBuffer();
		JSONObject rawPeer = null;
		String name = null;
		boolean broadcast = false;
		if (mode == 0) {
			JSONObject peer = MP.getPeer(id, true);
			id = peer.getString("id");
			broadcast = peer.getBoolean("c", false);
			name = MP.getName(peer, false);
		} else if (mode == 1) {
			sb.append("resolvePhone&phone=").append(id);
			JSONObject r = ((JSONObject) MP.api(sb.toString())).getObject("res");
			MP.fillPeersCache(r);
			
			rawPeer = r.getArray("users").getObject(0);
			id = rawPeer.getString("id");
			name = MP.getNameRaw(rawPeer);
		} else {
			name = getTitle();
		}
		
		boolean isUser = id.charAt(0) != '-';
		boolean topic = mode == 0 && chatForm != null && chatForm.forum();
		StringItem s;
		
		if (topic) {
			s = new StringItem(null, chatForm.getTitle());
			s.setFont(MP.medPlainFont);
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			append(s);
		} else {
			if (MP.loadAvatars) {
				ImageItem img = new ImageItem("", null, 0, "");
				try {
					img.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				} catch (Exception ignored) {}
				MP.queueAvatar(id, img);
				append(img);
			}
			
			if (name != null) {
				s = new StringItem(null, name);
				s.setFont(MP.medPlainFont);
				s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				append(s);
			}
		}
		
		JSONObject full = null;
		if (mode != 3) {
			JSONObject fullInfo = (JSONObject) MP.api("getFullInfo&id=".concat(id));
			full = fullInfo.getObject("full");
			rawPeer = fullInfo.getObject(isUser ? "User" : "Chat");
			
			setTitle(MP.L[isUser ? LUserInfo : broadcast ? LChannelInfo : topic ? LTopicInfo : LGroupInfo]);
		}
		
		if (isUser) {
			if (rawPeer.has("status")) {
				sb.setLength(0);
				JSONObject status = rawPeer.getObject("status");
				if ("userStatusOnline".equals(status.getString("_"))) {
					sb.append(MP.L[LOnline]);
				} else if (status.has("was_online")) {
					sb.append(MP.L[LLastSeen]);
					sb.append(MP.localizeDate(status.getInt("was_online"), 3));
				} else {
					sb.append(MP.L[LOffline]);
				}
				
				s = new StringItem(null, sb.toString());
				s.setFont(MP.medPlainFont);
				s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				s.setItemCommandListener(MP.midlet);
				append(s);
			}
			
			if (rawPeer.has("phone")) {
				phone = "+".concat(rawPeer.getString("phone"));
				addCommand(MP.callCmd);
				
				s = new StringItem(MP.L[LMobile], phone);
				s.setFont(MP.medPlainFont);
				s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				s.addCommand(MP.callItemCmd);
				s.setItemCommandListener(MP.midlet);
				append(s);
			}
			
			if (full.has("about")) {
				s = new StringItem(MP.L[LAbout_User], full.getString("about"));
				s.setFont(MP.medPlainFont);
				s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				s.setItemCommandListener(MP.midlet);
				append(s);
			}
			
			if (rawPeer.has("username")) {
				s = new StringItem(MP.L[LUsername], "@".concat(rawPeer.getString("username")));
				s.setFont(MP.medPlainFont);
				s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				s.setItemCommandListener(MP.midlet);
				append(s);
			}
		} else {
			if (mode != 3) {
				if (full.getBoolean("can_view_participants", false)) {
					addCommand(MP.chatMembersCmd);
				}
				
				if (rawPeer.has("admin_rights")) {
					canBan = rawPeer.getObject("admin_rights").getBoolean("ban_users", false);
				}
				
				if (full.has("participants_count")) {
					s = new StringItem(null, MP.localizePlural(full.getInt("participants_count"),
							rawPeer.getBoolean("broadcast", false) ? L_subscriber : L_member));
					s.setFont(MP.medPlainFont);
					s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					s.setItemCommandListener(MP.midlet);
					append(s);
				}
				
				if (topic) {
					// TODO
				} else {
					if (full.has("about")) {
						s = new StringItem(MP.L[LAbout_Chat], full.getString("about"));
						s.setFont(MP.medPlainFont);
						s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
						s.setItemCommandListener(MP.midlet);
						append(s);
					}
				}
				
				if (rawPeer.has("username")) {
					String t = "t.me/".concat(rawPeer.getString("username"));
					s = new StringItem(MP.L[LLink], topic ? t.concat("/").concat(Integer.toString(chatForm.topMsgId())) : t);
					s.setFont(MP.medPlainFont);
					s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					s.setItemCommandListener(MP.midlet);
					append(s);
				}
			}
			
			if (mode != 0) {
				s = new StringItem(null, mode == 2 ? MP.L[LViewGroup] : MP.L[LJoinGroup], Item.BUTTON);
				s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				s.setDefaultCommand(mode == 2 ? MP.openChatCmd : MP.acceptInviteCmd);
				s.setItemCommandListener(MP.midlet);
				append(s);
				return;
			}
		}
		
		if (!topic && full.has("pinned_msg_id")) {
			this.pinnedMessageId = full.getInt("pinned_msg_id");
			
			s = new StringItem(null, MP.L[LGoToPinnedMessage]);
			s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			s.setDefaultCommand(MP.gotoPinnedMsgCmd);
			s.setItemCommandListener(MP.midlet);
			append(s);
			
			append(new Spacer(10, 8));
		}
		
		if (chatForm != null) {
			s = new StringItem(null, MP.L[LSearchMessages], Item.BUTTON);
			s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			s.setDefaultCommand(MP.searchMsgCmd);
			s.setItemCommandListener(MP.midlet);
			append(s);
			
			s = new StringItem(null, MP.L[LPhotos], Item.BUTTON);
			s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			s.setDefaultCommand(MP.chatPhotosCmd);
			s.setItemCommandListener(MP.midlet);
			append(s);
			
			s = new StringItem(null, MP.L[LVideos], Item.BUTTON);
			s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			s.setDefaultCommand(MP.chatVideosCmd);
			s.setItemCommandListener(MP.midlet);
			append(s);
			
			s = new StringItem(null, MP.L[LFiles], Item.BUTTON);
			s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			s.setDefaultCommand(MP.chatFilesCmd);
			s.setItemCommandListener(MP.midlet);
			append(s);
			
			s = new StringItem(null, MP.L[LAudioFiles], Item.BUTTON);
			s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			s.setDefaultCommand(MP.chatMusicCmd);
			s.setItemCommandListener(MP.midlet);
			append(s);

			s = new StringItem(null, MP.L[LVoiceMessages], Item.BUTTON);
			s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			s.setDefaultCommand(MP.chatVoiceCmd);
			s.setItemCommandListener(MP.midlet);
			append(s);
		}
		
		if (!isUser && !topic) {
			boolean left = rawPeer.getBoolean("left", false);
			// there is no method for leaving a group in telegram api
			if (left || rawPeer.has("username")) {
				s = new StringItem(null, MP.L[left ? LJoinGroup : LLeaveGroup], Item.BUTTON);
				s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				s.setDefaultCommand(left ? MP.joinChatCmd : MP.leaveChatCmd);
				s.setItemCommandListener(MP.midlet);
				append(s);
			}
		}
	}

}
