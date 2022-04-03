package net.guides.springboot.crud.repository;

import net.guides.springboot.crud.model.Employee;
import net.guides.springboot.crud.model.EmployeeTempHistory;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EmployeeTempHistoryRepository  extends MongoRepository<EmployeeTempHistory, String> {

}
