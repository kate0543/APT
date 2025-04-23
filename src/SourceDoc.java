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
            return readSourceData(files);
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

            List<Module> modules = readSourceData(files);

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

    public static List<Module> readSourceData(List<File> files) throws IOException {
        List<Module> modulesList = new ArrayList<>();

        // Define expected header names
        final String CRN_HEADER = "CRN";
        final String COMPONENT_DESC_HEADER = "COMPONENT_DESCRIPTION";
        final String SUBMISSION_DATE_HEADER = "Submission Date";
        final String MODULE_LEADER_HEADER = "Module Leader";
        final String COMPONENT_CODE_HEADER = "Component Code";
        final String MODULE_ADMIN_TEAM_HEADER = "Module Admin Team";

        for (File file : files) {
            try {
                // Read all lines from the CSV file using NIO
                List<String> lines = Files.readAllLines(file.toPath());

                // Clean the lines (e.g., remove double quotes) using a hypothetical DataPipeline class
                // Ensure DataPipeline.cleanLines is implemented and available in the scope.
                // Consider potential exceptions thrown by cleanLines.
                lines = DataPipeline.cleanLines(lines);

                // Check if the file is empty after cleaning
                if (lines.isEmpty()) {
                    System.err.println("Warning: File is empty or became empty after cleaning: " + file.getName());
                    continue; // Skip to the next file
                }

                // Get the header line (the first line from the list)
                String line = lines.get(0);
                if (line.isEmpty()) {
                    System.err.println("Warning: Header line is empty after cleaning: " + file.getName());
                    continue; // Skip to the next file
                }
                // Remove BOM if present (often found in UTF-8 files from Windows)
                if (line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }


                // Note: The subsequent code block needs modification.
                // Instead of using 'reader.readLine()' in a loop,
                // it should iterate through the 'lines' list starting from index 1.
                // Example: for (int i = 1; i < lines.size(); i++) { String dataLine = lines.get(i); ... }


                // Simple CSV split, assumes no commas within quoted fields
                String[] headers = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                System.out.println("Processing file: " + file.getName() + ", Headers: " + Arrays.toString(headers));
                System.out.println("Header count: " + headers.length);
                for (String header : headers) {
                    System.out.println("Header: " + header.trim());
                }
                Map<String, Integer> columnIndexMap = new HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    columnIndexMap.put(headers[i].trim(), i);
                }

                // Validate required columns exist
                List<String> requiredHeaders = List.of(CRN_HEADER, COMPONENT_DESC_HEADER, SUBMISSION_DATE_HEADER, MODULE_LEADER_HEADER);
                boolean missingHeader = false;
                for (String header : requiredHeaders) {
                    if (!columnIndexMap.containsKey(header)) {
                        System.err.println("Warning: File " + file.getName() + " is missing required column: " + header);
                        missingHeader = true;
                    }
                }
                if (missingHeader) {
                    continue;
                }

                // Iterate through the cleaned lines, starting from the second line (index 1)
                for (int i = 1; i < lines.size(); i++) {
                    String dataLine = lines.get(i);
                    if (dataLine.isEmpty()) {
                        continue; // Skip empty lines
                    }

                    // Simple CSV split, assumes no commas within quoted fields after cleaning
                    String[] values = dataLine.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

                    // Debugging: Check for problematic values (optional)
                    // for(String value : values) {
                    //     if(value.contains("\"")) {
                    //         System.out.println("Value with quotes: " + value.trim());
                    //     }
                    //     if(value.contains(",")) {
                    //         System.out.println("Value with comma: " + value.trim());
                    //     }
                    //     // System.out.println("Value: " + value.trim());
                    // }

                    String crn = getValue(values, columnIndexMap, CRN_HEADER);
                    String componentDesc = getValue(values, columnIndexMap, COMPONENT_DESC_HEADER);
                    String submissionDate = getValue(values, columnIndexMap, SUBMISSION_DATE_HEADER);
                    String moduleLeader = getValue(values, columnIndexMap, MODULE_LEADER_HEADER);
                    String componentCode = getValue(values, columnIndexMap, COMPONENT_CODE_HEADER);
                    String moduleAdminTeam = getValue(values, columnIndexMap, MODULE_ADMIN_TEAM_HEADER);

                    if ("null".equals(crn) || crn.isEmpty()) {
                        // Log or handle rows with missing CRN if necessary
                        continue; // Skip rows without a valid CRN
                    }

                    // Find existing module in the list or create a new one
                    Module currentModule = null;
                    for (Module module : modulesList) {
                        if (module.getModuleCRN().equals(crn)) {
                            currentModule = module;
                            break;
                        }
                    }

                    if (currentModule == null) {
                        currentModule = new Module();
                        currentModule.setModuleCRN(crn);
                        modulesList.add(currentModule);
                    }

                    // Update module leader and admin team (potentially overwrites if multiple leaders/teams listed for same CRN)
                    // Only update if the value is not "null" (as returned by getValue)
                    if (!"null".equals(moduleLeader)) {
                        currentModule.setModuleLeader(moduleLeader);
                    }
                    if (!"null".equals(moduleAdminTeam)) {
                        currentModule.setModuleAdminTeam(moduleAdminTeam);
                    }

                    // Create and add the component if component description is valid
                    if (!"null".equals(componentDesc) && !componentDesc.isEmpty()) {
                        Component component = new Component();
                        component.setComponentTitle(componentDesc);
                        // Debugging for quotes in component title (optional)
                        // if(componentDesc.contains("\""))
                        //     System.out.println("Component Title: " + componentDesc);

                        if (!"null".equals(submissionDate)) {
                            component.setComponentDeadline(submissionDate);
                        } else {
                            component.setComponentDeadline("N/A"); // Set default if submission date is missing
                        }
                        if (!"null".equals(componentCode)) {
                            component.setComponentCode(componentCode);
                        }
                        // Add component to the module
                        currentModule.addComponent(component);
                    }
                }
            } catch (IOException e) {
                System.err.println("Error reading source file " + file.getName() + ": " + e.getMessage());
                // Continue with the next file
            }
        }
        return modulesList;
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
        System.out.println("--- Starting Component Count Verification ---");
        String logFileName = "component_verification_log.csv"; // Name of the log file

        // Create a map of source modules by CRN for quick lookup
        Map<String, Module> sourceModuleMap = new HashMap<>();
        for (Module sourceModule : sourceModules) {
            if (sourceModule != null && sourceModule.getModuleCRN() != null) {
                sourceModuleMap.put(sourceModule.getModuleCRN(), sourceModule);
            }
        }

        // Use try-with-resources to ensure the writer is closed automatically
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFileName))) {
            // Write CSV header
            writer.println("Student Name,Student ID,Module CRN,Student Component Count,Source Component Count,Student Components,Source Components");

            for (Student student : students) {
                if (student.getModules() == null) {
                    continue; // Skip student if they have no modules
                }

                for (Module studentModule : student.getModules()) {
                    if (studentModule == null || studentModule.getModuleCRN() == null) {
                        continue; // Skip if module or CRN is null
                    }

                    String crn = studentModule.getModuleCRN();
                    Module sourceModule = sourceModuleMap.get(crn);

                    if (sourceModule == null) {
                        // Log this case if needed, maybe to a separate log or console
                        // System.out.println("Warning: Source module not found for CRN: " + crn + " for student: " + student.getName());
                        continue; // Skip verification if source module doesn't exist
                    }

                    int studentComponentCount = (studentModule.getComponents() != null) ? studentModule.getComponents().size() : 0;
                    int sourceComponentCount = (sourceModule.getComponents() != null) ? sourceModule.getComponents().size() : 0;

                    if (studentComponentCount != sourceComponentCount) {
                        // Mismatch found, prepare data for CSV row
                        String studentName = student.getName().replace(",", ""); // Basic handling for commas in names
                        Integer bannerId = student.getBannerID();
                        String moduleCrn = crn;

                        // Format component lists for CSV cell (e.g., separated by '|')
                        String studentComponentsStr = formatComponentList(studentModule.getComponents());
                        String sourceComponentsStr = formatComponentList(sourceModule.getComponents());

                        // Write the mismatch details to the CSV file
                        writer.printf("\"%s\",\"%s\",\"%s\",%d,%d,\"%s\",\"%s\"\n",
                                studentName,
                                bannerId,
                                moduleCrn,
                                studentComponentCount,
                                sourceComponentCount,
                                studentComponentsStr,
                                sourceComponentsStr);

                        // Optional: Still print to console if desired
                        System.out.println("\nComponent count mismatch found (logged to " + logFileName + "):");
                        System.out.println("  Student: " + student.getName() + " (ID: " + student.getBannerID() + ")");
                        System.out.println("  Module CRN: " + crn);
                        System.out.println("  Student Count: " + studentComponentCount + ", Source Count: " + sourceComponentCount);
                    }
                }
            }
            System.out.println("--- Verification Complete --- Log saved to " + logFileName);

        } catch (IOException e) {
            System.err.println("Error writing component verification log to " + logFileName + ": " + e.getMessage());
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
    
}