package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import javax.swing.JOptionPane;

class EndTurnPanel extends AbstractForumPosterPanel {
  private static final long serialVersionUID = -6282316384529504341L;

  EndTurnPanel(final GameData data, final MapPanel map) {
    super(data, map);
  }

  @Override
  void performDone() {
    if (forumPosterComponent.getHasPostedTurnSummary()
        || JOptionPane.YES_OPTION
            == JOptionPane.showConfirmDialog(
                JOptionPane.getFrameForComponent(EndTurnPanel.this),
                "Are you sure you don't want to post?",
                "Bypass post",
                JOptionPane.YES_NO_OPTION)) {
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
  protected boolean skipPosting() {
    return false;
  }
}
