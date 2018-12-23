package games.strategy.engine.framework.startup.ui.editors;

import java.awt.GridBagConstraints;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.framework.startup.ui.editors.validators.EmailValidator;
import games.strategy.engine.random.IRemoteDiceServer;
import games.strategy.engine.random.PropertiesDiceRoller;

/**
 * A class for to configure a Dice Server for the game.
 */
public class DiceServerEditor extends EditorPanel {
  private static final long serialVersionUID = -451810815037661114L;
  private final JButton testDiceButton = new JButton("Test Server");
  private final JTextField toAddress = new JTextField();
  private final JTextField ccAddress = new JTextField();
  private final JTextField gameId = new JTextField();
  private final JLabel toLabel = new JLabel("To:");
  private final JLabel ccLabel = new JLabel("Cc:");
  private final JComboBox<String> servers = new JComboBox<>();

  public DiceServerEditor() {
    final int bottomSpace = 1;
    final int labelSpace = 2;
    int row = 0;
    PropertiesDiceRoller.loadFromFile().forEach(server -> servers.addItem(server.getDisplayName()));
    add(new JLabel("Servers:"), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
    add(servers, new GridBagConstraints(1, row, 2, 1, 1.0, 0, GridBagConstraints.EAST,
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, bottomSpace, 0), 0, 0));
    row++;
    add(toLabel, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
    add(toAddress, new GridBagConstraints(1, row, 2, 1, 1.0, 0, GridBagConstraints.EAST,
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, bottomSpace, 0), 0, 0));
    row++;
    add(ccLabel, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
    add(ccAddress, new GridBagConstraints(1, row, 2, 1, 1.0, 0, GridBagConstraints.EAST,
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, bottomSpace, 0), 0, 0));
    row++;
    final JLabel gameIdLabel = new JLabel("Game Name:");
    add(gameIdLabel, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
    add(gameId, new GridBagConstraints(1, row, 2, 1, 1.0, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, bottomSpace, 0), 0, 0));
    row++;
    add(testDiceButton, new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE,
        new Insets(0, 0, bottomSpace, 0), 0, 0));
  }

  public boolean areFieldsValid() {
    boolean toValid = validateTextField(toAddress, toLabel, new EmailValidator(false));
    boolean ccValid = validateTextField(ccAddress, ccLabel, new EmailValidator(true));
    final boolean allValid = toValid && ccValid;
    testDiceButton.setEnabled(allValid);
    return allValid;
  }

  public void applyToGameProperties(final GameProperties properties) {
    properties.set(IRemoteDiceServer.NAME, servers.getSelectedItem());
    properties.set(IRemoteDiceServer.GAME_NAME, gameId.getText());
    properties.set(IRemoteDiceServer.EMAIL_1, toAddress.getText());
    properties.set(IRemoteDiceServer.EMAIL_2, ccAddress.getText());
  }

  public void populateFromGameProperties(final GameProperties properties) {
    servers.setSelectedItem(properties.get(IRemoteDiceServer.NAME));
    gameId.setText(properties.get(IRemoteDiceServer.GAME_NAME, ""));
    toAddress.setText(properties.get(IRemoteDiceServer.EMAIL_1, ""));
    ccAddress.setText(properties.get(IRemoteDiceServer.EMAIL_2, ""));
  }

  public IRemoteDiceServer createDiceServer() {
    // FIXME create dice server instance
    return null;
  }
}
