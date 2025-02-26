/*
 * Copyright (C) 2016 The CyanogenMod Project
 * Copyright (C) 2017-2020, 2022 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.setupwizard;

import static android.os.Binder.getCallingUserHandle;
import static android.os.UserHandle.USER_CURRENT;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_3BUTTON_OVERLAY;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL_OVERLAY;

import static org.lineageos.setupwizard.Manifest.permission.FINISH_SETUP;
import static org.lineageos.setupwizard.SetupWizardApp.ACTION_SETUP_COMPLETE;
import static org.lineageos.setupwizard.SetupWizardApp.DISABLE_NAV_KEYS;
import static org.lineageos.setupwizard.SetupWizardApp.ENABLE_RECOVERY_UPDATE;
import static org.lineageos.setupwizard.SetupWizardApp.KEY_SEND_METRICS;
import static org.lineageos.setupwizard.SetupWizardApp.LOGV;
import static org.lineageos.setupwizard.SetupWizardApp.NAVIGATION_OPTION_KEY;
import static org.lineageos.setupwizard.SetupWizardApp.UPDATE_RECOVERY_PROP;
import static org.lineageos.setupwizard.SetupWizardApp.ENABLE_GMS;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.om.IOverlayManager;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.ImageView;
import android.util.Log; 

import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;


import com.google.android.setupcompat.util.WizardManagerHelper;

import org.lineageos.setupwizard.wizardmanager.WizardManager;
import org.lineageos.setupwizard.util.SetupWizardUtils;

import lineageos.providers.LineageSettings;

public class FinishActivity extends BaseSetupWizardActivity {

    public static final String TAG = "BaikalSetupWizard:" + FinishActivity.class.getSimpleName();

    private ImageView mReveal;

    private SetupWizardApp mSetupWizardApp;

    private final Handler mHandler = new Handler();

    private volatile boolean mIsFinishing = false;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;

        if (LOGV) {
            logActivityState("onCreate savedInstanceState=" + savedInstanceState);
        }

        /*if (SetupWizardUtils.hasGMS(this)) {
            Log.v(TAG, "onCreate hasGMS, skip linage wizard");
            SetupWizardUtils.disableHome(this);
            finish();
            return;
        }*/
        mSetupWizardApp = (SetupWizardApp) getApplication();
        mReveal = (ImageView) findViewById(R.id.reveal);
        setNextText(R.string.start);
    }

    @Override
    public void onBackPressed() {
        if (!mIsFinishing) {
            super.onBackPressed();
        }
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.finish_activity;
    }

    @Override
    public void finish() {
        super.finish();
        if (!isResumed() || mResultCode != RESULT_CANCELED) {
            overridePendingTransition(R.anim.translucent_enter, R.anim.translucent_exit);
        }
    }

    @Override
    public void onNavigateNext() {
        applyForwardTransition(TRANSITION_ID_NONE);
        startFinishSequence();
    }

    private void finishSetup() {
        if (!mIsFinishing) {
            mIsFinishing = true;
            if( mSetupWizardApp.getSettingsBundle().containsKey(ENABLE_GMS) && mSetupWizardApp.getSettingsBundle().getBoolean(ENABLE_GMS) ) {
                Log.e(TAG, "finishSetup - completeSetup");
                completeSetup();
            } else {
                Log.e(TAG, "finishSetup - setupRevealImage");
                setupRevealImage();
            }
        }
    }

    private void startFinishSequence() {
        Intent i = new Intent(ACTION_SETUP_COMPLETE);
        i.setPackage(getPackageName());
        sendBroadcastAsUser(i, getCallingUserHandle(), FINISH_SETUP);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        hideNextButton();
        finishSetup();
    }

    private void setupRevealImage() {
        final Point p = new Point();
        getWindowManager().getDefaultDisplay().getRealSize(p);
        final WallpaperManager wallpaperManager =
                WallpaperManager.getInstance(this);
        wallpaperManager.forgetLoadedWallpaper();
        final Bitmap wallpaper = wallpaperManager.getBitmap();
        Bitmap cropped = null;
        if (wallpaper != null) {
            cropped = Bitmap.createBitmap(wallpaper, 0,
                    0, Math.min(p.x, wallpaper.getWidth()),
                    Math.min(p.y, wallpaper.getHeight()));
        }
        if (cropped != null) {
            mReveal.setScaleType(ImageView.ScaleType.CENTER_CROP);
            mReveal.setImageBitmap(cropped);
        } else {
            mReveal.setBackground(wallpaperManager
                    .getBuiltInDrawable(p.x, p.y, false, 0, 0));
        }
        animateOut();
    }

    private void animateOut() {
        int cx = (mReveal.getLeft() + mReveal.getRight()) / 2;
        int cy = (mReveal.getTop() + mReveal.getBottom()) / 2;
        int finalRadius = Math.max(mReveal.getWidth(), mReveal.getHeight());
        Animator anim =
                ViewAnimationUtils.createCircularReveal(mReveal, cx, cy, 0, finalRadius);
        anim.setDuration(900);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mReveal.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        completeSetup();
                    }
                });
            }
        });
        anim.start();
    }

    private void completeSetup() {
        
        if ( mSetupWizardApp.getSettingsBundle().containsKey(ENABLE_GMS) && mSetupWizardApp.getSettingsBundle().getBoolean(ENABLE_GMS) ) {
            Log.e(TAG, "completeSetup - enableGmsAndWizard");

            new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.gms_enable_confirmation).setMessage(R.string.gms_require_reboot)
                .setPositiveButton(R.string.gms_enable_reboot, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.e(TAG, "SetupWizardUtils.enableGmsAndWizard");

                        handleEnableMetrics(mSetupWizardApp);
                        handleNavKeys(mSetupWizardApp);
                        handleRecoveryUpdate(mSetupWizardApp);
                        handleNavigationOption(mSetupWizardApp);
                        final WallpaperManager wallpaperManager =
                            WallpaperManager.getInstance(mSetupWizardApp);
                        wallpaperManager.forgetLoadedWallpaper();
                        
                        finishAllAppTasks();
                        SetupWizardUtils.enableGmsAndWizard(mSetupWizardApp, true);
                        SetupWizardUtils.disableComponent(mSetupWizardApp, WizardManager.class);
                        //SetupWizardUtils.finishSetupWizard(mContext);
                        //SetupWizardUtils.disableHome(mContext);
                        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                        try {
                            // no confirm, wait till device is rebooted
                            pm.reboot(null);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to reboot (2).", e);
                        }
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() { 
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.e(TAG, "cancel");

                        handleEnableMetrics(mSetupWizardApp);
                        handleNavKeys(mSetupWizardApp);
                        handleRecoveryUpdate(mSetupWizardApp);
                        handleNavigationOption(mSetupWizardApp);
                        final WallpaperManager wallpaperManager =
                            WallpaperManager.getInstance(mSetupWizardApp);
                        wallpaperManager.forgetLoadedWallpaper();

                        finishAllAppTasks();
                        SetupWizardUtils.enableStatusBar(mSetupWizardApp);
                        Intent intent = WizardManagerHelper.getNextIntent(getIntent(),
                            Activity.RESULT_OK);
                        startActivityForResult(intent, NEXT_REQUEST);
                    }
                }).show();                                               
        } else {
            Log.e(TAG, "completeSetup - not enableGmsAndWizard");

            handleEnableMetrics(mSetupWizardApp);
            handleNavKeys(mSetupWizardApp);
            handleRecoveryUpdate(mSetupWizardApp);
            handleNavigationOption(mSetupWizardApp);
            final WallpaperManager wallpaperManager =
                    WallpaperManager.getInstance(mSetupWizardApp);
            wallpaperManager.forgetLoadedWallpaper();

            finishAllAppTasks();
            SetupWizardUtils.enableStatusBar(this);
            Intent intent = WizardManagerHelper.getNextIntent(getIntent(),
                    Activity.RESULT_OK);
            startActivityForResult(intent, NEXT_REQUEST);
        }
    }

    private static void handleEnableMetrics(SetupWizardApp setupWizardApp) {
        Bundle privacyData = setupWizardApp.getSettingsBundle();
        if (privacyData != null
                && privacyData.containsKey(KEY_SEND_METRICS)) {
            LineageSettings.Secure.putInt(setupWizardApp.getContentResolver(),
                    LineageSettings.Secure.STATS_COLLECTION,
                    privacyData.getBoolean(KEY_SEND_METRICS)
                            ? 1 : 0);
        }
    }

    private static void handleNavKeys(SetupWizardApp setupWizardApp) {
        if (setupWizardApp.getSettingsBundle().containsKey(DISABLE_NAV_KEYS)) {
            writeDisableNavkeysOption(setupWizardApp,
                    setupWizardApp.getSettingsBundle().getBoolean(DISABLE_NAV_KEYS));
        }
    }

    private static void handleRecoveryUpdate(SetupWizardApp setupWizardApp) {
        if (setupWizardApp.getSettingsBundle().containsKey(ENABLE_RECOVERY_UPDATE)) {
            boolean update = setupWizardApp.getSettingsBundle()
                    .getBoolean(ENABLE_RECOVERY_UPDATE);

            SystemProperties.set(UPDATE_RECOVERY_PROP, String.valueOf(update));
        }
    }

    private void handleNavigationOption(Context context) {
        Bundle settingsBundle = mSetupWizardApp.getSettingsBundle();
        if (settingsBundle.containsKey(NAVIGATION_OPTION_KEY)) {
            IOverlayManager overlayManager = IOverlayManager.Stub.asInterface(
                    ServiceManager.getService(Context.OVERLAY_SERVICE));
            String selectedNavMode = settingsBundle.getString(NAVIGATION_OPTION_KEY);

            try {
                overlayManager.setEnabledExclusiveInCategory(selectedNavMode, USER_CURRENT);
            } catch (Exception e) {}
        }
    }

    private static void handleGmsEnable(SetupWizardApp setupWizardApp) {
        if (setupWizardApp.getSettingsBundle().containsKey(ENABLE_GMS)) {
            boolean update = setupWizardApp.getSettingsBundle()
                    .getBoolean(ENABLE_GMS);

            SetupWizardUtils.enableGmsAndWizard(setupWizardApp, update);
        }
    }

    private static void writeDisableNavkeysOption(Context context, boolean enabled) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        final boolean virtualKeysEnabled = LineageSettings.System.getIntForUser(
                context.getContentResolver(), LineageSettings.System.FORCE_SHOW_NAVBAR, 0,
                UserHandle.USER_CURRENT) != 0;
        if (enabled != virtualKeysEnabled) {
            LineageSettings.System.putIntForUser(context.getContentResolver(),
                    LineageSettings.System.FORCE_SHOW_NAVBAR, enabled ? 1 : 0,
                    UserHandle.USER_CURRENT);
        }
    }
}
