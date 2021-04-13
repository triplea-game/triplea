package org.triplea.http.client.github;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * Represents the data returned by github API for their 'branches' endpoint. This class Presents a
 * simplified interface for what is otherwise a JSON response.
 */
@ToString
@AllArgsConstructor
@Getter
public class BranchInfoResponse {
  @SerializedName("commit")
  private final LastCommit lastCommit;

  /** Returns the date of the last commit. */
  public Instant getLastCommitDate() {
    return Instant.parse(lastCommit.commit.commitDetails.date);
  }

  @ToString
  @AllArgsConstructor
  @Getter
  private static class LastCommit {

    private final Commit commit;

    @ToString
    @AllArgsConstructor
    @Getter
    private static class Commit {
      @SerializedName("author")
      private final CommitDetails commitDetails;

      @ToString
      @AllArgsConstructor
      @Getter
      private static class CommitDetails {
        private final String date;
      }
    }
  }
}
