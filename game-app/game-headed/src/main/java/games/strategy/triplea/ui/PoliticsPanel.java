package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.triplea.attachments.PoliticalActionAttachment;
import games.strategy.triplea.delegate.remote.IPoliticsDelegate;
import games.strategy.triplea.util.PlayerOrderComparator;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.sound.ClipPlayer;
import org.triplea.sound.SoundPath;
import org.triplea.swing.SwingAction;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.key.binding.ButtonDownMask;
import org.triplea.swing.key.binding.KeyCode;
import org.triplea.swing.key.binding.KeyCombination;
import org.triplea.swing.key.binding.SwingKeyBinding;

/**
 * This panel is fired by ActionButtons and controls the selection of a valid political action to
 * attempt.
 */
public class PoliticsPanel extends ActionPanel {
  private static final long serialVersionUID = -4661479948450261578L;
  private final ClipPlayer clipPlayer;
  private JButton selectPoliticalActionButton = null;
  private JButton doneButton = null;
  private PoliticalActionAttachment choice = null;
  private boolean firstRun = true;
  private List<PoliticalActionAttachment> validPoliticalActions = null;

  /**
   * Fires up a JDialog showing the political landscape and valid actions, choosing an action will
   * release this model and trigger waitForRelease().
   */
  private final Action selectPoliticalActionAction;

  PoliticsPanel(final TripleAFrame frame) {
    super(frame);
    this.clipPlayer = frame.getUiContext().getClipPlayer();
    selectPoliticalActionAction =
        SwingAction.of(
            "Do Politics...",
            e -> {
              final Dimension screenResolution = Toolkit.getDefaultToolkit().getScreenSize();
              final int availHeight = screenResolution.height - 96;
              final int availWidth = screenResolution.width - 30;
              final int availHeightOverview = (int) ((float) availHeight * 2 / 3);
              final int availHeightChoice = (int) ((float) availHeight / 3);

              final JDialog politicalChoiceDialog = new JDialog(frame, "Political Actions", true);
              final Insets insets = new Insets(1, 1, 1, 1);
              final JPanel politicalChoicePanel = new JPanel();
              politicalChoicePanel.setLayout(new GridBagLayout());
              final PoliticalStateOverview overview =
                  new PoliticalStateOverview(getData(), getMap().getUiContext(), false);
              final JScrollPane overviewScroll = new JScrollPane(overview);
              overviewScroll.setBorder(BorderFactory.createEmptyBorder());
              // add 26 to height when the actions are empty, because for some stupid reason java
              // calculates the pack size wrong
              // (again)...
              // add 20 to either when the opposite needs scroll bars, because that is how big
              // scroll bars are..
              overviewScroll.setPreferredSize(
                  new Dimension(
                      (overviewScroll.getPreferredSize().width > availWidth
                          ? availWidth
                          : (overviewScroll.getPreferredSize().width
                              + (overviewScroll.getPreferredSize().height > availHeightOverview
                                  ? 20
                                  : 0))),
                      (overviewScroll.getPreferredSize().height > availHeightOverview
                          ? availHeightOverview
                          : (overviewScroll.getPreferredSize().height
                              + (validPoliticalActions.isEmpty() ? 26 : 0)
                              + (overviewScroll.getPreferredSize().width > availWidth ? 20 : 0)))));

              final JScrollPane choiceScroll =
                  new JScrollPane(politicalActionButtonPanel(politicalChoiceDialog));
              choiceScroll.setBorder(BorderFactory.createEmptyBorder());
              choiceScroll.setPreferredSize(
                  new Dimension(
                      (choiceScroll.getPreferredSize().width > availWidth
                          ? availWidth
                          : (choiceScroll.getPreferredSize().width
                              + (choiceScroll.getPreferredSize().height > availHeightChoice
                                  ? 20
                                  : 0))),
                      (choiceScroll.getPreferredSize().height > availHeightChoice
                          ? availHeightChoice
                          : choiceScroll.getPreferredSize().height
                              + (choiceScroll.getPreferredSize().width > availWidth ? 20 : 0))));

              final JSplitPane splitPane =
                  new JSplitPane(JSplitPane.VERTICAL_SPLIT, overviewScroll, choiceScroll);
              splitPane.setOneTouchExpandable(true);
              splitPane.setDividerSize(8);
              politicalChoicePanel.add(
                  splitPane,
                  new GridBagConstraints(
                      0,
                      0,
                      1,
                      1,
                      100.0,
                      100.0,
                      GridBagConstraints.CENTER,
                      GridBagConstraints.BOTH,
                      insets,
                      0,
                      0));
              SwingKeyBinding.addKeyBinding(
                  politicalChoiceDialog,
                  KeyCombination.of(KeyCode.ENTER, ButtonDownMask.CTRL),
                  () -> politicalChoiceDialog.setVisible(false));
              SwingKeyBinding.addKeyBinding(
                  politicalChoiceDialog,
                  KeyCode.ESCAPE,
                  () -> politicalChoiceDialog.setVisible(false));
              final JButton doneButton =
                  new JButton(
                      SwingAction.of("Done", event -> politicalChoiceDialog.setVisible(false)));
              SwingUtilities.invokeLater(doneButton::requestFocusInWindow);
              politicalChoicePanel.add(
                  doneButton,
                  new GridBagConstraints(
                      0,
                      1,
                      20,
                      1,
                      1.0,
                      1.0,
                      GridBagConstraints.EAST,
                      GridBagConstraints.NONE,
                      insets,
                      0,
                      0));
              politicalChoiceDialog.add(politicalChoicePanel);
              politicalChoiceDialog.pack();
              politicalChoiceDialog.setLocationRelativeTo(frame);
              politicalChoiceDialog.setVisible(true);
              politicalChoiceDialog.dispose();
            });
  }

  @Override
  public String toString() {
    return "Politics Panel";
  }

  @Override
  public void display(final GamePlayer gamePlayer) {
    super.display(gamePlayer);
    choice = null;
    SwingUtilities.invokeLater(
        () -> {
          removeAll();
          actionLabel.setText(gamePlayer.getName() + " Politics");
          add(SwingComponents.leftBox(actionLabel));

          selectPoliticalActionButton = new JButton(selectPoliticalActionAction);
          selectPoliticalActionButton.setEnabled(false);
          doneButton = createDoneButton();
          doneButton.setEnabled(false);

          add(createButtonsPanel(selectPoliticalActionButton, doneButton));

          SwingUtilities.invokeLater(() -> doneButton.requestFocusInWindow());
        });
  }

  @Override
  public void performDone() {
    if (!firstRun || youSureDoNothing()) {
      choice = null;
      release();
    }
  }

  /**
   * waits till someone calls release() and then returns the political action chosen.
   *
   * @return the choice of political action
   */
  PoliticalActionAttachment waitForPoliticalAction(
      final boolean firstRun, final IPoliticsDelegate politicsDelegate) {
    this.firstRun = firstRun;

    // Never use a delegate or bridge from a UI. Multiplayer games will not work.
    validPoliticalActions = new ArrayList<>(politicsDelegate.getValidActions());
    validPoliticalActions.sort(new PoliticalActionComparator(getCurrentPlayer(), getData()));
    if (this.firstRun && validPoliticalActions.isEmpty()) {
      // No Valid political actions, do nothing
      return null;
    }

    if (this.firstRun) {
      clipPlayer.play(SoundPath.CLIP_PHASE_POLITICS, getCurrentPlayer());
    }
    SwingUtilities.invokeLater(
        () -> {
          selectPoliticalActionButton.setEnabled(true);
          doneButton.setEnabled(true);
          // press the politics button for us.
          selectPoliticalActionAction.actionPerformed(null);
        });

    waitForRelease();
    return choice;
  }

  private JPanel politicalActionButtonPanel(final JDialog parent) {
    final JPanel politicalActionButtonPanel = new JPanel();
    politicalActionButtonPanel.setLayout(new GridBagLayout());
    int row = 0;
    final Insets insets = new Insets(1, 1, 1, 1);
    for (final PoliticalActionAttachment paa : validPoliticalActions) {
      politicalActionButtonPanel.add(
          getOtherPlayerFlags(paa),
          new GridBagConstraints(
              0,
              row,
              1,
              1,
              1.0,
              1.0,
              GridBagConstraints.WEST,
              GridBagConstraints.HORIZONTAL,
              insets,
              0,
              0));
      final JButton button =
          getMap()
              .getUiContext()
              .getResourceImageFactory()
              .getResourcesButton(
                  new ResourceCollection(getData(), paa.getCostResources()),
                  new PoliticsText(getMap().getUiContext().getResourceLoader())
                      .getButtonText(paa.getText()));
      button.addActionListener(
          ae -> {
            selectPoliticalActionButton.setEnabled(false);
            doneButton.setEnabled(false);
            validPoliticalActions = null;
            choice = paa;
            parent.setVisible(false);
            release();
          });
      politicalActionButtonPanel.add(
          button,
          new GridBagConstraints(
              1,
              row,
              1,
              1,
              1.0,
              1.0,
              GridBagConstraints.WEST,
              GridBagConstraints.HORIZONTAL,
              insets,
              0,
              0));
      politicalActionButtonPanel.add(
          getActionDescriptionLabel(paa),
          new GridBagConstraints(
              2,
              row,
              1,
              1,
              5.0,
              1.0,
              GridBagConstraints.WEST,
              GridBagConstraints.HORIZONTAL,
              insets,
              0,
              0));
      row++;
    }
    return politicalActionButtonPanel;
  }

  private boolean youSureDoNothing() {
    final int selectedOption =
        JOptionPane.showConfirmDialog(
            JOptionPane.getFrameForComponent(PoliticsPanel.this),
            "Are you sure you don't want to do anything?",
            "End Politics",
            JOptionPane.YES_NO_OPTION);
    return selectedOption == JOptionPane.YES_OPTION;
  }

  /**
   * Convenient method to get a JComponent showing the flags involved in this action.
   *
   * @param paa the political action attachment to get the "otherflags" for
   * @return a JComponent with the flags involved.
   */
  private JPanel getOtherPlayerFlags(final PoliticalActionAttachment paa) {
    final JPanel panel = new JPanel();
    for (final GamePlayer p : paa.getOtherPlayers()) {
      panel.add(
          new JLabel(new ImageIcon(this.getMap().getUiContext().getFlagImageFactory().getFlag(p))));
    }
    return panel;
  }

  private JLabel getActionDescriptionLabel(final PoliticalActionAttachment paa) {
    final String chanceString =
        paa.getChanceToHit() >= paa.getChanceDiceSides()
            ? ""
            : "[" + paa.getChanceToHit() + "/" + paa.getChanceDiceSides() + "] ";
    return new JLabel(
        chanceString
            + new PoliticsText(getMap().getUiContext().getResourceLoader())
                .getDescription(paa.getText()));
  }

  private static final class PoliticalActionComparator
      implements Comparator<PoliticalActionAttachment>, Serializable {
    private static final long serialVersionUID = -383223878890794945L;
    private final GameData gameData;
    private final GamePlayer player;

    PoliticalActionComparator(final GamePlayer currentPlayer, final GameData data) {
      gameData = data;
      player = currentPlayer;
    }

    @Override
    public int compare(final PoliticalActionAttachment paa1, final PoliticalActionAttachment paa2) {
      if (paa1.equals(paa2)) {
        return 0;
      }
      final PoliticalActionAttachment.RelationshipChange paa1RelationshipChange =
          CollectionUtils.getAny(paa1.getRelationshipChanges());
      final PoliticalActionAttachment.RelationshipChange paa2RelationshipChange =
          CollectionUtils.getAny(paa2.getRelationshipChanges());
      final RelationshipType paa1NewType = paa1RelationshipChange.relationshipType;
      final RelationshipType paa2NewType = paa2RelationshipChange.relationshipType;
      // sort by player
      final GamePlayer paa1p1 = paa1RelationshipChange.player1;
      final GamePlayer paa1p2 = paa1RelationshipChange.player2;
      final GamePlayer paa2p1 = paa2RelationshipChange.player1;
      final GamePlayer paa2p2 = paa2RelationshipChange.player2;
      final GamePlayer paa1OtherPlayer = (player.equals(paa1p1) ? paa1p2 : paa1p1);
      final GamePlayer paa2OtherPlayer = (player.equals(paa2p1) ? paa2p2 : paa2p1);
      if (!paa1OtherPlayer.equals(paa2OtherPlayer)) {
        final int order =
            new PlayerOrderComparator(gameData).compare(paa1OtherPlayer, paa2OtherPlayer);
        if (order != 0) {
          return order;
        }
      }
      // sort by archetype
      if (!paa1NewType.equals(paa2NewType)) {
        if (paa1NewType.getRelationshipTypeAttachment().isWar()
            && !paa2NewType.getRelationshipTypeAttachment().isWar()) {
          return -1;
        }
        if (!paa1NewType.getRelationshipTypeAttachment().isWar()
            && paa2NewType.getRelationshipTypeAttachment().isWar()) {
          return 1;
        }
        if (paa1NewType.getRelationshipTypeAttachment().isNeutral()
            && paa2NewType.getRelationshipTypeAttachment().isAllied()) {
          return -1;
        }
        if (paa1NewType.getRelationshipTypeAttachment().isAllied()
            && paa2NewType.getRelationshipTypeAttachment().isNeutral()) {
          return 1;
        }
      }
      // sort by name of new relationship type
      if (!paa1NewType.getName().equals(paa2NewType.getName())) {
        return paa1NewType.getName().compareTo(paa2NewType.getName());
      }
      return 0;
    }
  }
}
