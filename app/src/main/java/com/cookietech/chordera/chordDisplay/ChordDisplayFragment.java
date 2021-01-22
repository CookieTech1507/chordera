package com.cookietech.chordera.chordDisplay;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.SpannableStringBuilder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.cookietech.chordera.R;
import com.cookietech.chordera.Room.SongDataEntity;
import com.cookietech.chordera.Room.SongsEntity;
import com.cookietech.chordera.Util.NativeAdsFragment;
import com.cookietech.chordera.appcomponents.Constants;
import com.cookietech.chordera.appcomponents.NavigatorTags;
import com.cookietech.chordera.appcomponents.RemoteConfigManager;
import com.cookietech.chordera.chordDisplay.chordDetails.ChordDetailsDialogFragment;
import com.cookietech.chordera.chordDisplay.chordFormatter.ChordFormater;
import com.cookietech.chordera.databinding.FragmentChordDisplayBinding;
import com.cookietech.chordera.fragments.ChorderaFragment;
import com.cookietech.chordera.models.SelectionType;
import com.cookietech.chordera.models.SongsPOJO;
import com.cookietech.chordera.models.TabPOJO;
import com.cookietech.chordera.repositories.DatabaseResponse;

import com.cookietech.chordlibrary.ChordClass;
import com.cookietech.chordlibrary.Variation;

import com.cookietech.chordlibrary.ChordsAdapter;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ChordDisplayFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChordDisplayFragment extends ChorderaFragment implements ChordsDisplayAdapter.Communicator {


    FragmentChordDisplayBinding binding;
    private  ChordsDisplayAdapter chordsDisplayAdapter;
    ArrayList<Variation> chords =new ArrayList<>();
    private boolean isDarkModeActivated = false;
    private int lastSelectedTranspose;
    private SongsPOJO selectedSong;
    private SelectionType selectedTab;
    private TabPOJO tabData;
    private ImageView auto_scroll_btn;
    private ImageView play_youtube_btn;
    private TabulatorGenerator tabulatorGenerator = new TabulatorGenerator();
    private double autoScrollSpeed = 1;
    private ArrayList<ChordClass> initialChordList;

    public ChordDisplayFragment() {
        // Required empty public constructor
    }


    public static ChordDisplayFragment newInstance() {
        ChordDisplayFragment fragment = new ChordDisplayFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentChordDisplayBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        lastSelectedTranspose = 0;
        setDummyChords();
        initializeObserver();
        initializeChordsRecyclerView();
        initializeAutoScrollSpeedUi();

        setupMenuSelector();

        toggleMode();

        if(RemoteConfigManager.shouldShowChordDisplayNativeAds())
            setUpNativeAdFragment();



        binding.modeSwitch.setChecked(isDarkModeActivated);
        binding.modeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                isDarkModeActivated = isChecked;
                toggleMode();

                if(isDarkModeActivated){
                    binding.modeAnimationView.setText("Dark");
                    binding.modeAnimationView.setTextColor(Color.WHITE);
                    ObjectAnimator animation = ObjectAnimator.ofFloat(binding.modeAnimationView, View.ALPHA, 0f,1f,0f);
                    ObjectAnimator zoomAnimationX = ObjectAnimator.ofFloat(binding.modeAnimationView, View.SCALE_X, 0.5f,1f);
                    ObjectAnimator zoomAnimationY = ObjectAnimator.ofFloat(binding.modeAnimationView, View.SCALE_Y, 0.5f,1f);
                    AnimatorSet animatorSet = new AnimatorSet();
                    animatorSet.setDuration(250);
                    animatorSet.playTogether(animation,zoomAnimationX,zoomAnimationY);
                    animatorSet.start();

                }else{
                    binding.modeAnimationView.setText("Light");
                    binding.modeAnimationView.setTextColor(Color.parseColor("#22374C"));
                    ObjectAnimator animation = ObjectAnimator.ofFloat(binding.modeAnimationView, View.ALPHA, 0f,1f,0f);
                    ObjectAnimator zoomAnimationX = ObjectAnimator.ofFloat(binding.modeAnimationView, View.SCALE_X, 0.5f,1f);
                    ObjectAnimator zoomAnimationY = ObjectAnimator.ofFloat(binding.modeAnimationView, View.SCALE_Y, 0.5f,1f);
                    AnimatorSet animatorSet = new AnimatorSet();
                    animatorSet.setDuration(250);
                    animatorSet.playTogether(animation,zoomAnimationX,zoomAnimationY);
                    animatorSet.start();
                }

            }
        });

        binding.btnSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ChordDisplaySettingModal moreFragmentDialog = ChordDisplaySettingModal.newInstance();
                moreFragmentDialog.setCallback(new ChordDisplaySettingModal.MoreCallback() {

                    @Override
                    public void onTransposeSelected() {
                        ChordDisplayTransposeModal transposeModalDialog = ChordDisplayTransposeModal.newInstance(tabData.getKey(),lastSelectedTranspose);
                        transposeModalDialog.setCallback(new ChordDisplayTransposeModal.TransposeCallback() {
                            @Override
                            public void onTranspose(int transpose) {
                                Log.d("transpose_debug", "onTranspose: " + transpose);
                                lastSelectedTranspose = transpose;
                                binding.tvSongChords.setTranspose(transpose);
                                mainViewModel.transposeChords(initialChordList,transpose);
                            }
                        });
                        transposeModalDialog.show(requireFragmentManager(),"transpose_dialog");
                    }

                    @Override
                    public void onPrintSelected() {
                        Log.d("more_debug", "onPrintSelected: ");
                    }

                    @Override
                    public void onShareSelected() {
                        Log.d("more_debug", "onShareSelected: ");
                    }

                    @Override
                    public void onSettingSelected() {
                        Log.d("more_debug", "onSettingSelected: ");
                    }

                    @Override
                    public void onBackToHomeSelected() {
                        Log.d("more_debug", "onBackToHomeSelected: ");
                        mainViewModel.setNavigation(NavigatorTags.LANDING_FRAGMENT,NavigatorTags.CONTAINER_ID_DEFAULT);
                    }
                });
                moreFragmentDialog.show(requireFragmentManager(),"more_dialog");
            }
        });

        final BottomSheetBehavior<View> behavior = BottomSheetBehavior.from((View) binding.bottomSheet.chordDisplayBottomSheet);



        binding.displayScrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        /**Go to Auto Scroll fragment**/
        auto_scroll_btn = binding.rootLayout.findViewById(R.id.auto_scroll_btn);
        play_youtube_btn = binding.rootLayout.findViewById(R.id.play_youtube_btn);

        auto_scroll_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(requireContext(), "Hey", Toast.LENGTH_SHORT).show();
                mainViewModel.setNavigation(NavigatorTags.CHORD_DISPLAY_FULLSCREEN_FRAGMENT,1);
            }
        });

        binding.btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requireActivity().onBackPressed();
            }
        });

        play_youtube_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Log.d("youtube", "onClick: " + selectedSong.getYoutube_id());
                watchYoutubeVideo(requireContext(),selectedSong.getYoutube_id());
            }
        });

        /**Download Section**/
        binding.downloadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(requireContext(), "Hey Baby", Toast.LENGTH_SHORT).show();
                downloadSong();
            }
        });

        ChordTouchListener chordTouchListener = new ChordTouchListener(new ChordTouchListener.chordSelectionListener() {
            @Override
            public void onChordSelected(ChordClass chordClass) {
                ChordDetailsDialogFragment.newInstance(chordClass).show(requireFragmentManager(),"chord_selection");
            }
        });
        binding.tvSongChords.setOnTouchListener(chordTouchListener);



    }

    private void setUpNativeAdFragment() {
        FragmentTransaction transaction = requireFragmentManager().beginTransaction();
        Fragment adFragment = NativeAdsFragment.newInstance();
        transaction.add(binding.nativeAdHolder.getId(),adFragment);
        transaction.commitAllowingStateLoss();
    }

    private void initializeAutoScrollSpeedUi() {
        String speed = String.format("%.1f", autoScrollSpeed);
        binding.bottomSheet.autoscrollSpeedTv.setText("Speed "+speed+"x");
        binding.bottomSheet.autoscrollSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                autoScrollSpeed = (progress / 100f) + 0.5;
                String speed = String.format("%.1f", autoScrollSpeed);
                binding.bottomSheet.autoscrollSpeedTv.setText("Speed " + speed + "x");
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void setupMenuSelector() {
        final ArrayList<SelectionType> selectionTypeArrayList = new ArrayList<>();
        binding.menuSelector.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ViewGroup root = (ViewGroup) requireActivity().getWindow().getDecorView().findViewById(android.R.id.content);
                String[] limits = new String[]{"Guitar Chord", "Lyrics"};
                final View view = new View(requireContext());
                view.setLayoutParams(new ViewGroup.LayoutParams(1, 1));
                view.setBackgroundColor(Color.TRANSPARENT);

                root.addView(view);
                float toolbarheight = binding.toolbar.getHeight();
                view.setX(0);
                view.setY(toolbarheight);
                Context wrapper = new ContextThemeWrapper(requireContext(), R.style.PopupMenuStyle);
                PopupMenu popupMenu = new PopupMenu(wrapper, view, Gravity.CENTER);

                if(selectedSong != null){
                    Map<String, String> map = selectedSong.getSong_data();
                    Log.d("sohan_debug","map_size:"+String.valueOf(map.size()));
                    int i = 0;
                    for (Map.Entry<String, String> entry : map.entrySet()) {
                        // here entry.getKey() returns like 'guitar_chord'
                        // SelectionType.displaySelectionNameMap.get(entry.getKey()) returns Guitar Chord
                        if(SelectionType.displaySelectionNameMap.containsKey(entry.getKey()) && !entry.getKey().equals("guitar_chord"))
                        {
                            popupMenu.getMenu().add(1,i,i,SelectionType.displaySelectionNameMap.get(entry.getKey()));  //here assigning i as temporary item id
                            selectionTypeArrayList.add(new SelectionType(entry.getKey(), SelectionType.displaySelectionNameMap.get(entry.getKey()),String.valueOf(entry.getValue())));
                            i++;
                        }
                        else
                        {
                            Log.d("sohan_debug","key not found");
                            //TODO need to handle key not found
                        }
                    }

                }else{
                    Log.d("tab_debug", "getData: no data found");
                }
                popupMenu.setOnDismissListener(new PopupMenu.OnDismissListener()
                {
                    @Override
                    public void onDismiss(PopupMenu menu)
                    {
                        root.removeView(view);
                    }
                });

                popupMenu.show();

                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        //TODO if you have new selection type you should add here logic for that
                        Log.d("sohan_debug", (String) item.getTitle());
                        SelectionType selectionType = selectionTypeArrayList.get(item.getItemId());
                        if(((String) item.getTitle()).equals(SelectionType.displaySelectionNameMap.get("lyrics")))
                        {
                            mainViewModel.setSelectedTab(selectionType);
                            mainViewModel.setNavigation(NavigatorTags.SONG_DETAIL_FRAGMENT,1);
                        }
                        return true;
                    }
                });
            }
        });

    }

    private void downloadSong() {
        mainViewModel.roomInsertSongData(new SongDataEntity(tabData.getId(),tabData.getData(),tabData.getKey(),tabData.getTuning(),tabData.getData_type()));
    }

    private void initializeChordsRecyclerView() {
        binding.rvChords.setHasFixedSize(true);
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(),5);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        binding.rvChords.setLayoutManager(layoutManager);
        chordsDisplayAdapter = new ChordsDisplayAdapter(requireContext(),new ArrayList<ChordClass>(),this,binding.rvChords);
        binding.rvChords.setAdapter(chordsDisplayAdapter);
    }

    private void initializeObserver() {
        selectedSong = mainViewModel.getObservableSelectedSong().getValue();
        mainViewModel.getObservableSelectedSong().observe(fragmentLifecycleOwner, new Observer<SongsPOJO>() {
            @Override
            public void onChanged(SongsPOJO songsPOJO) {
                Log.d("tab_debug", "onChanged: " + songsPOJO.getSong_name());
                selectedSong = songsPOJO;
                updateView();
            }
        });
        mainViewModel.getObservableSelectedTab().observe(fragmentLifecycleOwner, new Observer<SelectionType>() {
            @Override
            public void onChanged(SelectionType selectionType) {
                selectedTab = selectionType;
                mainViewModel.loadTab(selectedTab);

            }
        });

       mainViewModel.getObservableSelectedTabLiveData().observe(fragmentLifecycleOwner, new Observer<TabPOJO>() {
           @Override
           public void onChanged(TabPOJO tabPOJO) {
                tabData = tabPOJO;
                mainViewModel.decodeChordsFromData(tabPOJO.getData());
                updateView();
           }
       });


        Log.d("from_debug", "initializeObserver: " + mainViewModel.getObservableSongListShowingCalledFrom().getValue());
        mainViewModel.getObservableSongListShowingCalledFrom().observe(fragmentLifecycleOwner, new Observer<String>() {
            @Override
            public void onChanged(String fromWhere) {
                if(fromWhere.equalsIgnoreCase(Constants.FROM_SAVED)){
                    binding.downloadBtn.setVisibility(View.GONE);
                }else{
                    binding.downloadBtn.setVisibility(View.VISIBLE);
                }
            }
        });

       mainViewModel.getObservableDownloadSongDataResponse().observe(fragmentLifecycleOwner, new Observer<DatabaseResponse>() {
           @Override
           public void onChanged(DatabaseResponse databaseResponse) {
               switch (databaseResponse.getResponse()){
                   case Storing:
                       Log.d("download_debug", "onChanged: song data storing");
                       binding.downloadBtn.setVisibility(View.INVISIBLE);
                       binding.downloadProgress.setVisibility(View.VISIBLE);
                        break;
                   case Stored:
                       Log.d("download_debug", "onChanged: song data stored");
                       Map<String,String> songDataMap = new HashMap<>();
                       songDataMap.put(tabData.getData_type(),tabData.getId());
                       mainViewModel.roomInsertSong(new SongsEntity(selectedSong.getId(),selectedSong.getArtist_name(),selectedSong.getSong_name(), selectedSong.getGenre(),selectedSong.getImage_url(),selectedSong.getSong_duration(),songDataMap,selectedSong.getYoutube_id()));
                       binding.downloadBtn.setVisibility(View.VISIBLE);
                       binding.downloadBtn.setImageResource(R.drawable.downloaded_icon);
                       binding.downloadProgress.setVisibility(View.INVISIBLE);
                       binding.downloadBtn.setOnClickListener(null);
                       break;
                   case Already_exist:
                       Log.d("download_debug", "onChanged: song data already exist");
                       Toast.makeText(requireContext(), "You Already downloaded this chord", Toast.LENGTH_SHORT).show();
                       binding.downloadBtn.setVisibility(View.VISIBLE);
                       binding.downloadBtn.setImageResource(R.drawable.downloaded_icon);
                       binding.downloadProgress.setVisibility(View.INVISIBLE);
                       binding.downloadBtn.setOnClickListener(null);
                       break;

               }

           }
       });

       mainViewModel.getObservableDownloadSongResponse().observe(fragmentLifecycleOwner, new Observer<DatabaseResponse>() {
           @Override
           public void onChanged(DatabaseResponse databaseResponse) {
               switch (databaseResponse.getResponse()){
                   case Storing:
                       Log.d("download_debug", "onChanged: song storing");
                       break;
                   case Stored:
                       Log.d("download_debug", "onChanged: song stored");
                       break;
                   case Already_exist:
                       Log.d("download_debug", "onChanged: song already exist");

               }
           }
       });
       mainViewModel.getObservableTabDataResponse().observe(fragmentLifecycleOwner, new Observer<DatabaseResponse>() {
           @Override
           public void onChanged(DatabaseResponse databaseResponse) {
               switch (databaseResponse.getResponse()){
                   case Invalid_data:
                       Log.d("callback_debug", "onChanged: Invalid_data");
                       break;
                   case No_internet:
                       Log.d("callback_debug", "onChanged: No_internet");
                       break;
                   case Fetching:
                       Log.d("callback_debug", "onChanged: Fetching");
                       break;
                   case Fetched:
                       Log.d("callback_debug", "onChanged: Fetched");
                       break;
                   case Error:
                       Log.d("callback_debug", "onChanged: Error");
                       break;
                   default:
                       break;
               }
           }
       });

       mainViewModel.getObservableTabDisplayChords().observe(fragmentLifecycleOwner, new Observer<ArrayList<ChordClass>>() {
           @Override
           public void onChanged(ArrayList<ChordClass> chordClasses) {
               initialChordList = chordClasses;
               chordsDisplayAdapter.setChords(chordClasses);
           }
       });


       mainViewModel.getObservableTransposedTabDisplayChords().observe(fragmentLifecycleOwner, new Observer<ArrayList<ChordClass>>() {
           @Override
           public void onChanged(ArrayList<ChordClass> chordClassArrayList) {
               chordsDisplayAdapter.setChords(chordClassArrayList);
           }
       });

    }

    private void updateView() {
        if(selectedSong != null){
            binding.tvSongName.setText(selectedSong.getSong_name());
            binding.tvBandName.setText(selectedSong.getArtist_name());
            binding.tvGenre.setText("Genre: "+ selectedSong.getGenre());
        }

        if(tabData != null){
            binding.tvTuning.setText("Tuning: "+ tabData.getTuning());
            binding.tvKey.setText("Key: "+ tabData.getKey());
            //binding.tvSongChords.setText(tabData.getData());
        }

/*        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        String lyricWithChord = "[Dm]এই অ[Am]বেলাই [Gm]তোমারি আকা[Bdim]শে  নিরব[Gaug] আপোসে ভেসে [G]যাই[Tab]";
        //Rect bounds = new Rect();
        //TextPaint textPaint = binding.tvChords.getPaint();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager  = requireActivity().getWindowManager();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        int marginPadding = 50;
        int rootWidth = displayMetrics.widthPixels - marginPadding;
        ChordFormater chordFormater = new ChordFormater(tabData.getData(),rootWidth);
        //chordFormater.processChord(0)
        spannableStringBuilder = chordFormater.getProcessedChord(0);*/


        if (tabData != null) {
/*            binding.tvSongChords.setFormattedText("{Intro}\n" +
                    "([Em],[C],[Am],[D]) " +
                    "(x4)\n" +
                    "{Verse 1}\n" +
                    "[Em]   Nona [C]Shopne Gora[Am]a To[D]mar Swmriti\n");*/
            binding.tvSongChords.setFormattedText(tabData.getData());
        }
    }

    private void setDummyChords(){
        chords.clear();
        chords.add(new Variation( new ArrayList<Integer>(){
            {
                add(-1);
                add(0);
                add(2);
                add(2);
                add(2);
                add(0);
            }
        },new ArrayList<Integer>(){
            {
                add(0);
                add(0);
                add(2);
                add(3);
                add(4);
                add(0);
            }
        }));

        chords.add(new Variation( new ArrayList<Integer>(){
            {
                add(-1);
                add(0);
                add(2);
                add(2);
                add(1);
                add(0);
            }
        },new ArrayList<Integer>(){
            {
                add(0);
                add(0);
                add(2);
                add(3);
                add(1);
                add(0);
            }
        }));

        chords.add(new Variation( new ArrayList<Integer>(){
            {
                add(-1);
                add(0);
                add(2);
                add(2);
                add(-1);
                add(-1);
            }
        },new ArrayList<Integer>(){
            {
                add(0);
                add(0);
                add(2);
                add(3);
                add(0);
                add(0);
            }
        }));

        chords.add(new Variation( new ArrayList<Integer>(){
            {
                add(-1);
                add(0);
                add(2);
                add(2);
                add(2);
                add(0);
            }
        },new ArrayList<Integer>(){
            {
                add(0);
                add(0);
                add(2);
                add(3);
                add(4);
                add(0);
            }
        }));

        chords.add(new Variation( new ArrayList<Integer>(){
            {
                add(-1);
                add(0);
                add(2);
                add(2);
                add(1);
                add(0);
            }
        },new ArrayList<Integer>(){
            {
                add(0);
                add(0);
                add(2);
                add(3);
                add(1);
                add(0);
            }
        }));

        chords.add(new Variation( new ArrayList<Integer>(){
            {
                add(-1);
                add(0);
                add(2);
                add(2);
                add(-1);
                add(-1);
            }
        },new ArrayList<Integer>(){
            {
                add(0);
                add(0);
                add(2);
                add(3);
                add(0);
                add(0);
            }
        }));

        chords.add(new Variation( new ArrayList<Integer>(){
            {
                add(-1);
                add(0);
                add(2);
                add(2);
                add(1);
                add(0);
            }
        },new ArrayList<Integer>(){
            {
                add(0);
                add(0);
                add(2);
                add(3);
                add(1);
                add(0);
            }
        }));
    }

    @Override
    public void onChordSelected(ChordClass chordClass) {
        ChordDetailsDialogFragment.newInstance(chordClass).show(requireFragmentManager(),"chord_selection");
    }

    private void activateLightMode(){
        binding.rootLayout.setBackgroundColor(getResources().getColor(R.color.white));
        binding.tvSongName.setTextColor(getResources().getColor(R.color.colorPrimary));
        binding.tvTuning.setTextColor(getResources().getColor(R.color.colorPrimary));
        binding.tvKey.setTextColor(getResources().getColor(R.color.colorPrimary));
        binding.tvChords.setTextColor(getResources().getColor(R.color.colorPrimary));
        binding.tvSongChords.setMode(TabulatorTextView.Mode.Light);
        binding.tvGenre.setTextColor(getResources().getColor(R.color.colorPrimary));
        binding.tvCapo.setTextColor(getResources().getColor(R.color.colorPrimary));
    }

    private void activateDarkMode(){
        binding.rootLayout.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        binding.tvSongName.setTextColor(getResources().getColor(R.color.white));
        binding.tvTuning.setTextColor(getResources().getColor(R.color.white));
        binding.tvKey.setTextColor(getResources().getColor(R.color.white));
        binding.tvChords.setTextColor(getResources().getColor(R.color.white));
        binding.tvSongChords.setMode(TabulatorTextView.Mode.Dark);
        binding.tvGenre.setTextColor(getResources().getColor(R.color.white));
        binding.tvCapo.setTextColor(getResources().getColor(R.color.white));
    }

    private void toggleMode() {

        if (!isDarkModeActivated){
            activateLightMode();
        }
        else {
            activateDarkMode();
        }
    }

    public static void watchYoutubeVideo(Context context, String id){
        Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + id));
        Intent webIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("http://www.youtube.com/watch?v=" + id));
        try {
            context.startActivity(appIntent);
        } catch (ActivityNotFoundException ex) {
            context.startActivity(webIntent);
        }
    }
}