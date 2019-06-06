package org.triplea.server.moderator.toolbox.api.key.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.Function;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.moderator.toolbox.ModeratorToolboxClient;
import org.triplea.server.moderator.toolbox.api.key.validation.exception.ApiKeyVerificationLockOutException;


@ExtendWith(MockitoExtension.class)
class ApiKeyValidationServiceTest {

  private static final String API_KEY = "The cloud screams faith like a fine tuna.";
  private static final int MODERATOR_ID = 77;

  @Mock
  private ValidKeyCache validKeyCache;
  @Mock
  private InvalidKeyLockOut invalidKeyLockOut;
  @Mock
  private Function<String, Optional<Integer>> apiKeyLookup;

  @InjectMocks
  private ApiKeyValidationService apiKeyValidationService;

  @Mock
  private HttpServletRequest httpServletRequest;

  @Test
  void missingKeyThrows() {
    when(httpServletRequest.getHeader(ModeratorToolboxClient.MODERATOR_API_KEY_HEADER)).thenReturn(null);
    assertThrows(IllegalArgumentException.class, () -> apiKeyValidationService.verifyApiKey(httpServletRequest));
  }

  @Test
  void emptyKeyThrows() {
    when(httpServletRequest.getHeader(ModeratorToolboxClient.MODERATOR_API_KEY_HEADER)).thenReturn("");
    assertThrows(IllegalArgumentException.class, () -> apiKeyValidationService.verifyApiKey(httpServletRequest));
  }

  @Test
  void cachedValuesReturnedImmediately() {
    when(httpServletRequest.getHeader(ModeratorToolboxClient.MODERATOR_API_KEY_HEADER)).thenReturn(API_KEY);
    when(validKeyCache.get(API_KEY)).thenReturn(Optional.of(MODERATOR_ID));

    assertThat(apiKeyValidationService.lookupModeratorIdByApiKey(httpServletRequest), is(MODERATOR_ID));

    verify(apiKeyLookup, never()).apply(any());
    verify(invalidKeyLockOut, never()).isLockedOut(any());
  }

  @Test
  void lockoutDoesNotAttemptKeyLookup() {
    when(httpServletRequest.getHeader(ModeratorToolboxClient.MODERATOR_API_KEY_HEADER)).thenReturn(API_KEY);
    when(validKeyCache.get(API_KEY)).thenReturn(Optional.empty());
    when(invalidKeyLockOut.isLockedOut(httpServletRequest)).thenReturn(true);

    assertThrows(
        ApiKeyVerificationLockOutException.class,
        () -> apiKeyValidationService.lookupModeratorIdByApiKey(httpServletRequest));

    verify(apiKeyLookup, never()).apply(any());
  }

  @Test
  void verifyKeyLookup() {
    when(httpServletRequest.getHeader(ModeratorToolboxClient.MODERATOR_API_KEY_HEADER)).thenReturn(API_KEY);
    when(validKeyCache.get(API_KEY)).thenReturn(Optional.empty());
    when(invalidKeyLockOut.isLockedOut(httpServletRequest)).thenReturn(false);
    when(apiKeyLookup.apply(API_KEY)).thenReturn(Optional.of(MODERATOR_ID));

    assertThat(apiKeyValidationService.lookupModeratorIdByApiKey(httpServletRequest), is(MODERATOR_ID));

    verify(validKeyCache).recordValid(API_KEY);
    verify(invalidKeyLockOut, never()).recordInvalid(any());
  }

  @Test
  void invalidKeysAreRecordedAndThrow() {
    when(httpServletRequest.getHeader(ModeratorToolboxClient.MODERATOR_API_KEY_HEADER)).thenReturn(API_KEY);
    when(validKeyCache.get(API_KEY)).thenReturn(Optional.empty());
    when(invalidKeyLockOut.isLockedOut(httpServletRequest)).thenReturn(false);
    when(apiKeyLookup.apply(API_KEY)).thenReturn(Optional.empty());

    assertThat(apiKeyValidationService.lookupModeratorIdByApiKey(httpServletRequest), is(MODERATOR_ID));

    verify(validKeyCache, never()).recordValid(any());
    verify(invalidKeyLockOut).recordInvalid(httpServletRequest);
  }
}
