package io.github.rainboom7.cosmetics.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.rainboom7.cosmetics.AppContainer
import io.github.rainboom7.cosmetics.CosmeticsApp

/** `viewModel()` that builds the ViewModel from the app's [AppContainer]. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    key: String? = null,
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = (LocalContext.current.applicationContext as CosmeticsApp).container
    return viewModel(key = key, factory = viewModelFactory { initializer { create(container) } })
}
