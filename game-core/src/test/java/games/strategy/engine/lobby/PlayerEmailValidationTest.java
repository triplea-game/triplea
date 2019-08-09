package games.strategy.engine.lobby;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class PlayerEmailValidationTest {
  @Test
  void shouldReturnTrueWhenAddressIsValid() {
    Arrays.asList(
            "some@some.com",
            "some.someMore@some.com",
            "some@some.com some2@some2.com",
            "some@some.com some2@some2.co.uk",
            "some@some.com some2@some2.co.br",
            "",
            "some@some.some.some.com")
        .forEach(
            it ->
                assertThat(
                    "'" + it + "' should be valid", PlayerEmailValidation.isValid(it), is(true)));
  }

  @Test
  void shouldReturnFalseWhenAddressIsInvalid() {
    Collections.singletonList("test")
        .forEach(
            it ->
                assertThat(
                    "'" + it + "' should be invalid",
                    PlayerEmailValidation.isValid(it),
                    is(false)));
  }
}
