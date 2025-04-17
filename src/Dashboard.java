package src;
import java.util.List;
import java.util.ArrayList;

public class Dashboard {
    private String dashboardName;
    private String description;
    private List<Qlikview> qlikviews;

    public Dashboard() {
        this.qlikviews = new ArrayList<>();
    }

    // Getters and setters
    public String getDashboardName() { return dashboardName; }
    public void setDashboardName(String dashboardName) { this.dashboardName = dashboardName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<Qlikview> getQlikviews() { return qlikviews; }
    public void addQlikview(Qlikview qlikview) { this.qlikviews.add(qlikview); }

    @Override
    public String toString() {
        return "Dashboard{dashboardName='" + dashboardName + "', description='" + description +
               "', qlikviews=" + qlikviews + "}";
    }
}