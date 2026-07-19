#!/usr/bin/env python3
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
game = root / "map" / "games" / "Small_Front_Meuse.xml"
required = [
    root / "map.yml",
    root / "map" / "map.properties",
    root / "map" / "centers.txt",
    root / "map" / "place.txt",
    root / "map" / "polygons.txt",
    game,
]
missing = [str(p.relative_to(root)) for p in required if not p.exists()]
if missing:
    raise SystemExit("Missing files: " + ", ".join(missing))

tree = ET.parse(game)
game_root = tree.getroot()
territories = {n.attrib["name"] for n in game_root.findall("./map/territory")}
connections = game_root.findall("./map/connection")
for c in connections:
    assert c.attrib["t1"] in territories
    assert c.attrib["t2"] in territories

centers = {}
pattern = re.compile(r"^(.*?)\s+\((\d+),(\d+)\)")
for line in (root / "map" / "centers.txt").read_text(encoding="utf-8").splitlines():
    m = pattern.match(line)
    if m:
        centers[m.group(1)] = (int(m.group(2)), int(m.group(3)))
assert set(centers) == territories, (set(centers) ^ territories)

road_targets = []
for att in game_root.findall("./attachmentList/attachment"):
    if att.attrib.get("javaClass","").endswith("SupplyTerritoryAttachment"):
        assert att.attrib["attachTo"] in territories
        for opt in att.findall("option"):
            if opt.attrib["name"] == "roadConnection":
                road_targets.append(opt.attrib["value"])
assert set(road_targets) <= territories

print(f"OK: {len(territories)} territories, {len(connections)} movement edges, {len(road_targets)} road declarations")
