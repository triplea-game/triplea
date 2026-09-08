package org.triplea.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class FileNameUtilsTest {
  @Nested
  final class RemoveIllegalCharactersTest {
    @Test
    void shouldRemoveIllegalCharacters() {
      assertThat(FileNameUtils.removeIllegalCharacters(FileNameUtils.ILLEGAL_CHARACTERS)).isEmpty();
    }

    @Test
    void shouldNotRemoveLegalCharacters() {
      assertThat(FileNameUtils.removeIllegalCharacters("AZaz09!-")).isEqualTo("AZaz09!-");
    }
  }

  @Nested
  final class ReplaceIllegalCharactersTest {
    @Test
    void shouldReplaceIllegalCharacters() {
      assertThat(FileNameUtils.replaceIllegalCharacters(FileNameUtils.ILLEGAL_CHARACTERS, '_'))
          .isEqualTo(
              String.join("", Collections.nCopies(FileNameUtils.ILLEGAL_CHARACTERS.length(), "_")));
    }

    @Test
    void shouldNotReplaceLegalCharacters() {
      assertThat(FileNameUtils.replaceIllegalCharacters("AZaz09!-", '_')).isEqualTo("AZaz09!-");
    }
  }
}
