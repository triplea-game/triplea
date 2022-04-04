package org.triplea.game;

import lombok.experimental.UtilityClass;

/**
 * Class for generating various exceptions for the game client. Rather than creating many custom
 * exceptions for all specific scenarios, instead here we can make them more generic and re-use
 * existing text and debugging code.
 */
@UtilityClass
public class Exceptions {

  public static class MissingFile extends RuntimeException {
    public MissingFile(String path) {
      super("Unable to find file: " + path);
    }

    public MissingFile(String path, Exception e) {
      super("Unable to find file: " + path, e);
    }
  }
}
