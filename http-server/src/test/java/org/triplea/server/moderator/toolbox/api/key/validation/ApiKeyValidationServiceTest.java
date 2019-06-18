package org.triplea.server.moderator.toolbox.api.key.validation;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.BiFunction;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.moderator.toolbox.ToolboxHttpHeaders;
import org.triplea.lobby.server.db.dao.ModeratorApiKeyDao;
import org.triplea.server.moderator.toolbox.api.key.InvalidKeyLockOut;
import org.triplea.server.moderator.toolbox.api.key.exception.ApiKeyLockOutException;
import org.triplea.server.moderator.toolbox.api.key.exception.IncorrectApiKeyException;


@ExtendWith(MockitoExtension.class)
class ApiKeyValidationServiceTest {

  private static final String API_KEY = "The cloud screams faith like a fine tuna.";
  private static final String API_KEY_PASSWORD = "Life ho! blow to be desired.";
  private static final String HASHED_KEY = "Aye, haul me cockroach, ye fine jolly roger!";
  private static final int MODERATOR_ID = 77;
  private static final String REMOTE_IP = "Pegleg of a heavy-hearted faith, ransack the urchin!";

  @Mock
  private ValidKeyCache validKeyCache;
  @Mock
  private InvalidKeyLockOut invalidKeyLockOut;
  @Mock
  private BiFunction<String, String, String> keyHasher;
  @Mock
  private ModeratorApiKeyDao moderatorApiKeyDao;

  @InjectMocks
  private ApiKeyValidationService apiKeyValidationService;

  @Mock
  private HttpServletRequest httpServletRequest;

  @Test
  void missingKeyThrows() {
    when(httpServletRequest.getHeader(ToolboxHttpHeaders.API_KEY_HEADER)).thenReturn(null);
    assertThrows(IllegalArgumentException.class, () -> apiKeyValidationService.verifyApiKey(httpServletRequest));
  }

  @Test
  void missingKeyPasswordThrows() {
    when(httpServletRequest.getHeader(ToolboxHttpHeaders.API_KEY_HEADER)).thenReturn(API_KEY);
    when(httpServletRequest.getHeader(ToolboxHttpHeaders.API_KEY_PASSWORD_HEADER)).thenReturn(null);
    assertThrows(IllegalArgumentException.class, () -> apiKeyValidationService.verifyApiKey(httpServletRequest));
  }

  @Test
  void emptyKeyThrows() {
    when(httpServletRequest.getHeader(ToolboxHttpHeaders.API_KEY_HEADER)).thenReturn("");
    assertThrows(IllegalArgumentException.class, () -> apiKeyValidationService.verifyApiKey(httpServletRequest));
  }

  @Test
  void emptyKeyPasswordThrows() {
    when(httpServletRequest.getHeader(ToolboxHttpHeaders.API_KEY_HEADER)).thenReturn(API_KEY);
    when(httpServletRequest.getHeader(ToolboxHttpHeaders.API_KEY_PASSWORD_HEADER)).thenReturn("");
    assertThrows(IllegalArgumentException.class, () -> apiKeyValidationService.verifyApiKey(httpServletRequest));
  }


  @Test
  void cachedValuesReturnedImmediately() {
    givenKeyAndPasswordIsInHeader();
    when(keyHasher.apply(API_KEY, API_KEY_PASSWORD)).thenReturn(HASHED_KEY);
    when(validKeyCache.get(HASHED_KEY)).thenReturn(Optional.of(MODERATOR_ID));

    assertThat(apiKeyValidationService.lookupModeratorIdByApiKey(httpServletRequest), is(MODERATOR_ID));

    verify(moderatorApiKeyDao, never()).lookupModeratorIdByApiKey(any());
    verify(invalidKeyLockOut, never()).isLockedOut(any());
  }

  private void givenKeyAndPasswordIsInHeader() {
    when(httpServletRequest.getHeader(ToolboxHttpHeaders.API_KEY_HEADER)).thenReturn(API_KEY);
    when(httpServletRequest.getHeader(ToolboxHttpHeaders.API_KEY_PASSWORD_HEADER)).thenReturn(API_KEY_PASSWORD);
  }

  @Test
  void lockoutDoesNotAttemptKeyLookup() {
    givenKeyAndPasswordIsInHeaderWithCacheMiss();
    when(invalidKeyLockOut.isLockedOut(httpServletRequest)).thenReturn(true);

    assertThrows(
        ApiKeyLockOutException.class,
        () -> apiKeyValidationService.lookupModeratorIdByApiKey(httpServletRequest));

    verify(moderatorApiKeyDao, never()).lookupModeratorIdByApiKey(any());
  }

  private void givenKeyAndPasswordIsInHeaderWithCacheMiss() {
    when(httpServletRequest.getHeader(ToolboxHttpHeaders.API_KEY_HEADER)).thenReturn(API_KEY);
    when(httpServletRequest.getHeader(ToolboxHttpHeaders.API_KEY_PASSWORD_HEADER)).thenReturn(API_KEY_PASSWORD);
    when(keyHasher.apply(API_KEY, API_KEY_PASSWORD)).thenReturn(HASHED_KEY);
    when(validKeyCache.get(HASHED_KEY)).thenReturn(Optional.empty());
  }


  @Test
  void verifyKeyLookup() {
    givenKeyAndPasswordIsInHeaderWithCacheMiss();
    when(httpServletRequest.getRemoteAddr()).thenReturn(REMOTE_IP);
    when(invalidKeyLockOut.isLockedOut(httpServletRequest)).thenReturn(false);
    when(moderatorApiKeyDao.lookupModeratorIdByApiKey(HASHED_KEY)).thenReturn(Optional.of(MODERATOR_ID));
    when(moderatorApiKeyDao.recordKeyUsage(HASHED_KEY, REMOTE_IP)).thenReturn(1);

    assertThat(apiKeyValidationService.lookupModeratorIdByApiKey(httpServletRequest), is(MODERATOR_ID));

    verify(validKeyCache).recordValid(HASHED_KEY, MODERATOR_ID);
    verify(moderatorApiKeyDao).recordKeyUsage(HASHED_KEY, REMOTE_IP);
    verify(invalidKeyLockOut, never()).recordInvalid(any());
  }

  @Test
  void invalidKeysAreRecordedAndThrow() {
    givenKeyAndPasswordIsInHeaderWithCacheMiss();
    when(invalidKeyLockOut.isLockedOut(httpServletRequest)).thenReturn(false);
    when(moderatorApiKeyDao.lookupModeratorIdByApiKey(HASHED_KEY)).thenReturn(Optional.empty());

    assertThrows(
        IncorrectApiKeyException.class, () -> apiKeyValidationService.lookupModeratorIdByApiKey(httpServletRequest));

    verify(invalidKeyLockOut).recordInvalid(httpServletRequest);
    verify(validKeyCache, never()).recordValid(any(), anyInt());
    verify(moderatorApiKeyDao, never()).recordKeyUsage(any(), any());
  }

  @Nested
  final class VerifySuperMod {
    @Test
    void verifySuperMod() {
      when(httpServletRequest.getHeader(ToolboxHttpHeaders.API_KEY_HEADER))
          .thenReturn(API_KEY);
      when(httpServletRequest.getHeader(ToolboxHttpHeaders.API_KEY_PASSWORD_HEADER))
          .thenReturn(API_KEY_PASSWORD);
      when(keyHasher.apply(API_KEY, API_KEY_PASSWORD))
          .thenReturn(HASHED_KEY);
      when(moderatorApiKeyDao.lookupSuperModeratorIdByApiKey(HASHED_KEY))
          .thenReturn(Optional.of(123));

      assertThat(
          apiKeyValidationService.verifySuperMod(httpServletRequest),
          is(123));
    }

    @Test
    void verifySuperModNotSuperMode() {
      when(httpServletRequest.getHeader(ToolboxHttpHeaders.API_KEY_HEADER))
          .thenReturn(API_KEY);
      when(httpServletRequest.getHeader(ToolboxHttpHeaders.API_KEY_PASSWORD_HEADER))
          .thenReturn(API_KEY_PASSWORD);
      when(keyHasher.apply(API_KEY, API_KEY_PASSWORD))
          .thenReturn(HASHED_KEY);
      when(moderatorApiKeyDao.lookupSuperModeratorIdByApiKey(HASHED_KEY))
          .thenReturn(Optional.empty());

      assertThrows(
          IncorrectApiKeyException.class,
          () -> apiKeyValidationService.verifySuperMod(httpServletRequest));
    }
  }

  @Nested
  final class LookupSuperModByApiKeyTest {
    @Test
    void lookupSuperModByApiKey() {
      when(httpServletRequest.getHeader(ToolboxHttpHeaders.API_KEY_HEADER))
          .thenReturn(API_KEY);
      when(httpServletRequest.getHeader(ToolboxHttpHeaders.API_KEY_PASSWORD_HEADER))
          .thenReturn(API_KEY_PASSWORD);
      when(keyHasher.apply(API_KEY, API_KEY_PASSWORD))
          .thenReturn(HASHED_KEY);
      when(moderatorApiKeyDao.lookupSuperModeratorIdByApiKey(HASHED_KEY))
          .thenReturn(Optional.of(123));

      assertThat(
          apiKeyValidationService.lookupSuperModByApiKey(httpServletRequest),
          isPresentAndIs(123));
    }


    @Test
    void lookupSuperModByApiKeyNotSuperMod() {
      when(httpServletRequest.getHeader(ToolboxHttpHeaders.API_KEY_HEADER))
          .thenReturn(API_KEY);
      when(httpServletRequest.getHeader(ToolboxHttpHeaders.API_KEY_PASSWORD_HEADER))
          .thenReturn(API_KEY_PASSWORD);
      when(keyHasher.apply(API_KEY, API_KEY_PASSWORD))
          .thenReturn(HASHED_KEY);
      when(moderatorApiKeyDao.lookupSuperModeratorIdByApiKey(HASHED_KEY))
          .thenReturn(Optional.empty());

      assertThat(
          apiKeyValidationService.lookupSuperModByApiKey(httpServletRequest),
          isEmpty());
    }
  }

  @Test
  void clearLockout() {
    apiKeyValidationService.clearLockoutCache(httpServletRequest);
    verify(invalidKeyLockOut).clearLockouts(httpServletRequest);
  }
}
