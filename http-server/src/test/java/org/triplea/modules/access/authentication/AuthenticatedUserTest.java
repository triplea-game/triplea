package org.triplea.modules.access.authentication;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.triplea.db.data.UserRole;
import org.triplea.domain.data.ApiKey;
import org.triplea.modules.TestData;

/**
 * We're not testing a whole lot here, the 'custom' getters of AuthenticatedUser do state checks for
 * validity. In this test we are running through invalid and valid states to ensure we have the
 * checks correct.
 */
@SuppressWarnings("InnerClassMayBeStatic")
class AuthenticatedUserTest {

  private static final ApiKey API_KEY = TestData.API_KEY;
  private static final String NAME = "name";
  private static final int USER_ID = 10;

  private static final List<String> rolesWithUserNames =
      List.of(UserRole.ADMIN, UserRole.MODERATOR, UserRole.PLAYER, UserRole.ANONYMOUS);

  @Nested
  class GetName {
    @Test
    void validStates() {
      AuthenticatedUser.builder().userRole(UserRole.HOST).apiKey(API_KEY).build().getName();
      rolesWithUserNames.forEach(
          roleWithName ->
              AuthenticatedUser.builder()
                  .name(NAME)
                  .userRole(roleWithName)
                  .apiKey(API_KEY)
                  .build()
                  .getName());
    }

    @Test
    void invalidStateIsHostWithName() {
      assertThrows(
          IllegalStateException.class,
          () ->
              AuthenticatedUser.builder()
                  .userRole(UserRole.HOST)
                  .name(NAME)
                  .apiKey(API_KEY)
                  .build()
                  .getName());
    }

    @Test
    void invalidStateIsNonHostWithoutName() {
      // following invalid because name is null
      rolesWithUserNames.forEach(
          roleWithName ->
              assertThrows(
                  IllegalStateException.class,
                  () ->
                      AuthenticatedUser.builder()
                          .userRole(roleWithName)
                          .apiKey(API_KEY)
                          .build()
                          .getName()));
    }
  }

  @Nested
  class GetUserIdOrThrow {
    @Test
    void validStates() {
      // all named roles except anonymous and host can have a user id
      AuthenticatedUser.builder()
          .userId(USER_ID)
          .userRole(UserRole.ADMIN)
          .apiKey(API_KEY)
          .build()
          .getUserIdOrThrow();

      AuthenticatedUser.builder()
          .userId(USER_ID)
          .userRole(UserRole.MODERATOR)
          .apiKey(API_KEY)
          .build()
          .getUserIdOrThrow();

      AuthenticatedUser.builder()
          .userId(USER_ID)
          .userRole(UserRole.PLAYER)
          .apiKey(API_KEY)
          .build()
          .getUserIdOrThrow();
    }

    @Test
    void hostWithUserIdIsInvalid() {
      assertThrows(
          IllegalStateException.class,
          () ->
              AuthenticatedUser.builder()
                  .userId(USER_ID)
                  .userRole(UserRole.HOST)
                  .apiKey(API_KEY)
                  .build()
                  .getUserIdOrThrow());
    }

    @Test
    void anonymousWithUserIdIsInvalid() {
      assertThrows(
          IllegalStateException.class,
          () ->
              AuthenticatedUser.builder()
                  .userId(USER_ID)
                  .userRole(UserRole.ANONYMOUS)
                  .apiKey(API_KEY)
                  .build()
                  .getUserIdOrThrow());
    }
  }
}
