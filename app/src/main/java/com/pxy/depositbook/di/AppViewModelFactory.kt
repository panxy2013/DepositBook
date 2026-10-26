package com.pxy.depositbook.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pxy.depositbook.DepositApp
import com.pxy.depositbook.data.repo.DepositRepository
import com.pxy.depositbook.viewmodel.CalendarViewModel
import com.pxy.depositbook.viewmodel.HomeViewModel
import com.pxy.depositbook.viewmodel.LogViewModel
import com.pxy.depositbook.viewmodel.StatsViewModel

/**
 * 极简的 ViewModel 工厂：通过 Compose 的 viewModel(factory = ...) 注入仓库。
 */
object AppViewModelFactory {

    fun provide(repository: DepositRepository): ViewModelProvider.Factory = viewModelFactory {
        initializer { HomeViewModel(repository) }
        initializer { StatsViewModel(repository) }
        initializer { LogViewModel(repository) }
        initializer { CalendarViewModel(repository) }
    }
}

/** 便捷扩展：从 CreationExtras 取 Application */
val CreationExtras.depositApp: DepositApp
    get() = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as DepositApp)
