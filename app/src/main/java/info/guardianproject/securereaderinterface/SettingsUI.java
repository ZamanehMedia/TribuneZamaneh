package info.guardianproject.securereaderinterface;

import info.guardianproject.securereader.Settings;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SettingsUI extends Settings
{
	public static final String KEY_ENABLE_SCREENSHOTS = "enable_screenshots";
	public static final String KEY_ONBOARDING_STAGE = "onboarding_stage";

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
	 * @return The onboarding stage we are on (if any)
	 * 
	 */
	public int getOnboardingStage()
	{
		return mPrefs.getInt(KEY_ONBOARDING_STAGE, 0);
	}

	/**
	 * @return Sets onboarding stage
	 * 
	 */
	public void setOnboardingStage(int value)
	{
		mPrefs.edit().putInt(KEY_ONBOARDING_STAGE, value).commit();
	}

	@Override
	public boolean hasShownHelp() {
		// Override this if we don't want to show the help screen on startup
		if (!BuildConfig.UI_ENABLE_STARTUP_HELP)
			return true;
		return super.hasShownHelp();
	}
	
	@Override
	public void resetSettings()
	{
		mPrefs.edit().clear().commit();
		super.resetSettings();
	}

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
