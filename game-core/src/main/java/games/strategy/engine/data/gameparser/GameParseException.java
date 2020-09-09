package games.strategy.engine.data.gameparser;

import java.util.Collection;
import org.xml.sax.SAXParseException;

/** A checked exception that indicates an error occurred while parsing a map. */
public final class GameParseException extends Exception {
  private static final long serialVersionUID = 4015574053053781872L;

  public GameParseException(final String message) {
    super(message);
  }

  public GameParseException(final Throwable cause) {
    super(cause);
  }

  public GameParseException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public GameParseException(final Collection<SAXParseException> parsingErrors) {
    super(formatErrorMessage(parsingErrors));
  }

  private static String formatErrorMessage(final Collection<SAXParseException> parsingErrors) {
    final StringBuilder errorMessage = new StringBuilder();
    parsingErrors.forEach(
        error ->
            errorMessage.append(
                "SAXParseException: Line: "
                    + error.getLineNumber()
                    + ", column: "
                    + error.getColumnNumber()
                    + ", error: "
                    + error.getMessage()));
    return errorMessage.toString();
  }
}
