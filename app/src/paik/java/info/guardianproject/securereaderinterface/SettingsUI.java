package info.guardianproject.securereaderinterface;

import info.guardianproject.securereader.Settings;
import android.content.Context;

public class SettingsUI extends Settings
{
	public static final String KEY_ENABLE_SCREENSHOTS = "enable_screenshots";

	public static final String KEY_FIRST_USE_HAS_SEEN_INTRO = "intro_seen";
	public static final String KEY_FIRST_USE_HAS_SELECTED_PROXY = "proxy_selected";
	public static final String KEY_FIRST_USE_HAS_CURATED_FEEDS = "curated_feeds";
	
	public SettingsUI(Context _context)
	{
		super(_context);
	}

	/**
	 * @return Gets whether screen captures are enabled
	 * 
	 */
	public boolean enableScreenshots()
	{
		return mPrefs.getBoolean(KEY_ENABLE_SCREENSHOTS, false);
	}

	/**
	 * @return Sets whether screen captures are enabled
	 * 
	 */
	public void setEnableScreenshots(boolean enable)
	{
		mPrefs.edit().putBoolean(KEY_ENABLE_SCREENSHOTS, enable).commit();
	}
	
	/**
	 * @return Gets first use setting
	 * 
	 */
	public boolean getFirstUse(String key)
	{
		return mPrefs.getBoolean("first_use_" + key, false);
	}

	/**
	 * @return Sets first use setting
	 * 
	 */
	public void setFirstUse(String key, boolean value)
	{
		mPrefs.edit().putBoolean("first_use_" + key, value).commit();
	}

	@Override
	public boolean hasShownHelp() {
		// Override this, we don't want to show the help screen on startup
		return true;
	}
	
	@Override
	public void resetSettings()
	{
		mPrefs.edit().clear().commit();
		super.resetSettings();
	}

	/**
	 * Return the two character language code for the currently selected language
	 */
	public String uiLanguageCode() {
		UiLanguage lang = uiLanguage();
		String language = "en";
		if (lang == UiLanguage.Farsi)
			language = "fa";
		else if (lang == UiLanguage.Tibetan)
			language = "bo";
		else if (lang == UiLanguage.Chinese)
			language = "zh";
		else if (lang == UiLanguage.Ukrainian)
			language = "uk";
		else if (lang == UiLanguage.Russian)
			language = "ru";
		else if (lang == UiLanguage.Spanish)
			language = "es";
		else if (lang == UiLanguage.Japanese)
			language = "ja";
		else if (lang == UiLanguage.Norwegian)
			language = "nb";
		else if (lang == UiLanguage.Turkish)
			language = "tr";
		return language;
	}
}
