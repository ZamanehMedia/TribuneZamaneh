package com.tribunezamaneh.rss;
		
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.tinymission.rss.Feed;
import com.tinymission.rss.Item;
import com.tribunezamaneh.rss.adapters.DrawerMenuAdapter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import info.guardianproject.securereader.Settings;
import info.guardianproject.securereader.SocialReader;
import info.guardianproject.securereader.SocialReporter;
import info.guardianproject.securereaderinterface.BuildConfig;
import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.ui.ContentFormatter;
import info.guardianproject.securereaderinterface.ui.UICallbackListener;
import info.guardianproject.securereaderinterface.ui.UICallbacks;
import info.guardianproject.securereaderinterface.views.StoryItemView;

public class App extends info.guardianproject.securereaderinterface.App implements SocialReader.SocialReaderFeedPreprocessor
{
	public static final String WORDPRESS_LOGIN_URL = "https://www.tribunezamaneh.com/wp-login.php";

	public static final String LOGTAG = "App";
	public static final boolean LOGGING = false;

	private ContentFormatter mItemContentFormatter = null;

	@Override
	public void onCreate()
	{
		super.onCreate();

		// Currently hardcode Farsi
		m_settings.setUiLanguage(Settings.UiLanguage.Farsi);
		m_settings.setReaderSwipeDirection(Settings.ReaderSwipeDirection.Rtl);
		applyUiLanguage(false);

		socialReader.setFeedPreprocessor(this);
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

					case R.integer.command_read_more: {
						if (LOGGING)
							Log.v(LOGTAG, "Read More");
						if (commandParameters != null && commandParameters.containsKey("url")) {
							try {
								Uri uri = Uri.parse(commandParameters.getString("url"));

								// Use tor or not?
								if (info.guardianproject.securereaderinterface.App.getSettings().requireProxy()) {
									Intent intent = new Intent(Intent.ACTION_VIEW, uri);
									String thisPackageName = info.guardianproject.securereaderinterface.App.getInstance().getPackageName();

									// Instead of using built in functionality, we create our own chooser so that we
									// can remove ourselves from the list (opening the story in this app would actually
									// take us to the AddFeed page, so it does not make sense to have it as an option)
									List<Intent> targetedShareIntents = new ArrayList<Intent>();
									List<ResolveInfo> resInfo = context.getPackageManager().queryIntentActivities(intent, 0);
									if (resInfo != null && resInfo.size() > 0) {
										for (ResolveInfo resolveInfo : resInfo) {
											String packageName = resolveInfo.activityInfo.packageName;

											Intent targetedShareIntent = (Intent) intent.clone();
											targetedShareIntent.setPackage(packageName);
											if (!packageName.equals(thisPackageName)) // Remove
											// ourselves
											{
												targetedShareIntents.add(targetedShareIntent);
											}
										}

										if (targetedShareIntents.size() > 0) {
											Intent chooserIntent = Intent.createChooser(targetedShareIntents.remove(0), null);
											chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedShareIntents.toArray(new Parcelable[]{}));
											context.startActivity(chooserIntent);
										}
									}
								} else {
									// Open
									Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
									context.startActivity(browserIntent);
								}
							} catch (Exception e) {
								if (LOGGING)
									Log.d(LOGTAG, "Error trying to open read more link: " + commandParameters.getString("url"));
							}
						}
						break;
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
	public ContentFormatter getItemContentFormatter() {
		if (mItemContentFormatter == null) {
			mItemContentFormatter = new HTMLContentFormatter();
		}
		return mItemContentFormatter;
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
			App.getInstance().socialReader.ssettings.setXMLRPCUsername("");
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

	@Override
	public String onGetFeedURL(Feed feed) {
		return null;
	}

	@Override
	public InputStream onFeedDownloaded(Feed feed, InputStream content, Map<String, String> headers) {
		return XsltTransform(m_context.getResources().openRawResource(R.raw.feed_transform_inline_images), content);
	}

	public static InputStream XsltTransform(InputStream stylesheet, InputStream data) {
		String html = "";

		try {
			Source xmlSource = new StreamSource(data);
			Source xsltSource = new StreamSource(stylesheet);

			StringWriter writer = new StringWriter();
			Result result = new StreamResult(writer);
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory.newTransformer(xsltSource);
			transformer.transform(xmlSource, result);
			html = writer.toString();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ByteArrayInputStream(html.getBytes());
	}
}
