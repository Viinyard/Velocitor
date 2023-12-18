package pro.vinyard.velocitor.core.template;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.component.support.SelectorItem;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import pro.vinyard.velocitor.core.environment.EnvironmentManager;
import pro.vinyard.velocitor.shell.CustomAbstractShellComponent;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@ShellComponent
public class TemplateShellComponent extends CustomAbstractShellComponent {

    @Autowired
    private TemplateManager templateManager;

    @Autowired
    private EnvironmentManager environmentManager;

    @ShellMethod(key = "template", value = "Action on template", group = "Template")
    public String template() {
        List<SelectorItem<Supplier<String>>> items = List.of(
                SelectorItem.of("Create template", this::createTemplate),
                SelectorItem.of("Delete template", this::deleteTemplates),
                SelectorItem.of("List templates", this::listTemplates)
        );

        return singleSelect(items, "Select action").get();
    }

    public String createTemplate() {
        String name = stringInput("Enter template name", null, false);

        if (name == null) {
            throw new IllegalArgumentException("Template name cannot be null");
        }

        return this.templateManager.createTemplate(name);
    }

    public String deleteTemplates() {
        List<SelectorItem<String>> items = this.templateManager.findAllTemplates().stream().map(File::getName).map(m -> SelectorItem.of(m, m)).toList();

        List<String> templates = multiSelector(items, "Select templates to delete");

        if (templates == null || templates.isEmpty()) {
            return "No template selected.";
        }

        StringBuilder stringBuilder = new StringBuilder();
        Consumer<String> appendLine = (value) -> stringBuilder.append(value).append(environmentManager.getLineSeparator());
        for (String template : templates) {
            appendLine.accept(this.templateManager.deleteTemplate(template));
        }

        return stringBuilder.toString();
    }

    public String listTemplates() {
        return this.templateManager.listTemplates();
    }
}
