# Drill agent runners

This module contains the plugins for build tools(gradle and maven) to easily run [autotest agents](https://github.com/Drill4J/autotest-agent).

## Examples

### Gradle

```groovy
plugins {
    id 'com.epam.drill.agent.runner.autotest'
}
drill {
    def testFramework = System.getProperty("testFramework") ?: ""
    version = System.getProperty("autotestAgentVersion") ?: "0.21.1"
    agentId = System.getProperty("agentId") ?: null
    groupId = System.getProperty("groupId") ?: null
    adminHost = System.getProperty("adminHost") ?: "localhost"
    adminPort = System.getProperty("adminPort") ?: 8090
    logLevel = com.epam.drill.agent.runner.LogLevels.TRACE
    additionalParams = System.getProperty("withProxy") == "true" ?
            [
                  "rawFrameworkPlugins": "$testFramework"
            ] : ["rawFrameworkPlugins": "$testFramework",
                 "devToolsProxyAddress": "http://$adminHost:8093"]
}
```
### Maven

```xml
<project>
    <properties>
        <drillAgentRunner.version>0.3.3</drillAgentRunner.version>
        <drillAgentPlugin.version>0.21.1</drillAgentPlugin.version>
    </properties>

    <profiles>
        <profile>
            <id>drillStable</id>
            <activation>
                <activeByDefault>false</activeByDefault>
            </activation>
            <pluginRepositories>
                <pluginRepository>
                    <id>drill</id>
                    <url>https://drill4j.jfrog.io/artifactory/drill</url>
                </pluginRepository>
            </pluginRepositories>
            <build>
                <plugins>
                    <plugin>
                        <groupId>com.epam.drill.agent.runner</groupId>
                        <artifactId>maven</artifactId>
                        <version>${drillAgentRunner.version}</version>
                        <executions>
                            <execution>
                                <goals>
                                    <goal>autotest</goal>
                                </goals>
                            </execution>
                        </executions>
                        <configuration>
                            <drill>
                                <version>${drillAgentPlugin.version}</version>
                                <agentId>Petclinic</agentId>
                                <adminHost>http://localhost</adminHost>
                                <adminPort>8090</adminPort>
                                <logLevel>TRACE</logLevel>
                                <logFile>target/drillLog.log</logFile>
                            </drill>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
        </profile>
    </profiles>
</project>
```
