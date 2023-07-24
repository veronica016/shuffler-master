package com.example.anujsharma.shuffler.fragments;


import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.anujsharma.shuffler.R;
import com.example.anujsharma.shuffler.activities.MainActivity;
import com.example.anujsharma.shuffler.adapters.MyDatabaseAdapter;
import com.example.anujsharma.shuffler.adapters.SearchHistoryRecyclerViewAdapter;
import com.example.anujsharma.shuffler.adapters.SearchSongRecyclerViewAdapter;
import com.example.anujsharma.shuffler.dao.PlaylistsDao;
import com.example.anujsharma.shuffler.dao.TracksDao;
import com.example.anujsharma.shuffler.dao.UsersDao;
import com.example.anujsharma.shuffler.models.HybridModel;
import com.example.anujsharma.shuffler.models.Playlist;
import com.example.anujsharma.shuffler.models.Song;
import com.example.anujsharma.shuffler.models.User;
import com.example.anujsharma.shuffler.utilities.Constants;
import com.example.anujsharma.shuffler.utilities.CustomLinearLayoutManager;
import com.example.anujsharma.shuffler.utilities.DialogBoxes;
import com.example.anujsharma.shuffler.utilities.InternetConnection;
import com.example.anujsharma.shuffler.utilities.SharedPreference;
import com.example.anujsharma.shuffler.utilities.Utilities;
import com.example.anujsharma.shuffler.volley.RequestCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class SearchFragment extends Fragment implements RequestCallback {

    public EditText etSearch;
    private TracksDao tracksDao;
    private UsersDao usersDao;
    private PlaylistsDao playlistsDao;
    private ImageView crossOut;
    private RecyclerView rvSearchResult;
    private SearchSongRecyclerViewAdapter searchSongRecyclerAdapter;
    private LinearLayoutManager layoutManager;

    private CustomLinearLayoutManager customLinearLayoutManager;

    private ArrayList<Song> songs;
    private ArrayList<User> users;
    private ArrayList<Playlist> playlists;
    private static int messageState;
    private Context context;
    private LinearLayout relativeLayout;
    private int currentSongIndex;
    private SharedPreference pref;
    private MyDatabaseAdapter myDatabaseAdapter;
    private SearchHistoryRecyclerViewAdapter searchHistoryRecyclerAdapter;
    private Playlist currentHistoryPlaylist;
    private ProgressBar progressBar;
    private RelativeLayout rlNoSearchHistory, rlNoSearchResultFound, rlNoInternet;

    public SearchFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getActivity();
        pref = new SharedPreference(context);
        songs = new ArrayList<>();
        users = new ArrayList<>();
        playlists = new ArrayList<>();
        tracksDao = new TracksDao(context, this);
        usersDao = new UsersDao(context, this);
        playlistsDao = new PlaylistsDao(context, this);
        myDatabaseAdapter = new MyDatabaseAdapter(context);

        searchSongRecyclerAdapter = new SearchSongRecyclerViewAdapter(context, new SearchSongRecyclerViewAdapter.ItemClickListener() {
            @Override
            public void onItemClick(View view, final int position, int check) {
                final Song song = songs.get(position);
                SeeAllFragment seeAllFragment;
                UserPageFragment userPageFragment;
                Bundle bundle;
                switch (check) {
                    case Constants.EACH_SONG_LAYOUT_CLICKED:
                        Playlist playlist = new Playlist(songs, etSearch.getText().toString());
                        ((MainActivity) getActivity()).playSongInMainActivity(position, playlist);
                        long added = myDatabaseAdapter.addToHistory(new HybridModel(Constants.TYPE_TRACK, song.getId(),
                                song.getSongArtwork(), song.getTitle(), song.getUser().getUsername()));
                        changeSelectedPosition(position + 1);
                        break;
                    case Constants.EACH_SONG_MENU_CLICKED:
                    case Constants.EACH_SONG_VIEW_LONG_CLICKED:
                        PopupMenu popupMenu = new PopupMenu(new ContextThemeWrapper(context, R.style.PopupMenu), view);
                        popupMenu.getMenuInflater().inflate(R.menu.each_song_pop_up_menu, popupMenu.getMenu());
                        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                switch (item.getItemId()) {
                                    case R.id.popGotoArtistProfile:
                                        myDatabaseAdapter.addToHistory(new HybridModel(Constants.TYPE_USER, song.getUser().getId(),
                                                song.getUser().getUserAvatar(), song.getUser().getUsername(), null));
                                        UserPageFragment userPageFragment = new UserPageFragment();
                                        Bundle bundle = new Bundle();
                                        bundle.putInt(Constants.TYPE, Constants.TYPE_USER);
                                        bundle.putLong(Constants.USER_ID_KEY, songs.get(position).getUser().getId());
                                        userPageFragment.setArguments(bundle);
                                        getFragmentManager().beginTransaction().replace(R.id.mainFrameContainer, userPageFragment, Constants.FRAGMENT_USER_PAGE)
                                                .addToBackStack(userPageFragment.getClass().getName()).commit();
                                        return true;
                                    case R.id.popLike:

                                        return true;
                                    case R.id.popShare:

                                        return true;
                                    case R.id.popAddToPlaylist:
                                        showAddToPlaylistDialog(song);
                                        return true;
                                }
                                return false;
                            }
                        });
                        popupMenu.show();
                        break;
                    case Constants.EACH_USER_LAYOUT_CLICKED:

                        myDatabaseAdapter.addToHistory(new HybridModel(Constants.TYPE_USER, users.get(position).getId(),
                                users.get(position).getUserAvatar(), users.get(position).getUsername(), null));
                        userPageFragment = new UserPageFragment();
                        bundle = new Bundle();
                        bundle.putInt(Constants.TYPE, Constants.TYPE_USER);
                        bundle.putParcelable(Constants.USER_MODEL_KEY, users.get(position));
                        userPageFragment.setArguments(bundle);
                        getFragmentManager().beginTransaction().replace(R.id.mainFrameContainer, userPageFragment, Constants.FRAGMENT_USER_PAGE)
                                .addToBackStack(userPageFragment.getClass().getName()).commit();
                        break;
                    case Constants.EACH_USER_MENU_CLICKED:

                        break;
                    case Constants.SEE_ALL_SONGS_CLICKED:
                        seeAllFragment = new SeeAllFragment();
                        bundle = new Bundle();
                        bundle.putInt("type", Constants.TYPE_TRACK);
                        bundle.putString("search", etSearch.getText().toString());
                        bundle.putParcelableArrayList(Constants.SONGS_MODEL_KEY, songs);
                        seeAllFragment.setArguments(bundle);
                        getFragmentManager().beginTransaction().replace(R.id.mainFrameContainer, seeAllFragment, Constants.FRAGMENT_SEE_ALL)
                                .addToBackStack(seeAllFragment.getClass().getName()).commit();
                        break;
                    case Constants.SEE_ALL_USERS_CLICKED:
                        seeAllFragment = new SeeAllFragment();
                        bundle = new Bundle();
                        bundle.putInt("type", Constants.TYPE_USER);
                        bundle.putString("search", etSearch.getText().toString());
                        bundle.putParcelableArrayList(Constants.USERS_MODEL_KEY, users);
                        seeAllFragment.setArguments(bundle);
                        getFragmentManager().beginTransaction().replace(R.id.mainFrameContainer, seeAllFragment, Constants.FRAGMENT_SEE_ALL)
                                .addToBackStack(seeAllFragment.getClass().getName()).commit();
                        break;

                    case Constants.EACH_PLAYLIST_LAYOUT_CLICKED:

                        myDatabaseAdapter.addToHistory(new HybridModel(Constants.TYPE_PLAYLIST, playlists.get(position).getPlaylistId(),
                                playlists.get(position).getArtworkUrl(), playlists.get(position).getTitle(), null));
                        userPageFragment = new UserPageFragment();
                        bundle = new Bundle();
                        bundle.putInt(Constants.TYPE, Constants.TYPE_PLAYLIST);
                        bundle.putParcelable(Constants.PLAYLIST_MODEL_KEY, playlists.get(position));
                        userPageFragment.setArguments(bundle);
                        getFragmentManager().beginTransaction().replace(R.id.mainFrameContainer, userPageFragment, Constants.FRAGMENT_USER_PAGE)
                                .addToBackStack(userPageFragment.getClass().getName()).commit();
                        break;

                    case Constants.EACH_PLAYLIST_MENU_CLICKED:

                        break;
                    case Constants.SEE_ALL_PLAYLISTS_CLICKED:
                        seeAllFragment = new SeeAllFragment();
                        bundle = new Bundle();
                        bundle.putInt("type", Constants.TYPE_PLAYLIST);
                        bundle.putString("search", etSearch.getText().toString());
                        bundle.putParcelableArrayList(Constants.PLAYLIST_MODEL_KEY, playlists);
                        seeAllFragment.setArguments(bundle);
                        getFragmentManager().beginTransaction().replace(R.id.mainFrameContainer, seeAllFragment, Constants.FRAGMENT_SEE_ALL)
                                .addToBackStack(seeAllFragment.getClass().getName()).commit();
                        break;
                }
            }
        });

        searchHistoryRecyclerAdapter = new SearchHistoryRecyclerViewAdapter(context, myDatabaseAdapter.getHistroyList(), new SearchHistoryRecyclerViewAdapter.ItemClickListener() {
            @Override
            public void onItemClick(View view, int position, int check) {
                List<HybridModel> historyList = myDatabaseAdapter.getHistroyList();
                HybridModel history;
                switch (check) {
                    case Constants.HISTORY_CROSS_CLICKED:
                        if (position >= 0) {
                            history = historyList.get(position);
                            myDatabaseAdapter.deleteFromHistory(history.getId());
                            searchHistoryRecyclerAdapter.deleteFromHistory(position + 1, myDatabaseAdapter.getHistroyList());
                            if (myDatabaseAdapter.getHistroyList().size() == 0) {
                                rlNoSearchHistory.setVisibility(View.VISIBLE);
                            }
                        }
                        break;
                    case Constants.HISTORY_LAYOUT_CLICKED:
                        history = historyList.get(position);
                        switch (history.getType()) {
                            case Constants.TYPE_TRACK:
                                tracksDao.getTrackWithId(String.valueOf(history.getId()));
                                break;
                            case Constants.TYPE_USER:
                                usersDao.getUserWithId(String.valueOf(history.getId()));
                                break;
                            case Constants.TYPE_PLAYLIST:
                                playlistsDao.getPlaylistFromPlaylistId(history.getId());
                                break;
                        }
                        break;
                    case Constants.CLEAR_HISTORY_CLICKED:
                        myDatabaseAdapter.deleteFromHistory(null);
                        searchHistoryRecyclerAdapter.updateHistory(new ArrayList<HybridModel>());
                        rlNoSearchHistory.setVisibility(View.VISIBLE);
                        break;
                }
            }
        });
    }


    public void manageMessageLayouts() {
        switch (messageState) {
            case Constants.SHOW_NO_HISTORY:
                showNoSearchHistoryMessage();
                break;
            case Constants.SHOW_NO_INTERNET_CONNECTION:
                showNoInternetMessage();
                break;
            case Constants.SHOW_NO_SEARCH_RESULT:
                showNoSearchReultFoundMessage();
                break;
            case Constants.SHOW_PROGRESS_BAR:
                showProgressBar();
                break;
            case Constants.SHOW_RECYCLER_VIEW:
                showRecyclerView();
                break;
        }
    }

    private void showNoInternetMessage() {
        rlNoInternet.setVisibility(View.VISIBLE);
        rlNoSearchResultFound.setVisibility(View.GONE);
        rlNoSearchHistory.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        rvSearchResult.setVisibility(View.GONE);
        messageState = Constants.SHOW_NO_INTERNET_CONNECTION;
    }

    private void showNoSearchReultFoundMessage() {
        messageState = Constants.SHOW_NO_SEARCH_RESULT;
        rlNoInternet.setVisibility(View.GONE);
        rlNoSearchResultFound.setVisibility(View.VISIBLE);
        rlNoSearchHistory.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        rvSearchResult.setVisibility(View.GONE);
    }

    private void showNoSearchHistoryMessage() {
        messageState = Constants.SHOW_NO_HISTORY;
        rlNoInternet.setVisibility(View.GONE);
        rlNoSearchResultFound.setVisibility(View.GONE);
        rlNoSearchHistory.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        rvSearchResult.setVisibility(View.GONE);
    }

    private void showProgressBar() {
        messageState = Constants.SHOW_PROGRESS_BAR;
        rlNoInternet.setVisibility(View.GONE);
        rlNoSearchResultFound.setVisibility(View.GONE);
        rlNoSearchHistory.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        rvSearchResult.setVisibility(View.GONE);
    }

    private void showRecyclerView() {
        messageState = Constants.SHOW_RECYCLER_VIEW;
        rlNoInternet.setVisibility(View.GONE);
        rlNoSearchResultFound.setVisibility(View.GONE);
        rlNoSearchHistory.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        rvSearchResult.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        manageMessageLayouts();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        ((MainActivity) context).modifyBottomLayout(1);
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        initialise(view);
        initialiseListeners();
        return view;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initialiseListeners() {
        crossOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (etSearch.length() > 0) etSearch.getText().clear();
                showHistory();
                InputMethodManager imgr = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imgr != null) {
                    imgr.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
                }
                etSearch.requestFocus();
            }
        });

        etSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    InputMethodManager imgr = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imgr != null) {
                        imgr.hideSoftInputFromWindow(getView().getRootView().getWindowToken(), 0);
                    }
                    etSearch.clearFocus();
                    performSearch();
                    return true;
                }
                return false;
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            rvSearchResult.setOnScrollChangeListener(new View.OnScrollChangeListener() {
                @Override
                public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                    InputMethodManager imgr = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imgr != null) {
                        imgr.hideSoftInputFromWindow(getView().getRootView().getWindowToken(), 0);
                    }
                    etSearch.clearFocus();
                }
            });
        }

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                /*if (rvSearchResult.getAdapter() instanceof SearchHistoryRecyclerViewAdapter) {
                    rvSearchResult.setAdapter(searchSongRecyclerAdapter);
                }*/
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                performSearch();
                rvSearchResult.setVisibility(View.GONE);
                if (charSequence.toString().trim().isEmpty()) showHistory();
                else {
                    if (rvSearchResult.getAdapter() instanceof SearchHistoryRecyclerViewAdapter) {
                        rvSearchResult.setAdapter(searchSongRecyclerAdapter);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }

    private void performSearch() {
        currentSongIndex = 0;
        clearSelectedPosition();
        layoutManager.scrollToPosition(0);
        customLinearLayoutManager.scrollToPosition(0);
        String query = etSearch.getText().toString();
        if (query.trim().isEmpty()) {
            showHistory();
        } else {
            showProgressBar();
            if (InternetConnection.isOnline(context)) tracksDao.getTracksWithQuery(query, 100);
            else {
                showNoInternetMessage();
            }
        }
    }

    private void showHistory() {
        if (myDatabaseAdapter.getHistroyList().size() == 0) {
            showNoSearchHistoryMessage();
        } else {
            showRecyclerView();
        }
        searchHistoryRecyclerAdapter.updateHistory(myDatabaseAdapter.getHistroyList());
        rvSearchResult.setAdapter(searchHistoryRecyclerAdapter);
        rvSearchResult.clearFocus();
//        etSearch.requestFocus();
    }

    private void initialise(View view) {
        etSearch = view.findViewById(R.id.xetSearch);
        crossOut = view.findViewById(R.id.ivSearchCross);
        rvSearchResult = view.findViewById(R.id.rvSearchResult);
        relativeLayout = view.findViewById(R.id.search_background);
        rlNoSearchResultFound = view.findViewById(R.id.rlNoSearchResultFound);
        layoutManager = new LinearLayoutManager(context);
        customLinearLayoutManager = new CustomLinearLayoutManager(context, new CustomLinearLayoutManager.MyErrorListener() {
            @Override
            public void onIoobeFound() {
                rlNoSearchResultFound.setVisibility(View.VISIBLE);
            }
        });
        rvSearchResult.setLayoutManager(customLinearLayoutManager);
        rvSearchResult.setAdapter(searchHistoryRecyclerAdapter);
        progressBar = view.findViewById(R.id.search_progressbar);
        rlNoSearchHistory = view.findViewById(R.id.rlNoSearchHistory);
        rlNoInternet = view.findViewById(R.id.rlNoInternetConnection);
        showHistory();
    }

    public void setBackground(String url) {
        /*Glide.with(context)
                .load(url)
                .into(new SimpleTarget<Drawable>() {
            @Override
            public void onResourceReady(Drawable resource, Transition<? super Drawable> transition) {
                relativeLayout.setBackground(resource);
            }
        });*/
    }

    @Override
    public void onListRequestSuccessful(ArrayList list, int check, boolean status) {
//        if (list != null && list.size() == 0) return;
        switch (check) {
            case Constants.SEARCH_SONGS_WITH_QUERY:
                if (status) {
                    songs.clear();

                    if (rvSearchResult.getAdapter() instanceof SearchHistoryRecyclerViewAdapter) {
                        rvSearchResult.setAdapter(searchSongRecyclerAdapter);
                    }

                    if (list != null) songs.addAll(list);
//                    setBackground(songs.get(0).getSongArtwork());
                    Collections.sort(songs, new Comparator<Song>() {
                        @Override
                        public int compare(Song o1, Song o2) {
                            if (o2.getLikesCount() > o1.getLikesCount()) return 1;
                            else if (o2.getLikesCount() < o1.getLikesCount()) return -1;
                            else return 0;
                        }
                    });
                    if (songs.size() > 4) {
                        changeSelectedPosition(Utilities.getSelectedPosition(context, songs.subList(0, 4), 1));
                        searchSongRecyclerAdapter.changeSongData(songs.subList(0, 4));
                    } else {
                        changeSelectedPosition(Utilities.getSelectedPosition(context, songs, 1));
                        searchSongRecyclerAdapter.changeSongData(songs);
                    }
                    usersDao.getUsersWithQuery(etSearch.getText().toString(), 20);
                }
                break;
            case Constants.SEARCH_USERS_WITH_QUERY:
                if (status) {
                    users.clear();
                    if (list != null) users.addAll(list);
                    Collections.sort(users, new Comparator<User>() {
                        @Override
                        public int compare(User o1, User o2) {
                            if (o2.getFollowersCount() > o1.getFollowersCount()) return 1;
                            else if (o2.getFollowersCount() < o1.getFollowersCount()) return -1;
                            else return 0;
                        }
                    });
                    if (users.size() > 4) {
                        searchSongRecyclerAdapter.changeUserData(users.subList(0, 4));
                    } else {
                        searchSongRecyclerAdapter.changeUserData(users);
                    }
                    playlistsDao.getPlaylistsWithQuery(etSearch.getText().toString(), 20);
                }
                break;
            case Constants.SEARCH_PLAYLISTS_WITH_QUERY:
                if (status) {
                    playlists.clear();
                    if (list != null) playlists.addAll(list);
                    Collections.sort(playlists, new Comparator<Playlist>() {
                        @Override
                        public int compare(Playlist o1, Playlist o2) {
                            if (o2.getLikesCount() > o1.getLikesCount()) return 1;
                            else if (o2.getLikesCount() < o1.getLikesCount()) return -1;
                            else return 0;
                        }
                    });
                    if (playlists.size() > 4) {
                        searchSongRecyclerAdapter.changePlaylistData(playlists.subList(0, 4));
                    } else {
                        searchSongRecyclerAdapter.changePlaylistData(playlists);
                    }
                }
                showRecyclerView();
                if (searchSongRecyclerAdapter.getItemCount() == 0) {
                    showNoSearchReultFoundMessage();
                }
                break;

            case Constants.SEARCH_SONG_WITH_PLAYLIST_ID:
                if (status) {
                    currentHistoryPlaylist.setSongs(list);
                    currentHistoryPlaylist.setTrack_count(list.size());
                    UserPageFragment userPageFragment = new UserPageFragment();
                    Bundle bundle = new Bundle();
                    bundle.putInt(Constants.TYPE, Constants.TYPE_PLAYLIST);
                    bundle.putParcelable(Constants.PLAYLIST_MODEL_KEY, currentHistoryPlaylist);
                    userPageFragment.setArguments(bundle);
                    getFragmentManager().beginTransaction().replace(R.id.mainFrameContainer, userPageFragment, Constants.FRAGMENT_USER_PAGE)
                            .addToBackStack(userPageFragment.getClass().getName()).commit();
                }
                break;

        }
    }

    @Override
    public void onObjectRequestSuccessful(Object object, int check, boolean status) {
        switch (check) {
            case Constants.SEARCH_SONG_WITH_ID:
                Song song = (Song) object;
                if ((getActivity()) != null && song != null) {
                    ArrayList<Song> songs = new ArrayList<>();
                    songs.add(song);
                    Playlist playlist = new Playlist(songs, "History");
                    ((MainActivity) getActivity()).playSongInMainActivity(0, playlist);
                }
                break;
            case Constants.SEARCH_USER_WITH_ID:
                User user = (User) object;
                if (user != null) {
                    UserPageFragment userPageFragment = new UserPageFragment();
                    Bundle bundle = new Bundle();
                    bundle.putInt(Constants.TYPE, Constants.TYPE_USER);
                    bundle.putParcelable(Constants.USER_MODEL_KEY, user);
                    userPageFragment.setArguments(bundle);

                    getFragmentManager().beginTransaction()
                            .replace(R.id.mainFrameContainer, userPageFragment, Constants.FRAGMENT_USER_PAGE)
                            .addToBackStack(userPageFragment.getClass().getName()).commit();
                }
                break;
            case Constants.SEARCH_PLAYLISTS_WITH_ID:
                currentHistoryPlaylist = (Playlist) object;
                UserPageFragment userPageFragment = new UserPageFragment();
                Bundle bundle = new Bundle();
                bundle.putInt(Constants.TYPE, Constants.TYPE_PLAYLIST);
                bundle.putParcelable(Constants.PLAYLIST_MODEL_KEY, currentHistoryPlaylist);
                userPageFragment.setArguments(bundle);
                getFragmentManager().beginTransaction().replace(R.id.mainFrameContainer, userPageFragment, Constants.FRAGMENT_USER_PAGE)
                        .addToBackStack(userPageFragment.getClass().getName()).commit();
                break;
        }
    }

    public void clearSelectedPosition() {
        searchSongRecyclerAdapter.notifyItemChanged(searchSongRecyclerAdapter.getSelectedPosition());
        searchSongRecyclerAdapter.setSelectedPosition(-1);
    }

    public void changeSelectedPosition(int index) {
        // currentIndex and selected position both are in recyclerview position
        searchSongRecyclerAdapter.notifyItemChanged(searchSongRecyclerAdapter.getSelectedPosition());
        currentSongIndex = index;
        searchSongRecyclerAdapter.setSelectedPosition(currentSongIndex);
        searchSongRecyclerAdapter.notifyItemChanged(currentSongIndex);
    }

    @Override
    public void onStart() {
        super.onStart();
        int newPos = Utilities.getSelectedPosition(context, songs, 1);
        changeSelectedPosition(newPos);
    }

    private void showAddToPlaylistDialog(Song song) {
        View mView = getLayoutInflater().inflate(R.layout.dialog_add_song_to_playlist, null);
        View createPlaylistView = getLayoutInflater().inflate(R.layout.create_playlist_dialog_layout, null);
        DialogBoxes.showAddSongToPlaylistDialog(context, mView, createPlaylistView, song);
    }
}
