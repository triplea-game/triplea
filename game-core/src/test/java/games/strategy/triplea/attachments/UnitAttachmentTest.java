package games.strategy.triplea.attachments;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.PlayerList;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.UnitTypeList;
import java.security.SecureRandom;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Tuple;

class UnitAttachmentTest {

  private final GameData gameData = mock(GameData.class);
  private final UnitAttachment attachment = new UnitAttachment("Test attachment", null, gameData);
  private final UnitTypeList unitTypeList = mock(UnitTypeList.class);
  private final PlayerList playerList = mock(PlayerList.class);

  private final String player1String = "Player1";
  private final String player2String = "Player2";
  private final String unit1String = "Unit1";
  private final String unit2String = "Unit2";

  private final GamePlayer player1 = mock(GamePlayer.class);
  private final GamePlayer player2 = mock(GamePlayer.class);
  private final UnitType unit1 = mock(UnitType.class);
  private final UnitType unit2 = mock(UnitType.class);

  @BeforeEach
  void setup() {
    when(gameData.getUnitTypeList()).thenReturn(unitTypeList);
    when(gameData.getPlayerList()).thenReturn(playerList);
    when(unitTypeList.getUnitType(unit1String)).thenReturn(unit1);
    when(unitTypeList.getUnitType(unit2String)).thenReturn(unit2);
    when(playerList.getPlayerId(player1String)).thenReturn(player1);
    when(playerList.getPlayerId(player2String)).thenReturn(player2);
  }

  @Test
  void testSetWhenCapturedChangesInto_invalidLength() {
    assertThrows(GameParseException.class, () -> attachment.setWhenCapturedChangesInto(""));
    assertThrows(GameParseException.class, () -> attachment.setWhenCapturedChangesInto(":"));
    assertThrows(GameParseException.class, () -> attachment.setWhenCapturedChangesInto("::"));
    assertThrows(GameParseException.class, () -> attachment.setWhenCapturedChangesInto(":::"));
    assertThrows(GameParseException.class, () -> attachment.setWhenCapturedChangesInto(":::::"));
    assertThrows(GameParseException.class, () -> attachment.setWhenCapturedChangesInto(":::::::"));

    assertThrows(GameParseException.class, () -> attachment.setWhenCapturedChangesInto("a"));
    assertThrows(GameParseException.class, () -> attachment.setWhenCapturedChangesInto("a:b"));
    assertThrows(GameParseException.class, () -> attachment.setWhenCapturedChangesInto("a:b:c"));
    assertThrows(GameParseException.class, () -> attachment.setWhenCapturedChangesInto("a:b:c:d"));
    assertThrows(
        GameParseException.class, () -> attachment.setWhenCapturedChangesInto("a:b:c:d:e:f"));
    assertThrows(
        GameParseException.class, () -> attachment.setWhenCapturedChangesInto("a:b:c:d:e:f:g:h"));
  }

  @Test
  void testSetWhenCapturedChangesInto_invalidArgs() {
    assertThrows(
        GameParseException.class,
        () -> attachment.setWhenCapturedChangesInto("NOT A PLAYER:any:true:Unit1:1"));
    // Testing for fail-fast here
    verify(playerList).getPlayerId("NOT A PLAYER");
    verify(playerList).getPlayerId(any());
    verify(unitTypeList, times(0)).getUnitType(any());
    assertThrows(
        GameParseException.class,
        () -> attachment.setWhenCapturedChangesInto("any:NOT A PLAYER:false:Unit2:1"));
    verify(playerList, times(2)).getPlayerId("NOT A PLAYER");
    verify(playerList).getPlayerId("any");
    verify(unitTypeList, times(0)).getUnitType(any());
    assertThrows(
        IllegalArgumentException.class,
        () -> attachment.setWhenCapturedChangesInto("Player1:any:NOT A BOOLEAN:Unit1:1"));
    verify(playerList).getPlayerId("Player1");
    verify(playerList, times(2)).getPlayerId("any");
    verify(unitTypeList, times(0)).getUnitType(any());
    assertThrows(
        GameParseException.class,
        () -> attachment.setWhenCapturedChangesInto("any:Player2:true:NOT A UNIT:1"));
    verify(playerList).getPlayerId("Player1");
    verify(playerList, times(3)).getPlayerId("any");
    verify(unitTypeList).getUnitType("NOT A UNIT");
    verify(unitTypeList).getUnitType(any());
    assertThrows(
        IllegalArgumentException.class,
        () -> attachment.setWhenCapturedChangesInto("any:any:false:Unit2:NOT A NUMBER"));
    verify(unitTypeList).getUnitType(unit2String);
    verify(unitTypeList, times(2)).getUnitType(any());
    verify(playerList, times(5)).getPlayerId("any");

    assertThrows(
        GameParseException.class, () -> attachment.setWhenCapturedChangesInto("q:w:e:r:t"));
    assertThrows(
        GameParseException.class, () -> attachment.setWhenCapturedChangesInto("q:w:e:r:t:z:u"));
  }

  @Test
  void testSetWhenCapturedChangesInto() throws Exception {
    final SecureRandom rand = new SecureRandom();

    final String from = "any";
    final String to = "any";
    final String trueString = Boolean.toString(true);
    final String falseString = Boolean.toString(false);

    final Map<String, Tuple<String, IntegerMap<UnitType>>> mapReference =
        attachment.getWhenCapturedChangesInto();

    final int random1 = rand.nextInt();
    final IntegerMap<UnitType> expected1 = new IntegerMap<>(ImmutableMap.of(unit1, random1));

    attachment.setWhenCapturedChangesInto(
        concatWithColon(from, to, trueString, unit1String, String.valueOf(random1)));
    assertEquals(Tuple.of(trueString, expected1), mapReference.get(from + ":" + to));

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
    assertEquals(Tuple.of(falseString, expected2), mapReference.get(from + ":" + to));
  }

  private static String concatWithColon(final String... args) {
    return String.join(":", args);
  }
}
