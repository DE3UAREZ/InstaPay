package eg.com.adcb.IPN.models;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity(name = "RejectedLogs")
public class RejectedLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;

    @Column(name = "RequestId")
    private String requestId;

    @Column(name = "Timestamp")
    private LocalDateTime date;

    @Column(name = "Request")
    private String request;

    @Column(name = "Response")
    private String response;

    @Column(name = "TransactionRefrence")
    private String TransactionRefrence;

    public RejectedLog() {
    }

    public RejectedLog(String requestId, LocalDateTime date, String request, String response,String TransactionRefrence) {
        this.requestId = requestId;
        this.date = date;
        this.request = request;
        this.response = response;
        this.TransactionRefrence=TransactionRefrence;

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getTransactionRefrence() {
        return TransactionRefrence;
    }

    public void setTransactionRefrence(String TransactionRefrence) {
        this.TransactionRefrence = TransactionRefrence;
    }


    @Override
    public String toString() {
        return "RejectedLog{" +
                "id=" + id +
                ", requestId='" + requestId + '\'' +
                ", request='" + request + '\'' +
                ", response='" + response + '\'' +
                ", TransactionRefrence='" + TransactionRefrence + '\'' +

                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RejectedLog that = (RejectedLog) o;

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

}
