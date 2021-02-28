package games.strategy.engine.framework;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.apache.commons.io.IOCase;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class GameDataFileUtilsTest {
  @Nested
  final class AddExtensionTest {
    private String addExtension(final String fileName) {
      return GameDataFileUtils.addExtension(fileName);
    }

    @Test
    void shouldAddExtensionWhenExtensionAbsent() {
      assertThat(addExtension("file"), is("file.tsvg"));
    }

    @Test
    void shouldAddExtensionWhenExtensionPresent() {
      assertThat(addExtension("file.tsvg"), is("file.tsvg.tsvg"));
    }
  }

  @Nested
  final class AddExtensionIfAbsentTest {
    @Nested
    final class WhenFileSystemIsCaseSensitiveTest {
      private String addExtensionIfAbsent(final String fileName) {
        return GameDataFileUtils.addExtensionIfAbsent(fileName, IOCase.SENSITIVE);
      }

      @Test
      void shouldAddExtensionWhenExtensionAbsent() {
        assertThat(addExtensionIfAbsent("file"), is("file.tsvg"));
      }

      @Test
      void shouldNotAddExtensionWhenSameCasedExtensionPresent() {
        assertThat(addExtensionIfAbsent("file.tsvg"), is("file.tsvg"));
      }

      @Test
      void shouldAddExtensionWhenDifferentCasedExtensionPresent() {
        assertThat(addExtensionIfAbsent("file.TSVG"), is("file.TSVG.tsvg"));
      }
    }

    @Nested
    final class WhenFileSystemIsCaseInsensitiveTest {
      private String addExtensionIfAbsent(final String fileName) {
        return GameDataFileUtils.addExtensionIfAbsent(fileName, IOCase.INSENSITIVE);
      }

      @Test
      void shouldAddExtensionWhenExtensionAbsent() {
        assertThat(addExtensionIfAbsent("file"), is("file.tsvg"));
      }

      @Test
      void shouldNotAddExtensionWhenSameCasedExtensionPresent() {
        assertThat(addExtensionIfAbsent("file.tsvg"), is("file.tsvg"));
      }

      @Test
      void shouldNotAddExtensionWhenDifferentCasedExtensionPresent() {
        assertThat(addExtensionIfAbsent("file.TSVG"), is("file.TSVG"));
      }
    }
  }

  @Nested
  final class IsCandidateFileNameTest {
    @Nested
    final class WhenFileSystemIsCaseSensitiveTest {
      private boolean isCandidateFileName(final String fileName) {
        return GameDataFileUtils.isCandidateFileName(fileName, IOCase.SENSITIVE);
      }

      @Test
      void shouldReturnFalseWhenExtensionAbsent() {
        assertThat(isCandidateFileName("file"), is(false));
      }

      @Test
      void shouldReturnTrueWhenSameCasedPrimaryExtensionPresent() {
        assertThat(isCandidateFileName("file.tsvg"), is(true));
      }

      @Test
      void shouldReturnFalseWhenDifferentCasedPrimaryExtensionPresent() {
        assertThat(isCandidateFileName("file.TSVG"), is(false));
      }

      @Test
      void shouldReturnTrueWhenSameCasedLegacyExtensionPresent() {
        assertThat(isCandidateFileName("file.svg"), is(true));
      }

      @Test
      void shouldReturnFalseWhenDifferentCasedLegacyExtensionPresent() {
        assertThat(isCandidateFileName("file.SVG"), is(false));
      }

      @Test
      void shouldReturnTrueWhenSameCasedMacOsAlternativeExtensionPresent() {
        assertThat(isCandidateFileName("filetsvg.gz"), is(true));
      }

      @Test
      void shouldReturnFalseWhenDifferentCasedMacOsAlternativeExtensionPresent() {
        assertThat(isCandidateFileName("fileTSVG.GZ"), is(false));
      }
    }

    @Nested
    final class WhenFileSystemIsCaseInsensitiveTest {
      private boolean isCandidateFileName(final String fileName) {
        return GameDataFileUtils.isCandidateFileName(fileName, IOCase.INSENSITIVE);
      }

      @Test
      void shouldReturnFalseWhenExtensionAbsent() {
        assertThat(isCandidateFileName("file"), is(false));
      }

      @Test
      void shouldReturnTrueWhenSameCasedPrimaryExtensionPresent() {
        assertThat(isCandidateFileName("file.tsvg"), is(true));
      }

      @Test
      void shouldReturnTrueWhenDifferentCasedPrimaryExtensionPresent() {
        assertThat(isCandidateFileName("file.TSVG"), is(true));
      }

      @Test
      void shouldReturnTrueWhenSameCasedLegacyExtensionPresent() {
        assertThat(isCandidateFileName("file.svg"), is(true));
      }

      @Test
      void shouldReturnTrueWhenDifferentCasedLegacyExtensionPresent() {
        assertThat(isCandidateFileName("file.SVG"), is(true));
      }

      @Test
      void shouldReturnTrueWhenSameCasedMacOsAlternativeExtensionPresent() {
        assertThat(isCandidateFileName("filetsvg.gz"), is(true));
      }

      @Test
      void shouldReturnTrueWhenDifferentCasedMacOsAlternativeExtensionPresent() {
        assertThat(isCandidateFileName("fileTSVG.GZ"), is(true));
      }
    }
  }
}
