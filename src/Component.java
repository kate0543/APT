package src;

public class Component {
    private String moduleCode;       // Module code to which this component belongs
    private String componentCode;    // Unique code for the component
    private String componentName;    // Name of the component (optional)
    private String componentType;    // Type of the component (e.g., Lecture, Lab, Assignment)
    private String componentResult;  // Result of the component
    private String deadline;         // Deadline for the component
    private String status;           // Status of the component
    private double score;            // Score of the component

    // No-argument constructor
    public Component() {
        // Initialize fields with default values if necessary
    }

    // Constructor with all fields
    public Component(String componentCode, String componentName, String componentType, String deadline, String status, double score) {
        this.componentCode = componentCode;
        this.componentName = componentName;
        this.componentType = componentType;
        this.deadline = deadline;
        this.status = status;
        this.score = score;
    }

    // Getters and setters for moduleCode
    public String getModuleCode() {
        return moduleCode;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = moduleCode;
    }

    // Getters and setters for componentCode
    public String getComponentCode() {
        return componentCode;
    }

    public void setComponentCode(String componentCode) {
        this.componentCode = componentCode;
    }

    // Getters and setters for componentName
    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    // Getters and setters for componentType
    public String getComponentType() {
        return componentType;
    }

    public void setComponentType(String componentType) {
        this.componentType = componentType;
    }

    // Getters and setters for componentResult
    public String getComponentResult() {
        return componentResult;
    }

    public void setComponentResult(String componentResult) {
        this.componentResult = componentResult;
    }

    // Getters and setters for deadline
    public String getDeadline() {
        return deadline;
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }

    // Getters and setters for status
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Getters and setters for score
    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    // Override toString for better readability
    @Override
    public String toString() {
        return "Component{" +
               "moduleCode='" + moduleCode + '\'' +
               ", componentCode='" + componentCode + '\'' +
               ", componentName='" + componentName + '\'' +
               ", componentType='" + componentType + '\'' +
               ", componentResult='" + componentResult + '\'' +
               ", deadline='" + deadline + '\'' +
               ", status='" + status + '\'' +
               ", score=" + score +
               '}';
    }
}