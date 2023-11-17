package com.trustwave.dbpjobservice.workflow.api.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

//TODO this is actually from dbp-jobtask-api, and should be moved there
public class EscapingEncoder {
    private final char separatorChar;
    private final char escapeChar;
    private final char nullChar;

    public EscapingEncoder(char separatorChar, char escapeChar, char nullChar) {
        if (separatorChar == escapeChar || separatorChar == nullChar || escapeChar == nullChar) {
            throw new IllegalArgumentException("use different separator, escape, and null chars");
        }
        this.separatorChar = separatorChar;
        this.escapeChar = escapeChar;
        this.nullChar = nullChar;
    }

    public EscapingEncoder(char separatorChar, char escapeChar) {
        this(separatorChar, escapeChar, (char) 1);
    }

    public String encode(Collection<String> strList) {
        if (strList == null || strList.size() == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();

        for (String s : strList) {
            encode(s, sb);
        }
        return finishEncode(sb);
    }

    public String encode(String s) {
        StringBuilder sb = new StringBuilder();
        encode(s, sb);
        return finishEncode(sb);
    }

    public void encode(String s, StringBuilder sb) {
        if (s == null) {
            sb.append(nullChar);
            sb.append(separatorChar);
            return;
        }

        final char[] chars = s.toCharArray();
        int i0 = 0;
        int i;

        for (i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == separatorChar || c == escapeChar || c == nullChar) {
                if (i > i0) {
                    sb.append(chars, i0, i - i0);
                    i0 = i;
                }
                sb.append(escapeChar);
            }
        }
        if (i > i0) {
            sb.append(chars, i0, i - i0);
        }
        sb.append(separatorChar);
    }

    public String finishEncode(StringBuilder sb) {
        int ind = sb.length() - 1;
        if (ind >= 0 && sb.charAt(ind) == separatorChar) {
            sb.deleteCharAt(ind);
        }
        return sb.toString();
    }

    public List<String> decode(String str) {
        List<String> list = new ArrayList<String>();
        if (str == null) {
            return list;
        }

        final char[] chars = str.toCharArray();
        StringBuilder sb = new StringBuilder();
        boolean nullSring = false;

        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == escapeChar) {
                ++i;
                if (i < chars.length) { // that should be true, just in case
                    sb.append(chars[i]);
                }
            }
            else if (chars[i] == nullChar) {
                if (sb.length() == 0   // should be true, just in case
                        && (i + 1 == chars.length || chars[i + 1] == separatorChar)) {
                    nullSring = true;
                }
            }
            else if (chars[i] == separatorChar) {
                list.add(nullSring ? null : sb.toString());
                nullSring = false;
                sb = new StringBuilder();
            }
            else {
                sb.append(chars[i]);
            }
        }
        list.add(nullSring ? null : sb.toString());

        return list;
    }

    public char getSeparatorChar() {
        return separatorChar;
    }

    public char getEscapeChar() {
        return escapeChar;
    }

    public char getNullChar() {
        return nullChar;
    }

}
