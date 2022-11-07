package org.triplea.config;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Optional;
import javax.annotation.concurrent.Immutable;

/**
 * Implementation of {@link PropertyReader} that uses a properties file from the classpath as the
 * property source. The current thread's context class loader at the time of the property read
 * request is used to locate the property source.
 *
 * <p>This implementation reads the properties file on each request. Thus, it will reflect real-time
 * changes made to the properties file outside of the virtual machine.
 */
@Immutable
public final class ResourcePropertyReader extends AbstractInputStreamPropertyReader {
  private final String resourceName;

  /**
   * Creates a property reader using the properties file with the specified resource name as the
   * source.
   *
   * @param resourceName The fully-qualified path to the resource on the classpath (e.g. {@code
   *     tld/package/subpackage/resource.ext}).
   */
  public ResourcePropertyReader(final String resourceName) {
    super("resource(" + checkNotNull(resourceName) + ")");

    this.resourceName = resourceName;
  }

  @Override
  protected InputStream newInputStream() throws FileNotFoundException {
    return Optional.ofNullable(
            Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName))
        .orElseThrow(() -> new FileNotFoundException("Resource not found: " + resourceName));
  }
}
