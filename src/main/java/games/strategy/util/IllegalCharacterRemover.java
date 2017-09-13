package games.strategy.util;

/**
 * Designed to remove/replace<br>
 * / \b \n \r \t \0 \f ` ? * \ < > | " ' : . , ^ [ ] = + ;
 */
public class IllegalCharacterRemover {
  private static final char[] ILLEGAL_CHARACTERS = {'/', '\b', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<',
      '>', '|', '\"', '\'', ':', '.', ',', '^', '[', ']', '=', '+', ';'};

  /**
   * Designed to remove / \b \n \r \t \0 \f ` ? * \ < > | " ' : . , ^ [ ] = + ;
   */
  public static String removeIllegalCharacter(final String text) {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < text.length(); ++i) {
      if (!isIllegalFileNameChar(text.charAt(i))) {
        sb.append(text.charAt(i));
      }
    }
    return sb.toString();
  }

  /**
   * Designed to replace / \b \n \r \t \0 \f ` ? * \ < > | " ' : . , ^ [ ] = + ;
   */
  public static String replaceIllegalCharacter(final String text, final char replacement) {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < text.length(); ++i) {
      if (!isIllegalFileNameChar(text.charAt(i))) {
        sb.append(text.charAt(i));
      } else {
        sb.append(replacement);
      }
    }
    return sb.toString();
  }

  private static boolean isIllegalFileNameChar(final char c) {
    boolean isIllegal = false;
    for (final char element : ILLEGAL_CHARACTERS) {
      if (c == element) {
        isIllegal = true;
        break;
      }
    }
    return isIllegal;
  }
}
