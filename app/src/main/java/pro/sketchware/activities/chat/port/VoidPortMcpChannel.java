package pro.sketchware.activities.chat.port;

import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Android-safe port of electron-main/mcpChannel.ts.
 *
 * The mobile app cannot spawn desktop stdio/SSE MCP clients reliably, so this
 * class preserves config parsing, status reporting and tool naming semantics.
 */
public final class VoidPortMcpChannel {
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private VoidPortMcpChannel() {
    }

    private static final class JsonRpcResponse {
        final JSONObject body;
        final String sessionId;

        JsonRpcResponse(JSONObject body, String sessionId) {
            this.body = body == null ? new JSONObject() : body;
            this.sessionId = sessionId == null ? "" : sessionId;
        }
    }

    public static final class ServerStatus {
        public final String name;
        public final String command;
        public final boolean enabled;
        public final String status;

        ServerStatus(String name, String command, boolean enabled, String status) {
            this.name = name == null ? "" : name;
            this.command = command == null ? "" : command;
            this.enabled = enabled;
            this.status = status == null ? "offline" : status;
        }
    }

    public static List<ServerStatus> readServerStatuses(SharedPreferences prefs) {
        List<ServerStatus> result = new ArrayList<>();
        JSONObject servers = VoidPortSettings.readMcpConfigObject(prefs).optJSONObject("mcpServers");
        JSONArray names = servers == null ? null : servers.names();
        for (int i = 0; names != null && i < names.length(); i++) {
            String name = names.optString(i, "");
            JSONObject server = servers.optJSONObject(name);
            if (server == null) {
                continue;
            }
            boolean enabled = server.optBoolean("enabled", true);
            String command = server.optString("command", "");
            String url = server.optString("url", "");
            if (command.isEmpty()) {
                command = url;
            }
            String status = !enabled ? "offline" : !url.isEmpty() ? "http-jsonrpc" : "stdio-config-only";
            result.add(new ServerStatus(name, command, enabled, status));
        }
        return result;
    }

    public static String buildPromptSummary(SharedPreferences prefs) {
        List<ServerStatus> servers = readServerStatuses(prefs);
        if (servers.isEmpty()) {
            return "MCP Android bridge: no configured servers.";
        }
        StringBuilder builder = new StringBuilder("MCP Android bridge: URL servers are callable through JSON-RPC HTTP; command/stdio servers need an Android-accessible URL endpoint.\n");
        for (ServerStatus server : servers) {
            builder.append("- ")
                    .append(server.name)
                    .append(" [")
                    .append(server.status)
                    .append("] ");
            if (!server.command.isEmpty()) {
                builder.append(server.command);
            }
            builder.append("\n");
        }
        return builder.toString().trim();
    }

    public static JSONArray getToolsAsMCP(SharedPreferences prefs) {
        JSONArray result = new JSONArray();
        JSONObject servers = VoidPortSettings.readMcpConfigObject(prefs).optJSONObject("mcpServers");
        JSONArray names = servers == null ? null : servers.names();
        for (int i = 0; names != null && i < names.length(); i++) {
            String serverName = names.optString(i, "");
            JSONObject server = servers.optJSONObject(serverName);
            if (server == null || !server.optBoolean("enabled", true)) {
                continue;
            }
            JSONArray declaredTools = firstArray(server, "tools", "toolDefinitions");
            if (declaredTools != null && declaredTools.length() > 0) {
                appendDeclaredTools(result, serverName, declaredTools);
                continue;
            }
            if (!server.optString("url", "").trim().isEmpty()) {
                result.put(genericServerTool(serverName));
            }
        }
        return result;
    }

    @Nullable
    public static String resolveServerNameForTool(SharedPreferences prefs, String prefixedToolName) {
        if (prefixedToolName == null || !prefixedToolName.startsWith("mcp_")) {
            return null;
        }
        JSONObject servers = VoidPortSettings.readMcpConfigObject(prefs).optJSONObject("mcpServers");
        JSONArray names = servers == null ? null : servers.names();
        for (int i = 0; names != null && i < names.length(); i++) {
            String serverName = names.optString(i, "");
            JSONObject server = servers.optJSONObject(serverName);
            if (server == null || !server.optBoolean("enabled", true)) {
                continue;
            }
            if (!findDirectToolName(serverName, server, prefixedToolName).isEmpty()) {
                return serverName;
            }
            if (addUniquePrefix(serverName, "call_tool").equals(prefixedToolName)) {
                return serverName;
            }
        }
        return null;
    }

    public static String callTool(SharedPreferences prefs, String prefixedToolName, JSONObject args) {
        JSONObject servers = VoidPortSettings.readMcpConfigObject(prefs).optJSONObject("mcpServers");
        JSONArray names = servers == null ? null : servers.names();
        for (int i = 0; names != null && i < names.length(); i++) {
            String serverName = names.optString(i, "");
            JSONObject server = servers.optJSONObject(serverName);
            if (server == null || !server.optBoolean("enabled", true)) {
                continue;
            }

            String directToolName = findDirectToolName(serverName, server, prefixedToolName);
            if (!directToolName.isEmpty()) {
                return callServerTool(serverName, server, directToolName, args);
            }

            String genericName = addUniquePrefix(serverName, "call_tool");
            if (genericName.equals(prefixedToolName)) {
                String actualToolName = args == null ? "" : args.optString("tool_name", "").trim();
                JSONObject actualArgs = parseArgumentsObject(args == null ? null : args.opt("arguments"));
                if (actualToolName.isEmpty()) {
                    return "MCP error: tool_name is required for " + genericName + ".";
                }
                return callServerTool(serverName, server, actualToolName, actualArgs);
            }
        }
        return "MCP error: tool '" + prefixedToolName + "' is not configured or is disabled.";
    }

    private static void appendDeclaredTools(JSONArray result, String serverName, JSONArray declaredTools) {
        for (int i = 0; i < declaredTools.length(); i++) {
            JSONObject declared = declaredTools.optJSONObject(i);
            if (declared == null) {
                continue;
            }
            String name = declared.optString("name", "").trim();
            if (name.isEmpty()) {
                continue;
            }
            JSONObject function = new JSONObject();
            try {
                function.put("name", addUniquePrefix(serverName, name));
                function.put("description", declared.optString("description", "MCP tool from " + serverName));
                JSONObject schema = declared.optJSONObject("inputSchema");
                if (schema == null) {
                    schema = declared.optJSONObject("parameters");
                }
                function.put("parameters", schema == null
                        ? new JSONObject().put("type", "object").put("properties", new JSONObject())
                        : schema);
                result.put(new JSONObject().put("type", "function").put("function", function));
            } catch (Exception ignored) {
            }
        }
    }

    private static JSONObject genericServerTool(String serverName) {
        JSONObject tool = new JSONObject();
        try {
            JSONObject properties = new JSONObject();
            properties.put("tool_name", new JSONObject()
                    .put("type", "string")
                    .put("description", "The MCP tool name exposed by server " + serverName + "."));
            properties.put("arguments", new JSONObject()
                    .put("type", "string")
                    .put("description", "JSON object string with arguments for the MCP tool. Use {} when empty."));
            JSONObject parameters = new JSONObject()
                    .put("type", "object")
                    .put("properties", properties)
                    .put("required", new JSONArray().put("tool_name"));
            JSONObject function = new JSONObject()
                    .put("name", addUniquePrefix(serverName, "call_tool"))
                    .put("description", "Calls an MCP tool on Android through the configured HTTP JSON-RPC URL for server " + serverName + ".")
                    .put("parameters", parameters);
            tool.put("type", "function").put("function", function);
        } catch (Exception ignored) {
        }
        return tool;
    }

    private static String findDirectToolName(String serverName, JSONObject server, String prefixedToolName) {
        JSONArray declaredTools = firstArray(server, "tools", "toolDefinitions");
        for (int i = 0; declaredTools != null && i < declaredTools.length(); i++) {
            JSONObject declared = declaredTools.optJSONObject(i);
            String name = declared == null ? "" : declared.optString("name", "").trim();
            if (!name.isEmpty() && addUniquePrefix(serverName, name).equals(prefixedToolName)) {
                return name;
            }
        }
        return "";
    }

    private static String callServerTool(String serverName, JSONObject server, String toolName, JSONObject args) {
        String url = server.optString("url", "").trim();
        if (url.isEmpty()) {
            return "MCP server '" + serverName + "' uses command/stdio. Android cannot launch desktop MCP stdio clients reliably; expose it as an HTTP URL in mcpServers." ;
        }
        try {
            String sessionId = initializeHttpSession(url);
            JsonRpcResponse response = postJsonRpc(url, new JSONObject()
                    .put("jsonrpc", "2.0")
                    .put("id", UUID.randomUUID().toString())
                    .put("method", "tools/call")
                    .put("params", new JSONObject()
                            .put("name", toolName)
                            .put("arguments", args == null ? new JSONObject() : args)), sessionId);
            if (response.body.has("error")) {
                return "MCP error from " + serverName + ": " + response.body.optJSONObject("error");
            }
            JSONObject result = response.body.optJSONObject("result");
            return result == null ? response.body.toString() : result.toString();
        } catch (Exception e) {
            return "MCP HTTP call failed for " + serverName + "/" + toolName + ": " + e.getMessage();
        }
    }

    private static String initializeHttpSession(String url) throws Exception {
        JsonRpcResponse init = postJsonRpc(url, new JSONObject()
                .put("jsonrpc", "2.0")
                .put("id", UUID.randomUUID().toString())
                .put("method", "initialize")
                .put("params", new JSONObject()
                        .put("protocolVersion", "2024-11-05")
                        .put("capabilities", new JSONObject())
                        .put("clientInfo", new JSONObject()
                                .put("name", "Sketchware KG Android")
                                .put("version", "android"))), "");
        String sessionId = init.sessionId;
        try {
            postJsonRpc(url, new JSONObject()
                    .put("jsonrpc", "2.0")
                    .put("method", "notifications/initialized")
                    .put("params", new JSONObject()), sessionId);
        } catch (Exception ignored) {
        }
        return sessionId;
    }

    private static JsonRpcResponse postJsonRpc(String url, JSONObject body, String sessionId) throws IOException {
        Headers.Builder headers = new Headers.Builder()
                .add("Content-Type", "application/json")
                .add("Accept", "application/json, text/event-stream");
        if (sessionId != null && !sessionId.trim().isEmpty()) {
            headers.add("Mcp-Session-Id", sessionId.trim());
        }
        Request request = new Request.Builder()
                .url(url)
                .headers(headers.build())
                .post(RequestBody.create(body.toString(), JSON_MEDIA_TYPE))
                .build();
        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            String raw = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + " - " + raw);
            }
            String nextSessionId = response.header("Mcp-Session-Id", sessionId == null ? "" : sessionId);
            return new JsonRpcResponse(parseJsonRpcBody(raw), nextSessionId);
        } catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException(e);
        }
    }

    private static JSONObject parseJsonRpcBody(String raw) throws Exception {
        String body = raw == null ? "" : raw.trim();
        if (body.startsWith("data:")) {
            String[] lines = body.split("\\r?\\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("data:")) {
                    String data = trimmed.substring(5).trim();
                    if (!data.isEmpty() && !"[DONE]".equals(data)) {
                        return new JSONObject(data);
                    }
                }
            }
        }
        return body.isEmpty() ? new JSONObject() : new JSONObject(body);
    }

    private static JSONArray firstArray(JSONObject object, String... keys) {
        for (String key : keys) {
            JSONArray array = object == null ? null : object.optJSONArray(key);
            if (array != null) {
                return array;
            }
        }
        return null;
    }

    private static JSONObject parseArgumentsObject(Object raw) {
        if (raw instanceof JSONObject) {
            return (JSONObject) raw;
        }
        if (raw == null || raw == JSONObject.NULL) {
            return new JSONObject();
        }
        try {
            return new JSONObject(String.valueOf(raw));
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }

    public static String addUniquePrefix(String serverName, String toolName) {
        String safeServer = slug(serverName);
        String safeTool = slug(toolName);
        if (safeServer.isEmpty()) {
            return safeTool;
        }
        return "mcp_" + safeServer + "_" + safeTool;
    }

    public static String removeMcpToolNamePrefix(String prefixedToolName) {
        if (prefixedToolName == null) {
            return "";
        }
        String value = prefixedToolName;
        if (value.startsWith("mcp_")) {
            value = value.substring(4);
        }
        int first = value.indexOf('_');
        return first >= 0 && first < value.length() - 1 ? value.substring(first + 1) : value;
    }

    private static String slug(String value) {
        return value == null ? "" : value.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }
}
