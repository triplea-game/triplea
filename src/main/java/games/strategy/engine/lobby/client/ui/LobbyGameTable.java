package games.strategy.engine.lobby.client.ui;

import java.awt.Component;
import java.awt.Font;

import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

import games.strategy.engine.lobby.server.GameDescription;

class LobbyGameTable extends JTable {
  private static final long serialVersionUID = 8632519876114231003L;
  private static final Font defaultFont = UIManager.getDefaults().getFont("Table.font");
  private static final Font italicFont = new Font(defaultFont.getFamily(), Font.ITALIC, defaultFont.getSize());

  LobbyGameTable(final TableRowSorter<LobbyGameTableModel> tableSorter) {
    super(tableSorter.getModel());
    setRowSorter(tableSorter);
  }

  @Override
  public Component prepareRenderer(final TableCellRenderer renderer, final int rowIndex, final int colIndex) {
    final Component component = super.prepareRenderer(renderer, rowIndex, colIndex);
    final LobbyGameTableModel lobbyGameTableModel = (LobbyGameTableModel) this.getModel();
    final GameDescription gameDescription = lobbyGameTableModel.get(convertRowIndexToModel(rowIndex));
    if (gameDescription.getBotSupportEmail() != null && gameDescription.getBotSupportEmail().length() > 0) {
      component.setFont(italicFont);
    } else {
      component.setFont(defaultFont);
    }
    return component;
  }
}
