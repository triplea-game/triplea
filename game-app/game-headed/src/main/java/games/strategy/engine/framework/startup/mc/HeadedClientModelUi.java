package games.strategy.engine.framework.startup.mc;

import games.strategy.engine.chat.ChatPanel;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.startup.ui.ClientOptions;
import games.strategy.engine.framework.ui.background.WaitWindow;
import games.strategy.triplea.UrlConstants;
import java.awt.Component;
import java.awt.Frame;
import java.util.Optional;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;
import org.triplea.game.chat.ChatModel;
import org.triplea.java.Interruptibles;
import org.triplea.swing.DialogBuilder;
import org.triplea.swing.EventThreadJOptionPane;
import org.triplea.swing.SwingAction;
import org.triplea.swing.SwingComponents;

/** Swing implementation of {@link ClientModelUi}. */
public class HeadedClientModelUi implements ClientModelUi {
  private final Component parent;
  private final Frame ui;
  private final Runnable showSelectType;
  private final WaitWindow gameLoadingWindow;

  public HeadedClientModelUi(final Component parent, final Runnable showSelectType) {
    this.parent = parent;
    this.ui = JOptionPane.getFrameForComponent(parent);
    this.showSelectType = showSelectType;
    final Interruptibles.Result<WaitWindow> window =
        Interruptibles.awaitResult(() -> SwingAction.invokeAndWaitResult(WaitWindow::new));
    if (!window.completed) {
      throw new IllegalStateException("Error while creating WaitWindow");
    }
    gameLoadingWindow =
        window.result.orElseThrow(
            () -> new IllegalStateException("Constructor did not return instance"));
  }

  @Override
  public Optional<ClientModel.ClientProps> promptForClientProps(final String defaultPlayerName) {
    final Interruptibles.Result<ClientModel.ClientProps> result =
        Interruptibles.awaitResult(
            () ->
                SwingAction.invokeAndWaitResult(
                    () -> {
                      final ClientOptions options =
                          new ClientOptions(ui, defaultPlayerName, GameRunner.PORT, "127.0.0.1");
                      options.setLocationRelativeTo(ui);
                      options.setVisible(true);
                      options.dispose();
                      if (!options.getOkPressed()) {
                        return null;
                      }
                      return ClientModel.ClientProps.builder()
                          .host(options.getAddress())
                          .name(options.getName())
                          .port(options.getPort())
                          .build();
                    }));
    if (!result.completed) {
      throw new IllegalStateException("Error during component creation of ClientOptions.");
    }
    return result.result;
  }

  @Override
  public String promptForPassword() {
    return Interruptibles.awaitResult(
            () ->
                SwingAction.invokeAndWaitResult(
                    () -> {
                      final JPasswordField passwordField = new JPasswordField();
                      passwordField.setColumns(15);
                      final JOptionPane optionPane =
                          new JOptionPane(
                              passwordField,
                              JOptionPane.QUESTION_MESSAGE,
                              JOptionPane.DEFAULT_OPTION) {
                            private static final long serialVersionUID = -6461902648509091914L;

                            @Override
                            public void selectInitialValue() {
                              super.selectInitialValue();
                              passwordField.requestFocusInWindow();
                            }
                          };
                      optionPane
                          .createDialog(ui, "Enter a password to join the game")
                          .setVisible(true);
                      return new String(passwordField.getPassword());
                    }))
        .result
        .orElse("");
  }

  @Override
  public void showAlreadyLoggedIn(final String assignedName) {
    DialogBuilder.builder()
        .parent(null)
        .title("Already Logged In")
        .infoMessage(
            "<html>Already logged in with another name, "
                + "cannot use a different name.<br/>"
                + "Logging in as: "
                + assignedName)
        .showDialog();
  }

  @Override
  public void showInvalidPort(final int port) {
    EventThreadJOptionPane.showMessageDialog(
        ui, "Invalid Port: " + port, "Error", JOptionPane.ERROR_MESSAGE);
  }

  @Override
  public void showLoginFailed(final String message) {
    EventThreadJOptionPane.showMessageDialog(ui, message);
  }

  @Override
  public void showConnectionError(final String errorMessage) {
    SwingComponents.showError(
        parent,
        "Error Connecting to Host",
        "Error: "
            + errorMessage
            +
"""


Check:
"""
            + "- The host is running\n"
            + "- You have the right port and IP address\n"
            + "- The host should use and can check their public IP address by "
            + "visiting 'whatismyip.com'\n"
            + "- The host can check that they are able to connect to their own game\n"
            + "using their public IP\n"
            + "Additional help can be found on the user-guide: "
            + UrlConstants.USER_GUIDE);
  }

  @Override
  public void showCannotJoinGame(final String reason) {
    SwingUtilities.invokeLater(
        () -> {
          showSelectType.run();
          EventThreadJOptionPane.showMessageDialog(ui, "Could not join game: " + reason);
        });
  }

  @Override
  public void showConnectionLost() {
    EventThreadJOptionPane.showMessageDialog(
        ui,
        "Connection to game host lost.\nPlease save and restart.",
        "Connection Lost!",
        JOptionPane.ERROR_MESSAGE);
  }

  @Override
  public void showHostBotWelcome(final ChatModel chat) {
    if (chat instanceof ChatPanel chatPanel) {
      chatPanel
          .getChatMessagePanel()
          .addServerMessage(
              "Welcome to an automated dedicated host service (a host bot). "
                  + "\nIf anyone disconnects, the autosave will be reloaded (a save might "
                  + "be loaded right now). "
                  + "\nYou can get the current save, or you can load a save (only saves that "
                  + "it has the map for).");
    }
  }

  @Override
  public void showGameLoading() {
    SwingUtilities.invokeLater(
        () -> {
          gameLoadingWindow.setVisible(true);
          gameLoadingWindow.setLocationRelativeTo(ui);
          gameLoadingWindow.showWait();
        });
  }

  @Override
  public void hideGameLoading() {
    gameLoadingWindow.doneWait();
  }

  @Override
  public void hideMainFrame() {
    SwingUtilities.invokeLater(() -> ui.setVisible(false));
  }
}
