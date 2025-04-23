package src;

/**
 * The Component class represents a module component with various attributes such as code, title, type, result, etc.
 */
public class Component {
    private String moduleID;            // Module code to which this component belongs
    private String moduleCRN;           // Course Reference Number (CRN) for the module

    private String componentCode = "";  // Unique code for the component
    private String componentTitle;      // Name of the component (optional)
    private String componentType;       // Type of the component (e.g., Lecture, Lab, Assignment)
    private String componentDeadline;   // Deadline for the component
    private String componentStatus;     // Status of the component, submitted or not
    private boolean componentPassed = false; // Indicates if the component was passed or not (optional)
    private String componentRecord;     // Raw component result with submission status
    private Integer componentScore;     // Converted Score of the component

    private boolean componentPMC;       // Personal Mitigation Plan
    private boolean componentRAP;       // Reasonable Academic Practice
    private boolean componentIYR = false; // In-Year Retrieval

    /**
     * Empty constructor.
     */
    public Component() {
    }

    /**
     * Constructor with moduleCRN, moduleID, componentTitle, and rawRecord.
     */
    public Component(String moduleCRN, String moduleID, String componentTitle, String rawRecord) {
        this.moduleCRN = moduleCRN;
        this.moduleID = moduleID;
        this.componentTitle = componentTitle;
        this.componentRecord = rawRecord;
        this.updateComponentInfo();
    }

    // Getters and Setters

    public String getModuleCode() {
        return moduleID;
    }

    public void setModuleCode(String moduleID) {
        this.moduleID = moduleID;
    }

    public String getModuleCRN() {
        return moduleCRN;
    }

    public void setModuleCRN(String moduleCRN) {
        this.moduleCRN = moduleCRN;
    }

    public String getComponentCode() {
        return componentCode;
    }

    public void setComponentCode(String componentCode) {
        this.componentCode = componentCode;
    }

    public String getComponentTitle() {
        return componentTitle;
    }

    public void setComponentTitle(String componentTitle) {
        this.componentTitle = componentTitle;
    }

    public String getComponentType() {
        return componentType;
    }

    public void setComponentType(String componentType) {
        this.componentType = componentType;
    }

    public String getComponentRecord() {
        return componentRecord;
    }

    public void setComponentRecord(String componentRecord) {
        this.componentRecord = componentRecord;
    }

    public String getComponentDeadline() {
        return componentDeadline;
    }

    public void setComponentDeadline(String componentDeadline) {
        this.componentDeadline = componentDeadline;
    }

    public String getComponentStatus() {
        return componentStatus;
    }

    public void setComponentStatus(String componentStatus) {
        this.componentStatus = componentStatus;
    }

    public double getComponentScore() {
        return componentScore;
    }

    public void setComponentScore(Integer componentScore) {
        this.componentScore = componentScore;
    }

    public boolean isComponentPMC() {
        return componentPMC;
    }

    public void setComponentPMC(boolean componentPMC) {
        this.componentPMC = componentPMC;
    }

    public boolean isComponentRAP() {
        return componentRAP;
    }

    public void setComponentRAP(boolean componentRAP) {
        this.componentRAP = componentRAP;
    }

    public boolean isComponentIYR() {
        return componentIYR;
    }

    public void setComponentIYR(boolean componentIYR) {
        this.componentIYR = componentIYR;
    }

    /**
     * Updates component information based on the raw record.
     */
    public void updateComponentInfo() {
        if (componentRecord != null) {
            try {
                int score = Integer.parseInt(componentRecord.replaceAll("[^\\d]", ""));
                System.out.println("Score: " + score);
                this.componentScore = score;
                this.componentPassed = score > 40;
            } catch (NumberFormatException e) {
                // Not a number, passed remains false
            }
        }

        // Update componentStatus based on componentRecord
        if (componentRecord != null) {
            if (componentRecord.contains("NS")) {
                componentStatus = "NS";
            } else if (componentRecord.contains("MM")) {
                componentStatus = "Running";
            }
            if (componentRecord.contains("**")) {
                if (componentStatus == null || componentStatus.isEmpty()) {
                    componentStatus = "Resit";
                } else if (!componentStatus.contains("Resit")) {
                    componentStatus += ",Resit";
                }
            }
            if (componentPassed) {
                componentStatus = "Submitted";
            }
        }

        System.out.println("Component Info Updated: " + this.toString());
    }

    /**
     * Checks if the component has failed.
     * @return true if failed, false otherwise
     */
    public boolean hasFailed() {
        if (this.componentRecord != null && this.componentRecord.contains("MM")) {
            return false; // Running component, not failed
        }
        if (this.componentScore != null) {
            return this.componentScore < 40;
        }
        return false; // Default to false if score is null
    }

    @Override
    public String toString() {
        return "Component{" +
                ", moduleCRN='" + moduleCRN + '\'' +
                ", componentTitle='" + componentTitle + '\'' +
                (componentDeadline != null && !componentDeadline.isEmpty()
                        ? ", componentDeadline='" + componentDeadline + '\''
                        : ", componentDeadline='not found'") +
                ", componentPassed=" + componentPassed +
                ", componentStatus='" + componentStatus + '\'' +
                ", componentScore=" + componentScore +
                ", componentIYR=" + componentIYR +
                ", componentRecord='" + componentRecord + '\'' +
                '}';
    }
}