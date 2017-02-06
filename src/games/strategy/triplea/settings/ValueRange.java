package games.strategy.triplea.settings;

/**
 * Encapsulates the permissible range for the value of a specific setting.
 *
 * <p>
 * Implementations may choose to track other associated values in addition to the range itself, such as a default value.
 * </p>
 */
public interface ValueRange {
  /**
   * Gets a meaningful textual description of the value range.
   *
   * <p>
   * The returned text should be a complete (but concise) description of the value range. It may include such values as
   * boundary conditions, default values, etc. The returned text may span multiple lines and will be rendered
   * appropriately.
   * </p>
   *
   * @return A meaningful textual description of the value range; never {@code null}.
   */
  String getDescription();
}
