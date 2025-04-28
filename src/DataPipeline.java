package src;

import java.util.List;
import java.util.logging.Logger;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.ArrayList;

public class DataPipeline {
    static Integer threshold = 30; // Example threshold for attendance
    static List<Student> priorityStudents = new ArrayList<>(); // List to hold student objects
    public static void main(String[] args) {
        String baseFolderPath = "data/BMT/";


        String targetProgrammeCode = "BMT"; // Target programme codes
        String logFolderPath = getLogFolderPath(targetProgrammeCode); // Get log folder path
        // Prepare log file
    File logDir = new File(logFolderPath);
    if (!logDir.exists()) {
        logDir.mkdirs();
    }
    File logFile = new File(logDir, "PriorityGroup_fetchstudents.csv");
 
    try (PrintWriter logWriter = new PrintWriter(new FileWriter(logFile, true))) {
            // Write header if file is new
             if (logFile.length() == 0) {
                logWriter.println("Timestamp,Level,File,Message");
                logWriter.flush();
            }

            // Fetch students
            List<Student> students = new ArrayList<>();
            try {
                students = StEP.fetchStudents(students, baseFolderPath, targetProgrammeCode );
            } catch (java.io.IOException e) {
                log(logWriter, "ERROR", "DataPipeline.java", "Error fetching students: " + e.getMessage());
                e.printStackTrace();
                return;
            }

            DataPipeline pipeline = new DataPipeline();

            log(logWriter, "INFO", "DataPipeline.java", "=== Priority Students ===");
            priorityStudents = pipeline.fetchPriorityStudents(students,targetProgrammeCode,"All Reasons");
            for (Student student : priorityStudents) {
                log(logWriter, "INFO", "DataPipeline.java", "Student ID: " + student.getBannerID() + ", Priority Reason: " + student.getPriorityReasons());
            }
            log(logWriter, "INFO", "DataPipeline.java", "Total Priority Students: " + priorityStudents.size());

            log(logWriter, "INFO", "DataPipeline.java", "=== Priority Students with Overdue Components ===");
            List<Student> overduePriority = pipeline.priorityUpdateComponent(students);
            log(logWriter, "INFO", "DataPipeline.java", "Overdue Priority Students: " + overduePriority.size());
            for (Student s : overduePriority) {
                // log(logWriter, "INFO", "DataPipeline.java", s.toString());
            }
        } catch (Exception e) {
            System.err.println("Error initializing log file: " + e.getMessage());
        }
    }

    /**
     * Calculates priority reasons for a student.
     * @param student The student to evaluate.
     * @return List of reasons for priority.
     */
    public static List<String> calculatePriorityGroup(Student student, String logFolderPath, String criteria) {
        List<String> reasons = new ArrayList<>();
        String reason = ""; // Initialize reason string

        switch (criteria) {
            case "Low Attendance":
                if (isLowAttendance(student, threshold)) {
                    reason = "Attendance is low: " + student.getStudentLastTermAttendanceRate() + "%";
                    reasons.add(reason);
                    logPriorityStudent("LowAttendance", logFolderPath, student, reason);
                }
                break;

            case "New Registration":
                if (isNewRegistration(student)) {
                    reason = "New registration status";
                    reasons.add(reason);
                    logPriorityStudent("NewRegistration", logFolderPath, student, reason);
                }
                break;

            case "Trailing Students":
                if (hasTrailingModules(student)) {
                    reason = "Has trailing modules";
                    reasons.add(reason);
                    logPriorityStudent("TrailingModules", logFolderPath, student, reason);
                }
                break;

            case "Fail Students":
                if (hasFailedComponents(student)) {
                    List<String> failedReasons = getFailedComponentReasons(student);
                    // Log each specific failed component reason
                    for (String failedReason : failedReasons) {
                        logPriorityStudent("FailedComponents", logFolderPath, student, failedReason);
                    }
                    // Add a summary reason to the list returned by the method
                    reason = "Has failed components";
                    reasons.add(reason);
                    // Optionally log the summary reason as well, if needed for a separate summary log
                    // logPriorityStudent("FailedComponentsSummary", logFolderPath, student, reason);
                }
                break;

            case "programmeregstatusnotre":
                if (isProgrammeRegStatusNotRE(student)) {
                    reason = "Programme registration status not RE";
                    reasons.add(reason);
                    logPriorityStudent("ProgrammeRegStatusNotRE", logFolderPath, student, reason);
                }
                break;

            case "moduleenrollmentnotre":
                if (hasModuleEnrollmentNotRE(student)) {
                    reason = "Module enrollment not RE";
                    reasons.add(reason);
                    logPriorityStudent("ModuleEnrollmentNotRE", logFolderPath, student, reason);
                }
                break;

            case "All Reasons": // Add a case to check all criteria if needed

                if (isLowAttendance(student, threshold)) {
                    reason = "Attendance is low: " + student.getStudentLastTermAttendanceRate() + "%";
                    reasons.add(reason);
                    logPriorityStudent("LowAttendance", logFolderPath, student, reason);
                }
                if (isNewRegistration(student)) {
                    reason = "New registration status";
                    reasons.add(reason);
                    logPriorityStudent("NewRegistration", logFolderPath, student, reason);
                }
                if (hasTrailingModules(student)) {
                    reason = "Has trailing modules";
                    reasons.add(reason);
                    logPriorityStudent("TrailingModules", logFolderPath, student, reason);
                }
                if (hasFailedComponents(student)) {
                    List<String> failedReasons = getFailedComponentReasons(student);
                     for (String failedReason : failedReasons) {
                        logPriorityStudent("FailedComponents", logFolderPath, student, failedReason);
                    }
                    reason = "Has failed components"; // Summary reason
                    reasons.add(reason);
                }
                if (isProgrammeRegStatusNotRE(student)) {
                    reason = "Programme registration status not RE";
                    reasons.add(reason);
                    logPriorityStudent("ProgrammeRegStatusNotRE", logFolderPath, student, reason);
                }
                if (hasModuleEnrollmentNotRE(student)) {
                    reason = "Module enrollment not RE";
                    reasons.add(reason);
                    logPriorityStudent("ModuleEnrollmentNotRE", logFolderPath, student, reason);
                }
                break;

            default:
                // Handle unknown criteria, maybe log a warning or throw an exception
                System.err.println("Unknown priority criteria: " + criteria);
                break;
        }

        return reasons;
    }

    private static boolean hasModuleEnrollmentNotRE(Student student) {
        for (Module module : student.getModules()) {
            if (module.getModuleEnrollment() != null && !module.getModuleEnrollment().equalsIgnoreCase("RE")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isProgrammeRegStatusNotRE(Student student) {
        return student.getProgrammeRegStatusCode() != null && !student.getProgrammeRegStatusCode().equalsIgnoreCase("RE");
    }

    // Helper function: Check if attendance is low
    private static boolean isLowAttendance(Student student,Integer threshold) {
        return student.getStudentLastTermAttendanceRate() <threshold;
    }

    // Helper function: Check if registration status is "new"
    private static boolean isNewRegistration(Student student) {
        return student.getStudentType() != null && student.getStudentType().equalsIgnoreCase("new");
    }

    // Helper function: Check if student has trailing modules
    private static boolean hasTrailingModules(Student student) {
        return student.isTrailing();
    }

    // Helper function: Check if student has failed components
    private static boolean hasFailedComponents(Student student) {
        return student.getFailedComponents().size() != 0;
    }

    // Helper function: Get failed component reasons
    private static List<String> getFailedComponentReasons(Student student) {
        List<String> failedReasons = new ArrayList<>();
        for (Module module : student.getModules()) {
            for (Component component : module.getComponents()) {
                if (!component.hasFailed()) {
                    failedReasons.add("Has failed components: " + component.getComponentTitle() + " in module: " + module.getModuleTitle());
                }
            }
        }
        return failedReasons;
    }

    // Helper function: Log priority student to CSV file
    private static void logPriorityStudent(String priorityType, String logFolderPath, Student student, String reason) {
         // Create log directory and file
         File logDir = new File(logFolderPath);
         if (!logDir.exists()) {
             logDir.mkdirs();
         }
         String fileName = "priority_student_list_" + priorityType + ".csv";

         File logFile = new File(logDir, fileName);
  
        boolean writeHeader = !logFile.exists();
        try (PrintWriter logWriter = new PrintWriter(new FileWriter(logFile, writeHeader))) {
            if (writeHeader) {
                logWriter.println("StudentName,StudentID,PriorityType,CriteriaDetails");
            }
            String studentName = student.getName().replace(",", " ");
            String studentId = String.valueOf(student.getBannerID());
            String criteria = reason.replace(",", " ");
            logWriter.println(studentName + "," + studentId + "," + priorityType + "," + criteria);
        } catch (Exception e) {
            System.err.println("Error logging priority student: " + e.getMessage());
        }
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
    public static List<Student> fetchPriorityStudents(List<Student> students,String targetProgrammeCode, String criteria) {
             
        List<Student> priorityStudents = new ArrayList<>();
        for (Student student : students) {
            List<String> priorityReasonsList = calculatePriorityGroup(student,getLogFolderPath(targetProgrammeCode),criteria);
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
    /**
     * Cleans a list of strings by removing all double quotes and trimming whitespace from each string.
     * Returns a new list containing the cleaned strings. The original list remains unchanged.
     *
     * @param lines The list of strings to clean.
     * @return A new list with cleaned strings, or an empty list if the input is null or empty.
     */
    public static List<String> cleanLines(List<String> lines) {
        List<String> cleanedList = new ArrayList<>();
        if (lines == null) {
            return cleanedList; // Return an empty list for null input
        }
        for (String line : lines) {
            if (line != null) {
                String cleanedLine = line.replace("\"", "").trim();
                cleanedList.add(cleanedLine);
            } else {
                cleanedList.add(null); // Preserve null entries if necessary, or skip/add empty string
            }
        }
        return cleanedList;
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
    
    public static String getLogFolderPath(String targetProgramme) {
        java.nio.file.Path logFolderPath = Paths.get("result", targetProgramme, "log");
        return logFolderPath.toString().replace("\\", "/");
    }
    
}