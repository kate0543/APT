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
    private Integer componentScore=0;     // Converted Score of the component

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
        // Initialize defaults
        this.componentScore = 0;
        this.componentPassed = false;
        this.componentStatus = "Running"; // Default status
        this.componentRAP = false;
        this.componentIYR = false;
        // componentPMC is not determined here, assuming set elsewhere if needed

        if (componentRecord == null || componentRecord.trim().isEmpty()) {
            // No record information or empty string, keep defaults and return
            return;
        }

        // 1. Extract flags from the record
        boolean isNS = componentRecord.contains("NS");
        boolean isRAP = componentRecord.contains("R"); // Assuming "R" consistently means RAP
        boolean isIYR = componentRecord.contains("IYR");
        boolean isResit = componentRecord.contains("**");

        // Update boolean component flags based on presence
        this.componentRAP = isRAP;
        this.componentIYR = isIYR;

        // 2. Try parsing the score
        boolean scoreParsedSuccessfully = false;
        try {
            String digits = componentRecord.replaceAll("[^\\d]", "");
            if (!digits.isEmpty()) {
                int score = Integer.parseInt(digits);
                this.componentScore = score;
                // Use >= 40 for passing, consistent with typical marking schemes
                this.componentPassed = score >= 40;
                scoreParsedSuccessfully = true;
            }
        } catch (NumberFormatException e) {
            // Score parsing failed (e.g., record contains only flags like "NS").
            // Score remains 0, passed remains false.
            // Optionally log this error: System.err.println("Could not parse score from record: " + componentRecord);
        }

        // 3. Determine primary Status based on score and flags (prioritizing flags if score is 0 or parsing failed)
        if (scoreParsedSuccessfully && this.componentScore > 0) {
            // Score parsed successfully and is positive
            this.componentStatus = "Submitted";
        } else {
            // Score is 0, or parsing failed. Check flags.
            if (isNS) {
                this.componentStatus = "NS"; // Not Submitted takes precedence
            } else if (isRAP) {
                this.componentStatus = "R"; // RAP
            } else if (isIYR) {
                this.componentStatus = "IYR"; // In-Year Retrieval
            } else if (scoreParsedSuccessfully && this.componentScore == 0) {
                // Score was parsed as 0, and no NS/R/IYR flags found.
                // Treat as submitted with a score of 0.
                this.componentStatus = "Submitted";
            } else {
                // No specific flags, score parsing failed (or was empty digits).
                // Keep the default status "Running".
                this.componentStatus = "Running";
            }
        }

        // 4. Handle Resit flag (Append or replace if necessary)
        if (isResit) {
            // Avoid appending if "Resit" is already part of the status string
            if (!this.componentStatus.contains("Resit")) {
                if (this.componentStatus.equals("Running")) {
                    // If the status is currently just the default "Running", replace it entirely.
                    this.componentStatus = "Resit";
                } else {
                    // Append ",Resit" to other determined statuses (e.g., "Submitted,Resit", "NS,Resit").
                    this.componentStatus += ",Resit";
                }
            }
        }
        // Note: The original debug print statement has been removed as part of optimization.
        // If needed for debugging, uncomment the line below:
        // System.out.println("Component Info Updated: " + this.toString());
    }

    /**
     * Checks if the component has failed.
     * @return true if failed, false otherwise
     */
    public boolean hasFailed() {
        if (this.componentRecord != null && this.componentRecord.contains("MM")) {
            return true; // Running component, not submitted, failed
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