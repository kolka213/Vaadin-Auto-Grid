package schwabe.code.views.masterdetail;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.repository.CrudRepository;
import schwabe.code.services.SpringContext;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.StreamSupport;

public class AutoGrid<T, ID, R extends CrudRepository<T, ID>> extends Composite<Div> {

    private final SplitLayout splitLayout;

    private final Grid<T> grid;

    private final Class<R> repository;
    private final Class<T> bean;
    private BeanValidationBinder<T> binder;

    public AutoGrid(Class<T> bean, Class<R> repository) {
        this.bean = bean;
        this.repository = repository;
        this.splitLayout = new SplitLayout();
        this.splitLayout.setSplitterPosition(80);
        this.grid = new Grid<>(this.bean);
        this.populateData(this.repository);

        this.splitLayout.addToPrimary(this.grid);
        this.createEditorLayout(this.splitLayout);

        this.grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                this.binder.setBean(event.getValue());
            } else {
                this.clearForm();
            }
        });

        this.getContent().add(splitLayout);
        this.getContent().setWidthFull();
        this.addClassNames("master-detail-view");
    }

    private void populateData(Class<R> repository) {
        CrudRepository<T, ID> instance = SpringContext.getBean(repository);
        grid.setItems(StreamSupport.stream(instance.findAll().spliterator(), false).toList());
    }

    private void createEditorLayout(SplitLayout splitLayout) {
        Div editorLayoutDiv = new Div();
        editorLayoutDiv.setClassName("editor-layout");

        Div editorDiv = new Div();
        editorDiv.setClassName("editor");
        editorLayoutDiv.add(editorDiv);

        FormLayout formLayout = new FormLayout();
        this.binder = new BeanValidationBinder<>(this.bean);
        Arrays.stream(this.bean.getDeclaredFields()).forEachOrdered(field -> {
            HasValue<?, ?> fieldComponent = createFieldOfType(field);
            binder.forField(fieldComponent).bind(field.getName());
            formLayout.add((Component) fieldComponent);
        });

        editorDiv.add(formLayout);
        this.createButtonLayout(editorLayoutDiv);
        splitLayout.addToSecondary(editorLayoutDiv);
    }

    private HasValue<?, ?> createFieldOfType(Field field){
        if (field.getType().isAssignableFrom(Boolean.class)
                || field.getType().getTypeName().equals("boolean")){
            return new Checkbox(convertToReadableName(field.getName()));
        }
        else if (field.getType().isAssignableFrom(Integer.class)
                || field.getType().getTypeName().equals("int")){
            return new IntegerField(convertToReadableName(field.getName()));
        }
        else if (field.getType().isAssignableFrom(Number.class)){
            return new NumberField(convertToReadableName(field.getName()));
        }
        else if (field.getType().isAssignableFrom(Date.class)
                || field.getType().isAssignableFrom(LocalDate.class)){
            return new DatePicker(convertToReadableName(field.getName()));
        }
        else if (field.getType().isAssignableFrom(LocalDateTime.class)){
            return new DateTimePicker(convertToReadableName(field.getName()));
        }
        return new TextField(convertToReadableName(field.getName()));
    }

    private static String convertToReadableName(String name){
        return StringUtils.capitalize(StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(name), StringUtils.SPACE));

    }

    private void createButtonLayout(Div editorLayoutDiv) {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setClassName("button-layout");
        Button cancel = new Button("Cancel", event -> clearForm());
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        Button save = new Button("Save", event -> {
            try {
                this.binder.writeBean(this.binder.getBean());
                SpringContext.getBean(this.repository).save(this.binder.getBean());
                clearForm();
                refreshGrid();
                Notification.show("Data updated");
            } catch (ValidationException e) {
                throw new RuntimeException(e);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        buttonLayout.add(save, cancel);
        editorLayoutDiv.add(buttonLayout);
    }

    private void clearForm() {
        this.binder.setBean(null);
        this.binder.refreshFields();
    }

    private void refreshGrid() {
        this.grid.select(null);
        this.grid.getDataProvider().refreshAll();
    }


}
