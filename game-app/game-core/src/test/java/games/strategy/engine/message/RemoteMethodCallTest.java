package games.strategy.engine.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class RemoteMethodCallTest {
  @Nested
  final class StringToClassTest {
    @Test
    void shouldReturnClassOfArgWhenStringIsNull() {
      assertThat(RemoteMethodCall.stringToClass(null, new Object())).isEqualTo(Object.class);
    }

    @Test
    void shouldReturnPrimitiveIntegerTypeWhenStringIsInt() {
      assertThat(RemoteMethodCall.stringToClass("int", null)).isEqualTo(Integer.TYPE);
    }

    @Test
    void shouldReturnPrimitiveShortTypeWhenStringIsShort() {
      assertThat(RemoteMethodCall.stringToClass("short", null)).isEqualTo(Short.TYPE);
    }

    @Test
    void shouldReturnPrimitiveByteTypeWhenStringIsByte() {
      assertThat(RemoteMethodCall.stringToClass("byte", null)).isEqualTo(Byte.TYPE);
    }

    @Test
    void shouldReturnPrimitiveLongTypeWhenStringIsLong() {
      assertThat(RemoteMethodCall.stringToClass("long", null)).isEqualTo(Long.TYPE);
    }

    @Test
    void shouldReturnPrimitiveFloatTypeWhenStringIsFloat() {
      assertThat(RemoteMethodCall.stringToClass("float", null)).isEqualTo(Float.TYPE);
    }

    @Test
    void shouldReturnPrimitiveDoubleTypeWhenStringIsDouble() {
      assertThat(RemoteMethodCall.stringToClass("double", null)).isEqualTo(Double.TYPE);
    }

    @Test
    void shouldReturnPrimitiveBooleanTypeWhenStringIsBoolean() {
      assertThat(RemoteMethodCall.stringToClass("boolean", null)).isEqualTo(Boolean.TYPE);
    }

    @Test
    void shouldReturnClassWhenStringIsKnownClassName() {
      assertThat(RemoteMethodCall.stringToClass("java.lang.String", null)).isEqualTo(String.class);
    }

    @Test
    void shouldThrowExceptionWhenStringIsUnknownClassName() {
      final Exception e =
          assertThrows(
              IllegalStateException.class,
              () -> RemoteMethodCall.stringToClass("some.unknown.Type", null));
      assertThat(e.getCause()).isInstanceOf(ClassNotFoundException.class);
    }
  }
}
