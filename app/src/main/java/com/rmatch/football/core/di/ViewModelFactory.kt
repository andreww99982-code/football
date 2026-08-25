package com.rmatch.football.core.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Small helper so that screens can build ViewModels without a DI framework. */
class SimpleViewModelFactory<T : ViewModel>(
    private val creator: () -> T
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <M : ViewModel> create(modelClass: Class<M>): M = creator() as M
}
