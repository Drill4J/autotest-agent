This library is copied from [lib-jvm-shared/logging](..%2Flib-jvm-shared%2Flogging), 
with the package changed to `com.epam.drill.autotest.logging` 
instead of the original `com.epam.drill.logging`. 
Changing the package resolves a class conflict issue with 
[LoggingConfiguration.kt](src%2FcommonMain%2Fkotlin%2Fcom%2Fepam%2Fdrill%2Fautotest%2Flogging%2FLoggingConfiguration.kt),
which might also be used by another agent, like the Application Agent, that also uses `lib-jvm-shared`.

Because the `LoggingConfiguration` objects are identical for both agents, only one instance will be initialized. 
So, the second agent will be unable to use its own logging settings,
and will instead use the default configuration (`logback` uses DEBUG level by default).

As a result, using the relocated package allows each agent running in the same application 
to have independent logging configurations.