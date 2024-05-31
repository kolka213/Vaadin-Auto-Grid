package schwabe.code;

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
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.data.repository.CrudRepository;
import schwabe.code.services.util.CapitalizeHelper;
import schwabe.code.services.util.SpringContext;

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


/**
 * Creates a {@link Grid} and populates data from the given repository.
 * The provided repository needs to be extending {@link CrudRepository}.
 *
 * <p>Currently supports only JPA Repositories.</p>
 * @param <T> The java bean type, also known as Source
 * @param <ID> The type of auto generated entity id, e.g {@link Long}
 * @param <R> The class type of the repository
 */
public class AutoGrid<T, ID, R extends CrudRepository<T, ID>> extends Composite<Div> {

    private BeanValidationBinder<T> binder;
    private FormLayout formLayout;

    private final Grid<T> grid;
    private final Class<R> repository;
    private final Class<T> bean;
    private final Map<Field, HasListDataView<String, AbstractListDataView<String>>> fieldComponentMap;
    private final Map<Field, ItemLabelGenerator<Object>> fieldItemLabelGeneratorMap;
    private CollectionComponentType displayType = CollectionComponentType.COMBOBOX;

    /**
     * Basic constructor for creating an instance of Auto-Grid.
     * @param bean the java bean
     * @param repository the {@link CrudRepository} class
     */
    public AutoGrid(Class<T> bean, Class<R> repository) {
        this.bean = bean;
        this.repository = repository;
        this.grid = new Grid<>(this.bean);
        this.orderColumnsByEntity();
        this.grid.getColumns().forEach(tColumn -> tColumn.setAutoWidth(true));
        this.fieldComponentMap = new HashMap<>();
        this.fieldItemLabelGeneratorMap = new HashMap<>();
        var splitLayout = new SplitLayout();
        splitLayout.setSplitterPosition(80);
        splitLayout.addToPrimary(this.grid);

        this.populateData(this.repository);
        this.createEditorLayout(splitLayout);

        this.grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                this.binder.setBean(event.getValue());
                this.repopulateListFieldComponents(this.fieldComponentMap, this.formLayout, event.getValue());
            } else {
                this.clearForm();
            }
        });

        this.getContent().add(splitLayout);
        this.getContent().setWidthFull();
        this.addClassNames("master-detail-view");
    }


    /**
     * Returns the display type of the collection fields.
     * <p>See {@link CollectionComponentType} for available options.</p>
     * @return
     */
    public CollectionComponentType getDisplayType() {
        return displayType;
    }

    /**
     * Get the instance of the underlying {@link Grid}.
     * @return the instance of Grid.
     */
    public Grid<T> getGrid() {
        return grid;
    }

    /**
     * Set a {@link Renderer} for a specific {@link com.vaadin.flow.component.grid.Grid.Column}.
     * @param property the field name of the source class
     * @param renderer the component renderer
     * @return a column instance for further chaining
     */
    public Grid.Column<T> setRendererForColumn(String property, Renderer<T> renderer) {
        return this.grid.getColumnByKey(property).setRenderer(renderer);
    }


    /**
     * Reorder columns based on the field declaration order of the superclass, then actual entity.
     */
    private void orderColumnsByEntity() {
        var declaredFields = Arrays.stream(this.bean.getDeclaredFields()).map(Field::getName).toList();

        // All columns from superclass
        var columnsNotFromEntity = this.grid.getColumns().stream().filter(column -> !declaredFields.contains(column.getKey()));

        // All columns from actual class
        var columnsFromEntity = this.grid.getColumns().stream().filter(column -> declaredFields.contains(column.getKey()));

        this.grid.setColumnOrder(Stream.concat(columnsNotFromEntity, columnsFromEntity).toList());
    }

    /**
     * Iterate over the collection type fields from the entity class and repopulate each field from the given object.
     * @param fieldHasValueMap the map containing the {@link Field}->{@link Component} mapping
     * @param formLayout layout component
     * @param object bean object
     */
    @SuppressWarnings("unchecked")
    private void repopulateListFieldComponents(Map<Field, HasListDataView<String, AbstractListDataView<String>>> fieldHasValueMap, FormLayout formLayout, T object) {
        fieldHasValueMap.forEach((key, value) -> {
            formLayout.getChildren().filter(component -> component.equals(value)).findFirst().ifPresent(component -> {
                try {
                    var val = (Collection<?>) new PropertyDescriptor(key.getName(), this.bean).getReadMethod().invoke(object);
                    val = val.stream().map(item -> fieldItemLabelGeneratorMap.getOrDefault(key, String::valueOf).apply(item)).toList();
                    ((HasListDataView<String, AbstractListDataView<String>>) component).setItems((Collection<String>) val);
                } catch (IllegalAccessException | IntrospectionException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }

    /**
     * Get the spring bean of the given repository class and call {@link CrudRepository#findAll() findAll()}
     * to populate the {@link Grid}.
     * @param repository class definition
     */
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
                this.fieldComponentMap.put(field, (HasListDataView<String, AbstractListDataView<String>>) fieldComponent);
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
            return new Checkbox(CapitalizeHelper.convertCamelCaseToReadableName(field.getName()));
        } else if (field.getType().isAssignableFrom(Integer.class)
                || field.getType().getTypeName().equals("int")) {
            return new IntegerField(CapitalizeHelper.convertCamelCaseToReadableName(field.getName()));
        } else if (field.getType().isAssignableFrom(Number.class)) {
            return new NumberField(CapitalizeHelper.convertCamelCaseToReadableName(field.getName()));
        } else if (field.getType().isAssignableFrom(Date.class)
                || field.getType().isAssignableFrom(LocalDate.class)) {
            return new DatePicker(CapitalizeHelper.convertCamelCaseToReadableName(field.getName()));
        } else if (field.getType().isAssignableFrom(LocalDateTime.class)) {
            return new DateTimePicker(CapitalizeHelper.convertCamelCaseToReadableName(field.getName()));
        } else if (Collection.class.isAssignableFrom(field.getType())) {
            return new ComboBox<>(CapitalizeHelper.convertCamelCaseToReadableName(field.getName()));
        }
        return new TextField(CapitalizeHelper.convertCamelCaseToReadableName(field.getName()));
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
        this.fieldComponentMap.values().forEach(HasListDataView::setItems);
    }

    private void refreshGrid() {
        this.grid.select(null);
        this.grid.getDataProvider().refreshAll();
    }

    /**
     * Create a field renderer for a collection field of the entity class.
     * Default is {@link CollectionComponentType#COMBOBOX} and creates a {@link ComboBox}
     * and applies the {@link ItemLabelGenerator} to its items.
     * @param property field name of the class
     * @param displayType see {@link CollectionComponentType} for available options
     * @param itemLabelGenerator the item label generator for the contents
     * @return a {@link com.vaadin.flow.component.grid.Grid.Column} instance
     */
    public Grid.Column<T> setCollectionFieldRenderer(String property, CollectionComponentType displayType, ItemLabelGenerator<Object> itemLabelGenerator) {
        this.displayType = displayType;
        var optField = fieldComponentMap.keySet().stream().filter(key -> key.getName().equals(property)).findFirst();
        if (optField.isPresent()) {
            var field = optField.get();
                Grid.Column<T> column;
                if (!displayType.equals(CollectionComponentType.COMBOBOX)) {
                    int index = this.formLayout.getChildren().toList().indexOf((Component) fieldComponentMap.get(field));
                    this.formLayout.remove((Component) this.fieldComponentMap.get(field));
                    var badgeListComponent = new BadgeListComponent(CapitalizeHelper.convertCamelCaseToReadableName(field.getName()), displayType);
                    badgeListComponent.setFlexWrap(FlexLayout.FlexWrap.WRAP);
                    this.fieldComponentMap.replace(field, this.fieldComponentMap.get(field), badgeListComponent);
                    this.formLayout.addComponentAtIndex(index, (Component) fieldComponentMap.get(field));
                }
                this.fieldItemLabelGeneratorMap.put(field, itemLabelGenerator);
                column = this.setRendererForColumn(property, new ComponentRenderer<>((ValueProvider<T, Component>) item -> {
                    Component fieldOfType;
                    if (this.displayType.equals(CollectionComponentType.COMBOBOX)) {
                        fieldOfType = (Component) createFieldOfType(field);
                        fieldOfType.getElement().setProperty("label", "");
                        fieldOfType.getElement().setAttribute("theme", "small");
                    } else {
                        fieldOfType = new BadgeListComponent("", displayType);
                        fieldOfType.setClassName(LumoUtility.Overflow.SCROLL);
                        fieldOfType.getStyle().setWidth("200px");
                    }
                    try {
                        var val = (Collection<?>) new PropertyDescriptor(property, this.bean).getReadMethod().invoke(item);
                        val = val.stream().map(i -> fieldItemLabelGeneratorMap.getOrDefault(field, String::valueOf).apply(i)).toList();
                        //noinspection unchecked
                        ((HasListDataView<String, AbstractListDataView<String>>) fieldOfType).setItems((Collection<String>) val);
                        return (Component) fieldOfType;
                    } catch (IllegalAccessException | IntrospectionException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }));
                return column;
        }
        return null;
    }

    /**
     * Creates a list of {@link Span} component styled as badges.
     */
    public static class BadgeListComponent extends Div implements HasListDataView<String, AbstractListDataView<String>> {

        private final String classNames;
        private final FlexLayout layout;

        private Collection<String> items = new ArrayList<>();

        private final AtomicReference<DataProvider<String, ?>> dataProvider = new AtomicReference<>();


        public BadgeListComponent(String label, CollectionComponentType componentType) {
            this.classNames = componentType.getType();
            this.layout = new FlexLayout();
            this.layout.addClassNames(LumoUtility.Gap.SMALL);
            this.addClassNames(LumoUtility.Gap.SMALL);
            this.setSizeUndefined();
            this.getElement().appendChild(ElementFactory.createLabel(label));
            this.add(this.layout);
        }

        /**
         * Sets the {@link com.vaadin.flow.component.orderedlayout.FlexLayout.FlexWrap} behaviour of the badge list container.
         * @param flexWrap
         */
        public void setFlexWrap(FlexLayout.FlexWrap flexWrap){
            this.layout.setFlexWrap(flexWrap);
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

        /**
         * Returns the {@link DataProvider} instance.
         * @return the instance
         */
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
