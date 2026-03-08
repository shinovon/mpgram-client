/*
Copyright (c) 2026 Arman Jussupgaliyev

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
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class UIDialog extends UIItem implements LangConstants {

	static final int COLOR_CHATS_ITEM_HIGHLIGHT_BG = 41;
	static final int COLOR_CHATS_ITEM_HIGHLIGHT_FG = 42;
	static final int COLOR_CHATS_ITEM_TITLE = 43;
	static final int COLOR_CHATS_ITEM_TEXT = 44;
	static final int COLOR_CHATS_ITEM_MEDIA = 45;
	static final int COLOR_CHATS_ITEM_SEPARATOR = 46;
	static final int COLOR_CHATS_ITEM_UNREAD_BG = 47;
	static final int COLOR_CHATS_ITEM_UNREAD_FG = 48;
	static final int COLOR_CHATS_ITEM_UNREAD_MUTED_BG = 49;

	String id;
	String title;
	String time;
	String text;
	String sender;
	boolean media;
	String unread;
	boolean silent;

	String titleRender;
	String textRender;
	String senderRender;
	int timeWidth;
	int senderWidth;
	int unreadWidth;
	
	Image image;
	boolean enableImage;
	int imageWidth;

	Font font;

	UIDialog(JSONObject dialog, boolean showMessage) {
		focusable = true;

		id = dialog.getString("id");

		JSONObject peer = MP.getPeer(id, false);
		title = MP.getName(peer, false);

		enableImage = MP.loadAvatars;

		if (showMessage) {
			silent = dialog.getBoolean("silent", peer.getBoolean("c", false) ?
					MP.muteBroadcasts : id.charAt(0) == '-' ? MP.muteChats : MP.muteUsers);

			int unreadCount = dialog.getInt("unread", 0);
			if (unreadCount != 0) unread = Integer.toString(unreadCount);

			JSONObject msg = dialog.getObject("msg", null);
			time = MP.localizeDate(msg.getLong("date"), 2);

			if (!peer.getBoolean("c", false)) {
				if (msg.getBoolean("out", false) && !id.equals(MP.selfId)) {
					sender = MP.L[LYou];
				} else if (id.charAt(0) == '-' && msg.has("from_id")) {
					sender = MP.getName(msg.getString("from_id"), true);
				}
			}

			if (msg.has("media")) {
				media = true;
				text = MP.L[LMedia];
			} else if (msg.has("fwd")) {
				media = true;
				text = MP.L[LForwardedMessage];
			} else if (msg.has("act")) {
				media = true;
				text = MP.L[LAction];
			} else {
				text = msg.getString("text");
			}
		}
	}

	void paint(Graphics g, int x, int y, int w) {
		int h = contentHeight;
		if (focus) {
			g.setColor(MPCanvas.colors[COLOR_CHATS_ITEM_HIGHLIGHT_BG]);
			g.fillRect(0, y, w, h);
			g.setColor(MPCanvas.colors[COLOR_CHATS_ITEM_HIGHLIGHT_FG]);
		}

		int fontHeight = font.getHeight();
		g.setFont(font);

		int tx = 4;
		
		if (enableImage) {
			if (image != null) g.drawImage(image, x + tx, y + ((h - imageWidth) >> 1), 0);
			tx += imageWidth + 4;
		}

		if (title != null) {
			String title = titleRender;
			if (title == null) {
				titleRender = title = UILabel.ellipsis(this.title, font, w - 8 - tx - timeWidth);
			}
			if (!focus) g.setColor(MPCanvas.colors[COLOR_CHATS_ITEM_TITLE]);
			g.drawString(title, x + tx, y + 6, 0);
		}
		if (time != null) {
			if (!focus) g.setColor(MPCanvas.colors[COLOR_CHATS_ITEM_TEXT]);
			g.drawString(time, w - timeWidth - 6, y + 6, 0);
		}

		if (sender != null) {
			String sender = senderRender;
			if (sender == null) {
				senderRender = sender = UILabel.ellipsis(this.sender, font, (w - tx) >> 1).concat(": ");
				senderWidth = font.stringWidth(sender);
			}
			if (!focus) g.setColor(MPCanvas.colors[COLOR_CHATS_ITEM_MEDIA]);
			g.drawString(sender, x + tx, y + fontHeight + 10, 0);
			tx += senderWidth;
		}

		if (unread != null) {
			g.setColor(MPCanvas.colors[silent ? COLOR_CHATS_ITEM_UNREAD_MUTED_BG : COLOR_CHATS_ITEM_UNREAD_BG]);
			int uw = unreadWidth;
			g.fillRect(w - 8 - uw, y + fontHeight + 8, uw + 4, fontHeight + 4);

			g.setColor(MPCanvas.colors[COLOR_CHATS_ITEM_UNREAD_FG]);
			g.drawString(unread, w - 6 - uw, y + fontHeight + 10, 0);
		}

		if (text != null) {
			String text = textRender;
			if (text == null) {
				int uw = unreadWidth == 0 ? 0 : (unreadWidth + 8);
				textRender = text = UILabel.ellipsis(this.text, font, w - tx - 4 - uw);
			}
			if (!focus) g.setColor(MPCanvas.colors[media ? COLOR_CHATS_ITEM_MEDIA : COLOR_CHATS_ITEM_TEXT]);
			g.drawString(text, x + tx, y + fontHeight + 10, 0);
		}

		if (next != null && !focus) {
			g.setColor(MPCanvas.colors[COLOR_CHATS_ITEM_SEPARATOR]);
			g.drawLine(x, y + h, w, y + h);
		}
	}

	public synchronized int layout(int width) {
		if (!layoutRequest && layoutWidth == width) {
			return contentHeight;
		}
		contentWidth = layoutWidth = width;

		titleRender = null;
		textRender = null;

		font = MP.chatsListFontSize < 2 ? MP.smallPlainFont : MP.medPlainFont;
		int fontHeight = font.getHeight();

		timeWidth = font.stringWidth(time);

		if (unread != null) {
			unreadWidth = font.stringWidth(unread);
		}

		if (enableImage) {
			MP.avatarSize = imageWidth = fontHeight * 2;
		}
		return contentHeight = (fontHeight + 8) * 2;
	}

	boolean tap(int x, int y, boolean longTap) {
		return action();
	}

	boolean action() {
		if (unread != null && Integer.parseInt(unread) < MP.messagesLimit) {
			unread = null;
			textRender = null;
			unreadWidth = 0;
		}

		((ChatsCanvas) container).select(this);
		return true;
	}

}
//#endif
