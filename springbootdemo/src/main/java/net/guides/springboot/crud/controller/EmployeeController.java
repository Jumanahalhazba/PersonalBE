package net.guides.springboot.crud.controller;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//added for image
import java.io.File;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

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
    private SequenceGeneratorService sequenceGeneratorService;

    @GetMapping("/login")
    public String login(){return "Success";}

    @GetMapping("/employees")
    public List < Employee > getAllEmployees() {
        return employeeRepository.findAll();
    }

    @GetMapping("/employees/{id}")
    public ResponseEntity < Employee > getEmployeeById(@PathVariable(value = "id") Long employeeId)
            throws ResourceNotFoundException {
        Employee employee = employeeRepository.findById(employeeId).orElseThrow(() -> new ResourceNotFoundException("Employee not found for this id :: " + employeeId));
        return ResponseEntity.ok().body(employee);
    }
    @CrossOrigin(origins = "http://localhost:3000", maxAge = 3600)
    @PostMapping("/employees")
    public Employee createEmployee(@Valid @RequestBody Employee employee) {
        System.out.println((employee));
        employee.setId(sequenceGeneratorService.generateSequence(Employee.SEQUENCE_NAME));
        return employeeRepository.save(employee);
    }
    @PostMapping("/employees/save")
    public Path saveUser(@ModelAttribute Employee employee, @ModelAttribute MultipartFile multipartFile) throws IOException {
        System.out.println("Filename = " + multipartFile.getOriginalFilename());

        System.out.println("yeeeeeeeeeeeeee");
        //String fileName = StringUtils.cleanPath(multipartFile.getOriginalFilename());
        //employee.setTitle(fileName);

        System.out.println("employee = " + employee);

        Random rnd = new Random();
        long rn = rnd.nextLong(456789098  );
        System.out.println("rnd = " + rn);

        employee.setId(rn);
        System.out.println("employee = " + employee.getId());


        Employee savedUser = employeeRepository.save(employee);

        System.out.println("savedUser = " + savedUser);

        String uploadDir = "/user-photos/" + savedUser.getId();

        //FileUploadUtil.saveFile(uploadDir, fileName, multipartFile);
        Path path = Paths.get(uploadDir);
        System.out.println("path = " + String.valueOf(path));

        try {
            Path destinationFile =
            path.resolve(
                            Paths.get(multipartFile.getOriginalFilename()))
                    .normalize().toAbsolutePath();

            System.out.println("destinationFile = " + destinationFile);

            System.out.println(destinationFile.getParent());
            System.out.println(destinationFile);

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
            System.out.println(e.toString());
            throw new StorageException("Failed to store file.", e);
        }
    }

    @RequestMapping(value = "/user-photos/**", method = RequestMethod.GET)
    public ResponseEntity<Resource> downloadFile(HttpServletRequest request) {
        String url = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        url = url.replaceFirst("/api/v1/user-photos/", "");

        System.out.println("Url = " + url);

        String path = "C:/user-photos/" + url;

        Resource file = loadAsResource(path);

        System.out.println("path = " + path);

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
    public ResponseEntity < Employee > updateEmployee(@PathVariable(value = "id") Long employeeId,
                                                      @Valid @ModelAttribute Employee employeeDetails, @ModelAttribute MultipartFile multipartFile) throws ResourceNotFoundException {
        Employee employee = employeeRepository.findById(employeeId).orElseThrow(() -> new ResourceNotFoundException("Employee not found for this id :: " + employeeId));

        employee.setEmailId(employeeDetails.getEmailId());
        employee.setLastName(employeeDetails.getLastName());
        employee.setFirstName(employeeDetails.getFirstName());
        employee.setTitle(employeeDetails.getTitle());
        employee.setTemp(employeeDetails.getTemp());

        if(multipartFile != null){
            String uploadDir = "/user-photos/" + employeeDetails.getId();

            //FileUploadUtil.saveFile(uploadDir, fileName, multipartFile);
            Path path = Paths.get(uploadDir);
            System.out.println("path = " + String.valueOf(path));

            try {
                Path destinationFile =
                        path.resolve(
                                        Paths.get(multipartFile.getOriginalFilename()))
                                .normalize().toAbsolutePath();

                System.out.println("destinationFile = " + destinationFile);

                System.out.println(destinationFile.getParent());
                System.out.println(destinationFile);

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
    public Map < String, Boolean > deleteEmployee(@PathVariable(value = "id") Long employeeId)
            throws ResourceNotFoundException {
        Employee employee = employeeRepository.findById(employeeId).orElseThrow(() -> new ResourceNotFoundException("Employee not found for this id :: " + employeeId));

        employeeRepository.delete(employee);
        Map < String, Boolean > response = new HashMap < > ();
        response.put("deleted", Boolean.TRUE);
        return response;
    }
//    //image add
//    private static String imageDirectory = System.getProperty("user.dir") + "/images/";
//    //@GetMapping(value = "/image", produces = {MediaType.IMAGE_PNG_VALUE, "application/json"})
//    @GetMapping("/image")
//    public String test(){return "Success";}
//    public ResponseEntity<?> uploadImage(@RequestParam("imageFile")MultipartFile file,
//                                         @RequestParam("imageName") String name) {
//        makeDirectoryIfNotExist(imageDirectory);
//        Path fileNamePath = Paths.get(imageDirectory,
//                name.concat(".").concat(FilenameUtils.getExtension(file.getOriginalFilename())));
//        try {
//            Files.write(fileNamePath, file.getBytes());
//            return new ResponseEntity<>(name, HttpStatus.CREATED);
//        } catch (IOException ex) {
//            return new ResponseEntity<>("Image is not uploaded", HttpStatus.BAD_REQUEST);
//        }
//    }
//
//    private void makeDirectoryIfNotExist(String imageDirectory) {
//        File directory = new File(imageDirectory);
//        if (!directory.exists()) {
//            directory.mkdir();
//        }
//    }
}