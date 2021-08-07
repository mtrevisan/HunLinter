@ECHO OFF
CD .\app
..\bin\java.exe -jar .\${project.build.finalName}.jar
EXIT
