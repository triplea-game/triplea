package games.strategy.triplea.ui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.util.ResourceCollectionUtils;
import games.strategy.sound.ClipPlayer;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.UserActionAttachment;
import games.strategy.triplea.delegate.remote.IUserActionDelegate;
import games.strategy.ui.SwingAction;

/**
 * Similar to PoliticsPanel, but for UserActionAttachment/Delegate.
 */
public class UserActionPanel extends ActionPanel {
  private static final long serialVersionUID = -2735582890226625860L;
  private final JLabel m_actionLabel = new JLabel();
  private JButton m_selectUserActionButton = null;
  private JButton m_doneButton = null;
  private UserActionAttachment m_choice = null;
  private final TripleAFrame m_parent;
  private boolean m_firstRun = true;
  private List<UserActionAttachment> m_validUserActions = Collections.emptyList();

  public UserActionPanel(final GameData data, final MapPanel map, final TripleAFrame parent) {
    super(data, map);
    m_parent = parent;
  }

  @Override
  public String toString() {
    return "Actions and Operations Panel";
  }

  @Override
  public void display(final PlayerID id) {
    super.display(id);
    m_choice = null;
    SwingUtilities.invokeLater(() -> {
      removeAll();
      m_actionLabel.setText(id.getName() + " Actions and Operations");
      add(m_actionLabel);
      m_selectUserActionButton = new JButton(SelectUserActionAction);
      m_selectUserActionButton.setEnabled(false);
      add(m_selectUserActionButton);
      m_doneButton = new JButton(DontBotherAction);
      m_doneButton.setEnabled(false);
      SwingUtilities.invokeLater(() -> m_doneButton.requestFocusInWindow());
      add(m_doneButton);
    });
  }

  /**
   * waits till someone calls release() and then returns the action chosen.
   *
   * @return the choice of action
   */
  public UserActionAttachment waitForUserActionAction(final boolean firstRun,
      final IUserActionDelegate iUserActionsDelegate) {
    m_firstRun = firstRun;

    m_validUserActions = new ArrayList<>(iUserActionsDelegate.getValidActions());
    Collections.sort(m_validUserActions, new UserActionComparator());
    if (m_validUserActions.isEmpty()) {
      // No Valid User actions, do nothing
      return null;
    } else {
      if (m_firstRun) {
        ClipPlayer.play(SoundPath.CLIP_PHASE_USER_ACTIONS, getCurrentPlayer());
      }
      SwingUtilities.invokeLater(() -> {
        m_selectUserActionButton.setEnabled(true);
        m_doneButton.setEnabled(true);
        // press the user action button for us.
        SelectUserActionAction.actionPerformed(null);
      });
    }
    waitForRelease();
    return m_choice;
  }

  /**
   * Fires up a JDialog showing valid actions,
   * choosing an action will release this model and trigger waitForRelease().
   */
  private final Action SelectUserActionAction = new AbstractAction("Take Action...") {
    private static final long serialVersionUID = 2389485901611958851L;

    @Override
    public void actionPerformed(final ActionEvent event) {
      final JDialog userChoiceDialog = new JDialog(m_parent, "Actions and Operations", true);

      final JPanel userChoicePanel = new JPanel();
      userChoicePanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
      userChoicePanel.setLayout(new GridBagLayout());
      int row = 0;

      final JScrollPane choiceScroll = new JScrollPane(getUserActionButtonPanel(userChoiceDialog));
      choiceScroll.setBorder(BorderFactory.createEtchedBorder());
      choiceScroll.setPreferredSize(getUserActionScrollPanePreferredSize(choiceScroll));
      userChoicePanel.add(choiceScroll, new GridBagConstraints(0, row++, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
          GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

      if (canSpendResourcesOnUserActions(m_validUserActions)) {
        final JLabel resourcesLabel = new JLabel(String.format("You have %s left",
            ResourceCollectionUtils.getProductionResources(getCurrentPlayer().getResources())));
        userChoicePanel.add(resourcesLabel, new GridBagConstraints(0, row++, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
            GridBagConstraints.HORIZONTAL, new Insets(8, 0, 0, 0), 0, 0));
      }

      final JButton noActionButton = new JButton(SwingAction.of("No Actions", e -> userChoiceDialog.setVisible(false)));
      SwingUtilities.invokeLater(() -> noActionButton.requestFocusInWindow());
      userChoicePanel.add(noActionButton, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
          GridBagConstraints.NONE, new Insets(12, 0, 0, 0), 0, 0));

      userChoiceDialog.add(userChoicePanel);
      userChoiceDialog.pack();
      userChoiceDialog.setLocationRelativeTo(m_parent);
      userChoiceDialog.setVisible(true);
      userChoiceDialog.dispose();
    }
  };

  @VisibleForTesting
  static boolean canSpendResourcesOnUserActions(final Collection<UserActionAttachment> userActions) {
    return userActions.stream().anyMatch(userAction -> userAction.getCostPU() > 0);
  }

  private JPanel getUserActionButtonPanel(final JDialog parent) {
    final JPanel userActionButtonPanel = new JPanel();
    userActionButtonPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    userActionButtonPanel.setLayout(new GridBagLayout());

    final int firstRow = 0;
    final int lastRow = m_validUserActions.size() - 1;
    int row = 0;
    for (final UserActionAttachment uaa : m_validUserActions) {
      final int topInset = (row == firstRow) ? 0 : 4;
      final int bottomInset = (row == lastRow) ? 0 : 4;
      final boolean canPlayerAffordUserAction = canPlayerAffordUserAction(getCurrentPlayer(), uaa);

      userActionButtonPanel.add(getOtherPlayerFlags(uaa), new GridBagConstraints(0, row, 1, 1, 0.0, 0.0,
          GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(topInset, 0, bottomInset, 4), 0, 0));

      final JButton button = new JButton(getActionButtonText(uaa));
      button.addActionListener(ae -> {
        m_selectUserActionButton.setEnabled(false);
        m_doneButton.setEnabled(false);
        m_validUserActions = Collections.emptyList();
        m_choice = uaa;
        parent.setVisible(false);
        release();
      });
      button.setEnabled(canPlayerAffordUserAction);
      userActionButtonPanel.add(button, new GridBagConstraints(1, row, 1, 1, 0.0, 0.0,
          GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(topInset, 4, bottomInset, 4), 0, 0));

      final JLabel descriptionLabel = getActionDescriptionLabel(uaa);
      descriptionLabel.setEnabled(canPlayerAffordUserAction);
      userActionButtonPanel.add(descriptionLabel, new GridBagConstraints(2, row, 1, 1, 0.0, 0.0,
          GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(topInset, 4, bottomInset, 0), 0, 0));

      row++;
    }

    return userActionButtonPanel;
  }

  @VisibleForTesting
  static boolean canPlayerAffordUserAction(final PlayerID player, final UserActionAttachment userAction) {
    return userAction.getCostPU() <= player.getResources().getQuantity(Constants.PUS);
  }

  private static Dimension getUserActionScrollPanePreferredSize(final JScrollPane scrollPane) {
    final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    final int availableHeight = screenSize.height - 120;
    final int availableWidth = screenSize.width - 30;

    return new Dimension(
        (scrollPane.getPreferredSize().width > availableWidth ? availableWidth
            : (scrollPane.getPreferredSize().width
                + (scrollPane.getPreferredSize().height > availableHeight ? 25 : 0))),
        (scrollPane.getPreferredSize().height > availableHeight ? availableHeight
            : (scrollPane.getPreferredSize().height)
                + (scrollPane.getPreferredSize().width > availableWidth ? 25 : 0)));
  }

  /**
   * This will stop the user action Phase.
   */
  private final Action DontBotherAction = new AbstractAction("Done") {
    private static final long serialVersionUID = 2835948679299520899L;

    @Override
    public void actionPerformed(final ActionEvent event) {
      if (!m_firstRun || youSureDoNothing()) {
        m_choice = null;
        release();
      }
    }

    private boolean youSureDoNothing() {
      final int rVal = JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(UserActionPanel.this),
          "Are you sure you dont want to do anything?", "End Actions", JOptionPane.YES_NO_OPTION);
      return rVal == JOptionPane.YES_OPTION;
    }
  };

  /**
   * Convenient method to get a JCompenent showing the flags involved in this
   * action.
   *
   * @param uaa
   *        the User action attachment to get the "otherflags" for
   * @return a JComponent with the flags involved.
   */
  private JPanel getOtherPlayerFlags(final UserActionAttachment uaa) {
    final JPanel panel = new JPanel();
    for (final PlayerID p : uaa.getOtherPlayers()) {
      panel.add(new JLabel(new ImageIcon(this.getMap().getUIContext().getFlagImageFactory().getFlag(p))));
    }
    return panel;
  }

  private static String getActionButtonText(final UserActionAttachment paa) {
    final String costString = paa.getCostPU() == 0 ? "" : "[" + paa.getCostPU() + " PU] ";
    return costString + UserActionText.getInstance().getButtonText(paa.getText());
  }

  private static JLabel getActionDescriptionLabel(final UserActionAttachment paa) {
    final String chanceString = paa.getChanceToHit() >= paa.getChanceDiceSides() ? ""
        : "[" + paa.getChanceToHit() + "/" + paa.getChanceDiceSides() + "] ";
    return new JLabel(chanceString + UserActionText.getInstance().getDescription(paa.getText()));
  }

  private static final class UserActionComparator implements Comparator<UserActionAttachment> {
    @Override
    public int compare(final UserActionAttachment uaa1, final UserActionAttachment uaa2) {
      if (uaa1.equals(uaa2)) {
        return 0;
      }
      return uaa1.getName().compareTo(uaa2.getName());
    }
  }
}
