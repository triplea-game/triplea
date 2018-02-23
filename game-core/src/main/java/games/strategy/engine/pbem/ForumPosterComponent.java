package games.strategy.engine.pbem;

import java.util.ArrayList;
import java.util.Collection;

import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.random.IRandomStats;
import games.strategy.triplea.delegate.GameStepPropertiesHelper;
import games.strategy.triplea.delegate.remote.IAbstractForumPosterDelegate;
import games.strategy.triplea.ui.MainGameFrame;
import games.strategy.triplea.ui.history.HistoryLog;
import games.strategy.ui.SwingAction;

/**
 * A panel used to configure and post a PBEM/PBF game.
 */
public final class ForumPosterComponent extends JPanel {
  private static final long serialVersionUID = 4754052934098190357L;

  private final GameData data;
  private PBEMMessagePoster poster;
  private MainGameFrame frame;
  private HistoryLog historyLog;
  private final JButton postButton;
  private final JCheckBox includeTerritoryCheckBox;
  private final JCheckBox includeTerritoryAllPlayersCheckBox;
  private final JCheckBox includeProductionCheckBox;
  private final JCheckBox showDetailsCheckBox;
  private final JCheckBox showDiceStatisticsCheckBox;
  private final JCheckBox includeSavegameCheckBox;
  private final JCheckBox repostTurnSummaryCheckBox;
  private final Action doneAction;
  private final String title;
  private IAbstractForumPosterDelegate forumPosterDelegate;

  public ForumPosterComponent(final GameData data, final Action doneAction, final String title) {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    setBorder(new EmptyBorder(5, 5, 0, 0));

    this.data = data;
    this.doneAction = doneAction;
    this.title = title;

    includeTerritoryCheckBox = new JCheckBox(SwingAction.of("Include territory summary", e -> updateHistoryLog()));
    includeTerritoryAllPlayersCheckBox =
        new JCheckBox(SwingAction.of("Include Full Territory Summary", e -> updateHistoryLog()));
    includeProductionCheckBox = new JCheckBox(SwingAction.of("Include Production Summary", e -> updateHistoryLog()));
    showDetailsCheckBox = new JCheckBox(SwingAction.of("Show dice/battle details", e -> updateHistoryLog()));
    showDiceStatisticsCheckBox =
        new JCheckBox(SwingAction.of("Include Overall Dice Statistics", e -> updateHistoryLog()));
    includeSavegameCheckBox = new JCheckBox("Include SaveGame");
    repostTurnSummaryCheckBox = new JCheckBox(SwingAction.of("Repost " + title, e -> updateAllowPost()));
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
    PBEMMessagePoster.postTurn(title, historyLog, includeSavegameCheckBox.isSelected(), poster,
        forumPosterDelegate, frame, postButton);
    repostTurnSummaryCheckBox.setSelected(false);
  }

  /**
   * Invoked by the parent container to layout the components of this panel based on the specified state.
   *
   * @return A reference to this panel; must be added to the parent container's layout.
   */
  public ForumPosterComponent layoutComponents(final PBEMMessagePoster poster,
      final IAbstractForumPosterDelegate forumPosterDelegate, final MainGameFrame frame,
      final boolean hasPosted, final boolean allowIncludeTerritorySummary,
      final boolean allowIncludeTerritoryAllPlayersSummary, final boolean allowIncludeProductionSummary,
      final boolean allowDiceBattleDetails, final boolean allowDiceStatistics) {
    this.forumPosterDelegate = forumPosterDelegate;
    this.frame = frame;
    this.poster = poster;
    historyLog = new HistoryLog();
    updateHistoryLog();
    // only show widgets if there are PBEM messengers
    removeAll();

    if (allowIncludeTerritorySummary) {
      add(includeTerritoryCheckBox);
    }
    if (allowIncludeTerritoryAllPlayersSummary) {
      add(includeTerritoryAllPlayersCheckBox);
    }
    if (allowIncludeProductionSummary) {
      add(includeProductionCheckBox);
    }
    if (allowDiceBattleDetails) {
      showDetailsCheckBox.setSelected(true);
      add(showDetailsCheckBox);
    }
    if (allowDiceStatistics) {
      add(showDiceStatisticsCheckBox);
    }
    // we always send savegame with emails i think?
    includeSavegameCheckBox.setSelected(
        (this.poster.getEmailSender() != null)
            || ((this.poster.getForumPoster() != null) && this.poster.getForumPoster().getIncludeSaveGame()));
    add(includeSavegameCheckBox);
    repostTurnSummaryCheckBox.setSelected(!hasPosted);
    add(repostTurnSummaryCheckBox);
    add(new JButton(SwingAction.of("View " + title, e -> viewHistoryLog())));
    postButton.setEnabled(!hasPosted);
    add(postButton);
    add(new JButton(doneAction));
    validate();
    return this;
  }

  private void viewHistoryLog() {
    historyLog.setVisible(true);
  }

  private void updateHistoryLog() {
    final Collection<PlayerID> allowedIDs = GameStepPropertiesHelper.getTurnSummaryPlayers(data);
    // clear first, then update
    historyLog.clear();
    historyLog.printFullTurn(data, showDetailsCheckBox.isSelected(), allowedIDs);
    if (includeTerritoryAllPlayersCheckBox.isSelected()) {
      for (final PlayerID player : data.getPlayerList().getPlayers()) {
        final Collection<PlayerID> players = new ArrayList<>();
        players.add(player);
        historyLog.printTerritorySummary(data, players);
      }
    } else if (includeTerritoryCheckBox.isSelected()) {
      historyLog.printTerritorySummary(data, allowedIDs);
    }
    if (includeProductionCheckBox.isSelected()) {
      historyLog.printProductionSummary(data);
    }
    if (showDiceStatisticsCheckBox.isSelected()) {
      historyLog.printDiceStatistics(data,
          (IRandomStats) frame.getGame().getRemoteMessenger().getRemote(IRandomStats.RANDOM_STATS_REMOTE_NAME));
    }
    historyLog.requestFocus();
  }

  public boolean getHasPostedTurnSummary() {
    return forumPosterDelegate.getHasPostedTurnSummary();
  }
}
