import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;

public class ColorTableEditor extends DefaultCellEditor {

	public ColorTableEditor() {
        super(new JTextField());
	}

	public boolean stopCellEditing() {
		try {
			String s = (String) getCellEditorValue();
			s = s.trim().toUpperCase();
			if(s.length() == 0) {
				return false;
			}
			Color.decode(s);
		} catch (NumberFormatException e) {
			return false;
		} catch (ClassCastException e) {
			return false;
		}
		return super.stopCellEditing();
	}
	
	public Component getTableCellEditorComponent(
			JTable table, Object value, boolean isSelected, int row, int column) {
		if (column == 1) {
			JPanel p = new JPanel();
			BorderLayout l = new BorderLayout();
			l.setVgap(0);
			p.setLayout(l);
			JComponent ec = (JComponent) super.getTableCellEditorComponent(table, value, isSelected, row, column);
			ec.setPreferredSize(new Dimension(80, 16));
			ec.requestFocus();
			p.add(ec, BorderLayout.CENTER);
			JPanel pc = new JPanel();
			pc.setBorder(new LineBorder(Color.black));
			FlowLayout l2 = (FlowLayout) pc.getLayout();
			l2.setVgap(0);
			l2.setHgap(0);
			p.add(pc, BorderLayout.EAST);
			pc.setPreferredSize(new Dimension(16, 16));
			Color c = Color.decode((String) value);
			pc.addMouseListener(new MouseListener() {

				@Override
				public void mouseClicked(MouseEvent paramMouseEvent) {
					JColorChooser jc = new JColorChooser();
					jc.setColor(c);
					jc.getSelectionModel().addChangeListener(new ChangeListener() {
						public void stateChanged(ChangeEvent paramChangeEvent) {
							table.setValueAt(jc.getColor(), row, column);
						}
					});
					JDialog d = new JDialog(SwingUtilities.windowForComponent(p));
					d.add(jc);
					d.pack();
					d.setVisible(true);
				}

				@Override
				public void mouseEntered(MouseEvent paramMouseEvent) {
				}

				@Override
				public void mouseExited(MouseEvent paramMouseEvent) {
				}

				@Override
				public void mousePressed(MouseEvent paramMouseEvent) {
				}

				@Override
				public void mouseReleased(MouseEvent paramMouseEvent) {
				}

			});
			pc.setBackground(c);
			return p;
		}
		return super.getTableCellEditorComponent(table, value, isSelected, row, column);
	}

}
