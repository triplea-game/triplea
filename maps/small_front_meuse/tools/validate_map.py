#!/usr/bin/env python3
from pathlib import Path
import re
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
game = root / "map" / "games" / "Small_Front_Meuse.xml"
required = [
    root / "map.yml",
    root / "map" / "map.properties",
    root / "map" / "centers.txt",
    root / "map" / "place.txt",
    root / "map" / "polygons.txt",
    root / "tools" / "UnitGen.java",
    game,
]
missing = [str(path.relative_to(root)) for path in required if not path.exists()]
if missing:
    raise SystemExit("Missing files: " + ", ".join(missing))

tree = ET.parse(game)
game_root = tree.getroot()
territories = {node.attrib["name"] for node in game_root.findall("./map/territory")}
connections = game_root.findall("./map/connection")
for connection in connections:
    assert connection.attrib["t1"] in territories
    assert connection.attrib["t2"] in territories

centers = {}
pattern = re.compile(r"^(.*?)\s+\((\d+),(\d+)\)")
for line in (root / "map" / "centers.txt").read_text(encoding="utf-8").splitlines():
    match = pattern.match(line)
    if match:
        centers[match.group(1)] = (int(match.group(2)), int(match.group(3)))
assert set(centers) == territories, set(centers) ^ territories

roads = set()
for attachment in game_root.findall("./attachmentList/attachment"):
    if attachment.attrib.get("javaClass", "").endswith("SupplyTerritoryAttachment"):
        source = attachment.attrib["attachTo"]
        assert source in territories
        for option in attachment.findall("option"):
            if option.attrib["name"] == "roadConnection":
                target = option.attrib["value"]
                assert target in territories
                roads.add(tuple(sorted((source, target))))

assert len(roads) == 43, len(roads)
for removed in {
    tuple(sorted(("Vielsalm", "Durbuy"))),
    tuple(sorted(("Hotton", "Marche"))),
    tuple(sorted(("Libramont", "Neufchateau"))),
}:
    assert removed not in roads, removed

redeployment_steps = [
    step for step in game_root.findall("./gamePlay/sequence/step")
    if step.attrib.get("display") == "Redeployment"
]
assert len(redeployment_steps) == 2
for step in redeployment_steps:
    properties = {item.attrib["name"]: item.attrib["value"] for item in step.findall("stepProperty")}
    assert properties.get("nonCombatMove") == "true"
    assert properties.get("removeAirThatCanNotLand") == "false"

print(
    f"OK: {len(territories)} territories, {len(connections)} movement edges, "
    f"{len(roads)} roads, fighter landing cleanup disabled"
)
