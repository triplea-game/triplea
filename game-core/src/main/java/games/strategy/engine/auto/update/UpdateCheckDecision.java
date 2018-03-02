package games.strategy.engine.auto.update;

import static games.strategy.engine.framework.ArgParser.CliProperties.DO_NOT_CHECK_FOR_UPDATES;
import static games.strategy.engine.framework.ArgParser.CliProperties.TRIPLEA_CLIENT;
import static games.strategy.engine.framework.ArgParser.CliProperties.TRIPLEA_SERVER;



class UpdateCheckDecision {

  static boolean shouldRun() {
    if (System.getProperty(TRIPLEA_SERVER, "false").equalsIgnoreCase("true")) {
      return false;
    }
    if (System.getProperty(TRIPLEA_CLIENT, "false").equalsIgnoreCase("true")) {
      return false;
    }

    if (System.getProperty(DO_NOT_CHECK_FOR_UPDATES, "false").equalsIgnoreCase("true")) {
      return false;
    }

    return true;
  }

}
