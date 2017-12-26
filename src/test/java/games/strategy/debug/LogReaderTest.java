package games.strategy.debug;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.PrintStream;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.junit.experimental.extensions.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

@ExtendWith(MockitoExtension.class)
public class LogReaderTest {

  @Mock
  private PrintStream stream;
  @Mock
  private JTextArea area;
  @Mock
  private GenericConsole console;

  @Test
  public void testStreamSplittingArray() throws Exception {
    final LogReader reader = new LogReader(stream, area, Boolean.TRUE::booleanValue, console);
    final String testString = "Some Test String";
    final byte[] testByteArray = testString.getBytes();
    reader.getStream().write(testByteArray);
    verify(stream).write(testByteArray, 0, testByteArray.length);
    SwingUtilities.invokeAndWait(() -> verify(area).append(testString));
  }

  @Test
  public void testStreamSplittingInt() throws Exception {
    final LogReader reader = new LogReader(stream, area, Boolean.TRUE::booleanValue, console);
    final String testString = " ";
    final int testInt = testString.codePointAt(0);
    reader.getStream().write(testInt);
    verify(stream).write(testInt);
    SwingUtilities.invokeAndWait(() -> verify(area).append(" "));
  }

  @Test
  public void testWriteInvisible() throws Exception {
    final LogReader reader = new LogReader(stream, area, Boolean.FALSE::booleanValue, console);
    final String testString = "Some Test String";
    reader.getStream().write(testString.getBytes());
    SwingUtilities.invokeAndWait(() -> verify(area).append(testString));
  }

  @Test
  public void testConsolePopup() throws Exception {
    final String testString = "Some Test String";
    new LogReader(stream, area, Boolean.TRUE::booleanValue, console).getStream().write(testString.getBytes());
    new LogReader(stream, area, Boolean.FALSE::booleanValue, console).getStream().write(testString.getBytes());
    SwingUtilities.invokeAndWait(() -> {
      verify(console).setVisible(true);
      verify(console, times(0)).setVisible(false);
    });
  }
}
