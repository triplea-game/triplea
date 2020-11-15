package org.triplea.http.client.lobby.moderator.toolbox;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data object to encapsulate paging parameters. Notably the next row to be fetched and how many
 * rows are to be fetched.
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PagingParams {
  /** The next row to fetch, zero-based. */
  private int rowNumber;
  /** How many rows to fetch, should be greater than zero. */
  private Integer pageSize;
}
