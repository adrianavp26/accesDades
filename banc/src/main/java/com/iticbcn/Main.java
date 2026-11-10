package com.iticbcn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool.PoolInitializationException;

public class Main {
    public static void main(String[] args) {

        Properties prop = new Properties();
        try {
            prop.load(Files.newBufferedReader(Path.of("src", "main", "resources", "db.properties")));
        } catch (IOException ex) {
            System.getLogger(Main.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }

        HikariConfig hkcfg = new HikariConfig();

        //parametria de la connexió
        hkcfg.setJdbcUrl(prop.getProperty("DB_URL"));
        hkcfg.setUsername(prop.getProperty("DB_USER"));
        hkcfg.setPassword(prop.getProperty("DB_PASSWORD"));
        hkcfg.setMaximumPoolSize(Integer.parseInt(prop.getProperty("DB_MAX_POOL")));

        try (HikariDataSource hkds = new HikariDataSource(hkcfg)) {
            try (Connection cnn = hkds.getConnection()) {
                for (String resultat : readAllStatements(cnn)) {
                    System.out.println("moviment amb id: " + resultat);
                }
                //ejercicio 1
                readStatementsByImport(cnn, 100.0);
                //exercici2:
                Scanner sc = new Scanner(System.in);
                List<String> dades = demanarDadesClient(sc);
                insertClient(cnn, dades);
            } catch (SQLException e) {
                System.out.println("No connectat!");
                e.printStackTrace();
            }
        } catch (PoolInitializationException e) {
            System.out.println("No s'ha pogut connectar a la base de dades: " + e.getMessage());
        }
    }

        public static List<String> readAllStatements (Connection conn) throws SQLException {
            List<String> resultatQuery = new ArrayList<String>();
            String query = "SELECT * FROM statements";
            PreparedStatement ps = conn.prepareStatement(query);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String resultat = String.join(";",rs.getString("id_statement"),
                                                                rs.getString("tipus_moviment"),
                                                                rs.getString("import"),
                                                                rs.getString("divisa"));
                    resultatQuery.add(resultat);
                }
            }
            return resultatQuery;
        }

        //excercici 1
        public static void readStatementsByImport(Connection conn, double quantitat) throws SQLException {
            String query = "SELECT * FROM statements WHERE import >= ?";

            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setDouble(1, quantitat);

                try (ResultSet rs = ps.executeQuery()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    String header = String.join(";",
                        meta.getColumnName(1),
                        meta.getColumnName(3),
                        meta.getColumnName(8),
                        meta.getColumnName(10));
                    System.out.println(header);

                    while (rs.next()) {
                        String resultat = String.join(";",
                            rs.getString("id_statement"),
                            rs.getString("tipus_moviment"),
                            rs.getString("import"),
                            rs.getString("divisa"));
                        System.out.println(resultat);
                    }
                }
            }
        }

        //exercici 2
        public static List<String> demanarDadesClient(Scanner sc) {
            List<String> dades = new ArrayList<>();
            System.out.print("Nom: ");
            dades.add(sc.nextLine().trim());
            System.out.print("Cognoms: ");
            dades.add(sc.nextLine().trim());
            System.out.print("Id fiscal: ");
            dades.add(sc.nextLine().trim());
            return dades;
        }
            //per mirar si el client existeix o no 
        public static boolean existeixClient(Connection conn, String idFiscal) throws SQLException {
            String query = "SELECT 1 FROM clients WHERE id_fiscal = ?";
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, idFiscal);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        }
            //per inserir les dades que s'han demanat dins del client adient
        public static void insertClient(Connection conn, List<String> dades) throws SQLException {
            if (existeixClient(conn, dades.get(2))) {
                System.out.println("El client ja existeix, no s'insereix.");
                return;
            }

            String insert = "INSERT INTO clients (nom, cognoms, id_fiscal) VALUES (?,?,?)";
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(insert)) {
                ps.setString(1, dades.get(0));
                ps.setString(2, dades.get(1));
                ps.setString(3, dades.get(2));
                ps.executeUpdate();
                conn.commit();
                System.out.println("Client inserit correctament.");
            } catch (SQLException e) {
                conn.rollback();
                System.out.println("Error en la inserció, s'ha fet rollback.");
                e.printStackTrace();
            } finally {
                conn.setAutoCommit(true);
            }
        }


    
}