package de.lessvoid.nifty.controls;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.input.NiftyInputEvent;
import de.lessvoid.nifty.input.NiftyStandardInputEvent;
import de.lessvoid.nifty.screen.KeyInputHandler;

/**
 * This adds all the nifty command line features to the console control:
 * - command line completion (for all registered commands)
 * - command history
 * - command processing which detects commands and directly calls your registered commands (via the ConsoleCommand interface)
 * @author void
 */
public class ConsoleCommands implements KeyInputHandler {
  private boolean commandCompletion = false;

  private ConsoleCommandSplitter splitter = new ConsoleCommandSplitter();

  private Map<String, ConsoleCommand> commands = new TreeMap<String, ConsoleCommand>();
  private List<String> commandHistory = new ArrayList<String>();
  private int commandHistoryLastCommand = -1;

  private Nifty nifty;
  private Console console;
  private TextField textfield;

  /**
   * You can implement this interface for individual commands and Nifty will call them
   * when the registered command has been detected.
   * @author void
   */
  public interface ConsoleCommand {
    /**
     * Execute the command. You'll get an array of all parameters. This works the
     * same as with java main, which means the first entry in the array will be the
     * command and all other array entries are the actual parameters.
     * @param args command and arguments
     */
    void execute(String[] args);
  }

  /**
   * Create and attach this to the given console.
   * @param console
   */
  public ConsoleCommands(final Nifty nifty, final Console console) {
    this.nifty = nifty;
    this.console = console;
    this.textfield = console.getTextField();
    this.textfield.getElement().addPreInputHandler(this);
  }

  /**
   * When the command completion is enabled pressing TAB will check for all known
   * commands. Use registerCommand() to register commands for this feature to work.
   * Please note that in this case you can't TAB away from the textfield. 
   * @param enabled true when command completion should be enabled and false if not
   */
  public void enableCommandCompletion(final boolean enabled) {
    commandCompletion = enabled;
  }

  /**
   * Register a command for the command completion feature.
   * @param command the command to make known
   */
  public void registerCommand(final String commandText, final ConsoleCommand command) {
    commands.put(commandText, command);
  }

  /**
   * Get all commands that are registered. 
   * @return list of all commands that have been registered
   */
  public List<String> getRegisteredCommands() {
    return Collections.unmodifiableList(Arrays.asList(commands.keySet().toArray(new String[0])));
  }
  
  @Override
  public boolean keyEvent(final NiftyInputEvent inputEvent) {
    if (!commandCompletion) {
      return false;
    }
    if (NiftyStandardInputEvent.NextInputElement.equals(inputEvent)) {
      List<String> matches = findMatches(textfield.getText());
      if (matches.size() == 1) {
        changeText(matches.get(0));
        return true;
      } else if (matches.size() > 1) {
        String shortest = findShortestMatch(matches);
        if (shortest.length() == textfield.getText().length()) {
          StringBuffer buffer = new StringBuffer("\n");
          for (String match : matches) {
            buffer.append("\\#cccf#" + match + "\n");
          }
          console.output(buffer.toString());
        }
        changeText(shortest);
      }
      return true;
    } else if (NiftyStandardInputEvent.MoveCursorUp.equals(inputEvent)) {
      if (commandHistoryLastCommand > 0) {
        commandHistoryLastCommand--;
        changeText(commandHistory.get(commandHistoryLastCommand));
      }
      return true;
    } else if (NiftyStandardInputEvent.MoveCursorDown.equals(inputEvent)) {
      if (commandHistoryLastCommand < commandHistory.size() - 1) {
        commandHistoryLastCommand++;
        changeText(commandHistory.get(commandHistoryLastCommand));
      } else {
        commandHistoryLastCommand = commandHistory.size();
        changeText("");
      }
      return true;
    } else if (NiftyStandardInputEvent.SubmitText.equals(inputEvent)) {
      String text = textfield.getText();
      console.output(text);
      textfield.setText("");

      // find command
      String[] split = splitter.split(text);
      if (split.length != 0) {
        String command = split[0];

        // is there a command that starts with this "command" string?
        for (Map.Entry<String, ConsoleCommand> registeredCommand : commands.entrySet()) {
          String[] s = registeredCommand.getKey().split(" ");
          if (s.length != 0) {
            String start = s[0];
            if (command.equals(start)) {
              ConsoleCommand consoleCommand = registeredCommand.getValue();
              consoleCommand.execute(split);
              addCommandToHistory(text);
              return true;
            }
          }
        }
      }

      console.outputError("Unknown command: " + text);

      // this means we have not found an appropriate command in the registered commands
      // we'll publish this now as the original console would do. this way you can still
      // subscribe for it (if you need to).
      nifty.publishEvent(console.getId(), new ConsoleExecuteCommandEvent(console, text));
      addCommandToHistory(text);
      return true;
    }

    return false;
  }

  /**
   * Find a ConsoleCommand with the given commandText. This will find commands that
   * begin with the given commandText as well.
   *
   * @param commandText the command to return
   * @return the ConsoleCommand or null if command does not exist
   */
  public ConsoleCommand findCommand(final String commandText) {
    ConsoleCommand command = commands.get(commandText);
    if (command != null) {
      return command;
    }
    List<String> commandMatches = findMatches(commandText);
    if (commandMatches.size() == 1) {
      return commands.get(commandMatches.get(0));
    }
    return null;
  }

  private void changeText(final String newText) {
    textfield.setText(newText);
    textfield.setCursorPosition(textfield.getText().length());
  }

  private void addCommandToHistory(final String text) {
    commandHistory.add(text);
    commandHistoryLastCommand = commandHistory.size();
  }

  List<String> findMatches(final String text) {
    List<String> result = new ArrayList<String>();
    if (text == null || text.length() == 0) {
      return result;
    }
    for (String command : commands.keySet()) {
      if (command.equals(text)) {
        result.add(command);
      } else if (command.startsWith(text)) {
        result.add(command);
      }
    }
    return result;
  }

  /**
   * We know that all Strings in the given List start with the same string
   * that is at least one char long. This method will find the longest
   * substring that exists in all of the Strings in the List.
   */
  String findShortestMatch(final List<String> matches) {
    // step 1: first longest string
    String longest = "";
    for (String match : matches) {
      if (match.length() > longest.length()) {
        longest = match;
      }
    }

    // step 2: scan the longest string char by char and check if it is
    // contained in all string of the list
    String lastCheck = longest.substring(0, 1);

    for (int i=1; i<longest.length(); i++) {
      String check = longest.substring(0, i);
      for (String match : matches) {
        if (!match.startsWith(check)) {
          return lastCheck;
        }
      }
      // woo hoo they all match, we can update lastCheck
      lastCheck = check;
    }

    return lastCheck;
  }

}
