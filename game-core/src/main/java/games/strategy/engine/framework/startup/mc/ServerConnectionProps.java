package games.strategy.engine.framework.startup.mc;

/**
 * Simple data class for parameter values to connect to a remote host server (EG: to connect to a hosted game).
 */
public class ServerConnectionProps {
  /** Player name, the desired name for the current player connecting to remote host. */
  private String name;
  /** Remote host address. */
  private String host;
  private int port;
  /** Password to use to connect to the remotely hosted game, this is set by the host of the game. */
  private String password;

  public String getHost() {
    return host;
  }

  public void setHost(final String host) {
    this.host = host;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(final String password) {
    this.password = password;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public int getPort() {
    return port;
  }

  public void setPort(final int port) {
    this.port = port;
  }
}
