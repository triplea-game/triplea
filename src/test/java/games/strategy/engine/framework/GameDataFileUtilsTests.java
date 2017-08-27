package games.strategy.engine.framework;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.apache.commons.io.IOCase;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import games.strategy.engine.framework.GameDataFileUtils.SaveGameFormat;

@RunWith(Enclosed.class)
public final class GameDataFileUtilsTests {
  @RunWith(Enclosed.class)
  public static final class AddExtensionTests {
    public static final class WhenSaveGameFormatIsSerializationTest {
      private static String addExtension(final String fileName) {
        return GameDataFileUtils.addExtension(fileName, SaveGameFormat.SERIALIZATION);
      }

      @Test
      public void shouldAddExtensionWhenExtensionAbsent() {
        assertThat(addExtension("file"), is("file.tsvg"));
      }

      @Test
      public void shouldAddExtensionWhenExtensionPresent() {
        assertThat(addExtension("file.tsvg"), is("file.tsvg.tsvg"));
      }
    }

    public static final class WhenSaveGameFormatIsProxySerializationTest {
      private static String addExtension(final String fileName) {
        return GameDataFileUtils.addExtension(fileName, SaveGameFormat.PROXY_SERIALIZATION);
      }

      @Test
      public void shouldAddExtensionWhenExtensionAbsent() {
        assertThat(addExtension("file"), is("file.tsvgx"));
      }

      @Test
      public void shouldAddExtensionWhenExtensionPresent() {
        assertThat(addExtension("file.tsvgx"), is("file.tsvgx.tsvgx"));
      }
    }
  }

  @RunWith(Enclosed.class)
  public static final class AddExtensionIfAbsentTests {
    @RunWith(Enclosed.class)
    public static final class WhenSaveGameFormatIsSerializationTests {
      public static final class WhenFileSystemIsCaseSensitiveTest {
        private static String addExtensionIfAbsent(final String fileName) {
          return GameDataFileUtils.addExtensionIfAbsent(fileName, SaveGameFormat.SERIALIZATION, IOCase.SENSITIVE);
        }

        @Test
        public void shouldAddExtensionWhenExtensionAbsent() {
          assertThat(addExtensionIfAbsent("file"), is("file.tsvg"));
        }

        @Test
        public void shouldNotAddExtensionWhenSameCasedExtensionPresent() {
          assertThat(addExtensionIfAbsent("file.tsvg"), is("file.tsvg"));
        }

        @Test
        public void shouldAddExtensionWhenDifferentCasedExtensionPresent() {
          assertThat(addExtensionIfAbsent("file.TSVG"), is("file.TSVG.tsvg"));
        }
      }

      public static final class WhenFileSystemIsCaseInsensitiveTest {
        private static String addExtensionIfAbsent(final String fileName) {
          return GameDataFileUtils.addExtensionIfAbsent(fileName, SaveGameFormat.SERIALIZATION, IOCase.INSENSITIVE);
        }

        @Test
        public void shouldAddExtensionWhenExtensionAbsent() {
          assertThat(addExtensionIfAbsent("file"), is("file.tsvg"));
        }

        @Test
        public void shouldNotAddExtensionWhenSameCasedExtensionPresent() {
          assertThat(addExtensionIfAbsent("file.tsvg"), is("file.tsvg"));
        }

        @Test
        public void shouldNotAddExtensionWhenDifferentCasedExtensionPresent() {
          assertThat(addExtensionIfAbsent("file.TSVG"), is("file.TSVG"));
        }
      }
    }

    @RunWith(Enclosed.class)
    public static final class WhenSaveGameFormatIsProxySerializationTests {
      public static final class WhenFileSystemIsCaseSensitiveTest {
        private static String addExtensionIfAbsent(final String fileName) {
          return GameDataFileUtils.addExtensionIfAbsent(fileName, SaveGameFormat.PROXY_SERIALIZATION, IOCase.SENSITIVE);
        }

        @Test
        public void shouldAddExtensionWhenExtensionAbsent() {
          assertThat(addExtensionIfAbsent("file"), is("file.tsvgx"));
        }

        @Test
        public void shouldNotAddExtensionWhenSameCasedExtensionPresent() {
          assertThat(addExtensionIfAbsent("file.tsvgx"), is("file.tsvgx"));
        }

        @Test
        public void shouldAddExtensionWhenDifferentCasedExtensionPresent() {
          assertThat(addExtensionIfAbsent("file.TSVGX"), is("file.TSVGX.tsvgx"));
        }
      }

      public static final class WhenFileSystemIsCaseInsensitiveTest {
        private static String addExtensionIfAbsent(final String fileName) {
          return GameDataFileUtils.addExtensionIfAbsent(
              fileName,
              SaveGameFormat.PROXY_SERIALIZATION,
              IOCase.INSENSITIVE);
        }

        @Test
        public void shouldAddExtensionWhenExtensionAbsent() {
          assertThat(addExtensionIfAbsent("file"), is("file.tsvgx"));
        }

        @Test
        public void shouldNotAddExtensionWhenSameCasedExtensionPresent() {
          assertThat(addExtensionIfAbsent("file.tsvgx"), is("file.tsvgx"));
        }

        @Test
        public void shouldNotAddExtensionWhenDifferentCasedExtensionPresent() {
          assertThat(addExtensionIfAbsent("file.TSVGX"), is("file.TSVGX"));
        }
      }
    }
  }

  @RunWith(Enclosed.class)
  public static final class IsCandidateFileNameTests {
    @RunWith(Enclosed.class)
    public static final class WhenSaveGameFormatIsSerializationTests {
      public static final class WhenFileSystemIsCaseSensitiveTest {
        private static boolean isCandidateFileName(final String fileName) {
          return GameDataFileUtils.isCandidateFileName(fileName, SaveGameFormat.SERIALIZATION, IOCase.SENSITIVE);
        }

        @Test
        public void shouldReturnFalseWhenExtensionAbsent() {
          assertThat(isCandidateFileName("file"), is(false));
        }

        @Test
        public void shouldReturnTrueWhenSameCasedPrimaryExtensionPresent() {
          assertThat(isCandidateFileName("file.tsvg"), is(true));
        }

        @Test
        public void shouldReturnFalseWhenDifferentCasedPrimaryExtensionPresent() {
          assertThat(isCandidateFileName("file.TSVG"), is(false));
        }

        @Test
        public void shouldReturnTrueWhenSameCasedLegacyExtensionPresent() {
          assertThat(isCandidateFileName("file.svg"), is(true));
        }

        @Test
        public void shouldReturnFalseWhenDifferentCasedLegacyExtensionPresent() {
          assertThat(isCandidateFileName("file.SVG"), is(false));
        }

        @Test
        public void shouldReturnTrueWhenSameCasedMacOsAlternativeExtensionPresent() {
          assertThat(isCandidateFileName("filetsvg.gz"), is(true));
        }

        @Test
        public void shouldReturnFalseWhenDifferentCasedMacOsAlternativeExtensionPresent() {
          assertThat(isCandidateFileName("fileTSVG.GZ"), is(false));
        }
      }

      public static final class WhenFileSystemIsCaseInsensitiveTest {
        private static boolean isCandidateFileName(final String fileName) {
          return GameDataFileUtils.isCandidateFileName(fileName, SaveGameFormat.SERIALIZATION, IOCase.INSENSITIVE);
        }

        @Test
        public void shouldReturnFalseWhenExtensionAbsent() {
          assertThat(isCandidateFileName("file"), is(false));
        }

        @Test
        public void shouldReturnTrueWhenSameCasedPrimaryExtensionPresent() {
          assertThat(isCandidateFileName("file.tsvg"), is(true));
        }

        @Test
        public void shouldReturnTrueWhenDifferentCasedPrimaryExtensionPresent() {
          assertThat(isCandidateFileName("file.TSVG"), is(true));
        }

        @Test
        public void shouldReturnTrueWhenSameCasedLegacyExtensionPresent() {
          assertThat(isCandidateFileName("file.svg"), is(true));
        }

        @Test
        public void shouldReturnTrueWhenDifferentCasedLegacyExtensionPresent() {
          assertThat(isCandidateFileName("file.SVG"), is(true));
        }

        @Test
        public void shouldReturnTrueWhenSameCasedMacOsAlternativeExtensionPresent() {
          assertThat(isCandidateFileName("filetsvg.gz"), is(true));
        }

        @Test
        public void shouldReturnTrueWhenDifferentCasedMacOsAlternativeExtensionPresent() {
          assertThat(isCandidateFileName("fileTSVG.GZ"), is(true));
        }
      }
    }

    @RunWith(Enclosed.class)
    public static final class WhenSaveGameFormatIsProxySerializationTests {
      public static final class WhenFileSystemIsCaseSensitiveTest {
        private static boolean isCandidateFileName(final String fileName) {
          return GameDataFileUtils.isCandidateFileName(fileName, SaveGameFormat.PROXY_SERIALIZATION, IOCase.SENSITIVE);
        }

        @Test
        public void shouldReturnFalseWhenExtensionAbsent() {
          assertThat(isCandidateFileName("file"), is(false));
        }

        @Test
        public void shouldReturnTrueWhenSameCasedExtensionPresent() {
          assertThat(isCandidateFileName("file.tsvgx"), is(true));
        }

        @Test
        public void shouldReturnFalseWhenDifferentCasedExtensionPresent() {
          assertThat(isCandidateFileName("file.TSVGX"), is(false));
        }
      }

      public static final class WhenFileSystemIsCaseInsensitiveTest {
        private static boolean isCandidateFileName(final String fileName) {
          return GameDataFileUtils.isCandidateFileName(
              fileName,
              SaveGameFormat.PROXY_SERIALIZATION,
              IOCase.INSENSITIVE);
        }

        @Test
        public void shouldReturnFalseWhenExtensionAbsent() {
          assertThat(isCandidateFileName("file"), is(false));
        }

        @Test
        public void shouldReturnTrueWhenSameCasedExtensionPresent() {
          assertThat(isCandidateFileName("file.tsvgx"), is(true));
        }

        @Test
        public void shouldReturnTrueWhenDifferentCasedExtensionPresent() {
          assertThat(isCandidateFileName("file.TSVGX"), is(true));
        }
      }
    }
  }
}
