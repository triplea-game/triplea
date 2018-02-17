package games.strategy.triplea.ui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.border.EtchedBorder;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.events.GameDataChangeListener;
import games.strategy.engine.stats.IStat;
import games.strategy.triplea.Constants;

/**
 * Panel used to display the current players resources.
 */
public class ResourceBar extends AbstractStatPanel implements GameDataChangeListener {
  private static final long serialVersionUID = -7713792841831042952L;

  private final UiContext uiContext;
  private final List<IStat> resources = new ArrayList<>();
  private final List<JLabel> labels = new ArrayList<>();

  public ResourceBar(final GameData data, final UiContext uiContext) {
    super(data);
    this.uiContext = uiContext;
    setResources();
    initLayout();
    gameData.addDataChangeListener(this);
  }

  @Override
  protected void initLayout() {
    setBorder(new EtchedBorder(EtchedBorder.RAISED));
    labels.stream().forEachOrdered(this::add);
  }

  @Override
  public void setGameData(final GameData data) {
    gameData.removeDataChangeListener(this);
    gameData = data;
    gameData.addDataChangeListener(this);
  }

  private void setResources() {
    for (final Resource resource : gameData.getResourceList().getResources()) {
      if (resource.getName().equals(Constants.VPS)) {
        continue;
      }
      resources.add(new ResourceStat(resource));
      final JLabel label = new JLabel();
      label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20));
      labels.add(label);
    }
  }

  @Override
  public void gameDataChanged(final Change change) {
    gameData.acquireReadLock();
    try {
      final PlayerID player = gameData.getSequence().getStep().getPlayerId();
      if (player != null) {
        for (int i = 0; i < resources.size(); i++) {
          final String quantity = resources.get(i).getFormatter().format(resources.get(i).getValue(player, gameData));
          try {
            labels.get(i).setIcon(uiContext.getResourceImageFactory()
                .getIcon(gameData.getResourceList().getResource(resources.get(i).getName()), true));
            labels.get(i).setText(quantity);
            labels.get(i).setToolTipText(resources.get(i).getName());
          } catch (final IllegalStateException e) {
            labels.get(i).setText(resources.get(i).getName() + " " + quantity);
          }
        }
      }
    } finally {
      gameData.releaseReadLock();
    }
  }

}
