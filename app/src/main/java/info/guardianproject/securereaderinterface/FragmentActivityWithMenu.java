package info.guardianproject.securereaderinterface;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.LayoutInflaterCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageView;
import android.widget.TextView;

import com.tinymission.rss.Feed;
import com.tinymission.rss.Item;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.ArrayList;

import info.guardianproject.securereader.Settings.ProxyType;
import info.guardianproject.securereader.Settings.SyncMode;
import info.guardianproject.securereader.SocialReader;
import info.guardianproject.securereaderinterface.adapters.DrawerMenuRecyclerViewAdapter;
import info.guardianproject.securereaderinterface.models.FeedFilterType;
import info.guardianproject.securereaderinterface.ui.LayoutFactoryWrapper;
import info.guardianproject.securereaderinterface.ui.UICallbacks;
import info.guardianproject.securereaderinterface.ui.UICallbacks.OnCallbackListener;
import info.guardianproject.securereaderinterface.uiutil.ActivitySwitcher;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;
import info.guardianproject.securereaderinterface.widgets.CheckableButton;

public class FragmentActivityWithMenu extends LockableActivity implements DrawerMenuRecyclerViewAdapter.DrawerMenuCallbacks, OnCallbackListener {
    public static final String LOGTAG = "FragmentActivityWithMenu";
    public static final boolean LOGGING = false;

    private KillReceiver mKillReceiver;
    private SetUiLanguageReceiver mSetUiLanguageReceiver;
    private WipeReceiver mWipeReceiver;
    private int mIdMenu;
    private Menu mOptionsMenu;
    private boolean mDisplayHomeAsUp = false;

    /**
     * These are for action bar pull down support (e.g. used in full screen item view)
     */
    private boolean mUsePullDownActionBar = false;
    private int mToolbarHideOffset = 0;
    private int mPullDownStartToolbarOffset = 0;
    private float mPullDownStartY = 0;
    private int mPullDownTouchSlop = 0;
    private float mPullDownScrollAdjustment;
    private RectF mPullDownSlopRect;
    private RectF mPullDownCloseRect;
    private RectF mPullDownOpenRect;

    /**
     * The main menu that will host all content links.
     */
    protected View mLeftSideMenu;
    protected DrawerLayout mDrawerLayout;
    protected ActionBarDrawerToggle mDrawerToggle;

    private ArrayList<Runnable> mDeferredCommands = new ArrayList<Runnable>();
    protected Toolbar mToolbar;
    protected View mToolbarShadow;
    private LayoutInflater mInflater;
    private LayoutFactoryWrapper mLayoutFactoryWrapper;


    protected void setMenuIdentifier(int idMenu) {
        mIdMenu = idMenu;
    }

    protected boolean useLeftSideMenu() {
        return true;
    }

    @Override
    public void setContentView(int layoutResID) {
        View view = LayoutInflater.from(this).inflate(R.layout.activity_base, null);
        ViewStub stub = (ViewStub) view.findViewById(R.id.content_root);
        if (stub != null) {
            stub.setLayoutResource(layoutResID);
            stub.inflate();
            super.setContentView(view);
        } else {
            super.setContentView(layoutResID);
        }
    }

    protected void setContentViewNoBase(int layoutResID) {
        super.setContentView(layoutResID);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        App.getInstance().setCurrentLanguageInConfig(this);
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = LayoutInflater.from(this);
        mInflater = inflater.cloneInContext(this);
        mLayoutFactoryWrapper = new LayoutFactoryWrapper(inflater.getFactory());
        LayoutInflaterCompat.setFactory(mInflater, mLayoutFactoryWrapper);

        UICallbacks.getInstance().addListener(this);

        mKillReceiver = new KillReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(mKillReceiver, new IntentFilter(App.EXIT_BROADCAST_ACTION));
        mSetUiLanguageReceiver = new SetUiLanguageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(mSetUiLanguageReceiver, new IntentFilter(App.SET_UI_LANGUAGE_BROADCAST_ACTION));
        mWipeReceiver = new WipeReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(mWipeReceiver, new IntentFilter(App.WIPE_BROADCAST_ACTION));
    }

    protected LayoutFactoryWrapper getLayoutFactoryWrapper() {
        return mLayoutFactoryWrapper;
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolbar != null) {
            mToolbarShadow = findViewById(R.id.toolbar_shadow);
            setSupportActionBar(mToolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            setActionBarTitle(getSupportActionBar().getTitle());
        }
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        if (mDrawerLayout != null) {
            if (!useLeftSideMenu()) {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            } else {
                mMenuViewHolder = null;
                mLeftSideMenu = mDrawerLayout.findViewById(R.id.left_drawer);
                mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, 0, 0) {
                    private boolean isClosed = true;

                    @Override
                    public void onDrawerClosed(View drawerView) {
                        super.onDrawerClosed(drawerView);
                        runDeferredCommands();
                        isClosed = true;
                    }

                    @Override
                    public void onDrawerSlide(View drawerView, float slideOffset) {
                        super.onDrawerSlide(drawerView, slideOffset);
                        if (isClosed && slideOffset > 0) {
                            isClosed = false;
                            updateLeftSideMenu();
                            if (mMenuViewHolder != null) {
                                mMenuViewHolder.recyclerView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mMenuViewHolder.recyclerView.scrollToPosition(0);
                                    }
                                });
                            }
                        }
                    }
                };
                mDrawerLayout.setDrawerListener(mDrawerToggle);
                mDrawerToggle.syncState();
            }
        }

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mDrawerToggle != null)
            mDrawerToggle.syncState();
    }

    public void setDisplayHomeAsUp(boolean displayHomeAsUp) {
        mDisplayHomeAsUp = displayHomeAsUp;
        if (displayHomeAsUp) {
            if (mDrawerToggle != null) {
                mDrawerToggle.setDrawerIndicatorEnabled(false);
            } else {
                if (getSupportActionBar() != null)
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        } else {
            if (mDrawerToggle != null) {
                mDrawerToggle.setDrawerIndicatorEnabled(true);
            } else {
                if (getSupportActionBar() != null)
                    getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            }
        }
    }

    public void setActionBarTitle(CharSequence title) {
        if (mToolbar != null) {
            TextView tvTitle = (TextView) mToolbar.findViewById(R.id.toolbar_title);
            if (tvTitle != null) {
                tvTitle.setText(title);
                tvTitle.setSelected(true);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mDrawerLayout != null)
            performRotateTransition((ViewGroup) mDrawerLayout.getParent());
    }

    @SuppressLint("NewApi")
    @Override
    protected void onResume() {
        super.onResume();
        if (!isFinishing()) {
            invalidateOptionsMenu();
            updateLeftSideMenu();
        }
    }

    private final class KillReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    }

    private final class SetUiLanguageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            new Handler().post(new Runnable() {

                @Override
                public void run() {
                    recreateNowOrOnResume();
                }
            });
        }
    }

    private final class WipeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onWipe();
                }
            });
        }
    }

    /**
     * Override this to react to a wipe!
     */
    protected void onWipe() {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        UICallbacks.getInstance().removeListener(this);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mKillReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mSetUiLanguageReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mWipeReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mIdMenu == 0)
            return false;
        mOptionsMenu = menu;
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(mIdMenu, menu);

        getMenuInflater().inflate(R.menu.overflow_main, menu);

        colorizeMenuItems();
        return true;
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (menu != null && App.getSettings().passphraseTimeout() == 0) {
            // Remove lock app option
            MenuItem item = menu.findItem(R.id.menu_lock_app);
            if (item != null)
                item.setVisible(false);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    private void colorizeMenuItems() {
        if (mOptionsMenu == null || getSupportActionBar() == null)
            return;
        for (int i = 0; i < mOptionsMenu.size(); i++) {
            MenuItem item = mOptionsMenu.getItem(i);
            Drawable d = item.getIcon();
            if (d != null) {
                d.mutate();
                UIHelpers.colorizeDrawable(getSupportActionBar().getThemedContext(), R.attr.colorControlNormal, d);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        App.getInstance().setCurrentLanguageInConfig(this);
        if (mDrawerToggle != null)
            mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDisplayHomeAsUp && item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            this.startActivity(intent);
            this.overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right);
            return true;
        }

        if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item))
            return true;

        // Allow app to respond
        if (App.getInstance().onOptionsItemSelected(this, item.getItemId())) {
            return true;
        }

        switch (item.getItemId()) {
            case R.id.menu_panic: {
                Intent intent = new Intent(this, PanicActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            }

            case R.id.menu_media_downloads: {
                UICallbacks.handleCommand(this, R.integer.command_downloads, null);
                return true;
            }

            case R.id.menu_manage_feeds: {
                UICallbacks.handleCommand(this, R.integer.command_feed_add, null);
                return true;
            }

            case R.id.menu_preferences: {
                UICallbacks.handleCommand(this, R.integer.command_settings, null);
                return true;
            }

            case R.id.menu_about: {
                UICallbacks.handleCommand(this, R.integer.command_help, null);
                return true;
            }

            case R.id.menu_share_app: {
                UICallbacks.handleCommand(this, R.integer.command_shareapp, null);
                return true;
            }

            case R.id.menu_lock_app: {
                App.getInstance().socialReader.lockApp();
                return true;
            }

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @SuppressLint("NewApi")
    protected void performRotateTransition(final ViewGroup container) {
        // Get bitmap from intent!!!
        Bitmap bmp = App.getInstance().getTransitionBitmap();
        App.getInstance().putTransitionBitmap(null);
        if (bmp != null) {
            final ImageView snap = new ImageView(this);
            snap.setImageBitmap(bmp);
            container.addView(snap);
            snap.bringToFront();

            if (Build.VERSION.SDK_INT >= 11) {
                container.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                snap.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            } else {
                container.setDrawingCacheEnabled(true);
                snap.setDrawingCacheEnabled(true);
            }

            // Animate!!!
            ActivitySwitcher.animationOut(container, getWindowManager(), new ActivitySwitcher.AnimationFinishedListener() {
                @Override
                public void onAnimationFinished() {
                    container.removeView(snap);

                    ActivitySwitcher.animationIn(container, getWindowManager(), new ActivitySwitcher.AnimationFinishedListener() {
                        @Override
                        public void onAnimationFinished() {
                            container.post(new Runnable() {
                                @Override
                                @SuppressLint("NewApi")
                                public void run() {
                                    if (Build.VERSION.SDK_INT >= 11) {
                                        container.setLayerType(View.LAYER_TYPE_NONE, null);
                                    } else {
                                        container.setDrawingCacheEnabled(false);
                                    }

                                    container.clearAnimation();
                                    onAfterResumeAnimation();
                                }
                            });
                        }
                    });
                }
            });
        } else {
            onAfterResumeAnimation();
        }
    }

    protected void onAfterResumeAnimation() {
        // Override this to start doing stuff after the animation is complete
    }

    private class MenuViewHolder {
        public CheckableButton btnTorStatus;
        public CheckableButton btnShowPhotos;
        public RecyclerView recyclerView;
    }

    private MenuViewHolder mMenuViewHolder;

    protected void updateLeftSideMenu() {
        if (mLeftSideMenu != null) {
            createMenuViewHolder();
            new UpdateLeftSideMenuTask().execute();
        }
    }

    protected void refreshLeftSideMenu() {
        if (mLeftSideMenu != null && mMenuViewHolder != null) {
            if (mMenuViewHolder.recyclerView.getAdapter() != null) {
                mMenuViewHolder.recyclerView.getAdapter().notifyDataSetChanged();
            }
        }
    }

    private void createMenuViewHolder() {
        if (mMenuViewHolder == null) {
            mMenuViewHolder = new MenuViewHolder();
            View menuView = mLeftSideMenu;
            mMenuViewHolder.btnTorStatus = (CheckableButton) menuView.findViewById(R.id.btnMenuTor);
            mMenuViewHolder.btnShowPhotos = (CheckableButton) menuView.findViewById(R.id.btnMenuPhotos);
            mMenuViewHolder.recyclerView = (RecyclerView) menuView.findViewById(R.id.drawerMenuRecyclerView);
            mMenuViewHolder.recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

            Class adapterClass = App.getInstance().getDrawerMenuAdapterClass();
            try {
                Constructor cons = adapterClass.getConstructor(Context.class, DrawerMenuRecyclerViewAdapter.DrawerMenuCallbacks.class);
                Object instance = cons.newInstance(this, this);
                if (instance instanceof RecyclerView.Adapter) {
                    mMenuViewHolder.recyclerView.setAdapter((RecyclerView.Adapter) instance);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Hookup events
            mMenuViewHolder.btnTorStatus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (App.getSettings().requireProxy()) {
                        if (App.getInstance().socialReader.isOnline() == SocialReader.NOT_ONLINE_NO_PROXY) {
                            mDrawerLayout.closeDrawers();
                            App.getInstance().socialReader.connectProxy(FragmentActivityWithMenu.this);
                        }
                    }
                }
            });

            mMenuViewHolder.btnShowPhotos.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (App.getSettings().syncMode() == SyncMode.LetItFlow)
                        App.getSettings().setSyncMode(SyncMode.BitWise);
                    else
                        App.getSettings().setSyncMode(SyncMode.LetItFlow);
                    mMenuViewHolder.btnShowPhotos.setChecked(App.getSettings().syncMode() == SyncMode.LetItFlow);
                }
            });
        }
    }

    class UpdateLeftSideMenuTask extends ThreadedTask<Void, Void, Void> {
        private ArrayList<Feed> feeds;
        private boolean isUsingProxy;
        private boolean isUsingPsiphon;
        private boolean showImages;
        private boolean isOnline;

        @Override
        protected Void doInBackground(Void... values) {
            createMenuViewHolder();
            feeds = App.getInstance().socialReader.getSubscribedFeedsList();
            isUsingProxy = App.getInstance().socialReader.useProxy();
            isUsingPsiphon = (App.getSettings().proxyType() == ProxyType.Psiphon);
            isOnline = App.getInstance().socialReader.isProxyOnline();
            showImages = (App.getSettings().syncMode() == SyncMode.LetItFlow);

            // Update adapter
            RecyclerView.Adapter adapter = mMenuViewHolder.recyclerView.getAdapter();
            if (adapter instanceof DrawerMenuRecyclerViewAdapter) {
                ((DrawerMenuRecyclerViewAdapter)adapter).recalculateData();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            RecyclerView.Adapter adapter = mMenuViewHolder.recyclerView.getAdapter();
            if (adapter instanceof DrawerMenuRecyclerViewAdapter) {
                ((DrawerMenuRecyclerViewAdapter)adapter).update(feeds);
            } else {
                adapter.notifyDataSetChanged();
            }

            // Update TOR connection status
            //
            if (isUsingProxy) {
                mMenuViewHolder.btnTorStatus.setChecked(isOnline);
                if (isUsingPsiphon)
                    mMenuViewHolder.btnTorStatus.setText(isOnline ? R.string.menu_psiphon_connected : R.string.menu_psiphon_not_connected);
                else
                    mMenuViewHolder.btnTorStatus.setText(isOnline ? R.string.menu_tor_connected : R.string.menu_tor_not_connected);
                mMenuViewHolder.btnTorStatus.setCompoundDrawablesWithIntrinsicBounds(null, mMenuViewHolder.btnTorStatus.getContext().getResources().getDrawable(isUsingPsiphon ? R.drawable.button_psiphon_icon_selector : R.drawable.button_tor_icon_selector), null, null);
            } else {
                mMenuViewHolder.btnTorStatus.setChecked(false);
                mMenuViewHolder.btnTorStatus.setText(R.string.menu_tor_not_connected);
                mMenuViewHolder.btnTorStatus.setCompoundDrawablesWithIntrinsicBounds(null, mMenuViewHolder.btnTorStatus.getContext().getResources().getDrawable(R.drawable.button_tor_icon_selector), null, null);
            }
            mMenuViewHolder.btnShowPhotos.setChecked(showImages);
        }
    }

    @Override
    protected void onUnlockedActivityResult(int requestCode, int resultCode, Intent data) {
        super.onUnlockedActivityResult(requestCode, resultCode, data);
        if (requestCode == UICallbacks.RequestCode.CREATE_CHAT_ACCOUNT.Value) {
            if (resultCode == RESULT_OK) {
                App.getSettings().setChatUsernamePasswordSet();
                // Then redirect somewhere?
            }
        }
    }

    @Override
    public Object getSystemService(String name) {
        if (LAYOUT_INFLATER_SERVICE.equals(name)) {
            if (mInflater != null)
                return mInflater;
        }
        return super.getSystemService(name);
    }


    @Override
    public void runAfterMenuClose(Runnable runnable) {
        mDeferredCommands.add(runnable);
        if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            runDeferredCommands();
        }
    }

    private void runDeferredCommands() {
        for (Runnable r : mDeferredCommands)
            r.run();
        mDeferredCommands.clear();
    }

    @Override
    public void onFeedSelect(FeedFilterType type, long feedId, Object source) {
    }

    @Override
    public void onTagSelect(String tag) {
    }

    @Override
    public void onRequestResync(Feed feed) {
    }

    @Override
    public void onItemFavoriteStatusChanged(Item item) {
    }

    @Override
    public boolean onCommand(Context context, int command, Bundle commandParameters) {
        return false;
    }

    /**
     * This is a shortcut to {@link App#getCurrentFeedFilterType()} }
     *
     * @return the currently displayed feed type
     */
    protected FeedFilterType getCurrentFeedFilterType() {
        return App.getInstance().getCurrentFeedFilterType();
    }

    /**
     * This is a shortcut to {@link App#getCurrentFeed()} }
     *
     * @return the currently displayed feed (if any)
     */
    protected Feed getCurrentFeed() {
        return App.getInstance().getCurrentFeed();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mUsePullDownActionBar && mToolbar != null) {
            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                if (mPullDownTouchSlop == 0) {
                    final ViewConfiguration configuration = ViewConfiguration
                            .get(this);
                    mPullDownTouchSlop = ViewConfigurationCompat
                            .getScaledPagingTouchSlop(configuration);
                }
                mPullDownStartY = ev.getY();
                mPullDownStartToolbarOffset = mToolbarHideOffset;
            } else if (ev.getAction() == MotionEvent.ACTION_CANCEL
                    || ev.getAction() == MotionEvent.ACTION_UP) {
                int diff = mToolbarHideOffset - mPullDownStartToolbarOffset;
                if (diff < 0) {
                    if (-diff > mPullDownTouchSlop)
                        showActionBar();
                    else
                        hideActionBar();
                } else if (diff > 0) {
                    if (diff > mPullDownTouchSlop)
                        hideActionBar();
                    else
                        showActionBar();
                }
            } else if (ev.getAction() == MotionEvent.ACTION_MOVE) {
                float deltaY = mPullDownStartY - ev.getY();
                float newOffset = mToolbarHideOffset + deltaY;
                newOffset = Math.max(0, Math.min(newOffset, getMaxHideOffset()));
                setToolbarHideOffset((int)newOffset);
                //float toolbarDelta = mPullDownStartToolbarOffset - newOffset;
                //ev.offsetLocation(0, toolbarDelta);
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    public boolean getUsePullDownActionBar() {
        return this.mUsePullDownActionBar;
    }

    public int getPullDownActionBarHeight() {
        if (mToolbar != null)
            return mToolbar.getHeight() - (getUsePullDownActionBar() ? this.mToolbarHideOffset : 0);
        return 0;
    }

    public void setUsePullDownActionBar(boolean usePullDownActionBar) {
        mUsePullDownActionBar = usePullDownActionBar;
        if (!mUsePullDownActionBar) {
            // Show the bar!
            mToolbar.removeCallbacks(hideActionBarRunnable);
            animateActionBarHideOffsetTo(0);
        } else {
            // Use pulldown. If visible, it will be hidden soon...
            mToolbar.postDelayed(hideActionBarRunnable, 5000);
        }
    }

    public interface PullDownActionBarListener {
        void onOffsetChanged(int height, int offset);
    }

    private ArrayList<WeakReference<PullDownActionBarListener>> pullDownListeners = new ArrayList<WeakReference<PullDownActionBarListener>>();

    public void addPullDownActionBarListener(PullDownActionBarListener newListener) {
        boolean alreadyAdded = false;
        for (int i = pullDownListeners.size() - 1; i >= 0; i--) {
            PullDownActionBarListener listener = pullDownListeners.get(i).get();
            if (listener == null)
                pullDownListeners.remove(i);
            else if (listener == newListener)
                alreadyAdded = true; // No break, we want to cleanup list at the same time...
        }
        if (!alreadyAdded)
            pullDownListeners.add(new WeakReference<PullDownActionBarListener>(newListener));
    }

    public void removePullDownActionBarListener(PullDownActionBarListener oldListener) {
        for (int i = pullDownListeners.size() - 1; i >= 0; i--) {
            PullDownActionBarListener listener = pullDownListeners.get(i).get();
            if (listener == null || listener == oldListener)
                pullDownListeners.remove(i);
        }
    }

    private final Runnable hideActionBarRunnable = new Runnable() {
        @Override
        public void run() {
            hideActionBar();
        }
    };

    public void showActionBar() {
        if (mToolbar != null) {
            mToolbar.removeCallbacks(hideActionBarRunnable);
            animateActionBarHideOffsetTo(0);
            mToolbar.postDelayed(hideActionBarRunnable, 5000);
        }
    }

    public void hideActionBar() {
        animateActionBarHideOffsetTo(getMaxHideOffset());
    }

    private int getMaxHideOffset() {
        int maxOffset = mToolbar.getHeight();
        if (mToolbarShadow != null)
            maxOffset += mToolbarShadow.getHeight();
        return maxOffset;
    }

    private void animateActionBarHideOffsetTo(int target) {
        if (getMaxHideOffset() > 0) {
            int distance = Math.abs(mToolbarHideOffset - target);
            long duration = (300 * distance) / getMaxHideOffset();
            final ActionBarAnim anim = new ActionBarAnim(target);
            anim.setDuration(duration);
            mToolbar.startAnimation(anim);
        }
    }

    private void setToolbarHideOffset(int offset) {
        mToolbarHideOffset = offset;
        ViewCompat.setTranslationY(mToolbar, -mToolbarHideOffset);
        if (mToolbarShadow != null)
            ViewCompat.setTranslationY(mToolbarShadow, -mToolbarHideOffset);
        onSetToolbarHideOffset(offset);
        for (int i = pullDownListeners.size() - 1; i >= 0; i--) {
            PullDownActionBarListener listener = pullDownListeners.get(i).get();
            if (listener == null)
                pullDownListeners.remove(i);
            else
                listener.onOffsetChanged(mToolbar.getHeight() - offset, offset);
        }
    }

    protected void onSetToolbarHideOffset(int offset) {
        // Can be overridden
    }

    public class ActionBarAnim extends Animation {
        int fromOffset;
        int toOffset;

        public ActionBarAnim(int toOffset) {
            this.fromOffset = mToolbarHideOffset;
            this.toOffset = toOffset;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            int newOffset = (int) (fromOffset + ((toOffset - fromOffset) * interpolatedTime));
            setToolbarHideOffset(newOffset);
        }
    }
}
