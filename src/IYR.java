package src;

import java.util.*;
import java.io.File;
import java.io.IOException;

public class IYR {
    public static List<Integer> IYR_bannerIDs = new ArrayList<>(); // Assuming this is a field in the class

    public static void main(String[] args) throws IOException {
        String baseFolderPath = "data/BMT/";
        List<String> targetProgrammeCodesList = List.of("BMT.S", "BMT.F");
        List<Student> students = new ArrayList<>();
        students = SourceDoc.fetchStudents(students, baseFolderPath, targetProgrammeCodesList);

        List<File> dataFiles = locateIYRFiles(baseFolderPath);

        // Removed unused or incorrect IYR_RawIDs assignment

        students = updateIYRComponents(students, dataFiles);
        for (Student student : students) {
            if(student.getBannerID() == 617352){
                System.out.println("Student ID: " + student.getBannerID() );
                for (Module module : student.getModules()) {
                    System.out.println("Module CRN: " + module.getModuleCRN() );
                    for (Component component : module.getComponents()) {
                        System.out.println("Component Title: " + component.getComponentTitle() );
                        System.out.println("Component IYR: " + component.isComponentIYR() );
                    }
                }
            }
            // else{
            //     System.out.println("Student ID: " +"617352" +" not found.");
            // }
        }
        for (Integer id : IYR_bannerIDs) {
            System.out.println("IYR Banner ID: " + id);
            Student iyrStudent=DataPipeline.findStudentById(students, id);
            if (iyrStudent != null) {
                System.out.println("Found student with ID: " + iyrStudent.getBannerID());
                System.err.println(iyrStudent.toString());

            } else {
                System.out.println("Student with ID " + id + " not found in the list.");
            }
        }
        List<Student> studentsWithIYR = getStudentsWithIYR(students);
        if (studentsWithIYR.isEmpty()) {
            System.out.println("No students with IYR components found.");
        } else {
            System.out.println("Students with IYR components:");
            for (Student student : studentsWithIYR) {
                for (Module module : student.getModules()) {
                    for (Component component : module.getComponents()) {
                        if (component.isComponentIYR()) {
                            System.out.println("Student ID: " + student.getBannerID() + ", Module CRN: " + module.getModuleCRN() + ", Component Title: " + component.getComponentTitle());
                        }
                    }
                }
                System.out.println(student.toString());
            }
        }

        
    }

    public static List<File> locateIYRFiles(String baseFolderPath) {
        File folder = new File(baseFolderPath);
        File[] files = folder.listFiles((dir, name) -> name.contains("IYR") && name.endsWith("csv"));
        if (files != null) {
            return Arrays.asList(files);
        } else {
            return new ArrayList<>();
        }
    }
    public static List<Student> updateIYRComponents(List<Student> students, List<File> files) {
        for (File file : files) {
            try (Scanner scanner = new Scanner(file, "UTF-8")) {
                if (scanner.hasNextLine()) scanner.nextLine(); // skip header
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    String[] fields = line.split("\\t");
                    if (fields.length < 8) continue; // adjust if needed
                    String rawID = fields[0];

                    String crn = fields[4];
                    String componentTitle = fields[6];
                    // Remove '@' and leading zeros
                    String cleaned = rawID.replace("@", "").replaceFirst("^0+(?!$)", "");
                    int bannerID = Integer.parseInt(cleaned);
                    IYR_bannerIDs.add( bannerID); // Assuming this is a field in the class
                    Student student = DataPipeline.findStudentById(students, bannerID);
                    if (student != null) {
                        for (Module module : student.getModules()) {
                            if (module.getModuleCRN().equals(crn)) {
                                for (Component component : module.getComponents()) {
                                    if (component.getComponentTitle().equals(componentTitle)) {
                                        component.setComponentIYR(true);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return students;
    }

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
