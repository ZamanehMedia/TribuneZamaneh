
package com.tribunezamaneh.rss.views;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.tribunezamaneh.rss.App;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.impl.client.DefaultRedirectStrategy;
import cz.msebera.android.httpclient.impl.client.LaxRedirectStrategy;
import cz.msebera.android.httpclient.impl.client.RedirectLocations;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.protocol.BasicHttpContext;
import cz.msebera.android.httpclient.protocol.HttpContext;
import info.guardianproject.securereader.SocialReader;
import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.widgets.compat.Toast;

import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.message.BasicHeader;
import cz.msebera.android.httpclient.*;


public class WPSignInView extends FrameLayout {

	private EditText mEditUsername;
	private EditText mEditEmail;
	private EditText mEditPassword;
	private TextView mErrorView;
	private AsyncTask<String, Void, Integer> mPostTask;

	public interface OnLoginListener {
		void onLoggedIn(String username, String password);
	}

	private OnLoginListener mListener;

	public WPSignInView(Context context) {
		super(context);
		init(false);
	}

	public WPSignInView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(false);
	}

	private void init(boolean register) {
		removeAllViews();
		LayoutInflater inflater = LayoutInflater.from(getContext());
		inflater.inflate(register ? R.layout.wp_sign_in_register : R.layout.wp_sign_in, this);
		if (!this.isInEditMode()) {
			mEditUsername = (EditText) findViewById(R.id.editUsername);

			if (App.getInstance().socialReader.ssettings != null)
				mEditUsername.setText(App.getInstance().socialReader.ssettings.getXMLRPCUsername());

			mEditEmail = (EditText) findViewById(R.id.editEmail);
			mEditPassword = (EditText) findViewById(R.id.editPassword);
			mErrorView = (TextView) findViewById(R.id.signInError);
			mErrorView.setVisibility(View.GONE);
			Button mBtnLogin = (Button) findViewById(R.id.btnLogin);
			if (mBtnLogin != null) {
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
			Button mBtnRegister = (Button) findViewById(R.id.btnRegister);
			if (mBtnRegister != null) {
				mBtnRegister.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						if (mPostTask != null)
							mPostTask.cancel(true);
						mPostTask = new PostTaskRegister();
						mPostTask.execute(mEditUsername.getText().toString(), mEditEmail.getText().toString());
					}
				});
			}
			View btnSwitchToRegister = findViewById(R.id.btnSwitchToRegister);
			if (btnSwitchToRegister != null) {
				btnSwitchToRegister.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						init(true);
					}
				});
			}
			View btnSwitchToLogin = findViewById(R.id.btnSwitchToLogin);
			if (btnSwitchToLogin != null) {
				btnSwitchToLogin.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						init(false);
					}
				});
			}
			if (mEditUsername.getText().length() > 0)
				mEditPassword.requestFocus();
		}
	}

	public void setListener(OnLoginListener listener) {
		mListener = listener;
	}

	private class PostTask extends AsyncTask<String, Void, Integer> {

		private boolean loggedIn;
		private HttpClient httpClient;
		private ProgressDialog progressDialog;
		private String username;
		private String password;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			httpClient = App.getInstance().socialReader.getHttpClient();
			progressDialog = new ProgressDialog(getContext(),
					R.style.ModalDialogTheme);
			progressDialog.setIndeterminate(true);
			progressDialog.setMessage(getContext().getString(R.string.wp_sign_in_progress));
			progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialogInterface) {
					if (!PostTask.this.isCancelled()) {
						cancel(true);
					}
				}
			});
			progressDialog.show();
		}

		@Override
		protected Integer doInBackground(String... data) {
			try {
				username = data[0];
				password = data[1];

				HttpPost httpPost = new HttpPost(App.WORDPRESS_LOGIN_URL);
				httpPost.setHeader("User-Agent", SocialReader.USERAGENT);

				// Add your data
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
				nameValuePairs.add(new BasicNameValuePair("log", username));
				nameValuePairs.add(new BasicNameValuePair("pwd", password));
				nameValuePairs.add(new BasicNameValuePair("redirect_to", App.WORDPRESS_LOGIN_URL + "?loggedin=1"));
				nameValuePairs.add(new BasicNameValuePair("wp-submit", "Log In"));
				httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

				// Execute HTTP Post Request
				HttpContext context = new BasicHttpContext();
				HttpResponse response = httpClient.execute(httpPost, context);
				int code = response.getStatusLine().getStatusCode();
				if (code == 200) {
					RedirectLocations locations = (RedirectLocations) context.getAttribute(DefaultRedirectStrategy.REDIRECT_LOCATIONS);
					if (locations != null) {
						URI uri = locations.getAll().get(locations.getAll().size() - 1);
						if (uri != null && uri.getQuery().contains("loggedin=1")) {
							loggedIn = true;
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
			} else if (integer == 200) {
				mErrorView.setText(R.string.wp_sign_in_error_credentials);
				mErrorView.setVisibility(View.VISIBLE);
				mEditPassword.setText("");
			} else {
				mErrorView.setText(R.string.wp_sign_in_error_other);
				mErrorView.setVisibility(View.VISIBLE);
				mEditPassword.setText("");
			}
		}
	}

	private class PostTaskRegister extends AsyncTask<String, Void, Integer> {

		private boolean registered;
		private HttpClient httpClient;
		private ProgressDialog progressDialog;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			httpClient = App.getInstance().socialReader.getHttpClient();
			progressDialog = new ProgressDialog(getContext(),
					R.style.ModalDialogTheme);
			progressDialog.setIndeterminate(true);
			progressDialog.setMessage(getContext().getString(R.string.wp_sign_in_register_progress));
			progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialogInterface) {
					if (!PostTaskRegister.this.isCancelled()) {
						cancel(true);
					}
				}
			});
			progressDialog.show();
		}

		@Override
		protected Integer doInBackground(String... data) {
			try {
				String username = data[0];
				String email = data[1];

				SocialReader socialReader = App.getInstance().socialReader;

			//	httpClient.setRedirectStrategy(new LaxRedirectStrategy());

				HttpPost httpPost = new HttpPost(App.WORDPRESS_LOGIN_URL + "?action=register");
				httpPost.setHeader("User-Agent", SocialReader.USERAGENT);

				// Add your data
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
				nameValuePairs.add(new BasicNameValuePair("user_login", username));
				nameValuePairs.add(new BasicNameValuePair("user_email", email));
				nameValuePairs.add(new BasicNameValuePair("redirect_to", App.WORDPRESS_LOGIN_URL + "?registered=1"));
				nameValuePairs.add(new BasicNameValuePair("wp-submit", "Register"));
				httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

				// Execute HTTP Post Request
				HttpContext context = new BasicHttpContext();
				HttpResponse response = httpClient.execute(httpPost, context);
				int code = response.getStatusLine().getStatusCode();
				if (code == 200) {
					RedirectLocations locations = (RedirectLocations) context.getAttribute(DefaultRedirectStrategy.REDIRECT_LOCATIONS);
					if (locations != null) {
						URI uri = locations.getAll().get(locations.getAll().size() - 1);
						if (uri != null && uri.getQuery().contains("registered=1")) {
							registered = true;
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
			progressDialog.dismiss();
		}

		@Override
		protected void onPostExecute(Integer integer) {
			super.onPostExecute(integer);
			progressDialog.dismiss();
			if (integer == 200 && registered) {
				Toast.makeText(getContext(), R.string.wp_sign_in_register_done, Toast.LENGTH_LONG).show();
				init(false);
			} else if (integer == 200) {
				mErrorView.setText(R.string.wp_sign_in_error_register);
				mErrorView.setVisibility(View.VISIBLE);
			} else {
				mErrorView.setText(R.string.wp_sign_in_error_other);
				mErrorView.setVisibility(View.VISIBLE);
			}
		}
	}
}
