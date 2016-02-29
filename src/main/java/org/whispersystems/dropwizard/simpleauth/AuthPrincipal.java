package org.whispersystems.dropwizard.simpleauth;

import javax.security.auth.Subject;
import java.security.Principal;

public class AuthPrincipal implements Principal {

  private final Object authenticated;

  public AuthPrincipal(Object authenticated) {
    this.authenticated = authenticated;
  }

  @Override
  public String getName() {
    return null;
  }

  public boolean implies(Subject subject) {
    return false;
  }

  public Object getAuthenticated() {
    return authenticated;
  }
}
