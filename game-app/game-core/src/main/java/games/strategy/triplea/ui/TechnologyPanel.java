package games.strategy.triplea.ui;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.events.GameDataChangeListener;
import games.strategy.engine.stats.IStat;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.TechTracker;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Image;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import org.jetbrains.annotations.NotNull;

class TechnologyPanel extends JPanel implements GameDataChangeListener {
  private static final long serialVersionUID = 4340684166664492498L;

  IStat[] stats;
  private final Map<GamePlayer, ImageIcon> mapPlayerImage = new HashMap<>();
  protected GameData gameData;
  private final UiContext uiContext;
  private final TechTableModel techModel;

  TechnologyPanel(final GameData data, final UiContext uiContext) {
    this.gameData = data;
    this.uiContext = uiContext;
    techModel = new TechTableModel();
    fillPlayerIcons();
    initLayout();
    gameData.addDataChangeListener(this);
  }

  protected void initLayout() {
    setLayout(new GridLayout(1, 1));
    add(new JScrollPane(createTechTable()));
  }

  private JTable createTechTable() {
    final JTable techTable = new JTable(techModel);
    techTable.getTableHeader().setReorderingAllowed(false);
    techTable.getColumnModel().getColumn(0).setPreferredWidth(500);
    // show icons for players:
    final TableCellRenderer componentRenderer = new JComponentTableCellRenderer();
    for (int i = 1; i < techTable.getColumnCount(); i++) {
      final TableColumn column = techTable.getColumnModel().getColumn(i);
      column.setHeaderRenderer(componentRenderer);
      final String player = techTable.getColumnName(i);
      final JLabel value = new JLabel("", getIcon(player), SwingConstants.CENTER);
      value.setToolTipText(player);
      column.setHeaderValue(value);
    }
    // show tooltip for technology names
    final TableCellRenderer techNameComponentRenderer = new TechNameComponentRenderer();
    final TableColumn techNameColumn = techTable.getColumnModel().getColumn(0);
    techNameColumn.setHeaderRenderer(techNameComponentRenderer);
    techNameColumn.setCellRenderer(techNameComponentRenderer);
    return techTable;
  }

  static final class TechNameComponentRenderer extends DefaultTableCellRenderer {
    @Override
    public void setValue(Object aValue) {
      setToolTipText(aValue.toString());
      super.setValue(aValue);
    }
  }

  public void setGameData(final GameData data) {
    gameData.removeDataChangeListener(this);
    gameData = data;
    gameData.addDataChangeListener(this);
    gameDataChanged(null);
  }

  @Override
  public void gameDataChanged(final Change change) {
    techModel.markDirty();
    SwingUtilities.invokeLater(this::repaint);
  }

  /**
   * Gets the small flag for a given PlayerId.
   *
   * @param player the player to get the flag for
   * @return ImageIcon small flag
   */
  protected ImageIcon getIcon(final GamePlayer player) {
    ImageIcon icon = mapPlayerImage.get(player);
    if (icon == null && uiContext != null) {
      final Image img = uiContext.getFlagImageFactory().getSmallFlag(player);
      icon = new ImageIcon(img);
      icon.setDescription(player.getName());
      mapPlayerImage.put(player, icon);
    }
    return icon;
  }

  protected @Nullable ImageIcon getIcon(final String playerName) {
    final GamePlayer player = gameData.getPlayerList().getPlayerId(playerName);
    if (player == null) {
      return null;
    }
    return getIcon(player);
  }

  private void fillPlayerIcons() {
    for (final GamePlayer p : gameData.getPlayerList().getPlayers()) {
      getIcon(p);
    }
  }

  static class JComponentTableCellRenderer implements TableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(
        final JTable table,
        final Object value,
        final boolean isSelected,
        final boolean hasFocus,
        final int row,
        final int column) {
      return (JComponent) value;
    }
  }

  class TechTableModel extends AbstractTableModel {
    private static final long serialVersionUID = -4612476336419396081L;
    /* Flag to indicate whether data needs to be recalculated */
    private boolean isDirty = true;
    /* Column Header Names */
    /* Row Header Names */
    private String[] colList;
    /* Underlying data for the table */
    private final String[][] data;
    /* Convenience mapping of country names -> col */
    private final Map<String, Integer> colMap = new HashMap<>();
    /* Convenience mapping of technology names -> row */
    private final Map<String, Integer> rowMap = new HashMap<>();

    TechTableModel() {
      initColList();
      /* Load the country -> col mapping */
      for (int i = 0; i < colList.length; i++) {
        colMap.put(colList[i], i + 1);
      }
      data = getDataAndInitRowMap();
      clearAdvances();
    }

    private synchronized String[] @NotNull [] getDataAndInitRowMap() {
      final String[][] dataTable;
      boolean useTech = false;
      // copy so that the object doesn't change underneath us
      final GameData gameDataSync = TechnologyPanel.this.gameData;
      try (GameData.Unlocker ignored = gameDataSync.acquireReadLock()) {
        final int numTechs =
            TechAdvance.getTechAdvances(gameDataSync.getTechnologyFrontier()).size();
        if (gameDataSync.getResourceList().getResource(Constants.TECH_TOKENS) != null) {
          useTech = true;
          dataTable = new String[numTechs + 1][colList.length + 2];
        } else {
          dataTable = new String[numTechs][colList.length + 1];
        }
      }
      /* Load the technology -> row mapping */
      int row = 0;
      if (useTech) {
        rowMap.put("Tokens", row);
        dataTable[row][0] = "Tokens";
        row++;
      }
      final List<TechAdvance> techAdvances =
          TechAdvance.getTechAdvances(gameDataSync.getTechnologyFrontier(), null);
      for (final TechAdvance tech : techAdvances) {
        rowMap.put(tech.getName(), row);
        dataTable[row][0] = tech.getName();
        row++;
      }
      return dataTable;
    }

    private void clearAdvances() {
      /* Initialize the table with the tech names */
      for (int i = 0; i < data.length; i++) {
        for (int j = 1; j <= colList.length; j++) {
          data[i][j] = "";
        }
      }
    }

    private void initColList() {
      final List<GamePlayer> players = gameData.getPlayerList().getPlayers();
      colList = new String[players.size()];
      for (int i = 0; i < players.size(); i++) {
        colList[i] = players.get(i).getName();
      }
      Arrays.sort(colList);
    }

    synchronized void update() {
      clearAdvances();
      // copy so that the object doesn't change underneath us
      final GameData gameDataSync = TechnologyPanel.this.gameData;
      try (GameData.Unlocker ignored = gameDataSync.acquireReadLock()) {
        for (final GamePlayer pid : gameDataSync.getPlayerList().getPlayers()) {
          if (colMap.get(pid.getName()) == null) {
            throw new IllegalStateException(
                "Unexpected player in GameData.getPlayerList()" + pid.getName());
          }
          final int col = colMap.get(pid.getName());
          int row = 0;
          if (gameDataSync.getResourceList().getResource(Constants.TECH_TOKENS) != null) {
            final int tokens = pid.getResources().getQuantity(Constants.TECH_TOKENS);
            data[row][col] = Integer.toString(tokens);
          }
          final List<TechAdvance> advancesAll =
              TechAdvance.getTechAdvances(gameDataSync.getTechnologyFrontier());
          final List<TechAdvance> has =
              TechAdvance.getTechAdvances(gameDataSync.getTechnologyFrontier(), pid);
          for (final TechAdvance advance : advancesAll) {
            if (!has.contains(advance)) {
              row = rowMap.get(advance.getName());
              data[row][col] = "-";
            }
          }
          for (final TechAdvance advance :
              TechTracker.getCurrentTechAdvances(pid, gameDataSync.getTechnologyFrontier())) {
            row = rowMap.get(advance.getName());
            data[row][col] = "X";
          }
        }
      }
    }

    @Override
    public String getColumnName(final int col) {
      if (col == 0) {
        return "Technology";
      }
      return colList[col - 1];
    }

    /*
     * Recalculations the underlying data in a lazy manner.
     * Limitation: This is not a thread-safe implementation.
     */
    @Override
    public Object getValueAt(final int row, final int col) {
      if (isDirty) {
        update();
        isDirty = false;
      }
      return data[row][col];
    }

    // Trivial implementations of required methods
    @Override
    public int getColumnCount() {
      return colList.length + 1;
    }

    @Override
    public int getRowCount() {
      return data.length;
    }

    void markDirty() {
      isDirty = true;
    }
  }
}
