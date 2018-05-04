package org.whispersystems.dropwizard.simpleauth;

import org.glassfish.jersey.server.model.AnnotatedMethod;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

import io.dropwizard.auth.Auth;

public class AuthDynamicFeature implements DynamicFeature {

  private AuthFilter[] authFilters;

  public AuthDynamicFeature(AuthFilter... authFilters) {
    this.authFilters = authFilters;
  }

  @Override
  public void configure(ResourceInfo resourceInfo, FeatureContext context) {
    AnnotatedMethod annotatedMethod       = new AnnotatedMethod(resourceInfo.getResourceMethod());
    Annotation[][]  parameterAnnotations  = annotatedMethod.getParameterAnnotations();
    Class<?>[]      parameterTypes        = annotatedMethod.getParameterTypes      ();
    Type[]          parameterGenericTypes = annotatedMethod.getGenericParameterTypes();

    verifyAuthAnnotations(parameterAnnotations);

    for (int i=0;i<parameterAnnotations.length;i++) {
      for (Annotation annotation : parameterAnnotations[i]) {
        if (annotation instanceof Auth) {
          Type parameterType = parameterTypes[i];

          if (parameterType == Optional.class) {
            parameterType = ((ParameterizedType)parameterGenericTypes[i]).getActualTypeArguments()[0];
            context.register(new WebApplicationExceptionCatchingFilter(getFilterFor(parameterType)));
          } else {
            context.register(getFilterFor(parameterType));
          }
        }
      }
    }
  }

  private AuthFilter getFilterFor(Type parameterType) {
    for (AuthFilter filter : authFilters) {
      if (filter.supports(parameterType)) return filter;
    }

    throw new IllegalArgumentException("No authenticator for type: " + parameterType);
  }

  private void verifyAuthAnnotations(Annotation[][] parameterAnnotations) {
    int authCount = 0;

    for (Annotation[] annotations : parameterAnnotations) {
      for (Annotation annotation : annotations) {
        if (annotation instanceof Auth) authCount++;
      }
    }

    if (authCount > 1) {
      throw new IllegalArgumentException("Only one @Auth tag supported per resource method!");
    }
  }

}
