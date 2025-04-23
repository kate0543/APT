package src;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

 
public class EBR {

    private static String Term = "";
    private static String School = "";
    private static String programmeCode = "";
    private static String Level = "";

    private static String fileType = "ExamBoardReporter";
    private static String fileFormat = "CSV";

    public static void main(String[] args) throws IOException {
        String baseFolderPath = "data/BMT/"; // Base folder path for Qlikview data
        List<String> targetProgrammeCodesList = List.of("BMT.S", "BMT.F"); // Target programme codes
        List<Student> students = new ArrayList<>(); // List to store all students

        List<File> ModuleReports =locateEBRFiles(baseFolderPath+"EBR/", "ModuleReport",targetProgrammeCodesList.get(1));
        // students = Qlikview.fetchStudents(baseFolderPath, targetProgrammeCodesList);
        // students = updateStudentsList(students, addComponentsToStudents(ModuleReports, students));
        students = fetchStudentsMR(students, baseFolderPath, targetProgrammeCodesList);

        for (Student student : students) {
            for(Module module: student.getModules()){
                        // System.out.println();
                        // System.out.println("update matchingStudent "+student.getBannerID()+" "+student.getName());
                        // System.out.println("Module CRN: " + module.getModuleCRN() +" Module ID: " + module.getModuleTitle());
                       for(Component component: module.getComponents()){
                           if(component.getComponentTitle() != null && !component.getComponentTitle().isEmpty()){
                            if(component.getComponentTitle().contains("\"")) {
                               System.out.println(component.getComponentTitle());
                            }
                           }
                           
                       }
                       if(module.getComponents().isEmpty()){ 
                        
                            System.out.println("Student "+student.getBannerID()+" "+student.getName()+" has no components for module "+module.getModuleCRN());
                    
                       }
            }
    
        }
    
}
    public static List<Student> fetchStudentsMR(List<Student> students, String baseFolderPath, List<String> targetProgrammeCodesList) throws IOException {
        
        
        for (String targetProgrammeCode : targetProgrammeCodesList) {
            try {
                students.addAll(Qlikview.fetchStudents(baseFolderPath, List.of(targetProgrammeCode)));
                System.out.println("Found " + students.size() + " students for programme code: " + targetProgrammeCode);
            } catch (IOException e) {
                System.err.println("An error occurred while reading Qlikview data: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        for (String targetProgrammeCode : targetProgrammeCodesList) {
            try {
                List<File> files = locateEBRFiles(baseFolderPath + "/EBR", "ModuleReport",targetProgrammeCode);
                 students = updateStudentsList(students, addComponentsToStudents(files, students));
            } catch (IOException e) {
                System.err.println("An error occurred while processing EBR Module Report files: " + e.getMessage());
                e.printStackTrace();
            }
        }
        if (!students.isEmpty()) {
            for (Student student : students) {
                student.checkFailedComponents(); // Check for failed modules
            }
        }
        System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("                                EBR Module Reports Data processing completed.");
        System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------------------------");
    
        return students;
    }   
        public static List<Student> fetchStudentsPR(List<Student> students, String baseFolderPath, List<String> targetProgrammeCodesList) throws IOException {


        // for (String targetProgrammeCode : targetProgrammeCodesList) {
        //     try {
        //         List<File> files = locateEBRFiles(baseFolderPath + "/EBR", "ProgrammeReport",targetProgrammeCode);
        //         students = processProgrammeReport(files, students);
        //     } catch (IOException e) {
        //         System.err.println("An error occurred while processing EBR Porgramme files: " + e.getMessage());
        //         e.printStackTrace();
        //     }
        // }
        // if (!students.isEmpty()) {
        //     for (Student student : students) {
        //         student.checkFailedComponents(); // Check for failed modules
        //     }
        // }
        // System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------------------------");
        // System.out.println("                                EBR Programme Reports Data processing completed.");
        // System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------------------------");

        return students;
    }

    public static void setProgrammeCode(String code) {
        programmeCode = code;
    }

    /**
     * Locates EBR files for a given programme code.
     */
    public static List<File> locateEBRFiles(String folderPath, String reportType, String targetProgrammeCode) throws IOException {
        List<File> matchingFiles = new ArrayList<>();
        File folder = new File(folderPath);
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles((dir, name) ->
                name.endsWith(".csv") &&
                name.contains(reportType) &&
                name.contains(targetProgrammeCode)
            );
            if (files != null) {
                for (File file : files) {
                    matchingFiles.add(file);
                }
            }
        }

        System.out.println("Found " + matchingFiles.size() + " matching files for report type: " + reportType + " and programme code: " + targetProgrammeCode);
  

        matchingFiles.sort((f1, f2) -> f2.getName().compareTo(f1.getName()));
        // for (File file : matchingFiles) {
        //     System.out.println("Matching file: " + file.getName());
        // }

        return matchingFiles;
    }
    private static List<Student> processProgrammeReport(List<File> files, List<Student> students) {
        for (File file : files) {
            try {
                System.out.println("Processing Programme Report file: " + file.getName());
                List<String> lines = Files.readAllLines(file.toPath());
                if (lines.size() < 15) {
                    System.out.println("Skipping file - insufficient lines: " + file.getName());
                    continue;
                }

                // Extract programme information using extractValue helper
                String programmeTitle = extractValue(lines, "Programme title");
                if (programmeTitle.isEmpty()) {
                    System.out.println("Programme title is empty in file: " + file.getName());
                    continue;
                }
                String programmeCode = extractValue(lines, "Programme code");
                String programmeLevel = extractValue(lines, "Programme level");
                System.out.println("Programme: " + programmeTitle +
                                   " (" + programmeCode + ") Level: " + programmeLevel);

                // Locate the "Module code" line
                int moduleCRNLine = -1;
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).toLowerCase().contains("module code")) {
                        moduleCRNLine = i;
                        break;
                    }
                }
                if (moduleCRNLine == -1) {
                    System.out.println("Could not find Module code line in file: " + file.getName());
                    continue;
                }

                // Extract the CRNs and their positions
                String[] crnFields = lines.get(moduleCRNLine).split(",");
                List<String> validModuleCRNs = new ArrayList<>();
                List<Integer> moduleCRNPositions = new ArrayList<>();
                for (int i = 4; i < crnFields.length; i++) {
                    String crn = crnFields[i].trim();
                    if (crn.matches("\\d+")) {
                        validModuleCRNs.add(crn);
                        moduleCRNPositions.add(i);
                    }
                }
                System.out.println("Found " + validModuleCRNs.size() +
                                   " module CRNs: " + String.join(", ", validModuleCRNs));

                // Find where student data begins (lines that start with "@")
                int studentDataStart = -1;
                for (int i = moduleCRNLine + 2; i < lines.size(); i++) {
                    if (lines.get(i).startsWith("@")) {
                        studentDataStart = i;
                        break;
                    }
                }
                if (studentDataStart == -1) {
                    System.out.println("Could not find student data in file: " + file.getName());
                    continue;
                }

                // Process each student record
                for (int i = studentDataStart; i < lines.size(); i++) {
                    String line = lines.get(i);
                    if (!line.startsWith("@")) continue;
                    String[] fields = line.split(",");
                    if (fields.length < 5) continue;

                    // Parse Banner ID
                    String rawId = fields[0].replace("@", "").replaceFirst("^0+(?!$)", "");
                    int bannerId;
                    try {
                        bannerId = Integer.parseInt(rawId);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid Banner ID: " + fields[0]);
                        continue;
                    }

                    Student student = DataPipeline.findStudentById(students, bannerId);
                    if (student == null) {
                        System.out.println("Student not found: " + bannerId);
                        continue;
                    }

                    // Update each module record for this student
                    for (int j = 0; j < validModuleCRNs.size(); j++) {
                        String moduleCRN = validModuleCRNs.get(j);
                        int pos = moduleCRNPositions.get(j);
                        if (pos >= fields.length) continue;
                        String record = fields[pos].trim();
                        if (record.isEmpty()) continue;

                        // Clean up format
                        record = record.replace("*", "").replace("R", "").trim();

                        // Locate the Module object and update if not yet updated
                        Module module = findModuleByModuleCRN(student, moduleCRN);
                        if (module != null && !module.getModuleInfoUpdated()) {
                            module.setModuleRecord(record);
                            module.updateModuleInfo();
                            System.out.println("Updated module " + moduleCRN +
                                               " for student " + bannerId +
                                               " with record: " + record);
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Error processing Programme Report file " +
                                    file.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        return students;
    }
    private static String extractValue(List<String> lines, String key) {
        for (String line : lines) {
            String[] parts = line.split(",");
            if (parts.length > 1 && parts[0].trim().equals(key)) {
                return parts[1].trim();
            }
        }
        return "";
    }

    private static Module findModuleByModuleCRN(Student student, String moduleCRN) {
        for (Module module : student.getModules()) {
            if (module.getModuleCRN() != null && module.getModuleCRN().equals(moduleCRN)) {
                return module;
            }
        }
        return null;
    }
 
    public static Integer parseBannerID(String raw) {
        try {
            String cleaned = raw.replace("@", "").trim().split(",")[0].replaceFirst("^0+(?!$)", "");
            return Integer.parseInt(cleaned);
        } catch (Exception e) {
            System.err.println("Failed to parse Banner ID from: " + raw);
            return null;
        }
    }
 

    public static List<Student> addComponentsToStudents(List<File> csvFiles, List<Student> students) {
        List<Student> updatedStudents = students; // List to store updated students
        for (File csvFile : csvFiles) {
            try {
                List<String> lines = Files.readAllLines(csvFile.toPath());
                // It's generally better to parse CSV correctly rather than just removing quotes,
                // as quotes are often used to enclose fields containing commas.
                // lines = DataPipeline.cleanLines(lines); // Consider revising or removing this if it causes issues.

                // Extract module CRN and title using a safer method if possible
                String moduleCRN = "";
                String moduleTitle = "";
                String moduleID = "";

                if (lines.size() > 0) {
                    // Simple split might fail if fields contain commas. Consider a CSV parser library.
                    String[] moduleHeaderParts = lines.get(0).split(",");
                    if (moduleHeaderParts.length > 1) moduleCRN = moduleHeaderParts[1].trim();
                    if (moduleHeaderParts.length > 3) moduleTitle = moduleHeaderParts[3].trim(); // e.g., "Legal Aspects of Business"
                }
                if (lines.size() > 1) {
                    String[] subjectCourseParts = lines.get(1).split(",");
                    if (subjectCourseParts.length > 1) moduleID = subjectCourseParts[1].trim(); // e.g., "M221/20006"
                }


                // Extract component titles and their corresponding column indices
                Map<Integer, String> componentTitleMap = new HashMap<>(); // Map column index to title
                int componentTitleLineIndex = -1;
                int studentHeaderLineIndex = -1; // Find this early too

                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                     // Use contains for flexibility, trim and lowerCase for robustness
                    if (line.trim().toLowerCase().startsWith("component title")) {
                        componentTitleLineIndex = i;
                    }
                    if (line.trim().toLowerCase().startsWith("student id")) {
                        studentHeaderLineIndex = i;
                    }
                    if(componentTitleLineIndex != -1 && studentHeaderLineIndex != -1) break; // Found both headers
                }

                System.out.println();
                System.out.println("Processing file: " + csvFile.getName());
                System.out.println(moduleCRN + " " + moduleTitle + " " + moduleID);

                if (componentTitleLineIndex != -1) {
                    // *** Robust CSV Parsing Needed Here ***
                    // The simple split(",") is the likely cause of the problem.
                    // If a title like "Report - 1,500 words" exists, split(",") will break it.
                    // A proper CSV parser would handle quoted commas correctly.
                    // For now, we demonstrate the issue with the simple split:
                    String[] titleParts =lines.get(componentTitleLineIndex).split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                    // Component data usually starts after metadata columns (e.g., Student ID, Name, etc.)
                    // Let's assume the first component data is in column index 5 based on the original code.
                    int componentStartColumn = 5;
                    for (int i = componentStartColumn; i < titleParts.length; i++) {
                        // Trim and remove quotes from the component title
                        String componentTitle = titleParts[i].trim().replace("\"", "");
                         // Skip empty headers that might result from trailing commas or just quotes
                        if (!componentTitle.isEmpty()) {
                             // Store the column index and the cleaned title
                            componentTitleMap.put(i, componentTitle);
                            System.out.println("Found Component title: '" + componentTitle + "' at column index " + i);
                        }
                    }
                     if (componentTitleMap.isEmpty()) {
                         System.out.println("Warning: No component titles found or parsed correctly in " + csvFile.getName());
                     }
                } else {
                     System.out.println("Warning: 'Component title' line not found in " + csvFile.getName());
                }


                if (studentHeaderLineIndex != -1 && !componentTitleMap.isEmpty()) {
                    // Process student data
                    for (int i = studentHeaderLineIndex + 1; i < lines.size(); i++) {
                        String line = lines.get(i);
                        if (line.trim().isEmpty() || !line.trim().startsWith("@")) {
                            // Stop if line is empty or doesn't seem to be a student record
                            // Consider adding more robust checks for end-of-data
                             System.out.println("Reached end of student data or encountered non-student line at line " + (i + 1));
                            break;
                        }

                        // *** Robust CSV Parsing Needed Here Too ***
                        // Splitting student data the same way is problematic.
                        String[] studentParts = line.split(",");

                        Integer bannerId = parseBannerID(studentParts[0]); // Use the existing helper
                        if (bannerId == null) {
                             System.out.println("Skipping line due to invalid Banner ID: " + studentParts[0]);
                            continue; // Skip if banner ID is invalid
                        }


                        // Find the matching student
                        Student matchingStudent = DataPipeline.findStudentById(updatedStudents, bannerId);
                        if (matchingStudent == null) {
                            // System.out.println("Student not found: " + bannerId);
                            continue; // Skip if student not found
                        }

                        // Find the specific module for this student
                        Module studentModule = findModuleByModuleCRN(matchingStudent, moduleCRN);
                        if (studentModule == null) {
                            //  System.out.println("Module " + moduleCRN + " not found for student " + bannerId);
                            continue; // Skip if student doesn't have this module
                        }

                        // Check if components for this module seem loaded (basic check)
                        // A more robust check might compare against componentTitleMap size/keys
                        if (studentModule.getComponentDetailsLoaded()) {
                             System.out.println("Components already seem loaded for module " + moduleCRN + " student " + bannerId + ". Skipping.");
                            continue; // Skip if already processed
                        }

                        List<Component> components = new ArrayList<>();
                        // Iterate through the identified component columns
                        for (Map.Entry<Integer, String> entry : componentTitleMap.entrySet()) {
                            int columnIndex = entry.getKey();
                            String componentTitle = entry.getValue(); // Title is already cleaned

                            if (columnIndex < studentParts.length) {
                                String rawRecord = studentParts[columnIndex].trim();

                                // Create a new component
                                Component component = new Component(
                                    moduleCRN,     // moduleCRN
                                    moduleID,      // moduleID
                                    componentTitle, // componentTitle (cleaned)
                                    rawRecord      // rawRecord (potentially misaligned)
                                );
                                components.add(component);
                                 // System.out.println("  Added component: " + componentTitle + " with record: " + rawRecord);
                            } else {
                                // Handle cases where student line doesn't have enough columns
                                System.out.println("Warning: Missing data for component '" + componentTitle + "' (column " + columnIndex + ") for student " + bannerId);
                                // Optionally create a component with null/empty record or skip
                                Component component = new Component(moduleCRN, moduleID, componentTitle, ""); // Example: add with empty record
                                components.add(component);
                            }
                        }

                        // Add components to the student's module
                        if (!components.isEmpty()) {
                            studentModule.setComponents(components);
                            studentModule.setComponentDetailsLoaded(true); // Mark as processed
                            System.out.println("Successfully added " + components.size() + " components for module " + moduleCRN + " student " + bannerId);
                        } else {
                             System.out.println("No components were added for module " + moduleCRN + " student " + bannerId);
                        }
                    }
                } else {
                     if(studentHeaderLineIndex == -1) System.out.println("Warning: 'Student ID' line not found in " + csvFile.getName());
                     if(componentTitleMap.isEmpty() && componentTitleLineIndex != -1) System.out.println("Warning: Component titles found but map is empty - check parsing logic.");
                     System.out.println("Skipping student data processing for " + csvFile.getName() + " due to missing headers or titles.");
                }

            } catch (IOException e) {
                 System.err.println("Error processing file " + csvFile.getName() + ": " + e.getMessage());
                e.printStackTrace();
            } catch (NumberFormatException e) {
                 System.err.println("Error parsing number in file " + csvFile.getName() + ": " + e.getMessage());
                 // Log the line number if possible, or context
            } catch (ArrayIndexOutOfBoundsException e) {
                 System.err.println("Error accessing array index (likely due to unexpected CSV format) in file " + csvFile.getName() + ": " + e.getMessage());
                 // Log the line number or context
            } catch (Exception e) { // Catch broader exceptions
                 System.err.println("An unexpected error occurred processing file " + csvFile.getName() + ": " + e.getMessage());
                 e.printStackTrace();
            }
        }

        return updatedStudents;
    }

    /**
     * Updates the given list of students with new or modified student records.
     * If a student with the same BannerID exists, it is replaced; otherwise, the new student is added.
     */
    public static List<Student> updateStudentsList(List<Student> students, List<Student> updateStudents) {
        Map<Integer, Student> studentMap = new HashMap<>();
        for (Student student : students) {
            studentMap.put(student.getBannerID(), student);
        }
        for (Student updated : updateStudents) {
            studentMap.put(updated.getBannerID(), updated);
        }
        return new ArrayList<>(studentMap.values());
    }


}
