package org.levimc.pojavcontrols;

import androidx.annotation.Keep;

import java.util.ArrayList;
import java.util.List;

@Keep
public class CustomControls {
    public int version = 10;
    public float scaledAt = 100f;
    public static final int CURSOR_MODE_FOLLOW_FINGER = 0;
    public static final int CURSOR_MODE_RELATIVE = 1;
    public static final int CURSOR_ANIMATION_AUTO = 0;
    public static final int CURSOR_ANIMATION_GIF = 1;
    public static final int CURSOR_ANIMATION_SPRITE = 2;
    public static final int CURSOR_ANIMATION_FRAMES = 3;
    public float virtualMouseScale = 1f;
    public String virtualMouseImageUri = "";
    public List<String> virtualMouseFrameUris = new ArrayList<>();
    public int virtualMouseAnimationMode = CURSOR_ANIMATION_AUTO;
    public int virtualMouseSpriteColumns = 1;
    public int virtualMouseSpriteRows = 1;
    public int virtualMouseFrameDurationMs = 100;
    public String virtualMouseClickSoundUri = "";
    public int virtualMouseMode = CURSOR_MODE_FOLLOW_FINGER;
    public List<ControlData> mControlDataList = new ArrayList<>();
    public List<ControlDrawerData> mDrawerDataList = new ArrayList<>();
    public List<ControlJoystickData> mJoystickDataList = new ArrayList<>();

    public void normalize() {
        version = 10;
        if (scaledAt <= 0f) scaledAt = 100f;
        virtualMouseScale = Math.max(0.2f, Math.min(2f, virtualMouseScale <= 0f ? 1f : virtualMouseScale));
        if (virtualMouseImageUri == null) virtualMouseImageUri = "";
        if (virtualMouseAnimationMode < CURSOR_ANIMATION_AUTO || virtualMouseAnimationMode > CURSOR_ANIMATION_FRAMES) virtualMouseAnimationMode = CURSOR_ANIMATION_AUTO;
        if (virtualMouseFrameUris == null) virtualMouseFrameUris = new ArrayList<>();
        while (virtualMouseFrameUris.size() < 6) virtualMouseFrameUris.add("");
        if (virtualMouseFrameUris.size() > 6) virtualMouseFrameUris = new ArrayList<>(virtualMouseFrameUris.subList(0, 6));
        for (int i = 0; i < virtualMouseFrameUris.size(); i++) if (virtualMouseFrameUris.get(i) == null) virtualMouseFrameUris.set(i, "");
        virtualMouseSpriteColumns = Math.max(1, Math.min(16, virtualMouseSpriteColumns));
        virtualMouseSpriteRows = Math.max(1, Math.min(16, virtualMouseSpriteRows));
        virtualMouseFrameDurationMs = Math.max(30, Math.min(2000, virtualMouseFrameDurationMs <= 0 ? 100 : virtualMouseFrameDurationMs));
        if (virtualMouseClickSoundUri == null) virtualMouseClickSoundUri = "";
        if (virtualMouseMode != CURSOR_MODE_RELATIVE) virtualMouseMode = CURSOR_MODE_FOLLOW_FINGER;
        if (mControlDataList == null) mControlDataList = new ArrayList<>();
        if (mDrawerDataList == null) mDrawerDataList = new ArrayList<>();
        if (mJoystickDataList == null) mJoystickDataList = new ArrayList<>();
        for (ControlData button : mControlDataList) button.normalize();
        for (ControlDrawerData drawer : mDrawerDataList) drawer.normalize();
        for (ControlJoystickData joystick : mJoystickDataList) joystick.normalize();
    }

    public static CustomControls createDefault() {
        CustomControls controls = new CustomControls();
        controls.mControlDataList.add(button("Keyboard", ControlData.SPECIALBTN_KEYBOARD,
                "${margin}", "${margin}", 80, 30));
        controls.mControlDataList.add(button("GUI", ControlData.SPECIALBTN_TOGGLECTRL,
                "${margin}", "${bottom} - ${margin}", 50, 50));
        controls.mControlDataList.add(button("PRI", ControlData.SPECIALBTN_MOUSEPRI,
                "${right} - ${margin}", "${bottom} - ${margin} * 3 - ${height} * 2", 50, 50));
        controls.mControlDataList.add(button("SEC", ControlData.SPECIALBTN_MOUSESEC,
                "${right} - ${margin}", "${bottom} - ${margin} * 2 - ${height}", 50, 50));
        controls.mControlDataList.add(button("Mouse", ControlData.SPECIALBTN_VIRTUALMOUSE,
                "${right} - ${margin}", "${margin}", 80, 30));
        controls.mControlDataList.add(button("Chat", KeyMapper.GLFW_KEY_T,
                "${margin} * 2 + ${width}", "${margin}", 80, 30));
        controls.mControlDataList.add(button("Players", KeyMapper.GLFW_KEY_TAB,
                "${margin} * 3 + ${width} * 2", "${margin}", 80, 30));
        controls.mControlDataList.add(button("View", KeyMapper.GLFW_KEY_F1 + 4,
                "${margin}", "${height} + ${margin} * 2", 80, 30));
        controls.mControlDataList.add(button("W", KeyMapper.GLFW_KEY_W,
                "${margin} * 2 + ${width}", "${bottom} - ${margin} * 3 - ${height} * 2", 50, 50));
        controls.mControlDataList.add(button("A", KeyMapper.GLFW_KEY_A,
                "${margin}", "${bottom} - ${margin} * 2 - ${height}", 50, 50));
        controls.mControlDataList.add(button("S", KeyMapper.GLFW_KEY_S,
                "${margin} * 2 + ${width}", "${bottom} - ${margin}", 50, 50));
        controls.mControlDataList.add(button("D", KeyMapper.GLFW_KEY_D,
                "${margin} * 3 + ${width} * 2", "${bottom} - ${margin} * 2 - ${height}", 50, 50));
        controls.mControlDataList.add(button("Inventory", KeyMapper.GLFW_KEY_E,
                "${right} - ${margin} * 3 - ${width} * 2", "${bottom} - ${margin}", 80, 40));
        ControlData shift = button("Shift", KeyMapper.GLFW_KEY_LEFT_SHIFT,
                "${margin} * 2 + ${width}", "${bottom} - ${margin} * 4 - ${height} * 3", 60, 40);
        shift.isToggle = true;
        controls.mControlDataList.add(shift);
        controls.mControlDataList.add(button("Jump", KeyMapper.GLFW_KEY_SPACE,
                "${right} - ${margin} * 2 - ${width}", "${bottom} - ${margin} * 2 - ${height}", 60, 60));
        controls.normalize();
        return controls;
    }

    private static ControlData button(String name, int key, String x, String y, float width, float height) {
        return new ControlData(name, new int[]{key}, x, y, width, height);
    }
}
