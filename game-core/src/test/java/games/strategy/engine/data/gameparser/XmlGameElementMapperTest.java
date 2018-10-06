package games.strategy.engine.data.gameparser;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import games.strategy.engine.data.IAttachment;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.xml.TestAttachment;
import games.strategy.triplea.attachments.CanalAttachment;
import games.strategy.triplea.delegate.BattleDelegate;


/**
 * Simple test of XmlGameElementMapper to verify error handling and a quick check of the happy case.
 */
class XmlGameElementMapperTest {
  private static final String NAME_THAT_DOES_NOT_EXIST = "this is surely not a valid identifier";

  private XmlGameElementMapper testObj;

  @BeforeEach
  void setup() {
    testObj = new XmlGameElementMapper();
  }

  @Test
  void getDelegateReturnsEmptyIfDelegateNameDoesNotExist() {
    final Optional<IDelegate> resultObject = testObj.getDelegate(NAME_THAT_DOES_NOT_EXIST);
    assertThat(resultObject, isEmpty());
  }


  @Test
  void getDelegateHappyCase() {
    final Optional<IDelegate> resultObject = testObj.getDelegate("games.strategy.triplea.delegate.BattleDelegate");
    assertThat(resultObject, isPresent());
    assertThat(resultObject.get(), instanceOf(BattleDelegate.class));
  }


  @Test
  void getAttachmentReturnsEmptyIfAttachmentNameDoesNotExist() {
    final Optional<IAttachment> resultObject = testObj.getAttachment(NAME_THAT_DOES_NOT_EXIST, "", null, null);
    assertThat(resultObject, isEmpty());
  }


  @Test
  void getAttachmentHappyCase() {
    final Optional<IAttachment> resultObject = testObj.getAttachment("CanalAttachment", "", null, null);
    assertThat(resultObject, isPresent());
    assertThat(resultObject.get(), instanceOf(CanalAttachment.class));
  }

  @Test
  void testFullClassNames() {
    final Optional<IAttachment> result1 =
        testObj.getAttachment("games.strategy.engine.xml.TestAttachment", "", null, null);
    assertThat(result1, isPresent());
    assertThat(result1.get(), is(instanceOf(TestAttachment.class)));


    final Optional<IAttachment> result2 =
        testObj.getAttachment("games.strategy.triplea.attachments.CanalAttachment", "", null, null);
    assertThat(result2, isPresent());
    assertThat(result2.get(), is(instanceOf(CanalAttachment.class)));
  }
}
