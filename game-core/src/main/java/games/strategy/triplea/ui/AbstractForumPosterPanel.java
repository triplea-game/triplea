package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Round;
import games.strategy.engine.pbem.ForumPosterComponent;
import games.strategy.engine.pbem.PbemMessagePoster;
import games.strategy.engine.player.IPlayerBridge;
import games.strategy.triplea.delegate.GameStepPropertiesHelper;
import games.strategy.triplea.delegate.remote.IAbstractForumPosterDelegate;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

abstract class AbstractForumPosterPanel extends ActionPanel {
  private static final long serialVersionUID = -5084680807785728744L;

  IPlayerBridge playerBridge;
  PbemMessagePoster pbemMessagePoster;
  ForumPosterComponent forumPosterComponent;
  private final JLabel actionLabel;
  private TripleAFrame tripleAFrame;

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
  public void display(final GamePlayer gamePlayer) {
    super.display(gamePlayer);
    SwingUtilities.invokeLater(
        () -> {
          actionLabel.setText(gamePlayer.getName() + " " + getTitle());
          // defer component layout until waitForEndTurn()
        });
  }

  protected abstract boolean allowIncludeTerritorySummary();

  protected abstract boolean allowIncludeProductionSummary();

  protected abstract boolean allowDiceBattleDetails();

  protected abstract boolean allowDiceStatistics();

  protected abstract IAbstractForumPosterDelegate getForumPosterDelegate();

  protected abstract boolean getHasPostedTurnSummary();

  protected abstract boolean skipPosting();

  protected abstract String getTitle();

  @Override
  public abstract String toString();

  protected void waitForDone(final TripleAFrame frame, final IPlayerBridge bridge) {
    tripleAFrame = frame;
    playerBridge = bridge;
    // Nothing to do if there are no PBEM messengers
    pbemMessagePoster =
        new PbemMessagePoster(getData(), getCurrentPlayer(), getRound(), getTitle());
    if (!pbemMessagePoster.hasMessengers()) {
      return;
    }
    if (skipPosting() || GameStepPropertiesHelper.isSkipPosting(getData())) {
      return;
    }
    final boolean hasPosted = getHasPostedTurnSummary();
    SwingUtilities.invokeLater(
        () -> {
          removeAll();
          add(actionLabel);
          add(
              forumPosterComponent.layoutComponents(
                  pbemMessagePoster,
                  getForumPosterDelegate(),
                  tripleAFrame,
                  hasPosted,
                  allowIncludeTerritorySummary(),
                  allowIncludeProductionSummary(),
                  allowDiceBattleDetails(),
                  allowDiceStatistics()));
          validate();
        });
    waitForRelease();
  }
}
