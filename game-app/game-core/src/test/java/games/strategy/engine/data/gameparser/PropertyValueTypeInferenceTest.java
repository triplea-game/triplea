package games.strategy.engine.data.gameparser;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PropertyValueTypeInferenceTest {

  @Test
  void inferNullType() {
    assertThat(PropertyValueTypeInference.inferType(null), is(String.class));
  }

  @Test
  void inferString() {
    assertThat(PropertyValueTypeInference.inferType(""), is(String.class));
  }

  @Test
  void inferNumber() {
    assertThat(PropertyValueTypeInference.inferType("2"), is(Integer.class));
  }

  @Test
  void inferBoolean() {
    assertThat(PropertyValueTypeInference.inferType("false"), is(Boolean.class));
  }

  @Test
  void nullInputIsReturnedAsNull() {
    assertThat(PropertyValueTypeInference.castToInferredType(null), is(nullValue()));
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 0, 1, 100})
  void inferNumberValues(final int value) {
    final Object result = PropertyValueTypeInference.castToInferredType(String.valueOf(value));

    assertThat(result, is(value));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void inferNumberValues(final boolean value) {
    assertThat(
        PropertyValueTypeInference.castToInferredType(
            String.valueOf(value).toLowerCase(Locale.ROOT)),
        is(value));
    assertThat(
        PropertyValueTypeInference.castToInferredType(
            String.valueOf(value).toUpperCase(Locale.ROOT)),
        is(value));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "string"})
  void inferStringValue(final String value) {
    assertThat(PropertyValueTypeInference.castToInferredType(value), is(value));
  }
}
