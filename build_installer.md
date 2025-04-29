
APT/
├──APT_DEMO/ 
    ├── jre/
    ├── Installer/
    ├── SBS_LOGO.ico
    ├── APT_Installer.iss
    ├── APT.xml
    ├── APT.jar
    └── APT.exe
  
    



To xcopy  JRE, in cmd, cd project path then
"C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot\bin\javac" ^
More? -source 17 -target 17 ^
More? -d bin ^
More? src\Main.java
warning: [options] system modules path not set in conjunction with -source 17
1 warning

Foolproof JAR Creation
cd /d "C:\Users\cocon\Documents\GitHub\APT"

:: 1. Delete existing files
del APT_DEMO\APT.jar 2>nul
rmdir /s /q bin 2>nul
mkdir bin

:: 2. Compile with package awareness
"C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot\bin\javac" ^
-d bin ^
src\Main.java

:: 3. Build JAR - METHOD THAT ALWAYS WORKS
"C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot\bin\jar" ^
cvfe APT_DEMO\APT.jar src.Main ^
-C bin .

Verification Steps
cmd
:: A. Check JAR structure
"C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot\bin\jar" tf APT_DEMO\APT.jar

:: B. Test both execution methods
java -jar APT_DEMO\APT.jar
java -cp APT_DEMO\APT.jar src.Main

Optimize Your JRE
"C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot\bin\jlink.exe" ^
More? --module-path "C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot\jmods" ^
More? --add-modules java.base,java.desktop,java.sql ^
More? --output "C:\Users\cocon\Documents\GitHub\APT\APT_DEMO\jre" ^
More? --strip-debug --no-header-files --no-man-pages --compress=2
Warning: The 2 argument for --compress is deprecated and may be removed in a future release

To build EXE, install Luanch4j in cmd
cd "C:\Users\cocon\Documents\GitHub\APT"
"C:\Program Files (x86)\Launch4j\launch4jc.exe" APT.xml
Alternative Quick Build
cd "C:\Users\cocon\Documents\GitHub\APT"
"C:\Program Files (x86)\Launch4j\launch4jc.exe" ^
--output "APT_DEMO\APT.exe" ^
--jar "APT_DEMO\APT.jar" ^
--main-class "src.Main" ^
--jre-min-version "21.0.0" ^
--bundled-jre-path "./jre" ^
--bundled-jre-64-bit ^
--icon "APT_DEMO\SBS_LOGO.ico" ^
--header-type "gui"
To create installer, install Inno Setup
Create APT_Installer put in APT_DEMO