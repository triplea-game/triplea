package games.strategy.triplea.settings;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.example.mockito.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ClientSettingTest extends AbstractClientSettingTestCase {
  @Mock
  private Consumer<String> mockSaveListener;

  @Test
  public void saveActionListenerIsCalled() {
    ClientSetting.TEST_SETTING.addSaveListener(mockSaveListener);

    final String value = "value";
    ClientSetting.TEST_SETTING.save(value);

    Mockito.verify(mockSaveListener, Mockito.times(1))
        .accept(value);
  }
}
