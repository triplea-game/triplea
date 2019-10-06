package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.RelationshipType;
import games.strategy.triplea.Constants;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
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
  private final Set<Triple<PlayerId, PlayerId, RelationshipType>> editChanges = new HashSet<>();
  private int maxColumnWidth;

  public PoliticalStateOverview(
      final GameData data, final UiContext uiContext, final boolean editable) {
    this.uiContext = uiContext;
    this.data = data;
    this.editable = editable;
    drawPoliticsUi();
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

  /** does the actual adding of elements to this panel. */
  private void drawPoliticsUi() {
    this.setLayout(new GridBagLayout());
    final Insets insets = new Insets(5, 2, 5, 2);
    maxColumnWidth = 0;

    // draw horizontal labels
    int x = 1;
    int y = 0;
    for (final PlayerId p : data.getPlayerList()) {
      addCell(getPlayerLabel(p), insets, x++, y);
    }

    // draw vertical labels and dividers
    x = 1;
    for (final PlayerId p : data.getPlayerList()) {
      this.add(
          new JSeparator(),
          new GridBagConstraints(
              0,
              x++,
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
          getPlayerLabel(p),
          new GridBagConstraints(
              0,
              x++,
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
    for (final PlayerId verticalPlayer : data.getPlayerList()) {
      for (final PlayerId horizontalPlayer : data.getPlayerList()) {
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

  protected JPanel wrapInJPanel(final JComponent component, final Color background) {
    final JPanel p = new JPanel();
    p.add(component);
    if (background != null) {
      p.setBackground(background);
    }
    return p;
  }

  /** Gets a label showing the colored relationshipName between these two players. */
  private JPanel getRelationshipLabel(final PlayerId player1, final PlayerId player2) {
    if (player1.equals(player2)) {
      return wrapInJPanel(new JLabel(PoliticalStateOverview.LABEL_SELF), null);
    }
    RelationshipType relType = null;
    for (final Triple<PlayerId, PlayerId, RelationshipType> changesSoFar : editChanges) {
      if ((player1.equals(changesSoFar.getFirst()) && player2.equals(changesSoFar.getSecond()))
          || (player2.equals(changesSoFar.getFirst())
              && player1.equals(changesSoFar.getSecond()))) {
        relType = changesSoFar.getThird();
      }
    }
    if (relType == null) {
      data.acquireReadLock();
      try {
        relType = data.getRelationshipTracker().getRelationshipType(player1, player2);
      } finally {
        data.releaseReadLock();
      }
    }
    return getRelationshipComponent(player1, player2, relType, getRelationshipTypeColor(relType));
  }

  protected JPanel getRelationshipComponent(
      final PlayerId player1,
      final PlayerId player2,
      final RelationshipType relType,
      final Color relColor) {
    if (!editable) {
      return wrapInJPanel(new JLabel(relType.getName()), relColor);
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
            data.acquireReadLock();
            final RelationshipType actualRelationship;
            try {
              actualRelationship =
                  data.getRelationshipTracker().getRelationshipType(player1, player2);
            } finally {
              data.releaseReadLock();
            }
            if (!chosenRelationship.equals(actualRelationship)) {
              // add new change
              editChanges.add(Triple.of(player1, player2, chosenRelationship));
            }
            // redraw everything
            redrawPolitics();
          }
        });
    button.setBackground(relColor);
    return wrapInJPanel(button, relColor);
  }

  /**
   * returns a color to represent the relationship.
   *
   * @param relType which relationship to get the color for
   * @return the color to represent this relationship
   */
  private static Color getRelationshipTypeColor(final RelationshipType relType) {
    final String archeType = relType.getRelationshipTypeAttachment().getArcheType();
    if (archeType.equals(Constants.RELATIONSHIP_ARCHETYPE_ALLIED)) {
      return Color.green;
    }
    if (archeType.equals(Constants.RELATIONSHIP_ARCHETYPE_NEUTRAL)) {
      return Color.lightGray;
    }
    if (archeType.equals(Constants.RELATIONSHIP_ARCHETYPE_WAR)) {
      return Color.red;
    }
    throw new IllegalStateException(
        "PoliticsUI: RelationshipType: "
            + relType.getName()
            + " can only be of archeType Allied, Neutral or War");
  }

  /**
   * Gets a label showing the flag + name of this player.
   *
   * @param player the player to get the label for
   * @return the label representing this player
   */
  protected JLabel getPlayerLabel(final PlayerId player) {
    return new JLabel(
        player.getName(),
        new ImageIcon(uiContext.getFlagImageFactory().getFlag(player)),
        JLabel.LEFT);
  }

  /** Redraw this panel (because of changed politics). */
  public void redrawPolitics() {
    this.removeAll();
    this.drawPoliticsUi();
    this.revalidate();
  }

  Collection<Triple<PlayerId, PlayerId, RelationshipType>> getEditChanges() {
    if (!editable) {
      return null;
    }
    return editChanges;
  }
}
