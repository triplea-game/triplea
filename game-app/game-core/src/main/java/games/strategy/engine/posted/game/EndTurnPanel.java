package games.strategy.engine.posted.game;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.ui.panels.map.MapPanel;
import javax.swing.JOptionPane;

public class EndTurnPanel extends AbstractForumPosterPanel {
  private static final long serialVersionUID = -6282316384529504341L;

  public EndTurnPanel(final GameData data, final MapPanel map) {
    super(data, map);
  }

  @Override
  public void performDone() {
    if (forumPosterComponent.canPostTurnSummary()
        && (forumPosterComponent.getHasPostedTurnSummary()
            || JOptionPane.YES_OPTION
                == JOptionPane.showConfirmDialog(
                    JOptionPane.getFrameForComponent(EndTurnPanel.this),
                    "Are you sure you don't want to post?",
                    "Bypass post",
                    JOptionPane.YES_NO_OPTION))) {
      release();
    }
  }

  @Override
  protected String getTitle() {
    return "Turn Summary";
  }

  @Override
  protected boolean includeDetailsAndSummary() {
    return true;
  }

  @Override
  public String toString() {
    return "EndTurnPanel";
  }

  @Override
  protected boolean skipPosting() {
    return false;
  }
}
