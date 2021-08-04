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
REM BFCPEVERVERSION=${project.version}
REM BFCPEVERPRODUCT=${app.name}
REM BFCPEVERDESC=${project.description}
REM BFCPEVERCOMPANY=${app.menu}
REM BFCPEVERCOPYRIGHT=${app.copyright}
REM BFCPEEMBED=${project.build.directory}\packaging\run.bat
REM BFCPEOPTIONEND
@ECHO ON
@ECHO OFF
CD .\app
..\bin\java.exe -jar .\${project.build.finalName}.jar
EXIT
