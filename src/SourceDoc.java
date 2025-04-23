package src;

import java.io.*;
import java.util.*;

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
        for (Student student : students) {
            // Uncomment to print trailing modules or failed components
            // if (student.getTrailingModules() != null && !student.getTrailingModules().isEmpty()) {
            //     System.out.println("Student: " + student.getName() + ", Trailing Modules: " + student.getTrailingModules().size());
            // }
            // if (student.getFailedComponents() != null && !student.getFailedComponents().isEmpty()) {
            //     System.out.println("Student: " + student.getName() + ", Failed Components: " + student.getFailedComponents().size());
            //     for (Component component : student.getFailedComponents()) {
            //         System.out.println("Component: " + component.toString());
            //     }
            // }
        }
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
            students = EBR.fetchStudents(students, baseFolderPath, targetProgrammeCodesList);

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
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line = reader.readLine(); // Read header line
                if (line == null || line.isEmpty()) {
                    System.err.println("Warning: File is empty or missing header: " + file.getName());
                    continue;
                }

                // Simple CSV split, assumes no commas within quoted fields
                String[] headers = line.split(",");
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

                while ((line = reader.readLine()) != null) {
                    String[] values = line.split(",");

                    String crn = getValue(values, columnIndexMap, CRN_HEADER);
                    String componentDesc = getValue(values, columnIndexMap, COMPONENT_DESC_HEADER);
                    String submissionDate = getValue(values, columnIndexMap, SUBMISSION_DATE_HEADER);
                    String moduleLeader = getValue(values, columnIndexMap, MODULE_LEADER_HEADER);
                    String componentCode = getValue(values, columnIndexMap, COMPONENT_CODE_HEADER);
                    String moduleAdminTeam = getValue(values, columnIndexMap, MODULE_ADMIN_TEAM_HEADER);

                    if ("null".equals(crn) || crn.isEmpty()) {
                        continue;
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
                    if (!"null".equals(moduleLeader)) {
                        currentModule.setModuleLeader(moduleLeader);
                    }
                    if (!"null".equals(moduleAdminTeam)) {
                        currentModule.setModuleAdminTeam(moduleAdminTeam);
                    }

                    // Create and add the component
                    if (!"null".equals(componentDesc) && !componentDesc.isEmpty()) {
                        Component component = new Component();
                        component.setComponentTitle(componentDesc);
                        if (!"null".equals(submissionDate)) {
                            component.setComponentDeadline(submissionDate);
                        } else {
                            component.setComponentDeadline("N/A");
                        }
                        if (!"null".equals(componentCode)) {
                            component.setComponentCode(componentCode);
                        }
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
        // Create a map for quick lookup of source modules by CRN
        Map<String, Module> sourceModulesMap = new HashMap<>();
        for (Module module : sourceModules) {
            if (module.getModuleCRN() != null && !module.getModuleCRN().isEmpty()) {
                sourceModulesMap.put(module.getModuleCRN(), module);
            }
        }

        System.out.println("\n--- Verifying Component Counts ---");

        for (Student student : students) {
            if (student.getModules() == null) {
                continue;
            }
            for (Module studentModule : student.getModules()) {
                String crn = studentModule.getModuleCRN();
                if (crn == null || crn.isEmpty()) {
                    System.out.println("Warning: Student " + student.getName() + " has a module with no CRN.");
                    continue;
                }
                Module sourceModule = sourceModulesMap.get(crn);
                if (sourceModule == null) {
                    continue;
                }
                int studentComponentCount = (studentModule.getComponents() == null) ? 0 : studentModule.getComponents().size();
                int sourceComponentCount = (sourceModule.getComponents() == null) ? 0 : sourceModule.getComponents().size();

                if (studentComponentCount != sourceComponentCount) {
                    System.out.println("Warning: Mismatch for Student " + student.getName() +
                            ", Module CRN " + crn +
                            ", Module Title: " + studentModule.getModuleTitle() +
                            ". Student has " + studentComponentCount + " components, " +
                            "Source data shows " + sourceComponentCount + " components.");
                }
            }
        }
        System.out.println("--- Verification Complete ---");
    }
}
