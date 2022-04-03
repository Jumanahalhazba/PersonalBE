package net.guides.springboot.crud.controller;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
//added for image
import java.io.File;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import net.guides.springboot.crud.model.EmployeeTempHistory;
import net.guides.springboot.crud.repository.EmployeeTempHistoryRepository;
import net.guides.springboot.crud.storage.StorageException;
import net.guides.springboot.crud.storage.StorageFileNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import net.guides.springboot.crud.exception.ResourceNotFoundException;
import net.guides.springboot.crud.model.Employee;
import net.guides.springboot.crud.repository.EmployeeRepository;
import net.guides.springboot.crud.service.SequenceGeneratorService;
import org.apache.commons.io.FilenameUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.view.RedirectView;
//import org.springframework.util;
//import org.apache.commons.FileUpload.util;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class EmployeeController {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmployeeTempHistoryRepository employeeTempHistoryRepository;

    @Autowired
    private SequenceGeneratorService sequenceGeneratorService;

    @GetMapping("/login")
    public String login(){return "Success";}

    @GetMapping("/employees")
    public List < Employee > getAllEmployees() {
        return employeeRepository.findAll();
    }

    @GetMapping("/employees/{id}")
    public ResponseEntity < Employee > getEmployeeById(@PathVariable(value = "id") String employeeId)
            throws ResourceNotFoundException {
        Employee employee = employeeRepository.findById(employeeId).orElseThrow(() -> new ResourceNotFoundException("Employee not found for this id :: " + employeeId));
        return ResponseEntity.ok().body(employee);
    }

    @PostMapping("/employees/save")
    public Path saveUser(@ModelAttribute Employee employee, @ModelAttribute MultipartFile multipartFile) throws IOException {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+2"));

        Date date = new Date();
        employee.setDateUpdated(date);

        Employee savedUser = employeeRepository.save(employee);

        EmployeeTempHistory history = new EmployeeTempHistory(savedUser, date, Double.parseDouble(savedUser.getTemp()));
        employeeTempHistoryRepository.save(history);

        String uploadDir = "/user-photos/" + savedUser.getId();

        Path path = Paths.get(uploadDir);

        try {
            Path destinationFile =
            path.resolve(
                            Paths.get(multipartFile.getOriginalFilename()))
                    .normalize().toAbsolutePath();

            if (!destinationFile.getParent().equals(path.toAbsolutePath())) {
                // This is a security check
                throw new StorageException(
                        "Cannot store file outside current directory.");
            }

            if(!(new File(String.valueOf(destinationFile.getParent())).exists()))
                new File(String.valueOf(destinationFile.getParent() )).mkdir();

            try (InputStream inputStream = multipartFile.getInputStream()) {
                Files.copy(inputStream, destinationFile,
                        StandardCopyOption.REPLACE_EXISTING);

                return destinationFile;
            }
        } catch (IOException e) {
            throw new StorageException("Failed to store file.", e);
        }
    }

    @RequestMapping(value = "/user-photos/**", method = RequestMethod.GET)
    public ResponseEntity<Resource> downloadFile(HttpServletRequest request) {
        String url = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        url = url.replaceFirst("/api/v1/user-photos/", "").replaceAll("%20", " ");

        String path = "C:/user-photos/" + url;

        Resource file = loadAsResource(path);

        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getFilename() + "\"").body(file);
    }

    public Resource loadAsResource(String filename) {
        try {
            Path file = Path.of(filename);

            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new StorageFileNotFoundException(
                        "Could not read file: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new StorageFileNotFoundException("Could not read file: " + filename, e);
        }
    }

    @PutMapping("/employees/{id}")
    public ResponseEntity < Employee > updateEmployee(@PathVariable(value = "id") String employeeId,
                                                      @Valid @ModelAttribute Employee employeeDetails, @ModelAttribute MultipartFile multipartFile) throws ResourceNotFoundException, ParseException {
        Employee employee = employeeRepository.findById(employeeId).orElseThrow(() -> new ResourceNotFoundException("Employee not found for this id :: " + employeeId));

        TimeZone.setDefault(TimeZone.getTimeZone("GMT+2"));
        Date date = new Date();

        // update temp history
        if(Double.parseDouble(employee.getTemp()) != Double.parseDouble(employeeDetails.getTemp())) {
            EmployeeTempHistory history = new EmployeeTempHistory(employee, date, Double.parseDouble(employeeDetails.getTemp()));
            employeeTempHistoryRepository.save(history);
        }

        employee.setEmailId(employeeDetails.getEmailId());
        employee.setLastName(employeeDetails.getLastName());
        employee.setFirstName(employeeDetails.getFirstName());
        employee.setTitle(employeeDetails.getTitle());
        employee.setTemp(employeeDetails.getTemp());

        employee.setDateUpdated(date);

        if(multipartFile != null){
            String uploadDir = "/user-photos/" + employeeDetails.getId();

            //FileUploadUtil.saveFile(uploadDir, fileName, multipartFile);
            Path path = Paths.get(uploadDir);

            try {
                Path destinationFile =
                        path.resolve(
                                        Paths.get(multipartFile.getOriginalFilename()))
                                .normalize().toAbsolutePath();

                if (!destinationFile.getParent().equals(path.toAbsolutePath())) {
                    // This is a security check
                    throw new StorageException(
                            "Cannot store file outside current directory.");
                }

                if(!(new File(String.valueOf(destinationFile.getParent())).exists()))
                    new File(String.valueOf(destinationFile.getParent() )).mkdir();

                try (InputStream inputStream = multipartFile.getInputStream()) {
                    Files.copy(inputStream, destinationFile,
                            StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                System.out.println(e.toString());
                throw new StorageException("Failed to store file.", e);
            }
        }

        final Employee updatedEmployee = employeeRepository.save(employee);
        return ResponseEntity.ok(updatedEmployee);
    }

    @DeleteMapping("/employees/{id}")
    public Map < String, Boolean > deleteEmployee(@PathVariable(value = "id") String employeeId)
            throws ResourceNotFoundException {
        Employee employee = employeeRepository.findById(employeeId).orElseThrow(() -> new ResourceNotFoundException("Employee not found for this id :: " + employeeId));

        employeeRepository.delete(employee);
        Map < String, Boolean > response = new HashMap < > ();
        response.put("deleted", Boolean.TRUE);
        return response;
    }

    @GetMapping("/employees/history/{id}")
    public List<EmployeeTempHistory> getEmployeeHistory(@PathVariable(value = "id") String employeeId)
            throws ResourceNotFoundException {
        List<EmployeeTempHistory> list = employeeTempHistoryRepository.findAll();

        list = list.stream().filter(x ->  x.getEmployee().getId().equals(employeeId)).toList();

        if(list.size() > 7)
            return list.subList(list.size() - 8, list.size() - 1);

        return list;
    }
}