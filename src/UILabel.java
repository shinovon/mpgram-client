/*
Copyright (c) 2025-2026 Arman Jussupgaliyev


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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class UILabel extends UIItem implements Constants {

	static final int
			STYLE_STRIKETHROUGH = 1,
			STYLE_SPOILER = 2,
			STYLE_MONOSPACE = 4,
			STYLE_LINK = 8;

//#ifdef EMOJI_SUPPORT
	static Hashtable emojiTable;

	// resets every frame
	static int loadedEmojis;
	static int renderedEmojis;
//#endif

	Vector parsed; // Object[] {text, font, url, int[] {style} }
	Vector render; // Object[] { text, font, url, int[] {x, y, width, height, style} }
	Vector urls; // Object[] { url, Vector(elements of render) }
	Vector selectedParts; String selectedUrl;

	int color = -1, bgColor, linkColor = 0x0000FF, focusColor = 0xABABAB, monospaceColor, spoilerColor;
	boolean center, ellipsis, background;

	int focusIndex;

	boolean spoilersUnhidden;

	int emojiCount;

	public UILabel() {
		this.parsed = new Vector();
	}

	public UILabel(String text, Font font, String url) {
		if (EMOJI_SUPPORT) {
			parsed = new Vector();
			append(text, font, url, 0);
			return;
		}
		(this.parsed = new Vector())
		.addElement(new Object[] { text, font, url, null });
	}

	public UILabel(String text, JSONArray entities) {
		parsed = new Vector();
		MP.wrapRichText(this, null, text, entities, 0);
	}

	void append(String text, Font font, String url, int style) {
		if (url != null) {
			focusable = true;
			style |= STYLE_LINK;
		}
		if ((style & STYLE_SPOILER) != 0) {
			url = url == null ? "!" : "!".concat(url);
			focusable = true;
		}
		Object styleObj = style == 0 ? null : new int[] { style };

		if (EMOJI_SUPPORT) {
			if (emojiTable == null) {
				emojiTable = new Hashtable();
			}

			int l = text.length();

			StringBuffer sb = new StringBuffer();
			int i = 0;
			while (i < l) {
				char c = text.charAt(i);
				if (c == 0x2026) {
					sb.append("...");
					i++;
					continue;
				}
				if (c == 0x2023) {
					sb.append('-');
					i++;
					continue;
				}
				if (c == 0xFE0F) {
					i++;
					continue;
				}
				if (c == 0x20E3 && i != 0) {
					int start;
					char c2 = text.charAt(start = (i - 1));
					if (c2 == 0xFE0F && i != 1) {
						c2 = text.charAt(start = (i - 2));
					}
					if ((c2 >= '0' && c2 <= '9') || c2 == '*' || c2 == '#') {
						sb.deleteCharAt(sb.length() - 1);
						if (sb.length() != 0) {
							append2(sb.toString(), font, url, styleObj);
							sb.setLength(0);
						}

						i = appendEmoji(text, i, l, start, sb);
						continue;
					}
				}
				if (c >= 0xD800 && c <= 0xDBFF && i + 1 < l) {
					char c2 = text.charAt(i + 1);
					if (c2 >= 0xDC00 && c2 <= 0xDFFF) {
						int cp = ((c - 0xD800) << 10) + (c2 - 0xDC00) + 0x10000;

						if (cp >= 0x1F000 && cp <= 0x1FAFF) {
							if (sb.length() != 0) {
								append2(sb.toString(), font, url, styleObj);
								sb.setLength(0);
							}

							int start = i;
							i += 2;
							if (cp >= 0x1F1E6 && cp <= 0x1F1FF && i + 1 < l && text.charAt(i) == 0xD83C) {
								c2 = text.charAt(i + 1);
								if (c2 >= 0xDDE6 && c2 <= 0xDDFF) {
									i += 2;
								}
							}

							i = appendEmoji(text, i, l, start, sb);
							continue;
						}
					}
				} else if ((c >= 0x2600 && c <= 0x27BF) || (c >= 0x2300 && c <= 0x23FF)
						|| (c >= 0x2B05 && c <= 0x2B55) || (c >= 0x2190 && c <= 0x21FF)
						|| (c >= 0x25A0 && c <= 0x25FF)
						|| c == 0x00A9 || c == 0x00AE || c == 0x203C || c == 0x2049 || c == 0x2122
						|| c == 0x2139 || c == 0x3030 || c == 0x303D || c == 0x3297 || c == 0x3299) {
					if (sb.length() != 0) {
						append2(sb.toString(), font, url, styleObj);
						sb.setLength(0);
					}

					int start = i;
					i++;
					i = appendEmoji(text, i, l, start, sb);
					continue;
				}
				sb.append(c);
				i++;
			}
			if (sb.length() != 0) {
				append2(sb.toString(), font, url, styleObj);
			}
		} else {
			append2(text, font, url, styleObj);
		}
		requestLayout();
	}

	private void append2(String text, Font font, String url, Object style) {
		parsed.addElement(new Object[] { text, font, url, style });
	}

//#ifdef EMOJI_SUPPORT
	private int appendEmoji(String text, int i, int l, int start, StringBuffer sb) {
		if (!EMOJI_SUPPORT) return 0;

		emojiCount++;

		while (i < l) {
			char c = text.charAt(i);

			if (c == 0xFE0E || c == 0xFE0F || c == 0x20E3) {
				i++;
			} else if (c == 0x200D) {
				i++;
				if (i < l) {
					c = text.charAt(i);
					if (c >= 0xD800 && c <= 0xDBFF) {
						if (i + 1 < l && text.charAt(i + 1) >= 0xDC00 && text.charAt(i + 1) <= 0xDFFF) {
							i += 2;
						} else i++;
					} else i++;
				}
			} else if (c == 0xD83C && i + 1 < l) {
				char next = text.charAt(i + 1);
				if (next >= 0xDFFB && next <= 0xDFFF) {
					i += 2;
				} else break;
			} else break;
		}

		if (emojiCount < 200) {
			String code = stringToHex(sb, text.substring(start, i));
			sb.setLength(0);

			append2(code, null, null, null);
		}

		return i;
	}

	private static String stringToHex(StringBuffer sb, String s) {
		if (!EMOJI_SUPPORT) return null;

		char[] c = s.toCharArray();
		sb.setLength(0);
		int l = c.length;
		for (int i = 0; i < l; i++) {
			if (c[i] == 0xFE0F) continue;
			sb.append(Integer.toHexString(c[i] & 0xFFFF));
		}
		sb.append(".png");
		return sb.toString();
	}
//#endif

	void paint(Graphics g, int x, int y, int w) {
		if (render == null) return;
		int l = render.size();
		g.setColor(color);

		UIItem root = (UIItem) container;
		MPCanvas chat = (MPCanvas) root.container;
		int t = (chat.reverse ? chat.height - chat.bottom + chat.scroll - (root.y + root.contentHeight)
				: chat.top - chat.scroll + root.y) + this.y;

		for (int i = 0; i < l; ++i) {
			Object[] obj = (Object[]) render.elementAt(i);
			int[] pos = (int[]) obj[3];
//			if (getVisibility(pos) != 0) continue;
			if (t + pos[1] + pos[3] <= chat.top)
				continue;
			if (t + pos[1] >= chat.height - chat.bottom)
				break;
			Font font = (Font) obj[1];
			String text = (String) obj[0];
			int tx = x + pos[0], ty = y + pos[1];
			int tw = pos[2], th = pos[3];
			int style = pos[4];
			if ((style & STYLE_SPOILER) != 0 && !spoilersUnhidden) {
				g.setColor(spoilerColor);
				g.fillRect(tx, ty, tw, th);
				g.setColor(color);
			} else {
				if (background) {
					g.setColor(bgColor);
					g.fillRect(tx, ty, tw, th);
					g.setColor(color);
				}
				if ((style & STYLE_LINK) != 0) {
					g.setColor(linkColor);
				} else if ((style & STYLE_MONOSPACE) != 0) {
					g.setColor(monospaceColor);
				}
				if (EMOJI_SUPPORT && font == null) {
					emoji: {
						img: {
							if (text == null || ++renderedEmojis >= MP.maxLoadedEmojis) break img;

							Object img = null;
							if (emojiTable.containsKey(text)) {
								img = emojiTable.get(text);
								if (img == MP.json_null) break img;
							} else {
								if (++loadedEmojis >= 8) break img;
								try {
									int s = emojiTable.size();
									if (s > MP.maxLoadedEmojis) {
										Enumeration e = emojiTable.keys();

										int m = MP.maxLoadedEmojis >> 1;
										while (s > m && e.hasMoreElements()) {
											emojiTable.remove(e.nextElement());
											s--;
										}
									}
								} catch (Exception ignored) {}

								try {
									img = Image.createImage(text);
								} catch (Throwable ignored) {}
								if (img == null) {
									obj[0] = null;
									emojiTable.put(text, MP.json_null);
									break img;
								}
								emojiTable.put(text, img);
							}

							g.drawImage((Image) img, tx, ty, 0);
							break emoji;
						}
						g.fillRect(tx, ty, 16, 16);
					}
				} else {
					g.setFont(font);
					g.drawString(text, tx, ty, 0);
				}
				if ((style & STYLE_STRIKETHROUGH) != 0) {
					int ly = ty + (th >> 1) + 1;
					g.drawLine(tx, ly, tx + tw, ly);
				}
			}
			if (focus && selectedParts != null && selectedParts.contains(obj)) {
				g.setColor(focusColor);
				g.drawRect(tx, ty, tw, th);
				g.setColor(color);
			} else if ((style & STYLE_LINK) != 0 || (style & STYLE_MONOSPACE) != 0) {
				g.setColor(color);
			}
		}
	}

	public synchronized int layout(int width) {
		if (!layoutRequest && layoutWidth == width) {
			return contentHeight;
		}
		layoutWidth = width;
		width -= 4;

		if (render == null) {
			render = new Vector();
		} else render.removeAllElements();
		if (urls == null) {
			urls = new Vector();
		} else urls.removeAllElements();

		Vector res = render;
		int x = 0, y = 0, idx = 0, mw = 0;

		boolean center = this.center;

		int fh = 0;
		int l = parsed.size();
		boolean ellipsis = this.ellipsis;
		int[] out = new int[4];
		for (int ei = 0; ei < l; ++ei) {
			int startIdx = idx;
			Object[] e = (Object[]) parsed.elementAt(ei);
			String text = (String) e[0];
			Font font = (Font) e[1];
			String url = (String) e[2];
			int style = e[3] == null ? 0 : ((int[]) e[3])[0];

			if (font == null) {
				if (fh < 16) fh = 16;
				if (x + 18 >= width) {
					x = 0;
					y += fh;
				}
				res.addElement(new Object[] { text, null, url, new int[] {x, y + fh - 16, 16, 16, style} });
				x += 17;
				mw = Math.max(mw, x);
				continue;
			}

			String url2 = url;
			if ("!".equals(url)) url = null;

			fh = MP.getFontHeight(font);
			if (text == null || "\n".equals(text)) {
				if (ellipsis) {
					if (x != 0) x += font.charWidth(' ');
				} else {
					x = 0;
					y += fh;
				}
				continue;
			}

			int ch = 0;
			int sl = text.length();
			char c;
			while (ch < sl && ((c = text.charAt(ch)) < ' ')) {
				ch++;
				if (c != '\n') continue;
				if (ellipsis) {
					if (x != 0) x += font.charWidth(' ');
				} else {
					x = 0;
					y += fh;
				}
			}

			if (ellipsis) {
				int tw;
				boolean end = false;
				if (x + (tw = font.stringWidth(text = text.substring(ch).replace('\n', ' '))) >= width) {
					tw = font.stringWidth(text = ellipsis(text, font, width - x));
					end = true;
				}
				res.addElement(new Object[] { text, font, url, new int[] {x, y, tw, fh, style} });
				x += tw;
				if (end) break;
			} else if (text.indexOf('\n', ch) == -1) {
				split(text, font, url, width, x, y, idx, mw, ch, sl, fh, res, center, style, out);
				x = out[0]; y = out[1]; idx = out[2]; mw = out[3];
			} else {
				int j = ch;
				for (int i = ch; i < sl; ++i) {
					if ((c = text.charAt(i)) == '\n') {
						split(text, font, url, width, x, y, idx, mw, j, i, fh, res, center, style, out);
						x = 0; y = out[1] + fh; idx = out[2]; mw = out[3];
						j = i + 1;
					}
				}
				if (j != sl) {
					split(text, font, url, width, x, y, idx, mw, j, sl, fh, res, center, style, out);
					x = out[0]; y = out[1]; idx = out[2]; mw = out[3];
				}
			}

			if (url2 != null && (url != null || !spoilersUnhidden)) {
				Vector v = new Vector();
				for (int i = startIdx; i < idx; ++i) {
					v.addElement(res.elementAt(i));
				}
				urls.addElement(new Object[] {url2, v});
			}
		}
		if (center) centerRow(width, 0, x, y, res);

		contentWidth = y == 0 ? x : mw;
		return contentHeight = y + fh;
	}

	boolean grabFocus(int dir) {
		if (!focusable || urls.size() == 0) return false;
		focus = true;
		if (dir != 0) {
			focusIndex = dir == -1 ? urls.size() - 1 : 0;
		}
		focusLink(focusIndex);
		return true;
	}

	void focusLink(int idx) {
		Object[] o = (Object[]) urls.elementAt(idx);
		selectedUrl = (String) o[0];
		selectedParts = (Vector) o[1];
	}

	int traverse(int dir) {
		if (!focusable || urls.size() == 0) return Integer.MIN_VALUE;

		int next;

		if (dir == Canvas.UP) {
			if (focusIndex <= 0) {
				focusLink(focusIndex = 0);
				if (getVisibility(getLinkPos(0, 0)) == 1) {
					return 0;
				}
				return Integer.MIN_VALUE;
			}

			next = -1;
		} else if (dir == Canvas.DOWN) {
			if (focusIndex >= urls.size() - 1) {
				focusLink(focusIndex = urls.size() - 1);
				if (getVisibility(getLinkPos(focusIndex, selectedParts.size() - 1)) == -1) {
					return 0;
				}
				return Integer.MIN_VALUE;
			}

			next = 1;
		} else {
			return Integer.MIN_VALUE;
		}

		int[] pos = getLinkPos(focusIndex, next == 1 ? selectedParts.size() - 1 : 0);
		if (getVisibility(pos) == -next) {
			return 0;
		}

		focusLink(focusIndex = focusIndex + next);

		pos = getLinkPos(focusIndex, next == 1 ? selectedParts.size() - 1 : 0);
		if (getVisibility(pos) == -next) {
			return 0;
		}
		return Integer.MAX_VALUE;
	}

	boolean action() {
		if (!focusable || selectedUrl == null) return false;
		if (selectedUrl.startsWith("!")) {
			// unhide spoilers
			selectedUrl = selectedUrl.substring(1);
			if (!spoilersUnhidden) {
				int i = urls.size() - 1;
				do {
					if ("!".equals(((Object[]) urls.elementAt(i))[0])) {
						urls.removeElementAt(i);
					}
				} while (i-- != 0);
				spoilersUnhidden = true;
				selectedUrl = null;
				selectedParts = null;
				focusIndex = 0;
				return true;
			}
		}
		if (selectedUrl.length() == 0) {
			return false;
		}
		MP.openUrl(selectedUrl, true);
		return true;
	}

	boolean tap(int x, int y, boolean longTap) {
		if (longTap) return false;
		int idx = getUrlAt(x, y);
		if (idx != -1) {
			focusLink(idx);
			action();
			return true;
		}
		return false;
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

	int[] getLinkPos(int idx, int partIdx) {
		return (int[]) ((Object[]) ((Vector) ((Object[]) urls.elementAt(idx))[1]).elementAt(partIdx))[3];
	}

	private int getVisibility(int[] pos) {
		UIItem root = (UIItem) container;
		MPCanvas chat = (MPCanvas) root.container;
		int t = (chat.reverse ? chat.height - chat.bottom + chat.scroll - (root.y + root.contentHeight)
				: chat.top - chat.scroll + root.y) + this.y + pos[1];

		int off = chat.clipHeight / 8;
		return (t + pos[3]) <= chat.top + off ? 1 : t >= chat.height - chat.bottom - off ? -1 : 0;
	}

	static String ellipsis(String text, Font font, int width) {
		if (text.indexOf('\n') != -1) {
			text = text.replace('\n', ' ');
		}
		if (font.stringWidth(text) < width) return text;
		int l = text.length();
		width -= font.stringWidth("...");
		for (int i = 1; i < l; ++i) {
			if (font.substringWidth(text, 0, i) > width) {
				return text.substring(0, i - 1).concat("...");
			}
		}
		return "...";
	}

	private static boolean notEmpty(String s) {
		if (s == null) return false;

		int l = s.length();
		if (l == 0) return false;
		if (l > 3) return true;

		int i = 0;
		while (i < l) {
			if (s.charAt(i++) > ' ') return true;
		}

		return false;
	}

	private static void split(String text, Font font, String url, int width, int x, int y, int idx, int mw, int ch, int sl, int fh, Vector res, boolean center, int style, int[] out) {
		int dy = 0;
		if (res.size() != 0 && x != 0) {
			Font f = font;
			for (int i = res.size() - 1; i >= 0; --i) {
				int[] bounds = (int[]) ((Object[]) res.elementAt(i))[3];
				if (bounds[1] != y) break;
				f = (Font) ((Object[]) res.elementAt(i))[1];
			}
			dy = (f == null ? 16 : f.getBaselinePosition()) - font.getBaselinePosition();
		}
		if (ch != sl) {
			int ew = font.substringWidth(text, ch, sl - ch);
			if (x + ew < width) {
				String t = text.substring(ch, sl);
				if (notEmpty(t)) {
					res.addElement(new Object[] { t, font, url, new int[] {x, y + dy, ew, fh, style} });
					idx ++;
				}
				x += ew;
				mw = Math.max(mw, x);
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
									if (notEmpty(t)) {
										res.addElement(new Object[] { t, font, url, new int[] {x, y + dy, tw, fh, style} });
										idx ++;
									}
									mw = Math.max(mw, x + tw);
									x = 0; y += fh; dy = 0;
			
									i = ch = j;
									break w;
								}
							}

							String t = text.substring(ch, i);
							int tw = font.stringWidth(t);
							if (center) {
								x = centerRow(width, tw, x, y, res);
							}
							if (notEmpty(t)) {
								res.addElement(new Object[] { t, font, url, new int[] {x, y + dy, tw, fh, style} });
								idx ++;
							}
							mw = Math.max(mw, x + tw);
							x = 0; y += fh; dy = 0;
							ch = i;
						}
					}
				}
				if (ch != sl) {
					String t = text.substring(ch, sl);
					int tw = font.stringWidth(t);
					if (center) {
						x = centerRow(width, tw, x, y, res);
					}
					if (notEmpty(t)) {
						res.addElement(new Object[] { t, font, url, new int[] {x, y + dy, tw, fh, style} });
						idx ++;
					}
					x += tw;
					mw = Math.max(mw, x);
				}
			}
		}
		out[0] = x; out[1] = y; out[2] = idx; out[3] = mw;
	}

	private static int centerRow(int width, int t, int x, int y, Vector res) {
		int rw = (width - (x + t)) / 2;
		x += rw;
		for (int k = res.size() - 1; k >= 0; --k) {
			Object[] obj = (Object[]) res.elementAt(k);
			if (((int[]) obj[3])[1] == y) {
				((int[]) obj[3])[0] += rw;
			} else break;
		}
		return x;
	}

}
//#endif
