package schwabe.code.views.masterdetail;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Setter;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import org.springframework.data.domain.Pageable;
import schwabe.code.data.Role;
import schwabe.code.data.SamplePerson;
import schwabe.code.data.SamplePersonRepository;
import schwabe.code.services.SamplePersonService;

import java.util.Optional;

@Route("auto")
@RouteAlias(value = "")
public class AutoGridView extends Div {

    private final SamplePersonService personService;


    public AutoGridView(SamplePersonService personService) {
        this.personService = personService;
        setSizeFull();
        add(new AutoGrid<>(SamplePerson.class, SamplePersonRepository.class));
        SamplePerson samplePerson = this.personService.list(Pageable.ofSize(this.personService.count())).getContent().getFirst();
        ComboBox<Role> samplePersonComboBox = new ComboBox<>("ComboBox");

        Binder<SamplePerson> personBinder = new Binder<>();
        samplePersonComboBox.setItems(samplePerson.getRoles());

        personBinder.readBean(samplePerson);
        this.add(samplePersonComboBox);
    }
}
