package page.matthewt.diskspaceinvestigator

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import page.matthewt.diskspaceinvestigator.ui.App
import page.matthewt.diskspaceinvestigator.viewmodel.AppViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun main() = application {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val viewModel = AppViewModel(scope)

    Window(
        onCloseRequest = ::exitApplication,
        title = "Disk Space Investigator",
        state = rememberWindowState(width = 1200.dp, height = 800.dp),
        icon = painterResource("icon.png"),
    ) {
        App(viewModel)
    }
}
