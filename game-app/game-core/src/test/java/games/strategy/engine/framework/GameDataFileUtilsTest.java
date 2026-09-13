package games.strategy.engine.framework;

import static org.assertj.core.api.Assertions.assertThat;

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
      assertThat(addExtension("file")).isEqualTo("file.tsvg");
    }

    @Test
    void shouldAddExtensionWhenExtensionPresent() {
      assertThat(addExtension("file.tsvg")).isEqualTo("file.tsvg.tsvg");
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
        assertThat(addExtensionIfAbsent("file")).isEqualTo("file.tsvg");
      }

      @Test
      void shouldNotAddExtensionWhenSameCasedExtensionPresent() {
        assertThat(addExtensionIfAbsent("file.tsvg")).isEqualTo("file.tsvg");
      }

      @Test
      void shouldAddExtensionWhenDifferentCasedExtensionPresent() {
        assertThat(addExtensionIfAbsent("file.TSVG")).isEqualTo("file.TSVG.tsvg");
      }
    }

    @Nested
    final class WhenFileSystemIsCaseInsensitiveTest {
      private String addExtensionIfAbsent(final String fileName) {
        return GameDataFileUtils.addExtensionIfAbsent(fileName, IOCase.INSENSITIVE);
      }

      @Test
      void shouldAddExtensionWhenExtensionAbsent() {
        assertThat(addExtensionIfAbsent("file")).isEqualTo("file.tsvg");
      }

      @Test
      void shouldNotAddExtensionWhenSameCasedExtensionPresent() {
        assertThat(addExtensionIfAbsent("file.tsvg")).isEqualTo("file.tsvg");
      }

      @Test
      void shouldNotAddExtensionWhenDifferentCasedExtensionPresent() {
        assertThat(addExtensionIfAbsent("file.TSVG")).isEqualTo("file.TSVG");
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
        assertThat(isCandidateFileName("file")).isFalse();
      }

      @Test
      void shouldReturnTrueWhenSameCasedPrimaryExtensionPresent() {
        assertThat(isCandidateFileName("file.tsvg")).isTrue();
      }

      @Test
      void shouldReturnFalseWhenDifferentCasedPrimaryExtensionPresent() {
        assertThat(isCandidateFileName("file.TSVG")).isFalse();
      }

      @Test
      void shouldReturnTrueWhenSameCasedLegacyExtensionPresent() {
        assertThat(isCandidateFileName("file.svg")).isTrue();
      }

      @Test
      void shouldReturnFalseWhenDifferentCasedLegacyExtensionPresent() {
        assertThat(isCandidateFileName("file.SVG")).isFalse();
      }

      @Test
      void shouldReturnTrueWhenSameCasedMacOsAlternativeExtensionPresent() {
        assertThat(isCandidateFileName("filetsvg.gz")).isTrue();
      }

      @Test
      void shouldReturnFalseWhenDifferentCasedMacOsAlternativeExtensionPresent() {
        assertThat(isCandidateFileName("fileTSVG.GZ")).isFalse();
      }
    }

    @Nested
    final class WhenFileSystemIsCaseInsensitiveTest {
      private boolean isCandidateFileName(final String fileName) {
        return GameDataFileUtils.isCandidateFileName(fileName, IOCase.INSENSITIVE);
      }

      @Test
      void shouldReturnFalseWhenExtensionAbsent() {
        assertThat(isCandidateFileName("file")).isFalse();
      }

      @Test
      void shouldReturnTrueWhenSameCasedPrimaryExtensionPresent() {
        assertThat(isCandidateFileName("file.tsvg")).isTrue();
      }

      @Test
      void shouldReturnTrueWhenDifferentCasedPrimaryExtensionPresent() {
        assertThat(isCandidateFileName("file.TSVG")).isTrue();
      }

      @Test
      void shouldReturnTrueWhenSameCasedLegacyExtensionPresent() {
        assertThat(isCandidateFileName("file.svg")).isTrue();
      }

      @Test
      void shouldReturnTrueWhenDifferentCasedLegacyExtensionPresent() {
        assertThat(isCandidateFileName("file.SVG")).isTrue();
      }

      @Test
      void shouldReturnTrueWhenSameCasedMacOsAlternativeExtensionPresent() {
        assertThat(isCandidateFileName("filetsvg.gz")).isTrue();
      }

      @Test
      void shouldReturnTrueWhenDifferentCasedMacOsAlternativeExtensionPresent() {
        assertThat(isCandidateFileName("fileTSVG.GZ")).isTrue();
      }
    }
  }
}
