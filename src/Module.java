package src;

import java.util.ArrayList;
import java.util.List;

public class Module {
    // Fields
    private String programmeCode; // Programme code associated with the module (optional)
    private List<Component> components=new ArrayList<>(); // List of components (e.g., lectures, labs) associated with the module

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
    private String moduleMark; // Overall mark for the module (optional)
    private String moduleLeader; // Name of the module leader
    private String moduleAdminTeam; // Name of the module admin team
    private Integer numOfComponents=0; // Flag indicating if component details are loaded
    private boolean componentDetailsLoaded = false; // Flag indicating if component details are loaded for the student


    // Empty constructor
    public Module() {
    }
    // Constructor with parameters
    public Module(String programmeCode, List<Component> components, String moduleCRN, String moduleID,
                  String moduleTitle, int moduleYear, String moduleLevel, String moduleEnrollment,
                  String moduleCredit, String moduleSchool, String moduleTerm, String moduleRegStatus,
                  String moduleMark) {
        this.programmeCode = programmeCode;
        // Initialize components list even if null is passed
        this.components = (components != null) ? new ArrayList<>(components) : new ArrayList<>();
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
        this.moduleMark = moduleMark;
        
        // Initialize optional fields not included in this specific constructor if needed
        // this.moduleLeader = null; // Or some default value
        // this.moduleAdminTeam = null; // Or some default value
    }

    // Getters and Setters

        /**
     * Checks if the component details have been loaded for this student.
     * @return true if component details are loaded, false otherwise.
     */
    public boolean getComponentDetailsLoaded() {
        return componentDetailsLoaded;
    }

    public void setComponentDetailsLoaded(boolean componentDetailsLoaded) {
        this.componentDetailsLoaded = componentDetailsLoaded;
    }
    public Integer getNumOfComponents() {
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
        return components;
    }
    public void addComponent(Component component) {
        this.components.add(component);
    }
    public void setComponents(List<Component> components) {
        this.components = components;
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

    public String getModuleMark() {
        return moduleMark;
    }

    public void setModuleMark(String moduleMark) {
        this.moduleMark = moduleMark;
    }

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

 

    // toString method
    @Override
    public String toString() {
        return "Module{" +
                "programmeCode='" + programmeCode + '\'' +
                ", components amount=" + components.size() +
                ", moduleCRN='" + moduleCRN + '\'' +
                ", moduleID='" + moduleID + '\'' +
                ", moduleTitle='" + moduleTitle + '\'' +
                // "moduleAdminTeam=' "+  moduleAdminTeam + '\'' +  
                // ", moduleLeader='" + moduleLeader + '\'' +
                // ", moduleYear=" + moduleYear +
                ", moduleLevel='" + moduleLevel + '\'' +
                ", moduleEnrollment='" + moduleEnrollment + '\'' +
                // ", moduleCredit='" + moduleCredit + '\'' +
                // ", moduleSchool='" + moduleSchool + '\'' +
                // ", moduleTerm='" + moduleTerm + '\'' +
                // ", moduleRegStatus='" + moduleRegStatus + '\'' +
                ", moduleMark='" + moduleMark + '\'' +
                '}';
    }
}