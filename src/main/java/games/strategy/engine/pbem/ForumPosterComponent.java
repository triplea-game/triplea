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
  protected GameData m_data;
  // protected JLabel m_actionLabel;
  protected IPlayerBridge m_bridge;
  protected PBEMMessagePoster m_poster;
  protected MainGameFrame m_frame;
  protected HistoryLog m_historyLog;
  protected JButton m_postButton;
  protected JCheckBox m_includeTerritoryCheckbox;
  protected JCheckBox m_includeTerritoryAllPlayersCheckbox;
  protected JCheckBox m_includeProductionCheckbox;
  protected JCheckBox m_showDetailsCheckbox;
  protected JCheckBox m_showDiceStatisticsCheckbox;
  protected JCheckBox m_includeSavegameCheckBox;
  protected JCheckBox m_repostTurnSummaryCheckBox;
  protected Action m_viewAction;
  protected Action m_postAction;
  protected Action m_repostAction;
  protected Action m_includeTerritoryAction;
  protected Action m_includeTerritoryAllPlayersAction;
  protected Action m_includeProductionAction;
  protected Action m_showDetailsAction;
  protected Action m_showDiceStatisticsAction;
  protected Action m_doneAction;
  protected String m_title;
  protected IAbstractForumPosterDelegate m_forumPosterDelegate;

  public ForumPosterComponent(final GameData data, final Action doneAction, final String title) {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    setBorder(new EmptyBorder(5, 5, 0, 0));
    m_data = data;
    m_title = title;
    // m_actionLabel = new JLabel();
    m_viewAction = SwingAction.of("View " + m_title, e -> m_historyLog.setVisible(true));
    m_postAction = SwingAction.of("Post " + m_title, e -> {
      m_postButton.setEnabled(false);
      updateHistoryLog();
      PBEMMessagePoster.postTurn(m_title, m_historyLog, m_includeSavegameCheckBox.isSelected(), m_poster,
          m_forumPosterDelegate, m_frame, m_postButton);
      m_repostTurnSummaryCheckBox.setSelected(false);
    });
    m_includeTerritoryAction = SwingAction.of("Include territory summary", e ->

    updateHistoryLog());
    m_includeTerritoryAllPlayersAction = SwingAction.of("Include Full Territory Summary", e -> updateHistoryLog());
    m_includeProductionAction = SwingAction.of("Include Production Summary", e -> updateHistoryLog());
    m_showDetailsAction = SwingAction.of("Show dice/battle details", e -> updateHistoryLog());
    m_showDiceStatisticsAction = SwingAction.of("Include Overall Dice Statistics", e -> updateHistoryLog());
    m_repostAction = SwingAction.of("Repost " + m_title, e -> {
      if (m_repostTurnSummaryCheckBox.isSelected()) {
        m_postButton.setEnabled(true);
      } else {
        if (m_forumPosterDelegate != null) {
          m_postButton.setEnabled(!m_forumPosterDelegate.getHasPostedTurnSummary());
        }
      }
    });
    m_doneAction = doneAction;
    m_includeTerritoryCheckbox = new JCheckBox(m_includeTerritoryAction);
    m_includeTerritoryAllPlayersCheckbox = new JCheckBox(m_includeTerritoryAllPlayersAction);
    m_includeProductionCheckbox = new JCheckBox(m_includeProductionAction);
    m_showDetailsCheckbox = new JCheckBox(m_showDetailsAction);
    m_showDiceStatisticsCheckbox = new JCheckBox(m_showDiceStatisticsAction);
    m_includeSavegameCheckBox = new JCheckBox("Include SaveGame");
    m_repostTurnSummaryCheckBox = new JCheckBox(m_repostAction);
  }

  public ForumPosterComponent layoutComponents(final PBEMMessagePoster poster,
      final IAbstractForumPosterDelegate forumPosterDelegate, final IPlayerBridge bridge, final MainGameFrame frame,
      final boolean hasPosted, final boolean allowIncludeTerritorySummary,
      final boolean allowIncludeTerritoryAllPlayersSummary, final boolean allowIncludeProductionSummary,
      final boolean allowDiceBattleDetails, final boolean allowDiceStatistics) {
    m_forumPosterDelegate = forumPosterDelegate;
    m_frame = frame;
    m_bridge = bridge;
    // Nothing to do if there are no PBEM messengers
    m_poster = poster;
    m_historyLog = new HistoryLog();
    updateHistoryLog();
    // only show widgets if there are PBEM messengers
    removeAll();

    if (allowIncludeTerritorySummary) {
      add(m_includeTerritoryCheckbox);
    }
    if (allowIncludeTerritoryAllPlayersSummary) {
      add(m_includeTerritoryAllPlayersCheckbox);
    }
    if (allowIncludeProductionSummary) {
      add(m_includeProductionCheckbox);
    }
    if (allowDiceBattleDetails) {
      m_showDetailsCheckbox.setSelected(true);
      add(m_showDetailsCheckbox);
    }
    if (allowDiceStatistics) {
      add(m_showDiceStatisticsCheckbox);
    }
    // we always send savegame with emails i think?
    m_includeSavegameCheckBox.setSelected(m_poster.getEmailSender() != null
        || (m_poster.getForumPoster() != null && m_poster.getForumPoster().getIncludeSaveGame()));
    add(m_includeSavegameCheckBox);
    m_repostTurnSummaryCheckBox.setSelected(!hasPosted);
    add(m_repostTurnSummaryCheckBox);
    add(new JButton(m_viewAction));
    m_postButton = new JButton(m_postAction);
    m_postButton.setEnabled(!hasPosted);
    add(m_postButton);
    add(new JButton(m_doneAction));
    validate();
    // }
    // });
    // waitForRelease();
    return this;
  }

  private void updateHistoryLog() {
    final Collection<PlayerID> allowedIDs = GameStepPropertiesHelper.getTurnSummaryPlayers(m_data);
    // clear first, then update
    m_historyLog.clear();
    m_historyLog.printFullTurn(m_data, m_showDetailsCheckbox.isSelected(), allowedIDs);
    if (m_includeTerritoryAllPlayersCheckbox.isSelected()) {
      for (final PlayerID player : m_data.getPlayerList().getPlayers()) {
        final Collection<PlayerID> players = new ArrayList<>();
        players.add(player);
        m_historyLog.printTerritorySummary(m_data, players);
      }
    } else if (m_includeTerritoryCheckbox.isSelected()) {
      m_historyLog.printTerritorySummary(m_data, allowedIDs);
    }
    if (m_includeProductionCheckbox.isSelected()) {
      m_historyLog.printProductionSummary(m_data);
    }
    if (m_showDiceStatisticsCheckbox.isSelected()) {
      m_historyLog.printDiceStatistics(m_data,
          (IRandomStats) m_frame.getGame().getRemoteMessenger().getRemote(IRandomStats.RANDOM_STATS_REMOTE_NAME));
    }
    m_historyLog.requestFocus();
  }
  
  public boolean getHasPostedTurnSummary() {
    return m_forumPosterDelegate.getHasPostedTurnSummary();
  }
}
