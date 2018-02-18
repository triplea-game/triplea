package org.triplea.client.launch.screens.staging.panels.playerselection;

import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JPanel;

import org.triplea.client.launch.screens.staging.StagingScreen;

import games.strategy.engine.data.PlayerID;
import games.strategy.util.UnhandledSwitchCaseException;
import swinglib.JPanelBuilder;

/**
 * Set of panels grouped by game alliance, indicates current player selection and allows players to
 * pick sides.
 */
public class PlayerSelectionPanel {

  private final StagingScreen stagingScreen;
  private final PlayerSelectionModel playerSelectionModel;
  private final SwingComponentTracker swingComponents;

  public PlayerSelectionPanel(
      final StagingScreen stagingScreen,
      final PlayerSelectionModel playerSelectionModel) {
    this.stagingScreen = stagingScreen;
    this.playerSelectionModel = playerSelectionModel;
    swingComponents = new SwingComponentTracker(playerSelectionModel);
  }

  /**
   * Build swing panels with components around each alliance in the game data.
   */
  public List<JPanel> build() {
    return playerSelectionModel.getAlliances()
        .stream()
        .map(alliance -> buildAlliancePanel(alliance, stagingScreen))
        .collect(Collectors.toList());
  }

  private JPanel buildAlliancePanel(
      final String alliance,
      final StagingScreen stagingScreen) {

    switch (stagingScreen) {
      case SINGLE_PLAYER:
        return singlePlayerAlliancePanel(alliance);
      case NETWORK_CLIENT:
        return networkClientAlliancePanel(alliance);
      case NETWORK_HOST:
        return networkHostAlliancePanel(alliance);
      default:
        throw new UnhandledSwitchCaseException(stagingScreen);
    }
  }

  private JPanel singlePlayerAlliancePanel(final String alliance) {

    // first row, just the alliance 'h2' label and then spacers to fill out the columns
    final JPanelBuilder panelBuilder = JPanelBuilder.builder()
        .borderEtched()
        .gridBagLayout(2)
        .addHtmlLabel("<h2>" + alliance + "</h2>")
        .addEmptyLabel();

    // next we build one row per country
    for (final PlayerID country : playerSelectionModel.getPlayersInAlliance(alliance)) {
      panelBuilder.addLabel(country.getName());
      panelBuilder.add(swingComponents.buildAiSelectionCombo(country));
    }
    return panelBuilder.build();
  }


  /**
   * <pre>
   * .
   * [Country Label] [Player Label | Play Button] [Kick Button | Stand up button] [Ai/Human Combo]
   * </pre>
   */
  private JPanel networkHostAlliancePanel(final String alliance) {
    final JPanelBuilder panelBuilder = JPanelBuilder.builder()
        .borderEtched()
        .gridBagLayout(3)
        .addHtmlLabel("<h2>" + alliance + "</h2>")
        .addEmptyLabel()
        .addEmptyLabel();

    for (final PlayerID country : playerSelectionModel.getPlayersInAlliance(alliance)) {
      panelBuilder.addLabel(country.getName());
      panelBuilder.add(swingComponents.buildPlayerIndicator(StagingScreen.NETWORK_HOST, country));

      if (playerSelectionModel.isCountryTakenByCurrentPlayer(country)) {
        panelBuilder.add(swingComponents.buildAiSelectionCombo(country));
      } else {
        panelBuilder.addEmptyLabel();
      }
    }
    return panelBuilder.build();
  }

  /**
   * <pre>
   * .
   * [Country Label] [Player Label | Play button] [stand button]
   * </pre>
   */
  private JPanel networkClientAlliancePanel(final String alliance) {
    // TODO: make the alliance label a button to play the entire alliance
    final JPanelBuilder panelBuilder = JPanelBuilder.builder()
        .borderEtched()
        .gridBagLayout(2)
        .addHtmlLabel("<h2>" + alliance + "</h2>")
        .addEmptyLabel();

    for (final PlayerID country : playerSelectionModel.getPlayersInAlliance(alliance)) {
      panelBuilder.addLabel(country.getName());
      panelBuilder.add(swingComponents.buildPlayerIndicator(StagingScreen.NETWORK_CLIENT, country));
    }
    return panelBuilder.build();
  }
}
