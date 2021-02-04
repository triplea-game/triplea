There's an initiative to remove reflection from TripleA code. This is a list of where it is used (from: https://github.com/triplea-game/triplea/issues/3865):
This issue is created in order to discusso on how to eliminate the use of reflection on certain places in the game engine code:

#### GameDataManager.java `Class.forName(String).getDeclaredConstructor().newInstance()`
This class seems to be responsible for creating delegates from savegames via reflection.
I tried using XMLGameElementMapper instead of this, but unfortunately this class doesn't have mappings for all delegates, so loading a savegame often results in an exception.
The question I'm asking myself now is if I should just add the "missing bindings" to the Delegate map, or if I should create a different map, inheriting from it in order to prevent game xmls to load the currently non-default-loadable delegates.
@DanVanAtta Because you added this class in commit a38df933caa0b29cf0b8a5b2a5e6e61ba6bd583f, did you leave out the other Delegates on purpose, or did you just add the ones that were being used in existing game xmls?

#### RemoteMethodCall.java `Class#forName(String)`
I believe this class is responsible for getting reference to remote interface methods in order to use them.
From my quick analysis there is no checking what kind of class reference we even retrieve here, but I believe normally it should be one that is listen somewhere as reference in a RemoteName instance.
Maybe @ssoloff has some insight as he's been refactoring a lot of this stuff recently?
If my assumption is correct, it would probably be a good idea to use a HashMap here as well to avoid the use of reflection.

#### EndPoint.java `Method#*`, `Class#getMethod`
This seems to be the class that actually invokes methods on remote interfaces.
Also in `EndPoint#invokeSingle` we find the only (indirect) non-test usage of `RemoteMethodCall#stringToClass` mentioned 1 option further up.
With some extended refactoring we could maybe change both to completely rely on RemoteMethodCalls being mapped to lambdas and therefore eliminate the weird effect of reflection there.
Not sure how this affects compatibility though.

#### RemoteInterfaceHelper.java `Class#getMethods`
This class is responsible to sort methods in a remote interface in a specific order and therefore to to assign them a unique index being sent over the network.
Here we could replicate this mapping by creating a map that stores the current index and maps it to a lambda or something so we could easily add new methods to the interface without breaking the compatibility because the index suddenly changed because of that.

#### MapPropertyWrapper.java `Class#getMethods`/`Class#getDelcaredField`
I'm not sure what exactly this class does, but it looks like it's only being used by map creator code in order to create a properties file based on fields of the MapProperties class?
Definitely weird, but should again be easily fixable by creating a simple map and mapping them to properties instead of having some weird field-based logic there.

#### MapPropertiesMaker.java `Class#getMethods`
This is a weird class too that seems to export field of the MapProperties class for map making purposes.
It seems to make use of methods starting with "out" in order to print stuff. I'm convinced there is an easier and much simpler way to achieve the same thing.
