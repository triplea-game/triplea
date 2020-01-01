package games.strategy.engine.chat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import lombok.extern.java.Log;
import org.triplea.domain.data.UserName;

@Log
class ChatIgnoreList {
  private final Object lock = new Object();
  private final Set<UserName> ignore = new HashSet<>();

  ChatIgnoreList() {
    final Preferences prefs = getPrefNode();
    try {
      ignore.addAll(Arrays.stream(prefs.keys()).map(UserName::of).collect(Collectors.toSet()));
    } catch (final BackingStoreException e) {
      log.log(Level.FINE, e.getMessage(), e);
    }
  }

  void add(final UserName name) {
    synchronized (lock) {
      ignore.add(name);
      final Preferences prefs = getPrefNode();
      prefs.put(name.getValue(), "true");
      try {
        prefs.flush();
      } catch (final BackingStoreException e) {
        log.log(Level.FINE, e.getMessage(), e);
      }
    }
  }

  static Preferences getPrefNode() {
    return Preferences.userNodeForPackage(ChatIgnoreList.class);
  }

  void remove(final UserName name) {
    synchronized (lock) {
      ignore.remove(name);
      final Preferences prefs = getPrefNode();
      prefs.remove(name.getValue());
      try {
        prefs.flush();
      } catch (final BackingStoreException e) {
        log.log(Level.FINE, e.getMessage(), e);
      }
    }
  }

  boolean shouldIgnore(final UserName name) {
    synchronized (lock) {
      return ignore.contains(name);
    }
  }
}
