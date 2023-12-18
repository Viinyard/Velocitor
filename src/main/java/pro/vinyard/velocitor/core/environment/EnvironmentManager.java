package pro.vinyard.velocitor.core.environment;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.stereotype.Service;
import pro.vinyard.velocitor.core.UserProperties;
import pro.vinyard.velocitor.core.environment.entities.Environment;
import pro.vinyard.velocitor.core.exception.VelocitorException;
import pro.vinyard.velocitor.core.utils.DirectoryUtils;
import pro.vinyard.velocitor.core.utils.FileUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

@Service
public class EnvironmentManager {

    public static final String FILE_LOCATION = "config/environment/";
    public static final String SCHEMA_FILE = "environment.xsd";
    public static final String CONFIGURATION_FILE = "environment.xml";
    private final ApplicationHome home;
    Logger log = LoggerFactory.getLogger(EnvironmentManager.class);
    @Autowired
    private EnvironmentProperties environmentProperties;
    @Autowired
    private UserProperties userProperties;
    @Getter
    private File homeDirectory;

    public EnvironmentManager() {
        this.home = new ApplicationHome(EnvironmentManager.class);
    }

    @PostConstruct
    public void init() {
        Optional.ofNullable(userProperties.getProperty(UserProperties.HOME_DIRECTORY)).map(Path::of).ifPresent(this::setHomeDirectory);
    }

    public String getLineSeparator() {
        return System.lineSeparator();
    }

    public String getWorkingDirectory() {
        return DirectoryUtils.getWorkingDirectory();
    }

    public String getOperatingSystem() {
        return System.getProperty("os.name");
    }

    public void setHomeDirectory(Path homeDirectory) {
        try {
            File homeDirectoryFile = Optional.of(homeDirectory).map(Path::toFile).orElseThrow(() -> new IllegalArgumentException("Home directory cannot be null"));

            homeDirectoryFile = homeDirectoryFile.getCanonicalFile();

            if (!homeDirectoryFile.exists())
                throw new IllegalArgumentException("Home directory does not exist");
            if (!homeDirectoryFile.isDirectory())
                throw new IllegalArgumentException("Home directory is not a directory");

            this.homeDirectory = homeDirectoryFile;

            this.userProperties.setProperty(UserProperties.HOME_DIRECTORY, this.homeDirectory.getAbsolutePath());

        } catch (Exception e) {
            throw new IllegalArgumentException("Home directory is not valid");
        }
    }

    public String getJavaVersion() {
        return System.getProperty("java.runtime.version");
    }

    public File getJarFile() {
        return home.getSource();
    }

    public String getJarFileLocation() {
        return Optional.ofNullable(getJarFile()).map(File::getAbsolutePath).orElse(null);
    }

    public File getConfigurationFile() {
        return new File(getWorkspaceDirectory(), CONFIGURATION_FILE);
    }

    public boolean checkConfigurationFile() {
        return Optional.ofNullable(this.getConfigurationFile()).filter(File::exists).filter(File::isFile).isPresent();
    }

    public File getSchemaFile() {
        return new File(getWorkspaceDirectory(), SCHEMA_FILE);
    }

    public boolean checkSchemaFile() {
        return Optional.ofNullable(this.getSchemaFile()).filter(File::exists).filter(File::isFile).isPresent();
    }

    public File getWorkspaceDirectory() {
        return new File(homeDirectory, environmentProperties.getEnvironment().getWorkspaceDirectory());
    }

    public boolean checkWorkspaceDirectory() {
        return Optional.ofNullable(this.getWorkspaceDirectory()).filter(File::exists).filter(File::isDirectory).isPresent();
    }

    public File getModelDirectory() {
        return new File(getWorkspaceDirectory(), environmentProperties.getEnvironment().getModelDirectory());
    }

    public boolean checkModelDirectory() {
        return Optional.ofNullable(this.getModelDirectory()).filter(File::exists).filter(File::isDirectory).isPresent();
    }

    public File getTemplateDirectory() {
        return new File(getWorkspaceDirectory(), environmentProperties.getEnvironment().getTemplateDirectory());
    }

    public boolean checkTemplateDirectory() {
        return Optional.ofNullable(this.getTemplateDirectory()).filter(File::exists).filter(File::isDirectory).isPresent();
    }

    public String initialize() {
        StringBuilder stringBuilder = new StringBuilder();

        Consumer<String> appendLine = (value) -> stringBuilder.append(value).append(getLineSeparator());

        if (!this.checkWorkspaceDirectory()) {
            File workspaceDirectory = DirectoryUtils.createFolder(getWorkspaceDirectory());
            appendLine.accept(String.format("Workspace directory %s created.", workspaceDirectory.getAbsolutePath()));
        }

        if (!this.checkModelDirectory()) {
            File modelDirectory = DirectoryUtils.createFolder(getModelDirectory());
            appendLine.accept(String.format("Model directory %s created.", modelDirectory.getAbsolutePath()));
        }

        if (!this.checkTemplateDirectory()) {
            File templateDirectory = DirectoryUtils.createFolder(getTemplateDirectory());
            appendLine.accept(String.format("Template directory %s created.", templateDirectory.getAbsolutePath()));
        }

        if (!this.checkSchemaFile()) {
            File schemaFile = FileUtils.copyFile(FILE_LOCATION + SCHEMA_FILE, getSchemaFile());
            appendLine.accept(String.format("Environment schema file %s created.", schemaFile.getAbsolutePath()));
        }

        if (!this.checkConfigurationFile()) {
            File configurableFile = FileUtils.copyFile(FILE_LOCATION + CONFIGURATION_FILE, getConfigurationFile());
            appendLine.accept(String.format("Environment configuration file %s created.", configurableFile.getAbsolutePath()));
        }

        try {
            Environment environment = loadConfigurationFile();
            if (environment == null)
                appendLine.accept(String.format("Environment configuration file %s is invalid.", getConfigurationFile().getAbsolutePath()));
        } catch (VelocitorException e) {
            appendLine.accept(String.format("Environment configuration file %s is invalid : %s", getConfigurationFile().getAbsolutePath(), e.getMessage()));
        }

        return stringBuilder.toString();
    }

    public Environment loadConfigurationFile() throws VelocitorException {
        return FileUtils.loadConfigurationFile(this.getConfigurationFile(), Environment.class, FILE_LOCATION + SCHEMA_FILE);
    }

    public String getJarDirectoryLocation() {
        return home.getDir().toString();
    }

    public String getHomeDirectoryPath() {
        return Optional.ofNullable(getHomeDirectory()).map(File::getAbsolutePath).orElse(null);
    }

    public boolean checkHomeDirectory() {
        return Optional.ofNullable(this.getHomeDirectory()).filter(File::exists).filter(File::isDirectory).isPresent();
    }

    public boolean checkEnvironmentInitialized() {
        log.info("Check environment initialized, home directory is {}, workspace directory is {}, model directory is {}, template directory is {}, ressources properties file is {}", getHomeDirectoryPath(), getWorkspaceDirectory(), getModelDirectory(), getTemplateDirectory(), getConfigurationFile());
        return checkHomeDirectory()
                && checkWorkspaceDirectory()
                && checkModelDirectory()
                && checkTemplateDirectory()
                && checkConfigurationFile()
                && checkSchemaFile();
    }
}
