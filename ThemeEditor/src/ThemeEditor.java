import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JLabel;
import java.awt.Dimension;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JScrollPane;
import javax.swing.JComboBox;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class ThemeEditor {

	private JFrame frame;
	static JTable table;
	private JPanel previewCanvas;
	private ColorTableModel model;
	protected int selectedDemo;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ThemeEditor window = new ThemeEditor();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public ThemeEditor() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 911, 689);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JPanel panel_2 = new JPanel();
		frame.getContentPane().add(panel_2, BorderLayout.NORTH);
		
		JSplitPane splitPane = new JSplitPane();
		splitPane.setResizeWeight(1.0);
		frame.getContentPane().add(splitPane, BorderLayout.CENTER);
		
		JPanel panel_1 = new JPanel();
		splitPane.setRightComponent(panel_1);
		panel_1.setLayout(new BorderLayout(0, 0));
		
		JPanel panel_4 = new JPanel();
		FlowLayout flowLayout_2 = (FlowLayout) panel_4.getLayout();
		flowLayout_2.setAlignment(FlowLayout.LEFT);
		panel_1.add(panel_4, BorderLayout.SOUTH);
		
		JComboBox comboBox = new JComboBox(new String[] { "Chats Demo 1", "Chat Demo 1" });
		comboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				selectedDemo = comboBox.getSelectedIndex();
				previewCanvas.repaint();
			}
		});
		panel_4.add(comboBox);
		
		JPanel panel_3 = new JPanel();
		FlowLayout flowLayout = (FlowLayout) panel_3.getLayout();
		flowLayout.setAlignment(FlowLayout.LEFT);
		panel_1.add(panel_3, BorderLayout.CENTER);
		
		previewCanvas = new JPanel() {
			@Override
			public void paint(Graphics g) {
				int w = this.getWidth();
				int h = this.getHeight();
				try {
					switch (selectedDemo) {
					case 0: {
						g.setColor(model.getColor("CHATS_BG"));
						g.fillRect(0, 0, w, h);
						
						break;
					}
					case 1: {
						int th = 60;
						int bottom = 60;
						g.setColor(model.getColor("CHAT_BG"));
						g.fillRect(0, 0, w, h);
						g.setColor(model.getColor("CHAT_PANEL_BG"));
						g.fillRect(0, 0, w, th);
						g.setColor(model.getColor("CHAT_PANEL_BORDER"));
						g.fillRect(0, 0, w, 1);
						
						int tx = 60;
						int tw = w - 100;
						int bty = (th - 2) >> 1;
						g.setColor(model.getColor("CHAT_PANEL_FG"));
						// back icon
//						if (backIcon != null) {
//							int ax = (tx - 32) >> 1;
//							int ay = (th - 32) >> 1;
//							g.drawImage(backIcon, ax, ay, 0);
//						} else
						{
							int bx = (tx - 16) >> 1;
							g.fillRect(bx, bty, 16, 2);
							g.drawLine(bx, bty, bx + 7, bty-7);
							g.drawLine(bx, bty + 1, bx + 8, bty-7);
							g.drawLine(bx, bty, bx + 8, bty+8);
							g.drawLine(bx, bty + 1, bx + 7, bty+8);
						}
						
						int bw = 4;
						int bx = w - tx + ((tx - 3) >> 1);
						g.fillRect(bx, bty - (bw * 2), bw, bw);
						g.fillRect(bx, bty, bw, bw);
						g.fillRect(bx, bty + (bw * 2), bw, bw);
						
						g.setColor(model.getColor("CHAT_PANEL_BG"));
						g.fillRect(0, h - bottom, w, bottom);
						g.setColor(model.getColor("CHAT_PANEL_BORDER"));
						g.fillRect(0, h - bottom, w, 1);
						break;
					}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		panel_3.add(previewCanvas);
		previewCanvas.setPreferredSize(new Dimension(240, 320));
		previewCanvas.setMaximumSize(new Dimension(360, 640));
		previewCanvas.setMinimumSize(new Dimension(240, 320));
		
		JPanel panel_5 = new JPanel();
		FlowLayout flowLayout_1 = (FlowLayout) panel_5.getLayout();
		flowLayout_1.setAlignment(FlowLayout.LEFT);
		panel_1.add(panel_5, BorderLayout.NORTH);
		
		JLabel lblNewLabel = new JLabel("Preview");
		panel_5.add(lblNewLabel);
		
		JPanel panel = new JPanel();
		splitPane.setLeftComponent(panel);
		panel.setLayout(new BorderLayout(0, 0));
		
		JScrollPane scrollPane = new JScrollPane();
		panel.add(scrollPane, BorderLayout.CENTER);
		
		table = new JTable(model = new ColorTableModel());
		scrollPane.setViewportView(table);
		table.getColumnModel().getColumn(1).setWidth(120);
		table.setDefaultEditor(Object.class, new ColorTableEditor());
		
		try {
			model.load(Paths.get("..", "themes", "tint.json").toAbsolutePath());
			previewCanvas.repaint();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
