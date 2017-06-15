package games.strategy.engine.framework.system;

import games.strategy.engine.ClientContext;

public class Memory {
  public static final String TRIPLEA_MEMORY_SET = "triplea.memory.set";

  public static boolean isMemoryXmxSet() {
    // The presence of the system prop means we received this value from command line args
    // (our command line arg parsing puts each value into a system prop..)
    // If we see this as set, it means we have set teh memory and everything is fine.

    final boolean memSet = Boolean.parseBoolean(System.getProperty(TRIPLEA_MEMORY_SET, "false"));

    if (memSet) {
      // system prop is set, memory has been set..
      return true;
    }

    // case: no memory setting, next we check the user setting. If no user setting present, then we return
    // true since we are at a default memory config with a default user value (blank/not set)
    return !ClientContext.gameEnginePropertyReader().readMaxMemory().isSet;
  }

  public static long getMaxMemoryInBytes() {
    if (ClientContext.gameEnginePropertyReader().readMaxMemory().isSet) {
      final long userSetValue = readMaxMemory();
      return userSetValue * 1024 * 1024;
    }
    return Runtime.getRuntime().maxMemory();
  }

  private static long readMaxMemory() {
    return ClientContext.gameEnginePropertyReader().readMaxMemory().value;
  }
}
