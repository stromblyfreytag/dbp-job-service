package com.trustwave.dbpjobservice.workflow.light.db;

import java.util.List;

import com.googlecode.sarasvati.JoinType;
import com.googlecode.sarasvati.Node;
import com.googlecode.sarasvati.env.Env;
import com.trustwave.dbpjobservice.workflow.api.util.EscapingEncoder;

public class LightUtils {
    private static final char ENV_SEPARATOR_CHAR = ';';
    private static EscapingEncoder envEncoder =
            new EscapingEncoder(ENV_SEPARATOR_CHAR, '~');

    public static String encodeEnv(Env env) {
        if (env == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        EscapingEncoder encoder = getEnvEncoder();
        for (String name : env.getAttributeNames()) {
            String value = env.getAttribute(name);
            encoder.encode(name, sb);
            encoder.encode(value, sb);
        }
        encoder.finishEncode(sb);

        if (sb.length() == 0) {
            return null;
        }
        return sb.toString();
    }

    public static void decodeEnvString(String envStr, Env env) {
        if (envStr != null) {
            List<String> keyvalues = getEnvEncoder().decode(envStr);
            final int size = keyvalues.size();
            for (int i = 0; i < size - 1; ++i) {
                String key = keyvalues.get(i);
                String value = keyvalues.get(++i);
                env.setAttribute(key, value);
            }
        }
    }

    public static EscapingEncoder getEnvEncoder() {
        return envEncoder;
    }

    public static String mergeEnvStrings(String env1, String env2) {
        if (env1 == null || env1.isEmpty()) {
            return env2;
        }
        if (env2 == null || env2.isEmpty()) {
            return env1;
        }
        return env1 + ENV_SEPARATOR_CHAR + env2;
    }

    public static boolean isTokenSetMergeNode(Node node) {
		if (JoinType.TOKEN_SET.equals(node.getJoinType())) {
			return true;
		}
		if (JoinType.TOKEN_SET_OR.equals(node.getJoinType())) {
			return true;
		}
        return false;
    }

    /**
     * Returns true if node is a merge node for the specified token set name
     */
    public static boolean isTokenSetMergeNode(Node node, String tokenSetName) {
        if (!isTokenSetMergeNode(node)) {
            return false;
        }
        return tokenSetName.equals(node.getJoinParam());
    }
}
