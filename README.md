# dropwizard-simpleauth

A Dropwizard library that lets you use simple `@Auth` annotations for authenticating multiple
types, without having to deal with `@RolesAllowed` style authorizations.

Install from maven central:

```
<dependency>
  <groupId>org.whispersystems</groupId>
  <artifactId>dropwizard-simpleauth</artifactId>
  <version>${latest_version}</version>
</dependency>
```

## The details

This library allows writing an authenticated Dropwizard resource to look like this:

```
@Path("/api/v1/mail")
public class MailResource {

  @Timed
  @POST
  @Path("/{destination}/")
  @Consumes(MediaType.APPLICATION_JSON_TYPE)
  public void sendMessage(@Auth User sender,
                          @PathParam("destination") String destination,
                          @Valid Message message)
  {
    ...
  }
  
  @Timed
  @DELETE
  @Path("/{messageId}/")
  public void sendMessage(@Auth Admin admin,
                          @PathParam("messageId") long messageId)
  {
    ...
  }
  
  
}
```

No "authorization" tags like `@AllowAll`, `@DenyAll`, `@RolesAllowed` are used.  Instead,
the `@Auth` tag allows you to authenticate multiple different "principal" types (in this example
both `User` and `Admin`), neither of which have to extend `Principal`.

## Registering authenticators

To support authenticating multiple types, register multiple `AuthFilter`s:

`````
@Override
public void run(ExampleConfiguration configuration,
                Environment environment) 
{
    environment.jersey().register(new AuthDynamicFeature(
            new BasicCredentialAuthFilter.Builder<User>()
                .setAuthenticator(new UserAuthenticator())
                .setPrincipal(User.class)
                .buildAuthFilter(),
            new BasicCredentialAuthFilter.Builder<Admin>()
                .setAuthenticator(new AdminAuthenticator())
                .setPrincipal(Admin.class)
                .buildAuthFilter()));

    environment.jersey().register(new AuthValueFactoryProvider.Binder());
}
`````

That's it!