package org.triplea.swing;

import static org.mockito.Mockito.verify;

import java.awt.event.MouseEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MouseListenerBuilderTest {

  @Mock Runnable runner;
  @Mock MouseEvent mouseEvent;

  @Test
  void mouseClickedRunnable() {
    new MouseListenerBuilder().mouseClicked(runner).build().mouseClicked(mouseEvent);
    verify(runner).run();
  }

  @Test
  void mouseClickedConsumer() {
    new MouseListenerBuilder().mouseClicked(e -> runner.run()).build().mouseClicked(mouseEvent);
    verify(runner).run();
  }

  @Test
  void mousePressedRunnable() {
    new MouseListenerBuilder().mousePressed(runner).build().mousePressed(mouseEvent);
    verify(runner).run();
  }

  @Test
  void mousePressedConsumer() {
    new MouseListenerBuilder().mousePressed(e -> runner.run()).build().mousePressed(mouseEvent);
    verify(runner).run();
  }

  @Test
  void mouseReleasedRunnable() {
    new MouseListenerBuilder().mouseReleased(runner).build().mouseReleased(mouseEvent);
    verify(runner).run();
  }

  @Test
  void mouseReleasedConsumer() {
    new MouseListenerBuilder().mouseReleased(e -> runner.run()).build().mouseReleased(mouseEvent);
    verify(runner).run();
  }

  @Test
  void mouseEnteredRunnable() {
    new MouseListenerBuilder().mouseEntered(runner).build().mouseEntered(mouseEvent);
    verify(runner).run();
  }

  @Test
  void mouseEnteredConsumer() {
    new MouseListenerBuilder().mouseEntered(e -> runner.run()).build().mouseEntered(mouseEvent);
    verify(runner).run();
  }

  @Test
  void mouseExitedRunnable() {
    new MouseListenerBuilder().mouseExited(runner).build().mouseExited(mouseEvent);
    verify(runner).run();
  }

  @Test
  void mouseExitedConsumer() {
    new MouseListenerBuilder().mouseExited(e -> runner.run()).build().mouseExited(mouseEvent);
    verify(runner).run();
  }
}
