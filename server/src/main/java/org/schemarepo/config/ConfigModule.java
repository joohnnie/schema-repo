package org.schemarepo.config;

import java.io.PrintStream;
import java.util.Properties;

import javax.inject.Named;
import javax.inject.Singleton;

import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import org.schemarepo.CacheRepository;
import org.schemarepo.Repository;
import org.schemarepo.RepositoryCache;
import org.schemarepo.Validator;
import org.schemarepo.ValidatorFactory;

/**
 * A {@link Module} for configuration based on a set of {@link Properties}
 * <br/>
 * Binds every property value in the properties provided to the property name
 * in Guice, making them available with the {@link Named} annotation.  Guice
 * will automatically convert these to constant values, such as Integers,
 * Strings, or Class constants.
 * <br/>
 * Keys starting with "validator." bind {@link Validator} classes
 * in a {@link ValidatorFactory}, where the name is the remainder of the key
 * following "schema-repo.validator.".  For example, a property
 * "schema-repo.validator.backwards_compatible=com.foo.BackwardsCompatible"
 * will set a validator named "backwards_compatible" to an instance of the
 * class com.foo.BackwardsCompatible.
 */
public class ConfigModule implements Module {

  public static void printDefaults(PrintStream writer) {
    writer.println(Config.DEFAULTS);
  }

  private final Properties props;

  public ConfigModule(Properties props) {
    Properties copy = new Properties(Config.DEFAULTS);
    copy.putAll(props);
    this.props = copy;
  }

  @Override
  public void configure(Binder binder) {
    Names.bindProperties(binder, props);
  }

  @Provides
  @Singleton
  Repository provideRepository(Injector injector,
      @Named(Config.REPO_CLASS) Class<Repository> repoClass,
      @Named(Config.REPO_CACHE) Class<RepositoryCache> cacheClass) {
    Repository repo = injector.getInstance(repoClass);
    RepositoryCache cache = injector.getInstance(cacheClass);
    return new CacheRepository(repo, cache);
  }

  @Provides
  @Singleton
  ValidatorFactory provideValidatorFactory(Injector injector) {
    ValidatorFactory.Builder builder = new ValidatorFactory.Builder();
    for(String prop : props.stringPropertyNames()) {
      if (prop.startsWith(Config.VALIDATOR_PREFIX)) {
        String validatorName = prop.substring(Config.VALIDATOR_PREFIX.length());
        Class<Validator> validatorClass = injector.getInstance(
            Key.<Class<Validator>>get(
                new TypeLiteral<Class<Validator>>(){}, Names.named(prop)));
        builder.setValidator(validatorName, injector.getInstance(validatorClass));
      }
    }
    return builder.build();
  }
}
