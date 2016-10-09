package games.strategy.triplea.ui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.util.ArrayList;
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

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.sound.ClipPlayer;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.attachments.UserActionAttachment;
import games.strategy.triplea.delegate.remote.IUserActionDelegate;
import games.strategy.triplea.util.JFXUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * Similar to PoliticsPanel, but for UserActionAttachment/Delegate.
 */
public class UserActionPanel extends ActionPanel {
  private static final long serialVersionUID = -2735582890226625860L;
  private final Label m_actionLabel = new Label();
  private Button m_selectUserActionButton = null;
  private Button m_doneButton = null;
  private UserActionAttachment m_choice = null;
  private final TripleAFrame m_parent;
  private boolean m_firstRun = true;
  protected List<UserActionAttachment> m_validUserActions = null;

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
    Platform.runLater(() -> {
      getChildren().clear();
      m_actionLabel.setText(id.getName() + " Actions and Operations");
      getChildren().add(m_actionLabel);
      m_selectUserActionButton = JFXUtils.getButtonWithAction(SelectUserActionAction);
      m_selectUserActionButton.setDisable(true);
      getChildren().add(m_selectUserActionButton);
      m_doneButton = JFXUtils.getButtonWithAction(DontBotherAction);
      m_doneButton.setDisable(true);
      SwingUtilities.invokeLater(() -> m_doneButton.requestFocus());
      getChildren().add(m_doneButton);
    });
  }

  /**
   * waits till someone calls release() and then returns the action chosen
   *
   * @return the choice of action
   */
  public UserActionAttachment waitForUserActionAction(final boolean firstRun,
      final IUserActionDelegate iUserActionsDelegate) {
    m_firstRun = firstRun;

    m_validUserActions = new ArrayList<>(iUserActionsDelegate.getValidActions());
    Collections.sort(m_validUserActions, new UserActionComparator(getCurrentPlayer(), getData()));
    if (m_validUserActions.isEmpty()) {
      // No Valid User actions, do nothing
      return null;
    } else {
      if (m_firstRun) {
        ClipPlayer.play(SoundPath.CLIP_PHASE_USER_ACTIONS, getCurrentPlayer());
      }
      Platform.runLater(() -> {
        m_selectUserActionButton.setDisable(false);
        m_doneButton.setDisable(false);
        // press the user action button for us.
        SelectUserActionAction.handle(null);
      });
    }
    waitForRelease();
    return m_choice;
  }

  /**
   * Fires up a JDialog showing valid actions,
   * choosing an action will release this model and trigger waitForRelease()
   */
  private final EventHandler<ActionEvent> SelectUserActionAction = e -> {
      final Dimension screenResolution = Toolkit.getDefaultToolkit().getScreenSize();
      final int availHeight = screenResolution.height - 120;
      final int availWidth = screenResolution.width - 30;
      final JDialog userChoiceDialog = new JDialog(m_parent, "Actions and Operations", true);
      final Insets insets = new Insets(1, 1, 1, 1);
      int row = 0;
      final JPanel userChoicePanel = new JPanel();
      userChoicePanel.setLayout(new GridBagLayout());
      final JScrollPane choiceScroll = new JScrollPane(getUserActionButtonPanel(userChoiceDialog));
      choiceScroll.setBorder(BorderFactory.createEtchedBorder());
      choiceScroll.setPreferredSize(new Dimension(
          (choiceScroll.getPreferredSize().width > availWidth ? availWidth
              : (choiceScroll.getPreferredSize().width
                  + (choiceScroll.getPreferredSize().height > availHeight ? 25 : 0))),
          (choiceScroll.getPreferredSize().height > availHeight ? availHeight
              : (choiceScroll.getPreferredSize().height)
                  + (choiceScroll.getPreferredSize().width > availWidth ? 25 : 0))));
      userChoicePanel.add(choiceScroll, new GridBagConstraints(0, row++, 1, 1, 100.0, 100.0, GridBagConstraints.CENTER,
          GridBagConstraints.BOTH, insets, 0, 0));
      final Button noActionButton = JFXUtils.getButtonWithAction(ev -> userChoiceDialog.setVisible(false));
      Platform.runLater(noActionButton::requestFocus);
      userChoicePanel.add(noActionButton,
          new GridBagConstraints(0, row, 20, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, insets, 0, 0));
      userChoiceDialog.setMinimumSize(new Dimension(600, 300));
      userChoiceDialog.add(userChoicePanel);
      userChoiceDialog.pack();
      userChoiceDialog.setVisible(true);
      userChoiceDialog.dispose();
  };

  private JPanel getUserActionButtonPanel(final JDialog parent) {
    final JPanel userActionButtonPanel = new JPanel();
    userActionButtonPanel.setLayout(new GridBagLayout());
    int row = 0;
    final Insets insets = new Insets(1, 1, 1, 1);
    for (final UserActionAttachment uaa : m_validUserActions) {
      userActionButtonPanel.add(getOtherPlayerFlags(uaa), new GridBagConstraints(0, row, 1, 1, 1.0, 1.0,
          GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insets, 0, 0));
      final JButton button = new JButton(getActionButtonText(uaa));
      button.addActionListener(ae -> {
        m_selectUserActionButton.setDisable(true);
        m_doneButton.setDisable(true);
        m_validUserActions = null;
        m_choice = uaa;
        parent.setVisible(false);
        release();
      });
      userActionButtonPanel.add(button, new GridBagConstraints(1, row, 1, 1, 1.0, 1.0, GridBagConstraints.WEST,
          GridBagConstraints.HORIZONTAL, insets, 0, 0));
      userActionButtonPanel.add(getActionDescriptionLabel(uaa), new GridBagConstraints(2, row, 1, 1, 5.0, 1.0,
          GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insets, 0, 0));
      row++;
    }
    return userActionButtonPanel;
  }

  /**
   * This will stop the user action Phase
   */
  private final EventHandler<ActionEvent> DontBotherAction = e -> {
      if (!m_firstRun || youSureDoNothing()) {
        m_choice = null;
        release();
      }
  };
  
  private boolean youSureDoNothing() {
    final int rVal = JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(UserActionPanel.this),
        "Are you sure you dont want to do anything?", "End Actions", JOptionPane.YES_NO_OPTION);
    return rVal == JOptionPane.YES_OPTION;
  }

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

  private String getActionButtonText(final UserActionAttachment paa) {
    final String costString = paa.getCostPU() == 0 ? "" : "[" + paa.getCostPU() + " PU] ";
    return costString + UserActionText.getInstance().getButtonText(paa.getText());
  }

  private JLabel getActionDescriptionLabel(final UserActionAttachment paa) {
    final String chanceString = paa.getChanceToHit() >= paa.getChanceDiceSides() ? ""
        : "[" + paa.getChanceToHit() + "/" + paa.getChanceDiceSides() + "] ";
    return new JLabel(chanceString + UserActionText.getInstance().getDescription(paa.getText()));
  }
}


class UserActionComparator implements Comparator<UserActionAttachment> {
  public UserActionComparator(final PlayerID currentPlayer, final GameData data) {}

  @Override
  public int compare(final UserActionAttachment uaa1, final UserActionAttachment uaa2) {
    if (uaa1.equals(uaa2)) {
      return 0;
    }
    return uaa1.getName().compareTo(uaa2.getName());
  }
}
