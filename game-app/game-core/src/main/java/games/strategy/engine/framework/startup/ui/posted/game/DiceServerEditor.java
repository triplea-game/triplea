package games.strategy.engine.framework.startup.ui.posted.game;

import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.random.IRemoteDiceServer;
import games.strategy.engine.random.MartiDiceRoller;
import games.strategy.engine.random.PbemDiceRoller;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.settings.ClientSetting;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.net.URI;
import java.util.Arrays;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import org.triplea.awt.OpenFileUtility;
import org.triplea.domain.data.PlayerEmailValidation;
import org.triplea.swing.DocumentListenerBuilder;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.SwingComponents;

/** A class to configure a Dice Server for the game. */
public class DiceServerEditor extends JPanel {
  public static final URI PRODUCTION_URI = URI.create("https://dice.marti.triplea-game.org");

  private static final long serialVersionUID = -451810815037661114L;
  private final JButton registerButton =
      new JButtonBuilder("Register")
          .actionListener(() -> OpenFileUtility.openUrl(UrlConstants.MARTI_REGISTRATION))
          .toolTip(
              "<html>Opens email registration page to register with MARTI dice-roller.<br>"
                  + "Needs to be done once before MARTI dice server can be used.</html>")
          .build();
  private final JButton helpButton =
      new JButtonBuilder("Help")
          .actionListener(
              () ->
                  JOptionPane.showMessageDialog(
                      this,
                      new JLabel(
                          "<html><p style='width: 400px;'>"
                              + "Enter your your email address in the \"To\" field, and you "
                              + "opponents in the \"CC\" field, you may enter multiple "
                              + "addresses each separated by a space. Click the register "
                              + "button to register your email addresses, this must be done "
                              + "to receive dice emails."
                              + "</p></html>"),
                      "Dice Server Help",
                      JOptionPane.INFORMATION_MESSAGE))
          .toolTip("Click this button to show help text")
          .build();
  private final JButton testDiceButton = new JButton("Test Server");
  private final JTextField toAddress = new JTextField();
  private final JTextField ccAddress = new JTextField();
  private final JTextField gameId = new JTextField();
  private final JLabel toLabel = new JLabel("To:");
  private final JLabel ccLabel = new JLabel("Cc:");
  private final Runnable readyCallback;

  public DiceServerEditor(final Runnable readyCallback) {
    this.readyCallback = readyCallback;
    final int bottomSpace = 1;
    final int labelSpace = 2;

    final JPanel diceRollerOptions = new JPanel();
    diceRollerOptions.setLayout(new GridBagLayout());
    diceRollerOptions.setBorder(new TitledBorder("Dice Server Options"));
    add(diceRollerOptions);

    int row = 0;
    // Show the dice server URI only if it is set to a non-default value.
    if (!ClientSetting.diceRollerUri
        .getValueOrThrow()
        .equals(ClientSetting.diceRollerUri.getDefaultValue().orElseThrow())) {
      diceRollerOptions.add(
          new JLabel("Dice Server"),
          new GridBagConstraints(
              0,
              row,
              1,
              1,
              0,
              0,
              GridBagConstraints.WEST,
              GridBagConstraints.NONE,
              new Insets(0, 0, bottomSpace, labelSpace),
              0,
              0));
      diceRollerOptions.add(
          new JLabel(ClientSetting.diceRollerUri.getValueOrThrow().toString()),
          new GridBagConstraints(
              1,
              row,
              3,
              1,
              1.0,
              0,
              GridBagConstraints.EAST,
              GridBagConstraints.HORIZONTAL,
              new Insets(0, 0, bottomSpace, 0),
              0,
              0));
      row++;
    }

    diceRollerOptions.add(
        toLabel,
        new GridBagConstraints(
            0,
            row,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(0, 0, bottomSpace, labelSpace),
            0,
            0));
    diceRollerOptions.add(
        toAddress,
        new GridBagConstraints(
            1,
            row,
            3,
            1,
            1.0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.HORIZONTAL,
            new Insets(0, 0, bottomSpace, 0),
            0,
            0));
    row++;
    diceRollerOptions.add(
        ccLabel,
        new GridBagConstraints(
            0,
            row,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(0, 0, bottomSpace, labelSpace),
            0,
            0));
    diceRollerOptions.add(
        ccAddress,
        new GridBagConstraints(
            1,
            row,
            3,
            1,
            1.0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.HORIZONTAL,
            new Insets(0, 0, bottomSpace, 0),
            0,
            0));
    row++;
    final JLabel gameIdLabel = new JLabel("Game Name:");
    diceRollerOptions.add(
        gameIdLabel,
        new GridBagConstraints(
            0,
            row,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(0, 0, bottomSpace, labelSpace),
            0,
            0));
    diceRollerOptions.add(
        gameId,
        new GridBagConstraints(
            1,
            row,
            3,
            1,
            1.0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.HORIZONTAL,
            new Insets(0, 0, bottomSpace, 0),
            0,
            0));
    row++;
    diceRollerOptions.add(
        registerButton,
        new GridBagConstraints(
            1,
            row,
            1,
            1,
            1,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.HORIZONTAL,
            new Insets(0, 0, bottomSpace, 0),
            0,
            0));

    diceRollerOptions.add(
        testDiceButton,
        new GridBagConstraints(
            2,
            row,
            1,
            1,
            1,
            0,
            GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL,
            new Insets(0, 0, bottomSpace, 0),
            0,
            0));

    testDiceButton.addActionListener(
        e -> {
          final PbemDiceRoller random = new PbemDiceRoller(newDiceServer());
          random.test();
        });

    diceRollerOptions.add(
        helpButton,
        new GridBagConstraints(
            3,
            row,
            1,
            1,
            1,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.HORIZONTAL,
            new Insets(0, 0, bottomSpace, 0),
            0,
            0));

    new DocumentListenerBuilder(this::checkFieldsAndNotify).attachTo(toAddress, ccAddress, gameId);
  }

  private void checkFieldsAndNotify() {
    areFieldsValid();
    readyCallback.run();
  }

  public boolean areFieldsValid() {
    final boolean toValid =
        !toAddress.getText().isEmpty() && validateEmailAddresses(toAddress.getText());
    final boolean ccValid =
        !ccAddress.getText().isEmpty() && validateEmailAddresses(ccAddress.getText());

    final boolean allValid = toValid && ccValid;
    testDiceButton.setEnabled(allValid);
    testDiceButton.setToolTipText(
        allValid
            ? "Send a verified dice roll test email"
            : "First enter a valid 'to' and 'cc' email address");

    SwingUtilities.invokeLater(
        () -> {
          SwingComponents.highlightLabelIfNotValid(toValid, toLabel);
          SwingComponents.highlightLabelIfNotValid(ccValid, ccLabel);
        });
    return allValid;
  }

  private boolean validateEmailAddresses(String addresses) {
    return Arrays.stream(addresses.split("\\s+")).allMatch(PlayerEmailValidation::isValid);
  }

  public void applyToGameProperties(final GameProperties properties) {
    properties.set(IRemoteDiceServer.GAME_NAME, gameId.getText());
    properties.set(IRemoteDiceServer.EMAIL_1, toAddress.getText());
    properties.set(IRemoteDiceServer.EMAIL_2, ccAddress.getText());
  }

  public void populateFromGameProperties(final GameProperties properties) {
    gameId.setText(properties.get(IRemoteDiceServer.GAME_NAME, ""));
    toAddress.setText(properties.get(IRemoteDiceServer.EMAIL_1, ""));
    ccAddress.setText(properties.get(IRemoteDiceServer.EMAIL_2, ""));
  }

  public IRemoteDiceServer newDiceServer() {
    return MartiDiceRoller.builder()
        .diceRollerUri(ClientSetting.diceRollerUri.getValueOrThrow())
        .gameId(gameId.getText())
        .toAddress(toAddress.getText())
        .ccAddress(ccAddress.getText())
        .build();
  }
}
