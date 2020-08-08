package games.strategy.engine.framework.startup;

import static games.strategy.engine.framework.CliProperties.LOBBY_GAME_COMMENTS;
import static games.strategy.engine.framework.CliProperties.SERVER_PASSWORD;

import com.google.common.base.Strings;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Provides convenience methods to read system properties. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SystemPropertyReader {

  public static boolean serverIsPassworded() {
    return !Strings.nullToEmpty(System.getProperty(SERVER_PASSWORD)).isEmpty();
  }

  public static Optional<InetAddress> customHost() {
    return Optional.ofNullable(System.getProperty("customHost"))
        .map(
            customHost -> {
              try {
                return InetAddress.getByName(customHost);
              } catch (final UnknownHostException e) {
                throw new IllegalArgumentException("Invalid host address: " + customHost);
              }
            });
  }

  public static Optional<Integer> customPort() {
    return Optional.ofNullable(Integer.getInteger("customPort"));
  }

  public static String gameComments() {
    return Optional.of(System.getProperty(LOBBY_GAME_COMMENTS)).orElse("");
  }
}
