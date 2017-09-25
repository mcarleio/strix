# Strix example

This is an example project presenting strix in a simple non JEE project.

1. Execute `mvn jetty:run`
2. Create some authors
    ```bash
    curl -d "name=George R. R. Martin&birthday=1948-09-20" -H"Content-Type: application/x-www-form-urlencoded" http://localhost:8080/author
    curl -d "name=Joanne K. Rowling&birthday=1965-07-31" -H"Content-Type: application/x-www-form-urlencoded" http://localhost:8080/author
    ```
3. Create some books
    ```bash
    curl -d "name=A Game of Thrones - A Song of Ice and Fire&pages=694&author=George R. R. Martin" -H"Content-Type: application/x-www-form-urlencoded" http://localhost:8080/book
    curl -d "name=Harry Potter and the Philosopher's Stone&pages=309&author=Joanne K. Rowling" -H"Content-Type: application/x-www-form-urlencoded" http://localhost:8080/book
    ```
4. Query as you like
    ```bash
    # All books
    curl http://localhost:8080/book
    
    # All authors
    curl http://localhost:8080/author
    ```