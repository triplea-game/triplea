package org.triplea.server.moderator.toolbox.api.key;

import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.lobby.server.db.dao.ModeratorSingleUseKeyDao;
import org.triplea.lobby.server.db.dao.UserJdbiDao;

@ExtendWith(MockitoExtension.class)
class GenerateSingleUseKeyServiceTest {

  private static final String MODERATOR_NAME = "Parrots whine on greed at fort charles!";
  private static final int USER_ID = 523;
  private static final String GENERATED_KEY =
      "Aww there's nothing like the stormy adventure ding on the moon.";
  private static final String HASHED_KEY = "Bung holes are the landlubbers of the rainy passion.";

  @Mock private Function<String, String> singleUseKeyHasher;
  @Mock private ModeratorSingleUseKeyDao singleUseKeyDao;
  @Mock private UserJdbiDao userJdbiDao;
  @Mock private Supplier<String> keySupplier;

  @InjectMocks private GenerateSingleUseKeyService generateSingleUseKeyService;

  @Test
  void generateSingleUseKeyThrowsIfModeratorNameNotFound() {
    when(userJdbiDao.lookupUserIdByName(MODERATOR_NAME)).thenReturn(Optional.empty());

    assertThrows(
        IllegalStateException.class,
        () -> generateSingleUseKeyService.generateSingleUseKey(MODERATOR_NAME));
  }

  @Test
  void generateSingleUseKeyByModeratorName() {
    when(userJdbiDao.lookupUserIdByName(MODERATOR_NAME)).thenReturn(Optional.of(USER_ID));
    givenInsertReturnsRowCount(1);

    MatcherAssert.assertThat(
        generateSingleUseKeyService.generateSingleUseKey(MODERATOR_NAME), is(GENERATED_KEY));
  }

  private void givenInsertReturnsRowCount(final int rowCount) {
    when(keySupplier.get()).thenReturn(GENERATED_KEY);
    when(singleUseKeyHasher.apply(GENERATED_KEY)).thenReturn(HASHED_KEY);
    when(singleUseKeyDao.insertSingleUseKey(USER_ID, HASHED_KEY)).thenReturn(rowCount);
  }

  @Test
  void generateSingleUseKeyByUserId() {
    givenInsertReturnsRowCount(1);

    MatcherAssert.assertThat(
        generateSingleUseKeyService.generateSingleUseKey(USER_ID), is(GENERATED_KEY));
  }

  @Test
  void generateThrowsIfInsertFails() {
    givenInsertReturnsRowCount(0);

    assertThrows(
        IllegalStateException.class,
        () -> generateSingleUseKeyService.generateSingleUseKey(USER_ID));
  }
}
