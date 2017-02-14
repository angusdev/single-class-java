/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
import java.util.ArrayList;
import java.util.List;

/**
 * Wrap a long string to multiple lines.
 *
 * <p>
 * modified from org.apache.commons.lang3.text.WordUtils.wrap
 * </p>
 * 
 * @author http://twitter.com/angusdev
 * @version 1.0
 */
public class WrapWord {
    private static List<String> wrapWord(String str, int length) {
        if (str == null) {
            return null;
        }

        ArrayList<String> result = new ArrayList<String>();
        boolean wrapLongWords = true;

        final int inputLineLength = str.length();
        int offset = 0;
        final StringBuilder line = new StringBuilder(inputLineLength + 32);

        while (inputLineLength - offset > length) {
            if (str.charAt(offset) == ' ') {
                offset++;
                continue;
            }

            int spaceToWrapAt = str.lastIndexOf(' ', length + offset);
            int hyphenToWrapAt = str.lastIndexOf('-', length + offset - 1);
            int bestToWrapAt = Math.max(spaceToWrapAt, hyphenToWrapAt);
            if (spaceToWrapAt >= offset || hyphenToWrapAt >= offset) {
                // normal case
                line.append(str.substring(offset, bestToWrapAt));
                if (hyphenToWrapAt == bestToWrapAt) {
                    line.append('-');
                }
                result.add(line.toString());
                line.setLength(0);
                offset = bestToWrapAt + 1;
            }
            else {
                // really long word or URL
                if (wrapLongWords) {
                    // wrap really long word one line at a time
                    line.append(str.substring(offset, length + offset));
                    result.add(line.toString());
                    line.setLength(0);
                    offset += length;
                }
                else {
                    // do not wrap really long word, just extend beyond limit
                    spaceToWrapAt = str.indexOf(' ', length + offset);
                    if (spaceToWrapAt >= 0) {
                        line.append(str.substring(offset, spaceToWrapAt));
                        result.add(line.toString());
                        line.setLength(0);
                        offset = spaceToWrapAt + 1;
                    }
                    else {
                        line.append(str.substring(offset));
                        offset = inputLineLength;
                    }
                }
            }
        }

        line.append(str.substring(offset));
        if (line.length() > 0) {
            result.add(line.toString());
        }

        return result;
    }

    public static void main(String[] args) {
        for (String s : WrapWord.wrapWord("This is a really really long string", 7)) {
            System.out.println(s);
        }
    }
}
