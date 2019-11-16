package games.strategy.net;

import games.strategy.engine.framework.startup.login.ClientLogin;
import games.strategy.engine.framework.startup.mc.ClientModel;
import java.io.IOException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Factory class for implementations of {@link IClientMessenger}. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientMessengerFactory {

  /** Creates a client messenger suitable for connecting to a hosted game server. */
  public static IClientMessenger newClientMessenger(
      final ClientModel.ClientProps props,
      final IObjectStreamFactory objectStreamFactory,
      final ClientLogin clientLogin)
      throws IOException {
    final String mac = MacFinder.getHashedMacAddress();
    return new ClientMessenger(
        props.getHost(), props.getPort(), props.getName(), mac, objectStreamFactory, clientLogin);
  }
}
