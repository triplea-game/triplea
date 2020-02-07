package games.strategy.engine.posted.game;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.ui.panels.map.MapPanel;

public class MoveForumPosterPanel extends AbstractForumPosterPanel {
  private static final long serialVersionUID = -533962696697230277L;

  public MoveForumPosterPanel(final GameData data, final MapPanel map) {
    super(data, map);
  }

  @Override
  public void performDone() {
    release();
  }

  @Override
  protected String getTitle() {
    return "Move Summary";
  }

  @Override
  protected boolean includeDetailsAndSummary() {
    return false;
  }

  @Override
  protected boolean skipPosting() {
    return !pbemMessagePoster.alsoPostMoveSummary();
  }

  @Override
  public String toString() {
    return "MoveForumPosterPanel";
  }
}
