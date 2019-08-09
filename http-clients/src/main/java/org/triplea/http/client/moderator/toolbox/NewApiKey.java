package org.triplea.http.client.moderator.toolbox;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/** Simple object to support JSON encoding of an API key sent to client from server. */
@Getter
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class NewApiKey {
  private String apiKey;
}
