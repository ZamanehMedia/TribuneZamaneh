package info.guardianproject.securereaderinterface;

import info.guardianproject.securereader.Settings.ProxyType;
import info.guardianproject.securereader.Settings.UiLanguage;
import info.guardianproject.securereaderinterface.adapters.FeedListAdapterCurate;
import info.guardianproject.securereaderinterface.models.LockScreenCallbacks;
import info.guardianproject.securereaderinterface.ui.LayoutFactoryWrapper;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;
import info.guardianproject.securereader.SocialReader;
import info.guardianproject.securereaderinterface.R;
import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGImageView;
import com.caverock.androidsvg.SVGParseException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.LayoutInflaterCompat;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class LockScreenActivity extends Activity implements LockScreenCallbacks, OnFocusChangeListener, ICacheWordSubscriber
{
    private static final String LOGTAG = "LockScreenActivity";
	public static final boolean LOGGING = false;
	
	private EditText mEnterPassphrase;
	private EditText mNewPassphrase;
	private EditText mConfirmNewPassphrase;
	private Button mBtnOpen;
	private View mErrorView;
	
	private CacheWordHandler mCacheWord;
	private info.guardianproject.securereaderinterface.LockScreenActivity.SetUiLanguageReceiver mSetUiLanguageReceiver;
	private ProxyType mWaitingForProxyConnection;
	
	private View mRootView;
	private LayoutInflater mInflater;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		LayoutInflater inflater = LayoutInflater.from(this);
		mInflater = inflater.cloneInContext(this);
		LayoutInflaterCompat.setFactory(mInflater, new LayoutFactoryWrapper(inflater.getFactory()));
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		mCacheWord = new CacheWordHandler(this);
		mWaitingForProxyConnection = ProxyType.None;
		
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB)
		{
			 getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
			 WindowManager.LayoutParams.FLAG_SECURE);
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		mSetUiLanguageReceiver = new SetUiLanguageReceiver();
		LocalBroadcastManager.getInstance(this).registerReceiver(mSetUiLanguageReceiver, new IntentFilter(App.SET_UI_LANGUAGE_BROADCAST_ACTION));
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus && mWaitingForProxyConnection != ProxyType.None)
		{
			mRootView.removeCallbacks(mCheckProxyRunnable);
			ProxyType proxyType = mWaitingForProxyConnection;
			mWaitingForProxyConnection = ProxyType.None;
			boolean online = moveToNextIfProxyOnline(proxyType);
			if (!online)
			{
				waitForProxyConnection();
			}
		}
	}

	@Override
	protected void onPause() {
	    super.onPause();
	    if (mSetUiLanguageReceiver != null)
	    {
	    	LocalBroadcastManager.getInstance(this).unregisterReceiver(mSetUiLanguageReceiver);
	    	mSetUiLanguageReceiver = null;
	    }
	}
	
	@Override
	protected void onStart()
	{
		super.onStart();
		App.getInstance().onLockScreenResumed(this);
        mCacheWord.connectToService();
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		App.getInstance().onLockScreenPaused(this);
        mCacheWord.disconnectFromService();
	}

	@Override
	public boolean isInternalActivityOpened()
	{
		return false;
	}


	

	@Override
	public void setContentView(int layoutResID) 
	{
		mRootView = LayoutInflater.from(this).inflate(layoutResID, null);
		super.setContentView(mRootView);
	}

	private Bitmap takeSnapshot(View view)
	{
		if (view.getWidth() == 0 || view.getHeight() == 0)
			return null;

		view.setDrawingCacheEnabled(true);
		Bitmap bmp = view.getDrawingCache();
		Bitmap bitmap = Bitmap.createBitmap(bmp, 0, 0, view.getWidth(), view.getHeight()).copy(bmp.getConfig(), false);
		view.setDrawingCacheEnabled(false);
		return bitmap;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if ((keyCode == KeyEvent.KEYCODE_BACK))
		{
			// Back from lock screen means quit app. So send a kill signal to
			// any open activity and finish!	
			LocalBroadcastManager.getInstance(this).sendBroadcastSync(new Intent(App.EXIT_BROADCAST_ACTION));
			finish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	private void createCreatePassphraseView()
	{
		setContentView(R.layout.lock_screen_create_passphrase);

		mNewPassphrase = (EditText) findViewById(R.id.editNewPassphrase);
		mConfirmNewPassphrase = (EditText) findViewById(R.id.editConfirmNewPassphrase);

		// Passphrase is not set, so allow the user to create one!
		//
		Button btnCreate = (Button) findViewById(R.id.btnCreate);
		btnCreate.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// Disallow empty fields (user just pressing "create")
				if (mNewPassphrase.getText().length() == 0 && mConfirmNewPassphrase.getText().length() == 0)
					return;

				// Compare the two text fields!
				if (!mNewPassphrase.getText().toString().equals(mConfirmNewPassphrase.getText().toString()))
				{
					Toast.makeText(LockScreenActivity.this, getString(R.string.lock_screen_passphrases_not_matching), Toast.LENGTH_SHORT).show();
					mNewPassphrase.setText("");
					mConfirmNewPassphrase.setText("");
					mNewPassphrase.requestFocus();
					return; // Try again...
				}

				// Store
				try {
                    mCacheWord.setPassphrase(mNewPassphrase.getText().toString().toCharArray());
                } catch (GeneralSecurityException e) {
                	if (LOGGING)
                		Log.e(LOGTAG, "Cacheword initialization failed: " + e.getMessage());
                }
			}
		});
	}

	private void createLockView()
	{
		setContentView(R.layout.lock_screen_return);

		View root = findViewById(R.id.llRoot);
		root.setOnFocusChangeListener(this);

		mErrorView = root.findViewById(R.id.tvError);
		mErrorView.setVisibility(View.GONE);
		
		mEnterPassphrase = (EditText) findViewById(R.id.editEnterPassphrase);
		mEnterPassphrase.setTypeface(Typeface.DEFAULT);
		mEnterPassphrase.setTransformationMethod(new PasswordTransformationMethod());

		mBtnOpen = (Button) findViewById(R.id.btnOpen);
		mBtnOpen.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (TextUtils.isEmpty(mEnterPassphrase.getText()))
					return;
				
				if (App.getSettings().useKillPassphrase() && mEnterPassphrase.getText().toString().equals(App.getSettings().killPassphrase()))
				{
					// Kill password entered, wipe!
					App.getInstance().wipe(LockScreenActivity.this, SocialReader.DATA_WIPE);
					mEnterPassphrase.setText("");
					mErrorView.setVisibility(View.VISIBLE);
        			LocalBroadcastManager.getInstance(LockScreenActivity.this).sendBroadcastSync(new Intent(App.EXIT_BROADCAST_ACTION));
					finish();
					return; // Try again...
				}

				// Check passphrase
			    try {
                    mCacheWord.setPassphrase(mEnterPassphrase.getText().toString().toCharArray());
                } catch (GeneralSecurityException e) {
                	if (LOGGING)
                		Log.e(LOGTAG, "Cacheword pass verification failed: " + e.getMessage());
                    int failedAttempts = App.getSettings().currentNumberOfPasswordAttempts();
                    failedAttempts++;
                    App.getSettings().setCurrentNumberOfPasswordAttempts(failedAttempts);
                    if (failedAttempts == App.getSettings().numberOfPasswordAttempts())
                    {
                        // Ooops, to many attempts! Wipe the data...
                        App.getInstance().wipe(LockScreenActivity.this, SocialReader.DATA_WIPE);
            			LocalBroadcastManager.getInstance(LockScreenActivity.this).sendBroadcastSync(new Intent(App.EXIT_BROADCAST_ACTION));
                        finish();
                    }

                    mEnterPassphrase.setText("");
					mErrorView.setVisibility(View.VISIBLE);
                    return; // Try again...
                }
                
				App.getSettings().setCurrentNumberOfPasswordAttempts(0);
				UIHelpers.hideSoftKeyboard(LockScreenActivity.this);

			}
		});

		mEnterPassphrase.setOnEditorActionListener(new OnEditorActionListener()
		{
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
			{
				if (actionId == EditorInfo.IME_NULL || actionId == EditorInfo.IME_ACTION_GO)
				{
					InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

					Handler threadHandler = new Handler();

					imm.hideSoftInputFromWindow(v.getWindowToken(), 0, new ResultReceiver(threadHandler)
					{
						@Override
						protected void onReceiveResult(int resultCode, Bundle resultData)
						{
							super.onReceiveResult(resultCode, resultData);
							mBtnOpen.performClick();
						}
					});
					return true;
				}
				return false;
			}
		});
	}

	@Override
	public void onFocusChange(View v, boolean hasFocus)
	{
		if (hasFocus && !(v instanceof EditText))
		{
			LockScreenActivity.hideSoftKeyboard(this);
		}
	}

	public static void hideSoftKeyboard(Activity activity)
	{
		InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
		inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
	}

	@SuppressLint("NewApi")
	protected void onUiLanguageChanged()
	{
		Intent intentThis = getIntent();
		intentThis.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		finish();
		overridePendingTransition(0, 0);
		startActivity(intentThis);
		overridePendingTransition(0, 0);
	}

	@Override
    public void onCacheWordUninitialized() {
	    showNextOnboardingView();
    }

    @Override
    public void onCacheWordLocked() {
        createLockView();
    }

    @Override
    public void onCacheWordOpened() {
        App.getSettings().setCurrentNumberOfPasswordAttempts(0);

        Intent intent = (Intent) getIntent().getParcelableExtra("originalIntent");
        if (intent == null)
        	intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        Bitmap snap = takeSnapshot(mRootView);
        App.getInstance().putTransitionBitmap(snap);

        startActivity(intent);
        finish();
        LockScreenActivity.this.overridePendingTransition(0, 0);
    }
      
	@Override
	public Object getSystemService(String name)
	{
		if (LAYOUT_INFLATER_SERVICE.equals(name))
		{
			if (mInflater != null)
				return mInflater;
		}
		return super.getSystemService(name);
	}
	
	private final class SetUiLanguageReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			new Handler().post(new Runnable()
			{

				@Override
				public void run()
				{
					onUiLanguageChanged();
				}
			});
		}
	}
	
	public void onUnlocked()
	{
      App.getSettings().setCurrentNumberOfPasswordAttempts(0);

      Intent intent = (Intent) getIntent().getParcelableExtra("originalIntent");
      if (intent == null)
      	intent = new Intent(this, MainActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

      Bitmap snap = takeSnapshot(mRootView);
      App.getInstance().putTransitionBitmap(snap);

      startActivity(intent);
      finish();
      LockScreenActivity.this.overridePendingTransition(0, 0);
	}
	
	private boolean moveToNextIfProxyOnline(ProxyType type)
	{
		if (App.getSettings().proxyType() == type && App.getInstance().socialReader.isProxyOnline())
		{
			if (type == ProxyType.Tor)
				Toast.makeText(this, R.string.lock_screen_proxy_tor_connected, Toast.LENGTH_LONG).show();
			else if (type == ProxyType.Psiphon)
				Toast.makeText(this, R.string.lock_screen_proxy_psiphon_connected, Toast.LENGTH_LONG).show();
			App.getSettings().setFirstUse(SettingsUI.KEY_FIRST_USE_HAS_SELECTED_PROXY, true);
			showNextOnboardingView();
			return true;
		}
		return false;
	}
	
	private void showNextOnboardingView()
	{
		if (!App.getSettings().getFirstUse(SettingsUI.KEY_FIRST_USE_HAS_SEEN_INTRO))
			createOnboarding1View();
		else if (!App.getSettings().getFirstUse(SettingsUI.KEY_FIRST_USE_HAS_SELECTED_PROXY))
			createOnboarding2View();
		else if (!App.getSettings().getFirstUse(SettingsUI.KEY_FIRST_USE_HAS_CURATED_FEEDS))
			createOnboarding3View();
		else
			createCreatePassphraseView();
	}
	
	
	private void populateContainerWithSVG(int idSVG, int idContainer) {
		try {
			SVG svg = SVG.getFromResource(this, idSVG);

			SVGImageView svgImageView = new SVGImageView(this);
			svgImageView.setSVG(svg);
			svgImageView.setLayoutParams(new ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT));
			ViewGroup layout = (ViewGroup) findViewById(idContainer);
			layout.addView(svgImageView);
		} catch (SVGParseException e) {
			e.printStackTrace();
		}
	}
	
	private void createOnboarding1View()
	{
		setContentView(R.layout.lock_screen_onboard_1);
		
		View btnLanguage = mRootView.findViewById(R.id.btnLanguage);
		btnLanguage.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showLanguagesPopup();
			}
		});
				
		this.populateContainerWithSVG(R.raw.onboard_01, R.id.ivIllustration);
	
		View btnNext = mRootView.findViewById(R.id.btnNext);
		btnNext.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				App.getSettings().setFirstUse(SettingsUI.KEY_FIRST_USE_HAS_SEEN_INTRO, true);
				showNextOnboardingView();
			}
		});
	}

	private void createOnboarding2View()
	{
		setContentView(R.layout.lock_screen_onboard_2);
		this.populateContainerWithSVG(R.raw.onboard_psiphon, R.id.ivIllustrationPsiphon);
		this.populateContainerWithSVG(R.raw.onboard_tor, R.id.ivIllustrationTor);

		View btnNotNow = mRootView.findViewById(R.id.btnNotNow);
		btnNotNow.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				App.getSettings().setFirstUse(SettingsUI.KEY_FIRST_USE_HAS_SELECTED_PROXY, true);
				App.getSettings().setProxyType(ProxyType.None);
				App.getSettings().setRequireProxy(false);
				showNextOnboardingView();
			}
		});
		
		View btnConnectTor = mRootView.findViewById(R.id.btnConnectTor);
		btnConnectTor.setOnClickListener(new ProxyConnectClickListener(ProxyType.Tor));
		
		View btnConnectPsiphon = mRootView.findViewById(R.id.btnConnectPsiphon);
		btnConnectPsiphon.setOnClickListener(new ProxyConnectClickListener(ProxyType.Psiphon));
	}
	
	private void createOnboarding3View()
	{
		setContentView(R.layout.lock_screen_onboard_3);
		
		final FeedListAdapterCurate adapter = new FeedListAdapterCurate(this);
		
		View btnNext = mRootView.findViewById(R.id.btnNext);
		btnNext.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				App.getSettings().setFirstUse(SettingsUI.KEY_FIRST_USE_HAS_CURATED_FEEDS, true);
				
				String processedXML = adapter.getProcessedXML();
				if (processedXML != null)
					App.getInstance().setOverrideResources(new OPMLOverrideResources(processedXML));
				showNextOnboardingView();
			}
		});
		
		ListView lv = (ListView) mRootView.findViewById(R.id.lvFeeds);
		lv.setAdapter(adapter);
	}
	
	private void showLanguagesPopup()
	{
		try
		{
			View root = findViewById(R.id.llRoot);
			if (root == null)
				return;
			View anchor = root.findViewById(R.id.btnLanguage);
			if (anchor == null)
				return;

			try
			{
				LanguageItemModel[] rgLanguages = new LanguageItemModel[] { 
						new LanguageItemModel(getString(R.string.settings_language_english_nt), UiLanguage.English),
						new LanguageItemModel(getString(R.string.settings_language_farsi_nt), UiLanguage.Farsi)
				};
				final ArrayAdapter<LanguageItemModel> adapter = new ArrayAdapter<LanguageItemModel>(this, R.layout.language_picker_popup_item, R.id.tvItem,
						rgLanguages);

				ListView lv = new ListView(root.getContext());
				lv.setBackgroundResource(R.drawable.panel_bg_holo_light);
				lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
				lv.setAdapter(adapter);
				// lv.setDivider(mDivider);

				Rect rectGlobal = new Rect();
				anchor.getGlobalVisibleRect(rectGlobal);

				Rect rectGlobalParent = new Rect();
				root.getGlobalVisibleRect(rectGlobalParent);

				int maxHeight = rectGlobalParent.bottom - rectGlobal.top;
				lv.measure(MeasureSpec.makeMeasureSpec(Math.min(root.getWidth(), UIHelpers.dpToPx(200, root.getContext())), MeasureSpec.EXACTLY),
						MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST));

				final PopupWindow mPopup = new PopupWindow(lv, lv.getMeasuredWidth(), lv.getMeasuredHeight(), true);

				lv.setOnItemClickListener(new AdapterView.OnItemClickListener()
				{
					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, long id)
					{
						LanguageItemModel selectedLang = (LanguageItemModel) parent.getItemAtPosition(position);
						App.getSettings().setUiLanguage(selectedLang.getCode());
						mPopup.dismiss();
					}
				});

				mPopup.setOutsideTouchable(true);
				mPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
				mPopup.showAtLocation(anchor, Gravity.TOP | Gravity.LEFT, rectGlobal.left, rectGlobal.top);
				mPopup.setOnDismissListener(new PopupWindow.OnDismissListener()
				{
					@Override
					public void onDismiss()
					{
						// mPopup = null;
					}
				});

			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private class LanguageItemModel
	{
		private final String mName;
		private final UiLanguage mCode;

		public LanguageItemModel(String name, UiLanguage code)
		{
			mName = name;
			mCode = code;
		}

		public String getName()
		{
			return mName;
		}

		public UiLanguage getCode()
		{
			return mCode;
		}

		@Override
		public String toString()
		{
			return getName();
		}
	}
	
	public class OPMLOverrideResources extends Resources
	{
		private String mXML;
		
		public OPMLOverrideResources(String xml) {
			super(App.getInstance().getResources().getAssets(), App.getInstance().getResources().getDisplayMetrics(), App.getInstance().getResources().getConfiguration());
			mXML = xml;
		}
		
		@Override
		public InputStream openRawResource(int id) throws NotFoundException {
			if (id == R.raw.bigbuffalo_opml) {
				return new ByteArrayInputStream(mXML.getBytes(Charset.forName("UTF-8")));
			}
			return super.openRawResource(id);
		}
	}
	
	private class ProxyConnectClickListener implements View.OnClickListener
	{
		private ProxyType mProxyType;
		
		public ProxyConnectClickListener(ProxyType proxyType)
		{
			mProxyType = proxyType;
		}
	
		@Override
		public void onClick(View v)
		{
			tryToConnect();
		}

		private void tryToConnect()
		{
			// Have net?
			int onlineMode = App.getInstance().socialReader.isOnline();
			if (onlineMode == SocialReader.NOT_ONLINE_NO_WIFI || onlineMode == SocialReader.NOT_ONLINE_NO_WIFI_OR_NETWORK)
			{
				showNoNetToast();
			}
			else
			{
				mWaitingForProxyConnection = mProxyType;
				App.getSettings().setProxyType(mProxyType);
				App.getInstance().socialReader.connectProxy(LockScreenActivity.this);
				moveToNextIfProxyOnline(mProxyType);
			}
		}

		private void showNoNetToast()
		{
			try
			{
				View root = findViewById(R.id.llRoot);
				if (root == null)
					return;

				LayoutInflater inflater = getLayoutInflater();
				final PopupWindow mMenuPopup = new PopupWindow(inflater.inflate(R.layout.lock_screen_proxy_select_no_net, (ViewGroup) root, false), root
						.getWidth(), root
						.getHeight(), true);
				View viewRetry = mMenuPopup.getContentView().findViewById(R.id.btnRetry);
				viewRetry.setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						mMenuPopup.dismiss();
						tryToConnect();
					}
				});
				mMenuPopup.setOutsideTouchable(true);
				mMenuPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
				mMenuPopup.showAtLocation(root, Gravity.CENTER, 0, 0);
				mMenuPopup.getContentView().setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						mMenuPopup.dismiss();
					}
				});
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	// POLL for proxy connection
	private void waitForProxyConnection()
	{
		mWaitForProxyTries = 0;
		App.getInstance().socialReader.checkPsiphonStatus();
		mRootView.postDelayed(mCheckProxyRunnable, 1000);
	}
	
	private int mWaitForProxyTries = 0;
	private Runnable mCheckProxyRunnable = new Runnable()
	{
		@Override
		public void run() {
			boolean online = LockScreenActivity.this.moveToNextIfProxyOnline(App.getSettings().proxyType());
			mWaitForProxyTries++;
			if (!online && mWaitForProxyTries < 10)
				mRootView.postDelayed(mCheckProxyRunnable, 1000);
		}
	};
}
