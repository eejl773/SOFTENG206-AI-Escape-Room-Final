package nz.ac.auckland.se206.controllers.puzzles;

import java.lang.reflect.Field;
import java.util.List;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import nz.ac.auckland.se206.App;
import nz.ac.auckland.se206.HintManager;
import nz.ac.auckland.se206.SceneManager.AppUi;
import nz.ac.auckland.se206.constants.Algorithm;
import nz.ac.auckland.se206.constants.Description;
import nz.ac.auckland.se206.constants.GameState;
import nz.ac.auckland.se206.constants.GameState.Difficulty;
import nz.ac.auckland.se206.constants.Sequence;
import nz.ac.auckland.se206.gpt.ChatMessage;
import nz.ac.auckland.se206.gpt.GptPromptEngineering;
import nz.ac.auckland.se206.gpt.openai.ChatCompletionRequest;
import nz.ac.auckland.se206.gpt.openai.ChatCompletionResult;
import nz.ac.auckland.se206.gpt.openai.ChatCompletionResult.Choice;
import nz.ac.auckland.se206.speech.TextToSpeech;
import nz.ac.auckland.se206.utilities.Timer;

/** Controller class for the decryption puzzle scene. */
public class DecryptionPuzzleController {
  @FXML private Pane paBack;
  @FXML private Pane paDigit0;
  @FXML private Pane paDigit1;
  @FXML private Pane paDigit2;
  @FXML private Pane paDigit3;
  @FXML private Pane paDecryption;
  @FXML private Pane paBackOverlay;

  @FXML private Label lblTime;
  @FXML private Label lblDigit0;
  @FXML private Label lblDigit1;
  @FXML private Label lblDigit2;
  @FXML private Label lblDigit3;
  @FXML private Label lblHintCounter;

  @FXML private Polygon pgHint;

  @FXML private TextArea taChat;
  @FXML private TextArea taPseudocode;

  @FXML private TextField tfChat;

  private int hintIndex;
  private int psuedocodeIndex;

  private String sequence;
  private String algorithm;
  private String pseudocode;
  private String description;

  private ChatCompletionRequest gptRequest;

  private TextToSpeech tts;

  /** Initializes the decryption puzzle. */
  @FXML
  private void initialize() throws Exception {
    // get the game state instance of tts
    this.tts = GameState.tts;

    // Add the label to list of labels to be updated
    Timer.addLabel(lblTime);

    // Add the hint counter components
    HintManager.addHintComponents(lblHintCounter, pgHint);

    // Initialize the decryption puzzle chats and algorithms
    initializeChat();
    initializePseudocode();
  }

  /** When the mouse is hovering over the pane, the overlay appears (hint). */
  @FXML
  private void onHintEntered() {
    pgHint.setOpacity(0.25);
  }

  /** When the mouse is not hovering over the pane, the overlay disappears (hint). */
  @FXML
  private void onHintExited() {
    pgHint.setOpacity(0);
  }

  /** When the mouse is hovering over the pane, the overlay appears (back). */
  @FXML
  private void onBackPaneEntered() {
    paBackOverlay.setVisible(true);
  }

  /** When the mouse is not hovering over the pane, the overlay disappears (back). */
  @FXML
  private void onBackPaneExited() {
    paBackOverlay.setVisible(false);
  }

  /** When hint is clicked, give the user a hint. */
  @FXML
  private void onHintClicked() {
    // If the difficulty is hard, ignore user.
    if (GameState.gameDifficulty == Difficulty.HARD) {
      return;
    }

    getUserHint();
  }

  /** When back is clicked, go back to previous section (control room). */
  @FXML
  private void onBackPaneClicked() {
    App.setUi(AppUi.TERMINAL);
  }

  /**
   * Check if there is a keyboard event. If there is a keyboard event, handle the event
   * appropriately.
   *
   * @param keyEvent this event is generated when a key is pressed, released, or typed
   */
  @FXML
  private void onKeyPressed(KeyEvent keyEvent) {
    String userInput = "";

    // get the user input from the chat text field
    if (keyEvent.getCode() == KeyCode.ENTER) {
      userInput = tfChat.getText();
    }

    // trim the user input
    userInput = userInput.trim();

    // check if the user input is empty
    if (userInput == null || userInput.isEmpty()) {
      return;
    }

    // the user has entered a sequence and the puzzle is not solved
    if (isUserInputSequence(userInput) && !GameState.isDecryptionSolved) {
      handleUserSequence(userInput);
      return;
    }

    // initialize user chat message object
    ChatMessage userMessage;

    // create a new instance of user chat message object
    userMessage = new ChatMessage("user", userInput);

    // append the user's response to the text area
    setUserResponse(userInput);

    // get chatGPT's response and append it to the chatting text area
    getChatResponse(userMessage, false);
  }

  /**
   * Initialize GPT. Set the tokens and create a new instance of GPT request.
   *
   * <p>Note: I would love to be able to name this method 'initializeGPT'. Unfortunately, we are not
   * allowed to have acronyms as method names as per the naming convention.
   */
  private void initializeChat() {
    // initialize the chat message field
    ChatMessage gptMessage;

    // initialize GPT chat message object
    gptMessage = new ChatMessage("assistant", GptPromptEngineering.initializeDecryptionResponse());

    // initialize an instance of GPT request
    gptRequest = new ChatCompletionRequest();

    // set the 'n' parameter for the request -> has to be '1'
    gptRequest.setN(1);

    // set the temperature for the request -> [0,2]
    gptRequest.setTemperature(1.4);

    // set the 'top p' value for the request -> [0,1]
    gptRequest.setTopP(0.5);

    // set the max tokens -> has to be at least '1'
    gptRequest.setMaxTokens(100);

    // get a response from GPT to setup the chat
    getChatResponse(gptMessage, false);
  }

  /**
   * Initialize pseudocode instances. The method should get a 'random' pseudocode each round.
   *
   * @throws Exception thrown when there is an error initializing pseudocode instances.
   */
  private void initializePseudocode() throws Exception {
    // Get a random pseudo code
    psuedocodeIndex = (int) (Math.random() * (GameState.maxPseudocodes));

    System.out.println("Current psuedocode: " + psuedocodeIndex);

    // Hint index is initially zero
    hintIndex = 0;

    // Initialize the sequence
    intializeSequence();

    // Initialize the description and algorithm
    initializeDescription();
    initializeAlgorithm();

    // Append the description and algorithm to the text area
    taPseudocode.appendText(description);
    taPseudocode.appendText(algorithm);

    // Get the pseudocode in string form
    pseudocode = description + algorithm;
  }

  /**
   * Get the string sequence for the corresponding random pseudocode index.
   *
   * @return the string value of the sequence.
   * @throws Exception throw when class or field name is not found.
   */
  private void intializeSequence() throws Exception {
    // get the field name for the corresponding random pseudocode index
    String fieldName = "sequence" + Integer.toString(psuedocodeIndex);

    // create an object of 'Sequence'
    Sequence sequence = new Sequence();

    // create a runtime reference to the class, 'Sequence'
    Class<?> cls = sequence.getClass();

    // get the field for the corresponding field name
    Field fld = cls.getField(fieldName);

    // retrieve the object value from the field and cast it to string
    this.sequence = (String) fld.get(sequence);
  }

  /**
   * Get the string algorithm code snippet for the corresponding random pseudocode index.
   *
   * @return the string value of the algorithm code snippet.
   * @throws Exception throw when class or field name is not found.
   */
  private void initializeAlgorithm() throws Exception {
    // get the field name for the corresponding random pseudocode index
    String fieldName = "algorithm" + Integer.toString(psuedocodeIndex);

    // create an object of 'Algorithm'
    Algorithm algorithm = new Algorithm();

    // create a runtime reference to the class, 'Algorithm'
    Class<?> cls = algorithm.getClass();

    // get the field for the corresponding field name
    Field fld = cls.getField(fieldName);

    // retrieve the object value from the field and cast it to string
    this.algorithm = (String) fld.get(algorithm);
  }

  /**
   * Get the string description for the corresponding random pseudocode index.
   *
   * @return the string value of the description.
   * @throws Exception throw when class or field name is not found.
   */
  private void initializeDescription() throws Exception {
    // get the field name for the corresponding random pseudocode index
    String fieldName = "description" + Integer.toString(psuedocodeIndex);

    // create an object of 'Description'
    Description description = new Description();

    // create a runtime reference to the class, 'Description'
    Class<?> cls = description.getClass();

    // get the field for the corresponding field name
    Field fld = cls.getField(fieldName);

    // retrieve the object value from the field and cast it to string
    this.description = (String) fld.get(description);
  }

  /**
   * Generate a response from GPT.
   *
   * @param entityMessage the chat message to be sent to GPT.
   */
  private void getChatResponse(ChatMessage entityMessage, boolean isHint) {
    // add user input to GPT's user input history
    gptRequest.addMessage(entityMessage);

    // create a concurrent task for handling GPT response
    Task<Void> gptTask =
        new Task<Void>() {
          @Override
          protected Void call() throws Exception {
            // Set GPT's response
            setChatResponse();
            return null;
          }
        };

    // If the task is running, disable certain components
    gptTask.setOnRunning(
        event -> {
          disableComponents();
        });

    // If the task succeeds, then enable components
    gptTask.setOnSucceeded(
        event -> {
          enableComponents();

          // Remove previous message if it is a hint
          if (isHint) {
            removePreviousMessage();
          }
        });

    // Create a thread to handle GPT concurrency
    Thread gptThread = new Thread(gptTask);

    // Start the thread
    gptThread.start();
  }

  /**
   * @param userInput the user's input from the text field.
   * @return the user's sequence.
   */
  private String getUserSequence(String userInput) {
    // Initialize fields
    String userSequence;
    int position;

    // Stop at the first digit
    for (position = 0; position < userInput.length(); position++) {
      if (Character.isDigit(userInput.charAt(position))) {
        break;
      }
    }

    // Get the next four characters from the user's input
    userSequence = userInput.substring(position, position + GameState.maxSequence);

    return userSequence;
  }

  /** Generate a GPT response. GPT should give a hint for the current pseudocode. */
  private void getUserHint() {
    // Update the hint counter
    HintManager.updateHintCounter();

    // Get the incorrect line number for the following pseudocode and hint index
    int lineNumber = Integer.valueOf(sequence.charAt(hintIndex));

    // Get the hint prompt for GPT to analyze and generate a response
    String hint = GptPromptEngineering.getDecryptionHint(pseudocode, lineNumber);

    // Initialize a user hint message compatible for GPT to analyze
    ChatMessage userHintMessage = new ChatMessage("assistant", hint);

    // Get GPT's response
    getChatResponse(userHintMessage, true);

    // Update the hint index
    hintIndex = (hintIndex + 1) % GameState.maxSequence;
  }

  /**
   * Set the chat response from GPT. This includes printing the response to the text area.
   *
   * @throws Exception thrown when we fail to retrieve a response from GPT.
   */
  private void setChatResponse() throws Exception {
    // Get GPT's response
    ChatCompletionResult gptResult = gptRequest.execute();

    // Get GPT's choice
    Choice gptChoice = gptResult.getChoices().iterator().next();

    // Get GPT's chat message
    ChatMessage gptMessage = gptChoice.getChatMessage();

    // Get the content of gpt's message in the form of a string
    String gptOutput = gptMessage.getContent();

    // Append the result to the text area
    taChat.appendText("ai> " + gptOutput + "\n\n");

    // Make text-to-speech read GPT's output
    tts.speak(gptOutput, AppUi.DECRYPTION);
  }

  /**
   * Set the user's response. The user's response will be appended to the chat.
   *
   * @param userInput the user input from the text field.
   */
  private void setUserResponse(String userInput) {
    // append the text to the chat text area
    taChat.appendText("user> " + userInput + "\n\n");

    // clear the text field user input
    tfChat.clear();
  }

  /**
   * Set the digit for the given user sequence; for the corresponding index.
   *
   * @param userSequence the user's sequence.
   * @param index the index of the digit to be updated.
   */
  private void setDigit(String userSequence, int index) {
    // Get the current scene
    Scene currentScene = paDecryption.getScene();

    // Get the digit label associated with the index
    Label lblDigit = (Label) currentScene.lookup("#lblDigit" + String.valueOf(index));

    // Update the previous digit to the current one
    lblDigit.setText(String.valueOf(userSequence.charAt(index)));
  }

  /**
   * Set the pane color to green if the user correctly identifies the sequence.
   *
   * @param index the index of the pane to be updated.
   */
  private void setDigitPaneColor(int index) {
    // Get the current scene
    Scene currentScene = paDecryption.getScene();

    // Get the digit label associated with the index
    Pane paDigit = (Pane) currentScene.lookup("#paDigit" + String.valueOf(index));

    // Set the style class to green
    paDigit.getStyleClass().add("green-border-pane");
  }

  /**
   * Set the label color to green if the user correctly identifies the sequence.
   *
   * @param index the index of the digit to be updated.
   * @param color the color to be set for the label.
   */
  private void setDigitLabelColor(int index, Color color) {
    // Get the current scene
    Scene currentScene = paDecryption.getScene();

    // Get the digit label associated with the index
    Label lblDigit = (Label) currentScene.lookup("#lblDigit" + String.valueOf(index));

    // Update the digit to the input color
    lblDigit.setTextFill(color);
  }

  /**
   * Handle the user's sequence. This should handle both cases where the user either correctly
   * determines the sequence or they incorrectly identify the sequence. Updating the sequence labels
   * are also done through this method.
   *
   * @param userInput the user's input from the text field.
   */
  private void handleUserSequence(String userInput) {
    // Get the user's sequence
    String userSequence = getUserSequence(userInput);

    // Check if the user sequence is correct or not
    if (userSequence.equals(sequence)) {
      handleCorrectSequence(userSequence);
    } else {
      handleIncorrectSequence(userSequence);
    }

    // Update the user's sequence on the red boxes
    updateUserSequence(userSequence);

    // Set the user's response - sequence to the text area
    setUserResponse(userSequence);
  }

  /**
   * Handle the case where the user correctly identifies the sequence.
   *
   * @param userSequence the user's sequence.
   */
  private void handleCorrectSequence(String userSequence) {
    System.out.println("SEQUENCE IS CORRECT");

    // Update the labels and panes to green color
    for (int index = 0; index < GameState.maxSequence; index++) {
      setDigitPaneColor(index);
      setDigitLabelColor(index, Color.GREEN);
    }

    // The user has solved the puzzle
    GameState.isDecryptionSolved = true;
  }

  /**
   * Handle the case where the user incorrectly identifies the sequence.
   *
   * @param userSequence the user's sequence.
   */
  private void handleIncorrectSequence(String userSequence) {
    System.out.println("SEQUENCE IS INCORRECT");
  }

  /**
   * Update the user's sequence. This should update the previous displayed user sequence.
   *
   * @param userSequence the user's sequence.
   */
  private void updateUserSequence(String userSequence) {
    for (int index = 0; index < GameState.maxSequence; index++) {
      setDigit(userSequence, index);
    }
  }

  /** Enable components when a task is finished. */
  private void enableComponents() {
    // Enable the user input field
    tfChat.setDisable(false);

    // Enable the hint pane
    pgHint.setDisable(false);
  }

  /** disable components when a task is running. */
  private void disableComponents() {
    // Disable the user input field
    tfChat.setDisable(true);

    // Disable the hint pane
    pgHint.setDisable(true);
  }

  /**
   * Remove the previous message in GPT's history. This is to ensure that the users won't be able to
   * abuse the hint system.
   */
  private void removePreviousMessage() {
    // Get the current GPT messages
    List<ChatMessage> gptMessages = gptRequest.getMessages();

    // Remove last message sent
    gptMessages.remove(gptMessages.size() - 1);
  }

  /**
   * Check if the user's input has contains a four digit number. If it does, then handle it in
   * another function call.
   *
   * @param userInput the user's input from the text field.
   * @return a boolean based on whether the user's input is a sequence.
   */
  private Boolean isUserInputSequence(String userInput) {
    // Check if there are only four digits in the user's input
    if (!isUserSequenceFourDigits(userInput)) {
      return false;
    }

    // Check if the digits are contiguous in the user's input
    if (!isUserSequenceContiguous(userInput)) {
      return false;
    }

    return true;
  }

  /**
   * Check whether the user's input contains four digits or not.
   *
   * @param userInput
   * @return a boolean value based on whether the user's input contains four digits.
   */
  private Boolean isUserSequenceFourDigits(String userInput) {
    int count = 0;

    // Go through the user's input and check if there are only four digits
    for (int i = 0; i < userInput.length(); i++) {
      if (Character.isDigit(userInput.charAt(i))) {
        count++;
      }
    }

    return (count == GameState.maxSequence ? true : false);
  }

  /**
   * Check whether there exists four contiguous digits in the user's input.
   *
   * @param userInput the user's input from the text field.
   * @return a boolean value based on whether there are four contiguous digits.
   */
  private Boolean isUserSequenceContiguous(String userInput) {
    // Initialize fields
    String userSequence;
    int position;

    // Stop at the first digit
    for (position = 0; position < userInput.length(); position++) {
      if (Character.isDigit(userInput.charAt(position))) {
        break;
      }
    }

    // Get the next four characters from the user's input
    userSequence = userInput.substring(position, position + GameState.maxSequence);

    return isUserSequenceNumeric(userSequence);
  }

  /**
   * Check if the user's sequence is numeric.
   *
   * @param userSequence the user's sequence.
   * @return a boolean value whether the user's sequence is a valid four digit number.
   */
  private Boolean isUserSequenceNumeric(String userSequence) {
    return userSequence.matches("-?\\d+(\\.\\d+)?");
  }
}
