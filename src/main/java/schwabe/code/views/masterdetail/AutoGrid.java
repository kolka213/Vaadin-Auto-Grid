package schwabe.code.views.masterdetail;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.provider.*;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.repository.CrudRepository;
import schwabe.code.services.SpringContext;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class AutoGrid<T, ID, R extends CrudRepository<T, ID>> extends Composite<Div> {

    private BeanValidationBinder<T> binder;
    private FormLayout formLayout;

    private final Grid<T> grid;
    private final Class<R> repository;
    private final Class<T> bean;
    private final Map<Field, HasListDataView<String, AbstractListDataView<String>>> fieldHasValueMap;
    private final Map<Field, ItemLabelGenerator<Object>> fieldItemLabelGeneratorMap;
    private CollectionComponentType displayType = CollectionComponentType.COMBOBOX;

    public AutoGrid(Class<T> bean, Class<R> repository) {
        this.bean = bean;
        this.repository = repository;
        this.grid = new Grid<>(this.bean);
        this.orderColumnsByEntity();
        this.grid.getColumns().forEach(tColumn -> tColumn.setAutoWidth(true));
        this.fieldHasValueMap = new HashMap<>();
        this.fieldItemLabelGeneratorMap = new HashMap<>();
        var splitLayout = new SplitLayout();
        splitLayout.setSplitterPosition(80);
        splitLayout.addToPrimary(this.grid);

        this.populateData(this.repository);
        this.createEditorLayout(splitLayout);

        this.grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                this.binder.setBean(event.getValue());
                this.repopulateListFieldComponents(this.fieldHasValueMap, this.formLayout, event.getValue());
            } else {
                this.clearForm();
            }
        });

        this.getContent().add(splitLayout);
        this.getContent().setWidthFull();
        this.addClassNames("master-detail-view");
    }

    private void orderColumnsByEntity(){
        var declaredFields = Arrays.stream(this.bean.getDeclaredFields()).map(Field::getName).toList();
        var columnsNotFromEntity = this.grid.getColumns().stream().filter(column -> !declaredFields.contains(column.getKey()));
        var columnsFromEntity = this.grid.getColumns().stream().filter(column -> declaredFields.contains(column.getKey()));

        this.grid.setColumnOrder(Stream.concat(columnsNotFromEntity, columnsFromEntity).toList());
    }

    @SuppressWarnings("unchecked")
    private void repopulateListFieldComponents(Map<Field, HasListDataView<String, AbstractListDataView<String>>> fieldHasValueMap, FormLayout formLayout, T object) {
        fieldHasValueMap.forEach((key, value) -> {
            formLayout.getChildren().filter(component -> component.equals(value)).findFirst().ifPresent(component -> {
                try {
                    var val = (Collection<?>) new PropertyDescriptor(key.getName(), this.bean).getReadMethod().invoke(object);
                    val = val.stream().map(item -> fieldItemLabelGeneratorMap.getOrDefault(key, String::valueOf).apply(item)).toList();

                    //noinspection rawtypes
                    ((HasListDataView) component).setItems(val);
                } catch (IllegalAccessException | IntrospectionException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }

    private void populateData(Class<R> repository) {
        var instance = SpringContext.getBean(repository);
        this.grid.setItems(StreamSupport.stream(instance.findAll().spliterator(), false).toList());
    }

    private void createEditorLayout(SplitLayout splitLayout) {
        var editorLayoutDiv = new Div();
        editorLayoutDiv.setClassName("editor-layout");

        var editorDiv = new Div();
        editorDiv.setClassName("editor");
        editorLayoutDiv.add(editorDiv);

        this.formLayout = new FormLayout();
        this.binder = new BeanValidationBinder<>(this.bean);
        this.createFieldsFromBeanFields();

        editorDiv.add(this.formLayout);
        this.createButtonLayout(editorLayoutDiv);
        splitLayout.addToSecondary(editorLayoutDiv);
    }

    private void createFieldsFromBeanFields() {
        Arrays.stream(this.bean.getDeclaredFields()).forEachOrdered(field -> {
            var fieldComponent = createFieldOfType(field);
            if (Collection.class.isAssignableFrom(field.getType())) {
                //noinspection unchecked
                this.fieldHasValueMap.put(field, (HasListDataView<String, AbstractListDataView<String>>) fieldComponent);
                this.formLayout.add((Component) fieldComponent);
            } else {
                binder.forField(fieldComponent).bind(field.getName());
                this.formLayout.add((Component) fieldComponent);
            }
        });
    }

    private HasValue<?, ?> createFieldOfType(Field field) {
        if (field.getType().isAssignableFrom(Boolean.class)
                || field.getType().getTypeName().equals("boolean")) {
            return new Checkbox(convertToReadableName(field.getName()));
        } else if (field.getType().isAssignableFrom(Integer.class)
                || field.getType().getTypeName().equals("int")) {
            return new IntegerField(convertToReadableName(field.getName()));
        } else if (field.getType().isAssignableFrom(Number.class)) {
            return new NumberField(convertToReadableName(field.getName()));
        } else if (field.getType().isAssignableFrom(Date.class)
                || field.getType().isAssignableFrom(LocalDate.class)) {
            return new DatePicker(convertToReadableName(field.getName()));
        } else if (field.getType().isAssignableFrom(LocalDateTime.class)) {
            return new DateTimePicker(convertToReadableName(field.getName()));
        } else if (Collection.class.isAssignableFrom(field.getType())) {
            return new ComboBox<>(convertToReadableName(field.getName()));
        }
        return new TextField(convertToReadableName(field.getName()));
    }

    private static String convertToReadableName(String name) {
        return StringUtils.capitalize(StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(name), StringUtils.SPACE));

    }

    private void createButtonLayout(Div editorLayoutDiv) {
        var buttonLayout = new HorizontalLayout();
        buttonLayout.setClassName("button-layout");
        var cancel = new Button("Cancel", event -> clearForm());
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        var save = new Button("Save", event -> {
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
        this.fieldHasValueMap.values().forEach(HasListDataView::setItems);
    }

    private void refreshGrid() {
        this.grid.select(null);
        this.grid.getDataProvider().refreshAll();
    }

    public void addFieldComponentType(PropertyDescriptor property, CollectionComponentType displayType, ItemLabelGenerator<Object> itemLabelGenerator) {
        this.displayType = displayType;
        var optField = fieldHasValueMap.keySet().stream().filter(key -> key.getName().equals(property.getName())).findFirst();
        optField.ifPresent(field -> {
            if (!displayType.equals(CollectionComponentType.COMBOBOX)) {
                int index = this.formLayout.getChildren().toList().indexOf((Component) fieldHasValueMap.get(optField.get()));
                this.formLayout.remove((Component) fieldHasValueMap.get(optField.get()));
                fieldHasValueMap.replace(field, fieldHasValueMap.get(optField.get()), new BadgeListComponent(convertToReadableName(field.getName()), displayType.getType()));
                this.formLayout.addComponentAtIndex(index, (Component) fieldHasValueMap.get(optField.get()));
            }
            this.fieldItemLabelGeneratorMap.put(optField.get(), itemLabelGenerator);
        });
    }

    public static class BadgeListComponent extends Div implements HasListDataView<String, AbstractListDataView<String>> {

        private final String classNames;
        private final FlexLayout layout;

        private Collection<String> items = new ArrayList<>();

        private final AtomicReference<DataProvider<String, ?>> dataProvider = new AtomicReference<>();


        public BadgeListComponent(String label, String classNames) {
            this.classNames = classNames;
            this.layout = new FlexLayout();
            this.layout.setFlexDirection(FlexLayout.FlexDirection.ROW);
            this.layout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
            this.layout.addClassName(LumoUtility.Gap.SMALL);
            this.setWidthFull();
            this.addClassName(LumoUtility.Gap.SMALL);
            this.getElement().appendChild(ElementFactory.createLabel(label));
            this.add(this.layout);
        }

        @Override
        public AbstractListDataView<String> setItems(ListDataProvider<String> dataProvider) {
            this.dataProvider.set(Objects.requireNonNull(dataProvider));
            this.items = dataProvider.getItems();
            this.rebuild();
            return new AbstractListDataView<>(this::getDataProvider, this, (filter, sorting) -> rebuild()) {
                @Override
                public int getItemCount() {
                    return super.getItemCount();
                }
            };
        }


        private void rebuild() {
            this.layout.removeAll();

            synchronized (dataProvider) {
                final AtomicInteger itemCounter = new AtomicInteger(0);
                //noinspection unchecked
                items = (List<String>) getDataProvider()
                        .fetch(DataViewUtils.getQuery(this))
                        .collect(Collectors.toList());
                items.stream().map(this::createItemComponent).forEach(component -> {
                    this.layout.add(component);
                    itemCounter.incrementAndGet();
                });
            }
        }

        private Component createItemComponent(String item) {
            var span = new Span(item);
            span.getElement().getThemeList().add(this.classNames);
            this.add(span);
            return span;
        }

        public DataProvider<String, ?> getDataProvider() {
            return dataProvider.get();
        }

        @Override
        public AbstractListDataView<String> getListDataView() {
            return null;
        }
    }

    public enum CollectionComponentType {
        COMBOBOX(""),
        BADGE("badge"),
        BADGE_SUCCESS("badge success"),
        BADGE_ERROR("badge error"),
        BADGE_CONTRAST("badge contrast");

        private final String type;

        CollectionComponentType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }
}
