package schwabe.code.data;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import java.time.LocalDate;
import java.util.List;

@Entity(name = "TABLE_PERSON")
@Table(name = "TABLE_PERSON")
public class SamplePerson extends AbstractEntity {

    private String firstName;
    private String lastName;
    @Email
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String occupation;
    @Column(name = "ROLE")
    @ElementCollection
    @Enumerated(EnumType.STRING)
    private List<Role> role;
    private boolean important;

    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    public String getLastName() {
        return lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }
    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }
    public String getOccupation() {
        return occupation;
    }
    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }
    public List<Role> getRole() {
        return role;
    }
    public void setRole(List<Role> role) {
        this.role = role;
    }
    public boolean isImportant() {
        return important;
    }
    public void setImportant(boolean important) {
        this.important = important;
    }

}
