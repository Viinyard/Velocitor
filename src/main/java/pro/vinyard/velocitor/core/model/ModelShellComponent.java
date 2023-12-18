package pro.vinyard.velocitor.core.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.Availability;
import org.springframework.shell.component.support.SelectorItem;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import pro.vinyard.velocitor.core.environment.EnvironmentManager;
import pro.vinyard.velocitor.core.exception.VelocitorException;
import pro.vinyard.velocitor.shell.CustomAbstractShellComponent;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@ShellComponent
public class ModelShellComponent extends CustomAbstractShellComponent {

    @Autowired
    private ModelManager modelManager;

    @Autowired
    private EnvironmentManager environmentManager;


    @ShellMethod(key = "create-model", value = "Create a new model", group = "Model")
    @ShellMethodAvailability("environmentAvailability")
    public String createModel() {
        String name = stringInput("Enter model name", null, false);

        if (name == null) {
            throw new IllegalArgumentException("Model name cannot be null");
        }

        return this.modelManager.createModel(name);
    }

    public String deleteModels() {
        //String name = stringInput("Enter model name", null, false);
        List<SelectorItem<String>> items = this.modelManager.findAllModels().stream().map(m -> SelectorItem.of(m, m)).collect(Collectors.toList());

        List<String> models = multiSelector(items, "Select models to delete");

        if (models == null || models.isEmpty()) {
            return "No model selected.";
        }

        StringBuilder stringBuilder = new StringBuilder();
        Consumer<String> appendLine = (value) -> stringBuilder.append(value).append(environmentManager.getLineSeparator());
        for (String model : models) {
            appendLine.accept(this.modelManager.deleteModel(model));
        }

        return stringBuilder.toString();
    }

    public String checkModels() {
        List<SelectorItem<String>> items = this.modelManager.findAllModels().stream().map(m -> SelectorItem.of(m, m)).collect(Collectors.toList());

        List<String> models = multiSelector(items, "Select models to check");

        if (models == null || models.isEmpty()) {
            return "No model selected.";
        }

        StringBuilder stringBuilder = new StringBuilder();
        Consumer<String> appendLine = (value) -> stringBuilder.append(value).append(environmentManager.getLineSeparator());

        for (String model : models) {
            try {
                appendLine.accept(this.modelManager.checkModel(model));
            } catch (VelocitorException e) {
                appendLine.accept(String.format("Model %s is invalid : %s", model, e.getMessage()));
                throw new RuntimeException(e);
            }
        }

        return stringBuilder.toString();
    }

    public String listModels() {
        return this.modelManager.listModels();
    }


    @ShellMethod(key = "model", value = "Action on model", group = "Model")
    @ShellMethodAvailability("environmentAvailability")
    public String model() {
        List<SelectorItem<Supplier<String>>> items = Arrays.asList(
                SelectorItem.of("Create new model", this::createModel),
                SelectorItem.of("Delete model", this::deleteModels),
                SelectorItem.of("List models", this::listModels),
                SelectorItem.of("Check models", this::checkModels)
        );

        return singleSelect(items, "Select action").get();
    }

    public Availability environmentAvailability() {
        return environmentManager.checkEnvironmentInitialized() ? Availability.available() : Availability.unavailable("Environment not initialized.");
    }
}
