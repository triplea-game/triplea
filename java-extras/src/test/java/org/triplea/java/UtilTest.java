package org.triplea.java;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.triplea.java.function.NegationPredicate;

public class UtilTest {


  @Test
  public void isMailValid_ShouldReturnTrueWhenAddressIsValid() {
    Arrays.asList(
        "some@some.com",
        "some.someMore@some.com",
        "some@some.com some2@some2.com",
        "some@some.com some2@some2.co.uk",
        "some@some.com some2@some2.co.br",
        "",
        "some@some.some.some.com")
        .forEach(it -> assertThat("'" + it + "' should be valid", Util.isMailValid(it), is(true)));
  }

  @Test
  public void isMailValid_ShouldReturnFalseWhenAddressIsInvalid() {
    Collections.singletonList(
        "test")
        .forEach(it -> assertThat("'" + it + "' should be invalid", Util.isMailValid(it), is(false)));
  }

  @Test
  public void not_ShouldReturnLogicalNegationOfPredicate() {
    final Object t = new Object();

    assertThat(NegationPredicate.not(it -> false).test(t), is(true));
    assertThat(NegationPredicate.not(it -> true).test(t), is(false));
  }

  @Test
  void testIsInt() {
    assertThat(Util.isInt(""), is(false));
    assertThat(Util.isInt("12 34"), is(false));
    assertThat(Util.isInt("12.34"), is(false));
    assertThat(Util.isInt("1234"), is(true));
    assertThat(Util.isInt("0000000000000"), is(true));
    assertThat(Util.isInt("-0"), is(true));
    assertThat(Util.isInt("-4321"), is(true));
  }
}
