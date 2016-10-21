
package com.tribunezamaneh.rss.views;

import android.annotation.TargetApi;
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
import java.util.List;
import java.util.Map;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.NameValuePair;
import ch.boye.httpclientandroidlib.client.HttpClient;
import ch.boye.httpclientandroidlib.client.entity.UrlEncodedFormEntity;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.message.BasicNameValuePair;
import ch.boye.httpclientandroidlib.util.EntityUtils;
import info.guardianproject.netcipher.client.StrongHttpsClient;
import info.guardianproject.securereaderinterface.R;

public class WPSignInView extends FrameLayout {
	private Button mBtnLogin;
	private EditText mEditUsername;
	private EditText mEditPassword;
	private PostTask mPostTask;

	public interface OnLoginListener {
		void onLoggedIn();
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

		@Override
		protected Integer doInBackground(String... data) {
			try {
				URL url = new URL("https://www.postmodernapps.net/home/wp-login.php");
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setReadTimeout(10000);
				conn.setConnectTimeout(15000);
				conn.setRequestMethod("POST");
				conn.setDoOutput(true);
				OutputStream os = conn.getOutputStream();
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
				StringBuilder sb = new StringBuilder();
				sb.append(URLEncoder.encode("log", "UTF-8"));
				sb.append("=");
				sb.append(URLEncoder.encode(data[0], "UTF-8"));
				sb.append("&");
				sb.append(URLEncoder.encode("pwd", "UTF-8"));
				sb.append("=");
				sb.append(URLEncoder.encode(data[1], "UTF-8"));
				sb.append("&");
				sb.append(URLEncoder.encode("redirect_to", "UTF-8"));
				sb.append("=");
				sb.append(URLEncoder.encode("https://www.postmodernapps.net/home/wp-admin/", "UTF-8"));
				sb.append("&");
				sb.append(URLEncoder.encode("wp-submit", "UTF-8"));
				sb.append("=");
				sb.append(URLEncoder.encode("Log In", "UTF-8"));
				writer.write(sb.toString());
				writer.flush();
				writer.close();
				os.close();
				conn.connect();
				int code = conn.getResponseCode();
				if (code == 200) {
					Map<String, List<String>> headers = conn.getHeaderFields();
					if (headers != null && headers.containsKey("Set-Cookie")) {
						List<String> values = headers.get("Set-Cookie");
						for (String value : values) {
							if (value.startsWith("wordpress_logged_in")) {
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
		protected void onPostExecute(Integer integer) {
			super.onPostExecute(integer);
			if (integer == 200 && loggedIn) {
				if (mListener != null) {
					mListener.onLoggedIn();
				}
			} else {
				mEditPassword.setText("");
			}
		}
	}
}
