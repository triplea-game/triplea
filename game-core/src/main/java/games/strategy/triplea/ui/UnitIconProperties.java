package games.strategy.triplea.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.attachments.AbstractConditionsAttachment;
import games.strategy.triplea.attachments.AbstractPlayerRulesAttachment;
import games.strategy.triplea.attachments.ICondition;
import games.strategy.util.FileNameUtils;
import lombok.extern.java.Log;

/**
 * Loads objective text from objectives.properties.
 */
@Log
public final class UnitIconProperties extends PropertyFile {

  private static final String PROPERTY_FILE = "unit_icons.properties";
  private static Map<ICondition, Boolean> conditionsStatus = new HashMap<>();

  private UnitIconProperties(final GameData data) {
    super(PROPERTY_FILE);
    final String gameName =
        FileNameUtils.replaceIllegalCharacters(data.getGameName(), '_').replaceAll(" ", "_").concat(".");
    for (final String key : properties.stringPropertyNames()) {
      if (!key.startsWith(gameName)) {
        continue;
      }
      try {
        final String[] unitInfoAndCondition = key.split(";");
        final String[] unitInfo = unitInfoAndCondition[0].split("\\.", 3);
        if (unitInfoAndCondition.length != 2) {
          continue;
        }
        final ICondition condition =
            AbstractPlayerRulesAttachment.getCondition(unitInfo[1], unitInfoAndCondition[1], data);
        if (condition != null) {
          conditionsStatus.put(condition, false);
        }
      } catch (final RuntimeException e) {
        log.log(
            Level.SEVERE,
            "unit_icons.properties keys must be: <game_name>.<player>.<unit_type>;attachmentName OR "
                + "if always true: <game_name>.<player>.<unit_type>",
            e);
      }
    }
    conditionsStatus = getTestedConditions(data);
  }

  public static UnitIconProperties getInstance(final GameData data) {
    return PropertyFile.getInstance(UnitIconProperties.class, () -> new UnitIconProperties(data));
  }

  /**
   * Get all unit icon images for given player and unit type that are currently true, ensuring order
   * from the properties file.
   */
  public List<String> getImagePaths(final String player, final String unitType, final GameData data) {
    final List<String> imagePaths = new ArrayList<>();
    final String gameName =
        FileNameUtils.replaceIllegalCharacters(data.getGameName(), '_').replaceAll(" ", "_");
    final String startOfKey = gameName + "." + player + "." + unitType;
    for (final Object key : properties.keySet()) {
      try {
        final String keyString = key.toString();
        final String[] keyParts = keyString.split(";");
        if (startOfKey.equals(keyParts[0])) {
          if (keyParts.length == 2) {
            final ICondition condition = AbstractPlayerRulesAttachment.getCondition(player, keyParts[1], data);
            if (conditionsStatus.get(condition)) {
              imagePaths.add(properties.get(key).toString());
            }
          } else {
            imagePaths.add(properties.get(key).toString());
          }
        }
      } catch (final RuntimeException e) {
        log.log(
            Level.SEVERE,
            "unit_icons.properties keys must be: <game_name>.<player>.<unit_type>;attachmentName OR "
            + "if always true: <game_name>.<player>.<unit_type>",
            e);
      }
    }
    return imagePaths;
  }

  public boolean testIfConditionsHaveChanged(final GameData data) {
    final Map<ICondition, Boolean> testedConditions = getTestedConditions(data);
    if (!testedConditions.equals(conditionsStatus)) {
      conditionsStatus = testedConditions;
      return true;
    }
    return false;
  }

  private static Map<ICondition, Boolean> getTestedConditions(final GameData data) {
    final HashSet<ICondition> allConditionsNeeded =
        AbstractConditionsAttachment.getAllConditionsRecursive(new HashSet<>(conditionsStatus.keySet()), null);
    return AbstractConditionsAttachment.testAllConditionsRecursive(allConditionsNeeded, null,
        new ObjectiveDummyDelegateBridge(data));
  }
}
