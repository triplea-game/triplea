package games.strategy.engine.posted.game;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.posted.game.pbem.PbemMessagePoster;
import games.strategy.engine.random.IRandomStats;
import games.strategy.triplea.delegate.GameStepPropertiesHelper;
import games.strategy.triplea.delegate.remote.IAbstractForumPosterDelegate;
import games.strategy.triplea.ui.ActionButtonsPanel;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.history.HistoryLog;
import java.util.Collection;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.SwingAction;

/** A panel used to configure and post a PBEM/PBF game. */
public final class ForumPosterComponent extends JPanel {
  private static final long serialVersionUID = 4754052934098190357L;

  private final GameData data;
  private PbemMessagePoster poster;
  private TripleAFrame frame;
  private HistoryLog historyLog;
  private final JButton postButton;
  private final JCheckBox includeTerritoryCheckBox;
  private final JCheckBox includeProductionCheckBox;
  private final JCheckBox showDetailsCheckBox;
  private final JCheckBox showDiceStatisticsCheckBox;
  private final JCheckBox includeSavegameCheckBox;
  private final JCheckBox repostTurnSummaryCheckBox;
  private final Runnable doneAction;
  private final String title;
  private IAbstractForumPosterDelegate forumPosterDelegate;

  public ForumPosterComponent(final GameData data, final Runnable doneAction, final String title) {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    setBorder(new EmptyBorder(5, 5, 0, 0));

    this.data = data;
    this.doneAction = doneAction;
    this.title = title;

    includeTerritoryCheckBox =
        new JCheckBox(SwingAction.of("Include territory summary", e -> updateHistoryLog()));
    includeProductionCheckBox =
        new JCheckBox(SwingAction.of("Include Production Summary", e -> updateHistoryLog()));
    showDetailsCheckBox =
        new JCheckBox(SwingAction.of("Show dice/battle details", e -> updateHistoryLog()));
    showDiceStatisticsCheckBox =
        new JCheckBox(SwingAction.of("Include Overall Dice Statistics", e -> updateHistoryLog()));
    includeSavegameCheckBox = new JCheckBox("Include SaveGame");
    repostTurnSummaryCheckBox =
        new JCheckBox(SwingAction.of("Repost " + title, e -> updateAllowPost()));
    postButton = new JButton(SwingAction.of("Post " + title, e -> post()));
  }

  private void updateAllowPost() {
    if (repostTurnSummaryCheckBox.isSelected()) {
      postButton.setEnabled(true);
    } else if (forumPosterDelegate != null) {
      postButton.setEnabled(!forumPosterDelegate.getHasPostedTurnSummary());
    }
  }

  private void post() {
    postButton.setEnabled(false);
    updateHistoryLog();
    poster.postTurn(
        title,
        historyLog,
        includeSavegameCheckBox.isSelected(),
        forumPosterDelegate,
        frame,
        frame.getGame(),
        postButton);
    repostTurnSummaryCheckBox.setSelected(false);
  }

  /**
   * Invoked by the parent container to layout the components of this panel based on the specified
   * state.
   *
   * @return A reference to this panel; must be added to the parent container's layout.
   */
  public ForumPosterComponent layoutComponents(
      final PbemMessagePoster poster,
      final IAbstractForumPosterDelegate forumPosterDelegate,
      final TripleAFrame frame,
      final boolean hasPosted,
      final boolean includeDetailsAndSummary) {
    this.forumPosterDelegate = forumPosterDelegate;
    this.frame = frame;
    this.poster = poster;
    historyLog = new HistoryLog(frame);
    updateHistoryLog();
    // only show widgets if there are PBEM messengers
    removeAll();

    if (includeDetailsAndSummary) {
      add(includeTerritoryCheckBox);
      add(includeProductionCheckBox);
      showDetailsCheckBox.setSelected(true);
      add(showDetailsCheckBox);
      add(showDiceStatisticsCheckBox);
    }

    includeSavegameCheckBox.setSelected(data.getProperties().get("INCLUDE_SAVEGAME", false));
    add(includeSavegameCheckBox);
    repostTurnSummaryCheckBox.setSelected(!hasPosted);
    add(repostTurnSummaryCheckBox);
    add(new JButton(SwingAction.of("View " + title, e -> viewHistoryLog())));
    postButton.setEnabled(!hasPosted);
    add(postButton);
    add(
        new JButtonBuilder()
            .title("Done")
            .actionListener(doneAction)
            .toolTip(ActionButtonsPanel.DONE_BUTTON_TOOLTIP)
            .build());
    validate();
    return this;
  }

  private void viewHistoryLog() {
    historyLog.setVisible(true);
  }

  private void updateHistoryLog() {
    final Collection<GamePlayer> allowedIDs = GameStepPropertiesHelper.getTurnSummaryPlayers(data);
    // clear first, then update
    historyLog.clear();
    historyLog.printFullTurn(data, showDetailsCheckBox.isSelected(), allowedIDs);
    if (includeTerritoryCheckBox.isSelected()) {
      historyLog.printTerritorySummary(data, allowedIDs);
    }
    if (includeProductionCheckBox.isSelected()) {
      historyLog.printProductionSummary(data);
    }
    if (showDiceStatisticsCheckBox.isSelected()) {
      historyLog.printDiceStatistics(
          data,
          (IRandomStats)
              frame.getGame().getMessengers().getRemote(IRandomStats.RANDOM_STATS_REMOTE_NAME));
    }
    historyLog.requestFocus();
  }

  public boolean canPostTurnSummary() {
    return forumPosterDelegate != null;
  }

  public boolean getHasPostedTurnSummary() {
    return forumPosterDelegate.getHasPostedTurnSummary();
  }
}
