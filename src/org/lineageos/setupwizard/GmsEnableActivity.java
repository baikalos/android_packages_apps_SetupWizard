/*
 * Copyright (C) 2020-2022 The LineageOS Project
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
 * limitations under the License
 */

package org.lineageos.setupwizard;

import static org.lineageos.setupwizard.SetupWizardApp.ENABLE_GMS;

import android.app.Activity;
import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;

import com.google.android.setupcompat.util.WizardManagerHelper;

import org.lineageos.setupwizard.util.SetupWizardUtils;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;

public class GmsEnableActivity extends BaseSetupWizardActivity {

    public static final String TAG = "BaikalSetupWizard:" + GmsEnableActivity.class.getSimpleName();

    private CheckBox mGmsEnableCheckbox;
    private SetupWizardApp mSetupWizardApp;
    private static boolean sFirstTime = true;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = (Context)this;

        mSetupWizardApp = (SetupWizardApp) getApplication();
        getGlifLayout().setDescriptionText(getString(R.string.gms_enable_full_description,
                getString(R.string.gms_enable_description),
                getString(R.string.gms_enable_warning)));

        if (!SetupWizardUtils.hasGMSInstalled(this) ) {
            Log.v(TAG, "No Google Services installed, skipping GmsEnableActivity");
            
            Intent intent = WizardManagerHelper.getNextIntent(getIntent(), Activity.RESULT_OK);
            nextAction(NEXT_REQUEST, intent);
            finish();
            return;
        }

        if (!SetupWizardUtils.hasGMSDisabled(this) ) {
            Log.v(TAG, "Google Services already active, skipping GmsEnableActivity");
            /*
            Intent intent = WizardManagerHelper.getNextIntent(getIntent(), Activity.RESULT_OK);
            nextAction(NEXT_REQUEST, intent);
            finish();
            return;*/
        }

        Log.v(TAG, "GmsEnableActivity - Google Services exists but disabled");

        setNextText(R.string.next);
        mGmsEnableCheckbox = findViewById(R.id.gms_enable_checkbox);

        View cbView = findViewById(R.id.gms_enable_checkbox_view);
        cbView.setOnClickListener(v -> {
            mGmsEnableCheckbox.setChecked(!mGmsEnableCheckbox.isChecked());
        });

        // Allow overriding the default checkbox state
        /*if (sFirstTime) {
            mSetupWizardApp.getSettingsBundle().putBoolean(ENABLE_GMS,
                    SystemProperties.getBoolean(UPDATE_RECOVERY_PROP, false));
            //SetupWizardUtils.enableGmsAndWizard(this);
        }*/

        sFirstTime = false;
    }

    @Override
    public void onResume() {
        super.onResume();

        final Bundle myPageBundle = mSetupWizardApp.getSettingsBundle();
        final boolean checked = myPageBundle.getBoolean(ENABLE_GMS, false);
        mGmsEnableCheckbox.setChecked(checked);
        
    }

    @Override
    protected void onNextPressed() {
        mSetupWizardApp.getSettingsBundle().putBoolean(ENABLE_GMS,
                mGmsEnableCheckbox.isChecked());
        

        /*
        if( mGmsEnableCheckbox.isChecked() ) {
            new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.gms_enable_confirmation).setMessage(R.string.gms_require_reboot)
                .setPositiveButton(R.string.gms_enable_reboot, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SetupWizardUtils.enableGmsAndWizard(mContext, true);
                        //SetupWizardUtils.finishSetupWizard(mContext);
                        //SetupWizardUtils.disableHome(mContext);
                        Log.e(TAG, "SetupWizardUtils.enableGmsAndWizard");
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
                        Log.e(TAG, "retry");
                        return;
                    }
                }).show();                                               
        } else {*/
            Intent intent = WizardManagerHelper.getNextIntent(getIntent(), Activity.RESULT_OK);
            nextAction(NEXT_REQUEST, intent);
        /*}*/
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.gms_enable_page;
    }

    @Override
    protected int getTitleResId() {
        return R.string.gms_enable_title;
    }

    @Override
    protected int getIconResId() {
        return R.drawable.ic_system_update;
    }
}
