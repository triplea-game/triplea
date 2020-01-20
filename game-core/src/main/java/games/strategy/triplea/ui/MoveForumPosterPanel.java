package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;

class MoveForumPosterPanel extends AbstractForumPosterPanel {
  private static final long serialVersionUID = -533962696697230277L;

  MoveForumPosterPanel(final GameData data, final MapPanel map) {
    super(data, map);
  }

  @Override
  void performDone() {
    release();
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
  protected boolean skipPosting() {
    return !pbemMessagePoster.alsoPostMoveSummary();
  }
}
