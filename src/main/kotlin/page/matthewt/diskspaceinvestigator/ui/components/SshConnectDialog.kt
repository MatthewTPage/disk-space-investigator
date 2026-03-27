package page.matthewt.diskspaceinvestigator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.konyaco.fluent.component.Button
import com.konyaco.fluent.component.Text
import com.konyaco.fluent.FluentTheme
import page.matthewt.diskspaceinvestigator.ssh.SshConfig
import page.matthewt.diskspaceinvestigator.ssh.SshHostEntry

@Composable
fun SshConnectDialog(
    onConnect: (host: String, user: String, path: String, port: Int) -> Unit,
    onCancel: () -> Unit,
) {
    val sshHosts = remember { SshConfig.parseConfig() }

    var host by remember { mutableStateOf("") }
    var user by remember { mutableStateOf(System.getProperty("user.name") ?: "") }
    var path by remember { mutableStateOf("/") }
    var port by remember { mutableStateOf("22") }
    var selectedHost by remember { mutableStateOf<SshHostEntry?>(null) }

    Column(
        modifier = Modifier
            .width(450.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(FluentTheme.colors.background.solid.secondary)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Connect via SSH",
            style = FluentTheme.typography.subtitle,
            fontWeight = FontWeight.Bold,
        )

        // SSH config hosts
        if (sshHosts.isNotEmpty()) {
            Text(
                "SSH Config Hosts:",
                color = FluentTheme.colors.text.text.secondary,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                sshHosts.take(5).forEach { entry ->
                    Button(onClick = {
                        selectedHost = entry
                        host = entry.hostName
                        entry.user?.let { user = it }
                        entry.port?.let { port = it.toString() }
                    }) {
                        Text(entry.alias)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        OutlinedTextField(
            value = host,
            onValueChange = { host = it },
            modifier = Modifier.fillMaxWidth(),
            label = { androidx.compose.material3.Text("Host") },
            placeholder = { androidx.compose.material3.Text("hostname or IP") },
            singleLine = true,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = user,
                onValueChange = { user = it },
                modifier = Modifier.weight(2f),
                label = { androidx.compose.material3.Text("User") },
                singleLine = true,
            )
            OutlinedTextField(
                value = port,
                onValueChange = { port = it.filter { c -> c.isDigit() } },
                modifier = Modifier.weight(1f),
                label = { androidx.compose.material3.Text("Port") },
                singleLine = true,
            )
        }

        OutlinedTextField(
            value = path,
            onValueChange = { path = it },
            modifier = Modifier.fillMaxWidth(),
            label = { androidx.compose.material3.Text("Remote Path") },
            placeholder = { androidx.compose.material3.Text("/home/user") },
            singleLine = true,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            Button(onClick = onCancel) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    if (host.isNotBlank() && path.isNotBlank()) {
                        onConnect(host, user, path, port.toIntOrNull() ?: 22)
                    }
                },
            ) {
                Text("Connect")
            }
        }
    }
}
