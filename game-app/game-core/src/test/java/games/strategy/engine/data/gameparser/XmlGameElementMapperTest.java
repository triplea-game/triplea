package games.strategy.engine.data.gameparser;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.TestAttachment;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.triplea.attachments.CanalAttachment;
import games.strategy.triplea.attachments.FixedReinforcementAttachment;
import games.strategy.triplea.attachments.SupplyTerritoryAttachment;
import games.strategy.triplea.delegate.TestDelegate;
import games.strategy.triplea.delegate.battle.BattleDelegate;
import games.strategy.triplea.delegate.reinforcement.FixedReinforcementDelegate;
import games.strategy.triplea.delegate.supply.SupplyAwareMoveDelegate;
import games.strategy.triplea.delegate.supply.SupplyDelegate;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class XmlGameElementMapperTest {
  private final XmlGameElementMapper xmlGameElementMapper = new XmlGameElementMapper();

  @Nested
  final class NewDelegateTest {
    @Test
    void shouldReturnDelegateWhenNamePresent() {
      final Optional<IDelegate> result = xmlGameElementMapper.newDelegate("BattleDelegate");

      assertThat(result, isPresent());
      assertThat(result.get(), is(instanceOf(BattleDelegate.class)));
    }

    @Test
    void shouldReturnDelegateWhenFullyQualifiedNamePresent() {
      final Optional<IDelegate> result =
          xmlGameElementMapper.newDelegate("games.strategy.triplea.delegate.BattleDelegate");

      assertThat(result, isPresent());
      assertThat(result.get(), is(instanceOf(BattleDelegate.class)));
    }

    @Test
    void shouldReturnDelegateWhenNamePresentInAuxiliaryMap() {
      final String typeName = "TestDelegate";
      final XmlGameElementMapper xmlGameElementMapper =
          new XmlGameElementMapper(Map.of(typeName, TestDelegate::new), Map.of());

      final Optional<IDelegate> result = xmlGameElementMapper.newDelegate(typeName);

      assertThat(result, isPresent());
      assertThat(result.get(), is(instanceOf(TestDelegate.class)));
    }

    @Test
    void shouldReturnEmptyWhenNameAbsent() {
      assertThat(xmlGameElementMapper.newDelegate("__unknown__"), isEmpty());
    }
  }

  @Nested
  final class NewAttachmentTest {
    @Test
    void shouldReturnAttachmentWhenNamePresent() {
      final Optional<IAttachment> result =
          xmlGameElementMapper.newAttachment("CanalAttachment", "", null, null);

      assertThat(result, isPresent());
      assertThat(result.get(), is(instanceOf(CanalAttachment.class)));
    }

    @Test
    void shouldReturnAttachmentWhenFullyQualifiedNamePresent() {
      final Optional<IAttachment> result =
          xmlGameElementMapper.newAttachment(
              "games.strategy.triplea.attachments.CanalAttachment", "", null, null);

      assertThat(result, isPresent());
      assertThat(result.get(), is(instanceOf(CanalAttachment.class)));
    }

    @Test
    void shouldReturnAttachmentWhenNamePresentInAuxiliaryMap() {
      final String typeName = "TestAttachment";
      final XmlGameElementMapper xmlGameElementMapper =
          new XmlGameElementMapper(Map.of(), Map.of(typeName, TestAttachment::new));

      final Optional<IAttachment> result =
          xmlGameElementMapper.newAttachment(typeName, "", null, null);

      assertThat(result, isPresent());
      assertThat(result.get(), is(instanceOf(TestAttachment.class)));
    }

    @Test
    void shouldReturnEmptyWhenNameAbsent() {
      assertThat(xmlGameElementMapper.newAttachment("__unknown__", "", null, null), isEmpty());
    }
  }

  /** Type names are spelled exactly as the Small Front map XML declares them. */
  @Nested
  final class SmallFrontTest {
    @Test
    void shouldReturnFixedReinforcementDelegate() {
      final Optional<IDelegate> result =
          xmlGameElementMapper.newDelegate(
              "games.strategy.triplea.delegate.FixedReinforcementDelegate");

      assertThat(result, isPresent());
      assertThat(result.get(), is(instanceOf(FixedReinforcementDelegate.class)));
    }

    @Test
    void shouldReturnSupplyDelegate() {
      final Optional<IDelegate> result =
          xmlGameElementMapper.newDelegate("games.strategy.triplea.delegate.SupplyDelegate");

      assertThat(result, isPresent());
      assertThat(result.get(), is(instanceOf(SupplyDelegate.class)));
    }

    @Test
    void shouldReturnSupplyAwareMoveDelegate() {
      final Optional<IDelegate> result =
          xmlGameElementMapper.newDelegate(
              "games.strategy.triplea.delegate.SupplyAwareMoveDelegate");

      assertThat(result, isPresent());
      assertThat(result.get(), is(instanceOf(SupplyAwareMoveDelegate.class)));
    }

    @Test
    void shouldReturnSupplyTerritoryAttachment() {
      final Optional<IAttachment> result =
          xmlGameElementMapper.newAttachment(
              "games.strategy.triplea.attachments.SupplyTerritoryAttachment", "", null, null);

      assertThat(result, isPresent());
      assertThat(result.get(), is(instanceOf(SupplyTerritoryAttachment.class)));
    }

    @Test
    void shouldReturnFixedReinforcementAttachment() {
      final Optional<IAttachment> result =
          xmlGameElementMapper.newAttachment(
              "games.strategy.triplea.attachments.FixedReinforcementAttachment", "", null, null);

      assertThat(result, isPresent());
      assertThat(result.get(), is(instanceOf(FixedReinforcementAttachment.class)));
    }
  }
}
