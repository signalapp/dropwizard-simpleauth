package org.whispersystems.dropwizard.simpleauth;

import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.internal.inject.AbstractContainerRequestValueFactory;
import org.glassfish.jersey.server.internal.inject.AbstractValueFactoryProvider;
import org.glassfish.jersey.server.internal.inject.MultivaluedParameterExtractorProvider;
import org.glassfish.jersey.server.internal.inject.ParamInjectionResolver;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.spi.internal.ValueFactoryProvider;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.security.Principal;
import java.util.Optional;

import io.dropwizard.auth.Auth;

@Singleton
public class AuthValueFactoryProvider extends AbstractValueFactoryProvider {

  @Inject
  public AuthValueFactoryProvider(MultivaluedParameterExtractorProvider mpep,
                                  ServiceLocator injector)
  {
    super(mpep, injector, Parameter.Source.UNKNOWN);
  }

  @Override
  public AbstractContainerRequestValueFactory<?> createValueFactory(final Parameter parameter) {
    if (parameter.getAnnotation(Auth.class) == null) {
      return null;
    }

    if (parameter.getRawType() == Optional.class) {
      return new OptionalContainerRequestValueFactory(parameter);
    } else {
      return new StandardContainerReqeustValueFactory(parameter);
    }
  }

  private static class StandardContainerReqeustValueFactory extends AbstractContainerRequestValueFactory {
    private final Parameter parameter;

    StandardContainerReqeustValueFactory(Parameter parameter) {
      this.parameter = parameter;
    }

    /**
     * @return {@link Principal} stored on the request, or {@code null} if no object was found.
     */
    public Object provide() {
      Principal principal = getContainerRequest().getSecurityContext().getUserPrincipal();

      if (principal == null) {
        throw new IllegalStateException("Cannot inject a custom principal into unauthenticated request");
      }

      if (!(principal instanceof AuthPrincipal)) {
        throw new IllegalArgumentException("Cannot inject a non-AuthPrincipal into request");
      }

      if (!parameter.getRawType().isAssignableFrom(((AuthPrincipal)principal).getAuthenticated().getClass())) {
        throw new IllegalArgumentException("Authenticated principal is of the wrong type!");
      }

      return parameter.getRawType().cast(((AuthPrincipal) principal).getAuthenticated());
    }
  }

  private static class OptionalContainerRequestValueFactory extends AbstractContainerRequestValueFactory {
    private final Parameter parameter;

    OptionalContainerRequestValueFactory(Parameter parameter) {
      this.parameter = parameter;
    }

    /**
     * @return {@link Principal} stored on the request, or {@code null} if no object was found.
     */
    public Object provide() {
      Principal principal = getContainerRequest().getSecurityContext().getUserPrincipal();

      if (principal != null && !(principal instanceof AuthPrincipal)) {
        throw new IllegalArgumentException("Cannot inject a non-AuthPrincipal into request");
      }

      if (principal == null) return Optional.empty();
      else                   return Optional.of(((AuthPrincipal)principal).getAuthenticated());
    }
  }

  @Singleton
  private static class AuthInjectionResolver extends ParamInjectionResolver<Auth> {

    /**
     * Create new {@link Auth} annotation injection resolver.
     */
    public AuthInjectionResolver() {
      super(AuthValueFactoryProvider.class);
    }
  }

  /**
   * Injection binder for {@link AuthValueFactoryProvider} and {@link AuthInjectionResolver}.
   *
   */
  public static class Binder extends AbstractBinder {

    public Binder() {}

    @Override
    protected void configure() {
      bind(AuthValueFactoryProvider.class).to(ValueFactoryProvider.class).in(Singleton.class);
      bind(AuthInjectionResolver.class).to(new TypeLiteral<InjectionResolver<Auth>>() {
      }).in(Singleton.class);
    }
  }
}
