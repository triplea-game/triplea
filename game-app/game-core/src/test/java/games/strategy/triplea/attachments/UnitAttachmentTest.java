package games.strategy.triplea.attachments;

import static games.strategy.triplea.Constants.AIR_ATTACK_SUB_RESTRICTED;
import static games.strategy.triplea.Constants.IGNORE_SUB_IN_MOVEMENT;
import static games.strategy.triplea.Constants.SUBMERSIBLE_SUBS;
import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.PlayerList;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.UnitTypeList;
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.engine.data.properties.BooleanProperty;
import games.strategy.engine.data.properties.GameProperties;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Tuple;

@ExtendWith(MockitoExtension.class)
class UnitAttachmentTest {

  private final GameData gameData = mock(GameData.class);
  private final UnitAttachment attachment = new UnitAttachment("Test attachment", null, gameData);

  @Mock private UnitTypeList unitTypeList;

  @BeforeEach
  void setUp() {
    when(gameData.getUnitTypeList()).thenReturn(unitTypeList);
  }

  @Nested
  class WhenCapturedChangesInto {

    @Mock private PlayerList playerList;

    private final String player1String = "Player1";
    private final String player2String = "Player2";
    private final String unit1String = "Unit1";
    private final String unit2String = "Unit2";

    @Mock private GamePlayer player1;
    @Mock private GamePlayer player2;
    @Mock private UnitType unit1;
    @Mock private UnitType unit2;

    @BeforeEach
    void setUp() {
      when(gameData.getUnitTypeList()).thenReturn(unitTypeList);
      when(gameData.getPlayerList()).thenReturn(playerList);
      lenient().when(unitTypeList.getUnitType(unit1String)).thenReturn(Optional.of(unit1));
      lenient().when(unitTypeList.getUnitType(unit2String)).thenReturn(Optional.of(unit2));
      lenient().when(playerList.getPlayerId(player1String)).thenReturn(player1);
      lenient().when(playerList.getPlayerId(player2String)).thenReturn(player2);
    }

    @Test
    void setWhenCapturedChangesIntoWithInvalidLength() {
      assertThrows(GameParseException.class, () -> attachment.setWhenCapturedChangesInto(""));
      assertThrows(GameParseException.class, () -> attachment.setWhenCapturedChangesInto(":"));
      assertThrows(GameParseException.class, () -> attachment.setWhenCapturedChangesInto("::"));
      assertThrows(GameParseException.class, () -> attachment.setWhenCapturedChangesInto(":::"));
      assertThrows(GameParseException.class, () -> attachment.setWhenCapturedChangesInto(":::::"));
      assertThrows(
          GameParseException.class, () -> attachment.setWhenCapturedChangesInto(":::::::"));

      assertThrows(GameParseException.class, () -> attachment.setWhenCapturedChangesInto("a"));
      assertThrows(GameParseException.class, () -> attachment.setWhenCapturedChangesInto("a:b"));
      assertThrows(GameParseException.class, () -> attachment.setWhenCapturedChangesInto("a:b:c"));
      assertThrows(
          GameParseException.class, () -> attachment.setWhenCapturedChangesInto("a:b:c:d"));
      assertThrows(
          GameParseException.class, () -> attachment.setWhenCapturedChangesInto("a:b:c:d:e:f"));
      assertThrows(
          GameParseException.class, () -> attachment.setWhenCapturedChangesInto("a:b:c:d:e:f:g:h"));
    }

    @Test
    void setWhenCapturedChangesIntoWithInvalidArgs() {
      assertThrows(
          GameParseException.class,
          () -> attachment.setWhenCapturedChangesInto("NOT A PLAYER:any:true:Unit1:1"));
      // Testing for fail-fast here
      verify(playerList).getPlayerId("NOT A PLAYER");
      verify(unitTypeList, times(0)).getUnitTypeOrThrow(any());
      assertThrows(
          GameParseException.class,
          () -> attachment.setWhenCapturedChangesInto("any:NOT A PLAYER:false:Unit2:1"));
      verify(playerList, times(2)).getPlayerId("NOT A PLAYER");
      verify(unitTypeList, times(0)).getUnitTypeOrThrow(any());
      assertThrows(
          IllegalArgumentException.class,
          () -> attachment.setWhenCapturedChangesInto("Player1:any:NOT A BOOLEAN:Unit1:1"));
      verify(playerList).getPlayerId("Player1");
      verify(unitTypeList, times(0)).getUnitTypeOrThrow(any());
      assertThrows(
          GameParseException.class,
          () -> attachment.setWhenCapturedChangesInto("any:Player2:true:NOT A UNIT:1"));
      verify(playerList).getPlayerId("Player1");
      verify(unitTypeList).getUnitType("NOT A UNIT");
      verify(unitTypeList).getUnitType(any());
      assertThrows(
          IllegalArgumentException.class,
          () -> attachment.setWhenCapturedChangesInto("any:any:false:Unit2:NOT A NUMBER"));
      verify(unitTypeList).getUnitType(unit2String);
      verify(unitTypeList, times(2)).getUnitType(any());

      assertThrows(
          GameParseException.class, () -> attachment.setWhenCapturedChangesInto("q:w:e:r:t"));
      assertThrows(
          GameParseException.class, () -> attachment.setWhenCapturedChangesInto("q:w:e:r:t:z:u"));
    }

    @Test
    void setWhenCapturedChangesInto() throws Exception {
      final SecureRandom rand = new SecureRandom();

      final String from = "any";
      final String to = "any";
      final String trueString = Boolean.toString(true);
      final String falseString = Boolean.toString(false);

      final int random1 = rand.nextInt();
      final IntegerMap<UnitType> expected1 = new IntegerMap<>(ImmutableMap.of(unit1, random1));

      attachment.setWhenCapturedChangesInto(
          concatWithColon(from, to, trueString, unit1String, String.valueOf(random1)));
      assertEquals(
          Tuple.of(trueString, expected1),
          attachment.getWhenCapturedChangesInto().get(from + ":" + to));

      final int random2 = rand.nextInt();
      final int random3 = rand.nextInt();
      final IntegerMap<UnitType> expected2 = new IntegerMap<>();
      expected2.put(unit1, random2);
      expected2.put(unit2, random3);

      attachment.setWhenCapturedChangesInto(
          concatWithColon(
              from,
              to,
              falseString,
              unit1String,
              String.valueOf(random2),
              unit2String,
              String.valueOf(random3)));
      assertEquals(
          Tuple.of(falseString, expected2),
          attachment.getWhenCapturedChangesInto().get(from + ":" + to));
    }

    private String concatWithColon(final String... args) {
      return String.join(":", args);
    }
  }

  @Nested
  class IsSub {

    @Test
    void relatedPropertiesAreSet() throws MutableProperty.InvalidValueException {

      final UnitType sub = mock(UnitType.class);
      when(sub.getAttachment(UNIT_ATTACHMENT_NAME)).thenReturn(mock(UnitAttachment.class));
      final UnitType air = mock(UnitType.class);
      final UnitAttachment airAttachment = mock(UnitAttachment.class);
      when(airAttachment.isAir()).thenReturn(true);
      when(air.getAttachment(UNIT_ATTACHMENT_NAME)).thenReturn(airAttachment);
      when(unitTypeList.getAllUnitTypes()).thenReturn(Set.of(sub, air));

      final GameProperties properties = new GameProperties(gameData);
      when(gameData.getProperties()).thenReturn(properties);
      final BooleanProperty submersibleSubs = new BooleanProperty(SUBMERSIBLE_SUBS, "", true);
      properties.addEditableProperty(submersibleSubs);
      final BooleanProperty ignoreSubInMovement =
          new BooleanProperty(IGNORE_SUB_IN_MOVEMENT, "", true);
      properties.addEditableProperty(ignoreSubInMovement);
      final BooleanProperty airAttackSubRestricted =
          new BooleanProperty(AIR_ATTACK_SUB_RESTRICTED, "", true);
      properties.addEditableProperty(airAttackSubRestricted);

      attachment.getPropertyOrThrow("isSub").setValue(true);

      assertThat(attachment.getIsFirstStrike(), is(true));
      assertThat(attachment.getCanEvade(), is(true));
      assertThat(attachment.getCanMoveThroughEnemies(), is(true));
      assertThat(attachment.getCanBeMovedThroughByEnemies(), is(true));
      assertThat(attachment.getCanNotTarget(), containsInAnyOrder(air));
      assertThat(attachment.getCanNotBeTargetedBy(), containsInAnyOrder(air));
    }

    @Test
    void mapPropertyChangesAreReflected() throws MutableProperty.InvalidValueException {

      final GameProperties properties = new GameProperties(gameData);
      when(gameData.getProperties()).thenReturn(properties);
      final BooleanProperty submersibleSubs = new BooleanProperty(SUBMERSIBLE_SUBS, "", true);
      properties.addEditableProperty(submersibleSubs);
      final BooleanProperty ignoreSubInMovement =
          new BooleanProperty(IGNORE_SUB_IN_MOVEMENT, "", true);
      properties.addEditableProperty(ignoreSubInMovement);
      final BooleanProperty airAttackSubRestricted =
          new BooleanProperty(AIR_ATTACK_SUB_RESTRICTED, "", true);
      properties.addEditableProperty(airAttackSubRestricted);

      attachment.getPropertyOrThrow("isSub").setValue(true);

      // change the properties after isSub is set
      submersibleSubs.setValue(false);
      ignoreSubInMovement.setValue(false);
      airAttackSubRestricted.setValue(false);

      assertThat(attachment.getCanMoveThroughEnemies(), is(false));
      assertThat(attachment.getCanBeMovedThroughByEnemies(), is(false));
      assertThat(attachment.getCanNotBeTargetedBy(), is(empty()));

      // called in getCanNotBeTargetedBy when AIR_ATTACK_SUB_RESTRICTED is true
      verify(unitTypeList, never()).getAllUnitTypes();
    }
  }
}
