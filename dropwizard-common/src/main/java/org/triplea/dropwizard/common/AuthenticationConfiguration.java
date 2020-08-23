package org.triplea.dropwizard.common;

import com.codahale.metrics.MetricRegistry;
import com.google.common.cache.CacheBuilder;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.Authorizer;
import io.dropwizard.auth.CachingAuthenticator;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.setup.Environment;
import java.security.Principal;
import java.time.Duration;
import lombok.experimental.UtilityClass;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.triplea.http.client.AuthenticationHeaders;

@UtilityClass
public class AuthenticationConfiguration {

  public static <UserT extends Principal> void enableAuthentication(
      final Environment environment,
      final MetricRegistry metrics,
      final Authenticator<String, UserT> authenticator,
      final Authorizer<UserT> authorizer,
      final Class<UserT> principalClass) {
    environment
        .jersey()
        .register(
            new AuthDynamicFeature(
                new OAuthCredentialAuthFilter.Builder<UserT>()
                    .setAuthenticator(
                        new CachingAuthenticator<>(
                            metrics,
                            authenticator,
                            CacheBuilder.newBuilder()
                                .expireAfterAccess(Duration.ofMinutes(10))
                                .maximumSize(10000)))
                    .setAuthorizer(authorizer)
                    .setPrefix(AuthenticationHeaders.KEY_BEARER_PREFIX)
                    .buildAuthFilter()));
    environment.jersey().register(new AuthValueFactoryProvider.Binder<>(principalClass));
    environment.jersey().register(new RolesAllowedDynamicFeature());
  }
}
