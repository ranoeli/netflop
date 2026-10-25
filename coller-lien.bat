@echo off
rem Tape le contenu du presse-papiers du PC dans le champ selectionne de l'emulateur
rem (le champ doit etre en mode saisie : clavier de la TV affiche)
powershell -NoProfile -Command ^
  "$t = (Get-Clipboard -Raw).Trim() -replace ' ', '%%s';" ^
  "& \"$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe\" shell input text \"'$t'\""
echo Lien envoye a l'emulateur.
timeout /t 2 >nul
