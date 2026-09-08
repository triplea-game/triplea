package games.strategy.triplea.printgenerator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InfoForFileTest {

  @Test
  void csvFieldReturnsValueUnchangedWhenNoSpecialChars() {
    assertThat(InfoForFile.csvField("Athens")).isEqualTo("Athens");
  }

  @Test
  void csvFieldReturnsEmptyStringForNull() {
    assertThat(InfoForFile.csvField(null)).isEqualTo("");
  }

  @Test
  void csvFieldQuotesValueContainingSpace() {
    assertThat(InfoForFile.csvField("Cestra Regina")).isEqualTo("\"Cestra Regina\"");
  }

  @Test
  void csvFieldQuotesValueContainingComma() {
    assertThat(InfoForFile.csvField("Foo,Bar")).isEqualTo("\"Foo,Bar\"");
  }

  @Test
  void csvFieldDoublesInnerDoubleQuotes() {
    assertThat(InfoForFile.csvField("a\"b")).isEqualTo("\"a\"\"b\"");
  }

  @Test
  void csvFieldQuotesValueContainingNewline() {
    assertThat(InfoForFile.csvField("a\nb")).isEqualTo("\"a\nb\"");
    assertThat(InfoForFile.csvField("a\rb")).isEqualTo("\"a\rb\"");
  }
}
