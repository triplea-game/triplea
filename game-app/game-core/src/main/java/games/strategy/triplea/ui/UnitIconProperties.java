package games.strategy.triplea.ui;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameState;
import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.attachments.AbstractConditionsAttachment;
import games.strategy.triplea.attachments.AbstractPlayerRulesAttachment;
import games.strategy.triplea.attachments.ICondition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.triplea.util.FileNameUtils;

/** Loads unit icons from unit_icons.properties. */
@Slf4j
public final class UnitIconProperties {
  @NonNls private static final String PROPERTY_FILE = "unit_icons.properties";

  private final Properties properties;
  private Map<ICondition, Boolean> conditionsStatus = new HashMap<>();
  private final ImmutableList<UnitIconDescriptor> unitIconDescriptors;

  private UnitIconProperties(final Properties properties) {
    this.properties = properties;
    this.unitIconDescriptors = parseUnitIconDescriptors();
  }

  public UnitIconProperties(final GameData gameData, final ResourceLoader loader) {
    this(loader.loadPropertyFile(PROPERTY_FILE));
    initializeConditionsStatus(gameData, AbstractPlayerRulesAttachment::getCondition);
  }

  @VisibleForTesting
  UnitIconProperties(
      final Properties properties,
      final GameData gameData,
      final ConditionSupplier conditionSupplier) {
    this(properties);
    initializeConditionsStatus(gameData, conditionSupplier);
  }

  private ImmutableList<UnitIconDescriptor> parseUnitIconDescriptors() {
    final ImmutableList.Builder<UnitIconDescriptor> builder = ImmutableList.builder();
    for (final Map.Entry<Object, Object> property : properties.entrySet()) {
      try {
        builder.add(
            parseUnitIconDescriptor(property.getKey().toString(), property.getValue().toString()));
      } catch (final MalformedUnitIconDescriptorException e) {
        log.warn(
            "Expected "
                + PROPERTY_FILE
                + " property key to be of the form "
                + "<game_name>.<player>.<unit_type>;attachmentName OR if always true "
                + "<game_name>.<player>.<unit_type>",
            e);
      }
    }
    return builder.build();
  }

  /**
   * Parses a unit icon descriptor from a line in a unit_icons.properties file.
   *
   * <p>Each line in a unit_icons.properties file has the format:
   *
   * <p>{@code <encodedIconId>=<iconPath>}
   *
   * <p>Where {@code encodedIconId} is
   *
   * <p>{@code <encodedUnitTypeId>[;conditionName]}
   *
   * <p>Where {@code encodedUnitTypeId} is
   *
   * <p>{@code <gameName>.<playerName>.<unitTypeName>}
   */
  @VisibleForTesting
  static UnitIconDescriptor parseUnitIconDescriptor(
      final String encodedIconId, final String iconPath) {
    final List<String> iconIdTokens = Splitter.on(';').splitToList(encodedIconId);
    if (iconIdTokens.size() != 1 && iconIdTokens.size() != 2) {
      throw new MalformedUnitIconDescriptorException(encodedIconId);
    }
    final String encodedUnitTypeId = iconIdTokens.get(0);
    final Optional<String> conditionName =
        (iconIdTokens.size() == 2) ? Optional.ofNullable(iconIdTokens.get(1)) : Optional.empty();

    final List<String> unitTypeIdTokens = Splitter.on('.').limit(3).splitToList(encodedUnitTypeId);
    if (unitTypeIdTokens.size() != 3) {
      throw new MalformedUnitIconDescriptorException(encodedIconId);
    }
    final String gameName = unitTypeIdTokens.get(0);
    final String playerName = unitTypeIdTokens.get(1);
    final String unitTypeName = unitTypeIdTokens.get(2);

    return new UnitIconDescriptor(gameName, playerName, unitTypeName, conditionName, iconPath);
  }

  private void initializeConditionsStatus(
      final GameData gameData, final ConditionSupplier conditionSupplier) {
    final String gameName = normalizeGameName(gameData);
    for (final UnitIconDescriptor unitIconDescriptor : unitIconDescriptors) {
      if (unitIconDescriptor.matches(gameName)) {
        unitIconDescriptor.conditionName.ifPresent(
            conditionName -> {
              final @Nullable ICondition condition =
                  conditionSupplier.getCondition(
                      unitIconDescriptor.playerName, conditionName, gameData);
              if (condition != null) {
                conditionsStatus.put(condition, false);
              }
            });
      }
    }
    conditionsStatus = getTestedConditions(gameData);
  }

  private static String normalizeGameName(final GameData gameData) {
    return FileNameUtils.replaceIllegalCharacters(gameData.getGameName(), '_').replaceAll(" ", "_");
  }

  /**
   * Get all unit icon images for given player and unit type that are currently true, ensuring order
   * from the properties file.
   */
  public List<String> getImagePaths(
      final String playerName, final String unitTypeName, final GameData gameData) {
    return getImagePaths(
        playerName, unitTypeName, gameData, AbstractPlayerRulesAttachment::getCondition);
  }

  @VisibleForTesting
  List<String> getImagePaths(
      final String playerName,
      final String unitTypeName,
      final GameData gameData,
      final ConditionSupplier conditionSupplier) {
    final List<String> imagePaths = new ArrayList<>();
    final String gameName = normalizeGameName(gameData);
    for (final UnitIconDescriptor unitIconDescriptor : unitIconDescriptors) {
      if (unitIconDescriptor.matches(gameName, playerName, unitTypeName)) {
        unitIconDescriptor.conditionName.ifPresentOrElse(
            conditionName -> {
              if (conditionsStatus.get(
                  conditionSupplier.getCondition(playerName, conditionName, gameData))) {
                imagePaths.add(unitIconDescriptor.unitIconPath);
              }
            },
            () -> imagePaths.add(unitIconDescriptor.unitIconPath));
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
    final Set<ICondition> allConditionsNeeded =
        AbstractConditionsAttachment.getAllConditionsRecursive(
            new HashSet<>(conditionsStatus.keySet()), null);
    return AbstractConditionsAttachment.testAllConditionsRecursive(
        allConditionsNeeded, null, new ObjectiveDummyDelegateBridge(data));
  }

  @VisibleForTesting
  interface ConditionSupplier {
    @Nullable
    ICondition getCondition(String playerName, String conditionName, GameState gameData);
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

    boolean matches(final String gameName) {
      return this.gameName.equals(gameName);
    }

    boolean matches(final String gameName, final String playerName, final String unitTypeName) {
      return this.gameName.equals(gameName)
          && this.playerName.equals(playerName)
          && this.unitTypeName.equals(unitTypeName);
    }
  }

  @VisibleForTesting
  static final class MalformedUnitIconDescriptorException extends RuntimeException {
    private static final long serialVersionUID = 6408256411338749491L;

    MalformedUnitIconDescriptorException(final String message) {
      super(message);
    }
  }
}
