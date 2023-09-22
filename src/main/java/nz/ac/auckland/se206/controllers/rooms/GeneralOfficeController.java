package nz.ac.auckland.se206.controllers.rooms;

import java.io.IOException;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Polygon;
import nz.ac.auckland.se206.App;
import nz.ac.auckland.se206.ChatManager;
import nz.ac.auckland.se206.HintManager;
import nz.ac.auckland.se206.SceneManager.AppUi;
import nz.ac.auckland.se206.constants.GameState;
import nz.ac.auckland.se206.constants.Interactions;
import nz.ac.auckland.se206.utilities.Timer;

/** Controller class for the control room scene. */
public class GeneralOfficeController extends RoomController {
  @FXML private Pane paOffice;

  @FXML private Label lblTime;
  @FXML private Label lblHintCounter;

  @FXML private Button btnHint;
  @FXML private Button btnLeft;
  @FXML private Button btnRight;

  @FXML private Polygon pgDesktop;

  @FXML private TextArea taChat;
  @FXML private TextField tfChat;

  @FXML private ImageView imgAvatar;
  @FXML private ImageView imgAvatarShaddow;
  @FXML private ImageView imgEmotion;

  /** Initializes the general office. */
  @FXML
  private void initialize() {
    // Add the label to list of labels to be updated
    Timer.addLabel(lblTime);

    // Add the hint counter components
    HintManager.addHintComponents(lblHintCounter, btnHint);

    // Add the text area and text field to the list of chat components
    ChatManager.addChatComponents(taChat, tfChat);

    ChatManager.addAiInstance(imgAvatar, imgEmotion);
  }

  @FXML
  private void onAiClicked(MouseEvent event) {
    GameState.muted = GameState.muted == false;
    GameState.tts.stop();
    ChatManager.toggleAiMuted();
  }

  @FXML
  private void onMouseEnterAi(Event event) {
    // enter
    imgAvatarShaddow.setVisible(true);
  }

  @FXML
  private void onMouseExitAi(Event event) {
    // enter
    imgAvatarShaddow.setVisible(false);
  }

  /**
   * On mouse clicked, if the button is pressed, then switch to the left scene.
   *
   * @throws IOException
   */
  @FXML
  private void onLeftButton() throws IOException {
    App.setUi(AppUi.CONTROL);
  }

  /**
   * On mouse clicked, if the button is pressed, then switch to the right scene.
   *
   * @throws IOException
   */
  @FXML
  private void onRightButton() throws IOException {
    App.setUi(AppUi.BREAKER);
  }

  @FXML
  private void onDesktopEntered() {
    pgDesktop.setOpacity(GameState.overlayCapacity);
  }

  @FXML
  private void onDesktopExited() {
    pgDesktop.setOpacity(0);
  }

  /**
   * Handles the click event on the door.
   *
   * @param event the mouse event
   * @throws IOException if there is an error loading the chat view
   */
  @FXML
  public void onDesktopClicked(MouseEvent event) throws IOException {
    // We should not give anymore hints for clicking on the desktop
    Interactions.isDesktopClicked = true;

    if (!GameState.isRiddleResolved) {
      GameState.currentRoom = AppUi.RIDDLE;
      GameState.tts.stop(); // mute current tts message
      App.setRoot("puzzles/riddle");
      return;
    }
  }

  @FXML
  public void onHintClicked(MouseEvent mouseEvent) {
    // Update the hint counter
    HintManager.updateHintCounter();

    // If the desktop has not been clicked on yet
    if (!Interactions.isDesktopClicked) {
      ChatManager.getUserHint(false);
      return;
    }

    // Disable the hints button
    btnHint.setDisable(true);

    // Tell the player that the room has been completed
    ChatManager.getUserHint(true);
  }
}
