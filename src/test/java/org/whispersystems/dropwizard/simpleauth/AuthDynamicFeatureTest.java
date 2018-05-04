package org.whispersystems.dropwizard.simpleauth;


import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;

import java.util.Optional;

import io.dropwizard.auth.Auth;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.basic.BasicCredentials;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.*;

public class AuthDynamicFeatureTest {

  @Test
  public void testPrincipalTypes() throws NoSuchMethodException {
    AuthFilter stringPrincipal = new BasicCredentialAuthFilter.Builder<String>().setRealm("Hmm")
                                                                                .setPrincipal(String.class)
                                                                                .setAuthenticator(new StringAuthenticator())
                                                                                .buildAuthFilter();

    AuthFilter integerPrincipal = new BasicCredentialAuthFilter.Builder<Integer>().setRealm("Hmm")
                                                                                  .setPrincipal(Integer.class)
                                                                                  .setAuthenticator(new IntegerAuthenticator())
                                                                                  .buildAuthFilter();

    AuthDynamicFeature dynamicFeature = new AuthDynamicFeature(stringPrincipal, integerPrincipal);
    ResourceInfo       resourceInfo   = mock(ResourceInfo.class  );
    FeatureContext     featureContext = mock(FeatureContext.class);

    when(resourceInfo.getResourceMethod()).thenReturn(MockMethod.class.getDeclaredMethod("stringAuthParam", String.class));

    dynamicFeature.configure(resourceInfo, featureContext);
    verify(featureContext).register(eq(stringPrincipal));
    reset(featureContext);

    when(resourceInfo.getResourceMethod()).thenReturn(MockMethod.class.getDeclaredMethod("integerAuthParam", Integer.class));

    dynamicFeature.configure(resourceInfo, featureContext);
    verify(featureContext).register(eq(integerPrincipal));
    reset(featureContext);

    when(resourceInfo.getResourceMethod()).thenReturn(MockMethod.class.getDeclaredMethod("optionalStringAuthParam", Optional.of("test").getClass()));

    dynamicFeature.configure(resourceInfo, featureContext);

    ArgumentCaptor<ContainerRequestFilter> stringOptionalCaptor = ArgumentCaptor.forClass(ContainerRequestFilter.class);
    verify(featureContext).register(stringOptionalCaptor.capture());
    assertTrue(stringOptionalCaptor.getValue() instanceof WebApplicationExceptionCatchingFilter);
    assertEquals(((WebApplicationExceptionCatchingFilter)stringOptionalCaptor.getValue()).getUnderlying(), stringPrincipal);

    reset(featureContext);

    when(resourceInfo.getResourceMethod()).thenReturn(MockMethod.class.getDeclaredMethod("optionalIntegerAuthParam", Optional.of(1).getClass()));

    dynamicFeature.configure(resourceInfo, featureContext);

    ArgumentCaptor<ContainerRequestFilter> integerOptionalCaptor = ArgumentCaptor.forClass(ContainerRequestFilter.class);
    verify(featureContext, times(1)).register(integerOptionalCaptor.capture());
    assertTrue(integerOptionalCaptor.getValue() instanceof WebApplicationExceptionCatchingFilter);
    assertEquals(((WebApplicationExceptionCatchingFilter)integerOptionalCaptor.getValue()).getUnderlying(), integerPrincipal);
  }

  @Test
  public void testMultipleAuthTags() throws NoSuchMethodException {
    AuthDynamicFeature dynamicFeature = new AuthDynamicFeature(new AuthFilter[0]);
    ResourceInfo       resourceInfo   = mock(ResourceInfo.class  );
    FeatureContext     featureContext = mock(FeatureContext.class);

    when(resourceInfo.getResourceMethod()).thenReturn(MockMethod.class.getDeclaredMethod("multipleAuthParams", String.class, String.class));

    try {
      dynamicFeature.configure(resourceInfo, featureContext);
      throw new AssertionError("Shouldn't support multiple auth tags!");
    } catch (Exception e) {
      // Good
    }

    when(resourceInfo.getResourceMethod()).thenReturn(MockMethod.class.getDeclaredMethod("multipleOptionalAuthParams", Optional.of("foo").getClass(), Optional.of("bar").getClass()));

    try {
      dynamicFeature.configure(resourceInfo, featureContext);
      throw new AssertionError("Shouldn't support multiple auth tags!");
    } catch (Exception e) {
      // Good
    }

  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private static class MockMethod {
    public void multipleAuthParams(@Auth String foo, @Auth String bar) {}
    public void stringAuthParam(@Auth String foo) {}
    public void integerAuthParam(@Auth Integer bar) {}
    public void optionalStringAuthParam(@Auth Optional<String> foo) {}
    public void optionalIntegerAuthParam(@Auth Optional<Integer> bar) {}
    public void multipleOptionalAuthParams(@Auth Optional<String> foo, @Auth Optional<String> bar) {}
  }

  private static class StringAuthenticator implements Authenticator<BasicCredentials, String> {
    @Override
    public Optional<String> authenticate(BasicCredentials credentials)
        throws AuthenticationException
    {
      if (credentials.getUsername().equals("user") &&
          credentials.getPassword().equals("password"))
        return Optional.of("user");

      return Optional.empty();
    }
  }

  private static class IntegerAuthenticator implements Authenticator<BasicCredentials, Integer> {

    @Override
    public Optional<Integer> authenticate(BasicCredentials credentials) throws AuthenticationException {
      return Optional.of(1);
    }
  }

}
