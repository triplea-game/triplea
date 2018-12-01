package games.strategy.engine.random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import java.util.Properties;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import games.strategy.engine.framework.startup.ui.editors.IBean;

final class PropertiesDiceRollerTest {
  @Nested
  final class IsSameTypeTest {
    private final PropertiesDiceRoller reference = new PropertiesDiceRoller(newProperties("name1"));

    private Properties newProperties(final String displayName) {
      final Properties properties = new Properties();
      properties.put(PropertiesDiceRoller.PropertyKeys.DISPLAY_NAME, displayName);
      return properties;
    }

    @Test
    void shouldReturnTrueWhenOtherHasSameClassAndDisplayName() {
      assertThat(reference.isSameType(new PropertiesDiceRoller(newProperties("name1"))), is(true));
    }

    @Test
    void shouldReturnFalseWhenOtherHasSameClassButDifferentDisplayName() {
      assertThat(reference.isSameType(new PropertiesDiceRoller(newProperties("name2"))), is(false));
    }

    @Test
    void shouldReturnFalseWhenOtherHasDifferentClass() {
      assertThat(reference.isSameType(mock(IBean.class)), is(false));
    }

    @Test
    void shouldReturnFalseWhenOtherIsNull() {
      assertThat(reference.isSameType(null), is(false));
    }
  }
}
