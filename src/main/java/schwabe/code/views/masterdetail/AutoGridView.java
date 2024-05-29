package schwabe.code.views.masterdetail;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import schwabe.code.data.SamplePerson;
import schwabe.code.data.SamplePersonRepository;
import schwabe.code.services.SamplePersonService;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.Locale;

@Route("auto")
@RouteAlias(value = "")
public class AutoGridView extends Div {

    private final SamplePersonService personService;


    public AutoGridView(SamplePersonService personService) throws IntrospectionException {
        this.personService = personService;
        setSizeFull();
        var autoGrid = new AutoGrid<>(SamplePerson.class, SamplePersonRepository.class);
        autoGrid.addFieldComponentType(
                new PropertyDescriptor("roles", SamplePerson.class),
                AutoGrid.CollectionComponentType.BADGE,
                role -> role.toString().toLowerCase(Locale.GERMAN));

        add(autoGrid);
    }
}
