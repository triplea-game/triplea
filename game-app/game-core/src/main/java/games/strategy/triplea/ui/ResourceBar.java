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
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import org.triplea.java.collections.IntegerMap;
import org.triplea.swing.jpanel.GridBagConstraintsBuilder;

/** Panel used to display the current players resources. */
public class ResourceBar extends JPanel implements GameDataChangeListener {
  private static final long serialVersionUID = -7713792841831042952L;

  private final GameData gameData;
  private final UiContext uiContext;
  private final List<ResourceStat> resourceStats = new ArrayList<>();
  private volatile boolean updateScheduled;

  public ResourceBar(final GameData data, final UiContext uiContext) {
    this.gameData = data;
    this.uiContext = uiContext;
    setResources();
    initLayout();
    gameData.addDataChangeListener(this);
  }

  protected void initLayout() {
    setBorder(new EtchedBorder(EtchedBorder.RAISED));
    this.setLayout(new GridBagLayout());
  }

  private void setResources() {
    for (final Resource resource : gameData.getResourceList().getResources()) {
      if (resource.getName().equals(Constants.VPS)) {
        continue;
      }
      resourceStats.add(new ResourceStat(resource));
    }
  }

  @Override
  public void gameDataChanged(final Change change) {
    // When there are multiple gameDataChanged() notifications in a row, for example
    // from an "undo all" action, no need to do this repeatedly. This will just run
    // once if the swing code runs after all the notifications have been received.
    if (updateScheduled) {
      return;
    }
    updateScheduled = true;
    SwingUtilities.invokeLater(
        () -> {
          updateScheduled = false;
          final GamePlayer player;
          final IntegerMap<Resource> resourceIncomes;
          try {
            gameData.acquireReadLock();
            player = gameData.getSequence().getStep().getPlayerId();
            if (player == null) {
              return;
            }
            resourceIncomes = AbstractEndTurnDelegate.findEstimatedIncome(player, gameData);
          } finally {
            gameData.releaseReadLock();
          }

          this.removeAll();
          int count = 0;
          for (final ResourceStat resourceStat : resourceStats) {
            final Resource resource = resourceStat.resource;
            if (!resource.isDisplayedFor(player)) {
              continue;
            }
            final double quantity = resourceStat.getValue(player, gameData, uiContext.getMapData());
            final int income = resourceIncomes.getInt(resource);
            final StringBuilder text = new StringBuilder(IStat.DECIMAL_FORMAT.format(quantity));
            text.append(" (").append(income >= 0 ? "+" : "").append(income).append(")");
            final JLabel label =
                uiContext.getResourceImageFactory().getLabel(resource, text.toString());
            label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            add(label, new GridBagConstraintsBuilder(count++, 0).weightY(1).build());
          }
        });
  }
}
