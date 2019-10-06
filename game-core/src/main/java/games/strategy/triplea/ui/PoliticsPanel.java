package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.sound.ClipPlayer;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.attachments.PoliticalActionAttachment;
import games.strategy.triplea.delegate.remote.IPoliticsDelegate;
import games.strategy.triplea.util.PlayerOrderComparator;
import java.awt.Color;
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
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.SwingAction;

/**
 * This panel is fired by ActionButtons and controls the selection of a valid political action to
 * attempt.
 */
public class PoliticsPanel extends ActionPanel {
  private static final long serialVersionUID = -4661479948450261578L;
  private final JLabel actionLabel = new JLabel();
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

  PoliticsPanel(final GameData data, final MapPanel map, final TripleAFrame parent) {
    super(data, map);
    selectPoliticalActionAction =
        SwingAction.of(
            "Do Politics...",
            e -> {
              final Dimension screenResolution = Toolkit.getDefaultToolkit().getScreenSize();
              final int availHeight = screenResolution.height - 96;
              final int availWidth = screenResolution.width - 30;
              final int availHeightOverview = (int) ((float) availHeight * 2 / 3);
              final int availHeightChoice = (int) ((float) availHeight / 3);

              final JDialog politicalChoiceDialog = new JDialog(parent, "Political Actions", true);
              final Insets insets = new Insets(1, 1, 1, 1);
              final JPanel politicalChoicePanel = new JPanel();
              politicalChoicePanel.setLayout(new GridBagLayout());
              final PoliticalStateOverview overview =
                  new PoliticalStateOverview(getData(), getMap().getUiContext(), false) {

                    private GridBagConstraints makeGridBagConstraints(
                        final Insets insets, final int row) {
                      return new GridBagConstraints(
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
                          0);
                    }

                    @Override
                    protected JPanel getRelationshipComponent(
                        final PlayerId player1,
                        final PlayerId player2,
                        final RelationshipType relType,
                        final Color relColor) {
                      final JPanel panel = new JPanel();
                      panel.setOpaque(false);
                      panel.setLayout(new GridBagLayout());

                      final Insets insets = new Insets(1, 1, 1, 1);
                      int row = 0;
                      final JPanel status = wrapInJPanel(new JLabel(relType.getName()), relColor);
                      panel.add(status, makeGridBagConstraints(insets, row++));
                      if (!player1.equals(getCurrentPlayer())) {
                        return panel;
                      }

                      for (final PoliticalActionAttachment paa : validPoliticalActions) {
                        if (!paa.getOtherPlayers().contains(player2)) {
                          continue;
                        }
                        final JButton button =
                            getMap()
                                .getUiContext()
                                .getResourceImageFactory()
                                .getResourcesButton(
                                    new ResourceCollection(getData(), paa.getCostResources()),
                                    PoliticsText.getInstance().getButtonText(paa.getText()));
                        button.addActionListener(
                            ae -> {
                              selectPoliticalActionButton.setEnabled(false);
                              doneButton.setEnabled(false);
                              validPoliticalActions = null;
                              choice = paa;
                              politicalChoiceDialog.setVisible(false);
                              release();
                            });
                        panel.add(button, makeGridBagConstraints(insets, row++));
                      }

                      final GridBagConstraints constraints =
                          makeGridBagConstraints(new Insets(0, 0, 0, 0), row);
                      constraints.weighty = 1000.0;
                      panel.add(Box.createHorizontalStrut(1), constraints);
                      return panel;
                    }
                  };
              final JScrollPane overviewScroll = new JScrollPane(overview);
              overviewScroll.setBorder(BorderFactory.createEmptyBorder());
              politicalChoicePanel.add(
                  overviewScroll,
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
              final JButton noActionButton =
                  new JButton(
                      SwingAction.of(
                          "No Further Actions", event -> politicalChoiceDialog.setVisible(false)));
              SwingUtilities.invokeLater(noActionButton::requestFocusInWindow);
              politicalChoicePanel.add(
                  noActionButton,
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
              politicalChoiceDialog.setLocationRelativeTo(parent);
              politicalChoiceDialog.setVisible(true);
              politicalChoiceDialog.dispose();
            });
  }

  @Override
  public String toString() {
    return "Politics Panel";
  }

  @Override
  public void display(final PlayerId id) {
    super.display(id);
    choice = null;
    SwingUtilities.invokeLater(
        () -> {
          removeAll();
          actionLabel.setText(id.getName() + " Politics");
          add(actionLabel);
          selectPoliticalActionButton = new JButton(selectPoliticalActionAction);
          selectPoliticalActionButton.setEnabled(false);
          add(selectPoliticalActionButton);
          doneButton =
              new JButtonBuilder()
                  .title("Done")
                  .actionListener(this::performDone)
                  .toolTip(ActionButtons.DONE_BUTTON_TOOLTIP)
                  .enabled(false)
                  .build();
          SwingUtilities.invokeLater(() -> doneButton.requestFocusInWindow());
          add(doneButton);
        });
  }

  @Override
  void performDone() {
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
      ClipPlayer.play(SoundPath.CLIP_PHASE_POLITICS, getCurrentPlayer());
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

  private boolean youSureDoNothing() {
    final int selectedOption =
        JOptionPane.showConfirmDialog(
            JOptionPane.getFrameForComponent(PoliticsPanel.this),
            "Are you sure you dont want to do anything?",
            "End Politics",
            JOptionPane.YES_NO_OPTION);
    return selectedOption == JOptionPane.YES_OPTION;
  }

  private static final class PoliticalActionComparator
      implements Comparator<PoliticalActionAttachment>, Serializable {
    private static final long serialVersionUID = -383223878890794945L;
    private final GameData gameData;
    private final PlayerId player;

    PoliticalActionComparator(final PlayerId currentPlayer, final GameData data) {
      gameData = data;
      player = currentPlayer;
    }

    @Override
    public int compare(final PoliticalActionAttachment paa1, final PoliticalActionAttachment paa2) {
      if (paa1.equals(paa2)) {
        return 0;
      }
      final PoliticalActionAttachment.RelationshipChange paa1RelationshipChange =
          paa1.getRelationshipChanges().iterator().next();
      final PoliticalActionAttachment.RelationshipChange paa2RelationshipChange =
          paa2.getRelationshipChanges().iterator().next();
      final RelationshipType paa1NewType = paa1RelationshipChange.relationshipType;
      final RelationshipType paa2NewType = paa2RelationshipChange.relationshipType;
      // sort by player
      final PlayerId paa1p1 = paa1RelationshipChange.player1;
      final PlayerId paa1p2 = paa1RelationshipChange.player2;
      final PlayerId paa2p1 = paa2RelationshipChange.player1;
      final PlayerId paa2p2 = paa2RelationshipChange.player2;
      final PlayerId paa1OtherPlayer = (player.equals(paa1p1) ? paa1p2 : paa1p1);
      final PlayerId paa2OtherPlayer = (player.equals(paa2p1) ? paa2p2 : paa2p1);
      if (!paa1OtherPlayer.equals(paa2OtherPlayer)) {
        final int order =
            new PlayerOrderComparator(gameData).compare(paa1OtherPlayer, paa2OtherPlayer);
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
