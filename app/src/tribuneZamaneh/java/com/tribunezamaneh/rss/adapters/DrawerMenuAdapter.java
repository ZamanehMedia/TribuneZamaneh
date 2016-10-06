package com.tribunezamaneh.rss.adapters;

import android.content.Context;

import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.PostActivity;
import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.adapters.DrawerMenuRecyclerViewAdapter;
import info.guardianproject.securereaderinterface.ui.UICallbacks;

public class DrawerMenuAdapter extends DrawerMenuRecyclerViewAdapter{
    private int mCountPosts;

    public DrawerMenuAdapter(Context context, DrawerMenuCallbacks callbacks) {
        super(context, callbacks);
    }

    @Override
    public void recalculateData() {
        super.recalculateData();
        mCountPosts = App.getInstance().socialReporter.getPosts().size();
    }

    @Override
    protected void addNearbyItem(int count) {
        super.addNearbyItem(count);
        addPostsItem(mCountPosts);
    }

    protected void addPostsItem(int count) {
        add(new MenuEntry(R.drawable.ic_menu_stories, R.string.menu_post, R.string.menu_post_new, false, count, new SimpleMenuItemCallback() {
            @Override
            public void onClicked() {
                mCallbacks.runAfterMenuClose(new Runnable() {
                    @Override
                    public void run() {
                        UICallbacks.handleCommand(getContext(), R.integer.command_posts_list, null);
                    }
                });
            }

            @Override
            public void onShortcutClicked() {
                mCallbacks.runAfterMenuClose(new Runnable() {
                    @Override
                    public void run() {
                        UICallbacks.handleCommand(getContext(), R.integer.command_post_add, null);
                    }
                });
            }

            @Override
            public boolean isSelected() {
                return getContext() instanceof PostActivity;
            }
        }));
    }
}
