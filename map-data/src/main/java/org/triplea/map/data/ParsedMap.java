package org.triplea.map.data;

import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.triplea.map.data.elements.AttachmentListTag;
import org.triplea.map.data.elements.InfoTag;
import org.triplea.map.data.elements.TripleaTag;

/**
 * Represents all of the org.triplea.map.data read from a map. The org.triplea.map.data is in a 'raw' form where we simply represent
 * the org.triplea.map.data as closely as possible as POJOs without semantic meaning.
 */
@Getter
@Builder(builderClassName = "Builder")
public class ParsedMap {
  private final InfoTag infoTag;
  private final TripleaTag tripleaTag;
  private final AttachmentListTag attachmentListTag;
}
