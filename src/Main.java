package src;

import java.util.*;
import java.io.*;

public class Main {
    public static void main(String[] args) {
        String rootDataFolder = "data/BMT/";

        SourceDoc sourceDoc = new SourceDoc();
        EBR ebr = new EBR();
        Map<String, Student> students = new HashMap<>();

        try {
            // Step 1: Read Qlikview data for both BMT.F and BMT.S
            Map<String, String> qlikviewProgrammeData = Qlikview.readQlikviewData(rootDataFolder + "Qlikview"); 
            System.err.println("Qlikview Programme Data: " + qlikviewProgrammeData);

            // Step 2: Create a list of students from Qlikview Programme by registration
            for (Map.Entry<String, String> entry : qlikviewProgrammeData.entrySet()) {
                String registration = entry.getKey();
                String programmeInfo = entry.getValue();
                students.put(registration, new Student(registration, programmeInfo));
            }

            // Step 3: Add modules to students from Qlikview Module by registration
            for (Map.Entry<String, String> entry : qlikviewModuleData.entrySet()) {
                String registration = entry.getKey();
                String moduleInfo = entry.getValue();
                if (students.containsKey(registration)) {
                    Module module = new Module(moduleInfo);
                    students.get(registration).addModule(module);
                }
            }

            // Step 4: Read EBR data and update components for each student's modules
            Map<String, List<Component>> ebrData = ebr.readEBRData(rootDataFolder + "EBR");
            for (Map.Entry<String, List<Component>> entry : ebrData.entrySet()) {
                String registration = entry.getKey();
                if (students.containsKey(registration)) {
                    Student student = students.get(registration);
                    for (Component component : entry.getValue()) {
                        student.updateComponent(component);
                    }
                }
            }

            // Step 5: Update components' deadlines from SourceDoc
            Map<String, Module> modules = sourceDoc.readSourceData(rootDataFolder + "Source/SBS Source 24-25");
            for (Student student : students.values()) {
                for (Module module : student.getModules()) {
                    if (modules.containsKey(module.getModuleCode())) {
                        module.updateDeadlines(modules.get(module.getModuleCode()).getDeadlines());
                    }
                }
            }

            // Step 6: Calculate attendance from StEP
            Map<String, Integer> attendanceData = StEP.readAttendanceData(rootDataFolder + "StEP");
            for (Student student : students.values()) {
                student.calculateAttendance(attendanceData);
            }

            // Print the final list of students with updated information
            System.out.println("\nFinal List of Students:");
            for (Student student : students.values()) {
                System.out.println(student);
            }

        } catch (IOException e) {
            System.err.println("General I/O error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
        }
    }
}
