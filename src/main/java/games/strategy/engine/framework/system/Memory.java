package games.strategy.engine.framework.system;

import com.google.common.base.Strings;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientContext;
import games.strategy.engine.config.GameEngineProperty;

public class Memory {
  public static final String TRIPLEA_MEMORY_SET = "triplea.memory.set";


  public static boolean isMemoryXmxSet() {
    final String memSetString = System.getProperty(TRIPLEA_MEMORY_SET, "false");
    final boolean memSet = Boolean.parseBoolean(memSetString);
    // if we have already set the memory, then return.
    // (example: we used process runner to create a new triplea with a specific memory)
    if (memSet) {
      return true;
    }

    final String maxMemorySetting = ClientContext.propertyReader().readProperty(GameEngineProperty.MAX_MEMORY);
    if (Strings.nullToEmpty(maxMemorySetting).isEmpty() || Long.parseLong(maxMemorySetting) < 32L) {
      return true;
    }

    return false;
  }

  public static long getMaxMemoryInBytes() {
    final long userSetValue = readMaxMemory();

    if (userSetValue < 0) {
      return Runtime.getRuntime().maxMemory();
    } else {
      return userSetValue * 1024 * 1024;
    }
  }

  private static long readMaxMemory() {
    final String value = ClientContext.propertyReader().readProperty(GameEngineProperty.MAX_MEMORY);
    try {
      return Long.parseLong(value);
    } catch (final NumberFormatException e) {
      ClientLogger.logError(
          "Ignoring invalid number: " + value + ", for property: " + GameEngineProperty.MAX_MEMORY);
      return -1;
    }
  }
}
