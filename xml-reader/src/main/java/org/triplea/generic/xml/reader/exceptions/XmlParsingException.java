package org.triplea.generic.xml.reader.exceptions;

import javax.xml.stream.XMLStreamReader;

public class XmlParsingException extends Exception {
  private static final long serialVersionUID = -4759291721256502536L;

  public <T> XmlParsingException(
      final XMLStreamReader xmlStreamReader, final Class<T> pojo, final Throwable e) {
    super(
        String.format(
            "Parsing halted at line: %s, column: %s, while mapping to: %s, error: %s",
            xmlStreamReader.getLocation().getLineNumber(),
            xmlStreamReader.getLocation().getColumnNumber(),
            pojo.getCanonicalName(),
            e.getMessage()),
        e);
  }
}
