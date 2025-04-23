package src;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class EBR {

    private static String Term = ""; 
    private static String School = "";
    private static String programmeCode = "";
    private static String Level = "";  
    
    public static void main(String[] args) throws IOException {
        String baseFolderPath = "data/BMT/"; // Base folder path for Qlikview data
        List<String> targetProgrammeCodesList = List.of("BMT.S", "BMT.F"); // Target programme codes
        List<Student> students = new ArrayList<>(); // List to store all students

        for (String targetProgrammeCode : targetProgrammeCodesList) {
            try {
                students.addAll(Qlikview.fetchStudents(baseFolderPath, List.of(targetProgrammeCode)));
                System.out.println("Found " + students.size() + " students for programme code: " + targetProgrammeCode);
            } catch (IOException e) {
                System.err.println("An error occurred while reading Qlikview data: " + e.getMessage());
                e.printStackTrace();
            }
        }

      
        students = fetchStudents(students, baseFolderPath, targetProgrammeCodesList);

        for (Student student : students) { 
                    for(Module module : student.getModules()) { 
                        for (Component component : module.getComponents()) {
                    
                            System.out.println("Component: " + component.toString());
                        }
                    
                }
         
    }
}
    public static List<Student> fetchStudents(List<Student> students, String baseFolderPath, List<String> targetProgrammeCodesList) throws IOException{
        for (String targetProgrammeCode : targetProgrammeCodesList) { 
            try {
                List<File> files  = locateEBRFiles(baseFolderPath + "/EBR", targetProgrammeCode);

                students = addComponentsToStudents(files , students);
            } catch (IOException e) {
                System.err.println("An error occurred while processing EBR files: " + e.getMessage());
                e.printStackTrace();
            }
        }
        if (!students.isEmpty()) {
            for (Student student : students) {
                student.checkFailedComponents();// Check for failed modules
            }
        } 
        System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("                                EBR Data processing completed.");   
        System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------------------------");

        return students;
    }

    private static String fileType = "ExamBoardReporter"; 
    private static String fileFormat = "CSV"; 

    public static void setProgrammeCode(String code) {
        programmeCode = code;
    }

    public static List<File> locateEBRFiles(String folderPath, String targetProgrammeCode) throws IOException {

        
        List<File> matchingFiles = new ArrayList<>();
        
        // Use the provided folder path
        File folder = new File(folderPath);

        if (!folder.exists() || !folder.isDirectory()) {
            throw new IOException("Directory not found: " + folderPath);
        }

        // List all files in the directory
        File[] files = folder.listFiles();
        if (files == null || files.length == 0) {
            throw new IOException("No files found in directory: " + folderPath);
        }

        // Filter files containing the target programme code and ending with .csv
        for (File file : files) {
            if (file.isFile()) {
                if(file.getName().contains("ModuleReport")) {
                    String fileName = file.getName();
                    if (fileName.contains(targetProgrammeCode) && fileName.endsWith(".csv")) {
                    } else {
                        continue; // Skip files that do not match the criteria
                    }
                } else {
                    continue; // Skip non-file entries
                }
                matchingFiles.add(file);
            }
        }
                // Sort files by year (descending) extracted from filename like ModuleReport-BMT.F-YY-YY-...
                matchingFiles.sort((f1, f2) -> {
                    int year1 = -1;
                    int year2 = -1;
                    try {
                        String[] parts1 = f1.getName().split("-");
                        // Assuming year format YY is the 3rd part (index 2)
                        if (parts1.length >= 3) {
                            year1 = Integer.parseInt(parts1[2]);
                        }
                    } catch (NumberFormatException e) {
                         System.err.println("Could not parse year from filename: " + f1.getName() + " - " + e.getMessage());
                    }
                     try {
                        String[] parts2 = f2.getName().split("-");
                        // Assuming year format YY is the 3rd part (index 2)
                        if (parts2.length >= 3) {
                            year2 = Integer.parseInt(parts2[2]);
                        }
                    } catch (NumberFormatException e) {
                         System.err.println("Could not parse year from filename: " + f2.getName() + " - " + e.getMessage());
                    }
                    // Sort descending: latest year first. Files with unparseable years go last.
                    return Integer.compare(year2, year1);
                });

        return matchingFiles;
    }

    public static List<Student> addComponentsToStudents(List<File> csvFiles, List<Student> students) {
        for (File file : csvFiles) {
            try {
                List<String> lines = Files.readAllLines(file.toPath());
                if (lines.size() < 6) continue;
                
                // Debug the content to understand the format
                System.out.println("First line content: " + lines.get(0));
                
                // Adapt the parsing approach based on actual file format
                // Try to detect the delimiter (tab or comma)
                String firstLine = lines.get(0);
                String delimiter = firstLine.contains("\t") ? "\t" : ",";
                
                // Parse module information
                String[] moduleLine = firstLine.split(delimiter);
                if (moduleLine.length < 2) {
                    System.err.println("Cannot parse module line, unexpected format: " + firstLine);
                    continue;
                }
                
                String moduleCRN = moduleLine[1].trim(); // Module CRN used as moduleID
                String moduleTitle = moduleLine.length > 3 ? moduleLine[3].trim() : "Unknown Module";
                
                System.out.println("Processing file for module: " + moduleCRN + " - " + moduleTitle);
                
                // Identify component columns and create base components
                int componentStartIndex = 5;
                List<Integer> componentIndices = new ArrayList<>();
                List<Component> baseComponents = new ArrayList<>();
                
                // Make sure we handle the correct delimiter
                String[] compTitleLine = lines.get(2).split(delimiter);
                String[] weightLine = lines.get(3).split(delimiter);
                
                for (int i = componentStartIndex; i < compTitleLine.length; i++) {
                    String title = compTitleLine[i].trim();
                    if (!title.isEmpty()) {
                        componentIndices.add(i);
                        Component comp = new Component();
                        comp.setModuleCRN(moduleCRN);
                        comp.setComponentTitle(title);
                        comp.setComponentCode("C" + (i - componentStartIndex + 1));
                        comp.setComponentType("Assessment");
                        if (i < weightLine.length && !weightLine[i].isEmpty()) {
                            try {
                                String rawWeight = weightLine[i].replace("#", "").trim();
                                comp.setComponentScore(Integer.parseInt(rawWeight));
                            } catch (NumberFormatException e) {
                                comp.setComponentScore(0);
                            }
                        }
                        baseComponents.add(comp);
                    }
                }
                
                // Process each student line
                for (int i = 5; i < lines.size(); i++) {
                    String line = lines.get(i);
                    if (!line.startsWith("@")) continue;
                    
                    String[] tokens = line.split(delimiter);
                    if (tokens.length < 9) {
                        System.out.println("Skipping line with insufficient data: " + line);
                        continue;
                    }
                    
                    String bannerIdStr = tokens[0].trim().substring(1).replaceFirst("^0+(?!$)", "");
                    int bannerId;
                    try {
                        bannerId = Integer.parseInt(bannerIdStr);
                    } catch (NumberFormatException e) {
                        System.err.println("Failed to parse Banner ID: " + tokens[0]);
                        continue;
                    }
                    
                    Student student = DataPipeline.findStudentById(students, bannerId);
                    if (student == null) {
                        System.out.println("Student not found with ID: " + bannerId);
                        continue;
                    }
                    
                    // Find the relevant module in the student's module list
                    Module module = null;
                    for (Module m : student.getModules()) {
                        if (m.getModuleCRN() != null && m.getModuleCRN().equals(moduleCRN)) {
                            module = m;
                            break;
                        }
                    }
                    
                    if (module == null) {
                        System.out.println("Module " + moduleCRN + " not found for student " + bannerId);
                        continue;
                    }
                    
                    if (module.getComponentDetailsLoaded()) {
                        System.out.println("Components already loaded for student " + bannerId + ", module " + moduleCRN);
                        continue;
                    }
                    
                    List<Component> studentComponents = new ArrayList<>();
                    for (int j = 0; j < componentIndices.size() && j < baseComponents.size(); j++) {
                        int colIndex = componentIndices.get(j);
                        if (colIndex >= tokens.length) continue;
                        
                        String raw = tokens[colIndex].trim();
                        raw = raw.replace("*", ""); // Remove asterisks if present
                        
                        if (raw.equalsIgnoreCase("NS") || raw.isEmpty()) continue;
                        
                        // Create a new component and copy properties
                        Component comp = new Component();
                        Component baseComp = baseComponents.get(j);
                        
                        // Copy properties from base component
                        comp.setModuleCRN(baseComp.getModuleCRN());
                        comp.setComponentTitle(baseComp.getComponentTitle());
                        comp.setComponentCode(baseComp.getComponentCode());
                        comp.setComponentType(baseComp.getComponentType());
                        
                        // Set student-specific data
                        comp.setComponentRecord(raw);
                        comp.updateComponentInfo();

                        studentComponents.add(comp);
                        
                        System.out.println("Added component '" + comp.getComponentTitle() + 
                            "' with mark '" + raw + "' for student " + bannerId);
                    }
                    
                    module.setComponents(studentComponents);
                    module.setComponentDetailsLoaded(true);

                    System.out.println("Set " + studentComponents.size() + " components for student " + 
                        bannerId + ", module " + moduleCRN);
                }
                
            } catch (IOException e) {
                System.err.println("Error processing file " + file.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        return students;
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
    
       


}
