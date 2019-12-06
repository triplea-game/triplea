package org.triplea.server.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

import java.net.URI;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import org.hamcrest.collection.IsEmptyCollection;

/** Test to verify endpoints that are publicly accessible and do not require an API key. */
public abstract class BasicEndpointTest<T> extends DropwizardTest {
  private final Function<URI, T> clientBuilder;

  protected BasicEndpointTest(final Function<URI, T> clientBuilder) {
    this.clientBuilder = clientBuilder;
  }

  /** Use this to verify an endpoint that returns void. */
  protected void verifyEndpointReturningVoid(final Consumer<T> methodRunner) {
    methodRunner.accept(clientBuilder.apply(localhost));
  }

  /** Use this to verify an endpoint that returns data. */
  protected <X> X verifyEndpointReturningObject(final Function<T, X> methodRunner) {
    final X result = methodRunner.apply(clientBuilder.apply(localhost));
    assertThat(result, notNullValue());
    return result;
  }

  /**
   * Use this method to verify an endpoint that returns a collection of data. For better test
   * coverage, it is assumed that the DB will be set up so that some data will be returned, we
   * expect a non-empty collection to be returned.
   */
  protected void verifyEndpointReturningCollection(final Function<T, Collection<?>> methodRunner) {
    assertThat(methodRunner.apply(clientBuilder.apply(localhost)), IsEmptyCollection.empty());
  }
}
