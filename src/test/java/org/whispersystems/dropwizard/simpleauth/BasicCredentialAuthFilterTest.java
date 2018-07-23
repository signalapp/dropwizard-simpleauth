package org.whispersystems.dropwizard.simpleauth;

import com.google.common.io.BaseEncoding;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;

import java.io.IOException;
import java.util.Optional;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.basic.BasicCredentials;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class BasicCredentialAuthFilterTest {

  @Test
  public void testValidAuth() throws IOException {
    StringAuthenticator authenticator = new StringAuthenticator("user", "foo");

    AuthFilter authFilter = new BasicCredentialAuthFilter.Builder<String>().setAuthenticator(authenticator)
                                                                           .setPrincipal(String.class)
                                                                           .setRealm("Hmm")
                                                                           .buildAuthFilter();

    MultivaluedMap<String, String> headers = new MultivaluedHashMap<String, String>() {{
      add(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpmb28=");
    }};

    ContainerRequestContext containerRequestContext = mock(ContainerRequestContext.class);
    when(containerRequestContext.getHeaders()).thenReturn(headers);

    authFilter.filter(containerRequestContext);

    ArgumentCaptor<SecurityContext> captor = ArgumentCaptor.forClass(SecurityContext.class);
    verify(containerRequestContext).setSecurityContext(captor.capture());

    assertTrue(captor.getValue().getUserPrincipal() instanceof AuthPrincipal);
    assertEquals(((AuthPrincipal) captor.getValue().getUserPrincipal()).getAuthenticated(), "user");
  }

  @Test
  public void testEmptyUsername() throws IOException {
    StringAuthenticator authenticator = new StringAuthenticator("", "foo");

    AuthFilter authFilter = new BasicCredentialAuthFilter.Builder<String>().setAuthenticator(authenticator)
                                                                           .setPrincipal(String.class)
                                                                           .setRealm("Hmm")
                                                                           .buildAuthFilter();

    MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
    headers.add(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode(":foo".getBytes()));

    ContainerRequestContext containerRequestContext = mock(ContainerRequestContext.class);
    when(containerRequestContext.getHeaders()).thenReturn(headers);

    authFilter.filter(containerRequestContext);

    ArgumentCaptor<SecurityContext> captor = ArgumentCaptor.forClass(SecurityContext.class);
    verify(containerRequestContext).setSecurityContext(captor.capture());

    assertTrue(captor.getValue().getUserPrincipal() instanceof AuthPrincipal);
    assertEquals(((AuthPrincipal) captor.getValue().getUserPrincipal()).getAuthenticated(), "");
  }

  @Test
  public void testInvalidAuth() throws Exception {
    StringAuthenticator authenticator = new StringAuthenticator("user", "foo");

    AuthFilter authFilter = new BasicCredentialAuthFilter.Builder<String>().setAuthenticator(authenticator)
                                                                           .setPrincipal(String.class)
                                                                           .setRealm("Hmm")
                                                                           .buildAuthFilter();

    MultivaluedMap<String, String> headers = new MultivaluedHashMap<String, String>() {{
      add(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpiYXo=");
    }};

    ContainerRequestContext containerRequestContext = mock(ContainerRequestContext.class);
    when(containerRequestContext.getHeaders()).thenReturn(headers);

    try {
      authFilter.filter(containerRequestContext);
      throw new AssertionError("Shouldn't succeed");
    } catch (WebApplicationException wae) {
      verify(containerRequestContext, times(0)).setSecurityContext(any(SecurityContext.class));
      assertEquals(wae.getResponse().getStatus(), 401);
    }
  }


  private static class StringAuthenticator implements Authenticator<BasicCredentials, String> {

    private final String user;
    private final String password;

    public StringAuthenticator(String user, String password) {
      this.user     = user;
      this.password = password;
    }

    @Override
    public Optional<String> authenticate(BasicCredentials credentials) throws AuthenticationException {
      if (credentials.getUsername().equals(user) && credentials.getPassword().equals(password)) {
        return Optional.of(user);
      }

      return Optional.empty();
    }
  }
}
