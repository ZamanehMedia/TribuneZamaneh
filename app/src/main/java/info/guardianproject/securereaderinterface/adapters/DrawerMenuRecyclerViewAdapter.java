package info.guardianproject.securereaderinterface.adapters;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.tinymission.rss.Feed;

import java.util.ArrayList;
import java.util.List;

import info.guardianproject.securereader.Settings;
import info.guardianproject.securereader.SocialReader;
import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.MainActivity;
import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.models.FeedFilterType;
import info.guardianproject.securereaderinterface.ui.UICallbacks;

public class DrawerMenuRecyclerViewAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface DrawerMenuCallbacks {
        void viewAllFeeds();
        void viewFavorites();
        void viewShared();
        void receiveShare();
        void viewPopular();
        void viewDownloads();
        void viewFeed(Feed feedToView);
        void addNewFeed();
    }

    private final List<Object> mValues;
    private Context mContext;
    private DrawerMenuCallbacks mCallbacks;
    private final Adorner mAdorner;
    private boolean mIsOnline;

    private interface MenuItemCallback {
        void onClicked();
        void onRefresh();
        void onShortcutClicked();
        boolean isRefreshing();
        boolean isSelected();
    }

    private abstract class SimpleMenuItemCallback implements MenuItemCallback {

        @Override
        public void onClicked() {
        }

        @Override
        public void onRefresh() {
        }

        @Override
        public void onShortcutClicked() {
        }

        @Override
        public boolean isRefreshing() {
            return false;
        }
    }

    public DrawerMenuRecyclerViewAdapter(Context context, DrawerMenuCallbacks callbacks) {
        super();
        mContext = context;
        mCallbacks = callbacks;
        setHasStableIds(false);
        mValues = new ArrayList<>();
        mAdorner = new Adorner();
    }

    private boolean isOnline() {
        boolean isOnline = true;
        int onlineMode = App.getInstance().socialReader.isOnline();
        if (onlineMode == SocialReader.NOT_ONLINE_NO_WIFI || onlineMode == SocialReader.NOT_ONLINE_NO_WIFI_OR_NETWORK)
            isOnline = false;
        return isOnline;
    }

    public void update(ArrayList<Feed> feeds, int countFavorites, int countShared) {
        mIsOnline = isOnline();
        mValues.clear();

        addAllFeedsItem();
        addFavoritesItem(countFavorites);
        addNearbyItem(countShared);

        // Feeds
        if (feeds != null && feeds.size() > 0) {
            for (Feed feed : feeds) {
                addFeedItem(feed);
            }
        }

        notifyDataSetChanged();
    }

    protected void addAllFeedsItem() {
        mValues.add(new MenuEntry(R.drawable.ic_menu_news, R.string.feed_filter_all_feeds, 0, true, -1, new SimpleMenuItemCallback() {
            @Override
            public boolean isRefreshing() {
                return App.getInstance().socialReader.manualSyncInProgress();
            }

            @Override
            public void onClicked() {
                if (mCallbacks != null)
                    mCallbacks.viewAllFeeds();
            }

            @Override
            public void onRefresh() {
                UICallbacks.requestResync(null);
            }

            @Override
            public boolean isSelected() {
                return mContext instanceof MainActivity && App.getInstance().getCurrentFeedFilterType() == FeedFilterType.ALL_FEEDS;
            }
        }));
    }

    protected void addFavoritesItem(int count) {
        mValues.add(new MenuEntry(R.drawable.ic_filter_favorites, R.string.feed_filter_favorites, 0, false, count, new SimpleMenuItemCallback() {
            @Override
            public void onClicked() {
                if (mCallbacks != null)
                    mCallbacks.viewFavorites();
            }

            @Override
            public boolean isSelected() {
                return mContext instanceof MainActivity && App.getInstance().getCurrentFeedFilterType() == FeedFilterType.FAVORITES;
            }
        }));
    }

    protected void addNearbyItem(int count) {
        mValues.add(new MenuEntry(R.drawable.ic_filter_secure_share, R.string.feed_filter_shared_stories, R.string.menu_receive_share, false, count, new SimpleMenuItemCallback() {
            @Override
            public void onClicked() {
                if (mCallbacks != null)
                    mCallbacks.viewShared();
            }

            @Override
            public void onShortcutClicked() {
                if (mCallbacks != null)
                    mCallbacks.receiveShare();
            }

            @Override
            public boolean isSelected() {
                return mContext instanceof MainActivity && App.getInstance().getCurrentFeedFilterType() == FeedFilterType.SHARED;
            }
        }));
    }

    protected void addFeedItem(final Feed feed) {
        mValues.add(new MenuEntry(R.drawable.ic_filter_logo_placeholder,
                feed.getTitle(),
                0,
                true, -1, new SimpleMenuItemCallback() {
            @Override
            public void onClicked() {
                if (mCallbacks != null)
                    mCallbacks.viewFeed(feed);
            }

            @Override
            public boolean isRefreshing() {
                return feed.getStatus() == Feed.STATUS_SYNC_IN_PROGRESS;
            }

            @Override
            public void onRefresh() {
                UICallbacks.requestResync(feed);
            }

            @Override
            public boolean isSelected() {
                return mContext instanceof MainActivity && App.getInstance().getCurrentFeedFilterType() == FeedFilterType.SINGLE_FEED &&
                        App.getInstance().getCurrentFeed() != null && App.getInstance().getCurrentFeed().getDatabaseId() == feed.getDatabaseId();
            }
        }));
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder ret = null;
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.drawer_menu_item, parent, false);
        ret = new ViewHolderMenuItem(view);
        return ret;
    }


    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        Context context = holder.itemView.getContext();

        if (mValues.get(position) instanceof MenuEntry) {
            final MenuEntry e = (MenuEntry)mValues.get(position);

            ViewHolderMenuItem viewHolder = (ViewHolderMenuItem) holder;
            viewHolder.icon.setImageResource(e.resIdIcon);
            if (e.resIdTitle != 0)
                viewHolder.title.setText(e.resIdTitle);
            else
                viewHolder.title.setText(e.stringTitle);
            if (Build.VERSION.SDK_INT >= 23)
                viewHolder.title.setTextAppearance(e.menuItemCallback.isSelected() ? R.style.LeftSideMenuItemCurrentAppearance : R.style.LeftSideMenuItemAppearance);
            else
                viewHolder.title.setTextAppearance(mContext, e.menuItemCallback.isSelected() ? R.style.LeftSideMenuItemCurrentAppearance : R.style.LeftSideMenuItemAppearance);

            if (e.count < 0) {
                viewHolder.count.setVisibility(View.GONE);
            } else {
                viewHolder.count.setText(String.valueOf(e.count));
                viewHolder.count.setVisibility(View.VISIBLE);
            }
            if (e.showRefresh) {
                viewHolder.refresh.setVisibility(App.getSettings().syncFrequency() == Settings.SyncFrequency.Manual ? View.VISIBLE : View.INVISIBLE);
                viewHolder.refresh.setEnabled(mIsOnline);
                viewHolder.refresh.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        e.menuItemCallback.onRefresh();
                    }
                });
                if (viewHolder.refresh.getVisibility() == View.VISIBLE &&
                        e.menuItemCallback.isRefreshing())
                    viewHolder.refresh.startAnimation(AnimationUtils.loadAnimation(context, R.anim.rotate));
                else
                    viewHolder.refresh.clearAnimation();
            } else {
                viewHolder.refresh.clearAnimation();
                viewHolder.refresh.setOnClickListener(null);
                viewHolder.refresh.setVisibility(View.GONE);
            }
            if (e.resIdShortcutTitle != 0) {
                viewHolder.shortcut.setText(e.resIdShortcutTitle);
                viewHolder.shortcut.setVisibility(View.VISIBLE);
                viewHolder.shortcut.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        e.menuItemCallback.onShortcutClicked();
                    }
                });
            } else {
                viewHolder.shortcut.setOnClickListener(null);
                viewHolder.shortcut.setVisibility(View.GONE);
            }
            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    e.menuItemCallback.onClicked();
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolderMenuItem extends RecyclerView.ViewHolder {
        public final ImageView icon;
        public final TextView title;
        public final View refresh;
        public final TextView count;
        public final TextView shortcut;

        public ViewHolderMenuItem(View view) {
            super(view);
            icon = (ImageView) view.findViewById(R.id.icon);
            title = (TextView) view.findViewById(R.id.title);
            refresh = view.findViewById(R.id.refresh);
            count = (TextView) view.findViewById(R.id.count);
            shortcut = (TextView) view.findViewById(R.id.shortcut);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + title.getText() + "'";
        }
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        recyclerView.addItemDecoration(mAdorner);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        recyclerView.removeItemDecoration(mAdorner);
        super.onDetachedFromRecyclerView(recyclerView);
    }

    private class Adorner extends RecyclerView.ItemDecoration {
        private final Paint mPaint;
        public Adorner() {
            super();
            mPaint = new Paint();
            mPaint.setStrokeWidth(1);
            mPaint.setStyle(Paint.Style.STROKE);
            if (Build.VERSION.SDK_INT >= 23)
                mPaint.setColor(mContext.getResources().getColor(R.color.drawer_menu_divider_color, mContext.getTheme()));
            else
                mPaint.setColor(mContext.getResources().getColor(R.color.drawer_menu_divider_color));
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            int position = parent.getChildAdapterPosition(view);
            if (position > 0)
                outRect.top = 1;
        }

        @Override
        public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
            int dividerLeft = parent.getPaddingLeft();
            int dividerRight = parent.getWidth() - parent.getPaddingRight();

            int childCount = parent.getChildCount();
            for (int i = 0; i < childCount - 1; i++) {
                View child = parent.getChildAt(i);

                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

                int dividerTop = child.getBottom() + params.bottomMargin;
                canvas.drawLine(dividerLeft, dividerTop, dividerRight, dividerTop, mPaint);
            }
        }
    }

    private class MenuEntry {
        public int resIdIcon;
        public int resIdTitle;
        public String stringTitle;
        public int resIdShortcutTitle;
        public boolean showRefresh;
        public int count;
        public MenuItemCallback menuItemCallback;
        public MenuEntry(int resIdIcon, int resIdTitle, int resIdShortcutTitle, boolean showRefresh, int count, MenuItemCallback menuItemCallback) {
            this.resIdIcon = resIdIcon;
            this.resIdTitle = resIdTitle;
            this.resIdShortcutTitle = resIdShortcutTitle;
            this.showRefresh = showRefresh;
            this.count = count;
            this.menuItemCallback = menuItemCallback;
        }

        public MenuEntry(int resIdIcon, String stringTitle, int resIdShortcutTitle, boolean showRefresh, int count, MenuItemCallback menuItemCallback) {
            this.resIdIcon = resIdIcon;
            this.stringTitle = stringTitle;
            this.resIdShortcutTitle = resIdShortcutTitle;
            this.showRefresh = showRefresh;
            this.count = count;
            this.menuItemCallback = menuItemCallback;
        }

    }
}