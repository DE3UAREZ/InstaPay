package eg.com.adcb.IPN.models;

import org.hibernate.annotations.Immutable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Entity(name = "all_logs")
@Immutable
public class AllLogs {
    @Id
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

    @Column(name = "status")
    private String status;

    @Column(name = "TransactionRefrence")
    private String TransactionRefrence;


    public String getTransactionRefrence() {
        return TransactionRefrence;
    }

    public void setTransactionRefrence(String TransactionRefrence) {
        this.TransactionRefrence = TransactionRefrence;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getRequestId() {
        return requestId;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public String getRequest() {
        return request;
    }

    public String getResponse() {
        return response;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    @Override
    public String toString() {
        return "AllLogs{" +
                "id=" + id +
                ", requestId='" + requestId + '\'' +
                ", date=" + date +
                ", request='" + request + '\'' +
                ", response='" + response + '\'' +
                ", status='" + status + '\'' +
                ", TransactionRefrence='" + TransactionRefrence + '\'' +

                '}';
    }
}
