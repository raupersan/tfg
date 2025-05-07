package tfg;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import java.sql.*;

public class Main extends Application {

    private static final String DB_URL = "jdbc:sqlite:negocio.db";

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Sistema de Gestión de Negocio");

        TabPane tabPane = new TabPane();

        Tab clientesTab = new Tab("Clientes", ClienteView.crearVistaClientes(DB_URL));
        Tab productosTab = new Tab("Productos", ProductoView.crearVistaProductos(DB_URL));
        Tab ventasTab = new Tab("Ventas", VentaView.crearVistaVentas(DB_URL));
        
        clientesTab.setClosable(false);
        productosTab.setClosable(false);
        ventasTab.setClosable(false);

        tabPane.getTabs().addAll(clientesTab, productosTab, ventasTab);

        Scene scene = new Scene(tabPane, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();

        crearTablasSiNoExisten();
    }

    private void crearTablasSiNoExisten() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS clientes (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL, telefono TEXT, email TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS productos (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL, precio REAL, stock INTEGER)");
            stmt.execute("CREATE TABLE IF NOT EXISTS ventas (id INTEGER PRIMARY KEY AUTOINCREMENT, cliente TEXT, producto TEXT, cantidad INTEGER, total REAL, fecha TEXT)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
