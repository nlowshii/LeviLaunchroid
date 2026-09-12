package org.levimc.pojavcontrols;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;

final class ControlProfileImporter {
    private ControlProfileImporter() {
    }

    static CustomControls parse(JsonElement root, Gson gson) throws IOException {
        if (root == null || !root.isJsonObject()) throw new IOException("Invalid control profile");
        JsonObject object = root.getAsJsonObject();
        if (object.has("layers")) return fromZalithTwo(object);
        CustomControls controls = gson.fromJson(object, CustomControls.class);
        if (controls == null) throw new IOException("Invalid control profile");
        return controls;
    }

    private static CustomControls fromZalithTwo(JsonObject root) {
        CustomControls controls = new CustomControls();
        JsonArray layers = array(root, "layers");
        if (layers == null) return controls;
        for (JsonElement layerElement : layers) {
            if (!layerElement.isJsonObject()) continue;
            JsonObject layer = layerElement.getAsJsonObject();
            if (bool(layer, "hide", false)) continue;
            boolean displayInGame = !"in_menu".equals(string(layer, "visibilityType", "always"));
            boolean displayInMenu = !"in_game".equals(string(layer, "visibilityType", "always"));
            JsonArray buttons = array(layer, "normalButtons");
            if (buttons != null) {
                for (JsonElement buttonElement : buttons) {
                    if (!buttonElement.isJsonObject()) continue;
                    ControlData button = normalButton(buttonElement.getAsJsonObject(), displayInGame, displayInMenu);
                    if (button != null) controls.mControlDataList.add(button);
                }
            }
            JsonArray joysticks = array(layer, "joystickButtons");
            if (joysticks != null) {
                for (JsonElement joystickElement : joysticks) {
                    if (!joystickElement.isJsonObject()) continue;
                    ControlJoystickData joystick = joystick(joystickElement.getAsJsonObject(), displayInGame, displayInMenu);
                    if (joystick != null) controls.mJoystickDataList.add(joystick);
                }
            }
        }
        controls.normalize();
        return controls;
    }

    private static ControlData normalButton(JsonObject object, boolean displayInGame, boolean displayInMenu) {
        JsonObject position = child(object, "position");
        JsonObject size = child(object, "buttonSize");
        if (position == null) return null;
        float width = sizeDp(size, "widthDp", "widthPercentage", "widthReference", 50f);
        float height = sizeDp(size, "heightDp", "heightPercentage", "heightReference", 50f);
        String name = text(object.get("text"));
        if (name.isBlank()) name = "Button";
        int key = eventKey(object.get("clickEvents"));
        if (key == KeyMapper.GLFW_KEY_UNKNOWN) key = keyFromLabel(name);
        ControlData button = new ControlData(name, new int[]{key},
                positionExpression(position, true), positionExpression(position, false), width, height);
        button.displayInGame = displayInGame && visibilityAllows(object);
        button.displayInMenu = displayInMenu && visibilityAllows(object);
        button.isToggle = bool(object, "isToggleable", false);
        button.isSwipeable = bool(object, "isSwipple", false);
        button.passThruEnabled = bool(object, "isPenetrable", false);
        return button;
    }

    private static ControlJoystickData joystick(JsonObject object, boolean displayInGame, boolean displayInMenu) {
        JsonObject position = child(object, "position");
        if (position == null) return null;
        ControlJoystickData joystick = new ControlJoystickData();
        joystick.name = "Joystick";
        joystick.dynamicX = positionExpression(position, true);
        joystick.dynamicY = positionExpression(position, false);
        JsonObject size = child(object, "size");
        if (size == null) size = child(object, "buttonSize");
        joystick.width = sizeDp(size, "widthDp", "widthPercentage", "widthReference", 120f);
        joystick.height = sizeDp(size, "heightDp", "heightPercentage", "heightReference", 120f);
        joystick.displayInGame = displayInGame && visibilityAllows(object);
        joystick.displayInMenu = displayInMenu && visibilityAllows(object);
        joystick.forwardLock = bool(object, "canLock", false);
        joystick.absolute = false;
        return joystick;
    }

    private static boolean visibilityAllows(JsonObject object) {
        return !"never".equals(string(object, "visibilityType", "always"));
    }

    private static String positionExpression(JsonObject position, boolean xAxis) {
        String axis = xAxis ? "x" : "y";
        float value = decimal(position, axis, 0.5f);
        float ratio = value > 1f ? value / 10000f : value;
        String variable = xAxis ? "screen_width" : "screen_height";
        String size = xAxis ? "width" : "height";
        ratio = Math.max(0f, Math.min(1f, ratio));
        return "(${" + variable + "} - ${" + size + "}) * " + Float.toString(ratio);
    }

    private static float sizeDp(JsonObject object, String dpKey, String percentageKey,
                                String referenceKey, float fallback) {
        if (object == null) return fallback;
        String type = string(object, "type", "dp");
        if ("percentage".equals(type)) {
            float percentage = decimal(object, percentageKey, 0f);
            if (percentage > 0f) {
                float referencePixels = "screen_width".equals(string(object, referenceKey, "screen_height"))
                        ? 1920f : 1080f;
                float density = 3f;
                return Math.max(16f, Math.min(400f, percentage / 10000f * referencePixels / density));
            }
        }
        float value = decimal(object, dpKey, fallback);
        if (value <= 0f) return fallback;
        return Math.max(16f, Math.min(400f, value));
    }

    private static int eventKey(JsonElement eventsElement) {
        if (eventsElement == null || !eventsElement.isJsonArray()) return KeyMapper.GLFW_KEY_UNKNOWN;
        for (JsonElement eventElement : eventsElement.getAsJsonArray()) {
            if (!eventElement.isJsonObject()) continue;
            JsonObject event = eventElement.getAsJsonObject();
            if (!"key".equals(string(event, "type", ""))) continue;
            int key = KeyMapper.fromName(string(event, "key", ""));
            if (key != KeyMapper.GLFW_KEY_UNKNOWN) return key;
        }
        return KeyMapper.GLFW_KEY_UNKNOWN;
    }

    private static int keyFromLabel(String label) {
        String normalized = label == null ? "" : label.trim();
        if (normalized.equalsIgnoreCase("PRI") || normalized.equalsIgnoreCase("Primary")) {
            return ControlData.SPECIALBTN_MOUSEPRI;
        }
        if (normalized.equalsIgnoreCase("SEC") || normalized.equalsIgnoreCase("Secondary")) {
            return ControlData.SPECIALBTN_MOUSESEC;
        }
        if (normalized.equalsIgnoreCase("Mouse")) return ControlData.SPECIALBTN_VIRTUALMOUSE;
        if (normalized.equalsIgnoreCase("Keyboard")) return ControlData.SPECIALBTN_KEYBOARD;
        if (normalized.equalsIgnoreCase("GUI")) return ControlData.SPECIALBTN_TOGGLECTRL;
        if (normalized.equalsIgnoreCase("Jump")) return KeyMapper.GLFW_KEY_SPACE;
        if (normalized.equalsIgnoreCase("Inventory") || normalized.equalsIgnoreCase("Inv")) return KeyMapper.GLFW_KEY_E;
        return KeyMapper.fromName(normalized);
    }

    private static JsonArray array(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    private static JsonObject child(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static String text(JsonElement element) {
        if (element == null || element.isJsonNull()) return "";
        if (element.isJsonPrimitive()) return element.getAsString();
        if (!element.isJsonObject()) return "";
        JsonObject object = element.getAsJsonObject();
        String[] keys = new String[]{"default", "value", "en_us", "en-US", "en"};
        for (String key : keys) {
            String value = text(object.get(key));
            if (!value.isBlank()) return value;
        }
        return "";
    }

    private static String string(JsonObject object, String key, String fallback) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) return fallback;
        try {
            String value = element.getAsString();
            return value == null || value.isBlank() ? fallback : value;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean bool(JsonObject object, String key, boolean fallback) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) return fallback;
        try {
            return element.getAsBoolean();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int number(JsonObject object, String key, int fallback) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) return fallback;
        try {
            return element.getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static float decimal(JsonObject object, String key, float fallback) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) return fallback;
        try {
            return element.getAsFloat();
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
