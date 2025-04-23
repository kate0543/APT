package src;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a module with its details and associated components.
 */
public class Module {

    // --- Fields ---

    private String programmeCode; // Programme code associated with the module (optional)
    private List<Component> components = new ArrayList<>(); // List of components (e.g., lectures, labs) associated with the module

    private String moduleCRN; // Course Reference Number (CRN) for the module
    private String moduleID; // Unique identifier for the module
    private String moduleTitle; // Title or name of the module
    private int moduleYear; // Academic year of the module
    private String moduleLevel; // Level of the module (e.g., undergraduate, postgraduate)
    private String moduleEnrollment; // Enrollment status (e.g., second attempt capped, replacement first attempt uncapped, etc.)
    private String moduleCredit; // Credit value of the module
    private String moduleSchool; // School or department offering the module
    private String moduleTerm; // Term or session of the module
    private String moduleRegStatus; // Registration status (e.g., RP: retake the year, RE: student progressed)
    private String moduleLeader; // Name of the module leader
    private String moduleAdminTeam; // Name of the module admin team

    private Integer numOfComponents = 0; // Number of components associated with the module
    private boolean componentDetailsLoaded = false; // Flag indicating if component details are loaded for the student

    // Module result related fields
    private String moduleRecord; // Raw record string for the module's result (optional)
    private Integer moduleScore; // Calculated overall score for the module (optional)
    private boolean modulePassed = false; // Indicates if the module was passed (score > 40) (optional)
    private String moduleStatus; // Status derived from moduleRecord (e.g., Submitted, NS, Running, Resit)
    private boolean moduleInfoUpdated = false; // Flag indicating if module result info has been processed

    // --- Constructors ---

    /**
     * Default constructor. Initializes an empty Module object.
     */
    public Module() {
    }

    /**
     * Constructs a Module with specified details.
     * Note: Does not initialize result-related fields like moduleRecord, moduleScore, etc.
     *
     * @param programmeCode    The programme code.
     * @param components       The list of components (can be null, will be initialized as empty list).
     * @param moduleCRN        The module CRN.
     * @param moduleID         The module ID.
     * @param moduleTitle      The module title.
     * @param moduleYear       The academic year.
     * @param moduleLevel      The module level.
     * @param moduleEnrollment The enrollment status.
     * @param moduleCredit     The credit value.
     * @param moduleSchool     The offering school.
     * @param moduleTerm       The term.
     * @param moduleRegStatus  The registration status.
     */

 
    public Module(String programmeCode, List<Component> components, String moduleCRN, String moduleID,
                  String moduleTitle, int moduleYear, String moduleLevel, String moduleEnrollment,
                  String moduleCredit, String moduleSchool, String moduleTerm, String moduleRegStatus) {
        this.programmeCode = programmeCode;
        // Initialize components list safely
        this.components =  new ArrayList<>();
        this.moduleCRN = moduleCRN;
        this.moduleID = moduleID;
        this.moduleTitle = moduleTitle;
        this.moduleYear = moduleYear;
        this.moduleLevel = moduleLevel;
        this.moduleEnrollment = moduleEnrollment;
        this.moduleCredit = moduleCredit;
        this.moduleSchool = moduleSchool;
        this.moduleTerm = moduleTerm;
        this.moduleRegStatus = moduleRegStatus;
        // Note: moduleRecord, moduleLeader, moduleAdminTeam etc. are not set here
    }

    // --- Methods ---

    /**
     * Updates module score, passed status, and overall status based on the moduleRecord field.
     * Sets the moduleInfoUpdated flag to true after processing.
     */
    public void updateModuleInfo() {
        if (moduleRecord != null && !moduleRecord.trim().isEmpty()) {
            // Attempt to parse score
            try {
                // Extract digits, handle potential non-numeric parts gracefully
                String digitsOnly = moduleRecord.replaceAll("[^\\d]", "");
                if (!digitsOnly.isEmpty()) {
                    int score = Integer.parseInt(digitsOnly);
                    this.moduleScore = score;
                    this.modulePassed = score > 40; // Assuming 40 is the pass mark
                    System.out.println("Score Parsed: " + score + " for Module: " + moduleID); // Keep for debugging?
                } else {
                    // Handle cases where moduleRecord has no digits (e.g., "NS", "MM")
                    this.moduleScore = null; // Or 0, depending on desired logic
                    this.modulePassed = false;
                }
            } catch (NumberFormatException e) {
                // Log error or handle cases where parsing fails unexpectedly
                System.err.println("Could not parse score from moduleRecord: '" + moduleRecord + "' for Module: " + moduleID);
                this.moduleScore = null; // Ensure score is null if parsing fails
                this.modulePassed = false;
            }

            // Determine moduleStatus based on moduleRecord content and parsed result
            String calculatedStatus = "";
            if (moduleRecord.contains("NS")) { // Non-Submission
                calculatedStatus = "NS";
            } else if (moduleRecord.contains("MM")) { // Mitigating Circumstances / Mark Missing?
                calculatedStatus = "Running"; // Or potentially "MM"? Clarify meaning
            } else if (this.modulePassed) { // Passed based on score
                 calculatedStatus = "Submitted"; // Or "Passed"?
            } else if (this.moduleScore != null) { // Failed based on score
                 calculatedStatus = "Submitted"; // Still submitted, but failed
            }
            // Check for Resit indicator, potentially appending it
            if (moduleRecord.contains("**")) {
                if (calculatedStatus.isEmpty()) {
                    calculatedStatus = "Resit";
                } else if (!calculatedStatus.contains("Resit")) {
                    calculatedStatus += ",Resit";
                }
            }
            // Default status if none of the above matched but record exists
            if (calculatedStatus.isEmpty() && this.moduleScore == null) {
                 calculatedStatus = "Unknown"; // Or keep null/empty?
            }

            this.moduleStatus = calculatedStatus;

        } else {
            // Handle cases where moduleRecord is null or empty
            this.moduleStatus = "Pending"; // Or null, depending on requirements
            this.moduleScore = null;
            this.modulePassed = false;
        }

        this.moduleInfoUpdated = true;
        System.out.println("Module Info Updated: " + this.toString()); // Keep for debugging?
    }

    /**
     * Adds a component to the module's list of components.
     *
     * @param component The component to add.
     */
    public void addComponent(Component component) {
        if (component != null) {
            this.components.add(component);
            // Optionally update numOfComponents here if it should track the list size
            // this.numOfComponents = this.components.size();
        }
    }

    // --- Getters and Setters ---

    public boolean getModuleInfoUpdated() {
        return moduleInfoUpdated;
    }

    public void setModuleInfoUpdated(boolean moduleInfoUpdated) {
        this.moduleInfoUpdated = moduleInfoUpdated;
    }

    /**
     * Checks if the component details have been loaded for this student.
     *
     * @return true if component details are loaded, false otherwise.
     */
    public boolean getComponentDetailsLoaded() {
        return componentDetailsLoaded;
    }

    public void setComponentDetailsLoaded(boolean componentDetailsLoaded) {
        this.componentDetailsLoaded = componentDetailsLoaded;
    }

    public Integer getNumOfComponents() {
        // Consider returning components.size() directly if numOfComponents isn't strictly needed
        return numOfComponents;
    }

    public void setNumOfComponents(Integer numOfComponents) {
        this.numOfComponents = numOfComponents;
    }

    public String getProgrammeCode() {
        return programmeCode;
    }

    public void setProgrammeCode(String programmeCode) {
        this.programmeCode = programmeCode;
    }

    public List<Component> getComponents() {
        // Return a defensive copy if modification outside the class is a concern
        // return new ArrayList<>(components);
        return components;
    }

    public void setComponents(List<Component> components) {
        this.components = (components != null) ? new ArrayList<>(components) : new ArrayList<>();
        // Optionally update numOfComponents here
        // this.numOfComponents = this.components.size();
    }

    public String getModuleCRN() {
        return moduleCRN;
    }

    public void setModuleCRN(String moduleCRN) {
        this.moduleCRN = moduleCRN;
    }

    public String getModuleID() {
        return moduleID;
    }

    public void setModuleID(String moduleID) {
        this.moduleID = moduleID;
    }

    public String getModuleTitle() {
        return moduleTitle;
    }

    public void setModuleTitle(String moduleTitle) {
        this.moduleTitle = moduleTitle;
    }

    public int getModuleYear() {
        return moduleYear;
    }

    public void setModuleYear(int moduleYear) {
        this.moduleYear = moduleYear;
    }

    public String getModuleLevel() {
        return moduleLevel;
    }

    public void setModuleLevel(String moduleLevel) {
        this.moduleLevel = moduleLevel;
    }

    public String getModuleEnrollment() {
        return moduleEnrollment;
    }

    public void setModuleEnrollment(String moduleEnrollment) {
        this.moduleEnrollment = moduleEnrollment;
    }

    public String getModuleCredit() {
        return moduleCredit;
    }

    public void setModuleCredit(String moduleCredit) {
        this.moduleCredit = moduleCredit;
    }

    public String getModuleSchool() {
        return moduleSchool;
    }

    public void setModuleSchool(String moduleSchool) {
        this.moduleSchool = moduleSchool;
    }

    public String getModuleTerm() {
        return moduleTerm;
    }

    public void setModuleTerm(String moduleTerm) {
        this.moduleTerm = moduleTerm;
    }

    public String getModuleRegStatus() {
        return moduleRegStatus;
    }

    public void setModuleRegStatus(String moduleRegStatus) {
        this.moduleRegStatus = moduleRegStatus;
    }

    public String getModuleRecord() {
        return moduleRecord;
    }

    public void setModuleRecord(String moduleRecord) {
        this.moduleRecord = moduleRecord;
        // Consider triggering updateModuleInfo automatically or resetting flags
        // this.moduleInfoUpdated = false;
        // updateModuleInfo(); // Or let the caller decide when to update
    }

    public Integer getModuleScore() {
        return moduleScore;
    }

    // Typically, score is calculated, so a direct setter might not be desired
    // public void setModuleScore(Integer moduleScore) {
    //     this.moduleScore = moduleScore;
    // }

    public boolean isModulePassed() { // Renamed getter for boolean
        return modulePassed;
    }

    // Setter might not be needed if calculated by updateModuleInfo
    // public void setModulePassed(boolean modulePassed) {
    //     this.modulePassed = modulePassed;
    // }

    public String getModuleStatus() {
        return moduleStatus;
    }

    // Setter might not be needed if calculated by updateModuleInfo
    // public void setModuleStatus(String moduleStatus) {
    //     this.moduleStatus = moduleStatus;
    // }

    public String getModuleLeader() {
        return moduleLeader;
    }

    public void setModuleLeader(String moduleLeader) {
        this.moduleLeader = moduleLeader;
    }

    public String getModuleAdminTeam() {
        return moduleAdminTeam;
    }

    public void setModuleAdminTeam(String moduleAdminTeam) {
        this.moduleAdminTeam = moduleAdminTeam;
    }

    // --- Overridden Methods ---

    @Override
    public String toString() {
        // Provide a concise and informative representation
        return "Module{" +
                "moduleID='" + moduleID + '\'' +
                ", moduleTitle='" + moduleTitle + '\'' +
                ", moduleCRN='" + moduleCRN + '\'' +
                ", moduleLevel='" + moduleLevel + '\'' +
                ", moduleRecord='" + moduleRecord + '\'' +
                ", moduleScore=" + moduleScore +
                ", modulePassed=" + modulePassed +
                ", moduleStatus='" + moduleStatus + '\'' +
                ", components=" + components.size() +
                ", componentDetailsLoaded=" + componentDetailsLoaded +
                ", moduleInfoUpdated=" + moduleInfoUpdated +
                // Add other important fields as needed, avoid overly long strings
                // ", programmeCode='" + programmeCode + '\'' +
                // ", moduleYear=" + moduleYear +
                // ", moduleEnrollment='" + moduleEnrollment + '\'' +
                // ", moduleCredit='" + moduleCredit + '\'' +
                // ", moduleSchool='" + moduleSchool + '\'' +
                // ", moduleTerm='" + moduleTerm + '\'' +
                // ", moduleRegStatus='" + moduleRegStatus + '\'' +
                '}';
    }

    // Consider adding equals() and hashCode() if Modules will be stored in Sets or used as Map keys
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Module module = (Module) o;
        // Compare based on a unique identifier, CRN or ID usually good candidates
        return Objects.equals(moduleCRN, module.moduleCRN) && Objects.equals(moduleID, module.moduleID);
    }

    @Override
    public int hashCode() {
        // Hash based on the same fields used in equals()
        return Objects.hash(moduleCRN, moduleID);
    }
}