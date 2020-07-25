package games.strategy.triplea.ui;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.events.GameDataChangeListener;
import games.strategy.engine.stats.IStat;
import games.strategy.engine.stats.ResourceStat;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.AbstractEndTurnDelegate;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import org.triplea.java.collections.IntegerMap;

class EconomyPanel extends AbstractStatPanel {
  private static final long serialVersionUID = -7713792841831042952L;
  private final List<ResourceStat> resourceStats = new ArrayList<>();
  private ResourceTableModel resourceModel;
  private final UiContext uiContext;

  EconomyPanel(final GameData data, final UiContext uiContext) {
    super(data);
    this.uiContext = uiContext;
    initLayout();
  }

  protected void initLayout() {
    setLayout(new GridLayout(1, 1));
    resourceModel = new ResourceTableModel();
    final JTable table = new JTable(resourceModel);
    table.getTableHeader().setReorderingAllowed(false);
    final TableColumn column = table.getColumnModel().getColumn(0);
    column.setPreferredWidth(175);
    for (int i = 1; i < resourceModel.getColumnCount(); i++) {
      table.getColumnModel().getColumn(i).setHeaderRenderer(new DefaultTableCellRenderer());
      final JLabel label = (JLabel) table.getColumnModel().getColumn(i).getHeaderRenderer();
      final Resource resource = resourceStats.get(i - 1).resource;
      try {
        label.setIcon(uiContext.getResourceImageFactory().getIcon(resource, false));
        label.setToolTipText(resource.getName());
      } catch (final IllegalStateException e) {
        // ignore missing resource image
      }
    }
    final JScrollPane scroll = new JScrollPane(table);
    add(scroll);
  }

  class ResourceTableModel extends AbstractTableModel implements GameDataChangeListener {
    private static final long serialVersionUID = 5197895788633898324L;
    private boolean isDirty = true;
    private String[][] collectedData;

    ResourceTableModel() {
      setResourceColumns();
      gameData.addDataChangeListener(this);
    }

    private void setResourceColumns() {
      for (final Resource resource : gameData.getResourceList().getResources()) {
        if (resource.getName().equals(Constants.VPS)) {
          continue;
        }
        resourceStats.add(new ResourceStat(resource));
      }
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
        final List<GamePlayer> players = getPlayers();
        final Map<String, Set<GamePlayer>> allianceMap = getAllianceMap();
        collectedData = new String[players.size() + allianceMap.size()][resourceStats.size() + 1];
        int row = 0;
        final Map<GamePlayer, IntegerMap<Resource>> resourceIncomeMap = new HashMap<>();
        for (final GamePlayer player : players) {
          collectedData[row][0] = player.getName();
          final IntegerMap<Resource> resourceIncomes =
              AbstractEndTurnDelegate.findEstimatedIncome(player, gameData);
          resourceIncomeMap.put(player, resourceIncomes);
          for (int i = 0; i < resourceStats.size(); i++) {
            final ResourceStat resourceStat = resourceStats.get(i);
            final double amount = resourceStat.getValue(player, gameData, uiContext.getMapData());
            final int income = resourceIncomes.getInt(resourceStat.resource);
            collectedData[row][i + 1] = getResourceAmountAndIncome(resourceStat, amount, income);
          }
          row++;
        }
        for (final Entry<String, Set<GamePlayer>> alliance : allianceMap.entrySet()) {
          collectedData[row][0] = alliance.getKey();
          for (int i = 0; i < resourceStats.size(); i++) {
            final ResourceStat resourceStat = resourceStats.get(i);
            final double amount =
                resourceStat.getValue(alliance.getKey(), gameData, uiContext.getMapData());
            final int income =
                alliance.getValue().stream()
                    .mapToInt(p -> resourceIncomeMap.get(p).getInt(resourceStat.resource))
                    .sum();
            collectedData[row][i + 1] = getResourceAmountAndIncome(resourceStat, amount, income);
          }
          row++;
        }
      } finally {
        gameData.releaseReadLock();
      }
    }

    private String getResourceAmountAndIncome(
        final ResourceStat resourceStat, final double amount, final int income) {
      final StringBuilder resourceAmountAndIncome =
          new StringBuilder("<html><b>" + IStat.DECIMAL_FORMAT.format(amount) + "</b>&nbsp;(");
      if (income >= 0) {
        resourceAmountAndIncome.append("+");
      }
      resourceAmountAndIncome.append(income).append(")</html>");
      return resourceAmountAndIncome.toString();
    }

    @Override
    public void gameDataChanged(final Change change) {
      synchronized (this) {
        isDirty = true;
      }
      SwingUtilities.invokeLater(EconomyPanel.this::repaint);
    }

    @Override
    public String getColumnName(final int col) {
      return "";
    }

    @Override
    public int getColumnCount() {
      return resourceStats.size() + 1;
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

  public void setGameData(final GameData data) {
    gameData = data;
    resourceModel.setGameData(data);
    resourceModel.gameDataChanged(null);
  }
}
