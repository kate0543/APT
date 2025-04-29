## Folder Structure

```plaintext
rawData/                # (You can customize this folder name)
├── EBR/                # Save EBR system reports here
├── IYR/                # Save IYR report here
├── Qlikview/           # Save Qlikview reports here
├── Source/             # Save current school source document here
├── StEP/               # Save StEP attendance report here
├── ProcessExcelToCSV.vbs
└── RenameProgrammeReportWB.vbs
```

## Steps

### 1. Prepare Folders

1. Create a main folder (e.g., `rawData`).
2. Inside it, create the following subfolders:
    - `EBR`
    - `IYR`
    - `Qlikview`
    - `Source`
    - `StEP`

### 2. Download and Save Reports

- **EBR Reports**
  - Download programme reports from the EBR system and save them in the `EBR` folder.
  - For L4, L5, L6.
  - For programmes with placement (S) and normal.
  - For the last four years (e.g., if the current academic year is 24-25, download from 21-22, 22-23, 23-24, and 24-25).
  - *Maximum: 24 documents.*

- **Qlikview Reports**
  - Download the following from the Qlikview system and save in the `Qlikview` folder:
    - Module by Registration Status
    - Programme with Registration Status
  - For both programme with placement (S) and normal.
  - *Total: 4 files.*

- **Source Document**
  - Download the current school source document and save in the `Source` folder.
  - *Total: 1 file.*

- **IYR Report**
  - Download the IYR report and save in the `IYR` folder.
  - *Total: 1 file.*

- **StEP Attendance Report**
  - Obtain the StEP attendance report (currently from Chris) and save in the `StEP` folder.
  - *Total: 1 file.*

### 3. Prepare Scripts

- Copy `ProcessExcelToCSV.vbs` and `RenameProgrammeReportWB.vbs` into the main folder (`rawData`).
- Run the scripts via command line (`cscript script_name.vbs`) or by double-clicking the `.vbs` files.

### 4. Generate Priority Group

1. Run the `APP_DEMO` app.
    - If not installed, double-click `APP_DEMO_Installer.exe`.
2. When the UI loads:
    - Choose the root folder (the new folder you created; by default, look for the `Data` folder in the parent directory).
    - Choose the target folder to save the generated priority list and app runtime log files.
    - Select the Programme Code from the dropdown menu.
    - Select the Priority Reason from the dropdown menu.
        - *Note: Only "All Reasons" and "Low Attendance" set the low attendance threshold.*
    - Click **Load Data** to set up the data pipeline.
    - Click **Generate** to create the priority list.
    - Click **Reset** to change dataset, programme code, or priority reasons.
    - Alternatively, click **Export CSV** to save the exact priority list file.
