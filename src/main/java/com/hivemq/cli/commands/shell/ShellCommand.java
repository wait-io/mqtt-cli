/*
 * Copyright 2019 HiveMQ and the HiveMQ Community
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.hivemq.cli.commands.shell;

import com.hivemq.cli.DefaultCLIProperties;
import com.hivemq.cli.MqttCLIMain;
import com.hivemq.cli.utils.MqttUtils;
import org.jetbrains.annotations.NotNull;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;
import org.pmw.tinylog.LoggingContext;
import org.pmw.tinylog.labelers.TimestampLabeler;
import org.pmw.tinylog.policies.SizePolicy;
import org.pmw.tinylog.writers.ConsoleWriter;
import org.pmw.tinylog.writers.RollingFileWriter;
import picocli.CommandLine;
import picocli.shell.jline3.PicocliJLineCompleter;

import javax.inject.Inject;
import java.io.File;
import java.io.PrintWriter;

@CommandLine.Command(name = "shell", aliases = "sh",
        versionProvider = MqttCLIMain.CLIVersionProvider.class,
        description = "Starts MqttCLI in shell mode, to enable interactive mode with further sub commands.",
        footer = {"", "@|bold Press Ctl-C to exit.|@"},
        synopsisHeading = "%n@|bold Usage|@:  ",
        descriptionHeading = "%n",
        optionListHeading = "%n@|bold Options|@:%n",
        commandListHeading = "%n@|bold Commands|@:%n",
        separator = " ")

public class ShellCommand implements Runnable {

    private static final String DEFAULT_PROMPT = "mqtt> ";
    private static String prompt = DEFAULT_PROMPT;

    public static boolean DEBUG;
    public static boolean VERBOSE;
    private String logfilePath;

    public static PrintWriter TERMINAL_WRITER;

    private static LineReaderImpl currentReader;
    private static LineReaderImpl shellReader;
    private static LineReaderImpl contextReader;

    private static CommandLine currentCommandLine;
    private static CommandLine shellCommandLine;
    private static CommandLine contextCommandLine;

    private static boolean exitShell = false;

    private final DefaultCLIProperties defaultCLIProperties;

    @SuppressWarnings("NullableProblems")
    @CommandLine.Spec
    private @NotNull CommandLine.Model.CommandSpec spec;

    //needed for pico cli - reflection code generation
    public ShellCommand() {
        this(null);
    }

    @Inject
    ShellCommand(final @NotNull DefaultCLIProperties defaultCLIProperties) {
        this.defaultCLIProperties = defaultCLIProperties;
    }

    @CommandLine.Option(names = {"--version", "-V"}, versionHelp = true, description = "display version info")
    boolean versionInfoRequested;

    @CommandLine.Option(names = {"--help", "-h"}, usageHelp = true, description = "display this help message")
    boolean usageHelpRequested;

    @Override
    public void run() {

        final Level debugLevel = defaultCLIProperties.getShellDebugLevel();
        switch (debugLevel) {
            case TRACE:
                VERBOSE = true;
                DEBUG = false;
                break;
            case DEBUG:
                VERBOSE = false;
                DEBUG = true;
                break;
            case INFO:
                VERBOSE = false;
                DEBUG = false;
                break;
        }

        final String dir = defaultCLIProperties.getLogfilePath();

        final File dirFile = new File(dir);
        dirFile.mkdirs();

        final String logfileFormatPattern = "{date:yyyy-MM-dd HH:mm:ss}: {{level}:|min-size=6} {context:identifier}: {message}";

        final RollingFileWriter logfileWriter = new RollingFileWriter(dir + "hmq-cli.log", 30, false, new TimestampLabeler("yyyy-MM-dd"), new SizePolicy(1024 * 10));

        Configurator.defaultConfig()
                .writer(logfileWriter,
                        debugLevel,
                        logfileFormatPattern)
                .addWriter(new ConsoleWriter(),
                        Level.INFO,
                        "{message}")
                .activate();

        LoggingContext.put("identifier", "SHELL");

        logfilePath = logfileWriter.getFilename();

        if (VERBOSE) {
            Logger.trace("Command: {} ", this);
        }


        interact();
    }


    private void interact() {
        shellCommandLine = MqttCLIMain.MQTTCLI.shell();
        contextCommandLine = MqttCLIMain.MQTTCLI.shellContext();

        try {
            final Terminal terminal = TerminalBuilder.builder()
                    .name("MQTT Terminal")
                    .system(true)
                    .build();

            shellReader = (LineReaderImpl) LineReaderBuilder.builder()
                    .terminal(terminal)
                    .completer(new PicocliJLineCompleter(shellCommandLine.getCommandSpec()))
                    .parser(new DefaultParser())
                    .build();

            contextReader = (LineReaderImpl) LineReaderBuilder.builder()
                    .terminal(terminal)
                    .completer(new PicocliJLineCompleter(contextCommandLine.getCommandSpec()))
                    .parser(new DefaultParser())
                    .build();

            readFromShell();


            TERMINAL_WRITER = terminal.writer();
            TERMINAL_WRITER.println(shellCommandLine.getUsageMessage());
            TERMINAL_WRITER.flush();

            Logger.info("Using default values from properties file {}:", defaultCLIProperties.getFile().getPath());
            Logger.info("Host: {}, Port: {}, Mqtt-Version {}, Shell-Debug-Level: {}",
                    defaultCLIProperties.getHost(),
                    defaultCLIProperties.getPort(),
                    defaultCLIProperties.getMqttVersion(),
                    defaultCLIProperties.getShellDebugLevel());
            Logger.info("Writing Logfile to {}", logfilePath);

            String line;
            while (!exitShell) {
                try {
                    line = currentReader.readLine(prompt, null, (MaskingCallback) null, null);
                    final ParsedLine pl = currentReader.getParser().parse(line, prompt.length());
                    final String[] arguments = pl.words().toArray(new String[0]);
                    if (arguments.length != 0) {
                        currentCommandLine.execute(arguments);
                    }
                } catch (final UserInterruptException e) {
                    if (VERBOSE) {
                        Logger.trace("User interrupted shell: {}", e);
                    }
                    return;
                } catch (final EndOfFileException e) {
                    if (VERBOSE) {
                        Logger.trace(e);
                    }
                    else if (DEBUG) {
                        Logger.debug(e.getMessage());
                    }
                    Logger.error(MqttUtils.getRootCause(e).getMessage());
                    return;
                } catch (final Exception all) {
                    if (DEBUG) {
                        Logger.trace(all);
                    }
                    else if (VERBOSE) {
                        Logger.debug(all.getMessage());
                    }
                    Logger.error(MqttUtils.getRootCause(all).getMessage());
                }
            }
        } catch (final Throwable t) {
            if (VERBOSE) {
                Logger.trace(t);
            }
            else if (DEBUG) {
                Logger.debug(t.getMessage());
            }
            Logger.error(MqttUtils.getRootCause(t).getMessage());

        }
    }

    static void exitShell() {
        exitShell = true;
    }

    static void readFromContext() {
        currentReader = contextReader;
        currentCommandLine = contextCommandLine;
        prompt = new AttributedStringBuilder()
                .style(AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW))
                .append(ShellContextCommand.contextClient.getConfig().getClientIdentifier().get().toString())
                .style(AttributedStyle.DEFAULT)
                .append("@")
                .style(AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW))
                .append(ShellContextCommand.contextClient.getConfig().getServerHost())
                .style(AttributedStyle.DEFAULT)
                .append("> ")
                .toAnsi();
    }

    static void readFromShell() {

        currentReader = shellReader;
        currentCommandLine = shellCommandLine;
        prompt = new AttributedStringBuilder()
                .style(AttributedStyle.DEFAULT)
                .append(DEFAULT_PROMPT)
                .toAnsi();
    }


    static void usage(Object command) {
        currentCommandLine.usage(command, System.out);
    }

    static String getUsageMessage() {
        return currentCommandLine.getUsageMessage();
    }

    static void clearScreen() {
        currentReader.clearScreen();
    }

    static boolean isVerbose() {
        return VERBOSE;
    }

    static boolean isDebug() {
        return DEBUG;
    }

    @Override
    public String toString() {
        return "Shell:: {" +
                "logfilePath=" + logfilePath +
                ", debug=" + DEBUG +
                ", verbose=" + VERBOSE +
                "}";
    }


}
