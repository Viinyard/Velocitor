package pro.vinyard.velocitor.core.model;

import org.apache.velocity.VelocityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.vinyard.velocitor.core.environment.EnvironmentManager;
import pro.vinyard.velocitor.core.environment.entities.Environment;
import pro.vinyard.velocitor.core.exception.VelocitorException;
import pro.vinyard.velocitor.core.generation.GenerationManager;
import pro.vinyard.velocitor.core.model.entities.Directive;
import pro.vinyard.velocitor.core.model.entities.Directives;
import pro.vinyard.velocitor.core.model.entities.Model;
import pro.vinyard.velocitor.core.template.TemplateManager;
import pro.vinyard.velocitor.core.utils.DirectoryUtils;
import pro.vinyard.velocitor.core.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Component
public class ModelManager {

    public static final String FILE_LOCATION = "config/model/";
    public static final String CONFIGURATION_FILE = "model.xml";
    public static final String SCHEMA_FILE = "model.xsd";

    @Autowired
    private EnvironmentManager environmentManager;

    @Autowired
    private TemplateManager templateManager;

    @Autowired
    private GenerationManager generationManager;

    public String createModel(String name) {
        StringBuilder stringBuilder = new StringBuilder();
        Consumer<String> appendLine = (value) -> stringBuilder.append(value).append(environmentManager.getLineSeparator());

        File modelFolder = DirectoryUtils.createFolder(environmentManager.getModelDirectory(), name);
        appendLine.accept(String.format("Model directory %s created.", modelFolder.getAbsolutePath()));

        File xmlFile = FileUtils.copyFile(FILE_LOCATION + CONFIGURATION_FILE, new File(modelFolder, CONFIGURATION_FILE));
        appendLine.accept(String.format("Model configuration file %s created.", xmlFile.getAbsolutePath()));

        File xsdFile = FileUtils.copyFile(FILE_LOCATION + SCHEMA_FILE, new File(modelFolder, SCHEMA_FILE));
        appendLine.accept(String.format("Model configuration validator file %s created.", xsdFile.getAbsolutePath()));

        return stringBuilder.toString();
    }

    public String deleteModel(String name) {
        File modelFolder = new File(environmentManager.getModelDirectory(), name);

        if (!modelFolder.exists())
            return String.format("Model %s does not exist.", name);

        DirectoryUtils.deleteFolder(modelFolder);

        return String.format("Model %s deleted.", name);
    }

    public String listModels() {
        StringBuilder stringBuilder = new StringBuilder();
        Consumer<String> appendLine = (value) -> stringBuilder.append(value).append(environmentManager.getLineSeparator());

        List<String> models = findAllModels();

        if (models == null || models.isEmpty()) {
            appendLine.accept("No model found.");
            return stringBuilder.toString();
        }

        appendLine.accept("Models:");
        models.forEach((model) -> appendLine.accept(String.format(" - %s", model)));

        return stringBuilder.toString();
    }

    public File getModelFolder(String name) {
        return new File(environmentManager.getModelDirectory(), name);
    }

    public File getConfigurationFile(String name) {
        return new File(getModelFolder(name), CONFIGURATION_FILE);
    }

    public File getSchemaFile(String name) {
        return new File(getModelFolder(name), SCHEMA_FILE);
    }

    public String checkModel(String name) throws VelocitorException {
        File modelFolder = getModelFolder(name);

        if (!modelFolder.exists()) {
            throw new VelocitorException(String.format("Model %s does not exist.", name));
        }

        File xmlFile = getConfigurationFile(name);
        if (!xmlFile.exists()) {
            throw new VelocitorException(String.format("Model configuration file %s does not exist.", xmlFile.getName()));
        }

        File xsdFile = getSchemaFile(name);
        if (!xsdFile.exists()) {
            throw new VelocitorException(String.format("Model configuration validator file %s does not exist.", xsdFile.getName()));
        }

        this.checkConfigurationFile(xmlFile);

        return String.format("Model %s is valid.", name);
    }

    private void checkConfigurationFile(File xmlFile) throws VelocitorException {
        Model model = loadConfigurationFile(xmlFile);

        List<String> templates = Optional.of(model).map(Model::getDirectives).map(Directives::getDirectiveList).orElseGet(Collections::emptyList).stream().map(Directive::getTemplate).toList();

        templates.forEach(t -> templateManager.checkTemplate(t));
    }

    public Model loadModel(String name) throws VelocitorException, IOException {
        File xmlFile = getConfigurationFile(name);
        Environment environment = this.environmentManager.loadConfigurationFile();

        VelocityContext velocityContext = new VelocityContext();

        environment.getProperties().getPropertyList().forEach(p -> velocityContext.put(p.getKey(), p.getValue()));

        String configuration = generationManager.processTemplate(velocityContext, xmlFile);

        return loadConfigurationFile(configuration);
    }


    private Model loadConfigurationFile(File xmlFile) throws VelocitorException {
        return FileUtils.loadConfigurationFile(xmlFile, Model.class, FILE_LOCATION + SCHEMA_FILE);
    }

    private Model loadConfigurationFile(String xml) throws VelocitorException {
        return FileUtils.loadConfigurationFile(xml, Model.class, FILE_LOCATION + SCHEMA_FILE);
    }

    public List<String> findAllModels() {
        return DirectoryUtils.listFolders(environmentManager.getModelDirectory()).stream().map(File::getName).toList();
    }
}
