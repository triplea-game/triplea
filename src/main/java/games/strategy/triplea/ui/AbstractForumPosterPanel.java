package games.strategy.triplea.ui;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Round;
import games.strategy.engine.pbem.ForumPosterComponent;
import games.strategy.engine.pbem.PBEMMessagePoster;
import games.strategy.triplea.delegate.GameStepPropertiesHelper;
import games.strategy.triplea.delegate.remote.IAbstractForumPosterDelegate;

public abstract class AbstractForumPosterPanel extends ActionPanel {
  private static final long serialVersionUID = -5084680807785728744L;
  private final JLabel actionLabel;
  protected IPlayerBridge playerBridge;
  PBEMMessagePoster pbemMessagePoster;
  private TripleAFrame tripleAFrame;
  ForumPosterComponent forumPosterComponent;

  AbstractForumPosterPanel(final GameData data, final MapPanel map) {
    super(data, map);
    actionLabel = new JLabel();
  }

  private int getRound() {
    final Object[] pathFromRoot = getData().getHistory().getLastNode().getPath();
    for (final Object pathNode : pathFromRoot) {
      final HistoryNode curNode = (HistoryNode) pathNode;
      if (curNode instanceof Round) {
        return ((Round) curNode).getRoundNo();
      }
    }
    return 0;
  }

  @Override
  public void display(final PlayerID id) {
    super.display(id);
    SwingUtilities.invokeLater(() -> {
      actionLabel.setText(id.getName() + " " + getTitle());
      // defer componenet layout until waitForEndTurn()
    });
  }

  protected abstract boolean allowIncludeTerritorySummary();

  protected abstract boolean allowIncludeTerritoryAllPlayersSummary();

  protected abstract boolean allowIncludeProductionSummary();

  protected abstract boolean allowDiceBattleDetails();

  protected abstract boolean allowDiceStatistics();

  protected abstract IAbstractForumPosterDelegate getForumPosterDelegate();

  protected abstract boolean postTurnSummary(final PBEMMessagePoster poster, final boolean includeSaveGame);

  protected abstract boolean getHasPostedTurnSummary();

  protected abstract void setHasPostedTurnSummary(boolean posted);

  protected abstract boolean skipPosting();

  protected abstract String getTitle();

  @Override
  public abstract String toString();

  protected void waitForDone(final TripleAFrame frame, final IPlayerBridge bridge) {
    tripleAFrame = frame;
    playerBridge = bridge;
    // Nothing to do if there are no PBEM messengers
    pbemMessagePoster = new PBEMMessagePoster(getData(), getCurrentPlayer(), getRound(), getTitle());
    if (!pbemMessagePoster.hasMessengers()) {
      return;
    }
    if (skipPosting() || GameStepPropertiesHelper.isSkipPosting(getData())) {
      return;
    }
    final boolean hasPosted = getHasPostedTurnSummary();
    SwingUtilities.invokeLater(() -> {
      removeAll();
      add(actionLabel);
      add(forumPosterComponent.layoutComponents(pbemMessagePoster, getForumPosterDelegate(), playerBridge,
          tripleAFrame, hasPosted,
          allowIncludeTerritorySummary(), allowIncludeTerritoryAllPlayersSummary(), allowIncludeProductionSummary(),
          allowDiceBattleDetails(), allowDiceStatistics()));
      validate();
    });
    waitForRelease();
  }
}
