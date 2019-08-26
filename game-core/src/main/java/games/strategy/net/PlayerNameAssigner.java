package games.strategy.net;

import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import games.strategy.engine.lobby.PlayerNameValidation;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.triplea.lobby.common.LobbyConstants;

/** Utility class that will assign a name to a newly logging in player. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlayerNameAssigner {

  /**
   * Returns a node name, based on the specified node name, that is unique across all nodes. The
   * node name is made unique by adding a numbered suffix to the existing node name. For example,
   * for the second node with the name "foo", this method will return "foo (1)".
   *
   * @param desiredName The name being requested by a new user joining.
   * @param mac The (hashed) mac of the user that is joining. Used to determine if the user is
   *     already connected under a different name.
   */
  public static String assignName(
      final String desiredName,
      final String mac,
      final Multimap<String, String> loggedInMacsToNames) {
    Preconditions.checkArgument(PlayerNameValidation.serverSideValidate(desiredName) == null);
    Preconditions.checkNotNull(mac);
    Preconditions.checkNotNull(loggedInMacsToNames);

    String currentName = desiredName;

    if (!isBotName(desiredName)) {
      if (currentName.length() > 50) {
        currentName = currentName.substring(0, 50);
      }
      currentName =
          findExistingLoggedInNameFromSameMac(mac, loggedInMacsToNames).orElse(currentName);
    }

    final Collection<String> playerNames = loggedInMacsToNames.values();

    final String originalName = currentName;
    for (int i = 1; playerNames.contains(currentName); i++) {
      currentName = originalName + " (" + i + ")";
    }
    return currentName;
  }

  private static Optional<String> findExistingLoggedInNameFromSameMac(
      final String mac, final Multimap<String, String> loggedInMacsToNames) {

    return loggedInMacsToNames.entries().stream()
        .filter(entry -> mac.equals(entry.getKey()))
        .map(Map.Entry::getValue)
        .min(Comparator.naturalOrder());
  }

  private static boolean isBotName(final String desiredName) {
    return desiredName.startsWith("Bot") && desiredName.endsWith(LobbyConstants.LOBBY_WATCHER_NAME);
  }
}
