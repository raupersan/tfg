package tfg;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.sql.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

public class ClienteView {

	public static ScrollPane crearVistaClientes(String dbUrl) {
		VBox layout = new VBox(10);
		layout.setPadding(new Insets(10));
		layout.getChildren().add(OpenAIClient.crearBotonChatGPTResumen(dbUrl, "clientes"));

		TextField nombreField = new TextField();
		nombreField.setPromptText("Nombre del cliente");
		TextField telefonoField = new TextField();
		telefonoField.setPromptText("Teléfono");
		TextField emailField = new TextField();
		emailField.setPromptText("Email");

		Button guardarBtn = new Button("Guardar Cliente");
		Button eliminarBtn = new Button("Eliminar Cliente Seleccionado");
		Button editarBtn = new Button("Editar Cliente");
		Button exportarBtn = new Button("Exportar");
		Button exportarPdfBtn = new Button("Exportar a PDF");
		Button exportarExcelBtn = new Button("Exportar a Excel");

		HBox exportarOpciones = new HBox(10);
		exportarOpciones.getChildren().addAll(exportarPdfBtn, exportarExcelBtn);
		exportarOpciones.setVisible(false);
		exportarOpciones.setManaged(false);

		exportarBtn.setOnAction(e -> {
			boolean mostrar = !exportarOpciones.isVisible();
			exportarOpciones.setVisible(mostrar);
			exportarOpciones.setManaged(mostrar);
		});
		
		HBox botonesExportar = new HBox(10);
		botonesExportar.getChildren().addAll(exportarBtn, exportarOpciones);

		TableView<Cliente> tabla = new TableView<>();
		ObservableList<Cliente> clientes = FXCollections.observableArrayList();

		TableColumn<Cliente, String> colNombre = new TableColumn<>("Nombre");
		colNombre.setCellValueFactory(
				data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getNombre()));

		TableColumn<Cliente, String> colTelefono = new TableColumn<>("Teléfono");
		colTelefono.setCellValueFactory(
				data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getTelefono()));

		TableColumn<Cliente, String> colEmail = new TableColumn<>("Email");
		colEmail.setCellValueFactory(
				data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getEmail()));

		tabla.getColumns().addAll(colNombre, colTelefono, colEmail);
		tabla.setItems(clientes);

		cargarClientes(dbUrl, clientes);

		guardarBtn.setOnAction(e -> guardarCliente(dbUrl, nombreField, telefonoField, emailField, clientes));

		eliminarBtn.setOnAction(e -> {
			Cliente seleccionado = tabla.getSelectionModel().getSelectedItem();
			if (seleccionado != null) {
				try (Connection conn = DriverManager.getConnection(dbUrl)) {
					String sql = "DELETE FROM clientes WHERE id = ?";
					PreparedStatement pstmt = conn.prepareStatement(sql);
					pstmt.setInt(1, seleccionado.getId());
					pstmt.executeUpdate();
					cargarClientes(dbUrl, clientes);
				} catch (SQLException ex) {
					ex.printStackTrace();
				}
			}
		});

		editarBtn.setOnAction(e -> {
			Cliente seleccionado = tabla.getSelectionModel().getSelectedItem();
			if (seleccionado != null) {
				nombreField.setText(seleccionado.getNombre());
				telefonoField.setText(seleccionado.getTelefono());
				emailField.setText(seleccionado.getEmail());
				guardarBtn.setOnAction(ev -> {
					try (Connection conn = DriverManager.getConnection(dbUrl)) {
						String sql = "UPDATE clientes SET nombre = ?, telefono = ?, email = ? WHERE id = ?";
						PreparedStatement pstmt = conn.prepareStatement(sql);
						pstmt.setString(1, nombreField.getText());
						pstmt.setString(2, telefonoField.getText());
						pstmt.setString(3, emailField.getText());
						pstmt.setInt(4, seleccionado.getId());
						pstmt.executeUpdate();
						nombreField.clear();
						telefonoField.clear();
						emailField.clear();
						cargarClientes(dbUrl, clientes);
						guardarBtn.setOnAction(
								ev2 -> guardarCliente(dbUrl, nombreField, telefonoField, emailField, clientes));
					} catch (SQLException ex) {
						ex.printStackTrace();
					}
				});
			}
		});
		exportarExcelBtn.setOnAction(e -> {
		    FileChooser fileChooser = new FileChooser();
		    fileChooser.setTitle("Guardar archivo Excel");
		    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo CSV", "*.csv"));
		    File archivo = fileChooser.showSaveDialog(layout.getScene().getWindow());
		    if (archivo != null) {
		        try (
		            FileOutputStream fos = new FileOutputStream(archivo);
		            OutputStreamWriter osw = new OutputStreamWriter(fos, java.nio.charset.StandardCharsets.UTF_8);
		            java.io.PrintWriter writer = new java.io.PrintWriter(osw);
		            Connection conn = DriverManager.getConnection(dbUrl);
		            Statement stmt = conn.createStatement();
		            ResultSet rs = stmt.executeQuery("SELECT * FROM clientes ORDER BY nombre ASC")
		        ) {
					//Secuencia especial que se pone al principio de un archivo UTF-8
					fos.write(0xEF);
		            fos.write(0xBB);
		            fos.write(0xBF);
					writer.println("Nombre;Teléfono;Email");
					while (rs.next()) {
						String nombre = rs.getString("nombre");
						String telefono = rs.getString("telefono");
						String email = rs.getString("email");
						writer.println(nombre + ";" + telefono + ";" + email);
					}

					Alert alert = new Alert(Alert.AlertType.INFORMATION);
					alert.setTitle("Exportación");
					alert.setHeaderText(null);
					alert.setContentText("Archivo CSV creado con éxito: " + archivo.getAbsolutePath());
					alert.showAndWait();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});
		exportarPdfBtn.setOnAction(e -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Guardar archivo PDF");
			fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo PDF", "*.pdf"));
			File archivo = fileChooser.showSaveDialog(layout.getScene().getWindow());
			if (archivo != null) {
				try {
					Document document = new Document();
					PdfWriter.getInstance(document, new FileOutputStream(archivo));
					document.open();
					document.add(new Paragraph("Historial de Ventas"));
					document.add(new Paragraph(" "));
					PdfPTable table = new PdfPTable(1);
					for (Cliente c : clientes) {
						table.addCell(c.getNombre());
						table.addCell(String.valueOf(c.getTelefono()));
						table.addCell(String.valueOf(c.getEmail()));
					}
					document.add(table);
					document.close();
					alerta("Exportación completada", "El archivo ha sido creado exitosamente.");
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});

		HBox botones = new HBox(10);
		botones.getChildren().addAll(guardarBtn, editarBtn, eliminarBtn, exportarBtn);

		layout.getChildren().addAll(
			    nombreField, telefonoField, emailField,
			    botones,
			    botonesExportar, 
			    tabla
			);

		ScrollPane scrollPane = new ScrollPane(layout);
		scrollPane.setFitToWidth(true);
		return scrollPane;
	}

	private static void guardarCliente(String dbUrl, TextField nombreField, TextField telefonoField,
			TextField emailField, ObservableList<Cliente> clientes) {
		String nombre = nombreField.getText();
		String telefono = telefonoField.getText();
		String email = emailField.getText();
		if (!nombre.isBlank()) {
			try (Connection conn = DriverManager.getConnection(dbUrl)) {
				String sql = "INSERT INTO clientes(nombre, telefono, email) VALUES (?, ?, ?)";
				PreparedStatement pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, nombre);
				pstmt.setString(2, telefono);
				pstmt.setString(3, email);
				pstmt.executeUpdate();
				nombreField.clear();
				telefonoField.clear();
				emailField.clear();
				cargarClientes(dbUrl, clientes);
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
		}
	}

	private static void cargarClientes(String dbUrl, ObservableList<Cliente> clientes) {
		clientes.clear();
		try (Connection conn = DriverManager.getConnection(dbUrl)) {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM clientes");
			while (rs.next()) {
				clientes.add(new Cliente(rs.getInt("id"), rs.getString("nombre"), rs.getString("telefono"),
						rs.getString("email")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private static void alerta(String titulo, String mensaje) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle(titulo);
		alert.setHeaderText(null);
		alert.setContentText(mensaje);
		alert.showAndWait();
	}
}
