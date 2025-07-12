import javax.microedition.lcdui.Graphics;

public class UIPageButton extends UIItem {
	
	private int dir;

	public UIPageButton(int dir) {
		this.dir = dir;
		contentHeight = MP.medBoldFontHeight + 4;
		focusable = true;
	}
	
	void paint(Graphics g, int x, int y, int w) {
		if (focus) {
			g.setColor(ChatCanvas.colors[ChatCanvas.COLOR_CHAT_HIGHLIGHT_BG]);
			g.fillRect(x, y, w, contentHeight);
		}
		g.setColor(ChatCanvas.colors[ChatCanvas.COLOR_CHAT_FG]);
		g.setFont(MP.medBoldFont);
		g.drawString(MP.L[dir == -1 ? LangConstants.OlderMessages : LangConstants.NewerMessages], x + (w >> 1), y, Graphics.HCENTER | Graphics.TOP);
	}
	
	boolean action() {
		((ChatCanvas) container).paginate(dir);
		return true;
	}

	boolean tap(int x, int y, boolean longTap) {
		return !longTap && action();
	}

}
