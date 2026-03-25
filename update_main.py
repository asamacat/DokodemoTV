import sys

def modify_main():
    with open('app/src/main/java/com/example/dokodemotv/MainActivity.kt', 'r') as f:
        content = f.read()

    old_factory = """        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = true

                setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                    if (visibility == android.view.View.VISIBLE) {
                        onShowControls()
                    }
                })

                // Pass D-Pad Up events back up if the controller doesn't handle them
                setOnKeyListener { _, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        onDpadUp()
                        true
                    } else if (event.action == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
                        onShowControls()
                        false
                    } else {
                        false
                    }
                }
            }
        }"""

    new_factory = """        factory = {
            object : PlayerView(context) {
                override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                    if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        onDpadUp()
                        return true
                    } else if (event.action == KeyEvent.ACTION_DOWN && (event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER || event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                        onShowControls()
                        // let super handle it to show/hide controller or do default actions
                    }
                    return super.dispatchKeyEvent(event)
                }
            }.apply {
                player = exoPlayer
                useController = true

                setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                    if (visibility == android.view.View.VISIBLE) {
                        onShowControls()
                    }
                })
            }
        }"""

    if old_factory in content:
        content = content.replace(old_factory, new_factory)
        with open('app/src/main/java/com/example/dokodemotv/MainActivity.kt', 'w') as f:
            f.write(content)
        print("Successfully updated MainActivity.kt")
    else:
        print("Could not find the target string in MainActivity.kt")

modify_main()
