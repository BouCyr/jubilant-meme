# Code guidelines

## Data structures

### Documents

All documents must be regular classes.

They do not use getters and setters, and expose their fields directly as public.

### Transient objects

All transient objects MUST BE java records.

This includes:
 - any complex data received by a controller
 - any complex data returned by a controller, if not a straight documents as found in the DB

## Logging

Logging use logback through slf4j.

Loggers are declared as the first line of the class ; they are defined s :
```java
private static final Logger LOGGER = LoggerFactory.getLogger($CLASS$.class);
```

Log levels are used as:
 - TRACE : not used
 - DEBUG : fine details of an operation, seldom used, only in critical parts of the process
 - INFO  : Main steps of the normal proceed of a process. Every business process should log its start and its end on an INFO level (if not in WARN or ERROR). Main step should be logged as well. Any write (in the DB, on the filesystem) or call to the outside (call of an external WS/Rest Api, etc.) should be logged at INFO level.
 - WARN  : An anomaly happened, or something unexpected that could show a misconfiguration of a client, a non blocking problem in data. Anything that may require overview by a human operator, but does not mean the system is globally down.
 - ERROR : The system did not behave as expected ; the system is misconfigured ; some external resources are down. To be used when the service could not be done ; should require immediate attention by a human operator.

## Code architecture

The code has 4 base packages:
 - config : house any configuration (spring) global to the whole application ; configuration classes, technical beans and so on are in this package
 - in 
 - middle
 - out

### In

Any start of a business process is defined here. Anything that triggers a treatment by the app is defined here.

This can be:
 - incoming HTTP request : The Controllers and RestControllers is defined in a root package in.http
 - file detection : Any mechanism that reads a file when it arrives is defined in a root package in.file
 - scheduled operation : any treatment that should run at fixed interval, or at fixed time is defined here, through a method annotated with @Scheduled. Note that other trgiggers (e.g. file detection) may use @Scheduled to check the triggers ; in this cas the trigger is not deemed to be a scheduled trigger, but an 'incoming file' trigger
 - startup : any code that must be run at the startup of the application is defined in a in.startup root package. This is implemented through a spring bean implementing CommandLineRunner.
 - messaging system : JMS/AMqp listeners is defined in a root "in.messaging" packge. NOTE : the app does not use JMS for the moment
 - async : any async/background task triggered by another process is defined in an 'in.async' root package

Classes in the 'in' package should be thin wrapper.

A trigger MUST execute one and only method.

The method only do basic validity checks on the input, conversion from the input format to the business model, and call one and only one method of a Process.

e.g. a controller can take an Integer fooId: in parameter of its findFoo() method.

It should :
 - log the call
 - check the input is valid (not 0, not negative, etc.)
 - build a FoodId from it
 - pass it to the find(FooId id) of a Process
 - return the output of the Process method

### Out

Any code that makes a call to an external system, performs read or write IO operations is defined here.

This code be:
 - DB operation (read data from a repository, write data). The code will be in 'out.mongo','out.sql', etc. depending on the actual DB used.
 - Any webservice calls to another system ; actual package will be named based on the distant service called, regardless of the implementation (calls to a LLM will be in out.ai or out.llm, and not in out.openAi)
 - An operation on the file system will be in an out.fs package

The code in 'out' will :
 - Convert the data from the business type to the actual type expected by the external system (ie FooId to ObjectId) before calling
 - Convert from DB/Storage format to the expected business model (e.g. ObjectId to FooId)

### Middle

Receives triggers from 'in', implements the business logic, and makes the calls to 'out' if required.

This tier is split in two :
 - Process : (called from 'in') aggregates calls to Services if several services have to be called. Transactions will be started around the process execution
 - Service : implements one feature, calls the 'out' classes

If a process ony use one service call, the process class MUST be still be present.

_e.g. a Process that allow creation of a `Foo` and its list of `Bar` may call FooService (through dependency injection) and BarService._

If somehow a Process has to be split in several transactions (e.g. several operations are performed, one can fail and the other may proceed), an additional "Feature" tier is used, it is directly called by the ìn`package, and pass the calls to "Process" class. Transactions are stilla round "Process calls".

## Abstraction / Dependency injection

Spring beans should never depend directly on implementations.

All dependencies should be expressed through Functional Interfaces ; implementations may implement as many such functional interfaces as required.

Those functional interfaces will be public, the implementations will be package private.

Each implementation (Process, service, repository/out classes ) should pretty much have their own package.

_Yes, there will a lot of those functional interfaces_


## Code writing

### use of this

The use of `this` keyword is compulsory anywhere possible (except for `static` calls of course).
  

### Strict typing

For all methods parameters and return types of methods in "middle" package, and of public methods of "out" package (meant to be called by 'middle' methods), strict typing is mandatory.

This means that:
 - `Number` types MUST only be used to represent quantity and amounts
 - `String` MUST only be used to represent messages/texts to be displayed by a human.

Anything else must be put in on Object of the correct type:
 - Path for file and folder (e.g. no `String inputPath = /opt/var/file.txt`)
 - LocalDateTime for date/time (no `long timeStamp...`)
 - Duration for duration (no `long delay_in_ms`, use `Duration delay=...`)
 - etc.

Any id should be type to a specific type, dedicated to the relevant object (e.g. no ` String idCustomer`, no `Id customerId`, but `CustomerId toBeAdded`).

Such id classes MUST be a simple wrapper around the actual technical data type.

Checks must be done to ensure that no such id may contain a null value.

### Nullability

No public method in "middle" and "out" package should accept `null` parameters, or return `null`.

All parameters should be annotated with `@NonNull`.

No public method in "middle" and "out" package should be able to return `null` ; always use Optional<X> or throw Exception if a "no data" output is possible.




