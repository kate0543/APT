package src;

import java.io.*;
import java.util.*;

// TODO: Record which module is trailing

public class Qlikview {
    public static void main(String[] args) {
        String baseFolderPath = "data/BMT/"; // Replace with the actual base folder path
        List<String> targetProgrammeCodesList = List.of("BMT.S", "BMT.F"); // Replace with the actual target programme codes
        List<Student> students = new ArrayList<>(); // List to store all students

        try {
            students = fetchStudents(baseFolderPath, targetProgrammeCodesList);

            if (!students.isEmpty()) {
                for (Student student : students) {
                    // Uncomment to print students with trailing modules
                    // if (!student.getTrailingModules().isEmpty()) {
                    //     System.out.println("Student: " + student.getBannerID() + " has " + student.getTrailingModules().size() + " trailing modules: " + student.getTrailingModules());
                    // }
                }
            } else {
                System.out.println("No students found for the specified programme codes.");
            }
        } catch (IOException e) {
            System.err.println("An error occurred while reading Qlikview data: " + e.getMessage());
            e.printStackTrace();
        }

        // Uncomment to print all students' details
        // for (Student student : students) {
        //     System.out.println(student);
        // }

        for (Student student : students) {
            if (student.getBannerID() == 654875) { // Compare int using ==
                System.out.println("Student ID: " + student);
                for (Module module : student.getModules()) {
                    // Uncomment to filter by module CRN
                    // if (module.getModuleCRN().equals("59131")) {
                    System.out.println("Module: " + module.getModuleTitle());
                    // }
                }
            }
        }
    }

    public static List<Student> fetchStudents(String baseFolderPath, List<String> targetProgrammeCodesList) throws IOException {
        List<Student> students = new ArrayList<>();

        for (String targetProgrammeCode : targetProgrammeCodesList) {
            try {
                students.addAll(locateQlikviewFiles(baseFolderPath + "/Qlikview/", targetProgrammeCode));
                System.out.println("Found " + students.size() + " students for programme code: " + targetProgrammeCode);
            } catch (IOException e) {
                System.err.println("An error occurred while reading Qlikview data: " + e.getMessage());
                e.printStackTrace();
            }
        }

        if (!students.isEmpty()) {
            for (Student student : students) {
                student.checkTrailingModules(); // Calculate trailing status for each student
            }
        } else {
            System.out.println("No students found for the specified programme codes.");
        }

        System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("            Qlikview Data processing completed.");
        System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------------------------");

        return students;
    }

    public static List<Student> locateQlikviewFiles(String folderPath, String targetProgrammeCode) throws IOException {
        List<Student> students = new ArrayList<>();

        File folder = new File(folderPath);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".csv") && name.contains(targetProgrammeCode));

        if (files != null && files.length > 0) {
            for (File file : files) {
                if (file.getName().contains(targetProgrammeCode) && file.getName().contains("Programme")) {
                    students.addAll(readProgrammeData(file)); // Read programme data from the file
                }
            }
            for (File file : files) {
                if (file.getName().contains("Module")) {
                    addModulesToStudents(file, students); // Add module data to students
                }
            }
        } else {
            System.err.println("No matching CSV files found in folder: " + folder.getAbsolutePath());
        }

        return students;
    }

    public static List<Student> readProgrammeData(File file) throws IOException {
        List<Student> students = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String headerLine = reader.readLine(); // Read the header line
            if (headerLine == null) {
                return students; // Return empty list if file is empty
            }

            String[] headers = headerLine.split(",");
            Map<String, Integer> columnIndexMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                columnIndexMap.put(headers[i].trim(), i);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");

                int bannerID = columnIndexMap.containsKey("Banner ID") && !fields[columnIndexMap.get("Banner ID")].trim().isEmpty()
                        ? Integer.parseInt(fields[columnIndexMap.get("Banner ID")].trim().replace("@", ""))
                        : 0;

                String firstName = columnIndexMap.containsKey("First Name") && !fields[columnIndexMap.get("First Name")].trim().isEmpty()
                        ? fields[columnIndexMap.get("First Name")].trim()
                        : null;

                String lastName = columnIndexMap.containsKey("Last Name") && !fields[columnIndexMap.get("Last Name")].trim().isEmpty()
                        ? fields[columnIndexMap.get("Last Name")].trim()
                        : null;

                String name = (firstName != null && lastName != null) ? firstName + " " + lastName : null;

                String programmeCode = columnIndexMap.containsKey("Programme Code") && !fields[columnIndexMap.get("Programme Code")].trim().isEmpty()
                        ? fields[columnIndexMap.get("Programme Code")].trim()
                        : null;

                int programmeYear = columnIndexMap.containsKey("Programme Year") && !fields[columnIndexMap.get("Programme Year")].trim().isEmpty()
                        ? Integer.parseInt(fields[columnIndexMap.get("Programme Year")].trim())
                        : 0;

                String programmeRegStatusCode = columnIndexMap.containsKey("Reg Status Code") && !fields[columnIndexMap.get("Reg Status Code")].trim().isEmpty()
                        ? fields[columnIndexMap.get("Reg Status Code")].trim()
                        : null;

                String programmeRegStatus = columnIndexMap.containsKey("Reg Status") && !fields[columnIndexMap.get("Reg Status")].trim().isEmpty()
                        ? fields[columnIndexMap.get("Reg Status")].trim()
                        : null;

                String studentType = columnIndexMap.containsKey("Student Type Summary") && !fields[columnIndexMap.get("Student Type Summary")].trim().isEmpty()
                        ? fields[columnIndexMap.get("Student Type Summary")].trim()
                        : null;

                String studentResidency = columnIndexMap.containsKey("Residency") && !fields[columnIndexMap.get("Residency")].trim().isEmpty()
                        ? fields[columnIndexMap.get("Residency")].trim()
                        : null;

                Student student = new Student(
                        programmeYear,
                        programmeCode,
                        programmeRegStatusCode,
                        programmeRegStatus,
                        null, // Modules are not provided in the Programme CSV
                        bannerID,
                        0, // Network ID is not provided in the CSV
                        name,
                        studentType,
                        false, // Is Trailing is not provided in the CSV
                        studentResidency
                );

                students.add(student);
            }
        }

        return students;
    }

    public static void addModulesToStudents(File file, List<Student> students) throws IOException {
        if (students.isEmpty()) {
            System.err.println("No students found to add modules to.");
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String headerLine = reader.readLine(); // Read the header line
            if (headerLine == null) {
                return; // Exit if file is empty
            }

            String[] headers = headerLine.split(",");
            Map<String, Integer> columnIndexMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                columnIndexMap.put(headers[i].trim(), i);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                int bannerID = columnIndexMap.containsKey("Banner ID") && !fields[columnIndexMap.get("Banner ID")].trim().isEmpty()
                        ? Integer.parseInt(fields[columnIndexMap.get("Banner ID")].trim().replace("@", ""))
                        : 0;

                if (bannerID == 0) {
                    System.err.println("Invalid Banner ID: " + bannerID);
                    System.err.println("___________________________________________________________________________________________________");
                    continue; // Skip if Banner ID is invalid
                }

                Module module = new Module(
                        columnIndexMap.containsKey("Programme Code") && !fields[columnIndexMap.get("Programme Code")].trim().isEmpty()
                                ? fields[columnIndexMap.get("Programme Code")].trim()
                                : null,
                        null, // Components are not provided in the CSV
                        columnIndexMap.containsKey("CRN") && !fields[columnIndexMap.get("CRN")].trim().isEmpty()
                                ? fields[columnIndexMap.get("CRN")].trim()
                                : null,
                        columnIndexMap.containsKey("Module ID") && !fields[columnIndexMap.get("Module ID")].trim().isEmpty()
                                ? fields[columnIndexMap.get("Module ID")].trim()
                                : null,
                        columnIndexMap.containsKey("Module Title") && !fields[columnIndexMap.get("Module Title")].trim().isEmpty()
                                ? fields[columnIndexMap.get("Module Title")].trim()
                                : null,
                        columnIndexMap.containsKey("Year") && !fields[columnIndexMap.get("Year")].trim().isEmpty()
                                ? Integer.parseInt(fields[columnIndexMap.get("Year")].trim())
                                : 0,
                        columnIndexMap.containsKey("Module Level") && !fields[columnIndexMap.get("Module Level")].trim().isEmpty()
                                ? fields[columnIndexMap.get("Module Level")].trim()
                                : null,
                        columnIndexMap.containsKey("Module Enrolment") && !fields[columnIndexMap.get("Module Enrolment")].trim().isEmpty()
                                ? fields[columnIndexMap.get("Module Enrolment")].trim()
                                : null,
                        columnIndexMap.containsKey("Credits") && !fields[columnIndexMap.get("Credits")].trim().isEmpty()
                                ? fields[columnIndexMap.get("Credits")].trim()
                                : null,
                        columnIndexMap.containsKey("Module School") && !fields[columnIndexMap.get("Module School")].trim().isEmpty()
                                ? fields[columnIndexMap.get("Module School")].trim()
                                : null,
                        columnIndexMap.containsKey("Part of Term") && !fields[columnIndexMap.get("Part of Term")].trim().isEmpty()
                                ? fields[columnIndexMap.get("Part of Term")].trim()
                                : null,
                        columnIndexMap.containsKey("Reg Status") && !fields[columnIndexMap.get("Reg Status")].trim().isEmpty()
                                ? fields[columnIndexMap.get("Reg Status")].trim()
                                : null,
                        null // Module mark is optional and not provided in the CSV
                );

                for (Student student : students) {
                    if (student.getBannerID() == bannerID) {
                        student.addModule(module); // Add the module to the student
                    }
                }
            }
        }
    }
}
