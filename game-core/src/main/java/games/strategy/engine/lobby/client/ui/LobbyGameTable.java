package games.strategy.engine.lobby.client.ui;

import java.awt.Component;
import java.awt.Font;
import java.util.Collections;

import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

import com.google.common.base.Strings;

import games.strategy.engine.lobby.server.GameDescription;

class LobbyGameTable extends JTable {
  private static final long serialVersionUID = 8632519876114231003L;
  private static final Font DEFAULT_FONT = UIManager.getDefaults().getFont("Table.font");
  private static final Font ITALIC_FONT = DEFAULT_FONT.deriveFont(Font.ITALIC);

  LobbyGameTable(final LobbyGameTableModel gameTableModel) {
    super(gameTableModel);
    final TableRowSorter<LobbyGameTableModel> tableSorter = new TableRowSorter<>(gameTableModel);
    // by default, sort newest first
    final int dateColumn = gameTableModel.getColumnIndex(LobbyGameTableModel.Column.Started);
    tableSorter.setSortKeys(Collections.singletonList(new RowSorter.SortKey(dateColumn, SortOrder.DESCENDING)));
    setRowSorter(tableSorter);
  }

  @Override
  public Component prepareRenderer(final TableCellRenderer renderer, final int rowIndex, final int colIndex) {
    final Component component = super.prepareRenderer(renderer, rowIndex, colIndex);
    final LobbyGameTableModel lobbyGameTableModel = (LobbyGameTableModel) getModel();
    final GameDescription gameDescription = lobbyGameTableModel.get(convertRowIndexToModel(rowIndex));
    final boolean isHostBot = Strings.nullToEmpty(gameDescription.getBotSupportEmail()).length() > 0;
    component.setFont(isHostBot ? ITALIC_FONT : DEFAULT_FONT);
    return component;
  }
}
