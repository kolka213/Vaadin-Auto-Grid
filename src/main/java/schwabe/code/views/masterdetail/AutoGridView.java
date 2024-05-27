package schwabe.code.views.masterdetail;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import schwabe.code.data.SamplePerson;
import schwabe.code.data.SamplePersonRepository;

@Route("auto")
@RouteAlias(value = "")
public class AutoGridView extends Div {


    public AutoGridView() {
        setSizeFull();
        add(new AutoGrid<>(SamplePerson.class, SamplePersonRepository.class));
    }
}
