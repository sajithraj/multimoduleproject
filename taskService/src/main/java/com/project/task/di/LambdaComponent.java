package com.project.task.di;

import com.project.task.di.module.ObjectMapperModule;
import com.project.task.di.module.TaskModule;
import com.project.task.router.UnifiedEventRouter;
import dagger.Component;

import javax.inject.Singleton;

@Singleton
@Component(modules = {ObjectMapperModule.class, TaskModule.class})
public interface LambdaComponent {

    // Expose only the router used by the Lambda handler. Dagger will still construct
    // all transitive dependencies (services, deserializers, etc.) when creating the router.
    UnifiedEventRouter unifiedEventRouter();

}
