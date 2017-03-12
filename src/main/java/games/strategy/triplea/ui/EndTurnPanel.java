package games.strategy.triplea.ui;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import games.strategy.engine.data.GameData;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.engine.pbem.ForumPosterComponent;
import games.strategy.engine.pbem.PBEMMessagePoster;
import games.strategy.triplea.delegate.remote.IAbstractForumPosterDelegate;
import games.strategy.ui.SwingAction;

public class EndTurnPanel extends AbstractForumPosterPanel {
  private static final long serialVersionUID = -6282316384529504341L;
  protected AbstractAction m_doneAction = SwingAction.of("Done", e -> {
    if (m_forumPosterComponent.getHasPostedTurnSummary() || JOptionPane.YES_OPTION ==
        JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(EndTurnPanel.this),
        "Are you sure you don't want to post?", "Bypass post", JOptionPane.YES_NO_OPTION)) {
        release();
    }
  });

  public EndTurnPanel(final GameData data, final MapPanel map) {
    super(data, map);
    m_forumPosterComponent = new ForumPosterComponent(getData(), m_doneAction, getTitle());
  }

  @Override
  protected String getTitle() {
    return "Turn Summary";
  }

  @Override
  public String toString() {
    return "EndTurnPanel";
  }

  @Override
  protected boolean allowIncludeTerritorySummary() {
    return true;
  }

  @Override
  protected boolean allowIncludeTerritoryAllPlayersSummary() {
    return false;
  }

  @Override
  protected boolean allowIncludeProductionSummary() {
    return true;
  }

  @Override
  protected boolean allowDiceBattleDetails() {
    return true;
  }

  @Override
  protected boolean allowDiceStatistics() {
    return true;
  }

  @Override
  protected IAbstractForumPosterDelegate getForumPosterDelegate() {
    return (IAbstractForumPosterDelegate) m_bridge.getRemoteDelegate();
  }

  @Override
  protected boolean getHasPostedTurnSummary() {
    final IAbstractForumPosterDelegate delegate = (IAbstractForumPosterDelegate) m_bridge.getRemoteDelegate();
    return delegate.getHasPostedTurnSummary();
  }

  @Override
  protected void setHasPostedTurnSummary(final boolean posted) {
    final IAbstractForumPosterDelegate delegate = (IAbstractForumPosterDelegate) m_bridge.getRemoteDelegate();
    delegate.setHasPostedTurnSummary(posted);
  }

  @Override
  protected boolean postTurnSummary(final PBEMMessagePoster poster, final boolean includeSaveGame) {
    final IAbstractForumPosterDelegate delegate = (IAbstractForumPosterDelegate) m_bridge.getRemoteDelegate();
    return delegate.postTurnSummary(poster, getTitle(), includeSaveGame);
  }

  @Override
  protected boolean skipPosting() {
    return false;
  }

  public void waitForEndTurn(final TripleAFrame frame, final IPlayerBridge bridge) {
    super.waitForDone(frame, bridge);
  }
}
