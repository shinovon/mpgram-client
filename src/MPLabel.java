import java.util.Vector;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

public class MPLabel extends MPItem {
	
	static final int WIDTH_MARGIN = 2;
	
	Vector parsed = new Vector();
	Vector render;
	Vector urls;

	int color;
	boolean center;
	
	public MPLabel(String text, Font font, String url) {
		(this.parsed = new Vector())
		.addElement(new Object[] { text, font, url });
	}
	
	public MPLabel(String text, JSONArray entities) {
		parsed = new Vector();
		MP.wrapRichText(this, null, text, entities, 0);
	}

	void appendWord(String text, Font font, String url) {
		parsed.addElement(new Object[] { text, font, url });
	}
	
	void paint(Graphics g, int x, int y, int w, int h) {
		if (render == null) return;
		int l = render.size();
		g.setColor(color);
		for (int i = 0; i < l; ++i) {
			Object[] obj = (Object[]) render.elementAt(i);
			int[] pos = (int[]) obj[2];
			g.setFont((Font) obj[1]);
			g.drawString((String) obj[0],
					x + pos[0] + (center ? w >> 1 : 0) + WIDTH_MARGIN, y + pos[1],
					center ? Graphics.TOP | Graphics.HCENTER : 0);
		}
	}

	void layout(int width) {
		if (layout == width) return;
		layout = width;
		
		width -= WIDTH_MARGIN * 2;
		
		if (render == null) {
			render = new Vector();
		} else render.removeAllElements();
		
		Vector res = render;
		int x = 0, y = 0, idx = 0;
		
		int fh = 0;
		int l = parsed.size();
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
				int[] out = split(text, font, width, x, y, idx, ch, sl, fh, res);
				x = out[0]; y = out[1]; idx = out[2];
			} else {
				int j = ch;
				for (int i = ch; i < sl; ++i) {
					if ((c = text.charAt(i)) == '\n') {
						int[] out = split(text, font, width, x, y, idx, j, i, fh, res);
						x = 0; y = out[1] + fh; idx = out[2];
						j = i + 1;
					}
				}
				if (j != sl) {
					int[] out = split(text, font, width, x, y, idx, j, sl, fh, res);
					x = out[0]; y = out[1]; idx = out[2];
				}
			}
			
			if (url != null) {
				Vector v = new Vector();
				for (int i = startIdx; i < idx; ++i) {
					v.addElement(res.elementAt(i));
				}
				urls.addElement(new Object[] {url, v});
			}
		}
		
		contentWidth = y == 0 ? x : width;
		contentHeight = y + fh;
	}
	
	private static int[] split(String text, Font font, int width, int x, int y, int idx, int ch, int sl, int fh, Vector res) {
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
									res.addElement(new Object[] { text.substring(ch, ++ j), font, new int[] {x, y} });
									x = 0; y += fh; idx ++;
									
									i = ch = j;
									break w;
								}
							}
	
							res.addElement(new Object[] { text.substring(ch, i), font, new int[] {x, y} });
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
		return new int[] {x, y, idx};
	}
	
}
