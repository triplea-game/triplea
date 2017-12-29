package games.strategy.triplea.ui;

import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.events.GameDataChangeListener;
import games.strategy.engine.stats.IStat;
import games.strategy.triplea.Constants;

public class EconomyPanel extends AbstractStatPanel {
  private static final long serialVersionUID = -7713792841831042952L;
  private IStat[] statsResource;
  private ResourceTableModel resourceModel;

  public EconomyPanel(final GameData data) {
    super(data);
    initLayout();
  }

  @Override
  protected void initLayout() {
    setLayout(new GridLayout(1, 1));
    resourceModel = new ResourceTableModel();
    final JTable table = new JTable(resourceModel);
    table.getTableHeader().setReorderingAllowed(false);
    final TableColumn column = table.getColumnModel().getColumn(0);
    column.setPreferredWidth(175);
    final JScrollPane scroll = new JScrollPane(table);
    add(scroll);
  }

  class ResourceTableModel extends AbstractTableModel implements GameDataChangeListener {
    private static final long serialVersionUID = 5197895788633898324L;
    private boolean isDirty = true;
    private String[][] collectedData;

    public ResourceTableModel() {
      setResourceCollums();
      gameData.addDataChangeListener(this);
      isDirty = true;
    }

    private void setResourceCollums() {
      final List<IStat> statList = new ArrayList<>();
      for (final Resource resource : gameData.getResourceList().getResources()) {
        if (resource.getName().equals(Constants.TECH_TOKENS) || resource.getName().equals(Constants.VPS)) {
          continue;
        }
        statList.add(new ResourceStat(resource));
      }
      statsResource = statList.toArray(new IStat[statList.size()]);
    }

    @Override
    public synchronized Object getValueAt(final int row, final int col) {
      if (isDirty) {
        loadData();
        isDirty = false;
      }
      return collectedData[row][col];
    }

    private synchronized void loadData() {
      gameData.acquireReadLock();
      try {
        final List<PlayerID> players = getPlayers();
        final Collection<String> alliances = getAlliances();
        collectedData = new String[players.size() + alliances.size()][statsResource.length + 1];
        int row = 0;
        for (final PlayerID player : players) {
          collectedData[row][0] = player.getName();
          for (int i = 0; i < statsResource.length; i++) {
            collectedData[row][i + 1] =
                statsResource[i].getFormatter().format(statsResource[i].getValue(player, gameData));
          }
          row++;
        }
        for (final String alliance : alliances) {
          collectedData[row][0] = alliance;
          for (int i = 0; i < statsResource.length; i++) {
            collectedData[row][i + 1] =
                statsResource[i].getFormatter().format(statsResource[i].getValue(alliance, gameData));
          }
          row++;
        }
      } finally {
        gameData.releaseReadLock();
      }
    }

    @Override
    public void gameDataChanged(final Change change) {
      synchronized (this) {
        isDirty = true;
      }
      SwingUtilities.invokeLater(() -> repaint());
    }

    @Override
    public String getColumnName(final int col) {
      if (col == 0) {
        return "Player";
      }
      return statsResource[col - 1].getName();
    }

    @Override
    public int getColumnCount() {
      return statsResource.length + 1;
    }

    @Override
    public synchronized int getRowCount() {
      if (!isDirty) {
        return collectedData.length;
      }

      gameData.acquireReadLock();
      try {
        return gameData.getPlayerList().size() + getAlliances().size();
      } finally {
        gameData.releaseReadLock();
      }
    }

    public synchronized void setGameData(final GameData data) {
      synchronized (this) {
        gameData.removeDataChangeListener(this);
        gameData = data;
        gameData.addDataChangeListener(this);
        isDirty = true;
      }
      repaint();
    }
  }

  @Override
  public void setGameData(final GameData data) {
    gameData = data;
    resourceModel.setGameData(data);
    resourceModel.gameDataChanged(null);
  }
}
