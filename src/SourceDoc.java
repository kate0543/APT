package src;

import java.io.*;
import java.util.*;
import java.nio.file.Files;

public class SourceDoc {

    public static void main(String[] args) {
        String baseFolderPath = "data/SBMT/";

        String targetProgrammeCode = "SBMT"; // Replace with the actual target programme codes
        String logFolderPath = DataPipeline.getLogFolderPath(targetProgrammeCode);

        List<Student> students = new ArrayList<>();
        try {
            students = fetchStudents(students, baseFolderPath, targetProgrammeCode, new DataPipeline());
        } catch (IOException e) {
            System.err.println("Error fetching students: " + e.getMessage());
            return;
        }
    }

    public static List<Module> fetchSourceModules(String baseFolderPath, String targetProgrammeCode,DataPipeline pipeline) throws IOException {
        String logFolderPath = pipeline.getLogFolderPath(targetProgrammeCode);

        String tabNameString = "All Assessments Main Campus";
        List<File> files = locateSourceFiles(baseFolderPath + "Source/", tabNameString);
        if (files.isEmpty()) {
            System.err.println("No matching source files found: " + tabNameString);
            return null;
        } else {
            return readSourceDataAllAssessments(files.get(0), logFolderPath);
        }
    }

    public static List<Student> fetchStudents(List<Student> students, String baseFolderPath, String targetProgrammeCode,DataPipeline pipeline) throws IOException {
        String logFolderPath = pipeline.getLogFolderPath(targetProgrammeCode);

        List<String> targetProgrammeCodesList = List.of(targetProgrammeCode + ".S", targetProgrammeCode + ".F");

        // students = Qlikview.fetchStudents(baseFolderPath, targetProgrammeCode);
        students = EBR.fetchStudents(students, baseFolderPath, targetProgrammeCode, pipeline);
        String tabNameString = "All Assessments Main Campus";
        List<File> files = locateSourceFiles(baseFolderPath + "Source/", tabNameString);
        if (files.isEmpty()) {
            System.err.println("No matching source files found: " + tabNameString);
            return students;
        } else {
            for (File file : files) {
                System.out.println("Found " + files.size() + " matching file: " + file.getAbsolutePath());
            }
        }

        List<Module> modules = readSourceDataAllAssessments(files.get(0), logFolderPath);
        students = updateStudentComponentDDL(students, modules, logFolderPath);
        verifyComponentCounts(students, logFolderPath, modules,false);
        verifyComponentCounts(students, logFolderPath, modules,true);

        System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("            Source Document Data processing completed.");
        System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------------------------");

        return students;
    }

    public static List<File> locateSourceFiles(String folderPath, String tabNameString) throws IOException {
        List<File> matchingFiles = new ArrayList<>();
        File folder = new File(folderPath);

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".csv") && name.contains(tabNameString));
        System.out.println("Found " + (files != null ? files.length : 0) + " CSV files in folder: " + folder.getAbsolutePath());
        if (files != null && files.length > 0) {
            for (File file : files) {
                System.out.println("Adding source file: " + file.getName());
                matchingFiles.add(file);
            }
        } else {
            System.err.println("No matching CSV files found in folder: " + folder.getAbsolutePath());
        }

        return matchingFiles;
    }

    /**
     * Verifies that the component counts for each student's modules match the source modules.
     * Logs mismatches and attempts to update student modules if components are missing.
     */
    public static void verifyComponentCounts(List<Student> students, String logFolderPath, List<Module> sourceModules, boolean update) {
        File logDir = new File(logFolderPath);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        String logFileName = "SourceDoc_component_verification_log";

        if(update){logFileName += "_update.csv";}
        else{logFileName += ".csv";}
        File logFile = new File(logDir, logFileName);

        // Create a map of source modules by CRN for quick lookup
        Map<String, Module> sourceModuleMap = new HashMap<>();
        for (Module sourceModule : sourceModules) {
            if (sourceModule != null && sourceModule.getModuleCRN() != null) {
                sourceModuleMap.put(sourceModule.getModuleCRN(), sourceModule);
            }
        }

        // Use try-with-resources to ensure the writer is closed automatically
        try (PrintWriter logWriter = new PrintWriter(new FileWriter(logFile))) {
            // Write CSV header manually as log function doesn't handle headers
            logWriter.println("Timestamp,Level,File,Message,Student Name,Student ID,Module CRN,Student Component Count,Source Component Count,Student Components,Source Components");
            logWriter.flush(); // Ensure header is written immediately

            DataPipeline.log(logWriter, "INFO", "verifyComponentCounts", "Starting Component Count Verification");

            for (Student student : students) {
                if (student.getModules() == null) {
                    DataPipeline.log(logWriter, "DEBUG", "verifyComponentCounts", "Skipping student with null modules: " + student.getName());
                    continue; // Skip student if they have no modules
                }

                for (Module studentModule : student.getModules()) {
                    if (studentModule == null || studentModule.getModuleCRN() == null) {
                        DataPipeline.log(logWriter, "DEBUG", "verifyComponentCounts", "Skipping null module or module with null CRN for student: " + student.getName());
                        continue; // Skip if module or CRN is null
                    }

                    String crn = studentModule.getModuleCRN();
                    Module sourceModule = sourceModuleMap.get(crn);

                    if (sourceModule == null) {
                        // Log this case if needed
                        String message = String.format("Source module not found for CRN: %s for student: %s (%d)",
                                crn, student.getName(), student.getBannerID());
                        DataPipeline.log(logWriter, "WARN", "verifyComponentCounts", message);
                        continue; // Skip verification if source module doesn't exist
                    }

                    int studentComponentCount = (studentModule.getComponents() != null) ? studentModule.getComponents().size() : 0;
                    int sourceComponentCount = (sourceModule.getComponents() != null) ? sourceModule.getComponents().size() : 0;

                    if (studentComponentCount != sourceComponentCount) {
                        String studentName = student.getName().replace(",", ""); // Basic handling for commas in names
                        Integer bannerId = student.getBannerID();
                        String moduleCrn = crn;

                        // Format component lists for CSV cell (e.g., separated by '|')
                        String studentComponentsStr = formatComponentList(studentModule.getComponents());
                        String sourceComponentsStr = formatComponentList(sourceModule.getComponents());

                        if (studentComponentCount < sourceComponentCount) {
                            // Add missing components from sourceModule to studentModule
                            for (Component sourceComponent : sourceModule.getComponents()) {
                                boolean exists = false;
                                if (studentModule.getComponents() != null) {
                                    for (Component studentComponent : studentModule.getComponents()) {
                                        if (studentComponent != null && sourceComponent.getComponentTitle() != null &&
                                            sourceComponent.getComponentTitle().equals(studentComponent.getComponentTitle())) {
                                            exists = true;
                                            break;
                                        }
                                    }
                                }
                                if (!exists) {
                                    // Clone the source component (shallow copy)
                                    Component newComponent = new Component();
                                    newComponent.setModuleCRN(sourceComponent.getModuleCRN());
                                    newComponent.setComponentTitle(sourceComponent.getComponentTitle());
                                    newComponent.setComponentDeadline(sourceComponent.getComponentDeadline());
                                    newComponent.setComponentCode(sourceComponent.getComponentCode());
                                    studentModule.getComponents().add(newComponent);
                                }
                            }
                            // After updating, check if the component count matches
                            int updatedStudentComponentCount = (studentModule.getComponents() != null) ? studentModule.getComponents().size() : 0;
                            if (updatedStudentComponentCount == sourceComponentCount) {
                                String updateSuccessMessage = String.format(
                                    "Updated student module components successfully for Student: %s (%d), Module CRN: %s. Updated Count: %d, Source Count: %d. Student Components: [%s], Source Components: [%s]",
                                    studentName,
                                    bannerId,
                                    moduleCrn,
                                    updatedStudentComponentCount,
                                    sourceComponentCount,
                                    formatComponentList(studentModule.getComponents()),
                                    sourceComponentsStr
                                );
                                DataPipeline.log(logWriter, "INFO", "verifyComponentCounts", updateSuccessMessage);
                            } else {
                                String updateFailMessage = String.format(
                                    "Failed to update student module components to match source for Student: %s (%d), Module CRN: %s. Updated Count: %d, Source Count: %d. Student Components: [%s], Source Components: [%s]",
                                    studentName,
                                    bannerId,
                                    moduleCrn,
                                    updatedStudentComponentCount,
                                    sourceComponentCount,
                                    formatComponentList(studentModule.getComponents()),
                                    sourceComponentsStr
                                );
                                DataPipeline.log(logWriter, "ERROR", "verifyComponentCounts", updateFailMessage);
                            }
                        } else {
                            fixOverloadComponents(student, moduleCrn, logFolderPath);
                            String duplicateMessage = String.format(
                                "Potentially one component added multiple times for this student, please check. Student: %s (%d), Module CRN: %s. Student Count: %d, Source Count: %d. Student Components: [%s], Source Components: [%s]",
                                studentName,
                                bannerId,
                                moduleCrn,
                                studentComponentCount,
                                sourceComponentCount,
                                studentComponentsStr,
                                sourceComponentsStr
                            );
                            DataPipeline.log(logWriter, "ERROR", "verifyComponentCounts", duplicateMessage);
                        }

                        // Log the mismatch details using the log function
                        String mismatchMessage = String.format(
                            "Component count mismatch for Student: %s (%d), Module CRN: %s. Student Count: %d, Source Count: %d. Student Components: [%s], Source Components: [%s]",
                            studentName,
                            bannerId,
                            moduleCrn,
                            studentComponentCount,
                            sourceComponentCount,
                            studentComponentsStr,
                            sourceComponentsStr
                        );
                        DataPipeline.log(logWriter, "ERROR", "verifyComponentCounts", mismatchMessage);

                        // Log a simpler version indicating mismatch found and logged
                        String summaryMessage = String.format(
                            "Component count mismatch found (logged) for Student: %s (%d), Module CRN: %s. Student: %d, Source: %d",
                            student.getName(),
                            student.getBannerID(),
                            crn,
                            studentComponentCount,
                            sourceComponentCount
                        );
                        DataPipeline.log(logWriter, "ERROR", "verifyComponentCounts", summaryMessage);
                    } else {
                        String matchMessage = String.format(
                            "Component count matches for Student: %s (%d), Module CRN: %s. Count: %d",
                            student.getName(),
                            student.getBannerID(),
                            crn,
                            studentComponentCount
                        );
                        // DataPipeline.log(logWriter, "DEBUG", "verifyComponentCounts", matchMessage);
                    }
                }
            }
            // Log completion
            DataPipeline.log(logWriter, "INFO", "verifyComponentCounts", "Verification Complete - Log saved to " + logFileName);

        } catch (IOException e) {
            // Attempt to log the error using a new writer in append mode
            try (PrintWriter errorWriter = new PrintWriter(new FileWriter(logFile, true))) {
                String fatalMessage = String.format("Error writing component verification log to %s: %s", logFileName, e.getMessage());
                DataPipeline.log(errorWriter, "FATAL", "verifyComponentCounts", fatalMessage);
                // Log stack trace elements
                for (StackTraceElement element : e.getStackTrace()) {
                    DataPipeline.log(errorWriter, "FATAL", "verifyComponentCounts", "  at " + element.toString());
                }
            } catch (IOException ex) {
                // If logging the error itself fails, print to standard error
                System.err.println("FATAL: Could not write to log file " + logFileName + ": " + ex.getMessage());
                System.err.println("Original Error during verification: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static String formatComponentList(List<Component> components) {
        if (components == null || components.isEmpty()) {
            return "(No components)";
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Component comp : components) {
            if (!first) {
                sb.append(" | ");
            }
            if (comp != null && comp.getComponentTitle() != null) {
                // Escape double quotes within the title by doubling them, and remove commas
                String title = comp.getComponentTitle().replace("\"", "\"\"").replace(",", "");
                sb.append(title);
            } else {
                sb.append("null component");
            }
            first = false;
        }
        return sb.toString();
    }

    /**
     * Updates student component deadlines and codes from the source modules.
     * Also updates module leader and admin team.
     */
    public static List<Student> updateStudentComponentDDL(List<Student> students, List<Module> sourceModules, String logFolderPath) {
        File logDir = new File(logFolderPath);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        File logFile = new File(logDir, "SourceDoc_updateStudentComponentDDL_log.csv");
        for (Student student : students) {
            if (student.getModules() != null) {
                for (Module module : student.getModules()) {
                    if (module.getComponents() != null) {
                        for (Component component : module.getComponents()) {
                            for (Module sourceModule : sourceModules) {
                                if (sourceModule.getModuleCRN().equals(module.getModuleCRN())) {
                                    module.setModuleLeader(sourceModule.getModuleLeader());
                                    module.setModuleAdminTeam(sourceModule.getModuleAdminTeam());
                                    for (Component sourceComponent : sourceModule.getComponents()) {
                                        if (sourceComponent.getComponentTitle().equals(component.getComponentTitle())) {
                                            component.setComponentDeadline(sourceComponent.getComponentDeadline());
                                            component.setComponentCode(sourceComponent.getComponentCode());
                                            // Log the update
                                            String logMsg = String.format(
                                                "Updated component for Student: %s (%d), Module CRN: %s,Module Title: %s, Component: %s, New Deadline: %s",
                                                student.getName(),
                                                student.getBannerID(),
                                                module.getModuleCRN(),
                                                module.getModuleTitle(),
                                                component.getComponentTitle(),
                                                component.getComponentDeadline()
                                            );
                                            try (PrintWriter logWriter = new PrintWriter(new FileWriter(logFile, true))) {
                                                DataPipeline.log(logWriter, "INFO", "updateStudentComponentDDL", logMsg);
                                            } catch (IOException e) {
                                                System.err.println("Error logging component update: " + e.getMessage());
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return students;
    }

    /**
     * Reads source data from a CSV file and returns a list of modules with their components.
     */
    public static List<Module> readSourceDataAllAssessments(File file, String logFolderPath) throws IOException {
        List<Module> modulesList = new ArrayList<>();
        File logDir = new File(logFolderPath);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        File logFile = new File(logDir, "SourceDoc_readSourceDataAllAssessments_log.csv");

        // Define expected header names
        final String CRN_HEADER = "CRN";
        final String COMPONENT_DESC_HEADER = "COMPONENT_DESCRIPTION";
        final String SUBMISSION_DATE_HEADER = "Submission Date";
        final String MODULE_LEADER_HEADER = "Module Leader";
        final String MODULE_ADMIN_TEAM_HEADER = "Admin Team";

        String line = "";
        List<String> header = null;
        int crnIndex = -1;
        int componentDescIndex = -1;
        int submissionDateIndex = -1;
        int moduleLeaderIndex = -1;
        int moduleAdminTeamIndex = -1;

        // Use try-with-resources for PrintWriter to ensure it's closed
        try (PrintWriter logWriter = new PrintWriter(new FileWriter(logFile));
             BufferedReader br = new BufferedReader(new FileReader(file))) {

            DataPipeline.log(logWriter, "INFO", "readSourceDataAllAssessments", "Starting to read source data from file: " + file.getName());

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.contains("PG")) { // Skip empty lines and lines containing "PG"
                    DataPipeline.log(logWriter, "DEBUG", "readSourceDataAllAssessments", "Skipping line: " + line);
                    continue;
                }

                String[] values = line.split(","); // Use comma as separator

                if (header == null) {
                    header = java.util.Arrays.asList(values);
                    DataPipeline.log(logWriter, "INFO", "readSourceDataAllAssessments", "Header found: " + header);
                    // Find indexes for all relevant headers
                    for (int i = 0; i < header.size(); i++) {
                        String col = header.get(i).trim().replaceAll("^\"|\"$", ""); // Trim and remove surrounding quotes
                        if (col.equalsIgnoreCase(CRN_HEADER)) crnIndex = i;
                        else if (col.equalsIgnoreCase(COMPONENT_DESC_HEADER)) componentDescIndex = i;
                        else if (col.equalsIgnoreCase(SUBMISSION_DATE_HEADER)) submissionDateIndex = i;
                        else if (col.equalsIgnoreCase(MODULE_LEADER_HEADER)) moduleLeaderIndex = i;
                        else if (col.equalsIgnoreCase(MODULE_ADMIN_TEAM_HEADER)) moduleAdminTeamIndex = i;
                    }
                    // Check if essential columns were found
                    if (crnIndex == -1) {
                        DataPipeline.log(logWriter, "ERROR", "readSourceDataAllAssessments", "'" + CRN_HEADER + "' column not found in header: " + header);
                        // Optionally throw an exception or return early if CRN is mandatory
                        // throw new IOException("'" + CRN_HEADER + "' column not found in header.");
                    }
                    if (componentDescIndex == -1) DataPipeline.log(logWriter, "WARN", "readSourceDataAllAssessments", "'" + COMPONENT_DESC_HEADER + "' column not found.");
                    if (submissionDateIndex == -1) DataPipeline.log(logWriter, "WARN", "readSourceDataAllAssessments", "'" + SUBMISSION_DATE_HEADER + "' column not found.");
                    if (moduleLeaderIndex == -1) DataPipeline.log(logWriter, "WARN", "readSourceDataAllAssessments", "'" + MODULE_LEADER_HEADER + "' column not found.");
                    if (moduleAdminTeamIndex == -1) DataPipeline.log(logWriter, "WARN", "readSourceDataAllAssessments", "'" + MODULE_ADMIN_TEAM_HEADER + "' column not found.");

                    continue; // Skip processing the header row itself
                }

                // Ensure the row has enough columns for the required indices
                if (values.length <= Math.max(crnIndex, Math.max(componentDescIndex, Math.max(submissionDateIndex, Math.max(moduleLeaderIndex, moduleAdminTeamIndex))))) {
                    DataPipeline.log(logWriter, "WARN", "readSourceDataAllAssessments", "Skipping row due to insufficient columns: " + Arrays.toString(values));
                    continue;
                }

                // Extract values safely using indices, trim whitespace and remove quotes
                String crn = (crnIndex != -1 && crnIndex < values.length) ? values[crnIndex].trim().replaceAll("^\"|\"$", "") : "";
                String componentDesc = (componentDescIndex != -1 && componentDescIndex < values.length) ? values[componentDescIndex].trim().replaceAll("^\"|\"$", "") : "";
                String submissionDate = (submissionDateIndex != -1 && submissionDateIndex < values.length) ? values[submissionDateIndex].trim().replaceAll("^\"|\"$", "") : "";
                String moduleLeader = (moduleLeaderIndex != -1 && moduleLeaderIndex < values.length) ? values[moduleLeaderIndex].trim().replaceAll("^\"|\"$", "") : "";
                String moduleAdminTeam = (moduleAdminTeamIndex != -1 && moduleAdminTeamIndex < values.length) ? values[moduleAdminTeamIndex].trim().replaceAll("^\"|\"$", "") : "";

                if (crn.isEmpty()) {
                    DataPipeline.log(logWriter, "WARN", "readSourceDataAllAssessments", "Skipping row with empty CRN: " + Arrays.toString(values));
                    continue; // Skip rows without a CRN
                }

                // Find or create the module
                Module module = modulesList.stream()
                        .filter(m -> m.getModuleCRN() != null && m.getModuleCRN().equals(crn))
                        .findFirst()
                        .orElse(null);

                if (module == null) {
                    module = new Module();
                    module.setModuleCRN(crn);
                    module.setModuleLeader(moduleLeader);
                    module.setModuleAdminTeam(moduleAdminTeam);
                    modulesList.add(module);
                    DataPipeline.log(logWriter, "DEBUG", "readSourceDataAllAssessments", "Created new module for CRN: " + crn);
                } else {
                    // Update leader/admin only if they are currently empty/null
                    if ((module.getModuleLeader() == null || module.getModuleLeader().isEmpty()) && !moduleLeader.isEmpty()) {
                        module.setModuleLeader(moduleLeader);
                    }
                    if ((module.getModuleAdminTeam() == null || module.getModuleAdminTeam().isEmpty()) && !moduleAdminTeam.isEmpty()) {
                        module.setModuleAdminTeam(moduleAdminTeam);
                    }
                }

                // Create and add the component if description is not empty
                if (!componentDesc.isEmpty()) {
                    Component component = new Component();
                    component.setModuleCRN(crn);
                    component.setComponentTitle(componentDesc);
                    component.setComponentDeadline(submissionDate);
                    // Avoid adding duplicate components (based on title)
                    boolean componentExists = module.getComponents().stream()
                            .anyMatch(c -> c.getComponentTitle() != null && c.getComponentTitle().equals(componentDesc));
                    if (!componentExists) {
                        module.getComponents().add(component);
                        DataPipeline.log(logWriter, "DEBUG", "readSourceDataAllAssessments", "Added component '" + componentDesc + "' to module CRN: " + crn);
                    } else {
                        DataPipeline.log(logWriter, "WARN", "readSourceDataAllAssessments", "Duplicate component title '" + componentDesc + "' skipped for module CRN: " + crn);
                    }
                } else {
                    DataPipeline.log(logWriter, "WARN", "readSourceDataAllAssessments", "Skipping component with empty description for module CRN: " + crn + " in row: " + Arrays.toString(values));
                }
            }
            DataPipeline.log(logWriter, "INFO", "readSourceDataAllAssessments", "Finished reading source data. Total modules processed: " + modulesList.size());

        } catch (IOException e) {
            // Log the exception using a separate try-catch for the logger itself
            try (PrintWriter errorWriter = new PrintWriter(new FileWriter(logFile, true))) { // Append mode for errors
                DataPipeline.log(errorWriter, "FATAL", "readSourceDataAllAssessments", "Error reading file: " + file.getAbsolutePath() + ". Message: " + e.getMessage());
                DataPipeline.log(errorWriter, "FATAL", "readSourceDataAllAssessments", "Current working directory: " + System.getProperty("user.dir"));
                // Log stack trace elements
                for (StackTraceElement element : e.getStackTrace()) {
                    DataPipeline.log(errorWriter, "FATAL", "readSourceDataAllAssessments", "  at " + element.toString());
                }
            } catch (IOException logEx) {
                // If logging fails, print to standard error as a last resort
                System.err.println("FATAL: Error writing log file: " + logEx.getMessage());
                System.err.println("Original Error reading file: " + file.getAbsolutePath() + ". Message: " + e.getMessage());
                e.printStackTrace(); // Print original stack trace
            }
            // Re-throw the original exception to signal failure
            throw e;
        }
        return modulesList;
    }

    public static Student fixOverloadComponents(Student student, String moduleCrn, String logFolderPath) {
        File logDir = new File(logFolderPath);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        File logFile = new File(logDir, "SourceDoc_fixOverloadComponents_log.csv");

        Module moduleToFix = student.getModules().stream()
            .filter(m -> m.getModuleCRN().equals(moduleCrn))
            .findFirst()
            .orElse(null);
        if (moduleToFix != null) {
            if (moduleToFix.getModuleCRN().equals(moduleCrn)) {
                try (PrintWriter logWriter = new PrintWriter(new FileWriter(logFile, true))) {
                    // Log before removing duplicates
                    DataPipeline.log(logWriter, "INFO", "fixOverloadComponents",
                        String.format("\nBefore removing duplicates: Student: %d %s, Module CRN: %s, Components count: %d",
                            student.getBannerID(), student.getName(), moduleCrn, moduleToFix.getComponents().size()));
                    int idx = 0;
                    for (Component component : moduleToFix.getComponents()) {
                        DataPipeline.log(logWriter, "INFO", "fixOverloadComponents",
                            String.format("Component #%d: %s", idx++, component != null ? component.toString() : "null"));
                    }

                    Set<String> seenTitles = new HashSet<>();
                    Iterator<Component> iterator = moduleToFix.getComponents().iterator();
                    while (iterator.hasNext()) {
                        Component comp = iterator.next();
                        String titleKey = (comp.getComponentTitle() != null ? comp.getComponentTitle() : "");
                        if (seenTitles.contains(titleKey)) {
                            DataPipeline.log(logWriter, "INFO", "fixOverloadComponents",
                                String.format("Removing duplicate: Student: %d %s, Module CRN: %s, Title: %s, Component: %s",
                                    student.getBannerID(), student.getName(), moduleCrn, titleKey, comp != null ? comp.toString() : "null"));
                            iterator.remove();
                        } else {
                            seenTitles.add(titleKey);
                        }
                    }

                    // Log after removing duplicates
                    DataPipeline.log(logWriter, "INFO", "fixOverloadComponents",
                        String.format("\nAfter removing duplicates: Student: %d %s, Module CRN: %s, Components count: %d",
                            student.getBannerID(), student.getName(), moduleCrn, moduleToFix.getComponents().size()));
                    idx = 0;
                    for (Component component : moduleToFix.getComponents()) {
                        DataPipeline.log(logWriter, "INFO", "fixOverloadComponents",
                            String.format("Component #%d: %s", idx++, component != null ? component.toString() : "null"));
                    }
                } catch (IOException e) {
                    System.err.println("Error logging in fixOverloadComponents: " + e.getMessage());
                }
            }
        }

        return student;
    }
     
 
}
