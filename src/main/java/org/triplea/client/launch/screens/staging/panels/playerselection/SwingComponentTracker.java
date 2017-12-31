package org.triplea.client.launch.screens.staging.panels.playerselection;

import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.triplea.client.launch.screens.staging.StagingScreen;

import com.google.common.base.Preconditions;

import games.strategy.engine.data.PlayerID;
import swinglib.JButtonBuilder;
import swinglib.JComboBoxBuilder;

/**
 * Creates and then tracks swing components for player selection, allows us to then update the values
 * displayed by these components as for example a game host changes settings and we then need to update
 * client UIs.
 */
public class SwingComponentTracker implements Consumer<PlayerSelectionModel> {

  private final PlayerSelectionModel playerSelectionModel;

  SwingComponentTracker(final PlayerSelectionModel playerSelectionModel) {
    this.playerSelectionModel = playerSelectionModel;
    this.playerSelectionModel.addGuiListener(this);
  }

  @Override
  public void accept(final PlayerSelectionModel updatedModel) {
    System.out.println("UPDATE GUI!");
  }


  JComponent buildAiSelectionCombo(final PlayerID country) {
    return JComboBoxBuilder.builder()
        .menuOptions(playerSelectionModel.getPlayerSelectionTypes())
        .itemListener(selection -> playerSelectionModel.updatePlayerSelection(country, selection))
        .useLastSelectionAsFutureDefault("singlePlayer" + playerSelectionModel.buildSelectionIdString(country))
        .build();
  }

  JComponent buildPlayerIndicator(final StagingScreen stagingScreen, final PlayerID country) {
    if (playerSelectionModel.isCountryTakenByCurrentPlayer(country)) {
      return JButtonBuilder.builder()
          .title(playerSelectionModel.getCurrentPlayerName())
          .toolTip("Click to un-select " + country)
          .actionListener(() -> playerSelectionModel.updateCountryToEmptyPlayerSelection(country))
          .build();
    } else if (playerSelectionModel.isCountryTakenByAnyPlayer(country)) {
      if (stagingScreen == StagingScreen.NETWORK_CLIENT) {
        return new JLabel(playerSelectionModel.getPlayerNameByCountry(country));
      }

      Preconditions.checkState(stagingScreen == StagingScreen.NETWORK_HOST);
      return JButtonBuilder.builder()
          .title(playerSelectionModel.getPlayerNameByCountry(country))
          .toolTip("Click to remove " + playerSelectionModel.getPlayerNameByCountry(country) + " from " + country)
          .actionListener(() -> playerSelectionModel.updateCountryToEmptyPlayerSelection(country))
          .build();
    } else {
      return JButtonBuilder.builder()
          .title("Play")
          .toolTip("Click to play the game as " + country)
          .actionListener(() -> playerSelectionModel.updateCountryToCurrentPlayer(country))
          .build();
    }
  }
}
