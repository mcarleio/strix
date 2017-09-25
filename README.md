# strix
A lightweight and simple transaction library for non JEE projects.

## Simple Usage

You can find a simple example project under [example](./example/). In short terms you have to:
1. Include strix as dependency
    ```xml
    <dependency>
        <groupId>io.mcarle</groupId>
        <artifactId>strix</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
    ```
    
2. Include aspectj-maven-plugin and define strix as `aspectLibrary`
    ```xml
    <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>aspectj-maven-plugin</artifactId>
        <version>1.10</version>
        <executions>
            <execution>
                <goals>
                    <goal>compile</goal>
                    <goal>test-compile</goal>
                </goals>
            </execution>
        </executions>
        <configuration>
            <complianceLevel>${maven.compiler.source}</complianceLevel>
            <source>${maven.compiler.source}</source>
            <target>${maven.compiler.target}</target>
            <aspectLibraries>
                <aspectLibrary>
                    <groupId>io.mcarle</groupId>
                    <artifactId>strix</artifactId>
                </aspectLibrary>
            </aspectLibraries>
        </configuration>
    </plugin>
    ```

3. Start strix before you need call any transactional methods 
    ```java
    import io.mcarle.strix.Strix;
    import org.glassfish.jersey.servlet.ServletContainer;

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
    ```

4. Annotate your classes or methods, which should be transactional with strix's [`@Transactional`](./src/main/java/io/mcarle/strix/annotation/Transactional.java) like
    ```java
    import io.mcarle.strix.annotation.Transactional;
    import static io.mcarle.strix.Strix.em;
 
    @Transactional
    public class ExampleManager {
       public long find(Class entityClass, Long id) {
            return em().find(entityClass, id);
       }
    }
    ``` 

## License

Unless explicitly stated otherwise all files in this repository are licensed under the Apache Software License 2.0

Copyright 2017 Marcel Carlé

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.