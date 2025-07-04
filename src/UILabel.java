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
import java.util.Vector;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

public class UILabel extends UIItem {
	
	static final int WIDTH_MARGIN = 2;
	
	Vector parsed = new Vector();
	Vector render;
	Vector urls;
	Vector selectedParts; String selectedUrl;

	int color = -1, bgColor, linkColor;
	boolean center, wrap = true, background;
	
	int focusIndex;
	
	public UILabel() {}
	
	public UILabel(String text, Font font, String url) {
		(this.parsed = new Vector())
		.addElement(new Object[] { text, font, url });
	}
	
	public UILabel(String text, JSONArray entities) {
		parsed = new Vector();
		MP.wrapRichText(this, null, text, entities, 0);
	}

	void appendWord(String text, Font font, String url) {
		parsed.addElement(new Object[] { text, font, url });
		requestLayout();
	}

	void paint(Graphics g, int x, int y, int w) {
		if (render == null) return;
		int l = render.size();
		g.setColor(color);
		for (int i = 0; i < l; ++i) {
			Object[] obj = (Object[]) render.elementAt(i);
			int[] pos = (int[]) obj[3];
			Font font = (Font) obj[1];
			String text = (String) obj[0];
			int tx = x + pos[0] + WIDTH_MARGIN, ty = y + pos[1];
			int tw = pos[2], th = pos[3];
			if (background) {
				g.setColor(bgColor);
				g.fillRect(tx, ty, tw, th);
				g.setColor(color);
			}
			if (obj[2] != null) {
				g.setColor(linkColor);
			}
			g.setFont(font);
			g.drawString(text, tx, ty, 0);
			if (focus && selectedParts != null && selectedParts.contains(obj)) {
				g.setColor(0xababab);
				g.drawRect(tx, ty, tw, th);
				g.setColor(color);
			} else if (obj[2] != null) {
				g.setColor(color);
			}
		}
	}

	public synchronized int layout(int width) {
		if (!layoutRequest && layoutWidth == width) {
			return contentHeight;
		}
		layoutWidth = width;
		
		width -= WIDTH_MARGIN * 2;
		
		if (render == null) {
			render = new Vector();
		} else render.removeAllElements();
		if (urls == null) {
			urls = new Vector();
		} else urls.removeAllElements();
		
		Vector res = render;
		int x = 0, y = 0, idx = 0;
		
		boolean center = this.center;
		
		int fh = 0;
		int l = parsed.size();
		int[] out = new int[3];
		for (int ei = 0; ei < l; ++ei) {
			int startIdx = idx;
			Object[] e = (Object[]) parsed.elementAt(ei);
			String text = (String) e[0];
			Font font = (Font) e[1];
			String url = (String) e[2];
			
			fh = font.getHeight();
			if (text == null || "\n".equals(text)) {
				x = 0;
				y += fh;
				continue;
			}
			
			int ch = 0;
			int sl = text.length();
			char c;
			while (ch < sl && ((c = text.charAt(ch)) < ' ')) {
				ch++;
				if (c != '\n') continue;
				x = 0;
				y += fh;
			}
			
			if (text.indexOf('\n', ch) == -1) {
				split(text, font, url, width, x, y, idx, ch, sl, fh, res, center, out);
				x = out[0]; y = out[1]; idx = out[2];
			} else {
				int j = ch;
				for (int i = ch; i < sl; ++i) {
					if ((c = text.charAt(i)) == '\n') {
						split(text, font, url, width, x, y, idx, j, i, fh, res, center, out);
						x = 0; y = out[1] + fh; idx = out[2];
						j = i + 1;
					}
				}
				if (j != sl) {
					split(text, font, url, width, x, y, idx, j, sl, fh, res, center, out);
					x = out[0]; y = out[1]; idx = out[2];
				}
			}
			
			if (url != null) {
				focusable = true;
				Vector v = new Vector();
				for (int i = startIdx; i < idx; ++i) {
					v.addElement(res.elementAt(i));
				}
				urls.addElement(new Object[] {url, v});
			}
		}
		if (center) centerRow(width, 0, x, y, res);
		
		contentWidth = y == 0 ? x : width;
		return contentHeight = y + fh;
	}
	
	boolean grabFocus() {
		if (!focusable) return false;
		focus = true;
		if (selectedParts == null) {
			focusLink(focusIndex);
		}
		return true;
	}
	
	void focusLink(int idx) {
		Object[] o = (Object[]) urls.elementAt(idx);
		selectedUrl = (String) o[0];
		selectedParts = (Vector) o[1];
	}
	
	int traverse(int dir, int height, int scrollY) {
		if (!focusable || urls.size() == 0) return 0;
		if (dir == Canvas.UP) {
			if (focusIndex <= 0) {
				focusLink(focusIndex = 0);
				return 0;
			}
			focusLink(--focusIndex);
			return Integer.MAX_VALUE;
		} else if (dir == Canvas.DOWN) {
			if (focusIndex >= urls.size() - 1) {
				focusLink(focusIndex = urls.size() - 1);
				return 0;
			}
			focusLink(++focusIndex);
			return Integer.MAX_VALUE;
		}
		return 0;
	}
	
	boolean action() {
		if (!focusable || selectedUrl == null) return false;
		MP.openUrl(selectedUrl);
		return true;
	}
	
	void tap(int x, int y) {
		int idx = getUrlAt(x, y);
		if (idx != -1) {
			focusLink(idx);
			action();
		}
	}
	
	synchronized int getUrlAt(int x, int y) {
		int l = urls.size();
		for (int i = 0; i < l; ++i) {
			Vector v = (Vector) ((Object[]) urls.elementAt(i))[1];
			int l2 = v.size();
			for (int j = 0; j < l2; ++j) {
				Object[] o = (Object[]) v.elementAt(j);
				int[] pos = (int[]) o[3];
				if (x >= pos[0] && x < pos[0] + pos[2] && y >= pos[1] && y < pos[1] + pos[3]) {
					return i;
				}
				if (pos[1] > y) break;
			}
		}
		return -1;
	}
	
	static String ellipsis(String text, Font font, int width) {
		if (font.stringWidth(text) < width) return text;
		int l = text.length();
		width -= font.stringWidth("...") + 6;
		for (int i = 1; i < l; ++i) {
			String s = text.substring(0, i);
			if (font.stringWidth(s) > width) {
				return s.concat("...");
			}
		}
		return "...";
	}
	
	private static void split(String text, Font font, String url, int width, int x, int y, int idx, int ch, int sl, int fh, Vector res, boolean center, int[] out) {
		if (ch != sl) {
			int ew = font.substringWidth(text, ch, sl - ch);
			if (x + ew < width) {
				res.addElement(new Object[] { text.substring(ch, sl), font, url, new int[] {x, y, ew, fh} });
				x += ew; idx ++;
			} else {
				for (int i = ch; i < sl; i++) {
					if (x + font.stringWidth(text.substring(ch, i+1)) >= width) {
						w: {
							for (int j = i; j > ch; j--) {
								char c = text.charAt(j);
								if (c == ' ' || (c >= ',' && c <= '/')) {
									String t = text.substring(ch, ++ j);
									int tw = font.stringWidth(t);
									if (center) {
										x = centerRow(width, tw, x, y, res);
									}
									res.addElement(new Object[] { t, font, url, new int[] {x, y, tw, fh} });
									x = 0; y += fh; idx ++;
									
									i = ch = j;
									break w;
								}
							}

							String t = text.substring(ch, i);
							int tw = font.stringWidth(t);
							if (center) {
								x = centerRow(width, tw, x, y, res);
							}
							res.addElement(new Object[] { t, font, url, new int[] {x, y, tw, fh} });
							x = 0; y += fh; idx ++;
							ch = i;
						}
					}
				}
				if (ch != sl) {
					String s = text.substring(ch, sl);
					int tw = font.stringWidth(s);
					res.addElement(new Object[] { s, font, url, new int[] {x, y, tw, fh} });
					x += tw; idx ++;
				}
			}
		}
		out[0] = x; out[1] = y; out[2] = idx;
	}
	
	private static int centerRow(int width, int t, int x, int y, Vector res) {
		int rw = (width - (x + t)) / 2;
		x += rw;
		for (int k = res.size() - 1; k >= 0; --k) {
			Object[] obj = (Object[]) res.elementAt(k);
			if (((int[]) obj[2])[1] == y) {
				((int[]) obj[2])[0] += rw;
			} else break;
		}
		return x;
	}
	
}
