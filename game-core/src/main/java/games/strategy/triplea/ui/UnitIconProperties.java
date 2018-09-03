package games.strategy.triplea.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;

import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.attachments.AbstractConditionsAttachment;
import games.strategy.triplea.attachments.AbstractPlayerRulesAttachment;
import games.strategy.triplea.attachments.ICondition;
import games.strategy.util.FileNameUtils;
import games.strategy.util.OptionalUtils;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.java.Log;

/**
 * Loads unit icons from unit_icons.properties.
 */
@Log
public final class UnitIconProperties extends PropertyFile {
  private static final String PROPERTY_FILE = "unit_icons.properties";

  private Map<ICondition, Boolean> conditionsStatus = new HashMap<>();

  private UnitIconProperties(final GameData gameData) {
    super(PROPERTY_FILE);

    initializeConditionsStatus(gameData, AbstractPlayerRulesAttachment::getCondition);
  }

  @VisibleForTesting
  UnitIconProperties(final Properties properties, final GameData gameData, final ConditionSupplier conditionSupplier) {
    super(properties);

    initializeConditionsStatus(gameData, conditionSupplier);
  }

  private void initializeConditionsStatus(final GameData gameData, final ConditionSupplier conditionSupplier) {
    final String gameName = normalizeGameName(gameData);
    for (final String key : properties.stringPropertyNames()) {
      // TODO: extract parse error handling to parseUnitIconDescriptor method
      // will log and return a Null Object in the error case
      try {
        final UnitIconDescriptor unitIconDescriptor = parseUnitIconDescriptor(key, properties.getProperty(key));
        // TODO: eventually extract a matches(gameName) method
        if (gameName.equals(unitIconDescriptor.gameName)) {
          unitIconDescriptor.conditionName.ifPresent(conditionName -> {
            final @Nullable ICondition condition =
                conditionSupplier.getCondition(unitIconDescriptor.playerName, conditionName, gameData);
            if (condition != null) {
              conditionsStatus.put(condition, false);
            }
          });
        }
      } catch (final RuntimeException e) {
        log.log(
            Level.SEVERE,
            "unit_icons.properties keys must be: <game_name>.<player>.<unit_type>;attachmentName OR "
                + "if always true: <game_name>.<player>.<unit_type>",
            e);
      }
    }
    conditionsStatus = getTestedConditions(gameData);
  }

  private static String normalizeGameName(final GameData gameData) {
    return FileNameUtils.replaceIllegalCharacters(gameData.getGameName(), '_').replaceAll(" ", "_");
  }

  /**
   * Parses a unit icon descriptor from a line in a unit_icons.properties file.
   *
   * <p>
   * Each line in a unit_icons.properties file has the format:
   * </p>
   * <p>
   * {@code <encodedIconId>=<iconPath>}
   * </p>
   * <p>
   * Where {@code encodedIconId} is
   * </p>
   * <p>
   * {@code <encodedUnitTypeId>[;conditionName]}
   * </p>
   * <p>
   * Where {@code encodedUnitTypeId} is
   * </p>
   * <p>
   * {@code <gameName>.<playerName>.<unitTypeName>}
   * </p>
   */
  @VisibleForTesting
  static UnitIconDescriptor parseUnitIconDescriptor(final String encodedIconId, final String iconPath) {
    final String[] iconIdTokens = encodedIconId.split(";");
    final String encodedUnitTypeId = iconIdTokens[0];
    final Optional<String> conditionName = (iconIdTokens.length == 2)
        ? Optional.ofNullable(iconIdTokens[1])
        : Optional.empty();

    final String[] unitTypeIdTokens = encodedUnitTypeId.split("\\.", 3);
    final String gameName = unitTypeIdTokens[0];
    final String playerName = unitTypeIdTokens[1];
    final String unitTypeName = unitTypeIdTokens[2];

    return new UnitIconDescriptor(gameName, playerName, unitTypeName, conditionName, iconPath);
  }

  public static UnitIconProperties getInstance(final GameData data) {
    return PropertyFile.getInstance(UnitIconProperties.class, () -> new UnitIconProperties(data));
  }

  /**
   * Get all unit icon images for given player and unit type that are currently true, ensuring order
   * from the properties file.
   */
  public List<String> getImagePaths(final String playerName, final String unitTypeName, final GameData gameData) {
    return getImagePaths(playerName, unitTypeName, gameData, AbstractPlayerRulesAttachment::getCondition);
  }

  @VisibleForTesting
  List<String> getImagePaths(
      final String playerName,
      final String unitTypeName,
      final GameData gameData,
      final ConditionSupplier conditionSupplier) {
    final List<String> imagePaths = new ArrayList<>();
    final String gameName = normalizeGameName(gameData);
    for (final Object key : properties.keySet()) {
      try {
        final UnitIconDescriptor unitIconDescriptor =
            parseUnitIconDescriptor(key.toString(), properties.getProperty(key.toString()));
        // TODO: to test the other branch, modify test to add a satisfied condition for a different game
        // just add additional properties for entries that have different game name, different player name, and
        // different unit type name
        // TODO: eventually extract a matches(gameName, playerName, unitTypeName) method
        if (gameName.equals(unitIconDescriptor.gameName)
            && playerName.equals(unitIconDescriptor.playerName)
            && unitTypeName.equals(unitIconDescriptor.unitTypeName)) {
          OptionalUtils.ifPresentOrElse(
              unitIconDescriptor.conditionName,
              conditionName -> {
                if (conditionsStatus.get(conditionSupplier.getCondition(playerName, conditionName, gameData))) {
                  imagePaths.add(unitIconDescriptor.unitIconPath);
                }
              },
              () -> imagePaths.add(unitIconDescriptor.unitIconPath));
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

  private Map<ICondition, Boolean> getTestedConditions(final GameData data) {
    final HashSet<ICondition> allConditionsNeeded =
        AbstractConditionsAttachment.getAllConditionsRecursive(new HashSet<>(conditionsStatus.keySet()), null);
    return AbstractConditionsAttachment.testAllConditionsRecursive(allConditionsNeeded, null,
        new ObjectiveDummyDelegateBridge(data));
  }

  @VisibleForTesting
  interface ConditionSupplier {
    @Nullable
    ICondition getCondition(String playerName, String conditionName, GameData gameData);
  }

  @AllArgsConstructor
  @EqualsAndHashCode
  @ToString
  @VisibleForTesting
  static final class UnitIconDescriptor {
    final String gameName;
    final String playerName;
    final String unitTypeName;
    final Optional<String> conditionName;
    final String unitIconPath;
  }
}
