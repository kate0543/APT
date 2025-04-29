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
        String baseFolderPath = "data/SBMT/"; // Base folder path for Qlikview data
        String targetProgrammeCode = "SBMT"; // Replace with the actual target programme codes
        String logFolderPath = DataPipeline.getLogFolderPath(targetProgrammeCode);

        List<Student> students = new ArrayList<>(); // List to store all students

        List<File> ModuleReports = locateEBRFiles(baseFolderPath + "EBR/", "ModuleReport", targetProgrammeCode);
        // students = Qlikview.fetchStudents(baseFolderPath, targetProgrammeCodesList);
        // students = updateStudentsList(students, addComponentsToStudents(ModuleReports, students));
        students = Qlikview.fetchStudents(baseFolderPath, targetProgrammeCode);
        students = fetchStudents(students, baseFolderPath, targetProgrammeCode);

        for (Student student : students) {
            for (Module module : student.getModules()) {
                // System.out.println();
                // System.out.println("update matchingStudent "+student.getBannerID()+" "+student.getName());
                // System.out.println("Module CRN: " + module.getModuleCRN() +" Module ID: " + module.getModuleTitle());
                for (Component component : module.getComponents()) {
                    if (component.getComponentTitle() != null && !component.getComponentTitle().isEmpty()) {
                        if (component.getComponentTitle().contains("\"")) {
                            System.out.println(component.getComponentTitle());
                        }
                    }
                }
                if (module.getComponents().isEmpty()) {
                    // System.out.println("Student "+student.getBannerID()+" "+student.getName()+" has no components for module "+module.getModuleCRN());
                }
            }
        }
    }

    public static List<Student> fetchStudents(List<Student> students, String baseFolderPath, String targetProgrammeCode) throws IOException {
        students = fetchStudentsMR(students, baseFolderPath, targetProgrammeCode);
        students = fetchStudentsPR(students, baseFolderPath, targetProgrammeCode);
        return students;
    }

    public static List<Student> fetchStudentsMR(List<Student> students, String baseFolderPath, String targetProgrammeCode) throws IOException {
        String logFolderPath = DataPipeline.getLogFolderPath(targetProgrammeCode);

        List<String> targetProgrammeCodesList = List.of(targetProgrammeCode + ".S", targetProgrammeCode + ".F"); // Replace with the actual target programme codes

        students = Qlikview.fetchStudents(baseFolderPath, targetProgrammeCode);

        for (String code : targetProgrammeCodesList) {
            try {
                List<File> files = locateEBRFiles(baseFolderPath + "/EBR", "ModuleReport", code);
                students = updateStudentsList(students, addComponentsToStudents(files, logFolderPath, students));
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
        System.out.println("            EBR Module Reports Data processing completed.");
        System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------------------------");

        return students;
    }

    public static List<Student> fetchStudentsPR(List<Student> students, String baseFolderPath, String targetProgrammeCode) throws IOException {
        String logFolderPath = DataPipeline.getLogFolderPath(targetProgrammeCode);

        List<String> targetProgrammeCodesList = List.of(targetProgrammeCode + ".S", targetProgrammeCode + ".F");

        for (String code : targetProgrammeCodesList) {
            try {
                List<File> files = locateEBRFiles(baseFolderPath + "/EBR", "ProgrammeReport", code);
                students = processProgrammeReport(files, logFolderPath, students);
            } catch (IOException e) {
                System.err.println("An error occurred while processing EBR Programme Report files: " + e.getMessage());
                e.printStackTrace();
            }
        }
        if (!students.isEmpty()) {
            for (Student student : students) {
                student.checkFailedComponents();
            }
        }
        System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("            EBR Programme Reports Data processing completed.");
        System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------------------------");

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
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".csv") && name.contains(reportType) && name.contains(targetProgrammeCode));
        System.out.println("Found " + (files != null ? files.length : 0) + " matching files for report type: " + reportType + " and programme code: " + targetProgrammeCode + " in folder: " + folder.getAbsolutePath());
        if (files != null && files.length > 0) {
            for (File file : files) {
                matchingFiles.add(file);
            }
        } else {
            System.err.println("No matching CSV files found in folder: " + folder.getAbsolutePath());
        }

        matchingFiles.sort((f1, f2) -> f2.getName().compareTo(f1.getName()));
        return matchingFiles;
    }

    private static List<Student> processProgrammeReport(List<File> files, String logFolderPath, List<Student> students) {
        // Create log directory and file
        File logDir = new File(logFolderPath);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        File logFile = new File(logDir, "EBR_Programme_Report_loading_log.csv");
        try (PrintWriter logWriter = new PrintWriter(new FileWriter(logFile, true))) {
            // Write header if file is new
            if (logFile.length() == 0) {
                logWriter.println("Timestamp,Level,File,Message");
            }
            for (File file : files) {
                try {
                    DataPipeline.log(logWriter, "INFO", file.getName(), "Processing Programme Report file");
                    List<String> lines = Files.readAllLines(file.toPath());
                    if (lines.size() < 15) {
                        DataPipeline.log(logWriter, "WARNING", file.getName(), "Skipping file - insufficient lines");
                        continue;
                    }

                    // Extract programme information using extractValue helper
                    String programmeTitle = extractValue(lines, "Programme title");
                    if (programmeTitle.isEmpty()) {
                        DataPipeline.log(logWriter, "WARNING", file.getName(), "Programme title is empty");
                        continue;
                    }
                    String programmeCode = extractValue(lines, "Programme code");
                    String programmeLevel = extractValue(lines, "Programme level");
                    DataPipeline.log(logWriter, "INFO", file.getName(), "Programme: " + programmeTitle +
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
                        DataPipeline.log(logWriter, "WARNING", file.getName(), "Could not find Module code line");
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
                    DataPipeline.log(logWriter, "INFO", file.getName(), "Found " + validModuleCRNs.size() +
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
                        DataPipeline.log(logWriter, "WARNING", file.getName(), "Could not find student data");
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
                            DataPipeline.log(logWriter, "WARNING", file.getName(), "Invalid Banner ID: " + fields[0]);
                            continue;
                        }

                        Student student = DataPipeline.findStudentById(students, bannerId);
                        if (student == null) {
                            DataPipeline.log(logWriter, "INFO", file.getName(), "Student not found: " + bannerId + " in the provided qlikview student list");
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
                                String updateMsg = module.updateModuleInfo();
                                DataPipeline.log(logWriter, "INFO", file.getName(), updateMsg);
                            }
                        }
                    }
                } catch (IOException e) {
                    DataPipeline.log(logWriter, "ERROR", file.getName(), "Error processing Programme Report file: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to create log file: " + e.getMessage());
            e.printStackTrace();
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

    public static List<Student> addComponentsToStudents(List<File> csvFiles, String logFolderPath, List<Student> students) {
        List<Student> updatedStudents = students; // List to store updated students

        // Create log directory and file
        File logDir = new File(logFolderPath);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }

        File logFile = new File(logDir, "EBR_Module_Report_loading_log.csv");
        try (PrintWriter logWriter = new PrintWriter(new FileWriter(logFile, true))) {
            // Write header if file is new
            if (logFile.length() == 0) {
                logWriter.println("Timestamp,Level,File,Message");
            }

            for (File csvFile : csvFiles) {
                try {
                    List<String> lines = Files.readAllLines(csvFile.toPath());

                    // Extract module CRN and title
                    String moduleCRN = "";
                    String moduleTitle = "";
                    String moduleID = "";

                    if (lines.size() > 0) {
                        String[] moduleHeaderParts = lines.get(0).split(",");
                        if (moduleHeaderParts.length > 1) moduleCRN = moduleHeaderParts[1].trim();
                        if (moduleHeaderParts.length > 3) moduleTitle = moduleHeaderParts[3].trim();
                    }
                    if (lines.size() > 1) {
                        String[] subjectCourseParts = lines.get(1).split(",");
                        if (subjectCourseParts.length > 1) moduleID = subjectCourseParts[1].trim();
                    }

                    // Extract component titles and their corresponding column indices
                    Map<Integer, String> componentTitleMap = new HashMap<>(); // Map column index to title
                    int componentTitleLineIndex = -1;
                    int studentHeaderLineIndex = -1;

                    for (int i = 0; i < lines.size(); i++) {
                        String line = lines.get(i);
                        if (line.trim().toLowerCase().startsWith("component title")) {
                            componentTitleLineIndex = i;
                        }
                        if (line.trim().toLowerCase().startsWith("student id")) {
                            studentHeaderLineIndex = i;
                        }
                        if (componentTitleLineIndex != -1 && studentHeaderLineIndex != -1) break;
                    }

                    // Log file processing info
                    DataPipeline.log(logWriter, "INFO", csvFile.getName(), "found module info:" + moduleCRN + " " + moduleTitle + " " + moduleID);

                    if (componentTitleLineIndex != -1) {
                        String[] titleParts = lines.get(componentTitleLineIndex).split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                        int componentStartColumn = 5;
                        for (int i = componentStartColumn; i < titleParts.length; i++) {
                            String componentTitle = titleParts[i].trim().replace("\"", "");
                            if (!componentTitle.isEmpty()) {
                                componentTitleMap.put(i, componentTitle);
                            }
                        }
                        if (componentTitleMap.isEmpty()) {
                            DataPipeline.log(logWriter, "WARNING", csvFile.getName(), "No component titles found or parsed correctly");
                        }
                    } else {
                        DataPipeline.log(logWriter, "WARNING", csvFile.getName(), "'Component title' line not found");
                    }

                    if (studentHeaderLineIndex != -1 && !componentTitleMap.isEmpty()) {
                        for (int i = studentHeaderLineIndex + 1; i < lines.size(); i++) {
                            String line = lines.get(i);
                            if (line.trim().isEmpty() || !line.trim().startsWith("@")) {
                                break;
                            }

                            String[] studentParts = line.split(",");
                            Integer bannerId = parseBannerID(studentParts[0]);
                            if (bannerId == null) {
                                DataPipeline.log(logWriter, "INFO", csvFile.getName(), "Skipping line due to invalid Banner ID: " + studentParts[0]);
                                continue;
                            }

                            Student matchingStudent = DataPipeline.findStudentById(updatedStudents, bannerId);
                            if (matchingStudent == null) {
                                continue;
                            }

                            Module studentModule = findModuleByModuleCRN(matchingStudent, moduleCRN);
                            if (studentModule == null) {
                                continue;
                            }

                            if (studentModule.getComponentDetailsLoaded()) {
                                continue;
                            }

                            List<Component> components = new ArrayList<>();
                            for (Map.Entry<Integer, String> entry : componentTitleMap.entrySet()) {
                                int columnIndex = entry.getKey();
                                String componentTitle = entry.getValue();

                                if (columnIndex < studentParts.length) {
                                    String rawRecord = studentParts[columnIndex].trim();
                                    Component component = new Component(
                                            moduleCRN,
                                            moduleID,
                                            componentTitle,
                                            rawRecord
                                    );
                                    components.add(component);
                                } else {
                                    DataPipeline.log(logWriter, "WARNING", csvFile.getName(), "Missing data for component '" + componentTitle +
                                            "' (column " + columnIndex + ") for student " + bannerId);
                                    Component component = new Component(moduleCRN, moduleID, componentTitle, "");
                                    components.add(component);
                                }
                            }

                            if (!components.isEmpty()) {
                                studentModule.setComponents(components);
                                studentModule.setComponentDetailsLoaded(true);
                                DataPipeline.log(logWriter, "INFO", csvFile.getName(), "Successfully added " + components.size() +
                                        " components for module " + moduleCRN + ":  " + moduleTitle + " student " + bannerId + ": " + matchingStudent.getName());
                            } else {
                                DataPipeline.log(logWriter, "INFO", csvFile.getName(), "No components were added for module " +
                                        moduleCRN + " student " + bannerId + " " + matchingStudent.getName());
                            }
                        }
                    } else {
                        if (studentHeaderLineIndex == -1)
                            DataPipeline.log(logWriter, "WARNING", csvFile.getName(), "'Student ID' line not found");
                        if (componentTitleMap.isEmpty() && componentTitleLineIndex != -1)
                            DataPipeline.log(logWriter, "WARNING", csvFile.getName(), "Component titles found but map is empty - check parsing logic");
                        DataPipeline.log(logWriter, "WARNING", csvFile.getName(), "Skipping student data processing due to missing headers or titles");
                    }

                } catch (IOException e) {
                    DataPipeline.log(logWriter, "ERROR", csvFile.getName(), "Error processing file: " + e.getMessage());
                    e.printStackTrace();
                } catch (NumberFormatException e) {
                    DataPipeline.log(logWriter, "ERROR", csvFile.getName(), "Error parsing number: " + e.getMessage());
                } catch (ArrayIndexOutOfBoundsException e) {
                    DataPipeline.log(logWriter, "ERROR", csvFile.getName(), "Error accessing array index: " + e.getMessage());
                } catch (Exception e) {
                    DataPipeline.log(logWriter, "ERROR", csvFile.getName(), "An unexpected error occurred: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to create log file: " + e.getMessage());
            e.printStackTrace();
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
