package games.strategy.engine.lobby.client.login;

import java.util.Map;

import com.google.common.base.Strings;

/**
 * Server properties.
 *
 * <p>
 * Generally there is one lobby server, but that server may move.
 * </p>
 *
 * <p>
 * To keep track of this, we always have a properties file in a constant location that points to the current lobby
 * server.
 * </p>
 *
 * <p>
 * The properties file may indicate that the server is not available using the ERROR_MESSAGE key.
 * </p>
 */
public class LobbyServerProperties {
  public final String host;
  public final int port;
  public final String serverErrorMessage;
  public final String serverMessage;

  /**
   * Inits a bare-bones object without server message.
   * @param host The host address of the lobby, typically an IP address
   * @param port The port the lobby is listening on
   */
  public LobbyServerProperties(final String host, final int port) {
    this.host = host;
    this.port = port;
    this.serverErrorMessage = "";
    this.serverMessage = "";
  }

  /**
   * Typical constructor for lobby properties based on a yaml object. Parses lobby
   * host, port, message, and an error message used to indicate potential down times
   * to the user.
   * @param yamlProps Yaml object with lobby properties from the point of view of the game
   *                  client.
   */
  public LobbyServerProperties(final Map<String, Object> yamlProps) {
    this.host = (String) yamlProps.get("host");
    this.port = (Integer) yamlProps.get("port");
    this.serverMessage = Strings.nullToEmpty((String) yamlProps.get("message"));
    this.serverErrorMessage = Strings.nullToEmpty((String) yamlProps.get("error_message"));
  }

  /**
   * @return True if the server is available. If not then see <code>serverErrorMessage</code>
   */
  public boolean isServerAvailable() {
    return serverErrorMessage.isEmpty();
  }
}
