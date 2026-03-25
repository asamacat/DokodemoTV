import re

with open('app/src/main/java/com/example/dokodemotv/MainActivity.kt', 'r') as f:
    content = f.read()

# Add zapMessage state
app_start_marker = """    var showMenuButton by remember { mutableStateOf(true) }"""

app_new_start = """    var showMenuButton by remember { mutableStateOf(true) }

    var zapMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(zapMessage) {
        if (zapMessage != null) {
            delay(2000)
            zapMessage = null
        }
    }"""

content = content.replace(app_start_marker, app_new_start)

# Update VideoPlayerContent call to set zapMessage
video_player_call_old = """                onZapNext = {
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
                }"""

video_player_call_new = """                onZapNext = {
                    if (allChannels.isNotEmpty()) {
                        val currentIndex = allChannels.indexOfFirst { it.streamUrl == selectedUrl }
                        val nextIndex = if (currentIndex == -1 || currentIndex == allChannels.lastIndex) 0 else currentIndex + 1
                        val nextChannel = allChannels[nextIndex]
                        selectedUrl = nextChannel.streamUrl
                        zapMessage = nextChannel.name
                    }
                },
                onZapPrevious = {
                    if (allChannels.isNotEmpty()) {
                        val currentIndex = allChannels.indexOfFirst { it.streamUrl == selectedUrl }
                        val prevIndex = if (currentIndex <= 0) allChannels.lastIndex else currentIndex - 1
                        val prevChannel = allChannels[prevIndex]
                        selectedUrl = prevChannel.streamUrl
                        zapMessage = prevChannel.name
                    }
                }"""

content = content.replace(video_player_call_old, video_player_call_new)

# Add Overlay UI inside the Scaffold Box
scaffold_box_end = """                ) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                }
            }
        }
    }"""

scaffold_box_new_end = """                ) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                }
            }

            // Zap Message Overlay
            if (zapMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = zapMessage ?: "",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
            }
        }
    }"""

content = content.replace(scaffold_box_end, scaffold_box_new_end)

with open('app/src/main/java/com/example/dokodemotv/MainActivity.kt', 'w') as f:
    f.write(content)
