package com.project.task.di.module;

import com.project.task.mapper.TaskMapper;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

@Module
public class TaskModule {

    @Provides
    @Singleton
    public TaskMapper provideTaskMapper() {
        return TaskMapper.INSTANCE;
    }

}
