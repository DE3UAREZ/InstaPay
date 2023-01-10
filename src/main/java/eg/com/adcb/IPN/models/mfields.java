package eg.com.adcb.IPN.models;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity(name = "mfields")
public class mfields {
    @Column(name = "id")
    @Id
    private int id;
    @Column(name = "field_req")
    private String field_req;
    @Column(name = "field_name")
    private String field_name;

    public int getId() {
        return id;
    }

    public String getField_req() {
        return field_req;
    }

    public String getField_name() {
        return field_name;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setField_req(String field_req) {
        this.field_req = field_req;
    }

    public void setField_name(String field_name) {
        this.field_name = field_name;
    }

    @Override
    public String toString() {
        return "mfields{" +
                "id=" + id +
                ", field_req='" + field_req + '\'' +
                ", field_name='" + field_name + '\'' +
                '}';
    }
}
