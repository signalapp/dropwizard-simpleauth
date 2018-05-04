package org.whispersystems.dropwizard.simpleauth;

import com.google.common.base.Preconditions;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestFilter;

import java.lang.reflect.Type;
import java.util.Optional;

import io.dropwizard.auth.DefaultUnauthorizedHandler;
import io.dropwizard.auth.UnauthorizedHandler;

@Priority(Priorities.AUTHENTICATION)
public abstract class AuthFilter<C, P> implements ContainerRequestFilter {

  protected String              prefix;
  protected String              realm;
  protected Authenticator<C, P> authenticator;
  protected Class<P>            principalType;
  protected UnauthorizedHandler unauthorizedHandler = new DefaultUnauthorizedHandler();


  public boolean supports(Type clazz) {
    return clazz.equals(principalType);
  }

  /**
   * Abstract builder for auth filters.
   *
   * @param <C> the type of credentials that the filter accepts
   * @param <P> the type of the principal that the filter accepts
   */
  public abstract static class AuthFilterBuilder<C, P, T extends AuthFilter<C, P>> {

    private String realm = "realm";
    private String prefix = "Basic";
    private Authenticator<C, P> authenticator;
    private Class<P> principalType;

    /**
     * Sets the given realm
     *
     * @param realm a realm
     * @return the current builder
     */
    public AuthFilterBuilder<C, P, T> setRealm(String realm) {
      this.realm = realm;
      return this;
    }

    /**
     * Sets the given prefix
     *
     * @param prefix a prefix
     * @return the current builder
     */
    public AuthFilterBuilder<C, P, T> setPrefix(String prefix) {
      this.prefix = prefix;
      return this;
    }

    /**
     * Sets the given authenticator
     *
     * @param authenticator an {@link io.dropwizard.auth.Authenticator}
     * @return the current builder
     */
    public AuthFilterBuilder<C, P, T> setAuthenticator(Authenticator<C, P> authenticator) {
      this.authenticator = authenticator;
      return this;
    }

    public AuthFilterBuilder<C, P, T> setPrincipal(Class<P> principalType) {
      this.principalType = principalType;
      return this;
    }

    /**
     * Builds an instance of the filter with a provided authenticator,
     * an authorizer, a prefix, and a realm.
     *
     * @return a new instance of the filter
     */
    public T buildAuthFilter() {
      Preconditions.checkArgument(realm != null, "Realm is not set");
      Preconditions.checkArgument(prefix != null, "Prefix is not set");
      Preconditions.checkArgument(authenticator != null, "Authenticator is not set");

      T authFilter = newInstance();
      authFilter.authenticator = authenticator;
      authFilter.prefix        = prefix;
      authFilter.realm         = realm;
      authFilter.principalType = principalType;
      return authFilter;
    }

    protected abstract T newInstance();
  }
}
