package tfg;

import javafx.application.Application;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.*;

public class Main extends Application {

    private static final String DB_URL = "jdbc:sqlite:negocio.db";
    private TableView<Cliente> tablaClientes;
    private ObservableList<Cliente> listaClientes;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Sistema de Gestión de Negocio");

        TabPane tabPane = new TabPane();

        Tab clientesTab = new Tab("Clientes", crearVistaClientes());
        Tab productosTab = new Tab("Productos", new Label("Vista productos próximamente"));
        Tab ventasTab = new Tab("Ventas", new Label("Vista ventas próximamente"));

        tabPane.getTabs().addAll(clientesTab, productosTab, ventasTab);

        Scene scene = new Scene(tabPane, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();

        crearTablasSiNoExisten();
        cargarClientes();
    }

    
    private VBox crearVistaClientes() {
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));

        TextField nombreField = new TextField();
        nombreField.setPromptText("Nombre del cliente");
        TextField telefonoField = new TextField();
        telefonoField.setPromptText("Teléfono");
        TextField emailField = new TextField();
        emailField.setPromptText("Email");

        Button guardarBtn = new Button("Guardar Cliente");
        guardarBtn.setOnAction(e -> {
            guardarCliente(nombreField.getText(), telefonoField.getText(), emailField.getText());
            nombreField.clear();
            telefonoField.clear();
            emailField.clear();
            cargarClientes();
        });

        Button eliminarBtn = new Button("Eliminar Cliente Seleccionado");
        eliminarBtn.setOnAction(e -> {
            Cliente seleccionado = tablaClientes.getSelectionModel().getSelectedItem();
            if (seleccionado != null) {
                eliminarCliente(seleccionado.getId());
                cargarClientes();
            }
        });

        tablaClientes = new TableView<>();
        TableColumn<Cliente, Number> columnaId = new TableColumn<>("ID");
        columnaId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getId()));

        TableColumn<Cliente, String> columnaNombre = new TableColumn<>("Nombre");
        columnaNombre.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNombre()));

        TableColumn<Cliente, String> columnaTelefono = new TableColumn<>("Teléfono");
        columnaTelefono.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTelefono()));

        TableColumn<Cliente, String> columnaEmail = new TableColumn<>("Email");
        columnaEmail.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));

        tablaClientes.getColumns().addAll(columnaId, columnaNombre, columnaTelefono, columnaEmail);

        listaClientes = FXCollections.observableArrayList();
        tablaClientes.setItems(listaClientes);

        layout.getChildren().addAll(
                new Label("Nuevo Cliente:"),
                nombreField, telefonoField, emailField,
                guardarBtn, eliminarBtn,
                new Label("Lista de Clientes:"),
                tablaClientes);

        return layout;
    }

    private void guardarCliente(String nombre, String telefono, String email) {
        if (nombre == null || nombre.trim().isEmpty()) return;
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "INSERT INTO clientes(nombre, telefono, email) VALUES(?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, nombre);
            pstmt.setString(2, telefono);
            pstmt.setString(3, email);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void eliminarCliente(int id) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "DELETE FROM clientes WHERE id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void cargarClientes() {
        listaClientes.clear();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM clientes");
            while (rs.next()) {
                Cliente cliente = new Cliente(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("telefono"),
                        rs.getString("email")
                );
                listaClientes.add(cliente);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void crearTablasSiNoExisten() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS clientes (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL, telefono TEXT, email TEXT)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
