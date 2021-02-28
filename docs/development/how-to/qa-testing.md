# QA Testing

- [Network games](#network-games)
	- [Server running NEW should be able to communicate with client running OLD](#server-running-new-should-be-able-to-communicate-with-client-running-old)
	- [Lobby and bot running OLD should be able to communicate with client running NEW](#lobby-and-bot-running-old-should-be-able-to-communicate-with-client-running-new)
- [Manual Regression Testing](#manual-regression-testing)

### Network games

Ensure compatibility between network nodes running different versions.

#### Server running NEW should be able to communicate with client running OLD

1. Host a local network game using NEW.
1. Connect to game started in (1) using OLD.
    * Ensure OLD and NEW select players in adversarial roles.
1. Progress far enough in the game to fight a battle.

#### Lobby and bot running OLD should be able to communicate with client running NEW

1. Run a lobby with at least one bot using OLD.
    * This can be done locally or you can use the production lobby if a bot is available.
1. Connect to lobby started in (1) using NEW and start a game with bot.
    * Ensure OLD and NEW select players in adversarial roles.
1. Progress far enough in the game to fight a battle.

## Manual Regression Testing

- Various maps can be opened, no delegates broken
- Launch an AI only game and let some AI players go at for a dozen rounds, verify no errors.
- Verify important combat rules are working (can be combined with above step)

- Local host + client
  - This means start up two copies of triplea.  In one copy you click ‘host networked game’
and start hosting.  In the other copy you click ‘connect to networked game’ and join.  The
client should choose the FIRST player in the list (this is to test that the delegates are
sending data correctly). Start the game and play a round against yourself, making sure
everything looks good.

- Play by Email, and Play by Forum
  - You can test both at once by setting up a game against yourself that has both settings enabled.
  You will probably need to add two email addresses for this to work.
  - Test all forums
  - Recommend test play by email with both a gmail and a hotmail account.

