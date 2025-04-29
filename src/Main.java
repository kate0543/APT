package src;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static JFrame frame;
    private static JTextArea logArea;
    private static JFileChooser folderChooser; 
    private static JComboBox<String> reasonCombo;
    private static JComboBox<String> programmeCombo;
    private static JTextField attendanceRateField;
    private static JLabel attendanceThresholdLabel;
    private static JButton generateBtn;
    private static JButton exportBtn;

    private static File rootFolder;
    private static File targetFolder;
    private static String priorityString = "";
    private static String targetProgrammeCode = "";
    private static List<String> targetProgrammeCodesList = new ArrayList<>();

    // Default attendance threshold
    private static int default_attendance_threshold = 30;

    // Made static to be accessed from static main method and lambdas
    static List<Student> students = new ArrayList<>();
    static List<Student> priorityStudents = new ArrayList<>();
    static String choosenCiteria = "";
    static DataPipeline pipeline = new DataPipeline();

    // Programme ComboBox setup (editable, with default options)
 
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
    public enum ProgrammeList {
        SBMT("SBMT"),
        LBL("LBL");

        private final String displayName;

        ProgrammeList(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            setupUI();
        });
    }

    private static void setupUI() {
        frame = new JFrame("Student Priority Group Tool");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 500);
        frame.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Padding between components
        gbc.anchor = GridBagConstraints.WEST;

        // Components
        JButton chooseRootBtn = new JButton("Choose Root Folder");
        JButton chooseTargetBtn = new JButton("Choose Target Folder"); 
        
        // Initialize programmeField before using it
        setupProgrammeCombo();
        setupReasonCombo();
        setupAttendanceField();

        JButton loadDataBtn = new JButton("Load Data");
        generateBtn = new JButton("Generate Priority Group");
        generateBtn.setEnabled(true);

        // Create Export Button
        exportBtn = new JButton("Export to CSV");
        exportBtn.setEnabled(false); // Initially disabled

        // Add Reset Button
        JButton resetBtn = new JButton("Reset");

        // Add components to panel
        addComponentsToPanel(topPanel, gbc, chooseRootBtn, chooseTargetBtn, loadDataBtn, generateBtn, resetBtn);

        // Log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);

        // Frame Layout
        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);

        folderChooser = new JFileChooser();
        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        // Setup action listeners
        setupActionListeners(chooseRootBtn, chooseTargetBtn,loadDataBtn, generateBtn, exportBtn, resetBtn);

        // Set initial visibility of attendance field
        updateAttendanceFieldVisibility();

        frame.setVisible(true);
    }

    private static void setupReasonCombo() {
        reasonCombo = new JComboBox<>();
        reasonCombo.addItem("Select Priority Reason...");
        for (PriorityCriteriaList criteria : PriorityCriteriaList.values()) {
            reasonCombo.addItem(criteria.toString());
        }

        reasonCombo.addActionListener(e -> {
            Object selectedItem = reasonCombo.getSelectedItem();
            if (selectedItem != null && !selectedItem.toString().equals("Select Priority Reason...")) {
                choosenCiteria = selectedItem.toString();
                updateAttendanceFieldVisibility();
            } else {
                choosenCiteria = ""; // Reset if default is selected
                updateAttendanceFieldVisibility();
            }
        });
    }

    private static void setupProgrammeCombo() {
        programmeCombo = new JComboBox<>(); 
        programmeCombo.addItem("Select Programme Code...");
        for (ProgrammeList programme : ProgrammeList.values()) {
            programmeCombo.addItem(programme.toString());
        } 
        programmeCombo.addActionListener(e -> {
            Object selected = programmeCombo.getSelectedItem();
            if (selected != null && !selected.toString().equals("Select Programme Code...")) {
                // Update targetProgrammeCode based on selected item
                targetProgrammeCode = selected.toString().trim();
            }
            else {
                targetProgrammeCode = ""; // Reset if default is selected
            }
        });
    }

    private static void setupAttendanceField() {
        attendanceThresholdLabel = new JLabel("Attendance Threshold (%):");
        
        attendanceRateField = new JTextField(5);
        attendanceRateField.setText(String.valueOf(default_attendance_threshold));
        attendanceRateField.setToolTipText("Enter Attendance Rate Threshold (%)");

        attendanceRateField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateAttendanceThreshold();
            }
        });
    }
    
    private static void updateAttendanceFieldVisibility() {
        boolean shouldShow = choosenCiteria.equals(PriorityCriteriaList.ALL_REASONS.toString()) || 
                            choosenCiteria.equals(PriorityCriteriaList.LOW_ATTENDANCE.toString());
        
        attendanceThresholdLabel.setVisible(shouldShow);
        attendanceRateField.setVisible(shouldShow);
    }
    
    private static void addComponentsToPanel(JPanel topPanel, GridBagConstraints gbc, 
                                        JButton chooseRootBtn, JButton chooseTargetBtn, JButton loadDataBtn, 
                                        JButton generateBtn, JButton resetBtn) {
        // Set up panel with some padding and spacing
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // File Selection Section
        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filePanel.add(chooseRootBtn);
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        topPanel.add(filePanel, gbc);

        filePanel.add(chooseTargetBtn);
        
        gbc.gridx = 5;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        topPanel.add(filePanel, gbc);

        // Add separator
        gbc.gridy = 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        topPanel.add(new JSeparator(), gbc);
        
        // Parameters Section
        JPanel paramsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints paramGbc = new GridBagConstraints();
        paramGbc.insets = new Insets(2, 5, 2, 5);
        paramGbc.anchor = GridBagConstraints.WEST;
        
        // Programme row
        paramGbc.gridx = 0;
        paramGbc.gridy = 0;
        paramsPanel.add(new JLabel("Programme Code:"), paramGbc);
        
        paramGbc.gridx = 1;
        paramGbc.weightx = 1.0;
        paramGbc.fill = GridBagConstraints.HORIZONTAL;
        paramsPanel.add(programmeCombo, paramGbc);
        
        // Reason row
        paramGbc.gridx = 0;
        paramGbc.gridy = 1;
        paramGbc.weightx = 0;
        paramGbc.fill = GridBagConstraints.NONE;
        paramsPanel.add(new JLabel("Priority Reason:"), paramGbc);
        
        paramGbc.gridx = 1;
        paramGbc.weightx = 1.0;
        paramGbc.fill = GridBagConstraints.HORIZONTAL;
        paramsPanel.add(reasonCombo, paramGbc);
        
        // Attendance threshold row
        paramGbc.gridx = 0;
        paramGbc.gridy = 2;
        paramGbc.weightx = 0;
        paramGbc.fill = GridBagConstraints.NONE;
        paramsPanel.add(attendanceThresholdLabel, paramGbc);
        
        paramGbc.gridx = 1;
        paramGbc.fill = GridBagConstraints.NONE;
        paramGbc.anchor = GridBagConstraints.WEST;
        paramsPanel.add(attendanceRateField, paramGbc);
        
        // Add parameters panel to main panel
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        topPanel.add(paramsPanel, gbc);
        
        // Add separator
        gbc.gridy = 3;
        topPanel.add(new JSeparator(), gbc);
        
        // Action Buttons Section
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttonPanel.add(loadDataBtn);
        buttonPanel.add(generateBtn);
        buttonPanel.add(exportBtn);
        buttonPanel.add(resetBtn);
        
        gbc.gridy = 4;
        gbc.insets = new Insets(5, 5, 10, 5);
        topPanel.add(buttonPanel, gbc);
    }
    
    private static void setupActionListeners(JButton chooseRootBtn, JButton chooseTargetBtn, JButton loadDataBtn, 
                                          JButton generateBtn, JButton exportBtn, JButton resetBtn) {
        // Choose Root Button Action
        chooseRootBtn.addActionListener(e -> handleChooseRoot(chooseRootBtn));
        chooseTargetBtn.addActionListener(e -> handleChooseTarget(chooseTargetBtn));

        // Load Data Button Action
        loadDataBtn.addActionListener(e -> handleLoadData());

        // Generate Button Action
        generateBtn.addActionListener(e -> handleGeneratePriorityGroup());

        // Export Button Action
        exportBtn.addActionListener(e -> handleExportToCsv());

        // Reset Button Action
        resetBtn.addActionListener(e -> handleReset(chooseRootBtn, chooseTargetBtn));
    }
    
    private static void handleChooseRoot(JButton chooseRootBtn) {
        int result = folderChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            rootFolder = folderChooser.getSelectedFile();
            logArea.append("Root folder set to: " + rootFolder.getAbsolutePath() + "\n");
            chooseRootBtn.setEnabled(false);
            pipeline.baseFolderPath= (rootFolder.getAbsolutePath() + "\\").replace("\\", "/");
            
        }
        System.out.println(pipeline.baseFolderPath);
    }
    private static void handleChooseTarget(JButton chooseTargetBtn) {
        int result = folderChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            targetFolder = folderChooser.getSelectedFile();
            logArea.append("Target folder set to: " + targetFolder.getAbsolutePath() + "\n");
            chooseTargetBtn.setEnabled(false);
            pipeline.logFolderPath= (targetFolder.getAbsolutePath() + "\\").replace("\\", "/");
        }
        System.out.println(pipeline.logFolderPath);

    }
    
    private static void handleLoadData() {
        if (rootFolder == null) {
            showWrappedMessageDialog("Please choose a root folder first.");
            return;
        }
        
        String programme = programmeCombo.getSelectedItem() != null ? programmeCombo.getSelectedItem().toString().trim() : "";
        if (programme.isEmpty()) {
            showWrappedMessageDialog("Please select a programme code.");
            return;
        }
        
        logArea.append("Loading data from: " + rootFolder.getAbsolutePath() + "\n");
        
        try {
            // Update targetProgrammeCode from the input field
            targetProgrammeCode = programme;
            
            // Generate programme codes list (with S and F suffixes) 
            
            // Resolve path for program data
            Path baseFolderPath = rootFolder.toPath().resolve(programme);
            
            // Load students from data path
            students = StEP.fetchStudents(students, baseFolderPath.toString() + "\\", targetProgrammeCode,pipeline);
            logArea.append("Fetched " + students.size() + " students for programme: " + programme + "\n");
            
            // Enable generate button if students were loaded
            if (!students.isEmpty()) {
                generateBtn.setEnabled(true);
            }
        } catch (IOException ex) {
            logArea.append("Error loading students: " + ex.getMessage() + "\n");
            showWrappedMessageDialog("Error loading data: " + ex.getMessage(), 
                                  "Data Loading Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            logArea.append("Unexpected error: " + ex.getMessage() + "\n");
            showWrappedMessageDialog("Unexpected error: " + ex.getMessage(), 
                                   "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private static void handleGeneratePriorityGroup() {
        // Ensure data has been loaded
        if (students == null || students.isEmpty()) {
            showWrappedMessageDialog("Please load data first using the 'Load Data' button.");
            return;
        }

        // Get programme from field
        String programme = programmeCombo.getSelectedItem() != null ? programmeCombo.getSelectedItem().toString().trim() : "";
        
        // Validate attendance threshold if needed
        boolean isAttendanceRelevant = choosenCiteria.equals(PriorityCriteriaList.ALL_REASONS.toString()) || 
                                    choosenCiteria.equals(PriorityCriteriaList.LOW_ATTENDANCE.toString());
        
        if (isAttendanceRelevant && !updateAttendanceThreshold()) {
            return;
        }

        // Validate criteria selection
        if (choosenCiteria == null || choosenCiteria.isEmpty() || 
            choosenCiteria.equals("Select Priority Reason...")) {
            showWrappedMessageDialog("Please select a valid priority reason.");
            return;
        }

        try {
            // Log generation parameters
            logArea.append("--- Generating Priority Group ---\n");
            logArea.append("Programme: " + (programme.isEmpty() ? "Not specified" : programme) + "\n");
            logArea.append("Selected Reason: " + choosenCiteria + "\n");
            
            if (isAttendanceRelevant) {
                logArea.append("Attendance Threshold: " + default_attendance_threshold + "%\n");
            }
            
            logArea.append("Using loaded student data (" + students.size() + " students).\n");

            // Create data pipeline and set threshold 
            pipeline.threshold = default_attendance_threshold;

            System.out.println("logFolderPath: "+pipeline.baseFolderPath+", baseFolderPath: "+pipeline.logFolderPath); 

            // Get priority students
            priorityStudents = pipeline.fetchPriorityStudents(students, targetProgrammeCode, choosenCiteria);

            // Handle results
            if (priorityStudents == null || priorityStudents.isEmpty()) {
                logArea.append("No priority students found for the selected criteria: " + choosenCiteria + "\n");
                exportBtn.setEnabled(false);
            } else {
                logArea.append("Found " + priorityStudents.size() + " priority students for criteria: " + choosenCiteria + "\n");
                
                // Display each student
                for (Student student : priorityStudents) {
                    logArea.append("  - " + student.toString() + "\n");
                }
                
                // Enable export
                exportBtn.setEnabled(true);
            }

            logArea.append("--- Generation Complete ---\n\n");
            
        } catch (Exception ex) {
            logArea.append("Error generating priority group: " + ex.getMessage() + "\n");
            showWrappedMessageDialog("Error generating priority group: " + ex.getMessage(),
                                  "Generation Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private static void handleExportToCsv() {
        if (priorityStudents == null || priorityStudents.isEmpty()) {
            showWrappedMessageDialog("No priority students data available to export.");
            return;
        }

        JFileChooser fileSaver = new JFileChooser();
        fileSaver.setDialogTitle("Save Priority Students CSV");
        
        // Create suggested filename
        String sanitizedCriteria = choosenCiteria.replace(" ", "_").replace("/", "_");
        String suggestedFilename = "priority_students_" + sanitizedCriteria + ".csv";
        fileSaver.setSelectedFile(new File(suggestedFilename));
        
        fileSaver.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files", "csv"));

        int userSelection = fileSaver.showSaveDialog(frame);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileSaver.getSelectedFile();
            
            // Ensure CSV extension
            if (!fileToSave.getName().toLowerCase().endsWith(".csv")) {
                fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".csv");
            }

            logArea.append("Exporting priority students to: " + fileToSave.getAbsolutePath() + "\n");

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileToSave))) {
                // Write CSV Header with better field names
                boolean isAttendanceRelevant = choosenCiteria.equals(PriorityCriteriaList.ALL_REASONS.toString()) || 
                                            choosenCiteria.equals(PriorityCriteriaList.LOW_ATTENDANCE.toString());
                
                String header = "StudentInfo,Criteria";
                if (isAttendanceRelevant) {
                    header += ",Attendance Threshold";
                }
                writer.write(header);
                writer.newLine();

                // Write student data
                for (Student student : priorityStudents) {
                    // Using a more robust CSV formatting approach
                    // This assumes Student.toString() returns something like "Smith, John (12345)"
                    String studentInfo = " ["+csvEscape(student.toString())+"]";
                    String line = studentInfo + "," + csvEscape(choosenCiteria);
                    
                    if (isAttendanceRelevant) {
                        line += "," + default_attendance_threshold;
                    }
                    
                    writer.write(line);
                    writer.newLine();
                }
                
                logArea.append("Export successful.\n");
                showWrappedMessageDialog("Priority students exported successfully to:\n" + 
                                      fileToSave.getAbsolutePath());

            } catch (IOException ex) {
                logArea.append("Error exporting to CSV: " + ex.getMessage() + "\n");
                showWrappedMessageDialog("Error exporting file: " + ex.getMessage(), 
                                      "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private static String csvEscape(String value) {
        if (value == null) {
            return "";
        }
        
        // If value contains comma, quote, or newline, wrap in quotes and escape quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
    
    private static void handleReset(JButton chooseRootBtn, JButton chooseTargetBtn) {
        // Reset UI components
        rootFolder = null;
        programmeCombo.setSelectedIndex(0);
        reasonCombo.setSelectedIndex(0);
        attendanceRateField.setText(String.valueOf(default_attendance_threshold));
        updateAttendanceFieldVisibility(); // Reset visibility
        logArea.setText("");
        chooseRootBtn.setEnabled(true);
        chooseTargetBtn.setEnabled(true);
        generateBtn.setEnabled(true);
        exportBtn.setEnabled(false);
        // Clear data structures
        students.clear();
        priorityStudents.clear();
        targetProgrammeCodesList.clear();
        targetProgrammeCode = "";
        choosenCiteria = "";

        logArea.append("Application reset.\n");
    }
    
    // Method to validate and update the default attendance threshold
    private static boolean updateAttendanceThreshold() {
        String rateText = attendanceRateField.getText().trim();
        
        // If field is empty, set back to default
        if (rateText.isEmpty()) {
            attendanceRateField.setText(String.valueOf(default_attendance_threshold));
            return true;
        }
        
        try {
            int newThreshold = Integer.parseInt(rateText);
            
            // Validate range (0-100%)
            if (newThreshold < 0 || newThreshold > 100) {
                showWrappedMessageDialog("Attendance threshold must be between 0 and 100.", 
                    "Invalid Input", 
                    JOptionPane.ERROR_MESSAGE);
                attendanceRateField.requestFocus();
                return false;
            }
            
            // Update the default threshold
            default_attendance_threshold = newThreshold;
            return true;
        } catch (NumberFormatException ex) {
            showWrappedMessageDialog("Please enter a valid integer for attendance threshold.",
                "Invalid Input", 
                JOptionPane.ERROR_MESSAGE);
            attendanceRateField.requestFocus();
            return false;
        }
    }
    
    // Helper method to show wrapped text in JOptionPane
    private static void showWrappedMessageDialog(String message) {
        showWrappedMessageDialog(message, "Message", JOptionPane.INFORMATION_MESSAGE);
    }
    
    // Helper method to show wrapped text in JOptionPane with custom title and message type
    private static void showWrappedMessageDialog(String message, String title, int messageType) {
        JTextArea textArea = new JTextArea(message);
        textArea.setEditable(false);
        textArea.setWrapStyleWord(true);  // Wrap at word boundaries
        textArea.setLineWrap(true);       // Enable line wrapping
        textArea.setBackground(UIManager.getColor("Label.background"));
        textArea.setFont(UIManager.getFont("Label.font"));
        textArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Create a panel that will contain the text area
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(textArea, BorderLayout.CENTER);
        
        // Set a reasonable maximum width (you can adjust this)
        int maxWidth = 600;  // pixels
        panel.setPreferredSize(new Dimension(maxWidth, textArea.getPreferredSize().height));
        
        // Show the dialog - the text will wrap automatically within the panel's width
        JOptionPane.showMessageDialog(frame, panel, title, messageType);
    }
}