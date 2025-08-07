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
//#ifndef NO_CHAT_CANVAS
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
		g.drawString(MP.L[dir == -1 ? LangConstants.LOlderMessages : LangConstants.LNewerMessages],
				x + (w >> 1), y + 2, Graphics.HCENTER | Graphics.TOP);
	}
	
	boolean action() {
		((ChatCanvas) container).paginate(dir);
		return true;
	}

	boolean tap(int x, int y, boolean longTap) {
		return !longTap && action();
	}

}
//#endif
