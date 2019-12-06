package org.triplea.server.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.base.Preconditions;
import java.net.URI;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.HttpInteractionException;

/**
 * Test to verify endpoints that are protected behind authorization/authentication. For each verify
 * method, we'll verify the endpoint can only be used by authorized roles and is denied for all
 * other roles.
 */
// TODO: rename to AuthenticatedEndpointTest
public abstract class ProtectedEndpointTest<T> extends DropwizardTest {
  private final BiFunction<URI, ApiKey, T> clientBuilder;
  @Nullable private final AllowedUserRole defaultAllowedUserRole;

  /** Constructor where allowed user role needs to specified for each endpoint test. */
  protected ProtectedEndpointTest(final BiFunction<URI, ApiKey, T> clientBuilder) {
    this(null, clientBuilder);
  }

  /**
   * Constructor where a default user role is provided for each endpoint test. A role specified
   * specific in an endpoint test will override the default.
   */
  protected ProtectedEndpointTest(
      @Nullable final AllowedUserRole allowedUserRole,
      final BiFunction<URI, ApiKey, T> clientBuilder) {
    this.defaultAllowedUserRole = allowedUserRole;
    this.clientBuilder = clientBuilder;
  }

  /** Use this to verify an endpoint that returns void. */
  protected void verifyEndpoint(final Consumer<T> methodRunner) {
    Preconditions.checkState(defaultAllowedUserRole != null);
    verifyEndpoint(defaultAllowedUserRole, methodRunner);
  }

  protected void verifyEndpoint(
      final AllowedUserRole allowedUserRole, final Consumer<T> methodRunner) {
    Preconditions.checkState(
        defaultAllowedUserRole != null,
        "Default allowed role must be set, or use method overload that specifies an allowed role");

    methodRunner.accept(clientBuilder.apply(localhost, allowedUserRole.getAllowedKey()));

    allowedUserRole
        .getDisallowedKeys()
        .forEach(
            notAllowedKey ->
                assertThrows(
                    HttpInteractionException.class,
                    () -> methodRunner.accept(clientBuilder.apply(localhost, notAllowedKey)),
                    error(notAllowedKey)));
  }

  private static String error(final ApiKey key) {
    return "Role permissions problem, key was allowed access:  " + key;
  }

  protected <X> X verifyEndpointReturningObject(final Function<T, X> methodRunner) {
    Preconditions.checkState(
        defaultAllowedUserRole != null,
        "Default allowed role must be set, or use method overload that specifies an allowed role");
    return verifyEndpointReturningObject(defaultAllowedUserRole, methodRunner);
  }

  /**
   * Verifies an endpoint that returns a result or an object.
   *
   * @param allowedUserRole Role that is allowed access to the endpoint.
   * @param methodRunner Function to invoke the endpoint.
   */
  protected <X> X verifyEndpointReturningObject(
      final AllowedUserRole allowedUserRole, final Function<T, X> methodRunner) {
    Preconditions.checkNotNull(allowedUserRole);

    final X value =
        methodRunner.apply(clientBuilder.apply(localhost, allowedUserRole.getAllowedKey()));
    assertThat(value, is(notNullValue()));

    allowedUserRole
        .getDisallowedKeys()
        .forEach(
            notAllowedKey ->
                assertThrows(
                    HttpInteractionException.class,
                    () -> methodRunner.apply(clientBuilder.apply(localhost, notAllowedKey))));
    return value;
  }

  protected void verifyEndpointReturningCollection(final Function<T, Collection<?>> methodRunner) {
    Preconditions.checkState(
        defaultAllowedUserRole != null,
        "Default allowed role must be set, or use method overload that specifies an allowed role");
    verifyEndpointReturningCollection(defaultAllowedUserRole, methodRunner);
  }

  /**
   * Use this method to verify an endpoint that returns a collection of data. For better test
   * coverage, it is assumed that the DB will be set up so that some data will be returned, we
   * expect a non-empty collection to be returned.
   */
  protected void verifyEndpointReturningCollection(
      final AllowedUserRole allowedUserRole, final Function<T, Collection<?>> methodRunner) {
    Preconditions.checkNotNull(allowedUserRole);

    assertThat(
        methodRunner.apply(clientBuilder.apply(localhost, allowedUserRole.getAllowedKey())),
        not(empty()));

    allowedUserRole
        .getDisallowedKeys()
        .forEach(
            notAllowedKey ->
                assertThrows(
                    HttpInteractionException.class,
                    () -> methodRunner.apply(clientBuilder.apply(localhost, notAllowedKey)),
                    error(notAllowedKey)));
  }
}
