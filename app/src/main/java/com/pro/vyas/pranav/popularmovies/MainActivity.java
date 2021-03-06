package com.pro.vyas.pranav.popularmovies;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionButton;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionMenu;
import com.oguzdev.circularfloatingactionmenu.library.SubActionButton;
import com.pro.vyas.pranav.popularmovies.asyncTaskUtils.LoadMasterGenreAsyncTask;
import com.pro.vyas.pranav.popularmovies.asyncTaskUtils.LoadMovieAsyncTask;
import com.pro.vyas.pranav.popularmovies.models.GenreModel;
import com.pro.vyas.pranav.popularmovies.models.MainModel;
import com.pro.vyas.pranav.popularmovies.models.MovieModel;
import com.pro.vyas.pranav.popularmovies.prefencesUtils.SharedPrefenceUtils;
import com.pro.vyas.pranav.popularmovies.recyclerUtils.MovieAdapter;
import com.wang.avi.AVLoadingIndicatorView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.content.res.Configuration.ORIENTATION_PORTRAIT;
import static com.pro.vyas.pranav.popularmovies.constantUtils.Constants.*;

public class MainActivity extends AppCompatActivity implements LoadMovieAsyncTask.LoadMovieAsynCallback, LoadMasterGenreAsyncTask.MasterGenreCallBack {

    private SharedPreferences mainPrefs;

    private static final String TAG = "MainActivity";
    public static String sortByFinal = sortByPopularity;
    private static int currPage = 1;

    @BindView(R.id.rv_main)
    RecyclerView rvMain;
    @BindView(R.id.text_details_main)
    TextView tvData;
    @BindView(R.id.LoadingIndicator)
    AVLoadingIndicatorView loadingIndicatorView;
    @BindView(R.id.image_backgroundProgress_main)
    ImageView ivBackgroundProgress;
    @BindView(R.id.text_progress)
    TextView tvProgress;
    @BindView(R.id.image_no_connection)
    ImageView ivNoConnection;
    @BindView(R.id.toolbar_main)
    Toolbar toolbarMain;

    private Drawer drawer;
    private MovieAdapter adapter;
    List<MovieModel> movie;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.INTERNET,
                        Manifest.permission.ACCESS_NETWORK_STATE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        // check if all permissions are granted
                        if (report.areAllPermissionsGranted()) {
                            preLoadItems();
                        }

                        // check for permanent denial of any permission
                        if (report.isAnyPermissionPermanentlyDenied()) {
                            // permission is denied permenantly, navigate user to app settings
                            AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                            dialog.setTitle("Not Given Permissions");
                            dialog.setMessage("We want permission to acess your Netwok state \nand Internet ");
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                })
                .onSameThread()
                .check();
    }

    private void preLoadItems() {
        AndroidNetworking.initialize(getApplicationContext());
        mainPrefs = getSharedPreferences("firsttimePrefs", Context.MODE_PRIVATE);
        setSupportActionBar(toolbarMain);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        currPage = 1;
        fetchDataFromUrl("1");
        buildDrawer(MainActivity.this, toolbarMain, getSupportActionBar());
        attachFloatingActionMenu();
        ivNoConnection.setVisibility(View.GONE);
        adapter = new MovieAdapter(this);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(this, this.getResources().getConfiguration().orientation == ORIENTATION_PORTRAIT ? 2 : 3);
        rvMain.setAdapter(adapter);
        rvMain.setHasFixedSize(true);
        rvMain.setLayoutManager(layoutManager);
    }

    public void fetchDataFromUrl(final String pageNo) {
        loadingIndicatorView.smoothToShow();
        ivBackgroundProgress.setVisibility(View.VISIBLE);
        tvProgress.setVisibility(View.VISIBLE);
        String[] array = {pageNo};
        LoadMovieAsyncTask loadMovie = new LoadMovieAsyncTask(this, this);
        loadMovie.setProgressIndicatores(loadingIndicatorView, ivBackgroundProgress, tvProgress);
        loadMovie.execute(array);
    }

    public void attachFloatingActionMenu() {
        ImageView mainBtnIcon = new ImageView(this);
        mainBtnIcon.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_input_add));

        final FloatingActionButton actionButton = new FloatingActionButton.Builder(this)
                .setContentView(mainBtnIcon)
                .build();

        SubActionButton.Builder itemBuilder = new SubActionButton.Builder(this);

        SubActionButton sortBtn = addButton(getResources().getDrawable(R.drawable.ic_sort_by), itemBuilder);
        SubActionButton backBtn = addButton(getResources().getDrawable(R.drawable.ic_arrow_back), itemBuilder);
        SubActionButton nextBtn = addButton(getResources().getDrawable(R.drawable.ic_arrow_right), itemBuilder);
        SubActionButton chooseBtn = addButton(getResources().getDrawable(R.drawable.ic_page_no), itemBuilder);

        sortBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeSortOrder();
            }
        });
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currPage > 1) {
                    currPage--;
                    fetchDataFromUrl(String.valueOf(currPage));
                } else {
                    Toast.makeText(MainActivity.this, "Already At Page 1", Toast.LENGTH_SHORT).show();
                }
            }
        });
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currPage++;
                fetchDataFromUrl(String.valueOf(currPage));
            }
        });
        chooseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choosePageNo();
            }
        });

        final FloatingActionMenu actionMenu = new FloatingActionMenu.Builder(MainActivity.this)
                .addSubActionView(sortBtn)
                .addSubActionView(backBtn)
                .addSubActionView(nextBtn)
                .addSubActionView(chooseBtn)
                .attachTo(actionButton)
                .build();

        final SharedPrefenceUtils sharedPrefenceUtils = new SharedPrefenceUtils(mainPrefs);

        if (sharedPrefenceUtils.isFirstTimeRun()) {
            //Display display = getWindowManager().getDefaultDisplay();
            //Drawable iconNext = this.getResources().getDrawable(R.drawable.ic_arrow_right);
            //Rect rect1 = new Rect(0, 0, iconNext.getIntrinsicWidth() * 2, iconNext.getIntrinsicHeight() * 2);
            //rect1.offset(display.getWidth() / 2, display.getHeight() / 2);
            final TapTarget target1 = TapTarget.forView(actionButton, INTRO_TITLE_ACTION_BUTTON, INTRO_DETAIL_ACTION_BUTTON);
            TapTarget target2 = TapTarget.forView(backBtn, INTRO_TITLE_BACK_BUTTON, INTRO_DETAIL_BACK_BUTTON).outerCircleColor(R.color.colorAccentIntro).tintTarget(false);
            TapTarget target3 = TapTarget.forView(nextBtn, INTRO_TITLE_NEXT_BUTTON, INTRO_DETAIL_NEXT_BUTTON).outerCircleColor(R.color.colorYello).textColor(R.color.colorBlackIntro).textColor(R.color.colorBlack).tintTarget(false);
            //TapTarget target4 = TapTarget.forBounds(rect1, INTRO_TITLE_REFRESH_BUTTON, INTRO_DETAIL_REFRESH_BUTTON).icon(getBaseContext().getResources().getDrawable(R.drawable.ic_loading)).outerCircleColor(R.color.colorGreenIntro).textColor(R.color.colorBlack).tintTarget(false);
            TapTarget target5 = TapTarget.forView(sortBtn, INTRO_TITLE_SORT_BUTTON, INTRO_DETAIL_SORT_BUTTON).outerCircleColor(R.color.colorSkyBlueIntro).textColor(R.color.colorBlack).tintTarget(false);
            TapTarget target6 = TapTarget.forView(chooseBtn, INTRO_TITLE_PAGE_NO_BUTTON, INTRO_DETAIL_PAGE_NO_BUTTON).outerCircleColor(R.color.colorRedIntro).descriptionTextColor(R.color.colorBlack).tintTarget(false);
            TapTarget target7 = TapTarget.forToolbarNavigationIcon(toolbarMain, "Click here for More tools");
            //TapTarget target8 = TapTarget.forBounds(rect1, "Click here for Seeing Favoirites Movies", "Favourites are the movies details that are saved offline for future and when device is offline this is where movies can be watched").icon(getBaseContext().getResources().getDrawable(R.drawable.ic_favourite_heart_fillled_red)).tintTarget(false);

            final TapTargetSequence sequenceOne = new TapTargetSequence(MainActivity.this)
                    .targets(target1, target2, target3, target5, target6, target7)
                    .listener(new TapTargetSequence.Listener() {
                        @Override
                        public void onSequenceFinish() {
                            sharedPrefenceUtils.setFirstTimeRun(false);
                            Toast.makeText(MainActivity.this, "You Just Completed Toturial...\nGive it a Try", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {
                            if (lastTarget == target1) {
                                actionButton.performClick();
                            }
                        }

                        @Override
                        public void onSequenceCanceled(TapTarget lastTarget) {
                            Toast.makeText(MainActivity.this, "Toturial is not completed...\nIt is Compulsory to complete it firsttime\nIt will show again at next Start", Toast.LENGTH_SHORT).show();
                        }
                    });
            sequenceOne.start();
        }
    }

    public SubActionButton addButton(Drawable drawable, SubActionButton.Builder builder) {
        ImageView btnImage = new ImageView(this);
        btnImage.setImageDrawable(drawable);
        return builder.setContentView(btnImage).build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sort_by_menu:
                changeSortOrder();
                return true;

            case R.id.favourites_menu:
                Intent intent = new Intent(MainActivity.this, FavoritesActivity.class);
                startActivity(intent);

            case R.id.refresh_menu:
                refreshPage();

            default:
                return false;
        }
    }

    private void refreshPage() {
        tvData.setText(R.string.REFRESHING);
        ivNoConnection.setVisibility(View.GONE);
        fetchDataFromUrl((String.valueOf(currPage)));
    }

    public void changeSortOrder() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setPositiveButton("Popularity", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sortByFinal = sortByPopularity;
                        currPage = 1;
                        fetchDataFromUrl("1");
                    }
                })
                .setNegativeButton("IMDB Rating", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sortByFinal = sortByImdbRating;
                        currPage = 1;
                        fetchDataFromUrl("1");
                    }
                })
                .setNeutralButton("Upcoming Movies", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sortByFinal = sortByUpcoming;
                        currPage = 1;
                        fetchDataFromUrl("1");
                    }
                })
                .setTitle("Sort Movies By ")
                .create();
        dialog.setCancelable(true);
        dialog.show();
    }

    public void buildDrawer(final Activity context, Toolbar toolbar, final ActionBar actionbar) {
        new DrawerBuilder().withActivity(context).build();
        final PrimaryDrawerItem favouriteDrawer = new PrimaryDrawerItem().withIdentifier(1).withName(R.string.drawer_item_favorites).withSelectable(false);
        final PrimaryDrawerItem aboutDrawer = new PrimaryDrawerItem().withIdentifier(2).withName("About this App").withSelectable(false);
        favouriteDrawer.withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
            @Override
            public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                Intent intent = new Intent(MainActivity.this, FavoritesActivity.class);
                startActivity(intent);
                return false;
            }
        });
        aboutDrawer.withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
            @Override
            public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                Intent i = new Intent(MainActivity.this,AboutAppActivity.class);
                startActivity(i);
                return false;
            }
        });
        AccountHeader aboutHeader = new AccountHeaderBuilder()
                .withActivity(context)
                .withHeaderBackground(R.drawable.background_detail_vector)
                .addProfiles(
                        new ProfileDrawerItem().withName("Movie-Mania").withEmail("provyas4399@gmail.com").withIcon(context.getResources().getDrawable(R.drawable.ic_launcher_foreground))
                )
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean currentProfile) {
                        return false;
                    }
                })
                .build();

        drawer = new DrawerBuilder()
                .withActivity(context)
                .withDisplayBelowStatusBar(true)
                .withTranslucentStatusBar(true)
                .withActionBarDrawerToggle(true)
                .withAccountHeader(aboutHeader)
                .withToolbar(toolbar)
                .withActionBarDrawerToggleAnimated(true)
                .addDrawerItems(
                        favouriteDrawer,
                        aboutDrawer,
                        new DividerDrawerItem()
                )
                .withSelectedItemByPosition(3
                )
                .build();
//        drawer.setOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
//            @Override
//            public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
//                switch ((int) drawerItem.getIdentifier()){
//                    case 1:
//                        Toast.makeText(context, "Clicked 1", Toast.LENGTH_SHORT).show();
//                        break;
//
//                    case 2:
//                        Toast.makeText(context, "Clicked 1=2", Toast.LENGTH_SHORT).show();
//                        break;
//
//                }
//                return true;
//            }
//        });
        drawer.setOnDrawerNavigationListener(new Drawer.OnDrawerNavigationListener() {
            @Override
            public boolean onNavigationClickListener(View clickedView) {
                if (drawer.isDrawerOpen()) {
                    drawer.getActionBarDrawerToggle().setDrawerIndicatorEnabled(false);
                    actionbar.setDisplayHomeAsUpEnabled(true);
                    //Toast.makeText(context, "Is Closing Drawer", Toast.LENGTH_SHORT).show();
                } else {
                    actionbar.setDisplayHomeAsUpEnabled(false);
                    drawer.getActionBarDrawerToggle().setDrawerIndicatorEnabled(true);
                    //Toast.makeText(context, "Is Opening Drawer", Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });
    }

    public void choosePageNo() {
        LayoutInflater li = LayoutInflater.from(MainActivity.this);
        View chooseView = li.inflate(R.layout.choose_page_dialog, null);

        android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setView(chooseView);
        final EditText userInput = chooseView
                .findViewById(R.id.edit_choose_page_dialog);
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                String result = userInput.getText().toString();
                                if (result.length() != 0) {
                                    currPage = Integer.valueOf(result);
                                    fetchDataFromUrl(result);
                                } else {
                                    Toast.makeText(MainActivity.this, "Page No is Invalid", Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        android.app.AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public void attachWithRecyclerView(List<MovieModel> movieResult) {
        adapter.setMovies(movieResult);
        ivBackgroundProgress.setVisibility(View.GONE);
        ivNoConnection.setVisibility(View.GONE);
    }


    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen()) {
            drawer.closeDrawer();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onComplete(MainModel model) {
        if (model != null) {
            LoadMasterGenreAsyncTask masterGenreAsyncTask = new LoadMasterGenreAsyncTask(this, this);
            masterGenreAsyncTask.execute();
            movie = model.getResults();
        } else {
            Snackbar sbar = Snackbar.make(tvData, "Network Unavailable", Snackbar.LENGTH_LONG);
            sbar.show();
            tvData.setText(R.string.NO_INTERNET_TITLE);
            tvProgress.setText(R.string.NO_INTERNET_DETAIL);
            loadingIndicatorView.smoothToHide();
            ivNoConnection.setVisibility(View.VISIBLE);
            ivBackgroundProgress.setVisibility(View.VISIBLE);
            tvProgress.setVisibility(View.VISIBLE);
            rvMain.setVisibility(View.INVISIBLE);
        }
    }

    private String convertIdsToString(List<String> genre_id) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < genre_id.size(); i++) {
            result.append(genre_id.get(i)).append(",");
        }
        return result.toString();
    }

    @Override
    public void onCompleteMasterGenre(List<GenreModel> genreModels) {
        List<String> ids = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (GenreModel genre : genreModels) {
            ids.add(genre.getId());
            names.add(genre.getName());
        }
        for (int i = 0; i < movie.size(); i++) {
            List<String> SelectedGenre = new ArrayList<>();
            List<String> genre_id = movie.get(i).getGenre_ids();
            for (String id : genre_id) {
                int idPosition = ids.indexOf(id);
                String genreName = names.get(idPosition);
                SelectedGenre.add(genreName);
            }
            String movieGenre = convertIdsToString(SelectedGenre);
            movie.get(i).setTag_Genre(movieGenre);
        }
//        tvData.setText(String.format(Locale.getDefault(),"+Total Result : %d    +Total Pages : %d\n+Current Page : %d    +Current Sort : %s", model.getTotal_results(), model.getTotal_pages(), currPage, (Objects.equals(sortByFinal, sortByPopularity)) ? "Popularity" : (Objects.equals(sortByFinal, sortByImdbRating)) ? "IMDB Rating" : "Upcoming")
//        );
        tvData.setText(String.format(Locale.getDefault(), "+Current Page : %d    +Current Sort : %s", currPage, (Objects.equals(sortByFinal, sortByPopularity)) ? "Popularity" : (Objects.equals(sortByFinal, sortByImdbRating)) ? "IMDB Rating" : "Upcoming")
        );
        rvMain.setVisibility(View.VISIBLE);
        tvProgress.setVisibility(View.GONE);
        attachWithRecyclerView(movie);
    }
}
