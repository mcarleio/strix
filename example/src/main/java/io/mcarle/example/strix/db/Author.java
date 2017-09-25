package io.mcarle.example.strix.db;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
public class Author implements Serializable {

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true)
    @Basic(optional = false)
    private String name;

    @Basic
    @Temporal(TemporalType.DATE)
    private Date birthday;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }
}
