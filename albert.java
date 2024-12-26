import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

public class albert extends Application {

    private CodeArea codeArea;
    private File currentFile;
    private VBox fileListView;
    private HBox openFilesView;
    private Map<File, Boolean> unsavedFiles = new HashMap<>();
    private static final String OPENED_FILES_PATH = "opened_files.txt";

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Code Editor");

        BorderPane root = new BorderPane();

        // Create CodeArea
        codeArea = new CodeArea();
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

        fileListView = new VBox();
        fileListView.setPrefWidth(200);
        fileListView.setStyle("-fx-border-color: gray; -fx-border-width: 0 1 0 0; -fx-padding: 5;");

        openFilesView = new HBox();
        openFilesView.setSpacing(5);
        openFilesView.setStyle("-fx-padding: 5; -fx-border-color: gray; -fx-border-width: 0 0 1 0;");

        // Menu bar
        MenuBar menuBar = new MenuBar();

        // File Menu
        Menu fileMenu = new Menu("File");

        MenuItem newCppFile = new MenuItem("Create .cpp file");
        newCppFile.setOnAction(e -> createNewFile(primaryStage, "cpp"));

        MenuItem newHeaderFile = new MenuItem("Create .h file");
        newHeaderFile.setOnAction(e -> createNewFile(primaryStage, "h"));

        MenuItem newJavaFile = new MenuItem("Create .java file");
        newJavaFile.setOnAction(e -> createNewFile(primaryStage, "java"));

        MenuItem newPythonFile = new MenuItem("Create .py file");
        newPythonFile.setOnAction(e -> createNewFile(primaryStage, "py"));

        MenuItem openFile = new MenuItem("Open file");
        openFile.setOnAction(e -> openSingleFile(primaryStage));

        MenuItem openFolder = new MenuItem("Open folder");
        openFolder.setOnAction(e -> openFolder(primaryStage));

        fileMenu.getItems().addAll(newCppFile, newHeaderFile, newJavaFile, newPythonFile, openFile, openFolder);
        menuBar.getMenus().add(fileMenu);

        root.setTop(new VBox(menuBar, openFilesView));
        root.setCenter(codeArea);
        root.setLeft(fileListView);

        Scene scene = new Scene(root, 800, 600);

        codeArea.prefWidthProperty().bind(scene.widthProperty().subtract(fileListView.getPrefWidth()));
        codeArea.prefHeightProperty().bind(scene.heightProperty());

        // Save file on Ctrl+S combination 
        scene.setOnKeyPressed(event -> {
            if (event.isControlDown() && event.getCode().toString().equals("S")) {
                saveFile();
            }
        });

        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(event -> saveOpenedFiles());
        loadOpenedFiles();
        primaryStage.show();
    }

    private void createNewFile(Stage stage, String extension) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Create New File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("*." + extension, "*." + extension));
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                Files.createFile(file.toPath());
                codeArea.clear();
                currentFile = file;
                unsavedFiles.put(file, false);
                updateOpenFilesView(file);
            } catch (IOException ex) {
                showError("Could not create the file.");
            }
        }
    }

    private void openSingleFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open File");
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            openFile(file);
        }
    }

    private void openFolder(Stage stage) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Open Folder");
        File folder = directoryChooser.showDialog(stage);
    
        if (folder != null && folder.isDirectory()) {
            File[] files = folder.listFiles(); // Убираем фильтрацию, показываем все файлы
    
            if (files != null) {
                fileListView.getChildren().clear(); // Очищаем список перед добавлением новых файлов
    
                for (File file : files) {
                    if (file.isFile()) { // Проверяем, что это файл, а не директория
                        Button fileButton = new Button(file.getName());
                        fileButton.setOnAction(e -> openFile(file)); // Открытие файла при нажатии
                        fileListView.getChildren().add(fileButton);
                    }
                }
            } else {
                showError("No files found in the selected folder.");
            }
        }
    }
    

    private void openFile(File file) {
        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            codeArea.replaceText(content);
            currentFile = file;
            unsavedFiles.put(file, false);
            updateOpenFilesView(file);
        } catch (IOException ex) {
            showError("Could not open the file.");
        }
    }

    private void updateOpenFilesView(File fileToSelect) {
        openFilesView.getChildren().clear();
    
        unsavedFiles.forEach((openFile, isUnsaved) -> {
            HBox fileBox = new HBox();
            fileBox.setSpacing(5);
    
            // button to choose the file
            Button fileButton = new Button(openFile.getName());
            fileButton.setOnAction(e -> {
                try {
                    // loading choosed file
                    String content = new String(Files.readAllBytes(openFile.toPath()));
                    codeArea.replaceText(content);
                    currentFile = openFile;
                    unsavedFiles.put(openFile, false); // update state
                } catch (IOException ex) {
                    showError("Could not open the file.");
                }
            });
    
            // check if file saved
            Circle unsavedIndicator = new Circle(5, isUnsaved ? Color.RED : Color.TRANSPARENT);
    
            // close file
            Button closeButton = new Button("x");
            closeButton.setOnAction(e -> closeFile(openFile));
    
            fileBox.getChildren().addAll(fileButton, unsavedIndicator, closeButton);
            openFilesView.getChildren().add(fileBox);
        });
    
        // Выбираем файл, если передан
        if (fileToSelect != null && unsavedFiles.containsKey(fileToSelect)) {
            currentFile = fileToSelect;
            try {
                String content = new String(Files.readAllBytes(fileToSelect.toPath()));
                codeArea.replaceText(content);
            } catch (IOException ex) {
                showError("Could not open the file.");
            }
        }
    }
    

    private void closeFile(File file) {
        unsavedFiles.remove(file);
        if (file.equals(currentFile)) {
            codeArea.clear();
            currentFile = null;
        }
        updateOpenFilesView(null);
    }

    private void saveFile() {
        if (currentFile != null) {
            try {
                Files.write(currentFile.toPath(), codeArea.getText().getBytes());
                unsavedFiles.put(currentFile, false);
                updateOpenFilesView(currentFile);
            } catch (IOException ex) {
                showError("Could not save the file.");
            }
        } else {
            showError("No file is currently open.");
        }
    }

    private void loadOpenedFiles() {
        File openedFilesFile = new File(OPENED_FILES_PATH);
        if (openedFilesFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(openedFilesFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    File file = new File(line);
                    if (file.exists() && file.isFile()) {
                        openFile(file);
                    }
                }
            } catch (IOException ex) {
                showError("Could not load opened files.");
            }
        }
    }

    private void saveOpenedFiles() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(OPENED_FILES_PATH))) {
            for (File file : unsavedFiles.keySet()) {
                writer.write(file.getAbsolutePath());
                writer.newLine();
            }
        } catch (IOException ex) {
            showError("Could not save opened files.");
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
