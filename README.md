# lightning-cas

A library that provides support for Jasig's CAS (Central Authentication Service) protocol.

### Usage:

First, configure the module by injecting the following parameters:

```java
InjectorModule injector = new InjectorModule();
injector.bindAnnotationToInstance(CASDomain.class, "netid.rice.edu");
injector.bindAnnotationToInstance(CASPath.class, "/cas");
injector.bindAnnotationToInstance(CASDomain.class, "@rice.edu");
```

Next, include the controller by adding it to your scan prefix:

```
lightning.plugins.cas.controllers
```

That's it! Redirect the user to `/login` to log them in and `/logout` to log them out. 
The user will be redirected back to `/` on success.

