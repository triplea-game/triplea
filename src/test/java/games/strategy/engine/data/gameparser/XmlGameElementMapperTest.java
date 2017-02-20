package games.strategy.engine.data.gameparser;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import games.strategy.engine.data.IAttachment;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.triplea.attachments.CanalAttachment;
import games.strategy.triplea.delegate.BattleDelegate;


/**
 * Simple test of XmlGameElementMapper to verify error handling and a quick check of the happy case
 */
public class XmlGameElementMapperTest {
  private static final String NAME_THAT_DOES_NOT_EXIST = "this is surely not a valid identifier";

  private XmlGameElementMapper testObj;

  @Before
  public void setup() {
    testObj = new XmlGameElementMapper();
  }

  @Test
  public void getDelegateReturnsEmptyIfDelegateNameDoesNotExist() {
    Optional<IDelegate> resultObject = testObj.getDelegate(NAME_THAT_DOES_NOT_EXIST);
    assertThat(resultObject.isPresent(), is(false));
  }


  @Test
  public void getDelegateHappyCase() {
    Optional<IDelegate> resultObject = testObj.getDelegate(XmlGameElementMapper.BATTLE_DELEGATE_NAME);
    assertThat(resultObject.isPresent(), is(true));
    assertThat(resultObject.get(), instanceOf(BattleDelegate.class));
  }


  @Test
  public void getAttachmentReturnsEmptyIfAttachmentNameDoesNotExist() {
    Optional<IAttachment> resultObject = testObj.getAttachment(NAME_THAT_DOES_NOT_EXIST, "", null, null);
    assertThat(resultObject.isPresent(), is(false));
  }


  @Test
  public void getAttachmentHappyCase() {
    Optional<IAttachment> resultObject = testObj.getAttachment(XmlGameElementMapper.CANAL_ATTACHMENT_NAME, "", null, null);
    assertThat(resultObject.isPresent(), is(true));
    assertThat(resultObject.get(), instanceOf(CanalAttachment.class));
  }
}
