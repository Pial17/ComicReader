package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.scanner.CbzScanner
import com.example.ui.model.ThemeSetting
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.LibraryViewModel
import com.example.ui.viewmodel.ReaderViewModel
import com.example.util.StoragePermissionHelper
import kotlinx.coroutines.launch

sealed class Screen {
    object Library : Screen()
    data class Reader(val bookId: Long, val startPage: Int? = null) : Screen()
    object Settings : Screen()
}

class MainActivity : ComponentActivity() {

    private val libraryViewModel: LibraryViewModel by viewModels()
    private val readerViewModel: ReaderViewModel by viewModels()

    private var activeScreen by mutableStateOf<Screen>(Screen.Library)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle intent if launched with CBZ file
        handleIntent(intent)

        setContent {
            val themeSetting by libraryViewModel.settingsManager.themeSetting.collectAsState()
            val isDark = when (themeSetting) {
                ThemeSetting.DARK -> true
                ThemeSetting.LIGHT -> false
                ThemeSetting.SYSTEM -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDark) {
                RequestStoragePermission(onPermissionGranted = {
                    libraryViewModel.scanStorage()
                })

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (val current = activeScreen) {
                        is Screen.Library -> {
                            LibraryScreen(
                                viewModel = libraryViewModel,
                                onOpenBook = { bookId, startPage ->
                                    readerViewModel.loadBook(bookId, startPage)
                                    activeScreen = Screen.Reader(bookId, startPage)
                                },
                                onOpenSettings = {
                                    activeScreen = Screen.Settings
                                }
                            )
                        }
                        is Screen.Reader -> {
                            BackHandler {
                                readerViewModel.closeBook()
                                activeScreen = Screen.Library
                            }
                            androidx.compose.runtime.key(current.bookId) {
                                ReaderScreen(
                                    viewModel = readerViewModel,
                                    bookId = current.bookId,
                                    initialPage = current.startPage ?: 1,
                                    onNavigateBack = {
                                        readerViewModel.closeBook()
                                        activeScreen = Screen.Library
                                    }
                                )
                            }
                        }
                        is Screen.Settings -> {
                            BackHandler {
                                activeScreen = Screen.Library
                            }
                            SettingsScreen(
                                viewModel = libraryViewModel,
                                onNavigateBack = {
                                    activeScreen = Screen.Library
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (StoragePermissionHelper.hasFullStoragePermission(this)) {
            if (libraryViewModel.books.value.isEmpty() && !libraryViewModel.isScanning.value) {
                libraryViewModel.scanStorage()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri: Uri? = intent?.data
        if (uri != null) {
            lifecycleScope.launch {
                val existing = libraryViewModel.repository.getBookByPath(uri.toString())
                val book = existing ?: CbzScanner(this@MainActivity, libraryViewModel.repository).importSingleCbz(uri.toString())
                if (book != null) {
                    readerViewModel.loadBook(book.id, book.currentPage)
                    activeScreen = Screen.Reader(book.id, book.currentPage)
                }
            }
        }
    }
}

@Composable
private fun RequestStoragePermission(onPermissionGranted: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val legacyLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onPermissionGranted()
        }
    }

    val manageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (StoragePermissionHelper.hasFullStoragePermission(context)) {
            onPermissionGranted()
        }
    }

    // Auto-scan on resume if permission was just granted in settings
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (StoragePermissionHelper.hasFullStoragePermission(context)) {
                    onPermissionGranted()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        if (StoragePermissionHelper.hasFullStoragePermission(context)) {
            onPermissionGranted()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                manageLauncher.launch(StoragePermissionHelper.createPermissionIntent(context))
            } else {
                legacyLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
