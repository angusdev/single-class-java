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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple Command Line Interpreter.
 * 
 * @author http://twitter.com/angusdev
 * @version 20170322
 */
public class CLI {
    private static final Pattern SWITCH_PATTERN = Pattern.compile("^(\\-\\-?[^=]+)\\=?(.*)$");
    private String[] commandArgs;
    private String[] args;
    private Map<String, String> switches = new HashMap<String, String>();

    public CLI(String[] args) {
        this.commandArgs = args;

        List<String> argsList = new ArrayList<String>();
        for (int i = 0; i < args.length; i++) {
            String s = args[i];
            Matcher m = SWITCH_PATTERN.matcher(s);
            if (m.matches()) {
                switches.put(m.group(1), m.group(2));
            }
            else {
                argsList.add(s);
            }
        }
        this.args = argsList.toArray(new String[0]);
    }

    public String[] getCommandArgs() {
        return commandArgs;
    }

    public String[] getArgs() {
        return args;
    }

    public boolean contains(String shortSwitch, String... longSwitch) {
        return getSwitch(shortSwitch, longSwitch) != null;
    }

    public String getSwitch(String shortSwitch, String... longSwitch) {
        String value = switches.get("-" + shortSwitch);
        if (value != null) {
            return value;
        }
        for (int i = 0; longSwitch != null && i < longSwitch.length; i++) {
            value = switches.get("--" + longSwitch[i]);
            if (value != null) {
                return value;
            }
        }

        return null;
    }

    public static void main(String[] args) {
        String line = "--version -h --debug=true foo bar";
        CLI cli = new CLI(line.split(" "));
        System.out.println(line);
        printContainTest(cli, "v", "version");
        printContainTest(cli, "v", "verbose");
        printContainTest(cli, "h", "help");
        printContainTest(cli, "d", "debug");
        System.out.println("args.length : " + cli.getArgs().length);
        System.out.println("args[0] : " + cli.getArgs()[0]);
        System.out.println("args[1] : " + cli.getArgs()[1]);

        System.out.println();
        System.out.println();
        line = "foo --version -h bar --debug=true";
        cli = new CLI(line.split(" "));
        System.out.println(line);
        printContainTest(cli, "v", "version");
        printContainTest(cli, "v", "verbose");
        printContainTest(cli, "h", "help");
        printContainTest(cli, "d", "debug");
        System.out.println("args.length : " + cli.getArgs().length);
        System.out.println("args[0] : " + cli.getArgs()[0]);
        System.out.println("args[1] : " + cli.getArgs()[1]);
    }

    private static void printContainTest(CLI cli, String shortSwitch, String... longSwitch) {
        System.out.print("  contains -" + shortSwitch);
        for (int i = 0; longSwitch != null && i < longSwitch.length; i++) {
            System.out.print(" --" + longSwitch[i]);
        }
        String value = cli.getSwitch(shortSwitch, longSwitch);
        System.out.print(" : " + cli.contains(shortSwitch, longSwitch)
                + (value != null && value.length() > 0 ? " (" + value + ")" : ""));
        System.out.println();
    }
}
