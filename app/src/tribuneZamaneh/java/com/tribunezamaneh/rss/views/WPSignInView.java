
package com.tribunezamaneh.rss.views;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.HttpAuthHandler;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.tinymission.rss.Feed;
import com.tribunezamaneh.rss.App;

import org.xml.sax.InputSource;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.boye.httpclientandroidlib.Header;
import ch.boye.httpclientandroidlib.HttpRequest;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.NameValuePair;
import ch.boye.httpclientandroidlib.ProtocolException;
import ch.boye.httpclientandroidlib.client.HttpClient;
import ch.boye.httpclientandroidlib.client.RedirectStrategy;
import ch.boye.httpclientandroidlib.client.entity.UrlEncodedFormEntity;
import ch.boye.httpclientandroidlib.client.methods.HttpGet;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.client.methods.HttpUriRequest;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.impl.client.LaxRedirectStrategy;
import ch.boye.httpclientandroidlib.message.BasicNameValuePair;
import ch.boye.httpclientandroidlib.protocol.HttpContext;
import ch.boye.httpclientandroidlib.util.EntityUtils;
import info.guardianproject.netcipher.client.StrongHttpsClient;
import info.guardianproject.securereader.SocialReader;
import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;

public class WPSignInView extends FrameLayout {

	private static final String LOGIN_URL = "https://www.postmodernapps.net/home/wp-login.php";
	private static final String REDIRECT_URL = "https://www.postmodernapps.net/home/wp-admin/";

	private Button mBtnLogin;
	private EditText mEditUsername;
	private EditText mEditPassword;
	private View mErrorView;
	private PostTask mPostTask;

	public interface OnLoginListener {
		void onLoggedIn(String username, String password);
	}

	private OnLoginListener mListener;

	public WPSignInView(Context context) {
		super(context);
		init();
	}

	public WPSignInView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		LayoutInflater inflater = LayoutInflater.from(getContext());
		inflater.inflate(R.layout.wp_sign_in, this);
		if (!this.isInEditMode()) {
			mBtnLogin = (Button) findViewById(R.id.btnLogin);
			mEditUsername = (EditText) findViewById(R.id.editUsername);
			mEditPassword = (EditText) findViewById(R.id.editPassword);
			mErrorView = findViewById(R.id.signInError);
			mErrorView.setVisibility(View.GONE);
			mBtnLogin.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (mPostTask != null)
						mPostTask.cancel(true);
					mPostTask = new PostTask();
					mPostTask.execute(mEditUsername.getText().toString(), mEditPassword.getText().toString());
				}
			});
		}
	}

	public void setListener(OnLoginListener listener) {
		mListener = listener;
	}

	private class PostTask extends AsyncTask<String, Void, Integer> {

		private boolean loggedIn;
		private StrongHttpsClient httpClient;
		private ProgressDialog progressDialog;
		private String username;
		private String password;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			httpClient = new StrongHttpsClient(App.getInstance().socialReader.applicationContext);
			progressDialog = new ProgressDialog(getContext(),
					R.style.ModalDialogTheme);
			progressDialog.setIndeterminate(true);
			progressDialog.setMessage(getContext().getString(R.string.wp_sign_in_progress));
			progressDialog.show();
		}

		@Override
		protected Integer doInBackground(String... data) {
			try {
				username = data[0];
				password = data[1];

				SocialReader socialReader = App.getInstance().socialReader;
				if (socialReader.relaxedHTTPS) {
					httpClient.enableSSLCompatibilityMode();
				}
				if (socialReader.useProxy())
				{
					httpClient.useProxy(true, socialReader.getProxyType(), socialReader.getProxyHost(), socialReader.getProxyPort());
				}
				httpClient.setRedirectStrategy(new LaxRedirectStrategy());

				HttpPost httpPost = new HttpPost(LOGIN_URL);
				httpPost.setHeader("User-Agent", SocialReader.USERAGENT);

				// Add your data
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
				nameValuePairs.add(new BasicNameValuePair("log", username));
				nameValuePairs.add(new BasicNameValuePair("pwd", password));
				nameValuePairs.add(new BasicNameValuePair("redirect_to", REDIRECT_URL));
				nameValuePairs.add(new BasicNameValuePair("wp-submit", "Log In"));
				httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

				// Execute HTTP Post Request
				HttpResponse response = httpClient.execute(httpPost);
				int code = response.getStatusLine().getStatusCode();
				if (code == 200) {
					Header[] headers = response.getHeaders("Set-Cookie");
					if (headers != null) {
						for (Header header : headers) {
							if (header.getValue().startsWith("wordpress_logged_in")) {
								loggedIn = true;
							}
						}
					}
				}
				return code;
			} catch (IOException e) {
				e.printStackTrace();
				return 501;
			}
		}

		@Override
		protected void onCancelled(Integer integer) {
			super.onCancelled(integer);
			progressDialog.dismiss();
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			progressDialog.dismiss();
		}

		@Override
		protected void onPostExecute(Integer integer) {
			super.onPostExecute(integer);
			progressDialog.dismiss();
			if (integer == 200 && loggedIn) {
				mErrorView.setVisibility(View.GONE);
				if (mListener != null) {
					mListener.onLoggedIn(username, password);
				}
			} else {
				mErrorView.setVisibility(View.VISIBLE);
				mEditPassword.setText("");
			}
		}
	}
}
