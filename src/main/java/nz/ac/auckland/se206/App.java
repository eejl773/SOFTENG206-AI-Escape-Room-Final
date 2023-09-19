package nz.ac.auckland.se206;

import java.io.IOException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import nz.ac.auckland.se206.SceneManager.AppUi;
import nz.ac.auckland.se206.constants.GameState;
import nz.ac.auckland.se206.speech.TextToSpeech;
import nz.ac.auckland.se206.utilities.KeyEventsHandler;
import nz.ac.auckland.se206.utilities.Timer;

/**
 * This is the entry point of the JavaFX application, while you can change this class, it should
 * remain as the class that runs the JavaFX application.
 */
public class App extends Application {

  private static Scene scene;

  public static void main(final String[] args) {
    launch();
  }

  public static void setRoot(String fxml) throws IOException {
    scene.setRoot(loadFxml(fxml));
  }

  // input enum for the ui the app is changing to
  public static void setUi(AppUi newUi) {
    // scene.setRoot
    // get the Parent for that Ui

    // if not in one of the main rooms
    if (GameState.currentRoom != AppUi.OFFICE
        && GameState.currentRoom != AppUi.BREAKER
        && GameState.currentRoom != AppUi.CONTROL) {
      GameState.tts.stop();
    }

    scene.setRoot(SceneManager.getUi(newUi));
    GameState.currentRoom = newUi;
  }

  /**
   * Returns the node associated to the input file. The method expects that the file is located in
   * "src/main/resources/fxml".
   *
   * @param fxml The name of the FXML file (without extension).
   * @return The node of the input file.
   * @throws IOException If the file is not found.
   */
  private static Parent loadFxml(final String fxml) throws IOException {
    return new FXMLLoader(App.class.getResource("/fxml/" + fxml + ".fxml")).load();
  }

  /**
   * Loads the requested font. The method expects that the file is located in
   * "src/main/resources/fonts".
   *
   * @param font the name of the font file (without extension).
   * @param size the size of the font.
   */
  private static void loadFont(final String font, final String extension, final int size) {
    Font.loadFont(App.class.getResourceAsStream("/fonts/" + font + "." + extension), size);
  }

  /**
   * This method initialises the breaker scene
   *
   * @throws IOException
   */
  public static void initializeBreakerScene() throws IOException {
    SceneManager.addAppUi(AppUi.BREAKER, loadFxml("rooms/breaker"));
  }

  /**
   * This method will initialize the control scene
   *
   * @throws IOException
   */
  public static void initializeControlScene() throws IOException {
    SceneManager.addAppUi(AppUi.CONTROL, loadFxml("rooms/control"));
  }

  /**
   * This method will initialize the office scene
   *
   * @throws IOException
   */
  public static void initializeOfficeScene() throws IOException {
    SceneManager.addAppUi(AppUi.OFFICE, loadFxml("rooms/office"));
  }

  /*
   * This method will initalize the scenes, by storing instatnces of the loaded fxmls in SceneManager
   * @throws IOException if fxml is not found
   */
  private static void initalizeScenes() throws IOException {
    // initialize the timer
    Timer.initialize();

    // main menue
    SceneManager.addAppUi(AppUi.MENU, loadFxml("menus/menu"));

    // options in main menu
    SceneManager.addAppUi(AppUi.OPTIONS, loadFxml("menus/options"));

    // terminal screen
    SceneManager.addAppUi(AppUi.TERMINAL, loadFxml("menus/terminal"));

    // winning screen
    SceneManager.addAppUi(AppUi.WINNING, loadFxml("menus/winning"));

    // losing screen
    SceneManager.addAppUi(AppUi.LOSING, loadFxml("menus/losing"));

    // remaining puzzle scenes
    initalizePuzzleScenes();
  }

  protected static void initalizePuzzleScenes() throws IOException {
    // office room
    SceneManager.addAppUi(AppUi.OFFICE, loadFxml("rooms/office"));

    // control room
    SceneManager.addAppUi(AppUi.CONTROL, loadFxml("rooms/control"));

    // breaker room
    SceneManager.addAppUi(AppUi.BREAKER, loadFxml("rooms/breaker"));

    // decryption puzzle in control room
    SceneManager.addAppUi(AppUi.DECRYPTION, loadFxml("puzzles/decryption"));

    // logic gate puzzle in breaker room
    SceneManager.addAppUi(AppUi.LOGIC_PUZZLE, loadFxml("puzzles/logicGates"));
  }

  /**
   * This method will initialize fonts. Like with the FXML files, fonts have to be loaded as well.
   */
  private void initializeFonts() {
    // load the terminal font
    loadFont("terminal", "ttf", 23);

    // load the terminal font (temporary, to see which font is better)
    loadFont("determination", "ttf", 23);

    // load the timer font
    loadFont("timer", "TTF", 23);
  }

  /**
   * This method is invoked when the application starts. It loads and shows the "Canvas" scene.
   *
   * @param stage The primary stage of the application.
   * @throws IOException If "src/main/resources/fxml/canvas.fxml" is not found.
   */
  @Override
  public void start(final Stage stage) throws IOException {

    // initialize new Text To Speach Instance
    GameState.tts = new TextToSpeech();

    // add scenes to sceneManager
    initalizeScenes();

    // initialize fonts
    initializeFonts();

    // Create an instance of KeyEventHandler
    KeyEventsHandler keyEventsHandler = new KeyEventsHandler();

    // set first scene to display
    scene = new Scene(SceneManager.getUi(AppUi.MENU), 720, 480);

    // place scene onto stage
    stage.setScene(scene);

    // show scene
    stage.show();

    // request control focus
    scene.getRoot().requestFocus();

    // Add the KeyEventHandler to the primary scene
    scene.addEventHandler(KeyEvent.KEY_PRESSED, keyEventsHandler);

    // on stage closeing
    // I am sure I heard Gallon say there was a much better solution
    stage.setOnCloseRequest(
        (WindowEvent event) -> {
          System.out.println("Application is closing.");
          // grab an instance of text to speach
          // terminate the text to speach instance because its weird and buggy
          GameState.tts.terminate();

          // close anything else
          Platform.exit();
        });
  }
}
