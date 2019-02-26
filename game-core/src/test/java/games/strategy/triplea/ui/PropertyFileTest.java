package games.strategy.triplea.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.TempDirectory;
import org.junitpioneer.jupiter.TempDirectory.TempDir;

import games.strategy.triplea.ResourceLoader;

@ExtendWith(TempDirectory.class)
public class PropertyFileTest {
  private final ResourceLoader mock = mock(ResourceLoader.class);
  private File file;

  @BeforeEach
  public void setup(@TempDir final Path tempDirPath) throws Exception {
    file = Files.createTempFile(tempDirPath, null, null).toFile();
    when(mock.getResource(file.getAbsolutePath())).thenReturn(file.toURI().toURL());
    PropertyFile.cache.invalidateAll();
  }

  @Test
  public void testConstructor() throws Exception {
    Files.write(file.toPath(), Arrays.asList("abc=def", "123: 456"));
    final PropertyFile instance = new PropertyFile(file.getAbsolutePath(), mock) {};
    assertEquals("def", instance.properties.getProperty("abc"));
    assertEquals("456", instance.properties.getProperty("123"));
    assertEquals(2, instance.properties.size());
  }

  @Test
  public void testCaching() {
    final DummyPropertyFile dummy = new DummyPropertyFile(file.getAbsolutePath(), mock);
    assertSame(dummy, PropertyFile.getInstance(DummyPropertyFile.class, () -> dummy));
    assertSame(dummy, PropertyFile.getInstance(DummyPropertyFile.class,
        () -> new DummyPropertyFile(file.getAbsolutePath(), mock)));
    DummyPropertyFile.cache.invalidateAll();
    final DummyPropertyFile dummy2 = new DummyPropertyFile(file.getAbsolutePath(), mock);
    assertSame(dummy2, PropertyFile.getInstance(DummyPropertyFile.class, () -> dummy2));
  }

  private static class DummyPropertyFile extends PropertyFile {
    DummyPropertyFile(final String path, final ResourceLoader loader) {
      super(path, loader);
    }
  }
}
