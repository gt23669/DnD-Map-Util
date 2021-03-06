package application;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Optional;

import controllers.ConfigureController;
import controllers.EntityController;
import controllers.UIController;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import models.Entity;
import models.Mob;
import models.Monster;
import models.Player;
import util.Map;

public class FPADriver extends Application {

	String pathMap = "MapView.fxml";
	String pathEntity = "EntityView.fxml";
	public static Scene sceneMap, sceneEntity;
	static String saveMapPath = "";
	public static ArrayList<Map> maps = new ArrayList<>();
	// private static ArrayList<Player> players = new ArrayList<>();
	// private static ArrayList<Monster> monsters = new ArrayList<>();
	// public static ArrayList<Items> items = new ArrayList<>();
	public static HashMap<String, Mob> AvaliableEntities = new HashMap();
	static public ArrayList<Pane> mapContents = new ArrayList<>();

	public static HashSet<String> currentlyActiveKeys;

	@SuppressWarnings("unchecked")
	@Override
	public void start(Stage Stage) throws IOException {
		FXMLLoader loader = new FXMLLoader((getClass().getResource(pathMap)));
		FXMLLoader loader2 = new FXMLLoader((getClass().getResource(pathEntity)));

		Parent root = loader.load();
		Parent root2 = loader2.load();

		UIController controller = loader.getController();
		EntityController controller2 = loader2.getController();

		controller.setMain(this);

		GridPane map = controller.mapGrid;

		sceneMap = new Scene(root, 1200, 800);
		sceneEntity = new Scene(root2, 1200, 800);
		Stage.setScene(sceneMap);
		Stage.setAlwaysOnTop(false);
		Stage.setResizable(false);
		Stage.show();
		initialLoad();
		initialEntityLoad();
		controller2.updateChoiceBox();
		controller.updateMapChoiceBox();
		controller.updateEntityChoiceBox();

		prepareActionHandlers();
		
		controller.entitySelect.onMouseReleasedProperty().set(e -> {
			controller.canPlace = true;
		});

		Stage.setOnCloseRequest(event -> {
			// System.out.println("App is closing");

			try {
				exitFileSave(maps, "./bulkLoad.txt");
				exitEntityFileSave(AvaliableEntities, "./bulkEntityLoad");
			} catch (IOException e) {
				System.out.println("general IOException in exit save");
			}
		});
		final KeyCodeCombination combo1 = new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN);
		final KeyCodeCombination combo2 = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN);
		final KeyCodeCombination combo3 = new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN);
		sceneEntity.addEventHandler(KeyEvent.KEY_RELEASED, new EventHandler() {

			@Override
			public void handle(Event event) {
				if (combo1.match((KeyEvent) event)) {
					getSavePath(Stage);
					importEntity(saveMapPath);

					controller.updateEntityChoiceBox();
				}
				if (combo2.match((KeyEvent) event)) {
					getSavePath2(Stage);
					exportEntity(AvaliableEntities);

				}
				if (combo3.match((KeyEvent) event)) {
					TextInputDialog textDialog = new TextInputDialog();
					textDialog.setTitle("Remove Entity");
					textDialog.setHeaderText(null);
					textDialog.setContentText("Which entity do you want to remove?");
					Optional<String> entityName = Optional.empty();
					entityName = textDialog.showAndWait();
					String name = entityName.get();
					for (String key : AvaliableEntities.keySet()) {
						if (AvaliableEntities.get(key).getName().equals(name)) {
							AvaliableEntities.remove(key);
							controller2.updateChoiceBox();
							break;
						}
					}
				}
			}

		});
		sceneMap.addEventHandler(KeyEvent.KEY_RELEASED, new EventHandler() {
			@Override
			public void handle(Event event) {
				if (combo1.match((KeyEvent) event)) {
					System.out.println("Key combo(ctrl+o) has been presssed");
					getSavePath(Stage);
					try {
						singleLoadFile();
						controller.updateMapChoiceBox();
					} catch (ClassNotFoundException e) {
						System.out.println("ClassNotFoundException in keycombo load");
					} catch (IOException e) {
						System.out.println("General IOException in keycombo load");
					}
				}
				if (combo2.match((KeyEvent) event)) {
					System.out.println("Key combo(ctrl+s) has been presssed");
					getSavePath2(Stage);
					File file = new File(saveMapPath);
					String name = file.getName();
					String[] strArr = new String[mapContents.size()];
					int i = 0;

					for (Pane p : mapContents) {
						strArr[i] = p.getStyle();
						i++;
					}
					Map m = new Map(name, strArr);
					try {
						singleSaveFile(m);
						controller.updateMapChoiceBox();
					} catch (IOException e) {
						System.out.println("General IOException in keycombo save");
					}
				}
				if (combo3.match((KeyEvent) event)) {
					System.out.println("Key combo(ctrl+r) has been presssed");
					TextInputDialog textDialog = new TextInputDialog();
					textDialog.setTitle("Remove Map");
					textDialog.setHeaderText(null);
					textDialog.setContentText("Which map do you want to remove?");
					Optional<String> mapName = Optional.empty();
					mapName = textDialog.showAndWait();
					String name = mapName.get();
					for (int i = 0; i < maps.size(); i++) {
						if (maps.get(i).name.matches(name)) {
							maps.remove(i);
						}
					}
					controller.updateMapChoiceBox();
				}
			}

		});
		controller.entitySceneSwap.setOnAction(e -> Stage.setScene(sceneEntity));
		
		controller2.confAttButton.setOnMouseClicked((event) -> {
		    try {
		        FXMLLoader loader3 = new FXMLLoader((getClass().getResource("ConfigureView.fxml")));

		 		Parent root3 = loader3.load();

		 		ConfigureController controller3 = loader3.getController();
		        controller3.applybutton.setOnAction(e -> {
		        	String[] finalAtts = controller3.returnAtts(String.valueOf(controller2.chooseEntity.getValue()));
		        	
		        	controller2.updateAttributeLabels(finalAtts[0], finalAtts[1], finalAtts[2], finalAtts[3], finalAtts[4], finalAtts[5]);
		        });
		        Scene scene = new Scene(root3, 600, 400);
		        Stage stage = new Stage();
		        stage.setTitle("New Window");
		        stage.setResizable(false);
		        stage.setScene(scene);
		        stage.show();
		    } catch (IOException e) {
	            e.printStackTrace();
		        System.out.println("Failed to create new Window.");
		    }
		});
		controller2.entities.setOnAction(e -> {
			Stage.setScene(sceneMap);
			controller.updateEntityChoiceBox();
		});
		controller2.Att1Slider.valueProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) {
				controller2.updateAtt1();
			}
		});
		controller2.Att2Slider.valueProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) {
				controller2.updateAtt2();
			}
		});
		controller2.Att3Slider.valueProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) {
				controller2.updateAtt3();
			}
		});
		controller2.Att4Slider.valueProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) {
				controller2.updateAtt4();
			}
		});
		controller2.Att5Slider.valueProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) {
				controller2.updateAtt5();
			}
		});
		controller2.Att6Slider.valueProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) {
				controller2.updateAtt6();
			}
		});
		controller2.HitDiceSelect
				.setItems(FXCollections.observableArrayList("d4", "d6", "d8", "d10", "d12", "d20", "d100"));

		controller.mapSelect.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue observable, Number oldValue, Number newValue) {
			}
		});

		for (int i = 0; i < controller.mapGrid.getRowConstraints().size(); i++) {
			for (int j = 0; j < controller.mapGrid.getColumnConstraints().size(); j++) {
				Pane p = new Pane();
				GridPane.setConstraints(p, j, i);
				controller.mapGrid.getChildren().add(p);

				p.setMinSize(10, 8);
				p.setPrefSize(50, 40);
				p.setMaxSize(80, 60);

				mapContents.add(p);

				p.setStyle("-fx-background-color: lightgreen; -fx-border-color: black; -fx-border-width: 0.5;");

				p.setOnMousePressed(e -> {
					if (controller.entitySelect.getValue() != null && controller.canPlace) {
						System.out.println(controller.entitySelect.getValue());
						Button b = new Button();

						Entity ent = new Entity(b, AvaliableEntities.get(controller.entitySelect.getValue()));

						b.setText(ent.mob.getName().substring(0, 2));
						b.setFont(new Font(8));
						b.setMinSize(p.widthProperty().doubleValue() - 8, p.heightProperty().doubleValue() - 8);
						b.setAlignment(Pos.CENTER);
						if (controller.selectedPic != null) {
							b.setStyle(controller.selectedPic);
							controller.selectedPic = null;
							b.textFillProperty().set(Paint.valueOf("#ffffff"));
						} else {
							b.setStyle("");
							b.textFillProperty().set(Paint.valueOf("#000000"));
						}
						b.layoutXProperty().bind(p.widthProperty().subtract(b.widthProperty()).divide(2));
						b.layoutYProperty().bind(p.heightProperty().subtract(b.heightProperty()).divide(2));

						b.setOnMouseClicked(fe -> {
							controller.entityName = ent;

							controller.descriptionArea.appendText(ent.mob.getName() + "\nMax hp:" + ent.mob.getMaxHP()
									+ "\nCurrent hp: " + ent.mob.getCurrentHP() + "\n\n");

							controller.HPBox.setDisable(false);
							controller.removeEntityButton.setDisable(false);
							controller.viewStatsButton.setDisable(false);
							controller.changeHealthButton.setDisable(false);

							if (controller.selectedPic != null) {
								b.setStyle(controller.selectedPic);
								controller.selectedPic = null;
								controller.descriptionArea.appendText("Icon set\n");
								b.textFillProperty().set(Paint.valueOf("#ffffff"));
							}

						});

						if (p.getChildren().size() < 1) {
							p.getChildren().add(b);
						} else {
							p.getChildren().remove(0);
						}
						controller.canSelect = false;
					} else {
						controller.HPBox.setDisable(true);
						controller.removeEntityButton.setDisable(true);
						controller.viewStatsButton.setDisable(true);
						controller.changeHealthButton.setDisable(true);

						p.setStyle(controller.color + "; -fx-border-color: black; -fx-border-width: 0.5;");
					}
					
					controller.canPlace = false;
					
				});

				p.setOnDragDetected(e -> {
					if (!controller.entitySelected) {
						Dragboard db = p.startDragAndDrop(TransferMode.COPY);
						ClipboardContent content = new ClipboardContent();
						content.putString(p.getStyle());
						db.setContent(content);
					}
				});
				p.setOnDragOver(new javafx.event.EventHandler<DragEvent>() {
					@Override
					public void handle(DragEvent event) {
						if (event.getGestureSource() != p && event.getDragboard().hasString()) {
							event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
						}
						event.consume();
					}
				});
				p.setOnDragEntered(new javafx.event.EventHandler<DragEvent>() {
					@Override
					public void handle(DragEvent event) {
						Dragboard db = event.getDragboard();
						if (event.getGestureSource() != p && event.getDragboard().hasString()) {
							p.setStyle(db.getString());
						}
					}
				});
				p.setOnDragExited(new javafx.event.EventHandler<DragEvent>() {
					public void handle(DragEvent event) {
						event.consume();
					}
				});
				p.setOnDragDropped(new javafx.event.EventHandler<DragEvent>() {
					public void handle(DragEvent event) {
						Dragboard db = event.getDragboard();
						boolean success = false;
						if (db.hasString()) {
							p.setStyle(db.getString());
							success = true;
						}
						event.consume();
					}
				});
			}
		}
	}

	public static void exitEntityFileSave(HashMap<String, Mob> hm, String filePath) throws IOException {
		File file = new File(filePath);
		if (file.exists()) {
			file.delete();
		}
		FileOutputStream fos = new FileOutputStream(filePath);
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeObject(hm);
		oos.close();
		bos.close();
		fos.close();
	}

	private void initialEntityLoad() {
		String filePath = "./bulkEntityLoad";
		File file = new File(filePath);
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				System.out.println("General IOException initialEntityLoad");
				e.printStackTrace();
			}
		}
		initialEntityFileLoad(filePath);

	}

	public static void initialEntityFileLoad(String filePath) {
		HashMap<String, Mob> entityContentsLoad = new HashMap<>();
		try {
			FileInputStream fis = new FileInputStream(filePath);
			BufferedInputStream bis = new BufferedInputStream(fis);
			ObjectInputStream ois = new ObjectInputStream(bis);
			entityContentsLoad = (HashMap<String, Mob>) ois.readObject();
			ois.close();
			bis.close();
			fis.close();
		} catch (EOFException e) {

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch blockS
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// entityContentsLoad=AvaliableEntities;
		AvaliableEntities = entityContentsLoad;

	}

	private void initialLoad() {
		String filePath = "./bulkLoad.txt";
		File file = new File(filePath);
		// SaveFile sf = new SaveFile(items, players, monsters, maps);
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				System.out.println("General IOException initialLoad");
				e.printStackTrace();
			}
		}
		try {
			initialFileLoad(filePath);
		} catch (EOFException e) {
			System.out.println("EOFException in initial load");
		} catch (ClassNotFoundException e) {
			System.out.println("Class not found exception in initial load");
		} catch (IOException e) {
			System.out.println("General IOException in initial load");
			e.printStackTrace();
		}

	}

	public static ArrayList returnMaps() {
		ArrayList choiceBoxMaps = new ArrayList<>();
		for (Map m : maps) {
			choiceBoxMaps.add(m);
		}
		return choiceBoxMaps;
	}

	public static void addPlayer(Player p) {
		if (AvaliableEntities.containsKey(p.getName())) {
			p.setName(p.getName() + "(1)");
			AvaliableEntities.put(p.getName(), p);
		} else {
			AvaliableEntities.put(p.getName(), p);
		}
	}

	public static void addMonster(Monster m) {
		if (AvaliableEntities.containsKey(m.getName())) {
			m.setName(m.getName() + "(1)");
			AvaliableEntities.put(m.getName(), m);
		} else {
			AvaliableEntities.put(m.getName(), m);
		}
	}

	public static void initialFileLoad(String filePath) throws IOException, ClassNotFoundException {
		FileInputStream fis = new FileInputStream(filePath);
		BufferedInputStream bis = new BufferedInputStream(fis);
		ObjectInputStream ois = new ObjectInputStream(bis);
		ArrayList<Map> mapContentsLoad = (ArrayList<Map>) ois.readObject();
		ois.close();
		bis.close();
		fis.close();

		for (int i = 0; i < mapContentsLoad.size(); i++) {
			maps.add(i, mapContentsLoad.get(i));
		}

	}

	public static void singleLoadFile() throws IOException, ClassNotFoundException {
		FileInputStream fis = new FileInputStream(saveMapPath);
		BufferedInputStream bis = new BufferedInputStream(fis);
		ObjectInputStream ois = new ObjectInputStream(bis);
		Map mapContentsLoad = (Map) ois.readObject();
		ois.close();
		bis.close();
		fis.close();
		boolean isUnique = true;
		for (int i = 0; i < maps.size(); i++) {
			if (maps.get(i).name.equals(mapContentsLoad.name)) {
				isUnique = false;
			}
		}

		if (isUnique) {
			maps.add(mapContentsLoad);
		}
		setMap(mapContentsLoad);
	}

	public static void exitFileSave(ArrayList<Map> m, String filePath) throws IOException {
		File file = new File(filePath);
		if (file.exists()) {
			file.delete();
		}
		FileOutputStream fos = new FileOutputStream(filePath);
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeObject(m);
		oos.close();
		bos.close();
		fos.close();
	}

	public static void singleSaveFile(Map m) throws IOException {
		File file = new File(saveMapPath);
		if (file.exists()) {
			file.delete();
		}
		FileOutputStream fos = new FileOutputStream(saveMapPath);
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeObject(m);
		oos.close();
		bos.close();
		fos.close();
		maps.add(m);
	}

	public static void importMap() {
		getSavePath(null);
		try {
			singleLoadFile();
		} catch (ClassNotFoundException e) {
			System.out.println("class not found");
		} catch (IOException e) {

		}
	}

	public static Mob importSingleEntity(String saveMapPath) {
		if (saveMapPath == null) {
			saveMapPath = FPADriver.saveMapPath;
		}

		Mob m = null;
		try {
			FileInputStream fis = new FileInputStream(saveMapPath);
			BufferedInputStream bis = new BufferedInputStream(fis);
			ObjectInputStream ois = new ObjectInputStream(bis);
			m = (Mob) ois.readObject();
			ois.close();
			bis.close();
			fis.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (m.getClass().getSimpleName().equals("Player")) {
			m = (Player) m;
		} else {
			m = (Monster) m;
		}

		AvaliableEntities.put(m.getName(), m);
		return m;

	}

	public static void importEntity(String saveMapPath) {
		HashMap<String, Mob> tempHM = new HashMap<>();
		try {
			FileInputStream fis = new FileInputStream(saveMapPath);
			BufferedInputStream bis = new BufferedInputStream(fis);
			ObjectInputStream ois = new ObjectInputStream(bis);
			tempHM = (HashMap) ois.readObject();
			ois.close();
			bis.close();
			fis.close();
		} catch (FileNotFoundException e) {
			System.out.println("FileNotFoundException in importSingleEntity");
		} catch (ClassNotFoundException e) {
			System.out.println("ClassNotFoundException in importSingleEntity");
		} catch (IOException e) {
			System.out.println("IOException in importSingleEntity");
			e.printStackTrace();
		}

		AvaliableEntities.putAll(tempHM);

	}

	public static void exportSingleEntity(Mob m, String saveMapPath) {
		if (saveMapPath == null) {
			saveMapPath = FPADriver.saveMapPath;
		}
		File file = new File(saveMapPath);
		if (file.exists()) {
			file.delete();
		}
		try {
			FileOutputStream fos = new FileOutputStream(saveMapPath);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(m);
			oos.close();
			bos.close();
			fos.close();
		} catch (FileNotFoundException e) {
			System.out.println("FileNotFoundException in exportSingleEntity");
		} catch (IOException e) {
			System.out.println("General IOExceptin in exportSingleEntity");
		}
	}

	public static void exportEntity(HashMap<String, Mob> avaliableEntities) {
		File file = new File(saveMapPath);
		if (file.exists()) {
			file.delete();
		}
		try {
			FileOutputStream fos = new FileOutputStream(saveMapPath);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(avaliableEntities);
			oos.close();
			bos.close();
			fos.close();
		} catch (FileNotFoundException e) {
			System.out.println("FileNotFoundException in exportSingleEntity");
		} catch (IOException e) {
			System.out.println("General IOExceptin in exportSingleEntity");
		}

	}

	// ------------------------------------------------daniel - the 192 starts here
	public static void exportMap(ArrayList<Pane> mapContents) {
		getSavePath(null);
		File file = new File(saveMapPath);
		String name = file.getName();
		String[] strArr = new String[mapContents.size()];
		int i = 0;

		for (Pane p : mapContents) {
			strArr[i] = p.getStyle();
			i++;
		}
		try {
			Map m = new Map(name, strArr);
			singleSaveFile(m);
		} catch (NoSuchElementException | IOException e) {

		}
	}

	public static void getSavePath2(Stage stage) {
		File file;
		FileChooser filechooser = new FileChooser();
		file = filechooser.showSaveDialog(stage);
		try {
			saveMapPath = file.getAbsolutePath();
		} catch (NullPointerException e) {
		}
	}

	public static void getSavePath(Stage stage) {
		if (stage != null) {
			File file;
			FileChooser FileChooser = new FileChooser();
			file = FileChooser.showOpenDialog(stage);
			try {
				saveMapPath = file.getAbsolutePath();
			} catch (NullPointerException e) {

			}
			System.out.println(saveMapPath);
		} else {

			TextInputDialog textDialog = new TextInputDialog();
			textDialog.setTitle("Save Path");
			textDialog.setHeaderText(null);
			textDialog.setContentText("Enter the filePath for the save file: ");
			Optional<String> filePath = Optional.empty();
			filePath = textDialog.showAndWait();
			try {
				saveMapPath = filePath.get();
			} catch (NoSuchElementException e) {

			}
		}
	}

	public static void setMap(Map pane) {
		ArrayList<Pane> loadPane = new ArrayList<>();
		for (int i = 0; i < pane.mapContents.length; i++) {
			Pane p = new Pane();

			mapContents.get(i).getChildren()
					.removeAll(FXCollections.observableArrayList(mapContents.get(i).getChildren()));
			p.setStyle(pane.mapContents[i]);
			loadPane.add(p);
		}
		for (int i = 0; i < loadPane.size(); i++) {
			mapContents.get(i).setStyle(loadPane.get(i).getStyle());
		}
	}

	public static void main(String[] args) {
		launch(args);
	}

	private static void prepareActionHandlers() {
		currentlyActiveKeys = new HashSet<String>();
		sceneMap.setOnKeyPressed(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				currentlyActiveKeys.add(event.getCode().toString());
			}
		});
		sceneMap.setOnKeyReleased(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				currentlyActiveKeys.remove(event.getCode().toString());

			}
		});
	}
}
