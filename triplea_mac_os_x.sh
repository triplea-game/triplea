#!/bin/sh

if ! java -version >& /dev/null
then
echo "Could not find Java."
echo "You must have Java installed and in your path."
exit
fi

relativePathToGame=`dirname $0`
cd $relativePathToGame

export PATH=/System/Library/Frameworks/JavaVM.framework/Versions/1.5/Home/bin/:$PATH
java -Xmx128m -cp bin/triplea.jar -Dapple.laf.useScreenMenuBar=true -Xdock:name="TripleA" -Xdock:icon="./icons/triplea_icon.png" games.strategy.engine.framework.GameRunner
