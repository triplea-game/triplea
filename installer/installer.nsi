;--------------------------------

;Solid seems to fail at times
;SetCompressor /SOLID lzma
SetCompressor  lzma

; The name of the installer
Name "TripleA"

; The file to write
OutFile "triplea_installer.exe"

; The default installation directory
InstallDir $PROGRAMFILES\TripleA

;--------------------------------

; Pages

Page directory
Page instfiles
UninstPage uninstConfirm
UninstPage instfiles

;--------------------------------


; The stuff to install
Section "Installer" ;No components page, name is not important

  ; Set output path to the installation directory.
  SetOutPath $INSTDIR

  ; Put file there
  File /r triplea*

  ;create the shortcut
  CreateDirectory $SMPROGRAMS\TripleA\TripleA_${version}
  CreateShortCut "$SMPROGRAMS\TripleA\TripleA_${version}\TripleA.lnk" "$INSTDIR\triplea_${version}\triplea.exe"
  CreateShortCut "$SMPROGRAMS\TripleA\TripleA_${version}\Uninstall.lnk" "$INSTDIR\triplea_${version}\uninstaller.exe"

  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\TripleAVersion${version}" \
                 "DisplayName" "TripleA Version ${version}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\TripleAVersion${version}" \
                 "UninstallString" "$INSTDIR\TripleA_${version}\uninstaller.exe"

  WriteUninstaller $INSTDIR\TripleA_${version}\uninstaller.exe

SectionEnd ; end the section

; The Uninstaller
Section "Uninstall"
  Delete $INSTDIR\uninstaller.exe
  RMDir /r $INSTDIR
  RMDIR /r $SMPROGRAMS\TripleA\TripleA_${version}
  RMdir $SMPROGRAMS\TripleA
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\TripleAVersion${version}"
SectionEnd




