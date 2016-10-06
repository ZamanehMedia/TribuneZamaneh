package info.guardianproject.securereaderinterface.adapters;


import android.content.Context;

import com.tinymission.rss.Feed;

import java.util.ArrayList;

import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.MainActivity;
import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.models.FeedFilterType;
import info.guardianproject.securereaderinterface.ui.UICallbacks;

public class DrawerMenuRecyclerViewAdapter
        extends DrawerMenuRecyclerViewAdapterBase {

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

    private DrawerMenuCallbacks mCallbacks;

    public DrawerMenuRecyclerViewAdapter(Context context, DrawerMenuCallbacks callbacks) {
        super(context);
        mCallbacks = callbacks;
        setHasStableIds(false);
    }

    public void update(ArrayList<Feed> feeds, int countFavorites, int countShared) {
        clear();
        addAllFeedsItem();
        addFavoritesItem(countFavorites);
        addNearbyItem(countShared);

        // Feeds
        if (feeds != null && feeds.size() > 0) {
            for (Feed feed : feeds) {
                addFeedItem(feed);
                addFeedItem(feed);
                addFeedItem(feed);
                addFeedItem(feed);
            }
        }

        notifyDataSetChanged();
    }

    protected void addAllFeedsItem() {
        add(new MenuEntry(R.drawable.ic_menu_news, R.string.feed_filter_all_feeds, 0, true, -1, new SimpleMenuItemCallback() {
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
        add(new MenuEntry(R.drawable.ic_filter_favorites, R.string.feed_filter_favorites, 0, false, count, new SimpleMenuItemCallback() {
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
        add(new MenuEntry(R.drawable.ic_filter_secure_share, R.string.feed_filter_shared_stories, R.string.menu_receive_share, false, count, new SimpleMenuItemCallback() {
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
        add(new MenuEntry(R.drawable.ic_filter_logo_placeholder,
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


}