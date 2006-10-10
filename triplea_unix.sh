if ! java -version > /dev/null 2>&1
then
echo "Could not find Java."
echo "You must have Java installed and in your path."
exit
fi

relativePathToGame=`dirname $0`
cd $relativePathToGame


java -Xmx128m -cp bin/triplea.jar games.strategy.engine.framework.GameRunner
