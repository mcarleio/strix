package io.mcarle.example.strix;

import io.mcarle.strix.Strix;
import org.glassfish.jersey.servlet.ServletContainer;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;

@WebServlet(urlPatterns = "/*", name = "ExampleServlet", loadOnStartup = 1, initParams = {
      @WebInitParam(name = "jersey.config.server.provider.packages", value = "io.mcarle.example.strix.resources")
})
public class ExampleServlet extends ServletContainer {

    @Override
    public void destroy() {
        super.destroy();
        Strix.shutdown();
    }

    @Override
    public void init() throws ServletException {
        Strix.startup();
        super.init();
    }
}
