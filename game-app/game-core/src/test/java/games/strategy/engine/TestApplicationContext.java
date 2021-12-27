package games.strategy.engine;

import org.triplea.game.ApplicationContext;

public class TestApplicationContext implements ApplicationContext {
  @Override
  public Class<?> getMainClass() {
    return TestApplicationContext.class;
  }
}
