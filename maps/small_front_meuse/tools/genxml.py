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

# Terrain by zone. The forest wall is the only Forest band, which is what makes it a wall.
TERRAIN = {'A': 'Open', 'B': 'Forest', 'C': 'Town', 'D': 'Open', 'E': 'Open', 'F': 'Town'}

GERMAN_START = ['Blankenheim', 'Prum', 'Bitburg', 'Echternach', 'Losheim Gap', 'Clervaux', 'Vianden']

MEUSE = ['Huy', 'Andenne', 'Namur', 'Dinant', 'Givet']
OBJECTIVES = ['St. Vith', 'Bastogne', 'Marche', 'Neufchateau', 'Namur', 'Dinant']

# Dropped from the Voronoi adjacency on purpose.
#   BB: the forest wall has no lateral roads, so the three breakthroughs are separate axes.
#   EF: the Meuse is a river; each crossing keeps exactly one approach.
DROP = {
    ('Clervaux', 'Vianden'), ('Losheim Gap', 'Clervaux'),
    ('Havelange', 'Huy'), ('Wellin', 'Namur'), ('Bertrix', 'Givet'),
}

# Supply sources: each side's rear.
SUPPLY_SOURCES = ['Blankenheim', 'Prum', 'Bitburg', 'Echternach',
                  'Huy', 'Andenne', 'Namur', 'Dinant', 'Givet']

# Main roads only - a strict subset of movement edges, so cutting one strands a spearhead.
ROADS = [
    ('Prum', 'Blankenheim'), ('Prum', 'Bitburg'), ('Bitburg', 'Echternach'),
    ('Blankenheim', 'Losheim Gap'), ('Prum', 'Clervaux'), ('Bitburg', 'Vianden'),
    ('Losheim Gap', 'St. Vith'), ('Clervaux', 'Houffalize'), ('Vianden', 'Bastogne'),
    ('St. Vith', 'Houffalize'), ('Houffalize', 'Wiltz'), ('Wiltz', 'Bastogne'),
    ('Bastogne', 'Martelange'),
    ('St. Vith', 'Vielsalm'), ('Houffalize', 'Erezee'), ('Wiltz', 'Hotton'),
    ('Bastogne', 'Saint-Hubert'), ('Martelange', 'Libramont'),
    ('Vielsalm', 'Erezee'), ('Erezee', 'La Roche'), ('La Roche', 'Hotton'),
    ('Hotton', 'Marche'), ('Hotton', 'Nassogne'), ('Nassogne', 'Neufchateau'),
    ('Saint-Hubert', 'Libramont'), ('Libramont', 'Neufchateau'),
    ('Vielsalm', 'Durbuy'), ('Marche', 'Ciney'), ('Marche', 'Rochefort'),
    ('Neufchateau', 'Beauraing'), ('Neufchateau', 'Bertrix'),
    ('Durbuy', 'Havelange'), ('Ciney', 'Havelange'), ('Rochefort', 'Ciney'),
    ('Rochefort', 'Wellin'), ('Beauraing', 'Wellin'), ('Beauraing', 'Bertrix'),
    ('Durbuy', 'Huy'), ('Havelange', 'Andenne'), ('Ciney', 'Namur'),
    ('Wellin', 'Dinant'), ('Beauraing', 'Givet'),
    ('Huy', 'Andenne'), ('Andenne', 'Namur'), ('Namur', 'Dinant'), ('Dinant', 'Givet'),
]


def connections(adj_path):
    edges = []
    for line in open(adj_path):
        a, b = line.strip().split(' | ')
        if (a, b) in DROP or (b, a) in DROP:
            continue
        edges.append(tuple(sorted((a, b))))
    return sorted(set(edges))


def main():
    adj_path, out_path = sys.argv[1], sys.argv[2]
    edges = connections(adj_path)
    names = sorted(ZONE)

    # roadConnection is declared one-sided; the engine treats roads as undirected.
    roads_by_source = {}
    for a, b in ROADS:
        assert tuple(sorted((a, b))) in edges, f'road {a}-{b} is not a movement edge'
        roads_by_source.setdefault(a, []).append(b)

    L = []
    L.append('<?xml version="1.0" encoding="UTF-8"?>')
    L.append('<!DOCTYPE game SYSTEM "game.dtd">')
    L.append('<game>')
    L.append('  <info name="Small Front: Meuse Corridor" version="0.1"/>')
    L.append('  <loader javaClass="games.strategy.triplea.TripleA"/>')
    L.append('  <triplea minimumVersion="2.7.0"/>')
    L.append('  <diceSides value="6"/>')
    L.append('')
    L.append('  <map>')
    for n in names:
        L.append(f'    <territory name="{n}"/>')
    for a, b in edges:
        L.append(f'    <connection t1="{a}" t2="{b}"/>')
    L.append('  </map>')
    L.append('')
    L.append('  <resourceList>\n    <resource name="PUs"/>\n  </resourceList>')
    L.append('')
    L.append('  <playerList>')
    L.append('    <player name="Germans" optional="false" defaultType="Human"/>')
    L.append('    <player name="Americans" optional="false" defaultType="Human"/>')
    L.append('    <alliance player="Germans" alliance="Axis"/>')
    L.append('    <alliance player="Americans" alliance="Allies"/>')
    L.append('  </playerList>')
    L.append('')
    L.append('  <unitList>')
    for u in ['infantry', 'artillery', 'armour', 'mechanized', 'fighter']:
        L.append(f'    <unit name="{u}"/>')
    L.append('  </unitList>')
    L.append('')
    L.append('  <relationshipTypes>\n    <relationshipType name="War"/>\n    <relationshipType name="Allied"/>\n  </relationshipTypes>')
    L.append('')
    L.append('  <territoryEffectList>')
    for t in ['Open', 'Forest', 'Town']:
        L.append(f'    <territoryEffect name="{t}"/>')
    L.append('  </territoryEffectList>')
    L.append('')
    L.append(GAMEPLAY)
    L.append('  <attachmentList>')
    L.append(STATIC_ATTACHMENTS)

    for n in names:
        z = ZONE[n]
        L.append(f'    <attachment name="territoryAttachment" attachTo="{n}" javaClass="games.strategy.triplea.attachments.TerritoryAttachment" type="territory">')
        L.append('      <option name="production" value="0"/>')
        L.append(f'      <option name="victoryCity" value="{1 if n in OBJECTIVES else 0}"/>')
        if n == 'Prum':
            L.append('      <option name="capital" value="Germans"/>')
        if n == 'Namur':
            L.append('      <option name="capital" value="Americans"/>')
        L.append(f'      <option name="territoryEffect" value="{TERRAIN[z]}"/>')
        L.append('    </attachment>')
        src = n in SUPPLY_SOURCES
        rc = roads_by_source.get(n, [])
        if src or rc:
            L.append(f'    <attachment name="supplyTerritoryAttachment" attachTo="{n}" javaClass="games.strategy.triplea.attachments.SupplyTerritoryAttachment" type="territory">')
            if src:
                L.append('      <option name="supplySource" value="true"/>')
            for t in rc:
                L.append(f'      <option name="roadConnection" value="{t}"/>')
            L.append('    </attachment>')

    L.append(REINFORCEMENTS_AND_SCORING)
    L.append('  </attachmentList>')
    L.append('')
    L.append('  <initialize>')
    L.append('    <ownerInitialize>')
    for n in names:
        L.append(f'      <territoryOwner territory="{n}" owner="{"Germans" if n in GERMAN_START else "Americans"}"/>')
    L.append('    </ownerInitialize>')
    L.append('    <unitInitialize>')
    for terr, units in UNITS:
        for ut, qty, owner in units:
            L.append(f'      <unitPlacement unitType="{ut}" territory="{terr}" quantity="{qty}" owner="{owner}"/>')
    L.append('    </unitInitialize>')
    L.append('    <relationshipInitialize>')
    L.append('      <relationship player1="Germans" player2="Americans" type="War" roundValue="0"/>')
    L.append('    </relationshipInitialize>')
    L.append('  </initialize>')
    L.append('')
    L.append(PROPERTIES)
    L.append('</game>')
    open(out_path, 'w', encoding='utf-8').write('\n'.join(L) + '\n')
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
      </step>
      <step name="germanEndTurn" delegate="endTurn" player="Germans"/>

      <step name="americanReinforcement" delegate="reinforcement" player="Americans"/>
      <step name="americanSupply" delegate="supply" player="Americans"/>
      <step name="americanCombatMove" delegate="move" player="Americans" display="Combat Move"/>
      <step name="americanBattle" delegate="battle" player="Americans"/>
      <step name="americanNonCombatMove" delegate="move" player="Americans" display="Redeployment">
        <stepProperty name="nonCombatMove" value="true"/>
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
      <option name="stackCapacity" value="6"/>
    </attachment>
    <attachment name="territoryEffectAttachment" attachTo="Forest" javaClass="games.strategy.triplea.attachments.TerritoryEffectAttachment" type="territoryEffect">
      <option name="maxGroundBattleRounds" value="2"/>
      <option name="maxAirBattleRounds" value="1"/>
      <option name="stackCapacity" value="3"/>
    </attachment>
    <attachment name="territoryEffectAttachment" attachTo="Town" javaClass="games.strategy.triplea.attachments.TerritoryEffectAttachment" type="territoryEffect">
      <option name="maxGroundBattleRounds" value="3"/>
      <option name="maxAirBattleRounds" value="1"/>
      <option name="stackCapacity" value="5"/>
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
    <attachment name="unitAttachment" attachTo="armour" javaClass="games.strategy.triplea.attachments.UnitAttachment" type="unitType">
      <option name="movement" value="2"/>
      <option name="combatMovement" value="2"/>
      <option name="redeploymentMovement" value="3"/>
      <option name="attack" value="2"/>
      <option name="defense" value="3"/>
      <option name="canBlitz" value="true"/>
      <option name="stackCost" value="2"/>
      <option name="tuv" value="6"/>
    </attachment>
    <attachment name="unitAttachment" attachTo="mechanized" javaClass="games.strategy.triplea.attachments.UnitAttachment" type="unitType">
      <option name="movement" value="2"/>
      <option name="combatMovement" value="2"/>
      <option name="redeploymentMovement" value="3"/>
      <option name="attack" value="2"/>
      <option name="defense" value="2"/>
      <option name="isInfantry" value="true"/>
      <option name="stackCost" value="2"/>
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
      <option name="airAttack" value="2"/>
      <option name="airDefense" value="2"/>
      <option name="stackCost" value="0"/>
      <option name="tuv" value="10"/>
    </attachment>
'''

REINFORCEMENTS_AND_SCORING = '''
    <attachment name="fixedReinforcementAttachment" attachTo="Germans" javaClass="games.strategy.triplea.attachments.FixedReinforcementAttachment" type="player">
      <option name="reinforcement" value="1:Prum:armour:2"/>
      <option name="reinforcement" value="1:Bitburg:mechanized:1"/>
      <option name="reinforcement" value="2:Prum:artillery:1"/>
      <option name="reinforcement" value="2:Blankenheim:armour:1"/>
      <option name="reinforcement" value="3:Bitburg:infantry:2"/>
      <option name="reinforcement" value="3:Prum:mechanized:1"/>
      <option name="reinforcement" value="4:Echternach:infantry:2"/>
      <option name="reinforcement" value="5:Prum:infantry:2"/>
    </attachment>
    <attachment name="fixedReinforcementAttachment" attachTo="Americans" javaClass="games.strategy.triplea.attachments.FixedReinforcementAttachment" type="player">
      <option name="reinforcement" value="2:Marche:infantry:2"/>
      <option name="reinforcement" value="3:Namur:armour:1"/>
      <option name="reinforcement" value="3:Ciney:fighter:1"/>
      <option name="reinforcement" value="4:Namur:infantry:2"/>
      <option name="reinforcement" value="4:Dinant:artillery:1"/>
      <option name="reinforcement" value="5:Namur:armour:2"/>
      <option name="reinforcement" value="6:Huy:infantry:2"/>
      <option name="reinforcement" value="6:Givet:infantry:2"/>
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
    <property name="Fog Of War Enabled" value="true" editable="false"/>
    <property name="Fog Of War Vision Radius" value="1" editable="false"/>
    <property name="Auto Termination" value="true" editable="true"/>
    <property name="Scoring Round" value="8" editable="false"/>
    <property name="Land Battle Rounds" value="3" editable="false"/>
    <property name="Air Battle Rounds" value="1" editable="false"/>
    <property name="Sea Battle Rounds" value="3" editable="false"/>
  </propertyList>'''

# The Germans open at 3.00 units per territory against 0.81, with 13 of the 26 American
# territories holding nothing at all. Density, not armour, is what let a spearhead run.
UNITS = [
    ('Prum', [('infantry', 2, 'Germans'), ('artillery', 1, 'Germans'), ('fighter', 1, 'Germans')]),
    ('Blankenheim', [('infantry', 2, 'Germans'), ('armour', 1, 'Germans')]),
    ('Bitburg', [('infantry', 2, 'Germans'), ('mechanized', 1, 'Germans')]),
    ('Echternach', [('infantry', 2, 'Germans')]),
    ('Losheim Gap', [('infantry', 2, 'Germans'), ('mechanized', 1, 'Germans')]),
    ('Clervaux', [('infantry', 2, 'Germans'), ('armour', 1, 'Germans')]),
    ('Vianden', [('infantry', 2, 'Germans'), ('artillery', 1, 'Germans')]),
    ('St. Vith', [('infantry', 3, 'Americans'), ('artillery', 1, 'Americans')]),
    ('Houffalize', [('infantry', 2, 'Americans')]),
    ('Wiltz', [('infantry', 1, 'Americans')]),
    ('Bastogne', [('infantry', 3, 'Americans'), ('artillery', 1, 'Americans')]),
    ('Martelange', [('infantry', 1, 'Americans')]),
    ('Vielsalm', [('infantry', 1, 'Americans')]),
    ('La Roche', [('infantry', 1, 'Americans')]),
    ('Erezee', [('infantry', 1, 'Americans')]),
    ('Hotton', [('infantry', 1, 'Americans')]),
    ('Nassogne', [('infantry', 1, 'Americans')]),
    ('Libramont', [('infantry', 1, 'Americans')]),
    ('Marche', [('infantry', 2, 'Americans'), ('armour', 1, 'Americans')]),
    ('Neufchateau', [('infantry', 1, 'Americans')]),
    ('Saint-Hubert', [('infantry', 1, 'Americans')]),
    ('Ciney', [('infantry', 1, 'Americans'), ('fighter', 1, 'Americans')]),
    ('Namur', [('infantry', 2, 'Americans')]),
    ('Dinant', [('infantry', 1, 'Americans')]),
]

if __name__ == '__main__':
    main()
