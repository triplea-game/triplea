package org.triplea.lobby.server.login;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.when;

import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.lobby.server.db.UserDao;

@ExtendWith(MockitoExtension.class)
class AllowCreateUserRulesTest {

  private static final String USERNAME = "username";
  private static final String ERROR = "error";

  private static final String VALID_EMAIL = "valid@email.com";

  @Mock private UserDao userDao;
  @Mock private Function<String, String> nameValidator;
  @Mock private Function<String, String> emailValidator;

  private AllowCreateUserRules allowCreateUserRules;

  @BeforeEach
  void setup() {
    allowCreateUserRules =
        AllowCreateUserRules.builder()
            .userDao(userDao)
            .nameValidator(nameValidator)
            .emailValidator(emailValidator)
            .build();
  }

  @Nested
  final class NotAllowedCases {
    @Test
    void badEmail() {
      assertThat(allowCreateUserRules.allowCreateUser(USERNAME, null), notNullValue());
      assertThat(allowCreateUserRules.allowCreateUser(USERNAME, ""), notNullValue());
      assertThat(allowCreateUserRules.allowCreateUser(USERNAME, "  "), notNullValue());
      assertThat(
          allowCreateUserRules.allowCreateUser(USERNAME, "space not allowed@invalid.com"),
          notNullValue());
    }

    @Test
    void invalidName() {
      when(nameValidator.apply(USERNAME)).thenReturn(ERROR);
      assertThat(allowCreateUserRules.allowCreateUser(USERNAME, VALID_EMAIL), notNullValue());
    }

    @Test
    void invalidEmail() {
      when(nameValidator.apply(USERNAME)).thenReturn(null);
      when(emailValidator.apply(VALID_EMAIL)).thenReturn(ERROR);
      assertThat(allowCreateUserRules.allowCreateUser(USERNAME, VALID_EMAIL), notNullValue());
    }

    @Test
    void userAlreadyExists() {
      when(nameValidator.apply(USERNAME)).thenReturn(null);
      when(emailValidator.apply(VALID_EMAIL)).thenReturn(null);
      when(userDao.doesUserExist(USERNAME)).thenReturn(true);
      assertThat(allowCreateUserRules.allowCreateUser(USERNAME, VALID_EMAIL), notNullValue());
    }
  }

  @Nested
  final class AllowCreateUser {
    @Test
    void allowCreateUser() {
      when(nameValidator.apply(USERNAME)).thenReturn(null);
      when(emailValidator.apply(VALID_EMAIL)).thenReturn(null);
      when(userDao.doesUserExist(USERNAME)).thenReturn(false);
      assertThat(allowCreateUserRules.allowCreateUser(USERNAME, VALID_EMAIL), nullValue());
    }
  }
}
