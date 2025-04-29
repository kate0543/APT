; -- APT_Installer.iss --
[Setup]
AppName=APT Application
AppVersion=1.0
AppPublisher=Your Company
DefaultDirName={autopf}
DefaultGroupName=APT
OutputDir=.\Installer
OutputBaseFilename=APT_Setup
Compression=lzma2
SolidCompression=yes
ArchitecturesAllowed=x64
ArchitecturesInstallIn64BitMode=x64

[Files]
; Main application files
Source: "APT_DEMO\APT.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "APT_DEMO\APT.jar"; DestDir: "{app}"; Flags: ignoreversion
Source: "APT_DEMO\SBS_LOGO.ico"; DestDir: "{app}"; Flags: ignoreversion

; JRE (include entire folder)
Source: "APT_DEMO\jre\*"; DestDir: "{app}\jre"; Flags: ignoreversion recursesubdirs

[Icons]
Name: "{group}\APT"; Filename: "{app}\APT.exe"; IconFilename: "{app}\SBS_LOGO.ico"
Name: "{commondesktop}\APT"; Filename: "{app}\APT.exe"; IconFilename: "{app}\SBS_LOGO.ico"

[Run]
Filename: "{app}\APT.exe"; Description: "Run APT"; Flags: postinstall nowait skipifsilent