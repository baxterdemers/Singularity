package com.hubspot.singularity;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import com.hubspot.mesos.client.SingularityMesosClientModule;
import com.hubspot.mesos.client.UserAndPassword;
import com.hubspot.singularity.auth.dw.SingularityAuthenticatorClass;
import com.hubspot.singularity.config.IndexViewConfiguration;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.SingularityDataModule;
import com.hubspot.singularity.data.history.SingularityDbModule;
import com.hubspot.singularity.data.history.SingularityHistoryModule;
import com.hubspot.singularity.data.transcoders.SingularityTranscoderModule;
import com.hubspot.singularity.data.zkmigrations.SingularityZkMigrationsModule;
import com.hubspot.singularity.event.SingularityEventModule;
import com.hubspot.singularity.jersey.SingularityJerseyModule;
import com.hubspot.singularity.mesos.SingularityMesosModule;
import com.hubspot.singularity.resources.SingularityOpenApiResource;
import com.hubspot.singularity.resources.SingularityResourceModule;
import com.hubspot.singularity.scheduler.SingularitySchedulerModule;

public class SingularityServiceModule
  extends DropwizardAwareModule<SingularityConfiguration> {
  private final Function<SingularityConfiguration, Module> dbModuleProvider;

  public SingularityServiceModule() {
    this.dbModuleProvider = SingularityDbModule::new;
  }

  public SingularityServiceModule(
    Function<SingularityConfiguration, Module> dbModuleProvider
  ) {
    this.dbModuleProvider = dbModuleProvider;
  }

  @Override
  public void configure(Binder binder) {
    binder.install(new SingularityMainModule(getConfiguration()));
    binder.install(new SingularityDataModule(getConfiguration()));
    binder.install(new SingularitySchedulerModule());
    binder.install(
      new SingularityResourceModule(getConfiguration().getUiConfiguration())
    );
    binder.install(new SingularityTranscoderModule());
    binder.install(new SingularityHistoryModule());
    binder.install(dbModuleProvider.apply(getConfiguration()));
    binder.install(new SingularityMesosModule());
    binder.install(new SingularityZkMigrationsModule());
    binder.install(new SingularityJerseyModule());

    MesosConfiguration mesosConfiguration = getConfiguration().getMesosConfiguration();
    if (
      mesosConfiguration.getMesosUsername().isPresent() &&
      mesosConfiguration.getMesosPassword().isPresent()
    ) {
      binder.install(
        new SingularityMesosClientModule(
          new UserAndPassword(
            mesosConfiguration.getMesosUsername().get(),
            mesosConfiguration.getMesosPassword().get()
          )
        )
      );
    } else {
      binder.install(new SingularityMesosClientModule());
    }

    // API Docs
    getEnvironment().jersey().register(SingularityOpenApiResource.class);

    binder.install(
      new SingularityEventModule(getConfiguration().getWebhookQueueConfiguration())
    );
  }

  @Provides
  @Singleton
  public IndexViewConfiguration provideIndexViewConfiguration() {
    SingularityConfiguration configuration = getConfiguration();
    return new IndexViewConfiguration(
      configuration.getUiConfiguration(),
      configuration.getMesosConfiguration().getDefaultMemory(),
      configuration.getMesosConfiguration().getDefaultCpus(),
      configuration.getMesosConfiguration().getDefaultDisk(),
      configuration.getMesosConfiguration().getAgentHttpPort(),
      configuration.getMesosConfiguration().getAgentHttpsPort(),
      configuration.getDefaultBounceExpirationMinutes(),
      configuration.getHealthcheckIntervalSeconds(),
      configuration.getHealthcheckTimeoutSeconds(),
      configuration.getHealthcheckMaxRetries(),
      configuration.getStartupTimeoutSeconds(),
      !Strings.isNullOrEmpty(configuration.getLoadBalancerUri()),
      configuration.getCommonHostnameSuffixToOmit(),
      configuration.getWarnIfScheduledJobIsRunningPastNextRunPct(),
      configuration.getAuthConfiguration().isEnabled() &&
      configuration
        .getAuthConfiguration()
        .getAuthenticators()
        .contains(SingularityAuthenticatorClass.WEBHOOK)
    );
  }
}
