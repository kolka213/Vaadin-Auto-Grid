package schwabe.code.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import schwabe.code.data.Role;
import schwabe.code.data.SamplePerson;
import schwabe.code.data.SamplePersonRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class SamplePersonService {

    private final SamplePersonRepository repository;


    public SamplePersonService(SamplePersonRepository repository) {
        this.repository = repository;
        SamplePerson person1 = new SamplePerson("String firstName", "String lastName", "email@email.com", "019191", LocalDate.now(), "occupation", List.of(Role.MANAGER, Role.SUPERVISOR, Role.WORKER, Role.EXTERNAL), true);
        SamplePerson person2 = new SamplePerson("Pascal", "Schwabe", "email@email.com", "019191", LocalDate.now(), "occupation", List.of(Role.EXTERNAL, Role.WORKER, Role.SUPERVISOR, Role.MANAGER), false);
        this.repository.saveAll(List.of(person1, person2));
    }

    public Optional<SamplePerson> get(Long id) {
        return repository.findById(id);
    }

    public SamplePerson update(SamplePerson entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<SamplePerson> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<SamplePerson> list(Pageable pageable, Specification<SamplePerson> filter) {
        return repository.findAll(filter, pageable);
    }

    public int count() {
        return (int) repository.count();
    }

}
