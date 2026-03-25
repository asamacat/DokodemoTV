import re

with open('app/src/main/java/com/example/dokodemotv/MainActivity.kt', 'r') as f:
    content = f.read()

# Find the start of DokodemoTVApp function
app_start_marker = """fun DokodemoTVApp(viewModel: PlayerViewModel = viewModel()) {
    val context = LocalContext.current
    val sources by viewModel.sources.collectAsState()
    var selectedUrl by remember { mutableStateOf<String?>(viewModel.initialUrl) }"""

app_new_start = """fun DokodemoTVApp(viewModel: PlayerViewModel = viewModel()) {
    val context = LocalContext.current
    val sources by viewModel.sources.collectAsState()
    var selectedUrl by remember { mutableStateOf<String?>(viewModel.initialUrl) }

    // Flat list of all channels for zapping
    val allChannels = remember(sources) {
        sources.flatMap { it.channels }
    }"""

content = content.replace(app_start_marker, app_new_start)

# Add zapping logic to VideoPlayerContent call inside DokodemoTVApp
video_player_call_old = """            VideoPlayerContent(
                url = selectedUrl,
                viewModel = viewModel,
                onDpadUp = { showBottomSheet = true; showMenuButton = true },
                onShowControls = { showMenuButton = true }
            )"""

video_player_call_new = """            VideoPlayerContent(
                url = selectedUrl,
                viewModel = viewModel,
                onDpadUp = { showBottomSheet = true; showMenuButton = true },
                onShowControls = { showMenuButton = true },
                onZapNext = {
                    if (allChannels.isNotEmpty()) {
                        val currentIndex = allChannels.indexOfFirst { it.streamUrl == selectedUrl }
                        val nextIndex = if (currentIndex == -1 || currentIndex == allChannels.lastIndex) 0 else currentIndex + 1
                        val nextChannel = allChannels[nextIndex]
                        selectedUrl = nextChannel.streamUrl
                        // TODO: Update zap message state
                    }
                },
                onZapPrevious = {
                    if (allChannels.isNotEmpty()) {
                        val currentIndex = allChannels.indexOfFirst { it.streamUrl == selectedUrl }
                        val prevIndex = if (currentIndex <= 0) allChannels.lastIndex else currentIndex - 1
                        val prevChannel = allChannels[prevIndex]
                        selectedUrl = prevChannel.streamUrl
                        // TODO: Update zap message state
                    }
                }
            )"""

content = content.replace(video_player_call_old, video_player_call_new)

with open('app/src/main/java/com/example/dokodemotv/MainActivity.kt', 'w') as f:
    f.write(content)
