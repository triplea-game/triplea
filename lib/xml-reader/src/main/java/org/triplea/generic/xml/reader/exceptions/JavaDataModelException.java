package org.triplea.generic.xml.reader.exceptions;

import java.lang.reflect.Field;

/**
 * Indicates an error in how the java model code is structured. This exception type indicates that
 * the Java code has a problem and needs to be fixed by a developer.
 */
public class JavaDataModelException extends RuntimeException {
  private static final long serialVersionUID = -2632524904380838428L;

  public <T> JavaDataModelException(final Field field, final String message) {
    super("Error in field: " + field + ", " + message);
  }

  public <T> JavaDataModelException(final String message, final Throwable cause) {
    super("Error in Java XML model code, " + message, cause);
  }

  public <T> JavaDataModelException(final String message) {
    super("Error in Java XML model code, " + message);
  }

  public <T> JavaDataModelException(final Field field, final String message, final Throwable e) {
    super("Error in Java XML model code, field: " + field + ", " + message, e);
  }
}
