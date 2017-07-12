package games.strategy.engine.framework.startup.login;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Map;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

@RunWith(Enclosed.class)
public final class ClientLoginTests {
  private static final String PASSWORD = "password";

  public static final class AddAuthenticationResponsePropertiesTest {
    @Test
    public void shouldIncludeV1ResponseProperties() {
      final Map<String, String> challenge = V1Authenticator.newChallenge();
      final Map<String, String> response = Maps.newHashMap();

      ClientLogin.addAuthenticationResponseProperties(PASSWORD, challenge, response);

      assertThat(V1Authenticator.canProcessResponse(response), is(true));
    }

    @Test
    public void shouldCreateResponseProcessableByV2AuthenticatorWhenV2ChallengePresent() throws Exception {
      final Map<String, String> challenge = Maps.newHashMap(ImmutableMap.<String, String>builder()
          .putAll(V1Authenticator.newChallenge())
          .putAll(V2Authenticator.newChallenge())
          .build());
      final Map<String, String> response = Maps.newHashMap();

      ClientLogin.addAuthenticationResponseProperties(PASSWORD, challenge, response);

      assertThat(V2Authenticator.canProcessResponse(response), is(true));
    }

    @Test
    public void shouldCreateResponseIgnoredByV2AuthenticatorWhenV2ChallengeAbsent() {
      final Map<String, String> challenge = V1Authenticator.newChallenge();
      final Map<String, String> response = Maps.newHashMap();

      ClientLogin.addAuthenticationResponseProperties(PASSWORD, challenge, response);

      assertThat(V2Authenticator.canProcessResponse(response), is(false));
    }
  }
}
