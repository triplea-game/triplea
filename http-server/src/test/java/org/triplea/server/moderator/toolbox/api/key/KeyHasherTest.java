package org.triplea.server.moderator.toolbox.api.key;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import java.util.function.BiFunction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.server.http.AppConfig;

class KeyHasherTest {

  private static final String API_KEY = "pirates";
  private static final String PASSWORD = "Arg, heavy-hearted plank. you won't hail the galley.";

  @Nested
  @ExtendWith(MockitoExtension.class)
  final class KeyHasherWithMocks {
    private static final String SALT = "Ahoy, never drink a corsair.";
    private static final String HASHED_PASSWORD = "All skulls desire black, mighty pirates.";

    @Mock
    private BiFunction<String, String, String> mockHasher;

    private KeyHasher keyHasher;

    @BeforeEach
    void setup() {
      keyHasher = new KeyHasher(SALT, mockHasher);
    }

    @Test
    void verifyWithMocks() {
      when(mockHasher.apply(PASSWORD, SALT)).thenReturn(HASHED_PASSWORD);
      assertThat(keyHasher.applyHash(PASSWORD), is(HASHED_PASSWORD));
    }

    @Test
    void verifyWithMocksSaltedApi() {
      when(mockHasher.apply(API_KEY + PASSWORD, SALT)).thenReturn(HASHED_PASSWORD);
      assertThat(keyHasher.applyHash(API_KEY, PASSWORD), is(HASHED_PASSWORD));
    }
  }

  @Nested
  final class KeyHasherWithBcrypt {
    private static final String PRE_GENERATED_SALT = "$2a$10$IhIXWg4HkQRWrZqjj9kV0u";

    private KeyHasher keyHasher;

    @BeforeEach
    void setup() {
      final AppConfig config = new AppConfig();
      config.setBcryptSalt(PRE_GENERATED_SALT);
      keyHasher = new KeyHasher(config);
    }

    @Test
    void verifyWithNoMocks() {
      final AppConfig config = new AppConfig();
      config.setBcryptSalt(PRE_GENERATED_SALT);
      assertThat(
          keyHasher.applyHash(API_KEY),
          is("$2a$10$IhIXWg4HkQRWrZqjj9kV0u7oIRjy4aljuF4BZcSbDXuKjB7nJwoPa"));
    }

    @Test
    void verifyWithNoMocksSaltedApi() {
      final AppConfig config = new AppConfig();
      config.setBcryptSalt(PRE_GENERATED_SALT);
      assertThat(
          keyHasher.applyHash(API_KEY, PASSWORD),
          is("$2a$10$IhIXWg4HkQRWrZqjj9kV0udxkPl.dwwsXdp./6Tkrh.fdpReVhEnC"));
    }
  }
}
