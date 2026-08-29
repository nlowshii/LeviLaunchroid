package org.levimc.pojavcontrols;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class PojavControlsEditorView extends FrameLayout {
    static final int REQUEST_IMPORT = 4101;
    static final int REQUEST_EXPORT = 4102;
    static final int REQUEST_CURSOR_IMAGE = 4103;
    static final int REQUEST_CURSOR_SOUND = 4104;
    static final int REQUEST_CURSOR_FRAME_BASE = 4200;
    static final int CURSOR_FRAME_COUNT = 6;

    private final Activity activity;
    private final Runnable closeAction;
    private final ControlRepository repository;
    private CustomControls profile;
    private String profileName;
    private final ControlEditorCanvas canvas;
    private final Spinner profileSpinner;
    private boolean profileSpinnerBusy;
    private int pendingCursorFrameSlot;

    PojavControlsEditorView(Activity activity, Runnable closeAction) {
        super(activity);
        this.activity = activity;
        this.closeAction = closeAction;
        repository = new ControlRepository(activity);
        profileName = repository.activeName();
        profile = repository.load(profileName);
        setClickable(true);
        setFocusable(true);
        setBackgroundColor(Color.TRANSPARENT);

        canvas = new ControlEditorCanvas(activity, this::showProperties);
        canvas.setProfile(profile);
        addView(canvas, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        float density = getResources().getDisplayMetrics().density;
        HorizontalScrollView toolbarScroll = new HorizontalScrollView(activity);
        toolbarScroll.setHorizontalScrollBarEnabled(false);
        toolbarScroll.setFillViewport(true);
        GradientDrawable toolbarBackground = new GradientDrawable();
        toolbarBackground.setColor(0xE02F343A);
        toolbarBackground.setCornerRadius(18 * density);
        toolbarScroll.setBackground(toolbarBackground);
        LinearLayout toolbar = new LinearLayout(activity);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(Math.round(12 * density), Math.round(8 * density),
                Math.round(12 * density), Math.round(8 * density));

        TextView title = new TextView(activity);
        title.setText(R.string.pojav_controls_editor);
        title.setTextColor(Color.WHITE);
        title.setTextSize(17);
        title.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        title.setSingleLine(true);
        title.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.addView(title, new LinearLayout.LayoutParams(Math.round(124 * density),
                Math.round(48 * density)));

        profileSpinner = new Spinner(activity);
        toolbar.addView(profileSpinner, new LinearLayout.LayoutParams(Math.round(180 * density),
                Math.round(48 * density)));
        profileSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (profileSpinnerBusy) return;
                String selected = String.valueOf(parent.getItemAtPosition(position));
                if (!selected.equals(profileName)) {
                    saveCurrent(false);
                    profileName = selected;
                    repository.setActive(selected);
                    profile = repository.load(selected);
                    canvas.setProfile(profile);
                    notifyProfileChanged();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        toolbar.addView(toolbarButton(R.string.pojav_controls_profiles, view -> showProfilesDialog()));
        toolbar.addView(toolbarButton(R.string.pojav_controls_mouse_settings, view -> showMouseSettings()));
        Button hide = toolbarButton(R.string.pojav_controls_hide_toolbar, null);
        toolbar.addView(hide);
        toolbar.addView(toolbarButton(R.string.pojav_controls_add, view -> showAddDialog()));
        toolbar.addView(toolbarButton(R.string.pojav_controls_save, view -> saveCurrent(true)));
        toolbar.addView(toolbarButton(R.string.pojav_controls_import, view -> startImport()));
        toolbar.addView(toolbarButton(R.string.pojav_controls_export, view -> startExport()));
        toolbar.addView(toolbarButton(R.string.pojav_controls_close, view -> close()));

        toolbarScroll.addView(toolbar, new HorizontalScrollView.LayoutParams(
                HorizontalScrollView.LayoutParams.WRAP_CONTENT, Math.round(64 * density)));
        LayoutParams toolbarParams = new LayoutParams(LayoutParams.MATCH_PARENT, Math.round(64 * density));
        toolbarParams.gravity = Gravity.TOP;
        toolbarParams.setMargins(Math.round(8 * density), Math.round(8 * density),
                Math.round(8 * density), 0);
        addView(toolbarScroll, toolbarParams);

        Button showToolbar = toolbarButton(R.string.pojav_controls_show_toolbar, null);
        showToolbar.setSingleLine(true);
        showToolbar.setGravity(Gravity.CENTER);
        showToolbar.setPadding(Math.round(8 * density), 0, Math.round(8 * density), 0);
        showToolbar.setVisibility(GONE);
        showToolbar.setBackgroundColor(0xD92F343A);
        LayoutParams showParams = new LayoutParams(Math.round(120 * density), Math.round(48 * density));
        showParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        addView(showToolbar, showParams);
        hide.setOnClickListener(view -> {
            toolbarScroll.setVisibility(GONE);
            showToolbar.setVisibility(VISIBLE);
        });
        showToolbar.setOnClickListener(view -> {
            showToolbar.setVisibility(GONE);
            toolbarScroll.setVisibility(VISIBLE);
        });
        reloadProfileSpinner(profileName);
    }

    void close() {
        saveCurrent(false);
        closeAction.run();
    }

    boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        boolean frameRequest = requestCode >= REQUEST_CURSOR_FRAME_BASE && requestCode < REQUEST_CURSOR_FRAME_BASE + CURSOR_FRAME_COUNT;
        if (requestCode != REQUEST_IMPORT && requestCode != REQUEST_EXPORT && requestCode != REQUEST_CURSOR_IMAGE && requestCode != REQUEST_CURSOR_SOUND && !frameRequest) return false;
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) return true;
        Uri uri = data.getData();
        try {
            if (requestCode == REQUEST_IMPORT) {
                String requested = uri.getLastPathSegment();
                if (requested != null && requested.endsWith(".json")) {
                    requested = requested.substring(0, requested.length() - 5);
                }
                try (InputStream input = activity.getContentResolver().openInputStream(uri)) {
                    if (input == null) throw new IllegalStateException();
                    profileName = repository.importProfile(requested, input);
                }
                repository.setActive(profileName);
                profile = repository.load(profileName);
                canvas.setProfile(profile);
                reloadProfileSpinner(profileName);
                notifyProfileChanged();
                Toast.makeText(activity, R.string.pojav_controls_imported, Toast.LENGTH_SHORT).show();
            } else if (requestCode == REQUEST_CURSOR_IMAGE) {
                int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                if ((flags & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0) {
                    try {
                        activity.getContentResolver().takePersistableUriPermission(uri, flags);
                    } catch (SecurityException ignored) {
                    }
                }
                profile.virtualMouseImageUri = uri.toString();
                saveCurrent(false);
                Toast.makeText(activity, R.string.pojav_controls_image_selected, Toast.LENGTH_SHORT).show();
            } else if (frameRequest) {
                profile.normalize();
                int slot = requestCode - REQUEST_CURSOR_FRAME_BASE;
                profile.virtualMouseFrameUris.set(slot, uri.toString());
                saveCurrent(false);
                Toast.makeText(activity, R.string.pojav_controls_frame_selected, Toast.LENGTH_SHORT).show();
            } else if (requestCode == REQUEST_CURSOR_SOUND) {
                int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                if ((flags & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0) {
                    try {
                        activity.getContentResolver().takePersistableUriPermission(uri, flags);
                    } catch (SecurityException ignored) {
                    }
                }
                profile.virtualMouseClickSoundUri = uri.toString();
                saveCurrent(false);
                Toast.makeText(activity, R.string.pojav_controls_sound_selected, Toast.LENGTH_SHORT).show();
            } else {
                saveCurrent(false);
                try (OutputStream output = activity.getContentResolver().openOutputStream(uri, "wt")) {
                    if (output == null) throw new IllegalStateException();
                    repository.exportProfile(profileName, output);
                }
            }
        } catch (Exception exception) {
            Toast.makeText(activity, R.string.pojav_controls_invalid, Toast.LENGTH_LONG).show();
        }
        return true;
    }

    private Button toolbarButton(int text, View.OnClickListener listener) {
        float density = getResources().getDisplayMetrics().density;
        Button button = new Button(activity);
        button.setText(text);
        button.setTextColor(0xFFEAFBF3);
        button.setTextSize(12);
        button.setGravity(Gravity.CENTER);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinWidth(0);
        button.setPadding(Math.round(14 * density), 0, Math.round(14 * density), 0);
        GradientDrawable background = new GradientDrawable();
        background.setColor(0xFF2B343A);
        background.setCornerRadius(8 * density);
        background.setStroke(Math.max(1, Math.round(density)), 0xFF46545C);
        button.setBackground(background);
        button.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, Math.round(44 * density)));
        button.setOnClickListener(listener);
        return button;
    }

    private void showMouseSettings() {
        float density = getResources().getDisplayMetrics().density;
        LinearLayout form = new LinearLayout(activity);
        form.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(18 * density);
        form.setPadding(padding, padding, padding, padding);

        TextView preview = new TextView(activity);
        preview.setText(R.string.pojav_controls_mouse_preview);
        preview.setTextColor(0xFFEAFBF3);
        preview.setTextSize(15);
        preview.setGravity(Gravity.CENTER);
        preview.setBackgroundColor(0xFF283238);
        form.addView(preview, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Math.round(58 * density)));

        TextView modeLabel = new TextView(activity);
        modeLabel.setText(R.string.pojav_controls_cursor_mode);
        modeLabel.setTextColor(0xFFB8C0C8);
        modeLabel.setTextSize(12);
        modeLabel.setPadding(0, Math.round(12 * density), 0, 0);
        form.addView(modeLabel);

        RadioGroup cursorModes = new RadioGroup(activity);
        RadioButton followFinger = new RadioButton(activity);
        followFinger.setId(View.generateViewId());
        followFinger.setText(R.string.pojav_controls_cursor_follow);
        followFinger.setTextColor(0xFFEAFBF3);
        cursorModes.addView(followFinger);
        RadioButton relativeCursor = new RadioButton(activity);
        relativeCursor.setId(View.generateViewId());
        relativeCursor.setText(R.string.pojav_controls_cursor_relative);
        relativeCursor.setTextColor(0xFFEAFBF3);
        cursorModes.addView(relativeCursor);
        cursorModes.check(profile.virtualMouseMode == CustomControls.CURSOR_MODE_RELATIVE
                ? relativeCursor.getId() : followFinger.getId());
        form.addView(cursorModes);

        TextView value = new TextView(activity);
        value.setTextColor(0xFFB8C0C8);
        value.setGravity(Gravity.CENTER_VERTICAL);
        form.addView(value, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Math.round(38 * density)));

        SeekBar scale = new SeekBar(activity);
        scale.setMax(180);
        scale.setProgress(Math.round((profile.virtualMouseScale - 0.2f) * 100f));
        form.addView(scale, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Math.round(42 * density)));
        Runnable updateValue = () -> value.setText(activity.getString(R.string.pojav_controls_mouse_scale)
                + ": " + Math.round((0.2f + scale.getProgress() / 100f) * 100f) + "%");
        scale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) { updateValue.run(); }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        updateValue.run();

        Button choose = new Button(activity);
        choose.setText(R.string.pojav_controls_choose_image);
        choose.setAllCaps(false);
        choose.setOnClickListener(view -> startCursorImagePick());
        form.addView(choose);

        TextView frameSlotLabel = new TextView(activity);
        frameSlotLabel.setText(R.string.pojav_controls_cursor_frame_slot);
        frameSlotLabel.setTextColor(0xFFB8C0C8);
        form.addView(frameSlotLabel);

        Spinner frameSlot = new Spinner(activity);
        String[] frameEntries = new String[CURSOR_FRAME_COUNT];
        for (int i = 0; i < CURSOR_FRAME_COUNT; i++) frameEntries[i] = "Frame " + (i + 1);
        frameSlot.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, frameEntries));
        form.addView(frameSlot);

        Button chooseFrame = new Button(activity);
        chooseFrame.setText(R.string.pojav_controls_choose_frame);
        chooseFrame.setAllCaps(false);
        chooseFrame.setOnClickListener(view -> {
            pendingCursorFrameSlot = frameSlot.getSelectedItemPosition();
            startCursorFramePick(pendingCursorFrameSlot);
        });
        form.addView(chooseFrame);

        TextView animationLabel = new TextView(activity);
        animationLabel.setText(R.string.pojav_controls_cursor_animation_mode);
        animationLabel.setTextColor(0xFFB8C0C8);
        form.addView(animationLabel);

        Spinner animationMode = new Spinner(activity);
        animationMode.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item,
                new String[]{activity.getString(R.string.pojav_controls_cursor_mode_auto),
                        activity.getString(R.string.pojav_controls_cursor_mode_gif),
                        activity.getString(R.string.pojav_controls_cursor_mode_sprite),
                        activity.getString(R.string.pojav_controls_cursor_mode_frames)}));
        animationMode.setSelection(Math.max(0, Math.min(3, profile.virtualMouseAnimationMode)));
        form.addView(animationMode);

        TextView columnsValue = new TextView(activity);
        columnsValue.setTextColor(0xFFB8C0C8);
        form.addView(columnsValue);
        SeekBar columns = new SeekBar(activity);
        columns.setMax(15);
        columns.setProgress(profile.virtualMouseSpriteColumns - 1);
        form.addView(columns);
        columns.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                columnsValue.setText(activity.getString(R.string.pojav_controls_sprite_columns) + ": " + (progress + 1));
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        columnsValue.setText(activity.getString(R.string.pojav_controls_sprite_columns) + ": " + profile.virtualMouseSpriteColumns);

        TextView rowsValue = new TextView(activity);
        rowsValue.setTextColor(0xFFB8C0C8);
        form.addView(rowsValue);
        SeekBar rows = new SeekBar(activity);
        rows.setMax(15);
        rows.setProgress(profile.virtualMouseSpriteRows - 1);
        form.addView(rows);
        rows.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                rowsValue.setText(activity.getString(R.string.pojav_controls_sprite_rows) + ": " + (progress + 1));
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        rowsValue.setText(activity.getString(R.string.pojav_controls_sprite_rows) + ": " + profile.virtualMouseSpriteRows);

        TextView durationValue = new TextView(activity);
        durationValue.setTextColor(0xFFB8C0C8);
        form.addView(durationValue);
        SeekBar duration = new SeekBar(activity);
        duration.setMax(1970);
        duration.setProgress(profile.virtualMouseFrameDurationMs - 30);
        form.addView(duration);
        duration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                durationValue.setText(activity.getString(R.string.pojav_controls_frame_duration) + ": " + (progress + 30));
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        durationValue.setText(activity.getString(R.string.pojav_controls_frame_duration) + ": " + profile.virtualMouseFrameDurationMs);

        Button clear = new Button(activity);
        clear.setText(R.string.pojav_controls_clear_image);
        clear.setAllCaps(false);
        clear.setOnClickListener(view -> {
            profile.virtualMouseImageUri = "";
            saveCurrent(false);
            Toast.makeText(activity, R.string.pojav_controls_image_cleared, Toast.LENGTH_SHORT).show();
        });
        form.addView(clear);

        Button chooseSound = new Button(activity);
        chooseSound.setText(R.string.pojav_controls_choose_sound);
        chooseSound.setAllCaps(false);
        chooseSound.setOnClickListener(view -> startCursorSoundPick());
        form.addView(chooseSound);

        Button clearSound = new Button(activity);
        clearSound.setText(R.string.pojav_controls_clear_sound);
        clearSound.setAllCaps(false);
        clearSound.setOnClickListener(view -> {
            profile.virtualMouseClickSoundUri = "";
            saveCurrent(false);
            Toast.makeText(activity, R.string.pojav_controls_sound_cleared, Toast.LENGTH_SHORT).show();
        });
        form.addView(clearSound);

        ScrollView scroll = new ScrollView(activity);
        scroll.addView(form, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        new AlertDialog.Builder(activity)
                .setTitle(R.string.pojav_controls_mouse_settings)
                .setView(scroll)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    profile.virtualMouseScale = 0.2f + scale.getProgress() / 100f;
                    profile.virtualMouseAnimationMode = animationMode.getSelectedItemPosition();
                    profile.virtualMouseSpriteColumns = columns.getProgress() + 1;
                    profile.virtualMouseSpriteRows = rows.getProgress() + 1;
                    profile.virtualMouseFrameDurationMs = duration.getProgress() + 30;
                    profile.virtualMouseMode = cursorModes.getCheckedRadioButtonId() == relativeCursor.getId()
                            ? CustomControls.CURSOR_MODE_RELATIVE : CustomControls.CURSOR_MODE_FOLLOW_FINGER;
                    profile.normalize();
                    saveCurrent(false);
                    canvas.rebuild();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void startCursorFramePick(int slot) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        activity.startActivityForResult(intent, REQUEST_CURSOR_FRAME_BASE + Math.max(0, Math.min(CURSOR_FRAME_COUNT - 1, slot)));
    }

    private void startCursorSoundPick() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/ogg");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        activity.startActivityForResult(intent, REQUEST_CURSOR_SOUND);
    }

    private void startCursorImagePick() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        activity.startActivityForResult(intent, REQUEST_CURSOR_IMAGE);
    }

    private void showAddDialog() {
        String[] entries = new String[]{activity.getString(R.string.pojav_controls_button),
                activity.getString(R.string.pojav_controls_joystick),
                activity.getString(R.string.pojav_controls_drawer)};
        new AlertDialog.Builder(activity)
                .setTitle(R.string.pojav_controls_add)
                .setItems(entries, (dialog, which) -> {
                    if (which == 0) profile.mControlDataList.add(new ControlData());
                    else if (which == 1) profile.mJoystickDataList.add(new ControlJoystickData());
                    else {
                        ControlDrawerData drawer = new ControlDrawerData();
                        drawer.buttonProperties.add(new ControlData("Button",
                                new int[]{KeyMapper.GLFW_KEY_SPACE},
                                "0.5 * ${screen_width}", "0.5 * ${screen_height}", 50, 50));
                        profile.mDrawerDataList.add(drawer);
                    }
                    canvas.rebuild();
                })
                .show();
    }

    private void showProfilesDialog() {
        List<String> profiles = repository.listProfiles();
        String[] actions = new String[]{activity.getString(R.string.pojav_controls_new_profile),
                activity.getString(R.string.pojav_controls_delete)};
        new AlertDialog.Builder(activity)
                .setTitle(R.string.pojav_controls_profiles)
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) showNewProfileDialog();
                    else if (profiles.size() > 1) showDeleteProfileDialog(profiles);
                })
                .show();
    }

    private void showNewProfileDialog() {
        EditText input = new EditText(activity);
        input.setHint(R.string.pojav_controls_profile_name);
        input.setSingleLine(true);
        int padding = Math.round(20 * getResources().getDisplayMetrics().density);
        LinearLayout wrapper = new LinearLayout(activity);
        wrapper.setPadding(padding, 0, padding, 0);
        wrapper.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        new AlertDialog.Builder(activity)
                .setTitle(R.string.pojav_controls_new_profile)
                .setView(wrapper)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String name = ControlRepository.sanitizeName(input.getText().toString());
                    if (name.isEmpty()) return;
                    saveCurrent(false);
                    profileName = repository.duplicate(profileName, name);
                    repository.setActive(profileName);
                    profile = repository.load(profileName);
                    canvas.setProfile(profile);
                    reloadProfileSpinner(profileName);
                    notifyProfileChanged();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showDeleteProfileDialog(List<String> profiles) {
        ArrayList<String> deletable = new ArrayList<>(profiles);
        deletable.remove("default");
        new AlertDialog.Builder(activity)
                .setTitle(R.string.pojav_controls_delete)
                .setItems(deletable.toArray(new String[0]), (dialog, which) -> {
                    String selected = deletable.get(which);
                    repository.delete(selected);
                    if (selected.equals(profileName)) {
                        profileName = repository.activeName();
                        profile = repository.load(profileName);
                        canvas.setProfile(profile);
                    }
                    reloadProfileSpinner(profileName);
                    notifyProfileChanged();
                })
                .show();
    }

    private void showProperties(ControlEditorCanvas.EditorTarget target) {
        float density = getResources().getDisplayMetrics().density;
        ScrollView scroll = new ScrollView(activity);
        LinearLayout form = new LinearLayout(activity);
        form.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(18 * density);
        form.setPadding(padding, padding, padding, padding);
        GradientDrawable panelBackground = new GradientDrawable();
        panelBackground.setColor(0xEE2F343A);
        panelBackground.setCornerRadius(22 * density);
        form.setBackground(panelBackground);
        scroll.setBackgroundColor(Color.TRANSPARENT);

        EditText name = field(form, R.string.pojav_controls_name, target.data.name);
        Button mapping = new Button(activity);
        mapping.setText(mappingText(target.data.keycodes));
        mapping.setAllCaps(false);
        int[] selectedCodes = Arrays.copyOf(target.data.keycodes, 1);
        if (target.type == ControlEditorCanvas.EditorTarget.BUTTON ||
                target.type == ControlEditorCanvas.EditorTarget.DRAWER_BUTTON) {
            addLabel(form, R.string.pojav_controls_mapping);
            form.addView(mapping);
            mapping.setOnClickListener(view -> showMappingDialog(selectedCodes, mapping));
        }

        EditText x = field(form, R.string.pojav_controls_position_x, target.data.dynamicX);
        EditText y = field(form, R.string.pojav_controls_position_y, target.data.dynamicY);
        SeekBar width = slider(form, R.string.pojav_controls_width, target.data.width, 400, "dp");
        SeekBar height = slider(form, R.string.pojav_controls_height, target.data.height, 400, "dp");
        bindLiveSlider(width, "dp", () -> {
            target.data.width = width.getProgress();
            target.data.normalize();
            canvas.rebuild();
        });
        bindLiveSlider(height, "dp", () -> {
            target.data.height = height.getProgress();
            target.data.normalize();
            canvas.rebuild();
        });
        addLabel(form, R.string.pojav_controls_opacity);
        SeekBar opacity = new SeekBar(activity);
        opacity.setMax(100);
        opacity.setProgress(Math.round(target.data.opacity * 100f));
        form.addView(opacity, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Math.round(42 * density)));
        TextView opacityValue = sliderValue(form, Math.round(target.data.opacity * 100f), "%");
        opacity.setTag(opacityValue);
        opacity.setOnSeekBarChangeListener(sliderListener(opacityValue, "%"));
        bindLiveSlider(opacity, "%", () -> {
            target.data.opacity = opacity.getProgress() / 100f;
            canvas.rebuild();
        });
        EditText background = field(form, R.string.pojav_controls_background,
                String.format("#%08X", target.data.bgColor));
        EditText stroke = field(form, R.string.pojav_controls_stroke,
                String.format("#%08X", target.data.strokeColor));
        SeekBar strokeWidth = slider(form, R.string.pojav_controls_stroke_width,
                target.data.strokeWidth, 20, "dp");
        bindLiveSlider(strokeWidth, "dp", () -> {
            target.data.strokeWidth = strokeWidth.getProgress();
            canvas.rebuild();
        });
        addLabel(form, R.string.pojav_controls_corner_radius);
        SeekBar radius = new SeekBar(activity);
        radius.setMax(100);
        radius.setProgress(Math.round(target.data.cornerRadius));
        form.addView(radius, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Math.round(42 * density)));
        TextView radiusValue = sliderValue(form, Math.round(target.data.cornerRadius), "%");
        radius.setTag(radiusValue);
        radius.setOnSeekBarChangeListener(sliderListener(radiusValue, "%"));
        bindLiveSlider(radius, "%", () -> {
            target.data.cornerRadius = radius.getProgress();
            canvas.rebuild();
        });
        addLabel(form, R.string.pojav_controls_shape);
        Spinner shape = new Spinner(activity);
        String[] shapeNames = new String[]{
                activity.getString(R.string.pojav_controls_shape_rounded),
                activity.getString(R.string.pojav_controls_shape_pill),
                activity.getString(R.string.pojav_controls_shape_square),
                activity.getString(R.string.pojav_controls_shape_circle)
        };
        shape.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, shapeNames));
        shape.setSelection(Math.max(0, Math.min(ControlData.SHAPE_CIRCLE, target.data.shape)));
        form.addView(shape);
        shape.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (target.data.shape == position) return;
                target.data.shape = position;
                canvas.rebuild();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        CheckBox toggle = check(form, R.string.pojav_controls_toggle, target.data.isToggle);
        CheckBox swipeable = check(form, R.string.pojav_controls_swipeable, target.data.isSwipeable);
        CheckBox passThrough = check(form, R.string.pojav_controls_pass_through, target.data.passThruEnabled);
        CheckBox inGame = check(form, R.string.pojav_controls_in_game, target.data.displayInGame);
        CheckBox inMenu = check(form, R.string.pojav_controls_in_menu, target.data.displayInMenu);

        CheckBox joystickAbsolute = null;
        CheckBox forwardLock = null;
        if (target.joystick != null) {
            joystickAbsolute = check(form, R.string.pojav_controls_joystick_absolute, target.joystick.absolute);
            forwardLock = check(form, R.string.pojav_controls_forward_lock, target.joystick.forwardLock);
        }

        Spinner orientation = null;
        if (target.type == ControlEditorCanvas.EditorTarget.DRAWER) {
            addLabel(form, R.string.pojav_controls_orientation);
            orientation = new Spinner(activity);
            ArrayAdapter<ControlDrawerData.Orientation> adapter = new ArrayAdapter<>(activity,
                    android.R.layout.simple_spinner_dropdown_item, ControlDrawerData.Orientation.values());
            orientation.setAdapter(adapter);
            orientation.setSelection(target.drawer.orientation.ordinal());
            form.addView(orientation);
            Button addDrawerButton = new Button(activity);
            addDrawerButton.setText(R.string.pojav_controls_button);
            addDrawerButton.setOnClickListener(view -> {
                target.drawer.buttonProperties.add(new ControlData());
                canvas.rebuild();
            });
            form.addView(addDrawerButton);
        }

        LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button clone = new Button(activity);
        clone.setText(R.string.pojav_controls_clone);
        Button delete = new Button(activity);
        delete.setText(R.string.pojav_controls_delete);
        actions.addView(clone, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        actions.addView(delete, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        form.addView(actions);
        scroll.addView(form);

        CheckBox finalJoystickAbsolute = joystickAbsolute;
        CheckBox finalForwardLock = forwardLock;
        Spinner finalOrientation = orientation;
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.pojav_controls_properties)
                .setView(scroll)
                .setPositiveButton(android.R.string.ok, (ignored, which) -> {
                    target.data.name = name.getText().toString().trim();
                    target.data.keycodes = Arrays.copyOf(selectedCodes, 1);
                    target.data.dynamicX = x.getText().toString().trim();
                    target.data.dynamicY = y.getText().toString().trim();
                    target.data.width = width.getProgress();
                    target.data.height = height.getProgress();
                    target.data.opacity = opacity.getProgress() / 100f;
                    target.data.bgColor = color(background, target.data.bgColor);
                    target.data.strokeColor = color(stroke, target.data.strokeColor);
                    target.data.strokeWidth = strokeWidth.getProgress();
                    target.data.cornerRadius = radius.getProgress();
                    target.data.shape = shape.getSelectedItemPosition();
                    target.data.isToggle = toggle.isChecked();
                    target.data.isSwipeable = swipeable.isChecked();
                    target.data.passThruEnabled = passThrough.isChecked();
                    target.data.displayInGame = inGame.isChecked();
                    target.data.displayInMenu = inMenu.isChecked();
                    if (target.joystick != null) {
                        target.joystick.absolute = finalJoystickAbsolute.isChecked();
                        target.joystick.forwardLock = finalForwardLock.isChecked();
                    }
                    if (finalOrientation != null) {
                        target.drawer.orientation =
                                (ControlDrawerData.Orientation) finalOrientation.getSelectedItem();
                    }
                    target.data.normalize();
                    canvas.rebuild();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        clone.setOnClickListener(view -> {
            target.cloneAction.run();
            dialog.dismiss();
        });
        delete.setOnClickListener(view -> {
            target.deleteAction.run();
            dialog.dismiss();
        });
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void showMappingDialog(int[] selectedCodes, Button mapping) {
        List<KeyMapper.Entry> entries = KeyMapper.entries();
        String[] names = new String[entries.size()];
        int selected = 0;
        for (int i = 0; i < entries.size(); i++) {
            names[i] = entries.get(i).name;
            if (selectedCodes.length > 0 && selectedCodes[0] == entries.get(i).glfwCode) selected = i;
        }
        int[] selectedIndex = new int[]{selected};
        new AlertDialog.Builder(activity)
                .setTitle(R.string.pojav_controls_mapping)
                .setSingleChoiceItems(names, selected, (dialog, which) -> selectedIndex[0] = which)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    selectedCodes[0] = entries.get(selectedIndex[0]).glfwCode;
                    mapping.setText(mappingText(selectedCodes));
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private String mappingText(int[] keycodes) {
        int code = keycodes == null || keycodes.length == 0
                ? KeyMapper.GLFW_KEY_UNKNOWN : keycodes[0];
        return code == KeyMapper.GLFW_KEY_UNKNOWN
                ? activity.getString(R.string.pojav_controls_mapping) : KeyMapper.nameOf(code);
    }

    private SeekBar slider(LinearLayout form, int label, float initial, int max, String suffix) {
        addLabel(form, label);
        SeekBar bar = new SeekBar(activity);
        bar.setMax(max);
        bar.setProgress(Math.max(0, Math.min(max, Math.round(initial))));
        form.addView(bar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Math.round(42 * getResources().getDisplayMetrics().density)));
        TextView value = sliderValue(form, bar.getProgress(), suffix);
        bar.setTag(value);
        bar.setOnSeekBarChangeListener(sliderListener(value, suffix));
        return bar;
    }

    private void bindLiveSlider(SeekBar bar, String suffix, Runnable action) {
        TextView value = (TextView) bar.getTag();
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar slider, int progress, boolean fromUser) {
                value.setText(progress + suffix);
                action.run();
            }
            @Override public void onStartTrackingTouch(SeekBar slider) {}
            @Override public void onStopTrackingTouch(SeekBar slider) {}
        });
    }

    private TextView sliderValue(LinearLayout form, int value, String suffix) {
        TextView result = new TextView(activity);
        result.setText(value + suffix);
        result.setTextColor(0xFFB8C0C8);
        result.setGravity(Gravity.CENTER_VERTICAL);
        form.addView(result, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Math.round(30 * getResources().getDisplayMetrics().density)));
        return result;
    }

    private SeekBar.OnSeekBarChangeListener sliderListener(TextView value, String suffix) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                value.setText(progress + suffix);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        };
    }

    private EditText field(LinearLayout form, int label, String value) {
        addLabel(form, label);
        EditText input = new EditText(activity);
        input.setText(value == null ? "" : value);
        input.setSingleLine(true);
        form.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return input;
    }

    private void addLabel(LinearLayout form, int label) {
        TextView view = new TextView(activity);
        view.setText(label);
        view.setTextColor(0xFFB8C0C8);
        view.setTextSize(12);
        view.setPadding(0, Math.round(8 * getResources().getDisplayMetrics().density), 0, 0);
        form.addView(view);
    }

    private CheckBox check(LinearLayout form, int label, boolean checked) {
        CheckBox box = new CheckBox(activity);
        box.setText(label);
        box.setChecked(checked);
        form.addView(box);
        return box;
    }

    private float number(EditText input, float fallback) {
        try {
            return Float.parseFloat(input.getText().toString().trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private int color(EditText input, int fallback) {
        String value = input.getText().toString().trim();
        try {
            if (!value.startsWith("#")) value = "#" + value;
            if (value.length() == 9) return (int) Long.parseLong(value.substring(1), 16);
            return Color.parseColor(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private void saveCurrent(boolean toast) {
        repository.save(profileName, profile);
        repository.setActive(profileName);
        notifyProfileChanged();
        if (toast) Toast.makeText(activity, R.string.pojav_controls_saved, Toast.LENGTH_SHORT).show();
    }

    private void reloadProfileSpinner(String selected) {
        profileSpinnerBusy = true;
        List<String> profiles = repository.listProfiles();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_dropdown_item, profiles);
        profileSpinner.setAdapter(adapter);
        profileSpinner.setSelection(Math.max(0, profiles.indexOf(selected)));
        profileSpinnerBusy = false;
    }

    private void startImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        activity.startActivityForResult(intent, REQUEST_IMPORT);
    }

    private void startExport() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, profileName + ".json");
        activity.startActivityForResult(intent, REQUEST_EXPORT);
    }

    private void notifyProfileChanged() {
        Intent intent = new Intent(PojavControls.ACTION_PROFILE_CHANGED);
        intent.setPackage(activity.getPackageName());
        activity.sendBroadcast(intent);
    }
}
