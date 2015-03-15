package com.example.manuel.bigbrotha;

import java.util.regex.Pattern;

/**
 * This class is used to find if an IPV4 address is valid and has a pattern or x.x.x.x
 * Regex found online.
 */
public class ValidateIP
{

    static private final String IPV4_REGEX = "(([0-1]?[0-9]{1,2}\\.)|(2[0-4][0-9]\\.)|(25[0-5]\\.)){3}(([0-1]?[0-9]{1,2})|(2[0-4][0-9])|(25[0-5]))";
    static private Pattern IPV4_PATTERN = Pattern.compile(IPV4_REGEX);

    /**
     * Method used to match a string to the regex
     * @param s  string to be matched to the regex
     * @return  true if its a valid address, false it not
     */
    public static boolean isValidIPV4(final String s)
    {
        return IPV4_PATTERN.matcher(s).matches();
    }
}