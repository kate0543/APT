package src;

import java.util.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class IYR {
    // List to store Banner IDs found in IYR files
    public static List<Integer> IYR_bannerIDs = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        String baseFolderPath = "data/SBMT/";
 
        String targetProgrammeCode = "SBMT";// Replace with the actual target programme codes
        String logFolderPath = DataPipeline.getLogFolderPath(targetProgrammeCode);
        List<Student> students = new ArrayList<>(); 
        students = fetchStudents(students, baseFolderPath, targetProgrammeCode, new DataPipeline()); // Fetch students from Qlikview data
    } 
        public static List<Student> fetchStudents(List<Student> students, String baseFolderPath, String targetProgrammeCode, DataPipeline pipeline) throws IOException {
            // students = Qlikview.fetchStudents(baseFolderPath, targetProgrammeCode);
            // students = EBR.fetchStudents(students, baseFolderPath, targetProgrammeCode);
            students = SourceDoc.fetchStudents(students, baseFolderPath, targetProgrammeCode, pipeline); 

            String logFolderPath = pipeline.getLogFolderPath(targetProgrammeCode);
            List<String> targetProgrammeCodesList = List.of(targetProgrammeCode+".S", targetProgrammeCode+".F"); // Replace with the actual target programme codes

            List<File> dataFiles = locateIYRFiles(baseFolderPath+"IYR/",targetProgrammeCode);

            students = updateIYRComponents(students, logFolderPath, dataFiles);

            System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------------------------");
            System.out.println("            IYR Data processing completed.");
            System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------------------------");
    
            return students;

          
        } 
    public static List<File> locateIYRFiles(String baseFolderPath, String targetProgrammeCode) {
        List<File> files = new ArrayList<>();
        File folder = new File(baseFolderPath);
        File[] matchedFiles = folder.listFiles((dir, name) -> name.endsWith(".csv") && name.contains("IYR"));
        System.out.println("Found " + (matchedFiles != null ? matchedFiles.length : 0) + " IYR CSV files in folder: " + folder.getAbsolutePath());
        if (matchedFiles != null && matchedFiles.length > 0) {
            for (File file : matchedFiles) { 
                files.add(file);
            }
        } else {
            System.err.println("No matching IYR CSV files found in folder: " + folder.getAbsolutePath());
        }
        return files;
    }


public static List<Student> updateIYRComponents(List<Student> students, String logFolderPath,List<File> files) {
    File logDir = new File(logFolderPath);
    if (!logDir.exists()) {
        logDir.mkdirs();
    }
    File logFile = new File(logDir, "IYR_updateIYRComponents_log.csv");

    List<Student> IYRStudents = new ArrayList<>(); // List to hold students with IYR components
    if (files.isEmpty()) {
        System.out.println("No IYR files found to process.");
        return students; // Return the original list if no files are found
    }
    // Define expected header names
    final String HEADER_BANNER_ID = "Banner ID";
    final String HEADER_CRN = "CRN";
    final String HEADER_COMP_DESC = "Comp Desc";

    List<Integer> IYR_bannerIDs = new ArrayList<>();

    try (PrintWriter logWriter = new PrintWriter(new FileWriter(logFile, true))) {
        for (File file : files) { 
            DataPipeline.log(logWriter, "INFO", file.getName(), "Processing file: " + file.getPath());

            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                List<String> header = null;
                int bannerIdIndex = -1, crnIndex = -1, compDescIndex = -1;

                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty() || line.contains("PG")) {
                        DataPipeline.log(logWriter, "DEBUG", file.getName(), "Skipping line: " + line);
                        continue;
                    }

                    String[] values = line.split(",", -1); // Include trailing empty fields

                    if (header == null) {
                        header = Arrays.asList(values);
                        DataPipeline.log(logWriter, "INFO", file.getName(), "Header found: " + header);

                        for (int i = 0; i < header.size(); i++) {
                            String col = header.get(i).trim().replaceAll("^\"|\"$", "");
                            if (col.equalsIgnoreCase(HEADER_BANNER_ID)) bannerIdIndex = i;
                            else if (col.equalsIgnoreCase(HEADER_CRN)) crnIndex = i;
                            else if (col.equalsIgnoreCase(HEADER_COMP_DESC)) compDescIndex = i;
                        }

                        if (bannerIdIndex == -1)
                            DataPipeline.log(logWriter, "ERROR", file.getName(), "'Banner ID' column not found in header: " + header);
                        if (crnIndex == -1)
                            DataPipeline.log(logWriter, "WARN", file.getName(), "'CRN' column not found.");
                        if (compDescIndex == -1)
                            DataPipeline.log(logWriter, "WARN", file.getName(), "'Comp Desc' column not found.");

                        continue; // Skip header row
                    }

                    if (values.length <= Math.max(bannerIdIndex, Math.max(crnIndex, compDescIndex))) {
                        DataPipeline.log(logWriter, "WARN", file.getName(), "Skipping row due to insufficient columns: " + Arrays.toString(values));
                        continue;
                    }

                    String rawID = values[bannerIdIndex].trim().replaceAll("^\"|\"$", "");
                    String crn = crnIndex != -1 ? values[crnIndex].trim().replaceAll("^\"|\"$", "") : "";
                    String componentTitle = compDescIndex != -1 ? values[compDescIndex].trim().replaceAll("^\"|\"$", "") : "";

                    if (rawID.isEmpty()) {
                        DataPipeline.log(logWriter, "WARN", file.getName(), "Skipping row with empty Banner ID: " + Arrays.toString(values));
                        continue;
                    }

                    String cleaned = rawID.replace("@", "").replaceFirst("^0+(?!$)", "");
                    try {
                        int bannerID = Integer.parseInt(cleaned);
                        IYR_bannerIDs.add(bannerID);
                        DataPipeline.log(logWriter, "DEBUG", file.getName(), "Parsed Banner ID: " + bannerID + " from raw ID: " + rawID);

                        Student student = DataPipeline.findStudentById(students, bannerID);
                        if (student != null) {
                            IYRStudents.add(student); // Add student to IYRStudents list
                            boolean componentUpdated = false;
                            for (Module module : student.getModules()) {
                                if (module.getModuleCRN().equals(crn)) {
                                    for (Component component : module.getComponents()) {
                                        if (component.getComponentTitle().equals(componentTitle)) {
                                            component.setComponentIYR(true);
                                            componentUpdated = true;
                                            DataPipeline.log(logWriter, "INFO", file.getName(), "Set IYR=true for Student " + bannerID + " ,"+student.getName()+", CRN " + crn +": "+module.getModuleTitle()+ ", Component '" + componentTitle + "'");
                                        }
                                    }
                                }
                            }
                            if (!componentUpdated) {
                                DataPipeline.log(logWriter, "WARN", file.getName(), "No matching module/component found for Student " + bannerID + ", CRN " + crn + ", Component '" + componentTitle + "'");
                            }
                        } else {
                            DataPipeline.log(logWriter, "WARN", file.getName(), "Student with Banner ID " + bannerID + " not found in the provided qlikview student list.");
                        }
                    } catch (NumberFormatException nfe) {
                        DataPipeline.log(logWriter, "ERROR", file.getName(), "Could not parse Banner ID from cleaned string: '" + cleaned + "' (raw: '" + rawID + "'). Line: " + line);
                    }
                }
            } catch (IOException e) {
                DataPipeline.log(logWriter, "ERROR", file.getName(), "IOException while reading file: " + e.getMessage());
            } catch (Exception e) {
                DataPipeline.log(logWriter, "ERROR", file.getName(), "Unexpected error processing line: " + e.getMessage());
            }
        }

        DataPipeline.log(logWriter, "INFO", "IYR_Update_Summary", "--- IYR Banner ID Check ---");

        
        if (IYR_bannerIDs.isEmpty()) {
            DataPipeline.log(logWriter, "INFO", "IYR_Update_Summary", "No Banner IDs found in IYR files.");
        } else {
            Set<Integer> uniqueIYRIds = new HashSet<>(IYR_bannerIDs);
            DataPipeline.log(logWriter, "INFO", "IYR_Update_Summary", "Total unique Banner IDs processed: " + uniqueIYRIds.size());
            for (Integer id : uniqueIYRIds) {
            Student iyrStudent = DataPipeline.findStudentById(students, id);
            if (iyrStudent != null) {
                DataPipeline.log(logWriter, "INFO", "IYR_Update_Summary", "IYR Banner ID " + id + " matched student in list.");
            } else {
                DataPipeline.log(logWriter, "WARN", "IYR_Update_Summary", "IYR Banner ID " + id + " not found in student list.");
            }
            }
        }
        DataPipeline.log(logWriter, "INFO", "IYR_Update_Summary", "--- End IYR Banner ID Check ---");


    } catch (IOException e) {
        System.err.println("FATAL: Could not write to log file: " + logFile.getPath() + " - " + e.getMessage());
        e.printStackTrace();
    }
    saveIYRStudentsToFile(IYRStudents,logFolderPath, "IYR_StudentsList.csv");

    return students;
}
public static void saveIYRStudentsToFile(List<Student> IYRStudens, String logFolderPath,String filePath) {
    File IYRFile = new File(logFolderPath, filePath);
    if (!IYRStudens.isEmpty()) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(IYRFile))) {
            writer.println("Student Name, Banner ID,CRN,Component Title"); // Header
            for (Student student : IYRStudens) {
                for (Module module : student.getModules()) {
                    for (Component component : module.getComponents()) {
                        if (component.isComponentIYR()) {
                            writer.println(student.toString() + "," + module.getModuleCRN() +","+module.getModuleTitle()+ "," + component.getComponentTitle());
                        }
                    }
                }
            }
            DataPipeline.log(writer, "INFO", "IYR_Student_Save", "IYR students saved to " + IYRFile.getAbsolutePath());
        } catch (IOException e) {
            DataPipeline.log(null, "ERROR", "IYR_Student_Save", "Error writing to file: " + e.getMessage());
        }
    } else {
        DataPipeline.log(null, "INFO", "IYR_Student_Save", "No students with IYR components found.");
    }
    } 
 
    /**
     * Get a list of students who have at least one IYR component.
     */
    public static List<Student> getStudentsWithIYR(List<Student> students) {
        List<Student> result = new ArrayList<>();
        for (Student student : students) {
            for (Module module : student.getModules()) {
                for (Component component : module.getComponents()) {
                    if (component.isComponentIYR()) {
                        result.add(student);
                        break;
                    }
                }
                if (result.contains(student)) break;
            }
        }
        return result;
    } 

    
}
