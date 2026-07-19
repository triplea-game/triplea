#!/usr/bin/env python3
"""Emits the Meuse Corridor game XML from the curated graph."""
import sys

ZONE = {
    'Blankenheim': 'A', 'Prum': 'A', 'Bitburg': 'A', 'Echternach': 'A',
    'Losheim Gap': 'B', 'Clervaux': 'B', 'Vianden': 'B',
    'St. Vith': 'C', 'Houffalize': 'C', 'Wiltz': 'C', 'Bastogne': 'C', 'Martelange': 'C',
    'Vielsalm': 'D', 'Erezee': 'D', 'La Roche': 'D', 'Hotton': 'D', 'Nassogne': 'D',
    'Saint-Hubert': 'D', 'Libramont': 'D', 'Marche': 'D', 'Neufchateau': 'D',
    'Durbuy': 'E', 'Ciney': 'E', 'Rochefort': 'E', 'Beauraing': 'E', 'Bertrix': 'E',
    'Havelange': 'E', 'Wellin': 'E',
    'Huy': 'F', 'Andenne': 'F', 'Namur': 'F', 'Dinant': 'F', 'Givet': 'F',
}

TERRAIN = {'A': 'Open', 'B': 'Forest', 'C': 'Town', 'D': 'Open', 'E': 'Open', 'F': 'Town'}
GERMAN_START = ['Blankenheim', 'Prum', 'Bitburg', 'Echternach', 'Losheim Gap', 'Clervaux', 'Vianden']
MEUSE = ['Huy', 'Andenne', 'Namur', 'Dinant', 'Givet']
OBJECTIVES = ['St. Vith', 'Bastogne', 'Marche', 'Neufchateau', 'Namur', 'Dinant']

# Dropped from the Voronoi adjacency on purpose.
#   BB: the forest wall has no lateral movement links, so the breakthroughs stay separate.
#   EF: the Meuse is a river; each crossing keeps exactly one approach.
DROP = {
    ('Clervaux', 'Vianden'), ('Losheim Gap', 'Clervaux'),
    ('Havelange', 'Huy'), ('Wellin', 'Namur'), ('Bertrix', 'Givet'),
}

SUPPLY_SOURCES = ['Blankenheim', 'Prum', 'Bitburg', 'Echternach',
                  'Huy', 'Andenne', 'Namur', 'Dinant', 'Givet']

# Main roads only. Three former cross-links are deliberately absent:
# Vielsalm-Durbuy, Hotton-Marche and Libramont-Neufchateau. Movement remains possible across those
# borders. La Roche-Marche preserves a second central supply axis without restoring a direct bypass.
ROADS = [
    ('Prum', 'Blankenheim'), ('Prum', 'Bitburg'), ('Bitburg', 'Echternach'),
    ('Blankenheim', 'Losheim Gap'), ('Prum', 'Clervaux'), ('Bitburg', 'Vianden'),
    ('Losheim Gap', 'St. Vith'), ('Clervaux', 'Houffalize'), ('Vianden', 'Bastogne'),
    ('St. Vith', 'Houffalize'), ('Houffalize', 'Wiltz'), ('Wiltz', 'Bastogne'),
    ('Bastogne', 'Martelange'),
    ('St. Vith', 'Vielsalm'), ('Houffalize', 'Erezee'), ('Wiltz', 'Hotton'),
    ('Bastogne', 'Saint-Hubert'), ('Martelange', 'Libramont'),
    ('Vielsalm', 'Erezee'), ('Erezee', 'La Roche'), ('La Roche', 'Hotton'),
    ('La Roche', 'Marche'), ('Hotton', 'Nassogne'), ('Nassogne', 'Neufchateau'),
    ('Saint-Hubert', 'Libramont'),
    ('Marche', 'Ciney'), ('Marche', 'Rochefort'),
    ('Neufchateau', 'Beauraing'), ('Neufchateau', 'Bertrix'),
    ('Durbuy', 'Havelange'), ('Ciney', 'Havelange'), ('Rochefort', 'Ciney'),
    ('Rochefort', 'Wellin'), ('Beauraing', 'Wellin'), ('Beauraing', 'Bertrix'),
    ('Durbuy', 'Huy'), ('Havelange', 'Andenne'), ('Ciney', 'Namur'),
    ('Wellin', 'Dinant'), ('Beauraing', 'Givet'),
    ('Huy', 'Andenne'), ('Andenne', 'Namur'), ('Namur', 'Dinant'), ('Dinant', 'Givet'),
]


def connections(adj_path):
    edges = []
    for line in open(adj_path, encoding='utf-8'):
        a, b = line.strip().split(' | ')
        if (a, b) in DROP or (b, a) in DROP:
            continue
        edges.append(tuple(sorted((a, b))))
    return sorted(set(edges))


def main():
    adj_path, out_path = sys.argv[1], sys.argv[2]
    edges = connections(adj_path)
    names = sorted(ZONE)

    roads_by_source = {}
    for a, b in ROADS:
        assert tuple(sorted((a, b))) in edges, f'road {a}-{b} is not a movement edge'
        roads_by_source.setdefault(a, []).append(b)

    lines = []
    add = lines.append
    add('<?xml version="1.0" encoding="UTF-8"?>')
    add('<!DOCTYPE game SYSTEM "game.dtd">')
    add('<game>')
    add('  <info name="Small Front: Meuse Corridor" version="0.1"/>')
    add('  <loader javaClass="games.strategy.triplea.TripleA"/>')
    add('  <triplea minimumVersion="2.7.0"/>')
    add('  <diceSides value="6"/>')
    add('')
    add('  <map>')
    for name in names:
        add(f'    <territory name="{name}"/>')
    for a, b in edges:
        add(f'    <connection t1="{a}" t2="{b}"/>')
    add('  </map>')
    add('')
    add('  <resourceList>')
    add('    <resource name="PUs"/>')
    add('  </resourceList>')
    add('')
    add('  <playerList>')
    add('    <player name="Germans" optional="false" defaultType="Human"/>')
    add('    <player name="Americans" optional="false" defaultType="Human"/>')
    add('    <alliance player="Germans" alliance="Axis"/>')
    add('    <alliance player="Americans" alliance="Allies"/>')
    add('  </playerList>')
    add('')
    add('  <unitList>')
    for unit in ['infantry', 'americanInfantry', 'artillery', 'selfPropelledArtillery',
                 'armour', 'mechanized', 'fighter', 'airfield']:
        add(f'    <unit name="{unit}"/>')
    add('  </unitList>')
    add('')
    add('  <relationshipTypes>')
    add('    <relationshipType name="War"/>')
    add('    <relationshipType name="Allied"/>')
    add('  </relationshipTypes>')
    add('')
    add('  <territoryEffectList>')
    for terrain in ['Open', 'Forest', 'Town']:
        add(f'    <territoryEffect name="{terrain}"/>')
    add('  </territoryEffectList>')
    add('')
    add(GAMEPLAY.rstrip())
    add('  <attachmentList>')
    add(STATIC_ATTACHMENTS.rstrip())

    for name in names:
        zone = ZONE[name]
        add(f'    <attachment name="territoryAttachment" attachTo="{name}" javaClass="games.strategy.triplea.attachments.TerritoryAttachment" type="territory">')
        add('      <option name="production" value="0"/>')
        add(f'      <option name="victoryCity" value="{1 if name in OBJECTIVES else 0}"/>')
        if name == 'Prum':
            add('      <option name="capital" value="Germans"/>')
        if name == 'Namur':
            add('      <option name="capital" value="Americans"/>')
        add(f'      <option name="territoryEffect" value="{TERRAIN[zone]}"/>')
        add('    </attachment>')
        source = name in SUPPLY_SOURCES
        road_targets = roads_by_source.get(name, [])
        if source or road_targets:
            add(f'    <attachment name="supplyTerritoryAttachment" attachTo="{name}" javaClass="games.strategy.triplea.attachments.SupplyTerritoryAttachment" type="territory">')
            if source:
                add('      <option name="supplySource" value="true"/>')
            for target in road_targets:
                add(f'      <option name="roadConnection" value="{target}"/>')
            add('    </attachment>')

    add(REINFORCEMENTS_AND_SCORING.rstrip())
    add('  </attachmentList>')
    add('')
    add('  <initialize>')
    add('    <ownerInitialize>')
    for name in names:
        owner = 'Germans' if name in GERMAN_START else 'Americans'
        add(f'      <territoryOwner territory="{name}" owner="{owner}"/>')
    add('    </ownerInitialize>')
    add('    <unitInitialize>')
    for territory, units in UNITS:
        for unit_type, quantity, owner in units:
            add(f'      <unitPlacement unitType="{unit_type}" territory="{territory}" quantity="{quantity}" owner="{owner}"/>')
    add('    </unitInitialize>')
    add('    <relationshipInitialize>')
    add('      <relationship player1="Germans" player2="Americans" type="War" roundValue="0"/>')
    add('    </relationshipInitialize>')
    add('  </initialize>')
    add('')
    add(PROPERTIES.rstrip())
    add('</game>')

    open(out_path, 'w', encoding='utf-8').write('\n'.join(lines) + '\n')
    print(f'territories={len(names)} connections={len(edges)} roads={len(ROADS)} objectives={len(OBJECTIVES)}')


GAMEPLAY = '''  <gamePlay>
    <delegate name="initialization" javaClass="games.strategy.triplea.delegate.InitializationDelegate" display="Initialize"/>
    <delegate name="reinforcement" javaClass="games.strategy.triplea.delegate.FixedReinforcementDelegate" display="Fixed Reinforcements"/>
    <delegate name="supply" javaClass="games.strategy.triplea.delegate.SupplyDelegate" display="Supply"/>
    <delegate name="move" javaClass="games.strategy.triplea.delegate.SupplyAwareMoveDelegate" display="Move"/>
    <delegate name="battle" javaClass="games.strategy.triplea.delegate.BattleDelegate" display="Battle"/>
    <delegate name="endTurn" javaClass="games.strategy.triplea.delegate.NoPUEndTurnDelegate" display="End Turn"/>
    <delegate name="endRound" javaClass="games.strategy.triplea.delegate.SmallFrontEndRoundDelegate" display="End Round"/>

    <sequence>
      <step name="initializationStep" delegate="initialization" maxRunCount="1"/>

      <step name="germanReinforcement" delegate="reinforcement" player="Germans"/>
      <step name="germanSupply" delegate="supply" player="Germans"/>
      <step name="germanCombatMove" delegate="move" player="Germans" display="Combat Move"/>
      <step name="germanBattle" delegate="battle" player="Germans"/>
      <step name="germanNonCombatMove" delegate="move" player="Germans" display="Redeployment">
        <stepProperty name="nonCombatMove" value="true"/>
        <stepProperty name="removeAirThatCanNotLand" value="false"/>
      </step>
      <step name="germanEndTurn" delegate="endTurn" player="Germans"/>

      <step name="americanReinforcement" delegate="reinforcement" player="Americans"/>
      <step name="americanSupply" delegate="supply" player="Americans"/>
      <step name="americanCombatMove" delegate="move" player="Americans" display="Combat Move"/>
      <step name="americanBattle" delegate="battle" player="Americans"/>
      <step name="americanNonCombatMove" delegate="move" player="Americans" display="Redeployment">
        <stepProperty name="nonCombatMove" value="true"/>
        <stepProperty name="removeAirThatCanNotLand" value="false"/>
      </step>
      <step name="americanEndTurn" delegate="endTurn" player="Americans"/>

      <step name="endRoundStep" delegate="endRound"/>
    </sequence>
  </gamePlay>
'''

STATIC_ATTACHMENTS = '''    <attachment name="relationshipTypeAttachment" attachTo="War" javaClass="games.strategy.triplea.attachments.RelationshipTypeAttachment" type="relationship">
      <option name="archeType" value="war"/>
    </attachment>
    <attachment name="relationshipTypeAttachment" attachTo="Allied" javaClass="games.strategy.triplea.attachments.RelationshipTypeAttachment" type="relationship">
      <option name="archeType" value="allied"/>
    </attachment>

    <attachment name="territoryEffectAttachment" attachTo="Open" javaClass="games.strategy.triplea.attachments.TerritoryEffectAttachment" type="territoryEffect">
      <option name="maxGroundBattleRounds" value="4"/>
      <option name="maxAirBattleRounds" value="2"/>
      <option name="stackCapacity" value="7"/>
    </attachment>
    <attachment name="territoryEffectAttachment" attachTo="Forest" javaClass="games.strategy.triplea.attachments.TerritoryEffectAttachment" type="territoryEffect">
      <option name="maxGroundBattleRounds" value="2"/>
      <option name="maxAirBattleRounds" value="1"/>
      <option name="stackCapacity" value="5"/>
    </attachment>
    <attachment name="territoryEffectAttachment" attachTo="Town" javaClass="games.strategy.triplea.attachments.TerritoryEffectAttachment" type="territoryEffect">
      <option name="maxGroundBattleRounds" value="3"/>
      <option name="maxAirBattleRounds" value="1"/>
      <option name="stackCapacity" value="6"/>
    </attachment>

    <attachment name="unitAttachment" attachTo="infantry" javaClass="games.strategy.triplea.attachments.UnitAttachment" type="unitType">
      <option name="movement" value="1"/>
      <option name="combatMovement" value="1"/>
      <option name="redeploymentMovement" value="1"/>
      <option name="attack" value="1"/>
      <option name="defense" value="2"/>
      <option name="isInfantry" value="true"/>
      <option name="artillerySupportable" value="true"/>
      <option name="stackCost" value="1"/>
      <option name="tuv" value="3"/>
    </attachment>
    <attachment name="unitAttachment" attachTo="americanInfantry" javaClass="games.strategy.triplea.attachments.UnitAttachment" type="unitType">
      <option name="movement" value="1"/>
      <option name="combatMovement" value="1"/>
      <option name="redeploymentMovement" value="2"/>
      <option name="attack" value="1"/>
      <option name="defense" value="2"/>
      <option name="isInfantry" value="true"/>
      <option name="artillerySupportable" value="true"/>
      <option name="stackCost" value="1"/>
      <option name="tuv" value="3"/>
    </attachment>
    <attachment name="unitAttachment" attachTo="artillery" javaClass="games.strategy.triplea.attachments.UnitAttachment" type="unitType">
      <option name="movement" value="1"/>
      <option name="combatMovement" value="1"/>
      <option name="redeploymentMovement" value="1"/>
      <option name="attack" value="2"/>
      <option name="defense" value="2"/>
      <option name="artillery" value="true"/>
      <option name="stackCost" value="1"/>
      <option name="tuv" value="4"/>
    </attachment>
    <attachment name="unitAttachment" attachTo="selfPropelledArtillery" javaClass="games.strategy.triplea.attachments.UnitAttachment" type="unitType">
      <option name="movement" value="2"/>
      <option name="combatMovement" value="2"/>
      <option name="redeploymentMovement" value="3"/>
      <option name="attack" value="2"/>
      <option name="defense" value="2"/>
      <option name="artillery" value="true"/>
      <option name="stackCost" value="1"/>
      <option name="tuv" value="5"/>
    </attachment>
    <attachment name="unitAttachment" attachTo="armour" javaClass="games.strategy.triplea.attachments.UnitAttachment" type="unitType">
      <option name="movement" value="2"/>
      <option name="combatMovement" value="2"/>
      <option name="redeploymentMovement" value="3"/>
      <option name="attack" value="2"/>
      <option name="attackRolls" value="2"/>
      <option name="defense" value="3"/>
      <option name="canBlitz" value="true"/>
      <option name="artillerySupportable" value="true"/>
      <option name="stackCost" value="2"/>
      <option name="tuv" value="7"/>
    </attachment>
    <attachment name="unitAttachment" attachTo="mechanized" javaClass="games.strategy.triplea.attachments.UnitAttachment" type="unitType">
      <option name="movement" value="2"/>
      <option name="combatMovement" value="2"/>
      <option name="redeploymentMovement" value="3"/>
      <option name="attack" value="2"/>
      <option name="defense" value="2"/>
      <option name="isInfantry" value="true"/>
      <option name="artillerySupportable" value="true"/>
      <option name="stackCost" value="1"/>
      <option name="tuv" value="4"/>
    </attachment>
    <attachment name="unitAttachment" attachTo="fighter" javaClass="games.strategy.triplea.attachments.UnitAttachment" type="unitType">
      <option name="movement" value="4"/>
      <option name="combatMovement" value="4"/>
      <option name="redeploymentMovement" value="4"/>
      <option name="attack" value="3"/>
      <option name="defense" value="3"/>
      <option name="isAir" value="true"/>
      <option name="canAirBattle" value="true"/>
      <option name="canIntercept" value="true"/>
      <option name="canEscort" value="true"/>
      <option name="canScramble" value="true"/>
      <option name="maxScrambleDistance" value="2"/>
      <option name="airAttack" value="2"/>
      <option name="airDefense" value="2"/>
      <option name="stackCost" value="0"/>
      <option name="tuv" value="10"/>
    </attachment>
    <attachment name="unitAttachment" attachTo="airfield" javaClass="games.strategy.triplea.attachments.UnitAttachment" type="unitType">
      <option name="movement" value="0"/>
      <option name="attack" value="0"/>
      <option name="defense" value="0"/>
      <option name="isInfrastructure" value="true"/>
      <option name="isAirBase" value="true"/>
      <option name="maxScrambleCount" value="2"/>
      <option name="stackCost" value="0"/>
      <option name="tuv" value="0"/>
    </attachment>
'''

REINFORCEMENTS_AND_SCORING = '''
    <attachment name="fixedReinforcementAttachment" attachTo="Germans" javaClass="games.strategy.triplea.attachments.FixedReinforcementAttachment" type="player">
      <option name="reinforcement" value="1:Prum:armour:2"/>
      <option name="reinforcement" value="1:Bitburg:mechanized:1"/>
      <option name="reinforcement" value="2:Prum:selfPropelledArtillery:1"/>
      <option name="reinforcement" value="2:Blankenheim:armour:1"/>
      <option name="reinforcement" value="3:Bitburg:infantry:2"/>
      <option name="reinforcement" value="3:Prum:mechanized:1"/>
      <option name="reinforcement" value="4:Echternach:infantry:2"/>
      <option name="reinforcement" value="4:Bitburg:fighter:1"/>
      <option name="reinforcement" value="5:Prum:infantry:2"/>
    </attachment>
    <attachment name="fixedReinforcementAttachment" attachTo="Americans" javaClass="games.strategy.triplea.attachments.FixedReinforcementAttachment" type="player">
      <option name="reinforcement" value="2:Marche:americanInfantry:2"/>
      <option name="reinforcement" value="3:Namur:armour:1"/>
      <option name="reinforcement" value="3:Ciney:fighter:1"/>
      <option name="reinforcement" value="4:Namur:americanInfantry:2"/>
      <option name="reinforcement" value="4:Dinant:artillery:1"/>
      <option name="reinforcement" value="5:Namur:armour:2"/>
      <option name="reinforcement" value="5:Namur:fighter:1"/>
      <option name="reinforcement" value="6:Huy:americanInfantry:2"/>
      <option name="reinforcement" value="6:Givet:americanInfantry:2"/>
      <option name="reinforcement" value="7:Namur:armour:1"/>
    </attachment>

    <attachment name="scoringAttachment" attachTo="Germans" javaClass="games.strategy.triplea.attachments.SmallFrontScoringAttachment" type="player">
      <option name="pointsPerObjective" value="1"/>
      <option name="suppliedOccupationBonus" value="2:Huy:Andenne:Namur:Dinant:Givet"/>
      <option name="suppliedOccupationBonus" value="1:Bastogne:Marche"/>
    </attachment>
    <attachment name="scoringAttachment" attachTo="Americans" javaClass="games.strategy.triplea.attachments.SmallFrontScoringAttachment" type="player">
      <option name="pointsPerObjective" value="1"/>
      <option name="enemyAbsentBonus" value="2:Huy:Andenne:Namur:Dinant:Givet"/>
      <option name="winsTies" value="true"/>
    </attachment>
'''

PROPERTIES = '''  <propertyList>
    <property name="Supply Network Enabled" value="true" editable="false"/>
    <property name="Out Of Supply Removal Turns" value="2" editable="false"/>
    <property name="Separate Air And Ground Combat" value="true" editable="false"/>
    <property name="Air Control Enabled" value="true" editable="false"/>
    <property name="Air Control Persistent" value="false" editable="false"/>
    <property name="Air Control Ground Attack Bonus" value="1" editable="false"/>
    <property name="Scramble Rules In Effect" value="true" editable="false"/>
    <property name="Scrambled Units Return To Base" value="true" editable="false"/>
    <property name="Scramble To Sea Only" value="false" editable="false"/>
    <property name="Scramble From Island Only" value="false" editable="false"/>
    <property name="Battles May Be Preceeded By Air Battles" value="true" editable="false"/>
    <property name="Can Scramble Into Air Battles" value="true" editable="false"/>
    <property name="Fog Of War Enabled" value="true" editable="false"/>
    <property name="Fog Of War Vision Radius" value="1" editable="false"/>
    <property name="Auto Termination" value="true" editable="true"/>
    <property name="Scoring Round" value="8" editable="false"/>
    <property name="Land Battle Rounds" value="3" editable="false"/>
    <property name="Air Battle Rounds" value="1" editable="false"/>
    <property name="Sea Battle Rounds" value="3" editable="false"/>
  </propertyList>'''

UNITS = [
    ('Prum', [('infantry', 2, 'Germans'), ('artillery', 1, 'Germans'), ('fighter', 1, 'Germans'), ('airfield', 1, 'Germans')]),
    ('Blankenheim', [('infantry', 2, 'Germans'), ('armour', 1, 'Germans')]),
    ('Bitburg', [('infantry', 2, 'Germans'), ('mechanized', 1, 'Germans'), ('fighter', 1, 'Germans'), ('airfield', 1, 'Germans')]),
    ('Echternach', [('infantry', 2, 'Germans')]),
    ('Losheim Gap', [('infantry', 2, 'Germans'), ('mechanized', 1, 'Germans')]),
    ('Clervaux', [('infantry', 2, 'Germans'), ('armour', 1, 'Germans')]),
    ('Vianden', [('infantry', 2, 'Germans'), ('selfPropelledArtillery', 1, 'Germans')]),
    ('St. Vith', [('americanInfantry', 2, 'Americans'), ('artillery', 1, 'Americans')]),
    ('Houffalize', [('americanInfantry', 1, 'Americans')]),
    ('Bastogne', [('americanInfantry', 2, 'Americans'), ('artillery', 1, 'Americans')]),
    ('Martelange', [('americanInfantry', 1, 'Americans')]),
    ('Vielsalm', [('americanInfantry', 1, 'Americans')]),
    ('La Roche', [('americanInfantry', 1, 'Americans')]),
    ('Erezee', [('americanInfantry', 1, 'Americans')]),
    ('Hotton', [('americanInfantry', 1, 'Americans')]),
    ('Nassogne', [('americanInfantry', 1, 'Americans')]),
    ('Libramont', [('americanInfantry', 1, 'Americans')]),
    ('Marche', [('americanInfantry', 2, 'Americans'), ('armour', 1, 'Americans')]),
    ('Neufchateau', [('americanInfantry', 1, 'Americans')]),
    ('Saint-Hubert', [('americanInfantry', 1, 'Americans')]),
    ('Ciney', [('americanInfantry', 1, 'Americans'), ('fighter', 1, 'Americans'), ('airfield', 1, 'Americans')]),
    ('Namur', [('americanInfantry', 2, 'Americans'), ('fighter', 1, 'Americans'), ('airfield', 1, 'Americans')]),
    ('Dinant', [('americanInfantry', 1, 'Americans')]),
]

if __name__ == '__main__':
    main()
