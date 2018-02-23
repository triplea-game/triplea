package games.strategy.engine.data.gameparser;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.xml.TestAttachment;
import games.strategy.triplea.attachments.CanalAttachment;
import games.strategy.triplea.delegate.BattleDelegate;


/**
 * Simple test of XmlGameElementMapper to verify error handling and a quick check of the happy case.
 */
public class XmlGameElementMapperTest {
  private static final String NAME_THAT_DOES_NOT_EXIST = "this is surely not a valid identifier";

  private XmlGameElementMapper testObj;

  @BeforeAll
  public static void disableErrorPopup() {
    ClientLogger.disableErrorPopupForTesting();
  }

  @AfterAll
  public static void resetErrorPopup() {
    // we do this to make sure any other tests will show the popup and have a similar method
    // call. We do not want test ordering to be important.
    ClientLogger.resetErrorPopupForTesting();
  }

  @BeforeEach
  public void setup() {
    testObj = new XmlGameElementMapper();
  }

  @Test
  public void getDelegateReturnsEmptyIfDelegateNameDoesNotExist() {
    final Optional<IDelegate> resultObject = testObj.getDelegate(NAME_THAT_DOES_NOT_EXIST);
    assertThat(resultObject.isPresent(), is(false));
  }


  @Test
  public void getDelegateHappyCase() {
    final Optional<IDelegate> resultObject = testObj.getDelegate(XmlGameElementMapper.BATTLE_DELEGATE_NAME);
    assertThat(resultObject.isPresent(), is(true));
    assertThat(resultObject.get(), instanceOf(BattleDelegate.class));
  }


  @Test
  public void getAttachmentReturnsEmptyIfAttachmentNameDoesNotExist() {
    final Optional<IAttachment> resultObject = testObj.getAttachment(NAME_THAT_DOES_NOT_EXIST, "", null, null);
    assertThat(resultObject.isPresent(), is(false));
  }


  @Test
  public void getAttachmentHappyCase() {
    final Optional<IAttachment> resultObject = testObj.getAttachment("CanalAttachment", "", null, null);
    assertThat(resultObject.isPresent(), is(true));
    assertThat(resultObject.get(), instanceOf(CanalAttachment.class));
  }

  @Test
  public void testFullClassNames() {
    final Optional<IAttachment> result1 =
        testObj.getAttachment("games.strategy.engine.xml.TestAttachment", "", null, null);
    assertThat(result1.isPresent(), is(true));
    assertThat(result1.get(), is(instanceOf(TestAttachment.class)));


    final Optional<IAttachment> result2 =
        testObj.getAttachment("games.strategy.triplea.attachments.CanalAttachment", "", null, null);
    assertThat(result2.isPresent(), is(true));
    assertThat(result2.get(), is(instanceOf(CanalAttachment.class)));
  }
}
