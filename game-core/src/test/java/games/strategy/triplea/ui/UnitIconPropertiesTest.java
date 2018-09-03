package games.strategy.triplea.ui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Optional;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.base.Joiner;

import games.strategy.triplea.ui.UnitIconProperties.UnitIconDescriptor;
import nl.jqno.equalsverifier.EqualsVerifier;

final class UnitIconPropertiesTest {
  @Nested
  final class ParseUnitIconDescriptorTest {
    private static final String GAME_NAME = "gameName";
    private static final String ICON_PATH = "path/to/icon.png";
    private static final String PLAYER_NAME = "playerName";
    private static final String UNIT_TYPE_NAME = "unitTypeName";

    private UnitIconDescriptor newUnitIconDescriptorWithCondition(final @Nullable String conditionName) {
      return new UnitIconDescriptor(
          GAME_NAME,
          PLAYER_NAME,
          UNIT_TYPE_NAME,
          Optional.ofNullable(conditionName),
          ICON_PATH);
    }

    private UnitIconDescriptor parseUnitIconDescriptorWithCondition(final @Nullable String conditionName) {
      final String encodedUnitTypeId = Joiner.on('.').join(GAME_NAME, PLAYER_NAME, UNIT_TYPE_NAME);
      final String encodedIconId = (conditionName != null)
          ? Joiner.on(';').join(encodedUnitTypeId, conditionName)
          : encodedUnitTypeId;
      return UnitIconProperties.parseUnitIconDescriptor(encodedIconId, ICON_PATH);
    }

    @Test
    void shouldParseUnitIconDescriptorWithCondition() {
      final String conditionName = "conditionName";

      assertThat(
          parseUnitIconDescriptorWithCondition(conditionName),
          is(newUnitIconDescriptorWithCondition(conditionName)));
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
