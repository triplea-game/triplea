#!/bin/bash

set -ev

if [ ! -d artifacts ]; then
  echo "Could not find artifacts folder from: $(pwd), current contents: $(ls)"
  exit -1
fi

mkdir -p winInstallTmp

cp artifacts/triple*zip winInstallTmp/
cp installer/installer.nsi winInstallTmp/
cd winInstallTmp
makensis -Dversion=$TRAVIS_TAG installer.nsi
cp triplea_installer.exe ../artifacts/
cd ..

