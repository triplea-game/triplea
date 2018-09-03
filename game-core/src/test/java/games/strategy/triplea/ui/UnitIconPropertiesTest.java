package games.strategy.triplea.ui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Properties;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.base.Joiner;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.attachments.ICondition;
import games.strategy.triplea.ui.UnitIconProperties.UnitIconDescriptor;
import nl.jqno.equalsverifier.EqualsVerifier;

final class UnitIconPropertiesTest {
  private static final String CONDITION_1_NAME = "condition1Name";
  private static final String CONDITION_2_NAME = "condition2Name";
  private static final String GAME_NAME = "gameName";
  private static final String ICON_1_PATH = "path/to/icon_1.png";
  private static final String ICON_2_PATH = "path/to/icon_2.png";
  private static final String ICON_3_PATH = "path/to/icon_3.png";
  private static final String PLAYER_NAME = "playerName";
  private static final String UNIT_TYPE_NAME = "unitTypeName";

  private static String formatIconId(final @Nullable String conditionName) {
    final String encodedUnitTypeId = Joiner.on('.').join(GAME_NAME, PLAYER_NAME, UNIT_TYPE_NAME);
    return (conditionName != null)
        ? Joiner.on(';').join(encodedUnitTypeId, conditionName)
        : encodedUnitTypeId;
  }

  @Nested
  final class GetImagePathsTest {
    private GameData givenGameData() {
      final GameData gameData = new GameData();
      gameData.setGameName(GAME_NAME);
      return gameData;
    }

    private ICondition givenCondition(final String name) {
      final ICondition condition = mock(ICondition.class);
      when(condition.getName()).thenReturn(name);
      return condition;
    }

    private UnitIconProperties.ConditionSupplier givenConditionSupplier(final GameData gameData) {
      final UnitIconProperties.ConditionSupplier conditionSupplier = mock(UnitIconProperties.ConditionSupplier.class);
      final ICondition condition1 = givenCondition(CONDITION_1_NAME);
      when(conditionSupplier.getCondition(PLAYER_NAME, CONDITION_1_NAME, gameData)).thenReturn(condition1);
      final ICondition condition2 = givenCondition(CONDITION_2_NAME);
      when(conditionSupplier.getCondition(PLAYER_NAME, CONDITION_2_NAME, gameData)).thenReturn(condition2);
      return conditionSupplier;
    }

    @Test
    void shouldReturnImagePathsForIconsWithEnabledConditionOrNoCondition() {
      final GameData gameData = givenGameData();
      final UnitIconProperties.ConditionSupplier conditionSupplier = givenConditionSupplier(gameData);
      final Properties properties = new OrderedProperties();
      properties.put(formatIconId(CONDITION_1_NAME), ICON_1_PATH);
      properties.put(formatIconId(CONDITION_2_NAME), ICON_2_PATH);
      properties.put(formatIconId(null), ICON_3_PATH);
      final UnitIconProperties unitIconProperties = new UnitIconProperties(properties, gameData, conditionSupplier);
      final Predicate<ICondition> isConditionEnabled = condition -> CONDITION_1_NAME.equals(condition.getName());

      assertThat(
          unitIconProperties.getImagePaths(
              PLAYER_NAME,
              UNIT_TYPE_NAME,
              gameData,
              conditionSupplier,
              isConditionEnabled),
          contains(ICON_1_PATH, ICON_3_PATH));
    }
  }

  @Nested
  final class ParseUnitIconDescriptorTest {
    private UnitIconDescriptor newUnitIconDescriptorWithCondition(final @Nullable String conditionName) {
      return new UnitIconDescriptor(
          GAME_NAME,
          PLAYER_NAME,
          UNIT_TYPE_NAME,
          Optional.ofNullable(conditionName),
          ICON_1_PATH);
    }

    private UnitIconDescriptor parseUnitIconDescriptorWithCondition(final @Nullable String conditionName) {
      return UnitIconProperties.parseUnitIconDescriptor(formatIconId(conditionName), ICON_1_PATH);
    }

    @Test
    void shouldParseUnitIconDescriptorWithCondition() {
      assertThat(
          parseUnitIconDescriptorWithCondition(CONDITION_1_NAME),
          is(newUnitIconDescriptorWithCondition(CONDITION_1_NAME)));
    }

    @Test
    void shouldParseUnitIconDescriptorWithoutCondition() {
      assertThat(parseUnitIconDescriptorWithCondition(null), is(newUnitIconDescriptorWithCondition(null)));
    }
  }

  @Nested
  final class UnitIconDescriptorTest {
    @Nested
    final class EqualsAndHashCodeTest {
      @Test
      void shouldBeEquatableAndHashable() {
        EqualsVerifier.forClass(UnitIconDescriptor.class).verify();
      }
    }
  }
}
