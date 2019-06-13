package org.triplea.http.client.moderator.toolbox;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * JSON data object for paging parameters, used when a table fetch will load more data than
 * we woudl want.
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PagingParams {
  /**
   * The next row to fetch, zero based.
   */
  private int rowNumber;

  /**
   * The number of rows to be fetched.
   */
  private int pageSize;
}
