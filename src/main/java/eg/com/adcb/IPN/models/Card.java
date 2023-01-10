package eg.com.adcb.IPN.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.Objects;

@Entity(name = "Cards")
public class Card {

    @Column(name = "Id")
    @Id
    private Long id;

    @Column(name = "Title")
    private String title;

    @Column(name = "Issuer")
    private String issuer;

    @Column(name = "StartsWith")
    private String startsWith;

    public Card() {
    }

    public Card(String title, String issuer, String startsWith) {
        this.title = title;
        this.issuer = issuer;
        this.startsWith = startsWith;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getStartsWith() {
        return startsWith;
    }

    public void setStartsWith(String startsWith) {
        this.startsWith = startsWith;
    }

    @Override
    public String toString() {
        return "Card{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", issuer='" + issuer + '\'' +
                ", startsWith='" + startsWith + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Card card = (Card) o;

        return Objects.equals(id, card.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
