package com.example.manuel.bigbrotha;

import java.util.regex.Pattern;

/**
 *
 */
public class ValidateIP
{

    static private final String IPV4_REGEX = "(([0-1]?[0-9]{1,2}\\.)|(2[0-4][0-9]\\.)|(25[0-5]\\.)){3}(([0-1]?[0-9]{1,2})|(2[0-4][0-9])|(25[0-5]))";
    static private Pattern IPV4_PATTERN = Pattern.compile(IPV4_REGEX);

    public static boolean isValidIPV4(final String s)
    {
        return IPV4_PATTERN.matcher(s).matches();
    }
}