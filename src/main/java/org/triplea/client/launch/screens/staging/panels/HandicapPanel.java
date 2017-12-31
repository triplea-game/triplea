package org.triplea.client.launch.screens.staging.panels;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import swinglib.JPanelBuilder;

/**
 * Panel that displays each playable country and provides fields for players to adjust bid and income (providing
 * that side with an advantage).
 * <p>
 * The layout is in a table, looks similar to this:
 *
 * <pre>
 *            Bid    Income Adjustment   Income % Adjustment
 *  Allies
 *  US         __          __                  __
 *  UK         __          __                  __
 *
 *  Axis
 *  Germany    __          __                  __
 *  Japan      __          __                  __
 * </pre>
 * </p>
 */
public class HandicapPanel {

  /**
   * Builds a 'Handicap' panel, see class comment for more details.
   */
  public static List<JComponent> build(final GameData gameData) {

    final List<JComponent> selectionPanels = new ArrayList<>();
    final Set<String> alliances = gameData.getAllianceTracker().getAlliances();

    for (final String alliance : alliances) {
      final JPanelBuilder allianceSelection = JPanelBuilder.builder()
          .borderEtched()
          .gridBagLayout(4)
          .addLabel("<html><h2>" + alliance + "</h2></html")
          .addLabel("<html><b>Bid</b></html")
          .addLabel("<html><b>Income<br />Bonus</b></html")
          .addLabel("<html><b>Income<br />Multiplier</b></html");

      for (final PlayerID player : gameData.getAllianceTracker().getPlayersInAlliance(alliance)) {
        allianceSelection.add(new JLabel(player.getName()));

        final JTextField bid = new JTextField("0", 3);
        allianceSelection.add(
            JPanelBuilder.builder()
                .flowLayout()
                .add(bid)
                .add(Box.createVerticalGlue())
                .build());

        final JTextField income = new JTextField("0", 3);
        allianceSelection.add(
            JPanelBuilder.builder()
                .flowLayout()
                .add(income)
                .add(Box.createVerticalGlue())
                .build());

        final JTextField multiplier = new JTextField("100%", 5);
        allianceSelection.add(
            JPanelBuilder.builder()
                .flowLayout()
                .add(multiplier)
                .add(Box.createVerticalGlue())
                .build());
      }
      selectionPanels.add(allianceSelection.build());
    }
    return selectionPanels;
  }

}
