package eg.com.adcb.IPN.models;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;


@Entity(name = "Errors")
public class Errors {
    @Column(name = "ID")
    @Id
    private Long id;

    @Column(name = "ErrorCode")
    private String ErrorCode;

    @Column(name = "ErrorDesc")
    private String ErrorDesc;

    @Column(name = "ReturnedDesc")
    private String ReturnedDesc;

    @Column(name = "ReturnedCode")
    private String ReturnedCode;

    @Column(name = "Notes")
    private String Notes;

    public Errors(Long id, String errorCode, String errorDesc, String returnedDesc, String returnedCode, String notes) {
        this.id = id;
        ErrorCode = errorCode;
        ErrorDesc = errorDesc;
        ReturnedDesc = returnedDesc;
        ReturnedCode = returnedCode;
        Notes = notes;
    }

    public Errors() {

    }

    public Long getId() {
        return id;
    }

    public String getErrorCode() {
        return ErrorCode;
    }

    public String getErrorDesc() {
        return ErrorDesc;
    }

    public String getReturnedDesc() {
        return ReturnedDesc;
    }

    public String getReturnedCode() {
        return ReturnedCode;
    }

    public String getNotes() {
        return Notes;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setErrorCode(String errorCode) {
        ErrorCode = errorCode;
    }

    public void setErrorDesc(String errorDesc) {
        ErrorDesc = errorDesc;
    }

    public void setReturnedDesc(String returnedDesc) {
        ReturnedDesc = returnedDesc;
    }

    public void setReturnedCode(String returnedCode) {
        ReturnedCode = returnedCode;
    }

    public void setNotes(String notes) {
        Notes = notes;
    }

    @Override
    public String toString() {
        return "Errors{" +
                "id=" + id +
                ", ErrorCode='" + ErrorCode + '\'' +
                ", ErrorDesc='" + ErrorDesc + '\'' +
                ", ReturnedDesc='" + ReturnedDesc + '\'' +
                ", ReturnedCode='" + ReturnedCode + '\'' +
                ", Notes='" + Notes + '\'' +
                '}';
    }
}
