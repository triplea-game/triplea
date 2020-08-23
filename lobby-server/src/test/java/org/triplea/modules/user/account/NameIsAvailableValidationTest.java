package org.triplea.modules.user.account;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.db.dao.user.UserJdbiDao;

@ExtendWith(MockitoExtension.class)
class NameIsAvailableValidationTest {

  @Mock private UserJdbiDao userJdbiDao;

  @InjectMocks private NameIsAvailableValidation nameIsAvailableValidation;

  @Test
  void userAlreadyExists() {
    when(userJdbiDao.lookupUserIdByName("name")).thenReturn(Optional.of(1));

    final Optional<String> result = nameIsAvailableValidation.apply("name");

    assertThat(result, isPresent());
  }

  @Test
  void userDoesNotAlreadyExists() {
    when(userJdbiDao.lookupUserIdByName("name")).thenReturn(Optional.empty());

    final Optional<String> result = nameIsAvailableValidation.apply("name");

    assertThat(result, isEmpty());
  }
}
