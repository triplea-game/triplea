# (Legacy) Engine Code Overview

```
This is the original design document published with Triplea.
It is quite old, potentially out of date, left intact for historical reference.
```

## Design Goals

The goal of the game engine is to provide a framework to
build turn based strategy games.The
engine provides networking and data model support.

## The big picture

The developer of a game using the TripleA engine must do
three things. Create a game.xml file
describing the game, write Delegates to handle game logic (or reuse existing
delegates), write GamePlayer classes that handle the user interaction (either
human or automated).

The engine provides a parser to parse the game xml data,
network transparent game data updates, and handles network communication
between Delegates and GamePlayers.

## Game Data

Game data is read from an xml file that follows the game.dtd
schema.
data\games\strategy\engine\xml directory).

## The game xml file specifies

* Game name and version<
* Class used to load the game
Territory names
* Connection between territories
* Resource types
* Players and alliances
* Unit types
* Production rules
* Initial unit placement,
* Initial territory ownership
* Initial resource allocation
* Attachments (more later)
* Game play sequence

All the data above is parsed by the game, and can be
accessed through the GameData object.

The xml file format must be general enough to describe the
data for any turn based strategy game, yet specific enough to be able to
describe a game completely.

The problem is that each game differs in the type of data
that must be specified.For example,
some games may need to specify how many hits a unit can take before it dies,
while other games don�t.In order for
the xml file to be flexible enough to handle all games the concept of an
attachment is used.

An attachment is a set of name:value pairs.<span
style="mso-spacerun: yes">  This set of name value pairs can be attached
to unit types, players, territories, or resources.

This tells the xml parser to create an instance of
UnitAttachment, attach it to the infantry unit type, and initialize the
attachment with an attack value of 1, and a defense value of 2.

The game parser uses the java bean syntax for setting the
attachment values.If an attachment
has an option name of code, then it will call setCode(value) to set the
attachments value.The setCode method
will always use a string as its argument.

After an attachment is created, the engine validates it by
calling validate().This allows the
attachment to throw a GameParseException if it is initialized with an invalid
state.Throwing a game parse exception
will stop the parser, and terminate the program.

getAttachment,
where key is the name of the attachment.
In the above example key would be unitAttachment.  Objects implementing the Attachable
interface are PlayerID, Resource, Territory, and UnitType.

##

Turn based strategy games generally go through a series of
well defined steps.In axis and allies,
these steps are tech roll, purchase, combat move, battle, non combat move,
place, collect resources.

The logic for each step is encapsulated in a delegate.

A delegate is the game logic for one step of the game.<span
style="mso-spacerun: yes">  In axis and allies there would be a delegate
for movement, a delegate for purchase, a delegate for placing etc.<span
style="mso-spacerun: yes">   A delegate is responsible for validating
player actions, and updating the game state to reflect player actions.

Ideally delegates would be independent of each other, and
could be mixed and matched.For example
if a risk game and an axis and allies game were both implemented in using the
TripleA framework, then you should be able to create a new game that combines
the axis and allies battle logic with the movement logic of risk, simply by reusing
the respective delegates.Currently
this flexibility is not available because there is a degree of interdependence
between the delegates.In tripleA, the
battle delegate uses services supplied by the movement delegate, and vice
versa.Other dependencies exist as
well.

Delegates are restricted in what they are allowed to do. A
delegate must talk to game players and get random data through its
DelegateBridge.Also a delegate cannot
change the game data directly, but must use the ChangeFactory to create
changes, and then add them through the Delegates DelegateBridge.

In the game xml file you can specify the sequence of steps
the game goes through in the gamePlay tag.

```xml
<delegate name="battle" javaClass="BattleDelegate"/>
<delegate name="move" javaClass="MoveDelegate"/>

<step name="usMove" delegate="move" player="bush"/>
<step name="usFight" delegate="battle" player="bush"/>
<step name="canMove" delegate="move" player="chretian"/>
<step name="canFight" delegate="battle" player="chretian"/>
```

This tells the engine that there are two delegates, battle
and move, and that there are four steps, usMove, usFight, canMove, canFight,
and that the game cycles through these 4 steps until the game ends.

 The delegates start method is called.

 If a GamePlayer is specified in the game xml file for the step, then the
 players start method is called. The argument to start is the name of the
 step specified in the game xml file.

 The delegates end method is called. Signifying that the step is over.

###

To preserve network transparency game data is not changed
directly by the delegates. All changes
to game data are made by through the DelegateBridge.addChange(Change
aChange). This allows the game engine
to synchronize changes in game data between machines.

The Change class encapsulates a change of game data. Changes are created using the ChangeFactory
class.

For example, a Delegate wanting to change the owner of a
territory would use the following code,

```java
Change aChange = ChangeFactory.changeOwner(aTerritory, aPlayer);
```

##

A GamePlayer is responsible for making moves in a game.<span
style="mso-spacerun: yes">  A game player can be anything from a GUI to
an AI to a PBEM interface.

A GamePlayer sends messages to the current game delegate
saying what moves the player would like to make. The Delegate will then
validate the move, and if the move is valid, alter the GameData.

##

Communication between delegates and game players is done
through a simple remote method invocation scheme.

A Delegate can send a message to any GamePlayer while it is
executing (ie after its start method is called, and before its end method
returns) and a GamePlayer can send a message to the Delegate that is currently
running while the GamePlayer is after its start method is called and before its
start method returns.

###

Because delegates and game players may not be on the same
machine, communication is done through Bridges.

A delegate can send a message to a player through its
delegateBridge using the sendMessage(..) functions.

A GamePlayer can only send a message to the games current
delegate. The message is sent using the
GamePlayers PlayerBridge.

The game engine takes care of routing these messages across
the network if necessary.
