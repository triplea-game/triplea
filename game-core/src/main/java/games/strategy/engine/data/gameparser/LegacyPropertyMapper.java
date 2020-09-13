package games.strategy.engine.data.gameparser;

import games.strategy.triplea.Constants;
import lombok.experimental.UtilityClass;

/**
 * This converts property names and values that were used in older map XMLs and updates those values
 * to their newer versions. This is useful to allow the game code to only use the newer names and
 * values while still being able to load older maps.
 */
@UtilityClass
class LegacyPropertyMapper {

  static String mapLegacyOptionName(final String optionName) {
    if (optionName.equalsIgnoreCase("isParatroop")) {
      return "isAirTransportable";
    } else if (optionName.equalsIgnoreCase("isInfantry")
        || optionName.equalsIgnoreCase("isMechanized")) {
      return "isLandTransportable";
    } else if (optionName.equalsIgnoreCase("occupiedTerrOf")) {
      return Constants.ORIGINAL_OWNER;
    } else if (optionName.equalsIgnoreCase("isImpassible")) {
      return "isImpassable";
    } else if (optionName.equalsIgnoreCase("turns")) {
      return "rounds";
    }
    return optionName;
  }

  public String mapLegacyOptionValue(final String optionName, final String optionValue) {
    if (optionName.equalsIgnoreCase("victoryCity")) {
      if (optionValue.equalsIgnoreCase("true")) {
        return "1";
      } else if (optionValue.equalsIgnoreCase("false")) {
        return "0";
      } else {
        return optionValue;
      }
    }
    return optionValue;
  }

  public String mapPropertyName(final String propertyName) {
    if (propertyName.equalsIgnoreCase("Battleships repair at end of round")
        || propertyName.equalsIgnoreCase("Units repair at end of round")) {
      return Constants.TWO_HIT_BATTLESHIPS_REPAIR_END_OF_TURN;
    } else if (propertyName.equalsIgnoreCase("Battleships repair at beginning of round")
        || propertyName.equalsIgnoreCase("Units repair at beginning of round")) {
      return Constants.TWO_HIT_BATTLESHIPS_REPAIR_BEGINNING_OF_TURN;
    }

    return propertyName;
  }

  static boolean ignoreOptionName(final String name) {
    return name.equalsIgnoreCase("takeUnitControl") || name.equalsIgnoreCase("giveUnitControl");
  }
}
