package org.triplea.generic.xml.reader.exceptions;

/**
 * Thrown if there are any generic errors opening or beginning to read an XML file. Such errors
 * would be IO problems, file permission problems, or similar system level issues unrelated to the
 * code configuration of XML content..
 */
public class XmlReadException extends RuntimeException {
  private static final long serialVersionUID = 575227372153503416L;

  public XmlReadException(final Throwable cause) {
    super("Unexpected error reading XML file: " + cause.getMessage(), cause);
  }
}
