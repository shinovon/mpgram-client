import javax.microedition.lcdui.Graphics;

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
public class UIMessage extends UIItem implements LangConstants {
	
	private static final int MAX_WIDTH = 440;
	private static final int MARGIN_HEIGHT = 1;
	static final int SPACE_HEIGHT = 3;
	private static final int MARGIN_WIDTH = 2;
	private static final int MARGIN_SIDE = 10;
	private static final int PADDING = 2;
	
	UILabel text;
	UIItem focusChild;
	int focusIndex = -1;
	
	boolean out;
	boolean edited;
	String name, time;
	int timeWidth;
	
	UIMessage(JSONObject message) {
		focusable = true;
		text = new UILabel("меня ломает невыносимо ломки лютые я не могу больше это терпеть мне нужна доза я хочу кайфа ломки невыносимые на меня нападают бесы очень мощно я хочу кайф и дозу меня угнетает то что у меня нет возможности достать кайф", MP.smallPlainFont, "");
		text.color = -1; // message fg color
		name = "steepy";
		time = "10:47";
		timeWidth = MP.smallPlainFont.stringWidth(time);
		out = false;
	}
	
	void paint(Graphics g, int x, int y, int w) {
		int h = contentHeight;
		int cw = Math.min(MAX_WIDTH, w);
		if (out && w < 900) {
			x += w - cw;
		}
		g.setColor(out ? 0x2B5278 : 0x182533); // TODO message bg color
		g.fillRect(x += MARGIN_WIDTH + (out && w < 900 ? MARGIN_SIDE : 0), y += MARGIN_HEIGHT,
				cw -= MARGIN_WIDTH * 2 + MARGIN_SIDE, h -= (MARGIN_HEIGHT * 2));
		if (focus) {
			g.setColor(-1);
			g.drawRect(x, y, cw - 1, h - 1);
		}
		cw -= PADDING * 2;
		int rx = x;
		x += PADDING;
		y += PADDING;
		cw -= PADDING;
		
		// name
		if (!out) {
			g.setColor(0x71BAFA); // TODO message author color
			g.setFont(MP.smallBoldFont);
			g.drawString(name, x, y, 0);
			y += MP.smallBoldFontHeight;
		}
		
		// photos

		// text
		if (text != null) {
			UILabel text = this.text;
			text.paint(g, x, y, cw);
			y += text.contentHeight;
		}
		
		// media
		
		// buttons
		
		// time
		g.setColor(out ? 0x7DA8D3 : 0x6D7F8F);
		g.setFont(MP.smallPlainFont);
		g.drawString(time, rx + cw, y, Graphics.TOP | Graphics.RIGHT);
		if (edited) {
			g.drawString(MP.L[Edited], rx + cw - timeWidth - MP.smallPlainFontSpaceWidth, y, Graphics.TOP | Graphics.RIGHT);
		}
	}
	
	int layout(int width) {
		if (!layoutRequest && layoutWidth == width) {
			return contentHeight;
		}
		layoutWidth = width;
		int cw = Math.min(MAX_WIDTH, width) - PADDING * 2 - MARGIN_WIDTH * 2 - MARGIN_SIDE;
		int h = MARGIN_HEIGHT + PADDING * 2;
		if (!out) {
			h += MP.smallBoldFontHeight;
		}
		if (text != null) {
			h += text.layout(cw);
		}
		// time
		h += MP.smallPlainFontHeight;
		return contentHeight = h + MARGIN_HEIGHT;
	}
	
	boolean grabFocus() {
		focus = true;
		return true;
	}
	
	void lostFocus() {
		focus = false;
	}
	
	int traverse(int dir, int height, int scrollY) {
		return 0;
	}
	
	void action() {
		if (focusChild != null) {
			focusChild.action();
		}
	}
	
	int[] menu() {
		int[] menu = focusChild != null ? focusChild.menu() : null;
		
		return menu;
	}

}
