package com.careerify.careerifyai.controller;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.careerify.careerifyai.service.GoogleGeminiService;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

@Controller
@RequestMapping("/careerify")
public class CareerifyController {

    private final GoogleGeminiService geminiService;
    private static final String MESSAGE = "message";

    @Value("${gcp.storage.bucket}")
    private String bucketName;

    @Value("${spring.ai.vertex.ai.gemini.projectId}")
    String projectID;

    @Autowired
    public CareerifyController(GoogleGeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @GetMapping("/home")
    public String getMethodName() {
        return "input";
    }

    @PostMapping("/generate")
    public String generateContent(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        try {
            return geminiService.generateContent(prompt);
        } catch (Exception e) {
            return "error";
        }
    }

    @PostMapping("/ask")
    // @Retryable(value = FailedPreconditionException.class, maxAttempts = 3, backoff = @Backoff(delay = 5, multiplier = 2))
    public String handleFileUpload(
            @RequestParam("textInput") String textInput,
            @RequestParam("imageFile") MultipartFile imageFile,
            Model model) {

        try {

            if(!textInput.isEmpty() && imageFile.isEmpty()) {
                String res = geminiService.generateContent(textInput);
                model.addAttribute(MESSAGE, res);
                return "input";
            }

            if (imageFile.isEmpty()) {
                model.addAttribute(MESSAGE, "Please upload a file!");
                return "input";
            }

            String fileName = generateUniqueFileName(imageFile.getOriginalFilename());
            String imageUri = uploadImageToGCS(imageFile, fileName);

            if (imageUri != null) {
                String res = geminiService.imageTextInput(imageUri , textInput);
                model.addAttribute(MESSAGE, "Response: " + res);
                model.addAttribute("showBack", true);
                model.addAttribute("geminiResponse", res);
                return "response";
            } else {
                model.addAttribute(MESSAGE, "File upload to GCS failed.");
                return "input";
            }

        } catch (IOException e) {
            model.addAttribute(MESSAGE, "Upload failed: " + e.getMessage());
            e.printStackTrace();
            return "input";
        }
    }

    private String generateUniqueFileName(String originalFileName) {
        String uuid = UUID.randomUUID().toString();
        String fileExtension = "";
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            fileExtension = originalFileName.substring(dotIndex);
        }
        return uuid + fileExtension;
    }

    private String uploadImageToGCS(MultipartFile imageFile, String fileName) throws IOException {
        try {
            Storage storage = StorageOptions.newBuilder().setProjectId(projectID).build().getService();

            BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, fileName).setContentType(imageFile.getContentType())
                    .build();
            Storage.BlobWriteOption precondition;
            if (storage.get(bucketName, fileName) == null) {
                precondition = Storage.BlobWriteOption.doesNotExist();
            } else {
                precondition = Storage.BlobWriteOption.generationMatch(
                        storage.get(bucketName, fileName).getGeneration());
            }
            // storage.createFrom(blobInfo, Paths.get(""), precondition);

            storage.create(blobInfo, imageFile.getInputStream(), precondition);

            return String.format("gs://%s/%s", bucketName, fileName);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
