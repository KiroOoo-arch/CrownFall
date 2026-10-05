package com.crownfall.realm

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.crownfall.realm.di.AppContainer
import com.crownfall.realm.navigation.CrownfallNavHost
import com.crownfall.realm.ui.theme.CrownfallTheme

class MainActivity : ComponentActivity() {

    private val container: AppContainer
        get() = (application as CrownfallApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent { CrownfallApp(container) }
    }

    override fun onStart() {
        super.onStart()
        // Music is opt-in through the settings volume; starting at zero is a no-op.
        container.audioEngine.startMusic()
    }

    override fun onStop() {
        super.onStop()
        container.audioEngine.stopMusic()
    }
}

@Composable
private fun CrownfallApp(container: AppContainer) {
    CrownfallTheme {
        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
            CrownfallNavHost(container)
        }
    }
}

