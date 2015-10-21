package games.strategy.engine.framework.ui;

import org.junit.Test;

public class NewGameChooserModelTest  {


  /** Simply create the object to see that we can do that without exception */
  @SuppressWarnings("unused")
  @Test
  public void testCreate() {
    new NewGameChooserModel(new ClearGameChooserCacheMessenger());
  }
}
