package org.triplea.game;

/** Provides information about the running application. */
public interface ApplicationContext {
  /** Returns the type containing the entry point from which the application was started. */
  Class<?> getMainClass();
}
