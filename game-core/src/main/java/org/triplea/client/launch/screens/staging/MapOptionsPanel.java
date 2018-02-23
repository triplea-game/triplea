package org.triplea.client.launch.screens.staging;

import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.data.properties.IEditableProperty;
import swinglib.JLabelBuilder;
import swinglib.JPanelBuilder;

/**
 * A panel that shows a listing of the current game options. The host of a game can update these, clients
 * can only view as read-only.
 */
class MapOptionsPanel {

  static JPanel build(final GameData gameData, final StagingScreen screen) {

    final GameProperties properties = gameData.getProperties();
    final List<IEditableProperty> props = properties.getEditableProperties();

    final JPanelBuilder panelBuilder = JPanelBuilder.builder()
        .gridBagLayout(2)
        .add(new JLabel("<html><h2>Map Options</h2></html>"))
        .add(new JPanel());

    props.stream()
        .filter(prop -> !prop.getName().contains("bid"))
        .forEach(prop -> {
          panelBuilder.add(JLabelBuilder.builder()
              .textWithMaxLength(prop.getName(), 25)
              .tooltip(prop.getName())
              .build());
          panelBuilder.add(
              (screen != StagingScreen.NETWORK_CLIENT) ? prop.getEditorComponent()
                  : prop.getEditorComponentDisabled());
        });
    return panelBuilder.build();
  }
}
