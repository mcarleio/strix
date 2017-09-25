package io.mcarle.example.strix.db;

import io.mcarle.strix.annotation.Transactional;

import java.util.List;

import static io.mcarle.strix.Strix.em;

@Transactional
public class AuthorRepository {

    public Author save(Author author) {
        return em().merge(author);
    }

    public long count() {
        return em().createQuery("SELECT count(*) FROM Author ", Long.class).getSingleResult();
    }

    public Author byName(String name) {
        List<Author> list = em().createQuery("SELECT a FROM Author a WHERE a.name = :name", Author.class)
              .setParameter("name", name)
              .getResultList();
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Author> list() {
        return em().createQuery("SELECT a FROM Author a", Author.class).getResultList();
    }

    public Author byId(long id) {
        return em().find(Author.class, id);
    }
}
