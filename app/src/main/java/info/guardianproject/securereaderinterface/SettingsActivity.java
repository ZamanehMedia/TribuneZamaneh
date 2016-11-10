package info.guardianproject.securereaderinterface;

import info.guardianproject.securereader.Settings;
import info.guardianproject.securereaderinterface.ui.LayoutFactoryWrapper;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;
import info.guardianproject.securereaderinterface.widgets.GroupView;
import info.guardianproject.securereaderinterface.widgets.InitialScrollScrollView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import info.guardianproject.cacheword.PassphraseSecrets;

public class SettingsActivity extends FragmentActivityWithMenu implements ICacheWordSubscriber, LayoutFactoryWrapper.Callback {
	private static final boolean LOGGING = true;
	private static final String LOGTAG = "Settings";

	public static final String EXTRA_GO_TO_GROUP = "go_to_group";

	SettingsUI mSettings;
	private InitialScrollScrollView rootView;

	private boolean mIsBeingRecreated;
	private String mLastChangedSetting;

	private ArrayList<RadioButtonChangeListener> mRadioButtonListeners;
	private ArrayList<CheckBoxChangeListener> mCheckboxListeners;
	private int mDynamicId = 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		mSettings = App.getSettings();
		super.onCreate(savedInstanceState);
		// Set these before inflating!
		getLayoutFactoryWrapper().setCallback(this);
		mRadioButtonListeners = new ArrayList<>();
		mCheckboxListeners = new ArrayList<>();
		setContentView(R.layout.activity_settings);
		setMenuIdentifier(R.menu.activity_settings);
		setDisplayHomeAsUp(true);

		rootView = (InitialScrollScrollView) findViewById(R.id.root);

		TypedArray array = this.obtainStyledAttributes(R.style.SettingsRadioButtonSubStyle, new int[]{android.R.attr.textColor});
		if (array != null) {
			int color = array.getColor(0, 0x999999);
			CharacterStyle colored = new ForegroundColorSpan(color);
			CharacterStyle small = new RelativeSizeSpan(0.85f);

			applySpanToAllRadioButtons(rootView, small, colored);

			array.recycle();
		}

		if (getIntent().hasExtra("savedInstance")) {
			Bundle savedInstance = getIntent().getBundleExtra("savedInstance");
			getIntent().removeExtra("savedInstance");
			if (savedInstance != null) {
				if (savedInstance.containsKey("expandedViews"))
					expandSelectedGroupViews(rootView, savedInstance.getIntegerArrayList("expandedViews"));

				int scrollToViewId = savedInstance.getInt("scrollToViewId", View.NO_ID);
				int scrollToViewOffset = savedInstance.getInt("scrollToViewOffset", 0);
				rootView.setInitialPosition(scrollToViewId, scrollToViewOffset);
			}
		}
	}

	private void applySpanToAllRadioButtons(ViewGroup parent, CharacterStyle... cs) {
		for (int i = 0; i < parent.getChildCount(); i++) {
			View view = parent.getChildAt(i);
			if (view instanceof RadioButton) {
				RadioButton rb = (RadioButton) view;
				rb.setText(setSpanOnMultilineText(rb.getText(), cs));
			} else if (view instanceof ViewGroup) {
				applySpanToAllRadioButtons((ViewGroup) view, cs);
			}
		}
	}

	private CharSequence setSpanOnMultilineText(CharSequence text, CharacterStyle... cs) {
		int idxBreak = text.toString().indexOf('\n');
		if (idxBreak > -1) {
			StringBuilder sb = new StringBuilder(text);
			sb.insert(idxBreak, "##");
			sb.append("##");
			return UIHelpers.setSpanBetweenTokens(sb.toString(), "##", cs);
		}
		return text;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (getIntent().hasExtra(EXTRA_GO_TO_GROUP)) {
			handleGoToGroup(getIntent().getIntExtra(EXTRA_GO_TO_GROUP, 0));
			getIntent().removeExtra(EXTRA_GO_TO_GROUP);
		}
	}

	private void handleGoToGroup(int goToSection) {
		if (goToSection != 0) {
			final View view = rootView.findViewById(goToSection);
			if (view != null) {
				if (view instanceof GroupView) {
					((GroupView) view).setExpanded(true, false);
				}

				rootView.post(new Runnable() {
					@Override
					public void run() {
						int top = view.getTop();
						rootView.scrollTo(0, top - 5);
					}
				});
			}
		}
	}

	private void promptForNewPassphrase(final int setTimeoutToThisValue) {
		View contentView = LayoutInflater.from(this).inflate(R.layout.settings_change_passphrase, null, false);

		final EditText editEnterPassphrase = (EditText) contentView.findViewById(R.id.editEnterPassphrase);
		final EditText editNewPassphrase = (EditText) contentView.findViewById(R.id.editNewPassphrase);
		final EditText editConfirmNewPassphrase = (EditText) contentView.findViewById(R.id.editConfirmNewPassphrase);

		// If we have an auto-generated PW, populate field and hide it
		if (mSettings.passphraseTimeout() == 0) {
			editEnterPassphrase.setText(mSettings.launchPassphrase());
			editEnterPassphrase.setVisibility(View.GONE);
		}

		Builder alert = new AlertDialog.Builder(this)
				.setTitle(mSettings.passphraseTimeout() == 0 ? R.string.settings_security_create_passphrase : R.string.settings_security_change_passphrase)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (editNewPassphrase.getText().length() == 0 && editConfirmNewPassphrase.getText().length() == 0) {
							dialog.dismiss();
							promptForNewPassphrase(setTimeoutToThisValue);
							return; // Try again...
						}

						if (!(editNewPassphrase.getText().toString().equals(editConfirmNewPassphrase.getText().toString()))) {
							Toast.makeText(SettingsActivity.this, getString(R.string.change_passphrase_not_matching), Toast.LENGTH_LONG).show();
							dialog.dismiss();
							promptForNewPassphrase(setTimeoutToThisValue);
							return; // Try again...
						}

						CacheWordHandler cwh = new CacheWordHandler((Context) SettingsActivity.this);

						char[] passwd = editEnterPassphrase.getText().toString().toCharArray();
						PassphraseSecrets secrets;
						try {
							secrets = PassphraseSecrets.fetchSecrets(SettingsActivity.this, passwd);
							cwh.changePassphrase(secrets, editNewPassphrase.getText().toString().toCharArray());
							Toast.makeText(SettingsActivity.this, getString(R.string.change_passphrase_changed), Toast.LENGTH_LONG).show();
							if (setTimeoutToThisValue != mSettings.passphraseTimeout())
								mSettings.setPassphraseTimeout(setTimeoutToThisValue);
						} catch (Exception e) {
							// Invalid password or the secret key has been
							if (LOGGING)
								Log.e(LOGTAG, e.getMessage());

							Toast.makeText(SettingsActivity.this, getString(R.string.change_passphrase_incorrect), Toast.LENGTH_LONG).show();
							dialog.dismiss();
							promptForNewPassphrase(setTimeoutToThisValue);
							return; // Try again...
						}

						dialog.dismiss();
					}
				})
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				})
				.setView(contentView);
		AlertDialog dialog = alert.create();
		dialog.show();
	}

	private void setTimeoutToNever() {
		View contentView = LayoutInflater.from(this).inflate(R.layout.settings_change_passphrase, null, false);

		final EditText editEnterPassphrase = (EditText) contentView.findViewById(R.id.editEnterPassphrase);
		final EditText editNewPassphrase = (EditText) contentView.findViewById(R.id.editNewPassphrase);
		final EditText editConfirmNewPassphrase = (EditText) contentView.findViewById(R.id.editConfirmNewPassphrase);
		editNewPassphrase.setVisibility(View.GONE);
		editConfirmNewPassphrase.setVisibility(View.GONE);
		Builder alert = new AlertDialog.Builder(this)
				.setTitle(R.string.settings_security_enter_passphrase)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						CacheWordHandler cwh = new CacheWordHandler((Context) SettingsActivity.this);

						char[] passwd = editEnterPassphrase.getText().toString().toCharArray();
						PassphraseSecrets secrets;
						try {
							secrets = PassphraseSecrets.fetchSecrets(SettingsActivity.this, passwd);
							cwh.changePassphrase(secrets, LockScreenActivity.getCWPassword().toCharArray());
							mSettings.setPassphraseTimeout(0);
						} catch (Exception e) {
							// Invalid password or the secret key has been
							if (LOGGING)
								Log.e(LOGTAG, e.getMessage());
							dialog.dismiss();
							setTimeoutToNever();
							return; // Try again...
						}
						dialog.dismiss();
					}
				})
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				})
				.setView(contentView);
		AlertDialog dialog = alert.create();
		dialog.show();
	}

	/**
	 * Lets the user input a kill passphrase
	 *
	 * @param setToOnIfSuccessful If true, update the settings if we manage to set the
	 *                            passphrase.
	 */
	private void promptForKillPassphrase(final boolean setToOnIfSuccessful) {
		View contentView = LayoutInflater.from(this).inflate(R.layout.settings_set_kill_passphrase, null, false);

		final EditText editNewPassphrase = (EditText) contentView.findViewById(R.id.editNewPassphrase);
		final EditText editConfirmNewPassphrase = (EditText) contentView.findViewById(R.id.editConfirmNewPassphrase);

		Builder alert = new AlertDialog.Builder(this)
				.setTitle(R.string.settings_security_set_kill_passphrase)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (editNewPassphrase.getText().length() == 0 && editConfirmNewPassphrase.getText().length() == 0) {
							dialog.dismiss();
							promptForKillPassphrase(setToOnIfSuccessful);
							return; // Try again...
						}

						// Check old
						boolean matching = (editNewPassphrase.getText().toString().equals(editConfirmNewPassphrase.getText().toString()));
						boolean sameAsPassphrase = false;
						CacheWordHandler cwh = new CacheWordHandler((Context) SettingsActivity.this);
						try {
							cwh.setPassphrase(editNewPassphrase.getText().toString().toCharArray());
							sameAsPassphrase = true;
						} catch (GeneralSecurityException e) {
							if (LOGGING)
								Log.e(LOGTAG, "Cacheword initialization failed: " + e.getMessage());
						}
						if (!matching || sameAsPassphrase) {
							editNewPassphrase.setText("");
							editConfirmNewPassphrase.setText("");
							editNewPassphrase.requestFocus();
							if (!matching)
								Toast.makeText(SettingsActivity.this, getString(R.string.lock_screen_passphrases_not_matching), Toast.LENGTH_LONG).show();
							else
								Toast.makeText(SettingsActivity.this, getString(R.string.settings_security_kill_passphrase_same_as_login), Toast.LENGTH_LONG).show();
							dialog.dismiss();
							promptForKillPassphrase(setToOnIfSuccessful);
							return; // Try again...
						}

						// Store
						App.getSettings().setKillPassphrase(editNewPassphrase.getText().toString());
						if (setToOnIfSuccessful)
							updateUseKillPassphrase();
						dialog.dismiss();
					}
				})
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				})
				.setView(contentView);
		AlertDialog dialog = alert.create();
		dialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				if (setToOnIfSuccessful)
					updateUseKillPassphrase();
			}
		});
		dialog.show();
	}

	private void updateUseKillPassphrase() {
		if (!TextUtils.isEmpty(mSettings.killPassphrase())) {
			mSettings.setUseKillPassphrase(true);
		} else {
			mSettings.setUseKillPassphrase(false);
		}
	}

	private void collectExpandedGroupViews(View current, ArrayList<Integer> expandedViews) {
		if (current instanceof ViewGroup) {
			for (int child = 0; child < ((ViewGroup) current).getChildCount(); child++)
				collectExpandedGroupViews(((ViewGroup) current).getChildAt(child), expandedViews);
		}
		if (current instanceof GroupView) {
			if (((GroupView) current).getExpanded())
				expandedViews.add(Integer.valueOf(current.getId()));
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// Dont call base, see http://stackoverflow.com/questions/4504024/android-localization-problem-not-all-items-in-the-layout-update-properly-when-s
		//super.onSaveInstanceState(outState);
		if (mIsBeingRecreated) {
			ArrayList<Integer> expandedViews = new ArrayList<Integer>();
			collectExpandedGroupViews(rootView, expandedViews);
			outState.putIntegerArrayList("expandedViews", expandedViews);

			if (mLastChangedSetting != null) {
				if (SettingsUI.KEY_ENABLE_SCREENSHOTS.equals(mLastChangedSetting)) {
					outState.putInt("scrollToViewId", R.id.chkEnableScreenshots);
					outState.putInt("scrollToViewOffset", UIHelpers.getRelativeTop(findViewById(R.id.chkEnableScreenshots)) - UIHelpers.getRelativeTop(rootView) - rootView.getScrollY());
				} else if (SettingsUI.KEY_UI_LANGUAGE.equals(mLastChangedSetting)) {
					outState.putInt("scrollToViewId", R.id.groupLanguage);
					outState.putInt("scrollToViewOffset", UIHelpers.getRelativeTop(findViewById(R.id.groupLanguage)) - UIHelpers.getRelativeTop(rootView) - rootView.getScrollY());
				}
			}
		}
	}

	private void expandSelectedGroupViews(View current, ArrayList<Integer> expandedViews) {
		for (int id : expandedViews) {
			View view = current.findViewById(id);
			if (view != null && view instanceof GroupView) {
				((GroupView) view).setExpanded(true, false);
			}
		}
	}

	@Override
	public void recreateNowOrOnResume() {
		mIsBeingRecreated = true;
		super.recreateNowOrOnResume();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		mLastChangedSetting = key;
		super.onSharedPreferenceChanged(sharedPreferences, key);
		if (key == SettingsUI.KEY_PROXY_TYPE) {
			App.getInstance().socialReader.connectProxy(SettingsActivity.this);
		}
		if (key.equals(SettingsUI.KEY_PROXY_TYPE) || key.equals(Settings.KEY_SYNC_MODE)) {
			updateLeftSideMenu();
		}
		refreshValues(key);
	}

	private void refreshValues(String key) {
		String bindingKey = getBindingKeyFromSettingsKey(key);
		if (bindingKey != null) {
			for (RadioButtonChangeListener listener : mRadioButtonListeners) {
				listener.onSettingChanged(bindingKey);
			}
			for (CheckBoxChangeListener listener : mCheckboxListeners) {
				listener.onSettingChanged(bindingKey);
			}
		}
	}


	private String getBindingKeyFromSettingsKey(String key) {
		int id = 0;
		switch (key) {
			case SettingsUI.KEY_PASSPHRASE_TIMEOUT:
				id = R.string.settingsBindingPassphraseTimeout;
				break;
			case SettingsUI.KEY_WIPE_APP:
				id = R.string.settingsBindingWipeApp;
				break;
			case SettingsUI.KEY_ARTICLE_EXPIRATION:
				id = R.string.settingsBindingArticleExpiration;
				break;
			case SettingsUI.KEY_SYNC_MODE:
				id = R.string.settingsBindingSyncMode;
				break;
			case SettingsUI.KEY_SYNC_FREQUENCY:
				id = R.string.settingsBindingSyncFrequency;
				break;
			case SettingsUI.KEY_SYNC_NETWORK:
				id = R.string.settingsBindingSyncNetwork;
				break;
			case SettingsUI.KEY_READER_SWIPE_DIRECTION:
				id = R.string.settingsBindingReaderSwipeDirection;
				break;
			case SettingsUI.KEY_UI_LANGUAGE:
				id = R.string.settingsBindingUiLanguage;
				break;
			case SettingsUI.KEY_PASSWORD_ATTEMPTS:
				id = R.string.settingsBindingNumberOfPasswordAttempts;
				break;
			case SettingsUI.KEY_USE_KILL_PASSPHRASE:
				id = R.string.settingsBindingUseKillPassphrase;
				break;
			case SettingsUI.KEY_PROXY_TYPE:
				id = R.string.settingsBindingProxyType;
				break;
			case SettingsUI.KEY_ENABLE_SCREENSHOTS:
				id = R.string.settingsBindingEnableScreenshots;
				break;
		}
		if (id != 0)
			return getString(id);
		return null;
	}

	public void screenshotsSectionClicked(View v) {
		mSettings.setEnableScreenshots(!mSettings.enableScreenshots());
	}

	public void setPassphraseClicked(View v) {
		if (mSettings.passphraseTimeout() != 0) {
			promptForNewPassphrase(mSettings.passphraseTimeout());
		}
	}

	public void setKillPassphraseClicked(View view) {
		promptForKillPassphrase(false);
	}

	@Override
	public void onCacheWordLocked() {
	}

	@Override
	public void onCacheWordOpened() {
	}

	@Override
	public void onCacheWordUninitialized() {
	}

	private String mLastSettingsKey;

	private boolean isValidType(Class<?> valueType) {
		if (valueType != null) {
			if (int.class == valueType) {
				return true;
			} else if (boolean.class == valueType) {
				return true;
			} else if (valueType.isEnum() && valueType.getDeclaringClass() == Settings.class) {
				return true;
			}
		}
		if (LOGGING)
			Log.e(LOGTAG, "Invalid type found: " + valueType);
		return false;
	}

	private Object getValue(String value, Class<?> valueType) {
		if (int.class == valueType) {
			return Integer.valueOf(value);
		} else if (boolean.class == valueType) {
			return Boolean.valueOf(value);
		} else if (valueType.isEnum()) {
			return Enum.valueOf((Class<Enum>) valueType, value);
		}
		return null;
	}

	@Override
	public void onViewCreated(View view, String name, Context context, AttributeSet attrs) {
		if (attrs != null) {
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SettingsControls);
			if (a.hasValue(R.styleable.SettingsControls_settings_key)) {
				mLastSettingsKey = a.getString(R.styleable.SettingsControls_settings_key);
				if (!TextUtils.isEmpty(mLastSettingsKey) && view != null && view instanceof CheckBox) {
					bindCheckbox((CheckBox) view, mLastSettingsKey);
				}
			}
			if (a.hasValue(R.styleable.SettingsControls_settings_value)) {
				String value = a.getString(R.styleable.SettingsControls_settings_value);
				if (!TextUtils.isEmpty(value)) {
					if (TextUtils.isEmpty(mLastSettingsKey)) {
						if (LOGGING)
							Log.d(LOGTAG, "No settings key defined for control with value " + value);
					} else if (view != null && view instanceof RadioButton) {
						bindRadioButton((RadioButton) view, mLastSettingsKey, value);
					}
					if (view != null && view.getId() == View.NO_ID) {
						view.setId(mDynamicId++);
					}
				}
			}
			a.recycle();
		}
	}

	private void bindRadioButton(RadioButton button, String key, String value) {
		try {
			String methodNameOfSetter = null;
			if (key.startsWith("get"))
				methodNameOfSetter = "set" + key.substring(3);
			else
				methodNameOfSetter = "set" + String.valueOf(key.charAt(0)).toUpperCase() + key.substring(1);
			final Method getter = mSettings.getClass().getMethod(key, (Class[]) null); // throws on failure
			Class<?> returnType = getter.getReturnType();
			if (returnType == null || !isValidType(returnType)) {
				if (LOGGING)
					Log.w(LOGTAG, "Invalid type for: " + key);
				return;
			}
			final Method setter = mSettings.getClass().getMethod(methodNameOfSetter, new Class<?>[]{returnType});
			Object valueObject = getValue(value, returnType);
			RadioButtonChangeListener listener = new RadioButtonChangeListener(button, key, valueObject, getter, setter);
			button.setOnCheckedChangeListener(listener);
		} catch (Exception e) {
			if (LOGGING)
				Log.v(LOGTAG, "Failed to hookup radio button with value " + value + " for key " + key + " :" + e.toString());
		}
	}

	private void bindCheckbox(CheckBox button, String key) {
		try {
			String methodNameOfSetter = null;
			if (key.startsWith("get"))
				methodNameOfSetter = "set" + key.substring(3);
			else
				methodNameOfSetter = "set" + String.valueOf(key.charAt(0)).toUpperCase() + key.substring(1);
			final Method getter = mSettings.getClass().getMethod(key, (Class[]) null); // throws on failure
			Class<?> returnType = getter.getReturnType();
			if (returnType == null || returnType != boolean.class) { // Checkboxes are booleans!
				if (LOGGING)
					Log.w(LOGTAG, "Invalid type for: " + key);
				return;
			}
			final Method setter = mSettings.getClass().getMethod(methodNameOfSetter, new Class<?>[]{returnType});
			CheckBoxChangeListener listener = new CheckBoxChangeListener(button, key, getter, setter);
			button.setOnCheckedChangeListener(listener);
		} catch (Exception e) {
			if (LOGGING)
				Log.v(LOGTAG, "Failed to hookup checkbox for key " + key + " :" + e.toString());
		}
	}

	private class RadioButtonChangeListener implements RadioButton.OnCheckedChangeListener {
		private final RadioButton mRadioButton;
		private final String mKey;
		private final Object mValue;
		private final Method mGetter;
		private final Method mSetter;

		RadioButtonChangeListener(RadioButton radioButton, String key, Object valueObject, Method getter, Method setter) {
			mRadioButton = radioButton;
			mKey = key;
			mValue = valueObject;
			mGetter = getter;
			mSetter = setter;
			mRadioButtonListeners.add(this);
			onSettingChanged(key); // Set initial value!
		}

		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			try {
				if (LOGGING)
					Log.d(LOGTAG, "onChecked change for button " + mValue.toString() + " : " + isChecked);
				if (isChecked) {
					Object currentValueInSettings = mGetter.invoke(mSettings, (Object[]) null);
					if (!currentValueInSettings.equals(mValue)) {

						// Special case, trying to set useKillPassphrase
						if (getString(R.string.settingsBindingPassphraseTimeout).equalsIgnoreCase(mKey)) {
							if (mSettings.passphraseTimeout() == 0 && (int) mValue != 0) {
								rootView.post(new Runnable() {
									@Override
									public void run() {
										refreshValues(Settings.KEY_PASSPHRASE_TIMEOUT);
										promptForNewPassphrase((int)mValue);
									}
								});
								return;
							} else if (mSettings.passphraseTimeout() != 0 && (int) mValue == 0) {
								rootView.post(new Runnable() {
									@Override
									public void run() {
										refreshValues(Settings.KEY_PASSPHRASE_TIMEOUT);
										setTimeoutToNever();
									}
								});
								return;
							}
						}

						// Special case, trying to set useKillPassphrase
						if (getString(R.string.settingsBindingUseKillPassphrase).equalsIgnoreCase(mKey)) {
							if (TextUtils.isEmpty(mSettings.killPassphrase())) {
								refreshValues(Settings.KEY_USE_KILL_PASSPHRASE);
								promptForKillPassphrase(true);
								return;
							}
						}
						mSetter.invoke(mSettings, mValue);

						// Special case, if we set proxy need to set requireProxy
						if (getString(R.string.settingsBindingProxyType).equalsIgnoreCase(mKey)) {
							mSettings.setRequireProxy(mValue != Settings.ProxyType.None);
						}
					}
				}
			} catch (Exception e) {
				if (LOGGING)
					Log.v(LOGTAG, "Failed checked change listener: " + e.toString());
			}
		}

		void onSettingChanged(String key) {
			if (mKey.equalsIgnoreCase(key)) {
				try {
					Object currentValueInSettings = mGetter.invoke(mSettings, (Object[]) null);
					if (currentValueInSettings.equals(mValue)) {
						if (LOGGING)
							Log.d(LOGTAG, "Checking button with value " + mValue.toString() + " because of settings change");
						mRadioButton.setChecked(true);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private class CheckBoxChangeListener implements CheckBox.OnCheckedChangeListener {
		private final CheckBox mCheckBox;
		private final String mKey;
		private final Method mGetter;
		private final Method mSetter;

		CheckBoxChangeListener(CheckBox checkBox, String key, Method getter, Method setter) {
			mCheckBox = checkBox;
			mKey = key;
			mGetter = getter;
			mSetter = setter;
			mCheckboxListeners.add(this);
			onSettingChanged(key);
		}

		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			try {
				boolean currentValueInSettings = (boolean) mGetter.invoke(mSettings, (Object[]) null);
				if (currentValueInSettings != isChecked)
					mSetter.invoke(mSettings, isChecked);
			} catch (Exception e) {
				if (LOGGING)
					Log.v(LOGTAG, "Failed checked change listener: " + e.toString());
			}
		}

		void onSettingChanged(String key) {
			if (mKey.equalsIgnoreCase(key)) {
				try {
					boolean currentValueInSettings = (boolean) mGetter.invoke(mSettings, (Object[]) null);
					if (currentValueInSettings != mCheckBox.isChecked()) {
						if (LOGGING)
							Log.d(LOGTAG, "Checking box with value " + currentValueInSettings + " because of settings change");
						mCheckBox.setChecked(currentValueInSettings);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
