package com.besome.sketch.editor.manage.library.supabase;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.besome.sketch.beans.ProjectLibraryBean;
import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import pro.sketchware.R;
import pro.sketchware.supabase.SupabaseConfig;
import pro.sketchware.utility.SketchwareUtil;

public class ManageSupabaseActivity extends BaseAppCompatActivity implements View.OnClickListener {

    private MaterialSwitch libSwitch;
    private TextInputEditText etSupabaseUrl;
    private TextInputEditText etSupabaseAnonKey;
    private TextInputEditText etSupabaseServiceKey;
    private Button btnSave;

    private ProjectLibraryBean supabaseLibraryBean;
    private String scId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manage_library_manage_supabase);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        findViewById(R.id.layout_main_logo).setVisibility(View.GONE);
        getSupportActionBar().setTitle("Supabase Manager");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        scId = savedInstanceState != null ? savedInstanceState.getString("sc_id") : getIntent().getStringExtra("sc_id");
        supabaseLibraryBean = SupabaseConfig.load(scId);

        LinearLayout layoutSwitch = findViewById(R.id.layout_switch);
        layoutSwitch.setOnClickListener(this);

        libSwitch = findViewById(R.id.lib_switch);
        etSupabaseUrl = findViewById(R.id.et_supabase_url);
        etSupabaseAnonKey = findViewById(R.id.et_supabase_anon_key);
        etSupabaseServiceKey = findViewById(R.id.et_supabase_service_key);
        btnSave = findViewById(R.id.btn_save);

        btnSave.setOnClickListener(this);

        configure();
    }

    private void configure() {
        libSwitch.setChecked("Y".equals(supabaseLibraryBean.useYn));
        etSupabaseUrl.setText(supabaseLibraryBean.data);
        etSupabaseAnonKey.setText(supabaseLibraryBean.reserved1);
        etSupabaseServiceKey.setText(supabaseLibraryBean.reserved2);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.layout_switch) {
            libSwitch.setChecked(!libSwitch.isChecked());
        } else if (id == R.id.btn_save) {
            save();
        }
    }

    private void save() {
        String url = etSupabaseUrl.getText().toString().trim();
        String anonKey = etSupabaseAnonKey.getText().toString().trim();
        String serviceKey = etSupabaseServiceKey.getText().toString().trim();

        if (libSwitch.isChecked() && (url.isEmpty() || anonKey.isEmpty())) {
            SketchwareUtil.toast("Please enter both URL and Anonymous Key to enable Supabase.", Toast.LENGTH_LONG);
            return;
        }

        supabaseLibraryBean.useYn = libSwitch.isChecked() ? "Y" : "N";
        supabaseLibraryBean.data = url;
        supabaseLibraryBean.reserved1 = anonKey;
        supabaseLibraryBean.reserved2 = serviceKey;

        SupabaseConfig.save(scId, supabaseLibraryBean);
        SketchwareUtil.toast("Supabase settings saved successfully!", Toast.LENGTH_SHORT);

        Intent intent = new Intent();
        intent.putExtra("supabase", supabaseLibraryBean);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("supabase", supabaseLibraryBean);
        setResult(Activity.RESULT_OK, intent);
        super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("sc_id", scId);
        super.onSaveInstanceState(outState);
    }
}
