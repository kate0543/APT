package src;

import java.io.*;
import java.util.*;
import java.nio.file.Files; 

public class SourceDoc {

    public static void main(String[] args) {
        String baseFolderPath = "data/BMT/";
        List<String> targetProgrammeCodesList = List.of("BMT.S", "BMT.F"); // Replace with the actual target programme codes

        List<Student> students = new ArrayList<>();
        try {
            students = fetchStudents(students, baseFolderPath, targetProgrammeCodesList);
        } catch (IOException e) {
            System.err.println("Error fetching students: " + e.getMessage());
            return;
        }
        // for (Student student : students) {
        //     // Uncomment to print trailing modules or failed components
        //     if (student.getTrailingModules() != null && !student.getTrailingModules().isEmpty()) {
        //         System.out.println("Student: " + student.getName() + ", Trailing Modules: " + student.getTrailingModules().size());
        //     }
        //     if (student.getFailedComponents() != null && !student.getFailedComponents().isEmpty()) {
        //         System.out.println("Student: " + student.getName() + ", Failed Components: " + student.getFailedComponents().size());
        //         for (Component component : student.getFailedComponents()) {
        //             System.out.println("Component: " + component.toString());
        //         }
        //     }
        // }
    }

    public static List<Module> fetchSourceModules(String baseFolderPath) throws IOException {
        String tabNameString = "All Assessments Main Campus";
        List<File> files = locateSourceFiles(baseFolderPath + "Source/", tabNameString);
        if (files.isEmpty()) {
            System.err.println("No matching source files found: " + tabNameString);
            return null;
        } else {

            return readSourceDataAllAssessments(files.get(0));
        }
    }

    public static List<Student> fetchStudents(List<Student> students, String baseFolderPath, List<String> targetProgrammeCodesList) throws IOException {
        try {
            // Fetch students from Qlikview and EBR systems
            students = Qlikview.fetchStudents(baseFolderPath, targetProgrammeCodesList);
            students = EBR.fetchStudentsMR(students, baseFolderPath, targetProgrammeCodesList);

            String tabNameString = "All Assessments Main Campus";
            // Locate and read module/component deadlines from the source files
            List<File> files = locateSourceFiles(baseFolderPath + "Source/", tabNameString);
            if (files.isEmpty()) {
                System.err.println("No matching source files found: " + tabNameString);
                return students;
            }

            List<Module> modules = readSourceDataAllAssessments(files.get(0));

            // For each student, update their module components with the deadline from the source data
            for (Student student : students) {
                if (student.getModules() != null) {
                    for (Module module : student.getModules()) {
                        if (module.getComponents() != null) {
                            for (Component component : module.getComponents()) {
                                // Check if the component's CRN matches any module in the source data
                                for (Module sourceModule : modules) {
                                    if (sourceModule.getModuleCRN().equals(module.getModuleCRN())) {
                                        module.setModuleLeader(sourceModule.getModuleLeader());
                                        module.setModuleAdminTeam(sourceModule.getModuleAdminTeam());
                                        // Update the component's deadline from the source data
                                        for (Component sourceComponent : sourceModule.getComponents()) {
                                            if (sourceComponent.getComponentTitle().equals(component.getComponentTitle())) {
                                                component.setComponentDeadline(sourceComponent.getComponentDeadline());
                                                component.setComponentCode(sourceComponent.getComponentCode());
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
            verifyComponentCounts(students, modules);

            System.out.println("Source Document Data processing completed.");
            return students;

        } catch (IOException e) {
            System.err.println("Error fetching students: " + e.getMessage());
            return students;
        }
    }

    // Helper method to safely get values from CSV row, used by readSourceData
    private static String getValue(String[] values, Map<String, Integer> columnIndexMap, String headerName) {
        Integer index = columnIndexMap.get(headerName);
        // Check if the index is valid and within the bounds of the values array
        if (index == null || index < 0 || index >= values.length || values[index] == null) {
            return "null";
        }
        return values[index].trim();
    }
    public static List<File> locateSourceFiles(String folderPath, String tabNameString) throws IOException {
        List<File> matchingFiles = new ArrayList<>();
        File folder = new File(folderPath);

        if (!folder.exists() || !folder.isDirectory()) {
            throw new IOException("Directory not found: " + folderPath);
        }

        File[] files = folder.listFiles();
        if (files == null || files.length == 0) {
            throw new IOException("No files found in directory: " + folderPath);
        }

        // Filter files containing the tab name and ending with .csv
        for (File file : files) {
            if (file.isFile() && file.getName().contains(tabNameString) && file.getName().endsWith(".csv")) {
                System.out.println("Found matching file: " + file.getAbsolutePath());
                matchingFiles.add(file);
            }
        }
        return matchingFiles;
    }


    

    /**
     * Verifies if the number of components for each module in the students list
     * matches the number of components for the corresponding module in the source data.
     *
     * @param students      The list of students with their modules and components.
     * @param sourceModules The list of modules read from the source document, used as the reference.
     */
    public static void verifyComponentCounts(List<Student> students, List<Module> sourceModules) {

        File logDir = new File("result/log");
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        String logFileName = "SourceDoc_component_verification_log.csv";
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

                        // Construct the detailed message for logging
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
                        // Log the mismatch details using the log function
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
                        DataPipeline.log(logWriter, "DEBUG", "verifyComponentCounts", matchMessage);
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

    /**
     * Helper method to format a list of components into a single string for CSV.
     * Handles null components and potential commas/quotes in titles.
     *
     * @param components List of Component objects.
     * @return A string representation of component titles, separated by '|'.
     */
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
 
        public static List<Module> readSourceDataAllAssessments(File file) throws IOException {
            List<Module> modulesList = new ArrayList<>();
            File logDir = new File("result/log");
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
            Map<String, List<List<String>>> modules = new HashMap<>(); // This map seems unused, consider removing if not needed later

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
 
    }
    

