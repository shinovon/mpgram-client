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
import javax.microedition.lcdui.Graphics;

public class UIItem {
	
	int contentWidth, contentHeight;
	int layoutWidth, y;
	boolean focus, focusable;
	boolean layoutRequest;
	Object container;
	
	/**
	 * 
	 * @param g Graphics context
	 * @param x Position on screen
	 * @param y Position on screen
	 * @param w Container width
	 */
	/* abstract */ void paint(Graphics g, int x, int y, int w) {}
	
	/**
	 * @param width Container width
	 * @return Calculated height
	 */
	int layout(int width) {
		layoutWidth = width;
		return contentHeight;
	}
	
	boolean grabFocus() {
		return false;
	}
	
	void lostFocus() {}
	
	/**
	 * @param dir Direction: Canvas.UP or Canvas.DOWN
	 * @param height Screen height
	 * @param scrollY Scrolled amount
	 * @return
	 * 0: not grabbed, Integer.MAX_VALUE: grabbed in screen visible area <br>
	 * other values: request scroll to this value
	 */
	int traverse(int dir, int height, int scrollY) {
		return 0;
	}
	
	/**
	 * Middle key pressed or tapped
	 */
	void action() {}
	
	/**
	 * Left soft key pressed or long tapped
	 * @return Array of localization entries
	 */
	int[] menu() {
		return null;
	}
	
	/* protected */ void requestLayout() {
		layoutRequest = true;
		if (container == null) return;
		
		if (container instanceof UIItem) {
			((UIItem) container).requestLayout();
		} else if (container instanceof ChatCanvas) {
			((ChatCanvas) container).requestLayout(this);
		}
	}

}
