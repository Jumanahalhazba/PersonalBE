package net.guides.springboot.crud.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "EmployeeTempHistory")
public class EmployeeTempHistory {
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public double getTemp() {
        return temp;
    }

    public void setTemp(double temp) {
        this.temp = temp;
    }

    public EmployeeTempHistory(Employee employee, Date date, double temp) {
        this.employee = employee;
        this.date = date;
        this.temp = temp;
    }

    @Id
    private String id;
    @DBRef
    private Employee employee;
    private Date date;

    private double temp;
}
