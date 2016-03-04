package games.strategy.engine.framework.headlessGameServer;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import games.strategy.util.ThreadUtil;

@RunWith(MockitoJUnitRunner.class)
public class HeadlessGameServerConsoleTest {

  private HeadlessGameServerConsole testObj;

  @Mock
  private BufferedReader mockBufferedReader;

  @Mock
  private PrintStream mockPrintStream;

  @Mock
  private HeadlessConsoleController mockHeadlessConsoleController;

  @Before
  public void setUp() {
    testObj = new HeadlessGameServerConsole(mockBufferedReader, mockPrintStream, mockHeadlessConsoleController);
  }

  /**
   * Start the test object processing, it will read from the mock output and should
   * send to the mock controller. The mock controller then just needs to check
   * that it gets the input we send.
   */
  @Test
  public void testProcess() {
    final String testValueToSendThru = " some value ";
    try {
      when(mockBufferedReader.readLine()).thenReturn(testValueToSendThru + "\n").thenReturn(null);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    testObj.start();

    // console thread reads on another thread, sleep for a bit to give it a chance to read.
    ThreadUtil.sleep(HeadlessGameServerConsole.LOOP_SLEEP_MS * 5);
    verify( mockHeadlessConsoleController,times(1)).process(testValueToSendThru.trim());
  }


}
