package games.strategy.triplea.ui;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.attachments.AbstractConditionsAttachment;
import games.strategy.triplea.attachments.AbstractPlayerRulesAttachment;
import games.strategy.triplea.attachments.ICondition;
import games.strategy.util.FileNameUtils;

/**
 * Loads objective text from objectives.properties.
 */
public class UnitIconProperties extends PropertyFile {

  private static final String PROPERTY_FILE = "unit_icons.properties";

  private static UnitIconProperties instance = null;
  private static Instant timestamp = Instant.EPOCH;
  private static Map<ICondition, Boolean> conditionsStatus = new HashMap<>();

  protected UnitIconProperties(final GameData data) {
    super(PROPERTY_FILE);
    final String gameName =
        FileNameUtils.replaceIllegalCharacters(data.getGameName(), '_').replaceAll(" ", "_").concat(".");
    for (final String key : properties.stringPropertyNames()) {
      if (!key.startsWith(gameName)) {
        continue;
      }
      try {
        final String[] unitInfoAndCondition = key.split(";");
        final String[] unitInfo = unitInfoAndCondition[0].split("\\.");
        if (unitInfoAndCondition.length != 2) {
          continue;
        }
        final ICondition condition =
            AbstractPlayerRulesAttachment.getCondition(unitInfo[1], unitInfoAndCondition[1], data);
        if (condition != null) {
          conditionsStatus.put(condition, false);
        }
      } catch (final Exception e) {
        System.err.println("unit_icons.properties keys must be: <game_name>.<player>.<unit_type>;attachmentName OR "
            + "if always true: <game_name>.<player>.<unit_type>");
      }
    }
    conditionsStatus = getTestedConditions(data);
  }

  public static UnitIconProperties getInstance(final GameData data) {
    // cache properties for 10 seconds
    if (instance == null || timestamp.plusSeconds(10).isBefore(Instant.now())) {
      instance = new UnitIconProperties(data);
      timestamp = Instant.now();
    }
    return instance;
  }

  /**
   * Get all unit icon images for given player and unit type that are currently true.
   */
  public List<String> getImagePaths(final String player, final String unitType, final GameData data) {
    final List<String> imagePaths = new ArrayList<>();
    final String gameName =
        FileNameUtils.replaceIllegalCharacters(data.getGameName(), '_').replaceAll(" ", "_");
    final String startOfKey = gameName + "." + player + "." + unitType;
    for (final Entry<Object, Object> entry : properties.entrySet()) {
      try {
        final String key = entry.getKey().toString();
        final String[] keyParts = key.split(";");
        if (startOfKey.equals(keyParts[0])) {
          if (keyParts.length == 2) {
            final ICondition condition = AbstractPlayerRulesAttachment.getCondition(player, keyParts[1], data);
            if (conditionsStatus.get(condition)) {
              imagePaths.add(entry.getValue().toString());
            }
          } else {
            imagePaths.add(entry.getValue().toString());
          }
        }
      } catch (final Exception e) {
        System.err.println("unit_icons.properties keys must be: <game_name>.<player>.<unit_type>;attachmentName OR "
            + "if always true: <game_name>.<player>.<unit_type>");
      }
    }
    return imagePaths;
  }

  public Set<Entry<Object, Object>> entrySet() {
    return properties.entrySet();
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
