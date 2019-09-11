package games.strategy.net;

import games.strategy.engine.framework.HeadlessAutoSaveType;
import games.strategy.engine.lobby.ApiKey;
import java.io.File;
import javax.annotation.Nullable;

/** A client messenger. Additional methods for selecting the game on the server. */
public interface IClientMessenger extends IMessenger {
  void changeServerGameTo(String gameName);

  void changeToLatestAutosave(HeadlessAutoSaveType typeOfAutosave);

  void changeToGameSave(byte[] bytes, String fileName);

  void changeToGameSave(File saveGame, String fileName);

  /** Listen for errors. */
  void addErrorListener(IMessengerErrorListener listener);

  /** Stop listening for errors. */
  void removeErrorListener(IMessengerErrorListener listener);

  boolean isPasswordChangeRequired();

  /**
   * When connecting to lobby, lobby will provide an API key. <br>
   * When connecting to a game host, an API key will not be provided.
   */
  // TODO: Project#12
  // - Create class 'HttpLobbyClient', inject as property into 'lobbyClient'
  // - Update LobbyLogin to instantiate an HttpLobbyClient and inject into lobbyClient
  // - Use this API key in LobbyLogin to create the HttpLobbyClient
  @SuppressWarnings("unused")
  @Nullable
  ApiKey getApiKey();
}
