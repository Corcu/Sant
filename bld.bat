@echo off
setlocal EnableDelayedExpansion

set CALYPSO_HOME=..\..
set CURRENT_PARAMETER=

for %%x in (%*) do (
   IF "!CURRENT_PARAMETER!"=="-PcalypsoHome" (
       set CALYPSO_HOME=%%x
   )

   set CURRENT_PARAMETER=%%x
   
   IF "!CURRENT_PARAMETER:~0,14!"=="-PcalypsoHome=" (
       set CALYPSO_HOME=!CURRENT_PARAMETER:~14!
   )
)

set GRADLE_HOME=%CALYPSO_HOME%\tools\gradle

call %GRADLE_HOME%\bin\gradle.bat %*

endlocal
