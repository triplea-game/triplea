package org.triplea.http.client.maps.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class UpdateMapTagRequest {
  private String mapName;
  private String tagName;
  private String newTagValue;
}
