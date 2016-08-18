package games.strategy.engine.lobby.client.login;

import java.util.Map;

/**
 * Server properties.
 * <p>
 * Generally there is one lobby server, but that server may move.
 * <p>
 * To keep track of this, we always have a properties file in a constant location that points to the current lobby
 * server.
 * <p>
 * The properties file may indicate that the server is not available using the ERROR_MESSAGE key.
 * <p>
 */
public class LobbyServerProperties {
  public final String host;
  public final int port;
  public final String serverErrorMessage;
  public final String serverMessage;

  public LobbyServerProperties(final Map<String, Object> yamlProps) {
    this.host = (String) yamlProps.get("host");
    this.port = (Integer) yamlProps.get("port");
    this.serverMessage = (String) yamlProps.get("message");
    this.serverErrorMessage = (String) yamlProps.get("error_message");
  }

  /**
   * @return if the server is available. If the server is not available then getServerErrorMessage will give a reason.
   */
  public boolean isServerAvailable() {
    return (serverErrorMessage == null) || serverErrorMessage.trim().length() <= 0;
  }
}
