package games.strategy.triplea.printgenerator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class InfoForFileTest {

  @Test
  void csvFieldReturnsValueUnchangedWhenNoSpecialChars() {
    assertThat(InfoForFile.csvField("Athens"), is("Athens"));
  }

  @Test
  void csvFieldReturnsEmptyStringForNull() {
    assertThat(InfoForFile.csvField(null), is(""));
  }

  @Test
  void csvFieldQuotesValueContainingSpace() {
    assertThat(InfoForFile.csvField("Cestra Regina"), is("\"Cestra Regina\""));
  }

  @Test
  void csvFieldQuotesValueContainingComma() {
    assertThat(InfoForFile.csvField("Foo,Bar"), is("\"Foo,Bar\""));
  }

  @Test
  void csvFieldDoublesInnerDoubleQuotes() {
    assertThat(InfoForFile.csvField("a\"b"), is("\"a\"\"b\""));
  }

  @Test
  void csvFieldQuotesValueContainingNewline() {
    assertThat(InfoForFile.csvField("a\nb"), is("\"a\nb\""));
    assertThat(InfoForFile.csvField("a\rb"), is("\"a\rb\""));
  }
}
