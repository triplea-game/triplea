package org.triplea.server.moderator.toolbox.api.key.registration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.lobby.server.db.dao.ModeratorApiKeyDao;
import org.triplea.lobby.server.db.dao.ModeratorKeyRegistrationDao;
import org.triplea.lobby.server.db.dao.ModeratorSingleUseKeyDao;
import org.triplea.server.moderator.toolbox.api.key.InvalidKeyLockOut;
import org.triplea.server.moderator.toolbox.api.key.exception.ApiKeyLockOutException;
import org.triplea.server.moderator.toolbox.api.key.exception.IncorrectApiKeyException;

@ExtendWith(MockitoExtension.class)
class ApiKeyRegistrationServiceTest {

  private static final String SINGLE_USE_KEY = "Krakens are the comrades of the dead greed.";
  private static final String PASSWORD =
      "Nothing like the lively endurance screaming on the hornpipe.";
  private static final String HASHED_KEY = "Belay, treasure!";
  private static final int MODERATOR_ID = 1007;
  private static final String NEW_API_KEY = "All winds burn addled, clear cannons.";
  private static final String NEW_HASHED_KEY = "Ah, loot me shipmate, ye sunny fish!";
  private static final String MACHINE_IP =
      "Aww there's nothing like the scrawny fortune waving on the sea.";

  @Mock private Function<String, String> singleKeyHasher;
  @Mock private BiFunction<String, String, String> keyHasher;

  @Mock private Supplier<String> newApiKeySupplier;

  @Mock private InvalidKeyLockOut invalidKeyLockOut;

  @Mock private ModeratorApiKeyDao moderatorApiKeyDao;

  @Mock private ModeratorSingleUseKeyDao moderatorSingleUseKeyDao;

  @Mock private Predicate<String> passwordBlackList;

  @Mock private ModeratorKeyRegistrationDao moderatorKeyRegistrationDao;

  @InjectMocks private ApiKeyRegistrationService apiKeyRegistrationService;

  @Mock private HttpServletRequest httpServletRequest;

  @Test
  void registerKeyThrowsIfLockedOut() {
    when(invalidKeyLockOut.isLockedOut(httpServletRequest)).thenReturn(true);
    assertThrows(
        ApiKeyLockOutException.class,
        () -> apiKeyRegistrationService.registerKey(httpServletRequest, SINGLE_USE_KEY, PASSWORD));
  }

  @Test
  void requestRejectedIfPasswordTooEasy() {
    when(invalidKeyLockOut.isLockedOut(httpServletRequest)).thenReturn(false);
    when(passwordBlackList.test(PASSWORD)).thenReturn(true);
    assertThrows(
        PasswordTooEasyException.class,
        () -> apiKeyRegistrationService.registerKey(httpServletRequest, SINGLE_USE_KEY, PASSWORD));
  }

  @Test
  void missingModeratorThrowsIncorrectKeyException() {
    when(invalidKeyLockOut.isLockedOut(httpServletRequest)).thenReturn(false);
    when(passwordBlackList.test(PASSWORD)).thenReturn(false);
    when(singleKeyHasher.apply(SINGLE_USE_KEY)).thenReturn(HASHED_KEY);
    when(moderatorSingleUseKeyDao.lookupModeratorBySingleUseKey(HASHED_KEY))
        .thenReturn(Optional.empty());

    assertThrows(
        IncorrectApiKeyException.class,
        () -> apiKeyRegistrationService.registerKey(httpServletRequest, SINGLE_USE_KEY, PASSWORD));
  }

  @Test
  void verifyHappyCase() {
    when(invalidKeyLockOut.isLockedOut(httpServletRequest)).thenReturn(false);
    when(passwordBlackList.test(PASSWORD)).thenReturn(false);
    when(singleKeyHasher.apply(SINGLE_USE_KEY)).thenReturn(HASHED_KEY);
    when(moderatorSingleUseKeyDao.lookupModeratorBySingleUseKey(HASHED_KEY))
        .thenReturn(Optional.of(MODERATOR_ID));
    when(newApiKeySupplier.get()).thenReturn(NEW_API_KEY);
    when(keyHasher.apply(NEW_API_KEY, PASSWORD)).thenReturn(NEW_HASHED_KEY);
    when(httpServletRequest.getRemoteAddr()).thenReturn(MACHINE_IP);

    assertThat(
        apiKeyRegistrationService.registerKey(httpServletRequest, SINGLE_USE_KEY, PASSWORD),
        is(NEW_API_KEY));

    verify(moderatorKeyRegistrationDao)
        .invalidateSingleUseKeyAndGenerateNew(
            ModeratorKeyRegistrationDao.Params.builder()
                .singleUseKeyDao(moderatorSingleUseKeyDao)
                .singleUseKey(HASHED_KEY)
                .apiKeyDao(moderatorApiKeyDao)
                .newKey(NEW_HASHED_KEY)
                .registeringMachineIp(MACHINE_IP)
                .userId(MODERATOR_ID)
                .build());
  }
}
