package games.strategy.engine.data.gameparser;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PropertyValueTypeInferenceTest {

  @Test
  void inferNullType() {
    assertThat(PropertyValueTypeInference.inferType(null)).isEqualTo(String.class);
  }

  @Test
  void inferString() {
    assertThat(PropertyValueTypeInference.inferType("")).isEqualTo(String.class);
  }

  @Test
  void inferNumber() {
    assertThat(PropertyValueTypeInference.inferType("2")).isEqualTo(Integer.class);
  }

  @Test
  void inferBoolean() {
    assertThat(PropertyValueTypeInference.inferType("false")).isEqualTo(Boolean.class);
  }

  @Test
  void nullInputIsReturnedAsNull() {
    assertThat(PropertyValueTypeInference.castToInferredType(null)).isNull();
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 0, 1, 100})
  void inferNumberValues(final int value) {
    final Object result = PropertyValueTypeInference.castToInferredType(String.valueOf(value));

    assertThat(result).isEqualTo(value);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void inferNumberValues(final boolean value) {
    assertThat(
            PropertyValueTypeInference.castToInferredType(
                String.valueOf(value).toLowerCase(Locale.ROOT)))
        .isEqualTo(value);
    assertThat(
            PropertyValueTypeInference.castToInferredType(
                String.valueOf(value).toUpperCase(Locale.ROOT)))
        .isEqualTo(value);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "string"})
  void inferStringValue(final String value) {
    assertThat(PropertyValueTypeInference.castToInferredType(value)).isEqualTo(value);
  }
}
