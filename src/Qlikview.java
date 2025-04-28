package src;

import java.io.*;
import java.util.*;

// TODO: Record which module is trailing

public class Qlikview {
    public static void main(String[] args) {
        String baseFolderPath = "data/BMT/"; // Replace with the actual base folder path
        // Split by "/" and get the subfolder name (last non-empty part)
  ;
        String targetProgrammeCode = "BMT";// Replace with the actual target programme codes
        String logFolderPath = DataPipeline.getLogFolderPath(targetProgrammeCode);
        List<Student> students = new ArrayList<>(); // List to store all students

        try {
            students = fetchStudents(baseFolderPath, targetProgrammeCode);

            if (!students.isEmpty()) {
                for (Student student : students) {
                    // Uncomment to print students with trailing modules
                    // System.out.println(student.toString());
                }
            } else {
                System.out.println("No students found for the specified programme codes.");
            }
        } catch (IOException e) {
            System.err.println("An error occurred while reading Qlikview data: " + e.getMessage());
            e.printStackTrace();
        }
 
    }

    public static List<Student> fetchStudents(String baseFolderPath, String targetProgrammeCode) throws IOException {
        List<Student> students = new ArrayList<>();
        String logFolderPath = DataPipeline.getLogFolderPath(targetProgrammeCode);
        List<String> targetProgrammeCodesList = List.of(targetProgrammeCode+".S", targetProgrammeCode+".F"); // Replace with the actual target programme codes
        for (String code : targetProgrammeCodesList) {
            try {
                students.addAll(locateQlikviewFiles(baseFolderPath + "/Qlikview/", logFolderPath,code));
                // System.out.println("Found " + students.size() + " students for programme code: " + targetProgrammeCode);
            } catch (IOException e) {
                System.err.println("An error occurred while reading Qlikview data: " + e.getMessage());
                e.printStackTrace();
            }
        }

        if (!students.isEmpty()) {
            for (Student student : students) {
                student.checkTrailingModules(); // Calculate trailing status for each student
            }
        } else {
            // System.out.println("No students found for the specified programme codes.");
        }

        System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("            Qlikview Data processing completed.");
        System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------------------------");

        return students;
    }

    public static List<Student> locateQlikviewFiles(String folderPath,String logFolderPath, String targetProgrammeCode) throws IOException {
        List<Student> students = new ArrayList<>();

        File folder = new File(folderPath);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".csv") && name.contains(targetProgrammeCode));

        if (files != null && files.length > 0) {
            // First pass: Read programme data to create Student objects
            for (File file : files) {
                if (file.getName().contains(targetProgrammeCode) && file.getName().contains("Programme")) {
                    students.addAll(readProgrammeData(file,logFolderPath)); // Read programme data from the file
                }
            }
            // Second pass: Add module data to the existing Student objects
            for (File file : files) {
                if (file.getName().contains("Module")) {
                    addModulesToStudents(file, logFolderPath,students); // Add module data to students
                }
            }
        } else {
            System.err.println("No matching CSV files found in folder: " + folder.getAbsolutePath());
        }

        return students;
    }

    public static List<Student> readProgrammeData(File file, String logFolderPath) throws IOException {
        List<Student> students = new ArrayList<>();
        File logDir = new File(logFolderPath);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        File logFile = new File(logDir, "Qlikview_readProgrammeData_log.csv");
        // Use try-with-resources for PrintWriter to ensure it's closed
        try (PrintWriter logWriter = new PrintWriter(new FileWriter(logFile, true));
             BufferedReader reader = new BufferedReader(new FileReader(file))) {

            String headerLine = reader.readLine(); // Read the header line
            if (headerLine == null) {
                DataPipeline.log(logWriter, "WARN", file.getName(), "File is empty.");
                return students; // Return empty list if file is empty
            }

            String[] headers = headerLine.split(",");
            Map<String, Integer> columnIndexMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                columnIndexMap.put(headers[i].trim(), i);
            }

            String line;
            int lineNumber = 1; // Start counting lines after header
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String[] fields = line.split(",");

                // Safely parse Banner ID
                int bannerID = 0;
                try {
                    bannerID = columnIndexMap.containsKey("Banner ID") && fields.length > columnIndexMap.get("Banner ID") && !fields[columnIndexMap.get("Banner ID")].trim().isEmpty()
                            ? Integer.parseInt(fields[columnIndexMap.get("Banner ID")].trim().replace("@", ""))
                            : 0;
                } catch (NumberFormatException e) {
                    DataPipeline.log(logWriter, "WARN", file.getName(), "Invalid Banner ID format on line " + lineNumber + ": " + (columnIndexMap.containsKey("Banner ID") && fields.length > columnIndexMap.get("Banner ID") ? fields[columnIndexMap.get("Banner ID")] : "N/A"));
                    continue; // Skip this record if Banner ID is invalid
                } catch (ArrayIndexOutOfBoundsException e) {
                    DataPipeline.log(logWriter, "WARN", file.getName(), "Missing Banner ID field on line " + lineNumber);
                    continue; // Skip this record if field is missing
                }


                // Safely get First Name
                String firstName = columnIndexMap.containsKey("First Name") && fields.length > columnIndexMap.get("First Name") && !fields[columnIndexMap.get("First Name")].trim().isEmpty()
                        ? fields[columnIndexMap.get("First Name")].trim()
                        : null;

                // Safely get Last Name
                String lastName = columnIndexMap.containsKey("Last Name") && fields.length > columnIndexMap.get("Last Name") && !fields[columnIndexMap.get("Last Name")].trim().isEmpty()
                        ? fields[columnIndexMap.get("Last Name")].trim()
                        : null;

                String name = (firstName != null && lastName != null) ? firstName + " " + lastName : null;

                // Safely get Programme Code
                String programmeCode = columnIndexMap.containsKey("Programme Code") && fields.length > columnIndexMap.get("Programme Code") && !fields[columnIndexMap.get("Programme Code")].trim().isEmpty()
                        ? fields[columnIndexMap.get("Programme Code")].trim()
                        : null;

                // Safely parse Programme Year
                int programmeYear = 0;
                 try {
                    programmeYear = columnIndexMap.containsKey("Programme Year") && fields.length > columnIndexMap.get("Programme Year") && !fields[columnIndexMap.get("Programme Year")].trim().isEmpty()
                            ? Integer.parseInt(fields[columnIndexMap.get("Programme Year")].trim())
                            : 0;
                 } catch (NumberFormatException e) {
                    DataPipeline.log(logWriter, "WARN", file.getName(), "Invalid Programme Year format on line " + lineNumber + " for Banner ID " + bannerID + ": " + (columnIndexMap.containsKey("Programme Year") && fields.length > columnIndexMap.get("Programme Year") ? fields[columnIndexMap.get("Programme Year")] : "N/A"));
                    // Decide if you want to skip or use default 0
                 } catch (ArrayIndexOutOfBoundsException e) {
                    DataPipeline.log(logWriter, "WARN", file.getName(), "Missing Programme Year field on line " + lineNumber + " for Banner ID " + bannerID);
                    // Decide if you want to skip or use default 0
                 }


                // Safely get Reg Status Code
                String programmeRegStatusCode = columnIndexMap.containsKey("Reg Status Code") && fields.length > columnIndexMap.get("Reg Status Code") && !fields[columnIndexMap.get("Reg Status Code")].trim().isEmpty()
                        ? fields[columnIndexMap.get("Reg Status Code")].trim()
                        : null;

                // Safely get Reg Status
                String programmeRegStatus = columnIndexMap.containsKey("Reg Status") && fields.length > columnIndexMap.get("Reg Status") && !fields[columnIndexMap.get("Reg Status")].trim().isEmpty()
                        ? fields[columnIndexMap.get("Reg Status")].trim()
                        : null;

                // Safely get Student Type
                String studentType = columnIndexMap.containsKey("Student Type Summary") && fields.length > columnIndexMap.get("Student Type Summary") && !fields[columnIndexMap.get("Student Type Summary")].trim().isEmpty()
                        ? fields[columnIndexMap.get("Student Type Summary")].trim()
                        : null;

                // Safely get Residency
                String studentResidency = columnIndexMap.containsKey("Residency") && fields.length > columnIndexMap.get("Residency") && !fields[columnIndexMap.get("Residency")].trim().isEmpty()
                        ? fields[columnIndexMap.get("Residency")].trim()
                        : null;

                if (bannerID == 0) {
                     DataPipeline.log(logWriter, "WARN", file.getName(), "Skipping record on line " + lineNumber + " due to missing or invalid Banner ID.");
                     continue; // Skip if Banner ID is essential and missing/invalid
                }


                Student student = new Student(
                        programmeYear,
                        programmeCode,
                        programmeRegStatusCode,
                        programmeRegStatus,
                        null, // Modules are added later from the Module CSV
                        bannerID,
                        0, // Network ID is not provided in the Programme CSV
                        name,
                        studentType,
                        false, // Is Trailing is calculated later
                        studentResidency
                );

                students.add(student);
            }
            DataPipeline.log(logWriter, "INFO", file.getName(), "Successfully read " + students.size() + " student records.");
        } catch (IOException e) {
            // Log the exception using a temporary writer if the main one failed or wasn't created
             try (PrintWriter errorWriter = new PrintWriter(new FileWriter(logFile, true))) {
                 DataPipeline.log(errorWriter, "ERROR", file.getName(), "IOException occurred while reading programme data: " + e.getMessage());
             } catch (IOException logEx) {
                 // If logging itself fails, print to stderr as a last resort
                 System.err.println("Failed to write error log for IOException in readProgrammeData: " + logEx.getMessage());
                 e.printStackTrace(); // Print original exception stack trace
             }
             throw e; // Re-throw the original exception
        } catch (Exception e) {
             // Catch unexpected runtime exceptions during processing
             try (PrintWriter errorWriter = new PrintWriter(new FileWriter(logFile, true))) {
                 DataPipeline.log(errorWriter, "ERROR", file.getName(), "Unexpected error occurred while processing programme data: " + e.getMessage());
             } catch (IOException logEx) {
                 System.err.println("Failed to write error log for unexpected exception in readProgrammeData: " + logEx.getMessage());
                 e.printStackTrace();
             }
             // Depending on the desired behavior, you might want to re-throw, wrap, or just log
             // For now, just logging and continuing might lose data, re-throwing is safer if possible
             throw new RuntimeException("Unexpected error processing file " + file.getName(), e);
        }

        return students;
    }

    public static void addModulesToStudents(File file,String logFolderPath, List<Student> students) throws IOException {
        File logDir = new File(logFolderPath);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        File logFile = new File(logDir, "Qlikview_addModulesToStudents_log.csv");

        // Use try-with-resources for PrintWriter to ensure it's closed
        try (PrintWriter logWriter = new PrintWriter(new FileWriter(logFile, true));
             BufferedReader reader = new BufferedReader(new FileReader(file))) {

            if (students.isEmpty()) {
                DataPipeline.log(logWriter, "WARN", file.getName(), "No students found in the list to add modules to.");
                return; // Nothing to do if the student list is empty
            }

            String headerLine = reader.readLine(); // Read the header line
            if (headerLine == null) {
                DataPipeline.log(logWriter, "INFO", file.getName(), "File is empty.");
                return; // Exit if file is empty
            }

            String[] headers = headerLine.split(",");
            Map<String, Integer> columnIndexMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                columnIndexMap.put(headers[i].trim(), i);
            }

            String line;
            int lineNumber = 1; // Start counting lines after header
            int modulesAddedCount = 0;
            int linesSkippedInvalidBannerID = 0;
            int linesSkippedStudentNotFound = 0;
            int linesSkippedParsingError = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String[] fields = line.split(",");
                int bannerID = 0;
                Module module = null;

                try {
                    // Safely parse Banner ID
                    bannerID = columnIndexMap.containsKey("Banner ID") && fields.length > columnIndexMap.get("Banner ID") && !fields[columnIndexMap.get("Banner ID")].trim().isEmpty()
                            ? Integer.parseInt(fields[columnIndexMap.get("Banner ID")].trim().replace("@", ""))
                            : 0;

                    if (bannerID == 0) {
                        DataPipeline.log(logWriter, "WARN", file.getName(), "Skipping line " + lineNumber + " due to missing or invalid Banner ID.");
                        linesSkippedInvalidBannerID++;
                        continue; // Skip this line if Banner ID is invalid
                    }

                    // Safely parse Year
                    int year = 0;
                    try {
                         year = columnIndexMap.containsKey("Year") && fields.length > columnIndexMap.get("Year") && !fields[columnIndexMap.get("Year")].trim().isEmpty()
                                ? Integer.parseInt(fields[columnIndexMap.get("Year")].trim())
                                : 0;
                    } catch (NumberFormatException e) {
                         DataPipeline.log(logWriter, "WARN", file.getName(), "Invalid Year format on line " + lineNumber + " for Banner ID " + bannerID + ": " + (columnIndexMap.containsKey("Year") && fields.length > columnIndexMap.get("Year") ? fields[columnIndexMap.get("Year")] : "N/A") + ". Using default 0.");
                         // Continue processing with default year 0
                    } catch (ArrayIndexOutOfBoundsException e) {
                         DataPipeline.log(logWriter, "WARN", file.getName(), "Missing Year field on line " + lineNumber + " for Banner ID " + bannerID + ". Using default 0.");
                         // Continue processing with default year 0
                    }


                    // Create Module object using safe accessors
                    module = new Module(
                            columnIndexMap.containsKey("Programme Code") && fields.length > columnIndexMap.get("Programme Code") && !fields[columnIndexMap.get("Programme Code")].trim().isEmpty()
                                    ? fields[columnIndexMap.get("Programme Code")].trim()
                                    : null,
                            null, // Components are not provided in the Module CSV
                            columnIndexMap.containsKey("CRN") && fields.length > columnIndexMap.get("CRN") && !fields[columnIndexMap.get("CRN")].trim().isEmpty()
                                    ? fields[columnIndexMap.get("CRN")].trim()
                                    : null,
                            columnIndexMap.containsKey("Module ID") && fields.length > columnIndexMap.get("Module ID") && !fields[columnIndexMap.get("Module ID")].trim().isEmpty()
                                    ? fields[columnIndexMap.get("Module ID")].trim()
                                    : null,
                            columnIndexMap.containsKey("Module Title") && fields.length > columnIndexMap.get("Module Title") && !fields[columnIndexMap.get("Module Title")].trim().isEmpty()
                                    ? fields[columnIndexMap.get("Module Title")].trim()
                                    : null,
                            year, // Use parsed year
                            columnIndexMap.containsKey("Module Level") && fields.length > columnIndexMap.get("Module Level") && !fields[columnIndexMap.get("Module Level")].trim().isEmpty()
                                    ? fields[columnIndexMap.get("Module Level")].trim()
                                    : null,
                            columnIndexMap.containsKey("Module Enrolment") && fields.length > columnIndexMap.get("Module Enrolment") && !fields[columnIndexMap.get("Module Enrolment")].trim().isEmpty()
                                    ? fields[columnIndexMap.get("Module Enrolment")].trim()
                                    : null,
                            columnIndexMap.containsKey("Credits") && fields.length > columnIndexMap.get("Credits") && !fields[columnIndexMap.get("Credits")].trim().isEmpty()
                                    ? fields[columnIndexMap.get("Credits")].trim()
                                    : null,
                            columnIndexMap.containsKey("Module School") && fields.length > columnIndexMap.get("Module School") && !fields[columnIndexMap.get("Module School")].trim().isEmpty()
                                    ? fields[columnIndexMap.get("Module School")].trim()
                                    : null,
                            columnIndexMap.containsKey("Part of Term") && fields.length > columnIndexMap.get("Part of Term") && !fields[columnIndexMap.get("Part of Term")].trim().isEmpty()
                                    ? fields[columnIndexMap.get("Part of Term")].trim()
                                    : null,
                            columnIndexMap.containsKey("Reg Status") && fields.length > columnIndexMap.get("Reg Status") && !fields[columnIndexMap.get("Reg Status")].trim().isEmpty()
                                    ? fields[columnIndexMap.get("Reg Status")].trim()
                                    : null
                    );

                } catch (NumberFormatException e) {
                    DataPipeline.log(logWriter, "ERROR", file.getName(), "Parsing error on line " + lineNumber + " for Banner ID field: " + e.getMessage());
                    linesSkippedParsingError++;
                    continue; // Skip line if Banner ID parsing fails critically
                } catch (ArrayIndexOutOfBoundsException e) {
                    DataPipeline.log(logWriter, "ERROR", file.getName(), "Missing expected field on line " + lineNumber + ". Check CSV format. Error: " + e.getMessage());
                    linesSkippedParsingError++;
                    continue; // Skip line if essential fields are missing
                } catch (Exception e) {
                    DataPipeline.log(logWriter, "ERROR", file.getName(), "Unexpected error processing line " + lineNumber + ": " + e.getMessage());
                    linesSkippedParsingError++;
                    continue; // Skip line on unexpected error
                }


                // Find the matching student and add the module
                boolean studentFound = false;
                if (module != null) { // Ensure module was created successfully
                    for (Student student : students) {
                        if (student.getBannerID() == bannerID) {
                            student.addModule(module); // Add the module to the student
                            modulesAddedCount++;
                            studentFound = true;
                            // Assuming a student ID is unique, we can break after finding the match
                            // If multiple students could share an ID (unlikely), remove the break
                            break;
                        }
                    }
                }

                if (!studentFound && bannerID != 0) { // Log only if Banner ID was valid but no student matched
                    DataPipeline.log(logWriter, "WARN", file.getName(), "No student found in the list for Banner ID: " + bannerID + " from line " + lineNumber);
                    linesSkippedStudentNotFound++;
                }
            }
            DataPipeline.log(logWriter, "INFO", file.getName(), "Finished processing. Added " + modulesAddedCount + " modules. Skipped lines: " + (linesSkippedInvalidBannerID + linesSkippedStudentNotFound + linesSkippedParsingError) + " (Invalid BannerID: " + linesSkippedInvalidBannerID + ", StudentNotFound: " + linesSkippedStudentNotFound + ", ParsingError: " + linesSkippedParsingError + ")");

        } catch (IOException e) {
             // Log the exception using a temporary writer if the main one failed or wasn't created
             try (PrintWriter errorWriter = new PrintWriter(new FileWriter(logFile, true))) {
                 DataPipeline.log(errorWriter, "ERROR", file.getName(), "IOException occurred while reading module data: " + e.getMessage());
             } catch (IOException logEx) {
                 // If logging itself fails, print to stderr as a last resort
                 System.err.println("Failed to write error log for IOException in addModulesToStudents: " + logEx.getMessage());
                 e.printStackTrace(); // Print original exception stack trace
             }
             throw e; // Re-throw the original exception
        } catch (Exception e) {
             // Catch unexpected runtime exceptions during processing
             try (PrintWriter errorWriter = new PrintWriter(new FileWriter(logFile, true))) {
                 DataPipeline.log(errorWriter, "ERROR", file.getName(), "Unexpected error occurred while processing module data: " + e.getMessage());
             } catch (IOException logEx) {
                 System.err.println("Failed to write error log for unexpected exception in addModulesToStudents: " + logEx.getMessage());
                 e.printStackTrace();
             }
             // Depending on the desired behavior, you might want to re-throw, wrap, or just log
             throw new RuntimeException("Unexpected error processing module file " + file.getName(), e);
        }
        
    }
 
}
