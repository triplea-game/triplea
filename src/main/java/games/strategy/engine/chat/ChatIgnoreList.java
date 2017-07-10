package games.strategy.engine.chat;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

class ChatIgnoreList {
  private static final Logger log = Logger.getLogger(ChatIgnoreList.class.getName());
  private final Object lock = new Object();
  private final Set<String> ignore = new HashSet<>();

  ChatIgnoreList() {
    final Preferences prefs = getPrefNode();
    try {
      Collections.addAll(ignore, prefs.keys());
    } catch (final BackingStoreException e) {
      log.log(Level.FINE, e.getMessage(), e);
    }
  }

  void add(final String name) {
    synchronized (lock) {
      ignore.add(name);
      final Preferences prefs = getPrefNode();
      prefs.put(name, "true");
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

  void remove(final String name) {
    synchronized (lock) {
      ignore.remove(name);
      final Preferences prefs = getPrefNode();
      prefs.remove(name);
      try {
        prefs.flush();
      } catch (final BackingStoreException e) {
        log.log(Level.FINE, e.getMessage(), e);
      }
    }
  }

  boolean shouldIgnore(final String name) {
    synchronized (lock) {
      return ignore.contains(name);
    }
  }
}
