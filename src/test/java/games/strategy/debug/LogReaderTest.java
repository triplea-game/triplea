package games.strategy.debug;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.PrintStream;

import javax.swing.JTextArea;

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
  public void testStreamSplittingArray() throws IOException {
    final LogReader reader = new LogReader(stream, area, Boolean.TRUE::booleanValue, console);
    final String testString = "Some Test String";
    final byte[] testByteArray = testString.getBytes();
    reader.getStream().write(testByteArray);
    verify(stream).write(testByteArray, 0, testByteArray.length);
    verify(area).append(testString);
  }

  @Test
  public void testStreamSplittingInt() throws IOException {
    final LogReader reader = new LogReader(stream, area, Boolean.TRUE::booleanValue, console);
    final String testString = " ";
    final int testInt = testString.codePointAt(0);
    reader.getStream().write(testInt);
    verify(stream).write(testInt);
    verify(area).append(" ");
  }

  @Test
  public void testWriteInvisible() throws IOException {
    final LogReader reader = new LogReader(stream, area, Boolean.FALSE::booleanValue, console);
    final String testString = "Some Test String";
    reader.getStream().write(testString.getBytes());
    verify(area).append(testString);
  }

  @Test
  public void testConsolePopup() throws IOException {
    final String testString = "Some Test String";
    new LogReader(stream, area, Boolean.TRUE::booleanValue, console).getStream().write(testString.getBytes());
    new LogReader(stream, area, Boolean.FALSE::booleanValue, console).getStream().write(testString.getBytes());
    verify(console).setVisible(true);
    verify(console, times(0)).setVisible(false);
  }
}
