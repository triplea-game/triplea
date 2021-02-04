List of some items known technical debt items in TripleA. These are practices/patterns in TripleA that are known to be bad. The purpose of this list is to identify these items and help us know which kinds of things we should continue and which other things we should be fixing.

### Tech Debt List
- all uses of Java reflection, these are not compile time safe
- virtually all instance of checks, these are brittle and not compile time safe
- logic code mixed in with UI code
- over-reliance on using inheritance to share code. Composition generally should be preferred
- Lack of IOC, [inversion of control](https://en.wikipedia.org/wiki/Inversion_of_control)

### Tech Debt in Test
- Game rules are not divorced from GameData code. This means in test we have to find a map with the right rules we want to test and parse it to gain a 'GameData' object. It is then difficult to vary state of that 'GameData' object. The fix here would be to continue decoupling 'Gamedata' object from rules so we can test those rules on their own. We can also look into building a layer that can more easily load a 'GameData' object.
- General lack of testing. It's easy to add features, without adding automated tests it becomes very difficult to modify or update those features again (as the manual testing needs to be repeated, manual testing is not reliable and can often be not thorough enough, and has to be repeated).
