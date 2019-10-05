package games.strategy.triplea.settings;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.function.Consumer;
import javax.annotation.Nullable;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.java.function.ThrowingFunction;

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
    void shouldReturnDefaultValueWhenDecodeValueThrowsException() {
      final String defaultValue = "defaultValue";
      final String illegalValue = "illegalValue";
      final ClientSetting<String> clientSetting =
          new FakeClientSetting(
              "name",
              defaultValue,
              FakeClientSetting.DEFAULT_ENCODE_VALUE,
              encodedValue -> {
                if (illegalValue.equals(encodedValue)) {
                  throw new ClientSetting.ValueEncodingException();
                }

                return FakeClientSetting.DEFAULT_DECODE_VALUE.apply(encodedValue);
              });
      clientSetting.setValue(illegalValue);

      assertThat(clientSetting.getValue(), isPresentAndIs(defaultValue));
    }
  }

  @Nested
  final class SetValueTest extends AbstractClientSettingTestCase {
    @Test
    void shouldNotChangeValueWhenEncodeValueThrowsException() {
      final String defaultValue = "defaultValue";
      final String legalValue = "legalValue";
      final String illegalValue = "illegalValue";
      final ClientSetting<String> clientSetting =
          new FakeClientSetting(
              "name",
              defaultValue,
              value -> {
                if (illegalValue.equals(value)) {
                  throw new ClientSetting.ValueEncodingException();
                }

                return FakeClientSetting.DEFAULT_ENCODE_VALUE.apply(value);
              },
              FakeClientSetting.DEFAULT_DECODE_VALUE);
      clientSetting.setValue(legalValue);

      clientSetting.setValue(illegalValue);

      assertThat(clientSetting.getValue(), isPresentAndIs(legalValue));
    }

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

    @Mock private Consumer<GameSetting<String>> listener;
    private final ClientSetting<String> clientSetting = new FakeClientSetting("TEST_SETTING");

    @Test
    void listenerShouldBeCalled() {
      doAnswer(
              invocation -> {
                final GameSetting<String> gameSetting = invocation.getArgument(0);
                assertThat(gameSetting.getValue(), isPresentAndIs(TEST_VALUE));
                return null;
              })
          .when(listener)
          .accept(clientSetting);
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

  private static final class FakeClientSetting extends ClientSetting<String> {
    static final ThrowingFunction<String, String, ValueEncodingException> DEFAULT_ENCODE_VALUE =
        value -> value;
    static final ThrowingFunction<String, String, ValueEncodingException> DEFAULT_DECODE_VALUE =
        encodedValue -> encodedValue;

    private final ThrowingFunction<String, String, ValueEncodingException> encodeValue;
    private final ThrowingFunction<String, String, ValueEncodingException> decodeValue;

    FakeClientSetting(final String name) {
      this(name, null);
    }

    FakeClientSetting(final String name, final @Nullable String defaultValue) {
      this(name, defaultValue, DEFAULT_ENCODE_VALUE, DEFAULT_DECODE_VALUE);
    }

    FakeClientSetting(
        final String name,
        final @Nullable String defaultValue,
        final ThrowingFunction<String, String, ValueEncodingException> encodeValue,
        final ThrowingFunction<String, String, ValueEncodingException> decodeValue) {
      super(String.class, name, defaultValue);

      this.encodeValue = encodeValue;
      this.decodeValue = decodeValue;
    }

    @Override
    protected String encodeValue(final String value) throws ValueEncodingException {
      return encodeValue.apply(value);
    }

    @Override
    protected String decodeValue(final String encodedValue) throws ValueEncodingException {
      return decodeValue.apply(encodedValue);
    }
  }
}
