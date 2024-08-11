package org.triplea.util;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;

/** A collection of useful methods for working with file names. */
public final class FileNameUtils {
  @NonNls @VisibleForTesting
  static final String ILLEGAL_CHARACTERS = "/\b\n\r\t\0\f`?*\\<>|\"':.,^[]=+;";

  private static final CharMatcher ILLEGAL_CHAR_MATCHER = CharMatcher.anyOf(ILLEGAL_CHARACTERS);

  private FileNameUtils() {}

  /**
   * Removes all illegal characters from the specified file name. Illegal file name characters
   * include:
   *
   * <pre>
   * / \b \n \r \t \0 \f ` ? * \ < > | " ' : . , ^ [ ] = + ;
   * </pre>
   *
   * @param fileName The file name.
   * @return The file name with all illegal characters removed.
   */
  public static String removeIllegalCharacters(final String fileName) {
    checkNotNull(fileName);

    return ILLEGAL_CHAR_MATCHER.removeFrom(fileName);
  }

  /**
   * Replaces all illegal characters in the specified file name with the specified replacement
   * character. Illegal file name characters include:
   *
   * <pre>
   * / \b \n \r \t \0 \f ` ? * \ < > | " ' : . , ^ [ ] = + ;
   * </pre>
   *
   * @param fileName The file name.
   * @param replacement The replacement character for any illegal file name character.
   * @return The file name with all illegal characters replaced with the specified replacement
   *     character.
   */
  public static String replaceIllegalCharacters(final String fileName, final char replacement) {
    checkNotNull(fileName);

    return ILLEGAL_CHAR_MATCHER.replaceFrom(fileName, replacement);
  }
}
