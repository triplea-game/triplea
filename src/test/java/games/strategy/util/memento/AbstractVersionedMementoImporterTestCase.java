package games.strategy.util.memento;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;

/**
 * A fixture for testing versioned memento importers to ensure they can successfully import all supported memento
 * versions into the current originator.
 *
 * @param <T> The type of the memento originator.
 */
public abstract class AbstractVersionedMementoImporterTestCase<T> {
  protected AbstractVersionedMementoImporterTestCase() {}

  /**
   * Asserts that the specified originators are equal.
   *
   * <p>
   * This implementation compares the two objects using the {@code equals} method.
   * </p>
   *
   * @param expected The expected originator; must not be {@code null}.
   * @param actual The actual originator; must not be {@code null}.
   *
   * @throws AssertionError If the two originators are not equal.
   */
  protected void assertOriginatorEquals(final T expected, final T actual) {
    checkNotNull(expected);
    checkNotNull(actual);

    assertThat(actual, is(expected));
  }

  /**
   * Creates the memento importer to be tested.
   *
   * @return The memento importer to be tested; never {@code null}.
   *
   * @throws Exception If the memento importer cannot be created.
   */
  protected abstract MementoImporter<T> createMementoImporter() throws Exception;

  /**
   * Returns a collection of supported memento versions that will be tested to ensure that they can be imported into the
   * current originator.
   *
   * @return A collection of supported memento versions; never {@code null}.
   */
  protected abstract List<SupportedVersion<T>> getSupportedVersions();

  @Test
  public void shouldBeAbleToImportAllSupportedVersions() throws Exception {
    final MementoImporter<T> mementoImporter = createMementoImporter();

    for (final SupportedVersion<T> supportedVersion : getSupportedVersions()) {
      final T actual = mementoImporter.importMemento(supportedVersion.memento);

      assertThat(actual, is(not(nullValue())));
      assertOriginatorEquals(supportedVersion.expected, actual);
    }
  }

  /**
   * Information about a single memento version used by the test fixture to ensure that the memento importer under test
   * can import it into the current originator.
   *
   * @param <T> The type of the memento originator.
   */
  protected static final class SupportedVersion<T> {
    /**
     * The expected imported originator; never {@code null}.
     */
    public final T expected;

    /**
     * The memento to be imported; never {@code null}.
     */
    public final Memento memento;

    /**
     * Initializes a new instance of the {@code SupportedVersion} class.
     *
     * @param expected The expected imported originator; must not be {@code null}.
     * @param memento The memento to be imported; must not be {@code null}.
     */
    public SupportedVersion(final T expected, final Memento memento) {
      checkNotNull(expected);
      checkNotNull(memento);

      this.expected = expected;
      this.memento = memento;
    }
  }
}
