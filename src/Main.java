/*
 * Main class for processing student, module, and attendance data.
 * (Implementation removed as requested.)
 *
 * TODO List:
 *
 * Data Acquisition & Access:
 *   - [ ] QlikView:
 *     - [ ] Access Student by Programme with Registration Status (Filter: UG, Main Campus, Reg Status != Withdrawn~)
 *     - [ ] Access Component Due Dates By Block
 *     - [ ] Access Student on Module by Registration Status
 *     - [ ] Download current year cohort registration status.
 *     - [ ] Download last 4 academic years EBR data for L4/5/6 (check CRN linkage).
 *   - [ ] Step (Jisc):
 *     - [ ] Clarify data structure with Chris (student-based vs. module-based).
 *     - [ ] Obtain engagement & attendance data.
 *     - [ ] Download last year JISC attendance T1 and T2 files.
 *     - [ ] Download current year T1 JISC file.
 *   - [ ] Blackboard:
 *     - [ ] Obtain admin access.
 *     - [ ] Access submission time data (module-based).
 *   - [ ] Gradebook:
 *     - [ ] Get access for pass/fail checks.
 *   - [ ] Exam Board Reporter (EBR):
 *     - [ ] Request access (AC TRAINING).
 *     - [ ] Download reports based on CRN (from QlikView).
 *     - [ ] Understand report views (Admin/Panel/History).
 *   - [ ] Source Document:
 *     - [ ] Use for component DDL & IYR Date Assessments Tabs.
 *     - [ ] Get LEAF module list for Law cohorts.
 *   - [ ] IYR List:
 *     - [ ] Obtain IYR list for entire SBS.
 *   - [ ] Verify Level 3 data sources.
 *
 * Data Integration & Processing:
 *   - [ ] Integrate Blackboard submission data to be student-based (using network ID).
 *   - [ ] Integrate Jisc data if module-based.
 *   - [ ] Link data across sources (QlikView CRN, Network ID, etc.).
 *   - [ ] Process QlikView data to identify:
 *     - [ ] Student Level
 *     - [ ] Submission dates
 *     - [ ] Last year progression (Resit status: RP/RE)
 *     - [ ] Modules per student
 *   - [ ] Process Jisc data for:
 *     - [ ] Attendance %
 *     - [ ] Engagement %
 *     - [ ] Last year Attendance & Engagement %
 *   - [ ] Process Blackboard data for:
 *     - [ ] Non-submission/late submission flags.
 *   - [ ] Process Gradebook data for pass/fail status.
 *   - [ ] Process EBR data for resit outcomes (** marker).
 *   - [ ] Handle PMC/RAP cases (check data, potentially exclude initially).
 *
 * Priority Group Identification (Initialize yearly, update per trimester/trigger):
 *   - [ ] Identify Level 3 & 4 students (Target: 2 meetings/trimester).
 *   - [ ] Identify students trailing a module (QlikView: additional retake module).
 *   - [ ] Identify students repeating the year with attendance (QlikView: Reg Status=RP).
 *   - [ ] Identify students who engaged in IYR last year (Submission date between DDL & resit + passed). (Note: No IYR for L3 now).
 *   - [ ] Identify students who progressed through resit period (Submission date after IYR + passed).
 *   - [ ] Identify students with < 70% engagement last year/trimester (Jisc).
 *   - [ ] Identify students engaging in IYR in the current year (L4 only, post-DDL).
 *   - [ ] Identify T2 students carrying a fail from T1 (For T2 start only).
 *   - [ ] Update priority group based on PMC status (P, R, RR).
 *
 * Implementation & Verification:
 *   - [ ] Start implementation with Law Programme data.
 *   - [ ] Follow with Business Management (BM) cohort.
 *   - [ ] Verify priority group logic and results with C.
 *   - [ ] Ensure all required data is shared via Teams channel.
 *
 * Future Work:
 *   - [ ] Incorporate PMC/RAP data analysis.
 *
 */
package src;

import javax.swing.*;
import javax.xml.crypto.Data;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static JFrame frame;
    private static JTextArea logArea;
    private static JFileChooser folderChooser;
    private static JTextField programmeField;
    private static JComboBox<String> reasonCombo;
    private static JButton generateBtn;

    private static File rootFolder;
    private static String priorityString="";
     private static String targetProgrammeCode="";
     private static List<String> targetProgrammeCodesList = new ArrayList<>(); // Example list, replace with actual data
 
     // Made static to be accessed from static main method and lambdas
     static List<Student> students = new  ArrayList<>();
     static List<Student> priorityStudents = new  ArrayList<>(); // Example list, replace with actual data
     static String choosenCiteria = ""; // Example criteria, replace with actual data

    public enum PriorityCriteriaList {
        ALL_REASONS("All Reasons"),
        LOW_ATTENDANCE("Low Attendance"),
        NEW_REGISTRATION("New Registration"),
        TRAILING_STUDENTS("Trailing Students"),
        FAIL_STUDENTS("Fail Students"),

        PROGRAMMEREGSTATUSNOTRE("Programme Reg Status Not RE"),
        MODULEENROLLMENTNOTRE("Module Enrollment Not RE");

        private final String displayName;

        PriorityCriteriaList(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {



            return displayName;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("Student Priority Group Tool");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(700, 500);
            frame.setLayout(new BorderLayout());

            JPanel topPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5); // padding between components
            gbc.anchor = GridBagConstraints.WEST;

            // Components
            JButton chooseRootBtn = new JButton("Choose Root Folder");
            programmeField = new JTextField(12);
            programmeField.setToolTipText("Enter Programme Code");

            reasonCombo = new JComboBox<>();
            reasonCombo.addItem("Select Priority Reason...");
            for (PriorityCriteriaList criteria : PriorityCriteriaList.values()) {
                reasonCombo.addItem(criteria.toString());
            }
            // Add ActionListener to update cchoosenCiteria when selection changes
            reasonCombo.addActionListener(e -> {
                Object selectedItem = reasonCombo.getSelectedItem();
                if (selectedItem != null && !selectedItem.toString().equals("Select Priority Reason...")) {
                    choosenCiteria = selectedItem.toString();
                    // Optional: Log the change for debugging
                    // logArea.append("Selected criteria: " + cchoosenCiteria + "\n");
                } else {
                    choosenCiteria = ""; // Reset if default is selected
                }
            });

            JButton loadDataBtn = new JButton("Load Data");
            generateBtn = new JButton("Generate Priority Group");
            generateBtn.setEnabled(true);

            // Row 0: Choose Root Folder Button
            gbc.gridx = 0;
            gbc.gridy = 0;
            topPanel.add(chooseRootBtn, gbc);

            // Row 1: Programme Label + TextField
            gbc.gridx = 0;
            gbc.gridy = 1;
            topPanel.add(new JLabel("Programme:"), gbc);

            gbc.gridx = 1;
            topPanel.add(programmeField, gbc);

            // Row 2: Reason Label + ComboBox
            gbc.gridx = 0;
            gbc.gridy = 2;
            topPanel.add(new JLabel("Reason:"), gbc);

            gbc.gridx = 1;
            topPanel.add(reasonCombo, gbc);

            // Row 3: Load Data and Generate Buttons
            gbc.gridx = 0;
            gbc.gridy = 3;
            topPanel.add(loadDataBtn, gbc);

            gbc.gridx = 1;
            topPanel.add(generateBtn, gbc);

            // Log area
            logArea = new JTextArea();
            logArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(logArea);

            // Frame Layout
            frame.add(topPanel, BorderLayout.NORTH);
            frame.add(scrollPane, BorderLayout.CENTER);

            folderChooser = new JFileChooser();
            folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            // Actions
            chooseRootBtn.addActionListener(e -> {
                int result = folderChooser.showOpenDialog(frame);
                if (result == JFileChooser.APPROVE_OPTION) {
                    rootFolder = folderChooser.getSelectedFile();
                    logArea.append("Root folder set to: " + rootFolder.getAbsolutePath() + "\n");
                    chooseRootBtn.setEnabled(false);
                }
            });

            loadDataBtn.addActionListener(e -> {
                if (rootFolder == null) {
                    JOptionPane.showMessageDialog(frame, "Please choose a root folder first.");
                    return;
                }
                logArea.append("Loading data from: " + rootFolder.getAbsolutePath() + "\n");
                // Example usage: fetch students for the entered programme code
                String programme = programmeField.getText().trim();
                if (programme.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Please enter a programme code.");
                    return;
                } else {
                    targetProgrammeCodesList = List.of(programme + ".S", programme + ".F"); // Example: replace with actual list of programme codes
                    try {
                        // Dummy students list, replace with actual data as needed
                        // Assuming StEP.fetchStudents returns the updated list and handles potential IOExceptions
                        Path baseFolderPath = rootFolder.toPath().resolve(programme);
                        students = StEP.fetchStudents(students, baseFolderPath.toString()+"/", targetProgrammeCode);
                        logArea.append("Fetched " + students.size() + " students for programme: " + programme + "\n");
                    } catch (IOException ex) {
                        logArea.append("Error loading students: " + ex.getMessage() + "\n");
                    }
                }
            });

            generateBtn.addActionListener(e -> {
                String programme = programmeField.getText().trim();
                String reason = (String) reasonCombo.getSelectedItem();

                if (programme.isEmpty() || reason == null || reason.equals("Select Priority Reason...")) {
                    JOptionPane.showMessageDialog(frame, "Please enter a programme and select a valid reason.");
                    return;
                }
                else{
                    priorityStudents= DataPipeline.fetchPriorityStudents(students, targetProgrammeCode,choosenCiteria);
                }
                        // Check if the list is empty or null
        if (priorityStudents == null || priorityStudents.isEmpty()) {
            logArea.append("No priority students found for the selected criteria.\n");
        } else {
            logArea.append("Priority Students:\n");
            // Iterate and display each student's name and ID
            for (Student student : priorityStudents) {
                // Assuming Student class has methods like getName() and getStudentId()
                // Adjust method names if they differ in your Student class definition
                logArea.append("  - Name: " + student.getName() + ", ID: " + student.getBannerID() + "\n");

                logArea.append("Generating priority group for Programme: " + programme + ", Reason: " + reason + "\n");

            }
        }
                
                // FIXME: The method calculatePriorityGroup(Student, String) in the type DataPipeline
                // is not applicable for the arguments (List<Student>, String).
                // You might need to iterate through the students list or change the DataPipeline method.
                // Example of iteration (if needed):
                // for (Student student : students) {
                //     DataPipeline.calculatePriorityGroup(student, reason);
                // }
                // Or adjust the DataPipeline method signature.
                // DataPipeline.calculatePriorityGroup(students, reason); // Keep commented out or implement fix

                logArea.append("Skipping priority group calculation due to method signature mismatch or pending implementation.\n");

                // TODO: Implement actual priority group generation logic based on 'students' list and 'reason'
                logArea.append("Priority group generation process initiated (actual logic pending).\n");
                // Example: List<Student> priorityGroup = DataPipeline.generateGroup(students, reason);
                // logArea.append("Generated priority group with " + priorityGroup.size() + " students.\n");
                logArea.append("Priority group generated (dummy output).\n");
                
            });

            // Add Reset Button
            JButton resetBtn = new JButton("Reset");
            gbc.gridx = 2; // Place it next to Generate button
            gbc.gridy = 3;
            topPanel.add(resetBtn, gbc);

            resetBtn.addActionListener(e -> {
                // Reset UI components
                rootFolder = null;
                programmeField.setText("");
                reasonCombo.setSelectedIndex(0); // Reset to "Select Priority Reason..."
                logArea.setText(""); // Clear log area
                chooseRootBtn.setEnabled(true); // Re-enable root folder selection
                generateBtn.setEnabled(true); // Keep enabled or disable based on logic

                // Clear data structures
                students.clear();
                priorityStudents.clear();
                targetProgrammeCodesList.clear();
                targetProgrammeCode = "";
                choosenCiteria = "";

                logArea.append("Application reset.\n");
            });

            frame.setVisible(true);
        });
    }
}
