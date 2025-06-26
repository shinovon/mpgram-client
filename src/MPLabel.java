import java.util.Vector;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

public class MPLabel extends MPItem {
	
	String text;
	Vector parsed = new Vector();
	Vector render;
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
		
		int x = 0, y = 0;
		for (int ei = 0; ei < l; ++ei) {
			Object[] e = (Object[]) parsed.elementAt(ei);
			String s = (String) e[0];
			Font f = (Font) e[1];
			
			int fh = f.getHeight();
			if (s == null || "\n".equals(e)) {
				x = 0;
				y += fh;
				continue;
			}
			
			int ch = 0;
			int sl = s.length();
			char c;
			while (ch < sl && ((c = s.charAt(ch)) == '\n' || c == '\r' || c == '\t')) {
				ch++;
				if (c != '\n') continue;
				x = 0;
				y += fh;
			}
			
			if (s.indexOf('\n', ch) == -1) {
				split(s, f, width, x, y, ch, sl, fh, res);
			} else {
				int j = ch;
				for (int i = ch; i < sl; ++i) {
					if ((c = s.charAt(i)) == '\n') {
						int[] t = split(s, f, width, x, y, j, i, fh, res);
						x = t[0]; y = t[1];
						j = i;
					}
				}
				if (j != sl) {
					split(s, f, width, x, y, j, sl, fh, res);
				}
			}
		}
	}
	
	private int[] split(String s, Font f, int width, int x, int y, int ch, int sl, int fh, Vector res) {
		int ew = f.substringWidth(s, ch, sl);
		if (x + ew < width) {
			res.addElement(new Object[] { s.substring(ch), f, new int[] {x, y} });
			x += ew;
		} else {
			for (int i = ch; i < sl; i++) {
				if (f.stringWidth(s.substring(ch, i+1)) >= width) {
					w: {
						for (int k = i; k > ch; k--) {
							char c = s.charAt(k);
							if (c == ' ' || (c >= ',' || c <= '/')) {
								res.addElement(new Object[] { s.substring(ch, k), f, new int[] {x, y} });
								res.addElement(new Object[] { s.substring(k, sl), f, new int[] {x = 0, y += fh} });
								x += f.substringWidth(s, k, sl);
								i = ch = k + 1;
								break w;
							}
						}
						
						i -= 2; // TODO ??
						res.addElement(new Object[] { s.substring(ch, i), f, new int[] {x, y} });
						res.addElement(new Object[] { s.substring(i, sl), f, new int[] {x = 0, y += fh} });
						x += f.substringWidth(s, i, sl);
						i = ch += 1;
					}
				}
			}
		}
		return new int[] {x,y};
	}
	
}
