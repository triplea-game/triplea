#!/usr/bin/env python3
import json
import os
import sys

root = sys.argv[1]
entries = []
for dirpath, dirnames, filenames in os.walk(root):
    dirnames[:] = [d for d in dirnames if d not in ('.git', '.claude')]
    for fn in sorted(filenames):
        full = os.path.join(dirpath, fn)
        rel = os.path.relpath(full, root).replace(os.sep, '/')
        if rel == 'manifest.json':
            continue
        entries.append({'path': rel, 'bytes': os.path.getsize(full)})
entries.sort(key=lambda e: e['path'])
with open(os.path.join(root, 'manifest.json'), 'w', encoding='utf-8') as f:
    json.dump(entries, f, indent=2)
    f.write('\n')
print('manifest entries:', len(entries))
