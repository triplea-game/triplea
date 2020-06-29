package games.strategy.engine.posted.game.pbf;

import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Value;

public interface ForumClient {

  /**
   * Returns a user id by user name, if user name is not found then returns an empty optional.
   *
   * @throws ForumPostingException Thrown if there are any errors communicating with forum.
   */
  GetUserIdResult getUserId(String userName);

  @Builder
  @Value
  class GetUserIdResult {
    @Nullable String errorMessage;
    @Nullable Integer userId;
  }
}
