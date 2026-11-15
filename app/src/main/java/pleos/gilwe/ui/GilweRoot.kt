package pleos.gilwe.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import pleos.gilwe.data.Catalog
import pleos.gilwe.data.Extras
import pleos.gilwe.vehicle.GearState

@Composable
fun GilweRoot(vm: AppViewModel, onExit: () -> Unit) {
    val screen by vm.screen.collectAsState()
    val lang by vm.screenLang.collectAsState()
    val audio by vm.audioLang.collectAsState()
    val category by vm.category.collectAsState()
    val start by vm.start.collectAsState()
    val trip by vm.trip.collectAsState()
    val vehicle by vm.vehicle.collectAsState()
    val restricted by vm.restricted.collectAsState()
    val stamps by vm.stamps.collectAsState()
    val speaking by vm.speakingId.collectAsState()
    val query by vm.searchQuery.collectAsState()
    val gearOverride by vm.gearOverride.collectAsState()
    val playback by vm.playback.collectAsState()
    var showQr by remember { mutableStateOf(false) }
    var showDemo by remember { mutableStateOf(false) }

    BackHandler { if (!vm.back()) onExit() }

    Box(Modifier.fillMaxSize().background(C.Bg)) {
        when (val s = screen) {
            Screen.Intro -> IntroScreen(
                lang, vm.hasHistory,
                onLang = { vm.go(Screen.ScreenLanguage(Screen.Intro)) },
                onStart = vm::getStarted,
                onContinue = { vm.go(Screen.Home) },
            )
            is Screen.ScreenLanguage -> LanguageScreen(
                title = lang.strings.chooseScreenLanguage, selected = lang,
                onBack = { vm.back() }, onPick = vm::setScreenLang, header = "",
            )
            Screen.Home -> HomeScreen(
                lang, start, category, vm::setCategory,
                onLang = { vm.go(Screen.ScreenLanguage(Screen.Home)) },
                onStamps = { vm.go(Screen.MyStamps) },
                onChangeStart = { vm.go(Screen.StartSearch(false)) },
                onExplore = { vm.go(Screen.RoutePreview(it.id)) },
                onDemoTools = { showDemo = true },
            )
            is Screen.StartSearch -> StartSearchScreen(
                lang, s.showResults, query, vm::setSearchQuery,
                onSubmit = { vm.go(Screen.StartSearch(true)) },
                onBack = { vm.back() },
                onChoose = vm::chooseStart,
            )
            is Screen.RoutePreview -> {
                val course = Catalog.course(s.courseId)
                RoutePreviewScreen(
                    lang, course, start, introPlaying = speaking == "intro:${course.id}",
                    onBack = { vm.back() },
                    onToggleIntro = { vm.startTripWithIntro(course.id) },
                    onChooseAudio = { vm.go(Screen.AudioLanguage(course.id)) },
                )
            }
            is Screen.AudioLanguage -> LanguageScreen(
                title = lang.strings.chooseAudioLanguage, selected = audio,
                onBack = { vm.back() }, onPick = { vm.startTrip(s.courseId, it) },
                header = Catalog.course(s.courseId).place.of(lang),
            )
            Screen.Drive -> trip?.let { t ->
                DriveScreen(
                    lang, t, audio, vehicle,
                    onTogglePause = vm::toggleStoryPause,
                    onNext = vm::skipToNextStory,
                    onDemoArrive = vm::skipToArrival,
                    onDemoTools = { showDemo = true },
                    onShowNavi = { vm.showNavigation() },
                    onRetryLocation = { vm.retryLocation() },
                )
            }
            Screen.Park -> trip?.let { t -> ParkScreen(lang, t.course, vehicle, vm::checkIn, onDemoTools = { showDemo = true }) }
            is Screen.StampCollected -> {
                val course = Catalog.course(s.courseId)
                StampCollectedScreen(
                    lang, course, stamps.map { Catalog.course(it.courseId) },
                    onExplore = { vm.go(Screen.Nearby(course.id)) },
                    onMyStamps = { vm.go(Screen.MyStamps) },
                    onSaveToPhone = { showQr = true },
                )
            }
            Screen.MyStamps -> MyStampsScreen(
                lang, stamps,
                onBack = { vm.back() },
                onSaveToPhone = { showQr = true },
                onPreviewNext = { vm.go(Screen.RoutePreview(it.id)) },
                onStories = { vm.go(Screen.RouteStories(it.id)) },
            )
            is Screen.RouteStories -> RouteStoriesScreen(
                lang, audio, Catalog.course(s.courseId),
                stamps.lastOrNull { it.courseId == s.courseId }, speaking,
                onBack = { vm.stopVoice(); vm.back() },
                onPlay = vm::replay,
                onOpen = vm::openStory,
            )
            is Screen.StoryListen -> Extras.story(s.storyId)?.let { (course, story) ->
                StoryListenScreen(
                    lang, audio, course, story, playback,
                    playing = speaking == story.id && playback?.paused != true,
                    restricted = restricted,
                    onBack = { vm.stopVoice(); vm.back() },
                    onTogglePlay = { vm.toggleListen(story) },
                    onSkipLine = vm::skipLine,
                    onOpenStory = vm::openStory,
                    onFull = { vm.go(Screen.StoryFull(story.id)) },
                )
            }
            is Screen.StoryFull -> Extras.story(s.storyId)?.let { (course, story) ->
                StoryFullScreen(
                    lang, audio, course, story, restricted,
                    onBack = { vm.back() },
                    onBackToListening = { vm.back() },
                )
            }
            is Screen.Nearby -> NearbyScreen(lang, Catalog.course(s.courseId), onBack = { vm.back() }, onMyStamps = { vm.go(Screen.MyStamps) })
        }

        // 주행 규제 중: 탐색·목록 화면은 가리고 주행 화면만 허용 (CarUxRestrictions)
        val browsing = screen !is Screen.Drive && screen !is Screen.Park
        if (restricted && browsing) {
            Box(Modifier.fillMaxSize().background(C.Bg.copy(alpha = 0.96f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    GilweLogo(size = 72.dp, nameSize = 32)
                    Spacer(Modifier.height(24.dp))
                    Text(lang.strings.drivingLimited, style = Type.h2)
                    if (trip != null) {
                        Spacer(Modifier.height(28.dp))
                        PillButton(lang.strings.continueLabel, { vm.go(Screen.Drive) }, Modifier.width(360.dp))
                    }
                }
            }
        }
    }

    if (showQr) SaveToPhoneDialog(lang, stamps, onDismiss = { showQr = false })

    // 시연 도구: 홈 로고 탭 → 기어 강제 (에뮬레이터에서 Virtual Console 없이도 흐름 시연)
    if (showDemo) {
        Dialog(onDismissRequest = { showDemo = false }) {
            CardBox(Modifier.width(620.dp), color = C.Card) {
                Text("Demo · Gear", style = Type.title)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Vehicle SDK: " + if (vehicle.gearReadable) "gear=${vehicle.gear}" else "gear unavailable",
                    style = Type.sub,
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SmallPill("Real", { vm.setGearOverride(null) }, selected = gearOverride == null)
                    SmallPill("P", { vm.setGearOverride(GearState.PARK) }, selected = gearOverride == GearState.PARK)
                    SmallPill("D", { vm.setGearOverride(GearState.DRIVE) }, selected = gearOverride == GearState.DRIVE)
                }
                Spacer(Modifier.height(24.dp))
                PillButton(lang.strings.close, { showDemo = false }, Modifier.padding(top = 4.dp))
            }
        }
    }
}
