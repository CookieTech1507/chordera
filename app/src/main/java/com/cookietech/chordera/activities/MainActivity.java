package com.cookietech.chordera.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.cookietech.chordera.Landing.LandingFragment;
import com.cookietech.chordera.R;
import com.cookietech.chordera.Splash.SplashFragment;
import com.cookietech.chordera.appcomponents.CookieTechFragmentManager;
import com.cookietech.chordera.appcomponents.NavigatorTags;
import com.cookietech.chordera.architecture.MainViewModel;
import com.cookietech.chordera.databinding.ActivityMainBinding;
import com.cookietech.chordlibrary.Fragment.ChordLibraryFragment;

import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {


    SplashFragment splashFragment;
    LandingFragment landingFragment;
    ChordLibraryFragment chordLibraryFragment;
    ActivityMainBinding binding;
    CookieTechFragmentManager cookieTechFragmentManager;
    MainViewModel mainViewModel;
    Observer<String> navigationObserver;
    long lastBackButtonPressed = 0;


    //skifjoaisdhjfo

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View root = binding.getRoot();
        setContentView(root);

        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        navigationObserver = new Observer<String>() {
            @Override
            public void onChanged(String tag) {
                navigateTo(tag);
            }
        };

        mainViewModel.getNavigation().observe(this,navigationObserver);

        cookieTechFragmentManager = CookieTechFragmentManager.getInstance();
        splashFragment = SplashFragment.newInstance();
        landingFragment = LandingFragment.newInstance();
        chordLibraryFragment = ChordLibraryFragment.newInstance();
        cookieTechFragmentManager.initCookieTechFragmentManager(getSupportFragmentManager());
        navigateTo(NavigatorTags.LANDING_FRAGMENT);
        mainViewModel.setNavigation(NavigatorTags.SPLASH_FRAGMENT);


    }

    private void navigateTo(String tag) {

        if(cookieTechFragmentManager.getIsFragmentAvailable(tag)){
            cookieTechFragmentManager.popFragmentExclusive(tag);
        }else{
            if(tag.equals(NavigatorTags.LANDING_FRAGMENT)){
                cookieTechFragmentManager.addFragmentToBackStackWithAnimation(landingFragment, NavigatorTags.LANDING_FRAGMENT,binding.mainFragmentHolder.getId(), R.anim.enter_zoom_in_fade_in,R.anim.exit_zoom_out_fade_out,R.anim.enter_zoom_in_fade_in,R.anim.exit_zoom_out_fade_out);
            }else if(tag.equals(NavigatorTags.SPLASH_FRAGMENT)){
                cookieTechFragmentManager.addFragmentToBackStackWithAnimation(splashFragment, NavigatorTags.SPLASH_FRAGMENT,binding.mainFragmentHolder.getId(),R.anim.enter_from_left,R.anim.exit_zoom_out_fade_out,R.anim.enter_zoom_in_fade_in,R.anim.exit_to_left);
            }else if(tag.equals(NavigatorTags.CHORD_LIBRARY_FRAGMENT)){
                cookieTechFragmentManager.addFragmentToBackStackWithAnimation(chordLibraryFragment,NavigatorTags.CHORD_LIBRARY_FRAGMENT,binding.mainFragmentHolder.getId(),R.anim.enter_from_right,R.anim.exit_zoom_out_fade_out,R.anim.enter_zoom_in_fade_in,R.anim.exit_to_right);
            }
        }

        Log.d("akash_debug", "navigateTo: " + cookieTechFragmentManager.getFragmentsTagList() + tag);

    }

    @Override
    protected void onStart() {
        super.onStart();
    }


    @Override
    public void onBackPressed() {
        if(Objects.requireNonNull(mainViewModel.getNavigation().getValue()).equals( NavigatorTags.SPLASH_FRAGMENT)){
            return;
        }
        if(Objects.requireNonNull(mainViewModel.getNavigation().getValue()).equals( NavigatorTags.LANDING_FRAGMENT)){
            Toast.makeText(this,"Double tap to exit",Toast.LENGTH_SHORT).show();
            long interval = System.currentTimeMillis() - lastBackButtonPressed;
            if(interval < 1500 && interval > 0){
                finish();
            }else{
                lastBackButtonPressed = System.currentTimeMillis();
            }
            return;
        }

        lastBackButtonPressed = System.currentTimeMillis();

        List<String> taglist = cookieTechFragmentManager.getFragmentsTagList();

        mainViewModel.setNavigation(taglist.get(taglist.size() - 2));
    }
}