package org.triplea.generic.xml.reader.exceptions;

import java.lang.reflect.Field;

/**
 * Indicates an in the XML data that is being read. This exception type indicates that the XML has a
 * problem and the XML author needs to fix the problem.
 */
public class XmlDataException extends Exception {
  private static final long serialVersionUID = -2775932925722575706L;

  public XmlDataException(final Field field, final String message) {
    super("Bad XML data while setting field: " + field + ", " + message);
  }
}
