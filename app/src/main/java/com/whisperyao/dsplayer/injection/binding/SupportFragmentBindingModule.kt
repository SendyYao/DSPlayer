package com.whisperyao.dsplayer.injection.binding


import androidx.fragment.app.Fragment
import com.whisperyao.dsplayer.activity.HomeActivity
import com.whisperyao.dsplayer.fragment.ContainerFragment
import com.whisperyao.dsplayer.fragment.ContainerSongFragment
import com.whisperyao.dsplayer.fragment.FileSongFragment
import com.whisperyao.dsplayer.fragment.HomePageDefaultGenreFragment
import com.whisperyao.dsplayer.fragment.HomePagePinsFragment
import com.whisperyao.dsplayer.fragment.LyricFragment
import com.whisperyao.dsplayer.fragment.PhoneLyricFragment
import com.whisperyao.dsplayer.fragment.PhoneLyricViewXFragment
import com.whisperyao.dsplayer.fragment.PlayerFragment
import com.whisperyao.dsplayer.fragment.PlayingQueueFragment
import com.whisperyao.dsplayer.fragment.PlaylistFragment
import com.whisperyao.dsplayer.fragment.PlaylistSongFragment
import com.whisperyao.dsplayer.fragment.RadioFragment
import com.whisperyao.dsplayer.fragment.RatingFragment
import com.whisperyao.dsplayer.fragment.TestFragment
import com.whisperyao.dsplayer.injection.Constants
import com.whisperyao.dsplayer.injection.module.SupportFragmentModule
import com.whisperyao.dsplayer.injection.module.ManagerModule
import com.whisperyao.dsplayer.ui.volume.VolumeDialog
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import javax.inject.Named

@Module
abstract class SupportFragmentBindingModule {

    @ContributesAndroidInjector(
        modules = [ContainerFragmentInstanceModule::class]
    )
    abstract fun containerFragment(): ContainerFragment

    @ContributesAndroidInjector(
        modules = [ContainerSongFragmentInstanceModule::class]
    )
    abstract fun containerSongFragment(): ContainerSongFragment

    @ContributesAndroidInjector(
        modules = [FileSongFragmentInstanceModule::class]
    )
    abstract fun fileSongFragment(): FileSongFragment

    @ContributesAndroidInjector(
        modules = [HomePageDefaultGenreFragmentInstanceModule::class]
    )
    abstract fun homePageDefaultGenreFragment(): HomePageDefaultGenreFragment

    @ContributesAndroidInjector(
        modules = [TestFragmentInstanceModule::class]
    )
    abstract fun testFragment(): TestFragment

    @ContributesAndroidInjector(
        modules = [HomePagePinsFragmentInstanceModule::class]
    )
    abstract fun homePagePinsFragment(): HomePagePinsFragment

    @ContributesAndroidInjector(
        modules = [PhoneLyricFragmentInstanceModule::class]
    )
    abstract fun phoneLyricFragment(): PhoneLyricFragment

    @ContributesAndroidInjector(
        modules = [PhoneLyricViewXFragmentInstanceModule::class]
    )
    abstract fun phoneLyricViewXFragment(): PhoneLyricViewXFragment

    @ContributesAndroidInjector(
        modules = [PlayerFragmentInstanceModule::class]
    )
    abstract fun playerFragment(): PlayerFragment

    @ContributesAndroidInjector(
        modules = [PlayingQueueFragmentInstanceModule::class]
    )
    abstract fun playingQueueFragment(): PlayingQueueFragment

    @ContributesAndroidInjector(
        modules = [PlaylistFragmentInstanceModule::class]
    )
    abstract fun playlistFragment(): PlaylistFragment

    @ContributesAndroidInjector(
        modules = [PlaylistSongFragmentInstanceModule::class]
    )
    abstract fun playlistSongFragment(): PlaylistSongFragment

    @ContributesAndroidInjector(
        modules = [RadioFragmentInstanceModule::class]
    )
    abstract fun radioFragment(): RadioFragment

    @ContributesAndroidInjector(
        modules = [RatingFragmentInstanceModule::class]
    )
    abstract fun ratingFragment(): RatingFragment


    @ContributesAndroidInjector(
        modules = [VolumeFragmentInstanceModule::class]
    )
    abstract fun volumeDialog(): VolumeDialog

    @Module(
        includes = [
            SupportFragmentModule::class,
            ManagerModule::class
        ]
    )
    class PlayingQueueFragmentInstanceModule {

        @Provides
        fun provideFragment(
            fragment: PlayingQueueFragment
        ): Fragment = fragment
    }

    @Module(
        includes = [
            SupportFragmentModule::class,
            ManagerModule::class
        ]
    )
    class VolumeFragmentInstanceModule {

        @Provides
        fun provideFragment(
            fragment: VolumeDialog
        ): Fragment = fragment
    }

    @Module(
        includes = [
            SupportFragmentModule::class,
            ManagerModule::class
        ]
    )
    class PlayerFragmentInstanceModule {

        @Provides
        fun provideFragment(
            playerFragment: PlayerFragment
        ): Fragment {
            return playerFragment
        }

        @Provides
        fun provideLyricFragment(
            @Named(Constants.LESS_THEN_10_INCH)
            lessThen10: Boolean
        ): LyricFragment {
            return if (lessThen10) {
                PhoneLyricViewXFragment()
                // PhoneLyricFragment()
            } else {
                // TabletLyricFragment()
                PhoneLyricFragment()
            }
        }

        @Provides
        fun providePlayingQueueFragment(
            playerFragment: PlayerFragment
        ): PlayingQueueFragment {

            return PlayingQueueFragment().apply {
                val controller =
                    (playerFragment.activity as? HomeActivity)?.provideController()

                controller?.let {
                    bindController(it)
                    setInitialPlaybackState(it.playbackState)
                }
            }
        }
    }

    @Module(includes = [SupportFragmentModule::class])
    class ContainerSongFragmentInstanceModule {
        @Provides
        fun provideFragment(
            fragment: ContainerSongFragment
        ): Fragment = fragment
    }

    @Module(includes = [SupportFragmentModule::class])
    class ContainerFragmentInstanceModule {
        @Provides
        fun provideFragment(
            fragment: ContainerFragment
        ): Fragment = fragment
    }

    @Module(includes = [SupportFragmentModule::class])
    class RatingFragmentInstanceModule {
        @Provides
        fun provideFragment(
            fragment: RatingFragment
        ): Fragment = fragment
    }

    @Module(includes = [SupportFragmentModule::class])
    class RadioFragmentInstanceModule {
        @Provides
        fun provideFragment(
            fragment: RadioFragment
        ): Fragment = fragment
    }

    @Module(includes = [SupportFragmentModule::class])
    class PlaylistFragmentInstanceModule {
        @Provides
        fun provideFragment(
            fragment: PlaylistFragment
        ): Fragment = fragment
    }

    @Module(includes = [SupportFragmentModule::class])
    class PlaylistSongFragmentInstanceModule {
        @Provides
        fun provideFragment(
            fragment: PlaylistSongFragment
        ): Fragment = fragment
    }

    @Module(includes = [SupportFragmentModule::class])
    class FileSongFragmentInstanceModule {
        @Provides
        fun provideFragment(
            fragment: FileSongFragment
        ): Fragment = fragment
    }

    @Module(includes = [SupportFragmentModule::class])
    class HomePageDefaultGenreFragmentInstanceModule {
        @Provides
        fun provideFragment(
            fragment: HomePageDefaultGenreFragment
        ): Fragment = fragment
    }

    @Module(includes = [SupportFragmentModule::class])
    class TestFragmentInstanceModule {
        @Provides
        fun provideFragment(
            fragment: TestFragment
        ): Fragment = fragment
    }

    @Module(includes = [SupportFragmentModule::class])
    class HomePagePinsFragmentInstanceModule {
        @Provides
        fun provideFragment(
            fragment: HomePagePinsFragment
        ): Fragment = fragment
    }

    @Module(includes = [SupportFragmentModule::class])
    class PhoneLyricFragmentInstanceModule {
        @Provides
        fun provideFragment(
            fragment: PhoneLyricFragment
        ): Fragment = fragment
    }

//    @Module(includes = [SupportFragmentModule::class])
//    class TabletLyricFragmentInstanceModule {
//        @Provides
//        fun provideFragment(
//            fragment: TabletLyricFragment
//        ): Fragment = fragment
//    }

    @Module(includes = [SupportFragmentModule::class])
    class PhoneLyricViewXFragmentInstanceModule {
        @Provides
        fun provideFragment(
            fragment: PhoneLyricViewXFragment
        ): Fragment = fragment
    }
}