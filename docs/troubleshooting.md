## Mac/Unix Game Freeze Troubleshooting 
Response to a frozen game problem, below are steps notes of what would be needed to debug such a problem:

- Which download file are you using? I'd like to be sure we're looking at the same file/problem.
- Could you please clarify a bit the steps you are taking to see the freeze. I'd like to understand exactly when/where you are seeing the freeze to help narrow down what the problem could be.
- Last, could you help provide a stack trace to give us some debug data that will tell us what the game is busy doing (or rather not doing). The stack trace can be uploaded to  "http://pastebin.com/" or any public file/text sharing site. Instructions to generate the stack trace are below:
  - launch the game from a console window, running: ./triplea_unix.sh  | tee output.txt
  - recreate the game freeze. 
  - In a second console, "`kill -3`" the process id of the running game. As a one line that would be:
    - `ps -ef | grep java | grep GameRunner | awk '{print $2}' | xargs kill -3`
  - Go back to the first console, a lot of new text should have shown up. Kill the game (ctrl+c), the file: "`output.txt`" will have the debug data we want.
