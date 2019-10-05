package games.strategy.engine.message;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class RemoteMethodCallTest {
  @Nested
  final class StringToClassTest {
    @Test
    void shouldReturnClassOfArgWhenStringIsNull() {
      assertThat(RemoteMethodCall.stringToClass(null, new Object()), is(Object.class));
    }

    @Test
    void shouldReturnPrimitiveIntegerTypeWhenStringIsInt() {
      assertThat(RemoteMethodCall.stringToClass("int", null), is(Integer.TYPE));
    }

    @Test
    void shouldReturnPrimitiveShortTypeWhenStringIsShort() {
      assertThat(RemoteMethodCall.stringToClass("short", null), is(Short.TYPE));
    }

    @Test
    void shouldReturnPrimitiveByteTypeWhenStringIsByte() {
      assertThat(RemoteMethodCall.stringToClass("byte", null), is(Byte.TYPE));
    }

    @Test
    void shouldReturnPrimitiveLongTypeWhenStringIsLong() {
      assertThat(RemoteMethodCall.stringToClass("long", null), is(Long.TYPE));
    }

    @Test
    void shouldReturnPrimitiveFloatTypeWhenStringIsFloat() {
      assertThat(RemoteMethodCall.stringToClass("float", null), is(Float.TYPE));
    }

    @Test
    void shouldReturnPrimitiveDoubleTypeWhenStringIsDouble() {
      assertThat(RemoteMethodCall.stringToClass("double", null), is(Double.TYPE));
    }

    @Test
    void shouldReturnPrimitiveBooleanTypeWhenStringIsBoolean() {
      assertThat(RemoteMethodCall.stringToClass("boolean", null), is(Boolean.TYPE));
    }

    @Test
    void shouldReturnClassWhenStringIsKnownClassName() {
      assertThat(RemoteMethodCall.stringToClass("java.lang.String", null), is(String.class));
    }

    @Test
    void shouldThrowExceptionWhenStringIsUnknownClassName() {
      final Exception e =
          assertThrows(
              IllegalStateException.class,
              () -> RemoteMethodCall.stringToClass("some.unknown.Type", null));
      assertThat(e.getCause(), is(instanceOf(ClassNotFoundException.class)));
    }
  }
}
