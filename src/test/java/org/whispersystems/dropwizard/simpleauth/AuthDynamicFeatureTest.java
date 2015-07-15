package org.whispersystems.dropwizard.simpleauth;


import com.google.common.base.Optional;
import org.junit.Test;

import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;

import io.dropwizard.auth.Auth;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.basic.BasicCredentials;
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

    when(resourceInfo.getResourceMethod()).thenReturn(MockMethod.class.getDeclaredMethod("integerAuthParam", Integer.class));

    dynamicFeature.configure(resourceInfo, featureContext);
    verify(featureContext).register(eq(integerPrincipal));
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
  }

  private static class MockMethod {
    public void multipleAuthParams(@Auth String foo, @Auth String bar) {}
    public void stringAuthParam(@Auth String foo) {}
    public void integerAuthParam(@Auth Integer bar) {}
  }

  private static class StringAuthenticator implements Authenticator<BasicCredentials, String> {
    @Override
    public Optional<String> authenticate(BasicCredentials credentials)
        throws AuthenticationException
    {
      if (credentials.getUsername().equals("user") &&
          credentials.getPassword().equals("password"))
        return Optional.of("user");

      return Optional.absent();
    }
  }

  private static class IntegerAuthenticator implements Authenticator<BasicCredentials, Integer> {

    @Override
    public Optional<Integer> authenticate(BasicCredentials credentials) throws AuthenticationException {
      return Optional.of(1);
    }
  }

}
