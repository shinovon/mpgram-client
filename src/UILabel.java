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
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

public class UILabel extends UIItem {
	
	static final int WIDTH_MARGIN = 2;
	
	Vector parsed = new Vector();
	Vector render;
	Hashtable urls;
	Vector selectedParts; String selectedUrl;

	int color = -1, bgColor, linkColor;
	boolean center, wrap = true, background;
	
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
		if (background) {
			g.setColor(bgColor);
		}
		g.setColor(color);
		for (int i = 0; i < l; ++i) {
			Object[] obj = (Object[]) render.elementAt(i);
			int[] pos = (int[]) obj[2];
			Font font = (Font) obj[1];
			String text = (String) obj[0];
			int tx = x + pos[0] + WIDTH_MARGIN, ty = y + pos[1];
			int tw = font.stringWidth(text), th = font.getHeight();
			if (background) {
				g.setColor(bgColor);
				g.fillRect(tx, ty, tw, th);
				g.setColor(color);
			}
			g.setFont(font);
			g.drawString(text,
					tx, ty, 0);
			if (selectedParts != null && selectedParts.contains(obj)) {
				g.setColor(0xababab);
				g.drawRect(tx, ty, tw, th);
				g.setColor(color);
			}
		}
	}

	public int layout(int width) {
		if (!layoutRequest && layoutWidth == width) {
			return contentHeight;
		}
		layoutWidth = width;
		
		width -= WIDTH_MARGIN * 2;
		
		if (render == null) {
			render = new Vector();
		} else render.removeAllElements();
		if (urls == null) {
			urls = new Hashtable();
		} else urls.clear();
		
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
				split(text, font, width, x, y, idx, ch, sl, fh, res, center, out);
				x = out[0]; y = out[1]; idx = out[2];
			} else {
				int j = ch;
				for (int i = ch; i < sl; ++i) {
					if ((c = text.charAt(i)) == '\n') {
						split(text, font, width, x, y, idx, j, i, fh, res, center, out);
						x = 0; y = out[1] + fh; idx = out[2];
						j = i + 1;
					}
				}
				if (j != sl) {
					split(text, font, width, x, y, idx, j, sl, fh, res, center, out);
					x = out[0]; y = out[1]; idx = out[2];
				}
			}
			
			if (url != null) {
				Vector v = new Vector();
				for (int i = startIdx; i < idx; ++i) {
					v.addElement(res.elementAt(i));
				}
				urls.put(url, v);
			}
		}
		if (center) centerRow(width, 0, x, y, res);
		
		contentWidth = y == 0 ? x : width;
		return contentHeight = y + fh;
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
	
	private static void split(String text, Font font, int width, int x, int y, int idx, int ch, int sl, int fh, Vector res, boolean center, int[] out) {
		if (ch != sl) {
			int ew = font.substringWidth(text, ch, sl - ch);
			if (x + ew < width) {
				res.addElement(new Object[] { text.substring(ch, sl), font, new int[] {x, y} });
				x += ew; idx ++;
			} else {
				for (int i = ch; i < sl; i++) {
					if (x + font.stringWidth(text.substring(ch, i+1)) >= width) {
						w: {
							for (int j = i; j > ch; j--) {
								char c = text.charAt(j);
								if (c == ' ' || (c >= ',' && c <= '/')) {
									String t = text.substring(ch, ++ j);
									if (center) {
										x = centerRow(width, font.stringWidth(t), x, y, res);
									}
									res.addElement(new Object[] { t, font, new int[] {x, y} });
									x = 0; y += fh; idx ++;
									
									i = ch = j;
									break w;
								}
							}

							String t = text.substring(ch, i);
							if (center) {
								x = centerRow(width, font.stringWidth(t), x, y, res);
							}
							res.addElement(new Object[] { t, font, new int[] {x, y} });
							x = 0; y += fh; idx ++;
							ch = i;
						}
					}
				}
				if (ch != sl) {
					String s = text.substring(ch, sl);
					res.addElement(new Object[] { s, font, new int[] {x, y} });
					x += font.stringWidth(s); idx ++;
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
