package io.mcarle.strix.entity;

import javax.persistence.*;
import java.io.Serializable;

@Entity
public class TestEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Version
    private Integer version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

}
