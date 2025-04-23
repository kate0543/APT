# APT - Academic Progress Tracking

Welcome to the APT project! This repository contains tools designed to process student and module data, primarily from Qlikview exports, Exam Board Reporter (EBR) files, additional source documents, and In-Year Retrieval (IYR) files, to aid in academic progress tracking and student support prioritization at Salford Business School.

## Features

-   Reads and parses student programme and module enrolment data from Qlikview CSV files.
-   Reads and parses module component results and programme-level module records from EBR CSV files (Module Reports and Programme Reports).
-   Reads and parses additional module/component details (e.g., deadlines, module leaders, component codes) from designated "Source" CSV files.
-   Associates modules and components with the corresponding students based on Banner ID.
-   Identifies students based on specific programme codes.
-   Merges data from Qlikview, EBR, and Source documents for a comprehensive view via the `StEP` class.
-   Provides a structure for analyzing student progression (e.g., identifying failed components, trailing modules).
-   Includes verification steps, such as comparing component counts between EBR and Source data.
-   **NEW:** Identifies "priority" students based on configurable criteria (e.g., failed components, overdue assignments, attendance, registration status) using `DataPipeline.java`.
-   **NEW:** Specifically flags students with overdue, unsubmitted components.
-   **NEW:** Processes 'In Year Retrieval' (IYR) data from specific CSV files to flag relevant components using `IYR.java`.

## Getting Started

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/your-username/APT.git
    ```
2.  **Navigate to the project directory:**
    ```bash
    cd APT
    ```
3.  **Prepare Data:**
    -   Ensure your Qlikview, EBR, and Source CSV files are placed in the appropriate subdirectories within `data/BMT/` (e.g., `data/BMT/Qlikview/`, `data/BMT/EBR/`, `data/BMT/Source/`). Update the `baseFolderPath` in relevant Java files (`StEP.java`, `DataPipeline.java`, `IYR.java`) if using a different base location.
    -   Ensure any In-Year Retrieval (IYR) CSV files (e.g., `*IYR*.csv`) are placed directly within the `baseFolderPath` (e.g., `data/BMT/`).
    -   Files should follow the naming conventions expected by the respective processing classes (`StEP.java`, `Qlikview.java`, `EBR.java`, `SourceDoc.java`, `IYR.java`). Refer to the source code of these classes for specific naming patterns and expected file locations.
4.  **Compile the Java code:**
    ```bash
    # Assuming source files are in a 'src' directory and output to 'bin'
    javac -d bin src/*.java
    ```
5.  **Run the application:**
    You can run different pipelines depending on your needs. To run the priority student identification pipeline (which may include IYR processing depending on implementation):
    ```bash
    # Example: Run the priority pipeline via DataPipeline
    java -cp bin src.DataPipeline
    ```
    *Note: Modify the `targetProgrammeCodesList` and `baseFolderPath` within the relevant Java files (`StEP.java`, `DataPipeline.java`, `IYR.java`) to specify which programmes and data locations to process.*
    *Note: The `IYR.java` class also contains a `main` method, likely for testing IYR processing independently.*

## Contributing

We welcome contributions! Please read our [contributing guidelines](CONTRIBUTING.md) before submitting a pull request.

## Project Background

The APT project was developed at Salford Business School, University of Salford, to provide a tool for processing and analyzing student academic data derived from Qlikview exports, Exam Board Reporter (EBR) files, IYR files, and other source documents. It aims to simplify the tracking of student progression, module enrolment, component-level results including deadlines, and to help identify students who may require additional support or are subject to specific processes like In-Year Retrieval.

## Classes and Usage

### Core Classes

-   **`StEP.java`** (Student Engagement Platform - Assumed): Acts as the primary data fetching and integration class. It likely orchestrates calls to fetch data from Qlikview, EBR, and Source documents, consolidating them into `Student`, `Module`, and `Component` objects. Called by `DataPipeline.java`.
-   **`DataPipeline.java`**: Provides a workflow specifically focused on identifying "priority" students. It calls `StEP.fetchStudents` to get the consolidated data and then applies logic (`calculatePriorityGroup`, `priorityUpdateComponent`) to flag students based on criteria like failed components or overdue assignments. May also incorporate IYR data processing by calling methods from `IYR.java`. Can serve as a main entry point for this specific analysis.
-   **`Qlikview.java`**: Reads and parses initial student and module enrolment data from Qlikview CSV exports. Creates `Student` and `Module` objects. Likely called by `StEP.java`.
-   **`EBR.java`**: Processes Exam Board Reporter (EBR) CSV files (`ModuleReport` and `ProgrammeReport`). Parses component details and module records. Likely called by `StEP.java`.
-   **`SourceDoc.java`**: Reads additional details (deadlines, codes, leaders) from "Source" CSV files and merges them. Performs verification checks. Likely called by `StEP.java`.
-   **`IYR.java`**: Reads 'In Year Retrieval' (IYR) CSV files located in the base data folder (files containing "IYR" and ending in `.csv`). Identifies students and components mentioned in these files and sets an `isComponentIYR` flag on the corresponding `Component` objects. Provides methods to locate IYR files (`locateIYRFiles`) and update student data (`updateIYRComponents`).
-   **`Student.java`**: Represents a student, holding details like Banner ID, name, programme code, registration status, residency, and a list of associated `Module` objects. Includes logic (`checkTrailingModules`, `checkFailedComponents`, `getFailedComponents`) and attributes (`priorityReasons`) related to progression and priority status. Includes methods like `updateReason` and `getPriorityReasons`.
-   **`Module.java`**: Represents a module, containing information such as CRN, module ID, title, year, level, credits, registration status, module leader, module admin team, and a list of associated `Component` objects.
-   **`Component.java`**: Represents a module component (e.g., an assessment). Holds details like title, code, type, weight, student's mark/status, submission deadline, and a flag (`componentIYR`) indicating if it's subject to In Year Retrieval. Includes logic (`hasFailed`, `getComponentDeadline`, `getComponentStatus`) relevant to priority checks.

### Input Data Format

The application expects CSV files in specific structures, processed by the relevant classes:

-   **Qlikview Data:**
    -   **Location:** `data/BMT/Qlikview/` (configurable).
    -   **Content:** Student demographics, programme registration, module enrolment.
    -   **Processed by:** `Qlikview.java` (likely via `StEP.java`).
-   **EBR Data:**
    -   **Location:** `data/BMT/EBR/` (configurable).
    -   **Content:** Module Report (component results), Programme Report (overall module records).
    -   **Processed by:** `EBR.java` (likely via `StEP.java`).
-   **Source Data:**
    -   **Location:** `data/BMT/Source/` (configurable).
    -   **Content:** Component deadlines, codes, module leaders, admin teams.
    -   **Processed by:** `SourceDoc.java` (likely via `StEP.java`).
-   **IYR Data:**
    -   **Location:** `data/BMT/` (configurable `baseFolderPath`). Files must contain "IYR" in the name and end with `.csv`.
    -   **Content:** Expected to contain at least Banner ID, Module CRN, and Component Title to identify components for IYR flagging.
    -   **Processed by:** `IYR.java`.

Refer to the respective classes for detailed format and naming convention requirements.

### Usage

1.  Configure the `baseFolderPath` and `targetProgrammeCodesList` variables in `src/StEP.java`, `src/DataPipeline.java`, and potentially `src/IYR.java` as needed.
2.  Place the corresponding Qlikview, EBR, Source, and IYR CSV data files in the correct directory structures (e.g., `data/BMT/Qlikview/`, `data/BMT/EBR/`, `data/BMT/Source/`, `data/BMT/`).
3.  Compile the Java source files (`javac -d bin src/*.java`).
4.  Run the desired main processing class. For priority student analysis:
    ```bash
    java -cp bin src.DataPipeline
    ```
    The processing order for this pipeline is generally:
    a.  `DataPipeline.main` is executed.
    b.  `StEP.fetchStudents` is called, which internally fetches and merges data from Qlikview, EBR, and Source files into a list of `Student` objects.
    c.  (Optional/Integrated) IYR data may be processed by calling `IYR.updateIYRComponents`.
    d.  `DataPipeline.fetchPriorityStudents` identifies students meeting general priority criteria using `calculatePriorityGroup`.
    e.  `DataPipeline.priorityUpdateComponent` identifies students with overdue components.
    f.  Results (lists of priority students, potentially including IYR status) are printed to the console.
5.  The program will process the files and print status messages, verification warnings, and the priority analysis results to the console.

Example snippet from `DataPipeline.java` showing the priority pipeline orchestration:

```java
// Inside the main method of DataPipeline.java
String baseFolderPath = "data/BMT/";
List<String> targetProgrammeCodesList = List.of("BMT.S", "BMT.F"); // Target programme codes

// Fetch students using StEP (which handles Qlikview, EBR, Source integration)
List<Student> students = new ArrayList<>();
try {
    students = StEP.fetchStudents(students, baseFolderPath, targetProgrammeCodesList);
} catch (java.io.IOException e) {
    // ... error handling ...
}

// Locate and process IYR files (Example integration point)
try {
    List<File> iyrFiles = IYR.locateIYRFiles(baseFolderPath);
    students = IYR.updateIYRComponents(students, iyrFiles);
    System.out.println("IYR component update complete.");
} catch (Exception e) {
    System.err.println("Error processing IYR files: " + e.getMessage());
    // Decide if processing should continue or halt
}


DataPipeline pipeline = new DataPipeline();

// Identify and print general priority students
System.out.println("=== Priority Students ===");
priorityStudents = pipeline.fetchPriorityStudents(students);
// ... printing logic ...

// Identify and print priority students with overdue components
System.out.println("\n=== Priority Students with Overdue Components ===");
List<Student> overduePriority = pipeline.priorityUpdateComponent(students);
// ... printing logic ...

// Example: Print students with IYR components (using IYR helper method)
System.out.println("\n=== Students with IYR Components ===");
List<Student> iyrStudents = IYR.getStudentsWithIYR(students);
// ... printing logic ...
```

## Author

This project was created and is maintained by Dr. Kate Han, Salford Business School, University of Salford. For inquiries, please contact Dr. Han at [k.han3@salford.ac.uk](mailto:k.han3@salford.ac.uk).

## License

This project is licensed under the MIT License. Copyright is held by Salford Business School, University of Salford.

