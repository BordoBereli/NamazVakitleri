package com.kutluoglu.core.designsystem.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module

/**
 * Created by F.K. on 24.10.2025.
 *
 */

@Module
@Configuration
@ComponentScan("com.kutluoglu.core.designsystem.**")
object CoreCommonModule