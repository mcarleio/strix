package io.mcarle.example.strix.db;

import io.mcarle.strix.annotation.Transactional;

import java.util.List;

import static io.mcarle.strix.Strix.em;

@Transactional
public class BookRepository {

    public Book save(Book book) {
        return em().merge(book);
    }

    public long count() {
        return em().createQuery("SELECT count(*) FROM Book", Long.class).getSingleResult();
    }

    public Book byName(String name) {
        List<Book> list = em().createQuery("SELECT b FROM Book b WHERE b.name = :name", Book.class)
              .setParameter("name", name)
              .getResultList();
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Book> list() {
        return em().createQuery("SELECT b FROM Book b", Book.class).getResultList();
    }

    public List<Book> listByAuthorName(String authorName) {
        return em().createQuery("SELECT b FROM Book b WHERE b.author.name = :name", Book.class)
              .setParameter("name", authorName)
              .getResultList();
    }

    public Book byId(long id) {
        return em().find(Book.class, id);
    }
}
