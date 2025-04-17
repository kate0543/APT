package src;

import java.util.List;
import java.util.ArrayList;

public class Module {
    // Fields
    private String moduleCode; // Unique code for the module
    private String moduleName; // Name of the module
    private int moduleYear; // Year of the module
    private String moduleLevel; // Level of the module (e.g., undergraduate, postgraduate)

    public String getModuleLevel() {
        return moduleLevel;
    }

    public void setModuleLevel(String moduleLevel) {
        this.moduleLevel = moduleLevel;
    }
    private String registrationStatus; // Registration status of the module
    private List<Component> components; // List of components in the module

    // Constructors
    public Module() {
        // No-argument constructor
    }

    public Module(String moduleCode, String moduleName) {
        this.moduleCode = moduleCode;
        this.moduleName = moduleName;
        this.components = new ArrayList<>();
    }

    // Getters and setters
    public String getModuleCode() {
        return moduleCode;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = moduleCode;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public int getModuleYear() {
        return moduleYear;
    }

    public void setModuleYear(int moduleYear) {
        this.moduleYear = moduleYear;
    }

    public String getRegistrationStatus() {
        return registrationStatus;
    }

    public void setRegistrationStatus(String registrationStatus) {
        this.registrationStatus = registrationStatus;
    }

    public List<Component> getComponents() {
        return components;
    }
    public void setComponents(List<Component> components) {
        this.components = components;
    }
    public void addComponent(Component component) {
        this.components.add(component);
    }

    // toString method for debugging and logging
    @Override
    public String toString() {
        return "Module{moduleCode='" + moduleCode + "', moduleName='" + moduleName +
               "', moduleYear=" + moduleYear + ", registrationStatus='" + registrationStatus +
               "', components=" + components + "}";
    }
}