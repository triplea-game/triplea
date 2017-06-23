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
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.engine.random.IRandomStats;
import games.strategy.triplea.delegate.GameStepPropertiesHelper;
import games.strategy.triplea.delegate.remote.IAbstractForumPosterDelegate;
import games.strategy.triplea.ui.MainGameFrame;
import games.strategy.triplea.ui.history.HistoryLog;
import games.strategy.ui.SwingAction;

public class ForumPosterComponent extends JPanel {
  private static final long serialVersionUID = 4754052934098190357L;
  protected GameData data;
  // protected JLabel m_actionLabel;
  protected IPlayerBridge bridge;
  protected PBEMMessagePoster m_poster;
  protected MainGameFrame frame;
  protected HistoryLog historyLog;
  protected JButton postButton;
  protected JCheckBox includeTerritoryCheckbox;
  protected JCheckBox includeTerritoryAllPlayersCheckbox;
  protected JCheckBox includeProductionCheckbox;
  protected JCheckBox showDetailsCheckbox;
  protected JCheckBox showDiceStatisticsCheckbox;
  protected JCheckBox includeSavegameCheckBox;
  protected JCheckBox repostTurnSummaryCheckBox;
  protected Action viewAction;
  protected Action postAction;
  protected Action repostAction;
  protected Action includeTerritoryAction;
  protected Action includeTerritoryAllPlayersAction;
  protected Action includeProductionAction;
  protected Action showDetailsAction;
  protected Action showDiceStatisticsAction;
  protected Action doneAction;
  protected String title;
  protected IAbstractForumPosterDelegate forumPosterDelegate;

  public ForumPosterComponent(final GameData data, final Action doneAction, final String title) {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    setBorder(new EmptyBorder(5, 5, 0, 0));
    this.data = data;
    this.title = title;
    // m_actionLabel = new JLabel();
    viewAction = SwingAction.of("View " + this.title, e -> historyLog.setVisible(true));
    postAction = SwingAction.of("Post " + this.title, e -> {
      postButton.setEnabled(false);
      updateHistoryLog();
      PBEMMessagePoster.postTurn(this.title, historyLog, includeSavegameCheckBox.isSelected(), m_poster,
          forumPosterDelegate, frame, postButton);
      repostTurnSummaryCheckBox.setSelected(false);
    });
    includeTerritoryAction = SwingAction.of("Include territory summary", e ->

    updateHistoryLog());
    includeTerritoryAllPlayersAction = SwingAction.of("Include Full Territory Summary", e -> updateHistoryLog());
    includeProductionAction = SwingAction.of("Include Production Summary", e -> updateHistoryLog());
    showDetailsAction = SwingAction.of("Show dice/battle details", e -> updateHistoryLog());
    showDiceStatisticsAction = SwingAction.of("Include Overall Dice Statistics", e -> updateHistoryLog());
    repostAction = SwingAction.of("Repost " + this.title, e -> {
      if (repostTurnSummaryCheckBox.isSelected()) {
        postButton.setEnabled(true);
      } else {
        if (forumPosterDelegate != null) {
          postButton.setEnabled(!forumPosterDelegate.getHasPostedTurnSummary());
        }
      }
    });
    this.doneAction = doneAction;
    includeTerritoryCheckbox = new JCheckBox(includeTerritoryAction);
    includeTerritoryAllPlayersCheckbox = new JCheckBox(includeTerritoryAllPlayersAction);
    includeProductionCheckbox = new JCheckBox(includeProductionAction);
    showDetailsCheckbox = new JCheckBox(showDetailsAction);
    showDiceStatisticsCheckbox = new JCheckBox(showDiceStatisticsAction);
    includeSavegameCheckBox = new JCheckBox("Include SaveGame");
    repostTurnSummaryCheckBox = new JCheckBox(repostAction);
  }

  public ForumPosterComponent layoutComponents(final PBEMMessagePoster poster,
      final IAbstractForumPosterDelegate forumPosterDelegate, final IPlayerBridge bridge, final MainGameFrame frame,
      final boolean hasPosted, final boolean allowIncludeTerritorySummary,
      final boolean allowIncludeTerritoryAllPlayersSummary, final boolean allowIncludeProductionSummary,
      final boolean allowDiceBattleDetails, final boolean allowDiceStatistics) {
    this.forumPosterDelegate = forumPosterDelegate;
    this.frame = frame;
    this.bridge = bridge;
    // Nothing to do if there are no PBEM messengers
    this.m_poster = poster;
    historyLog = new HistoryLog();
    updateHistoryLog();
    // only show widgets if there are PBEM messengers
    removeAll();

    if (allowIncludeTerritorySummary) {
      add(includeTerritoryCheckbox);
    }
    if (allowIncludeTerritoryAllPlayersSummary) {
      add(includeTerritoryAllPlayersCheckbox);
    }
    if (allowIncludeProductionSummary) {
      add(includeProductionCheckbox);
    }
    if (allowDiceBattleDetails) {
      showDetailsCheckbox.setSelected(true);
      add(showDetailsCheckbox);
    }
    if (allowDiceStatistics) {
      add(showDiceStatisticsCheckbox);
    }
    // we always send savegame with emails i think?
    includeSavegameCheckBox.setSelected(m_poster.getEmailSender() != null
        || (m_poster.getForumPoster() != null && m_poster.getForumPoster().getIncludeSaveGame()));
    add(includeSavegameCheckBox);
    repostTurnSummaryCheckBox.setSelected(!hasPosted);
    add(repostTurnSummaryCheckBox);
    add(new JButton(viewAction));
    postButton = new JButton(postAction);
    postButton.setEnabled(!hasPosted);
    add(postButton);
    add(new JButton(doneAction));
    validate();
    return this;
  }

  private void updateHistoryLog() {
    final Collection<PlayerID> allowedIDs = GameStepPropertiesHelper.getTurnSummaryPlayers(data);
    // clear first, then update
    historyLog.clear();
    historyLog.printFullTurn(data, showDetailsCheckbox.isSelected(), allowedIDs);
    if (includeTerritoryAllPlayersCheckbox.isSelected()) {
      for (final PlayerID player : data.getPlayerList().getPlayers()) {
        final Collection<PlayerID> players = new ArrayList<>();
        players.add(player);
        historyLog.printTerritorySummary(data, players);
      }
    } else if (includeTerritoryCheckbox.isSelected()) {
      historyLog.printTerritorySummary(data, allowedIDs);
    }
    if (includeProductionCheckbox.isSelected()) {
      historyLog.printProductionSummary(data);
    }
    if (showDiceStatisticsCheckbox.isSelected()) {
      historyLog.printDiceStatistics(data,
          (IRandomStats) frame.getGame().getRemoteMessenger().getRemote(IRandomStats.RANDOM_STATS_REMOTE_NAME));
    }
    historyLog.requestFocus();
  }
  
  public boolean getHasPostedTurnSummary() {
    return forumPosterDelegate.getHasPostedTurnSummary();
  }
}
