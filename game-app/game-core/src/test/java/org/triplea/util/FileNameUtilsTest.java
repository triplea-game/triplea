package org.triplea.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class FileNameUtilsTest {
  @Nested
  final class RemoveIllegalCharactersTest {
    @Test
    void shouldRemoveIllegalCharacters() {
      assertThat(FileNameUtils.removeIllegalCharacters(FileNameUtils.ILLEGAL_CHARACTERS), is(""));
    }

    @Test
    void shouldNotRemoveLegalCharacters() {
      assertThat(FileNameUtils.removeIllegalCharacters("AZaz09!-"), is("AZaz09!-"));
    }
  }

  @Nested
  final class ReplaceIllegalCharactersTest {
    @Test
    void shouldReplaceIllegalCharacters() {
      assertThat(
          FileNameUtils.replaceIllegalCharacters(FileNameUtils.ILLEGAL_CHARACTERS, '_'),
          is(String.join("", Collections.nCopies(FileNameUtils.ILLEGAL_CHARACTERS.length(), "_"))));
    }

    @Test
    void shouldNotReplaceLegalCharacters() {
      assertThat(FileNameUtils.replaceIllegalCharacters("AZaz09!-", '_'), is("AZaz09!-"));
    }
  }
}
