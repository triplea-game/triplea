package games.strategy.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;

/**
 * Designed to remove/replace the following illegal file name characters:
 *
 * <pre>
 * / \b \n \r \t \0 \f ` ? * \ < > | " ' : . , ^ [ ] = + ;
 * </pre>
 */
public final class IllegalCharacterRemover {
  @VisibleForTesting
  static final String ILLEGAL_CHARACTERS = "/\b\n\r\t\0\f`?*\\<>|\"\':.,^[]=+;";

  private static final CharMatcher ILLEGAL_CHAR_MATCHER = CharMatcher.anyOf(ILLEGAL_CHARACTERS);

  private IllegalCharacterRemover() {}

  /**
   * Designed to remove / \b \n \r \t \0 \f ` ? * \ < > | " ' : . , ^ [ ] = + ;
   */
  public static String removeIllegalCharacter(final String text) {
    return ILLEGAL_CHAR_MATCHER.removeFrom(text);
  }

  /**
   * Designed to replace / \b \n \r \t \0 \f ` ? * \ < > | " ' : . , ^ [ ] = + ;
   */
  public static String replaceIllegalCharacter(final String text, final char replacement) {
    return ILLEGAL_CHAR_MATCHER.replaceFrom(text, replacement);
  }
}
