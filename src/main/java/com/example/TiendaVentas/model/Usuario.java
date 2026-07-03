package com.example.TiendaVentas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Modelo sencillo para practicar envio de datos entre controlador y JSP.
@Entity
@Table(name = "usuarios")
public class Usuario {

    public static final String ROL_CLIENTE = "CLIENTE";
    public static final String ROL_ADMINISTRADOR = "ADMINISTRADOR";
    public static final String ROL_REPARTIDOR = "REPARTIDOR";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String nombre;
    private String apellido;
    @Column(unique = true, nullable = false)
    private String correo;
    @Column(nullable = false)
    private String dni;
    private String telefono;
    @Column(nullable = false)
    private String password;
    @Column(nullable = false)
    private String rol;
    @Column(name = "administrador_principal")
    private boolean administradorPrincipal;

    public Usuario() {
    }

    public Usuario(int id, String nombre, String correo, String rol) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.rol = normalizarRol(rol);
    }

    public Usuario(int id, String nombre, String correo, String password, String rol) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.password = password;
        this.rol = normalizarRol(rol);
    }

    public Usuario(int id, String nombre, String correo, String dni, String password, String rol) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.dni = dni;
        this.password = password;
        this.rol = normalizarRol(rol);
    }

    public Usuario(int id, String nombre, String apellido, String correo, String dni,
                   String telefono, String password, String rol) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.correo = correo;
        this.dni = dni;
        this.telefono = telefono;
        this.password = password;
        this.rol = normalizarRol(rol);
    }

    public Usuario(int id, String nombre, String apellido, String correo, String dni,
                   String telefono, String password, String rol, boolean administradorPrincipal) {
        this(id, nombre, apellido, correo, dni, telefono, password, rol);
        this.administradorPrincipal = administradorPrincipal;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = normalizarRol(rol);
    }

    public boolean isAdministradorPrincipal() {
        return administradorPrincipal;
    }

    public void setAdministradorPrincipal(boolean administradorPrincipal) {
        this.administradorPrincipal = administradorPrincipal;
    }

    public String getNombreCompleto() {
        if (apellido == null || apellido.trim().isEmpty()) {
            return nombre;
        }

        return nombre + " " + apellido;
    }

    public boolean isCliente() {
        return ROL_CLIENTE.equals(rol);
    }

    public boolean isAdministrador() {
        return ROL_ADMINISTRADOR.equals(rol);
    }

    public boolean isRepartidor() {
        return ROL_REPARTIDOR.equals(rol);
    }

    public String getRolTexto() {
        if (isAdministrador()) {
            return "Administrador";
        }

        if (isRepartidor()) {
            return "Repartidor";
        }

        return "Cliente";
    }

    public static String normalizarRol(String rol) {
        if (rol == null) {
            return ROL_CLIENTE;
        }

        if ("Administrador".equalsIgnoreCase(rol) || ROL_ADMINISTRADOR.equalsIgnoreCase(rol)) {
            return ROL_ADMINISTRADOR;
        }

        if ("Repartidor".equalsIgnoreCase(rol) || ROL_REPARTIDOR.equalsIgnoreCase(rol)) {
            return ROL_REPARTIDOR;
        }

        return ROL_CLIENTE;
    }
}
