package com.tribunezamaneh.rss.onboarding;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.HelpActivity;
import info.guardianproject.securereaderinterface.PanicActivity;
import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.adapters.FeedListAdapterCurate;
import info.guardianproject.securereaderinterface.onboarding.OnboardingFragment;

public class OnboardingHelpFragment extends OnboardingFragment {

    public OnboardingHelpFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_help, container, true);

        Button btnTestPanic = (Button) rootView.findViewById(R.id.btnTestPanic);
        btnTestPanic.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(getActivity(), PanicActivity.class);
                intent.putExtra("testing", true);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        });

        View btnDone = rootView.findViewById(R.id.btnDone);
        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!tryGetStoragePermissions()) {
                    // Asking, do nothing now...
                } else {
                    closeThis();
                }
            }
        });
        return rootView;
    }

    private void closeThis() {
        if (getListener() != null)
            getListener().onNextPressed();
    }

    private boolean tryGetStoragePermissions() {
        if (Build.VERSION.SDK_INT <= 18) {
            return true;
        }
        int permissionCheckRead = ActivityCompat.checkSelfPermission(getContext(),
                android.Manifest.permission.READ_EXTERNAL_STORAGE);
        int permissionCheckWrite = ActivityCompat.checkSelfPermission(getContext(),
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionCheckCamera = ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.CAMERA);
        if (    permissionCheckRead != PackageManager.PERMISSION_GRANTED ||
                permissionCheckWrite != PackageManager.PERMISSION_GRANTED ||
                permissionCheckCamera != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                            android.Manifest.permission.READ_EXTERNAL_STORAGE,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            android.Manifest.permission.CAMERA
                    },
                    1);
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    // We have permissions!
                } else {
                }
                closeThis();
            }
        }
    }
}
