package analysis.data.controller;
import analysis.data.service.FileProcessingService;
import analysis.data.service.FileProcessingService2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class FileProcessingController {

    @Autowired
    private FileProcessingService fileProcessingService;

    @GetMapping("/process")
    public String processFiles() {
        fileProcessingService.processFiles();
        return "Files processed successfully.";
    }
}
