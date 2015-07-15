package org.whispersystems.dropwizard.simpleauth;

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

public class AuthSecurityContext<P> implements SecurityContext {

  private final AuthPrincipal principal;
  private final boolean       secure;

  public AuthSecurityContext(P principal, boolean secure) {
    this.principal = new AuthPrincipal(principal);
    this.secure    = secure;
  }

  @Override
  public Principal getUserPrincipal() {
    return principal;
  }

  @Override
  public boolean isUserInRole(String role) {
    return false;
  }

  @Override
  public boolean isSecure() {
    return secure;
  }

  @Override
  public String getAuthenticationScheme() {
    return SecurityContext.BASIC_AUTH;
  }
}
