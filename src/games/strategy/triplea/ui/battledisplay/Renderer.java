package games.strategy.triplea.ui.battledisplay;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;

class Renderer implements TableCellRenderer {
  JLabel m_stamp = new JLabel();

  @Override
  public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
      final boolean hasFocus, final int row, final int column) {
    ((TableData) value).updateStamp(m_stamp);
    return m_stamp;
  }
}
