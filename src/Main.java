package src;


import java.util.*;
import java.io.*;
import java.nio.file.*;  
// Add the missing import or define the Component class
 
public class Main {

    public static void main(String[] args) {
        try {
            // Read Qlikview data to create a list of students
            List<Student> students = new ArrayList<>();
            students.addAll(readQlikviewData("data/BMT/Qlikview/24.25 BMT.F Module by Registration Status.xlsx"));
            students.addAll(readQlikviewData("data/BMT/Qlikview/24.25 BMT.F Programme with Registration Status.xlsx"));
            students.addAll(readQlikviewData("data/BMT/Qlikview/24.25 BMT.S Module by Registration Status.xlsx"));
            students.addAll(readQlikviewData("data/BMT/Qlikview/24.25 BMT.S Programme with Registration Status.xlsx"));

            // Get module deadlines and component info from SBS Source document
            Map<String, Module> modules = readSBSData("data/SBS Source 24-25.xlsx");

            // Get student pass or fail data from EBR folder
            Map<String, List<Component>> ebrData = new HashMap<>();
            Path ebrDir = Paths.get("data/BMT/EBR");
            if (Files.exists(ebrDir) && Files.isDirectory(ebrDir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(ebrDir, "ProgrammeReport-*.xlsx")) {
                    for (Path entry : stream) {
                        ebrData.putAll(readEBRData(entry.toAbsolutePath().toString()));
                    }
                }
            } else {
                System.err.println("Error: Directory 'data/BMT/EBR' does not exist or is not accessible.");
            }

            // Calculate trailing status for each student
            for (Student student : students) {
                calculateTrailingStatus(student, modules, ebrData);
            }

            // Output results
            for (Student student : students) {
                if (student.getRegistrationStatus().equals("21")) {
                    student.setNewOrContinue("New");
                } else {
                    student.setNewOrContinue("Continue");
                }

                // Populate modules for the student
                List<Module> studentModules = new ArrayList<>();
                for (Component component : ebrData.getOrDefault(student.getId(), Collections.emptyList())) {
                    Module module = modules.get(component.getComponentCode());
                    if (module != null) {
                        studentModules.add(module);
                    }
                }
                for (Module module : studentModules) {
                    Module studentModule = new Module(module.getModuleCode(), module.getModuleName());

                    // Populate components for the module
                    List<Component> moduleComponents = new ArrayList<>();
                    for (Component component : ebrData.getOrDefault(student.getId(), Collections.emptyList())) {
                        if (component.getComponentCode().equals(module.getModuleCode())) {
                            moduleComponents.add(component);
                        }
                    }
                    for (Component component : moduleComponents) {
                        studentModule.addComponent(new Component(
                            component.getComponentCode(),
                            component.getComponentName(),
                            component.getComponentType(),
                            component.getDeadline(), // Use module deadline
                            component.getStatus(),
                            component.getScore() // Include score in the component
                        ));
                    }

                    student.addModule(studentModule);
                }
            }
            for (Student student : students) {
                System.out.println(student);
            }

        } catch (IOException e) {
            System.err.println("Error reading data: " + e.getMessage());
        }
    }

    private static List<Student> readQlikviewData(String filePath) throws IOException {
        List<Student> students = new ArrayList<>();
        try (InputStream fis = new FileInputStream(filePath)) {
            try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(fis)) {
                java.util.zip.ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.getName().equals("xl/worksheets/sheet1.xml")) {
                        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                        javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
                        org.w3c.dom.Document doc = builder.parse(zis);
                        org.w3c.dom.NodeList rows = doc.getElementsByTagName("row");

                        for (int i = 1; i < rows.getLength(); i++) {
                            org.w3c.dom.Element row = (org.w3c.dom.Element) rows.item(i);
                            Student student = new Student();

                            org.w3c.dom.NodeList cells = row.getElementsByTagName("c");
                            student.setId(cells.item(0).getTextContent());
                            student.setName(cells.item(1).getTextContent());
                            student.setProgramme(cells.item(2).getTextContent());
                            student.setRegistrationStatus(cells.item(3).getTextContent());

                            List<Module> modules = new ArrayList<>();
                            for (int j = 4; j < cells.getLength(); j += 2) {
                                Module module = new Module();
                                module.setModuleCode(cells.item(j).getTextContent());
                                module.setModuleName(cells.item(j + 1).getTextContent());
                                modules.add(module);
                            }
                            student.setModules(modules);

                            students.add(student);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading Qlikview data from file: " + filePath + " - " + e.getMessage());
        }
        return students;
    }
    private static Map<String, Module> readSBSData(String filePath) throws IOException {
        Map<String, Module> modules = new HashMap<>();
        try (InputStream fis = new FileInputStream(filePath)) {
            try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(fis)) {
                java.util.zip.ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.getName().equals("xl/worksheets/sheet1.xml")) {
                        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                        javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
                        org.w3c.dom.Document doc = builder.parse(zis);
                        org.w3c.dom.NodeList rows = doc.getElementsByTagName("row");

                        for (int i = 1; i < rows.getLength(); i++) {
                            org.w3c.dom.Element row = (org.w3c.dom.Element) rows.item(i);
                            Module module = new Module();

                            org.w3c.dom.NodeList cells = row.getElementsByTagName("c");
                            module.setModuleCode(cells.item(0).getTextContent());
                            module.setModuleName(cells.item(1).getTextContent());

                            // Load components for the module
                            List<Component> components = new ArrayList<>();
                            for (int j = 2; j < cells.getLength(); j += 3) {
                                Component component = new Component();
                                component.setComponentCode(cells.item(j).getTextContent());
                                component.setComponentName(cells.item(j + 1).getTextContent());
                                component.setDeadline(cells.item(j + 2).getTextContent());
                                components.add(component);
                            }
                            module.setComponents(components);

                            modules.put(module.getModuleCode(), module);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading SBS data from file: " + filePath + " - " + e.getMessage());
        }
        return modules;
    }

    private static void calculateTrailingStatus(Student student, Map<String, Module> modules, Map<String, List<Component>> ebrData) {
        // Get the list of modules the student is enrolled in
        List<Module> studentModules = student.getModules();

        // Use a set to track unique module levels
        Set<String> moduleLevels = new HashSet<>();
        for (Module module : studentModules) {
            moduleLevels.add(module.getModuleLevel());
        }

        // If there is more than one unique module level, the student is trailing
        boolean isTrailing = moduleLevels.size() > 1;

        student.setTrailing(isTrailing);
    }

    private static Map<String, List<Component>> readEBRData(String filePath) throws IOException {
        Map<String, List<Component>> ebrData = new HashMap<>();
        try (InputStream fis = new FileInputStream(filePath)) {
            try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(fis)) {
                java.util.zip.ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.getName().equals("xl/worksheets/sheet1.xml")) {
                        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                        javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
                        org.w3c.dom.Document doc = builder.parse(zis);
                        org.w3c.dom.NodeList rows = doc.getElementsByTagName("row");

                        for (int i = 1; i < rows.getLength(); i++) {
                            org.w3c.dom.Element row = (org.w3c.dom.Element) rows.item(i);
                            Component component = new Component();

                            org.w3c.dom.NodeList cells = row.getElementsByTagName("c");
                            String studentId = cells.item(0).getTextContent();
                            component.setComponentCode(cells.item(1).getTextContent());
                            component.setComponentName(cells.item(2).getTextContent());
                            component.setComponentType(cells.item(3).getTextContent());
                            component.setDeadline(cells.item(4).getTextContent());
                            component.setStatus(cells.item(5).getTextContent());
                            component.setScore(Double.parseDouble(cells.item(6).getTextContent()));

                            ebrData.computeIfAbsent(studentId, k -> new ArrayList<>()).add(component);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading EBR data from file: " + filePath + " - " + e.getMessage());
        }
        return ebrData;
    }
}

