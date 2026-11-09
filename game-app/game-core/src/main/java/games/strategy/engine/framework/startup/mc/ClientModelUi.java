package games.strategy.engine.framework.startup.mc;

import games.strategy.engine.framework.startup.login.ClientLoginUi;
import java.util.Optional;
import org.triplea.game.chat.ChatModel;

/** User interaction needed by {@link ClientModel} while joining and starting a hosted game. */
public interface ClientModelUi extends ClientLoginUi {
  /** Asks the user which host to join and under what name; empty if the user cancels. */
  Optional<ClientModel.ClientProps> promptForClientProps(String defaultPlayerName);

  void showInvalidPort(int port);

  void showLoginFailed(String message);

  void showConnectionError(String errorMessage);

  /** Returns the user to game type selection and tells them why they could not join. */
  void showCannotJoinGame(String reason);

  void showConnectionLost();

  void showHostBotWelcome(ChatModel chat);

  void showGameLoading();

  void hideGameLoading();

  void hideMainFrame();
}
