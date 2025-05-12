package src;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import javax.xml.crypto.Data;

public class StEP {

    public static void main(String[] args) throws IOException {
        String baseFolderPath = "data/SBMT/";
 
        String targetProgrammeCode = "LBL";// Replace with the actual target programme codes
        String logFolderPath = DataPipeline.getLogFolderPath(targetProgrammeCode);

        List<Student> students = new ArrayList<>();
        students = fetchStudents(students, baseFolderPath, targetProgrammeCode, new DataPipeline()); // Fetch students from Qlikview data

        if (students == null) {
            System.out.println("No students found or error fetching students.");
            return;
        } else {
            System.out.println("Students fetched successfully.");
            for (Student student : students) {
                // System.out.println(student.toString());
            }
        }
        filterStudentsByLasttermAttendance(students,logFolderPath, 30); // Example threshold for filtering
    }

    /**
     * Fetch students and process attendance files.
     */
    public static List<Student> fetchStudents(List<Student> students, String baseFolderPath, String targetProgrammeCode,DataPipeline pipeline) throws IOException {

        // students = Qlikview.fetchStudents(baseFolderPath, targetProgrammeCode);
        // students = EBR.fetchStudents(students, baseFolderPath, targetProgrammeCode);
        
        // students = SourceDoc.fetchStudents(students, baseFolderPath, targetProgrammeCode);

        students= IYR.fetchStudents(students, baseFolderPath, targetProgrammeCode, pipeline);

        String logFolderPath = pipeline.getLogFolderPath(targetProgrammeCode);

        List<File> matchingFiles = new ArrayList<>();
        try {
            // Get all files matching the pattern
            matchingFiles = locateEBRFiles(baseFolderPath + "StEP/", targetProgrammeCode);

            if (matchingFiles.isEmpty()) {
                System.out.println("No matching files found in " + baseFolderPath + "StEP/");
                return null;
            }
            else for (File file : matchingFiles) {
                System.out.println("Found "+matchingFiles.size()+" matching file: " + file.getAbsolutePath());
            }


        } catch (IOException e) {
            System.err.println("Error processing files: " + e.getMessage());
            e.printStackTrace();
        }

        // If no files found, return the original list
        if (matchingFiles.isEmpty()) {
            System.out.println("No matching files found in " + baseFolderPath + "StEP/");
            return students;
        } else {
            students = calculateLastTermAttendance(matchingFiles, logFolderPath.toString(),students);
        }
        System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("            StEP Data processing completed.");
        System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------------------------");

        return students;
    }

    /**
     * Find files in the given folder that match the programme string and end with .csv.
     */
    private static List<File> locateEBRFiles(String folderPath, String targetProgrammeCode) throws IOException {
        List<File> matchingFiles = new ArrayList<>();

        File folder = new File(folderPath);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".csv") && name.contains(targetProgrammeCode));
        System.out.println("Found " + (files != null ? files.length : 0) + " CSV files in folder: " + folder.getAbsolutePath());
        if (files != null && files.length > 0) {
            for (File file : files) {
                matchingFiles.add(file);
            }
        } else {
            System.err.println("No matching CSV files found in folder: " + folder.getAbsolutePath());
        }

        return matchingFiles;
    }

    /**
     * Process attendance CSV files and update students' attendance data.
     */
    private static List<Student> calculateLastTermAttendance(List<File> files, String logFolderPath, List<Student> students) throws IOException {
       
        
       
        File logDir = new File(logFolderPath);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        File logFile = new File(logDir, "StEP_calculateLastTermAttendance_log.csv");
        final String STUDENT_ID_HEADER = "STUDENT_ID";
        final String EVENT_ATTENDED_HEADER = "EVENT_ATTENDED";

        try (PrintWriter logWriter = new PrintWriter(new FileWriter(logFile, true))) {
            for (File file : files) {
                Path filePath = file.toPath();
                log(logWriter, "INFO", filePath.getFileName().toString(), "Processing StEP attendance data from: " + filePath.getFileName());

                try (BufferedReader reader = Files.newBufferedReader(filePath)) {
                    String line;
                    String headerLine = reader.readLine();
                    if (headerLine == null) {
                        log(logWriter, "WARN", filePath.getFileName().toString(), "Skipping empty file: " + filePath.getFileName());
                        continue;
                    }

                    String[] headers = headerLine.split(",");
                    Map<String, Integer> headerMap = new HashMap<>();
                    for (int i = 0; i < headers.length; i++) {
                        headerMap.put(headers[i].trim(), i);
                    }

                    // Check if required headers are present
                    if (!headerMap.containsKey(STUDENT_ID_HEADER) || !headerMap.containsKey(EVENT_ATTENDED_HEADER)) {
                        log(logWriter, "WARN", filePath.getFileName().toString(),
                            "Skipping file due to missing required headers (" + STUDENT_ID_HEADER + ", " + EVENT_ATTENDED_HEADER + "): " + filePath.getFileName());
                        continue;
                    }

                    int studentIdIndex = headerMap.get(STUDENT_ID_HEADER);
                    int eventAttendedIndex = headerMap.get(EVENT_ATTENDED_HEADER);

                    while ((line = reader.readLine()) != null) {
                        String[] columns = parseCSVLine(line);

                        if (columns.length > Math.max(studentIdIndex, eventAttendedIndex)) {
                            String rawID = columns[studentIdIndex].trim();
                            String eventAttended = columns[eventAttendedIndex].trim();

                            // Remove leading '@' character and any leading zeros
                            String cleaned = rawID.replace("@", "").replaceFirst("^0+(?!$)", "");
                            if (cleaned.isEmpty()) {
                                log(logWriter, "WARN", filePath.getFileName().toString(),
                                    "Skipping record due to empty Banner ID in file " + filePath.getFileName());
                                continue;
                            }
                            int bannerID;
                            try {
                                bannerID = Integer.parseInt(cleaned);
                            } catch (NumberFormatException e) {
                                log(logWriter, "WARN", filePath.getFileName().toString(),
                                    "Skipping record due to invalid Banner ID format: '" + cleaned + "' in file " + filePath.getFileName());
                                continue;
                            }

                            // Find the student in the existing list
                            Student student = DataPipeline.findStudentById(students, bannerID);
                            if (student == null) {
                                continue;
                            }
                            student.incrementTotalSessionCountLastTerm();

                            // Check if the event was 'NotAttended' and increment the count
                            if (eventAttended.toLowerCase().contains("not") && eventAttended.toLowerCase().contains("attended")) {
                                student.incrementNotAttendedSessionCountLastTerm();
                            }
                        } else {
                            if (!line.trim().isEmpty()) {
                                log(logWriter, "WARN", filePath.getFileName().toString(),
                                    "Skipping malformed line (not enough columns based on header indices): " + line);
                            }
                        }
                    }
                }
            }
            log(logWriter, "INFO", "Summary", "Total number of students: " + students.size());
        }

        for (Student student : students) {
            // Calculate the attendance rate for the last term
            student.calculateLastTermAttendance();
        }
        return students;
    }
    public static List<Student> filterStudentsByLasttermAttendance(List<Student> students,String logFolderPath, Integer minAttendanceRate) throws IOException {
       
    List<Student> lowAttendanceStudents = new ArrayList<>();


    File logDir = new File(logFolderPath);
    if (!logDir.exists()) {
        logDir.mkdirs();
    }
    String logFileName = "StEP_lowAttendanceStudents_log_" + minAttendanceRate + "%.csv";
    File logFile = new File(logDir, logFileName);

     
    try (PrintWriter logWriter = new PrintWriter(new FileWriter(logFile))) {
        logWriter.println("StudentName,BannerID,LastTermAttendanceRate");
        for (Student student : students) {
            if (student.getStudentLastTermAttendanceRate() != null) {
                if (student.getStudentLastTermAttendanceRate() < minAttendanceRate) {
                    lowAttendanceStudents.add(student);
                    log(logWriter, "INFO", "LowAttendance", student.getName() + "," + student.getBannerID() + "," + student.getStudentLastTermAttendanceRate());
                }
            } else {
                log(logWriter, "WARN", "LowAttendance", "Student " + student.getBannerID() + " has no last term attendance data.");
            }
        }
        log(logWriter, "INFO", "LowAttendance", "Low attendance students log written to: " + logFile + " with " + lowAttendanceStudents.size() + " students.");
    } catch (IOException e) {
        log(new PrintWriter(System.err), "ERROR", "LowAttendance", "Error writing low attendance students log: " + e.getMessage());
    }
    return lowAttendanceStudents;
    }

    /**
     * Parse a CSV line handling quoted fields that may contain commas.
     * @param line The CSV line to parse
     * @return Array of field values
     */
    private static String[] parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder field = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(field.toString());
                field = new StringBuilder();
            } else {
                field.append(c);
            }
        }
        result.add(field.toString());
        return result.toArray(new String[0]);
    }
    public static void log(PrintWriter writer, String level, String file, String message) {
        // Format timestamp
        String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());

        // Escape any commas in the message to preserve CSV structure
        message = message.replace("\"", "\"\"");
        if (message.contains(",")) {
            message = "\"" + message + "\"";
        }

        // Write the log entry
        writer.println(timestamp + "," + level + ",\"" + file + "\"," + message);
        writer.flush();

        // Also print to console for debugging
        // System.out.println("[" + level + "] " + file + ": " + message);
    }
}
