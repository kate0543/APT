```plaintext
APT/
├── APT_DEMO/
│   ├── jre/
│   ├── Installer/
│   ├── SBS_LOGO.ico
│   ├── APT_Installer.iss
│   ├── APT.xml
│   ├── APT.jar
│   └── APT.exe
```

## Building and Packaging Instructions

### 1. Compile Java Source

Open `cmd`, navigate to your project path:

```cmd
cd /d "C:\Users\cocon\Documents\GitHub\APT"
```

Delete old files and prepare directories:

```cmd
del APT_DEMO\APT.jar 2>nul
rmdir /s /q bin 2>nul
mkdir bin
```

Compile with package awareness:

```cmd
"C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot\bin\javac" -d bin src\Main.java
```

> **Note:**  
> You may see:  
> `warning: [options] system modules path not set in conjunction with -source 17`  
> This warning can be ignored for most use cases.

### 2. Build the JAR

```cmd
"C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot\bin\jar" cvfe APT_DEMO\APT.jar src.Main -C bin .
```

### 3. Verify the JAR

Check JAR structure:

```cmd
"C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot\bin\jar" tf APT_DEMO\APT.jar
```

Test execution:

```cmd
java -jar APT_DEMO\APT.jar
java -cp APT_DEMO\APT.jar src.Main
```

### 4. Optimize Your JRE

Create a minimal JRE using `jlink`:

```cmd
"C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot\bin\jlink.exe" ^
    --module-path "C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot\jmods" ^
    --add-modules java.base,java.desktop,java.sql ^
    --output "C:\Users\cocon\Documents\GitHub\APT\APT_DEMO\jre" ^
    --strip-debug --no-header-files --no-man-pages --compress=2
```

> **Warning:**  
> The `--compress=2` argument is deprecated and may be removed in a future release.

### 5. Build the EXE

Install [Launch4j](http://launch4j.sourceforge.net/), then run:

```cmd
cd "C:\Users\cocon\Documents\GitHub\APT"
"C:\Program Files (x86)\Launch4j\launch4jc.exe" APT.xml
```

**Alternative Quick Build:**

```cmd
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
```

### 6. Create the Installer

Install [Inno Setup](https://jrsoftware.org/isinfo.php).  
Create the installer script (`APT_Installer.iss`) and place it in `APT_DEMO`.
