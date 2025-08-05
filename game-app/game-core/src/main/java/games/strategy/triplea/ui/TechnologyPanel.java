package games.strategy.triplea.ui;

import static games.strategy.triplea.Constants.SIGN_TECH_ENABLED;
import static games.strategy.triplea.Constants.SIGN_TECH_NOT_ENABLED;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameDataEvent;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.events.GameDataChangeListener;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.TechTracker;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Image;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

class TechnologyPanel extends JPanel implements GameDataChangeListener {

  private final Map<GamePlayer, ImageIcon> mapPlayerImage = new HashMap<>();
  protected GameData gameData;
  private final transient UiContext uiContext;
  private final TechTableModel techModel;

  TechnologyPanel(final GameData data, final UiContext uiContext) {
    this.gameData = data;
    this.uiContext = uiContext;
    techModel = new TechTableModel();
    setIconIfNeeded();
    initLayout();
    gameData.addGameDataEventListener(
        GameDataEvent.TECH_ATTACHMENT_CHANGED, () -> gameDataChanged(null));
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
    for (int columnId = 1; columnId < techTable.getColumnCount(); columnId++) {
      final TableColumn column = techTable.getColumnModel().getColumn(columnId);
      column.setHeaderRenderer(componentRenderer);
      final String player = techTable.getColumnName(columnId);
      final JLabel value = new JLabel("", getIcon(player).orElse(null), SwingConstants.CENTER);
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
    gameData.addGameDataEventListener(
        GameDataEvent.TECH_ATTACHMENT_CHANGED, () -> gameDataChanged(null));
    gameDataChanged(null);
  }

  @Override
  public void gameDataChanged(@Nullable final Change change) {
    techModel.markDirty();
    SwingUtilities.invokeLater(this::repaint);
  }

  /**
   * Gets the small flag for a given PlayerId.
   *
   * @param player the player to get the flag for
   * @return ImageIcon small flag
   */
  protected Optional<ImageIcon> getIcon(final GamePlayer player) {
    Optional<ImageIcon> icon = Optional.ofNullable(mapPlayerImage.get(player));
    if (icon.isEmpty() && uiContext != null) {
      final Image img = uiContext.getFlagImageFactory().getSmallFlag(player);
      icon = Optional.of(new ImageIcon(img));
      icon.get().setDescription(player.getName());
      mapPlayerImage.put(player, icon.get());
    }
    return icon;
  }

  protected Optional<ImageIcon> getIcon(final String playerName) {
    final GamePlayer player = gameData.getPlayerList().getPlayerId(playerName);
    return player == null ? Optional.empty() : getIcon(player);
  }

  private void setIconIfNeeded() {
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
    /* Flag to indicate whether data needs to be recalculated */
    private boolean isDirty = true;
    /* Column Header Names */
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
      for (int columnId = 0; columnId < colList.length; columnId++) {
        int columnNr = columnId + 1;
        colMap.put(colList[columnId], columnNr);
      }
      data = getDataAndInitRowMap();
      clearAdvances();
    }

    private synchronized String[][] getDataAndInitRowMap() {
      final String[][] dataTable;
      boolean useToken = false;
      // copy so that the object doesn't change underneath us
      final GameData gameDataSync = TechnologyPanel.this.gameData;
      try (GameData.Unlocker ignored = gameDataSync.acquireReadLock()) {
        final int numTechs =
            TechAdvance.getTechAdvances(gameDataSync.getTechnologyFrontier()).size();
        final int tableColNumber = colList.length + 1; // Icons need one more row
        if (gameDataSync.getResourceList().getResourceOptional(Constants.TECH_TOKENS).isPresent()) {
          useToken = true;
          dataTable = new String[numTechs + 1][tableColNumber]; // Create a row for tokens
        } else {
          dataTable = new String[numTechs][tableColNumber];
        }
      }
      /* Load the technology -> row mapping */
      int row = 0;
      if (useToken) {
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
      for (int rowId = 0; rowId < data.length; rowId++) {
        for (int columId = 1; columId <= colList.length; columId++) {
          data[rowId][columId] = "";
        }
      }
    }

    private void initColList() {
      final List<GamePlayer> players = gameData.getPlayerList().getPlayers();
      colList = players.stream().map(GamePlayer::getName).toArray(String[]::new);
      Arrays.sort(colList);
    }

    synchronized void update() {
      clearAdvances();
      // copy so that the object doesn't change underneath us
      final GameData gameDataSync = TechnologyPanel.this.gameData;
      try (GameData.Unlocker ignored = gameDataSync.acquireReadLock()) {
        for (final GamePlayer playerID : gameDataSync.getPlayerList().getPlayers()) {
          if (colMap.get(playerID.getName()) == null) {
            throw new IllegalStateException(
                MessageFormat.format(
                    "Unexpected player in GameData.getPlayerList(): {0}", playerID.getName()));
          }
          final int col = colMap.get(playerID.getName());
          int row = 0;
          if (gameDataSync
              .getResourceList()
              .getResourceOptional(Constants.TECH_TOKENS)
              .isPresent()) {
            final int tokens = playerID.getResources().getQuantity(Constants.TECH_TOKENS);
            data[row][col] = Integer.toString(tokens);
          }
          final List<TechAdvance> advancesAll =
              TechAdvance.getTechAdvances(gameDataSync.getTechnologyFrontier());
          final List<TechAdvance> has =
              TechAdvance.getTechAdvances(gameDataSync.getTechnologyFrontier(), playerID);
          for (final TechAdvance advance : advancesAll) {
            if (!has.contains(advance)) {
              row = rowMap.get(advance.getName());
              data[row][col] = SIGN_TECH_NOT_ENABLED;
            }
          }
          for (final TechAdvance advance :
              TechTracker.getCurrentTechAdvances(playerID, gameDataSync.getTechnologyFrontier())) {
            row = rowMap.get(advance.getName());
            data[row][col] = SIGN_TECH_ENABLED;
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

    @Override
    public int getColumnCount() {
      return colList.length
          + 1; // add one because there is a column for the name of the technologies
    }

    @Override
    public int getRowCount() {
      return data.length; // the number of rows of the 2D-array data
    }

    void markDirty() {
      isDirty = true;
    }
  }
}
