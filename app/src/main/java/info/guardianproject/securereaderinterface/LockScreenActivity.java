package info.guardianproject.securereaderinterface;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.LayoutInflaterCompat;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.security.GeneralSecurityException;
import java.util.UUID;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import info.guardianproject.securereader.Settings.ProxyType;
import info.guardianproject.securereader.SocialReader;
import info.guardianproject.securereaderinterface.models.LockScreenCallbacks;
import info.guardianproject.securereaderinterface.ui.LayoutFactoryWrapper;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;

public class LockScreenActivity extends Activity implements LockScreenCallbacks, OnFocusChangeListener, ICacheWordSubscriber, OnboardingFragmentListener
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

	private View mRootView;
	private int mRootViewId;
	private LayoutInflater mInflater;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// If not auto-login, make us non-transparent
		if (App.getSettings().passphraseTimeout() != 0) {
			getWindow().setBackgroundDrawableResource(R.drawable.background_news);
			setTheme(R.style.AppTheme);
		}
		super.onCreate(savedInstanceState);
		LayoutInflater inflater = LayoutInflater.from(this);
		mInflater = inflater.cloneInContext(this);
		LayoutInflaterCompat.setFactory(mInflater, new LayoutFactoryWrapper(inflater.getFactory()));
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		mCacheWord = new CacheWordHandler(this);

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
		if (layoutResID != mRootViewId) {
			mRootViewId = layoutResID;
			mRootView = LayoutInflater.from(this).inflate(layoutResID, null);
			super.setContentView(mRootView);
		}
	}

	private Bitmap takeSnapshot(View view)
	{
		if (view == null || view.getWidth() == 0 || view.getHeight() == 0)
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
					App.getInstance().wipe(LockScreenActivity.this, SocialReader.DATA_WIPE, false);
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
                        App.getInstance().wipe(LockScreenActivity.this, SocialReader.DATA_WIPE, false);
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
		if (App.getSettings().passphraseTimeout() == 0) {
			// No lock, use our stored PW
			try {
				mCacheWord.setPassphrase(getCWPassword().toCharArray());
			} catch (GeneralSecurityException e) {
				e.printStackTrace();
			}
		} else {
			createLockView();
		}
    }

    @Override
    public void onCacheWordOpened() {
        App.getSettings().setCurrentNumberOfPasswordAttempts(0);

        Intent intent = (Intent) getIntent().getParcelableExtra("originalIntent");
        if (intent == null)
        	intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        //Bitmap snap = takeSnapshot(mRootView);
        //App.getInstance().putTransitionBitmap(snap);

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

      //Bitmap snap = takeSnapshot(mRootView);
      //App.getInstance().putTransitionBitmap(snap);

      startActivity(intent);
      finish();
      LockScreenActivity.this.overridePendingTransition(0, 0);
	}
	
	private void showNextOnboardingView()
	{
		TypedArray onbarding_screens = getResources().obtainTypedArray(R.array.onboarding_screems);
		if (App.getSettings().getOnboardingStage() < onbarding_screens.length()) {
			int layoutId = onbarding_screens.getResourceId(App.getSettings().getOnboardingStage(), 0);
			setContentView(layoutId);
		} else {
			if (!BuildConfig.UI_ENABLE_CREATE_PASSPHRASE) {
				if (mRootView != null)
					mRootView.setAlpha(0.5f); // Fade it out a bit. TODO - show progress spinner?
				App.getSettings().setPassphraseTimeout(0);	//TODO - add a setting for this!
				try {
					mCacheWord.setPassphrase(getCWPassword().toCharArray());
				} catch (GeneralSecurityException e) {
					e.printStackTrace();
				}
			} else {
				createCreatePassphraseView();
			}
		}
	}

	@Override
	public void onNextPressed() {
		App.getSettings().setOnboardingStage(App.getSettings().getOnboardingStage() + 1);
		showNextOnboardingView();
	}

	static String getCWPassword() {
		String passphrase = App.getSettings().launchPassphrase();
		if (TextUtils.isEmpty(passphrase)) {
			passphrase = UUID.randomUUID().toString();
			App.getSettings().setLaunchPassphrase(passphrase);
		}
		return passphrase;
	}
}
