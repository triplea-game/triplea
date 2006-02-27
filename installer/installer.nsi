;--------------------------------

;Solid seems to fail at times
;SetCompressor /SOLID lzma
SetCompressor  lzma

; The name of the installer
Name "TripleA Installer"

; The file to write
OutFile "triplea_installer.exe"

; The default installation directory
InstallDir $PROGRAMFILES\TripleA

;--------------------------------

; Pages

Page directory
Page instfiles

;--------------------------------

; The stuff to install
Section "" ;No components page, name is not important

  ; Set output path to the installation directory.
  SetOutPath $INSTDIR
  
  ; Put file there
  File /r triplea* 

  ;create the shortcut
  CreateDirectory $SMPROGRAMS\TripleA\TripleA_${version}
  CreateShortCut "$SMPROGRAMS\TripleA\TripleA_${version}\triplea.lnk" "$INSTDIR\triplea_${version}\triplea.exe"

  
  
SectionEnd ; end the section

