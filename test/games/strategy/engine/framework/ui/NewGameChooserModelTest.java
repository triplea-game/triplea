package games.strategy.engine.framework.ui;

import org.junit.Test;
import org.mockito.Mock;

public class NewGameChooserModelTest  {

  @Mock
  private ClearGameChooserCacheMessenger mockClearGameChooserCacheMessenger;

  /** Simply create the object to see that we can do that without exception */
  @SuppressWarnings("unused")
  @Test
  public void testCreate() {
    new NewGameChooserModel(mockClearGameChooserCacheMessenger);
  }
}
