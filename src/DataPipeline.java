package src;

import java.util.List;
import java.util.ArrayList;

public class DataPipeline {
    static List<Student> priorityStudents = new ArrayList<>(); // List to hold student objects

    public static void main(String[] args) {
        String baseFolderPath = "data/BMT/";
        List<String> targetProgrammeCodesList = List.of("BMT.S", "BMT.F"); // Target programme codes

        // Fetch students
        List<Student> students = new ArrayList<>();
        try {
            students = StEP.fetchStudents(students, baseFolderPath, targetProgrammeCodesList);
        } catch (java.io.IOException e) {
            System.err.println("Error fetching students: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        DataPipeline pipeline = new DataPipeline();

        System.out.println("=== Priority Students ===");
        priorityStudents = pipeline.fetchPriorityStudents(students);
        for (Student student : priorityStudents) {
            System.out.println("Student ID: " + student.getBannerID() + ", Priority Reason: " + student.getPriorityReasons());
        }
        System.out.println("Total Priority Students: " + priorityStudents.size());

        System.out.println("\n=== Priority Students with Overdue Components ===");
        List<Student> overduePriority = pipeline.priorityUpdateComponent(students);
        System.out.println("Overdue Priority Students: " + overduePriority.size());
        for (Student s : overduePriority) {
            // System.out.println(s.toString());
        }
    }

    /**
     * Calculates priority reasons for a student.
     * @param student The student to evaluate.
     * @return List of reasons for priority.
     */
    public static List<String> calculatePriorityGroup(Student student) {
        List<String> reasons = new ArrayList<>();

        // Example: Uncomment and adjust as needed for your logic

        // Priority if last term attendance rate is less than 30%
        // if (student.getStudentLastTermAttendanceRate() < 30) {
        //     reasons.add("Attendance is low: " + student.getStudentLastTermAttendanceRate() + "%");
        // }

        // Priority if registration status is "new"
        // if (student.getProgrammeRegStatus() != null && student.getProgrammeRegStatus().equalsIgnoreCase("new")) {
        //     reasons.add("New registration status");
        // }

        // Priority if student has trailing modules
        // if (student.isTrailing()) {
        //     reasons.add("Has trailing modules");
        // }

        // Priority if student has failed components
        if (student.getFailedComponents().size() != 0) {
            for (Module module : student.getModules()) {
                for (Component component : module.getComponents()) {
                    if (!component.hasFailed()) {
                        reasons.add("Has failed components: " + component.getComponentTitle() + " in module: " + module.getModuleTitle());
                    }
                }
            }
            reasons.add("Has failed components");
        }

        // Priority if programme registration status is not "RE"
        // if (student.getProgrammeRegStatus() != null && !student.getProgrammeRegStatus().equalsIgnoreCase("RE")) {
        //     reasons.add("Programme registration status not RE");
        // }

        // Priority if any module enrollment is not "RE"
        // for (Module module : student.getModules()) {
        //     if (module.getModuleEnrollment() != null && !module.getModuleEnrollment().equalsIgnoreCase("RE")) {
        //         if (!reasons.contains("Module enrollment not RE")) {
        //             reasons.add("Module enrollment not RE");
        //         }
        //     }
        // }

        return reasons;
    }

    /**
     * Finds students with overdue components.
     * @param students List of students.
     * @return List of students with overdue components.
     */
    public List<Student> priorityUpdateComponent(List<Student> students) {
        List<Student> priorityStudents = new ArrayList<>();
        java.time.LocalDate currentDate = java.time.LocalDate.now();

        for (Student student : students) {
            boolean addToPriority = false;
            StringBuilder priorityReason = new StringBuilder();

            for (Module module : student.getModules()) {
                for (Component component : module.getComponents()) {
                    java.time.LocalDate deadline = null;
                    String deadlineStr = component.getComponentDeadline();
                    if (deadlineStr != null && !deadlineStr.isEmpty()) {
                        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
                        deadline = java.time.LocalDate.parse(deadlineStr, formatter);
                    }
                    String status = component.getComponentStatus();
                    if (deadline != null && deadline.isBefore(currentDate) && (status == null || !status.contains("Submitted"))) {
                        addToPriority = true;
                        priorityReason.append("Overdue component: ");
                        // .append(component.getComponentTitle())
                        // .append(" in module: ")
                        // .append(module.getModuleTitle())
                        // .append("; ");
                        break;
                    }
                }
                if (addToPriority) break;
            }

            if (addToPriority) {
                if (!priorityStudents.contains(student)) {
                    // Assuming Student class has updateReason(String) method
                    student.updateReason(priorityReason.toString());
                    priorityStudents.add(student);
                } else {
                    // Update the priority reason if already in the list
                    int idx = priorityStudents.indexOf(student);
                    Student existing = priorityStudents.get(idx);
                    existing.updateReason(priorityReason.toString());
                }
            }
        }

        return priorityStudents;
    }

    /**
     * Finds students with any priority reason.
     * @param students List of students.
     * @return List of students with priority reasons.
     */
    public List<Student> fetchPriorityStudents(List<Student> students) {
        List<Student> priorityStudents = new ArrayList<>();
        for (Student student : students) {
            List<String> priorityReasonsList = calculatePriorityGroup(student);
            String priorityReason = String.join("; ", priorityReasonsList);
            if (!priorityReason.isEmpty()) {
                if (!priorityStudents.contains(student)) {
                    student.updateReason(priorityReason);
                    priorityStudents.add(student);
                }
            }
        }
        return priorityStudents;
    }

    /**
     * Finds a student by Banner ID.
     * @param students List of students.
     * @param bannerID Banner ID to search for.
     * @return Student with the given Banner ID, or null if not found.
     */
    public static Student findStudentById(List<Student> students, int bannerID) {
        for (Student student : students) {
            // Assuming Student class has a getBannerID() method returning int
            if (student.getBannerID() == bannerID) {
                return student;
            }
        }
        return null; // Return null if no student with the given ID is found
    }
}