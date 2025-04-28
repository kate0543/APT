package src;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a student enrolled in a programme, including their modules and status.
 */
public class Student {
    // Programme-related fields
    private int programmeYear; // Year of the program the student is enrolled in
    private String programmeCode; // Code identifying the program
    private String programmeRegStatusCode; // Registration status code (e.g., RP = retake, RE = progressed, WN = withdrawn)
    private String programmeRegStatus; // Description of the registration status

    // Module and component lists
    private List<Module> modules = new ArrayList<>(); // List of modules the student is currently registered for
    private List<Module> trailingModules = new ArrayList<>(); // List of modules the student needs to retake (status 'RP')
    private List<Component> failedComponents = new ArrayList<>(); // List of failed components in the modules

    // Student identity and status
    private int bannerID; // Unique student identifier (Banner system)
    private int networkID; // Student network/Blackboard identifier
    private String name; // Full name of the student
    private String studentType; // Type of student (e.g., new, continuing)
    private boolean isTrailing; // Flag indicating if the student has trailing modules overall
    private String studentResidency; // Residency status of the student (e.g., Home, EU, International)

    // Attendance tracking
    private Integer studentLastYearAttendanceRate = 0; // Attendance percentage from the previous academic year
    private Integer studentLastTermAttendanceRate = 0; // Attendance percentage from the previous term
    private Integer totalSessionCountLastTerm = 0;
    private Integer notAttendedSessionCountLastTerm = 0; // Total number of sessions not attended in the last term
    private Integer totalSessionCountLastYear = 0;
    private Integer notAttendedSessionCountLastYear = 0; // Total number of sessions not attended in the last year

    // Priority reasons
    private List<String> priorityReasons = new ArrayList<>(); // List of reasons for priority status

    // Constructors
    public Student() {
    }

    public Student(int programmeYear, String programmeCode, String programmeRegStatusCode, String programmeRegStatus,
                   List<Module> modules, int bannerID, int networkID, String name, String studentType,
                   boolean isTrailing, String studentResidency) {
        this.programmeYear = programmeYear;
        this.programmeCode = programmeCode;
        this.programmeRegStatusCode = programmeRegStatusCode;
        this.programmeRegStatus = programmeRegStatus;
        this.modules = (modules != null) ? new ArrayList<>(modules) : new ArrayList<>();
        this.bannerID = bannerID;
        this.networkID = networkID;
        this.name = name;
        this.studentType = studentType;
        this.isTrailing = isTrailing;
        this.studentResidency = studentResidency;
    }

    // Priority reasons methods
    /**
     * Returns a defensive copy of the list of priority reasons.
     * @return A new list containing the student's priority reasons.
     */
    public List<String> getPriorityReasons() {
        return new ArrayList<>(priorityReasons);
    }

    /**
     * Sets the list of priority reasons for the student.
     * Makes a defensive copy of the provided list.
     * @param priorityReasons The list of priority reasons to set.
     */
    public void setPriorityReasons(List<String> priorityReasons) {
        this.priorityReasons = (priorityReasons != null) ? new ArrayList<>(priorityReasons) : new ArrayList<>();
    }

    /**
     * Adds a reason to the priorityReasons list if it does not already exist.
     * @param reason The reason to add.
     */
    public void updateReason(String reason) {
        if (!this.priorityReasons.contains(reason)) {
            priorityReasons.add(reason);
        }
    }

    // Attendance methods
    public Integer getTotalSessionCountLastTerm() {
        return totalSessionCountLastTerm;
    }

    public void setTotalSessionCountLastTerm(Integer totalSessionCountLastTerm) {
        this.totalSessionCountLastTerm = totalSessionCountLastTerm;
    }

    public void incrementTotalSessionCountLastTerm() {
        this.totalSessionCountLastTerm++;
    }

    public Integer getNotAttendedSessionCountLastTerm() {
        return notAttendedSessionCountLastTerm;
    }

    public void setNotAttendedSessionCountLastTerm(Integer notAttendedSessionCountLastTerm) {
        this.notAttendedSessionCountLastTerm = notAttendedSessionCountLastTerm;
    }

    public void incrementNotAttendedSessionCountLastTerm() {
        this.notAttendedSessionCountLastTerm++;
    }

    public Integer getTotalSessionCountLastYear() {
        return totalSessionCountLastYear;
    }

    public void setTotalSessionCountLastYear(Integer totalSessionCountLastYear) {
        this.totalSessionCountLastYear = totalSessionCountLastYear;
    }

    public void incrementTotalSessionCountLastYear() {
        this.totalSessionCountLastYear++;
    }

    public Integer getNotAttendedSessionCountLastYear() {
        return notAttendedSessionCountLastYear;
    }

    public void setNotAttendedSessionCountLastYear(Integer notAttendedSessionCountLastYear) {
        this.notAttendedSessionCountLastYear = notAttendedSessionCountLastYear;
    }

    public void incrementNotAttendedSessionCountLastYear() {
        this.notAttendedSessionCountLastYear++;
    }

    public Integer getStudentLastTermAttendanceRate() {
        if (studentLastTermAttendanceRate == null) {
            calculateLastTermAttendance(); // Ensure the attendance rate is calculated before returning
        }
        return studentLastTermAttendanceRate;
    }

    /**
     * Calculates the attendance rate for the last term.
     * Sets studentLastTermAttendanceRate as a percentage.
     */
    public void calculateLastTermAttendance() {
        if (totalSessionCountLastTerm == 0) {
            this.studentLastTermAttendanceRate = 0; // Avoid division by zero
        } else {
            int attendedSessionLastTermCount = totalSessionCountLastTerm - notAttendedSessionCountLastTerm;
            studentLastTermAttendanceRate = (int) Math.round(((double) attendedSessionLastTermCount / totalSessionCountLastTerm) * 100.0);
        }
    }

    public void setStudentLastTermAttendanceRate(Integer studentLastTermAttendanceRate) {
        this.studentLastTermAttendanceRate = studentLastTermAttendanceRate;
    }

    public Integer getStudentLastYearAttendanceRate() {
        if (studentLastYearAttendanceRate == null) {
            calculateStudentLastYearAttendanceRate(); // Ensure the attendance rate is calculated before returning
        }
        return studentLastYearAttendanceRate;
    }

    /**
     * Calculates the attendance rate for the last year.
     * Sets studentLastYearAttendanceRate as a percentage.
     */
    public void calculateStudentLastYearAttendanceRate() {
        if (totalSessionCountLastYear == 0) {
            studentLastYearAttendanceRate = 0; // Avoid division by zero
        } else {
            int attendedSessionLastYearCount = totalSessionCountLastYear - notAttendedSessionCountLastYear;
            studentLastYearAttendanceRate = (int) Math.round(((double) attendedSessionLastYearCount / totalSessionCountLastYear) * 100.0);
        }
    }

    public void setStudentLastYearAttendanceRate(Integer studentLastYearAttendanceRate) {
        this.studentLastYearAttendanceRate = studentLastYearAttendanceRate;
    }

    // Trailing and failed modules/components
    /**
     * Identifies modules marked for retake ('RP') and adds them to the trailingModules list.
     * This method iterates through the student's registered modules.
     */
    public void checkTrailingModules() {
        for (Module module : modules) {
            if (this.programmeRegStatusCode != null && this.programmeRegStatusCode.contains("RE")) {
                if (!"RE".equals(module.getModuleEnrollment())) {
                    this.trailingModules.add(module);
                }
            }
        }
        this.isTrailing = !this.trailingModules.isEmpty();
    }

    /**
     * Checks if the student has failed components in their modules and adds them to the failedComponents list.
     */
    public void checkFailedComponents() {
        for (Module module : modules) {
            for (Component component : module.getComponents()) {
                if (component != null && component.hasFailed()) {
                    component.updateComponentInfo();
                    this.failedComponents.add(component);
                }
            }
        }
    }

    // Module and component accessors
    /**
     * Returns a defensive copy of the list of modules the student is registered for.
     * @return A new list containing the student's modules.
     */
    public List<Module> getModules() {
        return new ArrayList<>(modules);
    }

    public void addModule(Module module) {
        if (module != null) {
            this.modules.add(module);
        }
    }

    /**
     * Sets the list of modules for the student.
     * Makes a defensive copy of the provided list.
     * @param modules The list of modules to set.
     */
    public void setModules(List<Module> modules) {
        this.modules = (modules != null) ? new ArrayList<>(modules) : new ArrayList<>();
    }

    /**
     * Returns a defensive copy of the list of trailing modules.
     * @return A new list containing the student's trailing modules.
     */
    public List<Module> getTrailingModules() {
        return new ArrayList<>(trailingModules);
    }

    /**
     * Sets the list of trailing modules for the student.
     * Makes a defensive copy of the provided list.
     * @param trailingModules The list of trailing modules to set.
     */
    public void setTrailingModules(List<Module> trailingModules) {
        this.trailingModules = (trailingModules != null) ? new ArrayList<>(trailingModules) : new ArrayList<>();
    }

    /**
     * Returns the module with the given CRN, or null if not found.
     * @param crn The CRN to search for.
     * @return The matching Module, or null.
     */
    public Module getModuleByCRN(String crn) {
        if (this.modules == null || crn == null || crn.isEmpty()) {
            return null;
        }
        for (Module module : this.modules) {
            if (module != null && module.getModuleCRN() != null && module.getModuleCRN().equals(crn)) {
                return module;
            }
        }
        return null;
    }

    /**
     * Returns a defensive copy of the list of failed components.
     * @return A new list containing the student's failed components.
     */
    public List<Component> getFailedComponents() {
        return new ArrayList<>(failedComponents);
    }

    /**
     * Sets the list of failed components for the student.
     * Makes a defensive copy of the provided list.
     * @param failedComponents The list of failed components to set.
     */
    public void setFailedComponents(List<Component> failedComponents) {
        this.failedComponents = (failedComponents != null) ? new ArrayList<>(failedComponents) : new ArrayList<>();
    }

    // Getters and setters for basic fields
    public int getProgrammeYear() {
        return programmeYear;
    }

    public void setProgrammeYear(int programmeYear) {
        this.programmeYear = programmeYear;
    }

    public String getProgrammeCode() {
        return programmeCode;
    }

    public void setProgrammeCode(String programmeCode) {
        this.programmeCode = programmeCode;
    }

    public String getProgrammeRegStatusCode() {
        return programmeRegStatusCode;
    }

    public void setProgrammeRegStatusCode(String programmeRegStatusCode) {
        this.programmeRegStatusCode = programmeRegStatusCode;
    }

    public String getProgrammeRegStatus() {
        return programmeRegStatus;
    }

    public void setProgrammeRegStatus(String programmeRegStatus) {
        this.programmeRegStatus = programmeRegStatus;
    }

    public int getBannerID() {
        return bannerID;
    }

    public void setBannerID(int bannerID) {
        this.bannerID = bannerID;
    }

    public int getNetworkID() {
        return networkID;
    }

    public void setNetworkID(int networkID) {
        this.networkID = networkID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStudentType() {
        return studentType;
    }

    public void setStudentType(String studentType) {
        this.studentType = studentType;
    }

    public boolean isTrailing() {
        return isTrailing;
    }

    public void setTrailing(boolean trailing) {
        isTrailing = trailing;
    }

    public String getStudentResidency() {
        return studentResidency;
    }

    public void setStudentResidency(String studentResidency) {
        this.studentResidency = studentResidency;
    }

    // toString method
    @Override
    public String toString() {
        int totalComponentCount = 0;
        int failedComponentCount = 0;

        if (this.modules != null) {
            for (Module module : this.modules) {
                if (module != null) {
                    List<Component> components = module.getComponents();
                    if (components != null) {
                        totalComponentCount += components.size();
                    }
                }
            }
        }
        if (this.failedComponents != null) {
            failedComponentCount = this.failedComponents.size();
        }

        return "Student{" +
                "bannerID=" + bannerID +
                ", name='" + name + '\'' +
                ", programmeYear=" + programmeYear +
                ", programmeCode='" + programmeCode + '\'' +
                ", programmeRegStatusCode='" + programmeRegStatusCode + '\'' +
                ", studentType='" + studentType + '\'' +
                ", isTrailing=" + isTrailing +
                ", modules amount=" + (modules != null ? modules.size() : 0) +
                ", components amount=" + totalComponentCount +
                ", failed components=" + failedComponentCount +
                ", trailingModules amount=" + (trailingModules != null ? trailingModules.size() : 0) +
                ", studentLastTermAttendanceRate=" + studentLastTermAttendanceRate + "%" +
                (totalSessionCountLastTerm != null ? ", totalSessionCountLastTerm=" + totalSessionCountLastTerm : "") +
                (notAttendedSessionCountLastTerm != null ? ", notAttendedSessionCountLastTerm=" + notAttendedSessionCountLastTerm : "") +
                '}';
    }
}
