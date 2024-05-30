package schwabe.code.views.masterdetail;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import schwabe.code.AutoGrid;
import schwabe.code.CapitalizeHelper;
import schwabe.code.data.SamplePerson;
import schwabe.code.data.SamplePersonRepository;
import schwabe.code.services.SamplePersonService;

@Route("auto")
@RouteAlias(value = "")
public class AutoGridView extends Div {

    private final SamplePersonService personService;


    public AutoGridView(SamplePersonService personService) {
        this.personService = personService;
        setSizeFull();
        var autoGrid = new AutoGrid<>(SamplePerson.class, SamplePersonRepository.class);
        autoGrid.addCollectionColumnField(
                "roles",
                AutoGrid.CollectionComponentType.BADGE,
                role -> CapitalizeHelper.convertToReadableName(String.valueOf(role)));

        add(autoGrid);
    }
}
