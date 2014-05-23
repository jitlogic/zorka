
Zorka Agent & Zorka Spy
=======================

Zorka is a programmable general purpose monitoring agent for Java applicatios. It features
bytecode instrumentation and is capable of tracing (profiling) production environments.
It is designed to integrate seamlessly your Java applications with popular monitoring systems.

For more information see [Zorka project page](http://zorka.io).


Building Zorka Agent
====================

Instructions for building the agent.

1. Import into Eclipse the following packages:
  * zorka
  * zorka-agent
  * zorka-common
  * zorka-common-test
  * zorka-core
  * zorka-dist
  * zorka-viewer

2. Comment out all zico modules from the main pom.xml

3. Copy the following maven plugin compiler into all pom.xml files:
  ```xml
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>${compiler.plugin.version}</version>
                <configuration>
                    <source>1.6</source>
                    <target>1.6</target>
                    <encoding>UTF-8</encoding>
                </configuration>
            </plugin>
```

4. Configure the zorka's Maven Build with the "install" goal

5. Copy the following files to a server:
  * zorka-dist/target/output/*


