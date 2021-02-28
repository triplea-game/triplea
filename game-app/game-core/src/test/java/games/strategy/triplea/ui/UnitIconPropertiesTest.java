package games.strategy.triplea.ui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameState;
import games.strategy.triplea.attachments.ICondition;
import games.strategy.triplea.ui.UnitIconProperties.MalformedUnitIconDescriptorException;
import games.strategy.triplea.ui.UnitIconProperties.UnitIconDescriptor;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import javax.annotation.Nullable;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
    return formatIconId(GAME_NAME, PLAYER_NAME, UNIT_TYPE_NAME, conditionName);
  }

  private static String formatIconId(
      final String gameName,
      final String playerName,
      final String unitTypeName,
      final @Nullable String conditionName) {
    final String encodedUnitTypeId = String.join(".", gameName, playerName, unitTypeName);
    return (conditionName != null)
        ? String.join(";", encodedUnitTypeId, conditionName)
        : encodedUnitTypeId;
  }

  @Nested
  final class GetImagePathsTest {
    private GameData givenGameData() {
      final GameData gameData = new GameData();
      gameData.setGameName(GAME_NAME);
      return gameData;
    }

    private ICondition givenCondition(final String name, final boolean satisfied) {
      final ICondition condition = mock(ICondition.class);
      when(condition.getConditions()).thenReturn(List.of());
      when(condition.getName()).thenReturn(name);
      when(condition.isSatisfied(any(), any())).thenReturn(satisfied);
      return condition;
    }

    private UnitIconProperties.ConditionSupplier givenConditionSupplier(
        final GameState gameData, final ICondition... conditions) {
      final UnitIconProperties.ConditionSupplier conditionSupplier =
          mock(UnitIconProperties.ConditionSupplier.class);
      for (final ICondition condition : conditions) {
        when(conditionSupplier.getCondition(PLAYER_NAME, condition.getName(), gameData))
            .thenReturn(condition);
      }
      return conditionSupplier;
    }

    @Test
    void shouldReturnImagePathsForIconsWithSatisfiedConditionOrNoCondition() {
      final GameData gameData = givenGameData();
      final UnitIconProperties.ConditionSupplier conditionSupplier =
          givenConditionSupplier(
              gameData,
              givenCondition(CONDITION_1_NAME, true),
              givenCondition(CONDITION_2_NAME, false));
      final Properties properties = new OrderedProperties();
      properties.put(formatIconId(CONDITION_1_NAME), ICON_1_PATH);
      properties.put(formatIconId(CONDITION_2_NAME), ICON_2_PATH);
      properties.put(formatIconId(null), ICON_3_PATH);
      final UnitIconProperties unitIconProperties =
          new UnitIconProperties(properties, gameData, conditionSupplier);

      assertThat(
          unitIconProperties.getImagePaths(
              PLAYER_NAME, UNIT_TYPE_NAME, gameData, conditionSupplier),
          contains(ICON_1_PATH, ICON_3_PATH));
    }

    @Test
    void shouldIgnoreIconsThatHaveDifferentUnitTypeId() {
      final GameData gameData = givenGameData();
      final UnitIconProperties.ConditionSupplier conditionSupplier =
          givenConditionSupplier(gameData);
      final Properties properties = new OrderedProperties();
      properties.put(formatIconId("otherGameName", PLAYER_NAME, UNIT_TYPE_NAME, null), ICON_1_PATH);
      properties.put(formatIconId(GAME_NAME, "otherPlayerName", UNIT_TYPE_NAME, null), ICON_2_PATH);
      properties.put(formatIconId(GAME_NAME, PLAYER_NAME, "otherUnitTypeName", null), ICON_3_PATH);
      final UnitIconProperties unitIconProperties =
          new UnitIconProperties(properties, gameData, conditionSupplier);

      assertThat(
          unitIconProperties.getImagePaths(
              PLAYER_NAME, UNIT_TYPE_NAME, gameData, conditionSupplier),
          is(empty()));
    }
  }

  @Nested
  final class ParseUnitIconDescriptorTest {
    private UnitIconDescriptor newUnitIconDescriptorWithCondition(
        final @Nullable String conditionName) {
      return new UnitIconDescriptor(
          GAME_NAME, PLAYER_NAME, UNIT_TYPE_NAME, Optional.ofNullable(conditionName), ICON_1_PATH);
    }

    private UnitIconDescriptor parseUnitIconDescriptorWithCondition(
        final @Nullable String conditionName) {
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
      assertThat(
          parseUnitIconDescriptorWithCondition(null), is(newUnitIconDescriptorWithCondition(null)));
    }

    @Test
    void shouldThrowExceptionWhenIconIdTokenCountNotEqualToOneOrTwo() {
      assertThrows(
          MalformedUnitIconDescriptorException.class,
          () -> UnitIconProperties.parseUnitIconDescriptor("", ICON_1_PATH));
      assertThrows(
          MalformedUnitIconDescriptorException.class,
          () -> UnitIconProperties.parseUnitIconDescriptor("a1.a2.a3;b1;c1", ICON_1_PATH));
    }

    @Test
    void shouldThrowExceptionWhenUnitTypeIdTokenCountNotEqualToThree() {
      assertThrows(
          MalformedUnitIconDescriptorException.class,
          () -> UnitIconProperties.parseUnitIconDescriptor("a1.a2;b1", ICON_1_PATH));
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

    @Nested
    final class MatchesGameNameTest {
      private final UnitIconDescriptor unitIconDescriptor =
          new UnitIconDescriptor(
              GAME_NAME, PLAYER_NAME, UNIT_TYPE_NAME, Optional.empty(), ICON_1_PATH);

      @Test
      void shouldReturnTrueWhenGameNameMatches() {
        assertThat(unitIconDescriptor.matches(GAME_NAME), is(true));
      }

      @Test
      void shouldReturnFalseWhenGameNameDoesNotMatch() {
        assertThat(unitIconDescriptor.matches("otherGameName"), is(false));
      }
    }

    @Nested
    final class MatchesGameNameAndPlayerNameAndUnitTypeNameTest {
      private final UnitIconDescriptor unitIconDescriptor =
          new UnitIconDescriptor(
              GAME_NAME, PLAYER_NAME, UNIT_TYPE_NAME, Optional.empty(), ICON_1_PATH);

      @Test
      void shouldReturnTrueWhenGameNameAndPlayerNameAndUnitTypeNameMatches() {
        assertThat(unitIconDescriptor.matches(GAME_NAME, PLAYER_NAME, UNIT_TYPE_NAME), is(true));
      }

      @Test
      void shouldReturnFalseWhenGameNameOrPlayerNameOrUnitTypeNameDoesNotMatch() {
        assertThat(
            unitIconDescriptor.matches("otherGameName", PLAYER_NAME, UNIT_TYPE_NAME), is(false));
        assertThat(
            unitIconDescriptor.matches(GAME_NAME, "otherPlayerName", UNIT_TYPE_NAME), is(false));
        assertThat(
            unitIconDescriptor.matches(GAME_NAME, PLAYER_NAME, "otherUnitTypeName"), is(false));
      }
    }
  }
}
