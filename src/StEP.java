package src;
public class StEP {
    private String stepName;
    private String description;
    private int stepOrder;

    // Getters and setters
    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getStepOrder() { return stepOrder; }
    public void setStepOrder(int stepOrder) { this.stepOrder = stepOrder; }

    @Override
    public String toString() {
        return "StEP{stepName='" + stepName + "', description='" + description +
               "', stepOrder=" + stepOrder + "}";
    }
}