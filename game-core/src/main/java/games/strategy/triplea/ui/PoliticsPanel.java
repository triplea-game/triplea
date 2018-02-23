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

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.RelationshipTypeList;
import games.strategy.sound.ClipPlayer;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.attachments.PoliticalActionAttachment;
import games.strategy.triplea.delegate.remote.IPoliticsDelegate;
import games.strategy.triplea.util.PlayerOrderComparator;
import games.strategy.ui.SwingAction;

/**
 * This panel is fired by ActionButtons and controls the selection of a valid
 * political action to attempt.
 */
public class PoliticsPanel extends ActionPanel {
  private static final long serialVersionUID = -4661479948450261578L;
  private final JLabel actionLabel = new JLabel();
  private JButton selectPoliticalActionButton = null;
  private JButton doneButton = null;
  private PoliticalActionAttachment choice = null;
  private TripleAFrame parent = null;
  private boolean firstRun = true;
  protected List<PoliticalActionAttachment> validPoliticalActions = null;

  public PoliticsPanel(final GameData data, final MapPanel map, final TripleAFrame parent) {
    super(data, map);
    this.parent = parent;
  }

  @Override
  public String toString() {
    return "Politics Panel";
  }

  @Override
  public void display(final PlayerID id) {
    super.display(id);
    choice = null;
    SwingUtilities.invokeLater(() -> {
      removeAll();
      actionLabel.setText(id.getName() + " Politics");
      add(actionLabel);
      selectPoliticalActionButton = new JButton(selectPoliticalActionAction);
      selectPoliticalActionButton.setEnabled(false);
      add(selectPoliticalActionButton);
      doneButton = new JButton(dontBotherAction);
      doneButton.setEnabled(false);
      SwingUtilities.invokeLater(() -> doneButton.requestFocusInWindow());
      add(doneButton);
    });
  }

  /**
   * waits till someone calls release() and then returns the political action
   * chosen.
   *
   * @return the choice of political action
   */
  public PoliticalActionAttachment waitForPoliticalAction(final boolean firstRun,
      final IPoliticsDelegate politicsDelegate) {
    this.firstRun = firstRun;

    // Never use a delegate or bridge from a UI. Multiplayer games will not work.
    validPoliticalActions = new ArrayList<>(politicsDelegate.getValidActions());
    Collections.sort(validPoliticalActions, new PoliticalActionComparator(getCurrentPlayer(), getData()));
    if (this.firstRun && validPoliticalActions.isEmpty()) {
      // No Valid political actions, do nothing
      return null;
    }

    if (this.firstRun) {
      ClipPlayer.play(SoundPath.CLIP_PHASE_POLITICS, getCurrentPlayer());
    }
    SwingUtilities.invokeLater(() -> {
      selectPoliticalActionButton.setEnabled(true);
      doneButton.setEnabled(true);
      // press the politics button for us.
      selectPoliticalActionAction.actionPerformed(null);
    });

    waitForRelease();
    return choice;
  }

  /**
   * Fires up a JDialog showing the political landscape and valid actions,
   * choosing an action will release this model and trigger waitForRelease().
   */
  private final Action selectPoliticalActionAction = SwingAction.of("Do Politics...", e -> {
    final Dimension screenResolution = Toolkit.getDefaultToolkit().getScreenSize();
    final int availHeight = screenResolution.height - 96;
    final int availWidth = screenResolution.width - 30;
    final int availHeightOverview = (int) (((float) availHeight * 2) / 3);
    final int availHeightChoice = (int) ((float) availHeight / 3);

    final JDialog politicalChoiceDialog = new JDialog(parent, "Political Actions", true);
    final Insets insets = new Insets(1, 1, 1, 1);
    final JPanel politicalChoicePanel = new JPanel();
    politicalChoicePanel.setLayout(new GridBagLayout());
    final PoliticalStateOverview overview = new PoliticalStateOverview(getData(), getMap().getUiContext(), false);
    final JScrollPane overviewScroll = new JScrollPane(overview);
    overviewScroll.setBorder(BorderFactory.createEmptyBorder());
    // add 26 to height when the actions are empty, because for some stupid reason java calculates the pack size wrong
    // (again)...
    // add 20 to either when the opposite needs scroll bars, because that is how big scroll bars are..
    overviewScroll.setPreferredSize(new Dimension(
        ((overviewScroll.getPreferredSize().width > availWidth) ? availWidth
            : (overviewScroll.getPreferredSize().width
            + ((overviewScroll.getPreferredSize().height > availHeightOverview) ? 20 : 0))),
        ((overviewScroll.getPreferredSize().height > availHeightOverview) ? availHeightOverview
            : (overviewScroll.getPreferredSize().height + (validPoliticalActions.isEmpty() ? 26 : 0)
            + ((overviewScroll.getPreferredSize().width > availWidth) ? 20 : 0)))));

    final JScrollPane choiceScroll = new JScrollPane(politicalActionButtonPanel(politicalChoiceDialog));
    choiceScroll.setBorder(BorderFactory.createEmptyBorder());
    choiceScroll.setPreferredSize(new Dimension(
        ((choiceScroll.getPreferredSize().width > availWidth) ? availWidth
            : (choiceScroll.getPreferredSize().width
            + ((choiceScroll.getPreferredSize().height > availHeightChoice) ? 20 : 0))),
        ((choiceScroll.getPreferredSize().height > availHeightChoice) ? availHeightChoice
            : ((choiceScroll.getPreferredSize().height)
            + ((choiceScroll.getPreferredSize().width > availWidth) ? 20 : 0)))));

    final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, overviewScroll, choiceScroll);
    splitPane.setOneTouchExpandable(true);
    splitPane.setDividerSize(8);
    politicalChoicePanel.add(splitPane, new GridBagConstraints(0, 0, 1, 1, 100.0, 100.0,
        GridBagConstraints.CENTER, GridBagConstraints.BOTH, insets, 0, 0));
    final JButton noActionButton =
        new JButton(SwingAction.of("No Actions", event -> politicalChoiceDialog.setVisible(false)));
    SwingUtilities.invokeLater(() -> noActionButton.requestFocusInWindow());
    politicalChoicePanel.add(noActionButton, new GridBagConstraints(0, 1, 20, 1, 1.0, 1.0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, insets, 0, 0));
    politicalChoiceDialog.add(politicalChoicePanel);
    politicalChoiceDialog.pack();
    politicalChoiceDialog.setLocationRelativeTo(parent);
    politicalChoiceDialog.setVisible(true);
    politicalChoiceDialog.dispose();

  });

  private JPanel politicalActionButtonPanel(final JDialog parent) {
    final JPanel politicalActionButtonPanel = new JPanel();
    politicalActionButtonPanel.setLayout(new GridBagLayout());
    int row = 0;
    final Insets insets = new Insets(1, 1, 1, 1);
    for (final PoliticalActionAttachment paa : validPoliticalActions) {
      politicalActionButtonPanel.add(getOtherPlayerFlags(paa), new GridBagConstraints(0, row, 1, 1, 1.0, 1.0,
          GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insets, 0, 0));
      final JButton button = new JButton(getActionButtonText(paa));
      button.addActionListener(ae -> {
        selectPoliticalActionButton.setEnabled(false);
        doneButton.setEnabled(false);
        validPoliticalActions = null;
        choice = paa;
        parent.setVisible(false);
        release();
      });
      politicalActionButtonPanel.add(button, new GridBagConstraints(1, row, 1, 1, 1.0, 1.0, GridBagConstraints.WEST,
          GridBagConstraints.HORIZONTAL, insets, 0, 0));
      politicalActionButtonPanel.add(getActionDescriptionLabel(paa), new GridBagConstraints(2, row, 1, 1, 5.0, 1.0,
          GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insets, 0, 0));
      row++;
    }
    return politicalActionButtonPanel;
  }

  /**
   * This will stop the politicsPhase.
   */
  private final Action dontBotherAction = SwingAction.of("Done", e -> {
    if (!firstRun || youSureDoNothing()) {
      choice = null;
      release();
    }
  });

  private boolean youSureDoNothing() {
    final int selectedOption = JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(PoliticsPanel.this),
        "Are you sure you dont want to do anything?", "End Politics", JOptionPane.YES_NO_OPTION);
    return selectedOption == JOptionPane.YES_OPTION;
  }

  /**
   * Convenient method to get a JCompenent showing the flags involved in this
   * action.
   *
   * @param paa
   *        the political action attachment to get the "otherflags" for
   * @return a JComponent with the flags involved.
   */
  private JPanel getOtherPlayerFlags(final PoliticalActionAttachment paa) {
    final JPanel panel = new JPanel();
    for (final PlayerID p : paa.getOtherPlayers()) {
      panel.add(new JLabel(new ImageIcon(this.getMap().getUiContext().getFlagImageFactory().getFlag(p))));
    }
    return panel;
  }

  private static String getActionButtonText(final PoliticalActionAttachment paa) {
    final String costString = (paa.getCostPU() == 0) ? "" : ("[" + paa.getCostPU() + " PU] ");
    return costString + PoliticsText.getInstance().getButtonText(paa.getText());
  }

  private static JLabel getActionDescriptionLabel(final PoliticalActionAttachment paa) {
    final String chanceString = (paa.getChanceToHit() >= paa.getChanceDiceSides()) ? ""
        : ("[" + paa.getChanceToHit() + "/" + paa.getChanceDiceSides() + "] ");
    return new JLabel(chanceString + PoliticsText.getInstance().getDescription(paa.getText()));
  }

  private static final class PoliticalActionComparator implements Comparator<PoliticalActionAttachment> {
    private final GameData gameData;
    private final PlayerID player;

    PoliticalActionComparator(final PlayerID currentPlayer, final GameData data) {
      gameData = data;
      player = currentPlayer;
    }

    @Override
    public int compare(final PoliticalActionAttachment paa1, final PoliticalActionAttachment paa2) {
      if (paa1.equals(paa2)) {
        return 0;
      }
      final String[] paa1RelationChange = paa1.getRelationshipChange().iterator().next().split(":");
      final String[] paa2RelationChange = paa2.getRelationshipChange().iterator().next().split(":");
      final RelationshipTypeList relationshipTypeList;
      gameData.acquireReadLock();
      try {
        relationshipTypeList = gameData.getRelationshipTypeList();
      } finally {
        gameData.releaseReadLock();
      }
      final RelationshipType paa1NewType = relationshipTypeList.getRelationshipType(paa1RelationChange[2]);
      final RelationshipType paa2NewType = relationshipTypeList.getRelationshipType(paa2RelationChange[2]);
      // sort by player
      final PlayerID paa1p1 = gameData.getPlayerList().getPlayerId(paa1RelationChange[0]);
      final PlayerID paa1p2 = gameData.getPlayerList().getPlayerId(paa1RelationChange[1]);
      final PlayerID paa2p1 = gameData.getPlayerList().getPlayerId(paa2RelationChange[0]);
      final PlayerID paa2p2 = gameData.getPlayerList().getPlayerId(paa2RelationChange[1]);
      final PlayerID paa1OtherPlayer = (player.equals(paa1p1) ? paa1p2 : paa1p1);
      final PlayerID paa2OtherPlayer = (player.equals(paa2p1) ? paa2p2 : paa2p1);
      if (!paa1OtherPlayer.equals(paa2OtherPlayer)) {
        final int order = new PlayerOrderComparator(gameData).compare(paa1OtherPlayer, paa2OtherPlayer);
        if (order != 0) {
          return order;
        }
      }
      // sort by achetype
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
