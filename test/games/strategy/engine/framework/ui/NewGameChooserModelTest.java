package games.strategy.engine.framework.ui;

import junit.framework.TestCase;

public class NewGameChooserModelTest extends TestCase {

  public void testCreate() {
    ClearGameChooserCacheMessenger messenger =  new ClearGameChooserCacheMessenger();
    final NewGameChooserModel model = new NewGameChooserModel(messenger);
    assertTrue(model.size() + "", model.size() > 0);
  }

}
