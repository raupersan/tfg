package tfg;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.*;
import java.time.LocalDate;

public class VentaView {

	public static ScrollPane crearVistaVentas(String dbUrl) {
		VBox layout = new VBox(10);
		layout.setPadding(new Insets(10));
		layout.getChildren().add(OpenAIClient.crearBotonChatGPTResumen(dbUrl, "ventas"));

		TextField clienteField = new TextField();
		clienteField.setPromptText("Nombre del cliente");
		TextField productoIdField = new TextField();
		productoIdField.setPromptText("ID del producto");
		TextField cantidadField = new TextField();
		cantidadField.setPromptText("Cantidad");
		TextField filtroField = new TextField();
		filtroField.setPromptText("Buscar cliente o producto");

		Button registrarBtn = new Button("Registrar Venta");
		Button buscarBtn = new Button("Buscar");
		Button exportarBtn = new Button("Exportar");
		Button exportarPdfBtn = new Button("Exportar a PDF");
		Button exportarExcelBtn = new Button("Exportar a Excel");

		HBox exportarOpciones = new HBox(10);
		exportarOpciones.getChildren().addAll(exportarPdfBtn, exportarExcelBtn);
		exportarOpciones.setVisible(false);
		HBox botonesVenta = new HBox(10);
		botonesVenta.getChildren().addAll(registrarBtn);

		HBox botonesBusqueda = new HBox(10);
		botonesBusqueda.getChildren().addAll(buscarBtn, filtroField);

		HBox botonesExportar = new HBox(10);
		botonesExportar.getChildren().addAll(exportarBtn, exportarOpciones);

		exportarBtn.setOnAction(e -> exportarOpciones.setVisible(!exportarOpciones.isVisible()));

		TableView<String> tablaVentas = new TableView<>();
		TableColumn<String, String> colVenta = new TableColumn<>("Ventas Registradas");
		colVenta.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()));
		tablaVentas.getColumns().add(colVenta);

		ObservableList<String> listaVentas = FXCollections.observableArrayList();
		tablaVentas.setItems(listaVentas);

		registrarBtn.setOnAction(e -> {
			try (Connection conn = DriverManager.getConnection(dbUrl)) {
				conn.setAutoCommit(false);
				int productoId = Integer.parseInt(productoIdField.getText());
				int cantidad = Integer.parseInt(cantidadField.getText());
				String sql = "SELECT nombre, precio, stock FROM productos WHERE id = ?";
				PreparedStatement prodStmt = conn.prepareStatement(sql);
				prodStmt.setInt(1, productoId);
				ResultSet rs = prodStmt.executeQuery();
				if (rs.next()) {
					String nombreProd = rs.getString("nombre");
					double precio = rs.getDouble("precio");
					int stock = rs.getInt("stock");
					if (stock >= cantidad) {
						double total = precio * cantidad;
						String insert = "INSERT INTO ventas(cliente, producto, cantidad, total, fecha) VALUES(?, ?, ?, ?, ?)";
						PreparedStatement ventaStmt = conn.prepareStatement(insert);
						ventaStmt.setString(1, clienteField.getText());
						ventaStmt.setString(2, nombreProd);
						ventaStmt.setInt(3, cantidad);
						ventaStmt.setDouble(4, total);
						ventaStmt.setString(5, LocalDate.now().toString());
						ventaStmt.executeUpdate();

						String update = "UPDATE productos SET stock = stock - ? WHERE id = ?";
						PreparedStatement updateStmt = conn.prepareStatement(update);
						updateStmt.setInt(1, cantidad);
						updateStmt.setInt(2, productoId);
						updateStmt.executeUpdate();

						conn.commit();
						listaVentas.add(LocalDate.now() + " - " + nombreProd + " x" + cantidad + " - " + total + " €");
						clienteField.clear();
						productoIdField.clear();
						cantidadField.clear();
					} else {
						alerta("Stock insuficiente", "No hay suficiente stock disponible.");
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});

		buscarBtn.setOnAction(e -> {
			String filtro = filtroField.getText().toLowerCase();
			listaVentas.clear();
			try (Connection conn = DriverManager.getConnection(dbUrl)) {
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT * FROM ventas ORDER BY fecha DESC");
				while (rs.next()) {
					String cliente = rs.getString("cliente");
					String producto = rs.getString("producto");
					int cantidad = rs.getInt("cantidad");
					double total = rs.getDouble("total");
					String fecha = rs.getString("fecha");
					String venta = String.format("%s - %s x%d - %.2f €", fecha, producto, cantidad, total);
					if (cliente.toLowerCase().contains(filtro) || producto.toLowerCase().contains(filtro)) {
						listaVentas.add(venta);
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
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
					for (String venta : listaVentas) {
						table.addCell(venta);
					}
					document.add(table);
					document.close();
					alerta("Exportación completada", "El archivo ha sido creado exitosamente.");
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});
		exportarExcelBtn.setOnAction(e -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Guardar archivo Excel");
			fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo CSV", "*.csv"));
			File archivo = fileChooser.showSaveDialog(layout.getScene().getWindow());
			if (archivo != null) {
				try (java.io.PrintWriter writer = new java.io.PrintWriter(archivo)) {
					writer.println("Fecha,Cliente,Producto,Cantidad,Total");
					for (String venta : listaVentas) {
						writer.println(venta.replace(" - ", ",").replace(" x", ",").replace(" €", ""));
					}
					alerta("Exportación completada", "Archivo CSV creado con éxito.");
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});

		layout.getChildren().addAll(
			    clienteField, productoIdField, cantidadField,
			    botonesVenta,
			    new Label("Buscar ventas:"),
			    botonesBusqueda,
			    botonesExportar,
			    new Label("Historial de Ventas:"),
			    tablaVentas
			);


		ScrollPane scrollPane = new ScrollPane(layout);
		scrollPane.setFitToWidth(true);
		return scrollPane;
	}

	private static void alerta(String titulo, String mensaje) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle(titulo);
		alert.setHeaderText(null);
		alert.setContentText(mensaje);
		alert.showAndWait();
	}
}
