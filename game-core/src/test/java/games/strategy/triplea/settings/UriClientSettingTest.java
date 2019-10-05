package games.strategy.triplea.settings;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class UriClientSettingTest {
  private final UriClientSetting clientSetting = new UriClientSetting("name");

  @Nested
  final class EncodeValueTest {
    @Test
    void shouldReturnEncodedValue() {
      assertThat(
          clientSetting.encodeValue(URI.create("http://localhost:1234/path")),
          is("http://localhost:1234/path"));
    }
  }

  @Nested
  final class DecodeValueTest {
    @Test
    void shouldReturnUriWhenEncodedValueIsLegal() throws Exception {
      assertThat(
          clientSetting.decodeValue("http://localhost:1234/path"),
          is(URI.create("http://localhost:1234/path")));
    }

    @Test
    void shouldThrowExceptionWhenEncodedValueIsIllegal() {
      final Exception e =
          assertThrows(
              ClientSetting.ValueEncodingException.class,
              () -> clientSetting.decodeValue(":not_a_uri"));
      assertThat(e.getCause(), is(instanceOf(URISyntaxException.class)));
    }
  }
}
