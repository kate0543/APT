## Folder Structure
<!-- Write script to auto create file folder structure for all downloaded data -->
<!-- Qlikview Need Mannually rename -->
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

- Copy `RenameProgrammeReportWB.vbs` into data ProgrammeCode folder ('rawData-ProgrammeCode')
- Note: for each programe, you need to make sure copy this script to each programme folder
- Double click to run the script
- Copy `ProcessExcelToCSV.vbs`  into the main folder (`rawData`).
- Note: you must follow the sequece of these steps when run scripts
- Double click to run the script
- Alternatively Run the scripts via command line (`cscript script_name.vbs`)  

### 4. Generate Priority Group

1. Run the `APP_DEMO` app.
    - If not installed, double-click `APP_DEMO_Installer.exe`.
2. When the UI loads:
    - Choose the root folder (the new folder you created; by default, look for the `csvData` folder in the parent directory).
    - Choose the target folder to save the generated priority list and app runtime log files.
    - Select the Programme Code from the dropdown menu.
    - Select the Priority Reason from the dropdown menu.
        - *Note: Only "All Reasons" and "Low Attendance" set the low attendance threshold.*
    - Click **Load Data** to set up the data pipeline.
    - Click **Generate** to create the priority list.
    - Click **Reset** to change dataset, programme code, or priority reasons.
    - Alternatively, click **Export CSV** to save the exact priority list file.

### 5. Note To Update Data

When getting new data, you need to remove both the old raw data and the corresponding converted clean data.

For example, for the Source Document Data:
- Need to regularly check for DDL updates.
- Remove the existing raw data file: `rawData/ProgrammeCode/Source/SBS Source 24-25.xlsx`
- Remove the existing converted CSV data: `csvData/ProgrammeCode/Source/SBS Source 24-25_All Assessments Main Campus.csv` (Assuming `csvData` is the output folder)
- Re-run `ProcessExcelToCSV.vbs`.
