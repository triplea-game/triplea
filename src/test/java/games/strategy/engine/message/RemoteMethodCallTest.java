package games.strategy.engine.message;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public final class RemoteMethodCallTest {
  @Test
  public void stringToClass_ShouldReturnArgWhenStringIsNull() {
    assertThat(RemoteMethodCall.stringToClass(null, new Object()), is(Object.class));
  }

  @Test
  public void stringToClass_ShouldReturnPrimitiveIntegerTypeWhenStringIsInt() {
    assertThat(RemoteMethodCall.stringToClass("int", null), is(Integer.TYPE));
  }

  @Test
  public void stringToClass_ShouldReturnPrimitiveShortTypeWhenStringIsShort() {
    assertThat(RemoteMethodCall.stringToClass("short", null), is(Short.TYPE));
  }

  @Test
  public void stringToClass_ShouldReturnPrimitiveByteTypeWhenStringIsByte() {
    assertThat(RemoteMethodCall.stringToClass("byte", null), is(Byte.TYPE));
  }

  @Test
  public void stringToClass_ShouldReturnPrimitiveLongTypeWhenStringIsLong() {
    assertThat(RemoteMethodCall.stringToClass("long", null), is(Long.TYPE));
  }

  @Test
  public void stringToClass_ShouldReturnPrimitiveFloatTypeWhenStringIsFloat() {
    assertThat(RemoteMethodCall.stringToClass("float", null), is(Float.TYPE));
  }

  @Test
  public void stringToClass_ShouldReturnPrimitiveDoubleTypeWhenStringIsDouble() {
    assertThat(RemoteMethodCall.stringToClass("double", null), is(Double.TYPE));
  }

  @Test
  public void stringToClass_ShouldReturnPrimitiveBooleanTypeWhenStringIsBoolean() {
    assertThat(RemoteMethodCall.stringToClass("boolean", null), is(Boolean.TYPE));
  }

  @Test
  public void stringToClass_ShouldReturnClassWhenStringIsKnownClassName() {
    assertThat(RemoteMethodCall.stringToClass("java.lang.String", null), is(String.class));
  }

  @Test
  public void stringToClass_ShouldThrowExceptionWhenStringIsUnknownClassName() {
    catchException(() -> RemoteMethodCall.stringToClass("some.unknown.Type", null));

    assertThat(caughtException(), is(instanceOf(IllegalStateException.class)));
    assertThat(caughtException().getCause(), is(instanceOf(ClassNotFoundException.class)));
  }
}
