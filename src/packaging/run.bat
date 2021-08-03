@ECHO OFF
REM BFCPEOPTIONSTART
REM Advanced BAT to EXE Converter www.BatToExeConverter.com
REM BFCPEEXE=${project.build.directory}\${jvm.outputFolder}\run.exe
REM BFCPEICON=${project.basedir}\app-icon.ico
REM BFCPEICONINDEX=1
REM BFCPEEMBEDDISPLAY=0
REM BFCPEEMBEDDELETE=1
REM BFCPEADMINEXE=0
REM BFCPEINVISEXE=1
REM BFCPEVERINCLUDE=1
REM BFCPEVERVERSION=2.0.1.0
REM BFCPEVERPRODUCT=HunLinter
REM BFCPEVERDESC=${project.description}
REM BFCPEVERCOMPANY=${app.menu}
REM BFCPEVERCOPYRIGHT=(c) 2021 Mauro Trevisan
REM BFCPEEMBED=${project.build.directory}\packaging\run.bat
REM BFCPEOPTIONEND
@ECHO ON
@echo off
cd .\app
..\bin\java.exe -jar .\HunLinter-2.0.1-SNAPSHOT.jar
exit
