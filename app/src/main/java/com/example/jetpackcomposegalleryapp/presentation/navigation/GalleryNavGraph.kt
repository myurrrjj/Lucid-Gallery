package com.example.jetpackcomposegalleryapp.presentation.navigation

import android.Manifest
import android.app.Activity
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest.Builder
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.jetpackcomposegalleryapp.core.util.MediaIntents
import com.example.jetpackcomposegalleryapp.presentation.gallery.screens.DetailScreen
import com.example.jetpackcomposegalleryapp.presentation.gallery.contract.GalleryEffect
import com.example.jetpackcomposegalleryapp.presentation.gallery.contract.GalleryEvent
import com.example.jetpackcomposegalleryapp.presentation.gallery.screens.mainGridScreen.GalleryScreen
import com.example.jetpackcomposegalleryapp.presentation.gallery.GalleryViewModel
import com.example.jetpackcomposegalleryapp.presentation.navigation.Route.Detail
import com.example.jetpackcomposegalleryapp.presentation.navigation.Route.DirtyTest
import com.example.jetpackcomposegalleryapp.presentation.navigation.Route.Gallery
import com.example.jetpackcomposegalleryapp.presentation.navigation.Route.Settings
import com.example.jetpackcomposegalleryapp.presentation.search.SearchScreen
import com.example.jetpackcomposegalleryapp.presentation.settings.SettingsScreen
import com.example.jetpackcomposegalleryapp.trial.DirtyTestScreen

@Suppress("EffectKeys")
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GalleryNavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sharedViewModel: GalleryViewModel = hiltViewModel()
    val permissionToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permission ->
        val isGranted = permission.values.all { it }
        sharedViewModel.setEvent(GalleryEvent.PermissionResult(isGranted))

    }
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            sharedViewModel.setEvent(GalleryEvent.LoadMedia)
            sharedViewModel.setEvent(GalleryEvent.ExitSelectionMode)
        }
    }

    LaunchedEffect(Unit) {
        sharedViewModel.effect.collect { effect ->
            when (effect) {
                is GalleryEffect.RequestPermission -> permissionLauncher.launch(permissionToRequest)
                is GalleryEffect.NavigateToDetail -> navController.navigate(Detail(effect.mediaId))
                is GalleryEffect.ShareMedia -> {
                    MediaIntents.shareMediaBatch(context, effect.uris)
                    sharedViewModel.setEvent(GalleryEvent.ExitSelectionMode)
                }

                is GalleryEffect.CopyMedia -> {
                    MediaIntents.copyToClipboard(context, effect.uris)

                }

                is GalleryEffect.EditMedia -> {
                    MediaIntents.editMedia(context, effect.uriString, effect.mimeType)
                }

                is GalleryEffect.DeleteMedia -> {
                    val uris = effect.uris.map { Uri.parse(it) }
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val pendingIntent =
                                MediaStore.createDeleteRequest(context.contentResolver, uris)
                            deleteLauncher.launch(
                                Builder(pendingIntent.intentSender).build()
                            )
                        } else {
                            uris.forEach { context.contentResolver.delete(it, null, null) }
                            sharedViewModel.setEvent(GalleryEvent.LoadMedia)
                            sharedViewModel.setEvent(GalleryEvent.ExitSelectionMode)
                        }
                    } catch (e: SecurityException) {
                        Toast.makeText(context, "Cannot delete these files", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                GalleryEffect.NavigateToSettings -> navController.navigate(Settings)
                GalleryEffect.NavigateToSearch -> navController.navigate(Route.Search)
            }
        }


    }

    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = DirtyTest,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),


            ) {
            composable<DirtyTest> {
                DirtyTestScreen()
            }
            composable<Gallery>(
                enterTransition = { fadeIn(tween(300)) },
                exitTransition = { fadeOut(tween(300, delayMillis = 90)) },
                popEnterTransition = { fadeIn(tween(300)) },
                popExitTransition = { fadeOut(tween(300)) }) {

                GalleryScreen(
                    onRequestPermission = { permissionLauncher.launch(permissionToRequest) },
                    viewModel = sharedViewModel,

                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable
                )
            }

            composable<Detail>(

                enterTransition = { fadeIn(tween(300)) },
                exitTransition = { fadeOut(tween(300, delayMillis = 90)) },
                popEnterTransition = { fadeIn(tween(300)) },
                popExitTransition = { fadeOut(tween(300)) }) { backStackEntry ->
                val args = backStackEntry.toRoute<Detail>()


                DetailScreen(
                    initialMediaId = args.mediaId,
                    onNavigateBack = { navController.popBackStack() },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable,
                    viewModel = sharedViewModel
                )
            }
            composable<Settings> {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() })
            }

            composable<Route.Search>(
                enterTransition = { fadeIn(tween(300)) },
                exitTransition = { fadeOut(tween(300, delayMillis = 90)) },
                popEnterTransition = { fadeIn(tween(300)) },
                popExitTransition = { fadeOut(tween(300)) }
            ) {
                SearchScreen(
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable,
                    onNavigateBack = {
                        sharedViewModel.setEvent(GalleryEvent.ClearSearchMediaList)
                        navController.popBackStack()
                    },
                    onNavigateToDetail = { mediaId -> navController.navigate(Detail(mediaId)) },
                    galleryViewModel = sharedViewModel
                )
            }
        }
    }
}