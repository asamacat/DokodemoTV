--- app/src/main/java/com/example/dokodemotv/MainActivity.kt
+++ app/src/main/java/com/example/dokodemotv/MainActivity.kt
@@ -40,6 +40,8 @@
 import androidx.media3.common.util.UnstableApi
 import androidx.media3.ui.PlayerView
+import androidx.media3.common.ForwardingPlayer
+import androidx.media3.common.Player
 import coil.compose.rememberAsyncImagePainter
 import com.example.dokodemotv.model.ChannelItem
 import android.widget.Toast
@@ -345,7 +347,9 @@
     url: String?,
     viewModel: PlayerViewModel,
     onDpadUp: () -> Unit,
-    onShowControls: () -> Unit
+    onShowControls: () -> Unit,
+    onZapNext: () -> Unit = {},
+    onZapPrevious: () -> Unit = {}
 ) {
     val context = LocalContext.current
     val exoPlayer = viewModel.exoPlayer

+    val forwardingPlayer = remember(exoPlayer, onZapNext, onZapPrevious) {
+        object : ForwardingPlayer(exoPlayer) {
+            override fun getAvailableCommands(): Player.Commands {
+                return super.getAvailableCommands().buildUpon()
+                    .add(Player.COMMAND_SEEK_TO_NEXT)
+                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
+                    .build()
+            }
+
+            override fun isCommandAvailable(command: @Player.Command Int): Boolean {
+                if (command == Player.COMMAND_SEEK_TO_NEXT || command == Player.COMMAND_SEEK_TO_PREVIOUS) {
+                    return true
+                }
+                return super.isCommandAvailable(command)
+            }
+
+            override fun seekToNext() {
+                onZapNext()
+            }
+
+            override fun seekToPrevious() {
+                onZapPrevious()
+            }
+        }
+    }
+
     LaunchedEffect(url) {
         if (url != null) viewModel.preparePlayer(url)
     }

     AndroidView(
         modifier = Modifier.fillMaxSize()
             .clickable { onShowControls() },
         factory = {
             PlayerView(context).apply {
-                player = exoPlayer
+                player = forwardingPlayer
                 useController = true
