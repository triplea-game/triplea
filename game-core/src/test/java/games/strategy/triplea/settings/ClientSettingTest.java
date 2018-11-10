package games.strategy.triplea.settings;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.function.Consumer;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import nl.jqno.equalsverifier.EqualsVerifier;

final class ClientSettingTest {
  @Nested
  final class EqualsAndHashCodeTest {
    @Test
    void shouldBeEquatableAndHashable() {
      EqualsVerifier.forClass(ClientSetting.class)
          .withNonnullFields("name")
          .withOnlyTheseFields("name")
          .verify();
    }
  }

  @Nested
  final class GetValueTest extends AbstractClientSettingTestCase {
    @Test
    void shouldReturnDefaultValueWhenParseValueThrowsException() {
      final String defaultValue = "defaultValue";
      final ClientSetting<String> clientSetting = new FakeClientSetting("name", defaultValue) {
        @Override
        protected String parseValue(final String encodedValue) {
          if (defaultValue.equals(encodedValue)) {
            return defaultValue;
          }

          throw new IllegalArgumentException();
        }
      };
      clientSetting.setValue("otherValue");

      assertThat(clientSetting.getValue(), isPresentAndIs(defaultValue));
    }
  }

  @Nested
  final class SetValueTest extends AbstractClientSettingTestCase {
    @Test
    void shouldClearPreferenceWhenValueEqualsDefaultValue() {
      final String name = "name";
      final String defaultValue = "defaultValue";
      final ClientSetting<String> clientSetting = new FakeClientSetting(name, defaultValue);
      clientSetting.setValue("otherValue");

      clientSetting.setValue(defaultValue);

      assertThat(getPreferences().get(name, null), is(nullValue()));
    }
  }

  @ExtendWith(MockitoExtension.class)
  @Nested
  final class ListenerTest extends AbstractClientSettingTestCase {
    private static final String TEST_VALUE = "value";

    @Mock
    private Consumer<GameSetting<String>> listener;
    private final ClientSetting<String> clientSetting = new FakeClientSetting("TEST_SETTING");

    @Test
    void listenerShouldBeCalled() {
      doAnswer(invocation -> {
        @SuppressWarnings("unchecked")
        final GameSetting<String> gameSetting = (GameSetting<String>) invocation.getArgument(0);
        assertThat(gameSetting.getValue(), isPresentAndIs(TEST_VALUE));
        return null;
      }).when(listener).accept(clientSetting);
      clientSetting.addListener(listener);

      clientSetting.setValue(TEST_VALUE);

      verify(listener).accept(clientSetting);
    }

    @Test
    void removedListenerShouldNotBeCalled() {
      clientSetting.addListener(listener);
      clientSetting.removeListener(listener);

      clientSetting.setValue(TEST_VALUE);

      verify(listener, never()).accept(clientSetting);
    }
  }

  @Nested
  final class ToStringTest {
    @Test
    void shouldReturnName() {
      final String name = "name";
      final ClientSetting<String> clientSetting = new FakeClientSetting(name);

      assertThat(clientSetting.toString(), is(name));
    }
  }

  private static class FakeClientSetting extends ClientSetting<String> {
    FakeClientSetting(final String name) {
      super(String.class, name);
    }

    FakeClientSetting(final String name, final String defaultValue) {
      super(String.class, name, defaultValue);
    }

    @Override
    protected String formatValue(final String value) {
      return value;
    }

    @Override
    protected String parseValue(final String encodedValue) {
      return encodedValue;
    }
  }
}
