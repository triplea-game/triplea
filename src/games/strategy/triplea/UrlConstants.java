package games.strategy.triplea;

public enum UrlConstants {
  GITHUB_ISSUES("http://github.com/triplea-game/triplea/issues/new");


  private final String urlString;

  private UrlConstants(final String value) {
    this.urlString = value;
  }

  @Override
  public String toString() {
    return urlString;
  }
}
