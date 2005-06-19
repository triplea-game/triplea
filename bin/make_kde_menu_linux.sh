#!/bin/sh
echo "TripleA KDE-startfile creator.";

# Default to the users home directory
pathToDesktop="$HOME/Desktop"

echo ""
echo -n "Enter path to your desktop [$pathToDesktop]:"
read givenPath
if [ -z "$givenPath" ] # Use the default
then
givenPath=$pathToDesktop
fi

# Full path name
pathToDesktopFile="$givenPath/Triplea.desktop"

# Loop until we can write to the file
while [ ! -w $givenPath -o -e $pathToDesktopFile -a ! -w $pathToDesktopFile ]
do
echo "Could not write to file [$pathToDesktopFile]."
echo ""
echo -n "Enter path to your desktop(exit with control-c):"
read givenPath
pathToDesktopFile="$givenPath/Triplea.desktop"
done

# Create the file
relativePathToGame=`dirname $0`
cd $relativePathToGame
absolutePathToGame=`pwd`
echo "[Desktop Entry]" > $pathToDesktopFile
echo "Name=TripleA" >> $pathToDesktopFile
echo "Comment=Strategy Engine Framework" >> $pathToDesktopFile
echo "Exec=$absolutePathToGame/triplea_unix.sh" >> $pathToDesktopFile
echo "Icon=$absolutePathToGame/../icons/triplea_icon.ico" >> $pathToDesktopFile
echo "Terminal=0" >> $pathToDesktopFile
echo "Type=Application" >> $pathToDesktopFile

echo "Created file [$pathToDesktopFile]."
echo ""
