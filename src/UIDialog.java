import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class UIDialog extends UIItem {

	String id;
	Image image;
	String name;
	String text;

	UIDialog(JSONObject dialog) {
		focusable = true;

		id = dialog.getString("id");

		JSONObject peer = MP.getPeer(id, false);
		name = MP.getName(peer, false);

		StringBuffer sb = new StringBuffer();
		MP.appendDialog(sb, peer, id, dialog.getObject("msg", null));

		text = sb.toString();
	}

	void paint(Graphics g, int x, int y, int w) {
		int h = contentHeight;
		if (focus) {
			g.setColor(ChatCanvas.colors[ChatsCanvas.COLOR_CHATS_ITEM_HIGHLIGHT_BG]);
			g.fillRect(0, y, w, h);
		}

		Font font = MP.chatsListFontSize < 2 ? MP.smallPlainFont : MP.medPlainFont;
		int fontHeight = font.getHeight();
		g.setFont(font);

		g.setColor(ChatCanvas.colors[ChatsCanvas.COLOR_CHATS_ITEM_TITLE]);
		g.drawString(name, x, y + 2, 0);

		g.setColor(ChatCanvas.colors[ChatsCanvas.COLOR_CHATS_ITEM_TEXT]);
		g.drawString(text, x, y + fontHeight + 4, 0);
	}

	public synchronized int layout(int width) {
		if (!layoutRequest && layoutWidth == width) {
			return contentHeight;
		}
		contentWidth = layoutWidth = width;

		Font font = MP.chatsListFontSize < 2 ? MP.smallPlainFont : MP.medPlainFont;
		int fontHeight = font.getHeight();

		if (MP.chatAvatar) {
			MP.avatarSize = fontHeight * 2;
		}
		return contentHeight = (fontHeight + 4) * 2;
	}

	boolean tap(int x, int y, boolean longTap) {
		return action();
	}

	boolean action() {
		MP.openChat(id, -1);
		return true;
	}

}
