package org.triplea.lobby.server.db.dao.api.key;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.PlayerName;
import org.triplea.lobby.server.db.dao.UserJdbiDao;
import org.triplea.lobby.server.db.dao.UserRoleDao;
import org.triplea.lobby.server.db.data.ApiKeyUserData;
import org.triplea.lobby.server.db.data.UserRoleLookup;

@ExtendWith(MockitoExtension.class)
class ApiKeyDaoWrapperTest {

  private static final ApiKey API_KEY = ApiKey.of("api-key");
  private static final String HASHED_KEY = "Dead, rainy shores proud swashbuckler";
  private static final PlayerName PLAYER_NAME = PlayerName.of("The_captain");

  private static final int USER_ID = 5;

  private static final int ANONYMOUS_USER_ROLE_ID = 10;
  private static final int HOST_USER_ROLE_ID = 20;
  private static final int REGISTERED_USER_ROLE_ID = 30;

  private static final InetAddress IP;

  static {
    try {
      IP = InetAddress.getLocalHost();
    } catch (final UnknownHostException e) {
      throw new IllegalStateException(e);
    }
  }

  @Mock private ApiKeyDao apiKeyDao;
  @Mock private UserJdbiDao userJdbiDao;
  @Mock private UserRoleDao userRoleDao;
  @Mock private Supplier<ApiKey> keyMaker;
  @Mock private Function<ApiKey, String> keyHashingFunction;

  @InjectMocks private ApiKeyDaoWrapper wrapper;

  @Mock private ApiKeyUserData apiKeyUserData;

  @Nested
  class LookupByApiKey {
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void foundCase() {
      givenKeyLookupResult(Optional.of(apiKeyUserData));

      final Optional<ApiKeyUserData> result = wrapper.lookupByApiKey(API_KEY);

      assertThat(result, isPresent());
      assertThat(result.get(), sameInstance(apiKeyUserData));
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void givenKeyLookupResult(final Optional<ApiKeyUserData> dataResult) {
      when(keyHashingFunction.apply(API_KEY)).thenReturn(HASHED_KEY);
      when(apiKeyDao.lookupByApiKey(HASHED_KEY)).thenReturn(dataResult);
    }

    @Test
    void notFoundCase() {
      givenKeyLookupResult(Optional.empty());

      final Optional<ApiKeyUserData> result = wrapper.lookupByApiKey(API_KEY);

      assertThat(result, isEmpty());
    }
  }

  @Nested
  class NewKey {

    @Test
    void newKeyForGameHosts() {
      givenNewKey();
      when(userRoleDao.lookupHostRoleId()).thenReturn(HOST_USER_ROLE_ID);
      when(apiKeyDao.storeKey(null, null, HASHED_KEY, IP.getHostAddress(), HOST_USER_ROLE_ID))
          .thenReturn(1);

      final ApiKey result = wrapper.newKey(IP);

      assertThat(result, sameInstance(API_KEY));
    }

    void givenNewKey() {
      when(keyMaker.get()).thenReturn(API_KEY);
      when(keyHashingFunction.apply(API_KEY)).thenReturn(HASHED_KEY);
    }

    @Test
    void newKeyForAnonymousUsers() {
      givenNewKey();
      when(userJdbiDao.lookupUserIdAndRoleIdByUserName(PLAYER_NAME.getValue()))
          .thenReturn(Optional.empty());
      when(userRoleDao.lookupAnonymousRoleId()).thenReturn(ANONYMOUS_USER_ROLE_ID);
      when(apiKeyDao.storeKey(
              null,
              PLAYER_NAME.getValue(),
              HASHED_KEY,
              IP.getHostAddress(),
              ANONYMOUS_USER_ROLE_ID))
          .thenReturn(1);

      final ApiKey result = wrapper.newKey(PLAYER_NAME, IP);

      assertThat(result, sameInstance(API_KEY));
    }

    @Test
    void newKeyForRegisteredUser() {
      givenNewKey();
      when(userJdbiDao.lookupUserIdAndRoleIdByUserName(PLAYER_NAME.getValue()))
          .thenReturn(
              Optional.of(
                  UserRoleLookup.builder()
                      .userId(USER_ID)
                      .userRoleId(REGISTERED_USER_ROLE_ID)
                      .build()));
      when(apiKeyDao.storeKey(
              USER_ID,
              PLAYER_NAME.getValue(),
              HASHED_KEY,
              IP.getHostAddress(),
              REGISTERED_USER_ROLE_ID))
          .thenReturn(1);

      final ApiKey result = wrapper.newKey(PLAYER_NAME, IP);

      assertThat(result, sameInstance(API_KEY));
    }
  }
}
