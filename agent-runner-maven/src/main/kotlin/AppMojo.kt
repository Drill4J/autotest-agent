/**
 * Copyright 2020 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import com.epam.drill.agent.runner.AppAgentConfiguration
import com.epam.drill.agent.runner.Configuration
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope

@Mojo(
    name = "app",
    defaultPhase = LifecyclePhase.INITIALIZE, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true
)
class AppMojo : AgentMojo() {

    @Parameter(required = true, property = "drill")
    lateinit var drill: AppAgentConfiguration

    override val config: Configuration
        get() = drill


}