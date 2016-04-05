package games.strategy.util;


import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import games.strategy.triplea.attachments.RulesAttachment;


/**
 * PropertyUtil test sets / gets variables via reflection.
 *
 * The set in PropertyUtil done through a setter method, the get is done by reading the private variable value directly.
 *
 * To test we set up some sample test objects and do read/set property operations on them.
 */
public class PropertyUtilTest {
  @Test
  public void testGetFieldObject() {
    final RulesAttachment testClass = new RulesAttachment("test", null, null);
    int uses = (Integer) PropertyUtil.getPropertyFieldObject("uses", testClass);
    // default value should be -1
    assertThat(uses, is(-1));
    PropertyUtil.set("uses", "3", testClass);
    uses = (Integer) PropertyUtil.getPropertyFieldObject("uses", testClass);
    assertThat(uses, is(3));
    final IntegerMap<String> unitPresence = new IntegerMap<String>();
    unitPresence.add("Blah", 3);
    PropertyUtil.set("unitPresence", unitPresence, testClass);
    assertThat(PropertyUtil.getPropertyFieldObject("unitPresence", testClass), is(unitPresence));
  }

  private static final String NEW_VALUE = "newValue";
  private static final String BAR = "bar";
  protected static final String DEFAULT = "default";

  @Test
  public void testHappyCaseWithGetAndSetFields() {
    PropertyClass testClass = new PropertyClass();
    PropertyUtil.set("bar", NEW_VALUE, testClass);
    assertThat(testClass.bar, is(NEW_VALUE));

    assertThat((String) PropertyUtil.getPropertyFieldObject(BAR, testClass), is(NEW_VALUE));
  }

  @Test
  public void testGetField() {
    assertThat((String) PropertyUtil.getPropertyFieldObject("bar", new PropertyClass()), is(DEFAULT));
  }

  @Test
  public void testGetFieldWithPrefixedPropertyNames() {
    assertThat((String) PropertyUtil.getPropertyFieldObject(BAR, new mUnderBarClass() ), is(DEFAULT));
  }



  @Test(expected = IllegalStateException.class)
  public void testErrorCaseWithNoSetterMethod() {
    PropertyUtil.set(BAR, NEW_VALUE, new NoSetterClass());
  }

  @Test(expected = IllegalStateException.class)
  public void testErrorCaseWithInvalidSetterMethod() {
    PropertyUtil.set(BAR, NEW_VALUE, new InvalidSetterClass());
  }

  @Test
  public void testNoOpSetterMethod() {
    NoOpSetterClass testClass = new NoOpSetterClass();
    PropertyUtil.set(BAR, NEW_VALUE, testClass);
    assertThat(
        "we are only really checking that the setter method was called, which did not do anything, thus we shoudl still have a default value.",
        testClass.bar, is("default"));
  }


}


class NoSetterClass {
  @SuppressWarnings("unused")
  private String bar = PropertyUtilTest.DEFAULT;
}


class InvalidSetterClass {
  @SuppressWarnings("unused")
  private String bar = PropertyUtilTest.DEFAULT;

  public void setBar() {}
}


class NoOpSetterClass {
  protected String bar = PropertyUtilTest.DEFAULT;

  public void setBar(@SuppressWarnings("unused") String value) {}
}


class PropertyClass {
  protected String bar = PropertyUtilTest.DEFAULT;

  public void setBar(String newValue) {
    bar = newValue;
  }
}


class mUnderBarClass {
  @SuppressWarnings("unused")
  private String m_bar = PropertyUtilTest.DEFAULT;

  public void setBar(String newValue) {
    m_bar = newValue;
  }
}
