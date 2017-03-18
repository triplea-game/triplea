package games.strategy.triplea.ui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.SwingUtilities;

import games.strategy.engine.data.GameData;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.engine.pbem.ForumPosterComponent;
import games.strategy.engine.pbem.PBEMMessagePoster;
import games.strategy.triplea.delegate.remote.IAbstractForumPosterDelegate;
import games.strategy.ui.SwingAction;

public class MoveForumPosterPanel extends AbstractForumPosterPanel {
  private static final long serialVersionUID = -533962696697230277L;

  public MoveForumPosterPanel(final GameData data, final MapPanel map) {
    super(data, map);
    AbstractAction m_doneAction = SwingAction.of("Done", e -> release());
    m_forumPosterComponent = new ForumPosterComponent(getData(), m_doneAction, getTitle());
  }

  @Override
  protected String getTitle() {                                                                       
    return "Move Summary";
  }

  @Override
  protected boolean allowIncludeTerritorySummary() {
    return false;
  }

  @Override
  protected boolean allowIncludeTerritoryAllPlayersSummary() {
    return false;
  }

  @Override
  protected boolean allowIncludeProductionSummary() {
    return false;
  }

  @Override
  protected boolean allowDiceBattleDetails() {
    return false;
  }

  @Override
  protected boolean allowDiceStatistics() {
    return false;
  }

  @Override
  public String toString() {
    return "MoveForumPosterPanel";
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
    return !m_poster.alsoPostMoveSummary();
  }

  @Override
  public void waitForDone(final TripleAFrame frame, final IPlayerBridge bridge) {
    super.waitForDone(frame, bridge);
  }
}
