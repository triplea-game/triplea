package games.strategy.engine.lobby.client.ui;

import java.awt.Component;
import java.awt.Font;

import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import games.strategy.engine.lobby.server.GameDescription;
import games.strategy.net.GUID;

class LobbyGameTable extends JTable {
  private static final long serialVersionUID = 8632519876114231003L;
  private GUID selectedGame;
  private boolean inTableChange = false;
  private final Font defaultFont = UIManager.getDefaults().getFont("Table.font");
  private final Font italicFont = new Font(defaultFont.getFamily(), Font.ITALIC, defaultFont.getSize());

  LobbyGameTable(final TableModel model) {
    super(model);
    getSelectionModel().addListSelectionListener(e -> {
      if (!inTableChange) {
        markSelection();
      }
    });
  }

  @Override
  public Component prepareRenderer(final TableCellRenderer renderer, final int rowIndex, final int colIndex) {
    final Component c = super.prepareRenderer(renderer, rowIndex, colIndex);
    if (this.dataModel instanceof TableSorter) {
      final TableSorter tmodel = (TableSorter) this.dataModel;
      if (tmodel.getTableModel() instanceof LobbyGameTableModel) {
        final LobbyGameTableModel lmodel = (LobbyGameTableModel) tmodel.getTableModel();
        final int row = tmodel.getUnderlyingModelRowAt(rowIndex);
        final GameDescription gd = lmodel.get(row);
        if (gd.getBotSupportEmail() != null && gd.getBotSupportEmail().length() > 0) {
          c.setFont(italicFont);
        } else {
          c.setFont(defaultFont);
        }
      }
    }
    return c;
  }

  /**
   * The sorting model will loose the currently selected row.
   * So we need to restore the selection after it has updated
   */
  @Override
  public void tableChanged(final TableModelEvent e) {
    inTableChange = true;
    try {
      super.tableChanged(e);
    } finally {
      inTableChange = false;
    }
    restoreSelection();
  }

  /**
   * record the id of the currently selected game.
   */
  private void markSelection() {
    final int selected = getSelectedRow();
    if (selected >= 0) {
      selectedGame = (GUID) getModel().getValueAt(selected, LobbyGameTableModel.Column.GUID.ordinal());
    } else {
      selectedGame = null;
    }
  }

  /**
   * Restore the selection to the marked value.
   */
  private void restoreSelection() {
    if (selectedGame == null) {
      return;
    }
    for (int i = 0; i < getModel().getRowCount(); i++) {
      final GUID current = (GUID) getModel().getValueAt(i, LobbyGameTableModel.Column.GUID.ordinal());
      if (current.equals(selectedGame)) {
        getSelectionModel().setSelectionInterval(i, i);
        break;
      }
    }
  }
}
