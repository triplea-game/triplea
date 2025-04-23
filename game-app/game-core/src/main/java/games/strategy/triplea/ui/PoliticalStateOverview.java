package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.framework.lookandfeel.LookAndFeel;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import org.triplea.swing.jpanel.JPanelBuilder;
import org.triplea.util.Triple;

/**
 * A panel that shows the current political state, this has no other functionality then a view on
 * the current politics.
 */
public class PoliticalStateOverview extends JPanel {
  private static final long serialVersionUID = -8445782272897831080L;
  private static final String LABEL_SELF = "----";

  private final UiContext uiContext;
  private final GameData data;
  private final boolean editable;
  private final Set<Triple<GamePlayer, GamePlayer, RelationshipType>> editChanges = new HashSet<>();
  private int maxColumnWidth;

  public PoliticalStateOverview(
      final GameData data, final UiContext uiContext, final boolean editable) {
    this.uiContext = uiContext;
    this.data = data;
    this.editable = editable;
    drawPoliticsUi();
  }

  /** does the actual adding of elements to this panel. */
  private void drawPoliticsUi() {
    this.setLayout(new GridBagLayout());
    final Insets insets = new Insets(5, 2, 5, 2);
    maxColumnWidth = 0;

    int x = 1;
    int y = 1;
    for (final GamePlayer p : data.getPlayerList()) {
      // add horizontal labels
      addCell(getPlayerLabel(p, JLabel.CENTER), insets, x++, 0);

      // add vertical labels and dividers
      this.add(
          new JSeparator(),
          new GridBagConstraints(
              0,
              y++,
              20,
              1,
              0.1,
              0.1,
              GridBagConstraints.WEST,
              GridBagConstraints.BOTH,
              new Insets(0, 0, 0, 0),
              0,
              0));
      this.add(
          getPlayerLabel(p, JLabel.LEFT),
          new GridBagConstraints(
              0,
              y++,
              1,
              1,
              1.0,
              1.0,
              GridBagConstraints.WEST,
              GridBagConstraints.BOTH,
              insets,
              0,
              0));
    }

    // draw cells
    x = 1;
    y = 2;
    for (final GamePlayer verticalPlayer : data.getPlayerList()) {
      for (final GamePlayer horizontalPlayer : data.getPlayerList()) {
        addCell(getRelationshipLabel(verticalPlayer, horizontalPlayer), insets, x++, y);
      }
      y = y + 2;
      x = 1;
    }

    // Add horizontal struts for all the columns to make them have the same width.
    for (int i = 0; i < data.getPlayerList().size(); i++) {
      addCell(Box.createHorizontalStrut(maxColumnWidth), insets, i + 1, y);
    }
  }

  private void addCell(final Component cell, final Insets insets, final int x, final int y) {
    maxColumnWidth = Math.max(maxColumnWidth, cell.getPreferredSize().width);
    this.add(
        cell,
        new GridBagConstraints(
            x,
            y,
            1,
            1,
            1.0,
            1.0,
            GridBagConstraints.CENTER,
            GridBagConstraints.BOTH,
            insets,
            0,
            0));
  }

  /** Gets a label showing the colored relationshipName between these two players. */
  private JPanel getRelationshipLabel(final GamePlayer player1, final GamePlayer player2) {
    if (player1.equals(player2)) {
      return new JPanelBuilder().add(new JLabel(PoliticalStateOverview.LABEL_SELF)).build();
    }
    final RelationshipType relType = computeRelationship(player1, player2);
    final JPanel panel =
        new JPanelBuilder().add(getRelationshipComponent(player1, player2, relType)).build();
    panel.setOpaque(true);
    panel.setBackground(LookAndFeel.getRelationshipTypeColor(relType));
    return panel;
  }

  private RelationshipType computeRelationship(final GamePlayer player1, final GamePlayer player2) {
    RelationshipType relType = null;
    for (final Triple<GamePlayer, GamePlayer, RelationshipType> changesSoFar : editChanges) {
      if ((player1.equals(changesSoFar.getFirst()) && player2.equals(changesSoFar.getSecond()))
          || (player2.equals(changesSoFar.getFirst())
              && player1.equals(changesSoFar.getSecond()))) {
        relType = changesSoFar.getThird();
      }
    }
    if (relType == null) {
      try (GameData.Unlocker ignored = data.acquireReadLock()) {
        relType = data.getRelationshipTracker().getRelationshipType(player1, player2);
      }
    }
    return relType;
  }

  private JComponent getRelationshipComponent(
      final GamePlayer player1, final GamePlayer player2, final RelationshipType relType) {
    if (!editable) {
      return new JLabel(relType.getName());
    }

    final JButton button = new JButton(relType.getName());
    button.addActionListener(
        e -> {
          final List<RelationshipType> types =
              new ArrayList<>(data.getRelationshipTypeList().getAllRelationshipTypes());
          types.remove(data.getRelationshipTypeList().getNullRelation());
          types.remove(data.getRelationshipTypeList().getSelfRelation());
          final Object[] possibilities = types.toArray();
          final RelationshipType chosenRelationship =
              (RelationshipType)
                  JOptionPane.showInputDialog(
                      PoliticalStateOverview.this,
                      "Change Current Relationship between "
                          + player1.getName()
                          + " and "
                          + player2.getName(),
                      "Change Current Relationship",
                      JOptionPane.PLAIN_MESSAGE,
                      null,
                      possibilities,
                      relType);
          if (chosenRelationship != null) {
            // remove any old ones
            editChanges.removeIf(
                changesSoFar ->
                    (player1.equals(changesSoFar.getFirst())
                            && player2.equals(changesSoFar.getSecond()))
                        || (player2.equals(changesSoFar.getFirst())
                            && player1.equals(changesSoFar.getSecond())));

            // see if there is actually a change
            final RelationshipType actualRelationship;
            try (GameData.Unlocker ignored = data.acquireReadLock()) {
              actualRelationship =
                  data.getRelationshipTracker().getRelationshipType(player1, player2);
            }
            if (!chosenRelationship.equals(actualRelationship)) {
              // add new change
              editChanges.add(Triple.of(player1, player2, chosenRelationship));
            }
            // redraw everything
            redrawPolitics();
          }
        });
    button.setBackground(LookAndFeel.getRelationshipTypeColor(relType));
    return button;
  }

  /**
   * Gets a label showing the flag + name of this player.
   *
   * @param player the player to get the label for
   * @param alignment the JLabel alignment
   * @return the label representing this player
   */
  protected JLabel getPlayerLabel(final GamePlayer player, final int alignment) {
    return new JLabel(
        player.getName(),
        new ImageIcon(uiContext.getFlagImageFactory().getFlag(player)),
        alignment);
  }

  /** Redraw this panel (because of changed politics). */
  public void redrawPolitics() {
    this.removeAll();
    this.drawPoliticsUi();
    this.revalidate();
  }

  @Nullable
  Collection<Triple<GamePlayer, GamePlayer, RelationshipType>> getEditChanges() {
    if (!editable) {
      return null;
    }
    return editChanges;
  }
}
