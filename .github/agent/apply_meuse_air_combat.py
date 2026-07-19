#!/usr/bin/env python3
from __future__ import annotations

import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def replace_once(relative: str, old: str, new: str) -> None:
    path = ROOT / relative
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{relative}: expected one match, found {count}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def insert_before(relative: str, marker: str, addition: str) -> None:
    replace_once(relative, marker, addition + marker)


def patch_air_control() -> None:
    path = "game-app/game-core/src/main/java/games/strategy/triplea/delegate/battle/steps/change/RemoveNonCombatants.java"
    replace_once(
        path,
        """    final boolean contested = !offenseAircraft.isEmpty() && !defenseAircraft.isEmpty();
    final GamePlayer controller = resolveController(offenseAircraft, defenseAircraft);
    final Change change =
        contested
            ? AirControlTracker.changeContested(battleState.getBattleSite(), gameData)
            : AirControlTracker.changeControl(battleState.getBattleSite(), controller, gameData);
    if (change.isEmpty()) {
      return;
    }

    bridge.addChange(change);
    final String historyText =
        contested
            ? "Air control over " + battleState.getBattleSite().getName() + " is contested"
            : controller.getName()
                + " gains air control over "
                + battleState.getBattleSite().getName();
    bridge.getHistoryWriter().addChildToEvent(historyText);
""",
        """    final GamePlayer controller = resolveController(offenseAircraft, defenseAircraft);
    final Change change;
    final String historyText;
    if (controller == null) {
      change = AirControlTracker.changeContested(battleState.getBattleSite(), gameData);
      historyText = "Air control over " + battleState.getBattleSite().getName() + " is contested";
    } else {
      change = AirControlTracker.changeControl(battleState.getBattleSite(), controller, gameData);
      historyText =
          controller.getName()
              + " gains air control over "
              + battleState.getBattleSite().getName();
    }
    if (change.isEmpty()) {
      return;
    }

    bridge.addChange(change);
    bridge.getHistoryWriter().addChildToEvent(historyText);
""",
    )
    replace_once(
        path,
        """  private @Nullable GamePlayer resolveController(
      final Collection<Unit> offenseAircraft, final Collection<Unit> defenseAircraft) {
    if (!offenseAircraft.isEmpty() && defenseAircraft.isEmpty()) {
      return battleState.getPlayer(BattleState.Side.OFFENSE);
    }
    if (offenseAircraft.isEmpty() && !defenseAircraft.isEmpty()) {
      return battleState.getPlayer(BattleState.Side.DEFENSE);
    }
    return null;
  }
""",
        """  private @Nullable GamePlayer resolveController(
      final Collection<Unit> offenseAircraft, final Collection<Unit> defenseAircraft) {
    return resolveController(
        offenseAircraft,
        defenseAircraft,
        battleState.getPlayer(BattleState.Side.OFFENSE),
        battleState.getPlayer(BattleState.Side.DEFENSE));
  }

  static @Nullable GamePlayer resolveController(
      final Collection<Unit> offenseAircraft,
      final Collection<Unit> defenseAircraft,
      final GamePlayer offensePlayer,
      final GamePlayer defensePlayer) {
    if (offenseAircraft.size() > defenseAircraft.size()) {
      return offensePlayer;
    }
    if (defenseAircraft.size() > offenseAircraft.size()) {
      return defensePlayer;
    }
    return null;
  }
""",
    )

    tests = """
  @Test
  void survivingNumericalAirSuperiorityControlsTheTerritory() {
    final List<Unit> offenseAircraft = List.of(mock(Unit.class), mock(Unit.class));
    final List<Unit> defenseAircraft = List.of(mock(Unit.class));

    assertThat(
            RemoveNonCombatants.resolveController(
                offenseAircraft, defenseAircraft, attacker, defender))
        .isEqualTo(attacker);
  }

  @Test
  void equalSurvivingAirForcesRemainContested() {
    final List<Unit> offenseAircraft = List.of(mock(Unit.class));
    final List<Unit> defenseAircraft = List.of(mock(Unit.class));

    assertThat(
            RemoveNonCombatants.resolveController(
                offenseAircraft, defenseAircraft, attacker, defender))
        .isNull();
  }

  @Test
  void defendingNumericalAirSuperiorityControlsTheTerritory() {
    final List<Unit> offenseAircraft = List.of(mock(Unit.class));
    final List<Unit> defenseAircraft = List.of(mock(Unit.class), mock(Unit.class));

    assertThat(
            RemoveNonCombatants.resolveController(
                offenseAircraft, defenseAircraft, attacker, defender))
        .isEqualTo(defender);
  }

"""
    insert_before(
        "game-app/game-core/src/test/java/games/strategy/triplea/delegate/battle/steps/change/RemoveNonCombatantsTest.java",
        "}\n",
        tests,
    )


def patch_genxml() -> None:
    path = "maps/small_front_meuse/tools/genxml.py"
    replace_once(
        path,
        "'armour', 'mechanized', 'fighter']:",
        "'armour', 'mechanized', 'fighter', 'airfield']:",
    )
    replace_once(
        path,
        '      <option name="attack" value="2"/>\n'
        '      <option name="defense" value="3"/>\n'
        '      <option name="canBlitz" value="true"/>\n'
        '      <option name="artillerySupportable" value="true"/>\n'
        '      <option name="stackCost" value="2"/>\n'
        '      <option name="tuv" value="6"/>\n',
        '      <option name="attack" value="2"/>\n'
        '      <option name="attackRolls" value="2"/>\n'
        '      <option name="defense" value="3"/>\n'
        '      <option name="canBlitz" value="true"/>\n'
        '      <option name="artillerySupportable" value="true"/>\n'
        '      <option name="stackCost" value="2"/>\n'
        '      <option name="tuv" value="7"/>\n',
    )
    replace_once(
        path,
        '      <option name="canEscort" value="true"/>\n'
        '      <option name="airAttack" value="2"/>\n',
        '      <option name="canEscort" value="true"/>\n'
        '      <option name="canScramble" value="true"/>\n'
        '      <option name="maxScrambleDistance" value="2"/>\n'
        '      <option name="airAttack" value="2"/>\n',
    )
    insert_before(
        path,
        "'''\n\nREINFORCEMENTS_AND_SCORING = ",
        """    <attachment name="unitAttachment" attachTo="airfield" javaClass="games.strategy.triplea.attachments.UnitAttachment" type="unitType">
      <option name="movement" value="0"/>
      <option name="attack" value="0"/>
      <option name="defense" value="0"/>
      <option name="isInfrastructure" value="true"/>
      <option name="isAirBase" value="true"/>
      <option name="maxScrambleCount" value="2"/>
      <option name="stackCost" value="0"/>
      <option name="tuv" value="0"/>
    </attachment>
""",
    )
    replace_once(
        path,
        '    <property name="Air Control Persistent" value="false" editable="false"/>\n'
        '    <property name="Air Control Ground Attack Bonus" value="1" editable="false"/>\n',
        '    <property name="Air Control Persistent" value="true" editable="false"/>\n'
        '    <property name="Air Control Ground Attack Bonus" value="1" editable="false"/>\n'
        '    <property name="Scramble Rules In Effect" value="true" editable="false"/>\n'
        '    <property name="Scrambled Units Return To Base" value="true" editable="false"/>\n'
        '    <property name="Scramble To Sea Only" value="false" editable="false"/>\n'
        '    <property name="Scramble From Island Only" value="false" editable="false"/>\n'
        '    <property name="Battles May Be Preceeded By Air Battles" value="true" editable="false"/>\n'
        '    <property name="Can Scramble Into Air Battles" value="true" editable="false"/>\n',
    )
    for old, new in {
        "('fighter', 1, 'Germans')])": "('fighter', 1, 'Germans'), ('airfield', 1, 'Germans')])",
        "('fighter', 1, 'Americans')])": "('fighter', 1, 'Americans'), ('airfield', 1, 'Americans')])",
    }.items():
        text = (ROOT / path).read_text(encoding="utf-8")
        expected = 2
        if text.count(old) != expected:
            raise RuntimeError(f"{path}: expected {expected} occurrences of {old!r}")
        (ROOT / path).write_text(text.replace(old, new), encoding="utf-8")


def patch_xml(relative: str) -> None:
    replace_once(relative, '    <unit name="fighter"/>\n', '    <unit name="fighter"/>\n    <unit name="airfield"/>\n')
    replace_once(
        relative,
        '      <option name="attack" value="2"/>\n'
        '      <option name="defense" value="3"/>\n'
        '      <option name="canBlitz" value="true"/>\n'
        '      <option name="artillerySupportable" value="true"/>\n'
        '      <option name="stackCost" value="2"/>\n'
        '      <option name="tuv" value="6"/>\n',
        '      <option name="attack" value="2"/>\n'
        '      <option name="attackRolls" value="2"/>\n'
        '      <option name="defense" value="3"/>\n'
        '      <option name="canBlitz" value="true"/>\n'
        '      <option name="artillerySupportable" value="true"/>\n'
        '      <option name="stackCost" value="2"/>\n'
        '      <option name="tuv" value="7"/>\n',
    )
    replace_once(
        relative,
        '      <option name="canEscort" value="true"/>\n'
        '      <option name="airAttack" value="2"/>\n',
        '      <option name="canEscort" value="true"/>\n'
        '      <option name="canScramble" value="true"/>\n'
        '      <option name="maxScrambleDistance" value="2"/>\n'
        '      <option name="airAttack" value="2"/>\n',
    )
    insert_before(
        relative,
        '    <attachment name="territoryAttachment" attachTo="Andenne"',
        """    <attachment name="unitAttachment" attachTo="airfield" javaClass="games.strategy.triplea.attachments.UnitAttachment" type="unitType">
      <option name="movement" value="0"/>
      <option name="attack" value="0"/>
      <option name="defense" value="0"/>
      <option name="isInfrastructure" value="true"/>
      <option name="isAirBase" value="true"/>
      <option name="maxScrambleCount" value="2"/>
      <option name="stackCost" value="0"/>
      <option name="tuv" value="0"/>
    </attachment>
""",
    )
    replace_once(
        relative,
        '      <unitPlacement unitType="fighter" territory="Prum" quantity="1" owner="Germans"/>\n',
        '      <unitPlacement unitType="fighter" territory="Prum" quantity="1" owner="Germans"/>\n'
        '      <unitPlacement unitType="airfield" territory="Prum" quantity="1" owner="Germans"/>\n',
    )
    replace_once(
        relative,
        '      <unitPlacement unitType="fighter" territory="Bitburg" quantity="1" owner="Germans"/>\n',
        '      <unitPlacement unitType="fighter" territory="Bitburg" quantity="1" owner="Germans"/>\n'
        '      <unitPlacement unitType="airfield" territory="Bitburg" quantity="1" owner="Germans"/>\n',
    )
    replace_once(
        relative,
        '      <unitPlacement unitType="fighter" territory="Ciney" quantity="1" owner="Americans"/>\n',
        '      <unitPlacement unitType="fighter" territory="Ciney" quantity="1" owner="Americans"/>\n'
        '      <unitPlacement unitType="airfield" territory="Ciney" quantity="1" owner="Americans"/>\n',
    )
    replace_once(
        relative,
        '      <unitPlacement unitType="fighter" territory="Namur" quantity="1" owner="Americans"/>\n',
        '      <unitPlacement unitType="fighter" territory="Namur" quantity="1" owner="Americans"/>\n'
        '      <unitPlacement unitType="airfield" territory="Namur" quantity="1" owner="Americans"/>\n',
    )
    replace_once(
        relative,
        '    <property name="Air Control Persistent" value="false" editable="false"/>\n'
        '    <property name="Air Control Ground Attack Bonus" value="1" editable="false"/>\n',
        '    <property name="Air Control Persistent" value="true" editable="false"/>\n'
        '    <property name="Air Control Ground Attack Bonus" value="1" editable="false"/>\n'
        '    <property name="Scramble Rules In Effect" value="true" editable="false"/>\n'
        '    <property name="Scrambled Units Return To Base" value="true" editable="false"/>\n'
        '    <property name="Scramble To Sea Only" value="false" editable="false"/>\n'
        '    <property name="Scramble From Island Only" value="false" editable="false"/>\n'
        '    <property name="Battles May Be Preceeded By Air Battles" value="true" editable="false"/>\n'
        '    <property name="Can Scramble Into Air Battles" value="true" editable="false"/>\n',
    )


def patch_unitgen() -> None:
    path = "maps/small_front_meuse/tools/UnitGen.java"
    replace_once(path, "    FIGHTER\n", "    FIGHTER,\n    AIRFIELD\n")
    replace_once(
        path,
        '          new UnitIcon("fighter.png", Symbol.FIGHTER));\n',
        '          new UnitIcon("fighter.png", Symbol.FIGHTER),\n'
        '          new UnitIcon("airfield.png", Symbol.AIRFIELD));\n',
    )
    replace_once(
        path,
        "      case FIGHTER -> fighter(g);\n",
        "      case FIGHTER -> fighter(g);\n      case AIRFIELD -> airfield(g);\n",
    )
    insert_before(
        path,
        "  private static void fighter(Graphics2D g) {\n",
        """  private static void airfield(Graphics2D g) {
    // A runway with centreline markings distinguishes the infrastructure counter from aircraft.
    g.drawRect(19, 15, 10, 22);
    g.drawLine(24, 17, 24, 21);
    g.drawLine(24, 24, 24, 28);
    g.drawLine(24, 31, 24, 35);
  }

""",
    )


def patch_smoke_test() -> None:
    path = "game-app/smoke-testing/src/test/java/games/strategy/engine/data/SmallFrontAiGameTest.java"
    replace_once(
        path,
        "import games.strategy.triplea.delegate.scoring.SmallFrontScoringService;\n",
        "import games.strategy.triplea.delegate.battle.AirControlTracker;\n"
        "import games.strategy.triplea.delegate.battle.ScrambleLogic;\n"
        "import games.strategy.triplea.delegate.scoring.SmallFrontScoringService;\n",
    )
    insert_before(
        path,
        "  private static GameData loadMap() {\n",
        """  @Test
  void nativeAirbasesProvideRearAreaScrambleAndAirControlPersists() {
    final GameData data = loadMap();
    final GamePlayer germans = data.getPlayerList().getPlayerId("Germans");
    final GamePlayer americans = data.getPlayerList().getPlayerId("Americans");

    final Territory hotton = data.getMap().getTerritoryOrThrow("Hotton");
    final Territory ciney = data.getMap().getTerritoryOrThrow("Ciney");
    final Unit americanFighter = unitIn(ciney, "fighter", "Americans");
    assertThat(new ScrambleLogic(data, germans, hotton).getUnitsThatCanScramble())
        .contains(americanFighter);

    final Territory stVith = data.getMap().getTerritoryOrThrow("St. Vith");
    final Territory prum = data.getMap().getTerritoryOrThrow("Prum");
    final Unit germanFighter = unitIn(prum, "fighter", "Germans");
    assertThat(new ScrambleLogic(data, americans, stVith).getUnitsThatCanScramble())
        .contains(germanFighter);

    assertThat(AirControlTracker.isPersistent(data)).isTrue();
  }

""",
    )


def patch_validator() -> None:
    path = "maps/small_front_meuse/tools/validate_map.py"
    replace_once(
        path,
        '    root / "map" / "polygons.txt",\n',
        '    root / "map" / "polygons.txt",\n'
        '    root / "map" / "units" / "Germans" / "airfield.png",\n'
        '    root / "map" / "units" / "Americans" / "airfield.png",\n',
    )
    insert_before(
        path,
        "redeployment_steps = [\n",
        """unit_options = {}
for attachment in game_root.findall("./attachmentList/attachment"):
    if attachment.attrib.get("javaClass", "").endswith("UnitAttachment"):
        unit_options[attachment.attrib["attachTo"]] = {
            option.attrib["name"]: option.attrib["value"] for option in attachment.findall("option")
        }

armour = unit_options["armour"]
assert armour["attack"] == "2"
assert armour["attackRolls"] == "2"
assert armour["defense"] == "3"
assert armour["tuv"] == "7"

fighter = unit_options["fighter"]
assert fighter["canScramble"] == "true"
assert fighter["maxScrambleDistance"] == "2"

airfield = unit_options["airfield"]
assert airfield["isInfrastructure"] == "true"
assert airfield["isAirBase"] == "true"
assert airfield["maxScrambleCount"] == "2"

airfield_placements = {
    (placement.attrib["territory"], placement.attrib["owner"])
    for placement in game_root.findall("./initialize/unitInitialize/unitPlacement")
    if placement.attrib["unitType"] == "airfield"
}
assert airfield_placements == {
    ("Prum", "Germans"),
    ("Bitburg", "Germans"),
    ("Ciney", "Americans"),
    ("Namur", "Americans"),
}, airfield_placements

properties = {
    prop.attrib["name"]: prop.attrib["value"] for prop in game_root.findall("./propertyList/property")
}
for name, expected in {
    "Air Control Persistent": "true",
    "Scramble Rules In Effect": "true",
    "Scrambled Units Return To Base": "true",
    "Scramble To Sea Only": "false",
    "Scramble From Island Only": "false",
    "Battles May Be Preceeded By Air Battles": "true",
    "Can Scramble Into Air Battles": "true",
}.items():
    assert properties.get(name) == expected, (name, properties.get(name))

""",
    )
    replace_once(
        path,
        'f"{len(roads)} roads, resilient central trunk, configured stack capacities"\n',
        'f"{len(roads)} roads, native radius-2 scramble, persistent air control, armour 2x@2 TUV 7"\n',
    )


def patch_docs() -> None:
    insert_before(
        "maps/small_front_meuse/README.md",
        "## Victory scoring\n",
        """## Air operations and armour

Fighters use TripleA's native airbase and scramble system rather than a scenario-specific interceptor
search. Prum, Bitburg, Ciney and Namur each contain a two-aircraft airfield; fighters may scramble up
to two movement edges into a pending battle and return to their originating base after combat. This
lets aircraft remain behind the front while still contesting nearby battles. Air control persists
until a later battle changes it, and surviving numerical superiority establishes control while equal
survivors leave it contested.

Armour rolls two attack dice at attack 2, retains one defense die at defense 3, costs two stack
capacity, and has TUV 7. The change concentrates its value in offensive shock without increasing its
defensive output.

""",
    )
    replace_once(
        "maps/small_front_meuse/tools/README.md",
        """combines the infantry cross with tracked mobility, self-propelled artillery uses a horizontal track
oval, and fighters use an infinity-shaped fixed-wing mark.
""",
        """combines the infantry cross with tracked mobility, self-propelled artillery uses a horizontal track
oval, fighters use an infinity-shaped fixed-wing mark, and airfields use a marked runway. The XML
connects those airfields to TripleA's native radius-2 scramble and return-to-base rules.
""",
    )


def main() -> None:
    patch_air_control()
    patch_genxml()
    patch_xml("maps/small_front_meuse/map/games/Small_Front_Meuse.xml")
    patch_xml("game-app/smoke-testing/src/test/resources/map-xmls/Small_Front_Meuse.xml")
    patch_unitgen()
    patch_smoke_test()
    patch_validator()
    patch_docs()

    map_root = ROOT / "maps/small_front_meuse"
    subprocess.run(
        [
            "java",
            "-Djava.awt.headless=true",
            str(map_root / "tools/UnitGen.java"),
            str(map_root),
        ],
        check=True,
        cwd=ROOT,
    )
    subprocess.run(
        [sys.executable, "tools/manifest.py", "."],
        check=True,
        cwd=map_root,
    )


if __name__ == "__main__":
    main()
