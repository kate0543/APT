package src;
import java.io.*;
import java.nio.file.*;
import java.util.*;


public class StEP {
    
    public static void main(String[] args) throws IOException {
        String baseFolderPath = "data/BMT/";
        List<String> targetProgrammeCodesList = List.of("BMT.S", "BMT.F"); // Replace with the actual target programme codes

        List<Student> students = new ArrayList<>();
        students = fetchStudents(students, baseFolderPath, targetProgrammeCodesList);
        if (students == null) {
            System.out.println("No students found or error fetching students.");
            return;
        }else{
            System.out.println("Students fetched successfully.");
            for(Student student : students){
                // System.out.println(student.toString());
            
            }
        }
    
        
        

    }
    
    public static List<Student> fetchStudents(List<Student> students, String baseFolderPath, List<String> targetProgrammeCodesList) throws IOException {

        students = SourceDoc.fetchStudents(students, baseFolderPath, targetProgrammeCodesList);
                    // Define the base directory
                    List<File> matchingFiles = new ArrayList<>();
                    try{
            // Get all files matching the pattern
            matchingFiles = findMatchingFiles(baseFolderPath+"StEP/", "BMT");
            
            if (matchingFiles.isEmpty()) {
                System.out.println("No matching files found in " +baseFolderPath+"StEP/");
                return null;
            }
            
            // Process each matching file
            for (File file : matchingFiles) {
                System.out.println("Processing file: " + file.getName());
                System.out.println();
            }
            
        } catch (IOException e) {
            System.err.println("Error processing files: " + e.getMessage());
            e.printStackTrace();
        }
  
        // This method should return a list of students from the database or any other source
        // For demonstration, returning an empty list
        if (matchingFiles.isEmpty()) {
            System.out.println("No matching files found in " + baseFolderPath + "StEP/");
            return students; // Return the original list of students if no files are found
        }else{
                    
        students=calculateLastTermAttendance(matchingFiles, students);
                }
        return students; // Return the updated list of students
    }
    private static List<File> findMatchingFiles(String baseFoldeString, String targetProgrammeString) throws IOException {
        List<File> matchingFiles = new ArrayList<>();
        Path baseFolderPath = Paths.get(baseFoldeString);

        if (!Files.exists(baseFolderPath) || !Files.isDirectory(baseFolderPath)) {
            System.err.println("Base folder does not exist or is not a directory: " + baseFoldeString);
            return matchingFiles; // Return empty list
        }

        // Define a filter for the files
        DirectoryStream.Filter<Path> filter = entry -> {
            String fileName = entry.getFileName().toString();
            // Check if it's a regular file, contains the target string, and ends with .csv
            return Files.isRegularFile(entry) &&
               fileName.contains(targetProgrammeString) &&
               fileName.toLowerCase().endsWith(".csv");
        };

        // Use try-with-resources to ensure the directory stream is closed
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(baseFolderPath, filter)) {
            for (Path entry : stream) {
            matchingFiles.add(entry.toFile());
            }
        } catch (IOException e) {
            System.err.println("Error reading directory: " + baseFoldeString);
            throw e; // Re-throw the exception to be handled by the caller
        }

        return matchingFiles;
    }
    
    private static List<Student> calculateLastTermAttendance(List<File> files, List<Student> students) throws IOException {
        // Define the expected header names
        final String STUDENT_ID_HEADER = "STUDENT_ID";
        final String EVENT_ATTENDED_HEADER = "EVENT_ATTENDED";
    
        for (File file : files) {
            Path filePath = file.toPath();
            System.out.println("Processing StEP attendance data from: " + filePath.getFileName());
    
            try (BufferedReader reader = Files.newBufferedReader(filePath)) {
                String line;
                // Read the header line
                String headerLine = reader.readLine();
                if (headerLine == null) {
                    System.err.println("Skipping empty file: " + filePath.getFileName());
                    continue; // Skip empty files
                }
    
                // Handle comma-separated values
                String[] headers = headerLine.split(",");
                Map<String, Integer> headerMap = new HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    headerMap.put(headers[i].trim(), i); // Store header name and its index
                }
    
                // Check if required headers are present
                if (!headerMap.containsKey(STUDENT_ID_HEADER) || !headerMap.containsKey(EVENT_ATTENDED_HEADER)) {
                    System.err.println("Skipping file due to missing required headers (" + STUDENT_ID_HEADER + ", " + EVENT_ATTENDED_HEADER + "): " + filePath.getFileName());
                    continue; // Skip files without the necessary headers
                }
    
                // Get the indices for the required columns
                int studentIdIndex = headerMap.get(STUDENT_ID_HEADER);
                int eventAttendedIndex = headerMap.get(EVENT_ATTENDED_HEADER);
    
                while ((line = reader.readLine()) != null) {
                    // Split by comma with handling for potential commas within quoted fields
                    String[] columns = parseCSVLine(line);
    
                    // Check if the line has enough columns based on the required indices
                    if (columns.length > Math.max(studentIdIndex, eventAttendedIndex)) {
                        String rawID = columns[studentIdIndex].trim(); // Trim whitespace
                        String eventAttended = columns[eventAttendedIndex].trim(); // Trim whitespace
    
            // Remove leading '@' character and any leading zeros
            String cleaned = rawID.replace("@", "").replaceFirst("^0+(?!$)", "");
            if (cleaned.isEmpty()) {
                // Skip if Banner ID is empty after cleaning
                System.err.println("Skipping record due to empty Banner ID in file " + filePath.getFileName());
                continue;
            }
            int bannerID;
            try {
                bannerID = Integer.parseInt(cleaned);
            } catch (NumberFormatException e) {
                System.err.println("Skipping record due to invalid Banner ID format: '" + cleaned + "' in file " + filePath.getFileName());
                continue; // Skip this record if Banner ID is not a valid integer
            }
            
            // Find the student in the existing list
            Student student = DataPipeline.findStudentById(students, bannerID);
            if (student == null) {
                // If student not found in the list, skip this record
                continue;
            }
                        student.incrementTotalSessionCountLastTerm();
    
                        // Check if the event was 'NotAttended' and increment the count
                        if ("NotAttended".equalsIgnoreCase(eventAttended)) {
                            student.incrementNotAttendedSessionCountLastTerm();
                        }
                        // System.out.println("Student found: " + student.toString()) ;

                    } else {
                        // Log or handle lines that don't have enough columns
                        if (!line.trim().isEmpty()) { // Avoid warning for blank lines
                            System.err.println("Skipping malformed line (not enough columns based on header indices) in " + filePath.getFileName() + ": " + line);
                        }
                    }
                }
            }  
        } // End of loop through files
    System.out.println("Total number of students: " + students.size());
        for (Student student: students) {
            // Calculate the attendance rate for the last term
            student.calculateLastTermAttendance(); 
            }
        return students; // Return the list of students with updated attendance counts
    }
    
    /**
     * Parse a CSV line handling quoted fields that may contain commas
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
                inQuotes = !inQuotes; // Toggle the inQuotes flag
            } else if (c == ',' && !inQuotes) {
                // End of field
                result.add(field.toString());
                field = new StringBuilder();
            } else {
                field.append(c);
            }
        }
        
        // Add the last field
        result.add(field.toString());
        
        return result.toArray(new String[0]);
    }
}