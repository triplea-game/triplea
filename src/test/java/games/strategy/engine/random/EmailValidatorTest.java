package games.strategy.engine.random;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import games.strategy.util.Util;

public class EmailValidatorTest {

  @Test
  public void testValidEmail() {
    final String[] good = new String[] {"some@some.com", "some.someMore@some.com", "some@some.com some2@some2.com",
        "some@some.com some2@some2.co.uk", "some@some.com some2@some2.co.br", "", "some@some.some.some.com"};
    final String[] bad = new String[] {"test"};
    for (final String element : good) {
      assertTrue(element + " is good but failed", Util.isMailValid(element));
    }
    for (final String element : bad) {
      assertFalse(element + " is bad but passed", Util.isMailValid(element));
    }
  }
}
