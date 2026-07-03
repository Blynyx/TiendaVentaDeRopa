package com.example.TiendaVentas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// POJO simple: solo atributos, constructores, getters y setters.
@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(nullable = false)
    private String nombre;
    private String descripcion;
    private double precio;
    @ManyToOne
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;
    private int stock;
    private String imagen;
    private boolean activo = true;

    public Producto() {
    }

    public Producto(int id, String nombre, double precio, String categoria) {
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
        setCategoria(categoria);
        this.activo = true;
    }

    public Producto(int id, String nombre, String descripcion, double precio, String categoria, int stock, String imagen) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        setCategoria(categoria);
        this.stock = stock;
        this.imagen = imagen;
        this.activo = true;
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

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public String getCategoria() {
        return categoria == null ? "" : categoria.getNombre();
    }

    public void setCategoria(String categoria) {
        if (categoria == null || categoria.trim().isEmpty()) {
            this.categoria = null;
            return;
        }

        this.categoria = new Categoria(0, categoria, "");
    }

    public Categoria getCategoriaEntidad() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public boolean isVisibleEnCatalogo() {
        return activo && categoria != null && categoria.isActiva();
    }

    public boolean isCategoriaActiva() {
        return categoria != null && categoria.isActiva();
    }

    public boolean isDesactivadoPorCategoria() {
        return activo && !isCategoriaActiva();
    }
}
