package games.strategy.triplea.ui;

import games.strategy.engine.data.AllianceTracker;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.events.GameDataChangeListener;
import games.strategy.engine.stats.IStat;
import games.strategy.engine.stats.ProductionStat;
import games.strategy.engine.stats.ResourceStat;
import games.strategy.engine.stats.TuvStat;
import games.strategy.engine.stats.UnitsStat;
import games.strategy.engine.stats.VictoryCityStat;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.ui.mapdata.MapData;
import java.awt.GridLayout;
import java.awt.Image;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

class StatPanel extends JPanel implements GameDataChangeListener {
  private static final long serialVersionUID = 4340684166664492498L;

  IStat[] stats;
  private final Map<GamePlayer, ImageIcon> mapPlayerImage = new HashMap<>();
  protected GameData gameData;
  private final UiContext uiContext;
  private final StatTableModel dataModel;

  StatPanel(final GameData data, final UiContext uiContext) {
    this.gameData = data;
    this.uiContext = uiContext;
    dataModel = new StatTableModel();
    fillPlayerIcons();
    initLayout();
    gameData.addDataChangeListener(this);
  }

  protected void initLayout() {
    setLayout(new GridLayout(1, 1));
    add(new JScrollPane(createPlayersTable()));
  }

  private JTable createPlayersTable() {
    final JTable statsTable = new JTable(dataModel);
    statsTable.getTableHeader().setReorderingAllowed(false);
    // By default, right-align columns and their headers.
    ((DefaultTableCellRenderer) statsTable.getTableHeader().getDefaultRenderer())
        .setHorizontalAlignment(SwingConstants.RIGHT);
    ((DefaultTableCellRenderer) statsTable.getDefaultRenderer(String.class))
        .setHorizontalAlignment(SwingConstants.RIGHT);
    final TableColumn leftColumn = statsTable.getColumnModel().getColumn(0);
    leftColumn.setPreferredWidth(175);
    // The left column should be left-aligned. Override the renderers for it to
    // defaults.
    leftColumn.setCellRenderer(new DefaultTableCellRenderer());
    // There is no way to directly construct the default table header renderer
    // (which differs from
    // the default table cell renderer on some L&Fs), so grab one from a temp
    // JTableHeader.
    leftColumn.setHeaderRenderer(new JTableHeader().getDefaultRenderer());
    return statsTable;
  }

  public void setGameData(final GameData data) {
    gameData.removeDataChangeListener(this);
    gameData = data;
    gameData.addDataChangeListener(this);
    gameDataChanged(null);
  }

  @Override
  public void gameDataChanged(final Change change) {
    dataModel.markDirty();
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

  /** Custom table model. This model is thread safe. */
  class StatTableModel extends AbstractTableModel {
    private static final long serialVersionUID = -6156153062049822444L;
    /* Underlying data for the table. If null, needs to be computed. */
    private String[][] collectedData;

    StatTableModel() {
      setStatColumns();
    }

    void setStatColumns() {
      final List<IStat> statsList = new ArrayList<>();
      statsList.add(new PuStat());
      statsList.add(new ProductionStat());
      statsList.add(new UnitsStat());
      statsList.add(new TuvStat());
      if (gameData.getMap().getTerritories().stream().anyMatch(Matches.territoryIsVictoryCity())) {
        statsList.add(new VictoryCityStat());
      }
      // only add the vps in pacific
      if (Properties.getPacificTheater(gameData.getProperties())) {
        statsList.add(new VpStat());
      }
      StatPanel.this.stats = statsList.toArray(new IStat[0]);
    }

    private synchronized void loadData() {
      // copy so that the object doesn't change underneath us
      final GameData gameDataSync = StatPanel.this.gameData;
      try (GameData.Unlocker ignored = gameDataSync.acquireReadLock()) {
        final List<GamePlayer> players = gameDataSync.getPlayerList().getSortedPlayers();
        final List<String> alliances = getAlliancesToShow(gameDataSync.getAllianceTracker());
        collectedData = new String[players.size() + alliances.size()][stats.length + 1];
        int row = 0;
        for (final GamePlayer player : players) {
          collectedData[row][0] = player.getName();
          for (int i = 0; i < stats.length; i++) {
            double value = stats[i].getValue(player, gameDataSync, uiContext.getMapData());
            collectedData[row][i + 1] = IStat.DECIMAL_FORMAT.format(value);
          }
          row++;
        }
        for (final String alliance : alliances) {
          collectedData[row][0] = "<html><b>" + alliance;
          for (int i = 0; i < stats.length; i++) {
            double value = stats[i].getValue(alliance, gameDataSync, uiContext.getMapData());
            collectedData[row][i + 1] = IStat.DECIMAL_FORMAT.format(value);
          }
          row++;
        }
      }
    }

    private List<String> getAlliancesToShow(AllianceTracker tracker) {
      return tracker.getAlliances().stream()
          .filter(a -> tracker.getPlayersInAlliance(a).size() > 1)
          .sorted()
          .collect(Collectors.toList());
    }

    /*
     * Re-calculations the underlying data in a lazy manner.
     * Limitation: This is not a thread-safe implementation.
     */
    @Override
    public synchronized Object getValueAt(final int row, final int col) {
      return getCollectedData()[row][col];
    }

    // Trivial implementations of required methods
    @Override
    public String getColumnName(final int col) {
      if (col == 0) {
        return "Player";
      }
      return stats[col - 1].getName();
    }

    @Override
    public int getColumnCount() {
      return stats.length + 1;
    }

    @Override
    public int getRowCount() {
      return getCollectedData().length;
    }

    synchronized void markDirty() {
      collectedData = null;
    }

    private synchronized String[][] getCollectedData() {
      if (collectedData == null) {
        loadData();
      }
      return collectedData;
    }
  }

  class PuStat extends ResourceStat {
    PuStat() {
      super(getResourcePUs(gameData));
    }
  }

  private static Resource getResourcePUs(final GameData data) {
    final Resource pus;
    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      pus = data.getResourceList().getResourceOrThrow(Constants.PUS);
    }
    return pus;
  }

  static class VpStat implements IStat {
    @Override
    public String getName() {
      return "VPs";
    }

    @Override
    public double getValue(final GamePlayer player, final GameData data, final MapData mapData) {
      final PlayerAttachment pa = PlayerAttachment.get(player);
      if (pa != null) {
        return pa.getVps();
      }
      return 0;
    }
  }
}
