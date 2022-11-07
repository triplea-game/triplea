package games.strategy.engine.data.gameparser;

import com.google.common.primitives.Ints;
import lombok.experimental.UtilityClass;

@UtilityClass
class PropertyValueTypeInference {

  @SuppressWarnings("UnstableApiUsage")
  public Class<?> inferType(final String value) {
    if (value == null) {
      return String.class;
    } else if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
      return Boolean.class;
    } else if (Ints.tryParse(value) != null) {
      return Integer.class;
    } else {
      return String.class;
    }
  }

  /**
   * Given an input, infers the type of value that is provided and returns casted object of
   * equivalent value cast to an appropriate type.
   *
   * @return
   *     <ul>
   *       <li>An 'int' type if the value is a number.
   *       <li>Boolean (case) insensitive if string is 'true' or 'false'
   *       <li>Otherwise returns the parameter value (string)
   *     </ul>
   */
  public Object castToInferredType(final String value) {
    final Class<?> inferredType = inferType(value);
    if (inferredType == Boolean.class) {
      return Boolean.parseBoolean(value);
    } else if (inferredType == Integer.class) {
      return Integer.parseInt(value);
    } else {
      return value;
    }
  }
}
