package games.strategy.engine.config;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Implementation of {@link PropertyReader} that uses a memory store as the property source.
 */
@Immutable
public final class MemoryPropertyReader extends AbstractPropertyReader {
  private final Map<String, String> properties;

  public MemoryPropertyReader(final Map<String, String> properties) {
    checkNotNull(properties);

    this.properties = new HashMap<>(properties);
  }

  @Override
  protected @Nullable String readPropertyInternal(final String key) {
    return properties.get(key);
  }
}
