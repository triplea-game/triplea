package games.strategy.engine.framework.startup.ui.editors;

import java.awt.GridBagConstraints;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentListener;

import games.strategy.engine.framework.startup.ui.editors.validators.EmailValidator;
import games.strategy.engine.random.IRemoteDiceServer;
import games.strategy.engine.random.PBEMDiceRoller;

/**
 * An class for editing a Dice Server bean.
 */
public class DiceServerEditor extends EditorPanel {
  private static final long serialVersionUID = -451810815037661114L;
  private final JButton testDiceyButton = new JButton("Test Server");
  private final JTextField toAddress = new JTextField();
  private final JTextField ccAddress = new JTextField();
  private final JTextField gameId = new JTextField();
  private final JLabel toLabel = new JLabel("To:");
  private final JLabel ccLabel = new JLabel("Cc:");
  private final IRemoteDiceServer remoteDiceServer;

  /**
   * Creating a new instance.
   *
   * @param diceServer
   *        the DiceServer bean to edit
   */
  public DiceServerEditor(final IRemoteDiceServer diceServer) {
    remoteDiceServer = diceServer;
    final int bottomSpace = 1;
    final int labelSpace = 2;
    int row = 0;
    if (remoteDiceServer.sendsEmail()) {
      add(toLabel, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
          new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
      add(toAddress, new GridBagConstraints(1, row, 2, 1, 1.0, 0, GridBagConstraints.EAST,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, bottomSpace, 0), 0, 0));
      toAddress.setText(remoteDiceServer.getToAddress());
      row++;
      add(ccLabel, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
          new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
      add(ccAddress, new GridBagConstraints(1, row, 2, 1, 1.0, 0, GridBagConstraints.EAST,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, bottomSpace, 0), 0, 0));
      ccAddress.setText(remoteDiceServer.getCcAddress());
      row++;
    }
    if (remoteDiceServer.supportsGameId()) {
      final JLabel m_gameIdLabel = new JLabel("Game ID:");
      add(m_gameIdLabel, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
          new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
      add(gameId, new GridBagConstraints(1, row, 2, 1, 1.0, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
          new Insets(0, 0, bottomSpace, 0), 0, 0));
      gameId.setText(remoteDiceServer.getGameId());
      row++;
    }
    add(testDiceyButton, new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE,
        new Insets(0, 0, bottomSpace, 0), 0, 0));
    setupListeners();
  }

  /**
   * Configures the listeners for the gui components.
   */
  private void setupListeners() {
    testDiceyButton.addActionListener(e -> {
      final PBEMDiceRoller random = new PBEMDiceRoller(getDiceServer(), null);
      random.test();
    });
    final DocumentListener docListener = new EditorChangedFiringDocumentListener();
    toAddress.getDocument().addDocumentListener(docListener);
    ccAddress.getDocument().addDocumentListener(docListener);
  }

  @Override
  public boolean isBeanValid() {
    boolean toValid = true;
    boolean ccValid = true;
    if (getDiceServer().sendsEmail()) {
      toValid = validateTextField(toAddress, toLabel, new EmailValidator(false));
      ccValid = validateTextField(ccAddress, ccLabel, new EmailValidator(true));
    }
    final boolean allValid = toValid && ccValid;
    testDiceyButton.setEnabled(allValid);
    return allValid;
  }

  @Override
  public IBean getBean() {
    return getDiceServer();
  }

  /**
   * Returns the currently configured dice server.
   *
   * @return the dice server
   */
  private IRemoteDiceServer getDiceServer() {
    if (remoteDiceServer.sendsEmail()) {
      remoteDiceServer.setCcAddress(ccAddress.getText());
      remoteDiceServer.setToAddress(toAddress.getText());
    }
    if (remoteDiceServer.supportsGameId()) {
      remoteDiceServer.setGameId(gameId.getText());
    }
    return remoteDiceServer;
  }
}
