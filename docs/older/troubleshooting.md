---
layout: longpage
title: Mac and Unix Game Freezse Troubleshooting
permalink: /game_freeze_troubleshooting
root: /dev_docs/
---

Response to a frozen game problem, below are steps notes of what would be needed to debug such a problem:

  - launch the game from a console window, running: ./triplea_unix.sh  | tee output.txt
  - recreate the game freeze.
  - In a second console, "`kill -3`" the process id of the running game. As a one line that would be:
     - `ps -ef | grep java | grep GameRunner | awk '{print $2}' | xargs kill -3`
  - Go back to the first console, a lot of new text should have shown up. Kill the game (ctrl+c), the file: "`output.txt`" will have the debug data we want.
