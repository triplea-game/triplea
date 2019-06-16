package org.triplea.lobby.server.db.dao;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.lobby.server.db.JdbiDatabase;
import org.triplea.test.common.Integration;


@Integration
@ExtendWith(MockitoExtension.class)
class ModeratorKeyRegistrationDaoTest {

  private static final String MACHINE_IP = "123";
  private static final int USER_ID = 2000;

  private static final String SINGLE_USE_KEY =
      "aaaaaaabbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5"
          + "976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86";

  private static final String NEW_KEY =
      "bbbbbbabbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5"
          + "976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86";

  private final ModeratorKeyRegistrationDao moderatorKeyRegistrationDao =
      JdbiDatabase.newConnection().onDemand(ModeratorKeyRegistrationDao.class);

  @Mock
  private ModeratorApiKeyDao moderatorApiKeyDao;

  @Mock
  private ModeratorSingleUseKeyDao moderatorSingleUseKeyDao;

  @Test
  void throwsIfSingleUseKeyIsNotUpdated() {
    when(moderatorSingleUseKeyDao.invalidateSingleUseKey(SINGLE_USE_KEY)).thenReturn(0);
    assertThrows(
        IllegalStateException.class,
        () -> moderatorKeyRegistrationDao.invalidateSingleUseKeyAndGenerateNew(params()));
  }

  @Test
  void throwsIfNewKeyIsNotInserted() {
    when(moderatorSingleUseKeyDao.invalidateSingleUseKey(SINGLE_USE_KEY)).thenReturn(1);
    when(moderatorApiKeyDao.insertNewApiKey(
        any(String.class), eq(USER_ID), eq(MACHINE_IP), eq(NEW_KEY))).thenReturn(0);
    assertThrows(
        IllegalStateException.class,
        () -> moderatorKeyRegistrationDao.invalidateSingleUseKeyAndGenerateNew(params()));
  }

  @Test
  void successCase() {
    when(moderatorSingleUseKeyDao.invalidateSingleUseKey(SINGLE_USE_KEY)).thenReturn(1);
    when(moderatorApiKeyDao.insertNewApiKey(
        any(String.class), eq(USER_ID), eq(MACHINE_IP), eq(NEW_KEY))).thenReturn(1);

    moderatorKeyRegistrationDao.invalidateSingleUseKeyAndGenerateNew(params());
  }

  private ModeratorKeyRegistrationDao.Params params() {
    return ModeratorKeyRegistrationDao.Params.builder()
        .apiKeyDao(moderatorApiKeyDao)
        .singleUseKeyDao(moderatorSingleUseKeyDao)
        .newKey(NEW_KEY)
        .singleUseKey(SINGLE_USE_KEY)
        .registeringMachineIp(MACHINE_IP)
        .userId(USER_ID)
        .build();
  }
}
