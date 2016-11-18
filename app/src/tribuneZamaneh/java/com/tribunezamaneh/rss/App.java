package com.tribunezamaneh.rss;
		
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.tribunezamaneh.rss.adapters.DrawerMenuAdapter;

import info.guardianproject.securereader.SocialReporter;
import info.guardianproject.securereaderinterface.BuildConfig;
import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.ui.UICallbackListener;
import info.guardianproject.securereaderinterface.ui.UICallbacks;

public class App extends info.guardianproject.securereaderinterface.App
{
	public static final String WORDPRESS_LOGIN_URL = "https://www.postmodernapps.net/home/wp-login.php";

	public static final String LOGTAG = "App";
	public static final boolean LOGGING = false;
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		SocialReporter.REQUIRE_PROXY = false;
		UICallbacks.getInstance().addListener(new UICallbackListener()
		{
			@Override
			public boolean onCommand(Context context, int command, Bundle commandParameters) {

				switch (command) {

					case R.integer.command_post_add:
					{
						Intent intent = new Intent(context, AddPostActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
						context.startActivity(intent);
						((Activity) context).overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
						break;
					}

					case R.integer.command_comment:
					{
						if (BuildConfig.UI_ENABLE_COMMENTS)
						{
							Intent intent = new Intent(context, CommentsActivity.class);
							intent.putExtra("item", commandParameters.getSerializable("item"));
							intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
							context.startActivity(intent);
						}
						return true;
					}

					case R.integer.command_posts_list:
					{
						Intent intent = new Intent(context, PostActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
						if (commandParameters != null && commandParameters.containsKey("go_to_tab"))
						{
							intent.putExtra("go_to_tab", commandParameters.getInt("go_to_tab", -1));
						}
						context.startActivity(intent);
						return true;
					}
				}
				return super.onCommand(context, command, commandParameters);
			}
		});
	}

	@Override
	public Class getDrawerMenuAdapterClass() {
		return DrawerMenuAdapter.class;
	}

	@Override
	protected void onPrepareOptionsMenu(Activity activity, Menu menu) {
		super.onPrepareOptionsMenu(activity, menu);

		// Show log out if we are logged in!
		MenuItem item = menu.findItem(R.id.menu_logout);
		if (item != null) {
			item.setVisible(com.tribunezamaneh.rss.App.isSignedIn());
		}
	}

	@Override
	public boolean onOptionsItemSelected(Activity activity, int itemId) {
		if (itemId == R.id.menu_add_post) {
			Intent intent = new Intent(this, AddPostActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			activity.startActivity(intent);
			activity.overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
			return true;
		} else if (itemId == R.id.menu_logout) {
			//App.getInstance().socialReader.ssettings.setXMLRPCUsername("");
			App.getInstance().socialReader.ssettings.setXMLRPCPassword("");
			//return true; Don't return, give Comments and AddPost a change to handle this as well!
		}
		return super.onOptionsItemSelected(activity, itemId);
	}

	public static boolean isSignedIn() {
		boolean isSignedIn = false;
		if (App.getInstance().socialReader != null && App.getInstance().socialReader.ssettings != null)
			isSignedIn = !TextUtils.isEmpty(App.getInstance().socialReader.ssettings.getXMLRPCPassword());
		return isSignedIn;
	}
}
