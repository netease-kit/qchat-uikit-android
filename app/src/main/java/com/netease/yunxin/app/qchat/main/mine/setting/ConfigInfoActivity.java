// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.app.qchat.main.mine.setting;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.netease.yunxin.app.qchat.R;
import com.netease.yunxin.app.qchat.databinding.ActivityConfigInfoBinding;
import com.netease.yunxin.app.qchat.welcome.WelcomeActivity;
import com.netease.yunxin.kit.common.ui.activities.BaseActivity;
import com.netease.yunxin.kit.common.ui.dialog.CommonConfirmDialog;

/**
 * OpenClaw配置信息页面
 * 允许用户动态配置AppKey、Account、Token、OpenClaw Account等参数
 */
public class ConfigInfoActivity extends BaseActivity {

    private ActivityConfigInfoBinding viewBinding;
    private ConfigInfoViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        changeStatusBarColor(R.color.color_e9eff5);
        
        viewBinding = ActivityConfigInfoBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
        viewModel = new ViewModelProvider(this).get(ConfigInfoViewModel.class);

        initView();
        observeViewModel();
    }

    private void initView() {
        // 设置标题栏返回按钮
        viewBinding.configTitleBar.setOnBackIconClickListener(v -> onBackPressed());

        // 加载现有配置并填充输入框
        viewModel.loadExistingConfig();
        fillInputsFromViewModel();

        // 设置输入框监听器
        setupInputWatchers();
        
        // 保存按钮点击事件
        viewBinding.btnSaveConfig.setOnClickListener(v -> {
            if (viewModel.validateAndSaveConfig()) {
                showRestartConfirmDialog();
            }
        });
        
        // 重置按钮点击事件
        viewBinding.tvResetConfig.setOnClickListener(v -> {
            viewModel.resetConfig();
            // 重置后清空输入框
            viewBinding.etAppKey.setText("");
            viewBinding.etAccount.setText("");
            viewBinding.etToken.setText("");
            viewBinding.etOpenclawAccount.setText("");
            Toast.makeText(this, getString(R.string.config_reset_success), Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * 将ViewModel中已加载的配置填充到输入框（仅在初始化时调用一次）
     */
    private void fillInputsFromViewModel() {
        String appKeyVal = viewModel.getAppKey().getValue();
        String accountVal = viewModel.getAccount().getValue();
        String tokenVal = viewModel.getToken().getValue();
        String openClawVal = viewModel.getOpenClawAccount().getValue();

        if (appKeyVal != null) viewBinding.etAppKey.setText(appKeyVal);
        if (accountVal != null) viewBinding.etAccount.setText(accountVal);
        if (tokenVal != null) viewBinding.etToken.setText(tokenVal);
        if (openClawVal != null) viewBinding.etOpenclawAccount.setText(openClawVal);
    }

    private void setupInputWatchers() {
        // AppKey输入监听
        viewBinding.etAppKey.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setAppKey(s.toString());
                validateAppKey(s.toString());
            }
        });
        
        // Account输入监听
        viewBinding.etAccount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setAccount(s.toString());
            }
        });
        
        // Token输入监听
        viewBinding.etToken.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setToken(s.toString());
            }
        });
        
        // OpenClaw Account输入监听
        viewBinding.etOpenclawAccount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setOpenClawAccount(s.toString());
            }
        });
    }

    private void validateAppKey(String appKey) {
        if (!viewModel.isValidAppKey(appKey)) {
            viewBinding.tvAppKeyError.setText(getString(R.string.config_app_key_error));
            viewBinding.tvAppKeyError.setVisibility(View.VISIBLE);
        } else {
            viewBinding.tvAppKeyError.setVisibility(View.GONE);
        }
    }

    /**
     * 显示重启应用确认对话框
     */
    private void showRestartConfirmDialog() {
        CommonConfirmDialog.Companion.show(
                this,
                getString(R.string.server_config_agent_title),
                getString(R.string.server_config_agent_dialog_content),
                getString(R.string.server_config_dialog_cancel),
                getString(R.string.server_config_dialog_positive),
                true,
                false,
                positive -> {
                    if (positive) {
                        restartApp();
                    } else {
                        finish();
                    }
                });
    }

    /**
     * 重启应用
     */
    private void restartApp() {
        // 清除所有Activity栈，重新启动Welcome页面
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        
        // 杀死当前进程，确保完全重启
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }

    private void observeViewModel() {
        // 观察保存按钮是否可用
        viewModel.getSaveButtonEnabled().observe(this, enabled -> {
            viewBinding.btnSaveConfig.setEnabled(enabled != null && enabled);
        });

        // 观察保存结果
        viewModel.getSaveResult().observe(this, result -> {
            if (result != null && !result.isSuccess()) {
                Toast.makeText(this, result.getErrorMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        
        // 观察验证错误
        viewModel.getValidationError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
