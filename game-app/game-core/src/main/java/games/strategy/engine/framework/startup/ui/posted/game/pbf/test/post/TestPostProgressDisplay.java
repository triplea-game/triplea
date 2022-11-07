package games.strategy.engine.framework.startup.ui.posted.game.pbf.test.post;

public interface TestPostProgressDisplay {

  void showSuccess(String message);

  void showFailure(Throwable throwable);

  void close();
}
