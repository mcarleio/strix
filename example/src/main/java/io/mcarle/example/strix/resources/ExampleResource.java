package io.mcarle.example.strix.resources;

import io.mcarle.example.strix.db.Author;
import io.mcarle.example.strix.db.AuthorRepository;
import io.mcarle.example.strix.db.Book;
import io.mcarle.example.strix.db.BookRepository;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

@Path("/")
public class ExampleResource {

    private final BookRepository bookRepository = new BookRepository();
    private final AuthorRepository authorRepository = new AuthorRepository();
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    @GET
    @Path("book")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Book> listBooks() {
        return bookRepository.list();
    }

    @GET
    @Path("book/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Book findBook(@PathParam("name") String name) {
        return bookRepository.byName(name);
    }

    @POST
    @Path("book")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Book saveBook(
          @FormParam("name") String name,
          @FormParam("pages") int pages,
          @FormParam("author") String author
    ) {
        Book book = new Book();
        book.setName(name);
        book.setPages(pages);
        book.setAuthor(authorRepository.byName(author));
        return bookRepository.save(book);
    }

    @GET
    @Path("author")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Author> listAuthors() {
        return authorRepository.list();
    }

    @GET
    @Path("author/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Author findAuthor(@PathParam("name") String name) {
        return authorRepository.byName(name);
    }

    @POST
    @Path("author")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Author saveAuthor(
          @FormParam("name") String name,
          @FormParam("birthday") String birthday
    ) throws ParseException {
        Author author = new Author();
        author.setName(name);
        if (birthday != null) {
            author.setBirthday(sdf.parse(birthday));
        }
        return authorRepository.save(author);
    }
}
