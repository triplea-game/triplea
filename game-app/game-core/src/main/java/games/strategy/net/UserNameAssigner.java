package games.strategy.net;

import com.google.common.base.Preconditions;
import java.util.Collection;
import lombok.experimental.UtilityClass;
import org.triplea.domain.data.UserName;

/** Utility class that will assign a name to a newly logging in player. */
@UtilityClass
public final class UserNameAssigner {

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
      final String desiredName, final String mac, final Collection<String> loggedInNames) {
    Preconditions.checkArgument(UserName.validate(desiredName).isEmpty());
    Preconditions.checkNotNull(mac);
    Preconditions.checkNotNull(loggedInNames);

    String currentName = desiredName;

    if (currentName.length() > 50) {
      currentName = currentName.substring(0, 50);
    }

    final String originalName = currentName;
    for (int i = 1; loggedInNames.contains(currentName); i++) {
      currentName = originalName + " (" + i + ")";
    }
    return currentName;
  }
}
