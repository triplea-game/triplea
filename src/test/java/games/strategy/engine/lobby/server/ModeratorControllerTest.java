package games.strategy.engine.lobby.server;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import org.junit.experimental.extensions.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import games.strategy.net.INode;

@ExtendWith(MockitoExtension.class)
public final class ModeratorControllerTest {
  @Mock
  private INode node;

  @Test
  public void getUsernameForNode_ShouldReturnUsernameUnchangedWhenSuffixAbsent() {
    when(node.getName()).thenReturn("username");

    assertThat(ModeratorController.getUsernameForNode(node), is("username"));
  }

  @Test
  public void getUsernameForNode_ShouldReturnUsernameWithoutSuffixWhenSuffixPresent() {
    when(node.getName()).thenReturn("username (1)");

    assertThat(ModeratorController.getUsernameForNode(node), is("username"));
  }
}
