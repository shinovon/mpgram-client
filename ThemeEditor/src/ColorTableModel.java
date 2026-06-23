import java.awt.Color;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableModel;

import cc.nnproject.json.JSONObject;
import cc.nnproject.json.JSONStream;

public class ColorTableModel implements TableModel {
	
	static final Object[] COLORS_ENUM = {
		    "CHAT_BG", 0,
		    "CHAT_FG", 1,
		    "CHAT_HIGHLIGHT_BG", 2,
		    "CHAT_PANEL_BG", 3,
		    "CHAT_PANEL_FG", 4,
		    "CHAT_PANEL_BORDER", 5,
		    "CHAT_MENU_BG", 6,
		    "CHAT_MENU_HIGHLIGHT_BG", 7,
		    "CHAT_MENU_FG", 8,
		    "CHAT_MENU_SEPARATOR", 9,
		    "CHAT_STATUS_FG", 10,
		    "CHAT_STATUS_HIGHLIGHT_FG", 11,
		    "CHAT_POINTER_HOLD", 12,
		    "CHAT_INPUT_ICON", 13,
		    "CHAT_SEND_ICON", 14,
		    "CHAT_INPUT_BORDER", 15,
		    "CHAT_SCROLLBAR", 16,

		    "MESSAGE_BG", 20,
		    "MESSAGE_OUT_BG", 21,
		    "MESSAGE_FG", 22,
		    "MESSAGE_LINK", 23,
		    "MESSAGE_LINK_FOCUS", 24,
		    "MESSAGE_SENDER", 25,
		    "MESSAGE_ATTACHMENT_BORDER", 26,
		    "MESSAGE_ATTACHMENT_TITLE", 27,
		    "MESSAGE_ATTACHMENT_SUBTITLE", 28,
		    "MESSAGE_ATTACHMENT_FOCUS_BG", 29,
		    "MESSAGE_COMMENT_BORDER", 30,
		    "MESSAGE_IMAGE", 31,
		    "MESSAGE_FOCUS_BORDER", 32,
		    "MESSAGE_TIME", 33,
		    "MESSAGE_OUT_TIME", 34,
		    "ACTION_BG", 35,
		    "MESSAGE_OUT_READ", 36,
		    "MESSAGE_VOICE_WAVEFORM", 37,

		    "CHATS_BG", 40,
		    "CHATS_ITEM_HIGHLIGHT_BG", 41,
		    "CHATS_ITEM_HIGHLIGHT_FG", 42,
		    "CHATS_ITEM_TITLE", 43,
		    "CHATS_ITEM_TEXT", 44,
		    "CHATS_ITEM_MEDIA", 45,
		    "CHATS_ITEM_SEPARATOR", 46,
		    "CHATS_ITEM_UNREAD_BG", 47,
		    "CHATS_ITEM_UNREAD_FG", 48,
		    "CHATS_ITEM_UNREAD_MUTED_BG", 49
	};
	
	private static int rows;
	private static ArrayList<String> keys;
	private static ArrayList<Integer> indexes;
	static Map<Integer, Integer> map;

	ColorTableModel() {
		init();
	}

	void init() {
		map = new HashMap<Integer, Integer>();
		keys = new ArrayList<String>();
		indexes = new ArrayList<Integer>();
		int count = 0;
		for (int i = 0; i < COLORS_ENUM.length; i += 2) {
			String n = (String) COLORS_ENUM[i];
			int c = (int) COLORS_ENUM[i + 1];
			keys.add(n);
			indexes.add(c);
			map.put(c, 0);
			count++;
		}
		rows = count;
		updateTable();
	}

	public void addTableModelListener(TableModelListener arg0) {}

	public Class<?> getColumnClass(int arg0) {
		return String.class;
	}

	public int getColumnCount() {
		return 2;
	}

	public String getColumnName(int columnIndex) {
		return columnIndex == 0 ? "Key" : "Value";
	}

	public int getRowCount() {
		return rows;
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		if(columnIndex == 0) {
			return keys.get(rowIndex);
		} else {
			int i = map.get(indexes.get(rowIndex));
			if((i & 0xFF000000) == 0) {
				i |= 0xFF000000;
			}
			String s = Integer.toHexString(i);
			s = s.toUpperCase();
			while(s.length() < 6) {
				s = "0" + s;
			}
			if(s.startsWith("FF")) {
				s = s.substring(2);
			}
			return s = "#" + s;
		}
		
	}

	public boolean isCellEditable(int arg0, int columnIndex) {
		if(columnIndex == 0)
			return false;
		else
			return true;
	}

	public void removeTableModelListener(TableModelListener arg0) {
	}

	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		if(columnIndex == 0)
			return;
		if(aValue instanceof Color) {
			Color c = (Color) aValue;
			int i = c.getRGB();
			if((i & 0xFF000000) == 0) {
				i |= 0xFF000000;
			}
			map.put(indexes.get(rowIndex), i);
			return;
		}
		int i = 0;
		String s = (String) aValue;
		s = s.trim().toUpperCase();
		i = Integer.decode(s);
		if((i & 0xFF000000) == 0) {
			i |= 0xFF000000;
		}
		map.put(indexes.get(rowIndex), i);
	}

	public void load(Path path) throws IOException {
		JSONStream stream = JSONStream.getStream(Files.newInputStream(path));
		try {
			JSONObject json = stream.nextObject();
			for (String key: keys) {
				String s = json.getString(key, null);
				if (s == null) {
					System.out.println("Missing: " + key);
				} else if (s.startsWith("0x")) {
					map.put(indexes.get(keys.indexOf(key)), Integer.parseInt(s.substring(2), 16));
				} else {
					System.out.println("Ignored: " + key + "=" + s);
				}
			}
		} finally {
			stream.close();
		}
//		JFileChooser fc = new JFileChooser(".");
//		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
//		fc.setFileFilter(new FileFilter() {
//			public boolean accept(File f) {
//		        return f.isDirectory() || f.getName().startsWith("jtthm_");
//		    }
//
//			public String getDescription() {
//				return "User theme files";
//			}
//		});
//		int c = fc.showOpenDialog(ui.frame);
//		if(c == JFileChooser.CANCEL_OPTION) {
//			return;
//		}
//
//		try {
//			File f = fc.getSelectedFile();
//			
//			FileInputStream is = new FileInputStream(f);
//			DataInputStream d = new DataInputStream(is);
//			ui.idField.setText(f.getName().substring("jthm_".length()));
//			try {
//				int i;
//				boolean a = false;
//				while((i = d.readShort()) != -1) {
//					if(i == 0 && !a) {
//						ui.authorField.setText(d.readUTF());
//						a = true;
//						continue;
//					}
//					if(i == 0) {
//						d.readUTF();
//						continue;
//					}
//					int x = d.readInt();
//					if(b) map.put(i, x);
//					else map.put(i, x);
//				}
//			} catch (IOException e) {
//				e.printStackTrace();
//			} finally {
//				d.close();
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		} 
		updateTable();
	}
	
	public Color getColor(String key) {
		return new Color(map.get(indexes.get(keys.indexOf(key))));
	}
	
	private void updateTable() {
		if (ThemeEditor.table == null) return;
		ThemeEditor.table.setModel(new TableModel() {

			public void addTableModelListener(TableModelListener arg0) {
			}

			public Class<?> getColumnClass(int arg0) {
				return null;
			}

			public int getColumnCount() {
				return 0;
			}

			public String getColumnName(int arg0) {
				return null;
			}

			public int getRowCount() {
				return 0;
			}

			public Object getValueAt(int arg0, int arg1) {
				return null;
			}

			public boolean isCellEditable(int arg0, int arg1) {
				return false;
			}

			public void removeTableModelListener(TableModelListener arg0) {
			}

			public void setValueAt(Object arg0, int arg1, int arg2) {
			}
			
		});
		ThemeEditor.table.setModel(this);
		ThemeEditor.table.repaint();
	}

}
