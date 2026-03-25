import re

with open('app/src/main/java/com/example/dokodemotv/MainActivity.kt', 'r') as f:
    content = f.read()

# Add imports
imports_to_add = """
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
"""
content = content.replace('import androidx.media3.ui.PlayerView\n', 'import androidx.media3.ui.PlayerView\n' + imports_to_add)

# Update VideoPlayerContent signature
signature_old = """fun VideoPlayerContent(
    url: String?,
    viewModel: PlayerViewModel,
    onDpadUp: () -> Unit,
    onShowControls: () -> Unit
) {"""

signature_new = """fun VideoPlayerContent(
    url: String?,
    viewModel: PlayerViewModel,
    onDpadUp: () -> Unit,
    onShowControls: () -> Unit,
    onZapNext: () -> Unit = {},
    onZapPrevious: () -> Unit = {}
) {"""

content = content.replace(signature_old, signature_new)

# Update inside VideoPlayerContent
body_old = """    val context = LocalContext.current
    val exoPlayer = viewModel.exoPlayer

    LaunchedEffect(url) {"""

body_new = """    val context = LocalContext.current
    val exoPlayer = viewModel.exoPlayer

    val forwardingPlayer = remember(exoPlayer, onZapNext, onZapPrevious) {
        object : ForwardingPlayer(exoPlayer) {
            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .build()
            }

            override fun isCommandAvailable(command: @Player.Command Int): Boolean {
                if (command == Player.COMMAND_SEEK_TO_NEXT || command == Player.COMMAND_SEEK_TO_PREVIOUS) {
                    return true
                }
                return super.isCommandAvailable(command)
            }

            override fun seekToNext() {
                onZapNext()
            }

            override fun seekToPrevious() {
                onZapPrevious()
            }
        }
    }

    LaunchedEffect(url) {"""

content = content.replace(body_old, body_new)

# Update PlayerView factory assignment
player_old = """            PlayerView(context).apply {
                player = exoPlayer
                useController = true"""

player_new = """            PlayerView(context).apply {
                player = forwardingPlayer
                useController = true"""

content = content.replace(player_old, player_new)

with open('app/src/main/java/com/example/dokodemotv/MainActivity.kt', 'w') as f:
    f.write(content)
