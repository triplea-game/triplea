package games.strategy.triplea.ui;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.triplea.ResourceLoader;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PropertyFileTest {
  private static final String RESOURCE = "resource";
  private final ResourceLoader mock = mock(ResourceLoader.class);

  @BeforeEach
  void setup() {
    PropertyFile.cache.invalidateAll();
  }

  @Test
  void testConstructor() {
    final Properties properties = new Properties();
    when(mock.loadAsResource(RESOURCE)).thenReturn(properties);
    final PropertyFile instance = new PropertyFile(RESOURCE, mock) {};
    assertSame(properties, instance.properties);
  }

  @Test
  void testCaching() {
    final DummyPropertyFile dummy = new DummyPropertyFile(RESOURCE, mock);
    assertSame(dummy, PropertyFile.getInstance(DummyPropertyFile.class, () -> dummy));
    assertSame(
        dummy,
        PropertyFile.getInstance(
            DummyPropertyFile.class, () -> new DummyPropertyFile(RESOURCE, mock)));
    DummyPropertyFile.cache.invalidateAll();
    final DummyPropertyFile dummy2 = new DummyPropertyFile(RESOURCE, mock);
    assertSame(dummy2, PropertyFile.getInstance(DummyPropertyFile.class, () -> dummy2));
  }

  private static class DummyPropertyFile extends PropertyFile {
    DummyPropertyFile(final String path, final ResourceLoader loader) {
      super(path, loader);
    }
  }
}
