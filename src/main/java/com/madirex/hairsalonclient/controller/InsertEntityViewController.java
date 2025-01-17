package com.madirex.hairsalonclient.controller;

import com.madirex.hairsalonclient.model.Service;
import com.madirex.hairsalonclient.model.User;
import com.madirex.hairsalonclient.model.UserGender;
import com.madirex.hairsalonclient.model.createDTOs.CreateService;
import com.madirex.hairsalonclient.model.createDTOs.CreateUser;
import com.madirex.hairsalonclient.restcontroller.APIRestConfig;
import com.madirex.hairsalonclient.utils.Util;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class InsertEntityViewController implements BaseController {
    @FXML
    private Label titleLabel;
    @FXML
    private VBox fieldsBox;
    @FXML
    private ImageView uploadImageView;
    @FXML
    private TextField imageURLTextField;
    private Map<String, TextField> entityFields;
    private Map<String, ChoiceBox<?>> entityChoiceBoxes;

    public static final int SERVICE_TYPE = 0;
    public static final int USER_TYPE = 1;

    private int currentType = SERVICE_TYPE;
    private Set<TextField> textfields = new LinkedHashSet<>();
    private Optional<String> imageBytesUploaded = Optional.empty();
    private ChoiceBox<UserGender> fieldGender;

    public InsertEntityViewController() {
        entityFields = new HashMap<>();
        entityChoiceBoxes = new HashMap<>();
    }

    @FXML
    public void initialize() {

    }

    public void setEntityType(int type) {
        if (type == USER_TYPE) {
            currentType = USER_TYPE;
            loadUserComponents();
        } else if (type == SERVICE_TYPE) {
            currentType = SERVICE_TYPE;
            loadServiceComponents();
        }
    }

    private void loadServiceComponents() {
        titleLabel.setText(Util.getString("text.addService"));
        addEntityField(Util.getString("text.name"));
        addEntityField(Util.getString("text.description"));
        addEntityField(Util.getString("text.price"));
        addEntityField(Util.getString("text.stock"));
    }

    private void loadUserComponents() {
        titleLabel.setText(Util.getString("text.addUser"));
        addEntityField(Util.getString("text.username"));
        addEntityField(Util.getString("text.name"));
        addEntityField(Util.getString("text.surname"));
        addEntityField(Util.getString("text.password"));
        addEntityField(Util.getString("text.telephone"));
        addEntityField(Util.getString("text.email"));
        addGenderChoiceBox();
    }

    private void addGenderChoiceBox() {
        HBox hbox = new HBox();
        String name = "gender";
        ChoiceBox<UserGender> field = new ChoiceBox<>();
        field.setItems(FXCollections.observableList(Arrays.stream(UserGender.values()).collect(Collectors.toList())));
        entityChoiceBoxes.put(name, field);
        hbox.getChildren().add(new Label(name));
        hbox.getChildren().add(field);
        fieldsBox.getChildren().add(hbox);
    }

    private TextField addEntityField(String name) {
        HBox hbox = new HBox();
        TextField field = new TextField();
        textfields.add(field);
        entityFields.put(name, field);
        hbox.getChildren().add(new Label(name));
        hbox.getChildren().add(field);
        fieldsBox.getChildren().add(hbox);
        return field;
    }

    @FXML
    protected void insertEntity() throws IOException {
        if (currentType == USER_TYPE) {
            insertUser();
        } else if (currentType == SERVICE_TYPE) {
            insertService();
        }
    }

    private void insertService() throws IOException {
        TextField[] information = getInformationArray();

        String user = information[0].getText();
        String image = "";

        if (imageBytesUploaded.isPresent()) {
            image = imageBytesUploaded.get();
        }

        String description = information[1].getText();

        try {
            Double price = Double.parseDouble(information[2].getText());
            Integer stock = Integer.parseInt(information[3].getText());

            CreateService service = new CreateService(image, user, description, price, stock);

            Service re = APIRestConfig.getServicesService().insertService(APIRestConfig.token, service).execute().body();
            if (re == null) {
                String moreInfo = getErrorService(service);
                Util.popUpAlert(Util.getString("title.error"), Util.getString("error.serviceNotCreated") + "\n" + moreInfo, Alert.AlertType.ERROR);
            } else {
                Util.popUpAlert(Util.getString("title.info"), Util.getString("text.userCreated"), Alert.AlertType.INFORMATION);
            }
        } catch (NumberFormatException e) {
            Util.popUpAlert(Util.getString("title.error"),
                    Util.getString("error.serviceNotCreated") + "\n" +
                            Util.getString("error.priceAndStockMustBeANumber"), Alert.AlertType.ERROR);
        }
    }

    @SneakyThrows
    private void insertUser() {
        TextField[] information = getInformationArray();
        String image = "";

        if (imageBytesUploaded.isPresent()) {
            image = imageBytesUploaded.get();
        }

        String username = information[0].getText();
        String name = information[1].getText();
        String surname = information[2].getText();
        String password = information[3].getText();
        String telephone = information[4].getText();
        String email = information[5].getText();
        UserGender gender = fieldGender.getValue();
        CreateUser user = new CreateUser(image, username, name, surname, password, telephone, email, gender);

        User re = APIRestConfig.getUsersService().insertUsers(APIRestConfig.token, user).execute().body();
        if (re == null) {
            String moreInfo = getErrorUser(user);
            Util.popUpAlert(Util.getString("title.error"), Util.getString("error.userNotCreated") + "\n" + moreInfo, Alert.AlertType.ERROR);
        } else {
            Util.popUpAlert(Util.getString("title.info"), Util.getString("text.userCreated"), Alert.AlertType.INFORMATION);
        }
    }

    private TextField[] getInformationArray() {
        TextField[] information = new TextField[textfields.size()];
        textfields.toArray(information);
        return information;
    }

    private String getErrorUser(CreateUser user) {
        AtomicBoolean filled = new AtomicBoolean(true);
        Arrays.stream(getInformationArray()).forEach(e -> {
            if (Objects.equals(e.getText(), "")) {
                filled.set(false);
            }
        });

        if (!filled.get()) {
            return Util.getString("error.dataNotFilled");
//        //TODO: condiciones de validación
//         } else if (!validMail(user.getEmail())) {
//            return Util.getString("error.invalidEmail");
        } else {
            return "";
        }
    }

    private String getErrorService(CreateService service) {
        if (service.getName() == null || service.getName().isEmpty()) {
            return Util.getString("error.nameEmpty");
        } else if (service.getPrice() <= 0) {
            return Util.getString("error.priceGreaterZero");
        } else if (service.getStock() < 0) {
            return Util.getString("error.stockGreaterZero");
        } else {
            return "";
        }
    }

    private boolean validMail(String mail) {
        String regex = ".*@.*\\\\..*";
        Pattern pat = Pattern.compile(regex);
        Matcher mat = pat.matcher(mail);
        return mat.matches();
    }

    @FXML
    protected void uploadImage() throws IOException {
        FileChooser fc = new FileChooser();
        File file = fc.showOpenDialog(null);
        if (file != null) {
            imageBytesUploaded = Optional.of(Files.readString(file.toPath()));
        } else {
            Util.popUpAlert(Util.getString("title.error"),
                    Util.getString("error.imageUpload"), Alert.AlertType.ERROR);
        }
    }
}
