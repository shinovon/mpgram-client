import java.util.Vector;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

public class MPLabel extends MPItem {
	
	String text;
	Vector parsed = new Vector();
	Vector render;
	Vector urls;
	boolean center;
	
	public MPLabel(String text, String font, String url) {
		this.text = text;
		(this.parsed = new Vector())
		.addElement(new Object[] { text, font, url });
	}
	
	public MPLabel(String text, JSONArray entities) {
		parsed = new Vector();
		MP.wrapRichText(this, null, this.text = text, entities, 0);
	}

	void appendWord(String text, Font font, String url) {
		parsed.addElement(new Object[] { text, font, url });
	}
	
	void paint(Graphics g, int x, int y, int w, int h) {
		if (render == null) {
			System.out.println(toString() + " early paint");
			return;
		}
		int l = render.size();
		for (int i = 0; i < l; ++i) {
			Object[] obj = (Object[]) render.elementAt(i);
			int[] pos = (int[]) obj[2];
			g.setFont((Font) obj[1]);
			g.drawString((String) obj[0],
					x + pos[0] + (center ? w >> 1 : 0), y + pos[1],
					center ? Graphics.TOP | Graphics.HCENTER : 0);
		}
	}

	void layout(int width) {
		if (render == null) {
			render = new Vector();
		} else render.removeAllElements();
		
		int l = parsed.size();
		
		Vector res = render;
		
		// TODO urls
		
		int x = 0, y = 0, idx = 0;
		for (int ei = 0; ei < l; ++ei) {
			int startIdx = idx;
			Object[] e = (Object[]) parsed.elementAt(ei);
			String text = (String) e[0];
			Font font = (Font) e[1];
			String url = (String) e[1];
			
			int fh = font.getHeight();
			if (text == null || "\n".equals(e)) {
				x = 0;
				y += fh;
				continue;
			}
			
			int ch = 0;
			int sl = text.length();
			char c;
			while (ch < sl && ((c = text.charAt(ch)) == '\n' || c == '\r' || c == '\t')) {
				ch++;
				if (c != '\n') continue;
				x = 0;
				y += fh;
			}
			
			if (text.indexOf('\n', ch) == -1) {
				idx = split(text, font, url, width, x, y, idx, ch, sl, fh, res)[2];
			} else {
				int j = ch;
				for (int i = ch; i < sl; ++i) {
					if ((c = text.charAt(i)) == '\n') {
						int[] out = split(text, font, url, width, x, y, idx, j, i, fh, res);
						x = out[0]; y = out[1]; idx = out[2];
						j = i;
					}
				}
				if (j != sl) {
					idx = split(text, font, url, width, x, y, idx, j, sl, fh, res)[2];
				}
			}
			
			if (url != null) {
				Vector v = new Vector();
				for (int i = startIdx; i < idx; ++i) {
					
				}
				urls.addElement(new Object[] {url, v});
			}
		}
	}
	
	private int[] split(String text, Font font, String url, int width, int x, int y, int idx, int ch, int sl, int fh, Vector res) {
		int ew = font.substringWidth(text, ch, sl);
		if (x + ew < width) {
			res.addElement(new Object[] { text.substring(ch), font, new int[] {x, y} });
			idx ++;
			x += ew;
		} else {
			for (int i = ch; i < sl; i++) {
				if (font.stringWidth(text.substring(ch, i+1)) >= width) {
					w: {
						for (int k = i; k > ch; k--) {
							char c = text.charAt(k);
							if (c == ' ' || (c >= ',' || c <= '/')) {
								res.addElement(new Object[] { text.substring(ch, k), font, new int[] {x, y} });
//								res.addElement(new Object[] { text.substring(k, sl), font, new int[] {x = 0, y += fh} });
								x = 0;
								y += fh;
								idx += 2;
								
//								x += font.substringWidth(text, k, sl);
								i = ch = k + 1;
								break w;
							}
						}
						
						i -= 1;
						res.addElement(new Object[] { text.substring(ch, i), font, new int[] {x, y} });
//						res.addElement(new Object[] { text.substring(i, sl), font, new int[] {x = 0, y += fh} });
						x = 0;
						y += fh;
						idx += 2;
						
//						x += font.substringWidth(text, i, sl);
						i = ch += 1;
					}
				}
			}
			if (ch != sl) {
				res.addElement(new Object[] { text.substring(ch, sl), font, new int[] {x, y} });
				x += font.substringWidth(text, ch, sl);
			}
		}
		return new int[] {x, y, idx};
	}
	
}
