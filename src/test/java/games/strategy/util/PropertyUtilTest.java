package games.strategy.util;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import games.strategy.triplea.attachments.RulesAttachment;

/**
 * PropertyUtil test sets / gets variables via reflection.
 *
 * <p>
 * The set in PropertyUtil done through a setter method, the get is done by reading the private variable value directly.
 * </p>
 *
 * <p>
 * To test we set up some sample test objects and do read/set property operations on them.
 * </p>
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
    final IntegerMap<String> unitPresence = new IntegerMap<>();
    unitPresence.add("Blah", 3);
    PropertyUtil.set("unitPresence", unitPresence, testClass);
    assertThat(PropertyUtil.getPropertyFieldObject("unitPresence", testClass), is(unitPresence));
  }

  private static final String NEW_VALUE = "newValue";
  private static final String BAR = "bar";
  protected static final String DEFAULT = "default";

  @Test
  public void testHappyCaseWithGetAndSetFields() {
    final PropertyClass testClass = new PropertyClass();
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
    assertThat((String) PropertyUtil.getPropertyFieldObject(BAR, new mUnderBarClass()), is(DEFAULT));
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
    final NoOpSetterClass testClass = new NoOpSetterClass();
    PropertyUtil.set(BAR, NEW_VALUE, testClass);
    assertThat(
        "we are only really checking that the setter method was called, which did not do anything, thus we should "
            + "still have a default value.",
        testClass.bar, is("default"));
  }

  private static class NoSetterClass {
    @SuppressWarnings("unused")
    private final String bar = PropertyUtilTest.DEFAULT;
  }

  private static class InvalidSetterClass {
    @SuppressWarnings("unused")
    private final String bar = PropertyUtilTest.DEFAULT;
    @SuppressWarnings("unused")
    public void setBar() {}
  }

  private static class NoOpSetterClass {
    protected String bar = PropertyUtilTest.DEFAULT;
    @SuppressWarnings("unused")
    public void setBar(final String value) {}
  }

  private static class PropertyClass {
    protected String bar = PropertyUtilTest.DEFAULT;
    @SuppressWarnings("unused")
    public void setBar(final String newValue) {
      bar = newValue;
    }
  }

  private static class mUnderBarClass {
    @SuppressWarnings("unused")
    private String m_bar = PropertyUtilTest.DEFAULT;
    @SuppressWarnings("unused")
    public void setBar(final String newValue) {
      m_bar = newValue;
    }
  }
}
