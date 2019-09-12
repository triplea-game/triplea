package games.strategy.engine.lobby;

import lombok.AllArgsConstructor;

/** Simple value object to encapsulate an API key and provide strong typing. */
@AllArgsConstructor(staticName = "of")
public class ApiKey {
  private final String apiKey;

  public String getValue() {
    return apiKey;
  }
}
