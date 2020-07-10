package games.strategy.engine.chat;

import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ChatPlayerPanelTest {

  @Mock private PlayerChatRenderer renderer;

  @Test
  public void setPlayerRendererWhenChatIsNull() {

    final ChatPlayerPanel chatPlayerPanel = new ChatPlayerPanel(null);

    chatPlayerPanel.setPlayerRenderer(renderer);

    verifyNoInteractions(renderer);
  }
}
