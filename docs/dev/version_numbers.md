---
layout: longpage
title: Version Numbers
permalink: /dev_docs/dev/version_numbers/
---


We're moving to a 3 number versioning system (discussed in: https://github.com/triplea-game/triplea/issues/1739) loosely based on semantic versioning.


**First** - major, compatibility number, only if this number is the same will save games and lobby work. For example, renaming a method called via RMI would cause problems between versions and would require a major number bump.
**Second** - minor/patch release number, doesn't affect compatibility but we can increment it to drive folks to a new release as otherwise they never upgrade (if we eventually have an update process prompt from TripleA then this wouldn't be needed)
**Third** - build number which specifies exactly what commit

